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
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import org.apache.kalumet.console.configuration.ConfigurationManager;
import org.apache.kalumet.model.Agent;
import org.apache.kalumet.model.ConfigurationFile;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.ws.client.ConfigurationFileClient;

import java.util.Iterator;

/**
 * J2EE application configuration files pane.
 */
public class ApplicationConfigurationFilesPane
  extends ContentPane
{

  private ApplicationWindow parent;

  private Grid grid;

  // status thread
  class StatusThread
    extends Thread
  {

    public String configurationFileName;

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
        Agent agent = kalumet.getAgent( parent.getParentPane().getEnvironmentWindow().getEnvironment().getAgent() );
        if ( agent == null )
        {
          throw new IllegalArgumentException( "agent not found." );
        }
        // call the web service
        ConfigurationFileClient client = new ConfigurationFileClient( agent.getHostname(), agent.getPort() );
        boolean uptodate =
          client.check( parent.getParentPane().getEnvironmentWindow().getEnvironmentName(), parent.getServerName(),
                        parent.getApplicationName(), configurationFileName );
        if ( uptodate )
        {
          message = "J2EE application " + parent.getApplicationName() + " configuration file " + configurationFileName
            + " is up to date.";
        }
        else
        {
          failure = true;
          message = "J2EE application " + parent.getApplicationName() + " configuration file " + configurationFileName
            + " is not up to date.";
        }
      }
      catch ( Exception e )
      {
        failure = true;
        message = "J2EE application " + parent.getApplicationName() + " configuration file " + configurationFileName
          + " status check failed: " + e.getMessage();
      }
      finally
      {
        ended = true;
      }
    }
  }

  // update thread
  class UpdateThread
    extends Thread
  {

    public String configurationFileName;

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
        Agent agent = kalumet.getAgent( parent.getParentPane().getEnvironmentWindow().getEnvironment().getAgent() );
        if ( agent == null )
        {
          throw new IllegalArgumentException( "agent not found." );
        }
        // call the web service
        ConfigurationFileClient client = new ConfigurationFileClient( agent.getHostname(), agent.getPort() );
        client.update( parent.getParentPane().getEnvironmentWindow().getEnvironmentName(), parent.getServerName(),
                       parent.getApplicationName(), configurationFileName, false );
      }
      catch ( Exception e )
      {
        failure = true;
        message = "J2EE application " + parent.getApplicationName() + " configuration file " + configurationFileName
          + " update failed: " + e.getMessage();
      }
      finally
      {
        ended = true;
      }
    }
  }

  // toggle active
  private ActionListener toggleActive = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if the user has the environment lock
      if ( !parent.getParentPane().getEnvironmentWindow().getEnvironment().getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !parent.getParentPane().getEnvironmentWindow().adminPermission
        && !parent.getParentPane().getEnvironmentWindow().jeeApplicationsPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // looking for the configuration file object
      ConfigurationFile configurationFile = parent.getApplication().getConfigurationFile( event.getActionCommand() );
      if ( configurationFile == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "configurationfile.notfound" ),
          parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // change the state and add a change event
      if ( configurationFile.isActive() )
      {
        configurationFile.setActive( false );
        parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
          "Disable J2EE application configuration file " + configurationFile.getName() );
      }
      else
      {
        configurationFile.setActive( true );
        parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
          "Enable J2EE application configuration file " + configurationFile.getName() );
      }
      // change the updated flag
      parent.getParentPane().getEnvironmentWindow().setUpdated( true );
      // update the journal log tab pane
      parent.getParentPane().getEnvironmentWindow().updateJournalPane();
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
      if ( !parent.getParentPane().getEnvironmentWindow().getEnvironment().getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !parent.getParentPane().getEnvironmentWindow().adminPermission
        && !parent.getParentPane().getEnvironmentWindow().jeeApplicationsPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // looking for the configuration file object
      ConfigurationFile configurationFile = parent.getApplication().getConfigurationFile( event.getActionCommand() );
      if ( configurationFile == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "configurationfile.notfound" ),
          parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // change the blocker state and add a change event
      if ( configurationFile.isBlocker() )
      {
        configurationFile.setBlocker( false );
        parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
          "Set not blocker for J2EE application configuration file " + configurationFile.getName() );
      }
      else
      {
        configurationFile.setBlocker( true );
        parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
          "Set blocker for J2EE application configuration file " + configurationFile.getName() );
      }
      // change the updated flag
      parent.getParentPane().getEnvironmentWindow().setUpdated( true );
      // update the journal log tab pane
      parent.getParentPane().getEnvironmentWindow().updateJournalPane();
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
      if ( !parent.getParentPane().getEnvironmentWindow().getEnvironment().getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !parent.getParentPane().getEnvironmentWindow().adminPermission
        && !parent.getParentPane().getEnvironmentWindow().jeeApplicationsPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // looking for the configuration file object
      final ConfigurationFile configurationFile =
        parent.getApplication().getConfigurationFile( event.getActionCommand() );
      if ( configurationFile == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "configurationfile.notfound" ),
          parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // display confirm window
      KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
        new ConfirmWindow( new ActionListener()
        {
          public void actionPerformed( ActionEvent event )
          {
            // delete the configuration file object
            parent.getApplication().getConfigurationFiles().remove( configurationFile );
            // add a change event
            parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
              "Delete J2EE application configuration file " + configurationFile.getName() );
            // change the updated flag
            parent.getParentPane().getEnvironmentWindow().setUpdated( true );
            // update the journal log tab pane
            parent.getParentPane().getEnvironmentWindow().updateJournalPane();
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
        "configurationfilewindow_" + parent.getParentPane().getEnvironmentWindow().getEnvironmentName() + "_"
          + parent.getServerName() + "_" + parent.getApplicationName() + "_" + event.getActionCommand() ) == null )
      {
        KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
          new ApplicationConfigurationFileWindow( ApplicationConfigurationFilesPane.this, event.getActionCommand() ) );
      }
    }
  };

  // create
  private ActionListener create = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
        new ApplicationConfigurationFileWindow( ApplicationConfigurationFilesPane.this, null ) );
    }
  };

  // copy
  private ActionListener copy = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // looking for the configuration file object
      ConfigurationFile configurationFile = parent.getApplication().getConfigurationFile( event.getActionCommand() );
      if ( configurationFile == null )
      {
        return;
      }
      try
      {
        KalumetConsoleApplication.getApplication().setCopyComponent( configurationFile.clone() );
      }
      catch ( Exception e )
      {
        return;
      }
    }
  };

  // up
  private ActionListener up = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if the user has the lock
      if ( !parent.getParentPane().getEnvironmentWindow().getEnvironment().getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !parent.getParentPane().getEnvironmentWindow().adminPermission
        && !parent.getParentPane().getEnvironmentWindow().jeeApplicationsPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // looking for the configuration file object
      ConfigurationFile configurationFile = parent.getApplication().getConfigurationFile( event.getActionCommand() );
      if ( configurationFile == null )
      {
        return;
      }
      // get the configuration file index
      int index = parent.getApplication().getConfigurationFiles().indexOf( configurationFile );
      // if the index is the first one, or the configuration file is not found
      // or the configuration files list doesn't contain at least two elements
      // do nothing
      if ( index == 0 || index == -1 || parent.getApplication().getConfigurationFiles().size() < 2 )
      {
        return;
      }
      // get the previous configuration file
      ConfigurationFile previous = (ConfigurationFile) parent.getApplication().getConfigurationFiles().get( index - 1 );
      // switch the configuration files
      parent.getApplication().getConfigurationFiles().set( index - 1, configurationFile );
      parent.getApplication().getConfigurationFiles().set( index, previous );
      // update the pane
      update();
    }
  };

  // down
  private ActionListener down = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if the user has the lock
      if ( !parent.getParentPane().getEnvironmentWindow().getEnvironment().getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !parent.getParentPane().getEnvironmentWindow().adminPermission
        && !parent.getParentPane().getEnvironmentWindow().jeeApplicationsPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // looking for the configuration file object
      ConfigurationFile configurationFile = parent.getApplication().getConfigurationFile( event.getActionCommand() );
      if ( configurationFile == null )
      {
        return;
      }
      // get the configuration file index
      int index = parent.getApplication().getConfigurationFiles().indexOf( configurationFile );
      // if the index is the last one, or the configuration file is not found
      // or if the configuration files list doesn't contain at least two elements
      // do nothing
      if ( index == -1 || index == parent.getApplication().getConfigurationFiles().size() - 1
        || parent.getApplication().getConfigurationFiles().size() < 2 )
      {
        return;
      }
      // get the next configuration file
      ConfigurationFile next = (ConfigurationFile) parent.getApplication().getConfigurationFiles().get( index + 1 );
      // switch the configuration files
      parent.getApplication().getConfigurationFiles().set( index + 1, configurationFile );
      parent.getApplication().getConfigurationFiles().set( index, next );
      // update the pane
      update();
    }
  };

  // status
  private ActionListener status = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if some change has not yet been saved
      if ( parent.getParentPane().getEnvironmentWindow().isUpdated() )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addError( Messages.getString( "environment.notsaved" ),
                                                                          parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      String configurationFileName = event.getActionCommand();
      // add a message into the log pane and the journal
      KalumetConsoleApplication.getApplication().getLogPane().addInfo(
        "J2EE application " + parent.getApplicationName() + " configuration file " + configurationFileName
          + " status check in progress...", parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
      parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
        "J2EE application " + parent.getApplicationName() + " configuration file " + configurationFileName
          + " status check requested." );
      // start the status thread
      final StatusThread statusThread = new StatusThread();
      statusThread.configurationFileName = configurationFileName;
      statusThread.start();
      // sync with the client
      KalumetConsoleApplication.getApplication().enqueueTask( KalumetConsoleApplication.getApplication().getTaskQueue(),
                                                              new Runnable()
                                                              {
                                                                public void run()
                                                                {
                                                                  if ( statusThread.ended )
                                                                  {
                                                                    if ( statusThread.failure )
                                                                    {
                                                                      KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                                                                        statusThread.message,
                                                                        parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
                                                                    }
                                                                    else
                                                                    {
                                                                      KalumetConsoleApplication.getApplication().getLogPane().addInfo(
                                                                        statusThread.message,
                                                                        parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
                                                                    }
                                                                  }
                                                                  else
                                                                  {
                                                                    KalumetConsoleApplication.getApplication().enqueueTask(
                                                                      KalumetConsoleApplication.getApplication().getTaskQueue(),
                                                                      this );
                                                                  }
                                                                }
                                                              } );
    }
  };

  // update
  private ActionListener update = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if the user has the lock
      if ( !parent.getParentPane().getEnvironmentWindow().getEnvironment().getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !parent.getParentPane().getEnvironmentWindow().adminPermission
        && !parent.getParentPane().getEnvironmentWindow().jeeApplicationsUpdatePermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if a change has not been saved
      if ( parent.getParentPane().getEnvironmentWindow().isUpdated() )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "environment.notsaved" ),
          parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // get the configuration file name
      final String configurationFileName = event.getActionCommand();
      // display confirm window
      KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
        new ConfirmWindow( new ActionListener()
        {
          public void actionPerformed( ActionEvent event )
          {
            // add a message in the log pane and in the journal
            KalumetConsoleApplication.getApplication().getLogPane().addInfo(
              "J2EE application " + parent.getApplicationName() + " configuration file " + configurationFileName
                + " update in progress...", parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
            parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
              "J2EE application " + parent.getApplicationName() + " configuration file " + configurationFileName
                + " update requested." );
            // start the update thread
            final UpdateThread updateThread = new UpdateThread();
            updateThread.configurationFileName = configurationFileName;
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
                                                                                      parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
                    parent.getParentPane().getEnvironmentWindow().getChangeEvents().add( updateThread.message );
                  }
                  else
                  {
                    KalumetConsoleApplication.getApplication().getLogPane().addConfirm(
                      "J2EE application " + parent.getApplicationName() + " configuration file " + configurationFileName
                        + " updated.", parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
                    parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
                      "J2EE application " + parent.getApplicationName() + " configuration file " + configurationFileName
                        + " updated." );
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
   * Create a new <code>ApplicationConfigurationFilesPane</code>.
   *
   * @param parent the parent <code>ApplicationWindow</code>
   */
  public ApplicationConfigurationFilesPane( ApplicationWindow parent )
  {
    super();
    setStyleName( "tab.content" );

    // update parent
    this.parent = parent;

    // column layout content
    Column content = new Column();
    add( content );

    // add the create button
    if ( parent.getParentPane().getEnvironmentWindow().adminPermission
      || parent.getParentPane().getEnvironmentWindow().jeeApplicationsPermission )
    {
      Button createButton = new Button( Messages.getString( "configurationfile.add" ), Styles.ADD );
      createButton.addActionListener( create );
      content.add( createButton );
    }

    // add the configuration files grid
    grid = new Grid( 5 );
    grid.setStyleName( "border.grid" );
    content.add( grid );

    // update the pane
    update();
  }

  /**
   * Update the pane
   */
  public void update()
  {
    // remove all configuration files grid children
    grid.removeAll();
    // add configuration files grid header
    Label actionHeader = new Label( " " );
    actionHeader.setStyleName( "grid.header" );
    grid.add( actionHeader );
    Label nameHeader = new Label( Messages.getString( "name" ) );
    nameHeader.setStyleName( "grid.header" );
    grid.add( nameHeader );
    Label uriHeader = new Label( Messages.getString( "uri" ) );
    uriHeader.setStyleName( "grid.header" );
    grid.add( uriHeader );
    Label pathHeader = new Label( Messages.getString( "path" ) );
    pathHeader.setStyleName( "grid.header" );
    grid.add( pathHeader );
    Label agentHeader = new Label( Messages.getString( "agent" ) );
    agentHeader.setStyleName( "grid.header" );
    grid.add( agentHeader );
    // add configuration file
    for ( Iterator configurationFileIterator = parent.getApplication().getConfigurationFiles().iterator();
          configurationFileIterator.hasNext(); )
    {
      ConfigurationFile configurationFile = (ConfigurationFile) configurationFileIterator.next();
      // row
      Row row = new Row();
      row.setCellSpacing( new Extent( 2 ) );
      row.setInsets( new Insets( 2 ) );
      grid.add( row );
      // copy
      Button copyButton = new Button( Styles.PAGE_COPY );
      copyButton.setActionCommand( configurationFile.getName() );
      copyButton.setToolTipText( Messages.getString( "copy" ) );
      copyButton.addActionListener( copy );
      row.add( copyButton );
      // active
      Button activeButton;
      if ( configurationFile.isActive() )
      {
        activeButton = new Button( Styles.LIGHTBULB );
        activeButton.setToolTipText( Messages.getString( "switch.disable" ) );
      }
      else
      {
        activeButton = new Button( Styles.LIGHTBULB_OFF );
        activeButton.setToolTipText( Messages.getString( "switch.enable" ) );
      }
      if ( parent.getParentPane().getEnvironmentWindow().adminPermission
        || parent.getParentPane().getEnvironmentWindow().jeeApplicationsPermission )
      {
        activeButton.setActionCommand( configurationFile.getName() );
        activeButton.addActionListener( toggleActive );
      }
      row.add( activeButton );
      // blocker
      Button blockerButton;
      if ( configurationFile.isBlocker() )
      {
        blockerButton = new Button( Styles.PLUGIN );
        blockerButton.setToolTipText( Messages.getString( "switch.notblocker" ) );
      }
      else
      {
        blockerButton = new Button( Styles.PLUGIN_DISABLED );
        blockerButton.setToolTipText( Messages.getString( "switch.blocker" ) );
      }
      if ( parent.getParentPane().getEnvironmentWindow().adminPermission
        || parent.getParentPane().getEnvironmentWindow().jeeApplicationsPermission )
      {
        blockerButton.setActionCommand( configurationFile.getName() );
        blockerButton.addActionListener( toggleBlocker );
      }
      row.add( blockerButton );
      // up
      Button upButton = new Button( Styles.ARROW_UP );
      upButton.setToolTipText( Messages.getString( "up" ) );
      if ( parent.getParentPane().getEnvironmentWindow().adminPermission
        || parent.getParentPane().getEnvironmentWindow().jeeApplicationsPermission )
      {
        upButton.setActionCommand( configurationFile.getName() );
        upButton.addActionListener( up );
      }
      row.add( upButton );
      // down
      Button downButton = new Button( Styles.ARROW_DOWN );
      downButton.setToolTipText( Messages.getString( "down" ) );
      if ( parent.getParentPane().getEnvironmentWindow().adminPermission
        || parent.getParentPane().getEnvironmentWindow().jeeApplicationsPermission )
      {
        downButton.setActionCommand( configurationFile.getName() );
        downButton.addActionListener( down );
      }
      row.add( downButton );
      // status
      Button statusButton = new Button( Styles.INFORMATION );
      statusButton.setToolTipText( Messages.getString( "status" ) );
      statusButton.setActionCommand( configurationFile.getName() );
      statusButton.addActionListener( status );
      row.add( statusButton );
      // update
      if ( parent.getParentPane().getEnvironmentWindow().adminPermission
        || parent.getParentPane().getEnvironmentWindow().jeeApplicationsUpdatePermission )
      {
        Button updateButton = new Button( Styles.COG );
        updateButton.setToolTipText( Messages.getString( "update" ) );
        updateButton.setActionCommand( configurationFile.getName() );
        updateButton.addActionListener( update );
        row.add( updateButton );
      }
      // delete
      if ( parent.getParentPane().getEnvironmentWindow().adminPermission
        || parent.getParentPane().getEnvironmentWindow().jeeApplicationsPermission )
      {
        Button deleteButton = new Button( Styles.DELETE );
        deleteButton.setToolTipText( Messages.getString( "delete" ) );
        deleteButton.setActionCommand( configurationFile.getName() );
        deleteButton.addActionListener( delete );
        row.add( deleteButton );
      }
      // name
      Button name = new Button( configurationFile.getName() );
      name.setActionCommand( configurationFile.getName() );
      name.addActionListener( edit );
      grid.add( name );
      // uri
      Label uri = new Label( configurationFile.getUri() );
      uri.setStyleName( "default" );
      grid.add( uri );
      // path
      Label path = new Label( configurationFile.getPath() );
      path.setStyleName( "default" );
      grid.add( path );
      // agent
      Label agent = new Label( configurationFile.getAgent() );
      agent.setStyleName( "default" );
      grid.add( agent );
    }
  }

  /**
   * Return the <code>ApplicationWindow</code> parent pane.
   *
   * @return the parent <code>ApplicationWindow</code>.
   */
  public ApplicationWindow getParentPane()
  {
    return parent;
  }

}
