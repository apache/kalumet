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
import nextapp.echo2.app.Grid;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.SplitPane;
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import org.apache.kalumet.console.configuration.ConfigurationManager;
import org.apache.kalumet.model.Agent;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.ws.client.FileClient;
import org.apache.kalumet.ws.client.SimplifiedFileObject;

import java.text.SimpleDateFormat;

/**
 * File browser pane.
 */
public class FileBrowserPane
    extends ContentPane
{

    private EnvironmentWindow parent;

    private TextField pathField;

    private Grid grid;

    // view
    private ActionListener view = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            String path = event.getActionCommand();
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
                new ViewFileWindow( path, parent.getEnvironment().getAgent() ) );
        }
    };

    // browse
    private ActionListener browse = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            if ( event != null && event.getActionCommand() != null )
            {
                pathField.setText( event.getActionCommand() );
            }

            update();
        }
    };

    /**
     * Create a new <code>FileBrowserPane</code>.
     *
     * @param parent the parent <code>EnvironmentWindow</code>.
     */
    public FileBrowserPane( EnvironmentWindow parent )
    {
        super();
        this.setStyleName( "tab.content" );

        // update the parent
        this.parent = parent;

        SplitPane content = new SplitPane( SplitPane.ORIENTATION_VERTICAL_TOP_BOTTOM, new Extent( 20 ) );
        add( content );

        Row browseRow = new Row();
        browseRow.setCellSpacing( new Extent( 2 ) );
        browseRow.setInsets( new Insets( 2 ) );
        content.add( browseRow );
        pathField = new TextField();
        pathField.setStyleName( "default" );
        pathField.setText( "/" );
        pathField.addActionListener( browse );
        browseRow.add( pathField );
        Button browseButton = new Button( Messages.getString( "browse" ), Styles.ACCEPT );
        browseButton.addActionListener( browse );
        browseRow.add( browseButton );

        grid = new Grid( 5 );
        grid.setStyleName( "border.grid" );
        grid.setColumnWidth( 0, new Extent( 50, Extent.PX ) );
        content.add( grid );

        // empty header
        // action header
        Label actionHeader = new Label( " " );
        actionHeader.setStyleName( "grid.header" );
        grid.add( actionHeader );
        // name header
        Label nameHeader = new Label( Messages.getString( "name" ) );
        nameHeader.setStyleName( "grid.header" );
        grid.add( nameHeader );
        // path header
        Label pathHeader = new Label( Messages.getString( "path" ) );
        pathHeader.setStyleName( "grid.header" );
        grid.add( pathHeader );
        // permission header
        Label fileModeHeader = new Label( Messages.getString( "size" ) );
        fileModeHeader.setStyleName( "grid.header" );
        grid.add( fileModeHeader );
        // modification data header
        Label fileModificationHeader = new Label( Messages.getString( "last.modification.date" ) );
        fileModificationHeader.setStyleName( "grid.header" );
        grid.add( fileModificationHeader );
    }

    /**
     * Update the pane.
     */
    public void update()
    {
        // check path file value
        String path = pathField.getText();
        if ( path == null || path.trim().length() < 1 )
        {
            return;
        }

        // cleanup the grid
        grid.removeAll();
        // action header
        Label actionHeader = new Label( " " );
        actionHeader.setStyleName( "grid.header" );
        grid.add( actionHeader );
        // name header
        Label nameHeader = new Label( Messages.getString( "name" ) );
        nameHeader.setStyleName( "grid.header" );
        grid.add( nameHeader );
        // path header
        Label pathHeader = new Label( Messages.getString( "path" ) );
        pathHeader.setStyleName( "grid.header" );
        grid.add( pathHeader );
        // permission header
        Label fileModeHeader = new Label( Messages.getString( "size" ) );
        fileModeHeader.setStyleName( "grid.header" );
        grid.add( fileModeHeader );
        // modification data header
        Label fileModificationHeader = new Label( Messages.getString( "last.modification.date" ) );
        fileModificationHeader.setStyleName( "grid.header" );
        grid.add( fileModificationHeader );

        try
        {
            // load Kalumet configuration
            Kalumet kalumet = ConfigurationManager.loadStore();
            // looking for the agent
            Agent agent = kalumet.getAgent( parent.getEnvironment().getAgent() );
            if ( agent == null )
            {
                throw new IllegalArgumentException( "agent not found." );
            }
            // call the WebService
            FileClient client = new FileClient( agent.getHostname(), agent.getPort() );
            SimplifiedFileObject[] files = client.browse( path );
            for ( int i = 0; i < files.length; i++ )
            {
                SimplifiedFileObject file = files[i];
                // actions
                Row actionRow = new Row();
                if ( file.isFile() )
                {
                    Button viewButton = new Button( Styles.INFORMATION );
                    viewButton.setToolTipText( Messages.getString( "view" ) );
                    viewButton.setActionCommand( file.getPath() );
                    viewButton.addActionListener( view );
                    actionRow.add( viewButton );
                }
                else
                {
                    Button browseButton = new Button( Styles.FOLDER_EXPLORE );
                    browseButton.setToolTipText( Messages.getString( "browse" ) );
                    browseButton.setActionCommand( file.getPath() );
                    browseButton.addActionListener( browse );
                    actionRow.add( browseButton );
                }
                grid.add( actionRow );
                // file name
                Button fileName = new Button( file.getName() );
                fileName.setActionCommand( file.getPath() );
                if ( file.isFile() )
                {
                    fileName.addActionListener( view );
                }
                else
                {
                    fileName.addActionListener( browse );
                }
                grid.add( fileName );
                // file path
                Label filePath = new Label( file.getPath() );
                grid.add( filePath );
                // file size
                Label fileSize = new Label( file.getSize() + " bytes" );
                grid.add( fileSize );
                // file date
                SimpleDateFormat dateFormatter = new SimpleDateFormat( "MM/dd/yyyy HH:mm" );
                Label fileDate = new Label( dateFormatter.format( file.getLastModificationDate() ) );
                grid.add( fileDate );
            }
        }
        catch ( Exception e )
        {
            KalumetConsoleApplication.getApplication().getLogPane().addError(
                "Browsing " + path + " error: " + e.getMessage(), parent.getEnvironmentName() );
        }
    }

}