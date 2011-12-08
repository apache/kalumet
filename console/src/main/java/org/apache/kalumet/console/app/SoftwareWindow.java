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
import nextapp.echo2.app.SplitPane;
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.WindowPane;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.extras.app.TabPane;
import nextapp.echo2.extras.app.layout.TabPaneLayoutData;
import org.apache.kalumet.FileManipulator;
import org.apache.kalumet.console.configuration.ConfigurationManager;
import org.apache.kalumet.model.*;
import org.apache.kalumet.ws.client.SoftwareClient;

/**
 * Software window.
 */
public class SoftwareWindow extends WindowPane {

    private String name;
    private Software software;
    private SoftwaresPane parent;
    private TextField nameField;
    private SelectField activeField;
    private SelectField blockerField;
    private SelectField beforeJeeField;
    private TextField uriField;
    private SelectField updateUnitField;
    private Grid updatePlanGrid;

    private static String[] UPDATE_UNITS = new String[]{Messages.getString("location"), Messages.getString("command"),
            Messages.getString("configurationfile"), Messages.getString("database")};

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
                // call the WebService
                SoftwareClient client = new SoftwareClient(agent.getHostname(), agent.getPort());
                client.update(parent.getEnvironmentWindow().getEnvironmentName(), name, false);
            } catch (Exception e) {
                failure = true;
                message = "Software " + name + " update failed: " + e.getMessage();
            } finally {
                ended = true;
            }
        }
    }

    // execute command thread
    class ExecuteCommandThread extends Thread {

        public String commandName;
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
                // call the WebService
                SoftwareClient client = new SoftwareClient(agent.getHostname(), agent.getPort());
                client.executeCommand(parent.getEnvironmentWindow().getEnvironmentName(), name, commandName, false);
            } catch (Exception e) {
                failure = true;
                message = "Command " + commandName + " execution failed: " + e.getMessage();
            } finally {
                ended = true;
            }
        }
    }

    // update location thread
    class UpdateLocationThread extends Thread {

        public String locationName;
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
                // call the WebService
                SoftwareClient client = new SoftwareClient(agent.getHostname(), agent.getPort());
                client.updateLocation(parent.getEnvironmentWindow().getEnvironmentName(), name, locationName, false);
            } catch (Exception e) {
                failure = true;
                message = "Location " + locationName + " update failed: " + e.getMessage();
            } finally {
                ended = true;
            }
        }
    }

    // update configuration file thread
    class UpdateConfigurationFileThread extends Thread {

        public String configurationFileName;
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
                // call the WebService
                SoftwareClient client = new SoftwareClient(agent.getHostname(), agent.getPort());
                client.updateConfigurationFile(parent.getEnvironmentWindow().getEnvironmentName(), name, configurationFileName, false);
            } catch (Exception e) {
                failure = true;
                message = "Configuration file " + configurationFileName + " update failed: " + e.getMessage();
            } finally {
                ended = true;
            }
        }
    }

    // update database thread
    class UpdateDatabaseThread extends Thread {

        public String databaseName;
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
                // call the WebService
                SoftwareClient client = new SoftwareClient(agent.getHostname(), agent.getPort());
                client.updateDatabase(parent.getEnvironmentWindow().getEnvironmentName(), name, databaseName, false);
            } catch (Exception e) {
                failure = true;
                message = "Database " + databaseName + " update failed: " + e.getMessage();
            } finally {
                ended = true;
            }
        }
    }

    // refresh
    private ActionListener refresh = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // looking for the software object
            SoftwareWindow.this.software = parent.getEnvironmentWindow().getEnvironment().getSoftware(name);
            if (SoftwareWindow.this.software == null) {
                SoftwareWindow.this.software = new Software();
            }
            // update the window
            update();
        }
    };
    // close
    private ActionListener close = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            SoftwareWindow.this.userClose();
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
                    // delete the software software
                    parent.getEnvironmentWindow().getEnvironment().getSoftwares().remove(software);
                    // change the updated flag
                    parent.getEnvironmentWindow().setUpdated(true);
                    // update the journal log tab pane
                    parent.getEnvironmentWindow().updateJournalPane();
                    // update the whole environment window
                    parent.getEnvironmentWindow().update();
                    // close the window
                    SoftwareWindow.this.userClose();
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
            int beforeJeeFieldIndex = beforeJeeField.getSelectedIndex();
            String uriFieldValue = uriField.getText();
            // check fields
            if (nameFieldValue == null || nameFieldValue.trim().length() < 1) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("software.mandatory"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // if the user change the software name, check if the name
            // doesnt't already exist
            if (name == null || (name != null && !name.equals(nameFieldValue))) {
                if (parent.getEnvironmentWindow().getEnvironment().getSoftware(nameFieldValue) != null) {
                    KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("software.exists"), getEnvironmentWindow().getEnvironmentName());
                    return;
                }
            }
            // update the software object
            software.setName(nameFieldValue);
            if (activeFieldIndex == 0) {
                software.setActive(true);
            } else {
                software.setActive(false);
            }
            if (blockerFieldIndex == 0) {
                software.setBlocker(true);
            } else {
                software.setBlocker(false);
            }
            if (beforeJeeFieldIndex == 0) {
                software.setBeforej2ee(true);
            } else {
                software.setBeforej2ee(false);
            }
            software.setUri(uriFieldValue);
            // add the software object if needed
            if (name == null) {
                try {
                    parent.getEnvironmentWindow().getEnvironment().addSoftware(software);
                } catch (Exception e) {
                    KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("software.exists"), getEnvironmentWindow().getEnvironmentName());
                    return;
                }
            }
            // update the window definition
            setTitle(Messages.getString("software") + " " + software.getName());
            setId("softwarewindow_" + parent.getEnvironmentWindow().getEnvironmentName() + "_" + software.getName());
            name = software.getName();
            // change the updated flag
            parent.getEnvironmentWindow().setUpdated(true);
            // update the journal log tab pane
            parent.getEnvironmentWindow().updateJournalPane();
            // update the whole environment window
            parent.getEnvironmentWindow().update();
            // update the window
            update();
        }
    };
    // copy
    private ActionListener copy = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            try {
                KalumetConsoleApplication.getApplication().setCopyComponent(software.clone());
            } catch (Exception e) {
                return;
            }
        }
    };
    // paste
    private ActionListener paste = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            Object copy = KalumetConsoleApplication.getApplication().getCopyComponent();
            // check the copy object
            if (copy == null || !(copy instanceof Software)) {
                return;
            }
            // update the object
            software = (Software) copy;
            name = null;
            // update the parent pane
            parent.update();
            // update the window
            update();
        }
    };
    // add update unit
    private ActionListener addUpdateUnit = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            if (updateUnitField.getSelectedIndex() == 0) {
                // location
                KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new SoftwareLocationWindow(SoftwareWindow.this, null));
            }
            if (updateUnitField.getSelectedIndex() == 1) {
                // command
                KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new SoftwareCommandWindow(SoftwareWindow.this, null));
            }
            if (updateUnitField.getSelectedIndex() == 2) {
                // configuration file
                KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new SoftwareConfigurationFileWindow(SoftwareWindow.this, null));
            }
            if (updateUnitField.getSelectedIndex() == 3) {
                // database
                KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new SoftwareDatabaseWindow(SoftwareWindow.this, null));
            }
        }
    };
    // edit location
    private ActionListener editLocation = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            if (KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().getComponent("softwarelocationwindow_" + parent.getEnvironmentWindow().getEnvironmentName() + "_" + name + "_" + event.getActionCommand()) == null) {
                KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new SoftwareLocationWindow(SoftwareWindow.this, event.getActionCommand()));
            }
        }
    };
    // edit command
    private ActionListener editCommand = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            if (KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().getComponent("softwarecommandwindow_" + parent.getEnvironmentWindow().getEnvironmentName() + "_" + name + "_" + event.getActionCommand()) == null) {
                KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new SoftwareCommandWindow(SoftwareWindow.this, event.getActionCommand()));
            }
        }
    };
    // edit configuration file
    private ActionListener editConfigurationFile = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            if (KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().getComponent("softwareconfigurationfilewindow_" + parent.getEnvironmentWindow().getEnvironmentName() + "_" + name + "_" + event.getActionCommand()) == null) {
                KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new SoftwareConfigurationFileWindow(SoftwareWindow.this, event.getActionCommand()));
            }
        }
    };
    // edit database
    private ActionListener editDatabase = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            if (KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().getComponent("softwaredatabasewindow_" + parent.getEnvironmentWindow().getEnvironmentName() + "_" + name + "_" + event.getActionCommand()) == null) {
                KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new SoftwareDatabaseWindow(SoftwareWindow.this, event.getActionCommand()));
            }
        }
    };
    // toggle active location
    private ActionListener toggleActiveLocation = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the lock
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
            // looking for the location object
            Location location = software.getLocation(event.getActionCommand());
            if (location == null) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("location.notfound"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // change the state and add an event
            if (location.isActive()) {
                location.setActive(false);
                parent.getEnvironmentWindow().getChangeEvents().add("Disable software " + name + " location " + location.getName());
            } else {
                location.setActive(true);
                parent.getEnvironmentWindow().getChangeEvents().add("Enable software " + name + " location " + location.getName());
            }
            // change the update flag
            parent.getEnvironmentWindow().setUpdated(true);
            // update the journal pane
            parent.getEnvironmentWindow().updateJournalPane();
            // update the pane
            update();
        }
    };
    // toggle active command
    private ActionListener toggleActiveCommand = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the lock
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
            // looking for the command object
            Command command = software.getCommand(event.getActionCommand());
            if (command == null) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("command.notfound"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // change the state and add an event
            if (command.isActive()) {
                command.setActive(false);
                parent.getEnvironmentWindow().getChangeEvents().add("Disable software " + name + " command " + command.getName());
            } else {
                command.setActive(true);
                parent.getEnvironmentWindow().getChangeEvents().add("Enable software " + name + " command " + command.getName());
            }
            // change the update flag
            parent.getEnvironmentWindow().setUpdated(true);
            // update the journal pane
            parent.getEnvironmentWindow().updateJournalPane();
            // update the pane
            update();
        }
    };
    // toggle active configuration file
    private ActionListener toggleActiveConfigurationFile = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the lock
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
            // looking for the configuration file object
            ConfigurationFile configurationFile = software.getConfigurationFile(event.getActionCommand());
            if (configurationFile == null) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("configurationfile.notfound"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            if (configurationFile.isActive()) {
                configurationFile.setActive(false);
                parent.getEnvironmentWindow().getChangeEvents().add("Disable software " + name + " command " + configurationFile.getName());
            } else {
                configurationFile.setActive(true);
                parent.getEnvironmentWindow().getChangeEvents().add("Enable software " + name + " command " + configurationFile.getName());
            }
            // change the update flag
            parent.getEnvironmentWindow().setUpdated(true);
            // update the journal pane
            parent.getEnvironmentWindow().updateJournalPane();
            // update the pane
            update();
        }
    };
    // toggle active database
    private ActionListener toggleActiveDatabase = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the lock
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
            // looking for the database object
            Database database = software.getDatabase(event.getActionCommand());
            if (database == null) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("database.notfound"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            if (database.isActive()) {
                database.setActive(false);
                parent.getEnvironmentWindow().getChangeEvents().add("Disable software " + name + " database " + database.getName());
            } else {
                database.setActive(true);
                parent.getEnvironmentWindow().getChangeEvents().add("Enable software " + name + " database " + database.getName());
            }
            // change the update flag
            parent.getEnvironmentWindow().setUpdated(true);
            // update the journal pane
            parent.getEnvironmentWindow().updateJournalPane();
            // update the pane
            update();
        }
    };
    // toggle blocker command
    private ActionListener toggleBlockerCommand = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the lock
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
            // looking for the command object
            Command command = software.getCommand(event.getActionCommand());
            if (command == null) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("command.notfound"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            if (command.isBlocker()) {
                command.setBlocker(false);
                parent.getEnvironmentWindow().getChangeEvents().add("Set not blocker for software " + name + " command " + command.getName());
            } else {
                command.setBlocker(true);
                parent.getEnvironmentWindow().getChangeEvents().add("Set blocker for software " + name + " command " + command.getName());
            }
            // change the update flag
            parent.getEnvironmentWindow().setUpdated(true);
            // update the journal pane
            parent.getEnvironmentWindow().updateJournalPane();
            // update the pane
            update();
        }
    };
    // toggle blocker configuration file
    private ActionListener toggleBlockerConfigurationFile = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the lock
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
            // looking for the configuration file object
            ConfigurationFile configurationFile = software.getConfigurationFile(event.getActionCommand());
            if (configurationFile == null) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("configurationfile.notfound"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            if (configurationFile.isBlocker()) {
                configurationFile.setBlocker(false);
                parent.getEnvironmentWindow().getChangeEvents().add("Set not blocker for software " + name + " configuration file " + configurationFile.getName());
            } else {
                configurationFile.setBlocker(true);
                parent.getEnvironmentWindow().getChangeEvents().add("Set blocker for software " + name + " configuration file " + configurationFile.getName());
            }
            // change the update flag
            parent.getEnvironmentWindow().setUpdated(true);
            // update the journal pane
            parent.getEnvironmentWindow().updateJournalPane();
            // update the pane
            update();
        }
    };
    // toggle blocker database
    private ActionListener toggleBlockerDatabase = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the lock
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
            // looking for the database object
            Database database = software.getDatabase(event.getActionCommand());
            if (database == null) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("database.notfound"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            if (database.isBlocker()) {
                database.setBlocker(false);
                parent.getEnvironmentWindow().getChangeEvents().add("Set not blocker for software " + name + " database " + database.getName());
            } else {
                database.setBlocker(true);
                parent.getEnvironmentWindow().getChangeEvents().add("Set blocker for software " + name + " database " + database.getName());
            }
            // change the update flag
            parent.getEnvironmentWindow().setUpdated(true);
            // update the journal pane
            parent.getEnvironmentWindow().updateJournalPane();
            // update the pane
            update();
        }
    };
    // toggle blocker location
    private ActionListener toggleBlockerLocation = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the lock
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
            // looking for the location object
            Location location = software.getLocation(event.getActionCommand());
            if (location == null) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("location.notfound"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            if (location.isBlocker()) {
                location.setBlocker(false);
                parent.getEnvironmentWindow().getChangeEvents().add("Set not blocker for software " + name + " location " + location.getName());
            } else {
                location.setBlocker(true);
                parent.getEnvironmentWindow().getChangeEvents().add("Set blocker for software " + name + " location " + location.getName());
            }
            // change the update flag
            parent.getEnvironmentWindow().setUpdated(true);
            // update the journal pane
            parent.getEnvironmentWindow().updateJournalPane();
            // update the pane
            update();
        }
    };
    // copy command
    private ActionListener copyCommand = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            Command command = software.getCommand(event.getActionCommand());
            if (command == null) {
                return;
            }
            try {
                KalumetConsoleApplication.getApplication().setCopyComponent(command.clone());
            } catch (Exception e) {
                return;
            }
        }
    };
    // copy configuration file
    private ActionListener copyConfigurationFile = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            ConfigurationFile configurationFile = software.getConfigurationFile(event.getActionCommand());
            if (configurationFile == null) {
                return;
            }
            try {
                KalumetConsoleApplication.getApplication().setCopyComponent(configurationFile.clone());
            } catch (Exception e) {
                return;
            }
        }
    };
    // copy database
    private ActionListener copyDatabase = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            Database database = software.getDatabase(event.getActionCommand());
            if (database == null) {
                return;
            }
            try {
                KalumetConsoleApplication.getApplication().setCopyComponent(database.clone());
            } catch (Exception e) {
                return;
            }
        }
    };
    // copy location
    private ActionListener copyLocation = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            Location location = software.getLocation(event.getActionCommand());
            if (location == null) {
                return;
            }
            try {
                KalumetConsoleApplication.getApplication().setCopyComponent(location.clone());
            } catch (Exception e) {
                return;
            }
        }
    };
    // delete command
    private ActionListener deleteCommand = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the lock
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
            // looking for the command object
            final Command command = software.getCommand(event.getActionCommand());
            if (command == null) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("command.notfound"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // display confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new ConfirmWindow(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    // delete the command
                    software.getUpdatePlan().remove(command);
                    // add an event
                    parent.getEnvironmentWindow().getChangeEvents().add("Delete software " + name + " command " + command.getName());
                    // set the update flag
                    parent.getEnvironmentWindow().setUpdated(true);
                    // update the journal pane
                    parent.getEnvironmentWindow().updateJournalPane();
                    // update the pane
                    update();
                }
            }));
        }
    };
    // delete configuration file
    private ActionListener deleteConfigurationFile = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the lock
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
            // looking for the configuration file object
            final ConfigurationFile configurationFile = software.getConfigurationFile(event.getActionCommand());
            if (configurationFile == null) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("configurationfile.notfound"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // display confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new ConfirmWindow(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    // delete the configuration file
                    software.getUpdatePlan().remove(configurationFile);
                    // add an event
                    parent.getEnvironmentWindow().getChangeEvents().add("Delete software " + name + " configuration file " + configurationFile.getName());
                    // update the journal pane
                    parent.getEnvironmentWindow().updateJournalPane();
                    // update the pane
                    update();
                }
            }));
        }
    };
    // delete database
    private ActionListener deleteDatabase = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the lock
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
            // looking for the database object
            final Database database = software.getDatabase(event.getActionCommand());
            if (database == null) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("database.notfound"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // display confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new ConfirmWindow(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    // delete the database
                    software.getUpdatePlan().remove(database);
                    // add an event
                    parent.getEnvironmentWindow().getChangeEvents().add("Delete software " + name + " database " + database.getName());
                    // update the journal pane
                    parent.getEnvironmentWindow().updateJournalPane();
                    // update the pane
                    update();
                }
            }));
        }
    };
    // delete location
    private ActionListener deleteLocation = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the lock
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
            // looking for the location object
            final Location location = software.getLocation(event.getActionCommand());
            if (location == null) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("location.notfound"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // display confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new ConfirmWindow(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    // delete the location
                    software.getUpdatePlan().remove(location);
                    // add an event
                    parent.getEnvironmentWindow().getChangeEvents().add("Delete software " + name + " location " + location.getName());
                    // update the journal pane
                    parent.getEnvironmentWindow().updateJournalPane();
                    // update the pane
                    update();
                }
            }));
        }
    };
    // up command
    private ActionListener upCommand = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the lock
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
            // get the command object
            Command command = software.getCommand(event.getActionCommand());
            if (command == null) {
                return;
            }
            // get the command index
            int index = software.getUpdatePlan().indexOf(command);
            if (index == 0 || index == -1 || software.getUpdatePlan().size() < 2) {
                return;
            }
            // get the previous update unit
            Object previous = software.getUpdatePlan().get(index - 1);
            // switch the update unit
            software.getUpdatePlan().set(index, previous);
            software.getUpdatePlan().set(index - 1, command);
            // update the pane
            update();
        }
    };
    // down command
    private ActionListener downCommand = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the lock
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
            // get the command object
            Command command = software.getCommand(event.getActionCommand());
            if (command == null) {
                return;
            }
            // get the command index
            int index = software.getUpdatePlan().indexOf(command);
            if (index == (software.getUpdatePlan().size() - 1) || index == -1 || software.getUpdatePlan().size() < 2) {
                return;
            }
            // get the next update unit
            Object next = software.getUpdatePlan().get(index + 1);
            // switch the update unit
            software.getUpdatePlan().set(index + 1, command);
            software.getUpdatePlan().set(index, next);
            // update the pane
            update();
        }
    };
    // up configuration file
    private ActionListener upConfigurationFile = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the lock
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
            // get the configuration file object
            ConfigurationFile configurationFile = software.getConfigurationFile(event.getActionCommand());
            if (configurationFile == null) {
                return;
            }
            // get the configuration file index
            int index = software.getUpdatePlan().indexOf(configurationFile);
            if (index == 0 || index == -1 || software.getUpdatePlan().size() < 2) {
                return;
            }
            // get the previous update unit
            Object previous = software.getUpdatePlan().get(index - 1);
            // switch update unit
            software.getUpdatePlan().set(index, previous);
            software.getUpdatePlan().set(index - 1, configurationFile);
            // update the pane
            update();
        }
    };
    // down configuration file
    private ActionListener downConfigurationFile = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the lock
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
            // get the configuration file object
            ConfigurationFile configurationFile = software.getConfigurationFile(event.getActionCommand());
            if (configurationFile == null) {
                return;
            }
            // get the configuration file index
            int index = software.getUpdatePlan().indexOf(configurationFile);
            if (index == -1 || index == (software.getUpdatePlan().size() - 1) || software.getUpdatePlan().size() < 2) {
                return;
            }
            // get the next update unit
            Object next = software.getUpdatePlan().get(index + 1);
            // switch update unit
            software.getUpdatePlan().set(index, next);
            software.getUpdatePlan().set(index + 1, configurationFile);
            // update the pane
            update();
        }
    };
    // up database
    private ActionListener upDatabase = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the lock
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
            // get the database object
            Database database = software.getDatabase(event.getActionCommand());
            if (database == null) {
                return;
            }
            // get the database index
            int index = software.getUpdatePlan().indexOf(database);
            if (index == -1 || index == 0 || software.getUpdatePlan().size() < 2) {
                return;
            }
            // get the previous update unit
            Object previous = software.getUpdatePlan().get(index - 1);
            // switch update unit
            software.getUpdatePlan().set(index - 1, database);
            software.getUpdatePlan().set(index, previous);
            // update the pane
            update();
        }
    };
    // down database
    private ActionListener downDatabase = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the lock
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
            // get the database object
            Database database = software.getDatabase(event.getActionCommand());
            if (database == null) {
                return;
            }
            // get the database index
            int index = software.getUpdatePlan().indexOf(database);
            if (index == -1 || index == (software.getUpdatePlan().size() - 1) || software.getUpdatePlan().size() < 2) {
                return;
            }
            // get the next update unit
            Object next = software.getUpdatePlan().get(index + 1);
            // switch update unit
            software.getUpdatePlan().set(index, next);
            software.getUpdatePlan().set(index + 1, database);
            // update the pane
            update();
        }
    };
    // up location
    private ActionListener upLocation = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the lock
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
            // get the location object
            Location location = software.getLocation(event.getActionCommand());
            if (location == null) {
                return;
            }
            // get the location index
            int index = software.getUpdatePlan().indexOf(location);
            if (index == -1 || index == 0 || software.getUpdatePlan().size() < 2) {
                return;
            }
            // get the previous update unit
            Object previous = software.getUpdatePlan().get(index - 1);
            // switch the update units
            software.getUpdatePlan().set(index - 1, location);
            software.getUpdatePlan().set(index, previous);
            // update the pane
            update();
        }
    };
    // down location
    private ActionListener downLocation = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the lock
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
            // get the location object
            Location location = software.getLocation(event.getActionCommand());
            if (location == null) {
                return;
            }
            // get the location index
            int index = software.getUpdatePlan().indexOf(location);
            if (index == -1 || index == (software.getUpdatePlan().size() - 1) || software.getUpdatePlan().size() < 2) {
                return;
            }
            // get the next update unit
            Object next = software.getUpdatePlan().get(index + 1);
            // switch the update units
            software.getUpdatePlan().set(index, next);
            software.getUpdatePlan().set(index + 1, location);
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
            // check if some change has not been saved
            if (getEnvironmentWindow().isUpdated()) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.notsaved"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // display confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new ConfirmWindow(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    // add a message into the log pane and the journal
                    KalumetConsoleApplication.getApplication().getLogPane().addInfo("Software " + name + " update in progress ...", parent.getEnvironmentWindow().getEnvironmentName());
                    parent.getEnvironmentWindow().getChangeEvents().add("Software " + name + " update requested.");
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
                                    KalumetConsoleApplication.getApplication().getLogPane().addConfirm("Software " + name + " updated.", parent.getEnvironmentWindow().getEnvironmentName());
                                    parent.getEnvironmentWindow().getChangeEvents().add("Software " + name + " updated.");
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
    // execute command
    private ActionListener executeCommand = new ActionListener() {
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
            if (getEnvironmentWindow().isUpdated()) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.notsaved"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            final String commandName = event.getActionCommand();
            // display confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new ConfirmWindow(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    // add a message into the log pane and the journal
                    KalumetConsoleApplication.getApplication().getLogPane().addInfo("Software " + name + " command " + commandName + " execution in progress ...", parent.getEnvironmentWindow().getEnvironmentName());
                    parent.getEnvironmentWindow().getChangeEvents().add("Software " + name + " command " + commandName + " execution requested.");
                    // start the execute command thread
                    final ExecuteCommandThread executeCommandThread = new ExecuteCommandThread();
                    executeCommandThread.commandName = commandName;
                    executeCommandThread.start();
                    // sync with the client
                    KalumetConsoleApplication.getApplication().enqueueTask(KalumetConsoleApplication.getApplication().getTaskQueue(), new Runnable() {
                        public void run() {
                            if (executeCommandThread.ended) {
                                if (executeCommandThread.failure) {
                                    KalumetConsoleApplication.getApplication().getLogPane().addError(executeCommandThread.message, parent.getEnvironmentWindow().getEnvironmentName());
                                    parent.getEnvironmentWindow().getChangeEvents().add(executeCommandThread.message);
                                } else {
                                    KalumetConsoleApplication.getApplication().getLogPane().addConfirm("Software " + name + " command " + commandName + " executed.", parent.getEnvironmentWindow().getEnvironmentName());
                                    parent.getEnvironmentWindow().getChangeEvents().add("Software " + name + " command " + commandName + " executed.");
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
    // update configuration file
    private ActionListener updateConfigurationFile = new ActionListener() {
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
            if (parent.getEnvironmentWindow().isUpdated()) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.notsaved"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            final String configurationFileName = event.getActionCommand();
            // display confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new ConfirmWindow(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    // add a message into the log pane and the journal
                    KalumetConsoleApplication.getApplication().getLogPane().addInfo("Software " + name + " configuration file " + configurationFileName + " update in progress ...", parent.getEnvironmentWindow().getEnvironmentName());
                    parent.getEnvironmentWindow().getChangeEvents().add("Software " + name + " configuration file " + configurationFileName + " update requested.");
                    // start the update thread
                    final UpdateConfigurationFileThread updateThread = new UpdateConfigurationFileThread();
                    updateThread.configurationFileName = configurationFileName;
                    updateThread.start();
                    // sync with the client
                    KalumetConsoleApplication.getApplication().enqueueTask(KalumetConsoleApplication.getApplication().getTaskQueue(), new Runnable() {
                        public void run() {
                            if (updateThread.ended) {
                                if (updateThread.failure) {
                                    KalumetConsoleApplication.getApplication().getLogPane().addError(updateThread.message, parent.getEnvironmentWindow().getEnvironmentName());
                                    parent.getEnvironmentWindow().getChangeEvents().add(updateThread.message);
                                } else {
                                    KalumetConsoleApplication.getApplication().getLogPane().addConfirm("Software " + name + " configuration file " + configurationFileName + " updated.", parent.getEnvironmentWindow().getEnvironmentName());
                                    parent.getEnvironmentWindow().getChangeEvents().add("Software " + name + " configuration file " + configurationFileName + " updated.");
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
    // update database
    private ActionListener updateDatabase = new ActionListener() {
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
            if (parent.getEnvironmentWindow().isUpdated()) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.notsaved"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // get the database name
            final String databaseName = event.getActionCommand();
            // display confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new ConfirmWindow(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    // add a message into the log pane and the journal
                    KalumetConsoleApplication.getApplication().getLogPane().addInfo("Software " + name + " database " + databaseName + " update in progress ...", parent.getEnvironmentWindow().getEnvironmentName());
                    parent.getEnvironmentWindow().getChangeEvents().add("Software " + name + " database " + databaseName + " update requested.");
                    // start the update thread
                    final UpdateDatabaseThread updateThread = new UpdateDatabaseThread();
                    updateThread.databaseName = databaseName;
                    updateThread.start();
                    // sync with the client
                    KalumetConsoleApplication.getApplication().enqueueTask(KalumetConsoleApplication.getApplication().getTaskQueue(), new Runnable() {
                        public void run() {
                            if (updateThread.ended) {
                                if (updateThread.failure) {
                                    KalumetConsoleApplication.getApplication().getLogPane().addError(updateThread.message, parent.getEnvironmentWindow().getEnvironmentName());
                                    parent.getEnvironmentWindow().getChangeEvents().add(updateThread.message);
                                } else {
                                    KalumetConsoleApplication.getApplication().getLogPane().addConfirm("Software " + name + " database " + databaseName + " updated.", parent.getEnvironmentWindow().getEnvironmentName());
                                    parent.getEnvironmentWindow().getChangeEvents().add("Software " + name + " database " + databaseName + " updated.");
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
    // update location
    private ActionListener updateLocation = new ActionListener() {
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
            if (getEnvironmentWindow().isUpdated()) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.notsaved"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // get the location name
            final String locationName = event.getActionCommand();
            // display confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new ConfirmWindow(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    // add a message into the log pane and the journal
                    KalumetConsoleApplication.getApplication().getLogPane().addInfo("Software " + name + " location " + locationName + " update in progress ...", parent.getEnvironmentWindow().getEnvironmentName());
                    parent.getEnvironmentWindow().getChangeEvents().add("Software " + name + " location " + locationName + " update requested.");
                    // start the update thread
                    final UpdateLocationThread updateThread = new UpdateLocationThread();
                    updateThread.locationName = locationName;
                    updateThread.start();
                    // sync with the client
                    KalumetConsoleApplication.getApplication().enqueueTask(KalumetConsoleApplication.getApplication().getTaskQueue(), new Runnable() {
                        public void run() {
                            if (updateThread.ended) {
                                if (updateThread.failure) {
                                    KalumetConsoleApplication.getApplication().getLogPane().addError(updateThread.message, parent.getEnvironmentWindow().getEnvironmentName());
                                    parent.getEnvironmentWindow().getChangeEvents().add(updateThread.message);
                                } else {
                                    KalumetConsoleApplication.getApplication().getLogPane().addConfirm("Software " + name + " location " + locationName + " updated.", parent.getEnvironmentWindow().getEnvironmentName());
                                    parent.getEnvironmentWindow().getChangeEvents().add("Software " + name + " location " + locationName + " updated.");
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
    // test URI
    private ActionListener testUri = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            String uri = FileManipulator.format(uriField.getText());
            boolean exists = false;
            FileManipulator fileManipulator = null;
            try {
                fileManipulator = new FileManipulator();
                exists = fileManipulator.exists(uri);
            } catch (Exception e) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning("Can't check the URI " + uri + ": " + e.getMessage(), parent.getEnvironmentWindow().getEnvironmentName());
            } finally {
                if (fileManipulator != null) {
                    fileManipulator.close();
                }
            }
            if (exists) {
                KalumetConsoleApplication.getApplication().getLogPane().addConfirm("URI " + uri + " exists.", parent.getEnvironmentWindow().getEnvironmentName());
            } else {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning("URI " + uri + " doesn't exists.", parent.getEnvironmentWindow().getEnvironmentName());
            }
        }
    };

    /**
     * Create a new <code>ExternalWindow</code>.
     *
     * @param parent       the parent <code>ExternalsPane</code>.
     * @param softwareName the original <code>External</code> name.
     */
    public SoftwareWindow(SoftwaresPane parent, String softwareName) {
        super();

        // update the parent tab pane
        this.parent = parent;

        // update the software name
        this.name = softwareName;

        // update the software object from the parent environment
        this.software = parent.getEnvironmentWindow().getEnvironment().getSoftware(softwareName);
        if (this.software == null) {
            this.software = new Software();
        }

        if (softwareName == null) {
            setTitle(Messages.getString("software"));
        } else {
            setTitle(Messages.getString("software") + " " + softwareName);
        }
        setId("softwarewindow_" + parent.getEnvironmentWindow().getEnvironmentName() + "_" + softwareName);
        setStyleName("default");
        setWidth(new Extent(800, Extent.PX));
        setHeight(new Extent(600, Extent.PX));
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
            Button applyButton = new Button(Messages.getString("apply"), Styles.DATABASE_SAVE);
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

        // add a tab pane
        TabPane tabPane = new TabPane();
        tabPane.setStyleName("default");
        splitPane.add(tabPane);

        // add the general pane
        TabPaneLayoutData tabLayoutData = new TabPaneLayoutData();
        tabLayoutData.setTitle(Messages.getString("general"));
        ContentPane generalPane = new ContentPane();
        generalPane.setStyleName("tab.content");
        generalPane.setLayoutData(tabLayoutData);
        tabPane.add(generalPane);

        // add the main grid
        Grid generalLayoutGrid = new Grid(2);
        generalLayoutGrid.setStyleName("default");
        generalLayoutGrid.setColumnWidth(0, new Extent(20, Extent.PERCENT));
        generalLayoutGrid.setColumnWidth(1, new Extent(80, Extent.PERCENT));
        generalPane.add(generalLayoutGrid);

        // add name field
        Label nameLabel = new Label(Messages.getString("name"));
        nameLabel.setStyleName("grid.cell");
        generalLayoutGrid.add(nameLabel);
        nameField = new TextField();
        nameField.setStyleName("default");
        nameField.setWidth(new Extent(100, Extent.PERCENT));
        generalLayoutGrid.add(nameField);

        // add active field
        Label activeLabel = new Label(Messages.getString("active"));
        activeLabel.setStyleName("grid.cell");
        generalLayoutGrid.add(activeLabel);
        activeField = new SelectField(MainScreen.LABELS);
        activeField.setStyleName("default");
        activeField.setWidth(new Extent(10, Extent.EX));
        generalLayoutGrid.add(activeField);

        // add blocker field
        Label blockerLabel = new Label(Messages.getString("blocker"));
        blockerLabel.setStyleName("grid.cell");
        generalLayoutGrid.add(blockerLabel);
        blockerField = new SelectField(MainScreen.LABELS);
        blockerField.setStyleName("default");
        blockerField.setWidth(new Extent(10, Extent.EX));
        generalLayoutGrid.add(blockerField);

        // add before JEE field
        Label beforeJeeLabel = new Label(Messages.getString("before.jee"));
        beforeJeeLabel.setStyleName("grid.cell");
        generalLayoutGrid.add(beforeJeeLabel);
        beforeJeeField = new SelectField(MainScreen.LABELS);
        beforeJeeField.setStyleName("default");
        beforeJeeField.setWidth(new Extent(10, Extent.EX));
        generalLayoutGrid.add(beforeJeeField);

        // add uri field
        Label uriLabel = new Label(Messages.getString("uri"));
        uriLabel.setStyleName("grid.cell");
        generalLayoutGrid.add(uriLabel);
        Row uriRow = new Row();
        generalLayoutGrid.add(uriRow);
        uriField = new TextField();
        uriField.setStyleName("default");
        uriField.setWidth(new Extent(500, Extent.PX));
        uriRow.add(uriField);
        Button testUriButton = new Button(Styles.WORLD);
        testUriButton.setToolTipText(Messages.getString("uri.test"));
        testUriButton.addActionListener(testUri);
        uriRow.add(testUriButton);

        // add the update plan tab
        ContentPane updatePlanPane = new ContentPane();
        updatePlanPane.setStyleName("tab.content");
        tabLayoutData = new TabPaneLayoutData();
        tabLayoutData.setTitle(Messages.getString("update.plan"));
        updatePlanPane.setLayoutData(tabLayoutData);
        tabPane.add(updatePlanPane);

        Column updatePlanContent = new Column();
        updatePlanPane.add(updatePlanContent);

        // add add update unit button
        Row addRow = new Row();
        addRow.setCellSpacing(new Extent(2));
        addRow.setInsets(new Insets(2));
        updatePlanContent.add(addRow);
        updateUnitField = new SelectField(SoftwareWindow.UPDATE_UNITS);
        updateUnitField.setStyleName("default");
        updateUnitField.setWidth(new Extent(50, Extent.EX));
        addRow.add(updateUnitField);
        Button addUpdateUnitButton = new Button(Messages.getString("update.unit.add"), Styles.ADD);
        addUpdateUnitButton.addActionListener(addUpdateUnit);
        addRow.add(addUpdateUnitButton);

        // add a update plan grid
        updatePlanGrid = new Grid(4);
        updatePlanGrid.setStyleName("border.grid");
        updatePlanGrid.setColumnWidth(0, new Extent(50, Extent.PX));
        updatePlanContent.add(updatePlanGrid);

        // update the window
        update();
    }

    /**
     * Update this window.
     */
    public void update() {
        // update the general pane
        // update the name field
        nameField.setText(software.getName());
        // update the active field
        if (software.isActive()) {
            activeField.setSelectedIndex(0);
        } else {
            activeField.setSelectedIndex(1);
        }
        // update the blocker field
        if (software.isBlocker()) {
            blockerField.setSelectedIndex(0);
        } else {
            blockerField.setSelectedIndex(1);
        }
        uriField.setText(software.getUri());
        // update the before JEE field
        if (software.isBeforej2ee()) {
            beforeJeeField.setSelectedIndex(0);
        } else {
            beforeJeeField.setSelectedIndex(1);
        }

        // update the update plan grid
        updatePlanGrid.removeAll();
        // add the headers
        Label actionHeader = new Label("");
        actionHeader.setStyleName("grid.header");
        updatePlanGrid.add(actionHeader);
        Label nameHeader = new Label(Messages.getString("name"));
        nameHeader.setStyleName("grid.header");
        updatePlanGrid.add(nameHeader);
        Label typeHeader = new Label(Messages.getString("type"));
        typeHeader.setStyleName("grid.header");
        updatePlanGrid.add(typeHeader);
        Label agentHeader = new Label(Messages.getString("agent"));
        agentHeader.setStyleName("grid.header");
        updatePlanGrid.add(agentHeader);
        // iterate in the update plan items
        for (Iterator itemIterator = software.getUpdatePlan().iterator(); itemIterator.hasNext(); ) {
            Object item = itemIterator.next();

            if (item instanceof Command) {
                // command
                Command command = (Command) item;
                // actions row
                Row row = new Row();
                row.setCellSpacing(new Extent(2));
                row.setInsets(new Insets(2));
                updatePlanGrid.add(row);
                // copy
                Button copyButton = new Button(Styles.PAGE_COPY);
                copyButton.setToolTipText(Messages.getString("copy"));
                copyButton.setActionCommand(command.getName());
                copyButton.addActionListener(copyCommand);
                row.add(copyButton);
                // active
                Button activeButton;
                if (command.isActive()) {
                    activeButton = new Button(Styles.LIGHTBULB);
                    activeButton.setToolTipText(Messages.getString("switch.disable"));
                } else {
                    activeButton = new Button(Styles.LIGHTBULB_OFF);
                    activeButton.setToolTipText(Messages.getString("switch.enable"));
                }
                if (getEnvironmentWindow().adminPermission
                        || getEnvironmentWindow().softwaresPermission) {
                    activeButton.setActionCommand(command.getName());
                    activeButton.addActionListener(toggleActiveCommand);
                }
                row.add(activeButton);
                // blocker
                Button blockerButton;
                if (command.isBlocker()) {
                    blockerButton = new Button(Styles.PLUGIN);
                    blockerButton.setToolTipText(Messages.getString("switch.notblocker"));
                } else {
                    blockerButton = new Button(Styles.PLUGIN_DISABLED);
                    blockerButton.setToolTipText(Messages.getString("switch.blocker"));
                }
                if (getEnvironmentWindow().adminPermission
                        || getEnvironmentWindow().softwaresPermission) {
                    blockerButton.setActionCommand(command.getName());
                    blockerButton.addActionListener(toggleBlockerCommand);
                }
                row.add(blockerButton);
                if (getEnvironmentWindow().adminPermission
                        || getEnvironmentWindow().softwaresPermission) {
                    // up
                    Button upButton = new Button(Styles.ARROW_UP);
                    upButton.setToolTipText(Messages.getString("up"));
                    upButton.setActionCommand(command.getName());
                    upButton.addActionListener(upCommand);
                    row.add(upButton);
                    // down
                    Button downButton = new Button(Styles.ARROW_DOWN);
                    downButton.setToolTipText(Messages.getString("down"));
                    downButton.setActionCommand(command.getName());
                    downButton.addActionListener(downCommand);
                    row.add(downButton);
                }
                if (getEnvironmentWindow().adminPermission
                        || getEnvironmentWindow().softwaresUpdatePermission) {
                    // execute command
                    Button executeButton = new Button(Styles.COG);
                    executeButton.setToolTipText(Messages.getString("execute"));
                    executeButton.setActionCommand(command.getName());
                    executeButton.addActionListener(executeCommand);
                    row.add(executeButton);
                }
                if (getEnvironmentWindow().adminPermission
                        || getEnvironmentWindow().softwaresPermission) {
                    // delete
                    Button deleteButton = new Button(Styles.DELETE);
                    deleteButton.setToolTipText(Messages.getString("delete"));
                    deleteButton.setActionCommand(command.getName());
                    deleteButton.addActionListener(deleteCommand);
                    row.add(deleteButton);
                }
                // name
                Button name = new Button(command.getName());
                name.setActionCommand(command.getName());
                name.addActionListener(editCommand);
                updatePlanGrid.add(name);
                // type
                Label type = new Label(Messages.getString("command"));
                updatePlanGrid.add(type);
                // agent
                Label agent = new Label(command.getAgent());
                updatePlanGrid.add(agent);
            }

            if (item instanceof Location) {
                // location
                Location location = (Location) item;
                // actions row
                Row row = new Row();
                row.setCellSpacing(new Extent(2));
                row.setInsets(new Insets(2));
                updatePlanGrid.add(row);

                // copy
                Button copyButton = new Button(Styles.PAGE_COPY);
                copyButton.setToolTipText(Messages.getString("copy"));
                copyButton.setActionCommand(location.getName());
                copyButton.addActionListener(copyLocation);
                row.add(copyButton);
                // active
                Button activeButton;
                if (location.isActive()) {
                    activeButton = new Button(Styles.LIGHTBULB);
                    activeButton.setToolTipText(Messages.getString("switch.disable"));
                } else {
                    activeButton = new Button(Styles.LIGHTBULB_OFF);
                    activeButton.setToolTipText(Messages.getString("switch.enable"));
                }
                if (getEnvironmentWindow().adminPermission
                        || getEnvironmentWindow().softwaresPermission) {
                    activeButton.setActionCommand(location.getName());
                    activeButton.addActionListener(toggleActiveLocation);
                }
                row.add(activeButton);
                // blocker
                Button blockerButton;
                if (location.isBlocker()) {
                    blockerButton = new Button(Styles.PLUGIN);
                    blockerButton.setToolTipText(Messages.getString("switch.notblocker"));
                } else {
                    blockerButton = new Button(Styles.PLUGIN_DISABLED);
                    blockerButton.setToolTipText(Messages.getString("switch.blocker"));
                }
                if (getEnvironmentWindow().adminPermission
                        || getEnvironmentWindow().softwaresPermission) {
                    blockerButton.setActionCommand(location.getName());
                    blockerButton.addActionListener(toggleBlockerLocation);
                }
                row.add(blockerButton);
                if (getEnvironmentWindow().adminPermission
                        || getEnvironmentWindow().softwaresPermission) {
                    // up
                    Button upButton = new Button(Styles.ARROW_UP);
                    upButton.setToolTipText(Messages.getString("up"));
                    upButton.setActionCommand(location.getName());
                    upButton.addActionListener(upLocation);
                    row.add(upButton);
                    // down
                    Button downButton = new Button(Styles.ARROW_DOWN);
                    downButton.setToolTipText(Messages.getString("down"));
                    downButton.setActionCommand(location.getName());
                    downButton.addActionListener(downLocation);
                    row.add(downButton);
                }
                if (getEnvironmentWindow().adminPermission
                        || getEnvironmentWindow().softwaresUpdatePermission) {
                    // update
                    Button updateButton = new Button(Styles.COG);
                    updateButton.setToolTipText(Messages.getString("update"));
                    updateButton.setActionCommand(location.getName());
                    updateButton.addActionListener(updateLocation);
                    row.add(updateButton);
                }
                if (getEnvironmentWindow().adminPermission
                        || getEnvironmentWindow().softwaresPermission) {
                    // delete
                    Button deleteButton = new Button(Styles.DELETE);
                    deleteButton.setToolTipText(Messages.getString("delete"));
                    deleteButton.setActionCommand(location.getName());
                    deleteButton.addActionListener(deleteLocation);
                    row.add(deleteButton);
                }
                // name
                Button name = new Button(location.getName());
                name.setActionCommand(location.getName());
                name.addActionListener(editLocation);
                updatePlanGrid.add(name);
                // type
                Label type = new Label(Messages.getString("location"));
                updatePlanGrid.add(type);
                // agent
                Label agent = new Label(location.getAgent());
                updatePlanGrid.add(agent);
            }

            if (item instanceof Database) {
                // database
                Database database = (Database) item;
                // actions row
                Row row = new Row();
                row.setCellSpacing(new Extent(2));
                row.setInsets(new Insets(2));
                updatePlanGrid.add(row);
                // copy
                Button copyButton = new Button(Styles.PAGE_COPY);
                copyButton.setToolTipText(Messages.getString("copy"));
                copyButton.setActionCommand(database.getName());
                copyButton.addActionListener(copyDatabase);
                row.add(copyButton);
                // active
                Button activeButton;
                if (database.isActive()) {
                    activeButton = new Button(Styles.LIGHTBULB);
                    activeButton.setToolTipText(Messages.getString("switch.disable"));
                } else {
                    activeButton = new Button(Styles.LIGHTBULB_OFF);
                    activeButton.setToolTipText(Messages.getString("switch.enable"));
                }
                if (getEnvironmentWindow().adminPermission
                        || getEnvironmentWindow().softwaresPermission) {
                    activeButton.setActionCommand(database.getName());
                    activeButton.addActionListener(toggleActiveDatabase);
                }
                row.add(activeButton);
                // blocker
                Button blockerButton;
                if (database.isBlocker()) {
                    blockerButton = new Button(Styles.PLUGIN);
                    blockerButton.setToolTipText(Messages.getString("switch.notblocker"));
                } else {
                    blockerButton = new Button(Styles.PLUGIN_DISABLED);
                    blockerButton.setToolTipText(Messages.getString("switch.blocker"));
                }
                if (getEnvironmentWindow().adminPermission
                        || getEnvironmentWindow().softwaresPermission) {
                    blockerButton.setActionCommand(database.getName());
                    blockerButton.addActionListener(toggleBlockerDatabase);
                }
                row.add(blockerButton);
                if (getEnvironmentWindow().adminPermission
                        || getEnvironmentWindow().softwaresPermission) {
                    // up
                    Button upButton = new Button(Styles.ARROW_UP);
                    upButton.setToolTipText(Messages.getString("up"));
                    upButton.setActionCommand(database.getName());
                    upButton.addActionListener(upDatabase);
                    row.add(upButton);
                    // down
                    Button downButton = new Button(Styles.ARROW_DOWN);
                    downButton.setToolTipText(Messages.getString("down"));
                    downButton.setActionCommand(database.getName());
                    downButton.addActionListener(downDatabase);
                    row.add(downButton);
                }
                if (getEnvironmentWindow().adminPermission
                        || getEnvironmentWindow().softwaresUpdatePermission) {
                    // update
                    Button updateButton = new Button(Styles.COG);
                    updateButton.setToolTipText(Messages.getString("update"));
                    updateButton.setActionCommand(database.getName());
                    updateButton.addActionListener(updateDatabase);
                    row.add(updateButton);
                }
                if (getEnvironmentWindow().adminPermission ||
                        getEnvironmentWindow().softwaresPermission) {
                    // delete
                    Button deleteButton = new Button(Styles.DELETE);
                    deleteButton.setToolTipText(Messages.getString("delete"));
                    deleteButton.setActionCommand(database.getName());
                    deleteButton.addActionListener(deleteDatabase);
                    row.add(deleteButton);
                }
                // name
                Button name = new Button(database.getName());
                name.setActionCommand(database.getName());
                name.addActionListener(editDatabase);
                updatePlanGrid.add(name);
                // type
                Label type = new Label(Messages.getString("database"));
                updatePlanGrid.add(type);
                // agent
                Label agent = new Label(database.getAgent());
                updatePlanGrid.add(agent);
            }

            if (item instanceof ConfigurationFile) {
                // configuration file
                ConfigurationFile configurationFile = (ConfigurationFile) item;
                // actions row
                Row row = new Row();
                row.setCellSpacing(new Extent(2));
                row.setInsets(new Insets(2));
                updatePlanGrid.add(row);
                // copy
                Button copyButton = new Button(Styles.PAGE_COPY);
                copyButton.setToolTipText(Messages.getString("copy"));
                copyButton.setActionCommand(configurationFile.getName());
                copyButton.addActionListener(copyConfigurationFile);
                row.add(copyButton);
                // active
                Button activeButton;
                if (configurationFile.isActive()) {
                    activeButton = new Button(Styles.LIGHTBULB);
                    activeButton.setToolTipText(Messages.getString("switch.disable"));
                } else {
                    activeButton = new Button(Styles.LIGHTBULB_OFF);
                    activeButton.setToolTipText(Messages.getString("switch.enable"));
                }
                if (getEnvironmentWindow().adminPermission
                        || getEnvironmentWindow().softwaresPermission) {
                    activeButton.setActionCommand(configurationFile.getName());
                    activeButton.addActionListener(toggleActiveConfigurationFile);
                }
                row.add(activeButton);
                // blocker
                Button blockerButton;
                if (configurationFile.isBlocker()) {
                    blockerButton = new Button(Styles.PLUGIN);
                    blockerButton.setToolTipText(Messages.getString("switch.notblocker"));
                } else {
                    blockerButton = new Button(Styles.PLUGIN_DISABLED);
                    blockerButton.setToolTipText(Messages.getString("switch.blocker"));
                }
                if (getEnvironmentWindow().adminPermission
                        || getEnvironmentWindow().softwaresPermission) {
                    blockerButton.setActionCommand(configurationFile.getName());
                    blockerButton.addActionListener(toggleBlockerConfigurationFile);
                }
                row.add(blockerButton);
                if (getEnvironmentWindow().adminPermission
                        || getEnvironmentWindow().softwaresPermission) {
                    // up
                    Button upButton = new Button(Styles.ARROW_UP);
                    upButton.setToolTipText(Messages.getString("up"));
                    upButton.setActionCommand(configurationFile.getName());
                    upButton.addActionListener(upConfigurationFile);
                    row.add(upButton);
                    // down
                    Button downButton = new Button(Styles.ARROW_DOWN);
                    downButton.setToolTipText(Messages.getString("down"));
                    downButton.setActionCommand(configurationFile.getName());
                    downButton.addActionListener(downConfigurationFile);
                    row.add(downButton);
                }
                if (getEnvironmentWindow().adminPermission
                        || getEnvironmentWindow().softwaresUpdatePermission) {
                    // update
                    Button updateButton = new Button(Styles.COG);
                    updateButton.setToolTipText(Messages.getString("update"));
                    updateButton.setActionCommand(configurationFile.getName());
                    updateButton.addActionListener(updateConfigurationFile);
                    row.add(updateButton);
                }
                if (getEnvironmentWindow().adminPermission
                        || getEnvironmentWindow().softwaresPermission) {
                    // delete
                    Button deleteButton = new Button(Styles.DELETE);
                    deleteButton.setToolTipText(Messages.getString("delete"));
                    deleteButton.setActionCommand(configurationFile.getName());
                    deleteButton.addActionListener(deleteConfigurationFile);
                    row.add(deleteButton);
                }
                // name
                Button name = new Button(configurationFile.getName());
                name.setActionCommand(configurationFile.getName());
                name.addActionListener(editConfigurationFile);
                updatePlanGrid.add(name);
                // type
                Label type = new Label(Messages.getString("configurationfile"));
                updatePlanGrid.add(type);
                // agent
                Label agent = new Label(configurationFile.getAgent());
                updatePlanGrid.add(agent);
            }
        }
    }

    public String getName() {
        return this.name;
    }

    public Software getSoftware() {
        return this.software;
    }

    public SoftwaresPane getParentPane() {
        return this.parent;
    }

    public EnvironmentWindow getEnvironmentWindow() {
        return parent.getEnvironmentWindow();
    }

}
