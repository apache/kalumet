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
import nextapp.echo2.app.Insets;
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
import org.apache.kalumet.model.ConfigurationFile;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.model.Mapping;
import org.apache.kalumet.ws.client.SoftwareClient;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Software configuration file window.
 */
public class SoftwareConfigurationFileWindow
  extends WindowPane
{

  private String name;

  private ConfigurationFile configurationFile;

  private SoftwareWindow parent;

  private TextField nameField;

  private SelectField activeField;

  private SelectField blockerField;

  private TextField uriField;

  private TextField pathField;

  private SelectField agentField;

  private Grid mappingsGrid;

  private TextField newMappingKeyField;

  private TextField newMappingValueField;

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
        client.updateConfigurationFile( parent.getParentPane().getEnvironmentWindow().getEnvironmentName(),
                                        parent.getName(), name, false );
      }
      catch ( Exception e )
      {
        failure = true;
        message = "Software " + parent.getName() + " configuration file " + name + " update failed: " + e.getMessage();
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
      // looking for the configuration file object
      SoftwareConfigurationFileWindow.this.configurationFile = parent.getSoftware().getConfigurationFile( name );
      if ( SoftwareConfigurationFileWindow.this.configurationFile == null )
      {
        SoftwareConfigurationFileWindow.this.configurationFile = new ConfigurationFile();
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
      SoftwareConfigurationFileWindow.this.userClose();
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
            // delete the configuration file
            parent.getSoftware().getUpdatePlan().remove( configurationFile );
            // add a change event
            parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
              "Delete software " + parent.getName() + " configuration file " + configurationFile.getName() );
            // change the updated flag
            parent.getParentPane().getEnvironmentWindow().setUpdated( true );
            // update the journal log tab pane
            parent.getParentPane().getEnvironmentWindow().updateJournalPane();
            // update the parent pane
            parent.update();
            // close the window
            SoftwareConfigurationFileWindow.this.userClose();
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
      // check fields
      if ( nameFieldValue == null || nameFieldValue.trim().length() < 1 || uriFieldValue == null
        || uriFieldValue.trim().length() < 1 )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "configurationfile.mandatory" ), getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // if the user change the configuration file name, check if the new
      // name doesn't already exist
      if ( name == null || ( name != null && !name.equals( nameFieldValue ) ) )
      {
        if ( parent.getSoftware().getConfigurationFile( nameFieldValue ) != null )
        {
          KalumetConsoleApplication.getApplication().getLogPane().addWarning(
            Messages.getString( "software.component.exists" ), getEnvironmentWindow().getEnvironmentName() );
          return;
        }
      }
      // add a change event
      if ( name != null )
      {
        parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
          "Change software " + parent.getName() + " configuration file " + configurationFile.getName() );
      }
      // update the configuration file object
      configurationFile.setName( nameFieldValue );
      if ( activeFieldIndex == 0 )
      {
        configurationFile.setActive( true );
      }
      else
      {
        configurationFile.setActive( false );
      }
      if ( blockerFieldIndex == 0 )
      {
        configurationFile.setBlocker( true );
      }
      else
      {
        configurationFile.setBlocker( false );
      }
      configurationFile.setUri( uriFieldValue );
      configurationFile.setPath( pathFieldValue );
      configurationFile.setAgent( agentFieldValue );
      // add the configuration file object if needed
      if ( name == null )
      {
        try
        {
          parent.getSoftware().addConfigurationFile( configurationFile );
          parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
            "Add software " + parent.getName() + " configuration file " + configurationFile.getName() );
        }
        catch ( Exception e )
        {
          KalumetConsoleApplication.getApplication().getLogPane().addWarning(
            Messages.getString( "software.component.exists" ), getEnvironmentWindow().getEnvironmentName() );
          return;
        }
      }
      // update the window definition
      setTitle( Messages.getString( "configurationfile" ) + " " + configurationFile.getName() );
      setId(
        "softwareconfigurationfilewindow_" + parent.getParentPane().getEnvironmentWindow().getEnvironmentName() + "_"
          + parent.getName() + "_" + configurationFile.getName() );
      name = configurationFile.getName();
      // change the updated flag
      parent.getParentPane().getEnvironmentWindow().setUpdated( true );
      // update the journal log tab pane
      parent.getParentPane().getEnvironmentWindow().updateJournalPane();
      // update the parent window
      parent.update();
      // update the window
      update();
    }
  };

  // delete mapping
  private ActionListener deleteMapping = new ActionListener()
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
      // looking for the mapping object
      Mapping mapping = configurationFile.getMapping( event.getActionCommand() );
      if ( mapping == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "mapping.notfound" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // delete the mapping object
      configurationFile.getMappings().remove( mapping );
      // add a change event
      parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
        "Delete software " + parent.getName() + " configuration file " + configurationFile.getName() + " mapping "
          + mapping.getKey() );
      // change the updated flag
      parent.getParentPane().getEnvironmentWindow().setUpdated( true );
      // update the journal log tab pane
      parent.getParentPane().getEnvironmentWindow().updateJournalPane();
      // update the window
      update();
    }
  };

  // edit mapping
  private ActionListener editMapping = new ActionListener()
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
      if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().softwareChangePermission)
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // get fields
      TextField keyField = (TextField) SoftwareConfigurationFileWindow.this.getComponent(
        "softwareconfigurationfilemappingkey_" + parent.getParentPane().getEnvironmentWindow().getEnvironmentName()
          + "_" + parent.getName() + "_" + name + "_" + event.getActionCommand() );
      TextField valueField = (TextField) SoftwareConfigurationFileWindow.this.getComponent(
        "softwareconfigurationfilemappingvalue_" + parent.getParentPane().getEnvironmentWindow().getEnvironmentName()
          + "_" + parent.getName() + "_" + name + "_" + event.getActionCommand() );
      // get fields value
      String keyFieldValue = keyField.getText();
      String valueFieldValue = valueField.getText();
      // check fields
      if ( keyFieldValue == null || keyFieldValue.trim().length() < 1 )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "mapping.mandatory" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // if the user change the mapping key, check if the key doesn't already
      // exist
      if ( !keyFieldValue.equals( event.getActionCommand() ) )
      {
        if ( configurationFile.getMapping( keyFieldValue ) != null )
        {
          KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "mapping.exists" ),
                                                                              getEnvironmentWindow().getEnvironmentName() );
          return;
        }
      }
      // looking for the mapping object
      Mapping mapping = configurationFile.getMapping( event.getActionCommand() );
      if ( mapping == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "mapping.notfound" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // add a change event
      parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
        "Change software " + parent.getName() + " configuration file " + configurationFile.getName() + " mapping "
          + mapping.getKey() );
      // update the mapping
      mapping.setKey( keyFieldValue );
      mapping.setValue( valueFieldValue );
      // change the updated flag
      parent.getParentPane().getEnvironmentWindow().setUpdated( true );
      // update the journal log tab pane
      parent.getParentPane().getEnvironmentWindow().updateJournalPane();
      // update the window
      update();
    }
  };

  // create mapping
  private ActionListener createMapping = new ActionListener()
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
      if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().softwareChangePermission)
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // get fields value
      String newMappingKeyFieldValue = newMappingKeyField.getText();
      String newMappingValueFieldValue = newMappingValueField.getText();
      // check fields
      if ( newMappingKeyFieldValue == null || newMappingKeyFieldValue.trim().length() < 1 )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "mapping.mandatory" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // create the mapping object
      Mapping mapping = new Mapping();
      mapping.setKey( newMappingKeyFieldValue );
      mapping.setValue( newMappingValueFieldValue );
      try
      {
        configurationFile.addMapping( mapping );
      }
      catch ( Exception e )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "mapping.exists" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // add a change event
      parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
        "Add software " + parent.getName() + " configuration file " + configurationFile.getName() + " mapping "
          + mapping.getKey() );
      // change the updated flag
      parent.getParentPane().getEnvironmentWindow().setUpdated( true );
      // update the journal log tab pane
      parent.getParentPane().getEnvironmentWindow().updateJournalPane();
      // update the window
      update();
    }
  };

  // copy button
  private ActionListener copy = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      try
      {
        KalumetConsoleApplication.getApplication().setCopyComponent( configurationFile.clone() );
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
      if ( copy == null || !( copy instanceof ConfigurationFile ) )
      {
        return;
      }
      configurationFile = (ConfigurationFile) copy;
      name = null;
      // update the parent pane
      parent.update();
      // update the window
      update();
    }
  };

  // copy mapping
  private ActionListener copyMapping = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // looking for the mapping object
      Mapping mapping = configurationFile.getMapping( event.getActionCommand() );
      if ( mapping == null )
      {
        return;
      }
      try
      {
        KalumetConsoleApplication.getApplication().setCopyComponent( mapping.clone() );
      }
      catch ( Exception e )
      {
        return;
      }
    }
  };

  // paste mapping
  private ActionListener pasteMapping = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      Object copy = KalumetConsoleApplication.getApplication().getCopyComponent();
      // check if the copy is correct
      if ( copy == null || !( copy instanceof Mapping ) )
      {
        return;
      }
      // update new fields
      newMappingKeyField.setText( ( (Mapping) copy ).getKey() );
      newMappingValueField.setText( ( (Mapping) copy ).getValue() );
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
      // display the confirm window
      KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
        new ConfirmWindow( new ActionListener()
        {
          public void actionPerformed( ActionEvent event )
          {
            // add a message into the log pane and the journal
            KalumetConsoleApplication.getApplication().getLogPane().addInfo(
              "Software " + parent.getName() + " configuration file " + name + " update in progress ...",
              parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
            parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
              "Software " + parent.getName() + " configuration file " + name + " update requested." );
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
                      "Software " + parent.getName() + " configuration file " + name + " updated.",
                      parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
                    parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(
                      "Software " + parent.getName() + " configuration file " + name + " updated." );
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
   * Create a software configuration file window.
   *
   * @param parent the parent software window.
   * @param name   the linked configuration file name.
   */
  public SoftwareConfigurationFileWindow( SoftwareWindow parent, String name )
  {
    super();

    // update the parent and name
    this.parent = parent;
    this.name = name;

    // update configuration file from the parent
    this.configurationFile = parent.getSoftware().getConfigurationFile( name );
    if ( this.configurationFile == null )
    {
      this.configurationFile = new ConfigurationFile();
    }

    // update the window title
    if ( name == null )
    {
      setTitle( Messages.getString( "configurationfile" ) );
    }
    else
    {
      setTitle( Messages.getString( "configurationfile" ) + " " + name );
    }
    setId( "softwareconfigurationfilewindow_" + parent.getParentPane().getEnvironmentWindow().getEnvironmentName() + "_"
             + parent.getName() + "_" + name );
    setStyleName( "default" );
    setWidth( new Extent( 600, Extent.PX ) );
    setHeight( new Extent( 400, Extent.PX ) );
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
    if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().softwareChangePermission)
    {
      // add the paste button
      Button pasteButton = new Button( Messages.getString( "paste" ), Styles.PAGE_PASTE );
      pasteButton.setStyleName( "control" );
      pasteButton.addActionListener( paste );
      controlRow.add( pasteButton );
    }
    if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().softwareUpdatePermission)
    {
      // add the update button
      Button updateButton = new Button( Messages.getString( "update" ), Styles.COG );
      updateButton.setStyleName( "control" );
      updateButton.addActionListener( update );
      controlRow.add( updateButton );
    }
    if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().softwareChangePermission)
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
    agentField.setWidth( new Extent( 100, Extent.PERCENT ) );
    generalLayoutGrid.add( agentField );

    // add the mappings tab
    tabLayoutData = new TabPaneLayoutData();
    tabLayoutData.setTitle( Messages.getString( "mappings" ) );
    ContentPane mappingsTabPane = new ContentPane();
    mappingsTabPane.setStyleName( "tab.content" );
    mappingsTabPane.setLayoutData( tabLayoutData );
    tabPane.add( mappingsTabPane );
    mappingsGrid = new Grid( 3 );
    mappingsGrid.setStyleName( "border.grid" );
    mappingsGrid.setColumnWidth( 0, new Extent( 50, Extent.PX ) );
    mappingsGrid.setColumnWidth( 1, new Extent( 50, Extent.PERCENT ) );
    mappingsGrid.setColumnWidth( 2, new Extent( 50, Extent.PERCENT ) );
    mappingsTabPane.add( mappingsGrid );

    // update the window
    update();
  }

  /**
   * Update the window.
   */
  public void update()
  {
    // update the configuration file name field
    nameField.setText( configurationFile.getName() );
    // update the configuration file active field
    if ( configurationFile.isActive() )
    {
      activeField.setSelectedIndex( 0 );
    }
    else
    {
      activeField.setSelectedIndex( 1 );
    }
    // update the configuration file blocker field
    if ( configurationFile.isBlocker() )
    {
      blockerField.setSelectedIndex( 0 );
    }
    else
    {
      blockerField.setSelectedIndex( 1 );
    }
    // update the configuration file uri field
    uriField.setText( configurationFile.getUri() );
    // update the configuration file path field
    pathField.setText( configurationFile.getPath() );
    // update the agent field
    List agentList = new LinkedList();
    try
    {
      Kalumet kalumet = ConfigurationManager.loadStore();
      agentList = kalumet.getAgents();
    }
    catch ( Exception e )
    {
      KalumetConsoleApplication.getApplication().getLogPane().addError(
        Messages.getString( "db.read" ) + ": " + e.getMessage(),
        parent.getParentPane().getEnvironmentWindow().getEnvironmentName() );
    }
    DefaultListModel agentListModel = (DefaultListModel) agentField.getModel();
    agentListModel.removeAll();
    for ( Iterator agentIterator = agentList.iterator(); agentIterator.hasNext(); )
    {
      Agent agent = (Agent) agentIterator.next();
      agentListModel.add( agent.getId() );
    }
    agentField.setSelectedItem( configurationFile.getAgent() );
    // remove all mappings grid children
    mappingsGrid.removeAll();
    // add mappings grid header
    Label mappingActionHeader = new Label( " " );
    mappingActionHeader.setStyleName( "grid.header" );
    mappingsGrid.add( mappingActionHeader );
    Label mappingKeyLabel = new Label( Messages.getString( "key" ) );
    mappingKeyLabel.setStyleName( "grid.header" );
    mappingsGrid.add( mappingKeyLabel );
    Label mappingValueLabel = new Label( Messages.getString( "value" ) );
    mappingValueLabel.setStyleName( "grid.header" );
    mappingsGrid.add( mappingValueLabel );
    // add mappings
    for ( Iterator mappingIterator = configurationFile.getMappings().iterator(); mappingIterator.hasNext(); )
    {
      Mapping mapping = (Mapping) mappingIterator.next();
      // row
      Row row = new Row();
      row.setCellSpacing( new Extent( 2 ) );
      row.setInsets( new Insets( 2 ) );
      mappingsGrid.add( row );
      // copy
      Button copyButton = new Button( Styles.PAGE_COPY );
      copyButton.setToolTipText( Messages.getString( "copy" ) );
      copyButton.setActionCommand( mapping.getKey() );
      copyButton.addActionListener( copyMapping );
      row.add( copyButton );
      // delete / edit
      if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().softwareChangePermission)
      {
        // delete
        Button deleteButton = new Button( Styles.DELETE );
        deleteButton.setToolTipText( Messages.getString( "delete" ) );
        deleteButton.setActionCommand( mapping.getKey() );
        deleteButton.addActionListener( deleteMapping );
        row.add( deleteButton );
        // edit
        Button editButton = new Button( Styles.ACCEPT );
        editButton.setToolTipText( Messages.getString( "apply" ) );
        editButton.setActionCommand( mapping.getKey() );
        editButton.addActionListener( editMapping );
        row.add( editButton );
      }
      // mapping key
      TextField mappingKeyField = new TextField();
      mappingKeyField.setStyleName( "default" );
      mappingKeyField.setWidth( new Extent( 100, Extent.PERCENT ) );
      mappingKeyField.setId(
        "softwareconfigurationfilemappingkey_" + parent.getParentPane().getEnvironmentWindow().getEnvironmentName()
          + "_" + parent.getName() + "_" + name + "_" + mapping.getKey() );
      mappingKeyField.setText( mapping.getKey() );
      mappingsGrid.add( mappingKeyField );
      // mapping value
      TextField mappingValueField = new TextField();
      mappingValueField.setStyleName( "default" );
      mappingValueField.setWidth( new Extent( 100, Extent.PERCENT ) );
      mappingValueField.setId(
        "softwareconfigurationfilemappingvalue_" + parent.getParentPane().getEnvironmentWindow().getEnvironmentName()
          + "_" + parent.getName() + "_" + name + "_" + mapping.getKey() );
      mappingValueField.setText( mapping.getValue() );
      mappingsGrid.add( mappingValueField );
    }
    if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().softwareChangePermission)
    {
      // row
      Row row = new Row();
      row.setCellSpacing( new Extent( 2 ) );
      row.setInsets( new Insets( 2 ) );
      mappingsGrid.add( row );
      // paste
      Button pasteButton = new Button( Styles.PAGE_PASTE );
      pasteButton.setToolTipText( Messages.getString( "paste" ) );
      pasteButton.addActionListener( pasteMapping );
      row.add( pasteButton );
      // add
      Button addButton = new Button( Styles.ADD );
      addButton.setToolTipText( Messages.getString( "add" ) );
      addButton.addActionListener( createMapping );
      row.add( addButton );
      // key
      newMappingKeyField = new TextField();
      newMappingKeyField.setStyleName( "default" );
      newMappingKeyField.setWidth( new Extent( 100, Extent.PERCENT ) );
      mappingsGrid.add( newMappingKeyField );
      // value
      newMappingValueField = new TextField();
      newMappingValueField.setStyleName( "default" );
      newMappingValueField.setWidth( new Extent( 100, Extent.PERCENT ) );
      mappingsGrid.add( newMappingValueField );
    }
  }

  public EnvironmentWindow getEnvironmentWindow()
  {
    return parent.getParentPane().getEnvironmentWindow();
  }

}
