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
package org.apache.kalumet.agent.updater;

import org.apache.kalumet.KalumetException;
import org.apache.kalumet.agent.Configuration;
import org.apache.kalumet.agent.utils.EventUtils;
import org.apache.kalumet.controller.core.ControllerException;
import org.apache.kalumet.controller.core.J2EEApplicationServerController;
import org.apache.kalumet.controller.core.J2EEApplicationServerControllerFactory;
import org.apache.kalumet.model.Environment;
import org.apache.kalumet.model.J2EEApplicationServer;
import org.apache.kalumet.model.JMSConnectionFactory;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.model.update.UpdateLog;
import org.apache.kalumet.model.update.UpdateMessage;
import org.apache.kalumet.utils.NotifierUtils;
import org.apache.kalumet.utils.PublisherUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JMS connection factory updater.
 */
public class JMSConnectionFactoryUpdater
{

  private static final transient Logger LOGGER = LoggerFactory.getLogger( JMSConnectionFactoryUpdater.class );

  /**
   * Updates a JMS connection factory.
   *
   * @param environment          the target <code>Environment</code>.
   * @param server               the target <code>J2EEApplicationServer</code>.
   * @param jmsConnectionFactory the target <code>JMSConnectionFactory</code>.
   * @param updateLog            the <code>UpdateLog</code> to use.
   */
  public static void update( Environment environment, J2EEApplicationServer server,
                             JMSConnectionFactory jmsConnectionFactory, UpdateLog updateLog )
    throws UpdateException
  {
    LOGGER.info( "Updating JMS connection factory {}", jmsConnectionFactory.getName() );
    updateLog.addUpdateMessage(
      new UpdateMessage( "info", "Updating JMS connection factory " + jmsConnectionFactory.getName() ) );
    EventUtils.post( environment, "UPDATE", "Updating JMS connection factory " + jmsConnectionFactory.getName() );
    if ( !jmsConnectionFactory.isActive() )
    {
      // the JMS connection factory is not active
      LOGGER.info( "JMS connection factory {} is inactive, so not updated", jmsConnectionFactory.getName() );
      updateLog.addUpdateMessage( new UpdateMessage( "info", "JMS connection factory " + jmsConnectionFactory.getName()
        + " is inactive, so not updated" ) );
      EventUtils.post( environment, "UPDATE",
                       "JMS Connection Factory " + jmsConnectionFactory.getName() + " is inactive, so not updated" );
      return;
    }
    J2EEApplicationServerController controller = null;
    try
    {
      // connect controller to J2EE application server
      LOGGER.debug( "Connecting to J2EE application server controller" );
      controller = J2EEApplicationServerControllerFactory.getController( environment, server );
    }
    catch ( KalumetException e )
    {
      LOGGER.error( "Can't connect to J2EE application server {} controller", server.getName(), e );
      throw new UpdateException( "Can't connect to J2EE application server " + server.getName() + " controller", e );
    }
    try
    {
      if ( controller.isJMSConnectionFactoryDeployed( jmsConnectionFactory.getName() ) )
      {
        // JMS connection factory already deployed in the J2EE application server
        LOGGER.info( "JMS connection factory {} already deployed", jmsConnectionFactory.getName() );
        updateLog.addUpdateMessage( new UpdateMessage( "info",
                                                       "JMS connection factory " + jmsConnectionFactory.getName()
                                                         + " already deployed" ) );
        EventUtils.post( environment, "UPDATE",
                         "JMS connection factory " + jmsConnectionFactory.getName() + " already deployed" );
      }
      else
      {
        // deploy the JMS connection factory
        controller.deployJMSConnectionFactory( jmsConnectionFactory.getName() );
        updateLog.setStatus( "Update performed" );
        updateLog.setUpdated( true );
        updateLog.addUpdateMessage(
          new UpdateMessage( "info", "JMS connection factory " + jmsConnectionFactory.getName() + " deployed" ) );
        EventUtils.post( environment, "UPDATE",
                         "JMS connection factory " + jmsConnectionFactory.getName() + " deployed" );
        LOGGER.info( "JMS connection factory {} deployed", jmsConnectionFactory.getName() );
      }
    }
    catch ( ControllerException e )
    {
      LOGGER.error( "JMS connection factory {} update failed", jmsConnectionFactory.getName(), e );
      throw new UpdateException( "JMS connection factory " + jmsConnectionFactory.getName() + " update failed", e );
    }
  }

  /**
   * Wrapper method to update a JMS connection factory via WS.
   *
   * @param environmentName          the target environment name.
   * @param serverName               the target J2EE application server name.
   * @param jmsConnectionFactoryName the target JMS connection factory name.
   * @throws KalumetException in case of update failure.
   */
  public static void update( String environmentName, String serverName, String jmsConnectionFactoryName )
    throws KalumetException
  {
    LOGGER.info( "JMS connection factory {} update requested by WS", jmsConnectionFactoryName );

    // load configuration.
    LOGGER.debug( "Loading configuration" );
    Kalumet kalumet = Kalumet.digeste( Configuration.CONFIG_LOCATION );

    // looking for component objects
    Environment environment = kalumet.getEnvironment( environmentName );
    if ( environment == null )
    {
      LOGGER.error( "Environment {} is not found in the configuration", environmentName );
      throw new KalumetException( "Environment " + environmentName + " is not found in the configuration" );
    }
    J2EEApplicationServer server = environment.getJ2EEApplicationServers().getJ2EEApplicationServer( serverName );
    if ( server == null )
    {
      LOGGER.error( "J2EE application server {} is not found in environment {}", serverName, environment.getName() );
      throw new KalumetException(
        "J2EE application server " + serverName + " is not found in environment " + environment.getName() );
    }
    JMSConnectionFactory jmsConnectionFactory = server.getJMSConnectionFactory( jmsConnectionFactoryName );
    if ( jmsConnectionFactory == null )
    {
      LOGGER.error( "JMS connection factory {} is not found in J2EE application server {}", jmsConnectionFactoryName,
                    server.getName() );
      throw new KalumetException(
        "JMS connection factory " + jmsConnectionFactoryName + " is not found in J2EE application server "
          + server.getName() );
    }

    // post event and create update log
    LOGGER.debug( "Posting event and creating update log" );
    EventUtils.post( environment, "UPDATE",
                     "JMS connection factory " + jmsConnectionFactory.getName() + " update requested by WS" );
    UpdateLog updateLog =
      new UpdateLog( "JMS connection factory " + jmsConnectionFactory.getName() + " update in progress ...",
                     environment.getName(), environment );

    // send a notification and waiting for the count down
    LOGGER.debug( "Send a notification and waiting for the count down" );
    EventUtils.post( environment, "UPDATE", "Send a notification and waiting for the count down" );
    NotifierUtils.waitAndNotify( environment );

    try
    {
      // call update
      LOGGER.debug( "Call JMS connection factory updater" );
      JMSConnectionFactoryUpdater.update( environment, server, jmsConnectionFactory, updateLog );
    }
    catch ( Exception e )
    {
      LOGGER.error( "JMS connection factory {} update failed", jmsConnectionFactory.getName(), e );
      EventUtils.post( environment, "ERROR",
                       "JMS connection factory " + jmsConnectionFactory.getName() + " update failed: "
                         + e.getMessage() );
      updateLog.setStatus( "JMS connection factory " + jmsConnectionFactory.getName() + " update failed" );
      updateLog.addUpdateMessage( new UpdateMessage( "error", "JMS connection factory " + jmsConnectionFactory.getName()
        + " update failed: " + e.getMessage() ) );
      PublisherUtils.publish( environment );
      throw new UpdateException( "JMS connection factory " + jmsConnectionFactory.getName() + " update failed", e );
    }

    // update completed.
    LOGGER.info( "JMS connection factory {} updated", jmsConnectionFactory.getName() );
    EventUtils.post( environment, "UPDATE", "JMS connection factory " + jmsConnectionFactory.getName() + " updated" );
    if ( updateLog.isUpdated() )
    {
      updateLog.setStatus( "JMS connection factory " + jmsConnectionFactory.getName() + " updated" );
    }
    else
    {
      updateLog.setStatus( "JMS connection factory " + jmsConnectionFactory.getName() + " is already up to date" );
    }
    updateLog.addUpdateMessage(
      new UpdateMessage( "info", "JMS connection factory " + jmsConnectionFactory.getName() + " updated" ) );
    LOGGER.info( "Publishing update report" );
    PublisherUtils.publish( environment );
  }

  /**
   * Check a JMS connection factory via WS.
   *
   * @param environmentName          the target environment name.
   * @param applicationServerName    the target J2EE application server name.
   * @param jmsConnectionFactoryName the target JMS connection factory name.
   * @return true if the JMS connection factory is up to date, false else.
   * @throws KalumetException in case of check failure.
   */
  public static boolean check( String environmentName, String applicationServerName, String jmsConnectionFactoryName )
    throws KalumetException
  {
    LOGGER.info( "JMS connection factory {} status check requested by WS", jmsConnectionFactoryName );

    // load configuration.
    LOGGER.debug( "Loading configuration" );
    Kalumet kalumet = Kalumet.digeste( Configuration.CONFIG_LOCATION );

    // looking for component objects
    Environment environment = kalumet.getEnvironment( environmentName );
    if ( environment == null )
    {
      LOGGER.error( "Environment {} is not found in the configuration", environmentName );
      throw new KalumetException( "Environment " + environmentName + " is not found in the configuration" );
    }
    J2EEApplicationServer server =
      environment.getJ2EEApplicationServers().getJ2EEApplicationServer( applicationServerName );
    if ( server == null )
    {
      LOGGER.error( "J2EE application server {} is not found in environment {}", applicationServerName,
                    environment.getName() );
      throw new KalumetException(
        "J2EE application server " + applicationServerName + " is not found in environment " + environment.getName() );
    }
    JMSConnectionFactory jmsConnectionFactory = server.getJMSConnectionFactory( jmsConnectionFactoryName );
    if ( jmsConnectionFactory == null )
    {
      LOGGER.error( "JMS connection factory {} is not found in the J2EE application server {}",
                    jmsConnectionFactoryName, server.getName() );
      throw new KalumetException(
        "JMS connection factory " + jmsConnectionFactoryName + " is not found in the J2EE application server "
          + server.getName() );
    }

    // post an event
    EventUtils.post( environment, "INFO",
                     "JMS connection factory " + jmsConnectionFactory.getName() + " status check requested by WS" );

    try
    {
      // get J2EE application server controller.
      LOGGER.debug( "Getting J2EE application server controller" );
      J2EEApplicationServerController controller =
        J2EEApplicationServerControllerFactory.getController( environment, server );
      // check if the JMS connection factory is deployed
      LOGGER.debug( "Check the status of the JMS connection factory " + jmsConnectionFactory.getName() );
      return controller.isJMSConnectionFactoryDeployed( jmsConnectionFactory.getName() );
    }
    catch ( Exception e )
    {
      LOGGER.error( "JMS connection factory {} status check failed", jmsConnectionFactory.getName(), e );
      throw new KalumetException( "JMS connection factory " + jmsConnectionFactory.getName() + " status check failed",
                                  e );
    }
  }

}
