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
import org.apache.kalumet.model.Group;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.model.User;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Admin groups window.
 */
public class AdminGroupsWindow
    extends WindowPane
{

    // attributes
    private List groups;

    private Grid groupsGrid;

    // close
    private ActionListener close = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            AdminGroupsWindow.this.userClose();
        }
    };

    // refresh
    private ActionListener refresh = new ActionListener()
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
            groups = kalumet.getSecurity().getGroups();
            update();
            KalumetConsoleApplication.getApplication().getLogPane().addInfo(
                Messages.getString( "groups" ) + " " + Messages.getString( "reloaded" ) );
        }
    };

    // delete
    private ActionListener delete = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            if ( event.getActionCommand().equals( "admin" ) )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "groups.admin" ) );
                return;
            }
            // display a confirm window
            final String groupId = event.getActionCommand();
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
                new ConfirmWindow( new ActionListener()
                {
                    public void actionPerformed( ActionEvent event )
                    {
                        Group groupToRemove = null;
                        for ( Iterator groupIterator = groups.iterator(); groupIterator.hasNext(); )
                        {
                            Group current = (Group) groupIterator.next();
                            if ( current.getId().equals( groupId ) )
                            {
                                groupToRemove = current;
                                break;
                            }
                        }
                        groups.remove( groupToRemove );
                        update();
                    }
                } ) );
        }
    };

    // save
    private ActionListener save = new ActionListener()
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
            // update the groups list in Kalumet
            kalumet.getSecurity().setGroups( (LinkedList) groups );
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
            KalumetConsoleApplication.getApplication().getLogPane().addConfirm( Messages.getString( "groups.saved" ) );
        }
    };

    // edit
    private ActionListener edit = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            if ( KalumetConsoleApplication.getApplication().getDefaultWindow().getComponent(
                "groupwindow_" + event.getActionCommand() ) == null )
            {
                KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
                    new AdminGroupWindow( AdminGroupsWindow.this, event.getActionCommand() ) );
            }
        }
    };

    // create
    private ActionListener create = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
                new AdminGroupWindow( AdminGroupsWindow.this, null ) );
        }
    };

    /**
     * Create a new <code>AdminGroupsWindow</code>.
     */
    public AdminGroupsWindow()
    {
        super();

        // check if the user that try to access this window is the admin
        if ( !KalumetConsoleApplication.getApplication().getUserid().equals( "admin" ) )
        {
            KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                Messages.getString( "groups.restricted" ) );
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
        this.groups = kalumet.getSecurity().getGroups();
        Collections.sort( this.groups );

        setTitle( Messages.getString( "groups" ) );
        setIcon( Styles.GROUP );
        setStyleName( "groups" );
        setId( "groupswindow" );
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
        refreshButton.addActionListener( refresh );
        refreshButton.setStyleName( "control" );
        controlRow.add( refreshButton );
        // add the save button
        Button saveButton = new Button( Messages.getString( "save" ), Styles.DATABASE_SAVE );
        saveButton.addActionListener( save );
        saveButton.setStyleName( "control" );
        controlRow.add( saveButton );
        // add the close button
        Button closeButton = new Button( Messages.getString( "close" ), Styles.CROSS );
        closeButton.addActionListener( close );
        closeButton.setStyleName( "control" );
        controlRow.add( closeButton );

        // add the column main pane
        Column content = new Column();
        content.setStyleName( "groups" );
        splitPane.add( content );

        // add a create group button
        Row row = new Row();
        content.add( row );
        Button createGroupButton = new Button( Messages.getString( "group.add" ), Styles.GROUP_ADD );
        createGroupButton.setToolTipText( Messages.getString( "add" ) );
        createGroupButton.addActionListener( create );
        row.add( createGroupButton );

        // add the groups list grid
        groupsGrid = new Grid( 4 );
        groupsGrid.setStyleName( "border.grid" );
        groupsGrid.setColumnWidth( 0, new Extent( 18, Extent.PX ) );
        groupsGrid.setColumnWidth( 1, new Extent( 33, Extent.PERCENT ) );
        groupsGrid.setColumnWidth( 2, new Extent( 33, Extent.PERCENT ) );
        groupsGrid.setColumnWidth( 3, new Extent( 33, Extent.PERCENT ) );
        content.add( groupsGrid );

        // update groups grid
        update();
    }

    /**
     * Update the pane.
     */
    protected void update()
    {
        // delete all groups grid child
        groupsGrid.removeAll();

        // add grid headers
        Label groupActionHeader = new Label( " " );
        groupActionHeader.setStyleName( "grid.header" );
        groupsGrid.add( groupActionHeader );
        Label groupIdHeader = new Label( Messages.getString( "id" ) );
        groupIdHeader.setStyleName( "grid.header" );
        groupsGrid.add( groupIdHeader );
        Label groupNameHeader = new Label( Messages.getString( "name" ) );
        groupNameHeader.setStyleName( "grid.header" );
        groupsGrid.add( groupNameHeader );
        Label groupMembersHeader = new Label( Messages.getString( "members" ) );
        groupMembersHeader.setStyleName( "grid.header" );
        groupsGrid.add( groupMembersHeader );

        // add groups
        for ( Iterator groupIterator = groups.iterator(); groupIterator.hasNext(); )
        {
            Group currentGroup = (Group) groupIterator.next();
            // action row with group id
            Row row = new Row();
            row.setStyleName( "grid.cell" );
            groupsGrid.add( row );
            Button deleteButton = new Button( Styles.GROUP_DELETE );
            deleteButton.addActionListener( delete );
            deleteButton.setActionCommand( currentGroup.getId() );
            deleteButton.setToolTipText( Messages.getString( "delete" ) );
            row.add( deleteButton );
            // id
            Button groupId = new Button( currentGroup.getId() );
            groupId.addActionListener( edit );
            groupId.setActionCommand( currentGroup.getId() );
            groupId.setStyleName( "grid.cell" );
            groupsGrid.add( groupId );
            // group name
            Label groupNameLabel = new Label( currentGroup.getName() );
            groupNameLabel.setStyleName( "grid.cell" );
            groupsGrid.add( groupNameLabel );
            // group members
            Column membersPane = new Column();
            membersPane.setStyleName( "grid.cell" );
            groupsGrid.add( membersPane );
            for ( Iterator userIterator = currentGroup.getUsers().iterator(); userIterator.hasNext(); )
            {
                User currentUser = (User) userIterator.next();
                Label member = new Label( currentUser.getName() + " (" + currentUser.getId() + ")" );
                member.setStyleName( "default" );
                membersPane.add( member );
            }
        }
    }

    protected List getGroups()
    {
        return this.groups;
    }

}
