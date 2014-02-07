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
import nextapp.echo2.app.Grid;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.PasswordField;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.SplitPane;
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.WindowPane;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import org.apache.kalumet.model.User;

import java.util.Iterator;

/**
 * Admin user window.
 */
public class AdminUserWindow
    extends WindowPane
{

    // attributes
    private String userId;

    private User user = null;

    private AdminUsersWindow parent;

    private TextField userIdField;

    private TextField userNameField;

    private TextField userEmailField;

    private PasswordField userPasswordField;

    private PasswordField userConfirmPasswordField;

    // close action listener
    private ActionListener closeActionListener = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            AdminUserWindow.this.userClose();
        }
    };

    // refresh action listener
    private ActionListener refreshActionListener = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            for ( Iterator userIterator = parent.getUsers().iterator(); userIterator.hasNext(); )
            {
                User current = (User) userIterator.next();
                if ( current.getId().equals( userId ) )
                {
                    user = current;
                    break;
                }
            }
            if ( user == null )
            {
                user = new User();
            }
            update();
        }
    };

    // apply action listener
    private ActionListener applyActionListener = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            String userNewId = userIdField.getText().trim();
            String userName = userNameField.getText().trim();
            String userEmail = userEmailField.getText().trim();
            String userPassword = userPasswordField.getText().trim();
            String userConfirmPassword = userConfirmPasswordField.getText().trim();
            String userPasswordCrypted = null;

            // check fields
            // user id mandatory
            if ( userNewId.length() < 1 )
            {
                KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
                    new ErrorWindow( Messages.getString( "AdminUserWindow.Error.Save" ),
                                     Messages.getString( "AdminUserWindow.Error.EmptyUserId" ) ) );
                return;
            }
            // password must match confirm password is not empty
            if ( userPassword.length() > 0 && !userPassword.equals( userConfirmPassword ) )
            {
                KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
                    new ErrorWindow( Messages.getString( "AdminUserWindow.Error.Save" ),
                                     Messages.getString( "AdminUserWindow.Error.PasswordMatch" ) ) );
                return;
            }
            // crypt password
            if ( userPassword.length() > 0 )
            {
                try
                {
                    userPasswordCrypted = User.md5PasswordCrypt( userPassword );
                }
                catch ( Exception e )
                {
                    KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
                        new ErrorWindow( Messages.getString( "AdminUserWindow.Error.PasswordCrypt" ),
                                         e.getMessage() ) );
                    return;
                }
            }
            // if the admin change the user id or if it's a new user, check if the
            // id is not already used
            if ( userId == null || userId.trim().length() < 1 || ( ( userId != null ) && ( userId.trim().length() > 0 )
                && ( !userNewId.equals( userId ) ) ) )
            {
                for ( Iterator userIterator = parent.getUsers().iterator(); userIterator.hasNext(); )
                {
                    User current = (User) userIterator.next();
                    if ( current.getId().equals( userNewId ) )
                    {
                        KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
                            new ErrorWindow( Messages.getString( "AdminUserWindow.Error.Add" ),
                                             Messages.getString( "AdminUserWindow.Error.UserIdAlreadyExists" ) ) );
                        return;
                    }
                }
            }

            // update the current user
            user.setId( userNewId );
            user.setName( userName );
            user.setEmail( userEmail );
            if ( userPasswordCrypted != null )
            {
                user.setPassword( userPasswordCrypted );
            }
            if ( userId == null || userId.trim().length() < 1 )
            {
                // it's a new user
                parent.getUsers().add( user );
            }
            setTitle( Messages.getString( "AdminUserWindow.Title" ) + " " + user.getId() );
            setId( "adminUserWindow_" + user.getId() );
            userId = user.getId();
            parent.update();
        }
    };

    // delete
    private ActionListener delete = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            // display a confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
                new ConfirmWindow( new ActionListener()
                {
                    public void actionPerformed( ActionEvent event )
                    {
                        // looking for the agent to remove
                        User userToRemove = null;
                        for ( Iterator userIterator = parent.getUsers().iterator(); userIterator.hasNext(); )
                        {
                            User user = (User) userIterator.next();
                            if ( user.getId().equals( userId ) )
                            {
                                userToRemove = user;
                                break;
                            }
                        }
                        parent.getUsers().remove( userToRemove );
                        AdminUserWindow.this.userClose();
                        parent.update();
                    }
                } ) );
        }
    };

    /**
     * Create a new <code>AdminUserWindow</code>.
     *
     * @param parent the <code>AdminUsersWindow</code> parent.
     * @param userId the <code>User</code> ID.
     */
    public AdminUserWindow( AdminUsersWindow parent, String userId )
    {
        super();

        // check if the user that try to access this window is the admin
        if ( !KalumetConsoleApplication.getApplication().getUserid().equals( "admin" ) )
        {
            KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                Messages.getString( "uses.warn.restricted" ) );
            return;
        }

        // update the original user id and parent admin users window
        this.parent = parent;
        this.userId = userId;

        // update the user object from users parent list
        for ( Iterator userIterator = parent.getUsers().iterator(); userIterator.hasNext(); )
        {
            User current = (User) userIterator.next();
            if ( current.getId().equals( userId ) )
            {
                this.user = current;
                break;
            }
        }
        if ( this.user == null )
        {
            this.user = new User();
        }

        if ( userId == null )
        {
            setTitle( Messages.getString( "user" ) );
        }
        else
        {
            setTitle( Messages.getString( "user" ) + " " + userId );
        }
        setId( "user_" + userId );
        setStyleName( "user" );
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
        // add the delete button
        Button deleteButton = new Button( Messages.getString( "delete" ), Styles.DELETE );
        deleteButton.setStyleName( "control" );
        deleteButton.addActionListener( delete );
        controlRow.add( deleteButton );
        // add the apply button
        Button applyButton = new Button( Messages.getString( "apply" ), Styles.DATABASE_GO );
        applyButton.setStyleName( "control" );
        applyButton.addActionListener( applyActionListener );
        controlRow.add( applyButton );
        // add the close button
        Button closeButton = new Button( Messages.getString( "close" ), Styles.CROSS );
        closeButton.setStyleName( "control" );
        closeButton.addActionListener( closeActionListener );
        controlRow.add( closeButton );

        // layout grid
        Grid layoutGrid = new Grid( 2 );
        layoutGrid.setStyleName( "default" );
        layoutGrid.setColumnWidth( 0, new Extent( 15, Extent.PERCENT ) );
        layoutGrid.setColumnWidth( 1, new Extent( 85, Extent.PERCENT ) );
        splitPane.add( layoutGrid );

        // add the user id field
        Label userIdLabel = new Label( Messages.getString( "id" ) );
        userIdLabel.setStyleName( "default" );
        layoutGrid.add( userIdLabel );
        userIdField = new TextField();
        userIdField.setStyleName( "default" );
        userIdField.setWidth( new Extent( 100, Extent.PERCENT ) );
        layoutGrid.add( userIdField );

        // add the user name field
        Label userNameLabel = new Label( Messages.getString( "name" ) );
        userNameLabel.setStyleName( "default" );
        layoutGrid.add( userNameLabel );
        userNameField = new TextField();
        userNameField.setStyleName( "default" );
        userNameField.setWidth( new Extent( 100, Extent.PERCENT ) );
        layoutGrid.add( userNameField );

        // add the user e-mail field
        Label userEmailLabel = new Label( Messages.getString( "email" ) );
        userEmailLabel.setStyleName( "default" );
        layoutGrid.add( userEmailLabel );
        userEmailField = new TextField();
        userEmailField.setStyleName( "default" );
        userEmailField.setWidth( new Extent( 100, Extent.PERCENT ) );
        layoutGrid.add( userEmailField );

        // add the user password field
        Label userPasswordLabel = new Label( Messages.getString( "password" ) );
        userPasswordLabel.setStyleName( "default" );
        layoutGrid.add( userPasswordLabel );
        userPasswordField = new PasswordField();
        userPasswordField.setStyleName( "default" );
        userPasswordField.setWidth( new Extent( 100, Extent.PERCENT ) );
        layoutGrid.add( userPasswordField );

        // add the user confirm password field
        Label userConfirmPasswordLabel = new Label( Messages.getString( "confirm" ) );
        userConfirmPasswordLabel.setStyleName( "default" );
        layoutGrid.add( userConfirmPasswordLabel );
        userConfirmPasswordField = new PasswordField();
        userConfirmPasswordField.setStyleName( "default" );
        userConfirmPasswordField.setWidth( new Extent( 100, Extent.PERCENT ) );
        layoutGrid.add( userConfirmPasswordField );

        // update fields value
        update();
    }

    /**
     * Update the fields value
     */
    protected void update()
    {
        userIdField.setText( user.getId() );
        userNameField.setText( user.getName() );
        userEmailField.setText( user.getEmail() );
        userPasswordField.setText( null );
        userConfirmPasswordField.setText( null );
    }

}
