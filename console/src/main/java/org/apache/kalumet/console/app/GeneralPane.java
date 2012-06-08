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
import nextapp.echo2.app.TextArea;
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.app.list.DefaultListModel;
import nextapp.echo2.extras.app.TabPane;
import nextapp.echo2.extras.app.layout.TabPaneLayoutData;
import org.apache.kalumet.console.configuration.ConfigurationManager;
import org.apache.kalumet.ws.client.AgentClient;

import java.util.Iterator;

/**
 * Environment general pane.
 */
public class GeneralPane
  extends TabPane
{

  private EnvironmentWindow parent;

  private TextField nameField;

  private TextField groupField;

  private SelectField agentField;

  private SelectField tagField;

  private SelectField autoUpdateField;

  private TextArea notesArea;

  private TextArea weblinksArea;

  private Grid freeFieldsGrid;

  private Grid variablesGrid;

  private Grid logFilesGrid;

  private TextField newFreeFieldNameField;

  private TextField newFreeFieldContentField;

  private TextField newVariableName;

  private TextField newVariableValue;

  private TextField newLogFileName;

  private TextField newLogFilePath;

  private SelectField newLogFileAgent;

  // agent status thread
  class AgentStatusThread
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
        Agent agent = kalumet.getAgent( (String) agentField.getSelectedItem() );
        if ( agent == null )
        {
          throw new IllegalArgumentException( "agent not found." );
        }
        // call the WebService
        AgentClient client = new AgentClient( agent.getHostname(), agent.getPort() );
        message = "Agent " + agent.getId() + " version " + client.getVersion() + " started.";
      }
      catch ( Exception e )
      {
        failure = true;
        message = "Agent status check failed: " + e.getMessage();
      }
      finally
      {
        ended = true;
      }
    }

  }

  // edit free field
  private ActionListener editFreeField = new ActionListener()
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
      if ( !getEnvironmentWindow().adminPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // edit free field
      // get the free field name
      String freeFieldName = event.getActionCommand();
      // get the free field fields
      String newFreeFieldNameValue = ( (TextField) GeneralPane.this.getComponent(
        "ffname_" + parent.getEnvironmentName() + "_" + freeFieldName ) ).getText();
      String newFreeFieldContentValue = ( (TextField) GeneralPane.this.getComponent(
        "ffcontent_" + parent.getEnvironmentName() + "_" + freeFieldName ) ).getText();
      // check if the mandatory fields are presents
      if ( newFreeFieldNameValue == null || newFreeFieldNameValue.trim().length() < 1
        || newFreeFieldContentValue == null || newFreeFieldContentValue.trim().length() < 1 )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "freefield.mandatory" ) );
        return;
      }
      // check if the user try to change the free field name
      if ( !freeFieldName.equals( newFreeFieldNameValue ) )
      {
        // if this case, check if the free field name is already in used
        if ( parent.getEnvironment().getFreeField( newFreeFieldNameValue ) != null )
        {
          KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "freefield.exists" ),
                                                                              getEnvironmentWindow().getEnvironmentName() );
          return;
        }
      }
      // get the freefield object in the parent environment
      FreeField freeField = parent.getEnvironment().getFreeField( freeFieldName );
      if ( freeField == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "freefield.notfound" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // add a change event
      parent.getChangeEvents().add( "Change free field " + freeField.getName() + " / " + freeField.getContent() );
      // update the free field object
      freeField.setName( newFreeFieldNameValue );
      freeField.setContent( newFreeFieldContentValue );
      // change the updated flag
      parent.setUpdated( true );
      // update the journal log tab pane
      parent.updateJournalPane();
      // update only the pane
      update();
    }
  };

  // create free field
  private ActionListener createFreeField = new ActionListener()
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
      if ( !getEnvironmentWindow().adminPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // get the free field value
      String newFreeFieldNameValue = newFreeFieldNameField.getText();
      String newFreeFieldContentValue = newFreeFieldContentField.getText();
      // check if the mandatory fields are presents
      if ( newFreeFieldNameValue == null || newFreeFieldNameValue.trim().length() < 1
        || newFreeFieldContentValue == null || newFreeFieldContentValue.trim().length() < 1 )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "freefield.mandatory" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // create the new free field
      FreeField freeField = new FreeField();
      freeField.setName( newFreeFieldNameValue );
      freeField.setContent( newFreeFieldContentValue );
      // add the new free field
      try
      {
        parent.getEnvironment().addFreeField( freeField );
      }
      catch ( Exception e )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "freefield.exists" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // add a change event
      parent.getChangeEvents().add( "Add free field " + freeField.getName() + " / " + freeField.getContent() );
      // change the updated flag
      parent.setUpdated( true );
      // update the journal log tab pane
      parent.updateJournalPane();
      // update only the pane
      update();
    }
  };

  // delete free field
  private ActionListener deleteFreeField = new ActionListener()
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
      if ( !getEnvironmentWindow().adminPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      String freeFieldName = event.getActionCommand();
      // looking for the free field object
      FreeField freeField = parent.getEnvironment().getFreeField( freeFieldName );
      if ( freeField == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "environment.notfound" ), getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      parent.getEnvironment().getFreeFields().remove( freeField );
      // add a change event
      parent.getChangeEvents().add( "Delete free field " + freeField.getName() );
      // change the updated flag
      parent.setUpdated( true );
      // update the journal log tab pane
      parent.updateJournalPane();
      // update only the pane
      update();
    }
  };

  // copy free field
  private ActionListener copyFreeField = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // looking for the free field object
      FreeField freeField = parent.getEnvironment().getFreeField( event.getActionCommand() );
      if ( freeField == null )
      {
        return;
      }
      try
      {
        // put the free field clone in the copy component
        KalumetConsoleApplication.getApplication().setCopyComponent( freeField.clone() );
      }
      catch ( Exception e )
      {
        return;
      }
    }
  };

  // paste free field
  private ActionListener pasteFreeField = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if the copy component is correct
      Object copy = KalumetConsoleApplication.getApplication().getCopyComponent();
      if ( copy == null || !( copy instanceof FreeField ) )
      {
        return;
      }
      // update new field fields with the clone
      newFreeFieldNameField.setText( ( (FreeField) copy ).getName() );
      newFreeFieldContentField.setText( ( (FreeField) copy ).getContent() );
    }
  };

  // up free field
  private ActionListener upFreeField = new ActionListener()
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
      if ( !getEnvironmentWindow().adminPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // looking for the freefield object
      FreeField freefield = parent.getEnvironment().getFreeField( event.getActionCommand() );
      if ( freefield == null )
      {
        return;
      }
      // get the freefield index
      int index = parent.getEnvironment().getFreeFields().indexOf( freefield );
      // if the index is the first one, or the freefield is not found
      // or the freefields list doesn't contain at least two elements
      // do nothing
      if ( index == 0 || index == -1 || parent.getEnvironment().getFreeFields().size() < 2 )
      {
        return;
      }
      // get the previous freefield
      FreeField previous = (FreeField) parent.getEnvironment().getFreeFields().get( index - 1 );
      // switch the freefields
      parent.getEnvironment().getFreeFields().set( index, previous );
      parent.getEnvironment().getFreeFields().set( index - 1, freefield );
      // update the pane
      update();
    }
  };

  // down free field
  private ActionListener downFreeField = new ActionListener()
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
      if ( !getEnvironmentWindow().adminPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // looking for the freefield object
      FreeField freefield = parent.getEnvironment().getFreeField( event.getActionCommand() );
      if ( freefield == null )
      {
        return;
      }
      // get the freefield index
      int index = parent.getEnvironment().getFreeFields().indexOf( freefield );
      // if the index is the last one, or the freefield is not found
      // or the freefields list doesn't contain at least two elements
      // do nothing
      if ( index == -1 || index == parent.getEnvironment().getFreeFields().size() - 1
        || parent.getEnvironment().getFreeFields().size() < 2 )
      {
        return;
      }
      // get the next freefield
      FreeField next = (FreeField) parent.getEnvironment().getFreeFields().get( index + 1 );
      // switch the freefields
      parent.getEnvironment().getFreeFields().set( index + 1, freefield );
      parent.getEnvironment().getFreeFields().set( index, next );
      // update the pane
      update();
    }
  };

  // copy variable
  private ActionListener copyVariable = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // looking for the variable object
      Variable variable = parent.getEnvironment().getVariable( event.getActionCommand() );
      if ( variable == null )
      {
        return;
      }
      try
      {
        // put the variable clone in the copy component
        KalumetConsoleApplication.getApplication().setCopyComponent( variable.clone() );
      }
      catch ( Exception e )
      {
        return;
      }
    }
  };

  // paste variable
  private ActionListener pasteVariable = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if the copy component is correct
      Object copy = KalumetConsoleApplication.getApplication().getCopyComponent();
      if ( copy == null || !( copy instanceof Variable ) )
      {
        return;
      }
      // update new variable fields with the clone
      newVariableName.setText( ( (Variable) copy ).getName() );
      newVariableValue.setText( ( (Variable) copy ).getValue() );
    }
  };

  // edit variable
  private ActionListener editVariable = new ActionListener()
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
      if ( !getEnvironmentWindow().adminPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // edit variable
      // get the variable name
      String variableName = event.getActionCommand();
      // get the variable fields
      String newVariableNameValue = ( (TextField) GeneralPane.this.getComponent(
        "variablename_" + parent.getEnvironmentName() + "_" + variableName ) ).getText();
      String newVariableValueValue = ( (TextField) GeneralPane.this.getComponent(
        "variablevalue_" + parent.getEnvironmentName() + "_" + variableName ) ).getText();
      // check if the mandatory fields are presents
      if ( newVariableNameValue == null || newVariableNameValue.trim().length() < 1 || newVariableValueValue == null
        || newVariableValueValue.trim().length() < 1 )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "variable.mandatory" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if the user try to change the variable name
      if ( !variableName.equals( newVariableNameValue ) )
      {
        // if this case, check if the variable name is already in used
        if ( parent.getEnvironment().getVariable( newVariableNameValue ) != null )
        {
          KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "variable.exists" ),
                                                                              getEnvironmentWindow().getEnvironmentName() );
          return;
        }
      }
      // get the variable object in the parent environment
      Variable variable = parent.getEnvironment().getVariable( variableName );
      if ( variable == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "variable.notfound" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // add a change event
      parent.getChangeEvents().add( "Change the variable " + variable.getName() + " / " + variable.getValue() );
      // update the variable object
      variable.setName( newVariableNameValue );
      variable.setValue( newVariableValueValue );
      // change the updated flag
      parent.setUpdated( true );
      // update the journal log tab pane
      parent.updateJournalPane();
      // update only the pane
      update();
    }
  };

  // delete variable
  private ActionListener deleteVariable = new ActionListener()
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
      if ( !getEnvironmentWindow().adminPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      String variableName = event.getActionCommand();
      // looking for the variable object
      Variable variable = parent.getEnvironment().getVariable( variableName );
      if ( variable == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "variable.notfound" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      parent.getEnvironment().getVariables().remove( variable );
      // add a change event
      parent.getChangeEvents().add( "Delete variable " + variable.getName() );
      // change the updated flag
      parent.setUpdated( true );
      // update the journal log tab pane
      parent.updateJournalPane();
      // update only the pane
      update();
    }
  };

  // create variable
  private ActionListener createVariable = new ActionListener()
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
      if ( !getEnvironmentWindow().adminPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // get the variable fields value
      String newVariableNameValue = newVariableName.getText();
      String newVariableValueValue = newVariableValue.getText();
      // check if the mandatory fields are presents
      if ( newVariableNameValue == null || newVariableNameValue.trim().length() < 1 || newVariableValueValue == null
        || newVariableValueValue.trim().length() < 1 )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "variable.mandatory" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // create the new variable
      Variable variable = new Variable();
      variable.setName( newVariableNameValue );
      variable.setValue( newVariableValueValue );
      // add the new variable
      try
      {
        parent.getEnvironment().addVariable( variable );
      }
      catch ( Exception e )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "variable.exists" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // add a change event
      parent.getChangeEvents().add( "Add variable " + variable.getName() + " / " + variable.getValue() );
      // change the updated flag
      parent.setUpdated( true );
      // update the journal log tab pane
      parent.updateJournalPane();
      // update only the pane
      update();
    }
  };

  // up variable
  private ActionListener upVariable = new ActionListener()
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
      if ( !getEnvironmentWindow().adminPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // looking for the variable object
      Variable variable = parent.getEnvironment().getVariable( event.getActionCommand() );
      if ( variable == null )
      {
        return;
      }
      // get the variable index
      int index = parent.getEnvironment().getVariables().indexOf( variable );
      // if the index is the first one, or the variable is not found
      // or the variables list doesn't contain at least two elements
      // do nothing
      if ( index == 0 || index == -1 || parent.getEnvironment().getVariables().size() < 2 )
      {
        return;
      }
      // get the previous variable
      Variable previous = (Variable) parent.getEnvironment().getVariables().get( index - 1 );
      // switch the variables
      parent.getEnvironment().getVariables().set( index, previous );
      parent.getEnvironment().getVariables().set( index - 1, variable );
      // update the pane
      update();
    }
  };

  // down variable
  private ActionListener downVariable = new ActionListener()
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
      if ( !getEnvironmentWindow().adminPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // looking for the variable object
      Variable variable = parent.getEnvironment().getVariable( event.getActionCommand() );
      if ( variable == null )
      {
        return;
      }
      // get the variable index
      int index = parent.getEnvironment().getVariables().indexOf( variable );
      // if the index is the last one, or the variable is not found
      // or the variables list doesn't contain at least two elements
      // do nothing
      if ( index == -1 || index == parent.getEnvironment().getVariables().size() - 1
        || parent.getEnvironment().getVariables().size() < 2 )
      {
        return;
      }
      // get the next variable
      Variable next = (Variable) parent.getEnvironment().getVariables().get( index + 1 );
      // switch the variables
      parent.getEnvironment().getVariables().set( index + 1, variable );
      parent.getEnvironment().getVariables().set( index, next );
      // update the pane
      update();
    }
  };

  // copy log file
  private ActionListener copyLogFile = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // looking for the log file object
      LogFile logFile = parent.getEnvironment().getLogFile( event.getActionCommand() );
      if ( logFile == null )
      {
        return;
      }
      try
      {
        KalumetConsoleApplication.getApplication().setCopyComponent( logFile.clone() );
      }
      catch ( Exception e )
      {
        return;
      }
    }
  };

  // delete log file
  private ActionListener deleteLogFile = new ActionListener()
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
      if ( !getEnvironmentWindow().adminPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // looking for the log file object
      LogFile logFile = parent.getEnvironment().getLogFile( event.getActionCommand() );
      if ( logFile == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "logfile.notfound" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // delete the log file
      parent.getEnvironment().getLogFiles().remove( logFile );
      // add a journal event
      parent.getChangeEvents().add( "Delete log file " + logFile.getName() );
      // switch on the updated flag
      parent.setUpdated( true );
      // refresh the journal pane
      parent.updateJournalPane();
      // update this pane
      update();
    }
  };

  // edit log file
  private ActionListener editLogFile = new ActionListener()
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
      if ( !getEnvironmentWindow().adminPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // looking for the log file object
      String logFileName = event.getActionCommand();
      // get the fields value
      String logFileNewName = ( (TextField) GeneralPane.this.getComponent(
        "logfilename_" + parent.getEnvironmentName() + "_" + logFileName ) ).getText();
      String logFileNewPath = ( (TextField) GeneralPane.this.getComponent(
        "logfilepath_" + parent.getEnvironmentName() + "_" + logFileName ) ).getText();
      String logFileNewAgent = (String) ( (SelectField) GeneralPane.this.getComponent(
        "logfileagent_" + parent.getEnvironmentName() + "_" + logFileName ) ).getSelectedItem();
      // check the fiels value
      if ( logFileNewName == null || logFileNewName.trim().length() < 1 || logFileNewPath == null
        || logFileNewPath.trim().length() < 1 )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "logfile.mandatory" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // if the user tries to change the log file name, check if the name is not already used
      if ( !logFileNewName.equals( logFileName ) )
      {
        if ( parent.getEnvironment().getLogFile( logFileNewName ) != null )
        {
          KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "logfile.exists" ),
                                                                              getEnvironmentWindow().getEnvironmentName() );
          return;
        }
      }
      // looking for the log file object
      LogFile logFile = parent.getEnvironment().getLogFile( logFileName );
      if ( logFile == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "logfile.notfound" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // add a change event
      parent.getChangeEvents().add( "Change log file " + logFile.getName() );
      // update the log file object
      logFile.setName( logFileNewName );
      logFile.setPath( logFileNewPath );
      logFile.setAgent( logFileNewAgent );
      // switch on the update flag
      parent.setUpdated( true );
      // update the journal log pane
      parent.updateJournalPane();
      // update this pane
      update();
    }
  };

  // add log file
  private ActionListener addLogFile = new ActionListener()
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
      if ( !getEnvironmentWindow().adminPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // get the provided value
      String newLogFileNameValue = newLogFileName.getText();
      String newLogFilePathValue = newLogFilePath.getText();
      String newLogFileAgentValue = (String) newLogFileAgent.getSelectedItem();
      // check the provided value
      if ( newLogFileNameValue == null || newLogFileNameValue.trim().length() < 1 || newLogFilePathValue == null
        || newLogFilePathValue.trim().length() < 1 )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "logfile.mandatory" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // create a new log file
      LogFile logFile = new LogFile();
      logFile.setName( newLogFileNameValue );
      logFile.setPath( newLogFilePathValue );
      logFile.setAgent( newLogFileAgentValue );
      // add the new log file
      try
      {
        parent.getEnvironment().addLogFile( logFile );
      }
      catch ( Exception e )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "logfile.exists" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // add a change event
      parent.getChangeEvents().add( "Add log file " + logFile.getName() + " / " + logFile.getPath() );
      // switch on the update flag
      parent.setUpdated( true );
      // update the journal log pane
      parent.updateJournalPane();
      // update this pane
      update();
    }
  };

  // paste log file
  private ActionListener pasteLogFile = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if the copied object is correct
      Object copy = KalumetConsoleApplication.getApplication().getCopyComponent();
      if ( copy == null || !( copy instanceof LogFile ) )
      {
        return;
      }
      // update the new fields
      newLogFileName.setText( ( (LogFile) copy ).getName() );
      newLogFilePath.setText( ( (LogFile) copy ).getPath() );
      newLogFileAgent.setSelectedItem( ( (LogFile) copy ).getAgent() );
    }
  };

  // up log file
  private ActionListener upLogFile = new ActionListener()
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
      if ( !getEnvironmentWindow().adminPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // looking for the logfile object
      LogFile logFile = getEnvironmentWindow().getEnvironment().getLogFile( event.getActionCommand() );
      if ( logFile == null )
      {
        return;
      }
      // get the logfile index
      int index = getEnvironmentWindow().getEnvironment().getLogFiles().indexOf( logFile );
      // if the index is the first one, or the log file is not found
      // or the logfiles list doesn't contain at least two elements
      // do nothing
      if ( index == 0 || index == -1 || getEnvironmentWindow().getEnvironment().getLogFiles().size() < 2 )
      {
        return;
      }
      // get the previous logfile
      LogFile previous = (LogFile) getEnvironmentWindow().getEnvironment().getLogFiles().get( index - 1 );
      // switch the logfiles
      getEnvironmentWindow().getEnvironment().getLogFiles().set( index - 1, logFile );
      getEnvironmentWindow().getEnvironment().getLogFiles().set( index, previous );
      // update the pane
      update();
    }
  };

  // down log file
  private ActionListener downLogFile = new ActionListener()
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
      if ( !getEnvironmentWindow().adminPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // looking for the logfile object
      LogFile logFile = getEnvironmentWindow().getEnvironment().getLogFile( event.getActionCommand() );
      if ( logFile == null )
      {
        return;
      }
      // get the logfile index
      int index = getEnvironmentWindow().getEnvironment().getLogFiles().indexOf( logFile );
      // if the index is the last one, or the logfile is not found
      // or the logfiles list doesn't contain at least two elements
      // do nothing
      if ( index == -1 || index == getEnvironmentWindow().getEnvironment().getLogFiles().size() - 1
        || getEnvironmentWindow().getEnvironment().getLogFiles().size() < 2 )
      {
        return;
      }
      // get the next logfile
      LogFile next = (LogFile) getEnvironmentWindow().getEnvironment().getLogFiles().get( index + 1 );
      // switch the logfiles
      getEnvironmentWindow().getEnvironment().getLogFiles().set( index + 1, logFile );
      getEnvironmentWindow().getEnvironment().getLogFiles().set( index, next );
      // update the pane
      update();
    }
  };

  // agent status
  private ActionListener agentStatus = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // add an event
      KalumetConsoleApplication.getApplication().getLogPane().addInfo( "Agent check in progress ...",
                                                                       parent.getEnvironmentName() );
      // start the agent status thread
      final AgentStatusThread agentStatusThread = new AgentStatusThread();
      agentStatusThread.start();
      // sync with the client
      KalumetConsoleApplication.getApplication().enqueueTask( KalumetConsoleApplication.getApplication().getTaskQueue(),
                                                              new Runnable()
                                                              {
                                                                public void run()
                                                                {
                                                                  if ( agentStatusThread.ended )
                                                                  {
                                                                    if ( agentStatusThread.failure )
                                                                    {
                                                                      KalumetConsoleApplication.getApplication().getLogPane().addError(
                                                                        agentStatusThread.message,
                                                                        parent.getEnvironmentName() );
                                                                    }
                                                                    else
                                                                    {
                                                                      KalumetConsoleApplication.getApplication().getLogPane().addConfirm(
                                                                        agentStatusThread.message,
                                                                        parent.getEnvironmentName() );
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

  // view log file
  private ActionListener viewLogFile = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      String logFileName = event.getActionCommand();
      // looking for the log file
      LogFile logFile = parent.getEnvironment().getLogFile( logFileName );
      if ( logFile == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addError( Messages.getString( "logfile.notfound" ),
                                                                          parent.getEnvironmentName() );
        return;
      }
      // define which agent to use
      String agentId;
      if ( logFile.getAgent() != null && logFile.getAgent().trim().length() > 0 )
      {
        agentId = logFile.getAgent();
      }
      else
      {
        agentId = parent.getEnvironment().getAgent();
      }
      // open a view file window
      KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
        new ViewFileWindow( logFile.getPath(), agentId ) );
    }
  };

  /**
   * Create a new <code>GeneralPane</code>.
   *
   * @param parent the parent <code>EnvironmentWindow</code>.
   */
  public GeneralPane( EnvironmentWindow parent )
  {
    super();
    setStyleName( "default" );

    // update parent
    this.parent = parent;

    // add the information tab pane
    TabPaneLayoutData tabLayoutData = new TabPaneLayoutData();
    tabLayoutData.setTitle( Messages.getString( "information" ) );
    ContentPane informationPane = new ContentPane();
    informationPane.setStyleName( "tab.content" );
    informationPane.setLayoutData( tabLayoutData );
    add( informationPane );

    // info grid
    Grid grid = new Grid( 2 );
    grid.setStyleName( "default" );
    grid.setWidth( new Extent( 100, Extent.PERCENT ) );
    grid.setColumnWidth( 0, new Extent( 10, Extent.PERCENT ) );
    grid.setColumnWidth( 1, new Extent( 90, Extent.PERCENT ) );
    informationPane.add( grid );

    // add environment name field
    Label nameLabel = new Label( Messages.getString( "name" ) );
    nameLabel.setStyleName( "grid.cell" );
    grid.add( nameLabel );
    nameField = new TextField();
    nameField.setStyleName( "default" );
    nameField.setWidth( new Extent( 100, Extent.PERCENT ) );
    grid.add( nameField );

    // add environment group field
    Label groupLabel = new Label( Messages.getString( "group" ) );
    groupLabel.setStyleName( "grid.cell" );
    grid.add( groupLabel );
    groupField = new TextField();
    groupField.setStyleName( "default" );
    groupField.setWidth( new Extent( 100, Extent.PERCENT ) );
    grid.add( groupField );

    // add the tag select field
    Label tagLabel = new Label( Messages.getString( "tag" ) );
    tagLabel.setStyleName( "grid.cell" );
    grid.add( tagLabel );
    Object[] tags = new Object[]{ Messages.getString( "production" ), Messages.getString( "preproduction" ),
      Messages.getString( "staging" ), Messages.getString( "testing" ), Messages.getString( "unstable" ),
      Messages.getString( "other" ) };
    tagField = new SelectField( tags );
    tagField.setStyleName( "default" );
    tagField.setWidth( new Extent( 50, Extent.EX ) );
    grid.add( tagField );

    // add the agent select field
    Label agentLabel = new Label( Messages.getString( "agent" ) );
    agentLabel.setStyleName( "grid.cell" );
    grid.add( agentLabel );
    Row agentRow = new Row();
    agentRow.setCellSpacing( new Extent( 2 ) );
    grid.add( agentRow );
    agentField = new SelectField();
    agentField.setStyleName( "default" );
    agentField.setWidth( new Extent( 50, Extent.EX ) );
    agentRow.add( agentField );
    Button agentButton = new Button( Styles.INFORMATION );
    agentButton.setToolTipText( Messages.getString( "status" ) );
    agentButton.addActionListener( agentStatus );
    agentRow.add( agentButton );

    // add the auto update select field
    Label autoUpdateLabel = new Label( Messages.getString( "autoupdate" ) );
    autoUpdateLabel.setStyleName( "grid.cell" );
    grid.add( autoUpdateLabel );
    autoUpdateField = new SelectField( MainScreen.LABELS );
    autoUpdateField.setStyleName( "default" );
    autoUpdateField.setWidth( new Extent( 10, Extent.EX ) );
    grid.add( autoUpdateField );

    // add the notes area
    Label environmentNotesLabel = new Label( Messages.getString( "notes" ) );
    environmentNotesLabel.setStyleName( "grid.cell" );
    grid.add( environmentNotesLabel );
    notesArea = new TextArea();
    notesArea.setStyleName( "default" );
    notesArea.setWidth( new Extent( 100, Extent.PERCENT ) );
    notesArea.setHeight( new Extent( 200, Extent.PX ) );
    grid.add( notesArea );

    // add the weblinks area
    Label environmentWeblinksLabel = new Label( Messages.getString( "weblinks" ) );
    environmentWeblinksLabel.setStyleName( "grid.cell" );
    grid.add( environmentWeblinksLabel );
    weblinksArea = new TextArea();
    weblinksArea.setStyleName( "default" );
    weblinksArea.setWidth( new Extent( 100, Extent.PERCENT ) );
    weblinksArea.setHeight( new Extent( 200, Extent.PX ) );
    grid.add( weblinksArea );

    // free fields tab
    tabLayoutData = new TabPaneLayoutData();
    tabLayoutData.setTitle( Messages.getString( "freefields" ) );
    ContentPane freeFieldPane = new ContentPane();
    freeFieldPane.setStyleName( "tab.content" );
    freeFieldPane.setLayoutData( tabLayoutData );
    add( freeFieldPane );

    // free fields grid
    freeFieldsGrid = new Grid( 3 );
    freeFieldsGrid.setStyleName( "border.grid" );
    freeFieldsGrid.setColumnWidth( 0, new Extent( 50, Extent.PX ) );
    freeFieldsGrid.setColumnWidth( 1, new Extent( 50, Extent.PERCENT ) );
    freeFieldsGrid.setColumnWidth( 2, new Extent( 50, Extent.PERCENT ) );
    freeFieldPane.add( freeFieldsGrid );

    // add the variable tab
    tabLayoutData = new TabPaneLayoutData();
    tabLayoutData.setTitle( Messages.getString( "variables" ) );
    ContentPane variablePane = new ContentPane();
    variablePane.setStyleName( "tab.content" );
    variablePane.setLayoutData( tabLayoutData );
    add( variablePane );

    variablesGrid = new Grid( 3 );
    variablesGrid.setStyleName( "border.grid" );
    variablesGrid.setColumnWidth( 0, new Extent( 50, Extent.PX ) );
    variablesGrid.setColumnWidth( 1, new Extent( 50, Extent.PERCENT ) );
    variablesGrid.setColumnWidth( 2, new Extent( 50, Extent.PERCENT ) );
    variablePane.add( variablesGrid );

    // add the log files tab
    tabLayoutData = new TabPaneLayoutData();
    tabLayoutData.setTitle( Messages.getString( "logfiles" ) );
    ContentPane logFilePane = new ContentPane();
    logFilePane.setStyleName( "tab.content" );
    logFilePane.setLayoutData( tabLayoutData );
    add( logFilePane );

    logFilesGrid = new Grid( 4 );
    logFilesGrid.setStyleName( "border.grid" );
    logFilesGrid.setColumnWidth( 0, new Extent( 50, Extent.PX ) );
    logFilesGrid.setColumnWidth( 1, new Extent( 33, Extent.PERCENT ) );
    logFilesGrid.setColumnWidth( 2, new Extent( 33, Extent.PERCENT ) );
    logFilesGrid.setColumnWidth( 3, new Extent( 33, Extent.PERCENT ) );
    logFilePane.add( logFilesGrid );

    // update field
    update();
  }

  /**
   * Update fields/grids
   */
  protected void update()
  {
    // set environment name
    nameField.setText( parent.getEnvironment().getName() );
    // set environment group
    groupField.setText( parent.getEnvironment().getGroup() );
    // load Kalumet configuration
    Kalumet kalumet = null;
    try
    {
      kalumet = ConfigurationManager.loadStore();
    }
    catch ( Exception e )
    {
      KalumetConsoleApplication.getApplication().getLogPane().addError(
        Messages.getString( "db.read" ) + ": " + e.getMessage() );
      return;
    }
    // select field model
    DefaultListModel agentModelList = (DefaultListModel) agentField.getModel();
    agentModelList.removeAll();
    agentModelList.add( "" );
    for ( Iterator agentIterator = kalumet.getAgents().iterator(); agentIterator.hasNext(); )
    {
      Agent agent = (Agent) agentIterator.next();
      agentModelList.add( agent.getId() );
    }
    agentField.setSelectedItem( parent.getEnvironment().getAgent() );
    // tag
    if ( parent.getEnvironment().getTag() == null )
    {
      tagField.setSelectedIndex( 5 );
    }
    else
    {
      if ( parent.getEnvironment().getTag().equals( Messages.getString( "production" ) ) )
      {
        tagField.setSelectedIndex( 0 );
      }
      else if ( parent.getEnvironment().getTag().equals( Messages.getString( "preproduction" ) ) )
      {
        tagField.setSelectedIndex( 1 );
      }
      else if ( parent.getEnvironment().getTag().equals( Messages.getString( "staging" ) ) )
      {
        tagField.setSelectedIndex( 2 );
      }
      else if ( parent.getEnvironment().getTag().equals( Messages.getString( "testing" ) ) )
      {
        tagField.setSelectedIndex( 3 );
      }
      else if ( parent.getEnvironment().getTag().equals( Messages.getString( "unstable" ) ) )
      {
        tagField.setSelectedIndex( 4 );
      }
      else
      {
        tagField.setSelectedIndex( 5 );
      }
    }
    // auto update flag
    if ( parent.getEnvironment().isAutoupdate() )
    {
      autoUpdateField.setSelectedIndex( 0 );
    }
    else
    {
      autoUpdateField.setSelectedIndex( 1 );
    }
    // update the environment notes area
    notesArea.setText( parent.getEnvironment().getNotes() );
    // update the environment weblinks area
    weblinksArea.setText( parent.getEnvironment().getWeblinks() );

    // update the free fields grid
    // remove all grid children
    freeFieldsGrid.removeAll();
    // add grid headers
    Label freeFieldActionsHeader = new Label( " " );
    freeFieldActionsHeader.setStyleName( "grid.header" );
    freeFieldsGrid.add( freeFieldActionsHeader );
    Label freeFieldNameHeader = new Label( Messages.getString( "name" ) );
    freeFieldNameHeader.setStyleName( "grid.header" );
    freeFieldsGrid.add( freeFieldNameHeader );
    Label freeFieldsContentHeader = new Label( Messages.getString( "content" ) );
    freeFieldsContentHeader.setStyleName( "grid.header" );
    freeFieldsGrid.add( freeFieldsContentHeader );
    // add free fields
    for ( Iterator freeFieldIterator = parent.getEnvironment().getFreeFields().iterator();
          freeFieldIterator.hasNext(); )
    {
      FreeField current = (FreeField) freeFieldIterator.next();
      // row
      Row row = new Row();
      row.setInsets( new Insets( 2 ) );
      row.setCellSpacing( new Extent( 2 ) );
      freeFieldsGrid.add( row );
      // free field copy
      Button copyButton = new Button( Styles.PAGE_COPY );
      copyButton.setToolTipText( Messages.getString( "copy" ) );
      copyButton.setActionCommand( current.getName() );
      copyButton.addActionListener( copyFreeField );
      row.add( copyButton );
      if ( getEnvironmentWindow().adminPermission )
      {
        // up
        Button upButton = new Button( Styles.ARROW_UP );
        upButton.setToolTipText( Messages.getString( "up" ) );
        upButton.setActionCommand( current.getName() );
        upButton.addActionListener( upFreeField );
        row.add( upButton );
        // down
        Button downButton = new Button( Styles.ARROW_DOWN );
        downButton.setToolTipText( Messages.getString( "down" ) );
        downButton.setActionCommand( current.getName() );
        downButton.addActionListener( downFreeField );
        row.add( downButton );
        // edit
        Button editButton = new Button( Styles.ACCEPT );
        editButton.setToolTipText( Messages.getString( "apply" ) );
        editButton.setActionCommand( current.getName() );
        editButton.addActionListener( editFreeField );
        row.add( editButton );
        // delete
        Button deleteButton = new Button( Styles.DELETE );
        deleteButton.setToolTipText( Messages.getString( "delete" ) );
        deleteButton.setActionCommand( current.getName() );
        deleteButton.addActionListener( deleteFreeField );
        row.add( deleteButton );
      }
      // free field name
      TextField freeFieldName = new TextField();
      freeFieldName.setStyleName( "default" );
      freeFieldName.setWidth( new Extent( 100, Extent.PERCENT ) );
      freeFieldName.setText( current.getName() );
      freeFieldName.setId( "ffname_" + parent.getEnvironment().getName() + "_" + current.getName() );
      freeFieldsGrid.add( freeFieldName );
      // free field content
      TextField freeFieldContent = new TextField();
      freeFieldContent.setStyleName( "default" );
      freeFieldContent.setWidth( new Extent( 100, Extent.PERCENT ) );
      freeFieldContent.setText( current.getContent() );
      freeFieldContent.setId( "ffcontent_" + parent.getEnvironment().getName() + "_" + current.getName() );
      freeFieldsGrid.add( freeFieldContent );
    }
    // add blank free field to add
    if ( getEnvironmentWindow().adminPermission )
    {
      Row row = new Row();
      row.setInsets( new Insets( 2 ) );
      row.setCellSpacing( new Extent( 2 ) );
      freeFieldsGrid.add( row );
      // paste
      Button pasteButton = new Button( Styles.PAGE_PASTE );
      pasteButton.setToolTipText( Messages.getString( "paste" ) );
      pasteButton.addActionListener( pasteFreeField );
      row.add( pasteButton );
      // add
      Button addButton = new Button( Styles.ADD );
      addButton.addActionListener( createFreeField );
      row.add( addButton );
      // new name field
      newFreeFieldNameField = new TextField();
      newFreeFieldNameField.setStyleName( "default" );
      newFreeFieldNameField.setWidth( new Extent( 100, Extent.PERCENT ) );
      freeFieldsGrid.add( newFreeFieldNameField );
      // new content field
      newFreeFieldContentField = new TextField();
      newFreeFieldContentField.setStyleName( "default" );
      newFreeFieldContentField.setWidth( new Extent( 100, Extent.PERCENT ) );
      freeFieldsGrid.add( newFreeFieldContentField );
    }

    // update the variables grid
    // remove all grid children
    variablesGrid.removeAll();
    // add grid headers
    Label variableActionsHeader = new Label( " " );
    variableActionsHeader.setStyleName( "grid.header" );
    variablesGrid.add( variableActionsHeader );
    Label variableNameHeader = new Label( Messages.getString( "name" ) );
    variableNameHeader.setStyleName( "grid.header" );
    variablesGrid.add( variableNameHeader );
    Label variableValueHeader = new Label( Messages.getString( "value" ) );
    variableValueHeader.setStyleName( "grid.header" );
    variablesGrid.add( variableValueHeader );
    // add variables fields
    for ( Iterator variableIterator = parent.getEnvironment().getVariables().iterator(); variableIterator.hasNext(); )
    {
      Variable current = (Variable) variableIterator.next();
      // row
      Row row = new Row();
      row.setInsets( new Insets( 2 ) );
      row.setCellSpacing( new Extent( 2 ) );
      variablesGrid.add( row );
      // copy
      Button copyButton = new Button( Styles.PAGE_COPY );
      copyButton.setToolTipText( Messages.getString( "copy" ) );
      copyButton.setActionCommand( current.getName() );
      copyButton.addActionListener( copyVariable );
      row.add( copyButton );
      if ( getEnvironmentWindow().adminPermission )
      {
        // up
        Button upButton = new Button( Styles.ARROW_UP );
        upButton.setToolTipText( Messages.getString( "up" ) );
        upButton.setActionCommand( current.getName() );
        upButton.addActionListener( upVariable );
        row.add( upButton );
        // down
        Button downButton = new Button( Styles.ARROW_DOWN );
        downButton.setToolTipText( Messages.getString( "down" ) );
        downButton.setActionCommand( current.getName() );
        downButton.addActionListener( downVariable );
        row.add( downButton );
        // edit
        Button editButton = new Button( Styles.ACCEPT );
        editButton.setToolTipText( Messages.getString( "apply" ) );
        editButton.setActionCommand( current.getName() );
        editButton.addActionListener( editVariable );
        row.add( editButton );
        // delete
        Button deleteButton = new Button( Styles.DELETE );
        deleteButton.setToolTipText( Messages.getString( "delete" ) );
        deleteButton.setActionCommand( current.getName() );
        deleteButton.addActionListener( deleteVariable );
        row.add( deleteButton );
      }
      // variable name
      TextField variableName = new TextField();
      variableName.setStyleName( "default" );
      variableName.setWidth( new Extent( 100, Extent.PERCENT ) );
      variableName.setText( current.getName() );
      variableName.setId( "variablename_" + parent.getEnvironment().getName() + "_" + current.getName() );
      variablesGrid.add( variableName );
      // variable value
      TextField variableContent = new TextField();
      variableContent.setStyleName( "default" );
      variableContent.setWidth( new Extent( 100, Extent.PERCENT ) );
      variableContent.setText( current.getValue() );
      variableContent.setId( "variablevalue_" + parent.getEnvironment().getName() + "_" + current.getName() );
      variablesGrid.add( variableContent );
    }
    // add blank variable to add
    if ( getEnvironmentWindow().adminPermission )
    {
      Row row = new Row();
      row.setInsets( new Insets( 2 ) );
      row.setCellSpacing( new Extent( 2 ) );
      variablesGrid.add( row );
      // paste
      Button pasteButton = new Button( Styles.PAGE_PASTE );
      pasteButton.setToolTipText( Messages.getString( "paste" ) );
      pasteButton.addActionListener( pasteVariable );
      row.add( pasteButton );
      // add
      Button addButton = new Button( Styles.ADD );
      addButton.setToolTipText( Messages.getString( "add" ) );
      addButton.addActionListener( createVariable );
      row.add( addButton );
      // new name
      newVariableName = new TextField();
      newVariableName.setStyleName( "default" );
      newVariableName.setWidth( new Extent( 100, Extent.PERCENT ) );
      variablesGrid.add( newVariableName );
      // new value 
      newVariableValue = new TextField();
      newVariableValue.setStyleName( "default" );
      newVariableValue.setWidth( new Extent( 100, Extent.PERCENT ) );
      variablesGrid.add( newVariableValue );
    }

    // update the log files grid
    logFilesGrid.removeAll();
    // add headers
    Label logFilesActionsHeader = new Label( " " );
    logFilesActionsHeader.setStyleName( "grid.header" );
    logFilesGrid.add( logFilesActionsHeader );
    Label logFilesNameHeader = new Label( Messages.getString( "name" ) );
    logFilesNameHeader.setStyleName( "grid.header" );
    logFilesGrid.add( logFilesNameHeader );
    Label logFilesPathHeader = new Label( Messages.getString( "path" ) );
    logFilesPathHeader.setStyleName( "grid.header" );
    logFilesGrid.add( logFilesPathHeader );
    Label logFilesAgentHeader = new Label( Messages.getString( "agent" ) );
    logFilesAgentHeader.setStyleName( "grid.header" );
    logFilesGrid.add( logFilesAgentHeader );
    // log files fields
    for ( Iterator logFileIterator = parent.getEnvironment().getLogFiles().iterator(); logFileIterator.hasNext(); )
    {
      LogFile logFile = (LogFile) logFileIterator.next();
      // action row
      Row row = new Row();
      row.setInsets( new Insets( 2 ) );
      row.setCellSpacing( new Extent( 2 ) );
      logFilesGrid.add( row );
      // copy button
      Button copyButton = new Button( Styles.PAGE_COPY );
      copyButton.setToolTipText( Messages.getString( "copy" ) );
      copyButton.setActionCommand( logFile.getName() );
      copyButton.addActionListener( copyLogFile );
      row.add( copyButton );
      // view button
      Button viewButton = new Button( Styles.INFORMATION );
      viewButton.setToolTipText( Messages.getString( "view" ) );
      viewButton.setActionCommand( logFile.getName() );
      viewButton.addActionListener( viewLogFile );
      row.add( viewButton );
      if ( getEnvironmentWindow().adminPermission )
      {
        // up button
        Button upButton = new Button( Styles.ARROW_UP );
        upButton.setToolTipText( Messages.getString( "up" ) );
        upButton.setActionCommand( logFile.getName() );
        upButton.addActionListener( upLogFile );
        row.add( upButton );
        // down button
        Button downButton = new Button( Styles.ARROW_DOWN );
        downButton.setToolTipText( Messages.getString( "down" ) );
        downButton.setActionCommand( logFile.getName() );
        downButton.addActionListener( downLogFile );
        row.add( downButton );
        // edit button
        Button editButton = new Button( Styles.ACCEPT );
        editButton.setToolTipText( Messages.getString( "apply" ) );
        editButton.setActionCommand( logFile.getName() );
        editButton.addActionListener( editLogFile );
        row.add( editButton );
        // delete button
        Button deleteButton = new Button( Styles.DELETE );
        deleteButton.setToolTipText( Messages.getString( "delete" ) );
        deleteButton.setActionCommand( logFile.getName() );
        deleteButton.addActionListener( deleteLogFile );
        row.add( deleteButton );
      }
      // add the log file name field
      TextField logFileNameField = new TextField();
      logFileNameField.setStyleName( "default" );
      logFileNameField.setWidth( new Extent( 100, Extent.PERCENT ) );
      logFileNameField.setId( "logfilename_" + parent.getEnvironmentName() + "_" + logFile.getName() );
      logFileNameField.setText( logFile.getName() );
      logFilesGrid.add( logFileNameField );
      // add the log file path field
      TextField logFilePathField = new TextField();
      logFilePathField.setStyleName( "default" );
      logFilePathField.setWidth( new Extent( 100, Extent.PERCENT ) );
      logFilePathField.setId( "logfilepath_" + parent.getEnvironmentName() + "_" + logFile.getName() );
      logFilePathField.setText( logFile.getPath() );
      logFilesGrid.add( logFilePathField );
      // add the log file agent field
      SelectField logFileAgentField = new SelectField();
      logFileAgentField.setStyleName( "default" );
      logFileAgentField.setWidth( new Extent( 100, Extent.PERCENT ) );
      logFileAgentField.setId( "logfileagent_" + parent.getEnvironmentName() + "_" + logFile.getName() );
      logFilesGrid.add( logFileAgentField );
      // populate the agent field
      DefaultListModel logFileAgentListModel = (DefaultListModel) logFileAgentField.getModel();
      logFileAgentListModel.removeAll();
      // add the blank agent
      logFileAgentListModel.add( "" );
      // add the agents
      for ( Iterator agentIterator = kalumet.getAgents().iterator(); agentIterator.hasNext(); )
      {
        Agent agent = (Agent) agentIterator.next();
        logFileAgentListModel.add( agent.getId() );
      }
      // select the correct item
      logFileAgentField.setSelectedItem( logFile.getAgent() );
    }
    if ( getEnvironmentWindow().adminPermission )
    {
      // action row
      Row row = new Row();
      row.setInsets( new Insets( 2 ) );
      row.setCellSpacing( new Extent( 2 ) );
      logFilesGrid.add( row );
      // paste button
      Button pasteButton = new Button( Styles.PAGE_PASTE );
      pasteButton.setToolTipText( Messages.getString( "paste" ) );
      pasteButton.addActionListener( pasteLogFile );
      row.add( pasteButton );
      // add button
      Button addButton = new Button( Styles.ADD );
      addButton.setToolTipText( Messages.getString( "add" ) );
      addButton.addActionListener( addLogFile );
      row.add( addButton );
      // add the new log file name field
      newLogFileName = new TextField();
      newLogFileName.setStyleName( "default" );
      newLogFileName.setWidth( new Extent( 100, Extent.PERCENT ) );
      logFilesGrid.add( newLogFileName );
      // add the new log file path field
      newLogFilePath = new TextField();
      newLogFilePath.setStyleName( "default" );
      newLogFilePath.setWidth( new Extent( 100, Extent.PERCENT ) );
      logFilesGrid.add( newLogFilePath );
      // add the new agents fields
      newLogFileAgent = new SelectField();
      newLogFileAgent.setStyleName( "default" );
      newLogFileAgent.setWidth( new Extent( 100, Extent.PERCENT ) );
      logFilesGrid.add( newLogFileAgent );
      DefaultListModel newLogFileAgentModel = (DefaultListModel) newLogFileAgent.getModel();
      newLogFileAgentModel.removeAll();
      newLogFileAgentModel.add( " " );
      for ( Iterator agentIterator = kalumet.getAgents().iterator(); agentIterator.hasNext(); )
      {
        Agent agent = (Agent) agentIterator.next();
        newLogFileAgentModel.add( agent.getId() );
      }
      newLogFileAgent.setSelectedIndex( 0 );
    }
  }

  public TextField getNameField()
  {
    return this.nameField;
  }

  public TextField getGroupField()
  {
    return this.groupField;
  }

  public SelectField getTagField()
  {
    return this.tagField;
  }

  public SelectField getAgentField()
  {
    return this.agentField;
  }

  public SelectField getAutoUpdateField()
  {
    return this.autoUpdateField;
  }

  public TextArea getNotesArea()
  {
    return this.notesArea;
  }

  public TextArea getWeblinksArea()
  {
    return this.weblinksArea;
  }

  public EnvironmentWindow getEnvironmentWindow()
  {
    return parent;
  }

}