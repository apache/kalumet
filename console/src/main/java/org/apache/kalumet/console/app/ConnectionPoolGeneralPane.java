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
 * JDBC connection pool general tab <code>ContentPane</code>.
 */
public class ConnectionPoolGeneralPane extends ContentPane {

    // attributes
    private ConnectionPoolWindow parent;

    private TextField nameField;
    private SelectField activeField;
    private SelectField blockerField;

    /**
     * Create a new <code>JDBCConnectionPoolGeneralTabPane</code>.
     *
     * @param parent the parent <code>JDBCConnectionPoolWindow</code>.
     */
    public ConnectionPoolGeneralPane(ConnectionPoolWindow parent) {
        super();
        setStyleName("tab.content");

        // update parent
        this.parent = parent;

        // add the general grid layout
        Grid layout = new Grid(2);
        layout.setStyleName("default");
        layout.setColumnWidth(0, new Extent(20, Extent.PERCENT));
        layout.setColumnWidth(1, new Extent(80, Extent.PERCENT));
        add(layout);

        // add the name field
        Label nameLabel = new Label(Messages.getString("name"));
        nameLabel.setStyleName("grid.cell");
        layout.add(nameLabel);
        nameField = new TextField();
        nameField.setStyleName("default");
        nameField.setWidth(new Extent(100, Extent.PERCENT));
        layout.add(nameField);

        // add the active field
        Label jdbcConnectionPoolActiveLabel = new Label(Messages.getString("active"));
        jdbcConnectionPoolActiveLabel.setStyleName("grid.cell");
        layout.add(jdbcConnectionPoolActiveLabel);
        activeField = new SelectField(MainScreen.LABELS);
        activeField.setStyleName("default");
        activeField.setWidth(new Extent(10, Extent.EX));
        activeField.setSelectedIndex(0);
        layout.add(activeField);

        // add the blocker field
        Label jdbcConnectionPoolBlockerLabel = new Label(Messages.getString("blocker"));
        jdbcConnectionPoolBlockerLabel.setStyleName("grid.cell");
        layout.add(jdbcConnectionPoolBlockerLabel);
        blockerField = new SelectField(MainScreen.LABELS);
        blockerField.setStyleName("default");
        blockerField.setWidth(new Extent(10, Extent.EX));
        blockerField.setSelectedIndex(0);
        layout.add(blockerField);

        // update the pane
        update();
    }

    /**
     * Update the pane.
     */
    public void update() {
        // update the JDBC connection pool name field
        nameField.setText(parent.getConnectionPool().getName());
        // update the JDBC connection pool active field
        if (parent.getConnectionPool().isActive()) {
            activeField.setSelectedIndex(0);
        } else {
            activeField.setSelectedIndex(1);
        }
        // update the JDBC connection pool blocker field
        if (parent.getConnectionPool().isBlocker()) {
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