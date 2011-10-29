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

import org.apache.kalumet.KalumetException;
import org.apache.kalumet.agent.Configuration;
import org.apache.kalumet.agent.utils.EventUtils;
import org.apache.kalumet.controller.core.ControllerException;
import org.apache.kalumet.controller.core.J2EEApplicationServerController;
import org.apache.kalumet.controller.core.J2EEApplicationServerControllerFactory;
import org.apache.kalumet.model.Environment;
import org.apache.kalumet.model.J2EEApplicationServer;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.model.SharedLibrary;
import org.apache.kalumet.model.update.UpdateLog;
import org.apache.kalumet.model.update.UpdateMessage;
import org.apache.kalumet.utils.NotifierUtils;
import org.apache.kalumet.utils.PublisherUtils;
import org.apache.kalumet.utils.VariableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared library updater.
 */
public class SharedLibraryUpdater {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(SharedLibraryUpdater.class);

    /**
     * Updates a shared library.
     *
     * @param environment   the target <code>Environment</code>.
     * @param server        the target <code>J2EEApplicationServer</code>.
     * @param sharedLibrary the target <code>SharedLibrary</code>.
     * @param updateLog     the <code>UpdateLog</code> to use.
     * @throws UpdateException in case of update failure.
     */
    public static void update(Environment environment, J2EEApplicationServer server, SharedLibrary sharedLibrary, UpdateLog updateLog) throws UpdateException {
        LOGGER.info("Updating shared library {}", sharedLibrary.getName());

        updateLog.addUpdateMessage(new UpdateMessage("info", "Updating shared library " + sharedLibrary.getName()));
        EventUtils.post(environment, "UPDATE", "Updating shared library " + sharedLibrary.getName());
        if (!sharedLibrary.isActive()) {
            // the shared library is not active
            LOGGER.info("Shared library {} is inactive, so not updated", sharedLibrary.getName());
            updateLog.addUpdateMessage(new UpdateMessage("info", "Shared library " + sharedLibrary.getName() + " is inactive, so not updated"));
            EventUtils.post(environment, "UPDATE", "Shared library " + sharedLibrary.getName() + " is inactive, so not updated");
            return;
        }
        J2EEApplicationServerController controller = null;
        try {
            // connect JMX controller to J2EE application server
            LOGGER.debug("Connecting to J2EE application server {} controller", server.getName());
            controller = J2EEApplicationServerControllerFactory.getController(environment, server);
        } catch (KalumetException e) {
            LOGGER.error("Can't connect to J2EE application server {} controller", server.getName(), e);
            throw new UpdateException("Can't connect to J2EE application server " + server.getName() + " controller", e);
        }
        // replaces variables in shared library class path.
        LOGGER.debug("Replacing variables into the shared library classpath");
        String mapClasspath = VariableUtils.replace(sharedLibrary.getClasspath(), environment.getVariables());
        try {
            if (controller.isSharedLibraryDeployed(sharedLibrary.getName())) {
                // the shared library is already deployed, check for update
                LOGGER.info("Shared library {} already deployed, checking for update", sharedLibrary.getName());
                if (controller.updateSharedLibrary(sharedLibrary.getName(), mapClasspath)) {
                    // the shared library has been updated
                    updateLog.setStatus("Update performed");
                    updateLog.setUpdated(true);
                    updateLog.addUpdateMessage(new UpdateMessage("info", "Shared library " + sharedLibrary.getName() + " updated"));
                    EventUtils.post(environment, "UPDATE", "Shared library " + sharedLibrary.getName() + " updated");
                    LOGGER.info("Shared library " + sharedLibrary.getName() + " udpated");
                }
            } else {
                // the shared library is not deployed, deploy it
                controller.deploySharedLibrary(sharedLibrary.getName(), mapClasspath);
                updateLog.setStatus("Update performed");
                updateLog.setUpdated(true);
                updateLog.addUpdateMessage(new UpdateMessage("info", "Shared library " + sharedLibrary.getName() + " deployed"));
                EventUtils.post(environment, "UPDATE", "Shared library " + sharedLibrary.getName() + " deployed");
                LOGGER.info("Shared library " + sharedLibrary.getName() + " deployed");
            }
        } catch (ControllerException exception) {
            LOGGER.error("Shared library {} update failed", sharedLibrary.getName(), exception);
            throw new UpdateException("Shared library " + sharedLibrary.getName() + " update failed", exception);
        }
    }

    /**
     * Wrapper method to update shared library via WS.
     *
     * @param environmentName       the target environment name.
     * @param applicationServerName the target J2EE application server name.
     * @param sharedLibraryName     the target shared library name.
     * @throws KalumetException in case of update failure.
     */
    public static void update(String environmentName, String applicationServerName, String sharedLibraryName) throws KalumetException {
        LOGGER.info("Shared library {} update requested by WS", sharedLibraryName);

        // load configuration.
        LOGGER.debug("Loading configuration");
        Kalumet kalumet = Kalumet.digeste(Configuration.CONFIG_LOCATION);

        // looking for component objects.
        LOGGER.debug("Looking for component objects");
        Environment environment = kalumet.getEnvironment(environmentName);
        if (environment == null) {
            LOGGER.error("Environment {} is not found in the configuration", environmentName);
            throw new KalumetException("Environment " + environmentName + " is not found in the configuration");
        }
        J2EEApplicationServer applicationServer = environment.getJ2EEApplicationServers().getJ2EEApplicationServer(applicationServerName);
        if (applicationServer == null) {
            LOGGER.error("J2EE application server {} is not found in environment {}", applicationServerName, environment.getName());
            throw new KalumetException("J2EE application server " + applicationServerName + " is not found in environment " + environment.getName());
        }
        SharedLibrary sharedLibrary = applicationServer.getSharedLibrary(sharedLibraryName);
        if (sharedLibrary == null) {
            LOGGER.error("Shared library {} is not found in J2EE application server {}", sharedLibraryName, applicationServer.getName());
            throw new KalumetException("Shared library " + sharedLibraryName + " is not found in J2EE application server " + applicationServer.getName());
        }

        // post an event and create update log
        LOGGER.debug("Posting an event and creating update log");
        EventUtils.post(environment, "UPDATE", "Shared library " + sharedLibrary.getName() + " update requested by WS");
        UpdateLog updateLog = new UpdateLog("Shared library " + sharedLibrary.getName() + " update in progress ...", environment.getName(), environment);

        // send a notification and waiting for the count down
        LOGGER.info("Send a notification and waiting for the count down");
        NotifierUtils.waitAndNotify(environment);

        try {
            // call updater
            LOGGER.debug("Call shared library updater");
            SharedLibraryUpdater.update(environment, applicationServer, sharedLibrary, updateLog);
        } catch (Exception e) {
            LOGGER.error("Shared library {} update failed", sharedLibrary.getName(), e);
            EventUtils.post(environment, "ERROR", "Shared library " + sharedLibrary.getName() + " update failed: " + e.getMessage());
            updateLog.setStatus("Shared library " + sharedLibrary.getName() + " update failed");
            updateLog.addUpdateMessage(new UpdateMessage("error", "Shared library " + sharedLibrary.getName() + " update failed: " + e.getMessage()));
            PublisherUtils.publish(environment);
            throw new UpdateException("Shared library " + sharedLibrary.getName() + " update failed", e);
        }

        // update completed.
        LOGGER.info("Shared library {} updated", sharedLibrary.getName());
        if (updateLog.isUpdated()) {
            updateLog.setStatus("Shared library " + sharedLibrary.getName() + " updated");
        } else {
            updateLog.setStatus("Shared library " + sharedLibrary.getName() + " already up to date");
        }
        updateLog.addUpdateMessage(new UpdateMessage("info", "Shared library " + sharedLibrary.getName() + " updated"));
        LOGGER.info("Publishing update report");
        PublisherUtils.publish(environment);
    }

    /**
     * Check if a shared library is up to date or not via WS.
     *
     * @param environmentName   the target environment name.
     * @param serverName        the target J2EE application server name.
     * @param sharedLibraryName the target shared library name.
     * @return true if the shared library is up to date, false else.
     * @throws KalumetException in case of check failure.
     */
    public static boolean check(String environmentName, String serverName, String sharedLibraryName) throws KalumetException {
        LOGGER.info("Shared library {} status check requested by WS", sharedLibraryName);

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
        SharedLibrary sharedLibrary = applicationServer.getSharedLibrary(sharedLibraryName);
        if (sharedLibrary == null) {
            LOGGER.error("Shared library {} is not found in J2EE application server {}", sharedLibraryName, applicationServer.getName());
            throw new KalumetException("Shared library " + sharedLibraryName + " is not found in J2EE application server " + applicationServer.getName());
        }

        try {
            // get J2EE application server controller
            LOGGER.debug("Getting J2EE application server controller");
            J2EEApplicationServerController controller = J2EEApplicationServerControllerFactory.getController(environment, applicationServer);
            // replaces variables in shared library class path.
            LOGGER.debug("Replacing variables into the shared library classpath");
            String classpath = VariableUtils.replace(sharedLibrary.getClasspath(), environment.getVariables());
            // check shared library using controller.
            LOGGER.debug("Checking status of the shared library using controller");
            return controller.isSharedLibraryUpToDate(sharedLibrary.getName(), classpath);
        } catch (Exception e) {
            LOGGER.error("Shared library {} status check failed", sharedLibrary.getName(), e);
            throw new KalumetException("Shared library " + sharedLibrary.getName() + " status check failed", e);
        }
    }

}
