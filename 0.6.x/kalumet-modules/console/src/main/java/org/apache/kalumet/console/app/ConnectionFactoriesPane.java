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
import org.apache.kalumet.console.configuration.ConfigurationManager;
import org.apache.kalumet.model.Agent;
import org.apache.kalumet.model.JEEApplicationServer;
import org.apache.kalumet.model.JMSConnectionFactory;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.ws.client.JMSConnectionFactoryClient;

import java.util.Iterator;

/**
 * Environment JMS connection factories pane.
 */
public class ConnectionFactoriesPane
  extends ContentPane
{

  private EnvironmentWindow parent;

  private SelectField scopeField;

  private Grid grid;

  private boolean newIsActive = true;

  private boolean newIsBlocker = false;

  private TextField newNameField;

  // status thread
  class StatusThread
    extends Thread
  {

    public String serverName;

    public String connectionFactoryName;

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
        JMSConnectionFactoryClient client = new JMSConnectionFactoryClient( agent.getHostname(), agent.getPort() );
        boolean uptodate = client.check( parent.getEnvironmentName(), serverName, connectionFactoryName );
        if ( uptodate )
        {
          message = "JMS connection factory " + connectionFactoryName + " is up to date.";
        }
        else
        {
          failure = true;
          message = "JMS connection factory " + connectionFactoryName + " is not up to date.";
        }
      }
      catch ( Exception e )
      {
        failure = true;
        message = "JMS connection factory " + connectionFactoryName + " status check failed: " + e.getMessage();
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

    public String connectionFactoryName;

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
        JMSConnectionFactoryClient client = new JMSConnectionFactoryClient( agent.getHostname(), agent.getPort() );
        client.update( parent.getEnvironmentName(), serverName, connectionFactoryName );
      }
      catch ( Exception e )
      {
        failure = true;
        message = "JMS connection factory " + connectionFactoryName + " update failed: " + e.getMessage();
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

  // edit
  private ActionListener edit = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check the user has the environment lock
      if ( !getEnvironementWindow().getEnvironment().getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            getEnvironementWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !getEnvironementWindow().adminPermission && !getEnvironementWindow().jeeResourcesChangePermission)
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restrictied" ),
                                                                            getEnvironementWindow().getEnvironmentName() );
        return;
      }
      // get the JMS connection factory name
      String name = event.getActionCommand();
      // get the JMS connection factory name field
      TextField nameField = (TextField) ConnectionFactoriesPane.this.getComponent(
        "cfname_" + parent.getEnvironmentName() + "_" + (String) scopeField.getSelectedItem() + "_" + name );
      String nameFieldValue = nameField.getText();
      // check the field value
      if ( nameFieldValue == null || nameFieldValue.trim().length() < 1 )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "connectionfactory.mandatory" ), getEnvironementWindow().getEnvironmentName() );
        return;
      }
      // if the use change the JMS connection factory name, check if the name
      // if not already in use
      if ( !name.equals( nameFieldValue ) )
      {
        if ( parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServer(
          (String) scopeField.getSelectedItem() ).getJMSConnectionFactory( nameFieldValue ) != null )
        {
          KalumetConsoleApplication.getApplication().getLogPane().addWarning(
            Messages.getString( "connectionfactory.exists" ), getEnvironementWindow().getEnvironmentName() );
          return;
        }
      }
      // looking for the JMS connection factory object
      JMSConnectionFactory connectionFactory =
        parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServer(
          (String) scopeField.getSelectedItem() ).getJMSConnectionFactory( name );
      if ( connectionFactory == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "connectionfactory.notfound" ), getEnvironementWindow().getEnvironmentName() );
        return;
      }
      // add a change event
      parent.getChangeEvents().add( "Change JMS connection factory " + connectionFactory.getName() );
      // update the JMS connection factory object
      connectionFactory.setName( nameFieldValue );
      // change the updated flag
      parent.setUpdated( true );
      // update the journal log tab pane
      parent.updateJournalPane();
      // update only the pane
      update();
    }
  };

  // create
  private ActionListener create = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if the user has the environment lock
      if ( !getEnvironementWindow().getEnvironment().getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            getEnvironementWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !getEnvironementWindow().adminPermission && !getEnvironementWindow().jeeResourcesChangePermission)
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironementWindow().getEnvironmentName() );
        return;
      }
      // get the new JMS connection factory name field value
      String newNameFieldValue = newNameField.getText();
      // check the mandatory field
      if ( newNameFieldValue == null || newNameFieldValue.trim().length() < 1 )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "connectionfactory.mandatory" ), getEnvironementWindow().getEnvironmentName() );
        return;
      }
      // create a new JMS connection factory object
      JMSConnectionFactory connectionFactory = new JMSConnectionFactory();
      connectionFactory.setName( newNameFieldValue );
      connectionFactory.setActive( newIsActive );
      // add the JMS connection factory
      try
      {
        parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServer(
          (String) scopeField.getSelectedItem() ).addJMSConnectionFactory( connectionFactory );
      }
      catch ( Exception e )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "connectionfactory.exists" ), getEnvironementWindow().getEnvironmentName() );
        return;
      }
      // add a change event
      parent.getChangeEvents().add( "Create JMS connection factory " + connectionFactory.getName() );
      // change the updated flag
      parent.setUpdated( true );
      // update the journal log tab pane
      parent.updateJournalPane();
      // update only the pane
      update();
    }
  };

  // delete
  private ActionListener delete = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if the user has the environment lock
      if ( !getEnvironementWindow().getEnvironment().getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            getEnvironementWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !getEnvironementWindow().adminPermission && !getEnvironementWindow().jeeResourcesChangePermission)
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironementWindow().getEnvironmentName() );
        return;
      }
      // get the JMS connection factory name
      final String name = event.getActionCommand();
      // display confirm window
      KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
        new ConfirmWindow( new ActionListener()
        {
          public void actionPerformed( ActionEvent event )
          {
            // looking for the JMS connection factory object
            JMSConnectionFactory connectionFactory =
              parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServer(
                (String) scopeField.getSelectedItem() ).getJMSConnectionFactory( name );
            if ( connectionFactory == null )
            {
              KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                Messages.getString( "connectionfactory.notfound" ), getEnvironementWindow().getEnvironmentName() );
              return;
            }
            // delete the JMS connection factory
            parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServer(
              (String) scopeField.getSelectedItem() ).getJMSConnectionFactories().remove( connectionFactory );
            // add a change event
            parent.getChangeEvents().add( "Delete JMS connection factory " + connectionFactory.getName() );
            // change the updated flag
            parent.setUpdated( true );
            // update the journal log tab pane
            parent.updateJournalPane();
            // update only the pane
            update();
          }
        } ) );
    }
  };

  // toggle active
  private ActionListener toggleActive = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if the user has the environment lock
      if ( !getEnvironementWindow().getEnvironment().getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            getEnvironementWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !getEnvironementWindow().adminPermission && !getEnvironementWindow().jeeResourcesChangePermission)
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironementWindow().getEnvironmentName() );
        return;
      }
      // looking for the JMS connection factory object
      JMSConnectionFactory connectionFactory =
        parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServer(
          (String) scopeField.getSelectedItem() ).getJMSConnectionFactory( event.getActionCommand() );
      if ( connectionFactory == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "connectionfactory.notfound" ), getEnvironementWindow().getEnvironmentName() );
        return;
      }
      // change the jms connection factory state and add a change event
      if ( connectionFactory.isActive() )
      {
        connectionFactory.setActive( false );
        parent.getChangeEvents().add( "Disable JMS connection factory " + connectionFactory.getName() );
      }
      else
      {
        connectionFactory.setActive( true );
        parent.getChangeEvents().add( "Enable JMS connection factory " + connectionFactory.getName() );
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
      if ( !getEnvironementWindow().getEnvironment().getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            getEnvironementWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !getEnvironementWindow().adminPermission && !getEnvironementWindow().jeeResourcesChangePermission)
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironementWindow().getEnvironmentName() );
        return;
      }
      // looking for the JMS connection factory object
      JMSConnectionFactory connectionFactory =
        parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServer(
          (String) scopeField.getSelectedItem() ).getJMSConnectionFactory( event.getActionCommand() );
      if ( connectionFactory == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "connectionfactory.notfound" ), getEnvironementWindow().getEnvironmentName() );
        return;
      }
      // change the jms connection factory blocker state and add a change
      // event
      if ( connectionFactory.isBlocker() )
      {
        connectionFactory.setBlocker( false );
        parent.getChangeEvents().add( "Set not blocker for JMS connection factory " + connectionFactory.getName() );
      }
      else
      {
        connectionFactory.setBlocker( true );
        parent.getChangeEvents().add( "Set blocker for JMS connection factory " + connectionFactory.getName() );
      }
      // change the updated flag
      parent.setUpdated( true );
      // update the journal log tab pane
      parent.updateJournalPane();
      // update the pane
      update();
    }
  };

  // new toggle active
  private ActionListener newToggleActive = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // toggle the state
      if ( newIsActive )
      {
        newIsActive = false;
      }
      else
      {
        newIsActive = true;
      }
      // update the pane
      update();
    }
  };

  // new toggle blocker
  private ActionListener newToggleBlocker = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // toggle the blocker state
      if ( newIsBlocker )
      {
        newIsBlocker = false;
      }
      else
      {
        newIsBlocker = true;
      }
      // update the pane
      update();
    }
  };

  // copy
  private ActionListener copy = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // looking for the JMS connection factory
      JMSConnectionFactory connectionFactory =
        parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServer(
          (String) scopeField.getSelectedItem() ).getJMSConnectionFactory( event.getActionCommand() );
      if ( connectionFactory == null )
      {
        return;
      }
      try
      {
        KalumetConsoleApplication.getApplication().setCopyComponent( connectionFactory.clone() );
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
      // check if the copy object is correct
      if ( copy == null || !( copy instanceof JMSConnectionFactory ) )
      {
        return;
      }
      // update the new fields
      newNameField.setText( ( (JMSConnectionFactory) copy ).getName() );
    }
  };

  // status
  private ActionListener status = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if some has not yet been saved
      if ( getEnvironementWindow().isUpdated() )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "environment.notsaved" ), getEnvironementWindow().getEnvironmentName() );
        return;
      }
      // get the connection factory
      String connectionFactoryName = event.getActionCommand();
      String serverName = (String) scopeField.getSelectedItem();
      // add a message into the log pane and the journal
      KalumetConsoleApplication.getApplication().getLogPane().addInfo(
        "JMS connection factory " + connectionFactoryName + " status check in progress...",
        parent.getEnvironmentName() );
      parent.getChangeEvents().add( "JMS connection factory " + connectionFactoryName + " status check requested." );
      // start the status thread
      final StatusThread statusThread = new StatusThread();
      statusThread.serverName = serverName;
      statusThread.connectionFactoryName = connectionFactoryName;
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
      if ( !getEnvironementWindow().getEnvironment().getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            getEnvironementWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !getEnvironementWindow().adminPermission && !getEnvironementWindow().jeeResourcesUpdatePermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironementWindow().getEnvironmentName() );
        return;
      }
      // check if some change has not been saved
      if ( getEnvironementWindow().isUpdated() )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "environment.notsaved" ), getEnvironementWindow().getEnvironmentName() );
        return;
      }
      // get the connection factory and server name
      final String serverName = (String) scopeField.getSelectedItem();
      final String connectionFactoryName = event.getActionCommand();
      // display confirm window
      KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
        new ConfirmWindow( new ActionListener()
        {
          public void actionPerformed( ActionEvent event )
          {
            // add a message into the log pane and the journal
            KalumetConsoleApplication.getApplication().getLogPane().addInfo(
              "JMS connection factory " + connectionFactoryName + " update in progress...",
              parent.getEnvironmentName() );
            parent.getChangeEvents().add( "JMS connection factory " + connectionFactoryName + " update requested." );
            // start the update thread
            final UpdateThread updateThread = new UpdateThread();
            updateThread.serverName = serverName;
            updateThread.connectionFactoryName = connectionFactoryName;
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
                      "JMS connection factory " + connectionFactoryName + " updated.", parent.getEnvironmentName() );
                    parent.getChangeEvents().add( "JMS connection factory " + connectionFactoryName + " updated." );
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
   * Create a new <code>ConnectionFactoriesPane</code>.
   *
   * @param parent the parent <code>EnvironmentWindow</code>.
   */
  public ConnectionFactoriesPane( EnvironmentWindow parent )
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

    // add JMS connection factories grid
    grid = new Grid( 2 );
    grid.setStyleName( "border.grid" );
    grid.setColumnWidth( 0, new Extent( 50, Extent.PX ) );
    grid.setColumnWidth( 1, new Extent( 100, Extent.PERCENT ) );
    content.add( grid );

    // update
    update();
  }

  /**
   * Update the pane.
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

    // remove all JMS connection factories grid children
    grid.removeAll();

    if ( scopeListModel.size() < 1 )
    {
      // no application server present
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

    // add JMS connection factories grid header
    Label connectionFactoryHeader = new Label( " " );
    connectionFactoryHeader.setStyleName( "grid.header" );
    grid.add( connectionFactoryHeader );
    Label connectionFactoryNameHeader = new Label( Messages.getString( "name" ) );
    connectionFactoryNameHeader.setStyleName( "grid.header" );
    grid.add( connectionFactoryNameHeader );
    // add the jms connection factories
    for ( Iterator jmsConnectionFactoryIterator =
            parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServer(
              applicationServerName ).getJMSConnectionFactories().iterator(); jmsConnectionFactoryIterator.hasNext(); )
    {
      JMSConnectionFactory connectionFactory = (JMSConnectionFactory) jmsConnectionFactoryIterator.next();
      // row
      Row row = new Row();
      row.setInsets( new Insets( 2 ) );
      row.setCellSpacing( new Extent( 2 ) );
      grid.add( row );
      // copy
      Button copyButton = new Button( Styles.PAGE_COPY );
      copyButton.setToolTipText( Messages.getString( "copy" ) );
      copyButton.setActionCommand( connectionFactory.getName() );
      copyButton.addActionListener( copy );
      row.add( copyButton );
      // active
      Button activeButton;
      if ( connectionFactory.isActive() )
      {
        activeButton = new Button( Styles.LIGHTBULB );
        activeButton.setToolTipText( Messages.getString( "switch.disable" ) );
      }
      else
      {
        activeButton = new Button( Styles.LIGHTBULB_OFF );
        activeButton.setToolTipText( Messages.getString( "switch.enable" ) );
      }
      if ( getEnvironementWindow().adminPermission || getEnvironementWindow().jeeResourcesChangePermission)
      {
        activeButton.setActionCommand( connectionFactory.getName() );
        activeButton.addActionListener( toggleActive );
      }
      row.add( activeButton );
      // blocker
      Button blockerButton;
      if ( connectionFactory.isBlocker() )
      {
        blockerButton = new Button( Styles.PLUGIN );
        blockerButton.setToolTipText( Messages.getString( "switch.notblocker" ) );
      }
      else
      {
        blockerButton = new Button( Styles.PLUGIN_DISABLED );
        blockerButton.setToolTipText( Messages.getString( "switch.blocker" ) );
      }
      if ( getEnvironementWindow().adminPermission || getEnvironementWindow().jeeResourcesChangePermission)
      {
        blockerButton.setActionCommand( connectionFactory.getName() );
        blockerButton.addActionListener( toggleBlocker );
      }
      row.add( blockerButton );
      // status
      Button statusButton = new Button( Styles.INFORMATION );
      statusButton.setToolTipText( Messages.getString( "status" ) );
      statusButton.setActionCommand( connectionFactory.getName() );
      statusButton.addActionListener( status );
      row.add( statusButton );
      if ( getEnvironementWindow().adminPermission || getEnvironementWindow().jeeResourcesUpdatePermission )
      {
        // update
        Button updateButton = new Button( Styles.COG );
        updateButton.setToolTipText( Messages.getString( "update" ) );
        updateButton.setActionCommand( connectionFactory.getName() );
        updateButton.addActionListener( update );
        row.add( updateButton );
      }
      if ( getEnvironementWindow().adminPermission || getEnvironementWindow().jeeResourcesChangePermission)
      {
        // edit
        Button editButton = new Button( Styles.ACCEPT );
        editButton.setToolTipText( Messages.getString( "apply" ) );
        editButton.setActionCommand( connectionFactory.getName() );
        editButton.addActionListener( edit );
        row.add( editButton );
        // delete
        Button deleteButton = new Button( Styles.DELETE );
        deleteButton.setToolTipText( Messages.getString( "delete" ) );
        deleteButton.setActionCommand( connectionFactory.getName() );
        deleteButton.addActionListener( delete );
        row.add( deleteButton );
      }
      // name
      TextField nameField = new TextField();
      nameField.setId(
        "cfname_" + parent.getEnvironmentName() + "_" + applicationServerName + "_" + connectionFactory.getName() );
      nameField.setStyleName( "default" );
      nameField.setWidth( new Extent( 100, Extent.PERCENT ) );
      nameField.setText( connectionFactory.getName() );
      grid.add( nameField );
    }

    // add the blank row to create a new JMS connection factory
    if ( getEnvironementWindow().adminPermission || getEnvironementWindow().jeeResourcesChangePermission)
    {
      // row
      Row row = new Row();
      row.setInsets( new Insets( 2 ) );
      row.setCellSpacing( new Extent( 2 ) );
      grid.add( row );
      // paste
      Button pasteButton = new Button( Styles.PAGE_PASTE );
      pasteButton.setToolTipText( Messages.getString( "paste" ) );
      pasteButton.addActionListener( paste );
      row.add( pasteButton );
      // active
      Button activeButton;
      if ( newIsActive )
      {
        activeButton = new Button( Styles.LIGHTBULB );
        activeButton.setToolTipText( Messages.getString( "switch.disable" ) );
      }
      else
      {
        activeButton = new Button( Styles.LIGHTBULB_OFF );
        activeButton.setToolTipText( Messages.getString( "switch.enable" ) );
      }
      activeButton.addActionListener( newToggleActive );
      row.add( activeButton );
      // blocker
      Button blockerButton;
      if ( newIsBlocker )
      {
        blockerButton = new Button( Styles.PLUGIN );
        blockerButton.setToolTipText( Messages.getString( "switch.notblocker" ) );
      }
      else
      {
        blockerButton = new Button( Styles.PLUGIN_DISABLED );
        blockerButton.setToolTipText( Messages.getString( "switch.blocker" ) );
      }
      blockerButton.addActionListener( newToggleBlocker );
      row.add( blockerButton );
      // add
      Button addButton = new Button( Styles.ADD );
      addButton.setToolTipText( Messages.getString( "add" ) );
      addButton.addActionListener( create );
      row.add( addButton );
      // name
      newNameField = new TextField();
      newNameField.setStyleName( "default" );
      newNameField.setWidth( new Extent( 100, Extent.PERCENT ) );
      grid.add( newNameField );
    }
  }

  public EnvironmentWindow getEnvironementWindow()
  {
    return parent;
  }

}