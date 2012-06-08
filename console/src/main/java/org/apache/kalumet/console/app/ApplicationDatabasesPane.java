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
import org.apache.kalumet.model.Database;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.ws.client.DatabaseClient;

import java.util.Iterator;

/**
 * J2EE application databases pane.
 */
public class ApplicationDatabasesPane
  extends ContentPane
{

  private ApplicationWindow parent;

  private Grid grid;

  class UpdateThread
    extends Thread
  {

    public String databaseName;

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
        // call the webservice
        DatabaseClient client = new DatabaseClient( agent.getHostname(), agent.getPort() );
        client.update( parent.getParentPane().getEnvironmentWindow().getEnvironmentName(), parent.getServerName(),
                       parent.getApplicationName(), databaseName, false );
      }
      catch ( Exception e )
      {
        failure = true;
        message = "J2EE application " + parent.getApplicationName() + " database " + databaseName + " update failed: "
          + e.getMessage();
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
      // looking for the database object
      Database database = parent.getApplication().getDatabase( event.getActionCommand() );
      if ( database == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "database.notfound" ),
                                                                            parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // change the state and add a change event
      if ( database.isActive() )
      {
        database.setActive( false );
        parent.getParentPane().getEnvironmentWindow().getChangeEvents().add( "Disable database " + database.getName() );
      }
      else
      {
        database.setActive( true );
        parent.getParentPane().getEnvironmentWindow().getChangeEvents().add( "Enable database " + database.getName() );
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
      // looking for the database object
      Database database = parent.getApplication().getDatabase( event.getActionCommand() );
      if ( database == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "database.notfound" ),
                                                                            parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // change the blocker state and add a change event
      if ( database.isBlocker() )
      {
        database.setBlocker( false );
        parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
          "Set not blocker for database " + database.getName() );
      }
      else
      {
        database.setBlocker( true );
        parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
          "Set blocker for database " + database.getName() );
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
      // looking for the database file object
      final Database database = parent.getApplication().getDatabase( event.getActionCommand() );
      if ( database == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "database.notfound" ),
                                                                            parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // display confirm window
      KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
        new ConfirmWindow( new ActionListener()
        {
          public void actionPerformed( ActionEvent event )
          {
            // delete the database object
            parent.getApplication().getDatabases().remove( database );
            // add a change event
            parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
              "Delete database " + database.getName() );
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
        "databasewindow_" + parent.getParentPane().getEnvironmentWindow().getEnvironmentName() + "_"
          + parent.getServerName() + "_" + parent.getApplicationName() + "_" + event.getActionCommand() ) == null )
      {
        KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
          new ApplicationDatabaseWindow( ApplicationDatabasesPane.this, event.getActionCommand() ) );
      }
    }
  };

  // create
  private ActionListener create = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
        new ApplicationDatabaseWindow( ApplicationDatabasesPane.this, null ) );
    }
  };

  // copy
  private ActionListener copy = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // looking for the database file object
      Database database = parent.getApplication().getDatabase( event.getActionCommand() );
      if ( database == null )
      {
        return;
      }
      try
      {
        KalumetConsoleApplication.getApplication().setCopyComponent( database.clone() );
      }
      catch ( Exception e )
      {
        return;
      }
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
      // check if some change has not been saved
      if ( parent.getParentPane().getEnvironmentWindow().isUpdated() )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "environment.notsaved" ),
          parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      final String databaseName = event.getActionCommand();
      // display confirm window
      KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
        new ConfirmWindow( new ActionListener()
        {
          public void actionPerformed( ActionEvent event )
          {
            // add a message in the log pane and the journal
            KalumetConsoleApplication.getApplication().getLogPane().addInfo(
              "J2EE application " + parent.getApplicationName() + " database " + databaseName
                + " update in progress...", parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
            parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
              "J2EE application " + parent.getApplicationName() + " database " + databaseName + " update requested." );
            // start the update thread
            final UpdateThread updateThread = new UpdateThread();
            updateThread.databaseName = databaseName;
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
                      "J2EE application " + parent.getApplicationName() + " database " + databaseName + " updated.",
                      parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
                    parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
                      "J2EE application " + parent.getApplicationName() + " database " + databaseName + " updated." );
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
   * Create a new <code>ApplicationDatabasesPane</code>.
   *
   * @param parent the parent <code>ApplicationWindow</code>.
   */
  public ApplicationDatabasesPane( ApplicationWindow parent )
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
      Button createButton = new Button( Messages.getString( "database.add" ), Styles.ADD );
      createButton.addActionListener( create );
      content.add( createButton );
    }

    // add the databases grid
    grid = new Grid( 8 );
    grid.setStyleName( "border.grid" );
    content.add( grid );

    // update the pane
    update();
  }

  /**
   * Update the pane.
   */
  public void update()
  {
    // remove all databases grid children
    grid.removeAll();
    // add databases grid header
    // action header
    Label actionHeader = new Label( " " );
    actionHeader.setStyleName( "grid.header" );
    grid.add( actionHeader );
    // name header
    Label nameHeader = new Label( Messages.getString( "name" ) );
    nameHeader.setStyleName( "grid.header" );
    grid.add( nameHeader );
    // driver header
    Label driverHeader = new Label( Messages.getString( "driver" ) );
    driverHeader.setStyleName( "grid.header" );
    grid.add( driverHeader );
    // user header
    Label userHeader = new Label( Messages.getString( "user" ) );
    userHeader.setStyleName( "grid.header" );
    grid.add( userHeader );
    // URL header
    Label urlHeader = new Label( Messages.getString( "url" ) );
    urlHeader.setStyleName( "grid.header" );
    grid.add( urlHeader );
    // connection pool header
    Label connectionPoolHeader = new Label( Messages.getString( "connectionpool" ) );
    connectionPoolHeader.setStyleName( "grid.header" );
    grid.add( connectionPoolHeader );
    // SQL command header
    Label sqlCommandHeader = new Label( Messages.getString( "sql.command" ) );
    sqlCommandHeader.setStyleName( "grid.header" );
    grid.add( sqlCommandHeader );
    // agent header
    Label agentHeader = new Label( Messages.getString( "agent" ) );
    agentHeader.setStyleName( "grid.header" );
    grid.add( agentHeader );
    // add database
    for ( Iterator databaseIterator = parent.getApplication().getDatabases().iterator(); databaseIterator.hasNext(); )
    {
      Database database = (Database) databaseIterator.next();
      // row
      Row row = new Row();
      row.setCellSpacing( new Extent( 2 ) );
      row.setInsets( new Insets( 2 ) );
      grid.add( row );
      // copy
      Button copyButton = new Button( Styles.PAGE_COPY );
      copyButton.setToolTipText( Messages.getString( "copy" ) );
      copyButton.setActionCommand( database.getName() );
      copyButton.addActionListener( copy );
      row.add( copyButton );
      // active
      Button activeButton;
      if ( database.isActive() )
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
        activeButton.setActionCommand( database.getName() );
        activeButton.addActionListener( toggleActive );
      }
      row.add( activeButton );
      // blocker
      Button blockerButton;
      if ( database.isBlocker() )
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
        blockerButton.setActionCommand( database.getName() );
        blockerButton.addActionListener( toggleBlocker );
      }
      row.add( blockerButton );
      // update
      if ( parent.getParentPane().getEnvironmentWindow().adminPermission
        || parent.getParentPane().getEnvironmentWindow().jeeApplicationsUpdatePermission )
      {
        Button updateButton = new Button( Styles.COG );
        updateButton.setToolTipText( Messages.getString( "update" ) );
        updateButton.setActionCommand( database.getName() );
        updateButton.addActionListener( update );
        row.add( updateButton );
      }
      // delete
      if ( parent.getParentPane().getEnvironmentWindow().adminPermission
        || parent.getParentPane().getEnvironmentWindow().jeeApplicationsPermission )
      {
        Button deleteButton = new Button( Styles.DELETE );
        deleteButton.setToolTipText( Messages.getString( "delete" ) );
        deleteButton.setActionCommand( database.getName() );
        deleteButton.addActionListener( delete );
        row.add( deleteButton );
      }
      // name
      Button databaseName = new Button( database.getName() );
      databaseName.setStyleName( "default" );
      databaseName.setActionCommand( database.getName() );
      databaseName.addActionListener( edit );
      grid.add( databaseName );
      // driver
      Label databaseDriver = new Label( database.getDriver() );
      databaseDriver.setStyleName( "default" );
      grid.add( databaseDriver );
      // user
      Label databaseUser = new Label( database.getUser() );
      databaseUser.setStyleName( "default" );
      grid.add( databaseUser );
      // URL
      Label databaseUrl = new Label( database.getJdbcurl() );
      databaseUrl.setStyleName( "default" );
      grid.add( databaseUrl );
      // connection pool
      Label connectionPool = new Label( database.getConnectionPool() );
      connectionPool.setStyleName( "default" );
      grid.add( connectionPool );
      // SQL command
      Label sqlCommand = new Label( database.getSqlCommand() );
      sqlCommand.setStyleName( "default" );
      grid.add( sqlCommand );
      // agent
      Label agent = new Label( database.getAgent() );
      agent.setStyleName( "default" );
      grid.add( agent );
    }
  }

  /**
   * Return the parent <code>ApplicationWindow</code>.
   *
   * @return the parent <code>ApplicationWindow</code>.
   */
  public ApplicationWindow getParentPane()
  {
    return parent;
  }

}
