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
import org.apache.kalumet.model.Environment;
import org.apache.kalumet.model.J2EEApplicationServer;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.model.Software;
import org.apache.kalumet.model.update.UpdateLog;
import org.apache.kalumet.model.update.UpdateMessage;
import org.apache.kalumet.utils.NotifierUtils;
import org.apache.kalumet.utils.PublisherUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * Environment updater.
 */
public class EnvironmentUpdater
{

  private final static transient Logger LOGGER = LoggerFactory.getLogger( EnvironmentUpdater.class );

  /**
   * Updates an environment identified by a given name.
   * The update is forced even if the auto update flag is set to false.
   *
   * @param name the environment name.
   * @throws KalumetException
   */
  public static void update( String name )
    throws KalumetException
  {
    // load the Kalumet configuration
    Kalumet kalumet = Kalumet.digeste( Configuration.CONFIG_LOCATION );
    // get the environment
    Environment environment = kalumet.getEnvironment( name );
    if ( environment == null )
    {
      LOGGER.error( "Environment {} is not found in the configuration", name );
      throw new KalumetException( "Environment " + name + " is not found in the configuration" );
    }
    try
    {
      EnvironmentUpdater.update( environment, true );
    }
    catch ( Exception e )
    {
      throw new KalumetException( e );
    }
  }

  /**
   * Updates an environment.
   *
   * @param environment the environment to update.
   * @throws UpdateException in case of update failure.
   */
  public static void update( Environment environment )
    throws UpdateException
  {
    EnvironmentUpdater.update( environment, false );
  }

  /**
   * Updates an environment.
   *
   * @param environment the environment to update.
   * @param force       true force the update (even if the autoupdate flag is false), false else
   * @throws UpdateException in case of update failure.
   */
  public static void update( Environment environment, boolean force )
    throws UpdateException
  {
    LOGGER.info( "Updating environment {}", environment.getName() );

    LOGGER.debug( "Loading configuration and updating the cache" );
    Kalumet kalumet = null;
    try
    {
      kalumet = Kalumet.digeste( Configuration.CONFIG_LOCATION );
      Configuration.CONFIG_CACHE = kalumet;
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't load configuration", e );
      EventUtils.post( environment, "ERROR", "Can't load configuration: " + e.getMessage() );
      throw new UpdateException( "Can't load configuration", e );
    }

    if ( !force && !environment.isAutoupdate() )
    {
      LOGGER.info( "Update is not forced and environment {} is not auto update", environment.getName() );
      LOGGER.info( "Update is not performed" );
      return;
    }

    LOGGER.debug( "Creating a update logger" );
    UpdateLog updateLog = null;
    try
    {
      updateLog =
        new UpdateLog( "Environment " + environment.getName() + " update in progress ...", environment.getName(),
                       environment );
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't create the update logger", e );
      EventUtils.post( environment, "ERROR", "Can't create the update logger: " + e.getMessage() );
      throw new UpdateException( "Can't create the update logger", e );
    }

    // posting start update event
    EventUtils.post( environment, "UPDATE", "Starting to update ..." );

    LOGGER.info( "Sending a notification and waiting for the update count down" );
    EventUtils.post( environment, "UPDATE", "Sending a notification and waiting for the update count donw" );
    NotifierUtils.waitAndNotify( environment );

    try
    {
      // update softwares flagged "before J2EE"
      LOGGER.info( "Updating softwares flagged before J2EE" );
      for ( Iterator softwareIterator = environment.getSoftwares().iterator(); softwareIterator.hasNext(); )
      {
        Software software = (Software) softwareIterator.next();
        try
        {
          if ( software.isBeforej2ee() )
          {
            SoftwareUpdater.update( environment, software, updateLog );
          }
        }
        catch ( Exception e )
        {
          if ( software.isBlocker() )
          {
            LOGGER.error( "Software {} update failed", software.getName() );
            EventUtils.post( environment, "ERROR",
                             "Software " + software.getName() + " update failed: " + e.getMessage() );
            updateLog.addUpdateMessage(
              new UpdateMessage( "error", "Software " + software.getName() + " update failed: " + e.getMessage() ) );
            updateLog.setStatus( "Environment " + environment.getName() + " update failed" );
            PublisherUtils.publish( environment );
            throw new UpdateException( "Software " + software.getName() + " update failed", e );
          }
          else
          {
            LOGGER.warn( "Software {} update failed", software.getName() );
            updateLog.addUpdateMessage(
              new UpdateMessage( "warn", "Software " + software.getName() + " update failed: " + e.getMessage() ) );
            updateLog.addUpdateMessage( new UpdateMessage( "info", "Software " + software.getName()
              + " is not an update blocker, update continues" ) );
            EventUtils.post( environment, "WARN",
                             "Software " + software.getName() + " update failed: " + e.getMessage() );
            EventUtils.post( environment, "INFO",
                             "Software " + software.getName() + " is not an update blocker, update continues" );
          }
        }
      }

      // update J2EE application servers
      LOGGER.info( "Updating J2EE application servers" );
      for ( Iterator j2eeApplicationServersIterator =
              environment.getJ2EEApplicationServers().getJ2EEApplicationServers().iterator();
            j2eeApplicationServersIterator.hasNext(); )
      {
        J2EEApplicationServer j2eeApplicationServer = (J2EEApplicationServer) j2eeApplicationServersIterator.next();
        try
        {
          J2EEApplicationServerUpdater.update( kalumet, environment, j2eeApplicationServer, updateLog );
        }
        catch ( Exception e )
        {
          if ( j2eeApplicationServer.isBlocker() )
          {
            LOGGER.error( "J2EE application server {} update failed", e );
            EventUtils.post( environment, "ERROR",
                             "J2EE application server " + j2eeApplicationServer.getName() + " update failed: "
                               + e.getMessage() );
            updateLog.addUpdateMessage( new UpdateMessage( "error",
                                                           "J2EE application server " + j2eeApplicationServer.getName()
                                                             + " update failed: " + e.getMessage() ) );
            updateLog.setStatus( "Environment " + environment.getName() + " update failed" );
            PublisherUtils.publish( environment );
            throw new UpdateException( "J2EE application server " + j2eeApplicationServer.getName() + " update failed",
                                       e );
          }
          else
          {
            LOGGER.warn( "J2EE application server {} update failed", e );
            updateLog.addUpdateMessage( new UpdateMessage( "warn",
                                                           "J2EE application server " + j2eeApplicationServer.getName()
                                                             + " update failed: " + e.getMessage() ) );
            updateLog.addUpdateMessage( new UpdateMessage( "info",
                                                           "J2EE application server " + j2eeApplicationServer.getName()
                                                             + " is not an update blocker, update continues" ) );
            EventUtils.post( environment, "WARN",
                             "J2EE application server " + j2eeApplicationServer.getName() + " update failed: "
                               + e.getMessage() );
            EventUtils.post( environment, "INFO", "J2EE application server " + j2eeApplicationServer.getName()
              + " is not an update blocker, update continues" );
          }
        }
      }

      // update softwares
      LOGGER.info( "Updating softwares" );
      for ( Iterator softwaresIterator = environment.getSoftwares().iterator(); softwaresIterator.hasNext(); )
      {
        Software software = (Software) softwaresIterator.next();
        try
        {
          if ( !software.isBeforej2ee() )
          {
            SoftwareUpdater.update( environment, software, updateLog );
          }
        }
        catch ( Exception e )
        {
          if ( software.isBlocker() )
          {
            LOGGER.error( "Software {} update failed", software.getName() );
            EventUtils.post( environment, "ERROR",
                             "Software " + software.getName() + " update failed: " + e.getMessage() );
            updateLog.addUpdateMessage(
              new UpdateMessage( "error", "Software " + software.getName() + " update failed: " + e.getMessage() ) );
            updateLog.setStatus( "Environment " + environment.getName() + " update failed" );
            PublisherUtils.publish( environment );
            throw new UpdateException( "Software " + software.getName() + " update failed", e );
          }
          else
          {
            LOGGER.warn( "Software {} update failed", software.getName() );
            updateLog.addUpdateMessage(
              new UpdateMessage( "warn", "Software " + software.getName() + " update failed: " + e.getMessage() ) );
            updateLog.addUpdateMessage( new UpdateMessage( "info", "Software " + software.getName()
              + " is not an update blocker, update continues" ) );
            EventUtils.post( environment, "WARN",
                             "Software " + software.getName() + " update failed: " + e.getMessage() );
            EventUtils.post( environment, "INFO",
                             "Software " + software.getName() + " is not an update blocker, update continues" );
          }
        }
      }
    }
    catch ( Exception e )
    {
      LOGGER.error( "Update failed", e );
      EventUtils.post( environment, "ERROR", "Update failed: " + e.getMessage() );
      updateLog.setStatus( "Environment " + environment.getName() + " update failed" );
      updateLog.addUpdateMessage( new UpdateMessage( "error", "Update failed: " + e.getMessage() ) );
      LOGGER.info( "Publishing update report" );
      PublisherUtils.publish( environment );
      throw new UpdateException( "Update failed", e );
    }

    // publish update result
    LOGGER.info( "Publishing update report" );
    if ( updateLog.isUpdated() )
    {
      updateLog.setStatus( "Environment " + environment.getName() + " updated" );
    }
    else
    {
      updateLog.setStatus( "Environment " + environment.getName() + " already up to date" );
    }
    updateLog.addUpdateMessage(
      new UpdateMessage( "info", "Environment " + environment.getName() + " update completed" ) );
    EventUtils.post( environment, "UPDATE", "Environment " + environment.getName() + " update completed" );
    LOGGER.info( "Publishing update report" );
    PublisherUtils.publish( environment );

    LOGGER.info( "Update completed" );
  }

}

