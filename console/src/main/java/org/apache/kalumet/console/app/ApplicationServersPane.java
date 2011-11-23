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
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import org.apache.kalumet.console.configuration.ConfigurationManager;
import org.apache.kalumet.model.Agent;
import org.apache.kalumet.model.J2EEApplicationServer;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.ws.client.J2EEApplicationServerClient;

/**
 * Environment application servers pane.
 */
public class ApplicationServersPane extends ContentPane {

    private EnvironmentWindow parent;
    private SelectField topologyField;
    private Grid serversGrid;

    // update thread
    class UpdateThread extends Thread {

        public String serverName;
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
                // call the WebService
                J2EEApplicationServerClient client = new J2EEApplicationServerClient(agent.getHostname(), agent.getPort());
                client.update(parent.getEnvironmentName(), serverName, false);
            } catch (Exception e) {
                message = "J2EE application server " + serverName + " update failed: " + e.getMessage();
                failure = true;
            } finally {
                ended = true;
            }
        }

    }

    // stop thread
    class StopThread extends Thread {

        public String serverName;
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
                // call the WebService
                J2EEApplicationServerClient client = new J2EEApplicationServerClient(agent.getHostname(), agent.getPort());
                client.stop(parent.getEnvironmentName(), serverName);
            } catch (Exception e) {
                message = "J2EE application server " + serverName + " stop failed: " + e.getMessage();
                failure = true;
            } finally {
                ended = true;
            }
        }

    }

    // start thread
    class StartThread extends Thread {

        public String serverName;
        public boolean ended = false;
        public boolean failure = false;
        public String message;

        public void run() {
            try {
                // load Kalumet configuration
                Kalumet kalumet = ConfigurationManager.loadStore();
                // looking for the agnet
                Agent agent = kalumet.getAgent(parent.getEnvironment().getAgent());
                if (agent == null) {
                    throw new IllegalArgumentException("agent not found.");
                }
                // call the WebService
                J2EEApplicationServerClient client = new J2EEApplicationServerClient(agent.getHostname(), agent.getPort());
                client.start(parent.getEnvironmentName(), serverName);
            } catch (Exception e) {
                message = "J2EE application server " + serverName + " start failed: " + e.getMessage();
                failure = true;
            } finally {
                ended = true;
            }
        }

    }

    // status thread
    class StatusThread extends Thread {

        public String serverName;
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
                // call the WebService
                J2EEApplicationServerClient client = new J2EEApplicationServerClient(agent.getHostname(), agent.getPort());
                message = client.status(parent.getEnvironmentName(), serverName);
            } catch (Exception e) {
                message = "J2EE application server " + serverName + " status failed: " + e.getMessage();
                failure = true;
            } finally {
                ended = true;
            }
        }
    }

    // update
    private ActionListener update = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // get JEE application server name
            final String serverName = event.getActionCommand();
            // check if the user has the environment lock
            if (!parent.getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), parent.getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!parent.adminPermission
                    && !parent.jeeServersUpdatePermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), parent.getEnvironmentName());
                return;
            }
            // check if no modification has been performed
            if (parent.isUpdated()) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.notsaved"), parent.getEnvironmentName());
                return;
            }
            // display confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new ConfirmWindow(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    // put message in the log pane and in the journal
                    KalumetConsoleApplication.getApplication().getLogPane().addInfo("J2EE application server " + serverName + " update in progress...", parent.getEnvironmentName());
                    parent.getChangeEvents().add("J2EE application server " + serverName + " update requested.");
                    // launch async task
                    final UpdateThread updateThread = new UpdateThread();
                    updateThread.serverName = serverName;
                    updateThread.start();
                    // sync with the client
                    KalumetConsoleApplication.getApplication().enqueueTask(KalumetConsoleApplication.getApplication().getTaskQueue(), new Runnable() {
                        public void run() {
                            if (updateThread.ended) {
                                if (updateThread.failure) {
                                    KalumetConsoleApplication.getApplication().getLogPane().addError(updateThread.message, parent.getEnvironmentName());
                                    parent.getChangeEvents().add(updateThread.message);
                                } else {
                                    KalumetConsoleApplication.getApplication().getLogPane().addConfirm("J2EE application server " + serverName + " updated.", parent.getEnvironmentName());
                                    parent.getChangeEvents().add("J2EE application server " + serverName + " updated.");
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
    // start
    private ActionListener start = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // get JEE application server name
            final String serverName = event.getActionCommand();
            // check if the user has the environment lock
            if (!parent.getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), parent.getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!parent.adminPermission
                    && !parent.jeeServersControlPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), parent.getEnvironmentName());
                return;
            }
            // check if no modification has been performed
            if (parent.isUpdated()) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.notsaved"), parent.getEnvironmentName());
                return;
            }
            // display confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new ConfirmWindow(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    // put message in the log pane and the journal
                    KalumetConsoleApplication.getApplication().getLogPane().addInfo("J2EE application server " + serverName + " start in progress...", parent.getEnvironmentName());
                    parent.getChangeEvents().add("J2EE application server " + serverName + " start requested.");
                    // launch async task
                    final StartThread startThread = new StartThread();
                    startThread.serverName = serverName;
                    startThread.start();
                    // sync with the client
                    KalumetConsoleApplication.getApplication().enqueueTask(KalumetConsoleApplication.getApplication().getTaskQueue(), new Runnable() {
                        public void run() {
                            if (startThread.ended) {
                                if (startThread.failure) {
                                    KalumetConsoleApplication.getApplication().getLogPane().addError(startThread.message, parent.getEnvironmentName());
                                    parent.getChangeEvents().add(startThread.message);
                                } else {
                                    KalumetConsoleApplication.getApplication().getLogPane().addConfirm("J2EE application server " + serverName + " started.", parent.getEnvironmentName());
                                    parent.getChangeEvents().add("J2EE application server " + serverName + " started.");
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
    // stop
    private ActionListener stop = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // get JEE application server name
            final String serverName = event.getActionCommand();
            // check if the user has the environment lock
            if (!parent.getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), parent.getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!parent.adminPermission
                    && !parent.jeeServersControlPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), parent.getEnvironmentName());
                return;
            }
            // check if no modification has been performed
            if (parent.isUpdated()) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.notsaved"), parent.getEnvironmentName());
                return;
            }
            // display confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new ConfirmWindow(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    // put a message in the log pane and in the journal
                    KalumetConsoleApplication.getApplication().getLogPane().addInfo("J2EE application server " + serverName + " stop in progress...", parent.getEnvironmentName());
                    parent.getChangeEvents().add("J2EE application server " + serverName + " stop requested.");
                    // launch async task
                    final StopThread stopThread = new StopThread();
                    stopThread.serverName = serverName;
                    stopThread.start();
                    // sync with the client
                    KalumetConsoleApplication.getApplication().enqueueTask(KalumetConsoleApplication.getApplication().getTaskQueue(), new Runnable() {
                        public void run() {
                            if (stopThread.ended) {
                                if (stopThread.failure) {
                                    KalumetConsoleApplication.getApplication().getLogPane().addError(stopThread.message, parent.getEnvironmentName());
                                    parent.getChangeEvents().add(stopThread.message);
                                } else {
                                    KalumetConsoleApplication.getApplication().getLogPane().addConfirm("J2EE application server " + serverName + " stopped.", parent.getEnvironmentName());
                                    parent.getChangeEvents().add("J2EE application server " + serverName + " stopped.");
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
    // status
    private ActionListener status = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if some change has not yet been saved
            if (parent.isUpdated()) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.notsaved"), parent.getEnvironmentName());
                return;
            }
            // get JEE application server name
            final String serverName = event.getActionCommand();
            // put a message in the log pane and in the journal
            KalumetConsoleApplication.getApplication().getLogPane().addInfo("J2EE application server " + serverName + " status check in progress...", parent.getEnvironmentName());
            parent.getChangeEvents().add("J2EE application server " + serverName + " status check.");
            // launch async task
            final StatusThread statusThread = new StatusThread();
            statusThread.serverName = serverName;
            statusThread.start();
            // sync with the client
            KalumetConsoleApplication.getApplication().enqueueTask(KalumetConsoleApplication.getApplication().getTaskQueue(), new Runnable() {
                public void run() {
                    if (statusThread.ended) {
                        if (statusThread.failure) {
                            KalumetConsoleApplication.getApplication().getLogPane().addWarning(statusThread.message, parent.getEnvironmentName());
                            parent.getChangeEvents().add(statusThread.message);
                        } else {
                            KalumetConsoleApplication.getApplication().getLogPane().addInfo("JEE server " + serverName + " status: " + statusThread.message, parent.getEnvironmentName());
                            parent.getChangeEvents().add("JEE server " + serverName + " status: " + statusThread.message);
                        }
                    } else {
                        KalumetConsoleApplication.getApplication().enqueueTask(KalumetConsoleApplication.getApplication().getTaskQueue(), this);
                    }
                }
            });
        }
    };
    // delete
    private ActionListener delete = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the environment lock
            if (!parent.getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), parent.getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!parent.adminPermission
                    && !parent.jeeServersPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), parent.getEnvironmentName());
                return;
            }
            // looking for the application server object
            final J2EEApplicationServer applicationServer = parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(event.getActionCommand());
            if (applicationServer == null) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("applicationserver.notfound"), parent.getEnvironmentName());
                return;
            }
            // display confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new ConfirmWindow(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    // delete the application server
                    parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServers().remove(applicationServer);
                    // add change event
                    parent.getChangeEvents().add("Delete J2EE application server " + applicationServer.getName());
                    // change the updated flag
                    parent.setUpdated(true);
                    // update the whole window
                    parent.update();
                }
            }));
        }
    };
    // edit
    private ActionListener edit = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            if (KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().getComponent("applicationserverwindow_" + parent.getEnvironmentName() + "_" + event.getActionCommand()) == null) {
                KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new ApplicationServerWindow(ApplicationServersPane.this, event.getActionCommand()));
            }
        }
    };
    // create
    private ActionListener create = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new ApplicationServerWindow(ApplicationServersPane.this, null));
        }
    };
    // copy
    private ActionListener copy = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // looking for the application server object
            J2EEApplicationServer applicationServer = parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(event.getActionCommand());
            if (applicationServer == null) {
                return;
            }
            try {
                // put the application server clone in the copy component
                KalumetConsoleApplication.getApplication().setCopyComponent(applicationServer.clone());
            } catch (Exception e) {
                return;
            }
        }
    };
    // toggle active
    private ActionListener toggleActive = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the environment lock
            if (!parent.getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), parent.getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!parent.adminPermission
                    && !parent.jeeServersPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), parent.getEnvironmentName());
                return;
            }
            // looking for the application server object
            J2EEApplicationServer server = parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(event.getActionCommand());
            if (server == null) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("applicationserver.notfound"), parent.getEnvironmentName());
                return;
            }
            // change the current server state
            if (server.isActive()) {
                server.setActive(false);
                parent.getChangeEvents().add("Disable J2EE application server " + server.getName());
            } else {
                server.setActive(true);
                parent.getChangeEvents().add("Enable J2EE application server " + server.getName());
            }
            // change the update flag
            parent.setUpdated(true);
            // update the journal tab pane
            parent.updateJournalPane();
            // update the pane
            update();
        }
    };
    // toggle blocker
    private ActionListener toggleBlocker = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the environment lock
            if (!parent.getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), parent.getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!parent.adminPermission
                    && !parent.jeeServersPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), parent.getEnvironmentName());
                return;
            }
            // looking for the application server object
            J2EEApplicationServer server = parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServer(event.getActionCommand());
            if (server == null) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("applicationserver.notfound"), parent.getEnvironmentName());
                return;
            }
            // change the current blocker state
            if (server.isBlocker()) {
                server.setBlocker(false);
            } else {
                server.setBlocker(true);
            }
            // change the update flag
            parent.setUpdated(true);
            // update the journal tab pane
            parent.updateJournalPane();
            // update the pane
            update();
        }
    };

    /**
     * Create a new <code>ApplicationServersPane</code>.
     *
     * @param parent the parent <code>EnvironmentWindow</code>.
     */
    public ApplicationServersPane(EnvironmentWindow parent) {
        super();
        setStyleName("tab.content");

        // update parent
        this.parent = parent;

        Column content = new Column();
        content.setCellSpacing(new Extent(2));
        add(content);

        // general grid layout
        Grid grid = new Grid(2);
        grid.setStyleName("default");
        grid.setColumnWidth(0, new Extent(20, Extent.PERCENT));
        grid.setColumnWidth(1, new Extent(80, Extent.PERCENT));
        content.add(grid);

        // add application servers topology
        Label topologyLabel = new Label(Messages.getString("topology"));
        topologyLabel.setStyleName("default");
        grid.add(topologyLabel);
        Object[] labels = new Object[]{Messages.getString("standalone"), Messages.getString("cluster")};
        topologyField = new SelectField(labels);
        topologyField.setStyleName("default");
        grid.add(topologyField);

        // add the create application server button
        if (parent.adminPermission
                || parent.jeeServersPermission) {
            Button createButton = new Button(Messages.getString("applicationserver.add"), Styles.ADD);
            createButton.addActionListener(create);
            content.add(createButton);
        }

        // add the J2EE application servers grid
        serversGrid = new Grid(5);
        serversGrid.setStyleName("border.grid");
        serversGrid.setColumnWidth(0, new Extent(50, Extent.PX));
        serversGrid.setColumnWidth(1, new Extent(25, Extent.PERCENT));
        serversGrid.setColumnWidth(2, new Extent(30, Extent.PERCENT));
        serversGrid.setColumnWidth(3, new Extent(30, Extent.PERCENT));
        serversGrid.setColumnWidth(4, new Extent(15, Extent.PERCENT));
        content.add(serversGrid);

        // update
        update();
    }

    /**
     * Update the pane
     */
    public void update() {
        // update the topology select field
        if (parent.getEnvironment().getJ2EEApplicationServers().isCluster()) {
            topologyField.setSelectedIndex(1);
        } else {
            topologyField.setSelectedIndex(0);
        }
        // update the J2EE application servers grid
        // remove all grid children
        serversGrid.removeAll();
        // add header
        Label serverActionsHeader = new Label(" ");
        serverActionsHeader.setStyleName("grid.header");
        serversGrid.add(serverActionsHeader);
        Label serverNameHeader = new Label(Messages.getString("name"));
        serverNameHeader.setStyleName("grid.header");
        serversGrid.add(serverNameHeader);
        Label serverTypeHeader = new Label(Messages.getString("type"));
        serverTypeHeader.setStyleName("grid.header");
        serversGrid.add(serverTypeHeader);
        Label serverUrlHeader = new Label(Messages.getString("jmx"));
        serverUrlHeader.setStyleName("grid.header");
        serversGrid.add(serverUrlHeader);
        Label serverAgentHeader = new Label(Messages.getString("agent"));
        serverAgentHeader.setStyleName("grid.header");
        serversGrid.add(serverAgentHeader);
        // add application servers
        for (Iterator serverIterator = parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServers().iterator(); serverIterator.hasNext(); ) {
            J2EEApplicationServer server = (J2EEApplicationServer) serverIterator.next();
            // application server name and actions
            Row row = new Row();
            row.setInsets(new Insets(2));
            row.setCellSpacing(new Extent(2));
            serversGrid.add(row);
            // copy button
            Button copyButton = new Button(Styles.PAGE_COPY);
            copyButton.setToolTipText(Messages.getString("copy"));
            copyButton.setActionCommand(server.getName());
            copyButton.addActionListener(copy);
            row.add(copyButton);
            // active button
            Button activeButton;
            if (server.isActive()) {
                activeButton = new Button(Styles.LIGHTBULB);
                activeButton.setToolTipText(Messages.getString("switch.disable"));
            } else {
                activeButton = new Button(Styles.LIGHTBULB_OFF);
                activeButton.setToolTipText(Messages.getString("switch.enable"));
            }
            if (parent.adminPermission
                    || parent.jeeServersPermission) {
                activeButton.setActionCommand(server.getName());
                activeButton.addActionListener(toggleActive);
            }
            row.add(activeButton);
            // blocker button
            Button blockerButton;
            if (server.isBlocker()) {
                blockerButton = new Button(Styles.PLUGIN);
                blockerButton.setToolTipText(Messages.getString("switch.notblocker"));
            } else {
                blockerButton = new Button(Styles.PLUGIN_DISABLED);
                blockerButton.setToolTipText(Messages.getString("switch.blocker"));
            }
            if (parent.adminPermission
                    || parent.jeeServersPermission) {
                blockerButton.setActionCommand(server.getName());
                blockerButton.addActionListener(toggleBlocker);
            }
            row.add(blockerButton);
            // status button
            Button statusButton = new Button(Styles.INFORMATION);
            statusButton.setToolTipText(Messages.getString("status"));
            statusButton.setActionCommand(server.getName());
            statusButton.addActionListener(status);
            row.add(statusButton);
            if (parent.adminPermission || parent.jeeServersControlPermission) {
                // stop button
                Button stopButton = new Button(Styles.FLAG_RED);
                stopButton.setToolTipText(Messages.getString("stop"));
                stopButton.setActionCommand(server.getName());
                stopButton.addActionListener(stop);
                row.add(stopButton);
                // start button
                Button startButton = new Button(Styles.FLAG_GREEN);
                startButton.setToolTipText(Messages.getString("start"));
                startButton.setActionCommand(server.getName());
                startButton.addActionListener(start);
                row.add(startButton);
            }
            if (parent.adminPermission || parent.jeeServersUpdatePermission) {
                // update button
                Button updateButton = new Button(Styles.COG);
                updateButton.setToolTipText(Messages.getString("update"));
                updateButton.setActionCommand(server.getName());
                updateButton.addActionListener(update);
                row.add(updateButton);
            }
            // delete button
            if (parent.adminPermission || parent.jeeServersPermission) {
                Button deleteButton = new Button(Styles.DELETE);
                deleteButton.setToolTipText(Messages.getString("delete"));
                deleteButton.setActionCommand(server.getName());
                deleteButton.addActionListener(delete);
                row.add(deleteButton);
            }
            // server name
            Button serverName = new Button(server.getName());
            serverName.setStyleName("default");
            serverName.setActionCommand(server.getName());
            serverName.addActionListener(edit);
            serversGrid.add(serverName);
            // server type
            Label serverType = new Label(Messages.getString("unknown"));
            if (server.getClassname().equals("org.apache.kalumet.controller.jboss.JBossController")) {
                serverType = new Label(Messages.getString("jboss4"));
            }
            if (server.getClassname().equals("org.apache.kalumet.controller.weblogic.WeblogicController")) {
                serverType = new Label(Messages.getString("weblogic8"));
            }
            if (server.getClassname().equals("org.apache.kalumet.controller.websphere.WebsphereController")) {
                serverType = new Label(Messages.getString("websphere5"));
            }
            serverType.setStyleName("default");
            serversGrid.add(serverType);
            // server JMX URL
            Label serverNetwork = new Label(server.getJmxurl());
            serverNetwork.setStyleName("default");
            serversGrid.add(serverNetwork);
            // server agent
            Label serverAgent = new Label(server.getAgent());
            serverAgent.setStyleName("default");
            serversGrid.add(serverAgent);
        }
    }

    /**
     * Get the parent <code>EnvironmentWindow</code>
     *
     * @return the parent <code>EnvironmentWindow</code>
     */
    public EnvironmentWindow getEnvironmentWindow() {
        return parent;
    }

    public SelectField getTopologyField() {
        return this.topologyField;
    }

}
