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
import org.apache.kalumet.utils.*;
import org.apache.kalumet.ws.client.ClientException;
import org.apache.kalumet.ws.client.SoftwareClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * Software software updater.
 */
public class SoftwareUpdater {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(SoftwareUpdater.class);

    /**
     * Updates a software.
     *
     * @param environment the target <code>Environment</code>.
     * @param software    the target <code>Software</code>.
     * @param updateLog   the <code>UpdateLog</code> to use.
     */
    public static void update(Environment environment, Software software, UpdateLog updateLog) throws UpdateException {
        LOGGER.info("Updating software {}", software.getName());

        if (!software.isActive()) {
            LOGGER.info("Software {} is inactive, so not updated", software.getName());
            updateLog.addUpdateMessage(new UpdateMessage("info", "Software " + software.getName() + " is inactive, so not updated"));
            EventUtils.post(environment, "UPDATE", "Software " + software.getName() + " is inactive, so not updated");
            return;
        }

        if (software.getAgent() != null && software.getAgent().trim().length() > 0 && !software.getAgent().equals(Configuration.AGENT_ID)) {
            LOGGER.info("Delegating software {} update to agent {}", software.getName(), software.getAgent());
            EventUtils.post(environment, "UPDATE", "Delegating software " + software.getName() + " update to agent " + software.getAgent());
            updateLog.addUpdateMessage(new UpdateMessage("info", "Delegating software " + software.getName() + " update to agent " + software.getAgent()));
            Agent delegationAgent = Configuration.CONFIG_CACHE.getAgent(software.getAgent());
            if (delegationAgent == null) {
                LOGGER.error("Agent {} is not found in the configuration", software.getAgent());
                throw new UpdateException("Agent " + software.getAgent() + " is not found in the configuration");
            }
            try {
                LOGGER.debug("Call software WS");
                SoftwareClient client = new SoftwareClient(delegationAgent.getHostname(), delegationAgent.getPort());
                client.update(environment.getName(), software.getName(), true);
            } catch (ClientException clientException) {
                LOGGER.error("Software {} update failed", software.getName(), clientException);
                throw new UpdateException("Software " + software.getName() + " update failed", clientException);
            }
            return;
        }

        // add an update message
        updateLog.addUpdateMessage(new UpdateMessage("info", "Updating software " + software.getName()));
        // post an event
        EventUtils.post(environment, "UPDATE", "Updating software " + software.getName());

        // iterate in the software update plan
        for (Iterator updatePlanIterator = software.getUpdatePlan().iterator(); updatePlanIterator.hasNext(); ) {
            Object item = updatePlanIterator.next();

            // command update
            if (item instanceof Command) {
                Command command = (Command) item;
                try {
                    SoftwareUpdater.executeCommand(environment, software, command, updateLog);
                } catch (Exception e) {
                    if (command.isBlocker()) {
                        LOGGER.error("Command {} execution failed", command.getName(), e);
                        updateLog.addUpdateMessage(new UpdateMessage("error", "Command " + command.getName() + " execution failed: " + e.getMessage()));
                        EventUtils.post(environment, "ERROR", "Command " + command.getName() + " execution failed: " + e.getMessage());
                        throw new UpdateException("Command " + command.getName() + " execution failed", e);
                    } else {
                        LOGGER.warn("Command {} execution failed", command.getName(), e);
                        updateLog.addUpdateMessage(new UpdateMessage("warn", "Command " + command.getName() + " execution failed: " + e.getMessage()));
                        updateLog.addUpdateMessage(new UpdateMessage("info", "Command " + command.getName() + " is not an update blocker, update continues"));
                        EventUtils.post(environment, "WARN", "Command " + command.getName() + " execution failed: " + e.getMessage());
                        EventUtils.post(environment, "UPDATE", "Command " + command.getName() + " is not an update blocker, update continues");
                    }
                }
            }

            // location update
            if (item instanceof Location) {
                Location location = (Location) item;
                try {
                    SoftwareUpdater.updateLocation(environment, software, location, updateLog);
                } catch (Exception e) {
                    if (location.isBlocker()) {
                        LOGGER.error("Location {} update failed", location.getName(), e);
                        updateLog.addUpdateMessage(new UpdateMessage("error", "Location " + location.getName() + " update failed: " + e.getMessage()));
                        EventUtils.post(environment, "ERROR", "Location " + location.getName() + " update failed: " + e.getMessage());
                        throw new UpdateException("Location " + location.getName() + " update failed", e);
                    } else {
                        LOGGER.warn("Location " + location.getName() + " update failed", e);
                        updateLog.addUpdateMessage(new UpdateMessage("warn", "Location " + location.getName() + " update failed: " + e.getMessage()));
                        updateLog.addUpdateMessage(new UpdateMessage("info", "Location " + location.getName() + " is not an update blocker, update continues"));
                        EventUtils.post(environment, "WARN", "Location " + location.getName() + " execution failed: " + e.getMessage());
                        EventUtils.post(environment, "UPDATE", "Location " + location.getName() + " is not an update blocker, update continues");
                    }
                }
            }

            // configuration update
            if (item instanceof ConfigurationFile) {
                ConfigurationFile configurationFile = (ConfigurationFile) item;
                try {
                    SoftwareUpdater.updateConfigurationFile(environment, software, configurationFile, updateLog);
                } catch (Exception e) {
                    if (configurationFile.isBlocker()) {
                        LOGGER.error("Configuration file {} update failed", configurationFile.getName(), e);
                        updateLog.addUpdateMessage(new UpdateMessage("error", "Configuration file " + configurationFile.getName() + " update failed: " + e.getMessage()));
                        EventUtils.post(environment, "ERROR", "Configuration file " + configurationFile.getName() + " update failed: " + e.getMessage());
                        throw new UpdateException("Configuration file " + configurationFile.getName() + " update failed", e);
                    } else {
                        LOGGER.warn("Configuration file {} update failed", configurationFile.getName(), e);
                        updateLog.addUpdateMessage(new UpdateMessage("warn", "Configuration file " + configurationFile.getName() + " update failed: " + e.getMessage()));
                        updateLog.addUpdateMessage(new UpdateMessage("info", "Configuration file " + configurationFile.getName() + " is not an update blocker, update continues"));
                        EventUtils.post(environment, "WARN", "Configuration file " + configurationFile.getName() + " update failed: " + e.getMessage());
                        EventUtils.post(environment, "UPDATE", "Configuration file " + configurationFile.getName() + " is not an update blocker, update continues");
                    }
                }
            }

            // database update
            if (item instanceof Database) {
                Database database = (Database) item;
                try {
                    SoftwareUpdater.updateDatabase(environment, software, database, updateLog);
                } catch (Exception e) {
                    if (database.isBlocker()) {
                        LOGGER.error("Database {} update failed", database.getName(), e);
                        updateLog.addUpdateMessage(new UpdateMessage("error", "Database " + database.getName() + " update failed: " + e.getMessage()));
                        EventUtils.post(environment, "ERROR", "Database " + database.getName() + " update failed: " + e.getMessage());
                        throw new UpdateException("Database " + database.getName() + " update failed", e);
                    } else {
                        LOGGER.warn("Database {} update failed", database.getName(), e);
                        updateLog.addUpdateMessage(new UpdateMessage("warn", "Database " + database.getName() + " update failed: " + e.getMessage()));
                        updateLog.addUpdateMessage(new UpdateMessage("info", "Database " + database.getName() + " is not an update blocker, update continues"));
                        EventUtils.post(environment, "WARN", "Database " + database.getName() + " update failed: " + e.getMessage());
                        EventUtils.post(environment, "UPDATE", "Database " + database.getName() + " is not an update blocker, update continues");
                    }
                }
            }
        }
    }

    /**
     * Wrapper method to update a software (via WebService).
     *
     * @param environmentName the target environment name.
     * @param softwareName    the target software name.
     * @param delegation      flag indicating if the update is a delegation from another agent (true), or a client call (false).
     */
    public static void update(String environmentName, String softwareName, boolean delegation) throws KalumetException {
        LOGGER.info("Software {} update requested by WS", softwareName);

        LOGGER.debug("Loading configuration");
        Kalumet kalumet = Kalumet.digeste(Configuration.CONFIG_LOCATION);

        Environment environment = kalumet.getEnvironment(environmentName);
        if (environment == null) {
            LOGGER.error("Environment {} is not found in the configuration", environmentName);
            throw new KalumetException("Environment " + environmentName + " is not found in the configuration");
        }
        Software software = environment.getSoftware(softwareName);
        if (software == null) {
            LOGGER.error("Software {} is not found in environment {}", softwareName, environment.getName());
            throw new KalumetException("Software " + softwareName + " is not found in environment " + environment.getName());
        }

        LOGGER.debug("Creating an update log");
        UpdateLog updateLog = new UpdateLog("Software " + software.getName() + " update in progress ...", environment.getName(), environment);
        EventUtils.post(environment, "UPDATE", "Software " + software.getName() + " update requested by WS");

        if (!delegation) {
            LOGGER.info("Send a notification and waiting for the count down");
            updateLog.addUpdateMessage(new UpdateMessage("info", "Send a notification and waiting for the count down"));
            EventUtils.post(environment, "UPDATE", "Send a notification and waiting for the count down");
            NotifierUtils.waitAndNotify(environment);
        }
        try {
            LOGGER.debug("Call the software updater");
            SoftwareUpdater.update(environment, software, updateLog);
        } catch (Exception e) {
            LOGGER.error("Software {} update failed", software.getName(), e);
            EventUtils.post(environment, "ERROR", "Software " + software.getName() + " update failed: " + e.getMessage());
            if (!delegation) {
                updateLog.setStatus("Software " + software.getName() + " update failed");
                updateLog.addUpdateMessage(new UpdateMessage("error", "Software " + software.getName() + " update failed: " + e.getMessage()));
                PublisherUtils.publish(environment);
            }
            throw new KalumetException("Software " + software.getName() + " update failed", e);
        }

        LOGGER.info("Software {} updated", software.getName());
        EventUtils.post(environment, "UPDATE", "Software " + software.getName() + " updated");
        if (!delegation) {
            LOGGER.debug("The update is a client call, publish result");
            EventUtils.post(environment, "UPDATE", "Update completed");
            if (updateLog.isUpdated()) {
                updateLog.setStatus("Software " + software.getName() + " updated");
            } else {
                updateLog.setStatus("Software " + software.getName() + " already up to date");
            }
            updateLog.addUpdateMessage(new UpdateMessage("info", "Update completed"));
            LOGGER.info("Publishing update report");
            PublisherUtils.publish(environment);
        }
    }

    /**
     * Execute a software command.
     *
     * @param environment the target environment.
     * @param software    the target software.
     * @param command     the target command.
     * @param updateLog   the <code>UpdateLog</code> to use.
     * @throws UpdateException in case of error during the command execution.
     */
    public static void executeCommand(Environment environment, Software software, Command command, UpdateLog updateLog) throws UpdateException {
        LOGGER.info("Executing software {} command {}", software.getName(), command.getName());

        if (!command.isActive()) {
            LOGGER.info("Software {} command {} is inactive, so not executed", software.getName(), command.getName());
            updateLog.addUpdateMessage(new UpdateMessage("info", "Software " + software.getName() + " command " + command.getName() + " is inactive, so not executed"));
            EventUtils.post(environment, "UPDATE", "Software " + software.getName() + " command " + command.getName() + " is inactive, so not executed");
            return;
        }

        if (command.getAgent() != null && command.getAgent().trim().length() > 0 && !command.getAgent().equals(Configuration.AGENT_ID)) {
            // delegates the command execution to another agent
            LOGGER.info("Delegating command {} execution to agent {}", command.getName(), command.getAgent());
            Agent agentDelegation = Configuration.CONFIG_CACHE.getAgent(command.getAgent());
            updateLog.addUpdateMessage(new UpdateMessage("info", "Delegating command " + command.getName() + " execution to agent " + command.getAgent()));
            EventUtils.post(environment, "UPDATE", "Delegating command " + command.getName() + " execution to agent " + command);
            if (agentDelegation == null) {
                LOGGER.error("Agent {} is not found in the configuration", command.getAgent());
                throw new UpdateException("Agent " + command.getAgent() + " is not found in the configuration");
            }
            try {
                LOGGER.debug("Call software WS");
                SoftwareClient client = new SoftwareClient(agentDelegation.getHostname(), agentDelegation.getPort());
                client.executeCommand(environment.getName(), software.getName(), command.getName(), true);
            } catch (ClientException clientException) {
                LOGGER.error("Command {} execution failed", command.getName(), clientException);
                throw new UpdateException("Command " + command.getName() + " execution failed: " + clientException.getMessage(), clientException);
            }
            return;
        }

        String commandExec = VariableUtils.replace(command.getCommand(), environment.getVariables());
        String output = null;
        try {
            output = CommandUtils.execute(commandExec);
        } catch (KalumetException kalumetException) {
            LOGGER.error("Command {} execution failed", command.getName(), kalumetException);
            throw new UpdateException("Command " + command.getName() + " execution failed", kalumetException);
        }

        LOGGER.info("Software {} command {} executed: {}", new Object[]{ software.getName(), command.getName(), output });
        // add an update message
        updateLog.addUpdateMessage(new UpdateMessage("info", "Software " + software.getName() + " command " + command.getName() + " executed: " + output));
        // post an event
        EventUtils.post(environment, "UPDATE", "Software " + software.getName() + " command " + command.getName() + " executed: " + output);
    }

    /**
     * Wrapper method to execute a software command (via WS).
     *
     * @param environmentName the target environment name.
     * @param softwareName    the target software name.
     * @param commandName     the target command name.
     * @param delegation      flag indicating if the execution is a delegation from another agent (true), or a client call (false).
     */
    public static void executeCommand(String environmentName, String softwareName, String commandName, boolean delegation) throws KalumetException {
        LOGGER.info("Software {} command {} execution requested by WS", softwareName, commandName);
        Kalumet kalumet = Kalumet.digeste(Configuration.CONFIG_LOCATION);
        Environment environment = kalumet.getEnvironment(environmentName);
        if (environment == null) {
            LOGGER.error("Environment {} is not found in the configuration", environmentName);
            throw new KalumetException("Environment " + environmentName + " is not found in the configuration");
        }
        Software software = environment.getSoftware(softwareName);
        if (software == null) {
            LOGGER.error("Software {} is not found in environment {}", softwareName, environmentName);
            throw new KalumetException("Software " + softwareName + " is not found in environment " + environmentName);
        }
        Command command = software.getCommand(commandName);
        if (command == null) {
            LOGGER.error("Command {} is not found in software {}", commandName, softwareName);
            throw new KalumetException("Command " + commandName + " is not found in software " + softwareName);
        }
        // update the agent configuration cache
        LOGGER.debug("Updating agent configuration cache");
        Configuration.CONFIG_CACHE = kalumet;
        // post a journal event
        EventUtils.post(environment, "UPDATE", "Software " + softwareName + " command " + commandName + " execution requested by WS");
        // create an update logger
        UpdateLog updateLog = new UpdateLog("Software " + softwareName + " command " + commandName + " execution in progress ...", environment.getName(), environment);
        if (!delegation) {
            // the call is not a delegation from another agent
            LOGGER.info("Send a notification and waiting for the count down");
            EventUtils.post(environment, "UPDATE", "Send a notification and waiting for the count down");
            NotifierUtils.waitAndNotify(environment);
        }
        try {
            // call the updater
            LOGGER.debug("Call the software updater");
            SoftwareUpdater.executeCommand(environment, software, command, updateLog);
        } catch (Exception e) {
            LOGGER.error("Command {} execution failed", command.getName(), e);
            EventUtils.post(environment, "ERROR", "Command " + command.getName() + " execution failed: " + e.getMessage());
            if (!delegation) {
                updateLog.setStatus("Command " + command.getName() + " execution failed");
                updateLog.addUpdateMessage(new UpdateMessage("error", "Command " + command.getName() + " execution failed: " + e.getMessage()));
                PublisherUtils.publish(environment);
            }
            throw new UpdateException("Command " + command.getName() + " execution failed", e);
        }

        // command execution is completed
        LOGGER.info("Command {} has been executed successfully", command.getName());
        EventUtils.post(environment, "UPDATE", "Command " + command.getName() + " has been executed successfully");
        if (!delegation) {
            updateLog.setStatus("Command " + command.getName() + " has been executed successfully");
            updateLog.addUpdateMessage(new UpdateMessage("info", "Command " + command.getName() + " has been executed successfully"));
            LOGGER.info("Publishing update report");
            PublisherUtils.publish(environment);
        }
    }

    /**
     * Wrapper method to update a location (via WS).
     *
     * @param environmentName the target environment name.
     * @param softwareName    the target software name.
     * @param locationName the target location name.
     * @param delegation true if the call is performed by another agent, false else.
     * @throws UpdateException in case of location update failure.
     */
    public static void updateLocation(String environmentName, String softwareName, String locationName, boolean delegation) throws KalumetException {
        LOGGER.info("Software {} location {} update requested by WS", softwareName, locationName);

        // loading configuration
        LOGGER.debug("Loading configuration");
        Kalumet kalumet = Kalumet.digeste(Configuration.CONFIG_LOCATION);
        Environment environment = kalumet.getEnvironment(environmentName);
        if (environment == null) {
            LOGGER.error("Environment {} is not found in the configuration", environmentName);
            throw new KalumetException("Environment " + environmentName + " is not found in the configuration");
        }
        Software software = environment.getSoftware(softwareName);
        if (software == null) {
            LOGGER.error("Software {} is not found in environment {}", softwareName, environmentName);
            throw new KalumetException("Software " + softwareName + " is not found in environment " + environmentName);
        }
        Location location = software.getLocation(locationName);
        if (location == null) {
            LOGGER.error("Location {} is not found in software {}",locationName, softwareName);
            throw new KalumetException("Location " + locationName + " is not found in software " + softwareName);
        }

        // update configuration cache
        LOGGER.debug("Updating configuration cache");
        Configuration.CONFIG_CACHE = kalumet;

        // post journal event
        EventUtils.post(environment, "UPDATE", "Software " + software.getName() + " location " + location.getName() + " update requested by WS");
        // create an update logger
        UpdateLog updateLog = new UpdateLog("Software " + software.getName() + " location " + location.getName() + " update in progress ....", environment.getName(), environment);

        if (!delegation) {
            // the call is not a delegation from another agent, it's an atomic update
            LOGGER.info("Send a notification and waiting for the count down");
            EventUtils.post(environment, "UPDATE", "Send a notification and waiting for the count down");
            NotifierUtils.waitAndNotify(environment);
        }
        try {
            // call software updater
            LOGGER.debug("Call software updater");
            SoftwareUpdater.updateLocation(environment, software, location, updateLog);
        } catch (Exception e) {
            LOGGER.error("Location {} update failed", location.getName(), e);
            EventUtils.post(environment, "ERROR", "Location " + location.getName() + " update failed: " + e.getMessage());
            if (!delegation) {
                updateLog.setStatus("Location " + locationName + " update failed");
                updateLog.addUpdateMessage(new UpdateMessage("error", "Location " + locationName + " update failed: " + e.getMessage()));
                PublisherUtils.publish(environment);
            }
            throw new UpdateException("Location " + location.getName() + " update failed", e);
        }

        // location updated
        LOGGER.info("Location {} updated", location.getName());
        EventUtils.post(environment, "UPDATE", "Location " + location.getName() + " updated");
        if (!delegation) {
            updateLog.setStatus("Location " + location.getName() + " updated");
            updateLog.addUpdateMessage(new UpdateMessage("info", "Location " + location.getName() + " updated"));
            LOGGER.info("Publishing update report");
            PublisherUtils.publish(environment);
        }
    }

    /**
     * Updates an <code>Software</code> <code>Location</code>.
     *
     * @param environment the target <code>Environment</code>.
     * @param software    the target <code>Software</code>.
     * @param location    the target <code>Location</code>.
     * @param updateLog   the <code>UpdateLog</code> to use.
     * @throws UpdateException in case of error during the location update.
     */
    public static void updateLocation(Environment environment, Software software, Location location, UpdateLog updateLog) throws UpdateException {
        LOGGER.info("Updating software {} location {}", software.getName(), location.getName());

        if (!location.isActive()) {
            LOGGER.info("Software {} location {} is inactive, so not updated", software.getName(), location.getName());
            updateLog.addUpdateMessage(new UpdateMessage("info", "Software " + software.getName() + " location " + location.getName() + " is inactive, so not updated"));
            EventUtils.post(environment, "UPDATE", "Software " + software.getName() + " location " + location.getName() + "is inactive, so not updated");
            return;
        }

        if (location.getAgent() != null && location.getAgent().trim().length() > 0 && !location.getAgent().equals(Configuration.AGENT_ID)) {
            // delegates the location update to another agent
            LOGGER.info("Delegating location {} update to agent {}", location.getName(), location.getAgent());
            Agent agentDelegation = Configuration.CONFIG_CACHE.getAgent(location.getAgent());
            updateLog.addUpdateMessage(new UpdateMessage("info", "Delegating location " + location.getName() + " update to agent " + location.getAgent()));
            EventUtils.post(environment, "UPDATE", "Delegating location " + location.getName() + " update to agent " + location.getAgent());
            if (agentDelegation == null) {
                LOGGER.error("Agent {} is not found in the configuration", location.getAgent());
                throw new UpdateException("Agent " + location.getAgent() + " is not found in the configuration");
            }
            try {
                // call the WebService
                LOGGER.debug("Call software WS");
                SoftwareClient client = new SoftwareClient(agentDelegation.getHostname(), agentDelegation.getPort());
                client.updateLocation(environment.getName(), software.getName(), location.getName(), true);
            } catch (ClientException clientException) {
                LOGGER.error("Location {} update failed", location.getName(), clientException);
                throw new UpdateException("Location " + location.getName() + " update failed: " + clientException.getMessage(), clientException);
            }
            return;
        }

        // constructs the location URI
        String locationUri = VariableUtils.replace(location.getUri(), environment.getVariables());
        if (!FileManipulator.protocolExists(locationUri)) {
            LOGGER.debug("The location URI is relative to the software URI");
            locationUri = FileManipulator.format(VariableUtils.replace(software.getUri(), environment.getVariables())) + "!/" + locationUri;
        }
        // constructs the location destination path
        String locationPath = VariableUtils.replace(location.getPath(), environment.getVariables());
        // get a file manipulator
        FileManipulator fileManipulator = null;
        try {
            fileManipulator = new FileManipulator();
        } catch (FileManipulatorException fileManipulatorException) {
            LOGGER.error("Can't initialize file manipulator", fileManipulatorException);
            throw new UpdateException("Can't initialize file manipulator", fileManipulatorException);
        }
        try {
            LOGGER.debug("Copying {} to {}", locationUri, locationPath);
            fileManipulator.copy(locationUri, locationPath);
            updateLog.setUpdated(true);
        } catch (FileManipulatorException fileManipulatorException) {
            LOGGER.error("Location {} update failed", location.getName(), fileManipulatorException);
            throw new UpdateException("Location " + location.getName() + " update failed", fileManipulatorException);
        }
        LOGGER.info("Software {} location {} updated", software.getName(), location.getName());
        updateLog.addUpdateMessage(new UpdateMessage("info", "Software " + software.getName() + " location " + location.getName() + " updated"));
        EventUtils.post(environment, "UPDATE", "Software " + software.getName() + " location " + location.getName() + " updated");
    }

    /**
     * Wrapper method to update a configuration file (via WS).
     *
     * @param environmentName       the target environment name.
     * @param softwareName          the target software name.
     * @param configurationFileName the target configuration file name.
     * @param delegation            true if the call is made by another agent (delegation), false if the call is made by a client.
     * @throws KalumetException in case of update failure.
     */
    public static void updateConfigurationFile(String environmentName, String softwareName, String configurationFileName, boolean delegation) throws KalumetException {
        LOGGER.info("Software {} configuration file {} update requested by WS", softwareName, configurationFileName);

        // loading configuration
        LOGGER.debug("Loading configuration");
        Kalumet kalumet = Kalumet.digeste(Configuration.CONFIG_LOCATION);

        Environment environment = kalumet.getEnvironment(environmentName);
        if (environment == null) {
            LOGGER.error("Environment {} is not found in the configuration", environmentName);
            throw new KalumetException("Environment " + environmentName + " is not found in the configuration");
        }
        Software software = environment.getSoftware(softwareName);
        if (software == null) {
            LOGGER.error("Software {} is not found in environment {}", softwareName, environmentName);
            throw new KalumetException("Software " + softwareName + " is not found in environment " + environmentName);
        }
        ConfigurationFile configurationFile = software.getConfigurationFile(configurationFileName);
        if (configurationFile == null) {
            LOGGER.error("Configuration file {} is not found in software {}", configurationFileName, softwareName);
            throw new KalumetException("Configuration file " + configurationFileName + " is not found in software " + softwareName);
        }

        // update agent configuration cache
        LOGGER.debug("Updating agent configuration cache");
        Configuration.CONFIG_CACHE = kalumet;

        // post journal event
        EventUtils.post(environment, "UPDATE", "Software " + software.getName() + " configuration file " + configurationFile.getName() + " update requested by WS");
        // create update log
        UpdateLog updateLog = new UpdateLog("Software " + software.getName() + " configuration file " + configurationFile.getName() + " update in progress ...", environment.getName(), environment);

        if (!delegation) {
            // the update is not call by another agent, it's an atomic update
            LOGGER.info("Send a notification and waiting for the count down");
            EventUtils.post(environment, "UPDATE", "Send a notification and waiting for the count down");
            NotifierUtils.waitAndNotify(environment);
        }

        try {
            // call software updater
            LOGGER.debug("Call software updater");
            SoftwareUpdater.updateConfigurationFile(environment, software, configurationFile, updateLog);
        } catch (Exception e) {
            LOGGER.error("Configuration file {} update failed", configurationFile.getName(), e);
            EventUtils.post(environment, "ERROR", "Configuration file " + configurationFile.getName() + " update failed: " + e.getMessage());
            if (!delegation) {
                updateLog.setStatus("Configuration file " + configurationFile.getName() + " update failed");
                updateLog.addUpdateMessage(new UpdateMessage("error", "Configuration file " + configurationFile.getName() + " update failed: " + e.getMessage()));
                PublisherUtils.publish(environment);
            }
            throw new UpdateException("Configuration file " + configurationFile.getName() + " update failed", e);
        }

        // configuration file updated
        LOGGER.info("Configuration file {} updated", configurationFile.getName());
        EventUtils.post(environment, "UPDATE", "Configuration file " + configurationFile.getName() + " updated");
        if (!delegation) {
            updateLog.setStatus("Configuration file " + configurationFile.getName() + " updated");
            updateLog.addUpdateMessage(new UpdateMessage("info", "Configuration file " + configurationFile.getName() + " updated"));
            LOGGER.info("Publishing update report");
            PublisherUtils.publish(environment);
        }
    }

    /**
     * Update an <code>Software</code> <code>ConfigurationFile</code>.
     *
     * @param environment       the target <code>Environment</code>.
     * @param software          the target <code>Software</code>.
     * @param configurationFile the target <code>ConfigurationFile</code>.
     * @param updateLog         the <code>UpdateLog</code> to use.
     * @throws UpdateException in case of error during the configuration file update.
     */
    public static void updateConfigurationFile(Environment environment, Software software, ConfigurationFile configurationFile, UpdateLog updateLog) throws UpdateException {
        LOGGER.info("Updating software {} configuration file {}", software.getName(), configurationFile.getName());

        if (!configurationFile.isActive()) {
            LOGGER.info("Software {} configuration file {} is inactive, so not updated", software.getName(), configurationFile.getName());
            updateLog.addUpdateMessage(new UpdateMessage("info", "Software " + software.getName() + " configuration file " + configurationFile.getName() + " is inactive, so not updated"));
            EventUtils.post(environment, "UPDATE", "Software " + software.getName() + " configuration file " + configurationFile.getName() + " is inactive, so not updated");
            return;
        }

        if (configurationFile.getAgent() != null && configurationFile.getAgent().trim().length() > 0 && !configurationFile.getAgent().equals(Configuration.AGENT_ID)) {
            // delegates configuration file update to another agent
            LOGGER.info("Delegating configuration file {} update to agent {}", configurationFile.getName(), configurationFile.getAgent());
            Agent agentDelegation = Configuration.CONFIG_CACHE.getAgent(configurationFile.getAgent());
            updateLog.addUpdateMessage(new UpdateMessage("info", "Delegating configuration file " + configurationFile.getName() + " update to agent " + configurationFile.getAgent()));
            EventUtils.post(environment, "UPDATE", "Delegating configuration file " + configurationFile.getName() + " update to agent " + configurationFile.getAgent());
            if (agentDelegation == null) {
                LOGGER.error("Agent {} is not found in the configuration", configurationFile.getAgent());
                throw new UpdateException("Agent " + configurationFile.getAgent() + " is not found in the configuration");
            }
            try {
                // call WS
                LOGGER.debug("Call software WS");
                SoftwareClient client = new SoftwareClient(agentDelegation.getHostname(), agentDelegation.getPort());
                client.updateConfigurationFile(environment.getName(), software.getName(), configurationFile.getName(), true);
            } catch (ClientException clientException) {
                LOGGER.error("Configuration file {} update failed", configurationFile.getName(), clientException);
                throw new UpdateException("Configuration file " + configurationFile.getName() + " update failed", clientException);
            }
            return;
        }

        // defines the configuration file URI
        LOGGER.debug("Getting configuration file URI");
        String configurationFileUri = VariableUtils.replace(configurationFile.getUri(), environment.getVariables());
        if (!FileManipulator.protocolExists(configurationFileUri)) {
            LOGGER.debug("The configuration file URI is relative to the software URI");
            configurationFileUri = FileManipulator.format(VariableUtils.replace(software.getUri(), environment.getVariables())) + "!/" + configurationFileUri;
        }

        // defines the software cache directory
        LOGGER.debug("Getting software cache directory");
        String softwareCacheDir = null;
        try {
            softwareCacheDir = FileManipulator.createSoftwareCacheDir(environment, software);
        } catch (FileManipulatorException fileManipulatorException) {
            LOGGER.error("Can't initialize software cache directory", fileManipulatorException);
            throw new UpdateException("Can't initialize software cache directory", fileManipulatorException);
        }

        // initializes file manipulator
        LOGGER.debug("Initializing file manipulator");
        FileManipulator fileManipulator = null;
        try {
            fileManipulator = new FileManipulator();
        } catch (FileManipulatorException fileManipulatorException) {
            LOGGER.error("Can't initialize file manipulator", fileManipulatorException);
            throw new UpdateException("Can't initialize file manipulator", fileManipulatorException);
        }

        // copy configuration file into cache
        String configurationFileCacheLocation = softwareCacheDir + "/config/" + configurationFile.getName();
        LOGGER.debug("Copying configuration file {} into cache", configurationFile.getName());
        try {
            fileManipulator.copy(configurationFileUri, configurationFileCacheLocation);
        } catch (FileManipulatorException fileManipulatorException) {
            LOGGER.error("Can't copy configuration file {} into cache", configurationFile.getName(), fileManipulatorException);
            throw new UpdateException("Can't copy configuration file " + configurationFile.getName() + " into cache", fileManipulatorException);
        }

        // change mappings into the configuration file cache
        LOGGER.debug("Replacing mappings key/value");
        for (Iterator mappingIterator = configurationFile.getMappings().iterator(); mappingIterator.hasNext(); ) {
            Mapping mapping = (Mapping) mappingIterator.next();
            FileManipulator.searchAndReplace(mapping.getKey(), VariableUtils.replace(mapping.getValue(), environment.getVariables()), configurationFileCacheLocation);
        }

        // compare configuration file cache with target
        LOGGER.debug("Comparing the configuration file cache with the target location");
        String configurationFileDestinationPath = VariableUtils.replace(configurationFile.getPath(), environment.getVariables());
        try {
            if (!fileManipulator.contentEquals(configurationFileCacheLocation, configurationFileDestinationPath)) {
                // the configuration file needs to be updated
                LOGGER.debug("Configuration file {} needs to be updated", configurationFile.getName());
                fileManipulator.copy(configurationFileCacheLocation, configurationFileDestinationPath);
                updateLog.setStatus("UPDATE PERFORMED");
                updateLog.setUpdated(true);
                updateLog.addUpdateMessage(new UpdateMessage("info", "Configuration file " + configurationFile.getName() + " updated"));
                EventUtils.post(environment, "UPDATE", "Configuration file " + configurationFile.getName() + " updated");
                LOGGER.info("Configuration file {} updated", configurationFile.getName());
            }
        } catch (FileManipulatorException fileManipulatorException) {
            LOGGER.error("Configuration file {} update failed", configurationFile.getName(), fileManipulatorException);
            throw new UpdateException("Configuration file " + configurationFile.getName() + " update failed: " + fileManipulatorException.getMessage(), fileManipulatorException);
        }
    }

    /**
     * Wrapper method to update a software database (via WS).
     *
     * @param environmentName the target environment name.
     * @param softwareName    the target software name.
     * @param databaseName    the target database name.
     * @param delegation      true if the call is made by another agent (delegation), false if the call is made by a client.
     * @throws KalumetException in case of update failure.
     */
    public static void updateDatabase(String environmentName, String softwareName, String databaseName, boolean delegation) throws KalumetException {
        LOGGER.info("Software {} database {} update requested by WS", softwareName, databaseName);

        // load configuration
        Kalumet kalumet = Kalumet.digeste(Configuration.CONFIG_LOCATION);
        Environment environment = kalumet.getEnvironment(environmentName);
        if (environment == null) {
            LOGGER.error("Environment {} is not found in the configuration", environmentName);
            throw new KalumetException("Environment " + environmentName + " is not found in the configuration");
        }
        Software software = environment.getSoftware(softwareName);
        if (software == null) {
            LOGGER.error("Software {} is not found in environment {}", softwareName, environmentName);
            throw new KalumetException("Software " + softwareName + " is not found in environment " + environmentName);
        }
        Database database = software.getDatabase(databaseName);
        if (database == null) {
            LOGGER.error("Database {} is not found in software {}", databaseName, softwareName);
            throw new KalumetException("Database " + databaseName + " is not found in software " + softwareName);
        }

        // updating configuration cache
        LOGGER.debug("Updating configuration cache");
        Configuration.CONFIG_CACHE = kalumet;

        // post journal event
        EventUtils.post(environment, "UPDATE", "Software " + software.getName() + " database " + database.getName() + " update requested by WS");
        // create update log
        UpdateLog updateLog = new UpdateLog("Software " + software.getName() + " database " + database.getName() + " update in progress ...", environment.getName(), environment);

        if (!delegation) {
            // the call is not a delegation from another agent, it's an atomic update
            LOGGER.info("Send a notification and waiting for the count down");
            EventUtils.post(environment, "UPDATE", "Send a notification and waiting for the count down");
            NotifierUtils.waitAndNotify(environment);
        }

        try {
            // call software updater
            LOGGER.debug("Call software updater");
            SoftwareUpdater.updateDatabase(environment, software, database, updateLog);
        } catch (Exception e) {
            LOGGER.error("Database {} update failed", database.getName(), e);
            EventUtils.post(environment, "ERROR", "Database " + database.getName() + " update failed: " + e.getMessage());
            if (!delegation) {
                updateLog.setStatus("Database " + database.getName() + " update failed");
                updateLog.addUpdateMessage(new UpdateMessage("error", "Database " + database.getName() + " update failed: " + e.getMessage()));
                PublisherUtils.publish(environment);
            }
            throw new UpdateException("Database " + database.getName() + " update failed", e);
        }

        // database updated
        LOGGER.info("Database {} updated", database.getName());
        EventUtils.post(environment, "UPDATE", "Database " + database.getName() + " updated");
        if (!delegation) {
            updateLog.setStatus("Database " + database.getName() + " updated");
            updateLog.addUpdateMessage(new UpdateMessage("info", "Database " + database.getName() + " updated"));
            LOGGER.info("Publishing update report");
            PublisherUtils.publish(environment);
        }
    }

    /**
     * Update a software database.
     *
     * @param environment the target <code>Environment</code>.
     * @param software    the target <code>Software</code>.
     * @param database    the target <code>Database</code>.
     * @param updateLog   the update logger to use.
     */
    public static void updateDatabase(Environment environment, Software software, Database database, UpdateLog updateLog) throws UpdateException {
        LOGGER.info("Update software {} database {}", software.getName(), database.getName());

        if (!database.isActive()) {
            LOGGER.info("Software {} database {} (environment {}) is inactive, so not updated", new Object[]{ software.getName(), database.getName(), environment.getName() });
            updateLog.addUpdateMessage(new UpdateMessage("info", "Software " + software.getName() + " database " + database.getName() + " is inactive, so not updated"));
            EventUtils.post(environment, "UPDATE", "Software " + software.getName() + " database " + database.getName() + " is inactive, so not updated");
            return;
        }

        if (database.getAgent() != null && database.getAgent().trim().length() > 0 && !database.getAgent().equals(Configuration.AGENT_ID)) {
            // the database update is delegated to another agent
            LOGGER.info("Delegating software {} database {} update to agent {}", new Object[]{ software.getName(), database.getName(), database.getAgent() });
            Agent delegationAgent = Configuration.CONFIG_CACHE.getAgent(database.getAgent());
            updateLog.addUpdateMessage(new UpdateMessage("info", "Delegating database " + database.getName() + " update to agent " + database.getAgent()));
            EventUtils.post(environment, "UPDATE", "Delegating database " + database.getName() + " update to agent " + database.getAgent());
            if (delegationAgent == null) {
                // the database agent is not found in the configuration
                LOGGER.error("Agent {} is not found in the configuration", database.getAgent());
                throw new UpdateException("Agent " + database.getAgent() + " is not found in the configuration");
            }
            try {
                // call the WebService
                LOGGER.debug("Call software WS");
                SoftwareClient client = new SoftwareClient(delegationAgent.getHostname(), delegationAgent.getPort());
                client.updateDatabase(environment.getName(), software.getName(), database.getName(), true);
            } catch (ClientException clientException) {
                LOGGER.error("Database {} update failed", database.getName(), clientException);
                throw new UpdateException("Database " + database.getName() + " update failed", clientException);
            }
            return;
        }

        // launch SQL scripts of the database
        LOGGER.debug("Executing SQL scripts");
        for (Iterator sqlScriptIterator = database.getSqlScripts().iterator(); sqlScriptIterator.hasNext(); ) {
            SqlScript sqlScript = (SqlScript) sqlScriptIterator.next();
            try {
                SoftwareUpdater.executeSqlScript(environment, software, database, sqlScript, updateLog);
            } catch (UpdateException updateException) {
                // SQL script execution has failed
                if (sqlScript.isBlocker()) {
                    // the SQL script is update blocker
                    LOGGER.error("SQL script {} execution failed", updateException);
                    throw new UpdateException("SQL script " + sqlScript.getName() + " execution failed", updateException);
                } else {
                    // the SQL script is not update blocker
                    LOGGER.warn("SQL script {} execution failed", updateException);
                    updateLog.addUpdateMessage(new UpdateMessage("warn", "SQL script " + sqlScript.getName() + " execution failed: " + updateException.getMessage()));
                    updateLog.addUpdateMessage(new UpdateMessage("info", "SQL script " + sqlScript.getName() + " is not update blocker, update continues"));
                    EventUtils.post(environment, "WARN", "SQL script " + sqlScript.getName() + " execution failed: " + updateException.getMessage());
                    EventUtils.post(environment, "UPDATE", "SQL script " + sqlScript.getName() + " is not update blocker, update continues");
                }
            }
        }
    }

    /**
     * Executes SQL script.
     *
     * @param environment the target <code>Environment</code>.
     * @param software    the target <code>Software</code>.
     * @param database    the target <code>Database</code>.
     * @param sqlScript   the target <code>SqlScript</code>.
     * @param updateLog   the update log to use.
     * @throws UpdateException in case of error during the SQL script execution.
     */
    public static void executeSqlScript(Environment environment, Software software, Database database, SqlScript sqlScript, UpdateLog updateLog) throws UpdateException {
        LOGGER.info("Executing SQL script {}", sqlScript.getName());
        updateLog.addUpdateMessage(new UpdateMessage("info", "Executing SQL script " + sqlScript.getName()));
        EventUtils.post(environment, "UPDATE", "Executing SQL script " + sqlScript.getName());

        if (!sqlScript.isActive()) {
            // the SQL script is not active
            LOGGER.info("SQL script {} is inactive, so not executed", sqlScript.getName());
            updateLog.addUpdateMessage(new UpdateMessage("info", "SQL script " + sqlScript.getName() + " is inactive, so not executed"));
            EventUtils.post(environment, "UPDATE", "SQL script " + sqlScript.getName() + " is inactive, so not executed");
            return;
        }

        // construct the SQL script URI
        String sqlScriptUri = VariableUtils.replace(sqlScript.getUri(), environment.getVariables());
        if (!FileManipulator.protocolExists(sqlScriptUri)) {
            // the SQL script URI is relative, constructs using the software URI
            LOGGER.debug("The SQL script URI is relative to the software URI");
            sqlScriptUri = FileManipulator.format(VariableUtils.replace(software.getUri(), environment.getVariables())) + "!/" + sqlScriptUri;
        }

        // get the cache directory
        LOGGER.debug("Getting software cache directory");
        String softwareCacheDir = null;
        try {
            softwareCacheDir = FileManipulator.createSoftwareCacheDir(environment, software);
        } catch (FileManipulatorException fileManipulatorException) {
            LOGGER.error("Can't initialize software cache directory", fileManipulatorException);
            throw new UpdateException("Can't initialize software cache directory", fileManipulatorException);
        }

        // get file manipulator instance
        LOGGER.debug("Initializing file manipulator");
        FileManipulator fileManipulator = null;
        try {
            fileManipulator = new FileManipulator();
        } catch (FileManipulatorException fileManipulatorException) {
            LOGGER.error("Can't initialize file manipulator", fileManipulatorException);
            throw new UpdateException("Can't initialize file manipulator", fileManipulatorException);
        }

        // copy the SQL script into the software cache directory
        LOGGER.debug("Copying the SQL script into the software cache directory");
        String sqlScriptCache = softwareCacheDir + "/sql/" + sqlScript.getName() + ".cache";
        String sqlScriptRuntime = softwareCacheDir + "/sql/" + sqlScript.getName();
        try {
            fileManipulator.copy(sqlScriptUri, sqlScriptCache);
        } catch (FileManipulatorException fileManipulatorException) {
            LOGGER.error("Can't copy SQL script from {} to {}", new Object[]{ sqlScriptUri, sqlScriptCache }, fileManipulatorException);
            throw new UpdateException("Can't copy SQL script " + sqlScriptUri + " to " + sqlScriptCache, fileManipulatorException);
        }

        if (fileManipulator.isFolder(sqlScriptCache)) {
            // TODO add a generic method to reuse in the case of directory

            // the user provided a directory
            updateLog.addUpdateMessage(new UpdateMessage("info", sqlScript.getName() + "is a folder, iterate in the SQL scripts"));
            EventUtils.post(environment, "UPDATE", sqlScript.getName() + " is a folder, iterate in the SQL scripts");
            LOGGER.info("{} is a folder, iterate in the SQL scripts", sqlScript.getName());
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
                            LOGGER.info("SQL script {} executed succesfully", name);
                        } else {
                            // execute SQL script using JDBC
                            String user = null;
                            String password = null;
                            String driver = null;
                            String url = null;
                            if (database.getConnectionPool() != null && database.getConnectionPool().trim().length() > 0) {
                                // the database is linked to a connection pool
                                // looking for the connection pool (from the cache)
                                LOGGER.debug("Database has a reference to a connection pool");
                                // looking for the connection pool definition
                                String connectionPoolName = VariableUtils.replace(database.getConnectionPool(), environment.getVariables());
                                JDBCConnectionPool connectionPool = null;
                                for (Iterator applicationServerIterator = environment.getJ2EEApplicationServers().getJ2EEApplicationServers().iterator(); applicationServerIterator.hasNext(); ) {
                                    J2EEApplicationServer applicationServer = (J2EEApplicationServer) applicationServerIterator.next();
                                    connectionPool = applicationServer.getJDBCConnectionPool(connectionPoolName);
                                    if (connectionPool != null) {
                                        break;
                                    }
                                }
                                if (connectionPool == null) {
                                    LOGGER.error("JDBC connection pool {} is not found in any J2EE application servers", connectionPoolName);
                                    throw new UpdateException("JDBC connection pool " + connectionPoolName + " is not found in any J2EE application servers");
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
                        LOGGER.info("SQL script " + sqlScript.getName() + " executed");
                    }
                } catch (Exception e) {
                    // SQL script execution failed, delete the SQL script from the cache
                    try {
                        fileManipulator.delete(sqlScriptRuntime);
                    } catch (FileManipulatorException fileManipulatorException) {
                        LOGGER.warn("Can't delete SQL script cache", fileManipulatorException);
                    }
                    LOGGER.error("SQL script {} execution failed", sqlScript.getName(), e);
                    throw new UpdateException("SQL script " + sqlScript.getName() + " execution failed", e);
                }
            }
        } else {
            // the user provided a single SQL script

            // change mappings into the SQL script
            LOGGER.debug("Replacing mappings into the SQL script {}", sqlScript.getName());
            for (Iterator mappingIterator = sqlScript.getMappings().iterator(); mappingIterator.hasNext(); ) {
                Mapping mapping = (Mapping) mappingIterator.next();
                FileManipulator.searchAndReplace(mapping.getKey(), mapping.getValue(), sqlScriptCache);
            }

            // compare the SQL script origin with the runtime one
            try {
                if (!fileManipulator.contentEquals(sqlScriptCache, sqlScriptRuntime)) {
                    // the SQL script needs to be executed
                    LOGGER.debug("The SQL script {} needs to be executed", sqlScript.getName());
                    // copy the SQL script cache to the runtime
                    LOGGER.debug("Copy the SQL script cache to the runtime");
                    fileManipulator.copy(sqlScriptCache, sqlScriptRuntime);
                    if (database.getSqlCommand() != null && database.getSqlCommand().trim().length() > 0) {
                        // execute the SQL script using a command
                        LOGGER.info("Executing the SQL script using a system command");
                        String command = VariableUtils.replace(database.getSqlCommand(), environment.getVariables());
                        String output = SqlScriptUtils.executeUsingCommand(sqlScriptRuntime, command);
                        updateLog.addUpdateMessage(new UpdateMessage("info", "SQL script " + sqlScript.getName() + " executed: " + output));
                        EventUtils.post(environment, "UPDATE", "SQL script " + sqlScript.getName() + " executed: " + output);
                        LOGGER.info("SQL script {} executed successfully", sqlScript.getName());
                    } else {
                        // execute SQL script using JDBC
                        LOGGER.info("Executing SQL script using JDBC");
                        String user = null;
                        String password = null;
                        String driver = null;
                        String url = null;
                        if (database.getConnectionPool() != null && database.getConnectionPool().trim().length() > 0) {
                            // the database has a reference to an existing connection pool
                            LOGGER.debug("Database has a reference to a connection pool");
                            // looking for the connection pool definition
                            String connectionPoolName = VariableUtils.replace(database.getConnectionPool(), environment.getVariables());
                            JDBCConnectionPool connectionPool = null;
                            for (Iterator applicationServerIterator = environment.getJ2EEApplicationServers().getJ2EEApplicationServers().iterator(); applicationServerIterator.hasNext(); ) {
                                J2EEApplicationServer applicationServer = (J2EEApplicationServer) applicationServerIterator.next();
                                connectionPool = applicationServer.getJDBCConnectionPool(connectionPoolName);
                                if (connectionPool != null) {
                                    break;
                                }
                            }
                            if (connectionPool == null) {
                                LOGGER.error("JDBC connection pool {} is not found in any J2EE application servers", connectionPoolName);
                                throw new UpdateException("JDBC connection pool " + connectionPoolName + " is not found in any J2EE application servers");
                            }
                            user = VariableUtils.replace(connectionPool.getUser(), environment.getVariables());
                            password = VariableUtils.replace(connectionPool.getPassword(), environment.getVariables());
                            driver = VariableUtils.replace(connectionPool.getDriver(), environment.getVariables());
                            url = VariableUtils.replace(connectionPool.getUrl(), environment.getVariables());
                        } else {
                            // use the database data
                            LOGGER.debug("Use database data definition");
                            user = VariableUtils.replace(database.getUser(), environment.getVariables());
                            password = VariableUtils.replace(database.getPassword(), environment.getVariables());
                            driver = VariableUtils.replace(database.getDriver(), environment.getVariables());
                            url = VariableUtils.replace(database.getJdbcurl(), environment.getVariables());
                        }
                        // execute SQL script
                        SqlScriptUtils.executeUsingJdbc(sqlScriptRuntime, driver, user, password, url);
                    }
                    // add log messages
                    updateLog.setStatus("Update performed");
                    updateLog.setUpdated(true);
                    updateLog.addUpdateMessage(new UpdateMessage("info", "SQL script " + sqlScript.getName() + " executed"));
                    EventUtils.post(environment, "UPDATE", "SQL script " + sqlScript.getName() + " executed");
                    LOGGER.info("SQL script {} executed sucessfully", sqlScript.getName());
                }
            } catch (Exception e) {
                // SQL script execution failed, delete SQL script from the cache
                try {
                    fileManipulator.delete(sqlScriptRuntime);
                } catch (FileManipulatorException fileManipulatorException) {
                    LOGGER.warn("Can't delete SQL script cache", fileManipulatorException);
                }
                LOGGER.error("SQL script {} execution failed", sqlScript.getName(), e);
                throw new UpdateException("SQL script " + sqlScript.getName() + " execution failed", e);
            }
        }
    }

}