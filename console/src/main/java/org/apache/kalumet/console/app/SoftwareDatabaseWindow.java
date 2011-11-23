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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import nextapp.echo2.app.Button;
import nextapp.echo2.app.Column;
import nextapp.echo2.app.ContentPane;
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.Grid;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.PasswordField;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.SelectField;
import nextapp.echo2.app.SplitPane;
import nextapp.echo2.app.TextArea;
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.WindowPane;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.app.list.DefaultListModel;
import nextapp.echo2.extras.app.TabPane;
import nextapp.echo2.extras.app.layout.TabPaneLayoutData;

import org.apache.commons.lang.StringUtils;
import org.apache.kalumet.console.configuration.ConfigurationManager;
import org.apache.kalumet.model.Agent;
import org.apache.kalumet.model.Database;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.model.SqlScript;
import org.apache.kalumet.ws.client.SoftwareClient;

/**
 * Software database window.
 */
public class SoftwareDatabaseWindow extends WindowPane {

    private static String[] DRIVERS = new String[]{Messages.getString("jdbc.driver.oracle.thin"), Messages.getString("jdbc.driver.ibm.db2"),
            Messages.getString("jdbc.driver.mysql"), Messages.getString("jdbc.driver.postgresql")};

    private String name;
    private Database database;
    private SoftwareWindow parent;
    private TextField nameField;
    private SelectField activeField;
    private SelectField blockerField;
    private TextArea sqlCommandArea;
    private SelectField driverField;
    private TextField userField;
    private PasswordField passwordField;
    private PasswordField confirmPasswordField;
    private TextField urlField;
    private SelectField agentField;

    private Grid sqlScriptsGrid;

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
                Agent agent = kalumet.getAgent(parent.getParentPane().getEnvironmentWindow().getEnvironment().getAgent());
                if (agent == null) {
                    throw new IllegalArgumentException("agent not found.");
                }
                // call the WebService
                SoftwareClient client = new SoftwareClient(agent.getHostname(), agent.getPort());
                client.updateDatabase(parent.getParentPane().getEnvironmentWindow().getEnvironmentName(), parent.getName(), name, false);
            } catch (Exception e) {
                failure = true;
                message = "Software " + parent.getName() + " database " + name + " update failed: " + e.getMessage();
            } finally {
                ended = true;
            }
        }
    }

    // refresh
    private ActionListener refresh = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // looking for the database object
            SoftwareDatabaseWindow.this.database = parent.getSoftware().getDatabase(name);
            if (SoftwareDatabaseWindow.this.database == null) {
                SoftwareDatabaseWindow.this.database = new Database();
            }
            // update the window
            update();
        }
    };
    // close
    private ActionListener close = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            SoftwareDatabaseWindow.this.userClose();
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
                    && !getEnvironmentWindow().softwaresPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // display confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new ConfirmWindow(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    // delete the database
                    parent.getSoftware().getUpdatePlan().remove(database);
                    // add a change event
                    parent.getParentPane().getEnvironmentWindow().getChangeEvents().add("Delete software " + parent.getName() + " database " + database.getName());
                    // change the updated flag
                    parent.getParentPane().getEnvironmentWindow().setUpdated(true);
                    // update the journal log tab pane
                    parent.getParentPane().getEnvironmentWindow().updateJournalPane();
                    // update the parent pane
                    parent.update();
                    // close the window
                    SoftwareDatabaseWindow.this.userClose();
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
                    && !getEnvironmentWindow().softwaresPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // get the fields value
            String nameFieldValue = nameField.getText();
            int activeFieldIndex = activeField.getSelectedIndex();
            int blockerFieldIndex = blockerField.getSelectedIndex();
            String sqlCommandAreaValue = sqlCommandArea.getText();
            int driverFieldIndex = driverField.getSelectedIndex();
            String userFieldValue = userField.getText();
            String passwordFieldValue = passwordField.getText();
            String confirmPasswordFieldValue = confirmPasswordField.getText();
            String urlFieldValue = urlField.getText();
            String agentFieldValue = (String) agentField.getSelectedItem();
            // check fields
            if (nameFieldValue == null || nameFieldValue.trim().length() < 1) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("database.mandatory"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            if (!passwordFieldValue.equals(confirmPasswordFieldValue)) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("database.password"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // if the user change the database name, check if the new database name
            // doesn't already exist
            if (name == null || (name != null && !name.equals(nameFieldValue))) {
                if (parent.getSoftware().getDatabase(nameFieldValue) != null) {
                    KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("software.component.exists"), getEnvironmentWindow().getEnvironmentName());
                    return;
                }
            }
            // add a change event
            if (name != null) {
                parent.getParentPane().getEnvironmentWindow().getChangeEvents().add("Change software " + parent.getName() + " database " + database.getName());
            }
            // update the database object
            database.setName(nameFieldValue);
            if (activeFieldIndex == 0) {
                database.setActive(true);
            } else {
                database.setActive(false);
            }
            if (blockerFieldIndex == 0) {
                database.setBlocker(true);
            } else {
                database.setBlocker(false);
            }
            if (driverFieldIndex == 0) {
                database.setDriver("oracle.jdbc.driver.OracleDriver");
            }
            if (driverFieldIndex == 1) {
                database.setDriver("com.ibm.db2.jcc.DB2Driver");
            }
            if (driverFieldIndex == 2) {
                database.setDriver("com.mysql.jdbc.Driver");
            }
            if (driverFieldIndex == 3) {
                database.setDriver("org.postgresql.Driver");
            }
            database.setSqlCommand(sqlCommandAreaValue);
            database.setUser(userFieldValue);
            database.setPassword(passwordFieldValue);
            database.setJdbcurl(urlFieldValue);
            database.setAgent(agentFieldValue);
            // add the database object if needed
            if (name == null) {
                try {
                    parent.getSoftware().addDatabase(database);
                    parent.getParentPane().getEnvironmentWindow().getChangeEvents().add("Add software " + parent.getName() + " database " + database.getName());
                } catch (Exception e) {
                    KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("database.exists"), getEnvironmentWindow().getEnvironmentName());
                    return;
                }
            }
            // update the window definition
            setTitle(Messages.getString("database") + " " + database.getName());
            setId("softwaredatabasewindow_" + parent.getParentPane().getEnvironmentWindow().getEnvironmentName() + "_" + parent.getName() + "_" + database.getName());
            name = database.getName();
            // change the updated flag
            parent.getParentPane().getEnvironmentWindow().setUpdated(true);
            // update the journal log tab pane
            parent.getParentPane().getEnvironmentWindow().updateJournalPane();
            // update the parent window
            parent.update();
            // update the window
            update();
        }
    };
    // toggle active sql script
    public ActionListener toggleActiveSqlScript = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the environment lock
            if (!getEnvironmentWindow().getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!getEnvironmentWindow().adminPermission
                    && !getEnvironmentWindow().softwaresPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // looking for the sql script object
            SqlScript sqlScript = database.getSqlScript(event.getActionCommand());
            if (sqlScript == null) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("sqlscript.notfound"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // change the sql script object state
            if (sqlScript.isActive()) {
                sqlScript.setActive(false);
                parent.getParentPane().getEnvironmentWindow().getChangeEvents().add("Disable SQL script " + sqlScript.getName());
            } else {
                sqlScript.setActive(true);
                parent.getParentPane().getEnvironmentWindow().getChangeEvents().add("Enable SQL script " + sqlScript.getName());
            }
            // change the updated flag
            parent.getParentPane().getEnvironmentWindow().setUpdated(true);
            // update the journal log tab pane
            parent.getParentPane().getEnvironmentWindow().updateJournalPane();
            // update the window
            update();
        }
    };
    // toggle blocker sql script
    public ActionListener toggleBlockerSqlScript = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the environment lock
            if (!getEnvironmentWindow().getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!getEnvironmentWindow().adminPermission
                    && !getEnvironmentWindow().softwaresPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // looking for the sql script object
            SqlScript sqlScript = database.getSqlScript(event.getActionCommand());
            if (sqlScript == null) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("sqlscript.notfound"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // change the sql script blocker state
            if (sqlScript.isBlocker()) {
                sqlScript.setBlocker(false);
                parent.getParentPane().getEnvironmentWindow().getChangeEvents().add("Set not blocker for SQL script " + sqlScript.getName());
            } else {
                sqlScript.setBlocker(true);
                parent.getParentPane().getEnvironmentWindow().getChangeEvents().add("Set blocker for SQL script " + sqlScript.getName());
            }
            // change the updated flag
            parent.getParentPane().getEnvironmentWindow().setUpdated(true);
            // update the journal log tab pane
            parent.getParentPane().getEnvironmentWindow().updateJournalPane();
            // update the window
            update();
        }
    };
    // delete sql script
    private ActionListener deleteSqlScript = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the environment lock
            if (!getEnvironmentWindow().getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // looking for the sql script object
            final SqlScript sqlScript = database.getSqlScript(event.getActionCommand());
            if (sqlScript == null) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("sqlscript.notfound"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // display confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new ConfirmWindow(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    // delete the sql script object
                    database.getSqlScripts().remove(sqlScript);
                    // add a change event
                    parent.getParentPane().getEnvironmentWindow().getChangeEvents().add("Delete SQL script " + sqlScript.getName());
                    // change the updated flag
                    parent.getParentPane().getEnvironmentWindow().setUpdated(true);
                    // update the journal log tab pane
                    parent.getParentPane().getEnvironmentWindow().updateJournalPane();
                    // update the window
                    update();
                }
            }));
        }
    };
    // edit sql script
    private ActionListener editSqlScript = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            if (KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().getComponent(
                    "softwaresqlscriptwindow_" + parent.getParentPane().getEnvironmentWindow().getEnvironmentName() + "_" + parent.getName() + "_" + name + "_" + event.getActionCommand()) == null) {
                KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new SoftwareDatabaseSqlScriptWindow(SoftwareDatabaseWindow.this, event.getActionCommand()));
            }
        }
    };
    // create sql script
    private ActionListener createSqlScript = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new SoftwareDatabaseSqlScriptWindow(SoftwareDatabaseWindow.this, null));
        }
    };
    // copy
    private ActionListener copy = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            try {
                KalumetConsoleApplication.getApplication().setCopyComponent(database.clone());
            } catch (Exception e) {
                return;
            }
        }
    };
    // paste
    private ActionListener paste = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            Object copy = KalumetConsoleApplication.getApplication().getCopyComponent();
            // check if the copy is correct
            if (copy == null || !(copy instanceof Database)) {
                return;
            }
            database = (Database) copy;
            name = null;
            // update the parent pane
            parent.update();
            // update the window
            update();
        }
    };
    // copy sql script
    private ActionListener copySqlScript = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // looking for the sql script object
            SqlScript sqlScript = database.getSqlScript(event.getActionCommand());
            if (sqlScript == null) {
                return;
            }
            try {
                KalumetConsoleApplication.getApplication().setCopyComponent(sqlScript.clone());
            } catch (Exception e) {
                return;
            }
        }
    };
    // up
    private ActionListener up = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the environment lock
            if (!getEnvironmentWindow().getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!getEnvironmentWindow().adminPermission
                    && !getEnvironmentWindow().softwaresPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // looking for the SQL script object
            SqlScript sqlScript = database.getSqlScript(event.getActionCommand());
            if (sqlScript == null) {
                return;
            }
            // get the SQL script index
            int index = database.getSqlScripts().indexOf(sqlScript);
            // if the SQL script index is the first one or the object is not found,
            // do nothing, the size of the list must contains at leat 2 SQL scripts
            if (index == 0 || index == -1 || database.getSqlScripts().size() < 2) {
                return;
            }
            // get the previous sql script
            SqlScript previous = (SqlScript) database.getSqlScripts().get(index - 1);
            // switch the SQL scripts
            database.getSqlScripts().set(index, previous);
            database.getSqlScripts().set(index - 1, sqlScript);
            // update the pane
            update();
        }
    };
    // down
    private ActionListener down = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the environment lock
            if (!getEnvironmentWindow().getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!getEnvironmentWindow().adminPermission
                    && !getEnvironmentWindow().softwaresPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // looking for the SQL script object
            SqlScript sqlScript = database.getSqlScript(event.getActionCommand());
            if (sqlScript == null) {
                return;
            }
            // get the SQL script index
            int index = database.getSqlScripts().indexOf(sqlScript);
            // if the SQL script index is the last one or the object is not found,
            // the size of the list must contains at least 2 SQL scripts
            if (index == -1 || index == database.getSqlScripts().size() - 1 || database.getSqlScripts().size() < 2) {
                return;
            }
            // get the next SQL script
            SqlScript next = (SqlScript) database.getSqlScripts().get(index + 1);
            // switch the application
            database.getSqlScripts().set(index + 1, sqlScript);
            database.getSqlScripts().set(index, next);
            // update the pane
            update();
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
                    && !getEnvironmentWindow().softwaresUpdatePermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // check if some change has not yet been saved
            if (!getEnvironmentWindow().isUpdated()) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.notsaved"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // display a confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new ConfirmWindow(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    // add a message into the log pane and the journal
                    KalumetConsoleApplication.getApplication().getLogPane().addInfo("Software " + parent.getName() + " database " + name + " update in progress ...", parent.getParentPane().getEnvironmentWindow().getEnvironmentName());
                    parent.getParentPane().getEnvironmentWindow().getChangeEvents().add("Software " + parent.getName() + " database " + name + " update requested.");
                    // start the update thread
                    final UpdateThread updateThread = new UpdateThread();
                    updateThread.start();
                    // sync with the client
                    KalumetConsoleApplication.getApplication().enqueueTask(KalumetConsoleApplication.getApplication().getTaskQueue(), new Runnable() {
                        public void run() {
                            if (updateThread.ended) {
                                if (updateThread.failure) {
                                    KalumetConsoleApplication.getApplication().getLogPane().addError(updateThread.message, parent.getParentPane().getEnvironmentWindow().getEnvironmentName());
                                    parent.getParentPane().getEnvironmentWindow().getChangeEvents().add(updateThread.message);
                                } else {
                                    KalumetConsoleApplication.getApplication().getLogPane().addConfirm("Software " + parent.getName() + " database " + name + " updated.", parent.getParentPane().getEnvironmentWindow().getEnvironmentName());
                                    parent.getParentPane().getEnvironmentWindow().getChangeEvents().add("Software " + parent.getName() + " database " + name + " updated.");
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
     * Create a new software database window.
     *
     * @param parent the parent software window.
     * @param name   the linked database name.
     */
    public SoftwareDatabaseWindow(SoftwareWindow parent, String name) {
        super();

        // update the parent pane
        this.parent = parent;
        this.name = name;

        // update the database object from the parent pane
        this.database = parent.getSoftware().getDatabase(name);
        if (this.database == null) {
            this.database = new Database();
        }

        if (name == null) {
            setTitle(Messages.getString("database"));
        } else {
            setTitle(Messages.getString("database") + " " + name);
        }
        setId("softwaredatabasewindow_" + parent.getParentPane().getEnvironmentWindow().getEnvironmentName() + "_" + parent.getName() + "_" + name);
        setStyleName("default");
        setWidth(new Extent(400, Extent.PX));
        setHeight(new Extent(300, Extent.PX));
        setModal(false);
        setDefaultCloseOperation(WindowPane.DISPOSE_ON_CLOSE);

        // create a split pane for the control buttons
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
                || getEnvironmentWindow().softwaresPermission) {
            // add the paste button
            Button pasteButton = new Button(Messages.getString("paste"), Styles.PAGE_PASTE);
            pasteButton.setStyleName("control");
            pasteButton.addActionListener(paste);
            controlRow.add(pasteButton);
        }
        if (getEnvironmentWindow().adminPermission
                || getEnvironmentWindow().softwaresUpdatePermission) {
            // add the update button
            Button updateButton = new Button(Messages.getString("update"), Styles.COG);
            updateButton.setStyleName("control");
            updateButton.addActionListener(update);
            controlRow.add(updateButton);
        }
        if (getEnvironmentWindow().adminPermission
                || getEnvironmentWindow().softwaresPermission) {
            // add the apply button
            Button applyButton = new Button(Messages.getString("apply"), Styles.ACCEPT);
            applyButton.setStyleName("control");
            applyButton.addActionListener(apply);
            controlRow.add(applyButton);
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
        ContentPane generalTabPane = new ContentPane();
        generalTabPane.setStyleName("tab.content");
        generalTabPane.setLayoutData(tabLayoutData);
        tabPane.add(generalTabPane);
        Grid generalLayoutGrid = new Grid(2);
        generalLayoutGrid.setStyleName("default");
        generalLayoutGrid.setColumnWidth(0, new Extent(20, Extent.PERCENT));
        generalLayoutGrid.setColumnWidth(1, new Extent(80, Extent.PERCENT));
        generalTabPane.add(generalLayoutGrid);
        // name
        Label nameLabel = new Label(Messages.getString("name"));
        nameLabel.setStyleName("grid.cell");
        generalLayoutGrid.add(nameLabel);
        nameField = new TextField();
        nameField.setStyleName("default");
        nameField.setWidth(new Extent(100, Extent.PERCENT));
        generalLayoutGrid.add(nameField);
        // active
        Label activeLabel = new Label(Messages.getString("active"));
        activeLabel.setStyleName("grid.cell");
        generalLayoutGrid.add(activeLabel);
        activeField = new SelectField(MainScreen.LABELS);
        activeField.setSelectedIndex(0);
        activeField.setStyleName("default");
        activeField.setWidth(new Extent(10, Extent.EX));
        generalLayoutGrid.add(activeField);
        // blocker
        Label blockerLabel = new Label(Messages.getString("blocker"));
        blockerLabel.setStyleName("grid.cell");
        generalLayoutGrid.add(blockerLabel);
        blockerField = new SelectField(MainScreen.LABELS);
        blockerField.setStyleName("default");
        blockerField.setSelectedIndex(1);
        blockerField.setWidth(new Extent(10, Extent.EX));
        generalLayoutGrid.add(blockerField);
        // SQL command
        Label sqlCommandLabel = new Label(Messages.getString("sql.command"));
        sqlCommandLabel.setStyleName("grid.cell");
        generalLayoutGrid.add(sqlCommandLabel);
        sqlCommandArea = new TextArea();
        sqlCommandArea.setStyleName("default");
        sqlCommandArea.setWidth(new Extent(100, Extent.PERCENT));
        sqlCommandArea.setHeight(new Extent(5, Extent.EX));
        generalLayoutGrid.add(sqlCommandArea);
        // driver
        Label driverLabel = new Label(Messages.getString("driver"));
        driverLabel.setStyleName("grid.cell");
        generalLayoutGrid.add(driverLabel);
        driverField = new SelectField(SoftwareDatabaseWindow.DRIVERS);
        driverField.setSelectedIndex(0);
        driverField.setStyleName("default");
        driverField.setWidth(new Extent(50, Extent.EX));
        generalLayoutGrid.add(driverField);
        // user
        Label userLabel = new Label(Messages.getString("user"));
        userLabel.setStyleName("grid.cell");
        generalLayoutGrid.add(userLabel);
        userField = new TextField();
        userField.setStyleName("default");
        userField.setWidth(new Extent(100, Extent.PERCENT));
        generalLayoutGrid.add(userField);
        // password
        Label passwordLabel = new Label(Messages.getString("password"));
        passwordLabel.setStyleName("grid.cell");
        generalLayoutGrid.add(passwordLabel);
        passwordField = new PasswordField();
        passwordField.setStyleName("default");
        passwordField.setWidth(new Extent(100, Extent.PERCENT));
        generalLayoutGrid.add(passwordField);
        // confirm password
        Label confirmPasswordLabel = new Label(Messages.getString("password.confirm"));
        confirmPasswordLabel.setStyleName("grid.cell");
        generalLayoutGrid.add(confirmPasswordLabel);
        confirmPasswordField = new PasswordField();
        confirmPasswordField.setStyleName("default");
        confirmPasswordField.setWidth(new Extent(100, Extent.PERCENT));
        generalLayoutGrid.add(confirmPasswordField);
        // URL
        Label urlLabel = new Label(Messages.getString("url"));
        urlLabel.setStyleName("grid.cell");
        generalLayoutGrid.add(urlLabel);
        urlField = new TextField();
        urlField.setStyleName("default");
        urlField.setWidth(new Extent(100, Extent.PERCENT));
        generalLayoutGrid.add(urlField);
        // agent
        Label agentLabel = new Label(Messages.getString("agent"));
        agentLabel.setStyleName("grid.cell");
        generalLayoutGrid.add(agentLabel);
        agentField = new SelectField();
        agentField.setStyleName("default");
        agentField.setWidth(new Extent(50, Extent.EX));
        generalLayoutGrid.add(agentField);

        // add the sql scripts tab
        tabLayoutData = new TabPaneLayoutData();
        tabLayoutData.setTitle(Messages.getString("sql.scripts"));
        ContentPane sqlScriptsTabPane = new ContentPane();
        sqlScriptsTabPane.setStyleName("tab.content");
        sqlScriptsTabPane.setLayoutData(tabLayoutData);
        tabPane.add(sqlScriptsTabPane);
        Column column = new Column();
        sqlScriptsTabPane.add(column);
        Button createButton = new Button(Messages.getString("sql.script.add"), Styles.ADD);
        createButton.addActionListener(createSqlScript);
        column.add(createButton);
        sqlScriptsGrid = new Grid(3);
        sqlScriptsGrid.setStyleName("border.grid");
        sqlScriptsGrid.setColumnWidth(0, new Extent(50, Extent.PX));
        column.add(sqlScriptsGrid);

        // update the window
        update();
    }

    /**
     * Update the window.
     */
    public void update() {
        // update the database name field
        nameField.setText(database.getName());
        // update the database active field
        if (database.isActive()) {
            activeField.setSelectedIndex(0);
        } else {
            activeField.setSelectedIndex(1);
        }
        // update the database blocker field
        if (database.isBlocker()) {
            blockerField.setSelectedIndex(0);
        } else {
            blockerField.setSelectedIndex(1);
        }
        // update the database system launcher
        sqlCommandArea.setText(database.getSqlCommand());
        // update the database driver field
        if (StringUtils.containsIgnoreCase(database.getDriver(), "oracle")) {
            driverField.setSelectedIndex(0);
        }
        if (StringUtils.containsIgnoreCase(database.getDriver(), "db2")) {
            driverField.setSelectedIndex(1);
        }
        if (StringUtils.containsIgnoreCase(database.getDriver(), "mysql")) {
            driverField.setSelectedIndex(2);
        }
        if (StringUtils.containsIgnoreCase(database.getDriver(), "postgre")) {
            driverField.setSelectedIndex(3);
        }
        // update the database user field
        userField.setText(database.getUser());
        // update the database password field
        passwordField.setText(database.getPassword());
        confirmPasswordField.setText(database.getPassword());
        // update the database url field
        urlField.setText(database.getJdbcurl());
        // update agent field
        List agents = new LinkedList();
        // load Kalumet configuration
        try {
            Kalumet kalumet = ConfigurationManager.loadStore();
            agents = kalumet.getAgents();
        } catch (Exception e) {
            KalumetConsoleApplication.getApplication().getLogPane().addError(Messages.getString("db.read") + ": " + e.getMessage(), parent.getParentPane().getEnvironmentWindow().getEnvironmentName());
        }
        DefaultListModel agentListModel = (DefaultListModel) agentField.getModel();
        agentListModel.removeAll();
        for (Iterator agentIterator = agents.iterator(); agentIterator.hasNext(); ) {
            Agent agent = (Agent) agentIterator.next();
            agentListModel.add(agent.getId());
        }
        agentField.setSelectedItem(database.getAgent());

        // remove all sql scripts grid children
        sqlScriptsGrid.removeAll();
        // add sql scripts grid header
        Label sqlScriptActionHeader = new Label(" ");
        sqlScriptActionHeader.setStyleName("grid.header");
        sqlScriptsGrid.add(sqlScriptActionHeader);
        Label sqlScriptNameHeader = new Label(Messages.getString("name"));
        sqlScriptNameHeader.setStyleName("grid.header");
        sqlScriptsGrid.add(sqlScriptNameHeader);
        Label sqlScriptUriHeader = new Label(Messages.getString("uri"));
        sqlScriptUriHeader.setStyleName("grid.header");
        sqlScriptsGrid.add(sqlScriptUriHeader);
        // add sql script
        for (Iterator sqlScriptIterator = database.getSqlScripts().iterator(); sqlScriptIterator.hasNext(); ) {
            SqlScript sqlScript = (SqlScript) sqlScriptIterator.next();
            // row
            Row row = new Row();
            row.setCellSpacing(new Extent(2));
            row.setInsets(new Insets(2));
            sqlScriptsGrid.add(row);
            // sqlscript active
            Button activeButton;
            if (sqlScript.isActive()) {
                activeButton = new Button(Styles.LIGHTBULB);
                activeButton.setToolTipText(Messages.getString("switch.disable"));
            } else {
                activeButton = new Button(Styles.LIGHTBULB_OFF);
                activeButton.setToolTipText(Messages.getString("switch.enable"));
            }
            if (getEnvironmentWindow().adminPermission
                    || getEnvironmentWindow().softwaresPermission) {
                activeButton.setActionCommand(sqlScript.getName());
                activeButton.addActionListener(toggleActiveSqlScript);
            }
            row.add(activeButton);
            // sqlscript blocker
            Button blockerButton;
            if (sqlScript.isBlocker()) {
                blockerButton = new Button(Styles.PLUGIN);
                blockerButton.setToolTipText(Messages.getString("switch.notblocker"));
            } else {
                blockerButton = new Button(Styles.PLUGIN_DISABLED);
                blockerButton.setToolTipText(Messages.getString("switch.blocker"));
            }
            if (getEnvironmentWindow().adminPermission
                    || getEnvironmentWindow().softwaresPermission) {
                blockerButton.setActionCommand(sqlScript.getName());
                blockerButton.addActionListener(toggleBlockerSqlScript);
            }
            row.add(blockerButton);
            // copy
            Button copyButton = new Button(Styles.PAGE_COPY);
            copyButton.setToolTipText(Messages.getString("copy"));
            copyButton.setActionCommand(sqlScript.getName());
            copyButton.addActionListener(copySqlScript);
            row.add(copyButton);
            // up / down / delete
            if (getEnvironmentWindow().adminPermission
                    || getEnvironmentWindow().softwaresPermission) {
                // up button
                Button upButton = new Button(Styles.ARROW_UP);
                upButton.setToolTipText(Messages.getString("up"));
                upButton.setActionCommand(sqlScript.getName());
                upButton.addActionListener(up);
                row.add(upButton);
                // down button
                Button downButton = new Button(Styles.ARROW_DOWN);
                downButton.setToolTipText(Messages.getString("down"));
                downButton.setActionCommand(sqlScript.getName());
                downButton.addActionListener(down);
                row.add(downButton);
                // delete button
                Button deleteButton = new Button(Styles.DELETE);
                deleteButton.setToolTipText(Messages.getString("delete"));
                deleteButton.setActionCommand(sqlScript.getName());
                deleteButton.addActionListener(deleteSqlScript);
                row.add(deleteButton);
            }
            // sqlscript name
            Button sqlScriptName = new Button(sqlScript.getName());
            sqlScriptName.setStyleName("default");
            sqlScriptName.setActionCommand(sqlScript.getName());
            sqlScriptName.addActionListener(editSqlScript);
            sqlScriptsGrid.add(sqlScriptName);
            // sqlscript uri
            Label sqlScriptUri = new Label(sqlScript.getUri());
            sqlScriptUri.setStyleName("default");
            sqlScriptsGrid.add(sqlScriptUri);
        }
    }

    public SoftwareWindow getParentPane() {
        return this.parent;
    }

    public Database getDatabase() {
        return this.database;
    }

    public String getName() {
        return this.name;
    }

    public EnvironmentWindow getEnvironmentWindow() {
        return parent.getParentPane().getEnvironmentWindow();
    }

}
