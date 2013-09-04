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
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.SplitPane;
import nextapp.echo2.app.TextArea;
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.app.layout.SplitPaneLayoutData;
import org.apache.kalumet.console.configuration.ConfigurationManager;
import org.apache.kalumet.model.Agent;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.ws.client.CommandClient;

public class ShellPane
  extends ContentPane
{

  private EnvironmentWindow parent;

  private TextArea echoArea;

  private TextField commandField;

  // action listeners
  // launch
  private ActionListener launch = new ActionListener()
  {

    // system command execution timeout
    private static final int EXECUTION_TIMEOUT_SECONDS = 120;

    public void actionPerformed( ActionEvent event )
    {
      // check if the user has the lock
      if ( !parent.getEnvironment().getLock().equals( KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "environment.locked" ) );
        return;
      }
      String command = commandField.getText();
      if ( command == null || command.trim().length() < 1 )
      {
        return;
      }
      try
      {
        // load Kalumet configuration
        Kalumet kalumet = ConfigurationManager.loadStore();
        // looking for the Kalumet agent
        Agent agent = kalumet.getAgent( parent.getEnvironment().getAgent() );
        // check if the agent exist
        if ( agent == null )
        {
          KalumetConsoleApplication.getApplication().getLogPane().addWarning(
            Messages.getString( "environment.shell.noagent" ) );
          return;
        }
        // call the webservice
        CommandClient client = new CommandClient( agent.getHostname(), agent.getPort() );
        String result = client.execute( command );
        // echo the command
        StringBuffer buffer = new StringBuffer();
        buffer.append( echoArea.getText() );
        buffer.append( "$> " );
        buffer.append( command );
        buffer.append( "\n" );
        buffer.append( result );
        buffer.append( "\n" );
        // update the main area
        echoArea.setText( buffer.toString() );
        // update the scroll
        echoArea.setVerticalScroll( new Extent( buffer.length() + 10 ) );
        // erase the main command field
        commandField.setText( "" );
      }
      catch ( Exception exception )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "shell.warn.command" ) + ": " + exception.getMessage() );
        return;
      }
    }

  };

  /**
   * Create a new <code>ShellPane</code>
   *
   * @param parent the parent <code>EnvironmentWindow</code>.
   */
  public ShellPane( EnvironmentWindow parent )
  {
    super();
    setStyleName( "tab.content" );

    // update parent
    this.parent = parent;

    // split pane content
    SplitPane content = new SplitPane( SplitPane.ORIENTATION_VERTICAL_BOTTOM_TOP, new Extent( 25 ) );
    this.add( content );

    // add the main command field and launch button
    // command row container
    Row commandRow = new Row();
    SplitPaneLayoutData layoutData = new SplitPaneLayoutData();
    layoutData.setInsets( new Insets( 4 ) );
    commandRow.setLayoutData( layoutData );
    commandRow.setCellSpacing( new Extent( 2 ) );
    commandRow.setInsets( new Insets( 2 ) );
    content.add( commandRow );
    commandField = new TextField();
    commandField.setStyleName( "default" );
    commandField.addActionListener( launch );
    commandRow.add( commandField );
    Button launcherButton = new Button( Messages.getString( "execute" ), Styles.ACCEPT );
    launcherButton.addActionListener( launch );
    commandRow.add( launcherButton );

    // add the main text area
    echoArea = new TextArea();
    echoArea.setStyleName( "default" );
    echoArea.setLayoutData( layoutData );
    echoArea.setEnabled( false );
    echoArea.setWidth( new Extent( 100, Extent.PERCENT ) );
    echoArea.setHeight( new Extent( 98, Extent.PERCENT ) );
    content.add( echoArea );
  }

}
