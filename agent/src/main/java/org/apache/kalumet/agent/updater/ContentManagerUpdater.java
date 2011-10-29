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
import org.apache.kalumet.model.*;
import org.apache.kalumet.model.update.UpdateLog;
import org.apache.kalumet.model.update.UpdateMessage;
import org.apache.kalumet.utils.NotifierUtils;
import org.apache.kalumet.utils.PublisherUtils;
import org.apache.kalumet.utils.VariableUtils;
import org.apache.kalumet.ws.client.ClientException;
import org.apache.kalumet.ws.client.ContentManagerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Iterator;

/**
 * Content manager updater.
 */
public class ContentManagerUpdater {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(ContentManagerUpdater.class);

    /**
     * Update a content manager.
     *
     * @param environment    the target <code>Environment</code>.
     * @param server         the target <code>J2EEApplicationServer</code>.
     * @param application    the target <code>J2EEApplication</code>.
     * @param contentManager the target <code>ContentManager</code>.
     * @param updateLog      the <code>UpdateLog</code> to use.
     * @throws UpdateException in case of update failure.
     */
    public static void update(Environment environment, J2EEApplicationServer server, J2EEApplication application, ContentManager contentManager, UpdateLog updateLog) throws UpdateException {
        LOGGER.info("Updating content manager {}", contentManager.getName());
        updateLog.addUpdateMessage(new UpdateMessage("info", "Updating content manager " + contentManager.getName()));
        EventUtils.post(environment, "UPDATE", "Updating content manager " + contentManager.getName());

        if (!contentManager.isActive()) {
            // the content manager is not active
            LOGGER.info("Content manager {} is inactive, so not updated", contentManager.getName());
            updateLog.addUpdateMessage(new UpdateMessage("info", "Content manager " + contentManager.getName() + " is inactive, so not updated"));
            EventUtils.post(environment, "UPDATE", "Content manager " + contentManager.getName() + " is inactive, so not updated");
            return;
        }

        if (contentManager.getAgent() != null && contentManager.getAgent().trim().length() > 0 && !contentManager.getAgent().equals(Configuration.AGENT_ID)) {
            // the content manager update is delegated to another agent
            LOGGER.info("Delegating content manager {} update to agent {}", contentManager.getName(), contentManager.getAgent());
            updateLog.addUpdateMessage(new UpdateMessage("info", "Delegating content manager " + contentManager.getName() + " update to agent " + contentManager.getAgent()));
            EventUtils.post(environment, "UPDATE", "Delegating content manager " + contentManager.getName() + " update to agent " + contentManager.getAgent());
            Agent delegationAgent = Configuration.CONFIG_CACHE.getAgent(contentManager.getAgent());
            if (delegationAgent == null) {
                // the content manager agent is not found in the configuration
                LOGGER.error("Agent {} is not found in the configuration", contentManager.getAgent());
                throw new UpdateException("Agent " + contentManager.getAgent() + " is not found in the configuration");
            }
            try {
                // call agent WebService
                LOGGER.debug("Calling content manager WebService WS");
                ContentManagerClient client = new ContentManagerClient(delegationAgent.getHostname(), delegationAgent.getPort());
                client.update(environment.getName(), server.getName(), application.getName(), contentManager.getName(), true);
            } catch (ClientException clientException) {
                LOGGER.error("Content manager {} update failed", contentManager.getName(), clientException);
                throw new UpdateException("Content manager " + contentManager.getName() + " update failed", clientException);
            }
            return;
        }

        try {
            LOGGER.debug("Loading content manager class name");
            Class contentManagerClass = Class.forName(VariableUtils.replace(contentManager.getClassname(), environment.getVariables()));
            Object contentManagerObject = contentManagerClass.newInstance();
            // call method properties
            for (Iterator propertyIterator = contentManager.getProperties().iterator(); propertyIterator.hasNext(); ) {
                Property property = (Property) propertyIterator.next();
                Method method = contentManagerClass.getMethod("set" + VariableUtils.replace(property.getName(), environment.getVariables()), new Class[]{String.class});
                method.invoke(contentManagerObject, new Object[]{VariableUtils.replace(property.getValue(), environment.getVariables())});
            }
            // call main method
            Method mainMethod = contentManagerClass.getMethod("main", new Class[]{});
            mainMethod.invoke(contentManagerObject, new Object[]{});
            LOGGER.info("Content manager {} updated", contentManager.getName());
            updateLog.setStatus("update performed");
            updateLog.setUpdated(true);
            updateLog.addUpdateMessage(new UpdateMessage("info", "Content manager " + contentManager.getName() + " updated"));
            EventUtils.post(environment, "UPDATE", "Content manager " + contentManager.getName() + " updated");
        } catch (Exception e) {
            LOGGER.error("Content manager {} update failed", contentManager.getName(), e);
            throw new UpdateException("Content manager " + contentManager.getName() + " update failed", e);
        }
    }

    /**
     * Wrapper method to update a content manager via WS.
     *
     * @param environmentName    the target environment name.
     * @param serverName         the target J2EE application server name.
     * @param applicationName    the target J2E application name.
     * @param contentManagerName the target content manager name.
     * @param delegation         flag indicating if the update is called by another agent (true) or by a client (false).
     * @throws KalumetException in case of update failure.
     */
    public static void update(String environmentName, String serverName, String applicationName, String contentManagerName, boolean delegation) throws KalumetException {
        LOGGER.info("Content manager {} update requested by WS", contentManagerName);

        // load configuration.
        LOGGER.debug("Loading configuration");
        Kalumet kalumet = Kalumet.digeste(Configuration.CONFIG_LOCATION);

        // looking for component objects.
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
        ContentManager contentManager = application.getContentManager(contentManagerName);
        if (contentManager == null) {
            LOGGER.error("Content manager {} is not found in J2EE application {}", contentManagerName, application.getName());
            throw new KalumetException("Content manager " + contentManagerName + " is not found in J2EE application " + application.getName());
        }

        // update configuration cache.
        LOGGER.debug("Updating configuration cache");
        Configuration.CONFIG_CACHE = kalumet;

        // post event and create an update log
        EventUtils.post(environment, "UPDATE", "Content manager " + contentManager.getName() + " update requested by WS");
        UpdateLog updateLog = new UpdateLog("Content manager " + contentManager.getName() + " update in progress ...", environment.getName(), environment);

        if (!delegation) {
            // the update is a client call,
            // send a notification and waiting for the count down
            LOGGER.info("Send a notification and waiting for the count down");
            EventUtils.post(environment, "UPDATE", "Send a notification and waiting for the count down");
            NotifierUtils.waitAndNotify(environment);
        }

        try {
            // call the content manager updater
            LOGGER.debug("Call content manager updater");
            ContentManagerUpdater.update(environment, applicationServer, application, contentManager, updateLog);
        } catch (Exception e) {
            LOGGER.error("Content manager " + contentManager.getName() + " update failed", e);
            EventUtils.post(environment, "ERROR", "Content manager " + contentManager.getName() + " update failed: " + e.getMessage());
            if (!delegation) {
                updateLog.setStatus("Content manager " + contentManager.getName() + " update failed");
                updateLog.addUpdateMessage(new UpdateMessage("error", "Content manager " + contentManager.getName() + " update failed: " + e.getMessage()));
                PublisherUtils.publish(environment);
            }
            throw new UpdateException("Content manager " + contentManager.getName() + " update failed", e);
        }

        // update completed
        LOGGER.info("Content manager {} updated", contentManager.getName());
        EventUtils.post(environment, "UPDATE", "Content manager " + contentManager.getName() + " updated");
        if (!delegation) {
            if (updateLog.isUpdated()) {
                updateLog.setStatus("Content manager " + contentManager.getName() + " updated");
            } else {
                updateLog.setStatus("Content manager " + contentManager.getName() + " already up to date");
            }
            updateLog.addUpdateMessage(new UpdateMessage("info", "Content manager " + contentManager.getName() + " updated"));
            LOGGER.info("Publishing update report");
            PublisherUtils.publish(environment);
        }

    }

}
