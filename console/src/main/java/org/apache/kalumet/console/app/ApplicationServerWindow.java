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
import org.apache.kalumet.model.J2EEApplicationServer;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.ws.client.J2EEApplicationServerClient;

/**
 * J2EE application server window.
 */
public class ApplicationServerWindow
  extends WindowPane
{

  private String serverName;

  private J2EEApplicationServer server = null;

  private ApplicationServersPane parent;

  private ApplicationServerGeneralPane generalPane;

  private ApplicationServerCachesPane cachesPane;

  public final static String JBOSS4_CONTROLLER_CLASSNAME = "org.apache.kalumet.controller.jboss.JBoss4Controller";

  public final static String JBOSS6_CONTROLLER_CLASSNAME = "org.apache.kalumet.controller.jboss.JBoss6Controller";

  public final static String WEBLOGIC_CONTROLLER_CLASSNAME =
    "org.apache.kalumet.controller.weblogic.WeblogicController";

  public final static String WEBSPHERE_CONTROLLER_CLASSNAME =
    "org.apache.kalumet.controller.websphere.WebsphereController";

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
        J2EEApplicationServerClient client = new J2EEApplicationServerClient( agent.getHostname(), agent.getPort() );
        message = client.status( parent.getEnvironmentWindow().getEnvironmentName(), serverName );
      }
      catch ( Exception e )
      {
        failure = true;
        message = "J2EE application server " + serverName + " status check failed: " + e.getMessage();
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
        J2EEApplicationServerClient client = new J2EEApplicationServerClient( agent.getHostname(), agent.getPort() );
        client.update( parent.getEnvironmentWindow().getEnvironmentName(), serverName, false );
      }
      catch ( Exception e )
      {
        failure = true;
        message = "J2EE application server " + serverName + " update failed: " + e.getMessage();
      }
      finally
      {
        ended = true;
      }
    }
  }

  // stop thread
  class StopThread
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
        J2EEApplicationServerClient client = new J2EEApplicationServerClient( agent.getHostname(), agent.getPort() );
        client.stop( parent.getEnvironmentWindow().getEnvironmentName(), serverName );
      }
      catch ( Exception e )
      {
        failure = true;
        message = "J2EE application server " + serverName + " stop failed: " + e.getMessage();
      }
      finally
      {
        ended = true;
      }
    }
  }

  // start thread
  class StartThread
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
        J2EEApplicationServerClient client = new J2EEApplicationServerClient( agent.getHostname(), agent.getPort() );
        client.start( parent.getEnvironmentWindow().getEnvironmentName(), serverName );
      }
      catch ( Exception e )
      {
        failure = true;
        message = "J2EE application server " + serverName + " start failed: " + e.getMessage();
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
      ApplicationServerWindow.this.userClose();
    }
  };

  // refresh
  private ActionListener refresh = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // looking for original application server object
      server = parent.getEnvironmentWindow().getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(
        serverName );
      if ( server == null )
      {
        server = new J2EEApplicationServer();
      }
      // update the window
      update();
    }
  };

  // apply
  private ActionListener apply = new ActionListener()
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
      if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeServersPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // get fields value
      String nameFieldValue = generalPane.getNameField().getText();
      int activeFieldIndex = generalPane.getActiveField().getSelectedIndex();
      int blockerFieldIndex = generalPane.getBlockerField().getSelectedIndex();
      int typeFieldIndex = generalPane.getTypeField().getSelectedIndex();
      String jmxFieldValue = generalPane.getJmxField().getText();
      String adminUserFieldValue = generalPane.getAdminUserField().getText();
      String adminPasswordFieldValue = generalPane.getAdminPasswordField().getText();
      String adminConfirmPasswordFieldValue = generalPane.getAdminConfirmPasswordField().getText();
      int updateRequireRestartFieldIndex = generalPane.getUpdateRequireRestartField().getSelectedIndex();
      int updateRequireCachesCleanFieldIndex = generalPane.getUpdateRequireCachesCleanField().getSelectedIndex();
      int stopUsingJmxFieldIndex = generalPane.getStopUsingJmxField().getSelectedIndex();
      String startupCommandAreaValue = generalPane.getStartupCommandArea().getText();
      String shutdownCommandAreaValue = generalPane.getShutdownCommandArea().getText();
      String agentFieldValue = (String) generalPane.getAgentField().getSelectedItem();
      // check fields
      // name and JMX are mandatory
      if ( nameFieldValue == null || nameFieldValue.trim().length() < 1 || jmxFieldValue == null
        || jmxFieldValue.trim().length() < 1 )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "applicationserver.mandatory" ), getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check password matching
      if ( !adminPasswordFieldValue.equals( adminConfirmPasswordFieldValue ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "applicationserver.password.notmatch" ), getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // if the user change the J2EE application server name, check if the
      // JEE application server name doesn't already exist
      if ( serverName == null || ( serverName != null && !serverName.equals( nameFieldValue ) ) )
      {
        if ( parent.getEnvironmentWindow().getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(
          nameFieldValue ) != null )
        {
          KalumetConsoleApplication.getApplication().getLogPane().addWarning(
            Messages.getString( "applicationserver.exists" ), getEnvironmentWindow().getEnvironmentName() );
          return;
        }
      }
      // add a change event
      if ( serverName != null )
      {
        parent.getEnvironmentWindow().getChangeEvents().add( "Change J2EE application server " + server.getName() );
      }
      // update the application server object
      server.setName( nameFieldValue );
      if ( activeFieldIndex == 0 )
      {
        server.setActive( true );
      }
      else
      {
        server.setActive( false );
      }
      if ( blockerFieldIndex == 0 )
      {
        server.setBlocker( true );
      }
      else
      {
        server.setBlocker( false );
      }
      if ( typeFieldIndex == 0 )
      {
        server.setClassname( ApplicationServerWindow.JBOSS4_CONTROLLER_CLASSNAME );
      }
      if ( typeFieldIndex == 1 )
      {
        server.setClassname( ApplicationServerWindow.JBOSS6_CONTROLLER_CLASSNAME );
      }
      if ( typeFieldIndex == 2 )
      {
        server.setClassname( ApplicationServerWindow.WEBLOGIC_CONTROLLER_CLASSNAME );
      }
      if ( typeFieldIndex == 3 )
      {
        server.setClassname( ApplicationServerWindow.WEBSPHERE_CONTROLLER_CLASSNAME );
      }
      server.setJmxurl( jmxFieldValue );
      server.setAdminuser( adminUserFieldValue );
      server.setAdminpassword( adminPasswordFieldValue );
      if ( updateRequireRestartFieldIndex == 0 )
      {
        server.setUpdateRequireRestart( true );
      }
      else
      {
        server.setUpdateRequireRestart( false );
      }
      if ( updateRequireCachesCleanFieldIndex == 0 )
      {
        server.setUpdateRequireCacheCleaning( true );
      }
      else
      {
        server.setUpdateRequireCacheCleaning( false );
      }
      if ( stopUsingJmxFieldIndex == 0 )
      {
        server.setUsejmxstop( true );
      }
      else
      {
        server.setUsejmxstop( false );
      }
      server.setStartupcommand( startupCommandAreaValue );
      server.setShutdowncommand( shutdownCommandAreaValue );
      server.setAgent( agentFieldValue );
      // add the application server object if needed
      if ( serverName == null )
      {
        try
        {
          parent.getEnvironmentWindow().getEnvironment().getJ2EEApplicationServers().addJ2EEApplicationServer( server );
          parent.getEnvironmentWindow().getChangeEvents().add( "Add J2EE application server " + server.getName() );
        }
        catch ( Exception e )
        {
          KalumetConsoleApplication.getApplication().getLogPane().addWarning(
            Messages.getString( "applicationserver.exists" ), getEnvironmentWindow().getEnvironmentName() );
          return;
        }
      }
      // update the window definition
      setTitle( Messages.getString( "applicationserver" ) + " " + server.getName() );
      setId( "applicationserverwindow_" + parent.getEnvironmentWindow().getEnvironmentName() + "_" + server.getName() );
      serverName = server.getName();
      // change the updated flag
      parent.getEnvironmentWindow().setUpdated( true );
      // update the whole environment window
      parent.getEnvironmentWindow().update();
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
      if ( !getEnvironmentWindow().adminPermission || !getEnvironmentWindow().jeeServersPermission )
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
            // delete the application server object
            parent.getEnvironmentWindow().getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServers().remove(
              server );
            // add a change event
            parent.getEnvironmentWindow().getChangeEvents().add( "Delete J2EE application server " + server.getName() );
            // update the window
            update();
            // update the whole parent window
            parent.getEnvironmentWindow().update();
            // close the window
            ApplicationServerWindow.this.userClose();
          }
        } ) );
    }
  };

  // copy
  private ActionListener copy = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      try
      {
        // put the application server clone in the copy component
        KalumetConsoleApplication.getApplication().setCopyComponent( server.clone() );
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
      // check if the copy component is correct
      Object copy = KalumetConsoleApplication.getApplication().getCopyComponent();
      if ( copy == null || !( copy instanceof J2EEApplicationServer ) )
      {
        return;
      }
      // update the application server object
      server = (J2EEApplicationServer) copy;
      serverName = null;
      // update the whole window
      parent.getEnvironmentWindow().update();
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
      if ( parent.getEnvironmentWindow().isUpdated() )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "environment.notsaved" ), parent.getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // add a message into the log pane and the journal
      KalumetConsoleApplication.getApplication().getLogPane().addInfo(
        "J2EE application server " + serverName + " status check in progress...",
        parent.getEnvironmentWindow().getEnvironmentName() );
      parent.getEnvironmentWindow().getChangeEvents().add(
        "J2EE application server " + serverName + " status check requested." );
      // start status thread
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
                                                                      parent.getEnvironmentWindow().getChangeEvents().add(
                                                                        statusThread.message );
                                                                    }
                                                                    else
                                                                    {
                                                                      KalumetConsoleApplication.getApplication().getLogPane().addInfo(
                                                                        "J2EE application server " + serverName
                                                                          + " status: " + statusThread.message,
                                                                        parent.getEnvironmentWindow().getEnvironmentName() );
                                                                      parent.getEnvironmentWindow().getChangeEvents().add(
                                                                        "J2EE application server " + serverName
                                                                          + " status: " + statusThread.message );
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
      if ( !getEnvironmentWindow().getEnvironment().getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeServersUpdatePermission )
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
            // add a message into the log pane and the journal
            KalumetConsoleApplication.getApplication().getLogPane().addInfo(
              "J2EE application server " + serverName + " update in progress...",
              parent.getEnvironmentWindow().getEnvironmentName() );
            parent.getEnvironmentWindow().getChangeEvents().add(
              "J2EE application server " + serverName + " update requested." );
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
                      "J2EE application server " + serverName + " updated.",
                      parent.getEnvironmentWindow().getEnvironmentName() );
                    parent.getEnvironmentWindow().getChangeEvents().add(
                      "J2EE application server " + serverName + " updated." );
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

  // stop
  private ActionListener stop = new ActionListener()
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
      if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeServersControlPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if some change has been saved
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
            // add a message into the log pane and the journal
            KalumetConsoleApplication.getApplication().getLogPane().addInfo(
              "J2EE application server " + serverName + " stop in progress...",
              parent.getEnvironmentWindow().getEnvironmentName() );
            parent.getEnvironmentWindow().getChangeEvents().add(
              "J2EE application server " + serverName + " stop requested." );
            // start the stop thread
            final StopThread stopThread = new StopThread();
            stopThread.start();
            // sync with the client
            KalumetConsoleApplication.getApplication().enqueueTask(
              KalumetConsoleApplication.getApplication().getTaskQueue(), new Runnable()
            {
              public void run()
              {
                if ( stopThread.ended )
                {
                  if ( stopThread.failure )
                  {
                    KalumetConsoleApplication.getApplication().getLogPane().addError( stopThread.message,
                                                                                      parent.getEnvironmentWindow().getEnvironmentName() );
                    parent.getEnvironmentWindow().getChangeEvents().add( stopThread.message );
                  }
                  else
                  {
                    KalumetConsoleApplication.getApplication().getLogPane().addConfirm(
                      "J2EE application server " + serverName + " stopped.",
                      parent.getEnvironmentWindow().getEnvironmentName() );
                    parent.getEnvironmentWindow().getChangeEvents().add(
                      "J2EE application server " + serverName + " stopped." );
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

  // start
  private ActionListener start = new ActionListener()
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
      if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeServersControlPermission )
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
            // add a message into the log pane and the journal
            KalumetConsoleApplication.getApplication().getLogPane().addInfo(
              "J2EE application server " + serverName + " start in progress...",
              parent.getEnvironmentWindow().getEnvironmentName() );
            parent.getEnvironmentWindow().getChangeEvents().add(
              "J2EE application server " + serverName + " start requested." );
            // start the start thread
            final StartThread startThread = new StartThread();
            startThread.start();
            // sync with the client
            KalumetConsoleApplication.getApplication().enqueueTask(
              KalumetConsoleApplication.getApplication().getTaskQueue(), new Runnable()
            {
              public void run()
              {
                if ( startThread.ended )
                {
                  if ( startThread.failure )
                  {
                    KalumetConsoleApplication.getApplication().getLogPane().addError( startThread.message,
                                                                                      parent.getEnvironmentWindow().getEnvironmentName() );
                    parent.getEnvironmentWindow().getChangeEvents().add( startThread.message );
                  }
                  else
                  {
                    KalumetConsoleApplication.getApplication().getLogPane().addConfirm(
                      "J2EE application server " + serverName + " started.",
                      parent.getEnvironmentWindow().getEnvironmentName() );
                    parent.getEnvironmentWindow().getChangeEvents().add(
                      "J2EE application server " + serverName + " started." );
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
   * Create a new <code>ApplicationServerWindow</code>.
   *
   * @param parent                the <code>ApplicationServersPane</code>.
   * @param ApplicationServerName the original J2EE application server name.
   */
  public ApplicationServerWindow( ApplicationServersPane parent, String ApplicationServerName )
  {
    super();

    // update the parent tab pane
    this.parent = parent;

    // update the application server name
    this.serverName = ApplicationServerName;

    // update the application server object from the parent environment
    this.server =
      parent.getEnvironmentWindow().getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer( serverName );
    if ( this.server == null )
    {
      this.server = new J2EEApplicationServer();
    }

    if ( serverName == null )
    {
      setTitle( Messages.getString( "applicationserver" ) );
    }
    else
    {
      setTitle( Messages.getString( "applicationserver" ) + " " + serverName );
    }
    setId( "applicationserverwindow_" + parent.getEnvironmentWindow().getEnvironmentName() + "_" + serverName );
    setStyleName( "default" );
    setWidth( new Extent( 800, Extent.PX ) );
    setHeight( new Extent( 600, Extent.PX ) );
    setIcon( Styles.APPLICATION );
    setModal( false );
    setDefaultCloseOperation( WindowPane.DISPOSE_ON_CLOSE );

    // create a split pane for the control buttons
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
    if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeServersPermission )
    {
      // add the paste button
      Button pasteButton = new Button( Messages.getString( "paste" ), Styles.PAGE_PASTE );
      pasteButton.setStyleName( "control" );
      pasteButton.addActionListener( paste );
      controlRow.add( pasteButton );
      // add the apply button
      Button applyButton = new Button( Messages.getString( "apply" ), Styles.ACCEPT );
      applyButton.setStyleName( "control" );
      applyButton.addActionListener( apply );
      controlRow.add( applyButton );
    }
    // add the status button
    Button statusButton = new Button( Messages.getString( "status" ), Styles.INFORMATION );
    statusButton.setStyleName( "control" );
    statusButton.addActionListener( status );
    controlRow.add( statusButton );
    if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeServersControlPermission )
    {
      // add the stop button
      Button stopButton = new Button( Messages.getString( "stop" ), Styles.FLAG_RED );
      stopButton.setStyleName( "control" );
      stopButton.addActionListener( stop );
      controlRow.add( stopButton );
      // add the start button
      Button startButton = new Button( Messages.getString( "start" ), Styles.FLAG_GREEN );
      startButton.setStyleName( "control" );
      startButton.addActionListener( start );
      controlRow.add( startButton );
    }
    if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeServersUpdatePermission )
    {
      // add the update button
      Button updateButton = new Button( Messages.getString( "update" ), Styles.COG );
      updateButton.setStyleName( "control" );
      updateButton.addActionListener( update );
      controlRow.add( updateButton );
    }
    if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeServersPermission )
    {
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

    // add tab pane
    TabPane tabPane = new TabPane();
    tabPane.setStyleName( "default" );
    splitPane.add( tabPane );

    // add the jee application server general tab
    TabPaneLayoutData tabLayoutData = new TabPaneLayoutData();
    tabLayoutData.setTitle( Messages.getString( "general" ) );
    generalPane = new ApplicationServerGeneralPane( this );
    generalPane.setLayoutData( tabLayoutData );
    tabPane.add( generalPane );

    // add the jee application server caches tab
    tabLayoutData = new TabPaneLayoutData();
    tabLayoutData.setTitle( Messages.getString( "caches" ) );
    cachesPane = new ApplicationServerCachesPane( this );
    cachesPane.setLayoutData( tabLayoutData );
    tabPane.add( cachesPane );

    // update the pane
    update();
  }

  /**
   * Update the pane.
   */
  public void update()
  {
    generalPane.update();
    cachesPane.update();
  }

  public J2EEApplicationServer getApplicationServer()
  {
    return this.server;
  }

  public String getApplicationServerName()
  {
    return this.serverName;
  }

  public EnvironmentWindow getEnvironmentWindow()
  {
    return parent.getEnvironmentWindow();
  }

}
