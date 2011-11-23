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
import nextapp.echo2.app.Row;
import nextapp.echo2.app.SplitPane;
import nextapp.echo2.app.WindowPane;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.extras.app.TabPane;
import nextapp.echo2.extras.app.layout.TabPaneLayoutData;
import org.apache.kalumet.console.configuration.ConfigurationManager;
import org.apache.kalumet.model.Agent;
import org.apache.kalumet.model.JDBCConnectionPool;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.ws.client.JDBCConnectionPoolClient;

/**
 * JDBC connection pool window.
 */
public class ConnectionPoolWindow extends WindowPane {

    private String connectionPoolName;
    private String applicationServerName;
    private JDBCConnectionPool connectionPool = null;
    private ConnectionPoolsPane parent;
    private ConnectionPoolGeneralPane generalPane;
    private ConnectionPoolDriverPane driverPane;
    private ConnectionPoolDatabasePane databasePane;
    private ConnectionPoolCapacityPane capacityPane;

    // status thread
    class StatusThread extends Thread {

        public boolean ended = false;
        public boolean failure = false;
        public String message;

        public void run() {
            try {
                // load Kalumet configuration
                Kalumet kalumet = ConfigurationManager.loadStore();
                // looking for the agent
                Agent agent = kalumet.getAgent(parent.getEnvironmentWindow().getEnvironment().getAgent());
                if (agent == null) {
                    throw new IllegalArgumentException("agent not found.");
                }
                // call the webservice
                JDBCConnectionPoolClient client = new JDBCConnectionPoolClient(agent.getHostname(), agent.getPort());
                boolean uptodate = client.check(parent.getEnvironmentWindow().getEnvironmentName(), applicationServerName, connectionPoolName);
                if (uptodate) {
                    message = "JDBC connection pool " + connectionPoolName + " is up to date.";
                } else {
                    failure = true;
                    message = "JDBC connection pool " + connectionPoolName + " is not up to date.";
                }
            } catch (Exception e) {
                failure = true;
                message = "JDBC connection pool " + connectionPoolName + " status check failed: " + e.getMessage();
            } finally {
                ended = true;
            }
        }
    }

    // update thread
    class UpdateThread extends Thread {

        public boolean ended = false;
        public boolean failure = false;
        public String message;

        public void run() {
            try {
                // load Kalumet configuration
                Kalumet kalumet = ConfigurationManager.loadStore();
                // looking for the agent
                Agent agent = kalumet.getAgent(parent.getEnvironmentWindow().getEnvironment().getAgent());
                if (agent == null) {
                    throw new IllegalArgumentException("agent not found.");
                }
                // call the webservice
                JDBCConnectionPoolClient client = new JDBCConnectionPoolClient(agent.getHostname(), agent.getPort());
                client.update(parent.getEnvironmentWindow().getEnvironmentName(), applicationServerName, connectionPoolName);
            } catch (Exception e) {
                failure = true;
                message = "JDBC connection pool " + connectionPoolName + " update failed: " + e.getMessage();
            } finally {
                ended = true;
            }
        }
    }

    // close
    private ActionListener close = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            ConnectionPoolWindow.this.userClose();
        }
    };
    // refresh
    private ActionListener refresh = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // looking for original JDBC connection pool object
            connectionPool = parent.getEnvironmentWindow().getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(applicationServerName).getJDBCConnectionPool(connectionPoolName);
            if (connectionPool == null) {
                connectionPool = new JDBCConnectionPool();
            }
            // update the window
            update();
        }
    };
    // delete
    private ActionListener delete = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the environment lock
            if (!getEnvironmentWindow().getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!getEnvironmentWindow().adminPermission
                    && !getEnvironmentWindow().jeeResourcesPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // display confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new ConfirmWindow(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    // remove the connection pool
                    parent.getEnvironmentWindow().getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(applicationServerName).getJDBCConnectionPools().remove(connectionPool);
                    // add a change event
                    parent.getEnvironmentWindow().getChangeEvents().add("Delete JDBC connection pool " + connectionPool.getName());
                    // change the updated flag
                    parent.getEnvironmentWindow().setUpdated(true);
                    // update the whole parent window
                    parent.getEnvironmentWindow().update();
                    // close the window
                    ConnectionPoolWindow.this.userClose();
                }
            }));
        }
    };
    // apply
    private ActionListener apply = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the environment lock
            if (!getEnvironmentWindow().getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!getEnvironmentWindow().adminPermission
                    && !getEnvironmentWindow().jeeResourcesPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricited"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // get fields value
            String nameFieldValue = generalPane.getNameField().getText();
            int activeFieldIndex = generalPane.getActiveField().getSelectedIndex();
            int blockerFieldIndex = generalPane.getBlockerField().getSelectedIndex();
            int driverFieldIndex = driverPane.getDriverField().getSelectedIndex();
            int helperFieldIndex = driverPane.getHelperField().getSelectedIndex();
            String classpathFieldValue = driverPane.getClasspathField().getText();
            String urlFieldValue = databasePane.getUrlField().getText();
            String userFieldValue = databasePane.getUserField().getText();
            String passwordFieldValue = databasePane.getPasswordField().getText();
            String confirmPasswordFieldValue = databasePane.getConfirmPasswordField().getText();
            String initialFieldValue = capacityPane.getInitialField().getText();
            String maximalFieldValue = capacityPane.getMaximalField().getText();
            String incrementFieldValue = capacityPane.getIncrementField().getText();
            // check select fields
            if (activeFieldIndex < 0 || driverFieldIndex < 0 || helperFieldIndex < 0) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("connectionpool.selected"));
                return;
            }
            // check name, url, user are mandatory
            if (nameFieldValue == null || nameFieldValue.trim().length() < 1 || urlFieldValue == null || urlFieldValue.trim().length() < 1 || userFieldValue == null
                    || userFieldValue.trim().length() < 1) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("connectionpool.mandatory"));
                return;
            }
            // check number value
            int jdbcConnectionPoolInitialFieldNumber;
            int jdbcConnectionPoolMaximalFieldNumber;
            int jdbcConnectionPoolIncrementFieldNumber;
            try {
                jdbcConnectionPoolInitialFieldNumber = new Integer(initialFieldValue).intValue();
                jdbcConnectionPoolMaximalFieldNumber = new Integer(maximalFieldValue).intValue();
                jdbcConnectionPoolIncrementFieldNumber = new Integer(incrementFieldValue).intValue();
            } catch (Exception e) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("connectionpool.notinteger"));
                return;
            }
            // check password matching
            if (!passwordFieldValue.equals(confirmPasswordFieldValue)) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("connectionpool.passwordmatch"));
                return;
            }
            // if the user change the JDBC connection pool name, check if the JDBC
            // connection pool name doesn't already exist
            if (connectionPoolName == null || (connectionPoolName != null && !connectionPoolName.equals(nameFieldValue))) {
                if (parent.getEnvironmentWindow().getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(applicationServerName).getJDBCConnectionPool(nameFieldValue) != null) {
                    KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("connectionpool.exists"));
                    return;
                }
            }
            // add a change event
            if (connectionPoolName != null) {
                parent.getEnvironmentWindow().getChangeEvents().add("Change JDBC connection pool " + connectionPool.getName());
            }
            // update the connection pool object
            connectionPool.setName(nameFieldValue);
            if (activeFieldIndex == 0) {
                connectionPool.setActive(true);
            } else {
                connectionPool.setActive(false);
            }
            if (blockerFieldIndex == 0) {
                connectionPool.setBlocker(true);
            } else {
                connectionPool.setBlocker(false);
            }
            if (driverFieldIndex == 0) {
                connectionPool.setDriver("oracle.jdbc.driver.OracleDriver");
            }
            if (driverFieldIndex == 1) {
                connectionPool.setDriver("oracle.jdbc.xa.client.OracleXADataSource");
            }
            if (driverFieldIndex == 2) {
                connectionPool.setDriver("com.ibm.db2.jcc.DB2Driver");
            }
            if (driverFieldIndex == 3) {
                connectionPool.setDriver("com.mysql.jdbc.Driver");
            }
            if (driverFieldIndex == 4) {
                connectionPool.setDriver("org.postgresql.Driver");
            }
            if (helperFieldIndex == 0) {
                connectionPool.setHelperclass(null);
            }
            if (helperFieldIndex == 1) {
                connectionPool.setHelperclass("com.ibm.websphere.rsadapter.GenericDataStoreHelper");
            }
            if (helperFieldIndex == 2) {
                connectionPool.setHelperclass("com.ibm.websphere.rsadapter.OracleDataStoreHelper");
            }
            connectionPool.setClasspath(classpathFieldValue);
            connectionPool.setUrl(urlFieldValue);
            connectionPool.setUser(userFieldValue);
            connectionPool.setPassword(passwordFieldValue);
            connectionPool.setInitial(jdbcConnectionPoolInitialFieldNumber);
            connectionPool.setMaximal(jdbcConnectionPoolMaximalFieldNumber);
            connectionPool.setIncrement(jdbcConnectionPoolIncrementFieldNumber);
            // add the application server object if needed
            if (connectionPoolName == null) {
                try {
                    parent.getEnvironmentWindow().getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(applicationServerName).addJDBCConnectionPool(connectionPool);
                    parent.getEnvironmentWindow().getChangeEvents().add("Add JDBC connection pool " + connectionPool.getName());
                } catch (Exception e) {
                    KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("connectionpool.exists"));
                    return;
                }
            }
            // update the window definition
            setTitle(Messages.getString("connectionpool") + " " + connectionPool.getName());
            setId("connectionpoolwindow_" + parent.getEnvironmentWindow().getEnvironmentName() + "_" + applicationServerName + "_" + connectionPool.getName());
            connectionPoolName = connectionPool.getName();
            // change the updated flag
            parent.getEnvironmentWindow().setUpdated(true);
            // update the window
            update();
            // update the whole environment window
            parent.getEnvironmentWindow().update();
        }
    };
    // copy
    private ActionListener copy = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            try {
                KalumetConsoleApplication.getApplication().setCopyComponent(connectionPool.clone());
            } catch (Exception e) {
                return;
            }
        }
    };
    // paste
    private ActionListener paste = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the copy is correct
            Object copy = KalumetConsoleApplication.getApplication().getCopyComponent();
            if (copy == null || !(copy instanceof JDBCConnectionPool)) {
                return;
            }
            connectionPool = (JDBCConnectionPool) copy;
            connectionPoolName = null;
            // update the parent pane
            parent.update();
            // update the window
            update();
        }
    };
    // status
    private ActionListener status = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if some change has not yet been saved
            if (getEnvironmentWindow().isUpdated()) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.notsaved"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // add a message into the log pane and the journal
            KalumetConsoleApplication.getApplication().getLogPane().addInfo("JDBC connection pool " + connectionPoolName + " status check in progress...", parent.getEnvironmentWindow().getEnvironmentName());
            parent.getEnvironmentWindow().getChangeEvents().add("JDBC connection pool " + connectionPoolName + " status check requested.");
            // start the status thread
            final StatusThread statusThread = new StatusThread();
            statusThread.start();
            // sync with the client
            KalumetConsoleApplication.getApplication().enqueueTask(KalumetConsoleApplication.getApplication().getTaskQueue(), new Runnable() {
                public void run() {
                    if (statusThread.ended) {
                        if (statusThread.failure) {
                            KalumetConsoleApplication.getApplication().getLogPane().addWarning(statusThread.message, parent.getEnvironmentWindow().getEnvironmentName());
                        } else {
                            KalumetConsoleApplication.getApplication().getLogPane().addInfo(statusThread.message, parent.getEnvironmentWindow().getEnvironmentName());
                        }
                        parent.getEnvironmentWindow().getChangeEvents().add(statusThread.message);
                    } else {
                        KalumetConsoleApplication.getApplication().enqueueTask(KalumetConsoleApplication.getApplication().getTaskQueue(), this);
                    }
                }
            });
        }
    };
    // update
    private ActionListener update = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the lock
            if (!getEnvironmentWindow().getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!getEnvironmentWindow().adminPermission
                    && !getEnvironmentWindow().jeeResourcesUpdatePermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // check if some change has not been saved
            if (getEnvironmentWindow().isUpdated()) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.notsaved"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // display confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new ConfirmWindow(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    // add a message into the log pane and the journal
                    KalumetConsoleApplication.getApplication().getLogPane().addInfo("JDBC connection pool " + connectionPoolName + " update in progress...", parent.getEnvironmentWindow().getEnvironmentName());
                    parent.getEnvironmentWindow().getChangeEvents().add("JDBC connection pool " + connectionPoolName + " update requested.");
                    // start the update thread
                    final UpdateThread updateThread = new UpdateThread();
                    updateThread.start();
                    // sync with the client
                    KalumetConsoleApplication.getApplication().enqueueTask(KalumetConsoleApplication.getApplication().getTaskQueue(), new Runnable() {
                        public void run() {
                            if (updateThread.ended) {
                                if (updateThread.failure) {
                                    KalumetConsoleApplication.getApplication().getLogPane().addError(updateThread.message, parent.getEnvironmentWindow().getEnvironmentName());
                                    parent.getEnvironmentWindow().getChangeEvents().add(updateThread.message);
                                } else {
                                    KalumetConsoleApplication.getApplication().getLogPane().addConfirm("JDBC connection pool " + connectionPoolName + " updated.", parent.getEnvironmentWindow().getEnvironmentName());
                                    parent.getEnvironmentWindow().getChangeEvents().add("JDBC connection pool " + connectionPoolName + " updated.");
                                }
                            } else {
                                KalumetConsoleApplication.getApplication().enqueueTask(KalumetConsoleApplication.getApplication().getTaskQueue(), this);
                            }
                        }
                    });
                }
            }));
        }
    };

    /**
     * Create a new <code>ConnectionPoolWindow</code>.
     *
     * @param parent                   the <code>ConnectionPoolsPane</code>.
     * @param applicationServerName the original J2EE application server name.
     * @param connectionPoolName   the original JDBC connection pool name.
     */
    public ConnectionPoolWindow(ConnectionPoolsPane parent, String applicationServerName, String connectionPoolName) {
        super();

        // update the parent tab pane
        this.parent = parent;

        // update the connection pool name
        this.connectionPoolName = connectionPoolName;
        this.applicationServerName = applicationServerName;

        // update the connection pool object from the parent environment
        this.connectionPool = parent.getEnvironmentWindow().getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(this.applicationServerName).getJDBCConnectionPool(connectionPoolName);
        if (this.connectionPool == null) {
            this.connectionPool = new JDBCConnectionPool();
        }

        if (this.connectionPoolName == null) {
            setTitle(Messages.getString("connectionpool"));
        } else {
            setTitle(Messages.getString("connectionpool") + " " + this.connectionPoolName);
        }
        setId("connectionpoolwindow_" + parent.getEnvironmentWindow().getEnvironmentName() + "_" + this.applicationServerName + "_" + this.connectionPoolName);
        setStyleName("default");
        setWidth(new Extent(600, Extent.PX));
        setHeight(new Extent(400, Extent.PX));
        setModal(false);
        setDefaultCloseOperation(WindowPane.DISPOSE_ON_CLOSE);

        // create a split pane for the control button
        SplitPane splitPane = new SplitPane(SplitPane.ORIENTATION_VERTICAL_BOTTOM_TOP, new Extent(32));
        add(splitPane);

        // add the control pane
        Row controlRow = new Row();
        controlRow.setStyleName("control");
        splitPane.add(controlRow);
        // add the refresh button
        Button refreshButton = new Button(Messages.getString("reload"), Styles.DATABASE_REFRESH);
        refreshButton.setStyleName("control");
        refreshButton.addActionListener(refresh);
        controlRow.add(refreshButton);
        // add the copy button
        Button copyButton = new Button(Messages.getString("copy"), Styles.PAGE_COPY);
        copyButton.setStyleName("control");
        copyButton.addActionListener(copy);
        controlRow.add(copyButton);
        if (getEnvironmentWindow().adminPermission
                || getEnvironmentWindow().jeeResourcesPermission) {
            // add the paste button
            Button pasteButton = new Button(Messages.getString("paste"), Styles.PAGE_PASTE);
            pasteButton.setStyleName("control");
            pasteButton.addActionListener(paste);
            controlRow.add(pasteButton);
            // add the apply button
            Button applyButton = new Button(Messages.getString("apply"), Styles.ACCEPT);
            applyButton.setStyleName("control");
            applyButton.addActionListener(apply);
            controlRow.add(applyButton);
        }
        // add the status button
        Button statusButton = new Button(Messages.getString("status"), Styles.INFORMATION);
        statusButton.setStyleName("control");
        statusButton.addActionListener(status);
        controlRow.add(statusButton);
        if (getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeResourcesUpdatePermission) {
            // add the update button
            Button updateButton = new Button(Messages.getString("update"), Styles.COG);
            updateButton.setStyleName("control");
            updateButton.addActionListener(update);
            controlRow.add(updateButton);
        }
        if (getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeResourcesPermission) {
            // add the delete button
            Button deleteButton = new Button(Messages.getString("delete"), Styles.DELETE);
            deleteButton.setStyleName("control");
            deleteButton.addActionListener(delete);
            controlRow.add(deleteButton);
        }
        // add the close button
        Button closeButton = new Button(Messages.getString("close"), Styles.CROSS);
        closeButton.setStyleName("control");
        closeButton.addActionListener(close);
        controlRow.add(closeButton);

        // add the main tab pane
        TabPane tabPane = new TabPane();
        tabPane.setStyleName("default");
        splitPane.add(tabPane);

        // add the general tab
        TabPaneLayoutData tabLayoutData = new TabPaneLayoutData();
        tabLayoutData.setTitle(Messages.getString("general"));
        generalPane = new ConnectionPoolGeneralPane(this);
        generalPane.setLayoutData(tabLayoutData);
        tabPane.add(generalPane);

        // add the driver tab
        tabLayoutData = new TabPaneLayoutData();
        tabLayoutData.setTitle(Messages.getString("driver"));
        driverPane = new ConnectionPoolDriverPane(this);
        driverPane.setLayoutData(tabLayoutData);
        tabPane.add(driverPane);

        // add the database tab
        tabLayoutData = new TabPaneLayoutData();
        tabLayoutData.setTitle(Messages.getString("database"));
        databasePane = new ConnectionPoolDatabasePane(this);
        databasePane.setLayoutData(tabLayoutData);
        tabPane.add(databasePane);

        // add the capacity tab
        tabLayoutData = new TabPaneLayoutData();
        tabLayoutData.setTitle(Messages.getString("capacity"));
        capacityPane = new ConnectionPoolCapacityPane(this);
        capacityPane.setLayoutData(tabLayoutData);
        tabPane.add(capacityPane);

        // update the window
        update();
    }

    /**
     * Update the pane.
     */
    public void update() {
        generalPane.update();
        driverPane.update();
        databasePane.update();
        capacityPane.update();
    }

    public JDBCConnectionPool getConnectionPool() {
        return this.connectionPool;
    }

    public String getConnectionPoolName() {
        return this.connectionPoolName;
    }

    public EnvironmentWindow getEnvironmentWindow() {
        return parent.getEnvironmentWindow();
    }

}