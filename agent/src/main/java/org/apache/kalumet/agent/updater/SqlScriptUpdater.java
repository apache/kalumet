/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.kalumet.agent.updater;

import org.apache.commons.vfs.FileObject;
import org.apache.kalumet.FileManipulator;
import org.apache.kalumet.FileManipulatorException;
import org.apache.kalumet.KalumetException;
import org.apache.kalumet.agent.Configuration;
import org.apache.kalumet.agent.utils.EventUtils;
import org.apache.kalumet.model.*;
import org.apache.kalumet.model.update.UpdateLog;
import org.apache.kalumet.model.update.UpdateMessage;
import org.apache.kalumet.utils.NotifierUtils;
import org.apache.kalumet.utils.PublisherUtils;
import org.apache.kalumet.utils.SqlScriptUtils;
import org.apache.kalumet.utils.VariableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * SQL script updater.
 */
public class SqlScriptUpdater {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(SqlScriptUpdater.class);

    /**
     * Executes SQL script.
     *
     * @param environment the target <code>Environment</code>.
     * @param server      the target <code>J2EEApplicationServer</code>.
     * @param application the target <code>J2EEApplication</code>.
     * @param database    the target <code>Database</code>.
     * @param sqlScript   the target <code>SqlScript</code>.
     * @param updateLog   the <code>UpdateLog</code> to use.
     * @throws UpdateException in case of update failure.
     */
    public static void execute(Environment environment, J2EEApplicationServer server, J2EEApplication application, Database database, SqlScript sqlScript, UpdateLog updateLog) throws UpdateException {
        LOGGER.info("Executing SQL script {}", sqlScript.getName());
        updateLog.addUpdateMessage(new UpdateMessage("info", "Executing SQL script " + sqlScript.getName()));
        EventUtils.post(environment, "UPDATE", "Executing SQL script " + sqlScript.getName());

        if (!sqlScript.isActive()) {
            // SQL script is not active
            LOGGER.info("SQL Script {} is inactive, so not executed", sqlScript.getName());
            updateLog.addUpdateMessage(new UpdateMessage("info", "SQL Script " + sqlScript.getName() + " is inactive, so not executed"));
            EventUtils.post(environment, "UPDATE", "SQL Script " + sqlScript.getName() + " is inactive, so not executed");
            return;
        }

        // construct the SQL script URI
        String sqlScriptUri = VariableUtils.replace(sqlScript.getUri(), environment.getVariables());
        if (!FileManipulator.protocolExists(sqlScriptUri)) {
            // the SQL script URI is relative , construct the SQL Script URI using
            // the J2EE Application URI
            LOGGER.debug("SQL Script URI is relative to J2EE application URI");
            sqlScriptUri = FileManipulator.format(VariableUtils.replace(application.getUri(), environment.getVariables())) + "!/" + sqlScriptUri;
        }
        // get the application cache directory
        String applicationCacheDir = null;
        try {
            applicationCacheDir = FileManipulator.createJ2EEApplicationCacheDir(environment, application);
        } catch (FileManipulatorException fileManipulatorException) {
            LOGGER.error("Can't initialize J2EE application cache directory", fileManipulatorException);
            throw new UpdateException("Can't initialize J2EE application cache directory", fileManipulatorException);
        }

        // get file manipulator instance
        FileManipulator fileManipulator = null;
        try {
            fileManipulator = FileManipulator.getInstance();
        } catch (FileManipulatorException fileManipulatorException) {
            LOGGER.error("Can't initialize the file manipulator", fileManipulatorException);
            throw new UpdateException("Can't initialize the file manipulator", fileManipulatorException);
        }

        // copy the SQL script in the application working directory
        String sqlScriptCache = applicationCacheDir + "/sql/" + sqlScript.getName() + ".cache";
        String sqlScriptRuntime = applicationCacheDir + "/sql/" + sqlScript.getName();
        try {
            fileManipulator.copy(sqlScriptUri, sqlScriptCache);
        } catch (FileManipulatorException fileManipulatorException) {
            LOGGER.error("Can't copy the SQL script from {} to {}", new Object[]{sqlScriptUri, sqlScriptCache}, fileManipulatorException);
            throw new UpdateException("Can't copy the SQL script from " + sqlScriptUri + " to " + sqlScriptCache, fileManipulatorException);
        }

        if (fileManipulator.isFolder(sqlScriptCache)) {
            // TODO add a generic method to reuse in the case of directory

            // the user provided a directory
            updateLog.addUpdateMessage(new UpdateMessage("info", sqlScript.getName() + " is a folder, iterate in the SQL scripts"));
            EventUtils.post(environment, "UPDATE", sqlScript.getName() + " is a folder, iterate in the SQL scripts");
            LOGGER.info(sqlScript.getName() + " is a folder, iterate in the SQL scripts");
            FileObject[] children = fileManipulator.browse(sqlScriptCache);
            for (int i = 0; i < children.length; i++) {
                FileObject current = children[i];
                String name = current.getName().getBaseName();
                String singleSqlScriptCache = sqlScriptCache + "/" + name;
                String singleSqlScriptRuntime = sqlScriptRuntime + "/" + name;
                // change mappings in the current SQL script
                for (Iterator mappingIterator = sqlScript.getMappings().iterator(); mappingIterator.hasNext(); ) {
                    Mapping mapping = (Mapping) mappingIterator.next();
                    FileManipulator.searchAndReplace(mapping.getKey(), VariableUtils.replace(mapping.getValue(), environment.getVariables()), singleSqlScriptCache);
                }
                try {
                    if (sqlScript.isForce() || (!fileManipulator.contentEquals(singleSqlScriptCache, singleSqlScriptRuntime))) {
                        fileManipulator.copy(singleSqlScriptCache, singleSqlScriptRuntime);
                        if (database.getSqlCommand() != null && database.getSqlCommand().trim().length() > 0) {
                            // execute SQL script using system command
                            String command = VariableUtils.replace(database.getSqlCommand(), environment.getVariables());
                            String output = SqlScriptUtils.executeUsingCommand(singleSqlScriptRuntime, command);
                            updateLog.addUpdateMessage(new UpdateMessage("info", "SQL script " + name + " executed: " + output));
                            EventUtils.post(environment, "UPDATE", "SQL script " + name + " executed: " + output);
                            LOGGER.info("SQL script " + name + " executed: " + output);
                        } else {
                            // execute SQL script using JDBC
                            String user = null;
                            String password = null;
                            String driver = null;
                            String url = null;
                            if (database.getConnectionPool() != null && database.getConnectionPool().trim().length() > 0) {
                                // the database is linked to a connection pool
                                // looking for the connection pool (from the cache)
                                String connectionPoolName = VariableUtils.replace(database.getConnectionPool(), environment.getVariables());
                                JDBCConnectionPool connectionPool = server.getJDBCConnectionPool(connectionPoolName);
                                if (connectionPool == null) {
                                    LOGGER.error("JDBC connection pool {} is not found in J2EE application server {}", database.getConnectionPool(), server.getName());
                                    throw new UpdateException("JDBC connection pool " + database.getConnectionPool() + " is not found in J2EE application server " + server.getName());
                                }
                                user = VariableUtils.replace(connectionPool.getUser(), environment.getVariables());
                                password = VariableUtils.replace(connectionPool.getPassword(), environment.getVariables());
                                driver = VariableUtils.replace(connectionPool.getDriver(), environment.getVariables());
                                url = VariableUtils.replace(connectionPool.getUrl(), environment.getVariables());
                            } else {
                                // use the database connection data
                                user = VariableUtils.replace(database.getUser(), environment.getVariables());
                                password = VariableUtils.replace(database.getPassword(), environment.getVariables());
                                driver = VariableUtils.replace(database.getDriver(), environment.getVariables());
                                url = VariableUtils.replace(database.getJdbcurl(), environment.getVariables());
                            }
                            // execute SQL script using JDBC
                            SqlScriptUtils.executeUsingJdbc(singleSqlScriptRuntime, driver, user, password, url);
                        }
                        // add message
                        updateLog.setStatus("Update performed");
                        updateLog.setUpdated(true);
                        updateLog.addUpdateMessage(new UpdateMessage("info", "SQL script " + sqlScript.getName() + " executed"));
                        EventUtils.post(environment, "UPDATE", "SQL script " + sqlScript.getName() + " executed");
                        LOGGER.info("SQL script {} executed", sqlScript.getName());
                    }
                } catch (Exception e) {
                    // SQL script execution failed, delete the SQL script from the cache
                    try {
                        fileManipulator.delete(sqlScriptRuntime);
                    } catch (FileManipulatorException fileManipulatorException) {
                        LOGGER.warn("Can't delete {}/sql/{}", new Object[]{applicationCacheDir, sqlScript.getName()}, fileManipulatorException);
                    }
                    LOGGER.error("SQL script {} execution failed", sqlScript.getName(), e);
                    throw new UpdateException("SQL script " + sqlScript.getName() + " execution failed", e);
                }
            }
        } else {
            // the user provided a single SQL script

            // change mappings in the SQL script
            for (Iterator mappingIterator = sqlScript.getMappings().iterator(); mappingIterator.hasNext(); ) {
                Mapping mapping = (Mapping) mappingIterator.next();
                FileManipulator.searchAndReplace(mapping.getKey(), VariableUtils.replace(mapping.getValue(), environment.getVariables()), sqlScriptCache);
            }

            // compare the SQL script with the target one
            try {
                if (sqlScript.isForce() || (!fileManipulator.contentEquals(sqlScriptCache, sqlScriptRuntime))) {
                    // the SQL script needs to be updated and executed
                    // copy the SQL script to the target
                    fileManipulator.copy(sqlScriptCache, sqlScriptRuntime);
                    if (database.getSqlCommand() != null && database.getSqlCommand().trim().length() > 0) {
                        // execute SQL script using system command
                        String command = VariableUtils.replace(database.getSqlCommand(), environment.getVariables());
                        String output = SqlScriptUtils.executeUsingCommand(sqlScriptRuntime, command);
                        updateLog.addUpdateMessage(new UpdateMessage("info", "SQL script " + sqlScript.getName() + " executed: " + output));
                        EventUtils.post(environment, "UPDATE", "SQL script " + sqlScript.getName() + " executed: " + output);
                        LOGGER.info("SQL script " + sqlScript.getName() + " executed: " + output);
                    } else {
                        // execute SQL script using JDBC
                        String user = null;
                        String password = null;
                        String driver = null;
                        String url = null;
                        if (database.getConnectionPool() != null && database.getConnectionPool().trim().length() > 0) {
                            // the database is linked to a connection pool
                            // looking for the connection pool (from the cache)
                            String connectionPoolName = VariableUtils.replace(database.getConnectionPool(), environment.getVariables());
                            JDBCConnectionPool connectionPool = server.getJDBCConnectionPool(connectionPoolName);
                            if (connectionPool == null) {
                                LOGGER.error("JDBC connection pool {} is not found in J2EE application server {}", database.getConnectionPool(), server.getName());
                                throw new UpdateException("JDBC connection pool " + database.getConnectionPool() + " is not found in J2EE application server " + server.getName());
                            }
                            user = VariableUtils.replace(connectionPool.getUser(), environment.getVariables());
                            password = VariableUtils.replace(connectionPool.getPassword(), environment.getVariables());
                            driver = VariableUtils.replace(connectionPool.getDriver(), environment.getVariables());
                            url = VariableUtils.replace(connectionPool.getUrl(), environment.getVariables());
                        } else {
                            // use the database connection data
                            user = VariableUtils.replace(database.getUser(), environment.getVariables());
                            password = VariableUtils.replace(database.getPassword(), environment.getVariables());
                            driver = VariableUtils.replace(database.getDriver(), environment.getVariables());
                            url = VariableUtils.replace(database.getJdbcurl(), environment.getVariables());
                        }
                        // execute SQL script using JDBC
                        SqlScriptUtils.executeUsingJdbc(sqlScriptRuntime, driver, user, password, url);
                    }
                    // add message
                    updateLog.setStatus("Update performed");
                    updateLog.setUpdated(true);
                    updateLog.addUpdateMessage(new UpdateMessage("info", "SQL script " + sqlScript.getName() + " executed"));
                    EventUtils.post(environment, "UPDATE", "SQL script " + sqlScript.getName() + " executed");
                    LOGGER.info("SQL script " + sqlScript.getName() + " executed");
                }
            } catch (Exception e) {
                // SQL script execution failed, delete the SQL script from the cache
                try {
                    fileManipulator.delete(sqlScriptRuntime);
                } catch (FileManipulatorException fileManipulatorException) {
                    LOGGER.warn("Can't delete {}/sql/{}", new Object[]{applicationCacheDir, sqlScript.getName()}, fileManipulatorException);
                }
                LOGGER.error("SQL script {} execution failed", sqlScript.getName(), e);
                throw new UpdateException("SQL script " + sqlScript.getName() + " execution failed", e);
            }
        }

        // change mappings in the SQL scripts
        for (Iterator mappingIterator = sqlScript.getMappings().iterator(); mappingIterator.hasNext(); ) {
            Mapping mapping = (Mapping) mappingIterator.next();
            FileManipulator.searchAndReplace(mapping.getKey(), VariableUtils.replace(mapping.getValue(), environment.getVariables()), sqlScriptCache);
        }

        // compare the SQL script with the target one
        try {
            if (sqlScript.isForce() || (!fileManipulator.contentEquals(sqlScriptCache, sqlScriptRuntime))) {
                // the SQL script needs to be updated and executed
                // copy the SQL script to the target
                fileManipulator.copy(sqlScriptCache, sqlScriptRuntime);
                if (database.getSqlCommand() != null && database.getSqlCommand().trim().length() > 0) {
                    // execute SQL script using system command
                    String command = VariableUtils.replace(database.getSqlCommand(), environment.getVariables());
                    String output = SqlScriptUtils.executeUsingCommand(applicationCacheDir + "/sql/" + sqlScript.getName(), command);
                    updateLog.addUpdateMessage(new UpdateMessage("info", "SQL script " + sqlScript.getName() + " executed: " + output));
                    EventUtils.post(environment, "UPDATE", "SQL script " + sqlScript.getName() + " executed: " + output);
                    LOGGER.info("SQL script {} executed: {}", sqlScript.getName(), output);
                } else {
                    // execute SQL script using JDBC
                    String user = null;
                    String password = null;
                    String driver = null;
                    String url = null;
                    if (database.getConnectionPool() != null && database.getConnectionPool().trim().length() > 0) {
                        // the database is linked to a connection pool
                        // looking for the connection pool (from the cache)
                        String connectionPoolName = VariableUtils.replace(database.getConnectionPool(), environment.getVariables());
                        JDBCConnectionPool connectionPool = server.getJDBCConnectionPool(connectionPoolName);
                        if (connectionPool == null) {
                            LOGGER.error("JDBC connection pool {} is not found in J2EE application server {}", database.getConnectionPool(), server.getName());
                            throw new UpdateException("JDBC connection pool " + database.getConnectionPool() + " is not found in J2EE application server " + server.getName());
                        }
                        user = VariableUtils.replace(connectionPool.getUser(), environment.getVariables());
                        password = VariableUtils.replace(connectionPool.getPassword(), environment.getVariables());
                        driver = VariableUtils.replace(connectionPool.getDriver(), environment.getVariables());
                        url = VariableUtils.replace(connectionPool.getUrl(), environment.getVariables());
                    } else {
                        // use the database connection data
                        user = VariableUtils.replace(database.getUser(), environment.getVariables());
                        password = VariableUtils.replace(database.getPassword(), environment.getVariables());
                        driver = VariableUtils.replace(database.getDriver(), environment.getVariables());
                        url = VariableUtils.replace(database.getJdbcurl(), environment.getVariables());
                    }
                    // execute SQL script using JDBC
                    SqlScriptUtils.executeUsingJdbc(sqlScriptRuntime, driver, user, password, url);
                }
                // add message
                updateLog.setStatus("Update performed");
                updateLog.setUpdated(true);
                updateLog.addUpdateMessage(new UpdateMessage("info", "SQL script " + sqlScript.getName() + " executed"));
                EventUtils.post(environment, "UPDATE", "SQL script " + sqlScript.getName() + " executed");
                LOGGER.info("SQL script {} executed", sqlScript.getName());
            }
        } catch (Exception e) {
            // SQL script execution failed, delete the SQL script from the cache
            try {
                fileManipulator.delete(sqlScriptRuntime);
            } catch (FileManipulatorException fileManipulatorException) {
                LOGGER.warn("Can't delete {}/sql/{}", new Object[]{applicationCacheDir, sqlScript.getName()}, fileManipulatorException);
            }
            LOGGER.error("SQL script {} execution failed", sqlScript.getName(), e);
            throw new UpdateException("SQL script " + sqlScript.getName() + " execution failed", e);
        }
    }

    /**
     * Wrapper method to execute a SQL script via WS.
     *
     * @param environmentName the target environment name.
     * @param serverName      the target J2EE application server name.
     * @param applicationName the target J2EE application name.
     * @param databaseName    the target database name.
     * @param sqlScriptName   the target SQL script name.
     * @throws KalumetException in case of execution failure.
     */
    public static void execute(String environmentName, String serverName, String applicationName, String databaseName, String sqlScriptName) throws KalumetException {
        LOGGER.info("SQL script {} execution requested by WS", sqlScriptName);

        // load configuration
        LOGGER.debug("Loading configuration");
        Kalumet kalumet = Kalumet.digeste(Configuration.CONFIG_LOCATION);

        // looking for component objects
        LOGGER.debug("Looking for component objects");
        Environment environment = kalumet.getEnvironment(environmentName);
        if (environment == null) {
            LOGGER.error("Environment {} is not found in the configuration", environmentName);
            throw new KalumetException("Environment " + environmentName + " is not found in the configuration");
        }
        J2EEApplicationServer applicationServer = environment.getJ2EEApplicationServers().getJ2EEApplicationServer(serverName);
        if (applicationServer == null) {
            LOGGER.error("J2EE application server {} is not found in environment {}", serverName, environment.getName());
            throw new KalumetException("J2EE application server " + serverName + " is not found in environment " + environment.getName());
        }
        J2EEApplication application = applicationServer.getJ2EEApplication(applicationName);
        if (application == null) {
            LOGGER.error("J2EE application {} is not found in J2EE application server {}", applicationName, applicationServer.getName());
            throw new KalumetException("J2EE application " + applicationName + " is not found in J2EE application server " + applicationServer.getName());
        }
        Database database = application.getDatabase(databaseName);
        if (database == null) {
            LOGGER.error("Database {} is not found in J2EE application {}", databaseName, application.getName());
            throw new KalumetException("Database " + databaseName + " is not found in J2EE application " + application.getName());
        }
        SqlScript sqlScript = database.getSqlScript(sqlScriptName);
        if (sqlScript == null) {
            LOGGER.error("SQL script {} is not found in database {}", sqlScriptName, database.getName());
            throw new KalumetException("SQL script " + sqlScriptName + " is not found in database " + database.getName());
        }

        // post an event and create the update log.
        LOGGER.debug("Post an event and create the update log");
        EventUtils.post(environment, "UPDATE", "SQL script " + sqlScript.getName() + " execution request by WS");
        UpdateLog updateLog = new UpdateLog("SQL script " + sqlScript.getName() + " execution in progress ...", sqlScript.getName(), environment);

        // send a notification and waiting for the count down.
        LOGGER.info("Send a notification and waiting for the count down");
        NotifierUtils.waitAndNotify(environment);

        try {
            // call execution
            LOGGER.debug("Call SQL script updater");
            SqlScriptUpdater.execute(environment, applicationServer, application, database, sqlScript, updateLog);
        } catch (Exception e) {
            LOGGER.error("SQL script {} execution failed", sqlScript.getName(), e);
            EventUtils.post(environment, "ERROR", "SQL script " + sqlScript.getName() + " execution failed: " + e.getMessage());
            updateLog.setStatus("SQL script " + sqlScript.getName() + " execution error");
            updateLog.addUpdateMessage(new UpdateMessage("error", "SQL script " + sqlScript.getName() + " execution failed: " + e.getMessage()));
            PublisherUtils.publish(environment);
            throw new UpdateException("SQL script " + sqlScript.getName() + " execution failed", e);
        }

        // execution completed.
        LOGGER.info("SQL script {} executed", sqlScript.getName());
        updateLog.setStatus("SQL script " + sqlScript.getName() + " executed");
        updateLog.addUpdateMessage(new UpdateMessage("info", "SQL script " + sqlScript.getName() + " executed"));
        LOGGER.info("Publishing update report");
        PublisherUtils.publish(environment);
    }

}
