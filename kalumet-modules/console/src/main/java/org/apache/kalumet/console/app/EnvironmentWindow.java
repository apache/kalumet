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
package org.apache.kalumet.console.app;

import nextapp.echo2.app.Button;
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.SplitPane;
import nextapp.echo2.app.WindowPane;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.extras.app.TabPane;
import nextapp.echo2.extras.app.layout.TabPaneLayoutData;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.kalumet.console.configuration.ConfigurationManager;
import org.apache.kalumet.model.Agent;
import org.apache.kalumet.model.Environment;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.model.log.Event;
import org.apache.kalumet.model.log.Journal;
import org.apache.kalumet.ws.client.EnvironmentClient;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Environment window.
 */
public class EnvironmentWindow
  extends WindowPane
{

  private String environmentName;

  private Environment environment;

  private LinkedList changeEvents;

  private boolean updated = false;

  private Button lockButton;

  public boolean adminPermission = false;

  public boolean updatePermission = false;

  public boolean jeeApplicationServersChangePermission = false;

  public boolean jeeApplicationServersUpdatePermission = false;

  public boolean jeeApplicationServersControlPermission = false;

  public boolean jeeResourcesChangePermission = false;

  public boolean jeeResourcesUpdatePermission = false;

  public boolean jeeApplicationsChangePermission = false;

  public boolean jeeApplicationsUpdatePermission = false;

  public boolean softwareChangePermission = false;

  public boolean softwareUpdatePermission = false;

  public boolean releasePermission = false;

  public boolean shellPermission = false;

  public boolean browserPermission = false;

  public boolean homepagePermission = false;

  private GeneralPane generalPane;

  private SecurityPane securityPane;

  private ApplicationServersPane applicationServersPane;

  private ConnectionPoolsPane connectionPoolsPane;

  private DataSourcesPane dataSourcesPane;

  private ConnectionFactoriesPane connectionFactoriesPane;

  private JmsServersPane jmsServersPane;

  private NameSpaceBindingsPane nameSpaceBindingsPane;

  private SharedLibrariesPane sharedLibrariesPane;

  private ApplicationsPane applicationsPane;

  private SoftwaresPane softwaresPane;

  private JournalPane journalPane;

  private NotifiersPane notifiersPane;

  private PublishersPane publishersPane;

  private ActionPane actionPane;

  private CheckerPane checkerPane;

  private ShellPane shellPane;

  private LogViewerPane logViewerPane;

  private FileBrowserPane fileBrowserane;

  private StatisticsPane statisticsPane;

  // update thread
  class UpdateThread
    extends Thread
  {

    public boolean ended = false;

    public boolean failure = false;

    public String message;

    public void run()
    {
      try
      {
        // load Kalumet configuration
        Kalumet kalumet = ConfigurationManager.loadStore();
        // looking for the agent
        Agent agent = kalumet.getAgent( environment.getAgent() );
        if ( agent == null )
        {
          throw new IllegalArgumentException( "agent not found." );
        }
        // call the webservice
        EnvironmentClient client = new EnvironmentClient( agent.getHostname(), agent.getPort() );
        client.update( environmentName );
      }
      catch ( Exception e )
      {
        failure = true;
        message = "Environment " + environmentName + " update failed: " + e.getMessage();
      }
      finally
      {
        ended = true;
      }
    }
  }

  // close
  private ActionListener close = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // if the user is only read-only
      if ( environmentName == null || ( !adminPermission && !updatePermission && !jeeApplicationServersChangePermission
        && !jeeApplicationServersUpdatePermission && !jeeApplicationServersControlPermission && !jeeResourcesChangePermission
        && !jeeResourcesUpdatePermission && !jeeApplicationsChangePermission && !jeeApplicationsUpdatePermission
        && !softwareChangePermission && !softwareUpdatePermission && !releasePermission && !homepagePermission ) )
      {
        // only close the window
        EnvironmentWindow.this.userClose();
        return;
      }
      // check if some changes has been made
      if ( isUpdated() )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "environment.notsaved" ), environmentName );
        return;
      }
      // load Kalumet configuration
      Kalumet kalumet = null;
      try
      {
        kalumet = ConfigurationManager.loadStore();
      }
      catch ( Exception e )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addError(
          Messages.getString( "db.read" ) + ": " + e.getMessage(), environmentName );
        return;
      }
      // looking for the environment object (updated)
      Environment current = kalumet.getEnvironment( environmentName );
      // raise an error if the environment is not found
      if ( current == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "environment.notsaved" ), environmentName );
        return;
      }
      // check if the user has the lock
      if ( current.getLock().equals( KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        current.setLock( "" );
        try
        {
          ConfigurationManager.writeStore( kalumet );
        }
        catch ( Exception e )
        {
          KalumetConsoleApplication.getApplication().getLogPane().addError(
            Messages.getString( "db.write" ) + ": " + e.getMessage(), environmentName );
          return;
        }
      }
      // close the window
      EnvironmentWindow.this.userClose();
    }
  };

  // toggle lock
  private ActionListener toggleLock = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if some change has been made
      if ( isUpdated() )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "environment.notsaved" ), environmentName );
        return;
      }
      // load Kalumet configuration
      Kalumet kalumet;
      try
      {
        kalumet = ConfigurationManager.loadStore();
      }
      catch ( Exception e )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addError(
          Messages.getString( "db.read" ) + ": " + e.getMessage(), environmentName );
        return;
      }
      // get the environment object (updated)
      Environment current = kalumet.getEnvironment( environmentName );
      // raise an error if the environment is not found
      if ( current == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "environment.notsaved" ), environmentName );
        return;
      }
      // if the lock is free, take it if I can
      if ( ( adminPermission || updatePermission || jeeApplicationServersChangePermission || jeeApplicationServersUpdatePermission
        || jeeApplicationServersControlPermission || jeeResourcesChangePermission || jeeResourcesUpdatePermission
        || jeeApplicationsChangePermission || jeeApplicationsUpdatePermission || softwareChangePermission
        || softwareUpdatePermission || releasePermission || homepagePermission ) && ( current.getLock() == null
        || current.getLock().trim().length() < 1 ) )
      {
        current.setLock( KalumetConsoleApplication.getApplication().getUserid() );
        environment = current;
        EnvironmentWindow.this.setTitle(
          Messages.getString( "environment" ) + " " + environmentName + " (" + Messages.getString( "locked.by" ) + " "
            + environment.getLock() + ")" );
        try
        {
          ConfigurationManager.writeStore( kalumet );
        }
        catch ( Exception e )
        {
          KalumetConsoleApplication.getApplication().getLogPane().addError(
            Messages.getString( "db.write" ) + ": " + e.getMessage(), environmentName );
          return;
        }
        // update the view
        update();
        return;
      }
      // if the user has the lock, toggle to unlock
      if ( KalumetConsoleApplication.getApplication().getUserid().equals( "admin" ) || current.getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        current.setLock( "" );
        environment = current;
        EnvironmentWindow.this.setTitle( Messages.getString( "environment" ) + " " + environmentName );
        try
        {
          ConfigurationManager.writeStore( kalumet );
        }
        catch ( Exception e )
        {
          KalumetConsoleApplication.getApplication().getLogPane().addError(
            Messages.getString( "db.write" ) + ": " + e.getMessage(), environmentName );
          return;
        }
        // update the view
        update();
        return;
      }
    }
  };

  // refresh
  private ActionListener refresh = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // load Kalumet configuration
      Kalumet kalumet = null;
      try
      {
        kalumet = ConfigurationManager.loadStore();
      }
      catch ( Exception e )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addError(
          Messages.getString( "db.read" ) + ": " + e.getMessage(), environmentName );
        return;
      }
      // looking for the environment in Kalumet
      environment = kalumet.getEnvironment( environmentName );
      if ( environment == null )
      {
        environment = new Environment();
        environment.setLock( KalumetConsoleApplication.getApplication().getUserid() );
      }
      // change the updated flag
      setUpdated( false );
      // reinit the change events
      changeEvents = new LinkedList();
      // update the window
      update();
    }
  };

  // save
  private ActionListener save = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if the user has the environment lock
      if ( !environment.getLock().equals( KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            environmentName );
        return;
      }
      // get fields value
      String nameFieldValue = generalPane.getNameField().getText();
      String groupFieldValue = generalPane.getGroupField().getText();
      String tagFieldValue = (String) generalPane.getTagField().getSelectedItem();
      String agentFieldValue = (String) generalPane.getAgentField().getSelectedItem();
      int autoUpdateFieldIndex = generalPane.getAutoUpdateField().getSelectedIndex();
      String notesAreaValue = generalPane.getNotesArea().getText();
      String weblinksAreaValue = generalPane.getWeblinksArea().getText();
      int applicationServersTopologyFieldIndex = applicationServersPane.getTopologyField().getSelectedIndex();
      String notifierCountDownFieldValue = notifiersPane.getCountDownField().getText();
      // check fields
      if ( nameFieldValue == null || nameFieldValue.trim().length() < 1 || groupFieldValue == null
        || groupFieldValue.trim().length() < 1 )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "environment.mandatory" ), environmentName );
        return;
      }
      // check if the count down is a integer
      int notifierCountDownInt;
      try
      {
        notifierCountDownInt = new Integer( notifierCountDownFieldValue ).intValue();
      }
      catch ( Exception e )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "notifier.warn.countdown" ), environmentName );
        notifierCountDownInt = 0;
      }
      // load the journal
      Journal journal = null;
      try
      {
        journal = ConfigurationManager.loadEnvironmentJournal( environmentName );
      }
      catch ( Exception e )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "journal.warn.read" ) + ": " + e.getMessage(), environmentName );
        return;
      }
      // load Kalumet configuration
      Kalumet kalumet = null;
      try
      {
        kalumet = ConfigurationManager.loadStore();
      }
      catch ( Exception e )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addError(
          Messages.getString( "db.read" ) + ": " + e.getMessage(), environmentName );
        return;
      }
      // if the user change the environment name, check if the name is not
      // already in used
      if ( environmentName == null || ( environmentName != null && !environmentName.equals( nameFieldValue ) ) )
      {
        if ( kalumet.getEnvironment( nameFieldValue ) != null )
        {
          KalumetConsoleApplication.getApplication().getLogPane().addError( Messages.getString( "environment.exists" ),
                                                                            environmentName );
          return;
        }
      }
      // add a change event
      if ( environmentName != null && !environmentName.equals( nameFieldValue ) )
      {
        getChangeEvents().add( "Change environment name." );
      }
      // update the environment
      environment.setName( nameFieldValue );
      environment.setGroup( groupFieldValue );
      environment.setTag( tagFieldValue );
      environment.setAgent( agentFieldValue );
      if ( autoUpdateFieldIndex == 0 )
      {
        environment.setAutoupdate( true );
      }
      else
      {
        environment.setAutoupdate( false );
      }
      environment.setNotes( notesAreaValue );
      environment.setWeblinks( weblinksAreaValue );
      if ( applicationServersTopologyFieldIndex == 0 )
      {
        environment.getJEEApplicationServers().setCluster( false );
      }
      else
      {
        environment.getJEEApplicationServers().setCluster( true );
      }
      environment.getNotifiers().setCountdown( notifierCountDownInt );
      // looking for the environment
      Environment toupdate = kalumet.getEnvironment( environmentName );
      if ( toupdate == null || environmentName == null )
      {
        // add the environment object if needed
        try
        {
          kalumet.addEnvironment( environment );
        }
        catch ( Exception e )
        {
          KalumetConsoleApplication.getApplication().getLogPane().addWarning(
            Messages.getString( "environment.exists" ), environmentName );
          return;
        }
      }
      else
      {
        // update the environment
        int index = kalumet.getEnvironments().indexOf( toupdate );
        kalumet.getEnvironments().set( index, environment );
      }
      // save the configuration
      try
      {
        ConfigurationManager.writeStore( kalumet );
      }
      catch ( Exception e )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addError(
          Messages.getString( "db.write" ) + ": " + e.getMessage(), environmentName );
        return;
      }
      // update the window definition
      if ( environment.getLock() == null || environment.getLock().trim().length() < 1 )
      {
        setTitle( Messages.getString( "environment" ) + " " + environment.getName() );
      }
      else
      {
        setTitle(
          Messages.getString( "environment" ) + " " + environment.getName() + " (" + Messages.getString( "locked.by" )
            + " " + environment.getLock() + ")" );
      }
      setId( "environmentwindow_" + environment.getName() );
      environmentName = environment.getName();
      // add change events in the journal
      for ( Iterator eventIterator = getChangeEvents().iterator(); eventIterator.hasNext(); )
      {
        String eventMessage = (String) eventIterator.next();
        Event journalEvent = new Event();
        journalEvent.setDate( ( (FastDateFormat) DateFormatUtils.ISO_DATETIME_FORMAT ).format( new Date() ) );
        journalEvent.setSeverity( "INFO" );
        journalEvent.setAuthor( KalumetConsoleApplication.getApplication().getUserid() );
        journalEvent.setContent( eventMessage );
        journal.addEvent( journalEvent );
      }
      // save the journal
      try
      {
        journal.writeXMLFile( ConfigurationManager.getEnvironmentJournalFile( environmentName ) );
      }
      catch ( Exception e )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "journal.warn.save" ) + ": " + e.getMessage(), environmentName );
        return;
      }
      // update the updated flag
      setUpdated( false );
      // update the change events
      changeEvents = new LinkedList();
      // update the environments pane
      KalumetConsoleApplication.getApplication().getEnvironmentsPane().update();
      // update the window
      update();
      // add a confirm
      KalumetConsoleApplication.getApplication().getLogPane().addConfirm( Messages.getString( "environment.saved" ),
                                                                          environmentName );
    }
  };

  // copy
  private ActionListener copy = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      try
      {
        KalumetConsoleApplication.getApplication().setCopyComponent( environment.clone() );
      }
      catch ( Exception e )
      {
        return;
      }
    }
  };

  // paste
  private ActionListener paste = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      Object copy = KalumetConsoleApplication.getApplication().getCopyComponent();
      // check the copy object
      if ( copy == null || !( copy instanceof Environment ) )
      {
        return;
      }
      environment = (Environment) copy;
      environment.setLock( KalumetConsoleApplication.getApplication().getUserid() );
      environmentName = null;
      // update the window
      update();
    }
  };

  // delete
  private ActionListener delete = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if the user has the environment lock
      if ( !environment.getLock().equals( KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            environmentName );
        return;
      }
      // display confirm window
      KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
        new ConfirmWindow( new ActionListener()
        {
          public void actionPerformed( ActionEvent event )
          {
            // load Kalumet configuration
            Kalumet kalumet = null;
            try
            {
              kalumet = ConfigurationManager.loadStore();
            }
            catch ( Exception e )
            {
              KalumetConsoleApplication.getApplication().getLogPane().addError(
                Messages.getString( "db.read" ) + ": " + e.getMessage(), environmentName );
              return;
            }
            // looking for the delete object
            Environment delete = kalumet.getEnvironment( environmentName );
            kalumet.getEnvironments().remove( delete );
            // save configuration
            try
            {
              ConfigurationManager.writeStore( kalumet );
            }
            catch ( Exception e )
            {
              KalumetConsoleApplication.getApplication().getLogPane().addError(
                Messages.getString( "db.write" ) + ": " + e.getMessage(), environmentName );
              return;
            }
            // update the environments pane
            KalumetConsoleApplication.getApplication().getEnvironmentsPane().update();
            // close the window
            EnvironmentWindow.this.userClose();
          }
        } ) );
    }
  };

  // update
  private ActionListener update = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if the user has the lock
      if ( !environment.getLock().equals( KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addError( Messages.getString( "environment.locked" ),
                                                                          environmentName );
        return;
      }
      // check if something has not been saved
      if ( isUpdated() )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addError( Messages.getString( "environment.notsaved" ),
                                                                          environmentName );
        return;
      }
      // display confirm window
      KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
        new ConfirmWindow( new ActionListener()
        {
          public void actionPerformed( ActionEvent event )
          {
            // add a message into the log pane and the journal
            KalumetConsoleApplication.getApplication().getLogPane().addInfo(
              "Environment " + environmentName + " update in progress...", environmentName );
            getChangeEvents().add( "Update requested." );
            // start the update thread
            final UpdateThread updateThread = new UpdateThread();
            updateThread.start();
            // sync with the client
            KalumetConsoleApplication.getApplication().enqueueTask(
              KalumetConsoleApplication.getApplication().getTaskQueue(), new Runnable()
            {
              public void run()
              {
                if ( updateThread.ended )
                {
                  if ( updateThread.failure )
                  {
                    KalumetConsoleApplication.getApplication().getLogPane().addError( updateThread.message,
                                                                                      environmentName );
                    getChangeEvents().add( updateThread.message );
                  }
                  else
                  {
                    KalumetConsoleApplication.getApplication().getLogPane().addConfirm(
                      "Environment " + environmentName + " updated.", environmentName );
                    getChangeEvents().add( "Update completed successfully." );
                  }
                }
                else
                {
                  KalumetConsoleApplication.getApplication().enqueueTask(
                    KalumetConsoleApplication.getApplication().getTaskQueue(), this );
                }
              }
            } );
          }
        } ) );
    }
  };

  /**
   * Create a new <code>EnvironmentWindow</code>.
   *
   * @param environmentName the environment name.
   */
  public EnvironmentWindow( String environmentName )
  {
    super();

    this.environmentName = environmentName;

    // init the change events
    this.changeEvents = new LinkedList();

    // init the updated flag
    this.updated = false;

    // load Kalumet configuration
    Kalumet kalumet = null;
    try
    {
      kalumet = ConfigurationManager.loadStore();
    }
    catch ( Exception e )
    {
      KalumetConsoleApplication.getApplication().getLogPane().addError(
        Messages.getString( "db.read" ) + ": " + e.getMessage() );
      return;
    }

    // update the environment object from Kalumet
    this.environment = kalumet.getEnvironment( environmentName );
    if ( this.environment == null )
    {
      this.environment = new Environment();
      this.environment.setLock( KalumetConsoleApplication.getApplication().getUserid() );
      this.adminPermission = true;
    }

    // check if the user has access to the environment
    if ( environmentName != null )
    {
      // update permission flags
      adminPermission = kalumet.getSecurity().checkEnvironmentUserAccess( this.environment,
                                                                          KalumetConsoleApplication.getApplication().getUserid(),
                                                                          "admin" );
      updatePermission = kalumet.getSecurity().checkEnvironmentUserAccess( this.environment,
                                                                           KalumetConsoleApplication.getApplication().getUserid(),
                                                                           "update" );
      jeeApplicationServersChangePermission = kalumet.getSecurity().checkEnvironmentUserAccess( this.environment,
                                                                               KalumetConsoleApplication.getApplication().getUserid(),
                                                                               "jee_application_servers_change" );
      jeeApplicationServersUpdatePermission = kalumet.getSecurity().checkEnvironmentUserAccess( this.environment,
                                                                                     KalumetConsoleApplication.getApplication().getUserid(),
                                                                                     "jee_application_servers_update" );
      jeeApplicationServersControlPermission = kalumet.getSecurity().checkEnvironmentUserAccess( this.environment,
                                                                                      KalumetConsoleApplication.getApplication().getUserid(),
                                                                                      "jee_application_servers_control" );
      jeeResourcesChangePermission = kalumet.getSecurity().checkEnvironmentUserAccess( this.environment,
                                                                                 KalumetConsoleApplication.getApplication().getUserid(),
                                                                                 "jee_resources_change" );
      jeeResourcesUpdatePermission = kalumet.getSecurity().checkEnvironmentUserAccess( this.environment,
                                                                                       KalumetConsoleApplication.getApplication().getUserid(),
                                                                                       "jee_resources_update" );
      jeeApplicationsChangePermission = kalumet.getSecurity().checkEnvironmentUserAccess( this.environment,
                                                                                    KalumetConsoleApplication.getApplication().getUserid(),
                                                                                    "jee_applications_change" );
      jeeApplicationsUpdatePermission = kalumet.getSecurity().checkEnvironmentUserAccess( this.environment,
                                                                                          KalumetConsoleApplication.getApplication().getUserid(),
                                                                                          "jee_applications_update" );
      softwareChangePermission = kalumet.getSecurity().checkEnvironmentUserAccess( this.environment,
                                                                              KalumetConsoleApplication.getApplication().getUserid(),
                                                                              "software_change" );
      softwareUpdatePermission = kalumet.getSecurity().checkEnvironmentUserAccess( this.environment,
                                                                                    KalumetConsoleApplication.getApplication().getUserid(),
                                                                                    "software_update" );
      releasePermission = kalumet.getSecurity().checkEnvironmentUserAccess( this.environment,
                                                                            KalumetConsoleApplication.getApplication().getUserid(),
                                                                            "release" );
      shellPermission = kalumet.getSecurity().checkEnvironmentUserAccess( this.environment,
                                                                          KalumetConsoleApplication.getApplication().getUserid(),
                                                                          "shell" );
      browserPermission = kalumet.getSecurity().checkEnvironmentUserAccess( this.environment,
                                                                            KalumetConsoleApplication.getApplication().getUserid(),
                                                                            "browser" );
      homepagePermission = kalumet.getSecurity().checkEnvironmentUserAccess( this.environment,
                                                                             KalumetConsoleApplication.getApplication().getUserid(),
                                                                             "homepage" );

      // check user permission on the environment
      if ( !kalumet.getSecurity().checkEnvironmentUserAccess( this.environment,
                                                              KalumetConsoleApplication.getApplication().getUserid(),
                                                              null ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "environment.restricted" ), this.environmentName );
        return;
      }
    }
    else
    {
      adminPermission = true;
    }

    // try to take the environment lock (if the user can)
    if ( ( adminPermission || updatePermission || jeeApplicationServersChangePermission || jeeApplicationServersUpdatePermission
      || jeeApplicationServersControlPermission || jeeResourcesChangePermission || jeeResourcesUpdatePermission
      || jeeApplicationsChangePermission || jeeApplicationsUpdatePermission || softwareChangePermission
      || softwareUpdatePermission || releasePermission || homepagePermission ) && ( this.environment.getLock() == null
      || this.environment.getLock().trim().length() < 1 ) )
    {
      // lock the environment (but not yet saved)
      this.environment.setLock( KalumetConsoleApplication.getApplication().getUserid() );
      // save the lock if required
      if ( environmentName != null )
      {
        try
        {
          ConfigurationManager.writeStore( kalumet );
        }
        catch ( Exception e )
        {
          KalumetConsoleApplication.getApplication().getLogPane().addError(
            Messages.getString( "db.write" ) + ": " + e.getMessage() );
          return;
        }
      }
    }

    if ( environmentName == null )
    {
      setTitle( Messages.getString( "environment" ) );
    }
    else
    {
      if ( environment.getLock() == null || environment.getLock().trim().length() < 1 )
      {
        setTitle( Messages.getString( "environment" ) + " " + environmentName );
      }
      else
      {
        setTitle(
          Messages.getString( "environment" ) + " " + environmentName + " (" + Messages.getString( "locked.by" ) + " "
            + this.environment.getLock() + ")" );
      }
    }
    setId( "environmentwindow_" + environmentName );
    setIcon( Styles.APPLICATION );
    setStyleName( "environment" );
    setModal( false );

    SplitPane splitPane = new SplitPane( SplitPane.ORIENTATION_VERTICAL_BOTTOM_TOP, new Extent( 32 ) );
    this.add( splitPane );

    // add the control pane
    Row controlRow = new Row();
    controlRow.setStyleName( "control" );
    splitPane.add( controlRow );
    // add the refresh button
    Button refreshButton = new Button( Messages.getString( "reload" ), Styles.DATABASE_REFRESH );
    refreshButton.setStyleName( "control" );
    refreshButton.setToolTipText( Messages.getString( "reload" ) );
    refreshButton.addActionListener( refresh );
    controlRow.add( refreshButton );
    // add the copy button
    Button copyButton = new Button( Messages.getString( "copy" ), Styles.PAGE_COPY );
    copyButton.setStyleName( "control" );
    copyButton.setToolTipText( Messages.getString( "copy" ) );
    copyButton.addActionListener( copy );
    controlRow.add( copyButton );
    if ( this.adminPermission || this.jeeApplicationServersChangePermission || this.jeeResourcesChangePermission
      || this.jeeApplicationsChangePermission || this.softwareChangePermission || this.releasePermission )
    {
      // add the paste button
      Button pasteButton = new Button( Messages.getString( "paste" ), Styles.PAGE_PASTE );
      pasteButton.setStyleName( "control" );
      pasteButton.setToolTipText( Messages.getString( "paste" ) );
      pasteButton.addActionListener( paste );
      controlRow.add( pasteButton );
      // add the save button
      Button saveButton = new Button( Messages.getString( "save" ), Styles.DATABASE_SAVE );
      saveButton.setStyleName( "control" );
      saveButton.setToolTipText( Messages.getString( "save" ) );
      saveButton.addActionListener( save );
      controlRow.add( saveButton );
    }
    // add the force unlock button if the user has the lock
    if ( this.adminPermission || this.updatePermission || this.jeeApplicationServersChangePermission || this.jeeApplicationServersControlPermission
      || this.jeeApplicationServersUpdatePermission || this.jeeResourcesChangePermission || this.jeeResourcesUpdatePermission
      || this.jeeApplicationsChangePermission || this.jeeApplicationsUpdatePermission || this.softwareChangePermission
      || this.softwareUpdatePermission || this.releasePermission || this.homepagePermission )
    {
      lockButton = new Button( Styles.LOCK );
      lockButton.addActionListener( toggleLock );
      updateLockButton();
      lockButton.setStyleName( "control" );
      controlRow.add( lockButton );
    }
    if ( this.adminPermission || this.updatePermission )
    {
      // add the update button
      Button updateButton = new Button( Messages.getString( "update" ), Styles.COG );
      updateButton.setToolTipText( Messages.getString( "update" ) );
      updateButton.addActionListener( update );
      updateButton.setStyleName( "control" );
      controlRow.add( updateButton );
    }
    if ( this.adminPermission )
    {
      // add the delete button
      Button deleteButton = new Button( Messages.getString( "delete" ), Styles.APPLICATION_DELETE );
      deleteButton.setToolTipText( Messages.getString( "delete" ) );
      deleteButton.setStyleName( "control" );
      deleteButton.addActionListener( delete );
      controlRow.add( deleteButton );
    }
    // add the close button
    Button closeButton = new Button( Messages.getString( "close" ), Styles.CROSS );
    closeButton.setStyleName( "control" );
    closeButton.addActionListener( close );
    controlRow.add( closeButton );

    // add the main tab pane
    TabPane tabPane = new TabPane();
    tabPane.setStyleName( "default" );
    splitPane.add( tabPane );

    // add the environment general tab
    TabPaneLayoutData tabLayoutData = new TabPaneLayoutData();
    tabLayoutData.setTitle( Messages.getString( "general" ) );
    generalPane = new GeneralPane( this );
    generalPane.setLayoutData( tabLayoutData );
    tabPane.add( generalPane );

    // add the environment security tab
    tabLayoutData = new TabPaneLayoutData();
    tabLayoutData.setTitle( Messages.getString( "security" ) );
    securityPane = new SecurityPane( this );
    securityPane.setLayoutData( tabLayoutData );
    tabPane.add( securityPane );

    // add the environment jee application servers tab
    tabLayoutData = new TabPaneLayoutData();
    tabLayoutData.setTitle( Messages.getString( "applicationservers" ) );
    applicationServersPane = new ApplicationServersPane( this );
    applicationServersPane.setLayoutData( tabLayoutData );
    tabPane.add( applicationServersPane );

    // add the ressource tab pane
    tabLayoutData = new TabPaneLayoutData();
    tabLayoutData.setTitle( Messages.getString( "resources" ) );
    TabPane resourcesPane = new TabPane();
    resourcesPane.setStyleName( "default" );
    resourcesPane.setLayoutData( tabLayoutData );
    tabPane.add( resourcesPane );

    // add the jdbc connection pools tab
    tabLayoutData = new TabPaneLayoutData();
    tabLayoutData.setTitle( Messages.getString( "connectionpools" ) );
    connectionPoolsPane = new ConnectionPoolsPane( this );
    connectionPoolsPane.setLayoutData( tabLayoutData );
    resourcesPane.add( connectionPoolsPane );

    // add the jdbc data sources tab
    tabLayoutData = new TabPaneLayoutData();
    tabLayoutData.setTitle( Messages.getString( "datasources" ) );
    dataSourcesPane = new DataSourcesPane( this );
    dataSourcesPane.setLayoutData( tabLayoutData );
    resourcesPane.add( dataSourcesPane );

    // add the jms connection factories tab
    tabLayoutData = new TabPaneLayoutData();
    tabLayoutData.setTitle( Messages.getString( "connectionfactories" ) );
    connectionFactoriesPane = new ConnectionFactoriesPane( this );
    connectionFactoriesPane.setLayoutData( tabLayoutData );
    resourcesPane.add( connectionFactoriesPane );

    // add the jms servers tab
    tabLayoutData = new TabPaneLayoutData();
    tabLayoutData.setTitle( Messages.getString( "jmsservers" ) );
    jmsServersPane = new JmsServersPane( this );
    jmsServersPane.setLayoutData( tabLayoutData );
    resourcesPane.add( jmsServersPane );

    // add the jndi name space bindings tab
    tabLayoutData = new TabPaneLayoutData();
    tabLayoutData.setTitle( Messages.getString( "namespacebindings" ) );
    nameSpaceBindingsPane = new NameSpaceBindingsPane( this );
    nameSpaceBindingsPane.setLayoutData( tabLayoutData );
    resourcesPane.add( nameSpaceBindingsPane );

    // add the shared librairies tab
    tabLayoutData = new TabPaneLayoutData();
    tabLayoutData.setTitle( Messages.getString( "sharedlibraries" ) );
    sharedLibrariesPane = new SharedLibrariesPane( this );
    sharedLibrariesPane.setLayoutData( tabLayoutData );
    resourcesPane.add( sharedLibrariesPane );

    // add the JEE applications tab
    tabLayoutData = new TabPaneLayoutData();
    tabLayoutData.setTitle( Messages.getString( "applications" ) );
    applicationsPane = new ApplicationsPane( this );
    applicationsPane.setLayoutData( tabLayoutData );
    tabPane.add( applicationsPane );

    // add the softwares tab
    tabLayoutData = new TabPaneLayoutData();
    tabLayoutData.setTitle( Messages.getString( "softwares" ) );
    softwaresPane = new SoftwaresPane( this );
    softwaresPane.setLayoutData( tabLayoutData );
    tabPane.add( softwaresPane );

    // add the journal log tab
    tabLayoutData = new TabPaneLayoutData();
    tabLayoutData.setTitle( Messages.getString( "journal" ) );
    journalPane = new JournalPane( this );
    journalPane.setLayoutData( tabLayoutData );
    tabPane.add( journalPane );

    // add the notifiers tab
    tabLayoutData = new TabPaneLayoutData();
    tabLayoutData.setTitle( Messages.getString( "notifier" ) );
    notifiersPane = new NotifiersPane( this );
    notifiersPane.setLayoutData( tabLayoutData );
    tabPane.add( notifiersPane );

    // add the publishers tab
    tabLayoutData = new TabPaneLayoutData();
    tabLayoutData.setTitle( Messages.getString( "publisher" ) );
    publishersPane = new PublishersPane( this );
    publishersPane.setLayoutData( tabLayoutData );
    tabPane.add( publishersPane );

    // add the actions tab
    tabLayoutData = new TabPaneLayoutData();
    tabLayoutData.setTitle( Messages.getString( "dashboard" ) );
    TabPane dashboardPane = new TabPane();
    dashboardPane.setStyleName( "default" );
    dashboardPane.setLayoutData( tabLayoutData );
    tabPane.add( dashboardPane );

    // add the updater action tab
    tabLayoutData = new TabPaneLayoutData();
    tabLayoutData.setTitle( Messages.getString( "control" ) );
    actionPane = new ActionPane( this );
    actionPane.setLayoutData( tabLayoutData );
    dashboardPane.add( actionPane );

    // add the checker tab
    tabLayoutData = new TabPaneLayoutData();
    tabLayoutData.setTitle( Messages.getString( "check" ) );
    checkerPane = new CheckerPane( this );
    checkerPane.setLayoutData( tabLayoutData );
    dashboardPane.add( checkerPane );

    if ( adminPermission || shellPermission )
    {
      // add the system launcher tab
      tabLayoutData = new TabPaneLayoutData();
      tabLayoutData.setTitle( Messages.getString( "shell" ) );
      shellPane = new ShellPane( this );
      shellPane.setLayoutData( tabLayoutData );
      dashboardPane.add( shellPane );
    }

    if ( adminPermission || browserPermission )
    {
      // add the file explorer tab
      tabLayoutData = new TabPaneLayoutData();
      tabLayoutData.setTitle( Messages.getString( "file.browser" ) );
      fileBrowserane = new FileBrowserPane( this );
      fileBrowserane.setLayoutData( tabLayoutData );
      dashboardPane.add( fileBrowserane );
    }

    // add the log viewer tab
    tabLayoutData = new TabPaneLayoutData();
    tabLayoutData.setTitle( Messages.getString( "logs.viewer" ) );
    logViewerPane = new LogViewerPane( this );
    logViewerPane.setLayoutData( tabLayoutData );
    dashboardPane.add( logViewerPane );

    // add the statistics tab
    tabLayoutData = new TabPaneLayoutData();
    tabLayoutData.setTitle( Messages.getString( "stats" ) );
    statisticsPane = new StatisticsPane( this );
    statisticsPane.setLayoutData( tabLayoutData );
    dashboardPane.add( statisticsPane );
  }

  /**
   * Get the environment linked with the window
   *
   * @return the <code>Environment</code> linked with the window
   */
  public Environment getEnvironment()
  {
    return this.environment;
  }

  /**
   * Get the environment name
   *
   * @return the current <code>Environment</code> name
   */
  public String getEnvironmentName()
  {
    return this.environmentName;
  }

  /**
   * Get the change events list
   *
   * @return the change events list
   */
  public List getChangeEvents()
  {
    return this.changeEvents;
  }

  /**
   * Get the updated flag
   *
   * @return the updated flag
   */
  public boolean isUpdated()
  {
    return this.updated;
  }

  /**
   * Set the updated flag
   *
   * @param updated the new updated flag value
   */
  public void setUpdated( boolean updated )
  {
    this.updated = updated;
  }

  /**
   * Update the lock button (display lock or unlock depending of the state)
   */
  public void updateLockButton()
  {
    // if the lock is taken by the user
    if ( environmentName != null && (
      environment.getLock().equals( KalumetConsoleApplication.getApplication().getUserid() )
        || KalumetConsoleApplication.getApplication().getUserid().equals( "admin" ) ) )
    {
      lockButton.setText( Messages.getString( "unlock" ) );
    }
    else
    {
      lockButton.setText( Messages.getString( "lock" ) );
    }
  }

  /**
   * Update the complete <code>EnvironmentWindow</code> with all children tab.
   */
  public void update()
  {
    generalPane.update();
    securityPane.update();
    applicationServersPane.update();
    connectionPoolsPane.update();
    dataSourcesPane.update();
    connectionFactoriesPane.update();
    jmsServersPane.update();
    nameSpaceBindingsPane.update();
    sharedLibrariesPane.update();
    applicationsPane.update();
    checkerPane.update();
    softwaresPane.update();
    journalPane.update();
    notifiersPane.update();
    publishersPane.update();
    actionPane.update();
    logViewerPane.update();
    this.updateLockButton();
  }

  /**
   * Only update the <code>EnvironmentJournalLogTabPane</code>
   */
  public void updateJournalPane()
  {
    journalPane.update();
  }

}