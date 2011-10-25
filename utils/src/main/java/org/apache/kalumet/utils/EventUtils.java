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
package org.apache.kalumet.utils;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.kalumet.model.Environment;
import org.apache.kalumet.model.Kalumet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Util class to post event to the Kalumet journal.
 */
public class EventUtils {

    private final static transient Logger LOGGER = LoggerFactory.getLogger(EventUtils.class);

    /**
     * Post an event to the Kalumet journal.
     *
     * @param environment the target environment..
     * @param author the author of the event.
     * @param severity the severity severity of the event.
     * @param event the event event.
     * @param kalumet the Kalumet configuration.
     */
    public static void post(Environment environment, String author, String severity, String event, Kalumet kalumet) {
        LOGGER.debug("Getting LogEventAppender property in Kalumet configuration");
        if (kalumet.getProperty("LogEventAppender") == null) {
            LOGGER.warn("Can't post event because the LogEventAppender is not define in the configuration");
            return;
        }
        String logEventAppender = kalumet.getProperty("LogEventAppender").getValue();

        // creating the HTTP client
        HttpClient httpClient = new HttpClient();
        // create the post method
        PostMethod postMethod = new PostMethod(logEventAppender);
        // add the HTTP parameters
        postMethod.addParameter("environment", environment.getName());
        postMethod.addParameter("author", author);
        postMethod.addParameter("severity", severity);
        postMethod.addParameter("event", event);
        try {
            httpClient.executeMethod(postMethod);
        } catch (Exception e) {
            // ignore
        } finally {
            postMethod.releaseConnection();
        }
    }

}
