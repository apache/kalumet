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
import nextapp.echo2.app.Grid;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.SelectField;
import nextapp.echo2.app.SplitPane;
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.WindowPane;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import org.apache.kalumet.console.configuration.ConfigurationManager;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.model.Property;

/**
 * Admin configuration window.
 */
public class AdminConfigurationWindow extends WindowPane {

    private TextField consoleLocationField;
    private SelectField ldapAuthenticationField;
    private TextField ldapServerField;
    private TextField ldapBaseDNField;
    private TextField ldapUidAttributeField;
    private TextField ldapMailAttributeField;
    private TextField ldapCnAttributeField;

    // close
    private ActionListener close = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            AdminConfigurationWindow.this.userClose();
        }
    };
    // refresh
    private ActionListener refresh = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            update();
        }
    };
    // save
    private ActionListener save = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            String logEventAppender = consoleLocationField.getText();
            int ldapAuthentification = ldapAuthenticationField.getSelectedIndex();
            String ldapServer = ldapServerField.getText();
            String ldapBaseDN = ldapBaseDNField.getText();
            String ldapUidAttribute = ldapUidAttributeField.getText();
            String ldapMailAttribute = ldapMailAttributeField.getText();
            String ldapCnAttribute = ldapCnAttributeField.getText();

            // check if the user is allowed to do it
            if (!KalumetConsoleApplication.getApplication().getUserid().equals("admin")) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("configuration.restricted"));
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

            // log event appender
            Property logEventAppenderProperty = kalumet.getProperty("LogEventAppender");
            if (logEventAppenderProperty == null) {
                logEventAppenderProperty = new Property();
                logEventAppenderProperty.setName("LogEventAppender");
                try {
                    kalumet.addProperty(logEventAppenderProperty);
                } catch (Exception e) {
                    // ignore
                }
            }
            kalumet.getProperty("LogEventAppender").setValue(logEventAppender);
            // ldap authentification
            Property ldapAuthentificationProperty = kalumet.getProperty("LdapAuthentication");
            if (ldapAuthentificationProperty == null) {
                ldapAuthentificationProperty = new Property();
                ldapAuthentificationProperty.setName("LdapAuthentication");
                try {
                    kalumet.addProperty(ldapAuthentificationProperty);
                } catch (Exception e) {
                    // ignore
                }
            }
            if (ldapAuthentification == 0) {
                kalumet.getProperty("LdapAuthentication").setValue("true");
            } else {
                kalumet.getProperty("LdapAuthentication").setValue("false");
            }
            // ldap server
            Property ldapServerProperty = kalumet.getProperty("LdapServer");
            if (ldapServerProperty == null) {
                ldapServerProperty = new Property();
                ldapServerProperty.setName("LdapServer");
                try {
                    kalumet.addProperty(ldapServerProperty);
                } catch (Exception e) {
                    // ignore
                }
            }
            kalumet.getProperty("LdapServer").setValue(ldapServer);
            // ldap base DN
            Property ldapBaseDNProperty = kalumet.getProperty("LdapBaseDN");
            if (ldapBaseDNProperty == null) {
                ldapBaseDNProperty = new Property();
                ldapBaseDNProperty.setName("LdapBaseDN");
                try {
                    kalumet.addProperty(ldapBaseDNProperty);
                } catch (Exception e) {
                    // ignore
                }
            }
            kalumet.getProperty("LdapBaseDN").setValue(ldapBaseDN);
            // ldap uid attribute
            Property ldapUidAttributeProperty = kalumet.getProperty("LdapUidAttribute");
            if (ldapUidAttributeProperty == null) {
                ldapUidAttributeProperty = new Property();
                ldapUidAttributeProperty.setName("LdapUidAttribute");
                try {
                    kalumet.addProperty(ldapUidAttributeProperty);
                } catch (Exception e) {
                    // ignore
                }
            }
            kalumet.getProperty("LdapUidAttribute").setValue(ldapUidAttribute);
            // ldap mail attribute
            Property ldapMailAttributeProperty = kalumet.getProperty("LdapMailAttribute");
            if (ldapMailAttributeProperty == null) {
                ldapMailAttributeProperty = new Property();
                ldapMailAttributeProperty.setName("LdapMailAttribute");
                try {
                    kalumet.addProperty(ldapMailAttributeProperty);
                } catch (Exception e) {
                    // ignore
                }
            }
            kalumet.getProperty("LdapMailAttribute").setValue(ldapMailAttribute);
            // ldap cn attribute
            Property ldapCnAttributeProperty = kalumet.getProperty("LdapCnAttribute");
            if (ldapCnAttributeProperty == null) {
                ldapCnAttributeProperty = new Property();
                ldapCnAttributeProperty.setName("LdapCnAttribute");
                try {
                    kalumet.addProperty(ldapCnAttributeProperty);
                } catch (Exception e) {
                    // ignore
                }
            }

            // save Kalumet configuration
            try {
                ConfigurationManager.writeStore(kalumet);
            } catch (Exception e) {
                KalumetConsoleApplication.getApplication().getLogPane().addError(Messages.getString("db.write") + ": " + e.getMessage());
                return;
            }

            KalumetConsoleApplication.getApplication().getLogPane().addConfirm(Messages.getString("configuration.saved"));
        }

    };

    /**
     * Create a new <code>AdminConfigurationWindow</code>.
     */
    public AdminConfigurationWindow() {
        super();

        // check if the user that try to access this window is the admin
        if (!KalumetConsoleApplication.getApplication().getUserid().equals("admin")) {
            KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("configuration.restricted"));
            AdminConfigurationWindow.this.userClose();
            return;
        }

        setTitle(Messages.getString("configuration"));
        setIcon(Styles.COMPUTER_EDIT);
        setId("configurationwindow");
        setStyleName("configuration");
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
        refreshButton.addActionListener(refresh);
        refreshButton.setStyleName("control");
        controlRow.add(refreshButton);
        // add the save button
        Button saveButton = new Button(Messages.getString("save"), Styles.DATABASE_SAVE);
        saveButton.addActionListener(save);
        saveButton.setStyleName("control");
        controlRow.add(saveButton);
        // add the close button
        Button closeButton = new Button(Messages.getString("close"), Styles.CROSS);
        closeButton.addActionListener(close);
        closeButton.setStyleName("control");
        controlRow.add(closeButton);


        // define a grid layout
        Grid content = new Grid(2);
        content.setStyleName("default");
        content.setWidth(new Extent(100, Extent.PERCENT));
        content.setColumnWidth(0, new Extent(20, Extent.PERCENT));
        content.setColumnWidth(1, new Extent(80, Extent.PERCENT));
        splitPane.add(content);

        // create the log event appender field
        Label logEventAppenderLabel = new Label(Messages.getString("configuration.journal.location"));
        logEventAppenderLabel.setStyleName("default");
        content.add(logEventAppenderLabel);
        consoleLocationField = new TextField();
        consoleLocationField.setStyleName("default");
        consoleLocationField.setWidth(new Extent(100, Extent.PERCENT));
        content.add(consoleLocationField);

        // create the ldap authentication field
        Label ldapAuthenticationLabel = new Label(Messages.getString("configuration.ldap.authentication"));
        ldapAuthenticationLabel.setStyleName("default");
        content.add(ldapAuthenticationLabel);
        ldapAuthenticationField = new SelectField(MainScreen.LABELS);
        ldapAuthenticationField.setStyleName("default");
        content.add(ldapAuthenticationField);

        // create the ldap server field
        Label ldapServerLabel = new Label(Messages.getString("configuration.ldap.server"));
        ldapServerLabel.setStyleName("default");
        content.add(ldapServerLabel);
        ldapServerField = new TextField();
        ldapServerField.setStyleName("default");
        ldapServerField.setWidth(new Extent(100, Extent.PERCENT));
        content.add(ldapServerField);

        // create the ldap base dn field
        Label ldapBaseDNLabel = new Label(Messages.getString("configuration.ldap.basedn"));
        ldapBaseDNLabel.setStyleName("default");
        content.add(ldapBaseDNLabel);
        ldapBaseDNField = new TextField();
        ldapBaseDNField.setStyleName("default");
        ldapBaseDNField.setWidth(new Extent(100, Extent.PERCENT));
        content.add(ldapBaseDNField);

        // create the ldap uid attribute field
        Label ldapUidAttributeLabel = new Label(Messages.getString("configuration.ldap.uid"));
        ldapUidAttributeLabel.setStyleName("default");
        content.add(ldapUidAttributeLabel);
        ldapUidAttributeField = new TextField();
        ldapUidAttributeField.setStyleName("default");
        ldapUidAttributeField.setWidth(new Extent(100, Extent.PERCENT));
        content.add(ldapUidAttributeField);

        // create the ldap mail attribute field
        Label ldapMailAttributeLabel = new Label(Messages.getString("configuration.ldap.mail"));
        ldapMailAttributeLabel.setStyleName("default");
        content.add(ldapMailAttributeLabel);
        ldapMailAttributeField = new TextField();
        ldapMailAttributeField.setStyleName("default");
        ldapMailAttributeField.setWidth(new Extent(100, Extent.PERCENT));
        content.add(ldapMailAttributeField);

        // create the ldap cn attribute field
        Label ldapCnAttributeLabel = new Label(Messages.getString("configuration.ldap.cn"));
        ldapCnAttributeLabel.setStyleName("default");
        content.add(ldapCnAttributeLabel);
        ldapCnAttributeField = new TextField();
        ldapCnAttributeField.setStyleName("default");
        ldapCnAttributeField.setWidth(new Extent(100, Extent.PERCENT));
        content.add(ldapCnAttributeField);

        update();
    }

    /**
     * Update the window content.
     */
    public void update() {
        // load Kalumet configuration
        Kalumet kalumet = null;
        try {
            kalumet = ConfigurationManager.loadStore();
        } catch (Exception e) {
            KalumetConsoleApplication.getApplication().getLogPane().addError(Messages.getString("db.read") + ": " + e.getMessage());
            return;
        }

        Property logEventAppenderProperty = kalumet.getProperty("LogEventAppender");
        if (logEventAppenderProperty != null) {
            consoleLocationField.setText(logEventAppenderProperty.getValue());
        } else {
            consoleLocationField.setText(null);
        }

        Property ldapAuthentificationProperty = kalumet.getProperty("LdapAuthentication");
        if (ldapAuthentificationProperty != null) {
            if (ldapAuthentificationProperty.getValue().equals("true")) {
                ldapAuthenticationField.setSelectedIndex(0);
            } else {
                ldapAuthenticationField.setSelectedIndex(1);
            }
        } else {
            ldapAuthenticationField.setSelectedIndex(1);
        }
        Property ldapServerProperty = kalumet.getProperty("LdapServer");
        if (ldapServerProperty != null) {
            ldapServerField.setText(ldapServerProperty.getValue());
        } else {
            ldapServerField.setText(null);
        }
        Property ldapBaseDNProperty = kalumet.getProperty("LdapBaseDN");
        if (ldapBaseDNProperty != null) {
            ldapBaseDNField.setText(ldapBaseDNProperty.getValue());
        } else {
            ldapBaseDNField.setText(null);
        }
        Property ldapUidAttributeProperty = kalumet.getProperty("LdapUidAttribute");
        if (ldapUidAttributeProperty != null) {
            ldapUidAttributeField.setText(ldapUidAttributeProperty.getValue());
        } else {
            ldapUidAttributeField.setText(null);
        }
        Property ldapMailAttributeProperty = kalumet.getProperty("LdapMailAttribute");
        if (ldapMailAttributeProperty != null) {
            ldapMailAttributeField.setText(ldapMailAttributeProperty.getValue());
        } else {
            ldapMailAttributeField.setText(null);
        }
        Property ldapCnAttributeProperty = kalumet.getProperty("LdapCnAttribute");
        if (ldapCnAttributeProperty != null) {
            ldapCnAttributeField.setText(ldapCnAttributeProperty.getValue());
        } else {
            ldapCnAttributeField.setText(null);
        }
    }

}
