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
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.SplitPane;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;

/**
 * Main screen <code>ContentPane</code>.
 */
public class MainScreen
  extends ContentPane
{

  // general constants
  public static String[] LABELS = new String[]{ Messages.getString( "yes" ), Messages.getString( "no" ) };

  /**
   * Create a new <code>MainScreen</code>.
   */
  public MainScreen()
  {
    super();

    // define the title pane
    SplitPane titlePane = new SplitPane( SplitPane.ORIENTATION_VERTICAL_TOP_BOTTOM, new Extent( 30, Extent.PX ) );
    titlePane.setResizable( false );
    add( titlePane );
    Label titleLabel = new Label( Messages.getString( "kalumet.console" ) );
    titleLabel.setStyleName( "title" );
    titlePane.add( titleLabel );

    // create the menu pane
    SplitPane menuPane = new SplitPane( SplitPane.ORIENTATION_VERTICAL, new Extent( 26 ) );
    menuPane.setResizable( false );
    titlePane.add( menuPane );
    Row menuRow = new Row();
    menuRow.setStyleName( "menu" );
    menuPane.add( menuRow );
    // new environment menu option
    Button newEnvironmentButton = new Button( Messages.getString( "environment.add" ), Styles.APPLICATION_ADD );
    newEnvironmentButton.setStyleName( "default" );
    newEnvironmentButton.addActionListener( new ActionListener()
    {
      public void actionPerformed( ActionEvent event )
      {
        // display a new environment window
        KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add( new EnvironmentWindow( null ) );
      }
    } );
    menuRow.add( newEnvironmentButton );
    // refresh menu option
    Button refreshButton = new Button( Messages.getString( "reload" ), Styles.DATABASE_REFRESH );
    refreshButton.setStyleName( "default" );
    refreshButton.addActionListener( new ActionListener()
    {
      public void actionPerformed( ActionEvent event )
      {
        // refresh the environments pane
        KalumetConsoleApplication.getApplication().getLogPane().addInfo(
          Messages.getString( "configuration" ) + " " + Messages.getString( "reloaded" ) );
        KalumetConsoleApplication.getApplication().getEnvironmentsPane().update();
      }
    } );
    menuRow.add( refreshButton );
    // add admin menu options
    if ( KalumetConsoleApplication.getApplication().getUserid().equals( "admin" ) )
    {
      // configuration menu option
      Button configurationButton = new Button( Messages.getString( "configuration" ), Styles.COMPUTER_EDIT );
      configurationButton.setStyleName( "default" );
      configurationButton.addActionListener( new ActionListener()
      {
        public void actionPerformed( ActionEvent event )
        {
          // display the configuration window
          if ( KalumetConsoleApplication.getApplication().getDefaultWindow().getComponent( "configurationwindow" )
            == null )
          {
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
              new AdminConfigurationWindow() );
          }
        }
      } );
      menuRow.add( configurationButton );
      // agents menu option
      Button agentButton = new Button( Messages.getString( "agents" ), Styles.COG );
      agentButton.setStyleName( "default" );
      agentButton.addActionListener( new ActionListener()
      {
        public void actionPerformed( ActionEvent event )
        {
          // display the agents window
          if ( KalumetConsoleApplication.getApplication().getDefaultWindow().getComponent( "agentswindow" ) == null )
          {
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add( new AdminAgentsWindow() );
          }
        }
      } );
      menuRow.add( agentButton );
      // users menu option
      Button usersButton = new Button( Messages.getString( "users" ), Styles.USER );
      usersButton.setStyleName( "default" );
      usersButton.addActionListener( new ActionListener()
      {
        public void actionPerformed( ActionEvent event )
        {
          // display the users window
          if ( KalumetConsoleApplication.getApplication().getDefaultWindow().getComponent( "userswindow" ) == null )
          {
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add( new AdminUsersWindow() );
          }
        }
      } );
      menuRow.add( usersButton );
      // groups menu option
      Button groupsButton = new Button( Messages.getString( "groups" ), Styles.GROUP );
      groupsButton.setStyleName( "default" );
      groupsButton.addActionListener( new ActionListener()
      {
        public void actionPerformed( ActionEvent event )
        {
          // display the groups window
          if ( KalumetConsoleApplication.getApplication().getDefaultWindow().getComponent( "groupswindow" ) == null )
          {
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add( new AdminGroupsWindow() );
          }
        }
      } );
      menuRow.add( groupsButton );
    }
    // stats menu option
    Button statsButton = new Button( Messages.getString( "stats" ), Styles.CHART_BAR );
    statsButton.setStyleName( "default" );
    statsButton.addActionListener( new ActionListener()
    {
      public void actionPerformed( ActionEvent event )
      {
        // display the stats window
        if ( KalumetConsoleApplication.getApplication().getDefaultWindow().getComponent( "statswindow" ) == null )
        {
          KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add( new StatsWindow() );
        }
      }
    } );
    menuRow.add( statsButton );
    // preference menu option
    Button preferenceButton = new Button( Messages.getString( "preferences" ), Styles.WRENCH );
    preferenceButton.setStyleName( "default" );
    preferenceButton.addActionListener( new ActionListener()
    {
      public void actionPerformed( ActionEvent event )
      {
        // display the preferences window
        if ( KalumetConsoleApplication.getApplication().getDefaultWindow().getComponent( "preferenceswindow" ) == null )
        {
          KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add( new PreferencesWindow() );
        }
      }
    } );
    menuRow.add( preferenceButton );
    // about menu option
    Button aboutButton = new Button( Messages.getString( "about" ), Styles.INFORMATION );
    aboutButton.setStyleName( "default" );
    aboutButton.addActionListener( new ActionListener()
    {
      public void actionPerformed( ActionEvent event )
      {
        // display the about window
        if ( KalumetConsoleApplication.getApplication().getDefaultWindow().getComponent( "aboutwindow" ) == null )
        {
          KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add( new AboutWindow() );
        }
      }
    } );
    menuRow.add( aboutButton );
    // disconnect menu option
    Button disconnectButton = new Button( Messages.getString( "disconnect" ), Styles.DISCONNECT );
    disconnectButton.setStyleName( "default" );
    disconnectButton.addActionListener( new ActionListener()
    {
      public void actionPerformed( ActionEvent event )
      {
        // disconnect the user
        KalumetConsoleApplication.getApplication().disconnect();
      }
    } );
    menuRow.add( disconnectButton );

    // create the split central pane
    SplitPane mainPane = new SplitPane( SplitPane.ORIENTATION_HORIZONTAL_LEFT_RIGHT, new Extent( 200, Extent.PX ) );
    mainPane.setStyleName( "default" );
    menuPane.add( mainPane );
    // add the left environments pane
    EnvironmentsPane environmentsPane = new EnvironmentsPane();
    KalumetConsoleApplication.getApplication().setEnvironmentsPane( environmentsPane );
    mainPane.add( environmentsPane );

    // add a split central pane
    SplitPane centralPane = new SplitPane( SplitPane.ORIENTATION_VERTICAL_BOTTOM_TOP, new Extent( 100, Extent.PX ) );
    centralPane.setStyleName( "default" );
    mainPane.add( centralPane );

    // add the log pane and register in the WAD application
    LogPane logPane = new LogPane();
    KalumetConsoleApplication.getApplication().setLogPane( logPane );
    centralPane.add( logPane );
    // add an info in the log pane
    logPane.addInfo( Messages.getString( "welcome" ) + " " + KalumetConsoleApplication.getApplication().getUserid() );

    // add the workspace pane
    ContentPane workspacePane = new ContentPane();
    workspacePane.setStyleName( "central" );
    centralPane.add( workspacePane );
  }

}