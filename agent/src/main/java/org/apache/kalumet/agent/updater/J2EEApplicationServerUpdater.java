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

import org.apache.kalumet.FileManipulator;
import org.apache.kalumet.KalumetException;
import org.apache.kalumet.agent.Configuration;
import org.apache.kalumet.agent.utils.EventUtils;
import org.apache.kalumet.controller.core.J2EEApplicationServerController;
import org.apache.kalumet.controller.core.J2EEApplicationServerControllerFactory;
import org.apache.kalumet.model.*;
import org.apache.kalumet.model.update.UpdateLog;
import org.apache.kalumet.model.update.UpdateMessage;
import org.apache.kalumet.utils.CommandUtils;
import org.apache.kalumet.utils.NotifierUtils;
import org.apache.kalumet.utils.PublisherUtils;
import org.apache.kalumet.utils.VariableUtils;
import org.apache.kalumet.ws.client.ClientException;
import org.apache.kalumet.ws.client.J2EEApplicationServerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;

/**
 * Update a J2EE application server.
 */
public class J2EEApplicationServerUpdater {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(J2EEApplicationServerUpdater.class);

    /**
     * Wrapper class to update a JEE application server (via WS).
     *
     * @param environmentName the target environment name.
     * @param serverName      the target JEE application server name.
     * @param delegation      flag indicates if the update is a atomic call or part of an update launched by another agent.
     * @throws KalumetException if the JEE application server update fails.
     */
    public static void update(String environmentName, String serverName, boolean delegation) throws KalumetException {
        LOGGER.info("J2EE application server {} update requested by WS", serverName);
        // load configuration
        LOGGER.debug("Loading configuration");
        Kalumet kalumet = Kalumet.digeste(Configuration.CONFIG_LOCATION);
        Environment environment = kalumet.getEnvironment(environmentName);
        if (environment == null) {
            LOGGER.error("Environment {} is not found in the configuration", environmentName);
            throw new KalumetException("Environment " + environmentName + " is not found in the configuration");
        }
        J2EEApplicationServer applicationServer = environment.getJ2EEApplicationServers().getJ2EEApplicationServer(serverName);
        if (applicationServer == null) {
            LOGGER.error("J2EE application server {} is not found in environment {}", serverName, environmentName);
            throw new KalumetException("J2EE application server " + serverName + " is not found in environment " + environmentName);
        }
        // update configuration cache
        LOGGER.debug("Updating configuration cache");
        Configuration.CONFIG_CACHE = kalumet;

        EventUtils.post(environment, "UPDATE", "J2EE application server " + serverName + " update requested by WS");
        UpdateLog updateLog = new UpdateLog("J2EE application server " + serverName + " update in progress ...", environment.getName(), environment);

        if (!delegation) {
            // it's not a delegation from another agent, send a notification and waiting for the count down
            LOGGER.info("Send a notification and waiting for the count down");
            EventUtils.post(environment, "UPDATE", "Send a notification and waiting for the count down");
            NotifierUtils.waitAndNotify(environment);
        }

        try {
            // launch the update
            LOGGER.debug("Call J2EE application server updater");
            J2EEApplicationServerUpdater.update(kalumet, environment, applicationServer, updateLog);
        } catch (Exception e) {
            // an error occurs
            LOGGER.error("J2EE application server {} update failed", serverName, e);
            EventUtils.post(environment, "ERROR", "J2EE application server " + serverName + " update failed: " + e.getMessage());
            if (!delegation) {
                // it's not a delegation from another agent, publish update result
                updateLog.setStatus("J2EE application server " + serverName + " update failed");
                updateLog.addUpdateMessage(new UpdateMessage("error", "J2EE application server " + serverName + " update failed: " + e.getMessage()));
                PublisherUtils.publish(environment);
            }
            throw new UpdateException("J2EE application server " + serverName + " update failed", e);
        }

        // update is completed
        LOGGER.info("J2EE application server {} updated", applicationServer.getName());
        EventUtils.post(environment, "UPDATE", "J2EE application server " + serverName + " updated");

        if (!delegation) {
            // it's not a delegation from another agent, publish update result
            if (updateLog.isUpdated()) {
                updateLog.setStatus("J2EE application server " + serverName + " updated");
            } else {
                updateLog.setStatus("J2EE application server " + serverName + " already up to date");
            }
            updateLog.addUpdateMessage(new UpdateMessage("info", "Update completed"));
            LOGGER.info("Publishing update report");
            PublisherUtils.publish(environment);
        }
    }

    /**
     * Update a J2EE application server.
     *
     * @param kalumet     the main configuration.
     * @param environment the target <code>Environment</code>.
     * @param server      the target <code>ApplicationServer</code> to update.
     * @param updateLog   the <code>UpdateLog</code> to use.
     */
    public static void update(Kalumet kalumet, Environment environment, J2EEApplicationServer server, UpdateLog updateLog) throws UpdateException {
        String applicationServerJmxUrl = VariableUtils.replace(server.getJmxurl(), environment.getVariables());
        LOGGER.info("Updating J2EE application server {}", server.getName());

        if (!server.isActive()) {
            LOGGER.info("J2EE application server {} is inactive, so not updated", server.getName());
            updateLog.addUpdateMessage(new UpdateMessage("info", "J2EE application server " + server.getName() + " is inactive, so not updated"));
            EventUtils.post(environment, "UPDATE", "J2EE application server " + server.getName() + " is inactive, so not updated");
            return;
        }

        if (server.getAgent() != null && server.getAgent().trim().length() > 0 &&
                !server.getAgent().equals(Configuration.AGENT_ID)) {
            // delegates the update to another agent
            LOGGER.info("Delegating J2EE application server {} update to agent {}", server.getName(), server.getAgent());
            EventUtils.post(environment, "UPDATE", "Delegating J2EE application server " + server.getName() + " update to agent " + server.getAgent());
            updateLog.addUpdateMessage(new UpdateMessage("info", "Delegating J2EE application server " + server.getName() + " update to agent " + server.getAgent()));
            Agent delegationAgent = Configuration.CONFIG_CACHE.getAgent(server.getAgent());
            if (delegationAgent == null) {
                // the target agent is not found in the configuration
                LOGGER.error("Agent {} not found in the configuration", server.getAgent());
                throw new UpdateException("Agent " + server.getAgent() + " not found in the configuration");
            }
            try {
                // request the update via WebService call
                LOGGER.debug("Call J2EE application server WS");
                J2EEApplicationServerClient client = new J2EEApplicationServerClient(delegationAgent.getHostname(), delegationAgent.getPort());
                client.update(environment.getName(), server.getName(), true);
            } catch (ClientException e) {
                // an error occurs during the update on the remote agent
                LOGGER.error("J2EE application server {} update failed", server.getName(), e);
                throw new UpdateException("J2EE application server " + server.getName() + " update failed", e);
            }
            return;
        }

        EventUtils.post(environment, "UPDATE", "Updating J2EE application server " + server.getName());
        updateLog.addUpdateMessage(new UpdateMessage("info", "J2EE application server " + server.getName() + " located " + applicationServerJmxUrl));

        // update JDBC connection pools
        LOGGER.info("Updating JDBC connection pools");
        for (Iterator connectionPoolIterator = server.getJDBCConnectionPools().iterator(); connectionPoolIterator.hasNext(); ) {
            JDBCConnectionPool connectionPool = (JDBCConnectionPool) connectionPoolIterator.next();
            try {
                JDBCConnectionPoolUpdater.update(environment, server, connectionPool, updateLog);
            } catch (UpdateException updateException) {
                // the JDBC connection pool update has failed
                if (connectionPool.isBlocker()) {
                    // connection pool is update blocker
                    LOGGER.error("JDBC connection pool {} update failed", connectionPool.getName(), updateException);
                    updateLog.addUpdateMessage(new UpdateMessage("error", "JDBC connection pool " + connectionPool.getName() + " update failed: " + updateException.getMessage()));
                    EventUtils.post(environment, "ERROR", "JDBC connection pool " + connectionPool.getName() + " update failed: " + updateException.getMessage());
                    throw new UpdateException("JDBC connection ool " + connectionPool.getName() + " update failed", updateException);
                } else {
                    // connection pool is not update blocker
                    LOGGER.warn("JDBC connection pool {} update failed", connectionPool.getName(), updateException);
                    updateLog.addUpdateMessage(new UpdateMessage("warn", "JDBC connection pool " + connectionPool.getName() + " update failed: " + updateException.getMessage()));
                    updateLog.addUpdateMessage(new UpdateMessage("info", "JDBC connection pool " + connectionPool.getName() + " is not update blocker, update continues"));
                    EventUtils.post(environment, "WARN", "JDBC connection pool " + connectionPool.getName() + " update failed: " + updateException.getMessage());
                    EventUtils.post(environment, "UPDATE", "JDBC connection pool " + connectionPool.getName() + " is not update blocker, update continues");
                }
            }
        }

        // update JDBC data sources
        LOGGER.info("Updating JDBC data sources");
        for (Iterator dataSourceIterator = server.getJDBCDataSources().iterator(); dataSourceIterator.hasNext(); ) {
            JDBCDataSource dataSource = (JDBCDataSource) dataSourceIterator.next();
            try {
                JDBCDataSourceUpdater.update(environment, server, dataSource, updateLog);
            } catch (UpdateException updateException) {
                // the JDBC data source update has failed
                if (dataSource.isBlocker()) {
                    // data source is update blocker
                    LOGGER.error("JDBC data source {} udpate failed", dataSource.getName(), updateException);
                    updateLog.addUpdateMessage(new UpdateMessage("error", "JDBC data source " + dataSource.getName() + " update failed: " + updateException.getMessage()));
                    EventUtils.post(environment, "ERROR", "JDBC data source " + dataSource.getName() + " update failed: " + updateException.getMessage());
                    throw new UpdateException("JDBC data source " + dataSource.getName() + " update failed", updateException);
                } else {
                    // data source is not update blocker
                    LOGGER.warn("JDBC data source {} update failed", dataSource.getName(), updateException);
                    updateLog.addUpdateMessage(new UpdateMessage("warn", "JDBC data source " + dataSource.getName() + " update failed: " + updateException.getMessage()));
                    updateLog.addUpdateMessage(new UpdateMessage("info", "JDBC data source " + dataSource.getName() + " is not update blocker, update continues"));
                    EventUtils.post(environment, "WARN", "JDBC data source " + dataSource.getName() + " update failed: " + updateException.getMessage());
                    EventUtils.post(environment, "UPDATE", "JDBC data source " + dataSource.getName() + " is not update blocker, update continues");
                }
            }
        }

        // update JMS connection factories
        LOGGER.info("Updating JMS connection factories");
        for (Iterator jmsConnectionFactoryIterator = server.getJMSConnectionFactories().iterator(); jmsConnectionFactoryIterator.hasNext(); ) {
            JMSConnectionFactory jmsConnectionFactory = (JMSConnectionFactory) jmsConnectionFactoryIterator.next();
            try {
                JMSConnectionFactoryUpdater.update(environment, server, jmsConnectionFactory, updateLog);
            } catch (UpdateException updateException) {
                // the JMS connection factory update has failed
                if (jmsConnectionFactory.isBlocker()) {
                    // JMS connection factory is update blocker
                    LOGGER.error("JMS connection factory {} update failed", jmsConnectionFactory.getName(), updateException);
                    updateLog.addUpdateMessage(new UpdateMessage("error", "JMS connection factory " + jmsConnectionFactory.getName() + " update failed: " + updateException.getMessage()));
                    EventUtils.post(environment, "ERROR", "JMS connection factory " + jmsConnectionFactory.getName() + " update failed: " + updateException.getMessage());
                    throw new UpdateException("JMS connection factory " + jmsConnectionFactory.getName() + " update failed: " + updateException.getMessage(), updateException);
                } else {
                    // JMS connection factory is not update blocker
                    LOGGER.warn("JMS connection factory {} update failed", jmsConnectionFactory.getName(), updateException);
                    updateLog.addUpdateMessage(new UpdateMessage("warn", "JMS connection factory " + jmsConnectionFactory.getName() + " update failed: " + updateException.getMessage()));
                    updateLog.addUpdateMessage(new UpdateMessage("info", "JMS connection factory " + jmsConnectionFactory.getName() + " is not update blocker, update continues"));
                    EventUtils.post(environment, "WARN", "JMS connection factory " + jmsConnectionFactory.getName() + " update failed: " + updateException.getMessage());
                    EventUtils.post(environment, "UPDATE", "JMS connection factory " + jmsConnectionFactory.getName() + " is not update blocker, update continues");
                }
            }
        }

        // update JMS servers
        LOGGER.info("Updating JMS servers");
        for (Iterator jmsServerIterator = server.getJMSServers().iterator(); jmsServerIterator.hasNext(); ) {
            JMSServer jmsServer = (JMSServer) jmsServerIterator.next();
            try {
                JMSServerUpdater.update(environment, server, jmsServer, updateLog);
            } catch (UpdateException updateException) {
                // the JMS server update has failed
                if (jmsServer.isBlocker()) {
                    // JMS server is update blocker
                    LOGGER.error("JMS server {} update failed", jmsServer.getName(), updateException);
                    updateLog.addUpdateMessage(new UpdateMessage("error", "JMS server " + jmsServer.getName() + " update failed: " + updateException.getMessage()));
                    EventUtils.post(environment, "ERROR", "JMS server " + jmsServer.getName() + " update failed: " + updateException.getMessage());
                    throw new UpdateException("JMS server " + jmsServer.getName() + " update failed", updateException);
                } else {
                    // JMS server is not update blocker
                    LOGGER.warn("JMS server {} update failed", jmsServer.getName(), updateException);
                    updateLog.addUpdateMessage(new UpdateMessage("warn", "JMS server " + jmsServer.getName() + " update failed: " + updateException.getMessage()));
                    updateLog.addUpdateMessage(new UpdateMessage("info", "JMS server " + jmsServer.getName() + " is not update blocker, update continues"));
                    EventUtils.post(environment, "WARN", "JMS server " + jmsServer.getName() + " update failed: " + updateException.getMessage());
                    EventUtils.post(environment, "UPDATE", "JMS server " + jmsServer.getName() + " is not update blocker, update continues");
                }
            }
        }

        // update JNDI name space bindings
        LOGGER.info("Updating JNDIbindings");
        for (Iterator jndiBindingsIterator = server.getJNDIBindings().iterator(); jndiBindingsIterator.hasNext(); ) {
            JNDIBinding jndiBinding = (JNDIBinding) jndiBindingsIterator.next();
            try {
                JNDIBindingUpdater.update(environment, server, jndiBinding, updateLog);
            } catch (UpdateException updateException) {
                // the JNDI binding update has failed
                if (jndiBinding.isBlocker()) {
                    // JNDIbinding is update blocker
                    LOGGER.error("JNDI binding {} update failed", jndiBinding.getName(), updateException);
                    updateLog.addUpdateMessage(new UpdateMessage("error", "JNDI binding " + jndiBinding.getName() + " update failed: " + updateException.getMessage()));
                    EventUtils.post(environment, "ERROR", "JNDI binding " + jndiBinding.getName() + " update failed: " + updateException.getMessage());
                    throw new UpdateException("JNDI binding " + jndiBinding.getName() + " update failed", updateException);
                } else {
                    // JNDI binding is not update blocker
                    LOGGER.warn("JNDI binding {} update failed", jndiBinding.getName(), updateException);
                    updateLog.addUpdateMessage(new UpdateMessage("warn", "JNDI binding " + jndiBinding.getName() + " update failed: " + updateException.getMessage()));
                    updateLog.addUpdateMessage(new UpdateMessage("info", "JNDI binding " + jndiBinding.getName() + " is not update blocker, update continues"));
                    EventUtils.post(environment, "WARN", "JNDI binding " + jndiBinding.getName() + " update failed: " + updateException.getMessage());
                    EventUtils.post(environment, "UPDATE", "JNDI binding " + jndiBinding.getName() + " is not update blocker, update continues");
                }
            }
        }

        // update shared libraries
        LOGGER.info("Updating shared libraries");
        for (Iterator sharedLibraryIterator = server.getSharedLibraries().iterator(); sharedLibraryIterator.hasNext(); ) {
            SharedLibrary sharedLibrary = (SharedLibrary) sharedLibraryIterator.next();
            try {
                SharedLibraryUpdater.update(environment, server, sharedLibrary, updateLog);
            } catch (UpdateException updateException) {
                // the shared library update has failed
                if (sharedLibrary.isBlocker()) {
                    // shared library is update blocker
                    LOGGER.error("Shared library {} update failed", sharedLibrary.getName(), updateException);
                    updateLog.addUpdateMessage(new UpdateMessage("error", "Shared library " + sharedLibrary.getName() + " update failed: " + updateException.getMessage()));
                    EventUtils.post(environment, "ERROR", "Shared library " + sharedLibrary.getName() + " update failed: " + updateException.getMessage());
                    throw new UpdateException("Shared library " + sharedLibrary.getName() + " update failed", updateException);
                } else {
                    // shared library is not update blocker
                    LOGGER.warn("Shared library {} update failed", sharedLibrary.getName(), updateException);
                    updateLog.addUpdateMessage(new UpdateMessage("warn", "Shared library " + sharedLibrary.getName() + " update failed: " + updateException.getMessage()));
                    updateLog.addUpdateMessage(new UpdateMessage("info", "Shared library " + sharedLibrary.getName() + " is not update blocker, update continues"));
                    EventUtils.post(environment, "WARN", "Shared library " + sharedLibrary.getName() + " update failed: " + updateException.getMessage());
                    EventUtils.post(environment, "UPDATE", "Shared library " + sharedLibrary.getName() + " is not update blocker, update continues");
                }
            }
        }

        // update J2EE applications
        LOGGER.info("Updating J2EE applications");
        for (Iterator applicationIterator = server.getJ2EEApplications().iterator(); applicationIterator.hasNext(); ) {
            J2EEApplication application = (J2EEApplication) applicationIterator.next();
            try {
                JEEApplicationUpdater.update(environment, server, application, updateLog);
            } catch (UpdateException updateException) {
                // the J2EE application update has failed
                if (application.isBlocker()) {
                    // J2EE application is update blocker
                    LOGGER.error("J2EE application {} update failed", application.getName(), updateException);
                    updateLog.addUpdateMessage(new UpdateMessage("error", "J2EE application " + application.getName() + " update failed: " + updateException.getMessage()));
                    EventUtils.post(environment, "ERROR", "J2EE application " + application.getName() + " update failed: " + updateException.getMessage());
                    throw new UpdateException("J2EE application " + application.getName() + " update failed", updateException);
                } else {
                    // J2EE application is not update blocker
                    LOGGER.warn("J2EE application {} update failed", application.getName(), updateException);
                    updateLog.addUpdateMessage(new UpdateMessage("warn", "J2EE application " + application.getName() + " update failed: " + updateException.getMessage()));
                    updateLog.addUpdateMessage(new UpdateMessage("info", "J2EE application " + application.getName() + " is not update blocker, update continues"));
                    EventUtils.post(environment, "WARN", "J2EE application " + application.getName() + " update failed: " + updateException.getMessage());
                    EventUtils.post(environment, "UPDATE", "J2EE application " + application.getName() + " is not update blocker, update continues");
                }
            }
        }

        // stop J2EE server
        LOGGER.info("Shutting down J2EE application server");
        try {
            J2EEApplicationServerUpdater.stop(environment, server, updateLog);
        } catch (UpdateException updateException) {
            // the J2EE application server stop has failed
            if (server.isBlocker()) {
                // J2EE application server is update blocker
                LOGGER.error("J2EE application server {} shutdown failed", server.getName(), updateException);
                updateLog.addUpdateMessage(new UpdateMessage("error", "J2EE application server " + server.getName() + " shutdown failed: " + updateException.getMessage()));
                EventUtils.post(environment, "ERROR", "J2EE application server " + server.getName() + " shutdown failed: " + updateException.getMessage());
                throw new UpdateException("J2EE application server " + server.getName() + " shutdown failed: " + updateException.getMessage(), updateException);
            } else {
                // J2EE application server is not update blocker
                LOGGER.warn("J2EE application server {} shutdown failed", server.getName(), updateException);
                updateLog.addUpdateMessage(new UpdateMessage("warn", "J2EE application server " + server.getName() + " shutdown failed: " + updateException.getMessage()));
                updateLog.addUpdateMessage(new UpdateMessage("info", "J2EE application server " + server.getName() + " is not update blocker, update continues"));
                EventUtils.post(environment, "WARN", "J2EE application server " + server.getName() + " shutdown failed: " + updateException.getMessage());
                EventUtils.post(environment, "UPDATE", "J2EE application server " + server.getName() + " is not update blocker, update continues");
            }
        }

        // clean the JEE application server cache
        LOGGER.info("Clean J2EE application server cache directories");
        try {
            J2EEApplicationServerUpdater.cleanCaches(environment, server, updateLog);
        } catch (UpdateException updateException) {
            // the J2EE application server cache directories cleaning has failed
            if (server.isBlocker()) {
                // J2EE application server is update blocker
                LOGGER.error("J2EE application server {} cache directories cleanup failed", server.getName(), updateException);
                updateLog.addUpdateMessage(new UpdateMessage("error", "J2EE application server " + server.getName() + " cache directories cleanup failed: " + updateException.getMessage()));
                EventUtils.post(environment, "ERROR", "J2EE application server " + server.getName() + " cache directories cleanup failed: " + updateException.getMessage());
                throw new UpdateException("J2EE application server " + server.getName() + " cache directories cleanup failed", updateException);
            } else {
                // J2EE application server is not update blocker
                LOGGER.warn("J2EE application server {} cache directories cleanup failed", server.getName(), updateException);
                updateLog.addUpdateMessage(new UpdateMessage("warn", "J2EE application server " + server.getName() + " cache directories cleanup failed: " + updateException.getMessage()));
                updateLog.addUpdateMessage(new UpdateMessage("info", "J2EE application server " + server.getName() + " is not update blocker, update continues"));
                EventUtils.post(environment, "WARN", "J2EE application server " + server.getName() + " cache directories cleanup failed: " + updateException.getMessage());
                EventUtils.post(environment, "UPDATE", "J2EE application server " + server.getName() + " is not update blocker, update continues");
            }
        }

        // start J2EE application server
        LOGGER.info("Starting J2EE application server");
        try {
            J2EEApplicationServerUpdater.start(kalumet, environment, server, updateLog);
        } catch (UpdateException updateException) {
            // the J2EE application server start has failed
            if (server.isBlocker()) {
                // J2EE application server is update blocker
                LOGGER.error("J2EE application server {} start failed", server.getName(), updateException);
                updateLog.addUpdateMessage(new UpdateMessage("error", "J2EE application server " + server.getName() + " start failed: " + updateException.getMessage()));
                EventUtils.post(environment, "ERROR", "J2EE application server " + server.getName() + " start failed: " + updateException.getMessage());
                throw new UpdateException("J2EE application server " + server.getName() + " start failed", updateException);
            } else {
                // J2EE application server is not update blocker
                LOGGER.warn("J2EE application server " + server.getName() + " start failed", updateException);
                updateLog.addUpdateMessage(new UpdateMessage("warn", "J2EE application server " + server.getName() + " start failed: " + updateException.getMessage()));
                updateLog.addUpdateMessage(new UpdateMessage("info", "J2EE application server " + server.getName() + " is not update blocker, update continues"));
                EventUtils.post(environment, "WARN", "J2EE application server " + server.getName() + " start failed: " + updateException.getMessage());
                EventUtils.post(environment, "UPDATE", "J2EE application server " + server.getName() + " is not update blocker, update continues");
            }
        }

        // update completed
        EventUtils.post(environment, "UPDATE", "J2EE application server updated");
    }

    /**
     * Shutdown a JEE server.
     *
     * @param environment the target <code>Environment</code>.
     * @param server      the <code>J2EEApplicationServer</code> to stop.
     * @param updateLog   the <code>UpdateLog</code> to use.
     */
    protected static void stop(Environment environment, J2EEApplicationServer server, UpdateLog updateLog) throws UpdateException {
        // TODO delegate the JEE server stop to another agent is required
        try {
            if (!server.isUpdateRequireRestart() || !updateLog.isUpdated()) {
                LOGGER.info("J2EE application server {} shutdown is not required", server.getName());
                updateLog.addUpdateMessage(new UpdateMessage("info", "J2EE application server " + server.getName() + " shutdown is not required"));
                EventUtils.post(environment, "UPDATE", "J2EE application server " + server.getName() + " shutdown is not required");
                return;
            }
            // the server restart is required
            LOGGER.info("J2EE application server {} shutdown is required", server.getName());
            updateLog.addUpdateMessage(new UpdateMessage("info", "J2EE application server " + server.getName() + " shutdown is required"));
            EventUtils.post(environment, "UPDATE", "J2EE application server " + server.getName() + " shutdown is required");
            if (server.isUsejmxstop()) {
                LOGGER.debug("J2EE application server shutdown is performed using JMX controller");
                LOGGER.debug("Getting J2EE application server JMX controller");
                J2EEApplicationServerController controller = J2EEApplicationServerControllerFactory.getController(environment, server);
                controller.shutdown();
                LOGGER.info("J2EE application server {} shutdown completed", server.getName());
                EventUtils.post(environment, "UPDATE", "J2EE server " + server.getName() + " shutdown completed");
                updateLog.addUpdateMessage(new UpdateMessage("info", "J2EE server " + server.getName() + " shutdown completed"));
                return;
            }
            LOGGER.debug("J2EE application server shutdown is performed using system command");
            String output = CommandUtils.execute(VariableUtils.replace(server.getShutdowncommand(), environment.getVariables()));
            LOGGER.info("J2EE application server " + server.getName() + " shutdown completed: " + output);
            updateLog.addUpdateMessage(new UpdateMessage("info", "J2EE application server " + server.getName() + " shutdown completed: " + output));
            EventUtils.post(environment, "UPDATE", "JEE server " + server.getName() + " shutdown completed: " + output);
        } catch (Exception exception) {
            LOGGER.error("J2EE application server " + server.getName() + " shutdown failed", exception);
            updateLog.addUpdateMessage(new UpdateMessage("error", "J2EE server " + server.getName() + " shutdown failed: " + exception.getMessage()));
            EventUtils.post(environment, "ERROR", "J2EE application server " + server.getName() + " shutdown failed: " + exception.getMessage());
            throw new UpdateException("J2EE application server " + server.getName() + " shutdown failed", exception);
        }
    }

    /**
     * Start a J2EE application server.
     *
     * @param kalumet     the configuration.
     * @param environment the target <code>Environment</code>.
     * @param server      the <code>J2EEApplicationServer</code> to start.
     * @param updateLog   the <code>UpdateLog</code> to use.
     */
    protected static void start(Kalumet kalumet, Environment environment, J2EEApplicationServer server, UpdateLog updateLog) throws UpdateException {
        // TODO delegate the JEE server start to another agent is required
        try {
            if (!server.isUpdateRequireRestart() || !updateLog.isUpdated()) {
                LOGGER.info("J2EE application server {} start is not required", server.getName());
                EventUtils.post(environment, "UPDATE", "J2EE application server " + server.getName() + " start is not required");
                updateLog.addUpdateMessage(new UpdateMessage("info", "J2EE application server " + server.getName() + " start is required"));
                return;
            }

            LOGGER.info("J2EE application server {} start is required", server.getName());
            updateLog.addUpdateMessage(new UpdateMessage("info", "J2EE application server " + server.getName() + " start is required"));
            EventUtils.post(environment, "UPDATE", "J2EE application server " + server.getName() + " start is required");

            // get the agent configuration
            Agent agent = kalumet.getAgent(Configuration.AGENT_ID);

            // check the agent max environment active
            if (agent.getMaxj2eeapplicationserversstarted() > 0) {
                // get the environments managed by the agent
                List agentEnvironments = kalumet.getEnvironmentsByAgent(Configuration.AGENT_ID);
                int applicationServersStarted = 0;
                for (Iterator agentEnvironmentsIterator = agentEnvironments.iterator(); agentEnvironmentsIterator.hasNext(); ) {
                    Environment agentEnvironment = (Environment) agentEnvironmentsIterator.next();
                    // check if the application server started into the environment
                    for (Iterator agentEnvironmentApplicationServersIterator = agentEnvironment.getJ2EEApplicationServers().getJ2EEApplicationServers().iterator(); agentEnvironmentApplicationServersIterator.hasNext(); ) {
                        J2EEApplicationServer agentEnvironmentApplicationServer = (J2EEApplicationServer) agentEnvironmentApplicationServersIterator.next();
                        // get the controller
                        J2EEApplicationServerController controller = J2EEApplicationServerControllerFactory.getController(environment, server);
                        if (!controller.isStopped()) {
                            applicationServersStarted++;
                            if (applicationServersStarted >= agent.getMaxj2eeapplicationserversstarted()) {
                                // the max number of application servers started is raised
                                throw new UpdateException("The maximum number of started J2EE application servers has been raised for the agent");
                            }
                        }
                    }
                }
            }

            // the start is performed using system command
            String output = CommandUtils.execute(VariableUtils.replace(server.getStartupcommand(), environment.getVariables()));
            // application server start has been performed
            LOGGER.info("J2EE application server {} start completed: {}", server.getName(), output);
            updateLog.addUpdateMessage(new UpdateMessage("info", "J2EE application server " + server.getName() + " start completed: " + output));
            EventUtils.post(environment, "UPDATE", "J2EE application server " + server.getName() + " start completed: " + output);
        } catch (Exception exception) {
            LOGGER.error("J2EE application server {} start failed", server.getName(), exception);
            updateLog.addUpdateMessage(new UpdateMessage("error", "J2EE application server " + server.getName() + " start failed: " + exception.getMessage()));
            EventUtils.post(environment, "ERROR", "J2EE application server " + server.getName() + " start failed: " + exception.getMessage());
            throw new UpdateException("J2EE application server " + server.getName() + " start failed", exception);
        }
    }

    /**
     * Cleanup J2EE application server caches.
     *
     * @param environment the <code>Environment</code>.
     * @param server      the target <code>J2EEApplicationServer</code>.
     * @param updateLog   the <code>UpdateLog</code> to use.
     */
    protected static void cleanCaches(Environment environment, J2EEApplicationServer server, UpdateLog updateLog) throws UpdateException {
        try {
            if (!server.isUpdateRequireCacheCleaning() || !updateLog.isUpdated()) {
                LOGGER.info("J2EE application server {} caches cleaning is not required", server.getName());
                updateLog.addUpdateMessage(new UpdateMessage("info", "J2EE application server " + server.getName() + " caches cleaning is not required"));
                EventUtils.post(environment, "UPDATE", "J2EE application server " + server.getName() + " caches cleaning is not required");
                return;
            }
            // the application server caches cleaning is required
            LOGGER.info("J2EE application server {} caches cleaning is required", server.getName());
            updateLog.addUpdateMessage(new UpdateMessage("info", "J2EE application server " + server.getName() + " caches cleaning is required"));
            EventUtils.post(environment, "UPDATE", "J2EE application server " + server.getName() + " caches cleaning is required");
            // initializes the file manipulator instance
            FileManipulator fileManipulator = FileManipulator.getInstance();
            for (Iterator cacheIterator = server.getCaches().iterator(); cacheIterator.hasNext(); ) {
                Cache cache = (Cache) cacheIterator.next();
                String path = VariableUtils.replace(cache.getPath(), environment.getVariables());
                fileManipulator.delete(path);
            }
        } catch (Exception exception) {
            LOGGER.error("J2EE application server {} cache directories cleanup failed", server.getName(), exception);
            updateLog.addUpdateMessage(new UpdateMessage("error", "J2EE application server" + server.getName() + " cache directories cleanup failed: " + exception.getMessage()));
            EventUtils.post(environment, "ERROR", "J2EE application server " + server.getName() + " cache directories cleanup failed: " + exception.getMessage());
            throw new UpdateException("J2EE application server " + server.getName() + " caches cleanup failed", exception);
        }
    }

    /**
     * Wrapper method to start J2EE application server (via WS).
     *
     * @param environmentName the target environment name.
     * @param serverName      the target J2EE application server name.
     */
    public static void start(String environmentName, String serverName) throws KalumetException {
        LOGGER.info("J2EE application server {} start requested by WS", serverName);

        LOGGER.debug("Loading configuration");
        Kalumet kalumet = Kalumet.digeste(Configuration.CONFIG_LOCATION);
        Environment environment = kalumet.getEnvironment(environmentName);
        if (environment == null) {
            LOGGER.error("Environment {} is not found in the configuration", environmentName);
            throw new UpdateException("Environment " + environmentName + " is not found in the configuration");
        }
        J2EEApplicationServer server = environment.getJ2EEApplicationServers().getJ2EEApplicationServer(serverName);
        if (server == null) {
            LOGGER.error("J2EE application server {} is not found in environment {}", serverName, environmentName);
            throw new UpdateException("J2EE application server " + serverName + " is not found in environment " + environmentName);
        }

        // get the agent configuration
        Agent agent = kalumet.getAgent(Configuration.AGENT_ID);

        // check the agent max environment active
        if (agent.getMaxj2eeapplicationserversstarted() > 0) {
            // get the environments managed by the agent
            List agentEnvironments = kalumet.getEnvironmentsByAgent(Configuration.AGENT_ID);
            int applicationServersStarted = 0;
            for (Iterator agentEnvironmentsIterator = agentEnvironments.iterator(); agentEnvironmentsIterator.hasNext(); ) {
                Environment agentEnvironment = (Environment) agentEnvironmentsIterator.next();
                // check if the application server started into the environment
                for (Iterator agentEnvironmentApplicationServersIterator = agentEnvironment.getJ2EEApplicationServers().getJ2EEApplicationServers().iterator(); agentEnvironmentApplicationServersIterator.hasNext(); ) {
                    J2EEApplicationServer agentEnvironmentApplicationServer = (J2EEApplicationServer) agentEnvironmentApplicationServersIterator.next();
                    // get the controller
                    J2EEApplicationServerController controller = J2EEApplicationServerControllerFactory.getController(environment, server);
                    if (!controller.isStopped()) {
                        applicationServersStarted++;
                        if (applicationServersStarted >= agent.getMaxj2eeapplicationserversstarted()) {
                            // the max number of application servers started is raised
                            throw new KalumetException("The maximum number of started J2EE application servers has been raised for the agent");
                        }
                    }
                }
            }
        }

        EventUtils.post(environment, "INFO", "J2EE application server " + serverName + " start requested by WS");
        // the start is performed using system command
        String output = CommandUtils.execute(VariableUtils.replace(server.getStartupcommand(), environment.getVariables()));
        // application server start has been performed
        LOGGER.info("J2EE application server {} STARTED: {}", serverName, output);
        EventUtils.post(environment, "INFO", "J2EE application server " + serverName + " started: " + output);
    }

    /**
     * Wrapper method to stop J2EE application server (via WS).
     *
     * @param environmentName the environment name.
     * @param serverName      the J2EE application server name.
     */
    public static void stop(String environmentName, String serverName) throws UpdateException {
        LOGGER.info("J2EE application server {} shutdown requested by WS", serverName);
        Kalumet kalumet;
        try {
            kalumet = Kalumet.digeste(Configuration.CONFIG_LOCATION);
        } catch (KalumetException e) {
            LOGGER.error("Can't load configuration", e);
            throw new UpdateException("Can't load configuration", e);
        }
        Environment environment = kalumet.getEnvironment(environmentName);
        if (environment == null) {
            LOGGER.error("Environment {} is not found in the configuration", environmentName);
            throw new UpdateException("Environment " + environmentName + " is not found in the configuration");
        }
        J2EEApplicationServer server = environment.getJ2EEApplicationServers().getJ2EEApplicationServer(serverName);
        if (server == null) {
            LOGGER.error("J2EE application server {} is not found in environment {}", serverName, environmentName);
            throw new UpdateException("J2EE application server " + serverName + " is not found in environment " + environmentName);
        }
        EventUtils.post(environment, "INFO", "J2EE application server " + serverName + " shutdown requested by WS");
        // check if the stop is made using JMX
        try {
            if (server.isUsejmxstop()) {
                J2EEApplicationServerController controller = J2EEApplicationServerControllerFactory.getController(environment, server);
                controller.shutdown();
                LOGGER.info("J2EE application server {} shutdown using the controller", serverName);
                EventUtils.post(environment, "INFO", "J2EE application server " + serverName + " shutdown using the controller");
                return;
            }
        } catch (Exception e) {
            LOGGER.error("J2EE application server {} shutdown failed", serverName, e);
            throw new UpdateException("J2EE application server " + serverName + " shutdown failed", e);
        }
        // no JMX stop, use system command call
        String shutdownCommand = VariableUtils.replace(server.getShutdowncommand(), environment.getVariables());
        String output = null;
        try {
            output = CommandUtils.execute(shutdownCommand);
        } catch (KalumetException e) {
            LOGGER.error("J2EE application server {} shutdown FAILED.", serverName, e);
            throw new UpdateException("J2EE application server " + serverName + " shutdown failed", e);
        }
        LOGGER.info("J2EE application server {} shutdown using system command: {}", serverName, output);
        EventUtils.post(environment, "INFO", "J2EE application server " + serverName + " shutdown using system command: " + output);
    }

    /**
     * Wrapper method to get J2EE application server status (via WS).
     *
     * @param environmentName       the environment name.
     * @param applicationServerName the J2EE application server name.
     * @return the J2EE application server current status.
     */
    public static String status(String environmentName, String applicationServerName) throws UpdateException {
        // TODO delegate the JEE server status to another agnt if required
        LOGGER.info("J2EE application server {} status check requested by WS", applicationServerName);

        LOGGER.debug("Loading configuration");
        Kalumet kalumet;
        try {
            kalumet = Kalumet.digeste(Configuration.CONFIG_LOCATION);
        } catch (KalumetException e) {
            LOGGER.error("Can't load configuration", e);
            throw new UpdateException("Can't load configuration", e);
        }
        Environment environment = kalumet.getEnvironment(environmentName);
        if (environment == null) {
            LOGGER.error("Environment {} is not found in the configuration", environmentName);
            throw new UpdateException("Environment " + environmentName + " is not found in the configuration");
        }
        J2EEApplicationServer server = environment.getJ2EEApplicationServers().getJ2EEApplicationServer(applicationServerName);
        if (server == null) {
            LOGGER.error("J2EE application server {} is not found in environment {}", applicationServerName, environmentName);
            throw new UpdateException("J2EE application server " + applicationServerName + " is not found in environment " + environmentName);
        }
        EventUtils.post(environment, "INFO", "J2EE application server " + applicationServerName + " status requested by WS");
        try {
            // get the controller
            J2EEApplicationServerController controller = J2EEApplicationServerControllerFactory.getController(environment, server);
            // get the application server status
            return controller.status();
        } catch (Exception e) {
            LOGGER.error("J2EE application server {} status check failed", applicationServerName, e);
            throw new UpdateException("J2EE application server " + applicationServerName + " status check failed", e);
        }
    }

}
