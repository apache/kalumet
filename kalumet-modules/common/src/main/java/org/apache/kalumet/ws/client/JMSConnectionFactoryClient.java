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
 * JMS connection factory WS client.
 */
public class JMSConnectionFactoryClient
    extends AbstractClient
{

    /**
     * Default constructor.
     *
     * @param host the hostname or IP address of the Kalumet agent WS server.
     * @param port the port number of the Kalumet agent WS server.
     * @throws ClientException in case of communication failure.
     */
    public JMSConnectionFactoryClient( String host, int port )
        throws ClientException
    {
        super( "http://" + host + ":" + port + "/axis/services/JMSConnectionFactoryService" );
    }

    /**
     * Wrapper method to update a JMS connection factory.
     *
     * @param environmentName       the target environment name.
     * @param applicationServerName the target JEE application server name.
     * @param connectionFactoryName the target JMS connection factory.
     * @throws ClientException in case of update failure.
     */
    public void update( String environmentName, String applicationServerName, String connectionFactoryName )
        throws ClientException
    {
        try
        {
            call.invoke( "update", new Object[]{ environmentName, applicationServerName, connectionFactoryName } );
        }
        catch ( Exception e )
        {
            throw new ClientException( "JMS connection factory " + connectionFactoryName + " update failed", e );
        }
    }

    /**
     * Wrapper method to check if a JMS connection factory is up to date or not.
     *
     * @param environmentName       the target environment name.
     * @param applicationServerName the target JEE application server name.
     * @param connectionFactoryName the target JMS connection factory name.
     * @return true if the JMS connection factory is up to date, false else.
     * @throws ClientException in case of status check failure.
     */
    public boolean check( String environmentName, String applicationServerName, String connectionFactoryName )
        throws ClientException
    {
        boolean upToDate = false;
        try
        {
            upToDate = ( (Boolean) call.invoke( "check", new Object[]{ environmentName, applicationServerName,
                connectionFactoryName } ) ).booleanValue();
        }
        catch ( Exception e )
        {
            throw new ClientException( "JMS connection factory " + connectionFactoryName + " status check failed", e );
        }
        return upToDate;
    }

}
