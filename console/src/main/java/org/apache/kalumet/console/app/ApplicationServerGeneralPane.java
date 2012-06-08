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

import nextapp.echo2.app.ContentPane;
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.Grid;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.PasswordField;
import nextapp.echo2.app.SelectField;
import nextapp.echo2.app.TextArea;
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.list.DefaultListModel;
import org.apache.kalumet.console.configuration.ConfigurationManager;
import org.apache.kalumet.model.Agent;
import org.apache.kalumet.model.Kalumet;

import java.util.Iterator;

/**
 * J2EE application server general pane.
 */
public class ApplicationServerGeneralPane
  extends ContentPane
{

  private static String[] APPLICATIONSERVER_TYPES =
    new String[]{ Messages.getString( "jboss4" ), Messages.getString( "jboss6" ), Messages.getString( "weblogic8" ),
      Messages.getString( "websphere5" ) };

  private ApplicationServerWindow parent;

  private TextField nameField;

  private SelectField activeField;

  private SelectField blockerField;

  private SelectField typeField;

  private TextField jmxField;

  private TextField adminUserField;

  private PasswordField adminPasswordField;

  private PasswordField adminConfirmPasswordField;

  private SelectField agentField;

  private SelectField updateRequireRestartField;

  private SelectField updateRequireCachesClean;

  private SelectField stopUsingJmx;

  private TextArea startupCommandArea;

  private TextArea shutdownCommandArea;

  /**
   * Create a new <code>ApplicationServerGeneralPane</code>.
   *
   * @param parent the parent <code>ApplicationServerWindow</code>.
   */
  public ApplicationServerGeneralPane( ApplicationServerWindow parent )
  {
    super();
    setStyleName( "tab.content" );

    // update parent
    this.parent = parent;

    // add the general layout grid
    Grid layout = new Grid( 2 );
    layout.setStyleName( "default" );
    layout.setColumnWidth( 0, new Extent( 20, Extent.PERCENT ) );
    layout.setColumnWidth( 1, new Extent( 80, Extent.PERCENT ) );
    layout.setInsets( new Insets( 2 ) );
    add( layout );

    // add the name field
    Label nameLabel = new Label( Messages.getString( "name" ) );
    nameLabel.setStyleName( "grid.cell" );
    layout.add( nameLabel );
    nameField = new TextField();
    nameField.setStyleName( "default" );
    nameField.setWidth( new Extent( 100, Extent.PERCENT ) );
    layout.add( nameField );
    // add the active select field
    Label activeLabel = new Label( Messages.getString( "active" ) );
    activeLabel.setStyleName( "grid.cell" );
    layout.add( activeLabel );
    activeField = new SelectField( MainScreen.LABELS );
    activeField.setStyleName( "default" );
    activeField.setWidth( new Extent( 10, Extent.EX ) );
    activeField.setSelectedIndex( 0 );
    layout.add( activeField );
    // add the blocker select field
    Label blockerLabel = new Label( Messages.getString( "blocker" ) );
    blockerLabel.setStyleName( "grid.cell" );
    layout.add( blockerLabel );
    blockerField = new SelectField( MainScreen.LABELS );
    blockerField.setStyleName( "default" );
    blockerField.setWidth( new Extent( 10, Extent.EX ) );
    blockerField.setSelectedIndex( 0 );
    layout.add( blockerField );
    // add the type select field
    Label typeLabel = new Label( Messages.getString( "type" ) );
    typeLabel.setStyleName( "grid.cell" );
    layout.add( typeLabel );
    typeField = new SelectField( ApplicationServerGeneralPane.APPLICATIONSERVER_TYPES );
    typeField.setStyleName( "default" );
    typeField.setSelectedIndex( 0 );
    typeField.setWidth( new Extent( 50, Extent.EX ) );
    layout.add( typeField );
    // add the jmx url field
    Label jmxLabel = new Label( Messages.getString( "jmx" ) );
    jmxLabel.setStyleName( "grid.cell" );
    layout.add( jmxLabel );
    jmxField = new TextField();
    jmxField.setStyleName( "default" );
    jmxField.setWidth( new Extent( 100, Extent.PERCENT ) );
    layout.add( jmxField );
    // add the admin user field
    Label adminUserLabel = new Label( Messages.getString( "user" ) );
    adminUserLabel.setStyleName( "grid.cell" );
    layout.add( adminUserLabel );
    adminUserField = new TextField();
    adminUserField.setStyleName( "default" );
    adminUserField.setWidth( new Extent( 100, Extent.PERCENT ) );
    layout.add( adminUserField );
    // add the admin user password field
    Label adminPasswordLabel = new Label( Messages.getString( "password" ) );
    adminPasswordLabel.setStyleName( "grid.cell" );
    layout.add( adminPasswordLabel );
    adminPasswordField = new PasswordField();
    adminPasswordField.setStyleName( "default" );
    adminPasswordField.setWidth( new Extent( 100, Extent.PERCENT ) );
    layout.add( adminPasswordField );
    Label adminConfirmPasswordLabel = new Label( Messages.getString( "password.confirm" ) );
    adminConfirmPasswordLabel.setStyleName( "grid.cell" );
    layout.add( adminConfirmPasswordLabel );
    adminConfirmPasswordField = new PasswordField();
    adminConfirmPasswordField.setStyleName( "default" );
    adminConfirmPasswordField.setWidth( new Extent( 100, Extent.PERCENT ) );
    layout.add( adminConfirmPasswordField );
    // add the agent field
    Label agentLabel = new Label( Messages.getString( "agent" ) );
    agentLabel.setStyleName( "grid.cell" );
    layout.add( agentLabel );
    agentField = new SelectField();
    agentField.setStyleName( "default" );
    agentField.setWidth( new Extent( 50, Extent.EX ) );
    layout.add( agentField );
    // add the update require restart field
    Label updateRequireRestartLabel = new Label( Messages.getString( "update.require.restart" ) );
    updateRequireRestartLabel.setStyleName( "grid.cell" );
    layout.add( updateRequireRestartLabel );
    updateRequireRestartField = new SelectField( MainScreen.LABELS );
    updateRequireRestartField.setStyleName( "default" );
    updateRequireRestartField.setSelectedIndex( 0 );
    updateRequireRestartField.setWidth( new Extent( 10, Extent.EX ) );
    layout.add( updateRequireRestartField );
    // add the update require cache cleaning field
    Label updateRequireCachesCleaningLabel = new Label( Messages.getString( "update.require.caches.clean" ) );
    updateRequireCachesCleaningLabel.setStyleName( "grid.cell" );
    layout.add( updateRequireCachesCleaningLabel );
    updateRequireCachesClean = new SelectField( MainScreen.LABELS );
    updateRequireCachesClean.setStyleName( "default" );
    updateRequireCachesClean.setSelectedIndex( 0 );
    updateRequireCachesClean.setWidth( new Extent( 10, Extent.EX ) );
    layout.add( updateRequireCachesClean );
    // add the use jmx stop field
    Label stopUsingJmxLabel = new Label( Messages.getString( "stop.using.jmx" ) );
    stopUsingJmxLabel.setStyleName( "grid.cell" );
    layout.add( stopUsingJmxLabel );
    stopUsingJmx = new SelectField( MainScreen.LABELS );
    stopUsingJmx.setStyleName( "default" );
    stopUsingJmx.setSelectedIndex( 0 );
    stopUsingJmx.setWidth( new Extent( 10, Extent.EX ) );
    layout.add( stopUsingJmx );
    // add the startup command area
    Label startupCommandLabel = new Label( Messages.getString( "applicationserver.startup" ) );
    startupCommandLabel.setStyleName( "grid.cell" );
    layout.add( startupCommandLabel );
    startupCommandArea = new TextArea();
    startupCommandArea.setStyleName( "default" );
    startupCommandArea.setWidth( new Extent( 100, Extent.PERCENT ) );
    startupCommandArea.setHeight( new Extent( 20, Extent.EX ) );
    layout.add( startupCommandArea );
    // add the shutdown command area
    Label serverShutdownCommandLabel = new Label( Messages.getString( "applicationserver.shutdown" ) );
    serverShutdownCommandLabel.setStyleName( "grid.cell" );
    layout.add( serverShutdownCommandLabel );
    shutdownCommandArea = new TextArea();
    shutdownCommandArea.setStyleName( "default" );
    shutdownCommandArea.setWidth( new Extent( 100, Extent.PERCENT ) );
    shutdownCommandArea.setHeight( new Extent( 20, Extent.EX ) );
    layout.add( shutdownCommandArea );

    // update the pane
    update();
  }

  /**
   * Update the pane.
   */
  public void update()
  {
    // updae the JEE server name field
    nameField.setText( parent.getApplicationServer().getName() );
    // update the JEE server active field
    if ( parent.getApplicationServer().isActive() )
    {
      activeField.setSelectedIndex( 0 );
    }
    else
    {
      activeField.setSelectedIndex( 1 );
    }
    // update the JEE server blocker field
    if ( parent.getApplicationServer().isBlocker() )
    {
      blockerField.setSelectedIndex( 0 );
    }
    else
    {
      blockerField.setSelectedIndex( 1 );
    }
    // update the J2EE application server type field
    if ( parent.getApplicationServer().getClassname().equals( ApplicationServerWindow.JBOSS4_CONTROLLER_CLASSNAME ) )
    {
      typeField.setSelectedIndex( 0 );
    }
    if ( parent.getApplicationServer().getClassname().equals( ApplicationServerWindow.JBOSS6_CONTROLLER_CLASSNAME ) )
    {
      typeField.setSelectedIndex( 1 );
    }
    if ( parent.getApplicationServer().getClassname().equals( ApplicationServerWindow.WEBLOGIC_CONTROLLER_CLASSNAME ) )
    {
      typeField.setSelectedIndex( 2 );
    }
    if ( parent.getApplicationServer().getClassname().equals( ApplicationServerWindow.WEBSPHERE_CONTROLLER_CLASSNAME ) )
    {
      typeField.setSelectedIndex( 3 );
    }
    // update the jee application server jmx field
    jmxField.setText( parent.getApplicationServer().getJmxurl() );
    // update the jee application server admin user field
    adminUserField.setText( parent.getApplicationServer().getAdminuser() );
    // update the jee application server admin password/confirm password
    // fields
    adminPasswordField.setText( parent.getApplicationServer().getAdminpassword() );
    adminConfirmPasswordField.setText( parent.getApplicationServer().getAdminpassword() );
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
    // update the jee application server agent
    DefaultListModel agentListModel = (DefaultListModel) agentField.getModel();
    agentListModel.removeAll();
    agentListModel.add( "" );
    for ( Iterator agentIterator = kalumet.getAgents().iterator(); agentIterator.hasNext(); )
    {
      Agent agent = (Agent) agentIterator.next();
      agentListModel.add( agent.getId() );
    }
    agentField.setSelectedItem( parent.getApplicationServer().getAgent() );
    // update the jee application server update require restart field
    if ( parent.getApplicationServer().isUpdateRequireRestart() )
    {
      updateRequireRestartField.setSelectedIndex( 0 );
    }
    else
    {
      updateRequireRestartField.setSelectedIndex( 1 );
    }
    // update the jee application server update require caches cleaning field
    if ( parent.getApplicationServer().isUpdateRequireCacheCleaning() )
    {
      updateRequireCachesClean.setSelectedIndex( 0 );
    }
    else
    {
      updateRequireCachesClean.setSelectedIndex( 1 );
    }
    // update the use jmx stop field
    if ( parent.getApplicationServer().isUsejmxstop() )
    {
      stopUsingJmx.setSelectedIndex( 0 );
    }
    else
    {
      stopUsingJmx.setSelectedIndex( 1 );
    }
    // update the startup command area
    startupCommandArea.setText( parent.getApplicationServer().getStartupcommand() );
    // update the shutdown command area
    shutdownCommandArea.setText( parent.getApplicationServer().getShutdowncommand() );
  }

  public TextField getNameField()
  {
    return this.nameField;
  }

  public SelectField getActiveField()
  {
    return this.activeField;
  }

  public SelectField getBlockerField()
  {
    return this.blockerField;
  }

  public SelectField getTypeField()
  {
    return this.typeField;
  }

  public TextField getJmxField()
  {
    return this.jmxField;
  }

  public TextField getAdminUserField()
  {
    return this.adminUserField;
  }

  public PasswordField getAdminPasswordField()
  {
    return this.adminPasswordField;
  }

  public PasswordField getAdminConfirmPasswordField()
  {
    return this.adminConfirmPasswordField;
  }

  public SelectField getUpdateRequireRestartField()
  {
    return this.updateRequireRestartField;
  }

  public SelectField getUpdateRequireCachesCleanField()
  {
    return this.updateRequireCachesClean;
  }

  public SelectField getStopUsingJmxField()
  {
    return this.stopUsingJmx;
  }

  public TextArea getStartupCommandArea()
  {
    return this.startupCommandArea;
  }

  public TextArea getShutdownCommandArea()
  {
    return this.shutdownCommandArea;
  }

  public SelectField getAgentField()
  {
    return this.agentField;
  }

}
