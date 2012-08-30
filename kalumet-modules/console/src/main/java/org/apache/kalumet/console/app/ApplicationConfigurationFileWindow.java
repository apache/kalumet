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
import org.apache.kalumet.ws.client.ConfigurationFileClient;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * JEE application configuration file window.
 */
public class ApplicationConfigurationFileWindow
  extends WindowPane
{

  private String configurationFileName;

  private ConfigurationFile configurationFile;

  private ApplicationConfigurationFilesPane parent;

  private TextField nameField;

  private SelectField activeField;

  private SelectField blockerField;

  private TextField uriField;

  private TextField pathField;

  private SelectField agentField;

  private Grid mappingsGrid;

  private TextField newMappingKeyField;

  private TextField newMappingValueField;

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
        // call the webservice
        ConfigurationFileClient client = new ConfigurationFileClient( agent.getHostname(), agent.getPort() );
        boolean uptodate =
          client.check( parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName(),
                        parent.getParentPane().getServerName(), parent.getParentPane().getApplicationName(),
                        configurationFileName );
        if ( uptodate )
        {
          message = "JEE application " + parent.getParentPane().getApplicationName() + " configuration file "
            + configurationFileName + " is up to date.";
        }
        else
        {
          failure = true;
          message = "JEE application " + parent.getParentPane().getApplicationName() + " configuration file "
            + configurationFileName + " is not up to date.";
        }
      }
      catch ( Exception e )
      {
        failure = true;
        message = "JEE application " + parent.getParentPane().getApplicationName() + " configuration file "
          + configurationFileName + " status check failed: " + e.getMessage();
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
        // call the webservice
        ConfigurationFileClient client = new ConfigurationFileClient( agent.getHostname(), agent.getPort() );
        client.update( parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName(),
                       parent.getParentPane().getServerName(), parent.getParentPane().getApplicationName(),
                       configurationFileName, false );
      }
      catch ( Exception e )
      {
        failure = true;
        message = "JEE application " + parent.getParentPane().getApplicationName() + " configuration file "
          + configurationFileName + " update failed: " + e.getMessage();
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
      ApplicationConfigurationFileWindow.this.configurationFile =
        parent.getParentPane().getApplication().getConfigurationFile( configurationFileName );
      if ( ApplicationConfigurationFileWindow.this.configurationFile == null )
      {
        ApplicationConfigurationFileWindow.this.configurationFile = new ConfigurationFile();
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
      ApplicationConfigurationFileWindow.this.userClose();
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
            // delete the configuration file
            parent.getParentPane().getApplication().getConfigurationFiles().remove( configurationFile );
            // add a change event
            parent.getParentPane().getParentPane().getEnvironmentWindow().getChangeEvents().add(
              "Delete JEE application configuration file " + configurationFile.getName() );
            // change the updated flag
            parent.getParentPane().getParentPane().getEnvironmentWindow().setUpdated( true );
            // update the journal log tab pane
            parent.getParentPane().getParentPane().getEnvironmentWindow().updateJournalPane();
            // update the parent pane
            parent.update();
            // close the window
            ApplicationConfigurationFileWindow.this.userClose();
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
          Messages.getString( "configurationfile.warn.mandatory" ) );
        return;
      }
      // if the user change the configuration file name, check if the new
      // name doesn't already exist
      if ( configurationFileName == null || ( configurationFileName != null && !configurationFileName.equals(
        nameFieldValue ) ) )
      {
        if ( parent.getParentPane().getApplication().getConfigurationFile( nameFieldValue ) != null )
        {
          KalumetConsoleApplication.getApplication().getLogPane().addWarning(
            Messages.getString( "configurationfile.warn.exists" ) );
          return;
        }
      }
      // add a change event
      if ( configurationFileName != null )
      {
        parent.getParentPane().getParentPane().getEnvironmentWindow().getChangeEvents().add(
          "Change JEE application configuration file " + configurationFile.getName() );
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
      if ( configurationFileName == null )
      {
        try
        {
          parent.getParentPane().getApplication().addConfigurationFile( configurationFile );
          parent.getParentPane().getParentPane().getEnvironmentWindow().getChangeEvents().add(
            "Add JEE application configuration file " + configurationFile.getName() );
        }
        catch ( Exception e )
        {
          KalumetConsoleApplication.getApplication().getLogPane().addWarning(
            Messages.getString( "configurationfile.warn.exists" ) );
          return;
        }
      }
      // update the window definition
      setTitle( Messages.getString( "configurationfile" ) + " " + configurationFile.getName() );
      setId(
        "configurationfilewindow_" + parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName()
          + "_" + parent.getParentPane().getServerName() + "_" + parent.getParentPane().getApplicationName() + "_"
          + configurationFile.getName() );
      configurationFileName = configurationFile.getName();
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
        "JEE application " + parent.getParentPane().getApplicationName() + " configuration file "
          + configurationFileName + " status check in progress...",
        parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
      parent.getParentPane().getParentPane().getEnvironmentWindow().getChangeEvents().add(
        "JEE application " + parent.getParentPane().getApplicationName() + " configuration file "
          + configurationFileName + " status check requested." );
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
                                                                        parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
                                                                    }
                                                                    else
                                                                    {
                                                                      KalumetConsoleApplication.getApplication().getLogPane().addInfo(
                                                                        statusThread.message,
                                                                        parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
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
      // check if no change has not been saved
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
            // add a message into the log pane and the journal
            KalumetConsoleApplication.getApplication().getLogPane().addInfo(
              "JEE application " + parent.getParentPane().getApplicationName() + " configuration file "
                + configurationFileName + " update in progress...",
              parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
            parent.getParentPane().getParentPane().getEnvironmentWindow().getChangeEvents().add(
              "JEE application " + parent.getParentPane().getApplicationName() + " configuration file "
                + configurationFileName + " update requested." );
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
                      "JEE application " + parent.getParentPane().getApplicationName() + " configuration file "
                        + configurationFileName + " updated.",
                      parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
                    parent.getParentPane().getParentPane().getEnvironmentWindow().getChangeEvents().add(
                      "JEE application " + parent.getParentPane().getApplicationName() + " configuration file "
                        + configurationFileName + " updated." );
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

  // delete mapping
  private ActionListener deleteMapping = new ActionListener()
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
      // looking for the mapping object
      Mapping mapping = configurationFile.getMapping( event.getActionCommand() );
      if ( mapping == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "configurationfile.warn.mapping.notfound" ) );
        return;
      }
      // delete the mapping object
      configurationFile.getMappings().remove( mapping );
      // add a change event
      parent.getParentPane().getParentPane().getEnvironmentWindow().getChangeEvents().add(
        "Delete JEE application configuration file " + configurationFile.getName() + " mapping " + mapping.getKey() );
      // change the updated flag
      parent.getParentPane().getParentPane().getEnvironmentWindow().setUpdated( true );
      // update the journal log tab pane
      parent.getParentPane().getParentPane().getEnvironmentWindow().updateJournalPane();
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
      // get fields
      TextField keyField = (TextField) ApplicationConfigurationFileWindow.this.getComponent(
        "configurationfilemappingkey_"
          + parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() + "_"
          + parent.getParentPane().getServerName() + "_" + parent.getParentPane().getApplicationName() + "_"
          + configurationFileName + "_" + event.getActionCommand() );
      TextField valueField = (TextField) ApplicationConfigurationFileWindow.this.getComponent(
        "configurationfilemappingvalue_"
          + parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() + "_"
          + parent.getParentPane().getServerName() + "_" + parent.getParentPane().getApplicationName() + "_"
          + configurationFileName + "_" + event.getActionCommand() );
      // get fields value
      String keyFieldValue = keyField.getText();
      String valueFieldValue = valueField.getText();
      // check fields
      if ( keyFieldValue == null || keyFieldValue.trim().length() < 1 )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "configurationfile.warn.mapping.mandatory" ) );
        return;
      }
      // if the user change the mapping key, check if the key doesn't already
      // exist
      if ( !keyFieldValue.equals( event.getActionCommand() ) )
      {
        if ( configurationFile.getMapping( keyFieldValue ) != null )
        {
          KalumetConsoleApplication.getApplication().getLogPane().addWarning(
            Messages.getString( "configurationfile.warn.mapping.exists" ) );
          return;
        }
      }
      // looking for the mapping object
      Mapping mapping = configurationFile.getMapping( event.getActionCommand() );
      if ( mapping == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "configurationfile.warn.mapping.notfound" ) );
        return;
      }
      // add a change event
      parent.getParentPane().getParentPane().getEnvironmentWindow().getChangeEvents().add(
        "Change JEE application configuration file " + configurationFile.getName() + " mapping " + mapping.getKey() );
      // update the mapping
      mapping.setKey( keyFieldValue );
      mapping.setValue( valueFieldValue );
      // change the updated flag
      parent.getParentPane().getParentPane().getEnvironmentWindow().setUpdated( true );
      // update the journal log tab pane
      parent.getParentPane().getParentPane().getEnvironmentWindow().updateJournalPane();
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
      // get fields value
      String newMappingKeyFieldValue = newMappingKeyField.getText();
      String newMappingValueFieldValue = newMappingValueField.getText();
      // check fields
      if ( newMappingKeyFieldValue == null || newMappingKeyFieldValue.trim().length() < 1 )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "configurationfile.warn.mapping.mandatory" ) );
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
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "configurationfile.warn.mapping.exists" ) );
        return;
      }
      // add a change event
      parent.getParentPane().getParentPane().getEnvironmentWindow().getChangeEvents().add(
        "Add JEE application configuration file " + configurationFile.getName() + " mapping " + mapping.getKey() );
      // change the updated flag
      parent.getParentPane().getParentPane().getEnvironmentWindow().setUpdated( true );
      // update the journal log tab pane
      parent.getParentPane().getParentPane().getEnvironmentWindow().updateJournalPane();
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
      configurationFileName = null;
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

  /**
   * Create a new <code>ApplicationConfigurationFileWindow</code>.
   *
   * @param parent                the <code>ApplicationConfigurationFilesPane</code>.
   * @param configurationFileName the original <code>ConfigurationFile</code> name.
   */
  public ApplicationConfigurationFileWindow( ApplicationConfigurationFilesPane parent, String configurationFileName )
  {
    super();

    // update the parent pane
    this.parent = parent;

    // update the configuration file name
    this.configurationFileName = configurationFileName;

    // update the configuration file object from the parent pane
    this.configurationFile = parent.getParentPane().getApplication().getConfigurationFile( configurationFileName );
    if ( this.configurationFile == null )
    {
      this.configurationFile = new ConfigurationFile();
    }

    if ( configurationFileName == null )
    {
      setTitle( Messages.getString( "configurationfile" ) );
    }
    else
    {
      setTitle( Messages.getString( "configurationfile" ) + " " + configurationFileName );
    }
    setId(
      "configurationfilewindow_" + parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName()
        + "_" + parent.getParentPane().getServerName() + "_" + parent.getParentPane().getApplicationName() + "_"
        + configurationFileName );
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
    // add the status button
    Button statusButton = new Button( Messages.getString( "status" ), Styles.INFORMATION );
    statusButton.setStyleName( "control" );
    statusButton.addActionListener( status );
    controlRow.add( statusButton );
    if ( parent.getParentPane().getParentPane().getEnvironmentWindow().adminPermission
      || parent.getParentPane().getParentPane().getEnvironmentWindow().jeeApplicationsUpdatePermission )
    {
      // add the update button
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
   * Update the window
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
        parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
    }
    DefaultListModel agentListModel = (DefaultListModel) agentField.getModel();
    agentListModel.removeAll();
    agentListModel.add( "" );
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
      if ( parent.getParentPane().getParentPane().getEnvironmentWindow().adminPermission
        || parent.getParentPane().getParentPane().getEnvironmentWindow().jeeApplicationsPermission )
      {
        // edit
        Button editButton = new Button( Styles.ACCEPT );
        editButton.setToolTipText( Messages.getString( "apply" ) );
        editButton.setActionCommand( mapping.getKey() );
        editButton.addActionListener( editMapping );
        row.add( editButton );
        // delete
        Button deleteButton = new Button( Styles.DELETE );
        deleteButton.setToolTipText( Messages.getString( "delete" ) );
        deleteButton.setActionCommand( mapping.getKey() );
        deleteButton.addActionListener( deleteMapping );
        row.add( deleteButton );
      }
      // mapping key
      TextField mappingKeyField = new TextField();
      mappingKeyField.setStyleName( "default" );
      mappingKeyField.setWidth( new Extent( 100, Extent.PERCENT ) );
      mappingKeyField.setId( "configurationfilemappingkey_"
                               + parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName()
                               + "_" + parent.getParentPane().getServerName() + "_"
                               + parent.getParentPane().getApplicationName() + "_" + configurationFileName + "_"
                               + mapping.getKey() );
      mappingKeyField.setText( mapping.getKey() );
      mappingsGrid.add( mappingKeyField );
      // mapping value
      TextField mappingValueField = new TextField();
      mappingValueField.setStyleName( "default" );
      mappingValueField.setWidth( new Extent( 100, Extent.PERCENT ) );
      mappingValueField.setId( "configurationfilemappingvalue_"
                                 + parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName()
                                 + "_" + parent.getParentPane().getServerName() + "_"
                                 + parent.getParentPane().getApplicationName() + "_" + configurationFileName + "_"
                                 + mapping.getKey() );
      mappingValueField.setText( mapping.getValue() );
      mappingsGrid.add( mappingValueField );
    }
    if ( parent.getParentPane().getParentPane().getEnvironmentWindow().adminPermission
      || parent.getParentPane().getParentPane().getEnvironmentWindow().jeeApplicationsPermission )
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

}
