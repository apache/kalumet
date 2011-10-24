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
import org.apache.kalumet.agent.utils.NotifierUtils;
import org.apache.kalumet.model.Environment;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.model.update.UpdateLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.security.krb5.Config;

/**
 * Environment updater.
 */
public class EnvironmentUpdater {

    private final static transient Logger LOGGER = LoggerFactory.getLogger(EnvironmentUpdater.class);

    /**
     * Updates an environment identified by a given name.
     * The update is forced even if the auto update flag is set to false.
     *
     * @param name the environment name.
     * @throws KalumetException
     */
    public static void update(String name) throws KalumetException {
        // load the Kalumet configuration
        Kalumet kalumet = Kalumet.digeste(Configuration.CONFIG_LOCATION);
        // get the environment
        Environment environment = kalumet.getEnvironment(name);
        if (environment == null) {
            LOGGER.error("Environment {} is not found in the configuration", name);
            throw new KalumetException("Environment " + name + " is not found in the configuration");
        }
        try {
            EnvironmentUpdater.update(environment, true);
        } catch (Exception e) {
            throw new KalumetException(e);
        }
    }

    /**
     * Updates an environment.
     *
     * @param environment the environment to update.
     * @throws UpdateException in case of update failure.
     */
    public static void update(Environment environment) throws UpdateException {
        EnvironmentUpdater.update(environment, false);
    }

    /**
     * Updates an environment.
     *
     * @param environment the environment to update.
     * @param force true force the update (even if the autoupdate flag is false), false else
     * @throws UpdateException in case of update failure.
     */
    public static void update(Environment environment, boolean force) throws UpdateException {
        LOGGER.info("Updating environment {}", environment.getName());

        LOGGER.debug("Loading configuration and updating the cache");
        Kalumet kalumet = null;
        try {
            kalumet = Kalumet.digeste(Configuration.CONFIG_LOCATION);
            Configuration.CONFIG_CACHE = kalumet;
        } catch (Exception e) {
            LOGGER.error("Can't load configuration", e);
            EventUtils.post(environment, "ERROR", "Can't load configuration: " + e.getMessage());
            throw new UpdateException("Can't load configuration", e);
        }

        if (!force && !environment.isAutoupdate()) {
            LOGGER.info("Update is not forced and environment {} is not auto update", environment.getName());
            LOGGER.info("Update is not performed");
            return;
        }

        LOGGER.debug("Creating a update logger");
        UpdateLog updateLog = null;
        try {
            updateLog = new UpdateLog("Environment " + environment.getName() + " update in progress ...", environment.getName(), environment);
        } catch (Exception e) {
            LOGGER.error("Can't create the update logger", e);
            EventUtils.post(environment, "ERROR", "Can't create the update logger: " + e.getMessage());
            throw new UpdateException("Can't create the update logger", e);
        }

        // posting start update event
        EventUtils.post(environment, "UPDATE", "Starting to update ...");

        LOGGER.info("Sending a notification and waiting for the update count down");
        EventUtils.post(environment, "UPDATE", "Sending a notification and waiting for the update count donw");
        NotifierUtils.waitAndNotify(environment);

        // TODO complete
    }

}
