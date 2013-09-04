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
import org.apache.kalumet.model.*;
import org.apache.kalumet.model.JEEApplication;
import org.apache.kalumet.model.update.UpdateLog;
import org.apache.kalumet.model.update.UpdateMessage;
import org.apache.kalumet.utils.NotifierUtils;
import org.apache.kalumet.utils.PublisherUtils;
import org.apache.kalumet.ws.client.ClientException;
import org.apache.kalumet.ws.client.DatabaseClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * Database updater.
 */
public class DatabaseUpdater
{

  private static final transient Logger LOGGER = LoggerFactory.getLogger( DatabaseUpdater.class );

  /**
   * Updates a database.
   *
   * @param environment the target <code>Environment</code>.
   * @param server      the target <code>JEEApplicationServer</code>.
   * @param application the target <code>JEEApplication</code>.
   * @param database    the target <code>Database</code>.
   * @param updateLog   the <code>UpdateLog</code> to use.
   */
  public static void update( Environment environment, JEEApplicationServer server, JEEApplication application,
                             Database database, UpdateLog updateLog )
    throws UpdateException
  {
    LOGGER.info( "Updating database {}", database.getName() );
    updateLog.addUpdateMessage( new UpdateMessage( "info", "Updating database " + database.getName() ) );
    EventUtils.post( environment, "UPDATE", "Updating database " + database.getName() );

    if ( !database.isActive() )
    {
      // database is not active
      LOGGER.info( "Database {} is inactive, so not updated", database.getName() );
      updateLog.addUpdateMessage(
        new UpdateMessage( "info", "Database " + database.getName() + " is inactive, so not updated" ) );
      EventUtils.post( environment, "UPDATE", "Database " + database.getName() + " is inactive, so not updated" );
      return;
    }

    if ( database.getAgent() != null && database.getAgent().trim().length() > 0 && !database.getAgent().equals(
      Configuration.AGENT_ID ) )
    {
      // database update delegated to another agent
      LOGGER.info( "Delegating database {} update to agent {}", database.getName(), database.getAgent() );
      updateLog.addUpdateMessage( new UpdateMessage( "info",
                                                     "Delegating database " + database.getName() + " update to agent "
                                                       + database.getAgent() ) );
      EventUtils.post( environment, "UPDATE",
                       "Delegating database " + database.getName() + " update to agent " + database.getAgent() );
      Agent delegationAgent = Configuration.CONFIG_CACHE.getAgent( database.getAgent() );
      if ( delegationAgent == null )
      {
        // database agent is not found in configuration
        LOGGER.error( "Agent {} is not found in the configuration", database.getAgent() );
        throw new UpdateException( "Agent " + database.getAgent() + " is not found in the configuration" );
      }
      try
      {
        // call WS
        LOGGER.debug( "Calling database WS" );
        DatabaseClient client = new DatabaseClient( delegationAgent.getHostname(), delegationAgent.getPort() );
        client.update( environment.getName(), server.getName(), application.getName(), database.getName(), true );
      }
      catch ( ClientException clientException )
      {
        LOGGER.error( "Database {} update failed", database.getName(), clientException );
        throw new UpdateException( "Database " + database.getName() + " update failed", clientException );
      }
      return;
    }

    // launch SQL scripts on the database
    LOGGER.debug( "Executing SQL scripts" );
    for ( Iterator sqlScriptIterator = database.getSqlScripts().iterator(); sqlScriptIterator.hasNext(); )
    {
      SqlScript sqlScript = (SqlScript) sqlScriptIterator.next();
      try
      {
        SqlScriptUpdater.execute( environment, server, application, database, sqlScript, updateLog );
      }
      catch ( UpdateException updateException )
      {
        // the SQL script execution failed
        if ( sqlScript.isBlocker() )
        {
          // the SQL script is update blocker
          LOGGER.error( "SQL script {} execution failed", sqlScript.getName(), updateException );
          updateLog.addUpdateMessage( new UpdateMessage( "error",
                                                         "SQL script " + sqlScript.getName() + " execution failed: "
                                                           + updateException.getMessage() ) );
          EventUtils.post( environment, "ERROR",
                           "SQL script " + sqlScript.getName() + " execution failed: " + updateException.getMessage() );
          throw new UpdateException( "SQL script " + sqlScript.getName() + " execution failed", updateException );
        }
        else
        {
          // the SQL script is not update blocker
          LOGGER.warn( "SQL script " + sqlScript.getName() + " execution failed", updateException );
          updateLog.addUpdateMessage( new UpdateMessage( "warn",
                                                         "SQL script " + sqlScript.getName() + " execution failed: "
                                                           + updateException.getMessage() ) );
          updateLog.addUpdateMessage( new UpdateMessage( "info", "SQL script " + sqlScript.getName()
            + " is not update blocker, update continues" ) );
          EventUtils.post( environment, "WARN",
                           "SQL script " + sqlScript.getName() + " execution failed: " + updateException.getMessage() );
          EventUtils.post( environment, "UPDATE",
                           "SQL script " + sqlScript.getName() + " is not update blocker, update continues" );
        }
      }
    }

    // update completed
    LOGGER.info( "Database {} updated", database.getName() );
    updateLog.addUpdateMessage( new UpdateMessage( "info", "Database " + database.getName() + " updated" ) );
    EventUtils.post( environment, "UPDATE", "Database " + database.getName() + " updated" );
  }

  /**
   * Wrapper method to update a database via WS.
   *
   * @param environmentName the target environment name.
   * @param serverName      the target JEE application server name.
   * @param applicationName the target JEE application name.
   * @param databaseName    the target database name.
   * @param delegation      true if the call is made by another agent, false if the call is made by a client.
   * @throws KalumetException in case of update failure.
   */
  public static void update( String environmentName, String serverName, String applicationName, String databaseName,
                             boolean delegation )
    throws KalumetException
  {
    LOGGER.info( "Database {} update requested by WS", databaseName );

    // load configuration.
    LOGGER.debug( "Loading configuration" );
    Kalumet kalumet = Kalumet.digeste( Configuration.CONFIG_LOCATION );
    Environment environment = kalumet.getEnvironment( environmentName );
    if ( environment == null )
    {
      LOGGER.error( "Environment {} is not found in the configuration", environmentName );
      throw new KalumetException( "Environment " + environmentName + " is not found in the configuration" );
    }
    JEEApplicationServer applicationServer =
      environment.getJEEApplicationServers().getJEEApplicationServer( serverName );
    if ( applicationServer == null )
    {
      LOGGER.error( "JEE application server {} is not found in environment {}", serverName, environmentName );
      throw new KalumetException(
        "JEE application server " + serverName + " is not found in environment " + environmentName );
    }
    JEEApplication application = applicationServer.getJEEApplication( applicationName );
    if ( application == null )
    {
      LOGGER.error( "JEE application {} is not found in JEE application server {}", applicationName, serverName );
      throw new KalumetException(
        "JEE application " + applicationName + " is not found in JEE application server " + serverName );
    }
    Database database = application.getDatabase( databaseName );
    if ( database == null )
    {
      LOGGER.error( "Database {} is not found in JEE application {}", databaseName, applicationName );
      throw new KalumetException( "Database " + databaseName + " is not found in JEE application " + applicationName );
    }

    // update configuration cache.
    LOGGER.debug( "Updating configuration cache" );
    Configuration.CONFIG_CACHE = kalumet;

    // post journal event
    EventUtils.post( environment, "UPDATE", "Database " + databaseName + " update requested by WS" );
    // create an update logger
    UpdateLog updateLog =
      new UpdateLog( "Database " + databaseName + " update in progress ...", environment.getName(), environment );

    if ( !delegation )
    {
      // the update is requested by a client
      LOGGER.info( "Send a notification and waiting for the count down" );
      EventUtils.post( environment, "UPDATE", "Send a notification and waiting for the count down" );
      NotifierUtils.waitAndNotify( environment );
    }

    try
    {
      // call the updater
      LOGGER.debug( "Call database updater" );
      DatabaseUpdater.update( environment, applicationServer, application, database, updateLog );
    }
    catch ( Exception e )
    {
      LOGGER.error( "Database {} update failed", database.getName(), e );
      EventUtils.post( environment, "ERROR", "Database " + database.getName() + " update failed: " + e.getMessage() );
      if ( !delegation )
      {
        updateLog.setStatus( "Database " + database.getName() + " update failed" );
        updateLog.addUpdateMessage(
          new UpdateMessage( "error", "Database " + database.getName() + " update failed: " + e.getMessage() ) );
        PublisherUtils.publish( environment );
      }
      throw new UpdateException( "Database " + database.getName() + " update failed", e );
    }

    // update completed
    LOGGER.info( "Database {} updated", database.getName() );
    EventUtils.post( environment, "UPDATE", "Database " + database.getName() + " updated" );
    if ( !delegation )
    {
      if ( updateLog.isUpdated() )
      {
        updateLog.setStatus( "Database " + database.getName() + " updated" );
      }
      else
      {
        updateLog.setStatus( "Database " + database.getName() + " already up to date" );
      }
      updateLog.addUpdateMessage( new UpdateMessage( "info", "Database " + database.getName() + " updated" ) );
      LOGGER.info( "Publishing update report" );
      PublisherUtils.publish( environment );
    }
  }

}
