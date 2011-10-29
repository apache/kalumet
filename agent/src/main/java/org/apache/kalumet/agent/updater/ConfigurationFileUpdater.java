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
import org.apache.kalumet.FileManipulatorException;
import org.apache.kalumet.KalumetException;
import org.apache.kalumet.agent.Configuration;
import org.apache.kalumet.agent.utils.EventUtils;
import org.apache.kalumet.model.*;
import org.apache.kalumet.model.update.UpdateLog;
import org.apache.kalumet.model.update.UpdateMessage;
import org.apache.kalumet.utils.NotifierUtils;
import org.apache.kalumet.utils.PublisherUtils;
import org.apache.kalumet.utils.VariableUtils;
import org.apache.kalumet.ws.client.ClientException;
import org.apache.kalumet.ws.client.ConfigurationFileClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * J2EE application configuration file updater.
 */
public class ConfigurationFileUpdater {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(ConfigurationFileUpdater.class);

    /**
     * Updates a <code>ConfigurationFile</code>.
     *
     * @param environment       the target <code>Environment</code>.
     * @param server            the target <code>J2EEApplicationServer</code>.
     * @param application       the target <code>J2EEApplication</code>.
     * @param configurationFile the target <code>ConfigurationFile</code>.
     * @param updateLog         the <code>UpdateLog</code> to use.
     */
    public static void update(Environment environment, J2EEApplicationServer server, J2EEApplication application, ConfigurationFile configurationFile, UpdateLog updateLog) throws UpdateException {
        LOGGER.info("Updating configuration file {}", configurationFile.getName());
        updateLog.addUpdateMessage(new UpdateMessage("info", "Updating configuration file " + configurationFile.getName()));
        EventUtils.post(environment, "UPDATE", "Updating configuration file " + configurationFile.getName());

        if (!configurationFile.isActive()) {
            LOGGER.info("Configuration file {} is inactive, so not updated", configurationFile.getName());
            updateLog.addUpdateMessage(new UpdateMessage("info", "Configuration file " + configurationFile.getName() + " is inactive, so not updated"));
            EventUtils.post(environment, "UPDATE", "Configuration file " + configurationFile.getName() + " is inactive, so not updated");
            return;
        }

        // check for update delegation
        if (configurationFile.getAgent() != null && configurationFile.getAgent().trim().length() > 0 && !configurationFile.getAgent().equals(Configuration.AGENT_ID)) {
            LOGGER.info("Delegating the configuration file {} update to agent {}", configurationFile.getName(), configurationFile.getAgent());
            updateLog.addUpdateMessage(new UpdateMessage("info", "Delegating the configuration file " + configurationFile.getName() + " update to agent " + configurationFile.getAgent()));
            EventUtils.post(environment, "UPDATE", "Delegating the configuration file " + configurationFile.getName() + " update to agent " + configurationFile.getAgent());
            Agent delegationAgent = Configuration.CONFIG_CACHE.getAgent(configurationFile.getAgent());
            if (delegationAgent == null) {
                // the delegation agent is not found
                LOGGER.error("Agent {} is not found in the configuration", configurationFile.getAgent());
                throw new UpdateException("Agent " + configurationFile.getAgent() + " is not found in the configuration");
            }
            try {
                LOGGER.debug("Call the configuration file WS");
                ConfigurationFileClient client = new ConfigurationFileClient(delegationAgent.getHostname(), delegationAgent.getPort());
                client.update(environment.getName(), server.getName(), application.getName(), configurationFile.getName(), true);
            } catch (ClientException clientException) {
                // delegation update has failed
                LOGGER.error("Configuration file {} update failed", configurationFile.getName(), clientException);
                throw new UpdateException("Configuration file " + configurationFile.getName() + " update failed", clientException);
            }
            return;
        }

        String configurationFileUri = VariableUtils.replace(configurationFile.getUri(), environment.getVariables());
        if (!FileManipulator.protocolExists(configurationFileUri)) {
            // the configuration file URI is relative, construct the URI using the J2EE application URI
            LOGGER.debug("The configuration file URI is relative to the J2EE application URI");
            configurationFileUri = FileManipulator.format(VariableUtils.replace(application.getUri(), environment.getVariables())) + "!/" + configurationFileUri;
        }

        // get a file manipulator
        FileManipulator fileManipulator = null;
        try {
            LOGGER.debug("Initializing file manipulator");
            fileManipulator = FileManipulator.getInstance();
        } catch (FileManipulatorException fileManipulatorException) {
            LOGGER.error("Can't initialize file manipulator", fileManipulatorException);
            throw new UpdateException("Can't initialize file manipulator", fileManipulatorException);
        }

        // get the application working directory
        String applicationCacheDir = null;
        try {
            LOGGER.debug("Initializing application cache directory");
            applicationCacheDir = FileManipulator.createJ2EEApplicationCacheDir(environment, application);
        } catch (FileManipulatorException fileManipulatorException) {
            LOGGER.error("Can't create J2EE application {} cache directory", application.getName(), fileManipulatorException);
            throw new UpdateException("Can't create J2EE application " + application.getName() + " cache directory", fileManipulatorException);
        }

        String configurationFileCache = applicationCacheDir + "/config/" + configurationFile.getName();
        try {
            // copy the configuration file in the application cache directory
            LOGGER.debug("Copying the configuration file {} to the application cache directory", configurationFile.getName());
            fileManipulator.copy(configurationFileUri, configurationFileCache);
        } catch (FileManipulatorException fileManipulatorException) {
            LOGGER.error("Can't copy {} to {}", new Object[]{configurationFileUri, configurationFileCache}, fileManipulatorException);
            throw new UpdateException("Can't copy " + configurationFileUri + " to " + configurationFileCache, fileManipulatorException);
        }

        // change mappings in the configuration file
        LOGGER.debug("Replacing mappings key/value");
        for (Iterator mappingIterator = configurationFile.getMappings().iterator(); mappingIterator.hasNext(); ) {
            Mapping mapping = (Mapping) mappingIterator.next();
            FileManipulator.searchAndReplace(mapping.getKey(), VariableUtils.replace(mapping.getValue(), environment.getVariables()), configurationFileCache);
        }

        // compare the configuration file with the target one
        try {
            if (!fileManipulator.contentEquals(configurationFileCache, VariableUtils.replace(configurationFile.getPath(), environment.getVariables()))) {
                // the configuration file has changed
                LOGGER.debug("Configuration file {} has changed", configurationFile.getName());
                fileManipulator.copy(configurationFileCache, VariableUtils.replace(configurationFile.getPath(), environment.getVariables()));
                updateLog.setStatus("Update performed");
                updateLog.setUpdated(true);
                LOGGER.info("Configuration file {} updated", configurationFile.getName());
                updateLog.addUpdateMessage(new UpdateMessage("info", "Configuration file " + configurationFile.getName() + " updated"));
                EventUtils.post(environment, "UPDATE", "Configuration file " + configurationFile.getName() + " updated");
            }
        } catch (Exception e) {
            // the configuration file update failed, delete from the cache
            try {
                fileManipulator.delete(configurationFileCache);
            } catch (FileManipulatorException fileManipulatorException) {
                LOGGER.warn("Can't delete {}", configurationFile.getName(), fileManipulatorException);
            }
            LOGGER.error("Configuration file {} update failed", configurationFile.getName(), e);
            throw new UpdateException("Configuration file " + configurationFile.getName() + " update failed", e);
        }
    }

    /**
     * Wrapper method to update a configuration file via WS.
     *
     * @param environmentName       the target environment name.
     * @param serverName            the target J2EE application server name.
     * @param applicationName       the target J2EE application name.
     * @param configurationFileName the target configuration file name.
     * @param delegation            flag indicating if the call is made by another agent (true), or by a client (false).
     * @throws KalumetException in case of update error.
     */
    public static void update(String environmentName, String serverName, String applicationName, String configurationFileName, boolean delegation) throws KalumetException {
        LOGGER.info("Configuration file {} update requested by WS", configurationFileName);

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
        J2EEApplication application = applicationServer.getJ2EEApplication(applicationName);
        if (application == null) {
            LOGGER.error("J2EE application {} is not found in J2EE application server {}", applicationName, serverName);
            throw new KalumetException("J2EE application " + applicationName + " is not found in J2EE application server " + serverName);
        }
        ConfigurationFile configurationFile = application.getConfigurationFile(configurationFileName);
        if (configurationFile == null) {
            LOGGER.error("Configuration file {} is not found in J2EE application {}", configurationFileName, applicationName);
            throw new KalumetException("Configuration file " + configurationFileName + " is not found in J2EE application " + applicationName);
        }

        // update cache
        LOGGER.debug("Updating configuration cache");
        Configuration.CONFIG_CACHE = kalumet;

        // post event and create update logger
        EventUtils.post(environment, "UPDATE", "Configuration file " + configurationFile.getName() + " update requested by WS");
        UpdateLog updateLog = new UpdateLog("Configuration file " + configurationFile.getName() + " update in progress ...", environment.getName(), environment);

        // send and wait a notification if it's not a delegation
        if (!delegation) {
            // it's a client call
            LOGGER.info("Send a notification and waiting for the count down");
            EventUtils.post(environment, "UPDATE", "Send a notification and waiting for the count down");
            NotifierUtils.waitAndNotify(environment);
        }

        try {
            // call the updater method
            LOGGER.debug("Call configuration file updater");
            ConfigurationFileUpdater.update(environment, applicationServer, application, configurationFile, updateLog);
        } catch (Exception e) {
            LOGGER.error("Configuration file {} update failed", configurationFile.getName(), e);
            EventUtils.post(environment, "ERROR", "Configuration file " + configurationFile.getName() + " update failed: " + e.getMessage());
            if (!delegation) {
                // it's a client call, send a publisher
                updateLog.setStatus("Configuration file " + configurationFile.getName() + " update failed");
                updateLog.addUpdateMessage(new UpdateMessage("error", "Configuration file " + configurationFile.getName() + " update failed: " + e.getMessage()));
                PublisherUtils.publish(environment);
            }
            throw new UpdateException("Configuration file " + configurationFile.getName() + " update FAILED: " + e.getMessage(), e);
        }

        LOGGER.info("Configuration file {} updated", configurationFile.getName());
        EventUtils.post(environment, "UPDATE", "Configuration file " + configurationFile.getName() + " updated");
        if (!delegation) {
            if (updateLog.isUpdated()) {
                updateLog.setStatus("Configuration file " + configurationFile.getName() + " updated");
            } else {
                updateLog.setStatus("Configuration file " + configurationFile.getName() + " already up to date");
            }
            updateLog.addUpdateMessage(new UpdateMessage("info", "Configuration file " + configurationFile.getName() + " updated"));
            LOGGER.info("Publishing update report");
            PublisherUtils.publish(environment);
        }
    }

    /**
     * Wrapper method to check if a configuration file is update to date or not via WS.
     *
     * @param environmentName       the target environment name.
     * @param serverName            the target J2EE application server name.
     * @param applicationName       the target J2EE application name.
     * @param configurationFileName the target configuration file name.
     * @return true if the configuration file is up to date, false else.
     * @throws KalumetException in case of error during configuration file check.
     */
    public static boolean check(String environmentName, String serverName, String applicationName, String configurationFileName) throws KalumetException {
        LOGGER.info("Configuration file {} status check requested by WS", configurationFileName);

        // load configuration.
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
        J2EEApplication application = applicationServer.getJ2EEApplication(applicationName);
        if (application == null) {
            LOGGER.error("J2EE application {} is not found in J2EE application server {}", applicationName, serverName);
            throw new KalumetException("J2EE application " + applicationName + " is not found in J2EE application server " + serverName);
        }
        ConfigurationFile configurationFile = application.getConfigurationFile(configurationFileName);
        if (configurationFile == null) {
            LOGGER.error("Configuration file {} is not found in J2EE application {}", configurationFileName, applicationName);
            throw new KalumetException("Configuration file " + configurationFileName + " is not found in J2EE application " + applicationName);
        }

        if (configurationFile.getAgent() != null && configurationFile.getAgent().trim().length() > 0 && !configurationFile.getAgent().equals(Configuration.AGENT_ID)) {
            // the check needs to be delegate to another agent
            LOGGER.info("Delegating configuration file {} check to agent {}", configurationFile.getName(), configurationFile.getAgent());
            Agent agentDelegation = Configuration.CONFIG_CACHE.getAgent(configurationFile.getAgent());
            if (agentDelegation == null) {
                LOGGER.error("Agent {} is not found in the configuration", configurationFile.getAgent());
                throw new KalumetException("Agent " + configurationFile.getAgent() + " is not found in the configuration");
            }
            // call the service
            ConfigurationFileClient client = new ConfigurationFileClient(agentDelegation.getHostname(), agentDelegation.getPort());
            return client.check(environmentName, serverName, applicationName, configurationFileName);
        }

        // initialize the file manipulator.
        LOGGER.debug("Initializing file manipulator");
        FileManipulator fileManipulator = FileManipulator.getInstance();

        // constructs the configuration file URI.
        String configurationFileUri = VariableUtils.replace(configurationFile.getUri(), environment.getVariables());
        if (!FileManipulator.protocolExists(configurationFileUri)) {
            // the configuration file URI doesn't contain protocol prefix,
            // constructs the configuration file URI using the application URI
            LOGGER.debug("Configuration file {} URI is relative to the J2EE application one", configurationFile.getName());
            configurationFileUri = FileManipulator.format(VariableUtils.replace(application.getUri(), environment.getVariables())) + "!/" + configurationFileUri;
        }

        // get the application cache directory.
        LOGGER.debug("Creating the application cache directory");
        String applicationCacheDir = FileManipulator.createJ2EEApplicationCacheDir(environment, application);

        // get the configuration file cache.
        LOGGER.debug("Creating the configuration file cache location");
        String configurationFileCache = applicationCacheDir + "/config/" + configurationFile.getName();

        // copy the configuration file in the application cache directory
        LOGGER.debug("Copying the configuration file {} to the application cache directory", configurationFile.getName());
        fileManipulator.copy(configurationFileUri, configurationFileCache);

        // change mappings into the configuration file.
        LOGGER.debug("Replacing mappings key/value");
        for (Iterator mappingIterator = configurationFile.getMappings().iterator(); mappingIterator.hasNext(); ) {
            Mapping mapping = (Mapping) mappingIterator.next();
            FileManipulator.searchAndReplace(mapping.getKey(), VariableUtils.replace(mapping.getValue(), environment.getVariables()), configurationFileCache);
        }

        // compare the configuration file with the target one.
        return fileManipulator.contentEquals(configurationFileCache, VariableUtils.replace(configurationFile.getPath(), environment.getVariables()));
    }

}
