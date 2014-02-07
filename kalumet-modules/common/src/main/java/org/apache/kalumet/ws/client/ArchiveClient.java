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
 * Archive WS client.
 */
public class ArchiveClient
    extends AbstractClient
{

    /**
     * Default constructor.
     *
     * @param host the hostname or IP address of the Kalumet agent WS server.
     * @param port the port number of the Kalumet agent WS server.
     * @throws ClientException in case of communication failure.
     */
    public ArchiveClient( String host, int port )
        throws ClientException
    {
        super( "http://" + host + ":" + port + "/axis/services/JEEApplicationArchiveService" );
    }

    /**
     * Wrapper method to call archive update.
     *
     * @param environmentName       the target environment name.
     * @param applicationServerName the target JEE application server name.
     * @param applicationName       the target JEE application name.
     * @param archiveName           the target archive name.
     * @param delegation            true if this call is a delegation from another agent, false else.
     * @throws ClientException in case of communication failure.
     */
    public void update( String environmentName, String applicationServerName, String applicationName,
                        String archiveName, boolean delegation )
        throws ClientException
    {
        try
        {
            call.invoke( "update", new Object[]{ environmentName, applicationServerName, applicationName, archiveName,
                new Boolean( delegation ) } );
        }
        catch ( Exception e )
        {
            throw new ClientException( "JEE archive " + archiveName + " update failed", e );
        }
    }

    /**
     * Wrapper method to call archive check.
     *
     * @param environmentName       the target environment name.
     * @param applicationServerName the target JEE application server name.
     * @param applicationName       the target JEE application name.
     * @param archiveName           the target archive name.
     * @return true if the JEE application archive is up to date, false else.
     * @throws ClientException in case of communication failure.
     */
    public boolean check( String environmentName, String applicationServerName, String applicationName,
                          String archiveName )
        throws ClientException
    {
        boolean upToDate = false;
        try
        {
            upToDate = ( (Boolean) call.invoke( "check",
                                                new Object[]{ environmentName, applicationServerName, applicationName,
                                                    archiveName } ) ).booleanValue();
        }
        catch ( Exception e )
        {
            throw new ClientException( "JEE archive " + archiveName + " check status failed", e );
        }
        return upToDate;
    }

}
