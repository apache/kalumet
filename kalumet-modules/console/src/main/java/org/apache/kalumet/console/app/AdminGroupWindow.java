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
import nextapp.echo2.app.ListBox;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.SplitPane;
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.WindowPane;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.app.list.DefaultListModel;
import nextapp.echo2.app.list.ListSelectionModel;
import org.apache.kalumet.console.configuration.ConfigurationManager;
import org.apache.kalumet.model.Group;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.model.User;

import java.util.Iterator;

/**
 * Admin group window.
 */
public class AdminGroupWindow
    extends WindowPane
{

    // attributes
    private String groupId;

    private Group group = null;

    private AdminGroupsWindow parent;

    private TextField idField;

    private TextField nameField;

    private ListBox membersBox;

    private ListBox usersBox;

    // close
    private ActionListener close = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            AdminGroupWindow.this.userClose();
        }
    };

    // refresh action listener
    private ActionListener refreshActionListener = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            // update the group from the parent list
            for ( Iterator groupIterator = parent.getGroups().iterator(); groupIterator.hasNext(); )
            {
                Group current = (Group) groupIterator.next();
                if ( current.getId().equals( groupId ) )
                {
                    group = current;
                    break;
                }
            }
            if ( group == null )
            {
                group = new Group();
            }
            update();
            KalumetConsoleApplication.getApplication().getLogPane().addInfo(
                groupId + " " + Messages.getString( "reloaded" ) );
        }
    };

    // add member action listener
    private ActionListener addMemberActionListener = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            // check if the users box is not empty
            if ( usersBox.getModel().size() < 1 )
            {
                return;
            }
            // check if the user has selected a user
            if ( usersBox.getSelectedValue() == null )
            {
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
                return;
            }
            // get the user object and add the user to the group members
            User found = null;
            for ( Iterator userIterator = kalumet.getSecurity().getUsers().iterator(); userIterator.hasNext(); )
            {
                User user = (User) userIterator.next();
                if ( user.getId().equals( (String) usersBox.getSelectedValue() ) )
                {
                    found = user;
                }
            }
            // check if the user has been found
            if ( found == null )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "group.warn.user.notfound" ) );
                return;
            }
            try
            {
                group.addUser( found );
            }
            catch ( Exception e )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "group.warn.user.alreadymember" ) );
                return;
            }
            // update view
            update();
            parent.update();
        }
    };

    // delete member action listener
    private ActionListener deleteMemberActionListener = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            // check if the members box is not empty
            if ( membersBox.getModel().size() < 1 )
            {
                return;
            }
            // check if the user has selected a member
            if ( membersBox.getSelectedValue() == null )
            {
                KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
                    new ErrorWindow( Messages.getString( "AdminGroupWindow.Error.Delete" ),
                                     Messages.getString( "AdminGroupWindow.Error.Delete.Select" ) ) );
                return;
            }
            // get the member object
            User found = null;
            for ( Iterator memberIterator = group.getUsers().iterator(); memberIterator.hasNext(); )
            {
                User member = (User) memberIterator.next();
                if ( member.getId().equals( membersBox.getSelectedValue() ) )
                {
                    found = member;
                }
            }
            // check if the member is found
            if ( found == null )
            {
                KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
                    new ErrorWindow( Messages.getString( "AdminGroupWindow.Error.Delete" ),
                                     Messages.getString( "AdminGroupWindow.Error.Delete.NotFound" ) ) );
                return;
            }
            group.getUsers().remove( found );
            // update view
            update();
            parent.update();
        }
    };

    // apply action listener
    private ActionListener applyActionListener = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            String groupNewId = idField.getText().trim();
            String groupName = nameField.getText().trim();

            // check fields
            // group id is mandatory
            if ( groupNewId.length() < 1 )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "group.warn.empty" ) );
                return;
            }
            // if the admin change the group id or if it's a new group, check if
            // the id is not already used
            if ( groupId == null || groupId.trim().length() < 1 || ( ( groupId != null ) && ( groupId.trim().length()
                > 0 ) && ( !groupNewId.equals( groupId ) ) ) )
            {
                for ( Iterator groupIterator = parent.getGroups().iterator(); groupIterator.hasNext(); )
                {
                    Group current = (Group) groupIterator.next();
                    if ( current.getId().equals( groupNewId ) )
                    {
                        KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
                            new ErrorWindow( Messages.getString( "AdminGroupWindow.Error.Save" ),
                                             Messages.getString( "AdminGroupWindow.Error.GroupIdAlreadyExists" ) ) );
                        return;
                    }
                }
            }

            // update the current group
            group.setId( groupNewId );
            group.setName( groupName );
            if ( groupId == null || groupId.trim().length() < 1 )
            {
                // it's a new group
                parent.getGroups().add( group );
            }
            setTitle( Messages.getString( "AdminGroupWindow.Title" ) + " " + group.getId() );
            setId( "adminwindow_" + group.getId() );
            groupId = group.getId();
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
                        Group groupToRemove = null;
                        for ( Iterator groupIterator = parent.getGroups().iterator(); groupIterator.hasNext(); )
                        {
                            Group group = (Group) groupIterator.next();
                            if ( group.getId().equals( groupId ) )
                            {
                                groupToRemove = group;
                                break;
                            }
                        }
                        parent.getGroups().remove( groupToRemove );
                        AdminGroupWindow.this.userClose();
                        parent.update();
                    }
                } ) );
        }
    };

    /**
     * Create a new <code>AdminGroupWindow</code>.
     *
     * @param parent  the <code>AdminGroupsWindow</code> parent.
     * @param groupId the <code>Group</code> ID.
     */
    public AdminGroupWindow( AdminGroupsWindow parent, String groupId )
    {
        super();

        // check if the user that try to access this window is the admin
        if ( !KalumetConsoleApplication.getApplication().getUserid().equals( "admin" ) )
        {
            KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                Messages.getString( "groups.warn.restricted" ) );
            return;
        }

        // update the original group id and parent admin groups window
        this.parent = parent;
        this.groupId = groupId;

        // update the group object from groups parent list
        for ( Iterator groupIterator = parent.getGroups().iterator(); groupIterator.hasNext(); )
        {
            Group current = (Group) groupIterator.next();
            if ( current.getId().equals( groupId ) )
            {
                this.group = current;
                break;
            }
        }
        if ( this.group == null )
        {
            this.group = new Group();
        }

        if ( groupId == null )
        {
            setTitle( Messages.getString( "group" ) );
        }
        else
        {
            setTitle( Messages.getString( "group" ) + " " + groupId );
        }
        setIcon( Styles.GROUP );
        setId( "groupwindow_" + groupId );
        setStyleName( "group" );
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
        closeButton.addActionListener( close );
        controlRow.add( closeButton );

        // content column pane
        Column content = new Column();
        content.setStyleName( "group" );
        splitPane.add( content );

        // layout grid
        Grid layoutGrid = new Grid( 2 );
        layoutGrid.setStyleName( "default" );
        layoutGrid.setColumnWidth( 0, new Extent( 15, Extent.PERCENT ) );
        layoutGrid.setColumnWidth( 1, new Extent( 85, Extent.PERCENT ) );
        content.add( layoutGrid );

        // add the group id field
        Label idLabel = new Label( Messages.getString( "id" ) );
        idLabel.setStyleName( "default" );
        layoutGrid.add( idLabel );
        idField = new TextField();
        idField.setStyleName( "default" );
        idField.setWidth( new Extent( 100, Extent.PERCENT ) );
        layoutGrid.add( idField );

        // add the group name field
        Label nameLabel = new Label( Messages.getString( "name" ) );
        nameLabel.setStyleName( "default" );
        layoutGrid.add( nameLabel );
        nameField = new TextField();
        nameField.setStyleName( "default" );
        nameField.setWidth( new Extent( 100, Extent.PERCENT ) );
        layoutGrid.add( nameField );

        // add the members header
        Label membersHeader = new Label( Messages.getString( "members" ) );
        membersHeader.setStyleName( "default" );
        layoutGrid.add( membersHeader );

        // grid members
        Grid membersGrid = new Grid( 2 );
        membersGrid.setStyleName( "default" );
        membersGrid.setWidth( new Extent( 100, Extent.PERCENT ) );
        membersGrid.setColumnWidth( 0, new Extent( 50, Extent.PERCENT ) );
        membersGrid.setColumnWidth( 1, new Extent( 50, Extent.PERCENT ) );
        content.add( membersGrid );

        // membersBox
        membersBox = new ListBox();
        membersBox.setStyleName( "default" );
        membersBox.setHeight( new Extent( 200, Extent.PX ) );
        membersBox.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        membersGrid.add( membersBox );
        // usersBox
        usersBox = new ListBox();
        usersBox.setStyleName( "default" );
        usersBox.setHeight( new Extent( 200, Extent.PX ) );
        usersBox.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        membersGrid.add( usersBox );
        // add user member button
        Button addMemberButton = new Button( Styles.ARROW_LEFT );
        addMemberButton.setStyleName( "group.member.add" );
        addMemberButton.addActionListener( addMemberActionListener );
        membersGrid.add( addMemberButton );
        // add delete member button
        Button deleteMemberButton = new Button( Styles.ARROW_RIGHT );
        deleteMemberButton.setStyleName( "group.member.delete" );
        deleteMemberButton.addActionListener( deleteMemberActionListener );
        membersGrid.add( deleteMemberButton );

        // update fields/boxes value
        update();
    }

    /**
     * Update the group fields/boxes
     */
    public void update()
    {
        idField.setText( group.getId() );
        nameField.setText( group.getName() );

        // update the members box
        DefaultListModel membersBoxModel = (DefaultListModel) membersBox.getModel();
        membersBoxModel.removeAll();
        for ( Iterator memberIterator = group.getUsers().iterator(); memberIterator.hasNext(); )
        {
            User member = (User) memberIterator.next();
            membersBoxModel.add( member.getId() );
        }

        // update the users box
        DefaultListModel usersBoxModel = (DefaultListModel) usersBox.getModel();
        usersBoxModel.removeAll();

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

        for ( Iterator userIterator = kalumet.getSecurity().getUsers().iterator(); userIterator.hasNext(); )
        {
            User user = (User) userIterator.next();
            // check if the user is not already a group member
            boolean find = false;
            for ( Iterator memberIterator = group.getUsers().iterator(); memberIterator.hasNext(); )
            {
                User member = (User) memberIterator.next();
                if ( member.getId().equals( user.getId() ) )
                {
                    find = true;
                }
            }
            if ( !find )
            {
                usersBoxModel.add( user.getId() );
            }
        }
    }

}
