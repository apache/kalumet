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
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.SplitPane;
import nextapp.echo2.app.TextArea;
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.WindowPane;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import org.apache.kalumet.FileManipulator;
import org.apache.kalumet.model.Archive;
import org.apache.kalumet.model.J2EEApplication;
import org.apache.kalumet.model.J2EEApplicationServer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;

/**
 * Environment home page window.
 */
public class HomePageWindow
  extends WindowPane
{

  private static final String TEMPLATE_LOCATION =
    "/org/apache/kalumet/console/app/resources/templates/environment-homepage.html";

  private EnvironmentWindow parent;

  private TextArea area;

  private TextField locationField;

  // refresh
  private ActionListener refresh = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      HomePageWindow.this.update();
    }
  };

  // close
  private ActionListener close = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      HomePageWindow.this.userClose();
    }
  };

  // copy
  private ActionListener copy = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // get the home page location
      String homePageLocation = locationField.getText();
      if ( homePageLocation == null || homePageLocation.trim().length() < 1 )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "homepage.warn.mandatory" ) );
        return;
      }
      FileManipulator fileManipulator = null;
      try
      {
        // init a file manipulator
        fileManipulator = new FileManipulator();
        // write the homepage area in the output stream
        PrintWriter writer = new PrintWriter( fileManipulator.write( homePageLocation ) );
        writer.print( area.getText() );
        writer.flush();
        writer.close();
      }
      catch ( Exception e )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "homepage.warn.error" ) + ": " + e.getMessage() );
        return;
      }
      finally
      {
        if ( fileManipulator != null )
        {
          fileManipulator.close();
        }
      }
      KalumetConsoleApplication.getApplication().getLogPane().addConfirm( Messages.getString( "homepage.generated" ) );
    }
  };

  /**
   * Create environment home page window.
   *
   * @param parent the parent <code>EnvironmentWindow</code>.
   */
  public HomePageWindow( EnvironmentWindow parent )
  {
    super();

    // update parent
    this.parent = parent;

    setTitle( parent.getEnvironmentName() + " " + Messages.getString( "homepage" ) );
    setId( "homepagewindow_" + parent.getEnvironmentName() );
    setStyleName( "default" );
    setWidth( new Extent( 800, Extent.PX ) );
    setHeight( new Extent( 600, Extent.PX ) );
    setModal( false );
    setDefaultCloseOperation( WindowPane.DISPOSE_ON_CLOSE );

    // create a split pane for the control button
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
    // add the close button
    Button closeButton = new Button( Messages.getString( "close" ), Styles.CROSS );
    closeButton.setStyleName( "control" );
    closeButton.addActionListener( close );
    controlRow.add( closeButton );

    // add split pane content
    SplitPane content = new SplitPane( SplitPane.ORIENTATION_VERTICAL_BOTTOM_TOP, new Extent( 20 ) );
    splitPane.add( content );

    // add the row location
    Row locationRow = new Row();
    locationRow.setInsets( new Insets( 2 ) );
    locationRow.setCellSpacing( new Extent( 2 ) );
    content.add( locationRow );
    // add the copy location label
    Label copyLocationLabel = new Label( Messages.getString( "location" ) );
    copyLocationLabel.setStyleName( "default" );
    locationRow.add( copyLocationLabel );
    // add the environment homepage location field
    locationField = new TextField();
    locationField.setStyleName( "default" );
    locationField.addActionListener( copy );
    locationRow.add( locationField );
    // add the copy button
    Button copyButton = new Button( Messages.getString( "copy" ), Styles.ACCEPT );
    copyButton.addActionListener( copy );
    locationRow.add( copyButton );

    // add the text area
    area = new TextArea();
    area.setStyleName( "default" );
    area.setWidth( new Extent( 98, Extent.PERCENT ) );
    area.setHeight( new Extent( 98, Extent.PERCENT ) );
    content.add( area );

    // update the pane
    update();
  }

  /**
   * Update the pane.
   */
  protected void update()
  {
    // generate replace strings
    String applications = null;
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
          applications =
            applications + "&nbsp; &nbsp; <a href=\"" + archive.getContext() + "\">" + archive.getName() + "</a>";
        }
      }
    }
    // change the values in the template
    BufferedReader templateBufferedReader = new BufferedReader(
      new InputStreamReader( HomePageWindow.class.getResourceAsStream( HomePageWindow.TEMPLATE_LOCATION ) ) );
    StringWriter writer = new StringWriter();
    BufferedWriter buffer = new BufferedWriter( writer );
    try
    {
      String templateLine = templateBufferedReader.readLine();
      while ( templateLine != null )
      {
        buffer.write( templateLine );
        buffer.newLine();
        templateLine = templateBufferedReader.readLine();
      }
      buffer.flush();
    }
    catch ( Exception e )
    {
      KalumetConsoleApplication.getApplication().getLogPane().addWarning(
        Messages.getString( "homepage.error" ) + ": " + e.getMessage() );
      return;
    }
    String template = writer.toString();
    template = template.replaceAll( "ENVIRONMENT_NAME", parent.getEnvironmentName() );
    if ( applications != null )
    {
      template = template.replaceAll( "ENVIRONMENT_APPLICATIONS", applications );
    }
    if ( parent.getEnvironment().getWeblinks() != null )
    {
      template = template.replaceAll( "ENVIRONMENT_WEBLINKS", parent.getEnvironment().getWeblinks() );
    }
    if ( parent.getEnvironment().getNotes() != null )
    {
      template = template.replaceAll( "ENVIRONMENT_NOTES", parent.getEnvironment().getNotes() );
    }
    // update the environment homepage area
    area.setText( template );
  }

}