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
import nextapp.echo2.app.list.DefaultListModel;
import org.apache.kalumet.model.Destination;
import org.apache.kalumet.model.Email;

import java.util.Iterator;

/**
 * Environment publishers pane.
 */
public class PublishersPane
    extends ContentPane
{

    private EnvironmentWindow parent;

    private SelectField scopeField;

    private Grid grid;

    private TextField newDestinationField;

    // scope select
    private ActionListener scopeSelect = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            update();
        }
    };

    // edit destination
    private ActionListener editDestination = new ActionListener()
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
            // get the destination address
            String address = event.getActionCommand();
            // get field
            TextField addressField = (TextField) PublishersPane.this.getComponent(
                "publisherdestination_" + parent.getEnvironmentName() + "_" + (String) scopeField.getSelectedItem()
                    + "_" + address );
            String addressFieldValue = addressField.getText();
            // check the field value
            if ( addressFieldValue == null || addressFieldValue.trim().length() < 1 )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "publisher.destination.mandatory" ),
                    getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // if the user change the destination address, check if the address is
            // not already in use
            if ( !address.equals( addressFieldValue ) )
            {
                if ( parent.getEnvironment().getPublisher( (String) scopeField.getSelectedItem() ).getDestination(
                    addressFieldValue ) != null )
                {
                    KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                        Messages.getString( "publisher.destination.exists" ),
                        getEnvironmentWindow().getEnvironmentName() );
                    return;
                }
            }
            // looking for the destination object
            Destination destination =
                parent.getEnvironment().getPublisher( (String) scopeField.getSelectedItem() ).getDestination( address );
            if ( destination == null )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "publisher.destination.notfound" ),
                    getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // add a change event
            parent.getChangeEvents().add( "Change publisher destination to " + destination.getAddress() );
            // update the destination object
            destination.setAddress( addressFieldValue );
            // change the updated flag
            parent.setUpdated( true );
            // update the journal log tab pane
            parent.updateJournalPane();
            // update the pane
            update();
        }
    };

    // create destination
    private ActionListener createDestination = new ActionListener()
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
            // get the new destination address field value
            String newAddressFieldValue = newDestinationField.getText();
            // check mandatory field
            if ( newAddressFieldValue == null || newAddressFieldValue.trim().length() < 1 )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "publisher.destination.mandatory" ),
                    getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // create a new destination object
            Destination destination = new Destination();
            destination.setAddress( newAddressFieldValue );
            // add the destination
            try
            {
                parent.getEnvironment().getPublisher( (String) scopeField.getSelectedItem() ).addDestination(
                    destination );
            }
            catch ( Exception e )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "publisher.destination.exists" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // add a change event
            parent.getChangeEvents().add( "Add publisher destination " + destination.getAddress() );
            // change the updated flag
            parent.setUpdated( true );
            // update the journal log tab pane
            parent.updateJournalPane();
            // update the tab
            update();
        }
    };

    // delete destination
    private ActionListener deleteDestination = new ActionListener()
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
            // get the destination address
            String address = event.getActionCommand();
            // looking for the destination object
            final Destination destination =
                parent.getEnvironment().getPublisher( (String) scopeField.getSelectedItem() ).getDestination( address );
            if ( destination == null )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "publisher.destination.notfound" ),
                    getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // display confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
                new ConfirmWindow( new ActionListener()
                {
                    public void actionPerformed( ActionEvent event )
                    {
                        // remove the destination
                        parent.getEnvironment().getPublisher(
                            (String) scopeField.getSelectedItem() ).getDestinations().remove( destination );
                        // add a change event
                        parent.getChangeEvents().add( "Delete publisher destination " + destination.getAddress() );
                        // change the updated flag
                        parent.setUpdated( true );
                        // update the journal log tab pane
                        parent.updateJournalPane();
                        // update the tab
                        update();
                    }
                } ) );
        }
    };

    // delete publisher
    private ActionListener deletePublisher = new ActionListener()
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
            // looking for the publisher object
            final Email publisher = parent.getEnvironment().getPublisher( (String) scopeField.getSelectedItem() );
            if ( publisher == null )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "publisher.notfound" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // display confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
                new ConfirmWindow( new ActionListener()
                {
                    public void actionPerformed( ActionEvent event )
                    {
                        // remove the publisher object
                        parent.getEnvironment().getPublishers().remove( publisher );
                        // add a change event
                        parent.getChangeEvents().add( "Delete publisher " + publisher.getMailhost() );
                        // change the updated flag
                        parent.setUpdated( true );
                        // update the journal
                        parent.updateJournalPane();
                        // update this pane
                        update();
                    }
                } ) );
        }
    };

    // edit publisher
    private ActionListener editPublisher = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            if ( KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().getComponent(
                "publisherwindow_" + parent.getEnvironmentName() + "_" + event.getActionCommand() ) == null )
            {
                KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
                    new PublisherWindow( PublishersPane.this, (String) scopeField.getSelectedItem() ) );
            }
        }
    };

    // create publisher
    private ActionListener createPublisher = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
                new PublisherWindow( PublishersPane.this, null ) );
        }
    };

    // copy publisher
    private ActionListener copyPublisher = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            // looking for the publisher
            Email publisher = parent.getEnvironment().getPublisher( (String) scopeField.getSelectedItem() );
            if ( publisher == null )
            {
                return;
            }
            try
            {
                KalumetConsoleApplication.getApplication().setCopyComponent( publisher.clone() );
            }
            catch ( Exception e )
            {
                return;
            }
        }
    };

    // copy destination
    private ActionListener copyDestination = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            // looking for the destination object
            Destination destination =
                parent.getEnvironment().getPublisher( (String) scopeField.getSelectedItem() ).getDestination(
                    event.getActionCommand() );
            if ( destination == null )
            {
                return;
            }
            try
            {
                KalumetConsoleApplication.getApplication().setCopyComponent( destination.clone() );
            }
            catch ( Exception e )
            {
                return;
            }
        }
    };

    // paste destination
    private ActionListener pasteDestination = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            Object copy = KalumetConsoleApplication.getApplication().getCopyComponent();
            // check the copy object
            if ( copy == null || !( copy instanceof Destination ) )
            {
                return;
            }
            // update the new field
            newDestinationField.setText( ( (Destination) copy ).getAddress() );
        }
    };

    /**
     * Create a new <code>PublishersPane</code>.
     *
     * @param parent the parent <code>EnvironmentWindow</code>.
     */
    public PublishersPane( EnvironmentWindow parent )
    {
        super();
        setStyleName( "tab.content" );

        // update parent
        this.parent = parent;

        // column layout
        Column content = new Column();
        content.setInsets( new Insets( 2 ) );
        content.setCellSpacing( new Extent( 2 ) );
        add( content );

        // scope row
        Row scopeRow = new Row();
        scopeRow.setInsets( new Insets( 2 ) );
        scopeRow.setCellSpacing( new Extent( 2 ) );
        content.add( scopeRow );
        // scope select field
        scopeField = new SelectField();
        scopeField.setStyleName( "default" );
        scopeField.setWidth( new Extent( 50, Extent.EX ) );
        scopeField.addActionListener( scopeSelect );
        scopeRow.add( scopeField );
        DefaultListModel scopeListModel = (DefaultListModel) scopeField.getModel();
        scopeListModel.removeAll();
        // add publishers in the scope select field
        for ( Iterator publisherIterator = parent.getEnvironment().getPublishers().iterator();
              publisherIterator.hasNext(); )
        {
            Email email = (Email) publisherIterator.next();
            scopeListModel.add( email.getMailhost() );
        }
        if ( scopeListModel.size() > 0 )
        {
            scopeField.setSelectedIndex( 0 );
        }
        // copy publisher button
        Button copyPublisherButton = new Button( Styles.PAGE_COPY );
        copyPublisherButton.setToolTipText( Messages.getString( "copy" ) );
        copyPublisherButton.addActionListener( copyPublisher );
        scopeRow.add( copyPublisherButton );
        // edit publisher
        Button editPublisherButton = new Button( Styles.ACCEPT );
        editPublisherButton.setToolTipText( Messages.getString( "edit" ) );
        editPublisherButton.addActionListener( editPublisher );
        scopeRow.add( editPublisherButton );
        if ( getEnvironmentWindow().adminPermission )
        {
            // delete
            Button deletePublisherButton = new Button( Styles.DELETE );
            deletePublisherButton.setToolTipText( Messages.getString( "delete" ) );
            deletePublisherButton.addActionListener( deletePublisher );
            scopeRow.add( deletePublisherButton );
            // create publisher
            Button createPublisherButton = new Button( Styles.ADD );
            createPublisherButton.setToolTipText( Messages.getString( "publisher.add" ) );
            createPublisherButton.addActionListener( createPublisher );
            scopeRow.add( createPublisherButton );
        }

        // add the destinations grid
        grid = new Grid( 2 );
        grid.setStyleName( "border.grid" );
        grid.setColumnWidth( 0, new Extent( 50, Extent.PX ) );
        grid.setColumnWidth( 1, new Extent( 100, Extent.PERCENT ) );
        content.add( grid );

        // update the pane
        update();
    }

    /**
     * Update the pane.
     */
    public void update()
    {
        // update the scope select field
        String publisherMailhost = null;
        DefaultListModel scopeListModel = (DefaultListModel) scopeField.getModel();
        if ( scopeListModel.size() > 0 )
        {
            publisherMailhost = (String) scopeField.getSelectedItem();
        }
        scopeListModel.removeAll();
        int scopeIndex = 0;
        int found = -1;
        for ( Iterator publisherIterator = parent.getEnvironment().getPublishers().iterator();
              publisherIterator.hasNext(); )
        {
            Email publisher = (Email) publisherIterator.next();
            scopeListModel.add( publisher.getMailhost() );
            if ( publisher.getMailhost().equals( publisherMailhost ) )
            {
                found = scopeIndex;
            }
            scopeIndex++;
        }

        // remove all destinations grid children
        grid.removeAll();
        // check if at least one publisher is present
        if ( scopeListModel.size() < 1 )
        {
            return;
        }
        // update the scope index
        if ( found == -1 )
        {
            scopeField.setSelectedIndex( 0 );
        }
        else
        {
            scopeField.setSelectedIndex( found );
        }
        // update the publisher mailhost from the scope field (in case of deletion)
        publisherMailhost = (String) scopeField.getSelectedItem();

        // add destinations grid header
        Label actionHeader = new Label( " " );
        actionHeader.setStyleName( "grid.header" );
        grid.add( actionHeader );
        Label destinationHeader = new Label( Messages.getString( "destination" ) );
        destinationHeader.setStyleName( "grid.header" );
        grid.add( destinationHeader );
        // add the destinations e-mails
        for ( Iterator destinationIterator =
                  parent.getEnvironment().getPublisher( publisherMailhost ).getDestinations().iterator();
              destinationIterator.hasNext(); )
        {
            Destination destination = (Destination) destinationIterator.next();
            // row
            Row row = new Row();
            grid.add( row );
            // copy
            Button copyButton = new Button( Styles.PAGE_COPY );
            copyButton.setActionCommand( destination.getAddress() );
            copyButton.addActionListener( copyDestination );
            row.add( copyButton );
            if ( getEnvironmentWindow().adminPermission )
            {
                // edit
                Button editButton = new Button( Styles.ACCEPT );
                editButton.setToolTipText( Messages.getString( "apply" ) );
                editButton.setActionCommand( destination.getAddress() );
                editButton.addActionListener( editDestination );
                row.add( editButton );
                // delete
                Button deleteButton = new Button( Styles.DELETE );
                deleteButton.setToolTipText( Messages.getString( "delete" ) );
                deleteButton.setActionCommand( destination.getAddress() );
                deleteButton.addActionListener( deleteDestination );
                row.add( deleteButton );
            }
            // destination
            TextField destinationAddress = new TextField();
            destinationAddress.setId(
                "publisherdestination_" + parent.getEnvironmentName() + "_" + publisherMailhost + "_"
                    + destination.getAddress() );
            destinationAddress.setText( destination.getAddress() );
            destinationAddress.setStyleName( "default" );
            destinationAddress.setWidth( new Extent( 100, Extent.PERCENT ) );
            grid.add( destinationAddress );
        }

        // add create destination row in the destinations grid
        if ( getEnvironmentWindow().adminPermission )
        {
            // row
            Row row = new Row();
            row.setInsets( new Insets( 2 ) );
            row.setCellSpacing( new Extent( 2 ) );
            grid.add( row );
            // paste
            Button pasteButton = new Button( Styles.PAGE_PASTE );
            pasteButton.setToolTipText( Messages.getString( "paste" ) );
            pasteButton.addActionListener( pasteDestination );
            row.add( pasteButton );
            // add
            Button addButton = new Button( Styles.ADD );
            addButton.setToolTipText( Messages.getString( "destination.add" ) );
            addButton.addActionListener( createDestination );
            row.add( addButton );
            // destination
            newDestinationField = new TextField();
            newDestinationField.setStyleName( "default" );
            newDestinationField.setWidth( new Extent( 100, Extent.PERCENT ) );
            grid.add( newDestinationField );
        }
    }

    /**
     * Return the parent <code>EnvironmentWindow</code>.
     *
     * @return the parent <code>EnvironmentWindow</code>.
     */
    public EnvironmentWindow getEnvironmentWindow()
    {
        return parent;
    }

}