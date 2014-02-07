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
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.PasswordField;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.SplitPane;
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.WindowPane;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.app.layout.SplitPaneLayoutData;
import org.apache.kalumet.console.configuration.ConfigurationManager;
import org.apache.kalumet.model.Group;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.model.User;

import java.util.Iterator;

/**
 * Preferences window.
 */
public class PreferencesWindow
    extends WindowPane
{

    // attributes
    private TextField userIdField;

    private TextField userNameField;

    private TextField userEmailField;

    private PasswordField userPasswordField;

    private PasswordField userConfirmPasswordField;

    // close button action listener
    private ActionListener closeButtonActionListener = new ActionListener()
    {

        public void actionPerformed( ActionEvent event )
        {
            PreferencesWindow.this.userClose();
        }

    };

    // refresh button action listener
    private ActionListener refreshButtonActionListener = new ActionListener()
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
            User user = kalumet.getSecurity().getUser( KalumetConsoleApplication.getApplication().getUserid() );
            if ( user == null )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addError(
                    Messages.getString( "error.user.notfound" ) );
                return;
            }
            userIdField.setText( user.getId() );
            userNameField.setText( user.getName() );
            userEmailField.setText( user.getEmail() );
            userPasswordField.setText( null );
            userConfirmPasswordField.setText( null );
        }

    };

    // save button action listener
    private ActionListener saveButtonActionListener = new ActionListener()
    {

        public void actionPerformed( ActionEvent event )
        {
            String userId = userIdField.getText().trim();
            String userName = userNameField.getText().trim();
            String userEmail = userEmailField.getText().trim();
            String userPassword = userPasswordField.getText();
            String userConfirmPassword = userConfirmPasswordField.getText();
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
            User user = kalumet.getSecurity().getUser( KalumetConsoleApplication.getApplication().getUserid() );
            if ( user == null )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addError(
                    Messages.getString( "error.user.notfound" ) );
                return;
            }
            if ( userId.length() < 1 )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "preferences.warn.username" ) );
                return;
            }
            if ( userPassword.length() > 0 && !userPassword.equals( userConfirmPassword ) )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "preferences.warn.password" ) );
                return;
            }
            if ( !userId.equals( KalumetConsoleApplication.getApplication().getUserid() )
                && KalumetConsoleApplication.getApplication().getUserid().equals( "admin" ) )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "preferences.warn.admin" ) );
                return;
            }
            if ( userPassword.length() > 0 )
            {
                try
                {
                    user.setPassword( User.md5PasswordCrypt( userPassword ) );
                }
                catch ( Exception e )
                {
                    KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                        Messages.getString( "preferences.warn.crypt" ) + ": " + e.getMessage() );
                    return;
                }
            }
            user.setEmail( userEmail );
            user.setName( userName );
            if ( !userId.equals( KalumetConsoleApplication.getApplication().getUserid() ) )
            {
                // update user group
                for ( Iterator userGroupIterator = kalumet.getSecurity().getUserGroups( user.getId() ).iterator();
                      userGroupIterator.hasNext(); )
                {
                    Group group = (Group) userGroupIterator.next();
                    for ( Iterator userIterator = group.getUsers().iterator(); userIterator.hasNext(); )
                    {
                        User current = (User) userIterator.next();
                        current.setId( userId );
                    }
                }
                // update the main user
                user.setId( userId );
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
                KalumetConsoleApplication.getApplication().disconnect();
                return;
            }
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
            KalumetConsoleApplication.getApplication().getLogPane().addConfirm(
                Messages.getString( "preferences.saved" ) );
        }

    };

    /**
     * Create a new <code>PreferencesWindow</code>.
     */
    public PreferencesWindow()
    {
        super();

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

        // identify the user
        User user = kalumet.getSecurity().getUser( KalumetConsoleApplication.getApplication().getUserid() );
        if ( user == null )
        {
            KalumetConsoleApplication.getApplication().getLogPane().addError(
                Messages.getString( "error.user.notfound" ) );
            return;
        }

        setTitle( Messages.getString( "preferences" ) );
        setIcon( Styles.WRENCH );
        setStyleName( "preferences" );
        setModal( false );
        setId( "preferenceswindow" );
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
        refreshButton.addActionListener( refreshButtonActionListener );
        refreshButton.setStyleName( "control" );
        controlRow.add( refreshButton );
        // add the save button
        Button saveButton = new Button( Messages.getString( "save" ), Styles.DATABASE_SAVE );
        saveButton.addActionListener( saveButtonActionListener );
        saveButton.setStyleName( "control" );
        controlRow.add( saveButton );
        // add the close button
        Button closeButton = new Button( Messages.getString( "close" ), Styles.CROSS );
        closeButton.addActionListener( closeButtonActionListener );
        closeButton.setStyleName( "control" );
        controlRow.add( closeButton );

        // define a grid layout
        Grid layoutGrid = new Grid( 2 );
        layoutGrid.setStyleName( "default" );
        SplitPaneLayoutData layoutData = new SplitPaneLayoutData();
        layoutData.setInsets( new Insets( 4 ) );
        layoutGrid.setLayoutData( layoutData );
        layoutGrid.setWidth( new Extent( 100, Extent.PERCENT ) );
        layoutGrid.setColumnWidth( 0, new Extent( 15, Extent.PERCENT ) );
        layoutGrid.setColumnWidth( 1, new Extent( 85, Extent.PERCENT ) );
        splitPane.add( layoutGrid );

        // create the user id field
        Label idLabel = new Label( Messages.getString( "username" ) );
        idLabel.setStyleName( "grid.cell" );
        layoutGrid.add( idLabel );
        userIdField = new TextField();
        userIdField.setText( user.getId() );
        userIdField.setStyleName( "default" );
        userIdField.setWidth( new Extent( 100, Extent.PERCENT ) );
        if ( KalumetConsoleApplication.getApplication().getUserid().equals( "admin" ) )
        {
            userIdField.setEnabled( false );
        }
        layoutGrid.add( userIdField );

        // create the user name field
        Label nameLabel = new Label( Messages.getString( "name" ) );
        nameLabel.setStyleName( "grid.cell" );
        layoutGrid.add( nameLabel );
        userNameField = new TextField();
        userNameField.setText( user.getName() );
        userNameField.setStyleName( "default" );
        userNameField.setWidth( new Extent( 100, Extent.PERCENT ) );
        layoutGrid.add( userNameField );

        // create the user e-mail field
        Label emailLabel = new Label( Messages.getString( "email" ) );
        emailLabel.setStyleName( "grid.cell" );
        layoutGrid.add( emailLabel );
        userEmailField = new TextField();
        userEmailField.setText( user.getEmail() );
        userEmailField.setStyleName( "default" );
        userEmailField.setWidth( new Extent( 100, Extent.PERCENT ) );
        layoutGrid.add( userEmailField );

        // create the user password field
        Label passwordLabel = new Label( Messages.getString( "password" ) );
        passwordLabel.setStyleName( "grid.cell" );
        layoutGrid.add( passwordLabel );
        userPasswordField = new PasswordField();
        userPasswordField.setStyleName( "default" );
        userPasswordField.setWidth( new Extent( 100, Extent.PERCENT ) );
        layoutGrid.add( userPasswordField );

        // create the user confirm password field
        Label passwordConfirmLabel = new Label( Messages.getString( "confirm" ) );
        passwordConfirmLabel.setStyleName( "grid.cell" );
        layoutGrid.add( passwordConfirmLabel );
        userConfirmPasswordField = new PasswordField();
        userConfirmPasswordField.setStyleName( "default" );
        userConfirmPasswordField.setWidth( new Extent( 100, Extent.PERCENT ) );
        layoutGrid.add( userConfirmPasswordField );

        // display user groups
        Label userGroupLabel = new Label( Messages.getString( "groups" ) );
        userGroupLabel.setStyleName( "grid.cell" );
        layoutGrid.add( userGroupLabel );
        Column groupsColumn = new Column();
        groupsColumn.setStyleName( "grid.action" );
        layoutGrid.add( groupsColumn );
        for ( Iterator userGroupIterator = kalumet.getSecurity().getUserGroups( user.getId() ).iterator();
              userGroupIterator.hasNext(); )
        {
            Group group = (Group) userGroupIterator.next();
            Label groupLabel = new Label( group.getName() );
            groupLabel.setStyleName( "default" );
            groupsColumn.add( groupLabel );
        }
    }

}