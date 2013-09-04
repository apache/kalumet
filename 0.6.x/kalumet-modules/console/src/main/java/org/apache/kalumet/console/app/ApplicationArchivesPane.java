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
import org.apache.kalumet.model.Archive;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.ws.client.ArchiveClient;

import java.util.Iterator;

/**
 * JEE application archives pane.
 */
public class ApplicationArchivesPane
  extends ContentPane
{

  private ApplicationWindow parent;

  private Grid grid;

  // status thread
  class StatusThread
    extends Thread
  {

    public String archiveName;

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
        ArchiveClient client = new ArchiveClient( agent.getHostname(), agent.getPort() );
        boolean uptodate =
          client.check( parent.getParentPane().getEnvironmentWindow().getEnvironmentName(), parent.getServerName(),
                        parent.getApplicationName(), archiveName );
        if ( uptodate )
        {
          message = "JEE application " + parent.getApplicationName() + " archive " + archiveName + " is up to date.";
        }
        else
        {
          failure = true;
          message =
            "JEE application " + parent.getApplicationName() + " archive " + archiveName + " is not up to date.";
        }
      }
      catch ( Exception e )
      {
        failure = true;
        message =
          "JEE application " + parent.getApplicationName() + " archive " + archiveName + " status check failed: "
            + e.getMessage();
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

    public String archiveName;

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
        ArchiveClient client = new ArchiveClient( agent.getHostname(), agent.getPort() );
        client.update( parent.getParentPane().getEnvironmentWindow().getEnvironmentName(), parent.getServerName(),
                       parent.getApplicationName(), archiveName, false );
      }
      catch ( Exception e )
      {
        failure = true;
        message = "JEE application " + parent.getApplicationName() + " archive " + archiveName + " update failed: "
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
        && !parent.getParentPane().getEnvironmentWindow().jeeApplicationsChangePermission)
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // looking for the archive object
      Archive archive = parent.getApplication().getArchive( event.getActionCommand() );
      if ( archive == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "archive.notfound" ),
                                                                            parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // change the state and add change event
      if ( archive.isActive() )
      {
        archive.setActive( false );
        parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
          "Disable JEE application archive " + archive.getName() );
      }
      else
      {
        archive.setActive( true );
        parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
          "Enable JEE application archive " + archive.getName() );
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
        && !parent.getParentPane().getEnvironmentWindow().jeeApplicationsChangePermission)
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // looking for the archive object
      Archive archive = parent.getApplication().getArchive( event.getActionCommand() );
      if ( archive == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "archive.notfound" ),
                                                                            parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // change the blocker state and add change event
      if ( archive.isBlocker() )
      {
        archive.setBlocker( false );
        parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
          "Set not blocker for JEE application archive " + archive.getName() );
      }
      else
      {
        archive.setBlocker( true );
        parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
          "Set blocker for JEE application archive " + archive.getName() );
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
        && !parent.getParentPane().getEnvironmentWindow().jeeApplicationsChangePermission)
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      final String archiveName = event.getActionCommand();
      // display confirm window
      KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
        new ConfirmWindow( new ActionListener()
        {
          public void actionPerformed( ActionEvent event )
          {
            // looking for the archive object
            Archive archive = parent.getApplication().getArchive( archiveName );
            if ( archive == null )
            {
              KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                Messages.getString( "archive.notfound" ),
                parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
              return;
            }
            // delete the archive object
            parent.getApplication().getArchives().remove( archive );
            // add a change event
            parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
              "Delete JEE application archive " + archive.getName() );
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

  // status
  private ActionListener status = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if some change has not yet been saved
      if ( parent.getParentPane().getEnvironmentWindow().isUpdated() )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "environment.notsaved" ),
          parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      String archiveName = event.getActionCommand();
      // add a message into the log pane and in the journal
      KalumetConsoleApplication.getApplication().getLogPane().addInfo(
        "JEE application " + parent.getApplicationName() + " archive " + archiveName + " status check in progress...",
        parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
      parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
        "JEE application " + parent.getApplicationName() + " archive " + archiveName + " check requested." );
      // start the status thread
      final StatusThread statusThread = new StatusThread();
      statusThread.archiveName = archiveName;
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
                                                                    parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
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
      // check if there is no pending change
      if ( parent.getParentPane().getEnvironmentWindow().isUpdated() )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "environment.notsaved" ),
          parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // get the archive name
      final String archiveName = event.getActionCommand();
      // display confirm window
      KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
        new ConfirmWindow( new ActionListener()
        {
          public void actionPerformed( ActionEvent event )
          {
            // add a message in the log pane and in the journal
            KalumetConsoleApplication.getApplication().getLogPane().addInfo(
              "JEE application " + parent.getApplicationName() + " archive " + archiveName + " update in progress...",
              parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
            parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
              "JEE application " + parent.getApplicationName() + " archive " + archiveName + " update requested." );
            // start the update thread
            final UpdateThread updateThread = new UpdateThread();
            updateThread.archiveName = archiveName;
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
                      "JEE application " + parent.getApplicationName() + " archive " + archiveName + " updated.",
                      parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
                    parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
                      "JEE application " + parent.getApplicationName() + " archive " + archiveName + " updated" );
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
        "archivewindow_" + parent.getParentPane().getEnvironmentWindow().getEnvironmentName() + "_"
          + parent.getServerName() + "_" + parent.getApplicationName() + "_" + event.getActionCommand() ) == null )
      {
        KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
          new ApplicationArchiveWindow( ApplicationArchivesPane.this, event.getActionCommand() ) );
      }
    }
  };

  // create
  private ActionListener create = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
        new ApplicationArchiveWindow( ApplicationArchivesPane.this, null ) );
    }
  };

  // copy
  private ActionListener copy = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // looking for the archive object
      Archive archive = parent.getApplication().getArchive( event.getActionCommand() );
      if ( archive == null )
      {
        return;
      }
      try
      {
        KalumetConsoleApplication.getApplication().setCopyComponent( archive.clone() );
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
        && !parent.getParentPane().getEnvironmentWindow().jeeApplicationsChangePermission)
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // looking for the archive object
      Archive archive = parent.getApplication().getArchive( event.getActionCommand() );
      if ( archive == null )
      {
        return;
      }
      // get the archive index
      int index = parent.getApplication().getArchives().indexOf( archive );
      // if the archive index is the first or if the archive object is not found
      // or if the archives list doesn't contain at least two elements, do nothing
      if ( index == 0 || index == -1 || parent.getApplication().getArchives().size() < 2 )
      {
        return;
      }
      // get the previous archive
      Archive previous = (Archive) parent.getApplication().getArchives().get( index - 1 );
      // switch the archives
      parent.getApplication().getArchives().set( index, previous );
      parent.getApplication().getArchives().set( index - 1, archive );
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
        && !parent.getParentPane().getEnvironmentWindow().jeeApplicationsChangePermission)
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // get the archive object
      Archive archive = parent.getApplication().getArchive( event.getActionCommand() );
      if ( archive == null )
      {
        return;
      }
      // get the archive index
      int index = parent.getApplication().getArchives().indexOf( archive );
      // if the archive index is the last one, or the archive not found
      // or the archives list doesn't contain at least two elements,
      // do nothing
      if ( index == -1 || index == parent.getApplication().getArchives().size() - 1
        || parent.getApplication().getArchives().size() < 2 )
      {
        return;
      }
      // get the next archive
      Archive next = (Archive) parent.getApplication().getArchives().get( index + 1 );
      // switch the archives
      parent.getApplication().getArchives().set( index + 1, archive );
      parent.getApplication().getArchives().set( index, next );
      // update the pane
      update();
    }
  };

  /**
   * Create a new <code>ApplicationArchivesPane</code>.
   *
   * @param parent the parent <code>ApplicationWindow</code>.
   */
  public ApplicationArchivesPane( ApplicationWindow parent )
  {
    super();
    setStyleName( "tab.content" );

    // update parent
    this.parent = parent;

    // add the column content layout
    Column content = new Column();
    content.setInsets( new Insets( 2 ) );
    add( content );

    // add the create button
    if ( parent.getParentPane().getEnvironmentWindow().adminPermission
      || parent.getParentPane().getEnvironmentWindow().jeeApplicationsChangePermission)
    {
      Button createButton = new Button( Messages.getString( "archive.add" ), Styles.ADD );
      createButton.addActionListener( create );
      content.add( createButton );
    }

    // add the archives grid
    grid = new Grid( 5 );
    grid.setStyleName( "border.grid" );
    grid.setColumnWidth( 0, new Extent( 50, Extent.PX ) );
    grid.setColumnWidth( 1, new Extent( 10, Extent.PERCENT ) );
    grid.setColumnWidth( 2, new Extent( 40, Extent.PERCENT ) );
    grid.setColumnWidth( 3, new Extent( 40, Extent.PERCENT ) );
    grid.setColumnWidth( 4, new Extent( 10, Extent.PERCENT ) );
    content.add( grid );

    // update the pane
    update();
  }

  /**
   * Update the pane.
   */
  public void update()
  {
    // remove all archives grid children
    grid.removeAll();
    // add archives grid header
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
    // add archive
    for ( Iterator archiveIterator = parent.getApplication().getArchives().iterator(); archiveIterator.hasNext(); )
    {
      Archive archive = (Archive) archiveIterator.next();
      // row
      Row row = new Row();
      row.setCellSpacing( new Extent( 2 ) );
      row.setInsets( new Insets( 2 ) );
      grid.add( row );
      // copy
      Button copyButton = new Button( Styles.PAGE_COPY );
      copyButton.setActionCommand( archive.getName() );
      copyButton.addActionListener( copy );
      copyButton.setToolTipText( Messages.getString( "copy" ) );
      row.add( copyButton );
      // active
      Button activeButton;
      if ( archive.isActive() )
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
        || parent.getParentPane().getEnvironmentWindow().jeeApplicationsChangePermission)
      {
        activeButton.setActionCommand( archive.getName() );
        activeButton.addActionListener( toggleActive );
      }
      row.add( activeButton );
      // blocker
      Button blockerButton;
      if ( archive.isBlocker() )
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
        || parent.getParentPane().getEnvironmentWindow().jeeApplicationsChangePermission)
      {
        blockerButton.setActionCommand( archive.getName() );
        blockerButton.addActionListener( toggleBlocker );
      }
      row.add( blockerButton );
      if ( parent.getParentPane().getEnvironmentWindow().adminPermission
        || parent.getParentPane().getEnvironmentWindow().jeeApplicationsChangePermission)
      {
        // up
        Button upButton = new Button( Styles.ARROW_UP );
        upButton.setToolTipText( Messages.getString( "up" ) );
        upButton.setActionCommand( archive.getName() );
        upButton.addActionListener( up );
        row.add( upButton );
        // down
        Button downButton = new Button( Styles.ARROW_DOWN );
        downButton.setToolTipText( Messages.getString( "down" ) );
        downButton.setActionCommand( archive.getName() );
        downButton.addActionListener( down );
        row.add( downButton );
      }
      // status
      Button statusButton = new Button( Styles.INFORMATION );
      statusButton.setActionCommand( archive.getName() );
      statusButton.addActionListener( status );
      statusButton.setToolTipText( Messages.getString( "status" ) );
      row.add( statusButton );
      if ( parent.getParentPane().getEnvironmentWindow().adminPermission
        || parent.getParentPane().getEnvironmentWindow().jeeApplicationsUpdatePermission )
      {
        // update
        Button updateButton = new Button( Styles.COG );
        updateButton.setActionCommand( archive.getName() );
        updateButton.addActionListener( update );
        updateButton.setToolTipText( Messages.getString( "update" ) );
        row.add( updateButton );
      }
      if ( parent.getParentPane().getEnvironmentWindow().adminPermission
        || parent.getParentPane().getEnvironmentWindow().jeeApplicationsChangePermission)
      {
        // delete
        Button deleteButton = new Button( Styles.DELETE );
        deleteButton.setActionCommand( archive.getName() );
        deleteButton.addActionListener( delete );
        deleteButton.setToolTipText( Messages.getString( "delete" ) );
        row.add( deleteButton );
      }
      // name
      Button name = new Button( archive.getName() );
      name.setStyleName( "default" );
      name.setActionCommand( archive.getName() );
      name.addActionListener( edit );
      grid.add( name );
      // uri
      Label uri = new Label( archive.getUri() );
      uri.setStyleName( "default" );
      grid.add( uri );
      // path
      Label path = new Label( archive.getPath() );
      path.setStyleName( "default" );
      grid.add( path );
      // agent
      Label agent = new Label( archive.getAgent() );
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
