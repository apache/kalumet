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
import org.apache.kalumet.controller.core.J2EEApplicationServerController;
import org.apache.kalumet.controller.core.J2EEApplicationServerControllerFactory;
import org.apache.kalumet.model.Environment;
import org.apache.kalumet.model.J2EEApplicationServer;
import org.apache.kalumet.model.JDBCConnectionPool;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.model.update.UpdateLog;
import org.apache.kalumet.model.update.UpdateMessage;
import org.apache.kalumet.utils.NotifierUtils;
import org.apache.kalumet.utils.PublisherUtils;
import org.apache.kalumet.utils.VariableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JDBC connection pool updater.
 */
public class JDBCConnectionPoolUpdater
{

  private static final transient Logger LOGGER = LoggerFactory.getLogger( JDBCConnectionPoolUpdater.class );

  /**
   * Update a JDBC connection pool.
   *
   * @param environment    the target <code>Environment</code>.
   * @param server         the target <code>J2EEApplicationServer</code>.
   * @param connectionPool the target <code>JDBCConnectionPool</code>.
   * @param updateLog      the target <code>UpdateLog</code> to use.
   */
  public static void update( Environment environment, J2EEApplicationServer server, JDBCConnectionPool connectionPool,
                             UpdateLog updateLog )
    throws UpdateException
  {
    LOGGER.info( "Updating JDBC connection pool {}", connectionPool.getName() );
    updateLog.addUpdateMessage(
      new UpdateMessage( "info", "Updating JDBC connection pool " + connectionPool.getName() ) );
    EventUtils.post( environment, "UPDATE", "Updating JDBC connection pool " + connectionPool.getName() );

    if ( !connectionPool.isActive() )
    {
      LOGGER.info( "JDBC connection pool {} is inactive, so not updated", connectionPool.getName() );
      updateLog.addUpdateMessage( new UpdateMessage( "info", "JDBC connection pool " + connectionPool.getName()
        + " is inactive, so not updated" ) );
      EventUtils.post( environment, "UPDATE",
                       "JDBC connection pool " + connectionPool.getName() + " is inactive, so not updated" );
      return;
    }

    // replace variables in connection pool data
    LOGGER.debug( "Replacing variables in connection pool data" );
    String jdbcDriver = VariableUtils.replace( connectionPool.getDriver(), environment.getVariables() );
    String jdbcUser = VariableUtils.replace( connectionPool.getUser(), environment.getVariables() );
    String jdbcPassword = VariableUtils.replace( connectionPool.getPassword(), environment.getVariables() );
    String jdbcUrl = VariableUtils.replace( connectionPool.getUrl(), environment.getVariables() );
    String jdbcClasspath = VariableUtils.replace( connectionPool.getClasspath(), environment.getVariables() );

    try
    {
      // connect to J2EE application server controller
      J2EEApplicationServerController controller =
        J2EEApplicationServerControllerFactory.getController( environment, server );
      // test if the JDBC connection pool is already present in the JEE server
      if ( controller.isJDBCConnectionPoolDeployed( connectionPool.getName() ) )
      {
        LOGGER.info( "JDBC connection pool {} already deployed, checking for update" );
        if ( controller.updateJDBCConnectionPool( connectionPool.getName(), jdbcDriver, connectionPool.getIncrement(),
                                                  connectionPool.getInitial(), connectionPool.getMaximal(), jdbcUser,
                                                  jdbcPassword, jdbcUrl, jdbcClasspath ) )
        {
          updateLog.setStatus( "Update performed" );
          updateLog.setUpdated( true );
          updateLog.addUpdateMessage(
            new UpdateMessage( "info", "JDBC connection pool " + connectionPool.getName() + " updated" ) );
          EventUtils.post( environment, "UPDATE", "JDBC connection pool " + connectionPool.getName() + " updated" );
          LOGGER.info( "JDBC connection pool {} updated", connectionPool.getName() );
        }
        else
        {
          updateLog.addUpdateMessage(
            new UpdateMessage( "info", "JDBC connection pool " + connectionPool.getName() + " already up to date" ) );
          EventUtils.post( environment, "UPDATE",
                           "JDBC connection pool " + connectionPool.getName() + " already up to date" );
          LOGGER.info( "JDBC connection pool {} already up to date", connectionPool.getName() );
        }
      }
      else
      {
        // deploy the JDBC connection pool
        controller.deployJDBCConnectionPool( connectionPool.getName(), jdbcDriver, connectionPool.getIncrement(),
                                             connectionPool.getInitial(), connectionPool.getMaximal(), jdbcUser,
                                             jdbcPassword, jdbcUrl, jdbcClasspath );
        updateLog.setStatus( "Update performed" );
        updateLog.setUpdated( true );
        updateLog.addUpdateMessage(
          new UpdateMessage( "info", "JDBC connection pool " + connectionPool.getName() + " deployed" ) );
        EventUtils.post( environment, "UPDATE", "JDBC connection pool " + connectionPool.getName() + " deployed" );
        LOGGER.info( "JDBC connection pool {} deployed", connectionPool.getName() );
      }
    }
    catch ( Exception e )
    {
      LOGGER.error( "JDBC connection pool {} update failed", connectionPool.getName(), e );
      throw new UpdateException( "JDBC connection pool " + connectionPool.getName() + " update failed", e );
    }
  }

  /**
   * Wrapper method to update a JDBC connection pool via WS.
   *
   * @param environmentName    the target environment name.
   * @param serverName         the target J2EE application server name.
   * @param connectionPoolName the target JDBC connection pool name.
   * @throws KalumetException in case of update failure.
   */
  public static void update( String environmentName, String serverName, String connectionPoolName )
    throws KalumetException
  {
    LOGGER.info( "JDBC connection pool {} update requested by WS", connectionPoolName );

    // load configuration.
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
    J2EEApplicationServer applicationServer =
      environment.getJ2EEApplicationServers().getJ2EEApplicationServer( serverName );
    if ( applicationServer == null )
    {
      LOGGER.error( "J2EE application server {} is not found in environment {}", serverName, environment.getName() );
      throw new KalumetException(
        "J2EE application server " + serverName + " is not found in environment " + environment.getName() );
    }
    JDBCConnectionPool connectionPool = applicationServer.getJDBCConnectionPool( connectionPoolName );
    if ( connectionPool == null )
    {
      LOGGER.error( "JDBC connection pool {} is not found in J2EE application server {}", connectionPoolName,
                    applicationServer.getName() );
      throw new KalumetException(
        "JDBC connection pool " + connectionPoolName + " is not found in J2EE application server "
          + applicationServer.getName() );
    }

    // post event and create update log
    EventUtils.post( environment, "UPDATE",
                     "JDBC connection pool " + connectionPool.getName() + " update request by WS" );
    UpdateLog updateLog =
      new UpdateLog( "JDBC connection pool " + connectionPool.getName() + " update in progress", environment.getName(),
                     environment );

    // send a notification and waiting for the count down.
    LOGGER.info( "Send a notification and waiting for the count down" );
    EventUtils.post( environment, "UPDATE", "Send a notification and waiting for the count down" );
    NotifierUtils.waitAndNotify( environment );

    try
    {
      // call the JDBC connection pool updater.
      LOGGER.debug( "Call connection pool updater" );
      JDBCConnectionPoolUpdater.update( environment, applicationServer, connectionPool, updateLog );
    }
    catch ( Exception e )
    {
      LOGGER.error( "JDBC connection pool {} update failed", connectionPool.getName(), e );
      EventUtils.post( environment, "ERROR",
                       "JDBC connection pool " + connectionPool.getName() + " update failed: " + e.getMessage() );
      updateLog.setStatus( "JDBC connection pool " + connectionPool.getName() + " update failed" );
      updateLog.addUpdateMessage( new UpdateMessage( "error", "JDBC connection pool " + connectionPool.getName()
        + " update failed: " + e.getMessage() ) );
      PublisherUtils.publish( environment );
      throw new UpdateException( "JDBC connection pool " + connectionPool.getName() + " update failed", e );
    }

    // update completed.
    LOGGER.info( "JDBC connection pool {} updated", connectionPool.getName() );
    EventUtils.post( environment, "UPDATE", "JDBC connection pool " + connectionPool.getName() + " updated" );
    if ( updateLog.isUpdated() )
    {
      updateLog.setStatus( "JDBC connection pool " + connectionPool.getName() + " updated" );
    }
    else
    {
      updateLog.setStatus( "JDBC connection pool " + connectionPool.getName() + " is already up to date" );
    }
    updateLog.addUpdateMessage(
      new UpdateMessage( "info", "JDBC connection pool " + connectionPool.getName() + " updated" ) );
    LOGGER.info( "Publishing update report" );
    PublisherUtils.publish( environment );
  }

  /**
   * Wrapper method to check if a JDBC connection pool is up to date via WS.
   *
   * @param environmentName    the target environment name.
   * @param serverName         the target J2EE application server name.
   * @param connectionPoolName the target JDBC connection pool name.
   * @return true if the JDBC connection pool is up to date, false else.
   * @throws KalumetException in case of check failure.
   */
  public static boolean check( String environmentName, String serverName, String connectionPoolName )
    throws KalumetException
  {
    LOGGER.info( "JDBC connection pool {} status check requested by WS", connectionPoolName );

    // load configuration.
    LOGGER.debug( "Loading configuration" );
    Kalumet kalumet = Kalumet.digeste( Configuration.CONFIG_LOCATION );

    // looking for component objects
    LOGGER.debug( "Looking for component objects." );
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
    JDBCConnectionPool connectionPool = applicationServer.getJDBCConnectionPool( connectionPoolName );
    if ( connectionPool == null )
    {
      LOGGER.error( "JDBC connection pool {} is not found in J2EE server {}", connectionPoolName,
                    applicationServer.getName() );
      throw new KalumetException(
        "JDBC connection pool " + connectionPoolName + " is not found in J2EE application server "
          + applicationServer.getName() );
    }

    // post an event
    EventUtils.post( environment, "INFO",
                     "JDBC connection pool " + connectionPool.getName() + " status check requested by WS" );

    try
    {
      // get the JEE server JMX controller.
      LOGGER.debug( "Getting the J2EE application server controller" );
      J2EEApplicationServerController controller =
        J2EEApplicationServerControllerFactory.getController( environment, applicationServer );
      // replace values with environment variables
      LOGGER.debug( "Replacing variables in connection pool data" );
      String jdbcDriver = VariableUtils.replace( connectionPool.getDriver(), environment.getVariables() );
      String jdbcUser = VariableUtils.replace( connectionPool.getUser(), environment.getVariables() );
      String jdbcPassword = VariableUtils.replace( connectionPool.getPassword(), environment.getVariables() );
      String jdbcUrl = VariableUtils.replace( connectionPool.getUrl(), environment.getVariables() );
      String jdbcClasspath = VariableUtils.replace( connectionPool.getClasspath(), environment.getVariables() );
      // check JDBC connection pool using JMX controller
      LOGGER.debug( "Checking JDBC connection pool using JMX controller." );
      return controller.isJDBCConnectionPoolUpToDate( connectionPool.getName(), jdbcDriver,
                                                      connectionPool.getIncrement(), connectionPool.getInitial(),
                                                      connectionPool.getMaximal(), jdbcUser, jdbcPassword, jdbcUrl,
                                                      jdbcClasspath );
    }
    catch ( Exception e )
    {
      LOGGER.error( "JDBC connection pool {} check failed", connectionPool.getName(), e );
      throw new KalumetException( "JDBC connection pool " + connectionPool.getName() + " check failed", e );
    }
  }

}
