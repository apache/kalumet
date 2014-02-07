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
import nextapp.echo2.app.ContentPane;
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.Grid;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.SelectField;
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import org.apache.commons.collections.iterators.ReverseListIterator;
import org.apache.commons.lang.StringUtils;
import org.apache.kalumet.console.configuration.ConfigurationManager;
import org.apache.kalumet.model.log.Event;
import org.apache.kalumet.model.log.Journal;

import java.util.Iterator;

/**
 * Environment journal pane.
 */
public class JournalPane
    extends ContentPane
{

    private EnvironmentWindow parent;

    private TextField filterField;

    private SelectField rowLimitField;

    private TextField newEventField;

    private Grid journalGrid;

    private Grid currentGrid;

    // row limit
    private ActionListener rowLimit = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            JournalPane.this.update();
        }
    };

    // filter
    private ActionListener filter = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            JournalPane.this.update();
        }
    };

    // create
    private ActionListener create = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            // check if the user has the environment lock
            if ( !getEnvironmentWindow().getEnvironment().getLock().equals(
                KalumetConsoleApplication.getApplication().getUserid() ) )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "environment.locked" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // get the event message
            String eventMessage = newEventField.getText();
            // check field
            if ( eventMessage == null || eventMessage.trim().length() < 1 )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "journal.mandatory" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // add the message
            parent.getChangeEvents().add( eventMessage );
            // update the pane
            update();
        }
    };

    // purge
    private ActionListener purge = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            // check if the user has the environment lock
            if ( !getEnvironmentWindow().getEnvironment().getLock().equals(
                KalumetConsoleApplication.getApplication().getUserid() ) )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "environment.locked" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // check if the user can do it
            if ( !getEnvironmentWindow().adminPermission )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "action.restricted" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // create a confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
                new ConfirmWindow( new ActionListener()
                {
                    public void actionPerformed( ActionEvent event )
                    {
                        // create a new empty journal
                        Journal journal = new Journal();
                        try
                        {
                            journal.writeXMLFile(
                                ConfigurationManager.getEnvironmentJournalFile( parent.getEnvironmentName() ) );
                        }
                        catch ( Exception e )
                        {
                            KalumetConsoleApplication.getApplication().getLogPane().addError(
                                Messages.getString( "journal.write" ) + ": " + e.getMessage(),
                                getEnvironmentWindow().getEnvironmentName() );
                            return;
                        }
                        // update the pane
                        update();
                    }
                } ) );

        }
    };

    // delete
    private ActionListener delete = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            // create a confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
                new ConfirmWindow( new ActionListener()
                {
                    public void actionPerformed( ActionEvent event )
                    {

                        // get the index
                        int index = new Integer( event.getActionCommand() ).intValue();
                        if ( index == -1 )
                        {
                            return;
                        }
                        // remove
                        parent.getChangeEvents().remove( index );
                        // update the pane
                        update();
                    }
                } ) );
        }
    };

    /**
     * Create a new <code>JournalPane</code>
     *
     * @param parent the parent <code>EnvironmentWindow</code>.
     */
    public JournalPane( EnvironmentWindow parent )
    {
        super();
        setStyleName( "tab.content" );

        // update parent
        this.parent = parent;

        // column layout
        Column content = new Column();
        content.setCellSpacing( new Extent( 2 ) );
        add( content );

        // add filter layout grid
        Grid filterGrid = new Grid( 2 );
        filterGrid.setStyleName( "default" );
        content.add( filterGrid );

        // add row limit field
        Label rowLimitLabel = new Label( Messages.getString( "rows" ) );
        rowLimitLabel.setStyleName( "default" );
        filterGrid.add( rowLimitLabel );
        String[] labels = new String[]{ "10", "25", "50", "100", "200", Messages.getString( "all" ) };
        rowLimitField = new SelectField( labels );
        rowLimitField.setStyleName( "default" );
        rowLimitField.setSelectedIndex( 0 );
        rowLimitField.addActionListener( rowLimit );
        filterGrid.add( rowLimitField );

        // add filter field
        Label filterLabel = new Label( Messages.getString( "filter" ) );
        filterLabel.setStyleName( "default" );
        filterGrid.add( filterLabel );
        Row filterRow = new Row();
        filterGrid.add( filterRow );
        filterField = new TextField();
        filterField.setStyleName( "default" );
        filterField.addActionListener( filter );
        filterRow.add( filterField );
        Button filterApplyButton = new Button( Styles.ACCEPT );
        filterApplyButton.setStyleName( "default" );
        filterApplyButton.addActionListener( filter );
        filterRow.add( filterApplyButton );

        // add purge button
        if ( getEnvironmentWindow().adminPermission )
        {
            Row purgeRow = new Row();
            content.add( purgeRow );
            Button purgeButton = new Button( Messages.getString( "purge" ), Styles.DELETE );
            purgeButton.addActionListener( purge );
            purgeRow.add( purgeButton );
        }

        // add journal log grid
        journalGrid = new Grid( 4 );
        journalGrid.setStyleName( "border.grid" );
        content.add( journalGrid );

        // add the current log grid
        currentGrid = new Grid( 2 );
        currentGrid.setStyleName( "border.grid" );
        currentGrid.setColumnWidth( 0, new Extent( 50, Extent.PX ) );
        content.add( currentGrid );

        // update the pane
        update();
    }

    /**
     * Update the pane
     */
    public void update()
    {
        // update journal log grid
        // remove all log grid children
        journalGrid.removeAll();
        // add log grid header
        Label dateHeader = new Label( Messages.getString( "date" ) );
        dateHeader.setStyleName( "grid.header" );
        journalGrid.add( dateHeader );
        Label severityHeader = new Label( Messages.getString( "severity" ) );
        severityHeader.setStyleName( "grid.header" );
        journalGrid.add( severityHeader );
        Label authorHeader = new Label( Messages.getString( "author" ) );
        authorHeader.setStyleName( "grid.header" );
        journalGrid.add( authorHeader );
        Label messageHeader = new Label( Messages.getString( "message" ) );
        messageHeader.setStyleName( "grid.header" );
        journalGrid.add( messageHeader );
        // load the journal log
        Journal journal = null;
        try
        {
            journal = ConfigurationManager.loadEnvironmentJournal( parent.getEnvironmentName() );
        }
        catch ( Exception e )
        {
            KalumetConsoleApplication.getApplication().getLogPane().addError(
                Messages.getString( "journal.read" ) + ": " + e.getMessage() );
            return;
        }
        // get the limit
        int rowLimit = -1;
        if ( rowLimitField.getSelectedIndex() == 0 )
        {
            rowLimit = 10;
        }
        if ( rowLimitField.getSelectedIndex() == 1 )
        {
            rowLimit = 25;
        }
        if ( rowLimitField.getSelectedIndex() == 2 )
        {
            rowLimit = 50;
        }
        if ( rowLimitField.getSelectedIndex() == 3 )
        {
            rowLimit = 100;
        }
        if ( rowLimitField.getSelectedIndex() == 4 )
        {
            rowLimit = 200;
        }
        int index = 0;
        for ( ReverseListIterator eventIterator = new ReverseListIterator( journal.getEvents() );
              eventIterator.hasNext(); )
        {
            Event event = (Event) eventIterator.next();
            if ( filterField.getText() == null || filterField.getText().trim().length() < 1 || (
                filterField.getText() != null && (
                    StringUtils.containsIgnoreCase( event.getDate(), filterField.getText() )
                        || StringUtils.containsIgnoreCase( event.getSeverity(), filterField.getText() )
                        || StringUtils.containsIgnoreCase( event.getAuthor(), filterField.getText() )
                        || StringUtils.containsIgnoreCase( event.getContent(), filterField.getText() ) ) ) )
            {
                Label eventDate = new Label( event.getDate() );
                eventDate.setStyleName( "default" );
                journalGrid.add( eventDate );
                Label eventSeverity = new Label( event.getSeverity() );
                eventSeverity.setStyleName( "default" );
                journalGrid.add( eventSeverity );
                Label eventAuthor = new Label( event.getAuthor() );
                eventAuthor.setStyleName( "default" );
                journalGrid.add( eventAuthor );
                Label eventContent = new Label( event.getContent() );
                eventContent.setStyleName( "default" );
                journalGrid.add( eventContent );
            }
            if ( rowLimit != -1 && index >= rowLimit )
            {
                break;
            }
            index++;
        }
        // update the current log grid
        // remove all current log grid children
        currentGrid.removeAll();
        // add current log grid header
        Label currentEventAction = new Label( " " );
        currentEventAction.setStyleName( "grid.header" );
        currentGrid.add( currentEventAction );
        Label currentEventContent = new Label( Messages.getString( "message" ) );
        currentEventContent.setStyleName( "grid.header" );
        currentGrid.add( currentEventContent );
        // add new event row
        Row row = new Row();
        row.setInsets( new Insets( 2 ) );
        row.setCellSpacing( new Extent( 2 ) );
        currentGrid.add( row );
        Button createButton = new Button( Styles.ADD );
        createButton.addActionListener( create );
        row.add( createButton );
        newEventField = new TextField();
        newEventField.setStyleName( "default" );
        newEventField.setWidth( new Extent( 500, Extent.PX ) );
        currentGrid.add( newEventField );
        // add current event
        for ( Iterator currentEventIterator = new ReverseListIterator( parent.getChangeEvents() );
              currentEventIterator.hasNext(); )
        {
            String message = (String) currentEventIterator.next();
            // row
            Row currentRow = new Row();
            currentRow.setCellSpacing( new Extent( 2 ) );
            currentRow.setInsets( new Insets( 2 ) );
            currentGrid.add( row );
            // delete
            Button deleteButton = new Button( Styles.DELETE );
            deleteButton.setToolTipText( Messages.getString( "delete" ) );
            deleteButton.setActionCommand( new Integer( parent.getChangeEvents().indexOf( message ) ).toString() );
            deleteButton.addActionListener( delete );
            currentRow.add( deleteButton );
            // message field
            TextField messageField = new TextField();
            messageField.setStyleName( "default" );
            messageField.setWidth( new Extent( 100, Extent.PERCENT ) );
            messageField.setId(
                "journalmessage_" + parent.getEnvironmentName() + "_" + parent.getChangeEvents().indexOf( message ) );
            messageField.setText( message );
            currentGrid.add( messageField );
        }
    }

    public EnvironmentWindow getEnvironmentWindow()
    {
        return parent;
    }

}