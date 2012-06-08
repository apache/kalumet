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
import org.apache.kalumet.model.ContentManager;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.ws.client.ContentManagerClient;

import java.util.Iterator;

/**
 * J2EE application content managers pane.
 */
public class ApplicationContentManagersPane
  extends ContentPane
{

  private ApplicationWindow parent;

  private Grid grid;

  class UpdateThread
    extends Thread
  {

    public String contentManagerName;

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
        ContentManagerClient client = new ContentManagerClient( agent.getHostname(), agent.getPort() );
        client.update( parent.getParentPane().getEnvironmentWindow().getEnvironmentName(), parent.getServerName(),
                       parent.getApplicationName(), contentManagerName, false );
      }
      catch ( Exception e )
      {
        failure = true;
        message = "J2EE application " + parent.getApplicationName() + " content manager " + contentManagerName
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
      // looking for the content manager object
      ContentManager contentManager = parent.getApplication().getContentManager( event.getActionCommand() );
      if ( contentManager == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "contentmanager.notfound" ),
          parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // change the state and add a change event
      if ( contentManager.isActive() )
      {
        contentManager.setActive( false );
        parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
          "Disable J2EE application " + parent.getApplicationName() + " content manager " + contentManager.getName() );
      }
      else
      {
        contentManager.setActive( true );
        parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
          "Enable J2EE application " + parent.getApplicationName() + " content manager " + contentManager.getName() );
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
      // looking for the content manager object
      ContentManager contentManager = parent.getApplication().getContentManager( event.getActionCommand() );
      if ( contentManager == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "contentmanager.notfound" ),
          parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // change the blocker state and add a change event
      if ( contentManager.isBlocker() )
      {
        contentManager.setBlocker( false );
        parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
          "Set not blocker for J2EE application " + parent.getApplicationName() + " content manager "
            + contentManager.getName() );
      }
      else
      {
        contentManager.setBlocker( true );
        parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
          "Set blocker for J2EE application " + parent.getApplicationName() + " content manager "
            + contentManager.getName() );
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
      // looking for the content manager object
      final ContentManager contentManager = parent.getApplication().getContentManager( event.getActionCommand() );
      if ( contentManager == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "contentmanager.notfound" ),
          parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // display confirm window
      KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
        new ConfirmWindow( new ActionListener()
        {
          public void actionPerformed( ActionEvent event )
          {
            // delete the content manager object
            parent.getApplication().getContentManagers().remove( contentManager );
            // add a change event
            parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
              "Delete J2EE application " + parent.getApplicationName() + " content manager "
                + contentManager.getName() );
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
      // get the content manager name
      final String contentManagerName = event.getActionCommand();
      // display confirm window
      KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
        new ConfirmWindow( new ActionListener()
        {
          public void actionPerformed( ActionEvent event )
          {
            // add a message into the log pane and the journal
            KalumetConsoleApplication.getApplication().getLogPane().addInfo(
              "J2EE application " + parent.getApplicationName() + " content manager " + contentManagerName
                + " update in progress...", parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
            parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
              "J2EE application " + parent.getApplicationName() + " content manager " + contentManagerName
                + " update requested." );
            // start the update thread
            final UpdateThread updateThread = new UpdateThread();
            updateThread.contentManagerName = contentManagerName;
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
                      "J2EE application " + parent.getApplicationName() + " content manager " + contentManagerName
                        + " updated.", parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
                    parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
                      "J2EE application " + parent.getApplicationName() + " content manager " + contentManagerName
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

  // edit
  private ActionListener edit = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      if ( KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().getComponent(
        "contentmanagerwindow_" + parent.getParentPane().getEnvironmentWindow().getEnvironmentName() + "_"
          + parent.getServerName() + "_" + parent.getApplicationName() + "_" + event.getActionCommand() ) == null )
      {
        KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
          new ApplicationContentManagerWindow( ApplicationContentManagersPane.this, event.getActionCommand() ) );
      }
    }
  };

  // create
  private ActionListener create = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
        new ApplicationContentManagerWindow( ApplicationContentManagersPane.this, null ) );
    }
  };

  // copy
  private ActionListener copy = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // looking for the content manager object
      ContentManager contentManager = parent.getApplication().getContentManager( event.getActionCommand() );
      if ( contentManager == null )
      {
        return;
      }
      try
      {
        KalumetConsoleApplication.getApplication().setCopyComponent( contentManager.clone() );
      }
      catch ( Exception e )
      {
        return;
      }
    }
  };

  /**
   * Create a new <code>ApplicationContentManagersPane</code>.
   *
   * @param parent the parent <code>ApplicationWindow</code>.
   */
  public ApplicationContentManagersPane( ApplicationWindow parent )
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
      Button createButton = new Button( Messages.getString( "contentmanager.add" ), Styles.ADD );
      createButton.addActionListener( create );
      content.add( createButton );
    }

    // add the content managers grid
    grid = new Grid( 4 );
    grid.setStyleName( "border.grid" );
    grid.setColumnWidth( 0, new Extent( 50, Extent.PX ) );
    content.add( grid );

    // update the pane
    update();
  }

  /**
   * Update the pane
   */
  public void update()
  {
    // remove all content mangers grid children
    grid.removeAll();
    // add content managers grid header
    // action header
    Label actionHeader = new Label( " " );
    actionHeader.setStyleName( "grid.header" );
    grid.add( actionHeader );
    // name header
    Label nameHeader = new Label( Messages.getString( "name" ) );
    nameHeader.setStyleName( "grid.header" );
    grid.add( nameHeader );
    // classname header
    Label classnameHeader = new Label( Messages.getString( "classname" ) );
    classnameHeader.setStyleName( "grid.header" );
    grid.add( classnameHeader );
    // agent header
    Label agentHeader = new Label( Messages.getString( "agent" ) );
    agentHeader.setStyleName( "grid.header" );
    grid.add( agentHeader );
    // add content manager
    for ( Iterator contentManagerIterator = parent.getApplication().getContentManagers().iterator();
          contentManagerIterator.hasNext(); )
    {
      ContentManager contentManager = (ContentManager) contentManagerIterator.next();
      // row
      Row row = new Row();
      row.setCellSpacing( new Extent( 2 ) );
      row.setInsets( new Insets( 2 ) );
      grid.add( row );
      // copy
      Button copyButton = new Button( Styles.PAGE_COPY );
      copyButton.setToolTipText( Messages.getString( "copy" ) );
      copyButton.setActionCommand( contentManager.getName() );
      copyButton.addActionListener( copy );
      row.add( copyButton );
      // active
      Button activeButton;
      if ( contentManager.isActive() )
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
        activeButton.setActionCommand( contentManager.getName() );
        activeButton.addActionListener( toggleActive );
      }
      row.add( activeButton );
      // blocker
      Button blockerButton;
      if ( contentManager.isBlocker() )
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
        blockerButton.setActionCommand( contentManager.getName() );
        blockerButton.addActionListener( toggleBlocker );
      }
      row.add( blockerButton );
      // up
      // TODO
      // down
      // TODO
      // update
      if ( parent.getParentPane().getEnvironmentWindow().adminPermission
        || parent.getParentPane().getEnvironmentWindow().jeeApplicationsUpdatePermission )
      {
        Button updateButton = new Button( Styles.COG );
        updateButton.setToolTipText( Messages.getString( "update" ) );
        updateButton.setActionCommand( contentManager.getName() );
        updateButton.addActionListener( update );
        row.add( updateButton );
      }
      // delete
      if ( parent.getParentPane().getEnvironmentWindow().adminPermission
        || parent.getParentPane().getEnvironmentWindow().jeeApplicationsPermission )
      {
        Button deleteButton = new Button( Styles.DELETE );
        deleteButton.setToolTipText( Messages.getString( "delete" ) );
        deleteButton.setActionCommand( contentManager.getName() );
        deleteButton.addActionListener( delete );
        row.add( deleteButton );
      }
      // name
      Button name = new Button( contentManager.getName() );
      name.setStyleName( "default" );
      name.setActionCommand( contentManager.getName() );
      name.addActionListener( edit );
      grid.add( name );
      // classname
      Label classname = new Label( contentManager.getClassname() );
      classname.setStyleName( "default" );
      grid.add( classname );
      // agent
      Label agent = new Label( contentManager.getAgent() );
      agent.setStyleName( "default" );
      grid.add( agent );
    }
  }

  /**
   * Get the parent <code>ApplicationWindow</code>.
   *
   * @return the parent <code>ApplicationWindow</code>.
   */
  public ApplicationWindow getParentPane()
  {
    return parent;
  }

}
