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
import nextapp.echo2.app.CheckBox;
import nextapp.echo2.app.Column;
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.SplitPane;
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.WindowPane;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import org.apache.commons.vfs.FileObject;
import org.apache.kalumet.FileManipulator;
import org.apache.kalumet.utils.VariableUtils;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Define an application checkbox.
 */
class ApplicationCheckBox
  extends CheckBox
{

  private String serverName;

  private String applicationName;

  public ApplicationCheckBox( String serverName, String applicationName )
  {
    super( applicationName );
    this.serverName = serverName;
    this.applicationName = applicationName;
  }

  public String getServerName()
  {
    return this.serverName;
  }

  public String getApplicationName()
  {
    return this.applicationName;
  }

}

/**
 * Define an application archive checkbox.
 */
class ArchiveCheckBox
  extends CheckBox
{

  private String serverName;

  private String applicationName;

  private String archiveName;

  public ArchiveCheckBox( String serverName, String applicationName, String archiveName )
  {
    super( archiveName );
    this.serverName = serverName;
    this.applicationName = applicationName;
    this.archiveName = archiveName;
  }

  public String getServerName()
  {
    return this.serverName;
  }

  public String getApplicationName()
  {
    return this.applicationName;
  }

  public String getArchiveName()
  {
    return this.archiveName;
  }

}

/**
 * Define an application configuration file checkbox.
 */
class ConfigurationFileCheckBox
  extends CheckBox
{

  private String serverName;

  private String applicationName;

  private String configurationFileName;

  public ConfigurationFileCheckBox( String serverName, String applicationName, String configurationFileName )
  {
    super( configurationFileName );
    this.serverName = serverName;
    this.applicationName = applicationName;
    this.configurationFileName = configurationFileName;
  }

  public String getServerName()
  {
    return this.serverName;
  }

  public String getApplicationName()
  {
    return this.applicationName;
  }

  public String getConfigurationFileName()
  {
    return this.configurationFileName;
  }

}

/**
 * Define an application SQL script checkbox.
 */
class SqlScriptCheckBox
  extends CheckBox
{

  private String serverName;

  private String applicationName;

  private String databaseName;

  private String sqlScriptName;

  public SqlScriptCheckBox( String serverName, String applicationName, String databaseName, String sqlScriptName )
  {
    super( sqlScriptName );
    this.serverName = serverName;
    this.applicationName = applicationName;
    this.databaseName = databaseName;
    this.sqlScriptName = sqlScriptName;
  }

  public String getServerName()
  {
    return this.serverName;
  }

  public String getApplicationName()
  {
    return this.applicationName;
  }

  public String getDatabaseName()
  {
    return this.databaseName;
  }

  public String getSqlScriptName()
  {
    return this.sqlScriptName;
  }

}

/**
 * Define a software checkbox.
 */
class SoftwareCheckBox
  extends CheckBox
{

  private String softwareName;

  public SoftwareCheckBox( String softwareName )
  {
    super( softwareName );
    this.softwareName = softwareName;
  }

  public String getSoftwareName()
  {
    return this.softwareName;
  }

}

/**
 * Environment publish release window.
 */
public class PublishReleaseWindow
  extends WindowPane
{

  private EnvironmentWindow parent;

  private TextField baseLocationField;

  private TextField applicationLocationField;

  private LinkedList applicationCheckBoxes = new LinkedList();

  private TextField archiveLocationField;

  private LinkedList archiveCheckBoxes = new LinkedList();

  private TextField configurationFileLocationField;

  private LinkedList configurationFileCheckBoxes = new LinkedList();

  private TextField sqlScriptLocationField;

  private LinkedList sqlScriptCheckBoxes = new LinkedList();

  private TextField softwareLocationField;

  private LinkedList softwareCheckBoxes = new LinkedList();

  // close
  private ActionListener close = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      PublishReleaseWindow.this.userClose();
    }
  };

  // publish
  private ActionListener publish = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // get the locations
      String baseLocation = baseLocationField.getText();
      String applicationLocation = applicationLocationField.getText();
      String archiveLocation = archiveLocationField.getText();
      String configurationFileLocation = configurationFileLocationField.getText();
      String sqlScriptLocation = sqlScriptLocationField.getText();
      String softwareLocation = softwareLocationField.getText();
      // check the locations
      if ( baseLocation == null || baseLocation.trim().length() < 1 || applicationLocation == null
        || applicationLocation.trim().length() < 1 || archiveLocation == null || archiveLocation.trim().length() < 1
        || configurationFileLocation == null || configurationFileLocation.trim().length() < 1
        || sqlScriptLocation == null || sqlScriptLocation.trim().length() < 1 || softwareLocation == null
        || softwareLocation.trim().length() < 1 )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "publish.mandatory" ),
                                                                            parent.getEnvironmentName() );
        return;
      }
      // update the environment location
      parent.getEnvironment().setReleaseLocation( baseLocation );
      parent.setUpdated( true );
      FileManipulator fileManipulator = null;
      try
      {
        // init file manipulator
        fileManipulator = new FileManipulator();
        // iterate in the application checkboxes
        for ( Iterator applicationCheckBoxIterator = applicationCheckBoxes.iterator();
              applicationCheckBoxIterator.hasNext(); )
        {
          ApplicationCheckBox checkBox = (ApplicationCheckBox) applicationCheckBoxIterator.next();
          if ( checkBox.isSelected() )
          {
            J2EEApplication application = parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(
              checkBox.getServerName() ).getJ2EEApplication( checkBox.getApplicationName() );
            String applicationUri =
              VariableUtils.replace( application.getUri(), parent.getEnvironment().getVariables() );
            FileObject file = fileManipulator.resolveFile( applicationUri );
            String fileName = file.getName().getBaseName();
            fileManipulator.copy( applicationUri, baseLocation + "/" + applicationLocation + "/" + fileName );
          }
        }
        // iterate in the archive checkboxes
        for ( Iterator archiveCheckBoxIterator = archiveCheckBoxes.iterator(); archiveCheckBoxIterator.hasNext(); )
        {
          ArchiveCheckBox checkBox = (ArchiveCheckBox) archiveCheckBoxIterator.next();
          if ( checkBox.isSelected() )
          {
            J2EEApplication application = parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(
              checkBox.getServerName() ).getJ2EEApplication( checkBox.getApplicationName() );
            Archive archive = application.getArchive( checkBox.getArchiveName() );
            String archiveUri;
            if ( FileManipulator.protocolExists( archive.getUri() ) )
            {
              archiveUri = VariableUtils.replace( archive.getUri(), parent.getEnvironment().getVariables() );
            }
            else
            {
              archiveUri = FileManipulator.format(
                VariableUtils.replace( application.getUri(), parent.getEnvironment().getVariables() ) + "!/"
                  + VariableUtils.replace( archive.getUri(), parent.getEnvironment().getVariables() ) );
            }
            FileObject file = fileManipulator.resolveFile( archiveUri );
            String fileName = file.getName().getBaseName();
            fileManipulator.copy( archiveUri, baseLocation + "/" + archiveLocation + "/" + fileName );
          }
        }
        // iterate in the configuration file checkboxes
        for ( Iterator configurationFileCheckBoxIterator = configurationFileCheckBoxes.iterator();
              configurationFileCheckBoxIterator.hasNext(); )
        {
          ConfigurationFileCheckBox checkBox = (ConfigurationFileCheckBox) configurationFileCheckBoxIterator.next();
          if ( checkBox.isSelected() )
          {
            J2EEApplication application = parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(
              checkBox.getServerName() ).getJ2EEApplication( checkBox.getApplicationName() );
            ConfigurationFile configurationFile =
              application.getConfigurationFile( checkBox.getConfigurationFileName() );
            String configurationFileUri;
            if ( FileManipulator.protocolExists( configurationFile.getUri() ) )
            {
              configurationFileUri =
                VariableUtils.replace( configurationFile.getUri(), parent.getEnvironment().getVariables() );
            }
            else
            {
              configurationFileUri = FileManipulator.format(
                VariableUtils.replace( application.getUri(), parent.getEnvironment().getVariables() ) + "!/"
                  + VariableUtils.replace( configurationFile.getUri(), parent.getEnvironment().getVariables() ) );
            }
            fileManipulator.copy( configurationFileUri,
                                  baseLocation + "/" + configurationFileLocation + "/" + configurationFile.getName() );
          }
        }
        // iterate in the SQL script checkboxes
        for ( Iterator sqlScriptCheckBoxIterator = sqlScriptCheckBoxes.iterator();
              sqlScriptCheckBoxIterator.hasNext(); )
        {
          SqlScriptCheckBox checkBox = (SqlScriptCheckBox) sqlScriptCheckBoxIterator.next();
          if ( checkBox.isSelected() )
          {
            J2EEApplication application = parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(
              checkBox.getServerName() ).getJ2EEApplication( checkBox.getApplicationName() );
            Database database = application.getDatabase( checkBox.getDatabaseName() );
            SqlScript sqlScript = database.getSqlScript( checkBox.getSqlScriptName() );
            String sqlScriptUri;
            if ( FileManipulator.protocolExists( sqlScript.getUri() ) )
            {
              sqlScriptUri = VariableUtils.replace( sqlScript.getUri(), parent.getEnvironment().getVariables() );
            }
            else
            {
              sqlScriptUri = FileManipulator.format(
                VariableUtils.replace( application.getUri(), parent.getEnvironment().getVariables() ) + "!/"
                  + VariableUtils.replace( sqlScript.getUri(), parent.getEnvironment().getVariables() ) );
            }
            fileManipulator.copy( sqlScriptUri, baseLocation + "/" + sqlScriptLocation + "/" + sqlScript.getName() );
          }
        }
        // iterate in the external checkboxes
        for ( Iterator externalCheckBoxIterator = softwareCheckBoxes.iterator(); externalCheckBoxIterator.hasNext(); )
        {
          SoftwareCheckBox checkBox = (SoftwareCheckBox) externalCheckBoxIterator.next();
          if ( checkBox.isSelected() )
          {
            Software software = parent.getEnvironment().getSoftware( checkBox.getSoftwareName() );
            String softwareUri = VariableUtils.replace( software.getUri(), parent.getEnvironment().getVariables() );
            FileObject file = fileManipulator.resolveFile( softwareUri );
            String fileName = file.getName().getBaseName();
            fileManipulator.copy( softwareUri, baseLocation + "/" + softwareLocation + "/" + fileName );
          }
        }
      }
      catch ( Exception e )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addError(
          Messages.getString( "release.error" ) + ": " + e.getMessage(), parent.getEnvironmentName() );
        return;
      }
      finally
      {
        if ( fileManipulator != null )
        {
          fileManipulator.close();
        }
      }
      KalumetConsoleApplication.getApplication().getLogPane().addConfirm( Messages.getString( "release.published" ) );
    }
  };

  /**
   * Create a new <code>PublishReleaseWindow</code>.
   *
   * @param parent the parent <code>EnvironmentWindow</code>.
   */
  public PublishReleaseWindow( EnvironmentWindow parent )
  {
    super();

    // update parent
    this.parent = parent;

    setTitle( parent.getEnvironmentName() + " " + Messages.getString( "release" ) );
    setId( "publishreleasewindow_" + parent.getEnvironmentName() );
    setModal( false );
    setStyleName( "default" );
    setWidth( new Extent( 600 ) );
    setHeight( new Extent( 600 ) );
    setDefaultCloseOperation( WindowPane.DISPOSE_ON_CLOSE );

    // create a split pane for the control button
    SplitPane splitPane = new SplitPane( SplitPane.ORIENTATION_VERTICAL_BOTTOM_TOP, new Extent( 32 ) );
    add( splitPane );

    // add the control pane
    Row controlRow = new Row();
    controlRow.setStyleName( "control" );
    splitPane.add( controlRow );
    // add the close button
    Button closeButton = new Button( Messages.getString( "close" ), Styles.CROSS );
    closeButton.setStyleName( "control" );
    closeButton.addActionListener( close );
    controlRow.add( closeButton );
    // add the publish button
    Button publishButton = new Button( Messages.getString( "publish" ), Styles.ACCEPT );
    publishButton.setStyleName( "control" );
    publishButton.addActionListener( publish );
    controlRow.add( publishButton );

    // create the main column
    Column column = new Column();
    column.setStyleName( "default" );
    column.setCellSpacing( new Extent( 2 ) );
    column.setInsets( new Insets( 2 ) );
    splitPane.add( column );

    // add base location
    Label baseLocationLabel = new Label( Messages.getString( "release.location" ) );
    column.add( baseLocationLabel );
    baseLocationField = new TextField();
    baseLocationField.setStyleName( "default" );
    baseLocationField.setWidth( new Extent( 100, Extent.PERCENT ) );
    baseLocationField.setText( parent.getEnvironment().getReleaseLocation() );
    column.add( baseLocationField );

    // add applications
    Label applicationsHeader = new Label( Messages.getString( "applications" ) );
    column.add( applicationsHeader );
    // add applications location
    applicationLocationField = new TextField();
    applicationLocationField.setStyleName( "default" );
    applicationLocationField.setWidth( new Extent( 100, Extent.PERCENT ) );
    applicationLocationField.setText( "/applications" );
    column.add( applicationLocationField );
    // add applications checkbox
    for ( Iterator applicationServerIterator =
            parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServers().iterator();
          applicationServerIterator.hasNext(); )
    {
      J2EEApplicationServer applicationServer = (J2EEApplicationServer) applicationServerIterator.next();
      for ( Iterator applicationIterator = applicationServer.getJ2EEApplications().iterator();
            applicationIterator.hasNext(); )
      {
        J2EEApplication application = (J2EEApplication) applicationIterator.next();
        ApplicationCheckBox checkBox = new ApplicationCheckBox( applicationServer.getName(), application.getName() );
        checkBox.setStyleName( "default" );
        applicationCheckBoxes.add( checkBox );
        column.add( checkBox );
      }
    }

    // add archives
    Label archivesHeader = new Label( Messages.getString( "archives" ) );
    column.add( archivesHeader );
    // add archives location
    archiveLocationField = new TextField();
    archiveLocationField.setStyleName( "default" );
    archiveLocationField.setWidth( new Extent( 100, Extent.PERCENT ) );
    archiveLocationField.setText( "/archives" );
    column.add( archiveLocationField );
    // add archives checkbox
    for ( Iterator applicationServerIterator =
            parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServers().iterator();
          applicationServerIterator.hasNext(); )
    {
      J2EEApplicationServer applicationServer = (J2EEApplicationServer) applicationServerIterator.next();
      for ( Iterator applicationIterator = applicationServer.getJ2EEApplications().iterator();
            applicationIterator.hasNext(); )
      {
        J2EEApplication application = (J2EEApplication) applicationIterator.next();
        for ( Iterator archiveIterator = application.getArchives().iterator(); archiveIterator.hasNext(); )
        {
          Archive archive = (Archive) archiveIterator.next();
          ArchiveCheckBox checkBox =
            new ArchiveCheckBox( applicationServer.getName(), application.getName(), archive.getName() );
          checkBox.setStyleName( "default" );
          archiveCheckBoxes.add( checkBox );
          column.add( checkBox );
        }
      }
    }

    // add configuration files
    Label configurationFilesHeader = new Label( Messages.getString( "configurationfiles" ) );
    column.add( configurationFilesHeader );
    // add configuration files location
    configurationFileLocationField = new TextField();
    configurationFileLocationField.setStyleName( "default" );
    configurationFileLocationField.setWidth( new Extent( 100, Extent.PERCENT ) );
    configurationFileLocationField.setText( "/config" );
    column.add( configurationFileLocationField );
    // add configuration files checkbox
    for ( Iterator applicationServerIterator =
            parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServers().iterator();
          applicationServerIterator.hasNext(); )
    {
      J2EEApplicationServer applicationServer = (J2EEApplicationServer) applicationServerIterator.next();
      for ( Iterator applicationIterator = applicationServer.getJ2EEApplications().iterator();
            applicationIterator.hasNext(); )
      {
        J2EEApplication application = (J2EEApplication) applicationIterator.next();
        for ( Iterator configurationFileIterator = application.getConfigurationFiles().iterator();
              configurationFileIterator.hasNext(); )
        {
          ConfigurationFile configurationFile = (ConfigurationFile) configurationFileIterator.next();
          ConfigurationFileCheckBox checkBox =
            new ConfigurationFileCheckBox( applicationServer.getName(), application.getName(),
                                           configurationFile.getName() );
          checkBox.setStyleName( "default" );
          configurationFileCheckBoxes.add( checkBox );
          column.add( checkBox );
        }
      }
    }

    // add SQL scripts
    Label sqlScriptsHeader = new Label( Messages.getString( "sql.scripts" ) );
    column.add( sqlScriptsHeader );
    // add SQL scripts location
    sqlScriptLocationField = new TextField();
    sqlScriptLocationField.setStyleName( "default" );
    sqlScriptLocationField.setWidth( new Extent( 100, Extent.PERCENT ) );
    sqlScriptLocationField.setText( "/sql" );
    column.add( sqlScriptLocationField );
    // add SQL scripts checkbox
    for ( Iterator applicationServerIterator =
            parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServers().iterator();
          applicationServerIterator.hasNext(); )
    {
      J2EEApplicationServer applicationServer = (J2EEApplicationServer) applicationServerIterator.next();
      for ( Iterator applicationIterator = applicationServer.getJ2EEApplications().iterator();
            applicationIterator.hasNext(); )
      {
        J2EEApplication application = (J2EEApplication) applicationIterator.next();
        for ( Iterator databaseIterator = application.getDatabases().iterator(); databaseIterator.hasNext(); )
        {
          Database database = (Database) databaseIterator.next();
          for ( Iterator sqlScriptIterator = database.getSqlScripts().iterator(); sqlScriptIterator.hasNext(); )
          {
            SqlScript sqlScript = (SqlScript) sqlScriptIterator.next();
            SqlScriptCheckBox checkBox =
              new SqlScriptCheckBox( applicationServer.getName(), application.getName(), database.getName(),
                                     sqlScript.getName() );
            checkBox.setStyleName( "default" );
            sqlScriptCheckBoxes.add( checkBox );
            column.add( checkBox );
          }
        }
      }
    }

    // add external softwares
    Label softwaresHeader = new Label( Messages.getString( "softwares" ) );
    column.add( softwaresHeader );
    // add external softwares location
    softwareLocationField = new TextField();
    softwareLocationField.setStyleName( "default" );
    softwareLocationField.setWidth( new Extent( 100, Extent.PERCENT ) );
    softwareLocationField.setText( "/contrib" );
    column.add( softwareLocationField );
    // add software checkbox
    for ( Iterator softwareIterator = parent.getEnvironment().getSoftwares().iterator(); softwareIterator.hasNext(); )
    {
      Software software = (Software) softwareIterator.next();
      SoftwareCheckBox checkBox = new SoftwareCheckBox( software.getName() );
      checkBox.setStyleName( "default" );
      softwareCheckBoxes.add( checkBox );
      column.add( checkBox );
    }

  }

}