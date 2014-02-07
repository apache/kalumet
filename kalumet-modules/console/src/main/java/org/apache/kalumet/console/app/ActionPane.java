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
import nextapp.echo2.app.Label;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import org.apache.kalumet.console.configuration.ConfigurationManager;
import org.apache.kalumet.model.Agent;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.ws.client.EnvironmentClient;

/**
 * Environment updater pane.
 */
public class ActionPane
    extends ContentPane
{

    private EnvironmentWindow parent;

    // update thread
    class UpdateThread
        extends Thread
    {

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
                // call the webservice
                EnvironmentClient webServiceClient = new EnvironmentClient( agent.getHostname(), agent.getPort() );
                webServiceClient.update( parent.getEnvironmentName() );
            }
            catch ( Exception e )
            {
                message = "Environment " + parent.getEnvironmentName() + " update failed: " + e.getMessage();
                failure = true;
            }
            finally
            {
                ended = true;
            }
        }

    }

    // update
    private ActionListener update = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            // check if the user has the lock
            if ( !parent.getEnvironment().getLock().equals( KalumetConsoleApplication.getApplication().getUserid() ) )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "environment.locked" ) );
                return;
            }
            // check if no modifications are in progress
            if ( parent.isUpdated() )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "environment.notsaved" ) );
                return;
            }
            // check if the user can launch the update
            if ( !parent.adminPermission && !parent.updatePermission )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "action.restricted" ) );
                return;
            }

            // add confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
                new ConfirmWindow( new ActionListener()
                {
                    public void actionPerformed( ActionEvent event )
                    {
                        // put message in the action events column
                        KalumetConsoleApplication.getApplication().getLogPane().addInfo(
                            parent.getEnvironmentName() + " update in progress ...", parent.getEnvironmentName() );
                        // launch the asynchronous task
                        final UpdateThread updateThread = new UpdateThread();
                        updateThread.start();
                        // synchro
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
                                    }
                                    else
                                    {
                                        KalumetConsoleApplication.getApplication().getLogPane().addConfirm(
                                            "Environment " + parent.getEnvironmentName() + " updated.",
                                            parent.getEnvironmentName() );
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

    // publish release
    private ActionListener publishRelease = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            // check if the user has the lock
            if ( !parent.getEnvironment().getLock().equals( KalumetConsoleApplication.getApplication().getUserid() ) )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "environment.warn.locked" ) );
                return;
            }
            // check if the user has the permission to publish a release
            if ( !parent.adminPermission && !parent.releasePermission )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "action.restricted" ) );
                return;
            }
            // check if no modifications are in progress
            if ( parent.isUpdated() )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "environment.notsaved" ) );
                return;
            }
            if ( KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().getComponent(
                "publishreleasewindow_" + parent.getEnvironmentName() ) == null )
            {
                KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
                    new PublishReleaseWindow( parent ) );
            }
        }
    };

    // publish home page
    private ActionListener publishHomePage = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            // check if the user has the lock
            if ( !parent.getEnvironment().getLock().equals( KalumetConsoleApplication.getApplication().getUserid() ) )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "environment.warn.locked" ) );
                return;
            }
            // check if the user can publish homepage
            if ( !parent.adminPermission && !parent.homepagePermission )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "action.restricted" ) );
                return;
            }
            // check if no modifications are in progress
            if ( parent.isUpdated() )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "environment.notsaved" ) );
                return;
            }
            if ( KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().getComponent(
                "homepagewindow_" + parent.getEnvironmentName() ) == null )
            {
                KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
                    new HomePageWindow( parent ) );
            }
        }
    };

    /**
     * Create a new <code>UpdaterPane</code>.
     *
     * @param parent the parent <code>EnvironmentWindow</code>.
     */
    public ActionPane( EnvironmentWindow parent )
    {
        super();
        setStyleName( "tab.content" );

        // update parent
        this.parent = parent;

        Grid layout = new Grid( 2 );
        layout.setStyleName( "border.grid" );
        layout.setColumnWidth( 0, new Extent( 50, Extent.PX ) );
        add( layout );

        // add environment grid header
        Label environmentActionHeader = new Label( " " );
        environmentActionHeader.setStyleName( "grid.header" );
        layout.add( environmentActionHeader );
        Label environmentHeader = new Label( Messages.getString( "environment" ) );
        environmentHeader.setStyleName( "grid.header" );
        layout.add( environmentHeader );

        if ( parent.adminPermission || parent.updatePermission )
        {
            // update button
            Button updateButton = new Button( Styles.COG );
            updateButton.addActionListener( update );
            layout.add( updateButton );
            Button updateLabel = new Button( Messages.getString( "update" ) );
            updateLabel.setStyleName( "default" );
            updateLabel.addActionListener( update );
            layout.add( updateLabel );
        }

        if ( parent.adminPermission || parent.releasePermission )
        {
            // release button
            Button releaseButton = new Button( Styles.LORRY );
            releaseButton.addActionListener( publishRelease );
            layout.add( releaseButton );
            Button releaseLabel = new Button( Messages.getString( "release" ) );
            releaseLabel.setStyleName( "default" );
            releaseLabel.addActionListener( publishRelease );
            layout.add( releaseLabel );
        }

        if ( parent.adminPermission || parent.homepagePermission )
        {
            // homepage button
            Button homePageButton = new Button( Styles.DRIVE_WEB );
            homePageButton.addActionListener( publishHomePage );
            layout.add( homePageButton );
            Button homePageLabel = new Button( Messages.getString( "homepage" ) );
            homePageLabel.setStyleName( "default" );
            homePageLabel.addActionListener( publishHomePage );
            layout.add( homePageLabel );
        }

        // update this pane
        update();
    }

    /**
     * Update the pane.
     */
    public void update()
    {
        // nothing to do
    }

}
