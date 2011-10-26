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
package org.apache.kalumet.agent.utils;

import org.apache.kalumet.KalumetException;
import org.apache.kalumet.agent.Configuration;
import org.apache.kalumet.model.Environment;
import org.apache.kalumet.model.Kalumet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Agent utils method to post event.
 */
public class EventUtils {

    private final static transient Logger LOGGER = LoggerFactory.getLogger(EventUtils.class);

    /**
     * Wrapper method to post an event.
     *
     * @param environment the target environment.
     * @param author the event author.
     * @param severity the event severity.
     * @param event the event message.
     */
    public static void post(Environment environment, String author, String severity, String event) {
        LOGGER.debug("Loading configuration from the cache");
        Kalumet kalumet = Configuration.CONFIG_CACHE;
        if (kalumet == null) {
            LOGGER.debug("No configuration in cache, updating it");
            try {
                kalumet = Kalumet.digeste(Configuration.CONFIG_LOCATION);
            } catch (KalumetException kalumetException) {
                LOGGER.warn("Can't post journal event", kalumetException);
                return;
            }
            Configuration.CONFIG_CACHE = kalumet;
        }
        org.apache.kalumet.utils.EventUtils.post(environment, author, severity, event, kalumet);
    }

    /**
     * Wrapper method to post an event.
     *
     * @param environment the target environment.
     * @param severity the event severity.
     * @param event the event message.
     */
    public static void post(Environment environment, String severity, String event) {
        EventUtils.post(environment, Configuration.AGENT_ID, severity, event);
    }

}
