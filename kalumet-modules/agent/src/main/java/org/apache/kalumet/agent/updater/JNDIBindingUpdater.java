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
import org.apache.kalumet.model.JNDIBinding;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.model.update.UpdateLog;
import org.apache.kalumet.model.update.UpdateMessage;
import org.apache.kalumet.utils.NotifierUtils;
import org.apache.kalumet.utils.PublisherUtils;
import org.apache.kalumet.utils.VariableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JNDI binding (JNDI alias) updater.
 */
public class JNDIBindingUpdater
{

  private static final transient Logger LOGGER = LoggerFactory.getLogger( JNDIBindingUpdater.class );

  /**
   * Update a JNDI binding..
   *
   * @param environment the target <code>Environment</code>.
   * @param server      the target <code>J2EEApplicationServer</code>.
   * @param jndiBinding the target <code>JNDIBinding</code>.
   * @param updateLog   the <code>UpdateLog</code> to use.
   * @throws UpdateException in case of update failure.
   */
  public static void update( Environment environment, J2EEApplicationServer server, JNDIBinding jndiBinding,
                             UpdateLog updateLog )
    throws UpdateException
  {
    LOGGER.info( "Updating JNDI binding {}", jndiBinding.getName() );
    updateLog.addUpdateMessage( new UpdateMessage( "info", "Updating JNDI binding " + jndiBinding.getName() ) );
    EventUtils.post( environment, "UPDATE", "Updating JNDI binding " + jndiBinding.getName() );
    if ( !jndiBinding.isActive() )
    {
      // the JNDI binding is not active
      LOGGER.info( "JNDI binding {} is inactive, so not updated", jndiBinding.getName() );
      updateLog.addUpdateMessage(
        new UpdateMessage( "info", "JNDI binding " + jndiBinding.getName() + " is inactive, so not updated" ) );
      EventUtils.post( environment, "UPDATE",
                       "JNDI binding " + jndiBinding.getName() + " is inactive, so not updated" );
      return;
    }
    J2EEApplicationServerController controller = null;
    try
    {
      // connect controller to J2EE application server
      LOGGER.debug( "Connecting to J2EE application server {} controller", server.getName() );
      controller = J2EEApplicationServerControllerFactory.getController( environment, server );
    }
    catch ( KalumetException e )
    {
      LOGGER.error( "Can't connect to J2EE application server {} controller", server.getName(), e );
      throw new UpdateException( "Can't connect to J2EE application server " + server.getName() + " controller", e );
    }
    // replaces variables in name space binding data
    LOGGER.debug( "Replacing variables in name space binding data" );
    String mapJndiName = VariableUtils.replace( jndiBinding.getJndiname(), environment.getVariables() );
    String mapJndiAlias = VariableUtils.replace( jndiBinding.getJndialias(), environment.getVariables() );
    String mapJndiProviderUrl = VariableUtils.replace( jndiBinding.getProviderurl(), environment.getVariables() );
    try
    {
      if ( controller.isJNDIBindingDeployed( jndiBinding.getName() ) )
      {
        // the JNDI binding is already deployed, check for update
        LOGGER.info( "JNDI binding {} already deployed, checking for update", jndiBinding.getName() );
        if ( controller.updateJNDIBinding( jndiBinding.getName(), mapJndiName, mapJndiAlias, mapJndiProviderUrl ) )
        {
          // the JNDI binding has been updated
          updateLog.setStatus( "Update performed" );
          updateLog.setUpdated( true );
          updateLog.addUpdateMessage(
            new UpdateMessage( "info", "JNDI binding " + jndiBinding.getName() + " updated" ) );
          EventUtils.post( environment, "UPDATE", "JNDI binding " + jndiBinding.getName() + " updated" );
          LOGGER.info( "JNDI binding {} updated", jndiBinding.getName() );
        }
      }
      else
      {
        // JNDI binding is not deployed, deploy it
        controller.deployJNDIBinding( jndiBinding.getName(), mapJndiName, mapJndiAlias, mapJndiProviderUrl );
        updateLog.setStatus( "Update performed" );
        updateLog.setUpdated( true );
        updateLog.addUpdateMessage(
          new UpdateMessage( "info", "JNDI binding " + jndiBinding.getName() + " deployed" ) );
        EventUtils.post( environment, "UPDATE", "JNDI binding " + jndiBinding.getName() + " deployed" );
        LOGGER.info( "JNDI binding {} deployed", jndiBinding.getName() );
      }
    }
    catch ( ControllerException e )
    {
      LOGGER.error( "JNDI binding {} update failed", jndiBinding.getName(), e );
      throw new UpdateException( "JNDI binding " + jndiBinding.getName() + " update failed", e );
    }
  }

  /**
   * Wrapper method to JNDI binding update via WS.
   *
   * @param environmentName the target environment name.
   * @param serverName      the target J2EE application server name.
   * @param bindingName     the target JNDI binding name.
   * @throws KalumetException in case of update failure.
   */
  public static void update( String environmentName, String serverName, String bindingName )
    throws KalumetException
  {
    LOGGER.info( "JNDI binding {} update requested by WS", bindingName );

    // load configuration
    LOGGER.debug( "Loading configuration" );
    Kalumet kalumet = Kalumet.digeste( Configuration.CONFIG_LOCATION );

    // looking for component objects.
    LOGGER.debug( "Looking for component objects" );
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
    JNDIBinding jndiBinding = server.getJNDIBinding( bindingName );
    if ( jndiBinding == null )
    {
      LOGGER.error( "JNDI binding {} is not found in J2EE application server {}", bindingName, server.getName() );
      throw new KalumetException(
        "JNDI binding " + bindingName + " is not found in J2EE application server " + server.getName() );
    }

    // post an event and create the update log.
    LOGGER.debug( "Posting an event and creating the update log" );
    EventUtils.post( environment, "UPDATE", "JNDI binding " + jndiBinding.getName() + " update requested by WS" );
    UpdateLog updateLog =
      new UpdateLog( "JNDI binding " + jndiBinding.getName() + " update in progress ...", jndiBinding.getName(),
                     environment );

    // send a notification and waiting for the count down.
    LOGGER.info( "Send a notification and waiting for the count down" );
    EventUtils.post( environment, "UPDATE", "Send a notification and waiting for the count down" );
    NotifierUtils.waitAndNotify( environment );

    try
    {
      // call update.
      LOGGER.debug( "Call JNDI binding updater" );
      JNDIBindingUpdater.update( environment, server, jndiBinding, updateLog );
    }
    catch ( Exception e )
    {
      LOGGER.error( "JNDI binding {} update failed", jndiBinding.getName(), e );
      EventUtils.post( environment, "ERROR",
                       "JNDI binding " + jndiBinding.getName() + " update failed: " + e.getMessage() );
      updateLog.setStatus( "JNDI binding " + jndiBinding.getName() + " update failed" );
      updateLog.addUpdateMessage(
        new UpdateMessage( "error", "JNDI binding " + jndiBinding.getName() + " update failed: " + e.getMessage() ) );
      PublisherUtils.publish( environment );
      throw new UpdateException( "JNDI binding " + jndiBinding.getName() + " update failed", e );
    }

    // update completed
    LOGGER.info( "JNDI binding {} updated", jndiBinding.getName() );
    EventUtils.post( environment, "UPDATE", "JNDI binding " + jndiBinding.getName() + " updated" );
    if ( updateLog.isUpdated() )
    {
      updateLog.setStatus( "JNDI binding " + jndiBinding.getName() + " updated" );
    }
    else
    {
      updateLog.setStatus( "JNDI binding " + jndiBinding.getName() + " already up to date" );
    }
    updateLog.addUpdateMessage( new UpdateMessage( "info", "JNDI binding " + jndiBinding.getName() + " updated" ) );
    LOGGER.info( "Publishing update report" );
    PublisherUtils.publish( environment );
  }

  /**
   * Check if a JNDI name space binding is up to date or not via WS.
   *
   * @param environmentName the target environment name.
   * @param serverName      the target J2EE application server name.
   * @param jndiBindingName the target JNDI binding name.
   * @return true if the JNDI name space binding is up to date, false else.
   * @throws KalumetException in case of JNDI name space binding check failure.
   */
  public static boolean check( String environmentName, String serverName, String jndiBindingName )
    throws KalumetException
  {
    LOGGER.info( "JNDI binding {} status check requested by WS", jndiBindingName );

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
      environment.getJ2EEApplicationServers().getJ2EEApplicationServer( serverName );
    if ( applicationServer == null )
    {
      LOGGER.error( "J2EE application server {} is not found in environment {}", serverName, environment.getName() );
      throw new KalumetException(
        "J2EE application server " + serverName + " is not found in environment " + environment.getName() );
    }
    JNDIBinding jndiBinding = applicationServer.getJNDIBinding( jndiBindingName );
    if ( jndiBinding == null )
    {
      LOGGER.error( "JNDI binding {} is not found in J2EE application server {}", jndiBindingName,
                    applicationServer.getName() );
      throw new KalumetException(
        "JNDI binding " + jndiBindingName + " is not found in J2EE application server " + applicationServer.getName() );
    }

    // post an event
    EventUtils.post( environment, "INFO", "JNDI binding " + jndiBinding.getName() + " status check requested by WS" );

    try
    {
      // get J2EE application server controller
      LOGGER.debug( "Getting J2EE aplication server controller" );
      J2EEApplicationServerController controller =
        J2EEApplicationServerControllerFactory.getController( environment, applicationServer );
      // replace JNDI binding data with environment variables.
      LOGGER.debug( "Replaces variables in JNDI binding data" );
      String jndiName = VariableUtils.replace( jndiBinding.getJndiname(), environment.getVariables() );
      String jndiAlias = VariableUtils.replace( jndiBinding.getJndialias(), environment.getVariables() );
      String jndiProviderUrl = VariableUtils.replace( jndiBinding.getProviderurl(), environment.getVariables() );
      // check if the JNDI binding is up to date.
      return controller.isJNDIBindingUpToDate( jndiBinding.getName(), jndiName, jndiAlias, jndiProviderUrl );
    }
    catch ( Exception e )
    {
      LOGGER.error( "JNDI binding {} status check failed", jndiBinding.getName(), e );
      throw new KalumetException( "JNDI binding " + jndiBinding.getName() + " status check failed", e );
    }
  }

}
