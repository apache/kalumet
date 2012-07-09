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
import nextapp.echo2.app.Column;
import nextapp.echo2.app.ContentPane;
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.Grid;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.SelectField;
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.app.list.DefaultListModel;
import org.apache.kalumet.FileManipulator;
import org.apache.kalumet.console.configuration.ConfigurationManager;
import org.apache.kalumet.model.Agent;
import org.apache.kalumet.model.J2EEApplication;
import org.apache.kalumet.model.J2EEApplicationServer;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.ws.client.J2EEApplicationClient;

import java.util.Iterator;

/**
 * Environment J2EE applications pane.
 */
public class ApplicationsPane
  extends ContentPane
{

  private EnvironmentWindow parent;

  private SelectField scopeField;

  private Grid grid;

  // update thread
  class UpdateThread
    extends Thread
  {

    public String serverName;

    public String applicationName;

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
        Agent agent = kalumet.getAgent( parent.getEnvironment().getAgent() );
        if ( agent == null )
        {
          throw new IllegalArgumentException( "agent not found." );
        }
        // call the webservice
        J2EEApplicationClient client = new J2EEApplicationClient( agent.getHostname(), agent.getPort() );
        client.update( parent.getEnvironmentName(), serverName, applicationName, false );
      }
      catch ( Exception e )
      {
        failure = true;
        message = "J2EE application " + applicationName + " update failed: " + e.getMessage();
      }
      finally
      {
        ended = true;
      }
    }
  }

  // scope select
  private ActionListener scopeSelect = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      update();
    }
  };

  // toggle active
  private ActionListener toggleActive = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if the user has the environment lock
      if ( !getEnvironmentWindow().getEnvironment().getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeApplicationsPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // looking for the J2EE application object
      J2EEApplication application = parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(
        (String) scopeField.getSelectedItem() ).getJ2EEApplication( event.getActionCommand() );
      if ( application == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "application.notfound" ), getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // change the J2EE application state and add a change event
      if ( application.isActive() )
      {
        application.setActive( false );
        parent.getChangeEvents().add( "Disable J2EE application " + application.getName() );
      }
      else
      {
        application.setActive( true );
        parent.getChangeEvents().add( "Enable J2EE application " + application.getName() );
      }
      // change the updated flag
      parent.setUpdated( true );
      // update the journal log tab pane
      parent.updateJournalPane();
      // update the pane
      update();
    }
  };

  // toggle blocker
  private ActionListener toggleBlocker = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if the user has the environment lock
      if ( !getEnvironmentWindow().getEnvironment().getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeApplicationsPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // looking for the jee application object
      J2EEApplication application = parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(
        (String) scopeField.getSelectedItem() ).getJ2EEApplication( event.getActionCommand() );
      if ( application == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "application.notfound" ), getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // change the jee application blocker and add a change event
      if ( application.isBlocker() )
      {
        application.setBlocker( false );
        parent.getChangeEvents().add( "Set not blocker for J2EE application " + application.getName() );
      }
      else
      {
        application.setBlocker( true );
        parent.getChangeEvents().add( "Set blocker for J2EE application " + application.getName() );
      }
      // change the updated flag
      parent.setUpdated( true );
      // update the journal log tab pane
      parent.updateJournalPane();
      // update the pane
      update();
    }
  };

  // delete
  private ActionListener delete = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if the user has the environment lock
      if ( !getEnvironmentWindow().getEnvironment().getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeApplicationsPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // looking for the jee application object
      final J2EEApplication application = parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(
        (String) scopeField.getSelectedItem() ).getJ2EEApplication( event.getActionCommand() );
      if ( application == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "application.notfound" ), getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // display confirm window
      KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
        new ConfirmWindow( new ActionListener()
        {
          public void actionPerformed( ActionEvent event )
          {
            // delete the j2ee application object
            parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(
              (String) scopeField.getSelectedItem() ).getJ2EEApplications().remove( application );
            // add a change event
            parent.getChangeEvents().add( "Delete J2EE application " + application.getName() );
            // change the updated flag
            parent.setUpdated( true );
            // update the journal log tab pane
            parent.updateJournalPane();
            // update the pane
            update();
          }
        } ) );
    }
  };

  // edit
  private ActionListener edit = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      if ( KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().getComponent(
        "applicationwindow_" + parent.getEnvironmentName() + "_" + (String) scopeField.getSelectedItem() + "_"
          + event.getActionCommand() ) == null )
      {
        KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
          new ApplicationWindow( ApplicationsPane.this, (String) scopeField.getSelectedItem(),
                                 event.getActionCommand() ) );
      }
    }
  };

  // create
  private ActionListener create = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
        new ApplicationWindow( ApplicationsPane.this, (String) scopeField.getSelectedItem(), null ) );
    }
  };

  // up
  private ActionListener up = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if the user has the environment lock
      if ( !getEnvironmentWindow().getEnvironment().getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeApplicationsPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // looking for the j2ee application object
      J2EEApplication application = parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(
        (String) scopeField.getSelectedItem() ).getJ2EEApplication( event.getActionCommand() );
      if ( application == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "application.notfound" ), getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // get the j2ee application index
      int index = parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(
        (String) scopeField.getSelectedItem() ).getJ2EEApplications().indexOf( application );
      // if the j2ee application index is the first one or the object is not
      // found, do nothing, the size of the list must constains at least 2
      // j2ee applications
      if ( index == 0 || index == -1 || parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(
        (String) scopeField.getSelectedItem() ).getJ2EEApplications().size() < 2 )
      {
        return;
      }
      // get the previous application
      J2EEApplication previous =
        (J2EEApplication) parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(
          (String) scopeField.getSelectedItem() ).getJ2EEApplications().get( index - 1 );
      // switch the application
      parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(
        (String) scopeField.getSelectedItem() ).getJ2EEApplications().set( index, previous );
      parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(
        (String) scopeField.getSelectedItem() ).getJ2EEApplications().set( index - 1, application );
      // update the pane
      update();
    }
  };

  // down
  private ActionListener down = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if the user has the environment lock
      if ( !getEnvironmentWindow().getEnvironment().getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeApplicationsPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // looking for the j2ee application object
      J2EEApplication application = parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(
        (String) scopeField.getSelectedItem() ).getJ2EEApplication( event.getActionCommand() );
      if ( application == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "application.notfound" ), getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // get the j2ee application index
      int index = parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(
        (String) scopeField.getSelectedItem() ).getJ2EEApplications().indexOf( application );
      // if the j2ee application index is the last one or the object is not
      // found, the size of the list must contains at least 2 j2ee
      // applications
      if ( index == -1 || index == parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(
        (String) scopeField.getSelectedItem() ).getJ2EEApplications().size() - 1 ||
        parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(
          (String) scopeField.getSelectedItem() ).getJ2EEApplications().size() < 2 )
      {
        return;
      }
      // get the next application
      J2EEApplication next =
        (J2EEApplication) parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(
          (String) scopeField.getSelectedItem() ).getJ2EEApplications().get( index + 1 );
      // switch the application
      parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(
        (String) scopeField.getSelectedItem() ).getJ2EEApplications().set( index + 1, application );
      parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(
        (String) scopeField.getSelectedItem() ).getJ2EEApplications().set( index, next );
      // update the pane
      update();
    }
  };

  // copy
  private ActionListener copy = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // looking for the j2ee application object
      J2EEApplication application = parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(
        (String) scopeField.getSelectedItem() ).getJ2EEApplication( event.getActionCommand() );
      if ( application == null )
      {
        return;
      }
      try
      {
        KalumetConsoleApplication.getApplication().setCopyComponent( application.clone() );
      }
      catch ( Exception e )
      {
        return;
      }
    }
  };

  // apply
  private ActionListener apply = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if the user has the environment lock
      if ( !getEnvironmentWindow().getEnvironment().getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeApplicationsPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // get the j2ee application name
      String name = event.getActionCommand();
      // get the j2ee application uri field
      TextField uriField = (TextField) ApplicationsPane.this.getComponent(
        "applicationuri_" + parent.getEnvironmentName() + "_" + (String) scopeField.getSelectedItem() + "_" + name );
      // get the URI field value
      String uriValue = uriField.getText();
      // looking for the j2ee application object
      J2EEApplication application = parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(
        (String) scopeField.getSelectedItem() ).getJ2EEApplication( name );
      if ( application == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "application.notfound" ), getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // add a change event
      parent.getChangeEvents().add( "Change J2EE application " + application.getName() + " URI to " + uriValue );
      // change the j2ee application object
      application.setUri( uriValue );
      // change the updated flag
      parent.setUpdated( true );
      // update the journal log tab pane
      parent.updateJournalPane();
      // update the pane
      update();
    }
  };

  // update
  private ActionListener update = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if the user has the lock
      if ( !getEnvironmentWindow().getEnvironment().getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeApplicationsUpdatePermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if some change has not been saved
      if ( getEnvironmentWindow().isUpdated() )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "environment.notsaved" ), getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // get the selected J2EE server and application
      final String serverName = (String) scopeField.getSelectedItem();
      final String applicationName = event.getActionCommand();
      // display confirm window
      KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
        new ConfirmWindow( new ActionListener()
        {
          public void actionPerformed( ActionEvent event )
          {
            // add a message into the log pane and the journal
            KalumetConsoleApplication.getApplication().getLogPane().addInfo(
              "J2EE application " + applicationName + " update in progress...", parent.getEnvironmentName() );
            parent.getChangeEvents().add( "J2EE application " + applicationName + " update requested." );
            // start the update thread
            final UpdateThread updateThread = new UpdateThread();
            updateThread.serverName = serverName;
            updateThread.applicationName = applicationName;
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
                                                                                      parent.getEnvironmentName() );
                    parent.getChangeEvents().add( updateThread.message );
                  }
                  else
                  {
                    KalumetConsoleApplication.getApplication().getLogPane().addConfirm(
                      "J2EE application " + applicationName + " updated.", parent.getEnvironmentName() );
                    parent.getChangeEvents().add( "J2EE application " + applicationName + " updated." );
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

  // test URI
  private ActionListener testUri = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      String name = event.getActionCommand();
      TextField uriField = (TextField) ApplicationsPane.this.getComponent(
        "applicationuri_" + parent.getEnvironmentName() + "_" + (String) scopeField.getSelectedItem() + "_" + name );
      String uri = FileManipulator.format( uriField.getText() );
      boolean exists = false;
      FileManipulator fileManipulator = null;
      try
      {
        fileManipulator = new FileManipulator();
        exists = fileManipulator.exists( uri );
      }
      catch ( Exception e )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          "Can't check the URI " + uri + ": " + e.getMessage(), parent.getEnvironmentName() );
      }
      finally
      {
        if ( fileManipulator != null )
        {
          fileManipulator.close();
        }
      }
      if ( exists )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addConfirm( "URI " + uri + " exists.",
                                                                            parent.getEnvironmentName() );
      }
      else
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( "URI " + uri + " doesn't exists.",
                                                                            parent.getEnvironmentName() );
      }
    }
  };

  /**
   * Create a new <code>ApplicationsPane</code>.
   *
   * @param parent the parent <code>EnvironmentWindow</code>.
   */
  public ApplicationsPane( EnvironmentWindow parent )
  {
    super();
    setStyleName( "tab.content" );

    // update parent
    this.parent = parent;

    // column layout
    Column content = new Column();
    content.setCellSpacing( new Extent( 2 ) );
    add( content );

    // add the scope field
    Grid layoutGrid = new Grid( 2 );
    layoutGrid.setStyleName( "default" );
    layoutGrid.setColumnWidth( 0, new Extent( 20, Extent.PERCENT ) );
    layoutGrid.setColumnWidth( 1, new Extent( 80, Extent.PERCENT ) );
    content.add( layoutGrid );
    Label scopeLabel = new Label( Messages.getString( "scope" ) );
    layoutGrid.add( scopeLabel );
    scopeField = new SelectField();
    scopeField.addActionListener( scopeSelect );
    scopeField.setStyleName( "default" );
    layoutGrid.add( scopeField );
    DefaultListModel scopeListModel = (DefaultListModel) scopeField.getModel();
    scopeListModel.removeAll();
    // add application servers in the scope select field
    for ( Iterator applicationServerIterator =
            parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServers().iterator();
          applicationServerIterator.hasNext(); )
    {
      J2EEApplicationServer applicationServer = (J2EEApplicationServer) applicationServerIterator.next();
      scopeListModel.add( applicationServer.getName() );
    }
    if ( scopeListModel.size() > 0 )
    {
      scopeField.setSelectedIndex( 0 );
    }

    // add the create button
    if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeApplicationsPermission )
    {
      Button createButton = new Button( Messages.getString( "application.add" ), Styles.ADD );
      createButton.addActionListener( create );
      content.add( createButton );
    }

    // add J2EE applications grid
    grid = new Grid( 4 );
    grid.setStyleName( "border.grid" );
    grid.setColumnWidth( 0, new Extent( 50, Extent.PX ) );
    grid.setColumnWidth( 1, new Extent( 10, Extent.PERCENT ) );
    grid.setColumnWidth( 2, new Extent( 80, Extent.PERCENT ) );
    grid.setColumnWidth( 3, new Extent( 10, Extent.PERCENT ) );
    content.add( grid );

    // update the pane
    update();
  }

  /**
   * Update the pane
   */
  public void update()
  {
    String applicationServerName = null;
    // update the scope select field
    DefaultListModel scopeListModel = (DefaultListModel) scopeField.getModel();
    if ( scopeListModel.size() > 0 )
    {
      applicationServerName = (String) scopeField.getSelectedItem();
    }
    scopeListModel.removeAll();
    int scopeIndex = 0;
    int found = -1;
    for ( Iterator applicationServerIterator =
            parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServers().iterator();
          applicationServerIterator.hasNext(); )
    {
      J2EEApplicationServer applicationServer = (J2EEApplicationServer) applicationServerIterator.next();
      scopeListModel.add( applicationServer.getName() );
      if ( applicationServer.getName().equals( applicationServerName ) )
      {
        found = scopeIndex;
      }
      scopeIndex++;
    }
    // remove all J2EE applications grid children
    grid.removeAll();
    // check if at least one application server is present
    if ( scopeListModel.size() < 1 )
    {
      return;
    }
    // update the scope select field selected index
    if ( found == -1 )
    {
      scopeField.setSelectedIndex( 0 );
    }
    else
    {
      scopeField.setSelectedIndex( found );
    }
    // update the application server name from the scope (in case of
    // application server deletion)
    applicationServerName = (String) scopeField.getSelectedItem();

    // add JEE applications grid header
    Label actionHeader = new Label( " " );
    actionHeader.setStyleName( "grid.header" );
    grid.add( actionHeader );
    Label nameHeader = new Label( Messages.getString( "name" ) );
    nameHeader.setStyleName( "grid.header" );
    grid.add( nameHeader );
    Label uriHeader = new Label( Messages.getString( "uri" ) );
    uriHeader.setStyleName( "grid.header" );
    grid.add( uriHeader );
    Label agentHeader = new Label( Messages.getString( "agent" ) );
    agentHeader.setStyleName( "grid.header" );
    grid.add( agentHeader );
    // add the JEE applications
    for ( Iterator applicationIterator = parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(
      applicationServerName ).getJ2EEApplications().iterator(); applicationIterator.hasNext(); )
    {
      J2EEApplication application = (J2EEApplication) applicationIterator.next();
      // row
      Row row = new Row();
      row.setInsets( new Insets( 2 ) );
      row.setCellSpacing( new Extent( 2 ) );
      grid.add( row );
      // copy
      Button copyButton = new Button( Styles.PAGE_COPY );
      copyButton.setToolTipText( Messages.getString( "copy" ) );
      copyButton.setActionCommand( application.getName() );
      copyButton.addActionListener( copy );
      row.add( copyButton );
      // active
      Button activeButton;
      if ( application.isActive() )
      {
        activeButton = new Button( Styles.LIGHTBULB );
        activeButton.setToolTipText( Messages.getString( "switch.disable" ) );
      }
      else
      {
        activeButton = new Button( Styles.LIGHTBULB_OFF );
        activeButton.setToolTipText( Messages.getString( "switch.enable" ) );
      }
      if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeApplicationsPermission )
      {
        activeButton.setActionCommand( application.getName() );
        activeButton.addActionListener( toggleActive );
      }
      row.add( activeButton );
      // blocker
      Button blockerButton;
      if ( application.isBlocker() )
      {
        blockerButton = new Button( Styles.PLUGIN );
        blockerButton.setToolTipText( Messages.getString( "switch.notblocker" ) );
      }
      else
      {
        blockerButton = new Button( Styles.PLUGIN_DISABLED );
        blockerButton.setToolTipText( Messages.getString( "switch.blocker" ) );
      }
      if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeApplicationsPermission )
      {
        blockerButton.setActionCommand( application.getName() );
        blockerButton.addActionListener( toggleBlocker );
      }
      row.add( blockerButton );
      if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeApplicationsPermission )
      {
        // up
        Button upButton = new Button( Styles.ARROW_UP );
        upButton.setToolTipText( Messages.getString( "up" ) );
        upButton.setActionCommand( application.getName() );
        upButton.addActionListener( up );
        row.add( upButton );
        // down
        Button downButton = new Button( Styles.ARROW_DOWN );
        downButton.setToolTipText( Messages.getString( "down" ) );
        downButton.setActionCommand( application.getName() );
        downButton.addActionListener( down );
        row.add( downButton );
      }
      if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeApplicationsUpdatePermission )
      {
        // update
        Button updateButton = new Button( Styles.COG );
        updateButton.setToolTipText( Messages.getString( "update" ) );
        updateButton.setActionCommand( application.getName() );
        updateButton.addActionListener( update );
        row.add( updateButton );
      }
      if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeApplicationsPermission )
      {
        // apply
        Button applyButton = new Button( Styles.ACCEPT );
        applyButton.setToolTipText( Messages.getString( "apply" ) );
        applyButton.setActionCommand( application.getName() );
        applyButton.addActionListener( apply );
        row.add( applyButton );
        // delete
        Button deleteButton = new Button( Styles.DELETE );
        deleteButton.setToolTipText( Messages.getString( "delete" ) );
        deleteButton.setActionCommand( application.getName() );
        deleteButton.addActionListener( delete );
        row.add( deleteButton );
      }
      // name
      Button nameField = new Button( application.getName() );
      nameField.setStyleName( "default" );
      nameField.setActionCommand( application.getName() );
      nameField.addActionListener( edit );
      grid.add( nameField );
      // uri
      Row uriRow = new Row();
      grid.add( uriRow );
      TextField uriField = new TextField();
      uriField.setStyleName( "default" );
      uriField.setWidth( new Extent( 500, Extent.PX ) );
      uriField.setId(
        "applicationuri_" + parent.getEnvironmentName() + "_" + applicationServerName + "_" + application.getName() );
      uriField.setText( application.getUri() );
      uriRow.add( uriField );
      // test
      Button testUriButton = new Button( Styles.WORLD );
      testUriButton.setToolTipText( Messages.getString( "uri.test" ) );
      testUriButton.setActionCommand( application.getName() );
      testUriButton.addActionListener( testUri );
      uriRow.add( testUriButton );
      // agent
      Label agent = new Label( application.getAgent() );
      agent.setStyleName( "default" );
      grid.add( agent );
    }
  }

  /**
   * Return the parent <code>EnvironmentWindow</code>.
   *
   * @return the parent <code>EnvironmentWindow</code>.
   */
  public EnvironmentWindow getEnvironmentWindow()
  {
    return this.parent;
  }

}