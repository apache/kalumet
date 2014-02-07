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
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import org.apache.kalumet.FileManipulator;
import org.apache.kalumet.console.configuration.ConfigurationManager;
import org.apache.kalumet.model.Agent;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.model.Software;
import org.apache.kalumet.ws.client.SoftwareClient;

import java.util.Iterator;

/**
 * Environment external manual applications pane.
 */
public class SoftwaresPane
    extends ContentPane
{

    private EnvironmentWindow parent;

    private Grid grid;

    // update thread
    class UpdateThread
        extends Thread
    {

        public String softwareName;

        public boolean ended = false;

        public boolean failure = false;

        public String message;

        public void run()
        {
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
                SoftwareClient client = new SoftwareClient( agent.getHostname(), agent.getPort() );
                client.update( parent.getEnvironmentName(), softwareName, false );
            }
            catch ( Exception e )
            {
                failure = true;
                message = "Software " + softwareName + " update failed: " + e.getMessage();
            }
            finally
            {
                ended = true;
            }
        }
    }

    // toggle blocker
    private ActionListener toggleBlocker = new ActionListener()
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
            if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().softwareChangePermission )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "action.restricted" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // looking for the external object
            Software software = parent.getEnvironment().getSoftware( event.getActionCommand() );
            if ( software == null )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "external.notfound" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // change the external state and add a change event
            if ( software.isBlocker() )
            {
                software.setBlocker( false );
                parent.getChangeEvents().add( "Set not blocker for software " + software.getName() );
            }
            else
            {
                software.setBlocker( true );
                parent.getChangeEvents().add( "Set blocker for software " + software.getName() );
            }
            // change the updated flag
            parent.setUpdated( true );
            // update the journal log pane
            parent.updateJournalPane();
            // update the pane
            update();
        }
    };

    // toggle active
    private ActionListener toggleActive = new ActionListener()
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
            if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().softwareChangePermission )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "action.restricted" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // looking for the software object
            Software software = parent.getEnvironment().getSoftware( event.getActionCommand() );
            if ( software == null )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "software.notfound" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // change the external state and add a change event
            if ( software.isActive() )
            {
                software.setActive( false );
                parent.getChangeEvents().add( "Disable software " + software.getName() );
            }
            else
            {
                software.setActive( true );
                parent.getChangeEvents().add( "Enable software " + software.getName() );
            }
            // change the updated flag
            parent.setUpdated( true );
            // update the journal log pane
            parent.updateJournalPane();
            // update the pane
            update();
        }
    };

    // toggle before
    private ActionListener toggleBefore = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            // check if the user has the lock
            if ( !getEnvironmentWindow().getEnvironment().getLock().equals(
                KalumetConsoleApplication.getApplication().getUserid() ) )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "environment.locked" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // check if the user can do it
            if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().softwareChangePermission )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "action.restricted" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // looking for the software object
            Software software = parent.getEnvironment().getSoftware( event.getActionCommand() );
            if ( software == null )
            {
                return;
            }
            // change the external software order
            if ( software.isBeforejee() )
            {
                software.setBeforejee( false );
                parent.getChangeEvents().add(
                    "Flag software " + software.getName() + " to be updated after JEE resources." );
            }
            else
            {
                software.setBeforejee( true );
                parent.getChangeEvents().add(
                    "Flag software " + software.getName() + " to be updated before JEE resources." );
            }
            // change the updated flag
            parent.setUpdated( true );
            // update the journal log pane
            parent.updateJournalPane();
            // update this pane
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
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "environment.locked" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // check if the user can do it
            if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().softwareChangePermission )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "action.restricted" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // looking for the external object
            final Software software = parent.getEnvironment().getSoftware( event.getActionCommand() );
            if ( software == null )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "software.notfound" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // display confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
                new ConfirmWindow( new ActionListener()
                {
                    public void actionPerformed( ActionEvent event )
                    {
                        // delete the external object
                        parent.getEnvironment().getSoftwares().remove( software );
                        // add a change event
                        parent.getChangeEvents().add( "Delete software " + software.getName() );
                        // change the updated flag
                        parent.setUpdated( true );
                        // update the whole environment window
                        parent.update();
                        // update the pane
                        update();
                    }
                } ) );
        }
    };

    // edit
    private ActionListener edit = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            if ( KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().getComponent(
                "softwarewindow_" + parent.getEnvironmentName() + "_" + event.getActionCommand() ) == null )
            {
                KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
                    new SoftwareWindow( SoftwaresPane.this, event.getActionCommand() ) );
            }
        }
    };

    // create
    private ActionListener create = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
                new SoftwareWindow( SoftwaresPane.this, null ) );
        }
    };

    // up
    private ActionListener up = new ActionListener()
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
            if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().softwareChangePermission )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "action.restricted" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // looking for the external object
            Software software = parent.getEnvironment().getSoftware( event.getActionCommand() );
            if ( software == null )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "software.notfound" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // get the external index
            int index = parent.getEnvironment().getSoftwares().indexOf( software );
            // if the external index is the first one or the object is not found,
            // do nothing, the list must contains at least 2 externals
            if ( index == 0 || index == -1 || parent.getEnvironment().getSoftwares().size() < 2 )
            {
                return;
            }
            // get the previous external
            Software previous = (Software) parent.getEnvironment().getSoftwares().get( index - 1 );
            // switch external
            parent.getEnvironment().getSoftwares().set( index, previous );
            parent.getEnvironment().getSoftwares().set( index - 1, software );
            // update the pane
            update();
        }
    };

    // down
    private ActionListener down = new ActionListener()
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
            if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().softwareChangePermission )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "action.restricted" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // looking for the software object
            Software software = parent.getEnvironment().getSoftware( event.getActionCommand() );
            if ( software == null )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "software.notfound" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // get the external index
            int index = parent.getEnvironment().getSoftwares().indexOf( software );
            // if the external index is the last one or the object is not found,
            // the list must contains at least 2 externals
            if ( index == -1 || index == parent.getEnvironment().getSoftwares().size() - 1
                || parent.getEnvironment().getSoftwares().size() < 2 )
            {
                return;
            }
            // get the next external
            Software next = (Software) parent.getEnvironment().getSoftwares().get( index + 1 );
            // switch the external
            parent.getEnvironment().getSoftwares().set( index + 1, software );
            parent.getEnvironment().getSoftwares().set( index, next );
            // update the pane
            update();
        }
    };

    // copy
    private ActionListener copy = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            // looking for the software object
            Software software = parent.getEnvironment().getSoftware( event.getActionCommand() );
            if ( software == null )
            {
                return;
            }
            try
            {
                KalumetConsoleApplication.getApplication().setCopyComponent( software.clone() );
            }
            catch ( Exception e )
            {
                return;
            }
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
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "environment.locked" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // check if the user can do it
            if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().softwareChangePermission )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "action.restricted" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // get the external software name
            String name = event.getActionCommand();
            // get the external software URI textfield
            TextField uriField = (TextField) SoftwaresPane.this.getComponent(
                "softwareuri_" + parent.getEnvironmentName() + "_" + name );
            // get the external software URI
            String uriFieldValue = uriField.getText();
            // looking for the software object
            Software software = parent.getEnvironment().getSoftware( name );
            if ( software == null )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "software.notfound" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // add a change event
            parent.getChangeEvents().add( "Change software " + software.getName() + " URI to " + uriFieldValue );
            // change the external software URI
            software.setUri( uriFieldValue );
            // change the updated flag
            parent.setUpdated( true );
            // update the journal log tab pane
            parent.updateJournalPane();
            // update this pane
            update();
        }
    };

    private ActionListener update = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            // check if the user has the lock
            if ( !getEnvironmentWindow().getEnvironment().getLock().equals(
                KalumetConsoleApplication.getApplication().getUserid() ) )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "environment.locked" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // check if the user can do it
            if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().softwareUpdatePermission )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "action.restricted" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // check if some change has not been saved
            if ( parent.isUpdated() )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "environment.notsaved" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // get the software name
            final String softwareName = event.getActionCommand();
            // display confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
                new ConfirmWindow( new ActionListener()
                {
                    public void actionPerformed( ActionEvent event )
                    {
                        // put a message into the log pane and the journal
                        KalumetConsoleApplication.getApplication().getLogPane().addInfo(
                            "Software " + softwareName + " update in progress ...", parent.getEnvironmentName() );
                        parent.getChangeEvents().add( "Software " + softwareName + " update requested." );
                        // start the update thread
                        final UpdateThread updateThread = new UpdateThread();
                        updateThread.softwareName = softwareName;
                        updateThread.start();
                        // sync with the client
                        KalumetConsoleApplication.getApplication().enqueueTask(
                            KalumetConsoleApplication.getApplication().getTaskQueue(), new Runnable()
                        {
                            public void run()
                            {
                                if ( updateThread.ended )
                                {
                                    if ( updateThread.failure )
                                    {
                                        KalumetConsoleApplication.getApplication().getLogPane().addError(
                                            updateThread.message, parent.getEnvironmentName() );
                                        parent.getChangeEvents().add( updateThread.message );
                                    }
                                    else
                                    {
                                        KalumetConsoleApplication.getApplication().getLogPane().addConfirm(
                                            "Software " + softwareName + " updated.", parent.getEnvironmentName() );
                                        parent.getChangeEvents().add( "Software " + softwareName + " updated." );
                                    }
                                }
                                else
                                {
                                    KalumetConsoleApplication.getApplication().enqueueTask(
                                        KalumetConsoleApplication.getApplication().getTaskQueue(), this );
                                }
                            }
                        } );
                    }
                } ) );
        }
    };

    // test URI
    private ActionListener testUri = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            String name = event.getActionCommand();
            TextField uriField = (TextField) SoftwaresPane.this.getComponent(
                "softwareuri_" + parent.getEnvironmentName() + "_" + name );
            String uri = FileManipulator.format( uriField.getText() );
            boolean exists = false;
            FileManipulator fileManipulator = null;
            try
            {
                fileManipulator = new FileManipulator();
                exists = fileManipulator.exists( uri );
            }
            catch ( Exception e )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    "Can't check the URI " + uri + ": " + e.getMessage(), parent.getEnvironmentName() );
            }
            finally
            {
                if ( fileManipulator != null )
                {
                    fileManipulator.close();
                }
            }
            if ( exists )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addConfirm( "URI " + uri + " exists.",
                                                                                    parent.getEnvironmentName() );
            }
            else
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning( "URI " + uri + " doesn't exists.",
                                                                                    parent.getEnvironmentName() );
            }
        }
    };

    /**
     * Create a new <code>ExternalsPane</code>.
     *
     * @param parent the parent <code>EnvironmentWindow</code>.
     */
    public SoftwaresPane( EnvironmentWindow parent )
    {
        super();
        setStyleName( "tab.content" );

        // update parent
        this.parent = parent;

        // column layout
        Column content = new Column();
        content.setCellSpacing( new Extent( 2 ) );
        add( content );

        // add the create button
        if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().softwareChangePermission )
        {
            Button createButton = new Button( Messages.getString( "software.add" ), Styles.ADD );
            createButton.addActionListener( create );
            content.add( createButton );
        }

        // add external softwares grid
        grid = new Grid( 4 );
        grid.setStyleName( "border.grid" );
        grid.setColumnWidth( 0, new Extent( 50, Extent.PX ) );
        grid.setColumnWidth( 1, new Extent( 20, Extent.PERCENT ) );
        grid.setColumnWidth( 2, new Extent( 60, Extent.PERCENT ) );
        grid.setColumnWidth( 3, new Extent( 20, Extent.PERCENT ) );
        content.add( grid );

        // update the pane
        update();
    }

    /**
     * Update the pane.
     */
    public void update()
    {
        // remove all softwares grid children
        grid.removeAll();

        // add softwares grid header
        Label actionHeader = new Label( " " );
        actionHeader.setStyleName( "grid.header" );
        grid.add( actionHeader );
        Label nameHeader = new Label( Messages.getString( "name" ) );
        nameHeader.setStyleName( "grid.header" );
        grid.add( nameHeader );
        Label uriHeader = new Label( Messages.getString( "uri" ) );
        uriHeader.setStyleName( "grid.header" );
        grid.add( uriHeader );
        Label agentHeader = new Label( Messages.getString( "agent" ) );
        agentHeader.setStyleName( "grid.header" );
        grid.add( agentHeader );
        // add the softwares
        for ( Iterator softwareIterator = parent.getEnvironment().getSoftwares().iterator();
              softwareIterator.hasNext(); )
        {
            Software software = (Software) softwareIterator.next();
            // row
            Row row = new Row();
            row.setInsets( new Insets( 2 ) );
            row.setCellSpacing( new Extent( 2 ) );
            grid.add( row );
            // before JEE resources
            Button orderButton;
            if ( software.isBeforejee() )
            {
                orderButton = new Button( Styles.BOOK_PREVIOUS );
                orderButton.setToolTipText( Messages.getString( "switch.afterjee" ) );
            }
            else
            {
                orderButton = new Button( Styles.BOOK_NEXT );
                orderButton.setToolTipText( Messages.getString( "switch.beforejee" ) );
            }
            if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().softwareChangePermission )
            {
                orderButton.setActionCommand( software.getName() );
                orderButton.addActionListener( toggleBefore );
            }
            row.add( orderButton );
            // copy
            Button copyButton = new Button( Styles.PAGE_COPY );
            copyButton.setToolTipText( Messages.getString( "copy" ) );
            copyButton.setActionCommand( software.getName() );
            copyButton.addActionListener( copy );
            row.add( copyButton );
            // active
            Button activeButton;
            if ( software.isActive() )
            {
                activeButton = new Button( Styles.LIGHTBULB );
                activeButton.setToolTipText( Messages.getString( "switch.disable" ) );
            }
            else
            {
                activeButton = new Button( Styles.LIGHTBULB_OFF );
                activeButton.setToolTipText( Messages.getString( "switch.enable" ) );
            }
            if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().softwareChangePermission )
            {
                activeButton.setActionCommand( software.getName() );
                activeButton.addActionListener( toggleActive );
            }
            row.add( activeButton );
            // blocker
            Button blockerButton;
            if ( software.isBlocker() )
            {
                blockerButton = new Button( Styles.PLUGIN );
                blockerButton.setToolTipText( Messages.getString( "switch.notblocker" ) );
            }
            else
            {
                blockerButton = new Button( Styles.PLUGIN_DISABLED );
                blockerButton.setToolTipText( Messages.getString( "switch.blocker" ) );
            }
            if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().softwareChangePermission )
            {
                blockerButton.setActionCommand( software.getName() );
                blockerButton.addActionListener( toggleBlocker );
            }
            row.add( blockerButton );
            if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().softwareChangePermission )
            {
                // up
                Button upButton = new Button( Styles.ARROW_UP );
                upButton.setToolTipText( Messages.getString( "up" ) );
                upButton.setActionCommand( software.getName() );
                upButton.addActionListener( up );
                row.add( upButton );
                // down
                Button downButton = new Button( Styles.ARROW_DOWN );
                downButton.setToolTipText( Messages.getString( "down" ) );
                downButton.setActionCommand( software.getName() );
                downButton.addActionListener( down );
                row.add( downButton );
            }
            if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().softwareUpdatePermission )
            {
                // update
                Button updateButton = new Button( Styles.COG );
                updateButton.setToolTipText( Messages.getString( "update" ) );
                updateButton.setActionCommand( software.getName() );
                updateButton.addActionListener( update );
                row.add( updateButton );
            }
            if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().softwareChangePermission )
            {
                // apply
                Button applyButton = new Button( Styles.ACCEPT );
                applyButton.setToolTipText( Messages.getString( "apply" ) );
                applyButton.setActionCommand( software.getName() );
                applyButton.addActionListener( apply );
                row.add( applyButton );
                // delete
                Button deleteButton = new Button( Styles.DELETE );
                deleteButton.setToolTipText( Messages.getString( "delete" ) );
                deleteButton.setActionCommand( software.getName() );
                deleteButton.addActionListener( delete );
                row.add( deleteButton );
            }
            // name
            Button nameButton = new Button( software.getName() );
            nameButton.setActionCommand( software.getName() );
            nameButton.addActionListener( edit );
            nameButton.setStyleName( "default" );
            grid.add( nameButton );
            // uri
            Row uriRow = new Row();
            grid.add( uriRow );
            TextField uri = new TextField();
            uri.setStyleName( "default" );
            uri.setWidth( new Extent( 500, Extent.PX ) );
            uri.setId( "softwareuri_" + parent.getEnvironmentName() + "_" + software.getName() );
            uri.setText( software.getUri() );
            uriRow.add( uri );
            Button uriTestButton = new Button( Styles.WORLD );
            uriTestButton.setToolTipText( Messages.getString( "uri.test" ) );
            uriTestButton.setActionCommand( software.getName() );
            uriTestButton.addActionListener( testUri );
            uriRow.add( uriTestButton );
            // agent
            Label agent = new Label( software.getAgent() );
            agent.setStyleName( "default" );
            grid.add( agent );
        }
    }

    /**
     * Get the parent <code>EnvironmentWindow</code>.
     *
     * @return the parent <code>EnvironmentWindow</code>.
     */
    public EnvironmentWindow getEnvironmentWindow()
    {
        return parent;
    }

}
