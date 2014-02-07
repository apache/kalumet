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
package org.apache.kalumet.agent;

import org.apache.axis.EngineConfiguration;
import org.apache.axis.configuration.FileProvider;
import org.apache.axis.transport.http.SimpleAxisServer;
import org.apache.kalumet.KalumetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Kalumet agent embedded HTTP WS server.
 * This embedded server listens for incoming SOAP messages. These messages come from Kalumet console or other WS clients.
 */
public class WsServer
{

    private final static transient Logger LOGGER = LoggerFactory.getLogger( WsServer.class );

    private final static int MAX_POOL_SIZE = 500;

    private final static int MAX_SESSIONS_NUMBER = 500;

    private SimpleAxisServer simpleAxisServer;

    /**
     * Create an embedded WS server.
     *
     * @param port     the WS server listen port.
     * @param wsddFile the WebService Deployment Descriptor.
     * @throws KalumetException in case of WS server creation failure.
     */
    public WsServer( int port, String wsddFile )
        throws KalumetException
    {
        simpleAxisServer = new SimpleAxisServer( MAX_POOL_SIZE, MAX_SESSIONS_NUMBER );
        LOGGER.debug( "Creating WS server" );
        LOGGER.debug( " Max pool size: " + MAX_POOL_SIZE );
        LOGGER.debug( " Max sessions number: " + MAX_SESSIONS_NUMBER );
        try
        {
            simpleAxisServer.setServerSocket( new ServerSocket( port ) );
            LOGGER.debug( "WS server started on port {}", port );
        }
        catch ( IOException e )
        {
            LOGGER.error( "Can't create WS server on port {}", port, e );
            throw new KalumetException( "Can't create WS server on port " + port, e );
        }
    }

    /**
     * Start the WS server.
     *
     * @throws KalumetException in case of WS server startup failure.
     */
    public void start()
        throws KalumetException
    {
        try
        {
            LOGGER.debug( "Starting WS server in daemon mode" );
            simpleAxisServer.start( true );
        }
        catch ( Exception e )
        {
            LOGGER.error( "Can't start WS server", e );
            throw new KalumetException( "Can't start WS server", e );
        }
    }

    /**
     * Define the WS server configuration.
     *
     * @param wsddFile the WebService Deployment Descriptor file.
     * @return the engine configuration of the WS server.
     * @throws KalumetException in case of configuration failure.
     */
    private EngineConfiguration getEngineConfiguration( String wsddFile )
        throws KalumetException
    {
        return new FileProvider( getClass().getResourceAsStream( wsddFile ) );
    }

}
