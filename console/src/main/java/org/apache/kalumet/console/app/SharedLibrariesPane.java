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
import org.apache.kalumet.console.configuration.ConfigurationManager;
import org.apache.kalumet.model.Agent;
import org.apache.kalumet.model.J2EEApplicationServer;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.model.SharedLibrary;
import org.apache.kalumet.ws.client.SharedLibraryClient;

/**
 * Environment shared libraries pane.
 */
public class SharedLibrariesPane extends ContentPane {

    private EnvironmentWindow parent;
    private SelectField scopeField;
    private Grid grid;
    private boolean newIsActive = true;
    private boolean newIsBlocker = false;
    private TextField newNameField;
    private TextField newClasspathField;

    // status thread
    class StatusThread extends Thread {

        public String serverName;
        public String sharedLibraryName;
        public boolean ended = false;
        public boolean failure = false;
        public String message;

        public void run() {
            try {
                // load Kalumet configuration
                Kalumet kalumet = ConfigurationManager.loadStore();
                // looking for the agent
                Agent agent = kalumet.getAgent(parent.getEnvironment().getAgent());
                if (agent == null) {
                    throw new IllegalArgumentException("agent not found.");
                }
                // call the webservice stub
                SharedLibraryClient client = new SharedLibraryClient(agent.getHostname(), agent.getPort());
                boolean uptodate = client.check(parent.getEnvironmentName(), serverName, sharedLibraryName);
                if (uptodate) {
                    message = "Shared library " + sharedLibraryName + " is up to date.";
                } else {
                    failure = true;
                    message = "Shared library " + sharedLibraryName + " is not up to date";
                }
            } catch (Exception e) {
                failure = true;
                message = "Shared library " + sharedLibraryName + " status check failed: " + e.getMessage();
            } finally {
                ended = true;
            }
        }
    }

    // update thread
    class UpdateThread extends Thread {

        public String serverName;
        public String sharedLibraryName;
        public boolean ended = false;
        public boolean failure = false;
        public String message;

        public void run() {
            try {
                // load Kalumet configuration
                Kalumet kalumet = ConfigurationManager.loadStore();
                // looking for the agent
                Agent agent = kalumet.getAgent(parent.getEnvironment().getAgent());
                if (agent == null) {
                    throw new IllegalArgumentException("agent not found.");
                }
                // call the webservice stub
                SharedLibraryClient client = new SharedLibraryClient(agent.getHostname(), agent.getPort());
                client.update(parent.getEnvironmentName(), serverName, sharedLibraryName);
            } catch (Exception e) {
                failure = true;
                message = "Shared library " + sharedLibraryName + " update failed: " + e.getMessage();
            } finally {
                ended = true;
            }
        }
    }

    // scope select
    private ActionListener scopeSelect = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            update();
        }
    };
    // toggle active
    private ActionListener toggleActive = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the environment lock
            if (!getEnvironmentWindow().getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeResourcesPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // looking for the shared library object
            SharedLibrary sharedLibrary = parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer((String) scopeField.getSelectedItem()).getSharedLibrary(event.getActionCommand());
            if (sharedLibrary == null) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("sharedlibrary.notfound"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // update the state
            if (sharedLibrary.isActive()) {
                sharedLibrary.setActive(false);
                // add a change event
                parent.getChangeEvents().add("Disable shared library " + sharedLibrary.getName());
            } else {
                sharedLibrary.setActive(true);
                parent.getChangeEvents().add("Enable shared library " + sharedLibrary.getName());
            }
            // change the updated flag
            parent.setUpdated(true);
            // update the journal log tab pane
            parent.updateJournalPane();
            // update the pane
            update();
        }
    };
    // toggle blocker
    private ActionListener toggleBlocker = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the environment lock
            if (!getEnvironmentWindow().getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeResourcesPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // looking for the shared library object
            SharedLibrary sharedLibrary = parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer((String) scopeField.getSelectedItem()).getSharedLibrary(event.getActionCommand());
            if (sharedLibrary == null) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("sharedlibrary.notfound"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // update the blocker state and add a change event
            if (sharedLibrary.isBlocker()) {
                sharedLibrary.setBlocker(false);
                parent.getChangeEvents().add("Set not blocker for shared library " + sharedLibrary.getName());
            } else {
                sharedLibrary.setBlocker(true);
                parent.getChangeEvents().add("Set blocker for shared library " + sharedLibrary.getName());
            }
            // change the updated flag
            parent.setUpdated(true);
            // update the journal log tab pane
            parent.updateJournalPane();
            // update the pane
            update();
        }
    };
    // new toggle active
    private ActionListener newToggleActive = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // toggle the state
            if (newIsActive) {
                newIsActive = false;
            } else {
                newIsActive = true;
            }
            // update the pane
            update();
        }
    };
    // new toggle blocker
    private ActionListener newToggleBlocker = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // toggle the blocker state
            if (newIsBlocker) {
                newIsBlocker = false;
            } else {
                newIsBlocker = true;
            }
            // update the pane
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
            if (!getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeResourcesPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // looking for the shared library
            final SharedLibrary sharedLibrary = parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer((String) scopeField.getSelectedItem()).getSharedLibrary(event.getActionCommand());
            if (sharedLibrary == null) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("sharedlibrary.notfound"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // display confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new ConfirmWindow(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    // remove the shared library
                    parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer((String) scopeField.getSelectedItem()).getSharedLibraries().remove(sharedLibrary);
                    // add a change event
                    parent.getChangeEvents().add("Delete shared library " + sharedLibrary.getName());
                    // change the updated flag
                    parent.setUpdated(true);
                    // update the journal log tab pane
                    parent.updateJournalPane();
                    // update only the pane
                    update();
                }
            }));
        }
    };
    // edit
    private ActionListener edit = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the environment lock
            if (!getEnvironmentWindow().getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeResourcesPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // get the shared library name
            String name = event.getActionCommand();
            // get the fields
            TextField nameField = (TextField) SharedLibrariesPane.this.getComponent("slname_" + parent.getEnvironmentName() + "_" + (String) scopeField.getSelectedItem() + "_" + name);
            TextField classpathField = (TextField) SharedLibrariesPane.this.getComponent("slclasspath_" + parent.getEnvironmentName() + "_" + (String) scopeField.getSelectedItem() + "_" + name);
            // get values
            String nameFieldValue = nameField.getText();
            String classpathFieldValue = classpathField.getText();
            // check values
            if (nameFieldValue == null || nameFieldValue.trim().length() < 1 || classpathFieldValue == null || classpathFieldValue.trim().length() < 1) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("sharedlibrary.mandatory"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // if the user change the shared library name, check if the shared
            // library name is not already in use
            if (!name.equals(nameFieldValue)) {
                if (parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer((String) scopeField.getSelectedItem()).getSharedLibrary(nameFieldValue) != null) {
                    KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("sharedlibrary.exists"), getEnvironmentWindow().getEnvironmentName());
                    return;
                }
            }
            // looking for the shared library object
            SharedLibrary sharedLibrary = parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer((String) scopeField.getSelectedItem()).getSharedLibrary(name);
            if (sharedLibrary == null) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("sharedlibrary.notfound"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // add a change event
            parent.getChangeEvents().add("Change shared library " + sharedLibrary.getName() + " / " + sharedLibrary.getClasspath());
            // change the shared library object
            sharedLibrary.setName(nameFieldValue);
            sharedLibrary.setClasspath(classpathFieldValue);
            // change the updated flag
            parent.setUpdated(true);
            // update the journal log tab pane
            parent.updateJournalPane();
            // update the pane
            update();
        }
    };
    // create
    private ActionListener create = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the environment lock
            if (!getEnvironmentWindow().getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeResourcesPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // get values
            String newNameFieldValue = newNameField.getText();
            String newClasspathFieldValue = newClasspathField.getText();
            // mandatory field
            if (newNameFieldValue == null || newNameFieldValue.trim().length() < 1 || newClasspathFieldValue == null || newClasspathFieldValue.trim().length() < 1) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("sharedlibrary.mandatory"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // create the new shared library object
            SharedLibrary sharedLibrary = new SharedLibrary();
            sharedLibrary.setName(newNameFieldValue);
            sharedLibrary.setClasspath(newClasspathFieldValue);
            sharedLibrary.setActive(newIsActive);
            // add the shared library
            try {
                parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer((String) scopeField.getSelectedItem()).addSharedLibrary(sharedLibrary);
            } catch (Exception e) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("sharedlibrary.exists"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // add a change event
            parent.getChangeEvents().add("Create shared library " + sharedLibrary.getName() + " / " + sharedLibrary.getClasspath());
            // change the updated flag
            parent.setUpdated(true);
            // update the journal log tab pane
            parent.updateJournalPane();
            // update the pane
            update();
        }
    };
    // copy
    private ActionListener copy = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // looking for the shared library object
            SharedLibrary sharedLibrary = parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer((String) scopeField.getSelectedItem()).getSharedLibrary(event.getActionCommand());
            if (sharedLibrary == null) {
                return;
            }
            try {
                KalumetConsoleApplication.getApplication().setCopyComponent(sharedLibrary);
            } catch (Exception e) {
                return;
            }
        }
    };
    // paste
    private ActionListener paste = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            Object copy = KalumetConsoleApplication.getApplication().getCopyComponent();
            // check if the copy object is correct
            if (copy == null || !(copy instanceof SharedLibrary)) {
                return;
            }
            // update the new fields
            newNameField.setText(((SharedLibrary) copy).getName());
            newClasspathField.setText(((SharedLibrary) copy).getClasspath());
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
            // get the server and shared library name
            String serverName = (String) scopeField.getSelectedItem();
            String sharedLibraryName = event.getActionCommand();
            // add a message in the log pane and the journal
            KalumetConsoleApplication.getApplication().getLogPane().addInfo("Shared library " + sharedLibraryName + " status check in progress...", parent.getEnvironmentName());
            parent.getChangeEvents().add("Shared library " + sharedLibraryName + " status check requested.");
            // start the status thread
            final StatusThread statusThread = new StatusThread();
            statusThread.serverName = serverName;
            statusThread.sharedLibraryName = sharedLibraryName;
            statusThread.start();
            // sync with the client
            KalumetConsoleApplication.getApplication().enqueueTask(KalumetConsoleApplication.getApplication().getTaskQueue(), new Runnable() {
                public void run() {
                    if (statusThread.ended) {
                        if (statusThread.failure) {
                            KalumetConsoleApplication.getApplication().getLogPane().addWarning(statusThread.message, parent.getEnvironmentName());
                        } else {
                            KalumetConsoleApplication.getApplication().getLogPane().addInfo(statusThread.message, parent.getEnvironmentName());
                        }
                        parent.getChangeEvents().add(statusThread.message);
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
            if (!getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeResourcesUpdatePermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // check if some change has not been saved
            if (getEnvironmentWindow().isUpdated()) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.notsaved"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // get the JEE server and shared library name
            final String serverName = (String) scopeField.getSelectedItem();
            final String sharedLibraryName = event.getActionCommand();
            // display confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new ConfirmWindow(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    // add a message in the log pane and in the journal
                    KalumetConsoleApplication.getApplication().getLogPane().addInfo("Shared library " + sharedLibraryName + " update in progress ...", parent.getEnvironmentName());
                    parent.getChangeEvents().add("Shared library " + sharedLibraryName + " update requested.");
                    // start the update thread
                    final UpdateThread updateThread = new UpdateThread();
                    updateThread.serverName = serverName;
                    updateThread.sharedLibraryName = sharedLibraryName;
                    updateThread.start();
                    // sync with the client
                    KalumetConsoleApplication.getApplication().enqueueTask(KalumetConsoleApplication.getApplication().getTaskQueue(), new Runnable() {
                        public void run() {
                            if (updateThread.ended) {
                                if (updateThread.failure) {
                                    KalumetConsoleApplication.getApplication().getLogPane().addError(updateThread.message, parent.getEnvironmentName());
                                    parent.getChangeEvents().add(updateThread.message);
                                } else {
                                    KalumetConsoleApplication.getApplication().getLogPane().addConfirm("Shared library " + sharedLibraryName + " updated.", parent.getEnvironmentName());
                                    parent.getChangeEvents().add("Shared library " + sharedLibraryName + " updated.");
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
     * Create a new <code>SharedLibrariesPane</code>.
     *
     * @param parent the parent <code>EnvironmentWindow</code>.
     */
    public SharedLibrariesPane(EnvironmentWindow parent) {
        super();
        setStyleName("tab.content");

        // update parent
        this.parent = parent;

        // column layout
        Column content = new Column();
        content.setCellSpacing(new Extent(2));
        add(content);

        // add the scope select field
        Grid layoutGrid = new Grid(2);
        layoutGrid.setStyleName("default");
        layoutGrid.setColumnWidth(0, new Extent(20, Extent.PERCENT));
        layoutGrid.setColumnWidth(1, new Extent(80, Extent.PERCENT));
        content.add(layoutGrid);
        Label scopeLabel = new Label(Messages.getString("scope"));
        scopeLabel.setStyleName("default");
        layoutGrid.add(scopeLabel);
        scopeField = new SelectField();
        scopeField.addActionListener(scopeSelect);
        scopeField.setStyleName("default");
        layoutGrid.add(scopeField);
        DefaultListModel scopeListModel = (DefaultListModel) scopeField.getModel();
        scopeListModel.removeAll();
        // add application servers in the scope select field
        for (Iterator applicationServerIterator = parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServers().iterator(); applicationServerIterator.hasNext(); ) {
            J2EEApplicationServer applicationServer = (J2EEApplicationServer) applicationServerIterator.next();
            scopeListModel.add(applicationServer.getName());
        }
        if (scopeListModel.size() > 0) {
            scopeField.setSelectedIndex(0);
        }

        // add shared libraries grid
        grid = new Grid(3);
        grid.setStyleName("border.grid");
        grid.setColumnWidth(0, new Extent(50, Extent.PX));
        grid.setColumnWidth(1, new Extent(50, Extent.PERCENT));
        grid.setColumnWidth(2, new Extent(50, Extent.PERCENT));
        content.add(grid);

        // update the pane
        update();
    }

    /**
     * Update the pane.
     */
    public void update() {
        String applicationServerName = null;
        // update the scope select field
        DefaultListModel scopeListModel = (DefaultListModel) scopeField.getModel();
        if (scopeListModel.size() > 0) {
            applicationServerName = (String) scopeField.getSelectedItem();
        }
        scopeListModel.removeAll();
        int scopeIndex = 0;
        int found = -1;
        for (Iterator applicationServerIterator = parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServers().iterator(); applicationServerIterator.hasNext(); ) {
            J2EEApplicationServer applicationServer = (J2EEApplicationServer) applicationServerIterator.next();
            scopeListModel.add(applicationServer.getName());
            if (applicationServer.getName().equals(applicationServerName)) {
                found = scopeIndex;
            }
            scopeIndex++;
        }

        // remove all shared libraries grid children
        grid.removeAll();

        // check if at least one application server is present
        if (scopeListModel.size() < 1) {
            return;
        }
        // update the scope select field selected index
        if (found == -1) {
            scopeField.setSelectedIndex(0);
        } else {
            scopeField.setSelectedIndex(found);
        }
        // update the application server name from the scope (in case of
        // application server deletion)
        applicationServerName = (String) scopeField.getSelectedItem();

        // add shared libraries grid header
        Label actionHeader = new Label(" ");
        actionHeader.setStyleName("grid.header");
        grid.add(actionHeader);
        Label nameHeader = new Label(Messages.getString("name"));
        nameHeader.setStyleName("grid.header");
        grid.add(nameHeader);
        Label classpathHeader = new Label(Messages.getString("classpath"));
        classpathHeader.setStyleName("grid.header");
        grid.add(classpathHeader);
        // add the shared libraries
        for (Iterator sharedLibraryIterator = parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(applicationServerName).getSharedLibraries().iterator(); sharedLibraryIterator.hasNext(); ) {
            SharedLibrary sharedLibrary = (SharedLibrary) sharedLibraryIterator.next();
            // row
            Row row = new Row();
            row.setInsets(new Insets(2));
            row.setCellSpacing(new Extent(2));
            grid.add(row);
            // copy
            Button copyButton = new Button(Styles.PAGE_COPY);
            copyButton.setToolTipText(Messages.getString("copy"));
            copyButton.setActionCommand(sharedLibrary.getName());
            copyButton.addActionListener(copy);
            row.add(copyButton);
            // active
            Button activeButton;
            if (sharedLibrary.isActive()) {
                activeButton = new Button(Styles.LIGHTBULB);
                activeButton.setToolTipText(Messages.getString("switch.disable"));
            } else {
                activeButton = new Button(Styles.LIGHTBULB_OFF);
                activeButton.setToolTipText(Messages.getString("switch.enable"));
            }
            if (getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeResourcesPermission) {
                activeButton.setActionCommand(sharedLibrary.getName());
                activeButton.addActionListener(toggleActive);
            }
            row.add(activeButton);
            // blocker
            Button blockerButton;
            if (sharedLibrary.isBlocker()) {
                blockerButton = new Button(Styles.PLUGIN);
                blockerButton.setToolTipText(Messages.getString("switch.notblocker"));
            } else {
                blockerButton = new Button(Styles.PLUGIN_DISABLED);
                blockerButton.setToolTipText(Messages.getString("switch.blocker"));
            }
            if (getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeResourcesPermission) {
                blockerButton.setActionCommand(sharedLibrary.getName());
                blockerButton.addActionListener(toggleBlocker);
            }
            row.add(blockerButton);
            // status
            Button statusButton = new Button(Styles.INFORMATION);
            statusButton.setToolTipText(Messages.getString("status"));
            statusButton.setActionCommand(sharedLibrary.getName());
            statusButton.addActionListener(status);
            row.add(statusButton);
            if (getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeResourcesUpdatePermission) {
                // update
                Button updateButton = new Button(Styles.COG);
                updateButton.setToolTipText(Messages.getString("update"));
                updateButton.setActionCommand(sharedLibrary.getName());
                updateButton.addActionListener(update);
                row.add(updateButton);
            }
            if (getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeResourcesPermission) {
                // edit
                Button editButton = new Button(Styles.ACCEPT);
                editButton.setToolTipText(Messages.getString("apply"));
                editButton.setActionCommand(sharedLibrary.getName());
                editButton.addActionListener(edit);
                row.add(editButton);
                // delete
                Button deleteButton = new Button(Styles.DELETE);
                deleteButton.setToolTipText(Messages.getString("delete"));
                deleteButton.setActionCommand(sharedLibrary.getName());
                deleteButton.addActionListener(delete);
                row.add(deleteButton);
            }
            // name
            TextField nameField = new TextField();
            nameField.setStyleName("default");
            nameField.setWidth(new Extent(100, Extent.PERCENT));
            nameField.setId("slname_" + parent.getEnvironmentName() + "_" + applicationServerName + "_" + sharedLibrary.getName());
            nameField.setText(sharedLibrary.getName());
            grid.add(nameField);
            // classpath
            TextField classpathField = new TextField();
            classpathField.setStyleName("default");
            classpathField.setWidth(new Extent(100, Extent.PERCENT));
            classpathField.setId("slclasspath_" + parent.getEnvironmentName() + "_" + applicationServerName + "_" + sharedLibrary.getName());
            classpathField.setText(sharedLibrary.getClasspath());
            grid.add(classpathField);
        }

        // add create shared library row in the shared libraries grid
        if (getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeResourcesPermission) {
            // row
            Row row = new Row();
            row.setInsets(new Insets(2));
            row.setCellSpacing(new Extent(2));
            grid.add(row);
            // active
            Button activeButton;
            if (newIsActive) {
                activeButton = new Button(Styles.LIGHTBULB);
                activeButton.setToolTipText(Messages.getString("switch.disable"));
            } else {
                activeButton = new Button(Styles.LIGHTBULB_OFF);
                activeButton.setToolTipText(Messages.getString("switch.enable"));
            }
            activeButton.addActionListener(newToggleActive);
            row.add(activeButton);
            // blocker
            Button blockerButton;
            if (newIsBlocker) {
                blockerButton = new Button(Styles.PLUGIN);
                blockerButton.setToolTipText(Messages.getString("switch.notblocker"));
            } else {
                blockerButton = new Button(Styles.PLUGIN_DISABLED);
                blockerButton.setToolTipText(Messages.getString("switch.blocker"));
            }
            blockerButton.addActionListener(newToggleBlocker);
            row.add(blockerButton);
            // paste
            Button pasteButton = new Button(Styles.PAGE_PASTE);
            pasteButton.setToolTipText(Messages.getString("paste"));
            pasteButton.addActionListener(paste);
            row.add(pasteButton);
            // add
            Button addButton = new Button(Styles.ADD);
            addButton.setToolTipText(Messages.getString("add"));
            addButton.addActionListener(create);
            row.add(addButton);
            // name
            newNameField = new TextField();
            newNameField.setStyleName("default");
            newNameField.setWidth(new Extent(100, Extent.PERCENT));
            grid.add(newNameField);
            // classpath
            newClasspathField = new TextField();
            newClasspathField.setStyleName("default");
            newClasspathField.setWidth(new Extent(100, Extent.PERCENT));
            grid.add(newClasspathField);
        }
    }

    public EnvironmentWindow getEnvironmentWindow() {
        return parent;
    }

}
