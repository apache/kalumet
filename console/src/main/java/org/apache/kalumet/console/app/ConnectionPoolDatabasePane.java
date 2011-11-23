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
import nextapp.echo2.app.PasswordField;
import nextapp.echo2.app.TextField;

/**
 * JDBC connection pool database tab <code>ContentPane</code>.
 */
public class ConnectionPoolDatabasePane extends ContentPane {

    // attributes
    private ConnectionPoolWindow parent;

    private TextField urlField;
    private TextField userField;
    private PasswordField passwordField;
    private PasswordField confirmPasswordField;

    /**
     * Create a new <code>JDBCConnectionPoolDatabaseTabPane</code>.<
     *
     * @param parent the parent <code>JDBCConnectionPoolWindow</code>.
     */
    public ConnectionPoolDatabasePane(ConnectionPoolWindow parent) {
        super();
        setStyleName("tab.content");

        // update parent
        this.parent = parent;

        // add the database layout grid
        Grid layout = new Grid(2);
        layout.setStyleName("default");
        layout.setColumnWidth(0, new Extent(20, Extent.PERCENT));
        layout.setColumnWidth(1, new Extent(80, Extent.PERCENT));
        add(layout);

        // add the URL field
        Label urlLabel = new Label(Messages.getString("jdbc"));
        urlLabel.setStyleName("grid.cell");
        layout.add(urlLabel);
        urlField = new TextField();
        urlField.setStyleName("default");
        urlField.setWidth(new Extent(100, Extent.PERCENT));
        layout.add(urlField);

        // add the user field
        Label userLabel = new Label(Messages.getString("user"));
        userLabel.setStyleName("grid.cell");
        layout.add(userLabel);
        userField = new TextField();
        userField.setStyleName("default");
        userField.setWidth(new Extent(100, Extent.PERCENT));
        layout.add(userField);

        // add the password field
        Label passwordLabel = new Label(Messages.getString("password"));
        passwordLabel.setStyleName("grid.cell");
        layout.add(passwordLabel);
        passwordField = new PasswordField();
        passwordField.setStyleName("default");
        passwordField.setWidth(new Extent(100, Extent.PERCENT));
        layout.add(passwordField);

        // add the confirm password field
        Label confirmPasswordLabel = new Label(Messages.getString("password.confirm"));
        confirmPasswordLabel.setStyleName("grid.cell");
        layout.add(confirmPasswordLabel);
        confirmPasswordField = new PasswordField();
        confirmPasswordField.setStyleName("default");
        confirmPasswordField.setWidth(new Extent(100, Extent.PERCENT));
        layout.add(confirmPasswordField);

        // update the pane
        update();
    }

    /**
     * Update the pane
     */
    public void update() {
        // update the JDBC connection pool URL field
        urlField.setText(parent.getConnectionPool().getUrl());
        // update the JDBC connection pool user field
        userField.setText(parent.getConnectionPool().getUser());
        // update the JDBC connection pool password field
        passwordField.setText(parent.getConnectionPool().getPassword());
        confirmPasswordField.setText(parent.getConnectionPool().getPassword());
    }

    public TextField getUrlField() {
        return this.urlField;
    }

    public TextField getUserField() {
        return this.userField;
    }

    public PasswordField getPasswordField() {
        return this.passwordField;
    }

    public PasswordField getConfirmPasswordField() {
        return this.confirmPasswordField;
    }

}