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
import org.apache.kalumet.model.JNDIBinding;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.ws.client.JNDIBindingClient;

/**
 * Environment JNDI name space bindings pane.
 */
public class NameSpaceBindingsPane extends ContentPane {

    private EnvironmentWindow parent;
    private SelectField scopeField;
    private Grid grid;
    private boolean newIsActive = true;
    private boolean newIsBlocker = false;
    private TextField newNameField;
    private TextField newJndiNameField;
    private TextField newJndiAliasField;
    private TextField newProviderUrlField;

    // status thread
    class StatusThread extends Thread {

        public String serverName;
        public String nameSpaceBindingName;
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
                // call the webservice
                JNDIBindingClient client = new JNDIBindingClient(agent.getHostname(), agent.getPort());
                boolean uptodate = client.check(parent.getEnvironmentName(), serverName, nameSpaceBindingName);
                if (uptodate) {
                    message = "JNDI binding " + nameSpaceBindingName + " is up to date.";
                } else {
                    failure = true;
                    message = "JNDI binding " + nameSpaceBindingName + " is not up to date.";
                }
            } catch (Exception e) {
                failure = true;
                message = "JNDI binding " + nameSpaceBindingName + " status check failed: " + e.getMessage();
            } finally {
                ended = true;
            }
        }
    }

    // update thread
    class UpdateThread extends Thread {

        public String serverName;
        public String nameSpaceBindingName;
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
                // call the webservice
                JNDIBindingClient client = new JNDIBindingClient(agent.getHostname(), agent.getPort());
                client.update(parent.getEnvironmentName(), serverName, nameSpaceBindingName);
            } catch (Exception e) {
                failure = true;
                message = "JNDI binding " + nameSpaceBindingName + " update failed: " + e.getMessage();
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
            // looking for the name space binding object
            JNDIBinding nameSpaceBinding = parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer((String) scopeField.getSelectedItem()).getJNDIBinding(event.getActionCommand());
            if (nameSpaceBinding == null) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("namespacebinding.notfound"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // update the state
            if (nameSpaceBinding.isActive()) {
                nameSpaceBinding.setActive(false);
                // add a change event
                parent.getChangeEvents().add("Disable JNDI binding " + nameSpaceBinding.getName());
            } else {
                nameSpaceBinding.setActive(true);
                parent.getChangeEvents().add("Enable JNDI binding " + nameSpaceBinding.getName());
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
            // looking for the name space binding object
            JNDIBinding nameSpaceBinding = parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer((String) scopeField.getSelectedItem()).getJNDIBinding(event.getActionCommand());
            if (nameSpaceBinding == null) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("namespacebinding.notfound"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // update the blocker state and add a change event
            if (nameSpaceBinding.isBlocker()) {
                nameSpaceBinding.setBlocker(false);
                parent.getChangeEvents().add("Set not blocker for JNDI binding " + nameSpaceBinding.getName());
            } else {
                nameSpaceBinding.setBlocker(true);
                parent.getChangeEvents().add("Set blocker JNDI binding " + nameSpaceBinding.getName());
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
            // get the name space binding name
            String name = event.getActionCommand();
            // get the fields
            TextField nameField = (TextField) NameSpaceBindingsPane.this.getComponent("nsbname_" + parent.getEnvironmentName() + "_" + (String) scopeField.getSelectedItem() + "_" + name);
            TextField jndiNameField = (TextField) NameSpaceBindingsPane.this.getComponent("nsbjndiname_" + parent.getEnvironmentName() + "_" + (String) scopeField.getSelectedItem() + "_" + name);
            TextField jndiAliasField = (TextField) NameSpaceBindingsPane.this.getComponent("nsbjndialias_" + parent.getEnvironmentName() + "_" + (String) scopeField.getSelectedItem() + "_" + name);
            TextField providerUrlField = (TextField) NameSpaceBindingsPane.this.getComponent("nsbproviderurl_" + parent.getEnvironmentName() + "_" + (String) scopeField.getSelectedItem() + "_" + name);
            // get values
            String nameFieldValue = nameField.getText();
            String jndiNameFieldValue = jndiNameField.getText();
            String jndiAliasFieldValue = jndiAliasField.getText();
            String providerUrlFieldValue = providerUrlField.getText();
            // check value
            if (nameFieldValue == null || nameFieldValue.trim().length() < 1 || jndiNameFieldValue == null || jndiNameFieldValue.trim().length() < 1 || jndiAliasFieldValue == null
                    || jndiAliasFieldValue.trim().length() < 1) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("namespacebinding.mandatory"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // if the user change the name space binding name, check if the name
            // space binding name is not already in use
            if (!name.equals(nameFieldValue)) {
                if (parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer((String) scopeField.getSelectedItem()).getJNDIBinding(nameFieldValue) != null) {
                    KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("namespacebinding.exists"), getEnvironmentWindow().getEnvironmentName());
                    return;
                }
            }
            // looking for the name space binding object
            JNDIBinding nameSpaceBinding = parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer((String) scopeField.getSelectedItem()).getJNDIBinding(name);
            if (nameSpaceBinding == null) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("namespacebinding.notfound"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // add a change event
            parent.getChangeEvents().add("Change JNDI binding " + nameSpaceBinding.getName());
            // change the name space binding object
            nameSpaceBinding.setName(nameFieldValue);
            nameSpaceBinding.setJndiname(jndiNameFieldValue);
            nameSpaceBinding.setJndialias(jndiAliasFieldValue);
            nameSpaceBinding.setProviderurl(providerUrlFieldValue);
            // change the updated flag
            parent.setUpdated(true);
            // update the journal log tab pane
            parent.updateJournalPane();
            // update only the pane
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
            // looking for the name space binding
            final JNDIBinding nameSpaceBinding = parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer((String) scopeField.getSelectedItem()).getJNDIBinding(event.getActionCommand());
            if (nameSpaceBinding == null) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("namespacebinding.notfound"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // display confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new ConfirmWindow(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    // remove the name space binding
                    parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer((String) scopeField.getSelectedItem()).getJNDIBindings().remove(nameSpaceBinding);
                    // add a change event
                    parent.getChangeEvents().add("Delete JNDI binding " + nameSpaceBinding.getName());
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
            String nameFieldValue = newNameField.getText();
            String jndiNameFieldValue = newJndiNameField.getText();
            String jndiAliasFieldValue = newJndiAliasField.getText();
            String providerUrlFieldValue = newProviderUrlField.getText();
            // mandatory field
            if (nameFieldValue == null || nameFieldValue.trim().length() < 1 || jndiNameFieldValue == null || jndiNameFieldValue.trim().length() < 1
                    || jndiAliasFieldValue == null || jndiAliasFieldValue.trim().length() < 1) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("namespacebinding.mandatory"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // create the new name space binding object
            JNDIBinding nameSpaceBinding = new JNDIBinding();
            nameSpaceBinding.setName(nameFieldValue);
            nameSpaceBinding.setJndiname(jndiNameFieldValue);
            nameSpaceBinding.setJndialias(jndiAliasFieldValue);
            nameSpaceBinding.setProviderurl(providerUrlFieldValue);
            nameSpaceBinding.setActive(newIsActive);
            nameSpaceBinding.setBlocker(newIsBlocker);
            // add the name space binding
            try {
                parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer((String) scopeField.getSelectedItem()).addJNDIBinding(nameSpaceBinding);
            } catch (Exception e) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("namespacebinding.exists"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // add a change events
            parent.getChangeEvents().add("Add JNDI binding " + nameSpaceBinding.getName());
            // change the updated flag
            parent.setUpdated(true);
            // update the journal log tab pane
            parent.updateJournalPane();
            // update only the pane
            update();
        }
    };
    // copy
    private ActionListener copy = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // looking for name space binding object
            JNDIBinding jndiBinding = parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer((String) scopeField.getSelectedItem()).getJNDIBinding(event.getActionCommand());
            if (jndiBinding == null) {
                return;
            }
            try {
                KalumetConsoleApplication.getApplication().setCopyComponent(jndiBinding);
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
            if (copy == null || !(copy instanceof JNDIBinding)) {
                return;
            }
            // update the new fields
            newNameField.setText(((JNDIBinding) copy).getName());
            newJndiNameField.setText(((JNDIBinding) copy).getJndiname());
            newJndiAliasField.setText(((JNDIBinding) copy).getJndialias());
            newProviderUrlField.setText(((JNDIBinding) copy).getProviderurl());
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
            // get the J2EE application server and JNDI name space binding name
            final String serverName = (String) scopeField.getSelectedItem();
            final String nameSpaceBindingName = event.getActionCommand();
            // add a message in the log pane and the journal
            KalumetConsoleApplication.getApplication().getLogPane().addInfo("JNDI binding " + nameSpaceBindingName + " status check in progress...", parent.getEnvironmentName());
            parent.getChangeEvents().add("JNDI binding " + nameSpaceBindingName + " status check requested.");
            // start the status thread
            final StatusThread statusThread = new StatusThread();
            statusThread.serverName = serverName;
            statusThread.nameSpaceBindingName = nameSpaceBindingName;
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
            // check if the user has the look
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
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // get the server and name space binding name
            final String serverName = (String) scopeField.getSelectedItem();
            final String nameSpaceBindingName = event.getActionCommand();
            // display confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new ConfirmWindow(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    // add a message in the log pane and the journal
                    KalumetConsoleApplication.getApplication().getLogPane().addInfo("JNDI binding " + nameSpaceBindingName + " update in progress...", parent.getEnvironmentName());
                    parent.getChangeEvents().add("JNDI binding " + nameSpaceBindingName + " update in progress...");
                    // start the update thread
                    final UpdateThread updateThread = new UpdateThread();
                    updateThread.serverName = serverName;
                    updateThread.nameSpaceBindingName = nameSpaceBindingName;
                    updateThread.start();
                    // sync with the client
                    KalumetConsoleApplication.getApplication().enqueueTask(KalumetConsoleApplication.getApplication().getTaskQueue(), new Runnable() {
                        public void run() {
                            if (updateThread.ended) {
                                if (updateThread.failure) {
                                    KalumetConsoleApplication.getApplication().getLogPane().addError(updateThread.message, parent.getEnvironmentName());
                                    parent.getChangeEvents().add(updateThread.message);
                                } else {
                                    KalumetConsoleApplication.getApplication().getLogPane().addConfirm("JNDI binding " + nameSpaceBindingName + " updated.", parent.getEnvironmentName());
                                    parent.getChangeEvents().add("JNDI binding " + nameSpaceBindingName + " updated.");
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
     * Create a new <code>NameSpaceBindingsPane</code>.
     *
     * @param parent the parent <code>EnvironmentWindow</code>.
     */
    public NameSpaceBindingsPane(EnvironmentWindow parent) {
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

        // add JNDI bindings grid
        grid = new Grid(5);
        grid.setStyleName("border.grid");
        grid.setColumnWidth(0, new Extent(50, Extent.PX));
        grid.setColumnWidth(1, new Extent(25, Extent.PERCENT));
        grid.setColumnWidth(2, new Extent(25, Extent.PERCENT));
        grid.setColumnWidth(3, new Extent(25, Extent.PERCENT));
        grid.setColumnWidth(4, new Extent(25, Extent.PERCENT));
        content.add(grid);

        // update the pane
        update();
    }

    /**
     * Update the pane
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

        // remove all JNDI bindings grid children
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

        // add JNDI name space bindings grid header
        Label actionHeader = new Label(" ");
        actionHeader.setStyleName("grid.header");
        grid.add(actionHeader);
        Label nameHeader = new Label(Messages.getString("name"));
        nameHeader.setStyleName("grid.header");
        grid.add(nameHeader);
        Label jndiNameHeader = new Label(Messages.getString("jndi.name"));
        jndiNameHeader.setStyleName("grid.header");
        grid.add(jndiNameHeader);
        Label jndiAliasHeader = new Label(Messages.getString("jndi.alias"));
        jndiAliasHeader.setStyleName("grid.header");
        grid.add(jndiAliasHeader);
        Label providerUrlHeader = new Label(Messages.getString("provider.url"));
        providerUrlHeader.setStyleName("grid.header");
        grid.add(providerUrlHeader);
        // add the jndi name space bindings
        for (Iterator jndiNameSpaceBindingIterator = parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(applicationServerName).getJNDIBindings().iterator(); jndiNameSpaceBindingIterator.hasNext(); ) {
            JNDIBinding nameSpaceBinding = (JNDIBinding) jndiNameSpaceBindingIterator.next();
            // row
            Row row = new Row();
            row.setInsets(new Insets(2));
            row.setCellSpacing(new Extent(2));
            grid.add(row);
            // copy
            Button copyButton = new Button(Styles.PAGE_COPY);
            copyButton.setToolTipText(Messages.getString("copy"));
            copyButton.setActionCommand(nameSpaceBinding.getName());
            copyButton.addActionListener(copy);
            row.add(copyButton);
            // active
            Button activeButton;
            if (nameSpaceBinding.isActive()) {
                activeButton = new Button(Styles.LIGHTBULB);
                activeButton.setToolTipText(Messages.getString("switch.disable"));
            } else {
                activeButton = new Button(Styles.LIGHTBULB_OFF);
                activeButton.setToolTipText(Messages.getString("switch.enable"));
            }
            if (getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeResourcesPermission) {
                activeButton.setActionCommand(nameSpaceBinding.getName());
                activeButton.addActionListener(toggleActive);
            }
            row.add(activeButton);
            // blocker
            Button blockerButton;
            if (nameSpaceBinding.isBlocker()) {
                blockerButton = new Button(Styles.PLUGIN);
                blockerButton.setToolTipText(Messages.getString("switch.notblocker"));
            } else {
                blockerButton = new Button(Styles.PLUGIN_DISABLED);
                blockerButton.setToolTipText(Messages.getString("switch.blocker"));
            }
            if (getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeResourcesPermission) {
                blockerButton.setActionCommand(nameSpaceBinding.getName());
                blockerButton.addActionListener(toggleBlocker);
            }
            row.add(blockerButton);
            // status
            Button statusButton = new Button(Styles.INFORMATION);
            statusButton.setToolTipText(Messages.getString("status"));
            statusButton.setActionCommand(nameSpaceBinding.getName());
            statusButton.addActionListener(status);
            row.add(statusButton);
            if (getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeResourcesUpdatePermission) {
                // update
                Button updateButton = new Button(Styles.COG);
                updateButton.setToolTipText(Messages.getString("update"));
                updateButton.setActionCommand(nameSpaceBinding.getName());
                updateButton.addActionListener(update);
                row.add(updateButton);
            }
            if (getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeResourcesPermission) {
                // edit
                Button editButton = new Button(Styles.ACCEPT);
                editButton.setToolTipText(Messages.getString("apply"));
                editButton.setActionCommand(nameSpaceBinding.getName());
                editButton.addActionListener(edit);
                row.add(editButton);
                // delete
                Button deleteButton = new Button(Styles.DELETE);
                deleteButton.setToolTipText(Messages.getString("delete"));
                deleteButton.setActionCommand(nameSpaceBinding.getName());
                deleteButton.addActionListener(delete);
                row.add(deleteButton);
            }
            // name
            TextField nameField = new TextField();
            nameField.setStyleName("default");
            nameField.setWidth(new Extent(100, Extent.PERCENT));
            nameField.setId("nsbname_" + parent.getEnvironmentName() + "_" + applicationServerName + "_" + nameSpaceBinding.getName());
            nameField.setText(nameSpaceBinding.getName());
            grid.add(nameField);
            // jndi name
            TextField jndiNameField = new TextField();
            jndiNameField.setStyleName("default");
            jndiNameField.setWidth(new Extent(100, Extent.PERCENT));
            jndiNameField.setId("nsbjndiname_" + parent.getEnvironmentName() + "_" + applicationServerName + "_" + nameSpaceBinding.getName());
            jndiNameField.setText(nameSpaceBinding.getJndiname());
            grid.add(jndiNameField);
            // jndi alias
            TextField jndiAliasField = new TextField();
            jndiAliasField.setStyleName("default");
            jndiAliasField.setWidth(new Extent(100, Extent.PERCENT));
            jndiAliasField.setId("nsbjndialias_" + parent.getEnvironmentName() + "_" + applicationServerName + "_" + nameSpaceBinding.getName());
            jndiAliasField.setText(nameSpaceBinding.getJndialias());
            grid.add(jndiAliasField);
            // provider url
            TextField providerUrlField = new TextField();
            providerUrlField.setStyleName("default");
            providerUrlField.setWidth(new Extent(100, Extent.PERCENT));
            providerUrlField.setId("nsbproviderurl_" + parent.getEnvironmentName() + "_" + applicationServerName + "_" + nameSpaceBinding.getName());
            providerUrlField.setText(nameSpaceBinding.getProviderurl());
            grid.add(providerUrlField);
        }

        // add create name space binding row in the name space bindings grid
        if (getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeResourcesPermission) {
            // row
            Row row = new Row();
            row.setInsets(new Insets(2));
            row.setCellSpacing(new Extent(2));
            grid.add(row);
            // paste
            Button pasteButton = new Button(Styles.PAGE_PASTE);
            pasteButton.setToolTipText(Messages.getString("paste"));
            pasteButton.addActionListener(paste);
            row.add(pasteButton);
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
            // jndi name
            newJndiNameField = new TextField();
            newJndiNameField.setStyleName("default");
            newJndiNameField.setWidth(new Extent(100, Extent.PERCENT));
            grid.add(newJndiNameField);
            // jndi alias
            newJndiAliasField = new TextField();
            newJndiAliasField.setStyleName("default");
            newJndiAliasField.setWidth(new Extent(100, Extent.PERCENT));
            grid.add(newJndiAliasField);
            // provider url
            newProviderUrlField = new TextField();
            newProviderUrlField.setStyleName("default");
            newProviderUrlField.setWidth(new Extent(100, Extent.PERCENT));
            grid.add(newProviderUrlField);
        }
    }

    public EnvironmentWindow getEnvironmentWindow() {
        return parent;
    }

}