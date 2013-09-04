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
import org.apache.kalumet.model.*;
import org.apache.kalumet.model.JEEApplicationServer;
import org.apache.kalumet.ws.client.JMSServerClient;

import java.util.Iterator;

/**
 * Environment JMS servers pane.
 */
public class JmsServersPane
  extends ContentPane
{

  private EnvironmentWindow parent;

  private SelectField scopeField;

  private Grid grid;

  // status thread
  class StatusThread
    extends Thread
  {

    public String serverName;

    public String jmsServerName;

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
        JMSServerClient client = new JMSServerClient( agent.getHostname(), agent.getPort() );
        boolean uptodate = client.check( parent.getEnvironmentName(), serverName, jmsServerName );
        if ( uptodate )
        {
          message = "JMS server " + jmsServerName + " is up to date.";
        }
        else
        {
          failure = true;
          message = "JMS server " + jmsServerName + " is not up to date.";
        }
      }
      catch ( Exception e )
      {
        failure = true;
        message = "JMS server " + jmsServerName + " status check failed: " + e.getMessage();
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

    public String serverName;

    public String jmsServerName;

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
        JMSServerClient client = new JMSServerClient( agent.getHostname(), agent.getPort() );
        client.update( parent.getEnvironmentName(), serverName, jmsServerName );
      }
      catch ( Exception e )
      {
        failure = true;
        message = "JMS server " + jmsServerName + " update failed: " + e.getMessage();
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
      if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeResourcesChangePermission)
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // looking for the JMS server object
      JMSServer jmsServer = parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServer(
        (String) scopeField.getSelectedItem() ).getJMSServer( event.getActionCommand() );
      if ( jmsServer == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "jmsserver.warn.notfound" ), getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // change the jms server state and add a change event
      if ( jmsServer.isActive() )
      {
        jmsServer.setActive( false );
        parent.getChangeEvents().add( "Disable JMS server " + jmsServer.getName() );
      }
      else
      {
        jmsServer.setActive( true );
        parent.getChangeEvents().add( "Enable JMS server " + jmsServer.getName() );
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
      // looking for the JMS server object
      JMSServer jmsServer = parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServer(
        (String) scopeField.getSelectedItem() ).getJMSServer( event.getActionCommand() );
      if ( jmsServer == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "jmsserver.notfound" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // change the jms server blocker state and add a change event
      if ( jmsServer.isBlocker() )
      {
        jmsServer.setBlocker( false );
        parent.getChangeEvents().add( "Set not blocker for JMS server " + jmsServer.getName() );
      }
      else
      {
        jmsServer.setBlocker( true );
        parent.getChangeEvents().add( "Set blocker for JMS server " + jmsServer.getName() );
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
      if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeResourcesChangePermission)
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // looking for the jms server object
      final JMSServer jmsServer = parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServer(
        (String) scopeField.getSelectedItem() ).getJMSServer( event.getActionCommand() );
      if ( jmsServer == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "jmsserver.notfound" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // display confirm window
      KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
        new ConfirmWindow( new ActionListener()
        {
          public void actionPerformed( ActionEvent event )
          {
            // delete the jms server object
            parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServer(
              (String) scopeField.getSelectedItem() ).getJMSServers().remove( jmsServer );
            // add a change event
            parent.getChangeEvents().add( "Delete JMS server " + jmsServer.getName() );
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
        "jmsserverwindow_" + parent.getEnvironmentName() + "_" + (String) scopeField.getSelectedItem() + "_"
          + event.getActionCommand() ) == null )
      {
        KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
          new JmsServerWindow( JmsServersPane.this, (String) scopeField.getSelectedItem(), event.getActionCommand() ) );
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
        new JmsServerWindow( JmsServersPane.this, (String) scopeField.getSelectedItem(), null ) );
    }
  };

  // copy
  private ActionListener copy = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // looking for the jms server object
      JMSServer jmsServer = parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServer(
        (String) scopeField.getSelectedItem() ).getJMSServer( event.getActionCommand() );
      if ( jmsServer == null )
      {
        return;
      }
      try
      {
        KalumetConsoleApplication.getApplication().setCopyComponent( jmsServer.clone() );
      }
      catch ( Exception e )
      {
        return;
      }
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
      // get the JEE server and JMS server name
      final String serverName = (String) scopeField.getSelectedItem();
      final String jmsServerName = event.getActionCommand();
      // add a message in the log pane and the journal
      KalumetConsoleApplication.getApplication().getLogPane().addInfo(
        "JMS server " + jmsServerName + " status check in progress...", parent.getEnvironmentName() );
      parent.getChangeEvents().add( "JMS server " + jmsServerName + " status check requested." );
      // start the status thread
      final StatusThread statusThread = new StatusThread();
      statusThread.serverName = serverName;
      statusThread.jmsServerName = jmsServerName;
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
      // check if the user has the lock
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
      // get the JEE server and JMS server name
      final String serverName = (String) scopeField.getSelectedItem();
      final String jmsServerName = event.getActionCommand();
      // display confirm window
      KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
        new ConfirmWindow( new ActionListener()
        {
          public void actionPerformed( ActionEvent event )
          {
            // add a message in the log pane and the journal
            KalumetConsoleApplication.getApplication().getLogPane().addInfo(
              "JMS server " + jmsServerName + " update in progress...", parent.getEnvironmentName() );
            parent.getChangeEvents().add( "JMS server " + jmsServerName + " update requested." );
            // start the update thread
            final UpdateThread updateThread = new UpdateThread();
            updateThread.serverName = serverName;
            updateThread.jmsServerName = jmsServerName;
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
                      "JMS server " + jmsServerName + " updated.", parent.getEnvironmentName() );
                    parent.getChangeEvents().add( "JMS server " + jmsServerName + " updated." );
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
   * Create a new <code>JmsServersPane</code>.
   *
   * @param parent the parent <code>EnvironmentWindow</code>.
   */
  public JmsServersPane( EnvironmentWindow parent )
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
    Label scopeLabel = new Label( Messages.getString( "scope" ) );
    scopeLabel.setStyleName( "default" );
    layoutGrid.add( scopeLabel );
    scopeField = new SelectField();
    scopeField.addActionListener( scopeSelect );
    scopeField.setStyleName( "default" );
    layoutGrid.add( scopeField );
    // populate the scope field
    DefaultListModel scopeModel = (DefaultListModel) scopeField.getModel();
    scopeModel.removeAll();
    for ( Iterator serversIterator =
            parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServers().iterator();
          serversIterator.hasNext(); )
    {
      JEEApplicationServer server = (JEEApplicationServer) serversIterator.next();
      scopeModel.add( server.getName() );
    }
    if ( scopeModel.size() > 0 )
    {
      scopeField.setSelectedIndex( 0 );
    }

    // add the create button
    if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeResourcesChangePermission)
    {
      Button createButton = new Button( Messages.getString( "jmsserver.add" ), Styles.ADD );
      createButton.setStyleName( "default" );
      createButton.addActionListener( create );
      content.add( createButton );
    }

    // add JMS servers grid
    grid = new Grid( 4 );
    grid.setStyleName( "border.grid" );
    grid.setColumnWidth( 0, new Extent( 50, Extent.PX ) );
    grid.setColumnWidth( 1, new Extent( 33, Extent.PERCENT ) );
    grid.setColumnWidth( 2, new Extent( 33, Extent.PERCENT ) );
    grid.setColumnWidth( 3, new Extent( 33, Extent.PERCENT ) );
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
    // update the scope field
    DefaultListModel scopeListModel = (DefaultListModel) scopeField.getModel();
    if ( scopeListModel.size() > 0 )
    {
      applicationServerName = (String) scopeField.getSelectedItem();
    }
    scopeListModel.removeAll();
    int scopeIndex = 0;
    int found = -1;
    for ( Iterator applicationServerIterator =
            parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServers().iterator();
          applicationServerIterator.hasNext(); )
    {
      JEEApplicationServer applicationServer = (JEEApplicationServer) applicationServerIterator.next();
      scopeListModel.add( applicationServer.getName() );
      if ( applicationServer.getName().equals( applicationServerName ) )
      {
        found = scopeIndex;
      }
      scopeIndex++;
    }

    // remove all JMS servers grid children
    grid.removeAll();

    // check if at least one application server is present
    if ( scopeListModel.size() < 1 )
    {
      return;
    }
    // update the scope field selected index
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

    // add JMS servers grid header
    Label actionHeader = new Label( " " );
    actionHeader.setStyleName( "grid.header" );
    grid.add( actionHeader );
    Label nameHeader = new Label( Messages.getString( "name" ) );
    nameHeader.setStyleName( "grid.header" );
    grid.add( nameHeader );
    Label queuesHeader = new Label( Messages.getString( "jmsqueues" ) );
    queuesHeader.setStyleName( "grid.header" );
    grid.add( queuesHeader );
    Label topicsHeader = new Label( Messages.getString( "jmstopics" ) );
    topicsHeader.setStyleName( "grid.header" );
    grid.add( topicsHeader );
    // add the jms servers
    for ( Iterator jmsServerIterator = parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServer(
      applicationServerName ).getJMSServers().iterator(); jmsServerIterator.hasNext(); )
    {
      JMSServer jmsServer = (JMSServer) jmsServerIterator.next();
      // row
      Row row = new Row();
      row.setInsets( new Insets( 2 ) );
      row.setCellSpacing( new Extent( 2 ) );
      grid.add( row );
      // copy
      Button copyButton = new Button( Styles.PAGE_COPY );
      copyButton.setToolTipText( Messages.getString( "copy" ) );
      copyButton.setActionCommand( jmsServer.getName() );
      copyButton.addActionListener( copy );
      row.add( copyButton );
      // active
      Button activeButton;
      if ( jmsServer.isActive() )
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
        activeButton.setActionCommand( jmsServer.getName() );
        activeButton.addActionListener( toggleActive );
      }
      row.add( activeButton );
      // blocker
      Button blockerButton;
      if ( jmsServer.isBlocker() )
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
        blockerButton.setActionCommand( jmsServer.getName() );
        blockerButton.addActionListener( toggleBlocker );
      }
      row.add( blockerButton );
      // status
      Button statusButton = new Button( Styles.INFORMATION );
      statusButton.setToolTipText( Messages.getString( "status" ) );
      statusButton.setActionCommand( jmsServer.getName() );
      statusButton.addActionListener( status );
      row.add( statusButton );
      if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeResourcesUpdatePermission )
      {
        // update
        Button updateButton = new Button( Styles.COG );
        updateButton.setToolTipText( Messages.getString( "update" ) );
        updateButton.setActionCommand( jmsServer.getName() );
        updateButton.addActionListener( update );
        row.add( updateButton );
      }
      if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeResourcesChangePermission)
      {
        // delete
        Button deleteButton = new Button( Styles.DELETE );
        deleteButton.setToolTipText( Messages.getString( "delete" ) );
        deleteButton.setActionCommand( jmsServer.getName() );
        deleteButton.addActionListener( delete );
        row.add( deleteButton );
      }
      // name
      Button jmsServerName = new Button( jmsServer.getName() );
      jmsServerName.setActionCommand( jmsServer.getName() );
      jmsServerName.addActionListener( edit );
      jmsServerName.setStyleName( "default" );
      grid.add( jmsServerName );
      // queues
      Column queuesColumn = new Column();
      grid.add( queuesColumn );
      for ( Iterator queueIterator = jmsServer.getJMSQueues().iterator(); queueIterator.hasNext(); )
      {
        JMSQueue queue = (JMSQueue) queueIterator.next();
        Label queueName = new Label( queue.getName() );
        queueName.setStyleName( "default" );
        queuesColumn.add( queueName );
      }
      // topics
      Column topicsColumn = new Column();
      grid.add( topicsColumn );
      for ( Iterator topicIterator = jmsServer.getJMSTopics().iterator(); topicIterator.hasNext(); )
      {
        JMSTopic topic = (JMSTopic) topicIterator.next();
        Label topicName = new Label( topic.getName() );
        topicName.setStyleName( "default" );
        topicsColumn.add( topicName );
      }
    }
  }

  /**
   * Return the parent <code>EnvironmentWindow</code>.
   *
   * @return the parent <code>EnvironmentWindow</code>.
   */
  public EnvironmentWindow getEnvironmentWindow()
  {
    return parent;
  }

}