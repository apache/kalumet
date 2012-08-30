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
import nextapp.echo2.extras.app.TabPane;
import nextapp.echo2.extras.app.layout.TabPaneLayoutData;
import org.apache.kalumet.model.Mapping;
import org.apache.kalumet.model.SqlScript;

import java.util.Iterator;

/**
 * JEE application database sql script window.
 */
public class ApplicationDatabaseSqlScriptWindow
  extends WindowPane
{

  private String sqlScriptName;

  private SqlScript sqlScript;

  private ApplicationDatabaseWindow parent;

  private TextField nameField;

  private SelectField activeField;

  private SelectField blockerField;

  private SelectField forceField;

  private TextField uriField;

  private Grid mappingsGrid;

  private TextField newMappingKeyField;

  private TextField newMappingValueField;

  // refresh
  private ActionListener refresh = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // looking for the sql script object
      ApplicationDatabaseSqlScriptWindow.this.sqlScript = parent.getDatabase().getSqlScript( sqlScriptName );
      if ( ApplicationDatabaseSqlScriptWindow.this.sqlScript == null )
      {
        ApplicationDatabaseSqlScriptWindow.this.sqlScript = new SqlScript();
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
      ApplicationDatabaseSqlScriptWindow.this.userClose();
    }
  };

  // delete
  private ActionListener delete = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if the user has the environment lock
      if ( !parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().getEnvironment().getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().adminPermission
        && !parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().jeeApplicationsPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // display confirm window
      KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
        new ConfirmWindow( new ActionListener()
        {
          public void actionPerformed( ActionEvent event )
          {
            // delete the sql script
            parent.getDatabase().getSqlScripts().remove( sqlScript );
            // add a change event
            parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().getChangeEvents().add(
              "Delete SQL script " + sqlScript.getName() );
            // change the updated flag
            parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().setUpdated( true );
            // update the journal log tab pane
            parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().updateJournalPane();
            // update the parent pane
            parent.update();
            // close the window
            ApplicationDatabaseSqlScriptWindow.this.userClose();
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
      if ( !parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().getEnvironment().getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().adminPermission
        && !parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().jeeApplicationsPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // get the fields value
      String nameFieldValue = nameField.getText();
      int activeFieldIndex = activeField.getSelectedIndex();
      int blockerFieldIndex = blockerField.getSelectedIndex();
      int forceFieldIndex = forceField.getSelectedIndex();
      String uriFieldValue = uriField.getText();
      // check fields
      if ( nameFieldValue == null || nameFieldValue.trim().length() < 1 || uriFieldValue == null
        || uriFieldValue.trim().length() < 1 )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "sql.script.mandatory" ),
          parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // if the user change the sql script name, check if the new name
      // doesn't already exist
      if ( sqlScriptName == null || ( sqlScriptName != null && !sqlScriptName.equals( nameFieldValue ) ) )
      {
        if ( parent.getDatabase().getSqlScript( nameFieldValue ) != null )
        {
          KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "sql.script.exists" ),
                                                                              parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
          return;
        }
      }
      // add a change event
      if ( sqlScriptName != null )
      {
        parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().getChangeEvents().add(
          "Change SQL script " + sqlScript.getName() );
      }
      // update the sql script object
      sqlScript.setName( nameFieldValue );
      if ( activeFieldIndex == 0 )
      {
        sqlScript.setActive( true );
      }
      else
      {
        sqlScript.setActive( false );
      }
      if ( blockerFieldIndex == 0 )
      {
        sqlScript.setBlocker( true );
      }
      else
      {
        sqlScript.setBlocker( false );
      }
      if ( forceFieldIndex == 0 )
      {
        sqlScript.setForce( true );
      }
      else
      {
        sqlScript.setForce( false );
      }
      sqlScript.setUri( uriFieldValue );
      // add the sql script object if needed
      if ( sqlScriptName == null )
      {
        try
        {
          parent.getDatabase().addSqlScript( sqlScript );
          parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().getChangeEvents().add(
            "Add SQL script " + sqlScript.getName() );
        }
        catch ( Exception e )
        {
          KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "sql.script.exists" ),
                                                                              parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
          return;
        }
      }
      // update the window definition
      setTitle( Messages.getString( "sql.script" ) + " " + sqlScript.getName() );
      setId( "sqlscriptwindow_"
               + parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName()
               + "_" + parent.getParentPane().getParentPane().getServerName() + "_"
               + parent.getParentPane().getParentPane().getApplicationName() + "_" + parent.getDatabaseName() + "_"
               + sqlScript.getName() );
      sqlScriptName = sqlScript.getName();
      // change the updated flag
      parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().setUpdated( true );
      // update the journal log tab pane
      parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().updateJournalPane();
      // update the parent window
      parent.update();
      // update the window
      update();
    }
  };

  // delete mapping
  public ActionListener deleteMapping = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if the user has the environment lock
      if ( !parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().getEnvironment().getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().adminPermission
        && !parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().jeeApplicationsPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // looking for the mapping object
      Mapping mapping = sqlScript.getMapping( event.getActionCommand() );
      if ( mapping == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "mapping.notfound" ),
                                                                            parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // delete the mapping object
      sqlScript.getMappings().remove( mapping );
      // add a change event
      parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().getChangeEvents().add(
        "Delete SQL script " + sqlScript.getName() + " mapping " + mapping.getKey() );
      // change the updated flag
      parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().setUpdated( true );
      // update the journal log tab pane
      parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().updateJournalPane();
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
      if ( !parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().getEnvironment().getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().adminPermission
        && !parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().jeeApplicationsPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // get fields
      TextField mappingKeyField = (TextField) ApplicationDatabaseSqlScriptWindow.this.getComponent( "mappingkey_"
                                                                                                      + parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName()
                                                                                                      + "_"
                                                                                                      + parent.getParentPane().getParentPane().getServerName()
                                                                                                      + "_"
                                                                                                      + parent.getParentPane().getParentPane().getApplicationName()
                                                                                                      + "_"
                                                                                                      + parent.getDatabaseName()
                                                                                                      + "_"
                                                                                                      + sqlScriptName
                                                                                                      + "_"
                                                                                                      + event.getActionCommand() );
      TextField mappingValueField = (TextField) ApplicationDatabaseSqlScriptWindow.this.getComponent( "mappingvalue_"
                                                                                                        + parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName()
                                                                                                        + "_"
                                                                                                        + parent.getParentPane().getParentPane().getServerName()
                                                                                                        + "_"
                                                                                                        + parent.getParentPane().getParentPane().getApplicationName()
                                                                                                        + "_"
                                                                                                        + parent.getDatabaseName()
                                                                                                        + "_"
                                                                                                        + sqlScriptName
                                                                                                        + "_"
                                                                                                        + event.getActionCommand() );
      // get fields value
      String mappingKeyFieldValue = mappingKeyField.getText();
      String mappingValueFieldValue = mappingValueField.getText();
      // check fields
      if ( mappingKeyFieldValue == null || mappingKeyFieldValue.trim().length() < 1 )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "mapping.mandatory" ),
                                                                            parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // if the user change the mapping key, check if the key doesn't already
      // exists
      if ( !mappingKeyFieldValue.equals( event.getActionCommand() ) )
      {
        if ( sqlScript.getMapping( mappingKeyFieldValue ) != null )
        {
          KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "mapping.exists" ),
                                                                              parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
          return;
        }
      }
      // looking for the mapping object
      Mapping mapping = sqlScript.getMapping( event.getActionCommand() );
      if ( mapping == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "mapping.notfound" ),
                                                                            parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // add change event
      parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().getChangeEvents().add(
        "Change SQL script " + sqlScript.getName() + " mapping " + mapping.getKey() );
      // update the mapping
      mapping.setKey( mappingKeyFieldValue );
      mapping.setValue( mappingValueFieldValue );
      // change the updated flag
      parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().setUpdated( true );
      // update the journal log tab pane
      parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().updateJournalPane();
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
      if ( !parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().getEnvironment().getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().adminPermission
        && !parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().jeeApplicationsPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // get fields value
      String newMappingKeyFieldValue = newMappingKeyField.getText();
      String newMappingValueFieldValue = newMappingValueField.getText();
      // check fields
      if ( newMappingKeyFieldValue == null || newMappingKeyFieldValue.trim().length() < 1 )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "mapping.mandatory" ),
                                                                            parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // create the mapping object
      Mapping mapping = new Mapping();
      mapping.setKey( newMappingKeyFieldValue );
      mapping.setValue( newMappingValueFieldValue );
      try
      {
        sqlScript.addMapping( mapping );
      }
      catch ( Exception e )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "mapping.exists" ),
                                                                            parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // add a change event
      parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().getChangeEvents().add(
        "Add SQL script " + sqlScript.getName() + " mapping " + mapping.getKey() );
      // change the updated flag
      parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().setUpdated( true );
      // update the journal log tab pane
      parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().updateJournalPane();
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
        KalumetConsoleApplication.getApplication().setCopyComponent( sqlScript.clone() );
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
      if ( copy == null || !( copy instanceof SqlScript ) )
      {
        return;
      }
      sqlScript = (SqlScript) copy;
      sqlScriptName = null;
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
      Mapping mapping = sqlScript.getMapping( event.getActionCommand() );
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
      // update new field
      newMappingKeyField.setText( ( (Mapping) copy ).getKey() );
      newMappingValueField.setText( ( (Mapping) copy ).getValue() );
    }
  };

  /**
   * Create a new <code>ApplicationDatabaseSqlScriptWindow</code>.
   *
   * @param parent        the parent <code>ApplicationDatabaseWindow</code>.
   * @param sqlScriptName the original <code>SqlScript</code> name.
   */
  public ApplicationDatabaseSqlScriptWindow( ApplicationDatabaseWindow parent, String sqlScriptName )
  {
    super();

    // update the parent pane
    this.parent = parent;

    // update the sql script name
    this.sqlScriptName = sqlScriptName;

    // update the sql script object from the parent pane
    this.sqlScript = parent.getDatabase().getSqlScript( sqlScriptName );
    if ( this.sqlScript == null )
    {
      this.sqlScript = new SqlScript();
    }

    if ( sqlScriptName == null )
    {
      setTitle( Messages.getString( "sql.script" ) );
    }
    else
    {
      setTitle( Messages.getString( "sql.script" ) + " " + sqlScriptName );
    }
    setId( "sqlscriptwindow_"
             + parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() + "_"
             + parent.getParentPane().getParentPane().getServerName() + "_"
             + parent.getParentPane().getParentPane().getApplicationName() + "_" + parent.getDatabaseName() + "_"
             + sqlScriptName );
    setStyleName( "default" );
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
    if ( parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().adminPermission
      || parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().jeeApplicationsPermission )
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
    activeField.setSelectedIndex( 0 );
    activeField.setStyleName( "default" );
    activeField.setWidth( new Extent( 10, Extent.EX ) );
    generalLayoutGrid.add( activeField );
    Label blockerLabel = new Label( Messages.getString( "blocker" ) );
    blockerLabel.setStyleName( "grid.cell" );
    generalLayoutGrid.add( blockerLabel );
    blockerField = new SelectField( MainScreen.LABELS );
    blockerField.setSelectedIndex( 0 );
    blockerField.setStyleName( "default" );
    blockerField.setWidth( new Extent( 10, Extent.EX ) );
    generalLayoutGrid.add( blockerField );
    Label forceLabel = new Label( Messages.getString( "force" ) );
    forceLabel.setStyleName( "grid.cell" );
    generalLayoutGrid.add( forceLabel );
    forceField = new SelectField( MainScreen.LABELS );
    forceField.setSelectedIndex( 0 );
    forceField.setStyleName( "default" );
    forceField.setWidth( new Extent( 10, Extent.EX ) );
    generalLayoutGrid.add( forceField );
    Label uriLabel = new Label( Messages.getString( "uri" ) );
    uriLabel.setStyleName( "grid.cell" );
    generalLayoutGrid.add( uriLabel );
    uriField = new TextField();
    uriField.setStyleName( "default" );
    uriField.setWidth( new Extent( 100, Extent.PERCENT ) );
    generalLayoutGrid.add( uriField );

    // add the mappings tab
    tabLayoutData = new TabPaneLayoutData();
    tabLayoutData.setTitle( Messages.getString( "mappings" ) );
    ContentPane mappingsTabPane = new ContentPane();
    mappingsTabPane.setStyleName( "tab.content" );
    mappingsTabPane.setLayoutData( tabLayoutData );
    tabPane.add( mappingsTabPane );
    mappingsGrid = new Grid( 3 );
    mappingsGrid.setStyleName( "grid.border" );
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
    // update the sql script name field
    nameField.setText( sqlScript.getName() );
    // update the sql script active field
    if ( sqlScript.isActive() )
    {
      activeField.setSelectedIndex( 0 );
    }
    else
    {
      activeField.setSelectedIndex( 1 );
    }
    // update the sql script blocker field
    if ( sqlScript.isBlocker() )
    {
      blockerField.setSelectedIndex( 0 );
    }
    else
    {
      blockerField.setSelectedIndex( 1 );
    }
    // update the sql script force field
    if ( sqlScript.isForce() )
    {
      forceField.setSelectedIndex( 0 );
    }
    else
    {
      forceField.setSelectedIndex( 1 );
    }
    // update the sql script uri field
    uriField.setText( sqlScript.getUri() );

    // remove all mappings grid children
    mappingsGrid.removeAll();
    // add mappings grid header
    Label mappingActionHeader = new Label( " " );
    mappingActionHeader.setStyleName( "grid.header" );
    mappingsGrid.add( mappingActionHeader );
    Label mappingKeyHeader = new Label( Messages.getString( "key" ) );
    mappingKeyHeader.setStyleName( "grid.header" );
    mappingsGrid.add( mappingKeyHeader );
    Label mappingValueHeader = new Label( Messages.getString( "value" ) );
    mappingValueHeader.setStyleName( "grid.header" );
    mappingsGrid.add( mappingValueHeader );
    // add mapping
    for ( Iterator mappingIterator = sqlScript.getMappings().iterator(); mappingIterator.hasNext(); )
    {
      Mapping mapping = (Mapping) mappingIterator.next();
      // row
      Row row = new Row();
      row.setCellSpacing( new Extent( 2 ) );
      row.setInsets( new Insets( 2 ) );
      mappingsGrid.add( row );
      // mapping copy
      Button copyButton = new Button( Styles.PAGE_COPY );
      copyButton.setToolTipText( Messages.getString( "copy" ) );
      copyButton.setActionCommand( mapping.getKey() );
      copyButton.addActionListener( copyMapping );
      row.add( copyButton );
      // mapping delete / edit
      if ( parent.getEnvironmentWindow().adminPermission || parent.getEnvironmentWindow().jeeApplicationsPermission )
      {
        // mapping delete
        Button deleteButton = new Button( Styles.DELETE );
        deleteButton.setToolTipText( Messages.getString( "delete" ) );
        deleteButton.setActionCommand( mapping.getKey() );
        deleteButton.addActionListener( deleteMapping );
        row.add( deleteButton );
        // mapping apply
        Button applyButton = new Button( Styles.ACCEPT );
        applyButton.setToolTipText( Messages.getString( "apply" ) );
        applyButton.setActionCommand( mapping.getKey() );
        applyButton.addActionListener( editMapping );
        row.add( applyButton );
      }
      // mapping key
      TextField mappingKeyField = new TextField();
      mappingKeyField.setStyleName( "default" );
      mappingKeyField.setWidth( new Extent( 100, Extent.PERCENT ) );
      mappingKeyField.setId( "mappingkey_"
                               + parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName()
                               + "_" + parent.getParentPane().getParentPane().getServerName() + "_"
                               + parent.getParentPane().getParentPane().getApplicationName() + "_"
                               + parent.getDatabaseName() + "_" + sqlScriptName + "_" + mapping.getKey() );
      mappingKeyField.setText( mapping.getKey() );
      mappingsGrid.add( mappingKeyField );
      // mapping value
      TextField mappingValueField = new TextField();
      mappingValueField.setStyleName( "default" );
      mappingValueField.setWidth( new Extent( 100, Extent.PERCENT ) );
      mappingValueField.setId( "mappingvalue_"
                                 + parent.getParentPane().getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName()
                                 + "_" + parent.getParentPane().getParentPane().getServerName() + "_"
                                 + parent.getParentPane().getParentPane().getApplicationName() + "_"
                                 + parent.getDatabaseName() + "_" + sqlScriptName + "_" + mapping.getKey() );
      mappingValueField.setText( mapping.getValue() );
      mappingsGrid.add( mappingValueField );
    }
    // add a new mapping
    if ( parent.getEnvironmentWindow().adminPermission || parent.getEnvironmentWindow().jeeApplicationsPermission )
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
      // new mapping key
      newMappingKeyField = new TextField();
      newMappingKeyField.setStyleName( "default" );
      newMappingKeyField.setWidth( new Extent( 100, Extent.PERCENT ) );
      mappingsGrid.add( newMappingKeyField );
      // new mapping value
      newMappingValueField = new TextField();
      newMappingValueField.setStyleName( "default" );
      newMappingValueField.setWidth( new Extent( 100, Extent.PERCENT ) );
      mappingsGrid.add( newMappingValueField );
    }
  }

}
