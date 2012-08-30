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

import org.apache.kalumet.FileManipulator;
import org.apache.kalumet.FileManipulatorException;
import org.apache.kalumet.KalumetException;
import org.apache.kalumet.agent.Configuration;
import org.apache.kalumet.agent.utils.EventUtils;
import org.apache.kalumet.model.Agent;
import org.apache.kalumet.model.Archive;
import org.apache.kalumet.model.ConfigurationFile;
import org.apache.kalumet.model.ContentManager;
import org.apache.kalumet.model.Database;
import org.apache.kalumet.model.Environment;
import org.apache.kalumet.model.JEEApplication;
import org.apache.kalumet.model.JEEApplicationServer;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.model.update.UpdateLog;
import org.apache.kalumet.model.update.UpdateMessage;
import org.apache.kalumet.utils.NotifierUtils;
import org.apache.kalumet.utils.PublisherUtils;
import org.apache.kalumet.utils.VariableUtils;
import org.apache.kalumet.ws.client.ClientException;
import org.apache.kalumet.ws.client.JEEApplicationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * JEE application updater.
 */
public class JEEApplicationUpdater
{

  private static final transient Logger LOGGER = LoggerFactory.getLogger( JEEApplicationUpdater.class );

  /**
   * Update a JEE application.
   *
   * @param environment the target <code>Environment</code>.
   * @param server      the target <code>JEEApplicationServer</code>.
   * @param application the target <code>JEEApplication</code>.
   * @param updateLog   the <code>UpdateLog</code> to use.
   * @throws UpdateException if the update failed.
   */
  public static void update( Environment environment, JEEApplicationServer server, JEEApplication application,
                             UpdateLog updateLog )
    throws UpdateException
  {
    LOGGER.info( "Updating JEE application {}", application.getName() );

    String applicationUri = VariableUtils.replace( application.getUri(), environment.getVariables() );

    updateLog.addUpdateMessage( new UpdateMessage( "info", "Updating JEE application " + application.getName() ) );
    updateLog.addUpdateMessage(
      new UpdateMessage( "summary", "JEE application " + application.getName() + " located " + applicationUri ) );
    EventUtils.post( environment, "UPDATE", "Updating JEE application " + application.getName() );

    if ( !application.isActive() )
    {
      // the application is inactive, not updated
      LOGGER.info( "JEE application {} is inactive, so not updated", application.getName() );
      updateLog.addUpdateMessage(
        new UpdateMessage( "info", "JEE application " + application.getName() + " is inactive, not updated" ) );
      EventUtils.post( environment, "UPDATE",
                       "JEE application " + application.getName() + " is inactive, not updated" );
      return;
    }

    if ( application.getAgent() != null && application.getAgent().trim().length() > 0 && !application.getAgent().equals(
      Configuration.AGENT_ID ) )
    {
      // delegates the application update to another agent
      LOGGER.info( "Delegating JEE application {} update to agent {}", application.getName(), application.getAgent() );
      Agent delegationAgent = Configuration.CONFIG_CACHE.getAgent( application.getAgent() );
      EventUtils.post( environment, "UPDATE",
                       "Delegating JEE application " + application.getName() + " update to agent "
                         + application.getAgent() );
      updateLog.addUpdateMessage( new UpdateMessage( "info", "Delegating JEE application " + application.getName()
        + " update to agent " + application.getAgent() ) );
      if ( delegationAgent == null )
      {
        LOGGER.error( "Agent " + application.getAgent() + " is not found in the configuration" );
        throw new UpdateException( "Agent " + application.getAgent() + " is not found in the configuration" );
      }
      try
      {
        LOGGER.debug( "Call JEE application WS" );
        JEEApplicationClient webServiceClient =
          new JEEApplicationClient( delegationAgent.getHostname(), delegationAgent.getPort() );
        webServiceClient.update( environment.getName(), server.getName(), application.getName(), true );
      }
      catch ( ClientException clientException )
      {
        LOGGER.error( "JEE application {} update failed", application.getName(), clientException );
        throw new UpdateException( "JEE application " + application.getName() + " update failed", clientException );
      }
      return;
    }

    try
    {
      // create the application directory in the environment working directory
      // (if needed)
      LOGGER.debug( "Creating the JEE application directory" );
      String applicationCacheDir = FileManipulator.createJEEApplicationCacheDir( environment, application );
    }
    catch ( FileManipulatorException e )
    {
      LOGGER.error( "Can't create JEE application cache directory", e );
      throw new UpdateException( "Can't create JEE application cache directory", e );
    }

    // update configuration files
    LOGGER.info( "Updating JEE application configuration files" );
    for ( Iterator configurationFileIterator = application.getConfigurationFiles().iterator();
          configurationFileIterator.hasNext(); )
    {
      ConfigurationFile configurationFile = (ConfigurationFile) configurationFileIterator.next();
      try
      {
        ConfigurationFileUpdater.update( environment, server, application, configurationFile, updateLog );
      }
      catch ( UpdateException updateException )
      {
        // the configuration file update has failed
        if ( configurationFile.isBlocker() )
        {
          // the configuration file is update blocker
          LOGGER.error( "Configuration file {} update failed", configurationFile.getName(), updateException );
          updateLog.addUpdateMessage( new UpdateMessage( "error", "Configuration file " + configurationFile.getName()
            + " update failed: " + updateException.getMessage() ) );
          EventUtils.post( environment, "ERROR",
                           "Configuration file " + configurationFile.getName() + " update failed: "
                             + updateException.getMessage() );
          throw new UpdateException( "Configuration file " + configurationFile.getName() + " update failed",
                                     updateException );
        }
        else
        {
          // the configuration file is not update blocker
          LOGGER.warn( "Configuration file {} update failed", configurationFile.getName(), updateException );
          updateLog.addUpdateMessage( new UpdateMessage( "warn", "Configuration file " + configurationFile.getName()
            + " update failed: " + updateException.getMessage() ) );
          updateLog.addUpdateMessage( new UpdateMessage( "info", "Configuration file " + configurationFile.getName()
            + " is not update blocker, update continues" ) );
          EventUtils.post( environment, "WARN", "Configuration file " + configurationFile.getName() + " update failed: "
            + updateException.getMessage() );
          EventUtils.post( environment, "UPDATE", "Configuration file " + configurationFile.getName()
            + " is not update blocker, update continues" );
        }
      }
    }

    // update database
    LOGGER.info( "Updating JEE application databases" );
    for ( Iterator databaseIterator = application.getDatabases().iterator(); databaseIterator.hasNext(); )
    {
      Database database = (Database) databaseIterator.next();
      try
      {
        DatabaseUpdater.update( environment, server, application, database, updateLog );
      }
      catch ( UpdateException updateException )
      {
        // the database update has failed
        if ( database.isBlocker() )
        {
          // the database is update blocker
          LOGGER.error( "Database {} update failed", database.getName(), updateException );
          updateLog.addUpdateMessage( new UpdateMessage( "error", "Database " + database.getName() + " update failed: "
            + updateException.getMessage() ) );
          EventUtils.post( environment, "ERROR",
                           "Database " + database.getName() + " update failed: " + updateException.getMessage() );
          throw new UpdateException( "Database " + database.getName() + " update failed", updateException );
        }
        else
        {
          // the database is not update blocker
          LOGGER.warn( "Database {} update failed", database.getName(), updateException );
          updateLog.addUpdateMessage( new UpdateMessage( "warn", "Database " + database.getName() + " update failed: "
            + updateException.getMessage() ) );
          updateLog.addUpdateMessage( new UpdateMessage( "info", "Database " + database.getName()
            + " is not update blocker, update continues" ) );
          EventUtils.post( environment, "WARN",
                           "Database " + database.getName() + " update failed: " + updateException.getMessage() );
          EventUtils.post( environment, "UPDATE",
                           "Database " + database.getName() + " is not update blocker, update continues" );
        }
      }
    }

    // update content managers
    LOGGER.info( "Updating JEE application content managers" );
    for ( Iterator contentManagerIterator = application.getContentManagers().iterator();
          contentManagerIterator.hasNext(); )
    {
      ContentManager contentManager = (ContentManager) contentManagerIterator.next();
      try
      {
        ContentManagerUpdater.update( environment, server, application, contentManager, updateLog );
      }
      catch ( UpdateException updateException )
      {
        // the content manager update has failed
        if ( contentManager.isBlocker() )
        {
          // the content manager is update blocker
          LOGGER.error( "Content manager {} update failed", contentManager.getName(), updateException );
          updateLog.addUpdateMessage( new UpdateMessage( "error", "Content manager " + contentManager.getName()
            + " update failed: " + updateException.getMessage() ) );
          EventUtils.post( environment, "ERROR", "Content manager " + contentManager.getName() + " update failed: "
            + updateException.getMessage() );
          throw new UpdateException( "Content manager " + contentManager.getName() + " update failed",
                                     updateException );
        }
        else
        {
          // the content manager is not update blocker
          LOGGER.warn( "Content manager {} update failed", contentManager.getName(), updateException );
          updateLog.addUpdateMessage( new UpdateMessage( "warn", "Content manager " + contentManager.getName()
            + " update failed: " + updateException.getMessage() ) );
          updateLog.addUpdateMessage( new UpdateMessage( "info", "Content manager " + contentManager.getName()
            + " is not update blocker, update continues" ) );
          EventUtils.post( environment, "WARN", "Content manager " + contentManager.getName() + " update failed: "
            + updateException.getMessage() );
          EventUtils.post( environment, "UPDATE",
                           "Content manager " + contentManager.getName() + " is not update blocker, update continues" );
        }
      }
    }

    // update archives
    LOGGER.info( "Updating JEE application archives" );
    for ( Iterator archiveIterator = application.getArchives().iterator(); archiveIterator.hasNext(); )
    {
      Archive archive = (Archive) archiveIterator.next();
      try
      {
        ArchiveUpdater.update( environment, server, application, archive, updateLog );
      }
      catch ( UpdateException updateException )
      {
        // the archive update has failed
        if ( archive.isBlocker() )
        {
          // the archive is update blocker
          LOGGER.error( "Archive {} update failed", archive.getName(), updateException );
          updateLog.addUpdateMessage( new UpdateMessage( "error", "Archive " + archive.getName() + " update failed: "
            + updateException.getMessage() ) );
          EventUtils.post( environment, "ERROR",
                           "Archive " + archive.getName() + " update failed: " + updateException.getMessage() );
          throw new UpdateException( "Archive " + archive.getName() + " update failed", updateException );
        }
        else
        {
          // the archive is not update blocker
          LOGGER.warn( "Archive {} update failed", archive.getName(), updateException );
          updateLog.addUpdateMessage( new UpdateMessage( "warn", "Archive " + archive.getName() + " update failed: "
            + updateException.getMessage() ) );
          updateLog.addUpdateMessage(
            new UpdateMessage( "info", "Archive " + archive.getName() + " is not update blocker, update continues" ) );
          EventUtils.post( environment, "WARN",
                           "Archive " + archive.getName() + " update failed: " + updateException.getMessage() );
          EventUtils.post( environment, "UPDATE",
                           "Archive " + archive.getName() + " is not update blocker, update continues" );
        }
      }
    }

    // JEE application update is completed
    LOGGER.info( "JEE application {} updated", application.getName() );
    updateLog.addUpdateMessage( new UpdateMessage( "info", "JEE application " + application.getName() + " updated" ) );
    EventUtils.post( environment, "UPDATE", "JEE application " + application.getName() + " updated" );
  }

  /**
   * Wrapper method to update a JEE application (via WS).
   *
   * @param environmentName the target environment name.
   * @param serverName      the target JEE application server name.
   * @param applicationName the target JEE application name.
   * @param delegation      flag indicating if the update is a delegation from another agent (true), or a client call (false).
   * @throws KalumetException in case of update error.
   */
  public static void update( String environmentName, String serverName, String applicationName, boolean delegation )
    throws KalumetException
  {
    LOGGER.info( "JEE application {} update requested by WS", applicationName );

    LOGGER.debug( "Loading the configuration" );
    Kalumet kalumet = Kalumet.digeste( Configuration.CONFIG_LOCATION );
    Environment environment = kalumet.getEnvironment( environmentName );
    if ( environment == null )
    {
      LOGGER.error( "The environment {} is not found in the the configuration", environmentName );
      throw new KalumetException( "The environment " + environmentName + " is not found in the the configuration" );
    }
    JEEApplicationServer applicationServer =
      environment.getJEEApplicationServers().getJEEApplicationServer( serverName );
    if ( applicationServer == null )
    {
      LOGGER.error( "The JEE application server {} is not found in the environment {}", serverName, environmentName );
      throw new KalumetException(
        "The JEE application server " + serverName + " is not found in the environment " + environmentName );
    }
    JEEApplication application = applicationServer.getJEEApplication( applicationName );
    if ( application == null )
    {
      LOGGER.error( "The JEE application {} is not found in the JEE application server {}", applicationName,
                    serverName );
      throw new KalumetException(
        "The JEE application " + applicationName + " is not found in the JEE application server " + serverName );
    }

    // update the agent cache
    LOGGER.debug( "Updating configuration cache" );
    Configuration.CONFIG_CACHE = kalumet;

    EventUtils.post( environment, "UPDATE", "JEE application {} update requested by WS", applicationName );
    UpdateLog updateLog =
      new UpdateLog( "JEE application " + applicationName + " update in progress ...", environment.getName(),
                     environment );

    if ( !delegation )
    {
      // it's a client call
      LOGGER.info( "Send a notification and waiting for the count down" );
      EventUtils.post( environment, "UPDATE", "Send a notification and waiting for the count down" );
      NotifierUtils.waitAndNotify( environment );
    }
    try
    {
      LOGGER.debug( "Call JEE application updater" );
      JEEApplicationUpdater.update(environment, applicationServer, application, updateLog);
    }
    catch ( Exception e )
    {
      LOGGER.error( "JEE application {} update failed", applicationName, e );
      EventUtils.post( environment, "ERROR",
                       "JEE application " + applicationName + " udpate failed: " + e.getMessage() );
      if ( !delegation )
      {
        updateLog.setStatus( "JEE application " + applicationName + " update failed" );
        updateLog.addUpdateMessage(
          new UpdateMessage( "error", "JEE application " + applicationName + " update failed: " + e.getMessage() ) );
        PublisherUtils.publish( environment );
      }
      throw new UpdateException( "JEE application " + applicationName + " update failed", e );
    }

    // update completed
    LOGGER.info( "JEE application {} updated", application.getName() );
    EventUtils.post( environment, "UPDATE", "JEE application " + application.getName() + " updated" );
    if ( !delegation )
    {
      if ( updateLog.isUpdated() )
      {
        updateLog.setStatus( "JEE application " + application.getName() + " updated" );
      }
      else
      {
        updateLog.setStatus( "JEE application " + application.getName() + " already up to date" );
      }
      updateLog.addUpdateMessage(
        new UpdateMessage( "info", "JEE application " + application.getName() + " updated" ) );
      LOGGER.info( "Publishing update report" );
      PublisherUtils.publish( environment );
    }
  }

}
