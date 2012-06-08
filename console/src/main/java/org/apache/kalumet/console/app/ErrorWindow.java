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
import nextapp.echo2.app.WindowPane;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;

/**
 * Error window.
 */
public class ErrorWindow
  extends WindowPane
{

  /**
   * Create a new <code>WindowPane</code> with the error message and the
   * exception stack trace.
   *
   * @param message the error message to display.
   * @param details the error detailed message.
   */
  public ErrorWindow( String message, String details )
  {
    super();

    setTitle( message );
    setStyleName( "error" );
    setIcon( Styles.EXCLAMATION );
    setModal( true );

    // define the split pane containing control row
    SplitPane splitPane = new SplitPane( SplitPane.ORIENTATION_VERTICAL_BOTTOM_TOP, new Extent( 32 ) );
    add( splitPane );

    // define the control row
    Row controlRow = new Row();
    controlRow.setStyleName( "control" );
    splitPane.add( controlRow );

    // define the close button
    Button closeButton = new Button( Messages.getString( "close" ), Styles.CROSS );
    closeButton.setStyleName( "control" );
    closeButton.addActionListener( new ActionListener()
    {
      public void actionPerformed( ActionEvent e )
      {
        ErrorWindow.this.userClose();
      }
    } );
    controlRow.add( closeButton );

    // define a content pane
    ContentPane contentPane = new ContentPane();
    splitPane.add( contentPane );

    // define the details
    Label detailsLabel = new Label( details );
    detailsLabel.setStyleName( "error" );
    contentPane.add( detailsLabel );
  }

}