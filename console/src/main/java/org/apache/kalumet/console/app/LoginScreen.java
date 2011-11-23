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
import nextapp.echo2.app.ContentPane;
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.Grid;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.PasswordField;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.SplitPane;
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.WindowPane;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;

/**
 * Login screen <code>ContentPane</code>.
 */
public class LoginScreen extends ContentPane {

    private TextField usernameField;
    private PasswordField passwordField;

    // the menu listener
    private ActionListener aboutButtonActionListener = new ActionListener() {

        public void actionPerformed(ActionEvent event) {
            if (KalumetConsoleApplication.getApplication().getDefaultWindow().getComponent("about") == null) {
                KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new AboutWindow());
            }
        }

    };

    /**
     * Create a new <code>LoginScreen</code>.
     */
    public LoginScreen() {
        super();

        // define a title pane
        SplitPane titlePane = new SplitPane(SplitPane.ORIENTATION_VERTICAL_TOP_BOTTOM, new Extent(30, Extent.PX));
        titlePane.setResizable(false);
        add(titlePane);
        Label titleLabel = new Label(Messages.getString("kalumet.console"));
        titleLabel.setStyleName("title");
        titlePane.add(titleLabel);

        // create a menu pane
        SplitPane menuPane = new SplitPane(SplitPane.ORIENTATION_VERTICAL, new Extent(26));
        menuPane.setResizable(false);
        titlePane.add(menuPane);

        // create the menu row
        Row menuRow = new Row();
        menuRow.setStyleName("menu");
        menuPane.add(menuRow);
        Button aboutButton = new Button(Messages.getString("about"), Styles.INFORMATION);
        aboutButton.setStyleName("default");
        aboutButton.addActionListener(aboutButtonActionListener);
        menuRow.add(aboutButton);

        // create the split central pane
        SplitPane mainPane = new SplitPane(SplitPane.ORIENTATION_HORIZONTAL_LEFT_RIGHT, new Extent(200, Extent.PX));
        menuPane.add(mainPane);
        ContentPane leftPane = new ContentPane();
        leftPane.setStyleName("left");
        mainPane.add(leftPane);
        ContentPane centralPane = new ContentPane();
        centralPane.setStyleName("central");
        mainPane.add(centralPane);

        // create a new window for login fields
        WindowPane loginWindow = new WindowPane();
        loginWindow.setStyleName("login");
        loginWindow.setTitle(Messages.getString("login"));
        loginWindow.setIcon(Styles.USER);
        loginWindow.setDefaultCloseOperation(WindowPane.DO_NOTHING_ON_CLOSE);
        add(loginWindow);

        SplitPane splitPane = new SplitPane(SplitPane.ORIENTATION_VERTICAL_BOTTOM_TOP, new Extent(32));
        loginWindow.add(splitPane);

        Row controlRow = new Row();
        controlRow.setStyleName("control");
        splitPane.add(controlRow);

        Button button = new Button(Messages.getString("continue"), Styles.ACCEPT);
        button.setStyleName("control");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                processLogin();
            }
        });
        controlRow.add(button);

        Grid layoutGrid = new Grid();
        layoutGrid.setStyleName("login");
        splitPane.add(layoutGrid);

        Label label = new Label(Messages.getString("username"));
        label.setStyleName("default");
        layoutGrid.add(label);

        usernameField = new TextField();
        usernameField.setStyleName("default");
        usernameField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                KalumetConsoleApplication.getApplication().setFocusedComponent(passwordField);
            }
        });
        layoutGrid.add(usernameField);

        label = new Label(Messages.getString("password"));
        label.setStyleName("default");
        layoutGrid.add(label);

        passwordField = new PasswordField();
        passwordField.setStyleName("default");
        layoutGrid.add(passwordField);
        passwordField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                processLogin();
            }
        });

        KalumetConsoleApplication.getApplication().setFocusedComponent(usernameField);
    }

    /**
     * Process a user log-in request
     */
    private void processLogin() {

        String userid = usernameField.getText();
        String password = passwordField.getText();

        usernameField.setText("");
        passwordField.setText("");
        KalumetConsoleApplication.getApplication().setFocusedComponent(usernameField);

        KalumetConsoleApplication.getApplication().connect(userid, password);
    }

}