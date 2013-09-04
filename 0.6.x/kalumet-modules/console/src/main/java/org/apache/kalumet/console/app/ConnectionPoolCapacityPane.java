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
import nextapp.echo2.app.Label;
import nextapp.echo2.app.TextField;

/**
 * JDBC connection pool capacity tab pane.
 */
public class ConnectionPoolCapacityPane
  extends ContentPane
{

  private ConnectionPoolWindow parent;

  private TextField initialField;

  private TextField maximalField;

  private TextField incrementField;

  /**
   * Create a new <code>JDBCConnectionPoolCapacityTabPane</code>.
   *
   * @param parent the parent <code>JDBCConnectionPoolWindow</code>.
   */
  public ConnectionPoolCapacityPane( ConnectionPoolWindow parent )
  {
    super();
    setStyleName( "tab.content" );

    // update parent
    this.parent = parent;

    // add the capacity layout grid
    Grid layout = new Grid( 2 );
    layout.setStyleName( "default" );
    layout.setColumnWidth( 0, new Extent( 20, Extent.PERCENT ) );
    layout.setColumnWidth( 1, new Extent( 80, Extent.PERCENT ) );
    add( layout );

    // add the initial field
    Label initialLabel = new Label( Messages.getString( "initial" ) );
    initialLabel.setStyleName( "grid.cell" );
    layout.add( initialLabel );
    initialField = new TextField();
    initialField.setStyleName( "default" );
    initialField.setWidth( new Extent( 10, Extent.EX ) );
    layout.add( initialField );

    // add the maximal field
    Label maximalLabel = new Label( Messages.getString( "maximal" ) );
    maximalLabel.setStyleName( "grid.cell" );
    layout.add( maximalLabel );
    maximalField = new TextField();
    maximalField.setStyleName( "default" );
    maximalField.setWidth( new Extent( 10, Extent.EX ) );
    layout.add( maximalField );

    // add the increment field
    Label incrementLabel = new Label( Messages.getString( "increment" ) );
    incrementLabel.setStyleName( "grid.cell" );
    layout.add( incrementLabel );
    incrementField = new TextField();
    incrementField.setStyleName( "default" );
    incrementField.setWidth( new Extent( 10, Extent.EX ) );
    layout.add( incrementField );

    // update the pane
    update();
  }

  /**
   * Update the pane
   */
  public void update()
  {
    // update the JDBC connection pool initial field
    initialField.setText( new Integer( parent.getConnectionPool().getInitial() ).toString() );
    // update the JDBC connection pool maximal field
    maximalField.setText( new Integer( parent.getConnectionPool().getMaximal() ).toString() );
    // update the JDBC connection pool increment field
    incrementField.setText( new Integer( parent.getConnectionPool().getIncrement() ).toString() );
  }

  public TextField getInitialField()
  {
    return this.initialField;
  }

  public TextField getMaximalField()
  {
    return this.maximalField;
  }

  public TextField getIncrementField()
  {
    return this.incrementField;
  }

}