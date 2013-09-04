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
import nextapp.echo2.app.WindowPane;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.extras.app.TabPane;
import nextapp.echo2.extras.app.layout.TabPaneLayoutData;
import org.apache.kalumet.model.Destination;
import org.apache.kalumet.model.Email;

import java.util.Iterator;

/**
 * Email publisher window.
 */
public class PublisherWindow
  extends WindowPane
{

  private String mailhost;

  private Email email;

  private PublishersPane parent;

  private TextField mailhostField;

  private TextField fromField;

  private Grid destinationsGrid;

  private TextField newDestinationField;

  // close
  private ActionListener close = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      PublisherWindow.this.userClose();
    }
  };

  // refresh
  private ActionListener refresh = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // looking for the original email publisher object
      email = parent.getEnvironmentWindow().getEnvironment().getPublisher( mailhost );
      if ( email == null )
      {
        email = new Email();
      }
      // update the window
      update();
    }
  };

  // delete
  private ActionListener delete = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if the user has the environment lock
      if ( !getEnvironmentWindow().getEnvironment().getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !getEnvironmentWindow().adminPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // display confirm window
      KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
        new ConfirmWindow( new ActionListener()
        {
          public void actionPerformed( ActionEvent event )
          {
            // remove the publisher email object
            parent.getEnvironmentWindow().getEnvironment().getPublishers().remove( email );
            // add a change event
            parent.getEnvironmentWindow().getChangeEvents().add( "Delete publisher " + email.getMailhost() );
            // change the updated flag
            parent.getEnvironmentWindow().setUpdated( true );
            // update the journal log tab pane
            parent.getEnvironmentWindow().updateJournalPane();
            // update the parent pane
            parent.update();
            // close the window
            PublisherWindow.this.userClose();
          }
        } ) );
    }
  };

  // apply
  private ActionListener apply = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if the user has the environment lock
      if ( !getEnvironmentWindow().getEnvironment().getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !getEnvironmentWindow().adminPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // get fields value
      String mailhostFieldValue = mailhostField.getText();
      String fromFieldValue = fromField.getText();
      // check fields
      if ( mailhostFieldValue == null || mailhostFieldValue.trim().length() < 1 || fromFieldValue == null
        || fromFieldValue.trim().length() < 1 )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "publisher.mandatory" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // add a change event
      if ( mailhost != null )
      {
        parent.getEnvironmentWindow().getChangeEvents().add( "Change publisher " + email.getMailhost() );
      }
      // update the email publisher object
      email.setMailhost( mailhostFieldValue );
      email.setFrom( fromFieldValue );
      // add the email publisher object if needed
      if ( mailhost == null )
      {
        try
        {
          parent.getEnvironmentWindow().getEnvironment().addPublisher( email );
          parent.getEnvironmentWindow().getChangeEvents().add( "Add publisher " + email.getMailhost() );
        }
        catch ( Exception e )
        {
          KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "publisher.exists" ),
                                                                              getEnvironmentWindow().getEnvironmentName() );
          return;
        }
      }
      // update the window definition
      setTitle( Messages.getString( "publisher" ) + " " + email.getMailhost() );
      setId( "publisherwindow_" + parent.getEnvironmentWindow().getEnvironmentName() + "_" + email.getMailhost() );
      mailhost = email.getMailhost();
      // change the updated flag
      parent.getEnvironmentWindow().setUpdated( true );
      // update the journal log tab pane
      parent.getEnvironmentWindow().updateJournalPane();
      // update the parent pane
      parent.update();
      // update the window
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
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !getEnvironmentWindow().adminPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // looking for the destination object
      final Destination destination = email.getDestination( event.getActionCommand() );
      if ( destination == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "destination.notfound" ), getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // display confirm window
      KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
        new ConfirmWindow( new ActionListener()
        {
          public void actionPerformed( ActionEvent event )
          {
            // delete the destination object
            email.getDestinations().remove( destination );
            // add a change event
            parent.getEnvironmentWindow().getChangeEvents().add( "Delete destination " + destination.getAddress() );
            // change the updated flag
            parent.getEnvironmentWindow().setUpdated( true );
            // update the journal log tab pane
            parent.getEnvironmentWindow().updateJournalPane();
            // update the parent pane
            parent.update();
            // update the window
            update();
          }
        } ) );
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
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !getEnvironmentWindow().adminPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // get field
      TextField destinationAddressField = (TextField) PublisherWindow.this.getComponent(
        "destination_" + parent.getEnvironmentWindow().getEnvironmentName() + "_" + mailhost + "_"
          + event.getActionCommand() );
      // get field value
      String destinationAddressFieldValue = destinationAddressField.getText();
      // check field value
      if ( destinationAddressFieldValue == null || destinationAddressFieldValue.trim().length() < 1 )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "destination.mandatory" ), getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // if the user change the destination address, check if the address is
      // not already in user
      if ( !destinationAddressFieldValue.equals( event.getActionCommand() ) )
      {
        if ( email.getDestination( destinationAddressFieldValue ) != null )
        {
          KalumetConsoleApplication.getApplication().getLogPane().addWarning(
            Messages.getString( "destination.exists" ), getEnvironmentWindow().getEnvironmentName() );
          return;
        }
      }
      // looking for the destination object
      Destination destination = email.getDestination( event.getActionCommand() );
      if ( destination == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "destination.notfound" ), getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // add a change event
      parent.getEnvironmentWindow().getChangeEvents().add( "Change destination " + destination.getAddress() );
      // update the destination object
      destination.setAddress( destinationAddressFieldValue );
      // change the updated flag
      parent.getEnvironmentWindow().setUpdated( true );
      // update the journal log tab pane
      parent.getEnvironmentWindow().updateJournalPane();
      // update the parent pane
      parent.update();
      // update the window
      update();
    }
  };

  // add destination
  private ActionListener addDestination = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if the user has the environment lock
      if ( !getEnvironmentWindow().getEnvironment().getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !getEnvironmentWindow().adminPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // get field value
      String newDestinationFieldValue = newDestinationField.getText();
      // check field
      if ( newDestinationFieldValue == null || newDestinationFieldValue.trim().length() < 1 )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "destination.mandatory" ), getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // create a new destination object
      Destination destination = new Destination();
      destination.setAddress( newDestinationFieldValue );
      // add the destination
      try
      {
        email.addDestination( destination );
      }
      catch ( Exception e )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "destination.exists" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // add a change event
      parent.getEnvironmentWindow().getChangeEvents().add( "Add destination " + destination.getAddress() );
      // change the updated flag
      parent.getEnvironmentWindow().setUpdated( true );
      // update the journal log tab pane
      parent.getEnvironmentWindow().updateJournalPane();
      // update the parent pane
      parent.update();
      // update the window
      update();
    }
  };

  // copy
  private ActionListener copy = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      try
      {
        KalumetConsoleApplication.getApplication().setCopyComponent( email.clone() );
      }
      catch ( Exception e )
      {
        return;
      }
    }
  };

  // paste
  private ActionListener paste = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      Object copy = KalumetConsoleApplication.getApplication().getCopyComponent();
      // check the copy object
      if ( copy == null || !( copy instanceof Email ) )
      {
        return;
      }
      // update
      email = (Email) copy;
      mailhost = null;
      // update the window
      update();
    }
  };

  // copy destination
  private ActionListener copyDestination = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // looking for the destination object
      Destination destination = email.getDestination( event.getActionCommand() );
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
      // update the new fields
      newDestinationField.setText( ( (Destination) copy ).getAddress() );
    }
  };

  /**
   * Create a new <code>PublisherWindow</code>.
   *
   * @param parent   the parent <code>PublishersPane</code>.
   * @param mailhost the original mailhost.
   */
  public PublisherWindow( PublishersPane parent, String mailhost )
  {
    super();

    // update the parent tab pane
    this.parent = parent;

    // update the email mailhost
    this.mailhost = mailhost;

    // update the email object from the parent environment
    this.email = parent.getEnvironmentWindow().getEnvironment().getPublisher( mailhost );
    if ( this.email == null )
    {
      this.email = new Email();
    }

    if ( mailhost == null )
    {
      setTitle( Messages.getString( "publisher" ) );
    }
    else
    {
      setTitle( Messages.getString( "publisher" ) + " " + mailhost );
    }
    setId( "publisherwindow_" + parent.getEnvironmentWindow().getEnvironmentName() + "_" + mailhost );
    setStyleName( "default" );
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
    // add the copy button
    Button copyButton = new Button( Messages.getString( "copy" ), Styles.PAGE_COPY );
    copyButton.setStyleName( "control" );
    copyButton.addActionListener( copy );
    controlRow.add( copyButton );
    if ( getEnvironmentWindow().adminPermission )
    {
      // add the paste button
      Button pasteButton = new Button( Messages.getString( "paste" ), Styles.PAGE_PASTE );
      pasteButton.setStyleName( "control" );
      pasteButton.addActionListener( paste );
      controlRow.add( pasteButton );
      // add the apply button
      Button applyButton = new Button( Messages.getString( "apply" ), Styles.ACCEPT );
      applyButton.setStyleName( "control" );
      applyButton.addActionListener( apply );
      controlRow.add( applyButton );
      // add the delete button
      Button deleteButton = new Button( Messages.getString( "delete" ), Styles.DELETE );
      deleteButton.setStyleName( "control" );
      deleteButton.addActionListener( delete );
      controlRow.add( deleteButton );
    }
    // add the close button
    Button closeButton = new Button( Messages.getString( "close" ), Styles.CROSS );
    closeButton.setStyleName( "control" );
    closeButton.addActionListener( close );
    controlRow.add( closeButton );

    // add the main tab pane
    TabPane tabPane = new TabPane();
    tabPane.setStyleName( "default" );
    splitPane.add( tabPane );

    // add the general tab
    TabPaneLayoutData tabLayoutData = new TabPaneLayoutData();
    tabLayoutData.setTitle( Messages.getString( "general" ) );
    ContentPane generalTabPane = new ContentPane();
    generalTabPane.setStyleName( "default" );
    generalTabPane.setLayoutData( tabLayoutData );
    tabPane.add( generalTabPane );
    Grid generalLayoutGrid = new Grid( 2 );
    generalLayoutGrid.setStyleName( "default" );
    generalLayoutGrid.setWidth( new Extent( 100, Extent.PERCENT ) );
    generalLayoutGrid.setColumnWidth( 0, new Extent( 20, Extent.PERCENT ) );
    generalTabPane.add( generalLayoutGrid );
    // mailhost
    Label emailMailhostLabel = new Label( Messages.getString( "mailhost" ) );
    emailMailhostLabel.setStyleName( "default" );
    generalLayoutGrid.add( emailMailhostLabel );
    mailhostField = new TextField();
    mailhostField.setStyleName( "default" );
    mailhostField.setWidth( new Extent( 100, Extent.PERCENT ) );
    generalLayoutGrid.add( mailhostField );
    Label emailFromLabel = new Label( Messages.getString( "from" ) );
    emailFromLabel.setStyleName( "default" );
    generalLayoutGrid.add( emailFromLabel );
    fromField = new TextField();
    fromField.setStyleName( "default" );
    fromField.setWidth( new Extent( 100, Extent.PERCENT ) );
    generalLayoutGrid.add( fromField );

    // add the destinations tab
    tabLayoutData = new TabPaneLayoutData();
    tabLayoutData.setTitle( Messages.getString( "destinations" ) );
    ContentPane destinationsTabPane = new ContentPane();
    destinationsTabPane.setStyleName( "default" );
    destinationsTabPane.setLayoutData( tabLayoutData );
    tabPane.add( destinationsTabPane );
    destinationsGrid = new Grid( 2 );
    destinationsGrid.setStyleName( "border.grid" );
    destinationsTabPane.add( destinationsGrid );

    // update the window
    update();
  }

  /**
   * Update the pane.
   */
  public void update()
  {
    // update the email mailhost field
    mailhostField.setText( email.getMailhost() );
    // update the email from field
    fromField.setText( email.getFrom() );
    // update the destinations grid
    // remove all destinations grid children
    destinationsGrid.removeAll();
    // add destinations grid header
    Label actionHeader = new Label( "" );
    actionHeader.setStyleName( "grid.header" );
    destinationsGrid.add( actionHeader );
    Label destinationAddressHeader = new Label( Messages.getString( "address" ) );
    destinationAddressHeader.setStyleName( "grid.header" );
    destinationsGrid.add( destinationAddressHeader );
    // add the destinations e-mails
    for ( Iterator destinationIterator = email.getDestinations().iterator(); destinationIterator.hasNext(); )
    {
      Destination destination = (Destination) destinationIterator.next();
      // row
      Row row = new Row();
      row.setInsets( new Insets( 2 ) );
      row.setCellSpacing( new Extent( 2 ) );
      destinationsGrid.add( row );
      // copy
      Button copyButton = new Button( Styles.PAGE_COPY );
      copyButton.setActionCommand( destination.getAddress() );
      copyButton.addActionListener( copyDestination );
      row.add( copyButton );
      if ( getEnvironmentWindow().adminPermission )
      {
        // edit button
        Button editButton = new Button( Styles.ACCEPT );
        editButton.setActionCommand( destination.getAddress() );
        editButton.addActionListener( editDestination );
        row.add( editButton );
        // delete
        Button deleteButton = new Button( Styles.DELETE );
        deleteButton.setActionCommand( destination.getAddress() );
        deleteButton.addActionListener( deleteDestination );
        row.add( deleteButton );
      }
      // destination
      TextField destinationField = new TextField();
      destinationField.setId( "destination_" + parent.getEnvironmentWindow().getEnvironmentName() + "_" + mailhost + "_"
                                + destination.getAddress() );
      destinationField.setStyleName( "default" );
      destinationField.setText( destination.getAddress() );
      destinationsGrid.add( destinationField );
    }
    // add create destination row in the destinations grid
    if ( getEnvironmentWindow().adminPermission )
    {
      // row
      Row row = new Row();
      row.setInsets( new Insets( 2 ) );
      row.setCellSpacing( new Extent( 2 ) );
      destinationsGrid.add( row );
      // paste
      Button pasteButton = new Button( Styles.PAGE_PASTE );
      pasteButton.addActionListener( pasteDestination );
      row.add( pasteButton );
      // add
      Button addButton = new Button( Styles.ADD );
      addButton.addActionListener( addDestination );
      row.add( addButton );
      // destination
      newDestinationField = new TextField();
      newDestinationField.setStyleName( "default" );
      destinationsGrid.add( newDestinationField );
    }
  }

  public EnvironmentWindow getEnvironmentWindow()
  {
    return parent.getEnvironmentWindow();
  }

}