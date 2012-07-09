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
import nextapp.echo2.app.SplitPane;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Log <code>ContentPane</code>.
 */
public class LogPane
  extends ContentPane
{

  private Grid mainGrid;

  /**
   * Create a new <code>LogPane</code>.
   */
  public LogPane()
  {
    super();

    // define a title pane
    SplitPane titlePane = new SplitPane( SplitPane.ORIENTATION_VERTICAL_TOP_BOTTOM, new Extent( 20 ) );
    titlePane.setResizable( false );
    add( titlePane );
    Label titleLabel = new Label( Messages.getString( "log" ) );
    titleLabel.setStyleName( "log" );
    titlePane.add( titleLabel );

    // define the main pane
    mainGrid = new Grid( 4 );
    mainGrid.setStyleName( "log" );
    mainGrid.setColumnWidth( 0, new Extent( 10, Extent.PX ) );
    mainGrid.setColumnWidth( 1, new Extent( 10, Extent.PERCENT ) );
    mainGrid.setColumnWidth( 2, new Extent( 10, Extent.PERCENT ) );
    mainGrid.setColumnWidth( 3, new Extent( 80, Extent.PERCENT ) );
    titlePane.add( mainGrid );
  }

  /**
   * Add a generic event into the log pane.
   *
   * @param message the event message.
   * @param scope   the event scope.
   */
  private void addEvent( String message, String scope )
  {
    SimpleDateFormat dateFormatter = new SimpleDateFormat( "MM/dd/yyyy HH:mm" );
    Label dateLabel = new Label( dateFormatter.format( new Date() ) );
    dateLabel.setStyleName( "default" );
    mainGrid.add( dateLabel, 1 );
    Label scopeLabel = new Label( scope );
    scopeLabel.setStyleName( "default" );
    mainGrid.add( scopeLabel, 2 );
    Label messageLabel = new Label( message );
    messageLabel.setStyleName( "default" );
    mainGrid.add( messageLabel, 3 );
  }

  /**
   * Add a confirmation message on a given scope.
   *
   * @param message the confirmation message.
   * @param scope   the confirmation scope.
   */
  public void addConfirm( String message, String scope )
  {
    Label iconLabel = new Label( Styles.ACCEPT );
    mainGrid.add( iconLabel, 0 );
    addEvent( message, scope );
  }

  /**
   * Add a confirmation message.
   *
   * @param message
   */
  public void addConfirm( String message )
  {
    this.addConfirm( message, "" );
  }

  /**
   * Add an info into the log pane on a given scope.
   *
   * @param message the info message.
   * @param scope   the info scope.
   */
  public void addInfo( String message, String scope )
  {
    Label iconLabel = new Label( Styles.INFORMATION );
    mainGrid.add( iconLabel, 0 );
    addEvent( message, scope );
  }

  /**
   * Add an info into the log pane.
   *
   * @param message the info message.
   */
  public void addInfo( String message )
  {
    this.addInfo( message, "" );
  }

  /**
   * Add a warning into the log pane on a given scope.
   *
   * @param message the warning message.
   * @param scope   the warning scope.
   */
  public void addWarning( String message, String scope )
  {
    Label iconLabel = new Label( Styles.ERROR );
    mainGrid.add( iconLabel, 0 );
    addEvent( message, scope );
  }

  /**
   * Add a warning into the log pane.
   *
   * @param message the warning message.
   */
  public void addWarning( String message )
  {
    this.addWarning( message, "" );
  }

  /**
   * Add an error into the log pane on a given scope.
   *
   * @param message the error message.
   * @param scope   the error scope.
   */
  public void addError( String message, String scope )
  {
    Label iconLabel = new Label( Styles.EXCLAMATION );
    mainGrid.add( iconLabel, 0 );
    addEvent( message, scope );
  }

  /**
   * Add an error into the log pane.
   *
   * @param message the error message.
   */
  public void addError( String message )
  {
    this.addError( message, "" );
  }

}