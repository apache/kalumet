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
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.app.list.DefaultListModel;
import nextapp.echo2.app.list.ListModel;
import org.apache.kalumet.console.configuration.ConfigurationManager;
import org.apache.kalumet.model.Agent;
import org.apache.kalumet.model.JEEApplicationServer;
import org.apache.kalumet.model.JDBCConnectionPool;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.ws.client.JDBCConnectionPoolClient;

import java.util.Iterator;

/**
 * Environment JDBC connection pools pane.
 */
public class ConnectionPoolsPane
  extends ContentPane
{

  private EnvironmentWindow parent;

  private SelectField scopeField;

  private Grid grid;

  // status thread
  class StatusThread
    extends Thread
  {

    public String connectionPoolName;

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
        // call the web service
        JDBCConnectionPoolClient client = new JDBCConnectionPoolClient( agent.getHostname(), agent.getPort() );
        boolean uptodate =
          client.check( parent.getEnvironmentName(), (String) scopeField.getSelectedItem(), connectionPoolName );
        if ( uptodate )
        {
          message = "JDBC connection pool " + connectionPoolName + " is up to date.";
        }
        else
        {
          failure = true;
          message = "JDBC connection pool " + connectionPoolName + " is not up to date.";
        }
      }
      catch ( Exception e )
      {
        failure = true;
        message = "JDBC connection pool " + connectionPoolName + " status failed: " + e.getMessage();
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

    public String connectionPoolName;

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
        // call the web service
        JDBCConnectionPoolClient client = new JDBCConnectionPoolClient( agent.getHostname(), agent.getPort() );
        client.update( parent.getEnvironmentName(), (String) scopeField.getSelectedItem(), connectionPoolName );
      }
      catch ( Exception e )
      {
        failure = true;
        message = "JDBC Connection pool " + connectionPoolName + " update failed: " + e.getMessage();
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
      if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeResourcesChangePermission)
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // looking for the JDBC connection pool object
      final JDBCConnectionPool connectionPool =
        parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServer(
          (String) scopeField.getSelectedItem() ).getJDBCConnectionPool( event.getActionCommand() );
      if ( connectionPool == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "connectionpool.notfound" ), getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // display confirm window
      KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
        new ConfirmWindow( new ActionListener()
        {
          public void actionPerformed( ActionEvent event )
          {
            // delete the connection pool
            parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServer(
              (String) scopeField.getSelectedItem() ).getJDBCConnectionPools().remove( connectionPool );
            // add change event and update the log pane
            parent.getChangeEvents().add( "Delete JDBC connection pool " + connectionPool.getName() );
            // change the updated flag
            parent.setUpdated( true );
            // update the whole window
            parent.update();
          }
        } ) );
    }
  };

  // status
  private ActionListener status = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if some change has not yet been saved
      if ( getEnvironmentWindow().isUpdated() )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "environment.notsaved" ), getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // get JDBC connection pool name
      final String connectionPoolName = event.getActionCommand();
      // put a message in the log pane and in the journal
      KalumetConsoleApplication.getApplication().getLogPane().addInfo(
        "JDBC connection pool " + connectionPoolName + " status check in progress...", parent.getEnvironmentName() );
      parent.getChangeEvents().add( "JDBC connection pool " + connectionPoolName + " status check." );
      // start the status thread
      final StatusThread statusThread = new StatusThread();
      statusThread.connectionPoolName = connectionPoolName;
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
                                                                        parent.getEnvironmentName() );
                                                                    }
                                                                    else
                                                                    {
                                                                      KalumetConsoleApplication.getApplication().getLogPane().addInfo(
                                                                        statusThread.message,
                                                                        parent.getEnvironmentName() );
                                                                    }
                                                                    parent.getChangeEvents().add(
                                                                      statusThread.message );
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
      // check if the user has the log
      if ( !getEnvironmentWindow().getEnvironment().getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeResourcesUpdatePermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricited" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if something has been changed
      if ( getEnvironmentWindow().isUpdated() )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "environment.notsaved" ), getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // get JDBC connection pool name
      final String connectionPoolName = event.getActionCommand();
      // display confirm window
      KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
        new ConfirmWindow( new ActionListener()
        {
          public void actionPerformed( ActionEvent event )
          {
            // put a message in the log pane and the journal
            KalumetConsoleApplication.getApplication().getLogPane().addInfo(
              "JDBC connection pool " + connectionPoolName + " update in progress..." );
            parent.getChangeEvents().add( "JDBC connection pool " + connectionPoolName + " update requested." );
            // start the update thread
            final UpdateThread updateThread = new UpdateThread();
            updateThread.connectionPoolName = connectionPoolName;
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
                      "JDBC connection pool " + connectionPoolName + " updated.", parent.getEnvironmentName() );
                    parent.getChangeEvents().add( "JDBC connection pool " + connectionPoolName + " updated." );
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

  // edit
  private ActionListener edit = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      if ( KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().getComponent(
        "connectionpoolwindow_" + parent.getEnvironmentName() + "_" + (String) scopeField.getSelectedItem() + "_"
          + event.getActionCommand() ) == null )
      {
        KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
          new ConnectionPoolWindow( ConnectionPoolsPane.this, (String) scopeField.getSelectedItem(),
                                    event.getActionCommand() ) );
      }
    }
  };

  // create
  private ActionListener create = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      ListModel listModel = scopeField.getModel();
      if ( listModel.size() == 0 )
      {
        return;
      }
      KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
        new ConnectionPoolWindow( ConnectionPoolsPane.this, (String) scopeField.getSelectedItem(), null ) );
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
      if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeResourcesChangePermission)
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricited" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // looking for the connection pool object
      JDBCConnectionPool connectionPool = parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServer(
        (String) scopeField.getSelectedItem() ).getJDBCConnectionPool( event.getActionCommand() );
      if ( connectionPool == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "connectionpool.notfound" ), getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // change the connection pool state and add a change event
      if ( connectionPool.isActive() )
      {
        connectionPool.setActive( false );
        parent.getChangeEvents().add( "Disable JDBC connection pool " + connectionPool.getName() );
      }
      else
      {
        connectionPool.setActive( true );
        parent.getChangeEvents().add( "Enable JDBC connection pool " + connectionPool.getName() );
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
      if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeResourcesChangePermission)
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // looking for the connection pool object
      JDBCConnectionPool connectionPool = parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServer(
        (String) scopeField.getSelectedItem() ).getJDBCConnectionPool( event.getActionCommand() );
      if ( connectionPool == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "connectionpool.notfound" ), getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // change the connection pool blocker state and add a change event
      if ( connectionPool.isBlocker() )
      {
        connectionPool.setBlocker( false );
        parent.getChangeEvents().add( "Set not blocker for JDBC connection pool " + connectionPool.getName() );
      }
      else
      {
        connectionPool.setBlocker( true );
        parent.getChangeEvents().add( "Set blocker for JDBC connection pool " + connectionPool.getName() );
      }
      // change the updated flag
      parent.setUpdated( true );
      // update the journal log tab pane
      parent.updateJournalPane();
      // update the pane
      update();
    }
  };

  // copy
  private ActionListener copy = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // looking for the connection pool object
      JDBCConnectionPool connectionPool = parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServer(
        (String) scopeField.getSelectedItem() ).getJDBCConnectionPool( event.getActionCommand() );
      if ( connectionPool == null )
      {
        return;
      }
      try
      {
        KalumetConsoleApplication.getApplication().setCopyComponent( connectionPool.clone() );
      }
      catch ( Exception e )
      {
        return;
      }
    }
  };

  /**
   * Create a new <code>ConnectionPoolsPane</code>.
   *
   * @param parent the parent <code>EnvironmentWindow</code>.
   */
  public ConnectionPoolsPane( EnvironmentWindow parent )
  {
    super();
    setStyleName( "tab.content" );

    // update parent
    this.parent = parent;

    // column layout
    Column content = new Column();
    content.setCellSpacing( new Extent( 2 ) );
    add( content );

    // add the scope select field
    Grid layoutGrid = new Grid( 2 );
    layoutGrid.setStyleName( "default" );
    layoutGrid.setColumnWidth( 0, new Extent( 20, Extent.PERCENT ) );
    layoutGrid.setColumnWidth( 1, new Extent( 80, Extent.PERCENT ) );
    content.add( layoutGrid );
    Label applicationServerScope = new Label( Messages.getString( "scope" ) );
    applicationServerScope.setStyleName( "default" );
    layoutGrid.add( applicationServerScope );
    scopeField = new SelectField();
    scopeField.addActionListener( scopeSelect );
    scopeField.setStyleName( "default" );
    layoutGrid.add( scopeField );
    DefaultListModel scopeListModel = (DefaultListModel) scopeField.getModel();
    scopeListModel.removeAll();
    // add application servers in the scope select field
    for ( Iterator applicationServerIterator =
            parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServers().iterator();
          applicationServerIterator.hasNext(); )
    {
      JEEApplicationServer applicationServer = (JEEApplicationServer) applicationServerIterator.next();
      scopeListModel.add( applicationServer.getName() );
    }
    if ( scopeListModel.size() > 0 )
    {
      scopeField.setSelectedIndex( 0 );
    }

    // add the create button
    if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeResourcesChangePermission)
    {
      Button createButton = new Button( Messages.getString( "connectionpool.add" ), Styles.ADD );
      createButton.addActionListener( create );
      content.add( createButton );
    }

    // add JDBC connection pools grid
    grid = new Grid( 5 );
    grid.setStyleName( "border.grid" );
    grid.setColumnWidth( 0, new Extent( 50, Extent.PX ) );
    content.add( grid );

    // update
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
    int index = 0;
    int found = -1;
    for ( Iterator applicationServerIterator =
            parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServers().iterator();
          applicationServerIterator.hasNext(); )
    {
      JEEApplicationServer applicationServer = (JEEApplicationServer) applicationServerIterator.next();
      scopeListModel.add( applicationServer.getName() );
      if ( applicationServer.getName().equals( applicationServerName ) )
      {
        found = index;
      }
      index++;
    }
    // remove all JDBC connection pools grid children
    grid.removeAll();
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
    // add JDBC connection pools grid header
    Label actionHeader = new Label( " " );
    actionHeader.setStyleName( "grid.header" );
    grid.add( actionHeader );
    Label nameHeader = new Label( Messages.getString( "name" ) );
    nameHeader.setStyleName( "grid.header" );
    grid.add( nameHeader );
    Label typeHeader = new Label( Messages.getString( "type" ) );
    typeHeader.setStyleName( "grid.header" );
    grid.add( typeHeader );
    Label userHeader = new Label( Messages.getString( "user" ) );
    userHeader.setStyleName( "grid.header" );
    grid.add( userHeader );
    Label urlHeader = new Label( Messages.getString( "url" ) );
    urlHeader.setStyleName( "grid.header" );
    grid.add( urlHeader );

    // add the jdbc connection pools
    for ( Iterator jdbcConnectionPoolIterator =
            parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServer(
              applicationServerName ).getJDBCConnectionPools().iterator(); jdbcConnectionPoolIterator.hasNext(); )
    {
      JDBCConnectionPool connectionPool = (JDBCConnectionPool) jdbcConnectionPoolIterator.next();
      // name and actions
      Row row = new Row();
      row.setInsets( new Insets( 2 ) );
      row.setCellSpacing( new Extent( 2 ) );
      grid.add( row );
      // copy
      Button copyButton = new Button( Styles.PAGE_COPY );
      copyButton.setToolTipText( Messages.getString( "copy" ) );
      copyButton.setActionCommand( connectionPool.getName() );
      copyButton.addActionListener( copy );
      row.add( copyButton );
      // active
      Button activeButton;
      if ( connectionPool.isActive() )
      {
        activeButton = new Button( Styles.LIGHTBULB );
        activeButton.setToolTipText( Messages.getString( "switch.disable" ) );
      }
      else
      {
        activeButton = new Button( Styles.LIGHTBULB_OFF );
        activeButton.setToolTipText( Messages.getString( "switch.enable" ) );
      }
      if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeResourcesChangePermission)
      {
        activeButton.setActionCommand( connectionPool.getName() );
        activeButton.addActionListener( toggleActive );
      }
      row.add( activeButton );
      // blocker
      Button blockerButton;
      if ( connectionPool.isBlocker() )
      {
        blockerButton = new Button( Styles.PLUGIN );
        blockerButton.setToolTipText( Messages.getString( "switch.notblocker" ) );
      }
      else
      {
        blockerButton = new Button( Styles.PLUGIN_DISABLED );
        blockerButton.setToolTipText( Messages.getString( "switch.blocker" ) );
      }
      if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeResourcesChangePermission)
      {
        blockerButton.setActionCommand( connectionPool.getName() );
        blockerButton.addActionListener( toggleBlocker );
      }
      row.add( blockerButton );
      // status
      Button statusButton = new Button( Styles.INFORMATION );
      statusButton.setToolTipText( Messages.getString( "status" ) );
      statusButton.setActionCommand( connectionPool.getName() );
      statusButton.addActionListener( status );
      row.add( statusButton );
      // update
      if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeResourcesUpdatePermission )
      {
        Button updateButton = new Button( Styles.COG );
        updateButton.setToolTipText( Messages.getString( "update" ) );
        updateButton.setActionCommand( connectionPool.getName() );
        updateButton.addActionListener( update );
        row.add( updateButton );
      }
      // delete
      if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeResourcesChangePermission)
      {
        Button deleteButton = new Button( Styles.DELETE );
        deleteButton.setToolTipText( Messages.getString( "delete" ) );
        deleteButton.setActionCommand( connectionPool.getName() );
        deleteButton.addActionListener( delete );
        row.add( deleteButton );
      }
      // name
      Button connectionPoolName = new Button( connectionPool.getName() );
      connectionPoolName.setActionCommand( connectionPool.getName() );
      connectionPoolName.addActionListener( edit );
      grid.add( connectionPoolName );
      // connection pool type (driver)
      Label connectionPoolType = new Label( connectionPool.getDriver() );
      connectionPoolType.setStyleName( "default" );
      grid.add( connectionPoolType );
      // connection pool user
      Label connectionPoolUser = new Label( connectionPool.getUser() );
      connectionPoolUser.setStyleName( "default" );
      grid.add( connectionPoolUser );
      // connection pool url
      Label connectionPoolUrl = new Label( connectionPool.getUrl() );
      connectionPoolUrl.setStyleName( "default" );
      grid.add( connectionPoolUrl );
    }
  }

  /**
   * Get the environment window
   *
   * @return the parent <code>EnvironmentWindow</code>
   */
  public EnvironmentWindow getEnvironmentWindow()
  {
    return parent;
  }

}