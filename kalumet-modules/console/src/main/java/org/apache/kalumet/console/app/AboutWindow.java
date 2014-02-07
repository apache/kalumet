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

/**
 * About window.
 */
public class AboutWindow
    extends WindowPane
{

    /**
     * Create a new <code>AboutWindow</code>.
     */
    public AboutWindow()
    {
        super();

        setTitle( Messages.getString( "about" ) );
        setIcon( Styles.INFORMATION );
        setStyleName( "about" );
        setId( "aboutwindow" );
        setModal( true );
        setDefaultCloseOperation( WindowPane.DISPOSE_ON_CLOSE );

        Label label;

        // split pane to put control
        SplitPane splitPane = new SplitPane( SplitPane.ORIENTATION_VERTICAL_BOTTOM_TOP, new Extent( 32 ) );
        add( splitPane );

        // control row
        Row controlRow = new Row();
        controlRow.setStyleName( "control" );
        splitPane.add( controlRow );

        // close button
        Button closeButton = new Button( Messages.getString( "close" ), Styles.CROSS );
        closeButton.setStyleName( "control" );
        closeButton.addActionListener( new ActionListener()
        {

            private static final long serialVersionUID = 8624164259974769878L;

            public void actionPerformed( ActionEvent e )
            {
                AboutWindow.this.userClose();
            }
        } );
        controlRow.add( closeButton );

        // define a column to store the several labels
        Column column = new Column();
        column.setStyleName( "about" );
        column.setCellSpacing( new Extent( 5 ) );

        // define the title label
        label = new Label( Messages.getString( "kalumet.console" ) );
        label.setStyleName( "about.title" );
        column.add( label );

        // define version label if possible
        Package p = Package.getPackage( "org.apache.kalumet.console" );
        if ( p != null && p.getImplementationVersion() != null )
        {
            label = new Label( "Version: " + p.getImplementationVersion() );
        }
        else
        {
            label = new Label( "" );
        }
        label.setStyleName( "default" );
        column.add( label );

        // define the jvm label
        label = new Label(
            "JVM: " + System.getProperty( "java.vm.vendor" ) + " " + System.getProperty( "java.vm.name" ) + " "
                + System.getProperty( "java.vm.version" ) );
        label.setStyleName( "default" );
        column.add( label );

        // define the os label
        label = new Label( "Host: " + System.getProperty( "os.arch" ) + " " + System.getProperty( "os.name" ) + " "
                               + System.getProperty( "os.version" ) );
        label.setStyleName( "default" );
        column.add( label );

        // define the copyright label
        label = new Label( "Apache 2.0 License" );
        label.setStyleName( "default" );
        column.add( label );

        // add the column to the split pane
        splitPane.add( column );

    }

}
