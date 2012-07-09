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
import nextapp.echo2.app.ContentPane;
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.Grid;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.SelectField;
import nextapp.echo2.app.SplitPane;
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.WindowPane;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.app.list.DefaultListModel;
import nextapp.echo2.extras.app.TabPane;
import nextapp.echo2.extras.app.layout.TabPaneLayoutData;
import org.apache.kalumet.console.configuration.ConfigurationManager;
import org.apache.kalumet.model.Agent;
import org.apache.kalumet.model.Archive;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.ws.client.ArchiveClient;

import java.util.Iterator;

/**
 * JEE application archive <code>WindowPane</code>.
 */
public class ApplicationArchiveWindow
  extends WindowPane
{

  private static String[] CLASSLOADER_ORDER =
    new String[]{ Messages.getString( "parentlast" ), Messages.getString( "parentfirst" ) };

  private static String[] CLASSLOADER_POLICY =
    new String[]{ Messages.getString( "single" ), Messages.getString( "multiple" ) };

  private String archiveName;

  private Archive archive;

  private ApplicationArchivesPane parent;

  private TextField nameField;

  private SelectField activeField;

  private SelectField blockerField;

  private TextField uriField;

  private TextField pathField;

  private SelectField agentField;

  private SelectField classloaderOrderField;

  private SelectField classloaderPolicyField;

  private TextField contextRootField;

  private TextField virtualHostField;

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
        Agent agent =
          kalumet.getAgent( parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironment().getAgent() );
        if ( agent == null )
        {
          throw new IllegalArgumentException( "agent not found." );
        }
        // call the web service
        ArchiveClient client = new ArchiveClient( agent.getHostname(), agent.getPort() );
        boolean uptodate =
          client.check( parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName(),
                        parent.getParentPane().getServerName(), parent.getParentPane().getApplicationName(),
                        archiveName );
        if ( uptodate )
        {
          message = "J2EE application " + parent.getParentPane().getApplicationName() + " archive " + archiveName
            + " is up to date.";
        }
        else
        {
          failure = true;
          message = "J2EE appliction " + parent.getParentPane().getApplicationName() + " archive " + archiveName
            + " is not up to date.";
        }
      }
      catch ( Exception e )
      {
        failure = true;
        message = "J2EE application " + parent.getParentPane().getApplicationName() + " archive " + archiveName
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
        Agent agent =
          kalumet.getAgent( parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironment().getAgent() );
        if ( agent == null )
        {
          throw new IllegalArgumentException( "agent not found." );
        }
        // call the web service
        ArchiveClient client = new ArchiveClient( agent.getHostname(), agent.getPort() );
        client.update( parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName(),
                       parent.getParentPane().getServerName(), parent.getParentPane().getApplicationName(), archiveName,
                       false );
      }
      catch ( Exception e )
      {
        failure = true;
        message = "J2EE application " + parent.getParentPane().getApplicationName() + " archive " + archiveName
          + " update failed: " + e.getMessage();
      }
      finally
      {
        ended = true;
      }
    }
  }

  // refresh
  private ActionListener refresh = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // looking for the archive object
      ApplicationArchiveWindow.this.archive = parent.getParentPane().getApplication().getArchive( archiveName );
      if ( ApplicationArchiveWindow.this.archive == null )
      {
        ApplicationArchiveWindow.this.archive = new Archive();
      }
      // update the window
      update();
    }
  };

  // close
  private ActionListener close = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      ApplicationArchiveWindow.this.userClose();
    }
  };

  // delete
  private ActionListener delete = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if the user has the environment lock
      if ( !parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironment().getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !parent.getParentPane().getParentPane().getEnvironmentWindow().adminPermission
        && !parent.getParentPane().getParentPane().getEnvironmentWindow().jeeApplicationsPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // display confirm window
      KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
        new ConfirmWindow( new ActionListener()
        {
          public void actionPerformed( ActionEvent event )
          {
            // delete the archive
            parent.getParentPane().getApplication().getArchives().remove( archive );
            // add a change events
            parent.getParentPane().getParentPane().getEnvironmentWindow().getChangeEvents().add(
              "Delete JEE application archive " + archive.getName() );
            // change the updated flag
            parent.getParentPane().getParentPane().getEnvironmentWindow().setUpdated( true );
            // update the journal log tab pane
            parent.getParentPane().getParentPane().getEnvironmentWindow().updateJournalPane();
            // update the parent pane
            parent.update();
            // close the window
            ApplicationArchiveWindow.this.userClose();
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
      if ( !parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironment().getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !parent.getParentPane().getParentPane().getEnvironmentWindow().adminPermission
        && !parent.getParentPane().getParentPane().getEnvironmentWindow().jeeApplicationsPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // get the fields value
      String nameFieldValue = nameField.getText();
      int activeFieldIndex = activeField.getSelectedIndex();
      int blockerFieldIndex = blockerField.getSelectedIndex();
      String uriFieldValue = uriField.getText();
      String pathFieldValue = pathField.getText();
      int classloaderOrderFieldIndex = classloaderOrderField.getSelectedIndex();
      int classloaderPolicyFieldIndex = classloaderPolicyField.getSelectedIndex();
      String contextRootFieldValue = contextRootField.getText();
      String virtualHostFieldValue = virtualHostField.getText();
      String agentFieldValue = (String) agentField.getSelectedItem();
      // check fields
      if ( nameFieldValue == null || nameFieldValue.trim().length() < 1 || uriFieldValue == null
        || uriFieldValue.trim().length() < 1 )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "archive.mandatory" ),
                                                                            parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // if the user change the archive name, check if the new name doesn't
      // already exist
      if ( archiveName == null || ( archiveName != null && !archiveName.equals( nameFieldValue ) ) )
      {
        if ( parent.getParentPane().getApplication().getArchive( nameFieldValue ) != null )
        {
          KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "archive.exists" ),
                                                                              parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
          return;
        }
      }
      // add a change event
      if ( archiveName != null )
      {
        String change = "Modify J2EE application archive " + nameFieldValue;
        if ( !archive.getUri().equals( uriFieldValue ) )
        {
          change += " URI from " + archive.getUri() + " to " + uriFieldValue;
        }
        if ( !archive.getPath().equals( pathFieldValue ) )
        {
          change += " path from " + archive.getPath() + " to " + pathFieldValue;
        }
        parent.getParentPane().getParentPane().getEnvironmentWindow().getChangeEvents().add( change );
      }
      // update the archive object
      archive.setName( nameFieldValue );
      if ( activeFieldIndex == 0 )
      {
        archive.setActive( true );
      }
      else
      {
        archive.setActive( false );
      }
      if ( blockerFieldIndex == 0 )
      {
        archive.setBlocker( true );
      }
      else
      {
        archive.setBlocker( false );
      }
      archive.setUri( uriFieldValue );
      archive.setPath( pathFieldValue );
      archive.setAgent( agentFieldValue );
      if ( classloaderOrderFieldIndex == 0 )
      {
        archive.setClassloaderorder( "PARENT_LAST" );
      }
      else
      {
        archive.setClassloaderorder( "PARENT_FIRST" );
      }
      if ( classloaderPolicyFieldIndex == 0 )
      {
        archive.setClassloaderpolicy( "SINGLE" );
      }
      else
      {
        archive.setClassloaderpolicy( "MULTIPLE" );
      }
      archive.setContext( contextRootFieldValue );
      archive.setVhost( virtualHostFieldValue );
      // add the archive object if needed
      if ( archiveName == null )
      {
        try
        {
          parent.getParentPane().getApplication().addArchive( archive );
          parent.getParentPane().getParentPane().getEnvironmentWindow().getChangeEvents().add(
            "Add J2EE application archive " + archive.getName() );
        }
        catch ( Exception e )
        {
          KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "archive.exists" ),
                                                                              parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
          return;
        }
      }
      // update the window definition
      setTitle( Messages.getString( "archive" ) + " " + archive.getName() );
      setId( "archivewindow_" + parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() + "_"
               + parent.getParentPane().getServerName() + "_" + parent.getParentPane().getApplicationName() + "_"
               + archive.getName() );
      archiveName = archive.getName();
      // change the updated flag
      parent.getParentPane().getParentPane().getEnvironmentWindow().setUpdated( true );
      // update the journal log tab pane
      parent.getParentPane().getParentPane().getEnvironmentWindow().updateJournalPane();
      // update the parent window
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
        KalumetConsoleApplication.getApplication().setCopyComponent( archive.clone() );
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
      // check if the copy is correct
      if ( copy == null || !( copy instanceof Archive ) )
      {
        return;
      }
      archive = (Archive) copy;
      archiveName = null;
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
      if ( parent.getParentPane().getParentPane().getEnvironmentWindow().isUpdated() )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "environment.notsaved" ),
          parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // add a message into the log pane and the journal
      KalumetConsoleApplication.getApplication().getLogPane().addInfo(
        "JEE application " + parent.getParentPane().getApplicationName() + " archive " + archiveName
          + " status check in progress...",
        parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
      parent.getParentPane().getParentPane().getEnvironmentWindow().getChangeEvents().add(
        "JEE application " + parent.getParentPane().getApplicationName() + " archive " + archiveName
          + " status check requested." );
      // launch the status thread
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
                                                                        parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
                                                                    }
                                                                    else
                                                                    {
                                                                      KalumetConsoleApplication.getApplication().getLogPane().addInfo(
                                                                        statusThread.message,
                                                                        parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
                                                                    }
                                                                    parent.getParentPane().getParentPane().getEnvironmentWindow().getChangeEvents().add(
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
      if ( !parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironment().getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !parent.getParentPane().getParentPane().getEnvironmentWindow().adminPermission
        && !parent.getParentPane().getParentPane().getEnvironmentWindow().jeeApplicationsUpdatePermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if there is no pending change
      if ( parent.getParentPane().getParentPane().getEnvironmentWindow().isUpdated() )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "environment.notsaved" ),
          parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // display confirm window
      KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
        new ConfirmWindow( new ActionListener()
        {
          public void actionPerformed( ActionEvent event )
          {
            // add a message in the log pane and the journal
            KalumetConsoleApplication.getApplication().getLogPane().addInfo(
              "JEE application " + parent.getParentPane().getApplicationName() + " archive " + archiveName
                + " update in progress...",
              parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
            parent.getParentPane().getParentPane().getEnvironmentWindow().getChangeEvents().add(
              "JEE application " + parent.getParentPane().getApplicationName() + " archive " + archiveName
                + " update requested." );
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
                                                                                      parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
                    parent.getParentPane().getParentPane().getEnvironmentWindow().getChangeEvents().add(
                      updateThread.message );
                  }
                  else
                  {
                    KalumetConsoleApplication.getApplication().getLogPane().addConfirm(
                      "JEE application " + parent.getParentPane().getApplicationName() + " archive " + archiveName
                        + " updated.",
                      parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
                    parent.getParentPane().getParentPane().getEnvironmentWindow().getChangeEvents().add(
                      "JEE application " + parent.getParentPane().getApplicationName() + " archive " + archiveName
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
   * Create a new <code>ApplicationArchiveWindow</code>.
   *
   * @param parent      the <code>ApplicationArchivesPane</code>.
   * @param archiveName the original <code>Archive</code> name.
   */
  public ApplicationArchiveWindow( ApplicationArchivesPane parent, String archiveName )
  {
    super();

    // update the parent pane
    this.parent = parent;

    // update the archive name
    this.archiveName = archiveName;

    // update the archive object from the parent pane
    this.archive = parent.getParentPane().getApplication().getArchive( archiveName );
    if ( this.archive == null )
    {
      this.archive = new Archive();
    }

    if ( archiveName == null )
    {
      setTitle( Messages.getString( "archive" ) );
    }
    else
    {
      setTitle( Messages.getString( "archive" ) + " " + archiveName );
    }
    setId( "archivewindow_" + parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() + "_"
             + parent.getParentPane().getServerName() + "_" + parent.getParentPane().getApplicationName() + "_"
             + archiveName );
    setStyleName( "default" );
    setWidth( new Extent( 450, Extent.PX ) );
    setHeight( new Extent( 300, Extent.PX ) );
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
    if ( parent.getParentPane().getParentPane().getEnvironmentWindow().adminPermission
      || parent.getParentPane().getParentPane().getEnvironmentWindow().jeeApplicationsPermission )
    {
      // add the paste button
      Button pasteButton = new Button( Messages.getString( "paste" ), Styles.PAGE_PASTE );
      pasteButton.setStyleName( "control" );
      pasteButton.addActionListener( paste );
      controlRow.add( pasteButton );
    }
    // status
    Button statusButton = new Button( Messages.getString( "status" ), Styles.INFORMATION );
    statusButton.setStyleName( "control" );
    statusButton.addActionListener( status );
    controlRow.add( statusButton );
    if ( parent.getParentPane().getParentPane().getEnvironmentWindow().adminPermission
      || parent.getParentPane().getParentPane().getEnvironmentWindow().jeeApplicationsUpdatePermission )
    {
      // update
      Button updateButton = new Button( Messages.getString( "update" ), Styles.COG );
      updateButton.setStyleName( "control" );
      updateButton.addActionListener( update );
      controlRow.add( updateButton );
    }
    if ( parent.getParentPane().getParentPane().getEnvironmentWindow().adminPermission
      || parent.getParentPane().getParentPane().getEnvironmentWindow().jeeApplicationsPermission )
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

    // add the general tab
    TabPaneLayoutData tabLayoutData = new TabPaneLayoutData();

    tabLayoutData.setTitle( Messages.getString( "general" ) );
    ContentPane generalTabPane = new ContentPane();
    generalTabPane.setStyleName( "tab.content" );
    generalTabPane.setLayoutData( tabLayoutData );
    tabPane.add( generalTabPane );

    Grid generalLayoutGrid = new Grid( 2 );
    generalLayoutGrid.setStyleName( "default" );
    generalLayoutGrid.setColumnWidth( 0, new Extent( 20, Extent.PERCENT ) );
    generalLayoutGrid.setColumnWidth( 1, new Extent( 80, Extent.PERCENT ) );
    generalTabPane.add( generalLayoutGrid );
    // name
    Label nameLabel = new Label( Messages.getString( "name" ) );
    nameLabel.setStyleName( "grid.cell" );
    generalLayoutGrid.add( nameLabel );
    nameField = new TextField();
    nameField.setStyleName( "default" );
    nameField.setWidth( new Extent( 100, Extent.PERCENT ) );
    generalLayoutGrid.add( nameField );
    // active
    Label activeLabel = new Label( Messages.getString( "active" ) );
    activeLabel.setStyleName( "grid.cell" );
    generalLayoutGrid.add( activeLabel );
    activeField = new SelectField( MainScreen.LABELS );
    activeField.setStyleName( "default" );
    activeField.setWidth( new Extent( 10, Extent.EX ) );
    activeField.setSelectedIndex( 0 );
    generalLayoutGrid.add( activeField );
    // blocker
    Label blockerLabel = new Label( Messages.getString( "blocker" ) );
    blockerLabel.setStyleName( "grid.cell" );
    generalLayoutGrid.add( blockerLabel );
    blockerField = new SelectField( MainScreen.LABELS );
    blockerField.setStyleName( "default" );
    blockerField.setWidth( new Extent( 10, Extent.EX ) );
    blockerField.setSelectedIndex( 1 );
    generalLayoutGrid.add( blockerField );
    // URI
    Label uriLabel = new Label( Messages.getString( "uri" ) );
    uriLabel.setStyleName( "grid.cell" );
    generalLayoutGrid.add( uriLabel );
    uriField = new TextField();
    uriField.setStyleName( "default" );
    uriField.setWidth( new Extent( 100, Extent.PERCENT ) );
    generalLayoutGrid.add( uriField );
    // path
    Label pathLabel = new Label( Messages.getString( "path" ) );
    pathLabel.setStyleName( "grid.cell" );
    generalLayoutGrid.add( pathLabel );
    pathField = new TextField();
    pathField.setStyleName( "default" );
    pathField.setWidth( new Extent( 100, Extent.PERCENT ) );
    generalLayoutGrid.add( pathField );
    // agent
    Label agentLabel = new Label( Messages.getString( "agent" ) );
    agentLabel.setStyleName( "grid.cell" );
    generalLayoutGrid.add( agentLabel );
    agentField = new SelectField();
    agentField.setStyleName( "default" );
    agentField.setWidth( new Extent( 50, Extent.EX ) );
    generalLayoutGrid.add( agentField );

    // add the deployment tab
    tabLayoutData = new TabPaneLayoutData();
    tabLayoutData.setTitle( Messages.getString( "deployment" ) );
    ContentPane deploymentTabPane = new ContentPane();
    deploymentTabPane.setStyleName( "tab.content" );
    deploymentTabPane.setLayoutData( tabLayoutData );
    tabPane.add( deploymentTabPane );
    Grid deploymentLayoutGrid = new Grid( 2 );
    deploymentLayoutGrid.setStyleName( "default" );
    deploymentLayoutGrid.setColumnWidth( 0, new Extent( 20, Extent.PERCENT ) );
    deploymentLayoutGrid.setColumnWidth( 1, new Extent( 80, Extent.PERCENT ) );
    deploymentTabPane.add( deploymentLayoutGrid );
    // classloader mode
    Label archiveClassloaderOrderLabel = new Label( Messages.getString( "classloader.order" ) );
    archiveClassloaderOrderLabel.setStyleName( "grid.cell" );
    deploymentLayoutGrid.add( archiveClassloaderOrderLabel );
    classloaderOrderField = new SelectField( ApplicationArchiveWindow.CLASSLOADER_ORDER );
    classloaderOrderField.setStyleName( "default" );
    classloaderOrderField.setSelectedIndex( 0 );
    classloaderOrderField.setWidth( new Extent( 50, Extent.EX ) );
    deploymentLayoutGrid.add( classloaderOrderField );
    // classloader policy
    Label archiveClassLoaderPolicyLabel = new Label( Messages.getString( "classloader.policy" ) );
    archiveClassLoaderPolicyLabel.setStyleName( "grid.cell" );
    deploymentLayoutGrid.add( archiveClassLoaderPolicyLabel );
    classloaderPolicyField = new SelectField( ApplicationArchiveWindow.CLASSLOADER_POLICY );
    classloaderPolicyField.setStyleName( "default" );
    classloaderPolicyField.setSelectedIndex( 0 );
    classloaderPolicyField.setWidth( new Extent( 50, Extent.EX ) );
    deploymentLayoutGrid.add( classloaderPolicyField );
    // context root
    Label archiveContextLabel = new Label( Messages.getString( "contextroot" ) );
    archiveContextLabel.setStyleName( "grid.cell" );
    deploymentLayoutGrid.add( archiveContextLabel );
    contextRootField = new TextField();
    contextRootField.setStyleName( "default" );
    contextRootField.setWidth( new Extent( 100, Extent.PERCENT ) );
    deploymentLayoutGrid.add( contextRootField );
    Label archiveVHostLabel = new Label( Messages.getString( "virtualhost" ) );
    archiveVHostLabel.setStyleName( "grid.cell" );
    deploymentLayoutGrid.add( archiveVHostLabel );
    virtualHostField = new TextField();
    virtualHostField.setStyleName( "default" );
    virtualHostField.setWidth( new Extent( 100, Extent.PERCENT ) );
    deploymentLayoutGrid.add( virtualHostField );

    // update the window
    update();
  }

  /**
   * Update the pane.
   */
  public void update()
  {
    // update the archive name field
    nameField.setText( archive.getName() );
    // update the archive active field
    if ( archive.isActive() )
    {
      activeField.setSelectedIndex( 0 );
    }
    else
    {
      activeField.setSelectedIndex( 1 );
    }
    // update the archive blocker field
    if ( archive.isBlocker() )
    {
      blockerField.setSelectedIndex( 0 );
    }
    else
    {
      blockerField.setSelectedIndex( 1 );
    }
    // update the archive uri field
    uriField.setText( archive.getUri() );
    // update the archive path field
    pathField.setText( archive.getPath() );
    // update the archive classloader order field
    if ( archive.getClassloaderorder() != null )
    {
      if ( archive.getClassloaderorder().equals( "PARENT_FIRST" ) )
      {
        classloaderOrderField.setSelectedIndex( 1 );
      }
      else
      {
        classloaderOrderField.setSelectedIndex( 0 );
      }
    }
    // update the archive classloader policy field
    if ( archive.getClassloaderpolicy() != null )
    {
      if ( archive.getClassloaderpolicy().equals( "SINGLE" ) )
      {
        classloaderPolicyField.setSelectedIndex( 0 );
      }
      else
      {
        classloaderPolicyField.setSelectedIndex( 1 );
      }
    }
    // update the archive context field
    contextRootField.setText( archive.getContext() );
    // update the archive vhost field
    virtualHostField.setText( archive.getVhost() );
    // update agent field
    // load Kalumet configuration
    Kalumet kalumet;
    try
    {
      kalumet = ConfigurationManager.loadStore();
    }
    catch ( Exception e )
    {
      KalumetConsoleApplication.getApplication().getLogPane().addError(
        Messages.getString( "db.read" ) + ": " + e.getMessage(),
        parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
      return;
    }
    // update agent list model
    DefaultListModel agentListModel = (DefaultListModel) agentField.getModel();
    agentListModel.removeAll();
    agentListModel.add( "" );
    for ( Iterator agentIterator = kalumet.getAgents().iterator(); agentIterator.hasNext(); )
    {
      Agent agent = (Agent) agentIterator.next();
      agentListModel.add( agent.getId() );
    }
    // select the item
    agentField.setSelectedItem( archive.getAgent() );
  }

}
