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
 * ConfigurationFIle WS client.
 */
public class ConfigurationFileClient extends AbstractClient {

    /**
     * Default constructor.
     *
     * @param host the hostname or IP address of the Kalumet agent WS server.
     * @param port the port number of the Kalumet agent WS server.
     * @throws ClientException in case of communication failure.
     */
    public ConfigurationFileClient(String host, int port) throws ClientException {
        super("http://" + host + ":" + port + "/axis/services/J2EEApplicationConfigurationFileService");
    }

    /**
     * Wrapper method to update a J2EE application configuration file.
     *
     * @param environmentName the target environment name.
     * @param applicationServerName the target J2EE application server name.
     * @param applicationName the target J2EE application name.
     * @param configurationFileName the target configuration file name.
     * @param delegation true if the call is a delegation from another agent, false else.
     * @throws ClientException in case of communication failure.
     */
    public void update(String environmentName, String applicationServerName, String applicationName, String configurationFileName, boolean delegation) throws ClientException {
        try {
            call.invoke("update", new Object[]{ environmentName, applicationServerName, applicationName, configurationFileName, new Boolean(delegation) });
        } catch (Exception e) {
            throw new ClientException("J2EE application configuration file " + configurationFileName + " update failed", e);
        }
    }

    /**
     * Wrapper method to check if the J2EE application configuration file is up to date.
     *
     * @param environmentName the target environment name.
     * @param applicationServerName the target J2EE application server name.
     * @param applicationName the target J2EE application name.
     * @param configurationFileName the target configuration file name.
     * @return true if the configuration file is up to date, false else.
     * @throws ClientException in case of communication failure.
     */
    public boolean check(String environmentName, String applicationServerName, String applicationName, String configurationFileName) throws ClientException {
        boolean upToDate = false;
        try {
            upToDate = ((Boolean) call.invoke("check", new Object[]{ environmentName, applicationServerName, applicationName, configurationFileName })).booleanValue();
        } catch (Exception e) {
            throw new ClientException("J2EE application configuration file " + configurationFileName + " status check failed", e);
        }
        return upToDate;
    }

}
