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
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import org.apache.kalumet.model.JMSQueue;

/**
 * JMS server queues pane.
 */
public class JmsQueuesPane extends ContentPane {

    private JmsServerWindow parent;
    private Grid grid;
    private TextField newQueueNameField;

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
            // looking for the jms queue object
            final JMSQueue jmsQueue = parent.getJMSServer().getJMSQueue(event.getActionCommand());
            if (jmsQueue == null) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("jmsqueue.notfound"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // display confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new ConfirmWindow(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    // delete the jms queue
                    parent.getJMSServer().getJMSQueues().remove(jmsQueue);
                    // add a change event
                    parent.getEnvironmentWindow().getChangeEvents().add("Delete JMS queue " + jmsQueue.getName());
                    // change the updated flag
                    parent.getEnvironmentWindow().setUpdated(true);
                    // update the whole environnment window
                    parent.getEnvironmentWindow().update();
                    // update the pane
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
            if (!getEnvironmentWindow().adminPermission
                    && !getEnvironmentWindow().jeeResourcesPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // get the field
            TextField queueNameField = (TextField) JmsQueuesPane.this.getComponent("queuename_" + parent.getEnvironmentWindow().getEnvironmentName() + "_" + parent.getServerName() + "_" + parent.getJMSServerName() + "_"
                    + event.getActionCommand());
            // get the field value
            String queueNameFieldValue = queueNameField.getText();
            // check field
            if (queueNameFieldValue == null || queueNameFieldValue.trim().length() < 1) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("jmsqueue.mandatory"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // looking for the JMS queue object
            JMSQueue jmsQueue = parent.getJMSServer().getJMSQueue(event.getActionCommand());
            if (jmsQueue == null) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("jmsqueue.notfound"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // if the user change the JMS queue name, check if the JMS queue name
            // doesn't already exist
            if (!queueNameFieldValue.equals(jmsQueue.getName())) {
                if (parent.getJMSServer().getJMSQueue(queueNameFieldValue) != null) {
                    KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("jmsqueue.exists"), getEnvironmentWindow().getEnvironmentName());
                    return;
                }
            }
            // add a change event
            parent.getEnvironmentWindow().getChangeEvents().add("Change JMS queue " + jmsQueue.getName());
            // update the jms queue object
            jmsQueue.setName(queueNameFieldValue);
            // change the updated flag
            parent.getEnvironmentWindow().setUpdated(true);
            // update the whole parent window
            parent.getEnvironmentWindow().update();
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
            // get the field value
            String newQueueNameFieldValue = newQueueNameField.getText();
            // check the field
            if (newQueueNameFieldValue == null || newQueueNameFieldValue.trim().length() < 1) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("jmsqueue.mandatory"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // create the jms queue object
            JMSQueue jmsQueue = new JMSQueue();
            jmsQueue.setName(newQueueNameFieldValue);
            // add the jms queue in the jms server
            try {
                parent.getJMSServer().addJMSQueue(jmsQueue);
            } catch (Exception e) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("jmsqueue.exists"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // add a change event
            parent.getEnvironmentWindow().getChangeEvents().add("Add JMS queue " + jmsQueue.getName());
            // change the updated flag
            parent.getEnvironmentWindow().setUpdated(true);
            // update the whole window
            parent.getEnvironmentWindow().update();
            // update the pane
            update();
        }
    };

    /**
     * Create a new <code>JMSQueuesPane</code>.
     *
     * @param parent the parent <code>JmsServerWindow</code>.
     */
    public JmsQueuesPane(JmsServerWindow parent) {
        super();
        setStyleName("tab.content");

        // update parent
        this.parent = parent;

        // add queues grid
        grid = new Grid(2);
        grid.setStyleName("border.grid");
        grid.setColumnWidth(0, new Extent(50, Extent.PX));
        add(grid);

        // update the pane
        update();
    }

    /**
     * Update the pane.
     */
    public void update() {
        // remove queues grid children
        grid.removeAll();
        // add queues header
        Label actionHeader = new Label(" ");
        actionHeader.setStyleName("grid.header");
        grid.add(actionHeader);
        Label nameHeader = new Label(Messages.getString("name"));
        nameHeader.setStyleName("grid.header");
        grid.add(nameHeader);
        // add the queue
        for (Iterator queueIterator = parent.getJMSServer().getJMSQueues().iterator(); queueIterator.hasNext(); ) {
            JMSQueue queue = (JMSQueue) queueIterator.next();
            // row
            Row row = new Row();
            row.setCellSpacing(new Extent(2));
            row.setInsets(new Insets(2));
            grid.add(row);
            if (getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeResourcesPermission) {
                // edit
                Button editButton = new Button(Styles.ACCEPT);
                editButton.setToolTipText(Messages.getString("apply"));
                editButton.setActionCommand(queue.getName());
                editButton.addActionListener(edit);
                row.add(editButton);
                // delete
                Button deleteButton = new Button(Styles.DELETE);
                deleteButton.setToolTipText(Messages.getString("delete"));
                deleteButton.setActionCommand(queue.getName());
                deleteButton.addActionListener(delete);
                row.add(deleteButton);
            }
            // name
            TextField nameField = new TextField();
            nameField.setStyleName("default");
            nameField.setWidth(new Extent(100, Extent.PERCENT));
            nameField.setId("queuename_" + parent.getEnvironmentWindow().getEnvironmentName() + "_" + parent.getServerName() + "_" + parent.getJMSServerName() + "_" + queue.getName());
            nameField.setText(queue.getName());
            grid.add(nameField);
        }
        if (getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeResourcesPermission) {
            // row
            Row row = new Row();
            row.setCellSpacing(new Extent(2));
            row.setInsets(new Insets(2));
            grid.add(row);
            // add
            Button createButton = new Button(Styles.ADD);
            createButton.setToolTipText(Messages.getString("add"));
            createButton.addActionListener(create);
            row.add(createButton);
            // name
            newQueueNameField = new TextField();
            newQueueNameField.setStyleName("default");
            newQueueNameField.setWidth(new Extent(100, Extent.PERCENT));
            grid.add(newQueueNameField);
        }
    }

    public EnvironmentWindow getEnvironmentWindow() {
        return parent.getEnvironmentWindow();
    }

}