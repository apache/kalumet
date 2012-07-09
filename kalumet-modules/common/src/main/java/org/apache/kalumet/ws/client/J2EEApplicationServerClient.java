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
 * J2EE application server WS client.
 */
public class J2EEApplicationServerClient
  extends AbstractClient
{

  /**
   * Default constructor.
   *
   * @param host the hostname or IP address of the Kalumet agent WS server.
   * @param port the port number of the Kalumet agent WS server.
   * @throws ClientException in case of communication failure.
   */
  public J2EEApplicationServerClient( String host, int port )
    throws ClientException
  {
    super( "http://" + host + ":" + port + "/axis/services/J2EEApplicationServerService" );
  }

  /**
   * Wrapper method to stop a J2EE application server.
   *
   * @param environmentName       the target environment name.
   * @param applicationServerName the target J2EE application server name.
   * @throws ClientException in case of stop failure.
   */
  public void stop( String environmentName, String applicationServerName )
    throws ClientException
  {
    try
    {
      call.invoke( "stop", new Object[]{ environmentName, applicationServerName } );
    }
    catch ( Exception e )
    {
      throw new ClientException( "J2EE application server " + applicationServerName + " stop failed", e );
    }
  }

  /**
   * Wrapper method to start a J2EE application server.
   *
   * @param environmentName       the target environment name.
   * @param applicationServerName the target J2EE application server name.
   * @throws ClientException in case of stop failure.
   */
  public void start( String environmentName, String applicationServerName )
    throws ClientException
  {
    try
    {
      call.invoke( "start", new Object[]{ environmentName, applicationServerName } );
    }
    catch ( Exception e )
    {
      throw new ClientException( "J2EE application server " + applicationServerName + " start failed", e );
    }
  }

  /**
   * Wrapper method to check the status of a J2EE application server.
   *
   * @param environmentName       the target environment name.
   * @param applicationServerName the target J2EE application server name.
   * @return the current status of the J2EE application server.
   * @throws ClientException in case of status check failure.
   */
  public String status( String environmentName, String applicationServerName )
    throws ClientException
  {
    String status = null;
    try
    {
      status = (String) call.invoke( "status", new Object[]{ environmentName, applicationServerName } );
    }
    catch ( Exception e )
    {
      throw new ClientException( "J2EE application server " + applicationServerName + " status check failed", e );
    }
    return status;
  }

  /**
   * Wrapper method to update a J2EE application server.
   *
   * @param environmentName       the target environment name.
   * @param applicationServerName the target J2EE application server name.
   * @param delegation            if true, the call is a delegation from another agent, false else.
   * @throws ClientException in case of update failure.
   */
  public void update( String environmentName, String applicationServerName, boolean delegation )
    throws ClientException
  {
    try
    {
      call.invoke( "update", new Object[]{ environmentName, applicationServerName, new Boolean( delegation ) } );
    }
    catch ( Exception e )
    {
      throw new ClientException( "J2EE application server " + applicationServerName + " update failed", e );
    }
  }

}
