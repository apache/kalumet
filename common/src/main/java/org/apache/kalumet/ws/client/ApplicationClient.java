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
package org.apache.kalumet.ws.client;


/**
 * Application WS client.
 */
public class ApplicationClient extends AbstractClient {

    /**
     * Default constructor.
     *
     * @param host hostname or IP address of the Kalumet agent WS server.
     * @param port port number of the Kalumet agent WS server.
     * @throws ClientException in case of communication failure.
     */
    public ApplicationClient(String host, int port) throws ClientException {
        super("http://" + host + ":" + port + "/axis/services/J2EEApplicationServer");
    }

    /**
     * Wrapper method to update a J2EE application.
     *
     * @param environmentName the target environment name.
     * @param applicationServerName the target J2EE application server name.
     * @param applicationName
     * @param delegation
     * @throws ClientException
     */
    public void update(String environmentName, String applicationServerName, String applicationName, boolean delegation) throws ClientException {
        try {
            call.invoke("update", new Object[]{ environmentName, applicationServerName, applicationName, new Boolean(delegation) });
        } catch (Exception e) {
            throw new ClientException("J2EE application " + applicationName + " update failed", e);
        }
    }

}
