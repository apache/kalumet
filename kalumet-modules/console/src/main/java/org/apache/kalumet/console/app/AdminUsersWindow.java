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
import nextapp.echo2.app.Grid;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.SplitPane;
import nextapp.echo2.app.WindowPane;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import org.apache.kalumet.console.configuration.ConfigurationManager;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.model.User;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Admin users window.
 *
 * @author onofre
 */
public class AdminUsersWindow
    extends WindowPane
{

    // attributes
    private List users;

    private Grid usersGrid;

    // close action listener
    private ActionListener closeActionListener = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            AdminUsersWindow.this.userClose();
        }
    };

    // refresh action listener
    private ActionListener refreshActionListener = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
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
            users = kalumet.getSecurity().getUsers();
            update();
            KalumetConsoleApplication.getApplication().getLogPane().addInfo(
                Messages.getString( "users" ) + " " + Messages.getString( "reloaded" ) );
        }
    };

    // delete action listener
    private ActionListener deleteActionListener = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            if ( event.getActionCommand().equals( "admin" ) )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "users.warn.admin" ) );
                return;
            }
            final String userId = event.getActionCommand();
            // display confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
                new ConfirmWindow( new ActionListener()
                {
                    public void actionPerformed( ActionEvent event )
                    {
                        User userToRemove = null;
                        for ( Iterator userIterator = users.iterator(); userIterator.hasNext(); )
                        {
                            User current = (User) userIterator.next();
                            if ( current.getId().equals( userId ) )
                            {
                                userToRemove = current;
                                break;
                            }
                        }
                        users.remove( userToRemove );
                        update();
                    }
                } ) );
        }
    };

    // save action listener
    private ActionListener saveActionListener = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
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
            // update the users list in Kalumet
            kalumet.getSecurity().setUsers( (LinkedList) users );
            // write the XML file
            try
            {
                ConfigurationManager.writeStore( kalumet );
            }
            catch ( Exception e )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addError(
                    Messages.getString( "db.write" ) + ": " + e.getMessage() );
                return;
            }
            KalumetConsoleApplication.getApplication().getLogPane().addConfirm( Messages.getString( "users.saved" ) );
        }
    };

    // edit action listener
    private ActionListener editActionListener = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            if ( KalumetConsoleApplication.getApplication().getDefaultWindow().getComponent(
                "user_" + event.getActionCommand() ) == null )
            {
                KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
                    new AdminUserWindow( AdminUsersWindow.this, event.getActionCommand() ) );
            }
        }
    };

    // add action listener
    private ActionListener addActionListener = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
                new AdminUserWindow( AdminUsersWindow.this, null ) );
        }
    };

    /**
     * Create a new <code>WindowPane</code>.
     */
    public AdminUsersWindow()
    {
        super();

        // check if the user that try to access this window is the admin
        if ( !KalumetConsoleApplication.getApplication().getUserid().equals( "admin" ) )
        {
            KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                Messages.getString( "users.warn.restricted" ) );
            this.userClose();
            return;
        }

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
            this.userClose();
            return;
        }
        this.users = kalumet.getSecurity().getUsers();
        Collections.sort( this.users );

        setTitle( Messages.getString( "users" ) );
        setStyleName( "users" );
        setIcon( Styles.USER );
        setId( "userswindow" );
        setModal( false );
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
        refreshButton.addActionListener( refreshActionListener );
        controlRow.add( refreshButton );
        // add the save button
        Button saveButton = new Button( Messages.getString( "save" ), Styles.DATABASE_SAVE );
        saveButton.setStyleName( "control" );
        saveButton.addActionListener( saveActionListener );
        controlRow.add( saveButton );
        // add the close button
        Button closeButton = new Button( Messages.getString( "close" ), Styles.CROSS );
        closeButton.setStyleName( "control" );
        closeButton.addActionListener( closeActionListener );
        controlRow.add( closeButton );

        // add the column main pane
        Column content = new Column();
        content.setStyleName( "users" );
        splitPane.add( content );

        // add button
        Row row = new Row();
        content.add( row );
        Button addButton = new Button( Messages.getString( "user.add" ), Styles.USER_ADD );
        addButton.addActionListener( addActionListener );
        row.add( addButton );

        // add the users list grid
        usersGrid = new Grid( 4 );
        usersGrid.setStyleName( "border.grid" );
        usersGrid.setColumnWidth( 0, new Extent( 18, Extent.PX ) );
        usersGrid.setColumnWidth( 1, new Extent( 33, Extent.PERCENT ) );
        usersGrid.setColumnWidth( 2, new Extent( 33, Extent.PERCENT ) );
        usersGrid.setColumnWidth( 3, new Extent( 33, Extent.PERCENT ) );
        content.add( usersGrid );

        // update users grid
        update();
    }

    protected void update()
    {
        // delete all users grid child
        usersGrid.removeAll();

        // add grid headers
        Label userActionHeader = new Label( " " );
        userActionHeader.setStyleName( "grid.header" );
        usersGrid.add( userActionHeader );
        Label userIdHeader = new Label( Messages.getString( "id" ) );
        userIdHeader.setStyleName( "grid.header" );
        usersGrid.add( userIdHeader );
        Label userNameHeader = new Label( Messages.getString( "name" ) );
        userNameHeader.setStyleName( "grid.header" );
        usersGrid.add( userNameHeader );
        Label userEmailHeader = new Label( Messages.getString( "email" ) );
        userEmailHeader.setStyleName( "grid.header" );
        usersGrid.add( userEmailHeader );

        // add users
        for ( Iterator userIterator = users.iterator(); userIterator.hasNext(); )
        {
            User current = (User) userIterator.next();
            // action row with user id
            Row row = new Row();
            row.setStyleName( "grid.cell" );
            usersGrid.add( row );
            Button deleteButton = new Button( Styles.USER_DELETE );
            deleteButton.addActionListener( deleteActionListener );
            deleteButton.setActionCommand( current.getId() );
            deleteButton.setToolTipText( Messages.getString( "delete" ) );
            row.add( deleteButton );
            // id
            Button userId = new Button( current.getId() );
            userId.addActionListener( editActionListener );
            userId.setActionCommand( current.getId() );
            userId.setStyleName( "default" );
            usersGrid.add( userId );
            // user name
            Label userNameLabel = new Label( current.getName() );
            userNameLabel.setStyleName( "default" );
            usersGrid.add( userNameLabel );
            // user email
            Label userEmailLabel = new Label( current.getEmail() );
            userEmailLabel.setStyleName( "default" );
            usersGrid.add( userEmailLabel );
        }
    }

    /**
     * Get the users list.
     *
     * @return the users list.
     */
    protected List getUsers()
    {
        return this.users;
    }

}
