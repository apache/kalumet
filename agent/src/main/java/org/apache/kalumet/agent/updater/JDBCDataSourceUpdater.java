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
import org.apache.kalumet.model.update.UpdateLog;
import org.apache.kalumet.model.update.UpdateMessage;
import org.apache.kalumet.utils.NotifierUtils;
import org.apache.kalumet.utils.PublisherUtils;
import org.apache.kalumet.utils.VariableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JDBC data source updater.
 */
public class JDBCDataSourceUpdater
{

  private static final transient Logger LOGGER = LoggerFactory.getLogger( JDBCDataSourceUpdater.class );

  /**
   * Update a JDBC dataSource.
   *
   * @param environment the target <code>Environment</code>.
   * @param server      the target <code>J2EEApplicationServer</code>.
   * @param dataSource  the target <code>JDBCDataSource</code>.
   * @param updateLog   the <code>UpdateLog</code> to use.
   */
  public static void update( Environment environment, J2EEApplicationServer server, JDBCDataSource dataSource,
                             UpdateLog updateLog )
    throws UpdateException
  {
    LOGGER.info( "Updating JDBC data source {}", dataSource.getName() );
    updateLog.addUpdateMessage( new UpdateMessage( "info", "Updating JDBC data source " + dataSource.getName() ) );
    EventUtils.post( environment, "UPDATE", "Updating JDBC data source " + dataSource.getName() );

    if ( !dataSource.isActive() )
    {
      // the data source is not active
      LOGGER.info( "JDBC data source {} is inactive, so not updated", dataSource.getName() );
      updateLog.addUpdateMessage(
        new UpdateMessage( "info", "JDBC data source " + dataSource.getName() + " is inactive, so not updated" ) );
      EventUtils.post( environment, "UPDATE",
                       "JDBC Data Source " + dataSource.getName() + " is inactive, so not updated" );
      return;
    }

    LOGGER.debug( "Getting the JDBC connection pool {}", dataSource.getPool() );
    JDBCConnectionPool connectionPool = server.getJDBCConnectionPool( dataSource.getPool() );
    if ( connectionPool == null )
    {
      LOGGER.error( "JDBC connection pool {} is not found in the configuration", dataSource.getPool() );
      throw new UpdateException(
        "JDBC connection pool " + dataSource.getPool() + " is not found in the configuration" );
    }
    J2EEApplicationServerController controller = null;
    try
    {
      LOGGER.debug( "Connecting to J2EE application server controller" );
      controller = J2EEApplicationServerControllerFactory.getController( environment, server );
    }
    catch ( KalumetException e )
    {
      LOGGER.error( "Can't connect to J2EE application server {} controller", server.getName(), e );
      throw new UpdateException( "Can't connect to J2EE application server " + server.getName() + " controller", e );
    }
    // replace variables in the JDBC URL and helper class name
    LOGGER.debug( "Replacing variables in JDBC URL and helper class" );
    String jdbcUrl = VariableUtils.replace( connectionPool.getUrl(), environment.getVariables() );
    String helperClass = VariableUtils.replace( connectionPool.getHelperclass(), environment.getVariables() );
    try
    {
      if ( controller.isJDBCDataSourceDeployed( dataSource.getName() ) )
      {
        // JDBC data source already deployed in the JEE application server
        LOGGER.info( "JDBC data source " + dataSource.getName() + " already deployed, checking for update" );
        if ( controller.updateJDBCDataSource( dataSource.getName(), connectionPool.getName(), jdbcUrl, helperClass ) )
        {
          // JDBC data source has been updated
          LOGGER.info( "JDBC data source {} updated", dataSource.getName() );
          updateLog.setStatus( "Update performed" );
          updateLog.setUpdated( true );
          updateLog.addUpdateMessage(
            new UpdateMessage( "info", "JDBC data source " + dataSource.getName() + " updated" ) );
          EventUtils.post( environment, "UPDATE", "JDBC data source " + dataSource.getName() + " updated" );
        }
      }
      else
      {
        // JDBC data source is not deployed, deploy it
        LOGGER.debug( "JDBC data source {} is not deployed, deploying it", dataSource.getName() );
        controller.deployJDBCDataSource( dataSource.getName(), connectionPool.getName(), jdbcUrl, helperClass );
        updateLog.setStatus( "Update performed" );
        updateLog.setUpdated( true );
        updateLog.addUpdateMessage(
          new UpdateMessage( "info", "JDBC data source " + dataSource.getName() + " deployed" ) );
        EventUtils.post( environment, "UPDATE", "JDBC data source " + dataSource.getName() + " deployed" );
        LOGGER.info( "JDBC data source {} deployed", dataSource.getName() );
      }
    }
    catch ( Exception e )
    {
      LOGGER.error( "JDBC data source {} update failed", dataSource.getName(), e );
      throw new UpdateException( "JDBC data source " + dataSource.getName() + " update failed", e );
    }
  }

  /**
   * Wrapper method to update a JDBC data source via WS.
   *
   * @param environmentName the target environment name.
   * @param serverName      the target J2EE application server name.
   * @param dataSourceName  the target JDBC data source name.
   * @throws KalumetException in case of update failure.
   */
  public static void update( String environmentName, String serverName, String dataSourceName )
    throws KalumetException
  {
    LOGGER.info( "JDBC data source {} update requested by WS", dataSourceName );

    // load configuration
    LOGGER.debug( "Loading configuration" );
    Kalumet kalumet = Kalumet.digeste( Configuration.CONFIG_LOCATION );

    // looking for component objects
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
    JDBCDataSource dataSource = applicationServer.getJDBCDataSource( dataSourceName );
    if ( dataSource == null )
    {
      LOGGER.error( "JDBC data source {} is not found in J2EE application server {}", dataSourceName,
                    applicationServer.getName() );
      throw new KalumetException( "JDBC data source " + dataSourceName + " is not found in J2EE application server "
                                    + applicationServer.getName() );
    }

    // post event and create update log
    EventUtils.post( environment, "UPDATE", "JDBC data source " + dataSource.getName() + " update requested by WS" );
    UpdateLog updateLog =
      new UpdateLog( "JDBC data source " + dataSource.getName() + " update in progress ...", environment.getName(),
                     environment );

    // send a notification and waiting for the count down
    EventUtils.post( environment, "UPDATE", "Send a notification and waiting for the count down" );
    NotifierUtils.waitAndNotify( environment );

    try
    {
      // call the updater
      LOGGER.debug( "Call JDBC data source updater" );
      JDBCDataSourceUpdater.update( environment, applicationServer, dataSource, updateLog );
    }
    catch ( Exception e )
    {
      LOGGER.error( "JDBC data source {} update failed", dataSource.getName(), e );
      EventUtils.post( environment, "ERROR",
                       "JDBC data source " + dataSource.getName() + " update failed: " + e.getMessage() );
      updateLog.setStatus( "JDBC data source " + dataSource.getName() + " update failed" );
      updateLog.addUpdateMessage( new UpdateMessage( "error",
                                                     "JDBC data source " + dataSource.getName() + " update failed: "
                                                       + e.getMessage() ) );
      PublisherUtils.publish( environment );
      throw new KalumetException( "JDBC data source " + dataSource.getName() + " update failed", e );
    }

    // update completed
    LOGGER.info( "JDBC data source {} updated", dataSource.getName() );
    EventUtils.post( environment, "UPDATE", "JDBC data source " + dataSource.getName() + " updated" );
    if ( updateLog.isUpdated() )
    {
      updateLog.setStatus( "JDBC data source " + dataSource.getName() + " updated" );
    }
    else
    {
      updateLog.setStatus( "JDBC data source " + dataSource.getName() + " already up to date" );
    }
    updateLog.addUpdateMessage( new UpdateMessage( "info", "JMS data source " + dataSource.getName() + " updated" ) );
    LOGGER.info( "Publishing update report" );
    PublisherUtils.publish( environment );
  }

  /**
   * Wrapper method to check JDBC data source via WS.
   *
   * @param environmentName the target environment name.
   * @param serverName      the target J2EE application server name.
   * @param dataSourceName  the target JDBC data source name.
   * @return true if the JDBC data source is up to date, false else.
   * @throws KalumetException in case of check failure.
   */
  public static boolean check( String environmentName, String serverName, String dataSourceName )
    throws KalumetException
  {
    LOGGER.info( "JDBC data source {} status check requested by WS", dataSourceName );

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
    J2EEApplicationServer applicationServer =
      environment.getJ2EEApplicationServers().getJ2EEApplicationServer( serverName );
    if ( applicationServer == null )
    {
      LOGGER.error( "J2EE application server {} is not found in environment {}", serverName, environment.getName() );
      throw new KalumetException(
        "J2EE application server " + serverName + " is not found in environment " + environment.getName() );
    }
    JDBCDataSource dataSource = applicationServer.getJDBCDataSource( dataSourceName );
    if ( dataSource == null )
    {
      LOGGER.error( "JDBC data source {} is not found in J2EE application server {}", dataSourceName,
                    applicationServer.getName() );
      throw new KalumetException( "JDBC data source " + dataSourceName + " is not found in J2EE application server "
                                    + applicationServer.getName() );
    }

    // post an event
    EventUtils.post( environment, "INFO",
                     "JDBC data source " + dataSource.getName() + " status check requested by WS" );

    // Get JDBC data source connection pool.
    LOGGER.debug( "Getting JDBC data source connection pool" );
    JDBCConnectionPool connectionPool = applicationServer.getJDBCConnectionPool( dataSource.getPool() );
    if ( connectionPool == null )
    {
      LOGGER.error( "JDBC connection pool {} is not found in J2EE application server {}", dataSource.getPool(),
                    applicationServer.getName() );
      throw new KalumetException(
        "JDBC connection pool " + dataSource.getPool() + " is not found in J2EE application server "
          + applicationServer.getName() );
    }

    try
    {
      // connecting to J2EE application server controller
      LOGGER.debug( "Connecting to J2EE application server controller" );
      J2EEApplicationServerController controller =
        J2EEApplicationServerControllerFactory.getController( environment, applicationServer );
      // replace variables in the JDBC URL and helper class name
      LOGGER.debug( "Replacing variables in JDBC URL and helper class" );
      String jdbcUrl = VariableUtils.replace( connectionPool.getUrl(), environment.getVariables() );
      String helperClass = VariableUtils.replace( connectionPool.getHelperclass(), environment.getVariables() );
      // check the data source
      return controller.isJDBCDataSourceUpToDate( dataSource.getName(), dataSource.getPool(), jdbcUrl, helperClass );
    }
    catch ( Exception e )
    {
      LOGGER.error( "JDBC data source {} check failed", dataSource.getName(), e );
      throw new KalumetException( "JDBC data source " + dataSource.getName() + " check failed", e );
    }
  }

}
