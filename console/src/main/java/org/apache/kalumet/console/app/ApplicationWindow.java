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
import org.apache.kalumet.model.J2EEApplication;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.ws.client.J2EEApplicationClient;

/**
 * J2EE application window.
 */
public class ApplicationWindow extends WindowPane {

    private String applicationName;
    private String serverName;
    private J2EEApplication application = null;
    private ApplicationsPane parent;
    private ApplicationGeneralPane generalPane;
    private ApplicationArchivesPane archivesPane;
    private ApplicationContentManagersPane contentManagersPane;
    private ApplicationConfigurationFilesPane configurationFilesPane;
    private ApplicationDatabasesPane databasesPane;

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
                J2EEApplicationClient client = new J2EEApplicationClient(agent.getHostname(), agent.getPort());
                client.update(parent.getEnvironmentWindow().getEnvironmentName(), serverName, applicationName, false);
            } catch (Exception e) {
                failure = true;
                message = "J2EE application " + applicationName + " update failed: " + e.getMessage();
            } finally {
                ended = true;
            }
        }
    }

    // refresh
    private ActionListener refresh = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // looking for the J2EE application object
            ApplicationWindow.this.application = parent.getEnvironmentWindow().getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(serverName).getJ2EEApplication(applicationName);
            if (ApplicationWindow.this.application == null) {
                ApplicationWindow.this.application = new J2EEApplication();
            }
            // update the window
            update();
        }
    };
    // close
    private ActionListener close = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            ApplicationWindow.this.userClose();
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
                    && !getEnvironmentWindow().jeeApplicationsPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // display confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new ConfirmWindow(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    // delete the application
                    parent.getEnvironmentWindow().getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(serverName).getJ2EEApplications().remove(application);
                    // add a change event
                    parent.getEnvironmentWindow().getChangeEvents().add("Delete J2EE application " + application.getName());
                    // change the updated flag
                    parent.getEnvironmentWindow().setUpdated(true);
                    // update the journal log tab pane
                    parent.getEnvironmentWindow().updateJournalPane();
                    // update the parent pane
                    parent.update();
                    // close the window
                    ApplicationWindow.this.userClose();
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
                    && !getEnvironmentWindow().jeeApplicationsPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // get the fields value
            String nameFieldValue = generalPane.getNameField().getText();
            int activeFieldIndex = generalPane.getActiveField().getSelectedIndex();
            int blockerFieldIndex = generalPane.getBlockerField().getSelectedIndex();
            String uriFieldValue = generalPane.getUriField().getText();
            String agentFieldValue = (String) generalPane.getAgentField().getSelectedItem();
            // check fields
            if (nameFieldValue == null || nameFieldValue.trim().length() < 1) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("application.mandatory"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // if the user change the J2EE application name, check if the name
            // doesn't already exist
            if (applicationName == null || (applicationName != null && !applicationName.equals(nameFieldValue))) {
                if (parent.getEnvironmentWindow().getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(serverName).getJ2EEApplication(nameFieldValue) != null) {
                    KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("application.exists"), getEnvironmentWindow().getEnvironmentName());
                    return;
                }
            }
            // add a change event
            if (applicationName != null) {
                parent.getEnvironmentWindow().getChangeEvents().add("Change J2EE application " + application.getName());
            }
            // update the application object
            application.setName(nameFieldValue);
            if (activeFieldIndex == 0) {
                application.setActive(true);
            } else {
                application.setActive(false);
            }
            if (blockerFieldIndex == 0) {
                application.setBlocker(true);
            } else {
                application.setBlocker(false);
            }
            application.setUri(uriFieldValue);
            application.setAgent(agentFieldValue);
            // add the application object if needed
            if (applicationName == null) {
                try {
                    parent.getEnvironmentWindow().getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(serverName).addJ2EEApplication(application);
                    parent.getEnvironmentWindow().getChangeEvents().add("Add J2EE application " + application.getName());
                } catch (Exception e) {
                    KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("application.exists"), getEnvironmentWindow().getEnvironmentName());
                    return;
                }
            }
            // update the window definition
            setTitle(Messages.getString("application") + " " + application.getName());
            setId("applicationwindow_" + parent.getEnvironmentWindow().getEnvironmentName() + "_" + serverName + "_" + application.getName());
            applicationName = application.getName();
            // change the updated flag
            parent.getEnvironmentWindow().setUpdated(true);
            // update the journal log tab pane
            parent.getEnvironmentWindow().updateJournalPane();
            // update the parent pane
            parent.update();
            // update the window
            update();
        }
    };
    // copy
    private ActionListener copy = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            try {
                KalumetConsoleApplication.getApplication().setCopyComponent(application.clone());
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
            if (copy == null || !(copy instanceof J2EEApplication)) {
                return;
            }
            application = (J2EEApplication) copy;
            applicationName = null;
            // update the parent pane
            parent.update();
            // update the window
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
                    && !getEnvironmentWindow().jeeApplicationsUpdatePermission) {
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
                    KalumetConsoleApplication.getApplication().getLogPane().addInfo("J2EE application " + applicationName + " update in progress...", parent.getEnvironmentWindow().getEnvironmentName());
                    parent.getEnvironmentWindow().getChangeEvents().add("J2EE application " + applicationName + " update requested.");
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
                                    KalumetConsoleApplication.getApplication().getLogPane().addConfirm("J2EE application " + applicationName + " updated.", parent.getEnvironmentWindow().getEnvironmentName());
                                    parent.getEnvironmentWindow().getChangeEvents().add("J2EE application " + applicationName + " updated.");
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
     * Create a new <code>ApplicationWindow</code>.
     *
     * @param parent                   the parent <code>ApplicationsPane</code>.
     * @param applicationServerName the original J2EE application server name.
     * @param applicationName       the original J2EE application name.
     */
    public ApplicationWindow(ApplicationsPane parent, String applicationServerName, String applicationName) {
        super();

        // update the parent tab pane
        this.parent = parent;

        // update the j2ee application server name and j2ee application name
        this.serverName = applicationServerName;
        this.applicationName = applicationName;

        // update the application object from the parent environment
        this.application = parent.getEnvironmentWindow().getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(serverName).getJ2EEApplication(this.applicationName);
        if (this.application == null) {
            this.application = new J2EEApplication();
        }

        if (this.applicationName == null) {
            setTitle(Messages.getString("application"));
        } else {
            setTitle(Messages.getString("application") + " " + this.applicationName);
        }
        setId("applicationwindow_" + parent.getEnvironmentWindow().getEnvironmentName() + "_" + serverName + "_" + this.applicationName);
        setStyleName("default");
        setWidth(new Extent(600, Extent.PX));
        setHeight(new Extent(400, Extent.PX));
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
                || getEnvironmentWindow().jeeApplicationsPermission) {
            // add the paste button
            Button pasteButton = new Button(Messages.getString("paste"), Styles.PAGE_PASTE);
            pasteButton.setStyleName("control");
            pasteButton.addActionListener(paste);
            controlRow.add(pasteButton);
        }
        if (getEnvironmentWindow().adminPermission
                || getEnvironmentWindow().jeeApplicationsUpdatePermission) {
            // add the update button
            Button updateButton = new Button(Messages.getString("update"), Styles.COG);
            updateButton.setStyleName("control");
            updateButton.addActionListener(update);
            controlRow.add(updateButton);
        }
        if (getEnvironmentWindow().adminPermission
                || getEnvironmentWindow().jeeApplicationsPermission) {
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

        // add the j2ee application general tab
        TabPaneLayoutData tabLayoutData = new TabPaneLayoutData();
        tabLayoutData.setTitle(Messages.getString("general"));
        generalPane = new ApplicationGeneralPane(this);
        generalPane.setLayoutData(tabLayoutData);
        tabPane.add(generalPane);

        // add the j2ee application archives tab
        tabLayoutData = new TabPaneLayoutData();
        tabLayoutData.setTitle(Messages.getString("archives"));
        archivesPane = new ApplicationArchivesPane(this);
        archivesPane.setLayoutData(tabLayoutData);
        tabPane.add(archivesPane);

        // add the j2ee application configuration files tab
        tabLayoutData = new TabPaneLayoutData();
        tabLayoutData.setTitle(Messages.getString("configurationfiles"));
        configurationFilesPane = new ApplicationConfigurationFilesPane(this);
        configurationFilesPane.setLayoutData(tabLayoutData);
        tabPane.add(configurationFilesPane);

        // add the j2ee application databases tab
        tabLayoutData = new TabPaneLayoutData();
        tabLayoutData.setTitle(Messages.getString("databases"));
        databasesPane = new ApplicationDatabasesPane(this);
        databasesPane.setLayoutData(tabLayoutData);
        tabPane.add(databasesPane);

        // add the j2ee application content managers tab
        tabLayoutData = new TabPaneLayoutData();
        tabLayoutData.setTitle(Messages.getString("contentmanagers"));
        contentManagersPane = new ApplicationContentManagersPane(this);
        contentManagersPane.setLayoutData(tabLayoutData);
        tabPane.add(contentManagersPane);
    }

    public J2EEApplication getApplication() {
        return this.application;
    }

    public ApplicationsPane getParentPane() {
        return this.parent;
    }

    public String getServerName() {
        return this.serverName;
    }

    public String getApplicationName() {
        return this.applicationName;
    }

    public EnvironmentWindow getEnvironmentWindow() {
        return parent.getEnvironmentWindow();
    }

    /**
     * Update the pane.
     */
    public void update() {
        generalPane.update();
        archivesPane.update();
        contentManagersPane.update();
        configurationFilesPane.update();
        databasesPane.update();
    }

}