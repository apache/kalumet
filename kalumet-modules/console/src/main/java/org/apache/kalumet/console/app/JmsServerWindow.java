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
import org.apache.kalumet.console.configuration.ConfigurationManager;
import org.apache.kalumet.model.Agent;
import org.apache.kalumet.model.JMSServer;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.ws.client.JMSServerClient;

/**
 * JMS server window.
 */
public class JmsServerWindow
  extends WindowPane
{

  private String jmsServerName;

  private String serverName;

  private JMSServer jmsServer = null;

  private JmsServersPane parent;

  private JmsServerGeneralPane generalPane;

  private JmsQueuesPane jmsQueuesPane;

  private JmsTopicsPane jmsTopicsPane;

  // status thread
  class StatusThread
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
        Agent agent = kalumet.getAgent( parent.getEnvironmentWindow().getEnvironment().getAgent() );
        if ( agent == null )
        {
          throw new IllegalArgumentException( "agent not found." );
        }
        // call the webservice
        JMSServerClient client = new JMSServerClient( agent.getHostname(), agent.getPort() );
        boolean uptodate =
          client.check( parent.getEnvironmentWindow().getEnvironmentName(), serverName, jmsServerName );
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
        Agent agent = kalumet.getAgent( parent.getEnvironmentWindow().getEnvironment().getAgent() );
        if ( agent == null )
        {
          throw new IllegalArgumentException( "agent not found." );
        }
        // call the webservice
        JMSServerClient client = new JMSServerClient( agent.getHostname(), agent.getPort() );
        client.update( parent.getEnvironmentWindow().getEnvironmentName(), serverName, jmsServerName );
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

  // close
  private ActionListener close = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      JmsServerWindow.this.userClose();
    }
  };

  // refresh
  private ActionListener refresh = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // looking for original JMS server object
      jmsServer = parent.getEnvironmentWindow().getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(
        serverName ).getJMSServer( jmsServerName );
      if ( jmsServer == null )
      {
        jmsServer = new JMSServer();
      }
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
      if ( !getEnvironmentWindow().getEnvironment().getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeResourcesPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // display confirm window
      KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
        new ConfirmWindow( new ActionListener()
        {
          public void actionPerformed( ActionEvent event )
          {
            // remove the jms server
            parent.getEnvironmentWindow().getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(
              serverName ).getJMSServers().remove( jmsServer );
            // add a change event
            parent.getEnvironmentWindow().getChangeEvents().add( "Delete JMS server " + jmsServer.getName() );
            // change the updated flag
            parent.getEnvironmentWindow().setUpdated( true );
            // update the journal log tab pane
            parent.getEnvironmentWindow().updateJournalPane();
            // update the parent pane
            parent.update();
            // close the window
            JmsServerWindow.this.userClose();
          }
        } ) );
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
      if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeResourcesPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // get fields value
      String nameFieldValue = generalPane.getNameField().getText();
      int activeFieldIndex = generalPane.getActiveField().getSelectedIndex();
      int blockerFieldIndex = generalPane.getBlockerField().getSelectedIndex();
      // check name field
      if ( nameFieldValue == null || nameFieldValue.trim().length() < 1 )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "jmsserver.mandatory" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // if the user change the JMS server name, check if the JMS server name
      // doesn't already exist
      if ( jmsServerName == null || ( jmsServerName != null && !jmsServerName.equals( nameFieldValue ) ) )
      {
        if ( parent.getEnvironmentWindow().getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(
          serverName ).getJMSServer( nameFieldValue ) != null )
        {
          KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "jmsserver.exists" ),
                                                                              getEnvironmentWindow().getEnvironmentName() );
          return;
        }
      }
      // add a change event
      if ( jmsServerName != null )
      {
        parent.getEnvironmentWindow().getChangeEvents().add( "Change JMS server " + jmsServer.getName() );
      }
      // update the jms server object
      jmsServer.setName( nameFieldValue );
      if ( activeFieldIndex == 0 )
      {
        jmsServer.setActive( true );
      }
      else
      {
        jmsServer.setActive( false );
      }
      if ( blockerFieldIndex == 0 )
      {
        jmsServer.setBlocker( true );
      }
      else
      {
        jmsServer.setBlocker( false );
      }
      // add the jms server object if needed
      if ( jmsServerName == null )
      {
        try
        {
          parent.getEnvironmentWindow().getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(
            serverName ).addJMSServer( jmsServer );
          parent.getEnvironmentWindow().getChangeEvents().add(
            "Add JMS server (name [ " + nameFieldValue + " ]) in the J2EE application server " + serverName );
        }
        catch ( Exception e )
        {
          KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "jmsserver.exists" ),
                                                                              getEnvironmentWindow().getEnvironmentName() );
          return;
        }
      }
      // update the window definition
      setTitle( Messages.getString( "jmsserver" ) + " " + jmsServer.getName() );
      setId( "jmsserverwindow_" + parent.getEnvironmentWindow().getEnvironmentName() + "_" + serverName + "_"
               + jmsServer.getName() );
      jmsServerName = jmsServer.getName();
      // change the updated flag
      parent.getEnvironmentWindow().setUpdated( true );
      // update the journal log tab pane
      parent.getEnvironmentWindow().updateJournalPane();
      // update the parent tab
      parent.update();
      // update the window
      update();
    }
  };

  // copy
  private ActionListener copy = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
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

  // paste
  private ActionListener paste = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if the copy is correct
      Object copy = KalumetConsoleApplication.getApplication().getCopyComponent();
      if ( copy == null || !( copy instanceof JMSServer ) )
      {
        return;
      }
      jmsServer = (JMSServer) copy;
      jmsServerName = null;
      // update the parent pane
      parent.update();
      // update the window
      update();
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
      // add a message in the log pane and the journal
      KalumetConsoleApplication.getApplication().getLogPane().addInfo(
        "JMS server " + jmsServerName + " status check in progress...",
        parent.getEnvironmentWindow().getEnvironmentName() );
      parent.getEnvironmentWindow().getChangeEvents().add( "JMS server " + jmsServerName + " status check requested." );
      // start the status thread
      final StatusThread statusThread = new StatusThread();
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
                                                                        parent.getEnvironmentWindow().getEnvironmentName() );
                                                                    }
                                                                    else
                                                                    {
                                                                      KalumetConsoleApplication.getApplication().getLogPane().addInfo(
                                                                        statusThread.message,
                                                                        parent.getEnvironmentWindow().getEnvironmentName() );
                                                                    }
                                                                    parent.getEnvironmentWindow().getChangeEvents().add(
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
      // display confirm window
      KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
        new ConfirmWindow( new ActionListener()
        {
          public void actionPerformed( ActionEvent event )
          {
            // add a message in the log pane and journal
            KalumetConsoleApplication.getApplication().getLogPane().addInfo(
              "JMS server " + jmsServerName + " update in progress...",
              parent.getEnvironmentWindow().getEnvironmentName() );
            parent.getEnvironmentWindow().getChangeEvents().add( "JMS server " + jmsServerName + " update requested." );
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
                                                                                      parent.getEnvironmentWindow().getEnvironmentName() );
                    parent.getEnvironmentWindow().getChangeEvents().add( updateThread.message );
                  }
                  else
                  {
                    KalumetConsoleApplication.getApplication().getLogPane().addConfirm(
                      "JMS server " + jmsServerName + " updated.", parent.getEnvironmentWindow().getEnvironmentName() );
                    parent.getEnvironmentWindow().getChangeEvents().add( "JMS server " + jmsServerName + " updated." );
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
   * Create a new <code>JmsServerWindow</code>.
   *
   * @param parent                the <code>JmsServersPane</code>.
   * @param applicationServerName the original J2EE application server name.
   * @param jmsServerName         the original JMS server name.
   */
  public JmsServerWindow( JmsServersPane parent, String applicationServerName, String jmsServerName )
  {
    super();

    // update the parent tab pane
    this.parent = parent;

    // update the application server name and jms server name
    this.serverName = applicationServerName;
    this.jmsServerName = jmsServerName;

    // update the jms server object from the parent environment
    this.jmsServer =
      parent.getEnvironmentWindow().getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(
        serverName ).getJMSServer( jmsServerName );
    if ( this.jmsServer == null )
    {
      this.jmsServer = new JMSServer();
    }

    if ( jmsServerName == null )
    {
      setTitle( Messages.getString( "jmsserver" ) );
    }
    else
    {
      setTitle( Messages.getString( "jmsserver" ) + " " + jmsServerName );
    }
    setId( "jmsserverwindow_" + parent.getEnvironmentWindow().getEnvironmentName() + "_" + serverName + "_"
             + jmsServerName );
    setStyleName( "default" );
    setWidth( new Extent( 600, Extent.PX ) );
    setHeight( new Extent( 400, Extent.PX ) );
    setModal( false );
    setDefaultCloseOperation( WindowPane.DISPOSE_ON_CLOSE );

    // create a split pane for the control button
    SplitPane splitPane = new SplitPane( SplitPane.ORIENTATION_VERTICAL_BOTTOM_TOP, new Extent( 32 ) );
    add( splitPane );

    // add the control pane
    Row controlRow = new Row();
    controlRow.setStyleName( "control" );
    splitPane.add( controlRow );
    // add the refresh button
    Button refreshButton = new Button( Messages.getString( "reload" ), Styles.DATABASE_REFRESH );
    refreshButton.setStyleName( "control" );
    refreshButton.addActionListener( refresh );
    controlRow.add( refreshButton );
    // add the copy button
    Button copyButton = new Button( Messages.getString( "copy" ), Styles.PAGE_COPY );
    copyButton.setStyleName( "control" );
    copyButton.addActionListener( copy );
    controlRow.add( copyButton );
    if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeResourcesPermission )
    {
      // add the paste button
      Button pasteButton = new Button( Messages.getString( "paste" ), Styles.PAGE_PASTE );
      pasteButton.setStyleName( "control" );
      pasteButton.addActionListener( paste );
      controlRow.add( pasteButton );
    }
    // add the status button
    Button statusButton = new Button( Messages.getString( "status" ), Styles.INFORMATION );
    statusButton.setStyleName( "status" );
    statusButton.addActionListener( status );
    controlRow.add( statusButton );
    if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeResourcesUpdatePermission )
    {
      // add the update button
      Button updateButton = new Button( Messages.getString( "update" ), Styles.COG );
      updateButton.setStyleName( "control" );
      updateButton.addActionListener( update );
      controlRow.add( updateButton );
    }
    if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeResourcesPermission )
    {
      // add the apply button
      Button applyButton = new Button( Messages.getString( "apply" ), Styles.ACCEPT );
      applyButton.setStyleName( "control" );
      applyButton.addActionListener( apply );
      controlRow.add( applyButton );
      // add the delete button
      Button deleteButton = new Button( Messages.getString( "delete" ), Styles.DELETE );
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

    // add the jms server general tab
    TabPaneLayoutData tabLayoutData = new TabPaneLayoutData();
    tabLayoutData.setTitle( Messages.getString( "general" ) );
    generalPane = new JmsServerGeneralPane( this );
    generalPane.setLayoutData( tabLayoutData );
    tabPane.add( generalPane );

    // add the jms server queues tab
    tabLayoutData = new TabPaneLayoutData();
    tabLayoutData.setTitle( Messages.getString( "jmsqueues" ) );
    jmsQueuesPane = new JmsQueuesPane( this );
    jmsQueuesPane.setLayoutData( tabLayoutData );
    tabPane.add( jmsQueuesPane );

    // add the jms server topics tab
    tabLayoutData = new TabPaneLayoutData();
    tabLayoutData.setTitle( Messages.getString( "jmstopics" ) );
    jmsTopicsPane = new JmsTopicsPane( this );
    jmsTopicsPane.setLayoutData( tabLayoutData );
    tabPane.add( jmsTopicsPane );

    // update the window
    update();
  }

  /**
   * Update the window.
   */
  public void update()
  {
    generalPane.update();
    jmsQueuesPane.update();
    jmsTopicsPane.update();
  }

  public JMSServer getJMSServer()
  {
    return this.jmsServer;
  }

  public String getJMSServerName()
  {
    return this.jmsServerName;
  }

  public String getServerName()
  {
    return this.serverName;
  }

  public EnvironmentWindow getEnvironmentWindow()
  {
    return parent.getEnvironmentWindow();
  }

}