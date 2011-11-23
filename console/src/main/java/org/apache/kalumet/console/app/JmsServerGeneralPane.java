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

import nextapp.echo2.app.ContentPane;
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.Grid;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.SelectField;
import nextapp.echo2.app.TextField;

/**
 * JMS server general tab <code>ContentPane</code>.
 */
public class JmsServerGeneralPane extends ContentPane {

    private JmsServerWindow parent;

    private TextField nameField;
    private SelectField activeField;
    private SelectField blockerField;

    /**
     * Create a new <code>JMSServerGeneralTabPane</code>.
     *
     * @param parent the parent <code>JMSServerWindow</code>.
     */
    public JmsServerGeneralPane(JmsServerWindow parent) {
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

        // add name field
        Label nameLabel = new Label(Messages.getString("name"));
        nameLabel.setStyleName("grid.cell");
        layout.add(nameLabel);
        nameField = new TextField();
        nameField.setStyleName("default");
        nameField.setWidth(new Extent(100, Extent.PERCENT));
        layout.add(nameField);

        // add active field
        Label activeLabel = new Label(Messages.getString("active"));
        activeLabel.setStyleName("grid.cell");
        layout.add(activeLabel);
        activeField = new SelectField(MainScreen.LABELS);
        activeField.setSelectedIndex(0);
        activeField.setStyleName("default");
        activeField.setWidth(new Extent(10, Extent.EX));
        layout.add(activeField);

        // add blocker field
        Label blockerLabel = new Label(Messages.getString("blocker"));
        blockerLabel.setStyleName("grid.cell");
        layout.add(blockerLabel);
        blockerField = new SelectField(MainScreen.LABELS);
        blockerField.setSelectedIndex(0);
        blockerField.setStyleName("default");
        blockerField.setWidth(new Extent(10, Extent.EX));
        layout.add(blockerField);

        // update the pane
        update();
    }

    /**
     * Update the pane
     */
    public void update() {
        // update the name field
        nameField.setText(parent.getJMSServer().getName());
        // update the active field
        if (parent.getJMSServer().isActive()) {
            activeField.setSelectedIndex(0);
        } else {
            activeField.setSelectedIndex(1);
        }
        // update the blocker field
        if (parent.getJMSServer().isBlocker()) {
            blockerField.setSelectedIndex(0);
        } else {
            blockerField.setSelectedIndex(1);
        }
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

}