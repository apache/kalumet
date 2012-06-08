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
import org.apache.kalumet.model.update.UpdateLog;
import org.apache.kalumet.model.update.UpdateMessage;
import org.apache.kalumet.utils.NotifierUtils;
import org.apache.kalumet.utils.PublisherUtils;
import org.apache.kalumet.utils.VariableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * JMS server updater.
 */
public class JMSServerUpdater
{

  private static final transient Logger LOGGER = LoggerFactory.getLogger( JMSServerUpdater.class );

  /**
   * Update a JMS server.
   *
   * @param environment the target <code>Environment</code>.
   * @param server      the target <code>J2EEApplicationServer</code>.
   * @param jmsServer   the target <code>JMSServer</code>.
   * @param updateLog   the <code>UpdateLog</code> to use.
   * @thorws UpdateException in case of update failure.
   */
  public static void update( Environment environment, J2EEApplicationServer server, JMSServer jmsServer,
                             UpdateLog updateLog )
    throws UpdateException
  {
    LOGGER.info( "Updating JMS server {}", jmsServer.getName() );
    updateLog.addUpdateMessage( new UpdateMessage( "info", "Updating JMS server " + jmsServer.getName() ) );
    EventUtils.post( environment, "UPDATE", "Updating JMS server " + jmsServer.getName() );
    if ( !jmsServer.isActive() )
    {
      // JMS server is not active
      LOGGER.info( "JMS server {} is inactive, so not updated", jmsServer.getName() );
      updateLog.addUpdateMessage(
        new UpdateMessage( "info", "JMS server " + jmsServer.getName() + " is inactive, so not updated" ) );
      EventUtils.post( environment, "UPDATE", "JMS server " + jmsServer.getName() + " is inactive, so not updated" );
      return;
    }
    // construct the queues and topics list
    LinkedList queues = new LinkedList();
    LinkedList topics = new LinkedList();
    // construct the queues
    for ( Iterator queueIterator = jmsServer.getJMSQueues().iterator(); queueIterator.hasNext(); )
    {
      JMSQueue jmsQueue = (JMSQueue) queueIterator.next();
      queues.add( VariableUtils.replace( jmsQueue.getName(), environment.getVariables() ) );
    }
    // construct the topics
    for ( Iterator topicIterator = jmsServer.getJMSTopics().iterator(); topicIterator.hasNext(); )
    {
      JMSTopic jmsTopic = (JMSTopic) topicIterator.next();
      topics.add( VariableUtils.replace( jmsTopic.getName(), environment.getVariables() ) );
    }
    J2EEApplicationServerController controller = null;
    try
    {
      // connect JMX controller to JEE application server
      LOGGER.debug( "Connecting to J2EE application server {} controller", server.getName() );
      controller = J2EEApplicationServerControllerFactory.getController( environment, server );
    }
    catch ( KalumetException e )
    {
      LOGGER.error( "Can't connect to J2EE application server {} controller", server.getName(), e );
      throw new UpdateException( "Can't connect to J2EE application server " + server.getName() + " controller", e );
    }
    try
    {
      if ( controller.isJMSServerDeployed( jmsServer.getName() ) )
      {
        // JMS server already deployed, check for update
        LOGGER.info( "JMS server {} already deployed, checking for update", jmsServer.getName() );
        if ( controller.updateJMSServer( jmsServer.getName(), queues, topics ) )
        {
          updateLog.setStatus( "Update performed" );
          updateLog.setUpdated( true );
          updateLog.addUpdateMessage( new UpdateMessage( "info", "JMS server " + jmsServer.getName() + " updated" ) );
          EventUtils.post( environment, "UPDATE", "JMS server " + jmsServer.getName() + " updated" );
          LOGGER.info( "JMS server {} updated", jmsServer.getName() );
        }
      }
      else
      {
        // JMS server is not deployed, deploy it
        controller.deployJMSServer( jmsServer.getName(), queues, topics );
        updateLog.setStatus( "Update performed" );
        updateLog.setUpdated( true );
        updateLog.addUpdateMessage( new UpdateMessage( "info", "JMS server " + jmsServer.getName() + " deployed" ) );
        EventUtils.post( environment, "UPDATE", "JMS server " + jmsServer.getName() + " deployed" );
        LOGGER.info( "JMS server {} deployed" );
      }
    }
    catch ( ControllerException e )
    {
      LOGGER.error( "JMS server {} update failed", jmsServer.getName(), e );
      throw new UpdateException( "JMS server " + jmsServer.getName() + " update failed", e );
    }
  }

  /**
   * Wrapper method to update a JMS server via WS.
   *
   * @param environmentName       the target environment name.
   * @param applicationServerName the target J2EE application server name.
   * @param jmsServerName         the target JMS server name.
   * @throws KalumetException if case of update failure.
   */
  public static void update( String environmentName, String applicationServerName, String jmsServerName )
    throws KalumetException
  {
    LOGGER.info( "JMS server {} update requested by WS", jmsServerName );

    // load configuration.
    LOGGER.debug( "Loading configuration" );
    Kalumet kalumet = Kalumet.digeste( Configuration.CONFIG_LOCATION );

    // looking for component objects
    LOGGER.debug( "Looking for component objects" );
    Environment environment = kalumet.getEnvironment( environmentName );
    if ( environment == null )
    {
      LOGGER.error( "Environment {} is not found in the configuration", environmentName );
      throw new KalumetException( "Environment " + environmentName + " is not found in the configuration" );
    }
    J2EEApplicationServer applicationServer =
      environment.getJ2EEApplicationServers().getJ2EEApplicationServer( applicationServerName );
    if ( applicationServer == null )
    {
      LOGGER.error( "J2EE application server {} is not found in environment {}", applicationServerName,
                    environment.getName() );
      throw new KalumetException(
        "J2EE application server " + applicationServerName + " is not found in environment " + environment.getName() );
    }
    JMSServer jmsServer = applicationServer.getJMSServer( jmsServerName );
    if ( jmsServer == null )
    {
      LOGGER.error( "JMS server {} is not found in J2EE application server {}", jmsServerName,
                    applicationServer.getName() );
      throw new KalumetException(
        "JMS server " + jmsServerName + " is not found in J2EE application server " + applicationServer.getName() );
    }

    // post an event and create update log.
    LOGGER.debug( "Posting an event and creating update log" );
    EventUtils.post( environment, "UPDATE", "JMS server " + jmsServer.getName() + " update requested by WS" );
    UpdateLog updateLog =
      new UpdateLog( "JMS server " + jmsServer.getName() + " update in progress ...", environment.getName(),
                     environment );

    // send a notification and waiting for the count down.
    LOGGER.info( "Send a notification and waiting for the count down" );
    EventUtils.post( environment, "UPDATE", "Post an event and waiting for the count down" );
    NotifierUtils.waitAndNotify( environment );

    try
    {
      // call update
      LOGGER.debug( "Call JMS server updater" );
      JMSServerUpdater.update( environment, applicationServer, jmsServer, updateLog );
    }
    catch ( Exception e )
    {
      LOGGER.error( "JMS server {} update failed", jmsServer.getName(), e );
      EventUtils.post( environment, "ERROR",
                       "JMS server " + jmsServer.getName() + " update failed: " + e.getMessage() );
      updateLog.setStatus( "JMS server " + jmsServer.getName() + " update failed" );
      updateLog.addUpdateMessage(
        new UpdateMessage( "error", "JMS server " + jmsServer.getName() + " update failed: " + e.getMessage() ) );
      PublisherUtils.publish( environment );
      throw new KalumetException( "JMS server " + jmsServer.getName() + " update failed", e );
    }

    // update completed
    LOGGER.info( "JMS server {} updated", jmsServer.getName() );
    EventUtils.post( environment, "UPDATE", "JMS server " + jmsServer.getName() + " updated" );
    if ( updateLog.isUpdated() )
    {
      updateLog.setStatus( "JMS server " + jmsServer.getName() + " updated" );
    }
    else
    {
      updateLog.setStatus( "JMS server " + jmsServer.getName() + " already up to date" );
    }
    updateLog.addUpdateMessage( new UpdateMessage( "info", "JMS server " + jmsServer.getName() + " updated" ) );
    LOGGER.info( "Publishing update report" );
    PublisherUtils.publish( environment );
  }

  /**
   * Check a JMS server via WS.
   *
   * @param environmentName       the target environment name.
   * @param applicationServerName the target J2EE application server name.
   * @param jmsServerName         the target JMS server name.
   * @return true if the JMS server is up to date, false else.
   * @throws KalumetException in case of check failure.
   */
  public static boolean check( String environmentName, String applicationServerName, String jmsServerName )
    throws KalumetException
  {
    LOGGER.info( "JMS server {} status check requested by WS", jmsServerName );

    // load configuration.
    LOGGER.debug( "Loading configuration" );
    Kalumet kalumet = Kalumet.digeste( Configuration.CONFIG_LOCATION );

    // load component objects
    LOGGER.debug( "Loading component objects" );
    Environment environment = kalumet.getEnvironment( environmentName );
    if ( environment == null )
    {
      LOGGER.error( "Environment {} is not found in the configuration", environmentName );
      throw new KalumetException( "Environment " + environmentName + " is not found in the configuration" );
    }
    J2EEApplicationServer applicationServer =
      environment.getJ2EEApplicationServers().getJ2EEApplicationServer( applicationServerName );
    if ( applicationServer == null )
    {
      LOGGER.error( "J2EE application server {} is not found in environment {}", applicationServerName,
                    environment.getName() );
      throw new KalumetException(
        "J2EE application server " + applicationServerName + " is not found in environment " + environment.getName() );
    }
    JMSServer jmsServer = applicationServer.getJMSServer( jmsServerName );
    if ( jmsServer == null )
    {
      LOGGER.error( "JMS server {} is not found in J2EE application server {}", jmsServerName,
                    applicationServer.getName() );
      throw new KalumetException(
        "JMS server " + jmsServerName + " is not found in J2EE application server " + applicationServer.getName() );
    }

    // post an event.
    EventUtils.post( environment, "INFO", "JMS server " + jmsServer.getName() + " status check requested by WS" );

    try
    {
      // get J2EE application server controller.
      LOGGER.debug( "Getting J2EE application server controller" );
      J2EEApplicationServerController controller =
        J2EEApplicationServerControllerFactory.getController( environment, applicationServer );
      // construct the queue list.
      LOGGER.debug( "Constructing the queue list" );
      LinkedList queues = new LinkedList();
      for ( Iterator queueIterator = jmsServer.getJMSQueues().iterator(); queueIterator.hasNext(); )
      {
        JMSQueue queue = (JMSQueue) queueIterator.next();
        queues.add( queue );
      }
      // construct the topic list.
      LOGGER.debug( "Constructing the topic list" );
      LinkedList topics = new LinkedList();
      for ( Iterator topicIterator = jmsServer.getJMSTopics().iterator(); topicIterator.hasNext(); )
      {
        JMSTopic topic = (JMSTopic) topicIterator.next();
        topics.add( topic );
      }
      // check if the JMS server is up to date.
      LOGGER.debug( "Checking if JMS server {} is up to date", jmsServer.getName() );
      return controller.isJMSServerUpToDate( jmsServer.getName(), queues, topics );
    }
    catch ( Exception e )
    {
      LOGGER.error( "JMS server {} check failed", jmsServer.getName(), e );
      throw new KalumetException( "JMS server " + jmsServer.getName() + " check failed", e );
    }
  }

}
