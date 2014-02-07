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
import nextapp.echo2.app.list.ListModel;
import org.apache.kalumet.console.configuration.ConfigurationManager;
import org.apache.kalumet.model.Agent;
import org.apache.kalumet.model.JDBCConnectionPool;
import org.apache.kalumet.model.JDBCDataSource;
import org.apache.kalumet.model.JEEApplicationServer;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.ws.client.JDBCDataSourceClient;

import java.util.Iterator;

/**
 * Environment JDBC data sources pane.
 */
public class DataSourcesPane
    extends ContentPane
{

    private EnvironmentWindow parent;

    private SelectField scopeSelectField;

    private Grid grid;

    private boolean newIsActive = true;

    private boolean newIsBlocker = false;

    private TextField newNameField;

    private SelectField newConnectionPoolField;

    // status thread
    class StatusThread
        extends Thread
    {

        public String datasourceName;

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
                    throw new IllegalStateException( "agent not found." );
                }
                // call the web service
                JDBCDataSourceClient client = new JDBCDataSourceClient( agent.getHostname(), agent.getPort() );
                boolean ok = client.check( parent.getEnvironmentName(), (String) scopeSelectField.getSelectedItem(),
                                           datasourceName );
                if ( ok )
                {
                    message = "JDBC data source " + datasourceName + " up to date.";
                }
                else
                {
                    message = "JDBC data source " + datasourceName + " is not up to date.";
                }
            }
            catch ( Exception e )
            {
                failure = true;
                message = "JDBC data source " + datasourceName + " status check failed: " + e.getMessage();
            }
            finally
            {
                ended = true;
            }
        }

    }

    // update thread
    class UpdateThread
        extends Thread
    {

        public String datasourceName;

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
                    throw new IllegalStateException( "agent not found." );
                }
                // call the web service
                JDBCDataSourceClient client = new JDBCDataSourceClient( agent.getHostname(), agent.getPort() );
                client.update( parent.getEnvironmentName(), (String) scopeSelectField.getSelectedItem(),
                               datasourceName );
            }
            catch ( Exception e )
            {
                failure = true;
                message = "JDBC data source " + datasourceName + " update failed: " + e.getMessage();
            }
            finally
            {
                ended = true;
            }
        }

    }

    // scope select
    private ActionListener scopeSelect = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            update();
        }
    };

    // edit
    private ActionListener edit = new ActionListener()
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
            if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeResourcesChangePermission )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "action.restricted" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // get the data source name
            String dataSourceName = event.getActionCommand();
            // get the field
            TextField nameField = (TextField) DataSourcesPane.this.getComponent(
                "dsname_" + parent.getEnvironmentName() + "_" + (String) scopeSelectField.getSelectedItem() + "_"
                    + dataSourceName );
            SelectField connectionPoolField = (SelectField) DataSourcesPane.this.getComponent(
                "dspool_" + parent.getEnvironmentName() + "_" + (String) scopeSelectField.getSelectedItem() + "_"
                    + dataSourceName );
            // check if the user has selected a pool in the select field
            if ( connectionPoolField.getSelectedIndex() < 0 )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "datasource.noconnectionpool" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // get values
            String nameFieldValue = nameField.getText();
            String connectionPoolFieldValue = (String) connectionPoolField.getSelectedItem();
            // check mandatory fields
            if ( nameFieldValue == null || nameFieldValue.trim().length() < 1 || connectionPoolFieldValue == null
                || connectionPoolFieldValue.trim().length() < 1 )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "datasource.mandatory" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // if the user change the data source name, check if the data source
            // name is not already in use
            if ( !dataSourceName.equals( nameFieldValue ) )
            {
                if ( getEnvironmentWindow().getEnvironment().getJEEApplicationServers().getJEEApplicationServer(
                    (String) scopeSelectField.getSelectedItem() ).getJDBCDataSource( nameFieldValue ) != null )
                {
                    KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                        Messages.getString( "datasource.exists" ), getEnvironmentWindow().getEnvironmentName() );
                    return;
                }
            }
            // looking for the data source object
            JDBCDataSource dataSource = parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServer(
                (String) scopeSelectField.getSelectedItem() ).getJDBCDataSource( dataSourceName );
            if ( dataSource == null )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "datasource.notfound" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // add a change event
            parent.getChangeEvents().add(
                "Change JDBC data source " + dataSource.getName() + " / " + dataSource.getPool() );
            // change the data source object
            dataSource.setName( nameFieldValue );
            dataSource.setPool( connectionPoolFieldValue );
            // change the updated flag
            parent.setUpdated( true );
            // update the journal log tab pane
            parent.updateJournalPane();
            // update only the pane
            update();
        }
    };

    // create
    private ActionListener create = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            // check if the use has the environment lock
            if ( !getEnvironmentWindow().getEnvironment().getLock().equals(
                KalumetConsoleApplication.getApplication().getUserid() ) )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "environment.locked" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // check if the user can do it
            if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeResourcesChangePermission )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "action.restricted" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // check if the pool is selected
            ListModel listModel = newConnectionPoolField.getModel();
            if ( listModel.size() == 0 || newConnectionPoolField.getSelectedIndex() < 0 )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "datasource.noconnectionpool" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            String newConnectionPoolFieldValue = (String) newConnectionPoolField.getSelectedItem();
            String newNameFieldValue = newNameField.getText();
            // mandatory field
            if ( newConnectionPoolFieldValue == null || newConnectionPoolFieldValue.trim().length() < 1
                || newNameFieldValue == null || newNameFieldValue.trim().length() < 1 )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "datasource.mandatory" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // create the new data source
            JDBCDataSource dataSource = new JDBCDataSource();
            dataSource.setName( newNameFieldValue );
            dataSource.setPool( newConnectionPoolFieldValue );
            dataSource.setActive( newIsActive );
            // add the data source
            try
            {
                parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServer(
                    (String) scopeSelectField.getSelectedItem() ).addJDBCDataSource( dataSource );
            }
            catch ( Exception e )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "datasource.exists" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // add a change event
            parent.getChangeEvents().add(
                "Create JDBC data source " + dataSource.getName() + " / " + dataSource.getPool() );
            // change the updated flag
            parent.setUpdated( true );
            // update the journal log tab pane
            parent.updateJournalPane();
            // update only the pane
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
            if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeResourcesChangePermission )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "action.restricted" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // looking for the data source
            final JDBCDataSource dataSource =
                parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServer(
                    (String) scopeSelectField.getSelectedItem() ).getJDBCDataSource( event.getActionCommand() );
            if ( dataSource == null )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "datasource.notfound" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // display confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
                new ConfirmWindow( new ActionListener()
                {
                    public void actionPerformed( ActionEvent event )
                    {
                        // remove the data source
                        parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServer(
                            (String) scopeSelectField.getSelectedItem() ).getJDBCDataSources().remove( dataSource );
                        // add a change event
                        parent.getChangeEvents().add( "Delete JDBC data source " + dataSource.getName() );
                        // change the updated flag
                        parent.setUpdated( true );
                        // update the journal log tab pane
                        parent.updateJournalPane();
                        // update only the pane
                        update();
                    }
                } ) );
        }
    };

    // status
    private ActionListener status = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            // check if some change has not yet been saved
            if ( getEnvironmentWindow().isUpdated() )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "environment.notsaved" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // get JDBC data source name
            final String datasourceName = event.getActionCommand();
            // put a message into the log panel and in the journal
            KalumetConsoleApplication.getApplication().getLogPane().addInfo(
                "JDBC data source " + datasourceName + " status check in progress...", parent.getEnvironmentName() );
            parent.getChangeEvents().add( "JDBC data source " + datasourceName + " status check." );
            // start the status thread
            final StatusThread statusThread = new StatusThread();
            statusThread.datasourceName = datasourceName;
            statusThread.start();
            // sync with the client
            KalumetConsoleApplication.getApplication().enqueueTask(
                KalumetConsoleApplication.getApplication().getTaskQueue(), new Runnable()
            {
                public void run()
                {
                    if ( statusThread.ended )
                    {
                        if ( statusThread.failure )
                        {
                            KalumetConsoleApplication.getApplication().getLogPane().addWarning( statusThread.message,
                                                                                                parent.getEnvironmentName() );
                        }
                        else
                        {
                            KalumetConsoleApplication.getApplication().getLogPane().addInfo( statusThread.message,
                                                                                             parent.getEnvironmentName() );
                        }
                        parent.getChangeEvents().add( statusThread.message );
                    }
                    else
                    {
                        KalumetConsoleApplication.getApplication().enqueueTask(
                            KalumetConsoleApplication.getApplication().getTaskQueue(), this );
                    }
                }
            } );
        }
    };

    // update
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
            if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeResourcesUpdatePermission )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "action.restricted" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // check if something has been changed
            if ( getEnvironmentWindow().isUpdated() )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "environment.notsaved" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // get the data source name
            final String datasourceName = event.getActionCommand();
            // display confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
                new ConfirmWindow( new ActionListener()
                {
                    public void actionPerformed( ActionEvent event )
                    {
                        // put a message in the log pane and in the journal
                        KalumetConsoleApplication.getApplication().getLogPane().addInfo(
                            "JDBC data source " + datasourceName + " update in progress...",
                            parent.getEnvironmentName() );
                        parent.getChangeEvents().add( "JDBC data source " + datasourceName + " update requested." );
                        // start the update thread
                        final UpdateThread updateThread = new UpdateThread();
                        updateThread.datasourceName = datasourceName;
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
                                            "JDBC data source " + datasourceName + " updated.",
                                            parent.getEnvironmentName() );
                                        parent.getChangeEvents().add(
                                            "JDBC data source " + datasourceName + " updated." );
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
            if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeResourcesChangePermission )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "action.restricted" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // looking for the data source object
            JDBCDataSource dataSource = parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServer(
                (String) scopeSelectField.getSelectedItem() ).getJDBCDataSource( event.getActionCommand() );
            if ( dataSource == null )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "datasource.notfound" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // change the data source state and add a change event
            if ( dataSource.isActive() )
            {
                dataSource.setActive( false );
                parent.getChangeEvents().add( "Disable JDBC data source " + dataSource.getName() );
            }
            else
            {
                dataSource.setActive( true );
                parent.getChangeEvents().add( "Enable JDBC data source " + dataSource.getName() );
            }
            // change the updated flag
            parent.setUpdated( true );
            // update the journal log tab pane
            parent.updateJournalPane();
            // update the pane
            update();
        }
    };

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
            if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeResourcesChangePermission )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "action.restricted" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // looking for the data source object
            JDBCDataSource dataSource = parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServer(
                (String) scopeSelectField.getSelectedItem() ).getJDBCDataSource( event.getActionCommand() );
            if ( dataSource == null )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "datasource.notfound" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // change the data source blocker state and add a change event
            if ( dataSource.isBlocker() )
            {
                dataSource.setBlocker( false );
                parent.getChangeEvents().add( "Set Not blocker for JDBC data source " + dataSource.getName() );
            }
            else
            {
                dataSource.setBlocker( true );
                parent.getChangeEvents().add( "Set blocker for JDBC data source " + dataSource.getName() );
            }
            // change the updated flag
            parent.setUpdated( true );
            // update the journal log tab pane
            parent.updateJournalPane();
            // update the pane
            update();
        }
    };

    // new toggle active
    private ActionListener newToggleActive = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            // toggle the state
            if ( newIsActive )
            {
                newIsActive = false;
            }
            else
            {
                newIsActive = true;
            }
            // update the pane
            update();
        }
    };

    // new toggle blocker
    private ActionListener newToggleBlocker = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            // toggle the blocker state
            if ( newIsBlocker )
            {
                newIsBlocker = false;
            }
            else
            {
                newIsBlocker = true;
            }
            // update the pane
            update();
        }
    };

    // copy
    private ActionListener copy = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            // looking for the connection pool object
            JDBCDataSource dataSource = parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServer(
                (String) scopeSelectField.getSelectedItem() ).getJDBCDataSource( event.getActionCommand() );
            if ( dataSource == null )
            {
                return;
            }
            try
            {
                KalumetConsoleApplication.getApplication().setCopyComponent( dataSource.clone() );
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
            // check if the copy object is correct
            if ( copy == null || !( copy instanceof JDBCDataSource ) )
            {
                return;
            }
            // update the new fields
            newNameField.setText( ( (JDBCDataSource) copy ).getName() );
            DefaultListModel listModel = (DefaultListModel) newConnectionPoolField.getModel();
            newConnectionPoolField.setSelectedIndex( 0 );
            for ( int i = 0; i < listModel.size(); i++ )
            {
                String poolName = (String) listModel.get( i );
                if ( ( (JDBCDataSource) copy ).getPool().equals( poolName ) )
                {
                    newConnectionPoolField.setSelectedIndex( i );
                }
            }
        }
    };

    /**
     * Create a new <code>DataSourcesPane</code>.
     *
     * @param parent the parent <code>EnvironmentWindow</code>.
     */
    public DataSourcesPane( EnvironmentWindow parent )
    {
        super();
        setStyleName( "tab.content" );

        // update parent
        this.parent = parent;

        // column layout
        Column content = new Column();
        content.setCellSpacing( new Extent( 2 ) );
        add( content );

        // add the scope select field
        Grid layoutGrid = new Grid( 2 );
        layoutGrid.setStyleName( "default" );
        layoutGrid.setColumnWidth( 0, new Extent( 20, Extent.PERCENT ) );
        layoutGrid.setColumnWidth( 1, new Extent( 80, Extent.PERCENT ) );
        content.add( layoutGrid );
        Label scope = new Label( Messages.getString( "scope" ) );
        scope.setStyleName( "default" );
        layoutGrid.add( scope );
        scopeSelectField = new SelectField();
        scopeSelectField.addActionListener( scopeSelect );
        scopeSelectField.setStyleName( "default" );
        layoutGrid.add( scopeSelectField );
        DefaultListModel scopeListModel = (DefaultListModel) scopeSelectField.getModel();
        scopeListModel.removeAll();
        // add application servers in the scope select field
        for ( Iterator applicationServerIterator =
                  parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServers().iterator();
              applicationServerIterator.hasNext(); )
        {
            JEEApplicationServer applicationServer = (JEEApplicationServer) applicationServerIterator.next();
            scopeListModel.add( applicationServer.getName() );
        }
        if ( scopeListModel.size() > 0 )
        {
            scopeSelectField.setSelectedIndex( 0 );
        }

        // add JDBC data sources grid
        grid = new Grid( 3 );
        grid.setStyleName( "border.grid" );
        grid.setColumnWidth( 0, new Extent( 50, Extent.PX ) );
        grid.setColumnWidth( 1, new Extent( 50, Extent.PERCENT ) );
        grid.setColumnWidth( 2, new Extent( 50, Extent.PERCENT ) );
        content.add( grid );

        // update the pane
        update();
    }

    /**
     * Update the pane
     */
    public void update()
    {
        String applicationServerName = null;
        // update the scope select field
        DefaultListModel scopeListModel = (DefaultListModel) scopeSelectField.getModel();
        if ( scopeListModel.size() > 0 )
        {
            applicationServerName = (String) scopeSelectField.getSelectedItem();
        }
        scopeListModel.removeAll();
        int scopeIndex = 0;
        int found = -1;
        for ( Iterator applicationServerIterator =
                  parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServers().iterator();
              applicationServerIterator.hasNext(); )
        {
            JEEApplicationServer applicationServer = (JEEApplicationServer) applicationServerIterator.next();
            scopeListModel.add( applicationServer.getName() );
            if ( applicationServer.getName().equals( applicationServerName ) )
            {
                found = scopeIndex;
            }
            scopeIndex++;
        }

        // remove all JDBC data sources grid children
        grid.removeAll();
        // check if at least one application server is present
        if ( scopeListModel.size() < 1 )
        {
            return;
        }
        // update the scope select field selected index
        if ( found == -1 )
        {
            scopeSelectField.setSelectedIndex( 0 );
        }
        else
        {
            scopeSelectField.setSelectedIndex( found );
        }
        // update the application server name from the scope (in case of
        // application server deletion)
        applicationServerName = (String) scopeSelectField.getSelectedItem();

        Label dataSourceActionHeader = new Label( " " );
        dataSourceActionHeader.setStyleName( "grid.header" );
        grid.add( dataSourceActionHeader );
        Label dataSourceNameHeader = new Label( Messages.getString( "name" ) );
        dataSourceNameHeader.setStyleName( "grid.header" );
        grid.add( dataSourceNameHeader );
        Label dataSourcePoolHeader = new Label( Messages.getString( "connectionpool" ) );
        dataSourcePoolHeader.setStyleName( "grid.header" );
        grid.add( dataSourcePoolHeader );
        // add the jdbc data sources
        for ( Iterator jdbcDataSourceIterator =
                  parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServer(
                      applicationServerName ).getJDBCDataSources().iterator(); jdbcDataSourceIterator.hasNext(); )
        {
            JDBCDataSource dataSource = (JDBCDataSource) jdbcDataSourceIterator.next();
            // row
            Row row = new Row();
            row.setInsets( new Insets( 2 ) );
            row.setCellSpacing( new Extent( 2 ) );
            grid.add( row );
            // copy
            Button copyButton = new Button( Styles.PAGE_COPY );
            copyButton.setToolTipText( Messages.getString( "copy" ) );
            copyButton.setActionCommand( dataSource.getName() );
            copyButton.addActionListener( copy );
            row.add( copyButton );
            // active
            Button activeButton;
            if ( dataSource.isActive() )
            {
                activeButton = new Button( Styles.LIGHTBULB );
                activeButton.setToolTipText( Messages.getString( "switch.disable" ) );
            }
            else
            {
                activeButton = new Button( Styles.LIGHTBULB_OFF );
                activeButton.setToolTipText( Messages.getString( "switch.enable" ) );
            }
            if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeResourcesChangePermission )
            {
                activeButton.setActionCommand( dataSource.getName() );
                activeButton.addActionListener( toggleActive );
            }
            row.add( activeButton );
            // blocker
            Button blockerButton;
            if ( dataSource.isBlocker() )
            {
                blockerButton = new Button( Styles.PLUGIN );
                blockerButton.setToolTipText( Messages.getString( "switch.notblocker" ) );
            }
            else
            {
                blockerButton = new Button( Styles.PLUGIN_DISABLED );
                blockerButton.setToolTipText( Messages.getString( "switch.blocker" ) );
            }
            if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeResourcesChangePermission )
            {
                blockerButton.setActionCommand( dataSource.getName() );
                blockerButton.addActionListener( toggleBlocker );
            }
            row.add( blockerButton );
            // status
            Button statusButton = new Button( Styles.INFORMATION );
            statusButton.setToolTipText( Messages.getString( "status" ) );
            statusButton.setActionCommand( dataSource.getName() );
            statusButton.addActionListener( status );
            row.add( statusButton );
            if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeResourcesUpdatePermission )
            {
                // update
                Button updateButton = new Button( Styles.COG );
                updateButton.setToolTipText( Messages.getString( "update" ) );
                updateButton.setActionCommand( dataSource.getName() );
                updateButton.addActionListener( update );
                row.add( updateButton );
            }
            if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeResourcesChangePermission )
            {
                // edit
                Button editButton = new Button( Styles.ACCEPT );
                editButton.setToolTipText( Messages.getString( "apply" ) );
                editButton.setActionCommand( dataSource.getName() );
                editButton.addActionListener( edit );
                row.add( editButton );
                // delete
                Button deleteButton = new Button( Styles.DELETE );
                deleteButton.setToolTipText( Messages.getString( "delete" ) );
                deleteButton.setActionCommand( dataSource.getName() );
                deleteButton.addActionListener( delete );
                row.add( deleteButton );
            }
            // name
            TextField dataSourceName = new TextField();
            dataSourceName.setStyleName( "default" );
            dataSourceName.setWidth( new Extent( 100, Extent.PERCENT ) );
            dataSourceName.setId(
                "dsname_" + parent.getEnvironmentName() + "_" + applicationServerName + "_" + dataSource.getName() );
            dataSourceName.setText( dataSource.getName() );
            grid.add( dataSourceName );
            // data source pool
            SelectField dataSourcePool = new SelectField();
            dataSourcePool.setStyleName( "default" );
            dataSourcePool.setWidth( new Extent( 100, Extent.PERCENT ) );
            dataSourcePool.setId(
                "dspool_" + parent.getEnvironmentName() + "_" + applicationServerName + "_" + dataSource.getName() );
            DefaultListModel listModel = (DefaultListModel) dataSourcePool.getModel();
            listModel.removeAll();
            for ( Iterator poolIterator = parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServer(
                applicationServerName ).getJDBCConnectionPools().iterator(); poolIterator.hasNext(); )
            {
                JDBCConnectionPool pool = (JDBCConnectionPool) poolIterator.next();
                listModel.add( pool.getName() );
            }
            dataSourcePool.setSelectedItem( dataSource.getPool() );
            grid.add( dataSourcePool );
        }

        // add create data source row in the JDBC data sources grid
        if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeResourcesChangePermission )
        {
            // row
            Row row = new Row();
            row.setInsets( new Insets( 2 ) );
            row.setCellSpacing( new Extent( 2 ) );
            grid.add( row );
            // paste
            Button pasteButton = new Button( Styles.PAGE_PASTE );
            pasteButton.setToolTipText( Messages.getString( "paste" ) );
            pasteButton.addActionListener( paste );
            row.add( pasteButton );
            // active
            Button activeButton;
            if ( newIsActive )
            {
                activeButton = new Button( Styles.LIGHTBULB );
                activeButton.setToolTipText( Messages.getString( "switch.disable" ) );
            }
            else
            {
                activeButton = new Button( Styles.LIGHTBULB_OFF );
                activeButton.setToolTipText( Messages.getString( "switch.enable" ) );
            }
            activeButton.addActionListener( newToggleActive );
            row.add( activeButton );
            // blocker
            Button blockerButton;
            if ( newIsBlocker )
            {
                blockerButton = new Button( Styles.PLUGIN );
                blockerButton.setToolTipText( Messages.getString( "switch.notblocker" ) );
            }
            else
            {
                blockerButton = new Button( Styles.PLUGIN_DISABLED );
                blockerButton.setToolTipText( Messages.getString( "switch.blocker" ) );
            }
            blockerButton.addActionListener( newToggleBlocker );
            row.add( blockerButton );
            // add
            Button addButton = new Button( Styles.ADD );
            addButton.setToolTipText( Messages.getString( "add" ) );
            addButton.addActionListener( create );
            row.add( addButton );
            // name
            newNameField = new TextField();
            newNameField.setStyleName( "default" );
            newNameField.setWidth( new Extent( 100, Extent.PERCENT ) );
            grid.add( newNameField );
            // connection pool
            newConnectionPoolField = new SelectField();
            newConnectionPoolField.setStyleName( "default" );
            newConnectionPoolField.setWidth( new Extent( 100, Extent.PERCENT ) );
            DefaultListModel listModel = (DefaultListModel) newConnectionPoolField.getModel();
            listModel.removeAll();
            for ( Iterator poolIterator = parent.getEnvironment().getJEEApplicationServers().getJEEApplicationServer(
                applicationServerName ).getJDBCConnectionPools().iterator(); poolIterator.hasNext(); )
            {
                JDBCConnectionPool pool = (JDBCConnectionPool) poolIterator.next();
                listModel.add( pool.getName() );
            }
            grid.add( newConnectionPoolField );
        }
    }

    public EnvironmentWindow getEnvironmentWindow()
    {
        return parent;
    }

}