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
import org.apache.kalumet.controller.core.J2EEApplicationServerController;
import org.apache.kalumet.controller.core.J2EEApplicationServerControllerFactory;
import org.apache.kalumet.model.*;
import org.apache.kalumet.model.update.UpdateLog;
import org.apache.kalumet.model.update.UpdateMessage;
import org.apache.kalumet.utils.NotifierUtils;
import org.apache.kalumet.utils.PublisherUtils;
import org.apache.kalumet.utils.VariableUtils;
import org.apache.kalumet.ws.client.ArchiveClient;
import org.apache.kalumet.ws.client.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * J2EE application archive updater.
 */
public class ArchiveUpdater {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(ArchiveUpdater.class);

    /**
     * Wrapper method to update J2EE application archive.
     *
     * @param environmentName the target environment name.
     * @param serverName      the target J2EE application server name.
     * @param applicationName the target J2EE application name.
     * @param archiveName     the target archive name.
     * @param delegation      flag indicating if the update is a delegation from another agent (true), or a client call (false).
     * @throws UpdateException in case of update failure.
     */
    public static void update(String environmentName, String serverName, String applicationName, String archiveName, boolean delegation) throws KalumetException {
        LOGGER.info("Archive {} update requested by WS", archiveName);

        // load configuration.
        LOGGER.debug("Loading configuration");
        Kalumet kalumet = Kalumet.digeste(Configuration.CONFIG_LOCATION);
        Environment environment = kalumet.getEnvironment(environmentName);
        if (environment == null) {
            LOGGER.error("Environment {} is not found in the configuration", environmentName);
            throw new KalumetException("Environment " + environmentName + " is not found in the configuration");
        }
        J2EEApplicationServer server = environment.getJ2EEApplicationServers().getJ2EEApplicationServer(serverName);
        if (server == null) {
            LOGGER.error("J2EE application server {} is not found in environment {}", serverName, environmentName);
            throw new KalumetException("J2EE application server " + serverName + " is not found in environment " + environmentName);
        }
        J2EEApplication application = server.getJ2EEApplication(applicationName);
        if (application == null) {
            LOGGER.error("J2EE application {} is not found in J2EE application server {}", applicationName, serverName);
            throw new KalumetException("J2EE application " + applicationName + " is not found in J2EE application server " + serverName);
        }
        Archive archive = application.getArchive(archiveName);
        if (archive == null) {
            LOGGER.error("Archive {} is not found in J2EE application {}", archiveName, applicationName);
            throw new KalumetException("Archive " + archiveName + " is not found in J2EE application " + applicationName);
        }

        // update configuration cache
        LOGGER.debug("Updating configuration cache");
        Configuration.CONFIG_CACHE = kalumet;

        // post journal event
        EventUtils.post(environment, "UPDATE", "Archive " + archiveName + " update requested by WS");
        // create an update logger
        UpdateLog updateLog = new UpdateLog("Archive " + archiveName + " update in progress ...", environment.getName(), environment);

        if (!delegation) {
            // the update is a client call
            LOGGER.info("Send a notification and waiting for the count down");
            EventUtils.post(environment, "UPDATE", "Send a notification and waiting for the count down");
            NotifierUtils.waitAndNotify(environment);
        }

        try {
            // call the updater method
            ArchiveUpdater.update(environment, server, application, archive, updateLog);
        } catch (Exception e) {
            LOGGER.error("Archive {} update failed", archiveName, e);
            EventUtils.post(environment, "ERROR", "Archive " + archiveName + " update failed: " + e.getMessage());
            if (!delegation) {
                updateLog.setStatus("Archive " + archiveName + " update failed");
                updateLog.addUpdateMessage(new UpdateMessage("error", "Archive " + archiveName + " update failed: " + e.getMessage()));
                LOGGER.info("Publishing update report");
                PublisherUtils.publish(environment);
            }
            throw new UpdateException("Archive " + archiveName + " update failed", e);
        }

        // update completed
        LOGGER.info("Archive {} updated", archive.getName());
        EventUtils.post(environment, "UPDATE", "Archive " + archive.getName() + " updated");

        if (!delegation) {
            if (updateLog.isUpdated()) {
                updateLog.setStatus("Archive " + archive.getName() + " updated");
            } else {
                updateLog.setStatus("Archive " + archive.getName() + " already up to date");
            }
            updateLog.addUpdateMessage(new UpdateMessage("info", "Archive " + archive.getName() + " updated"));
            LOGGER.info("Publishing update report");
            PublisherUtils.publish(environment);
        }
    }

    /**
     * Updates a archive.
     *
     * @param environment the target <code>Environment</code>.
     * @param server      the target <code>J2EEApplicationServer</code>.
     * @param application the target JZEE <code>Application</code>.
     * @param archive     the target <code>Archive</code>.
     * @param updateLog   the <code>UpdateLog</code> to use.
     */
    public static void update(Environment environment, J2EEApplicationServer server, J2EEApplication application, Archive archive, UpdateLog updateLog) throws UpdateException {
        LOGGER.info("Updating archive {}", archive.getName());
        updateLog.addUpdateMessage(new UpdateMessage("info", "Updating archive " + archive.getName()));
        EventUtils.post(environment, "UPDATE", "Updating archive " + archive.getName());

        if (!archive.isActive()) {
            LOGGER.info("Archive {} is inactive, so not updated", archive.getName());
            updateLog.addUpdateMessage(new UpdateMessage("info", "Archive " + archive.getName() + " is inactive, so not updated"));
            EventUtils.post(environment, "UPDATE", "Archive " + archive.getName() + " is inactive, so not updated");
            return;
        }

        // check to delegate the update
        if (archive.getAgent() != null && archive.getAgent().trim().length() > 0 && !archive.getAgent().equals(Configuration.AGENT_ID)) {
            LOGGER.info("Delegating archive {} update to agent {}", archive.getName(), archive.getAgent());
            EventUtils.post(environment, "UPDATE", "Delegating archive " + archive.getName() + " update to agent " + archive.getAgent());
            updateLog.addUpdateMessage(new UpdateMessage("info", "Delegating archive " + archive.getName() + " update to agent " + archive.getAgent()));
            Agent delegationAgent = Configuration.CONFIG_CACHE.getAgent(archive.getAgent());
            if (delegationAgent == null) {
                LOGGER.error("Agent {} is not found in the configuration", archive.getAgent());
                throw new UpdateException("Agent " + archive.getAgent() + " is not found in the configuration");
            }
            try {
                LOGGER.debug("Call archive WS");
                ArchiveClient client = new ArchiveClient(delegationAgent.getHostname(), delegationAgent.getPort());
                client.update(environment.getName(), server.getName(), application.getName(), archive.getName(), true);
            } catch (ClientException clientException) {
                LOGGER.error("Archive " + archive.getName() + " update failed", clientException);
                throw new UpdateException("Archive " + archive.getName() + " update failed", clientException);
            }
            return;
        }

        // construct the archiveUri
        String archiveUri = VariableUtils.replace(archive.getUri(), environment.getVariables());
        if (!FileManipulator.protocolExists(archiveUri)) {
            // the archive URI is relative (no prefix protocol), use the
            // application URI to construct the VFS URI
            LOGGER.debug("Archive URI is relative (no protocol prefix) to J2EE application URI");
            archiveUri = FileManipulator.format(VariableUtils.replace(application.getUri(), environment.getVariables())) + "!/" + archiveUri;
        }
        // get the application cache directory
        String applicationCacheDir = null;
        // initialize the file manipulator instance
        FileManipulator fileManipulator = null;
        try {
            applicationCacheDir = FileManipulator.createJ2EEApplicationCacheDir(environment, application);
            fileManipulator = new FileManipulator();
        } catch (FileManipulatorException e) {
            LOGGER.error("Can't create J2EE application {} cache directory", application.getName(), e);
            throw new UpdateException("Can't create J2EE application " + application.getName() + " cache directory", e);
        } finally {
            if (fileManipulator != null) {
                fileManipulator.close();
            }
        }
        // define the archive cache location
        String archiveCache = applicationCacheDir + "/" + archive.getName();
        // define the archive installation URI
        String archiveInstallation = null;
        if (archive.getPath() == null || archive.getPath().trim().length() < 1) {
            LOGGER.error("Archive {} path is not defined", archive.getName());
            throw new UpdateException("Archive " + archive.getName() + " path is not defined");
        }
        // the archive path is defined, use it
        archiveInstallation = VariableUtils.replace(archive.getPath(), environment.getVariables());
        // get the J2EE application server controller
        LOGGER.debug("Getting the J2EE application server controller");
        J2EEApplicationServerController controller = null;
        try {
            controller = J2EEApplicationServerControllerFactory.getController(environment, server);
        } catch (KalumetException e) {
            LOGGER.error("Can't get the J2EE application server {} controller", server.getName(), e);
            throw new UpdateException("Can't get the J2EE application server " + server.getName() + " controller", e);
        }
        // check if the archive is already deployed
        try {
            if (controller.isJ2EEApplicationDeployed(archiveInstallation, archive.getName())) {
                // the archive is already deployed, check for update
                LOGGER.info("Archive {} is already deployed, check for update", archive.getName());
                updateLog.addUpdateMessage(new UpdateMessage("info", "Archive " + archive.getName() + " is already deployed, check for update"));
                EventUtils.post(environment, "UPDATE", "Archive " + archive.getName() + " is already deployed, check for update");
                if (!fileManipulator.checksumEquals(archiveUri, archiveCache)) {
                    // the archive file is different from the copy in the
                    // application directory, perform an update
                    // update the cache
                    fileManipulator.copy(archiveUri, archiveCache);
                    LOGGER.info("Archive {} (located {}) is different from the cache, performing update", archive.getName(), archiveUri);
                    updateLog.addUpdateMessage(new UpdateMessage("info", "Archive " + archive.getName() + " (located " + archiveUri + ") is different from the cache, performing update"));
                    EventUtils.post(environment, "UPDATE", "Archive " + archive.getName() + " (located " + archiveUri + ") is different from the cache, performing update");
                    updateLog.addUpdateMessage(new UpdateMessage("info", "Copy " + archiveUri + " to " + archiveInstallation));
                    EventUtils.post(environment, "UPDATE", "Copy " + archiveUri + " to " + archiveInstallation);
                    // update the archive path
                    fileManipulator.copy(archiveUri, archiveInstallation);
                    // undeploy the archive
                    LOGGER.info("Undeploying archive {}", archive.getName());
                    updateLog.addUpdateMessage(new UpdateMessage("info", "Undeploying archive " + archive.getName()));
                    EventUtils.post(environment, "UPDATE", "Undeploying archive " + archive.getName());
                    controller.undeployJ2EEApplication(archiveInstallation, archive.getName());
                    // deploy the archive
                    LOGGER.info("Deploying archive {}", archive.getName());
                    updateLog.addUpdateMessage(new UpdateMessage("info", "Deploying archive " + archive.getName()));
                    EventUtils.post(environment, "UPDATE", "Deploying archive " + archive.getName());
                    controller.deployJ2EEApplication(archiveInstallation, archive.getName(), archive.getClassloaderorder(), archive.getClassloaderpolicy(), VariableUtils.replace(archive.getVhost(), environment.getVariables()));
                    LOGGER.info("Archive {} updated", archive.getName());
                    updateLog.addUpdateMessage(new UpdateMessage("info", "Archive " + archive.getName() + " updated"));
                    EventUtils.post(environment, "UPDATE", "Archive " + archive.getName() + " updated");
                    updateLog.setUpdated(true);
                }
            } else {
                // the archive is not deployed
                LOGGER.info("Archive {} is not deployed, deploying it", archive.getName());
                updateLog.addUpdateMessage(new UpdateMessage("info", "Archive " + archive.getName() + " is not deployed, deploying it"));
                EventUtils.post(environment, "UPDATE", "Archive " + archive.getName() + " is not deployed, deploying it");
                // copy the archive agent locally
                fileManipulator.copy(archiveUri, archiveCache);
                // copy the archive to the destination path
                fileManipulator.copy(archiveUri, archiveInstallation);
                // deploy the archive
                LOGGER.info("Deploying archive {}", archive.getName());
                updateLog.addUpdateMessage(new UpdateMessage("info", "Deploying archive " + archive.getName()));
                EventUtils.post(environment, "UPDATE", "Deploying archive " + archive.getName());
                controller.deployJ2EEApplication(archiveInstallation, archive.getName(), archive.getClassloaderorder(), archive.getClassloaderpolicy(), VariableUtils.replace(archive.getVhost(), environment.getVariables()));
                updateLog.setUpdated(true);
            }
            // as some J2EE application server (like IBM WebSphere) change the archive file during deployment, update
            // the local archive with a original copy (for next update)
            LOGGER.debug("Restoring the original archive (before deployment) from {}", archiveUri);
            fileManipulator.copy(archiveUri, archiveCache);
            // check if the J2EE application is deployed (it should be)
            if (!controller.isJ2EEApplicationDeployed(archiveInstallation, archive.getName())) {
                LOGGER.error("Archive {} is not deployed whereas it should be. Please check the J2EE application server logs", archive.getName());
                throw new UpdateException("Archive " + archive.getName() + " is not deployed whereas it should be. Please check the J2EE application server logs");
            }
        } catch (Exception e) {
            // the archive update has failed
            LOGGER.error("Archive {} update failed", archive.getName(), e);
            try {
                fileManipulator.delete(archiveCache);
            } catch (FileManipulatorException fileManipulatorException) {
                LOGGER.warn("Can't delete " + archiveCache, fileManipulatorException);
            }
            throw new UpdateException("Archive " + archive.getName() + " update failed", e);
        }
    }

    /**
     * Wrapper method to check if a archive is up to date or not via WS.
     *
     * @param environmentName the target environment name.
     * @param serverName      the target J2EE application server name.
     * @param applicationName the target J2EE application name.
     * @param archiveName     the target archive name.
     * @return true if the archive is up to date, false else.
     * @throws KalumetException in case of status check failure
     */
    public static boolean check(String environmentName, String serverName, String applicationName, String archiveName) throws KalumetException {
        LOGGER.info("Checking status of archive {} via WS", archiveName);
        // load configuration
        LOGGER.debug("Load configuration");
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
        Archive archive = application.getArchive(archiveName);
        if (archive == null) {
            LOGGER.error("Archive {} is not found in J2EE application {}", archiveName, applicationName);
            throw new KalumetException("Archive " + archiveName + " is not found in J2EE application " + applicationName);
        }

        // check if the check should be delegated to another agent
        if (archive.getAgent() != null && archive.getAgent().trim().length() > 0 && !archive.getAgent().equals(Configuration.AGENT_ID)) {
            // the check needs to be delegated to another agent
            LOGGER.info("Delegating archive {} status check to agent {}", archive.getName(), archive.getAgent());
            Agent agentDelegation = kalumet.getAgent(archive.getAgent());
            if (agentDelegation == null) {
                LOGGER.error("Agent {} is not found in the configuration", archive.getName());
                throw new KalumetException("Agent " + archive.getAgent() + " is not found in the configuration");
            }
            // call the service
            ArchiveClient client = new ArchiveClient(agentDelegation.getHostname(), agentDelegation.getPort());
            return client.check(environmentName, serverName, applicationName, archiveName);
        }


        // get J2EE application server controller
        LOGGER.debug("Getting J2EE application server controller");
        J2EEApplicationServerController controller = J2EEApplicationServerControllerFactory.getController(environment, applicationServer);


        FileManipulator fileManipulator = null;
        try {
            fileManipulator = new FileManipulator();

            // get application cache directory
            LOGGER.debug("Getting application cache directory");
            String applicationCacheDirectory = FileManipulator.createJ2EEApplicationCacheDir(environment, application);

            // construct the archive URI
            String archiveUri = VariableUtils.replace(archive.getUri(), environment.getVariables());
            if (!FileManipulator.protocolExists(archiveUri)) {
                // the archive URI is relative (doesn't contain the protocol prefix), construct the URI using the application URI
                archiveUri = VariableUtils.replace(application.getUri(), environment.getVariables()) + "!/" + archiveUri;
            }

            // get the archive cache
            String archiveCache = applicationCacheDirectory + "/" + archive.getName();

            // get the archive installation path
            if (archive.getPath() == null || archive.getPath().trim().length() < 1) {
                LOGGER.error("Archive {} path is not defined", archive.getName());
                throw new KalumetException("Archive " + archive.getName() + " path is not defined");
            }
            String archiveInstallation = VariableUtils.replace(archive.getPath(), environment.getVariables());

            if (controller.isJ2EEApplicationDeployed(archiveInstallation, archive.getName())) {
                // check if the archive is deployed or not
                if (fileManipulator.checksumEquals(archiveUri, archiveCache)) {
                    // archive URI and cache are the same
                    LOGGER.debug("Archive URI and agent cache are the same");
                    return true;
                }
            }
        } finally {
            if (fileManipulator != null) {
                fileManipulator.close();
            }
        }

        return false;
    }

}
