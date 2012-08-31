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
import org.apache.kalumet.console.configuration.ConfigurationManager;
import org.apache.kalumet.model.Agent;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.model.Location;
import org.apache.kalumet.ws.client.SoftwareClient;

import java.util.Iterator;

/**
 * Software location window.
 */
public class SoftwareLocationWindow
  extends WindowPane
{

  private String name;

  private SoftwareWindow parent;

  private Location location;

  private TextField nameField;

  private SelectField activeField;

  private SelectField blockerField;

  private TextField uriField;

  private TextField pathField;

  private SelectField agentField;

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
        Agent agent = kalumet.getAgent( parent.getParentPane().getEnvironmentWindow().getEnvironment().getAgent() );
        if ( agent == null )
        {
          throw new IllegalArgumentException( "agent not found." );
        }
        // call the WebService
        SoftwareClient client = new SoftwareClient( agent.getHostname(), agent.getPort() );
        client.updateLocation( parent.getParentPane().getEnvironmentWindow().getEnvironmentName(), parent.getName(),
                               name, false );
      }
      catch ( Exception e )
      {
        failure = true;
        message = "Software " + parent.getName() + " location " + name + " update failed: " + e.getMessage();
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
      // looking for the location in the parent
      location = parent.getSoftware().getLocation( name );
      if ( location == null )
      {
        location = new Location();
      }
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
        KalumetConsoleApplication.getApplication().setCopyComponent( location.clone() );
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
      if ( copy == null || !( copy instanceof Location ) )
      {
        return;
      }
      location = (Location) copy;
      name = null;
      parent.update();
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
      if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().softwareChangePermission)
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // get the fields value
      String nameFieldValue = nameField.getText();
      int activeFieldIndex = activeField.getSelectedIndex();
      int blockerFieldIndex = blockerField.getSelectedIndex();
      String uriFieldValue = uriField.getText();
      String pathFieldValue = pathField.getText();
      String agentFieldValue = (String) agentField.getSelectedItem();
      if ( nameFieldValue == null || nameFieldValue.trim().length() < 1 || uriFieldValue == null
        || uriFieldValue.trim().length() < 1 || pathFieldValue == null || pathFieldValue.trim().length() < 1 )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "location.mandatory" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      location.setName( nameFieldValue );
      if ( activeFieldIndex == 0 )
      {
        location.setActive( true );
      }
      else
      {
        location.setActive( false );
      }
      if ( blockerFieldIndex == 0 )
      {
        location.setBlocker( true );
      }
      else
      {
        location.setBlocker( false );
      }
      location.setUri( uriFieldValue );
      location.setPath( pathFieldValue );
      location.setAgent( agentFieldValue );
      if ( name == null )
      {
        try
        {
          parent.getSoftware().addLocation( location );
          parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
            "Add software " + parent.getName() + " location " + name );
        }
        catch ( Exception e )
        {
          KalumetConsoleApplication.getApplication().getLogPane().addWarning(
            Messages.getString( "software.component.exists" ), getEnvironmentWindow().getEnvironmentName() );
          return;
        }
      }
      // update window definition
      setTitle( Messages.getString( "location" ) + " " + location.getName() );
      setId( "softwarelocationwindow_" + parent.getParentPane().getEnvironmentWindow().getEnvironmentName() + "_"
               + parent.getName() + "_" + name );
      name = location.getName();
      // change the update flag
      parent.getParentPane().getEnvironmentWindow().setUpdated( true );
      // update parent and window
      parent.update();
      update();
    }
  };

  // delete
  private ActionListener delete = new ActionListener()
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
      if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().softwareChangePermission)
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
            // delete the location
            parent.getSoftware().getUpdatePlan().remove( location );
            parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
              "Delete software " + parent.getName() + " location " + name );
            parent.getParentPane().getEnvironmentWindow().setUpdated( true );
            parent.getParentPane().getEnvironmentWindow().updateJournalPane();
            parent.update();
            SoftwareLocationWindow.this.userClose();
          }
        } ) );
    }
  };

  // close
  private ActionListener close = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      SoftwareLocationWindow.this.userClose();
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
      if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().softwareUpdatePermission)
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if some change has not yet been saved
      if ( getEnvironmentWindow().isUpdated() )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "environment.notsaved" ), getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // display a confirm window
      KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
        new ConfirmWindow( new ActionListener()
        {
          public void actionPerformed( ActionEvent event )
          {
            // add a message into the log panel and the journal
            KalumetConsoleApplication.getApplication().getLogPane().addInfo(
              "Software " + parent.getName() + " location " + name + " update in progress ...",
              parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
            parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
              "Software " + parent.getName() + " location " + name + " update requested." );
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
                                                                                      parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
                    parent.getParentPane().getEnvironmentWindow().getChangeEvents().add( updateThread.message );
                  }
                  else
                  {
                    KalumetConsoleApplication.getApplication().getLogPane().addConfirm(
                      "Software " + parent.getName() + " location " + name + " updated.",
                      parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
                    parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
                      "Software " + parent.getName() + " location " + name + " updated." );
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
   * Create a new software location window.
   *
   * @param parent the parent software window.
   * @param name   the linked location name.
   */
  public SoftwareLocationWindow( SoftwareWindow parent, String name )
  {
    super();

    // update parent and location name
    this.parent = parent;
    this.name = name;

    // update the location from the parent
    this.location = parent.getSoftware().getLocation( name );
    if ( this.location == null )
    {
      this.location = new Location();
    }

    if ( name == null )
    {
      setTitle( Messages.getString( "location" ) );
    }
    else
    {
      setTitle( Messages.getString( "location" ) + " " + name );
    }
    setId( "softwarelocationwindow_" + parent.getParentPane().getEnvironmentWindow().getEnvironmentName() + "_"
             + parent.getName() + "_" + name );
    setStyleName( "default" );
    setWidth( new Extent( 500, Extent.PX ) );
    setHeight( new Extent( 400, Extent.PX ) );
    setModal( false );
    setDefaultCloseOperation( WindowPane.DISPOSE_ON_CLOSE );

    // create the split pane
    SplitPane splitPane = new SplitPane( SplitPane.ORIENTATION_VERTICAL_BOTTOM_TOP, new Extent( 32 ) );
    add( splitPane );

    // add the control row
    Row controlRow = new Row();
    controlRow.setStyleName( "control" );
    splitPane.add( controlRow );
    // reload
    Button reloadButton = new Button( Messages.getString( "reload" ), Styles.DATABASE_REFRESH );
    reloadButton.setStyleName( "control" );
    reloadButton.addActionListener( refresh );
    controlRow.add( reloadButton );
    // copy
    Button copyButton = new Button( Messages.getString( "copy" ), Styles.PAGE_COPY );
    copyButton.setStyleName( "control" );
    copyButton.addActionListener( copy );
    controlRow.add( copyButton );
    if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().softwareChangePermission)
    {
      // paste
      Button pasteButton = new Button( Messages.getString( "paste" ), Styles.PAGE_PASTE );
      pasteButton.setStyleName( "control" );
      pasteButton.addActionListener( paste );
      controlRow.add( pasteButton );
    }
    if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().softwareUpdatePermission)
    {
      // update
      Button updateButton = new Button( Messages.getString( "update" ), Styles.COG );
      updateButton.setStyleName( "control" );
      updateButton.addActionListener( update );
      controlRow.add( updateButton );
    }
    if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().softwareChangePermission)
    {
      // apply
      Button applyButton = new Button( Messages.getString( "apply" ), Styles.ACCEPT );
      applyButton.setStyleName( "control" );
      applyButton.addActionListener( apply );
      controlRow.add( applyButton );
      // delete
      Button deleteButton = new Button( Messages.getString( "delete" ), Styles.DELETE );
      deleteButton.setStyleName( "control" );
      deleteButton.addActionListener( delete );
      controlRow.add( deleteButton );
    }
    // close
    Button closeButton = new Button( Messages.getString( "close" ), Styles.CROSS );
    closeButton.setStyleName( "control" );
    closeButton.addActionListener( close );
    controlRow.add( closeButton );

    // add the main grid
    Grid layout = new Grid( 2 );
    layout.setStyleName( "default" );
    layout.setColumnWidth( 0, new Extent( 20, Extent.PERCENT ) );
    layout.setColumnWidth( 1, new Extent( 80, Extent.PERCENT ) );
    splitPane.add( layout );

    // name
    Label nameLabel = new Label( Messages.getString( "name" ) );
    nameLabel.setStyleName( "grid.cell" );
    layout.add( nameLabel );
    nameField = new TextField();
    nameField.setStyleName( "default" );
    nameField.setWidth( new Extent( 100, Extent.PERCENT ) );
    layout.add( nameField );

    // active
    Label activeLabel = new Label( Messages.getString( "active" ) );
    activeLabel.setStyleName( "grid.cell" );
    layout.add( activeLabel );
    activeField = new SelectField( MainScreen.LABELS );
    activeField.setStyleName( "default" );
    activeField.setWidth( new Extent( 10, Extent.EX ) );
    layout.add( activeField );

    // blocker
    Label blockerLabel = new Label( Messages.getString( "blocker" ) );
    blockerLabel.setStyleName( "grid.cell" );
    layout.add( blockerLabel );
    blockerField = new SelectField( MainScreen.LABELS );
    blockerField.setStyleName( "default" );
    blockerField.setWidth( new Extent( 10, Extent.EX ) );
    layout.add( blockerField );

    // uri
    Label uriLabel = new Label( Messages.getString( "uri" ) );
    uriLabel.setStyleName( "grid.cell" );
    layout.add( uriLabel );
    uriField = new TextField();
    uriField.setStyleName( "default" );
    uriField.setWidth( new Extent( 100, Extent.PERCENT ) );
    layout.add( uriField );

    // path
    Label pathLabel = new Label( Messages.getString( "path" ) );
    pathLabel.setStyleName( "grid.cell" );
    layout.add( pathLabel );
    pathField = new TextField();
    pathField.setStyleName( "default" );
    pathField.setWidth( new Extent( 100, Extent.PERCENT ) );
    layout.add( pathField );

    // agent
    Label agentLabel = new Label( Messages.getString( "agent" ) );
    agentLabel.setStyleName( "grid.cell" );
    layout.add( agentLabel );
    agentField = new SelectField();
    agentField.setStyleName( "default" );
    agentField.setWidth( new Extent( 50, Extent.EX ) );
    layout.add( agentField );

    // update
    update();
  }

  public void update()
  {
    nameField.setText( location.getName() );
    if ( location.isActive() )
    {
      activeField.setSelectedIndex( 0 );
    }
    else
    {
      activeField.setSelectedIndex( 1 );
    }
    if ( location.isBlocker() )
    {
      blockerField.setSelectedIndex( 0 );
    }
    else
    {
      blockerField.setSelectedIndex( 1 );
    }
    uriField.setText( location.getUri() );
    pathField.setText( location.getPath() );
    // agent update
    Kalumet kalumet;
    try
    {
      kalumet = ConfigurationManager.loadStore();
    }
    catch ( Exception e )
    {
      KalumetConsoleApplication.getApplication().getLogPane().addError(
        Messages.getString( "db.read" ) + ": " + e.getMessage(),
        parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
      return;
    }
    // update agent list model
    DefaultListModel agentListModel = (DefaultListModel) agentField.getModel();
    agentListModel.removeAll();
    for ( Iterator agentIterator = kalumet.getAgents().iterator(); agentIterator.hasNext(); )
    {
      Agent agent = (Agent) agentIterator.next();
      agentListModel.add( agent.getId() );
    }
    agentField.setSelectedItem( location.getAgent() );
  }

  public EnvironmentWindow getEnvironmentWindow()
  {
    return parent.getParentPane().getEnvironmentWindow();
  }

}
