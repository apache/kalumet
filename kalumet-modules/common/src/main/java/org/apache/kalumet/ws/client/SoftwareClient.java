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
 * Software WS client.
 */
public class SoftwareClient
    extends AbstractClient
{

    /**
     * Default constructor.
     *
     * @param host the hostname or IP address of the Kalumet agent WS server.
     * @param port the port number of the Kalumet agent WS server.
     * @throws ClientException in case of communication failure.
     */
    public SoftwareClient( String host, int port )
        throws ClientException
    {
        super( "http://" + host + ":" + port + "/axis/services/SoftwareService" );
    }

    /**
     * Wrapper method to update a software.
     *
     * @param environmentName the target environment name.
     * @param softwareName    the target software name.
     * @param delegation      true if the call is performed by another agent, false else.
     * @throws ClientException in case of update failure.
     */
    public void update( String environmentName, String softwareName, boolean delegation )
        throws ClientException
    {
        try
        {
            call.invoke( "update", new Object[]{ environmentName, softwareName, new Boolean( delegation ) } );
        }
        catch ( Exception e )
        {
            throw new ClientException( "Software " + softwareName + " update failed", e );
        }
    }

    /**
     * Wrapper method to execute a command.
     *
     * @param environmentName the target environment name.
     * @param softwareName    the target software name.
     * @param commandName     the target command name.
     * @param delegation      true if the call is performed by another agent, false else.
     * @throws ClientException in case of command execution failure.
     */
    public void executeCommand( String environmentName, String softwareName, String commandName, boolean delegation )
        throws ClientException
    {
        try
        {
            call.invoke( "executeCommand",
                         new Object[]{ environmentName, softwareName, commandName, new Boolean( delegation ) } );
        }
        catch ( Exception e )
        {
            throw new ClientException( "Command " + commandName + " execution failed", e );
        }
    }

    /**
     * Wrapper method to update a location.
     *
     * @param environmentName the target environment name.
     * @param softwareName    the target software name.
     * @param locationName    the target location name.
     * @param delegation      true if the call is performed by another agent, false else.
     * @throws ClientException in case of location update failure.
     */
    public void updateLocation( String environmentName, String softwareName, String locationName, boolean delegation )
        throws ClientException
    {
        try
        {
            call.invoke( "updateLocation",
                         new Object[]{ environmentName, softwareName, locationName, new Boolean( delegation ) } );
        }
        catch ( Exception e )
        {
            throw new ClientException( "Location " + locationName + " update failed", e );
        }
    }

    /**
     * Wrapper method to update a configuration file.
     *
     * @param environmentName       the target environment name.
     * @param softwareName          the target software name.
     * @param configurationFileName the target configuration file name.
     * @param delegation            true if the call is performed by another agent, false else.
     * @throws ClientException in case of configuration file update failure.
     */
    public void updateConfigurationFile( String environmentName, String softwareName, String configurationFileName,
                                         boolean delegation )
        throws ClientException
    {
        try
        {
            call.invoke( "updateConfigurationFile", new Object[]{ environmentName, softwareName, configurationFileName,
                new Boolean( delegation ) } );
        }
        catch ( Exception e )
        {
            throw new ClientException( "Configuration file " + configurationFileName + " update failed", e );
        }
    }

    /**
     * Wrapper method to update a database.
     *
     * @param environmentName the target environment name.
     * @param softwareName    the target software name.
     * @param databaseName    the target database name.
     * @param delegation      true if the call is performed by another agent, false else.
     * @throws ClientException in case of database update failure.
     */
    public void updateDatabase( String environmentName, String softwareName, String databaseName, boolean delegation )
        throws ClientException
    {
        try
        {
            call.invoke( "updateDatabase",
                         new Object[]{ environmentName, softwareName, databaseName, new Boolean( delegation ) } );
        }
        catch ( Exception e )
        {
            throw new ClientException( "Database " + databaseName + " update failed", e );
        }
    }

}
