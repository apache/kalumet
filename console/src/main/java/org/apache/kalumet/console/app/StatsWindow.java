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
import nextapp.echo2.app.Column;
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.SplitPane;
import nextapp.echo2.app.WindowPane;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import org.apache.kalumet.console.configuration.ConfigurationManager;
import org.apache.kalumet.model.Kalumet;

/**
 * Stats window.
 */
public class StatsWindow
  extends WindowPane
{

  private Label agentsCount;

  private Label environmentsCount;

  private Label groupsCount;

  private Label usersCount;

  /**
   * Create a new <code>StatsWindow</code>.
   */
  public StatsWindow()
  {
    super();

    setTitle( Messages.getString( "stats" ) );
    setId( "statswindow" );
    setIcon( Styles.CHART_BAR );
    setStyleName( "stats" );
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
    refreshButton.addActionListener( new ActionListener()
    {
      public void actionPerformed( ActionEvent event )
      {
        update();
      }
    } );
    controlRow.add( refreshButton );
    // add the close button
    Button closeButton = new Button( Messages.getString( "close" ), Styles.CROSS );
    closeButton.addActionListener( new ActionListener()
    {
      public void actionPerformed( ActionEvent event )
      {
        userClose();
      }
    } );
    closeButton.setStyleName( "control" );
    controlRow.add( closeButton );

    // add the content pane
    Column content = new Column();
    content.setStyleName( "stats" );
    splitPane.add( content );

    // the agents count
    Row agentsCountRow = new Row();
    content.add( agentsCountRow );
    Label agentsCountLabel = new Label( Messages.getString( "agents.count" ) + ": " );
    agentsCountLabel.setStyleName( "default" );
    agentsCountRow.add( agentsCountLabel );
    agentsCount = new Label( " " );
    agentsCount.setStyleName( "default" );
    agentsCountRow.add( agentsCount );

    // the environments count
    Row environmentsCountRow = new Row();
    content.add( environmentsCountRow );
    Label environmentsCountLabel = new Label( Messages.getString( "environments.count" ) + ": " );
    environmentsCountLabel.setStyleName( "default" );
    environmentsCountRow.add( environmentsCountLabel );
    environmentsCount = new Label( " " );
    environmentsCount.setStyleName( "default" );
    environmentsCountRow.add( environmentsCount );

    // the groups count
    Row groupsCountRow = new Row();
    content.add( groupsCountRow );
    Label groupsCountLabel = new Label( Messages.getString( "groups.count" ) + ": " );
    groupsCountLabel.setStyleName( "default" );
    groupsCountRow.add( groupsCountLabel );
    groupsCount = new Label( " " );
    groupsCount.setStyleName( "default" );
    groupsCountRow.add( groupsCount );

    // the users count
    Row usersCountRow = new Row();
    content.add( usersCountRow );
    Label usersCountLabel = new Label( Messages.getString( "users.count" ) + ": " );
    usersCountLabel.setStyleName( "default" );
    usersCountRow.add( usersCountLabel );
    usersCount = new Label( " " );
    usersCount.setStyleName( "default" );
    usersCountRow.add( usersCount );

    // the reporting buttons
    Row agentMapRow = new Row();
    content.add( agentMapRow );
    Button agentMap = new Button( Messages.getString( "stats.agents" ), Styles.COG );
    agentMapRow.add( agentMap );

    Row physicalMapRow = new Row();
    content.add( physicalMapRow );
    Button physicalMap = new Button( Messages.getString( "stats.physical" ), Styles.COMPUTER );
    physicalMapRow.add( physicalMap );

    Row middlewareMapRow = new Row();
    content.add( middlewareMapRow );
    Button middlewareMap = new Button( Messages.getString( "stats.middlewares" ), Styles.BUILDING );
    middlewareMapRow.add( middlewareMap );

    update();
  }

  /**
   * Update the window content.
   */
  public void update()
  {
    // parse Kalumet configuration
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
    agentsCount.setText( new Integer( kalumet.getAgents().size() ).toString() );
    environmentsCount.setText( new Integer( kalumet.getEnvironments().size() ).toString() );
    groupsCount.setText( new Integer( kalumet.getSecurity().getGroups().size() ).toString() );
    usersCount.setText( new Integer( kalumet.getSecurity().getUsers().size() ).toString() );
  }

}
