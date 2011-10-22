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
 * J2EE application server Connection pool WS client.
 */
public class ConnectionPoolClient extends AbstractClient {

    /**
     * Default constructor.
     *
     * @param host hostname or IP address of the Kalumet agent WS server.
     * @param port port number of the Kalumet agent WS server.
     * @throws ClientException in case of communication failure.
     */
    public ConnectionPoolClient(String host, int port) throws ClientException {
        super("http://" + host + ":" + port + "/axis/services/J2EEApplicationServerConnectionPoolService");
    }

    /**
     * Wrapper method to update a connection pool in the J2EE application server.
     *
     * @param environmentName the target environment name.
     * @param applicationServerName the target J2EE application server name.
     * @param connectionPoolName the target connection pool name.
     * @throws ClientException in case of communication failure.
     */
    public void update(String environmentName, String applicationServerName, String connectionPoolName) throws ClientException {
        try {
            call.invoke("update", new Object[]{ environmentName, applicationServerName, connectionPoolName });
        } catch (Exception e) {
            throw new ClientException("Connection pool " + connectionPoolName + " update failed", e);
        }
    }

    /**
     * Wrapper method to check if a connection pool is up to date or not.
     *
     * @param environmentName the target environment name.
     * @param applicationServerName the target J2EE application server name.
     * @param connectionPoolName the target connection pool name.
     * @return true if the connection pool is up to date, false else.
     * @throws ClientException in case of communication failure.
     */
    public boolean check(String environmentName, String applicationServerName, String connectionPoolName) throws ClientException {
        boolean upToDate = false;
        try {
            upToDate = ((Boolean) call.invoke("check", new Object[]{ environmentName, applicationServerName, connectionPoolName })).booleanValue();
        } catch (Exception e) {
            throw new ClientException("Connection pool " + connectionPoolName + " status check failed", e);
        }
        return upToDate;
    }

}
