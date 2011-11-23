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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import nextapp.echo2.app.Button;
import nextapp.echo2.app.Column;
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.Grid;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.SplitPane;
import nextapp.echo2.app.WindowPane;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import org.apache.kalumet.console.configuration.ConfigurationManager;
import org.apache.kalumet.model.Agent;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.ws.client.AgentClient;

/**
 * Admin agents window.
 */
public class AdminAgentsWindow extends WindowPane {

    // attributes
    private List agents;
    private Grid agentsGrid;

    // status thread
    class StatusThread extends Thread {

        public boolean ended = false;
        public boolean failure = false;
        public String id;
        public String message;

        public void run() {
            try {
                // load Kalumet configuration
                Kalumet kalumet = ConfigurationManager.loadStore();
                // looking for the agent
                Agent agent = kalumet.getAgent(id);
                if (agent == null) {
                    throw new IllegalArgumentException("agent " + id + " not found.");
                }
                // call the WebService
                AgentClient client = new AgentClient(agent.getHostname(), agent.getPort());
                message = "Agent " + id + " version " + client.getVersion() + " started.";
            } catch (Exception e) {
                failure = true;
                message = "Agent " + id + " status check failed: " + e.getMessage();
            } finally {
                ended = true;
            }
        }
    }

    // close action listener
    private ActionListener closeActionListener = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            AdminAgentsWindow.this.userClose();
        }
    };
    // refresh action listener
    private ActionListener refreshActionListener = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            Kalumet kalumet = null;
            try {
                kalumet = ConfigurationManager.loadStore();
            } catch (Exception e) {
                KalumetConsoleApplication.getApplication().getLogPane().addError(Messages.getString("db.read") + ": " + e.getMessage());
                return;
            }
            agents = kalumet.getAgents();
            update();
            KalumetConsoleApplication.getApplication().getLogPane().addInfo(Messages.getString("agents") + " " + Messages.getString("reloaded"));
        }
    };
    // delete action listener
    private ActionListener deleteActionListener = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            final String agentId = event.getActionCommand();
            // display a confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new ConfirmWindow(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    // looking for the agent to remove
                    Agent agentToRemove = null;
                    for (Iterator agentIterator = agents.iterator(); agentIterator.hasNext(); ) {
                        Agent agent = (Agent) agentIterator.next();
                        if (agent.getId().equals(agentId)) {
                            agentToRemove = agent;
                            break;
                        }
                    }
                    // remove the agnet
                    agents.remove(agentToRemove);
                    // update the window
                    update();
                }
            }));
        }
    };
    // save action listener
    private ActionListener saveActionListener = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            Kalumet kalumet = null;
            // load Kalumet configuration
            try {
                kalumet = ConfigurationManager.loadStore();
            } catch (Exception e) {
                KalumetConsoleApplication.getApplication().getLogPane().addError(Messages.getString("db.read") + ": " + e.getMessage());
                return;
            }
            kalumet.setAgents((LinkedList) agents);
            try {
                ConfigurationManager.writeStore(kalumet);
            } catch (Exception e) {
                KalumetConsoleApplication.getApplication().getLogPane().addError(Messages.getString("db.write") + ": " + e.getMessage());
                return;
            }
            KalumetConsoleApplication.getApplication().getLogPane().addConfirm(Messages.getString("agents.saved"));
        }
    };
    // edit action listener
    private ActionListener editActionListener = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            if (KalumetConsoleApplication.getApplication().getDefaultWindow().getComponent("agentwindow_" + event.getActionCommand()) == null) {
                KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new AdminAgentWindow(AdminAgentsWindow.this, event.getActionCommand()));
            }
        }
    };
    // add action listener
    private ActionListener addActionListener = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new AdminAgentWindow(AdminAgentsWindow.this, null));
        }
    };
    // copy
    private ActionListener copy = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // get the agent id
            String agentId = event.getActionCommand();
            // load Kalumet configuration
            Kalumet kalumet = null;
            try {
                kalumet = ConfigurationManager.loadStore();
                // looking for the agent
                Agent agent = kalumet.getAgent(agentId);
                if (agent == null) {
                    return;
                }
                // store an agent clone in the copy component
                KalumetConsoleApplication.getApplication().setCopyComponent(agent.clone());
            } catch (Exception e) {
                return;
            }
        }
    };
    // status
    private ActionListener status = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // add an event
            KalumetConsoleApplication.getApplication().getLogPane().addInfo("Agent " + event.getActionCommand() + " status check in progress ...");
            // start the status thread
            final StatusThread statusThread = new StatusThread();
            statusThread.id = event.getActionCommand();
            statusThread.start();
            // sync with the client
            KalumetConsoleApplication.getApplication().enqueueTask(KalumetConsoleApplication.getApplication().getTaskQueue(), new Runnable() {
                public void run() {
                    if (statusThread.ended) {
                        if (statusThread.failure) {
                            KalumetConsoleApplication.getApplication().getLogPane().addError(statusThread.message);
                        } else {
                            KalumetConsoleApplication.getApplication().getLogPane().addConfirm(statusThread.message);
                        }
                    } else {
                        KalumetConsoleApplication.getApplication().enqueueTask(KalumetConsoleApplication.getApplication().getTaskQueue(), this);
                    }
                }
            });
        }
    };

    /**
     * Create a new <code>AdminAgentsWindow</code>.
     */
    public AdminAgentsWindow() {
        super();

        // check if the user that try to access this window is the admin
        if (!KalumetConsoleApplication.getApplication().getUserid().equals("admin")) {
            KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("agents.restricted"));
            AdminAgentsWindow.this.userClose();
            return;
        }

        // load Kalumet configuration
        Kalumet kalumet = null;
        try {
            kalumet = ConfigurationManager.loadStore();
        } catch (Exception e) {
            KalumetConsoleApplication.getApplication().getLogPane().addError(Messages.getString("db.read") + ": " + e.getMessage());
            return;
        }
        this.agents = kalumet.getAgents();
        Collections.sort(this.agents);

        setTitle(Messages.getString("agents"));
        setIcon(Styles.COG);
        setStyleName("agents");
        setId("agentswindow");
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
        refreshButton.addActionListener(refreshActionListener);
        controlRow.add(refreshButton);
        // add the save button
        Button saveButton = new Button(Messages.getString("save"), Styles.DATABASE_SAVE);
        saveButton.setStyleName("control");
        saveButton.addActionListener(saveActionListener);
        controlRow.add(saveButton);
        // add the close button
        Button closeButton = new Button(Messages.getString("close"), Styles.CROSS);
        closeButton.setStyleName("control");
        closeButton.addActionListener(closeActionListener);
        controlRow.add(closeButton);

        // add the column main pane
        Column content = new Column();
        content.setStyleName("agents");
        splitPane.add(content);

        // add  button
        Button addButton = new Button(Messages.getString("agent.add"), Styles.ADD);
        addButton.addActionListener(addActionListener);
        content.add(addButton);

        // add the agents list grid
        agentsGrid = new Grid(3);
        agentsGrid.setStyleName("border.grid");
        agentsGrid.setColumnWidth(0, new Extent(18, Extent.PX));
        agentsGrid.setColumnWidth(1, new Extent(50, Extent.PERCENT));
        agentsGrid.setColumnWidth(2, new Extent(50, Extent.PERCENT));
        content.add(agentsGrid);

        // update agents grid
        update();
    }

    /**
     *
     *
     */
    protected void update() {
        // delete all agents grid child
        agentsGrid.removeAll();

        // add grid headers
        Label agentActionHeader = new Label(" ");
        agentActionHeader.setStyleName("grid.header");
        agentsGrid.add(agentActionHeader);
        Label agentIdHeader = new Label(Messages.getString("id"));
        agentIdHeader.setStyleName("grid.header");
        agentsGrid.add(agentIdHeader);
        Label agentHostnameHeader = new Label(Messages.getString("hostname"));
        agentHostnameHeader.setStyleName("grid.header");
        agentsGrid.add(agentHostnameHeader);

        // add agents in grid
        for (Iterator agentIterator = agents.iterator(); agentIterator.hasNext(); ) {
            Agent agent = (Agent) agentIterator.next();
            // action row with agent id
            Row row = new Row();
            row.setInsets(new Insets(2));
            row.setCellSpacing(new Extent(2));
            agentsGrid.add(row);
            // copy button
            Button copyButton = new Button(Styles.PAGE_COPY);
            copyButton.addActionListener(copy);
            copyButton.setActionCommand(agent.getId());
            copyButton.setToolTipText(Messages.getString("copy"));
            row.add(copyButton);
            // delete button
            Button deleteButton = new Button(Styles.DELETE);
            deleteButton.addActionListener(deleteActionListener);
            deleteButton.setActionCommand(agent.getId());
            deleteButton.setToolTipText(Messages.getString("delete"));
            row.add(deleteButton);
            // status button
            Button statusButton = new Button(Styles.INFORMATION);
            statusButton.setActionCommand(agent.getId());
            statusButton.addActionListener(status);
            statusButton.setToolTipText(Messages.getString("status"));
            row.add(statusButton);

            Button idButton = new Button(agent.getId());
            idButton.addActionListener(editActionListener);
            idButton.setActionCommand(agent.getId());
            agentsGrid.add(idButton);

            // hostname label
            Label hostnameLabel = new Label(agent.getHostname() + ":" + agent.getPort());
            hostnameLabel.setStyleName("default");
            agentsGrid.add(hostnameLabel);
        }
    }

    /**
     * Get the agents list
     *
     * @return the agents list
     */
    protected List getAgents() {
        return this.agents;
    }

}
