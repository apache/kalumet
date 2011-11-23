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
import nextapp.echo2.app.ContentPane;
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.Grid;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.SplitPane;
import nextapp.echo2.app.TextArea;
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.WindowPane;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.extras.app.TabPane;
import nextapp.echo2.extras.app.layout.TabPaneLayoutData;
import org.apache.kalumet.model.Agent;
import org.apache.kalumet.ws.client.AgentClient;

/**
 * Admin agent window.
 */
public class AdminAgentWindow extends WindowPane {

    // attributes
    private String agentId;
    private Agent agent;
    private AdminAgentsWindow parent;
    private TextField idField;
    private TextField hostnameField;
    private TextField portField;
    private TextField cronField;
    private TextField maxEnvironmentsField;
    private TextField maxActiveApplicationServersField;

    // status thread
    class StatusThread extends Thread {

        public boolean ended = false;
        public boolean failure = false;
        public String message;

        public void run() {
            try {
                // call the WebService client
                AgentClient client = new AgentClient(agent.getHostname(), agent.getPort());
                message = "Agent " + agentId + " version " + client.getVersion() + " started.";
            } catch (Exception e) {
                failure = true;
                message = "Agent " + agentId + " status check failed: " + e.getMessage();
            } finally {
                ended = true;
            }
        }
    }

    // close
    private ActionListener close = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            AdminAgentWindow.this.userClose();
        }
    };
    // refresh
    private ActionListener refresh = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            for (Iterator agentIterator = parent.getAgents().iterator(); agentIterator.hasNext(); ) {
                Agent current = (Agent) agentIterator.next();
                if (agent.getId().equals(agentId)) {
                    agent = current;
                    break;
                }
            }
            if (agent == null) {
                agent = new Agent();
            }
            update();
        }
    };
    // apply
    private ActionListener apply = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user is the admin
            if (!KalumetConsoleApplication.getApplication().getUserid().equals("admin")) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("agents.restricted"));
                return;
            }

            String newId = idField.getText().trim();
            String newHostname = hostnameField.getText().trim();
            String newPort = portField.getText().trim();
            String newCron = cronField.getText().trim();
            String newMaxEnvironments = maxEnvironmentsField.getText().trim();
            String newMaxEnvironmentsActive = maxActiveApplicationServersField.getText().trim();

            // check fields
            if (newId.length() < 1) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("agent.mandatory"));
                return;
            }
            // if the admin change the agent id or if it's a new agent, check if
            // the id is not already used
            if (agentId == null || agentId.trim().length() < 1 || ((agentId != null) && (agentId.trim().length() > 0) && (!newId.equals(agentId)))) {
                for (Iterator agentIterator = parent.getAgents().iterator(); agentIterator.hasNext(); ) {
                    Agent current = (Agent) agentIterator.next();
                    if (current.getId().equals(newId)) {
                        KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("agent.exists"));
                        return;
                    }
                }
            }

            // update the current agent
            agent.setId(newId);
            agent.setHostname(newHostname);
            agent.setPort(new Integer(newPort).intValue());
            if (newCron.length() < 1) {
                newCron = "0 0 0 * * ?";
            }
            agent.setCron(newCron);
            agent.setMaxmanagedenvironments(new Integer(newMaxEnvironments).intValue());
            agent.setMaxj2eeapplicationserversstarted(new Integer(newMaxEnvironmentsActive).intValue());
            if (agentId == null || agentId.trim().length() < 1) {
                // it's a new agent
                parent.getAgents().add(agent);
            }
            setTitle(Messages.getString("agent") + " " + agent.getId());
            setId("agentwindow_" + agent.getId());
            agentId = agent.getId();
            parent.update();
        }
    };
    // paste
    public ActionListener paste = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // get the copy component
            Object copy = KalumetConsoleApplication.getApplication().getCopyComponent();
            if (copy == null || !(copy instanceof Agent)) {
                return;
            }
            Agent clone = (Agent) copy;
            idField.setText(clone.getId());
            hostnameField.setText(clone.getHostname());
            portField.setText(new Integer(clone.getPort()).toString());
            cronField.setText(clone.getCron());
            maxEnvironmentsField.setText(new Integer(clone.getMaxmanagedenvironments()).toString());
            maxActiveApplicationServersField.setText(new Integer(clone.getMaxj2eeapplicationserversstarted()).toString());
        }
    };
    // delete
    public ActionListener delete = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // display a confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new ConfirmWindow(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    // looking for the agent to remove
                    Agent agentToRemove = null;
                    for (Iterator agentIterator = parent.getAgents().iterator(); agentIterator.hasNext(); ) {
                        Agent agent = (Agent) agentIterator.next();
                        if (agent.getId().equals(agentId)) {
                            agentToRemove = agent;
                            break;
                        }
                    }
                    parent.getAgents().remove(agentToRemove);
                    AdminAgentWindow.this.userClose();
                }
            }));
        }
    };
    // status
    public ActionListener status = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // add an event
            KalumetConsoleApplication.getApplication().getLogPane().addInfo("Agent " + agentId + " status check in progress ...");
            // start the status thread
            final StatusThread statusThread = new StatusThread();
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
     * Create a new <code>AdminAgentWindow</code>.
     *
     * @param parent  the <code>AdminAgentsWindow</code> parent.
     * @param agentId the <code>Agent</code> ID.
     */
    public AdminAgentWindow(AdminAgentsWindow parent, String agentId) {
        super();

        // check if the user that try to access this window is the admin
        if (!KalumetConsoleApplication.getApplication().getUserid().equals("admin")) {
            KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("agents.restricted"));
            AdminAgentWindow.this.userClose();
            return;
        }

        // update the original agent id and parent admin agents window
        this.parent = parent;
        this.agentId = agentId;

        // update the agent object from agents parent list
        for (Iterator agentIterator = parent.getAgents().iterator(); agentIterator.hasNext(); ) {
            Agent current = (Agent) agentIterator.next();
            if (current.getId().equals(agentId)) {
                this.agent = current;
                break;
            }
        }
        if (this.agent == null) {
            this.agent = new Agent();
        }

        if (agentId == null) {
            setTitle(Messages.getString("agent"));
        } else {
            setTitle(Messages.getString("agent") + " " + agentId);
        }
        setIcon(Styles.COG);
        setId("agentwindow_" + agentId);
        setStyleName("agent");
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
        // add the delete button
        Button deleteButton = new Button(Messages.getString("delete"), Styles.DELETE);
        deleteButton.setStyleName("control");
        deleteButton.addActionListener(delete);
        controlRow.add(deleteButton);
        // add the paste button
        Button pasteButton = new Button(Messages.getString("paste"), Styles.PAGE_PASTE);
        pasteButton.setStyleName("control");
        pasteButton.addActionListener(paste);
        controlRow.add(pasteButton);
        // add the status button
        Button statusButton = new Button(Messages.getString("status"), Styles.INFORMATION);
        statusButton.setStyleName("control");
        statusButton.addActionListener(status);
        controlRow.add(statusButton);
        // add the apply button
        Button applyButton = new Button(Messages.getString("apply"), Styles.ACCEPT);
        applyButton.setStyleName("control");
        applyButton.addActionListener(apply);
        controlRow.add(applyButton);
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
        ContentPane generalTab = new ContentPane();
        generalTab.setStyleName("tab.content");
        generalTab.setLayoutData(tabLayoutData);
        tabPane.add(generalTab);

        // add the general grid layout
        Grid generalGridLayout = new Grid(2);
        generalGridLayout.setStyleName("agent");
        generalGridLayout.setWidth(new Extent(100, Extent.PERCENT));
        generalGridLayout.setColumnWidth(0, new Extent(10, Extent.PERCENT));
        generalGridLayout.setColumnWidth(1, new Extent(90, Extent.PERCENT));
        generalTab.add(generalGridLayout);

        // add the agent id field
        Label agentIdLabel = new Label(Messages.getString("id"));
        agentIdLabel.setStyleName("default");
        generalGridLayout.add(agentIdLabel);
        idField = new TextField();
        idField.setStyleName("default");
        idField.setWidth(new Extent(100, Extent.PERCENT));
        generalGridLayout.add(idField);

        // add the agent hostname field
        Label agentHostnameLabel = new Label(Messages.getString("hostname"));
        agentHostnameLabel.setStyleName("default");
        generalGridLayout.add(agentHostnameLabel);
        hostnameField = new TextField();
        hostnameField.setStyleName("default");
        hostnameField.setWidth(new Extent(100, Extent.PERCENT));
        generalGridLayout.add(hostnameField);

        // add the agent port field
        Label agentPortLabel = new Label(Messages.getString("port"));
        agentPortLabel.setStyleName("default");
        generalGridLayout.add(agentPortLabel);
        portField = new TextField();
        portField.setStyleName("default");
        portField.setWidth(new Extent(15, Extent.EX));
        generalGridLayout.add(portField);

        // add the scheduler tab
        tabLayoutData = new TabPaneLayoutData();
        tabLayoutData.setTitle(Messages.getString("scheduler"));
        ContentPane schedulerTab = new ContentPane();
        schedulerTab.setLayoutData(tabLayoutData);
        schedulerTab.setStyleName("tab.content");
        tabPane.add(schedulerTab);

        // add the scheduler grid layout
        Grid schedulerGridLayout = new Grid(2);
        schedulerGridLayout.setStyleName("agent");
        schedulerGridLayout.setWidth(new Extent(100, Extent.PERCENT));
        schedulerGridLayout.setColumnWidth(0, new Extent(10, Extent.PERCENT));
        schedulerGridLayout.setColumnWidth(1, new Extent(90, Extent.PERCENT));
        schedulerTab.add(schedulerGridLayout);

        // add the cron field
        Label cronLabel = new Label(Messages.getString("cron"));
        cronLabel.setStyleName("default");
        schedulerGridLayout.add(cronLabel);
        cronField = new TextField();
        cronField.setStyleName("default");
        cronField.setWidth(new Extent(100, Extent.PERCENT));
        schedulerGridLayout.add(cronField);

        // add the cron examples
        Label cronExamplesLabel = new Label(Messages.getString("examples"));
        cronExamplesLabel.setStyleName("grid.cell");
        schedulerGridLayout.add(cronExamplesLabel);
        TextArea cronExamples = new TextArea();
        cronExamples.setStyleName("default");
        cronExamples.setWidth(new Extent(100, Extent.PERCENT));
        cronExamples.setHeight(new Extent(20, Extent.EX));
        cronExamples.setText(Messages.getString("cron.examples"));
        cronExamples.setEnabled(false);
        schedulerGridLayout.add(cronExamples);

        // add the capacity tab
        tabLayoutData = new TabPaneLayoutData();
        tabLayoutData.setTitle(Messages.getString("capacity"));
        ContentPane capacityTab = new ContentPane();
        capacityTab.setLayoutData(tabLayoutData);
        capacityTab.setStyleName("tab.content");
        tabPane.add(capacityTab);

        // add the capacity grid layout
        Grid capacityGridLayout = new Grid(2);
        capacityGridLayout.setStyleName("agent");
        capacityGridLayout.setWidth(new Extent(100, Extent.PERCENT));
        capacityGridLayout.setColumnWidth(0, new Extent(10, Extent.PERCENT));
        capacityGridLayout.setColumnWidth(1, new Extent(90, Extent.PERCENT));
        capacityTab.add(capacityGridLayout);

        // add the max environments field
        Label agentMaxEnvironmentsLabel = new Label(Messages.getString("agent.maxenvironments"));
        agentMaxEnvironmentsLabel.setStyleName("default");
        capacityGridLayout.add(agentMaxEnvironmentsLabel);
        maxEnvironmentsField = new TextField();
        maxEnvironmentsField.setStyleName("default");
        maxEnvironmentsField.setWidth(new Extent(15, Extent.EX));
        capacityGridLayout.add(maxEnvironmentsField);

        // add the max active environments field
        Label agentMaxActiveEnvironmentsLabel = new Label(Messages.getString("agent.maxactiveapplicationservers"));
        agentMaxActiveEnvironmentsLabel.setStyleName("default");
        capacityGridLayout.add(agentMaxActiveEnvironmentsLabel);
        maxActiveApplicationServersField = new TextField();
        maxActiveApplicationServersField.setStyleName("default");
        maxActiveApplicationServersField.setWidth(new Extent(15, Extent.EX));
        capacityGridLayout.add(maxActiveApplicationServersField);

        // update the view
        update();
    }

    /**
     * Update the fields value.
     */
    protected void update() {
        // update fields
        idField.setText(agent.getId());
        hostnameField.setText(agent.getHostname());
        portField.setText(new Integer(agent.getPort()).toString());
        cronField.setText(agent.getCron());
        maxEnvironmentsField.setText(new Integer(agent.getMaxmanagedenvironments()).toString());
        maxActiveApplicationServersField.setText(new Integer(agent.getMaxj2eeapplicationserversstarted()).toString());
    }

}
