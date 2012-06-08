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
import nextapp.echo2.app.ContentPane;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.extras.app.AccordionPane;
import nextapp.echo2.extras.app.layout.AccordionPaneLayoutData;
import org.apache.kalumet.console.configuration.ConfigurationManager;
import org.apache.kalumet.model.Environment;
import org.apache.kalumet.model.Kalumet;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Display the list of environments organized by groups.
 */
public class EnvironmentsPane
  extends ContentPane
{

  private AccordionPane mainPane;

  // edit
  private ActionListener edit = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      String environmentName = event.getActionCommand();
      if ( KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().getComponent(
        "environmentwindow_" + environmentName ) == null )
      {
        KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
          new EnvironmentWindow( environmentName ) );
      }
    }
  };

  /**
   * Create a new environments <code>AccordionPane</code>.
   */
  public EnvironmentsPane()
  {
    super();
    mainPane = new AccordionPane();
    mainPane.setStyleName( "environments" );
    add( mainPane );
    update();
  }

  /**
   * Update the pane.
   */
  public void update()
  {
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

    // get user environments by groups
    Map userEnvironments =
      kalumet.getUserEnvironmentsByGroups( KalumetConsoleApplication.getApplication().getUserid() );

    // remove all
    mainPane.removeAll();

    // render environment groups
    List groups = new LinkedList( userEnvironments.keySet() );
    Collections.sort( groups );
    for ( Iterator groupIterator = groups.iterator(); groupIterator.hasNext(); )
    {
      String group = (String) groupIterator.next();
      Column groupColumn = new Column();
      groupColumn.setStyleName( "environments" );
      AccordionPaneLayoutData layoutData = new AccordionPaneLayoutData();
      // define the layoutData as the column layout
      groupColumn.setLayoutData( layoutData );
      // display the group
      layoutData.setTitle( group );
      // add the column to the pane
      mainPane.add( groupColumn );
      List environments = (List) userEnvironments.get( group );
      Collections.sort( environments );
      for ( Iterator environmentIterator = environments.iterator(); environmentIterator.hasNext(); )
      {
        Environment environment = (Environment) environmentIterator.next();
        Button environmentButton = new Button( environment.getName() );
        environmentButton.setStyleName( "default" );
        environmentButton.setActionCommand( environment.getName() );
        environmentButton.addActionListener( edit );
        groupColumn.add( environmentButton );
      }
    }
  }

}