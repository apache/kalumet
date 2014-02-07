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
 * Database WS client.
 */
public class DatabaseClient
    extends AbstractClient
{

    /**
     * Default constructor.
     *
     * @param host the hostname or IP address of the Kalumet agent WS server.
     * @param port the port number of the Kalumet agent WS server.
     * @throws ClientException in case of communication failure.
     */
    public DatabaseClient( String host, int port )
        throws ClientException
    {
        super( "http://" + host + ":" + port + "/axis/services/JEEApplicationDatabaseService" );
    }

    /**
     * Wrapper method to update a database.
     *
     * @param environmentName       the target environment name.
     * @param applicationServerName the target JEE application server name.
     * @param applicationName       the target JEE application name.
     * @param databaseName          the target database name.
     * @param delegation            if true, the call is a delegation from another agent, false else.
     * @throws ClientException in case of communication failure.
     */
    public void update( String environmentName, String applicationServerName, String applicationName,
                        String databaseName, boolean delegation )
        throws ClientException
    {
        try
        {
            call.invoke( "update", new Object[]{ environmentName, applicationServerName, applicationName, databaseName,
                new Boolean( delegation ) } );
        }
        catch ( Exception e )
        {
            throw new ClientException( "Database " + databaseName + " update failed", e );
        }
    }

}
