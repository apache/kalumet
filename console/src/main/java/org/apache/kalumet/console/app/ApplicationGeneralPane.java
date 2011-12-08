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

import nextapp.echo2.app.*;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.app.list.DefaultListModel;
import org.apache.kalumet.FileManipulator;
import org.apache.kalumet.console.configuration.ConfigurationManager;
import org.apache.kalumet.model.Agent;
import org.apache.kalumet.model.Kalumet;

/**
 * J2EE application general pane.
 */
public class ApplicationGeneralPane extends ContentPane {

    private ApplicationWindow parent;
    private TextField nameField;
    private SelectField activeField;
    private SelectField blockerField;
    private TextField uriField;
    private SelectField agentField;

    // test URI
    private ActionListener testUri = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            String uri = FileManipulator.format(getUriField().getText());
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
     * Create a new <code>ApplicationGeneralPane</code>.
     *
     * @param parent the parent <code>ApplicationWindow</code>.
     */
    public ApplicationGeneralPane(ApplicationWindow parent) {
        super();
        setStyleName("tab.content");

        // update parent
        this.parent = parent;

        // grid layout
        Grid layout = new Grid(2);
        layout.setStyleName("default");
        layout.setColumnWidth(0, new Extent(20, Extent.PERCENT));
        layout.setColumnWidth(1, new Extent(80, Extent.PERCENT));
        add(layout);

        // add application name field
        Label nameLabel = new Label(Messages.getString("name"));
        nameLabel.setStyleName("grid.cell");
        layout.add(nameLabel);
        nameField = new TextField();
        nameField.setStyleName("default");
        nameField.setWidth(new Extent(100, Extent.PERCENT));
        layout.add(nameField);

        // add the application active field
        Label activeLabel = new Label(Messages.getString("active"));
        activeLabel.setStyleName("grid.cell");
        layout.add(activeLabel);
        activeField = new SelectField(MainScreen.LABELS);
        activeField.setStyleName("default");
        activeField.setWidth(new Extent(10, Extent.EX));
        layout.add(activeField);

        // add the application blocker field
        Label blockerLabel = new Label(Messages.getString("blocker"));
        blockerLabel.setStyleName("grid.cell");
        layout.add(blockerLabel);
        blockerField = new SelectField(MainScreen.LABELS);
        blockerField.setStyleName("default");
        blockerField.setWidth(new Extent(10, Extent.EX));
        layout.add(blockerField);

        // add the application uri field)
        Label uriLabel = new Label(Messages.getString("uri"));
        uriLabel.setStyleName("grid.cell");
        layout.add(uriLabel);
        Row uriRow = new Row();
        layout.add(uriRow);
        uriField = new TextField();
        uriField.setWidth(new Extent(500, Extent.PX));
        uriField.setStyleName("default");
        uriRow.add(uriField);
        Button testUriButton = new Button(Styles.WORLD);
        testUriButton.setToolTipText(Messages.getString("uri.test"));
        testUriButton.addActionListener(testUri);
        uriRow.add(testUriButton);

        // add the application agent field
        Label agentLabel = new Label(Messages.getString("agent"));
        agentLabel.setStyleName("grid.cell");
        layout.add(agentLabel);
        agentField = new SelectField();
        agentField.setStyleName("default");
        agentField.setWidth(new Extent(50, Extent.EX));
        layout.add(agentField);

        // update the pane
        update();
    }

    /**
     * Update the pane.
     */
    public void update() {
        // update the application name field
        nameField.setText(parent.getApplication().getName());
        // update the application active field
        if (parent.getApplication().isActive()) {
            activeField.setSelectedIndex(0);
        } else {
            activeField.setSelectedIndex(1);
        }
        // update the application blocker field
        if (parent.getApplication().isBlocker()) {
            blockerField.setSelectedIndex(0);
        } else {
            blockerField.setSelectedIndex(1);
        }
        // update the application uri field
        uriField.setText(parent.getApplication().getUri());
        // update the application agent field
        // load Kalumet configuration
        Kalumet kalumet;
        try {
            kalumet = ConfigurationManager.loadStore();
        } catch (Exception e) {
            KalumetConsoleApplication.getApplication().getLogPane().addError(Messages.getString("db.read") + ": " + e.getMessage(), parent.getParentPane().getEnvironmentWindow().getEnvironmentName());
            return;
        }
        // update the agent field model
        DefaultListModel agentListModel = (DefaultListModel) agentField.getModel();
        agentListModel.removeAll();
        agentListModel.add("");
        for (Iterator agentIterator = kalumet.getAgents().iterator(); agentIterator.hasNext(); ) {
            Agent agent = (Agent) agentIterator.next();
            agentListModel.add(agent.getId());
        }
        agentField.setSelectedItem(parent.getApplication().getAgent());
    }

    public TextField getNameField() {
        return this.nameField;
    }

    public SelectField getActiveField() {
        return this.activeField;
    }

    public SelectField getBlockerField() {
        return this.blockerField;
    }

    public TextField getUriField() {
        return this.uriField;
    }

    public SelectField getAgentField() {
        return this.agentField;
    }

}
