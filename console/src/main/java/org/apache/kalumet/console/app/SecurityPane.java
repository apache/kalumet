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
import nextapp.echo2.app.list.DefaultListModel;
import org.apache.kalumet.console.configuration.ConfigurationManager;
import org.apache.kalumet.model.Access;
import org.apache.kalumet.model.Group;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.model.Property;

/**
 * Environment security pane.
 */
public class SecurityPane extends ContentPane {

    private EnvironmentWindow parent;
    private Grid accessesGrid;
    private SelectField groupField;

    // create
    private ActionListener create = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the lock
            if (!parent.getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), parent.getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!parent.adminPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), parent.getEnvironmentName());
                return;
            }
            // get the group id
            String groupId = (String) groupField.getSelectedItem();
            if (groupId == null || groupId.trim().length() < 1) {
                return;
            }
            // create the access
            Access access = new Access();
            access.setGroup(groupId);
            // admin
            Property property = new Property();
            property.setName("admin");
            property.setValue("false");
            access.getProperties().add(property);
            // update
            property = new Property();
            property.setName("update");
            property.setValue("false");
            access.getProperties().add(property);
            // JEE servers
            property = new Property();
            property.setName("jee_servers");
            property.setValue("false");
            access.getProperties().add(property);
            // JEE servers update
            property = new Property();
            property.setName("jee_servers_update");
            property.setValue("false");
            access.getProperties().add(property);
            // JEE servers control
            property = new Property();
            property.setName("jee_servers_control");
            property.setValue("false");
            access.getProperties().add(property);
            // JEE resources
            property = new Property();
            property.setName("jee_resources");
            property.setValue("false");
            access.getProperties().add(property);
            // JEE resources update
            property = new Property();
            property.setName("jee_resources_update");
            property.setValue("false");
            access.getProperties().add(property);
            // JEE applications
            property = new Property();
            property.setName("jee_applications");
            property.setValue("false");
            access.getProperties().add(property);
            // JEE applications update
            property = new Property();
            property.setName("jee_applications_update");
            property.setValue("false");
            access.getProperties().add(property);
            // softwares
            property = new Property();
            property.setName("softwares");
            property.setValue("false");
            access.getProperties().add(property);
            // softwares update
            property = new Property();
            property.setName("softwares_update");
            property.setValue("false");
            access.getProperties().add(property);
            // release
            property = new Property();
            property.setName("release");
            property.setValue("false");
            access.getProperties().add(property);
            // shell
            property = new Property();
            property.setName("shell");
            property.setValue("false");
            access.getProperties().add(property);
            // browser
            property = new Property();
            property.setName("browser");
            property.setValue("false");
            access.getProperties().add(property);
            // homepage
            property = new Property();
            property.setName("homepage");
            property.setValue("false");
            access.getProperties().add(property);
            // add the access
            try {
                parent.getEnvironment().addAccess(access);
            } catch (Exception e) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("access.exists"), parent.getEnvironmentName());
                return;
            }
            // add a change event
            parent.getChangeEvents().add("Add ACLs for group " + groupId + ".");
            // change the update flag
            parent.setUpdated(true);
            // update the journal
            parent.updateJournalPane();
            // update the pane
            update();
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
            if (!parent.adminPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), parent.getEnvironmentName());
                return;
            }
            String groupId = event.getActionCommand();
            // looking for the access object
            final Access access = parent.getEnvironment().getAccess(groupId);
            if (access == null) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("access.notfound"));
                return;
            }
            // display confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new ConfirmWindow(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    parent.getEnvironment().getAccesses().remove(access);
                    // add a change event
                    parent.getChangeEvents().add("Delete ACLs for group " + access.getGroup() + ".");
                    // change the updated flag
                    parent.setUpdated(true);
                    // update the journal log tab pane
                    parent.updateJournalPane();
                    // update only the pane
                    update();
                }
            }));
        }
    };
    // switch admin
    private ActionListener switchAdmin = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the environment lock
            if (!parent.getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), parent.getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!parent.adminPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), parent.getEnvironmentName());
                return;
            }
            // get the group access
            Access access = parent.getEnvironment().getAccess(event.getActionCommand());
            if (access == null) {
                return;
            }
            // switch admin ACL
            if (access.getProperty("admin").getValue().equals("true")) {
                access.getProperty("admin").setValue("false");
            } else {
                access.getProperty("admin").setValue("true");
            }
            // change the update flag
            parent.setUpdated(true);
            // update the panel
            update();
        }
    };
    // switch update
    private ActionListener switchUpdate = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the environment lock
            if (!parent.getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), parent.getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!parent.adminPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), parent.getEnvironmentName());
                return;
            }
            // get the group access
            Access access = parent.getEnvironment().getAccess(event.getActionCommand());
            if (access == null) {
                return;
            }
            // switch admin ACL
            if (access.getProperty("update").getValue().equals("true")) {
                access.getProperty("update").setValue("false");
            } else {
                access.getProperty("update").setValue("true");
            }
            // change the update flag
            parent.setUpdated(true);
            // update the panel
            update();
        }
    };
    // switch J2EE application servers
    private ActionListener switchJeeServers = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the environment lock
            if (!parent.getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), parent.getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!parent.adminPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), parent.getEnvironmentName());
                return;
            }
            // get the group access
            Access access = parent.getEnvironment().getAccess(event.getActionCommand());
            if (access == null) {
                return;
            }
            // switch admin ACL
            if (access.getProperty("jee_servers").getValue().equals("true")) {
                access.getProperty("jee_servers").setValue("false");
            } else {
                access.getProperty("jee_servers").setValue("true");
            }
            // change the update flag
            parent.setUpdated(true);
            // update the panel
            update();
        }
    };
    // switch JEE servers update
    private ActionListener switchJeeServersUpdate = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the environment lock
            if (!parent.getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), parent.getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!parent.adminPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), parent.getEnvironmentName());
                return;
            }
            // get the group access
            Access access = parent.getEnvironment().getAccess(event.getActionCommand());
            if (access == null) {
                return;
            }
            // switch admin ACL
            if (access.getProperty("jee_servers_update").getValue().equals("true")) {
                access.getProperty("jee_servers_update").setValue("false");
            } else {
                access.getProperty("jee_servers_update").setValue("true");
            }
            // change the update flag
            parent.setUpdated(true);
            // update the panel
            update();
        }
    };
    // switch JEE servers control
    private ActionListener switchJeeServersControl = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the environment lock
            if (!parent.getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), parent.getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!parent.adminPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), parent.getEnvironmentName());
                return;
            }
            // get the group access
            Access access = parent.getEnvironment().getAccess(event.getActionCommand());
            if (access == null) {
                return;
            }
            // switch admin ACL
            if (access.getProperty("jee_servers_control").getValue().equals("true")) {
                access.getProperty("jee_servers_control").setValue("false");
            } else {
                access.getProperty("jee_servers_control").setValue("true");
            }
            // change the update flag
            parent.setUpdated(true);
            // update the panel
            update();
        }
    };
    // switch JEE resources
    private ActionListener switchJeeResources = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the environment lock
            if (!parent.getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), parent.getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!parent.adminPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), parent.getEnvironmentName());
                return;
            }
            // get the group access
            Access access = parent.getEnvironment().getAccess(event.getActionCommand());
            if (access == null) {
                return;
            }
            // switch admin ACL
            if (access.getProperty("jee_resources").getValue().equals("true")) {
                access.getProperty("jee_resources").setValue("false");
            } else {
                access.getProperty("jee_resources").setValue("true");
            }
            // change the update flag
            parent.setUpdated(true);
            // update the panel
            update();
        }
    };
    // switch J2EE resources update
    private ActionListener switchJeeResourcesUpdate = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the environment lock
            if (!parent.getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), parent.getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!parent.adminPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), parent.getEnvironmentName());
                return;
            }
            // get the group access
            Access access = parent.getEnvironment().getAccess(event.getActionCommand());
            if (access == null) {
                return;
            }
            // switch admin ACL
            if (access.getProperty("jee_resources_update").getValue().equals("true")) {
                access.getProperty("jee_resources_update").setValue("false");
            } else {
                access.getProperty("jee_resources_update").setValue("true");
            }
            // change the update flag
            parent.setUpdated(true);
            // update the panel
            update();
        }
    };
    // switch J2EE applications
    private ActionListener switchJeeApplications = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the environment lock
            if (!parent.getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), parent.getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!parent.adminPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), parent.getEnvironmentName());
                return;
            }
            // get the group access
            Access access = parent.getEnvironment().getAccess(event.getActionCommand());
            if (access == null) {
                return;
            }
            // switch admin ACL
            if (access.getProperty("jee_applications").getValue().equals("true")) {
                access.getProperty("jee_applications").setValue("false");
            } else {
                access.getProperty("jee_applications").setValue("true");
            }
            // change the update flag
            parent.setUpdated(true);
            // update the panel
            update();
        }
    };
    // switch J2EE applications update
    private ActionListener switchJeeApplicationsUpdate = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the environment lock
            if (!parent.getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), parent.getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!parent.adminPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), parent.getEnvironmentName());
                return;
            }
            // get the group access
            Access access = parent.getEnvironment().getAccess(event.getActionCommand());
            if (access == null) {
                return;
            }
            // switch admin ACL
            if (access.getProperty("jee_applications_update").getValue().equals("true")) {
                access.getProperty("jee_applications_update").setValue("false");
            } else {
                access.getProperty("jee_applications_update").setValue("true");
            }
            // change the update flag
            parent.setUpdated(true);
            // update the panel
            update();
        }
    };
    // switch softwares
    private ActionListener switchSoftwares = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the environment lock
            if (!parent.getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), parent.getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!parent.adminPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), parent.getEnvironmentName());
                return;
            }
            // get the group access
            Access access = parent.getEnvironment().getAccess(event.getActionCommand());
            if (access == null) {
                return;
            }
            // switch admin ACL
            if (access.getProperty("softwares").getValue().equals("true")) {
                access.getProperty("softwares").setValue("false");
            } else {
                access.getProperty("softwares").setValue("true");
            }
            // change the update flag
            parent.setUpdated(true);
            // update the panel
            update();
        }
    };
    // switch softwares update
    private ActionListener switchSoftwaresUpdate = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the environment lock
            if (!parent.getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), parent.getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!parent.adminPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), parent.getEnvironmentName());
                return;
            }
            // get the group access
            Access access = parent.getEnvironment().getAccess(event.getActionCommand());
            if (access == null) {
                return;
            }
            // switch admin ACL
            if (access.getProperty("softwares_update").getValue().equals("true")) {
                access.getProperty("softwares_update").setValue("false");
            } else {
                access.getProperty("softwares_update").setValue("true");
            }
            // change the update flag
            parent.setUpdated(true);
            // update the panel
            update();
        }
    };
    // switch release
    private ActionListener switchRelease = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the environment lock
            if (!parent.getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), parent.getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!parent.adminPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), parent.getEnvironmentName());
                return;
            }
            // get the group access
            Access access = parent.getEnvironment().getAccess(event.getActionCommand());
            if (access == null) {
                return;
            }
            // switch admin ACL
            if (access.getProperty("release").getValue().equals("true")) {
                access.getProperty("release").setValue("false");
            } else {
                access.getProperty("release").setValue("true");
            }
            // change the update flag
            parent.setUpdated(true);
            // update the panel
            update();
        }
    };
    // switch shell
    private ActionListener switchShell = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the environment lock
            if (!parent.getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), parent.getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!parent.adminPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), parent.getEnvironmentName());
                return;
            }
            // get the group access
            Access access = parent.getEnvironment().getAccess(event.getActionCommand());
            if (access == null) {
                return;
            }
            // switch admin ACL
            if (access.getProperty("shell").getValue().equals("true")) {
                access.getProperty("shell").setValue("false");
            } else {
                access.getProperty("shell").setValue("true");
            }
            // change the update flag
            parent.setUpdated(true);
            // update the panel
            update();
        }
    };
    // switch browser
    private ActionListener switchBrowser = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the environment lock
            if (!parent.getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), parent.getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!parent.adminPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), parent.getEnvironmentName());
                return;
            }
            // get the group access
            Access access = parent.getEnvironment().getAccess(event.getActionCommand());
            if (access == null) {
                return;
            }
            // switch admin ACL
            if (access.getProperty("browser").getValue().equals("true")) {
                access.getProperty("browser").setValue("false");
            } else {
                access.getProperty("browser").setValue("true");
            }
            // change the update flag
            parent.setUpdated(true);
            // update the panel
            update();
        }
    };
    // switch homepage
    private ActionListener switchHomepage = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the environment lock
            if (!parent.getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), parent.getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!parent.adminPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), parent.getEnvironmentName());
                return;
            }
            // get the group access
            Access access = parent.getEnvironment().getAccess(event.getActionCommand());
            if (access == null) {
                return;
            }
            // switch admin ACL
            if (access.getProperty("homepage").getValue().equals("true")) {
                access.getProperty("homepage").setValue("false");
            } else {
                access.getProperty("homepage").setValue("true");
            }
            // change the update flag
            parent.setUpdated(true);
            // update the panel
            update();
        }
    };

    /**
     * Create a new <code>SecurityPane</code>.
     *
     * @param parent the parent <code>EnvironmentWindow</code>.
     */
    public SecurityPane(EnvironmentWindow parent) {
        super();
        setStyleName("tab.content");

        // update parent
        this.parent = parent;

        // column layout
        Column columnLayout = new Column();
        columnLayout.setStyleName("default");
        columnLayout.setCellSpacing(new Extent(2));
        columnLayout.setInsets(new Insets(2));
        add(columnLayout);

        if (parent.adminPermission) {
            // add group
            Row groupRow = new Row();
            columnLayout.add(groupRow);
            // add the select field
            Kalumet kalumet;
            try {
                kalumet = ConfigurationManager.loadStore();
            } catch (Exception e) {
                KalumetConsoleApplication.getApplication().getLogPane().addError(Messages.getString("kalumet.read"), parent.getEnvironmentName());
                return;
            }
            groupField = new SelectField();
            DefaultListModel model = (DefaultListModel) groupField.getModel();
            for (Iterator groupIterator = kalumet.getSecurity().getGroups().iterator(); groupIterator.hasNext(); ) {
                Group group = (Group) groupIterator.next();
                model.add(group.getId());
            }
            groupField.setStyleName("default");
            groupField.setWidth(new Extent(50, Extent.EX));
            groupRow.add(groupField);
            // add the add button
            Button addButton = new Button(Styles.ADD);
            addButton.setStyleName("default");
            addButton.addActionListener(create);
            groupRow.add(addButton);
        }

        // add accesses grid
        accessesGrid = new Grid(17);
        accessesGrid.setStyleName("border.grid");
        // actions
        accessesGrid.setColumnWidth(0, new Extent(50, Extent.PX));
        // group id
        // admin permission
        // update permission
        // JEE servers permission
        // JEE servers update permission
        // JEE servers control permission
        // JEE resources permission
        // JEE resources update permission
        // JEE applications permission
        // JEE applications update permission
        // softwares permission
        // softwares update permission
        // release permission
        // shell permission
        // browser permission
        // homepage permission
        columnLayout.add(accessesGrid);

        // update
        update();
    }

    /**
     * <p>
     * Update the pane.
     * </p>
     */
    public void update() {
        // update the accesses grid
        // remove all grid children
        accessesGrid.removeAll();
        // add grid headers
        Label actionHeader = new Label(" ");
        actionHeader.setStyleName("grid.header");
        accessesGrid.add(actionHeader);
        // group Id header
        Label accessGroupHeader = new Label(Messages.getString("group"));
        accessGroupHeader.setStyleName("grid.header");
        accessesGrid.add(accessGroupHeader);
        // admin permission header
        Label adminPermHeader = new Label("A");
        adminPermHeader.setToolTipText(Messages.getString("permission.admin"));
        adminPermHeader.setStyleName("grid.header");
        accessesGrid.add(adminPermHeader);
        // update permission header
        Label updatePermHeader = new Label("U");
        updatePermHeader.setToolTipText(Messages.getString("permission.update"));
        updatePermHeader.setStyleName("grid.header");
        accessesGrid.add(updatePermHeader);
        // JEE servers permission
        Label jeeServersPermHeader = new Label("JS");
        jeeServersPermHeader.setToolTipText(Messages.getString("permission.jeeservers"));
        jeeServersPermHeader.setStyleName("grid.header");
        accessesGrid.add(jeeServersPermHeader);
        // JEE servers update permission
        Label jeeServersUpdateHeader = new Label("JSU");
        jeeServersUpdateHeader.setToolTipText(Messages.getString("permission.jeeserversupdate"));
        jeeServersUpdateHeader.setStyleName("grid.header");
        accessesGrid.add(jeeServersUpdateHeader);
        // JEE servers control permission
        Label jeeServersControlHeader = new Label("JSC");
        jeeServersControlHeader.setToolTipText(Messages.getString("permission.jeeserverscontrol"));
        jeeServersControlHeader.setStyleName("grid.header");
        accessesGrid.add(jeeServersControlHeader);
        // JEE resources permission
        Label jeeResourcesHeader = new Label("JR");
        jeeResourcesHeader.setToolTipText(Messages.getString("permission.jeeresources"));
        jeeResourcesHeader.setStyleName("grid.header");
        accessesGrid.add(jeeResourcesHeader);
        // JEE resources update permission
        Label jeeResourcesUpdateHeader = new Label("JRU");
        jeeResourcesUpdateHeader.setToolTipText(Messages.getString("permission.jeeresourcesupdate"));
        jeeResourcesUpdateHeader.setStyleName("grid.header");
        accessesGrid.add(jeeResourcesUpdateHeader);
        // JEE applications permission
        Label jeeApplicationsHeader = new Label("JA");
        jeeApplicationsHeader.setToolTipText(Messages.getString("permission.jeeapplications"));
        jeeApplicationsHeader.setStyleName("grid.header");
        accessesGrid.add(jeeApplicationsHeader);
        // JEE applications update permission
        Label jeeApplicationsUpdateHeader = new Label("JAU");
        jeeApplicationsUpdateHeader.setToolTipText(Messages.getString("permission.jeeapplicationsupdate"));
        jeeApplicationsUpdateHeader.setStyleName("grid.header");
        accessesGrid.add(jeeApplicationsUpdateHeader);
        // softwares permission
        Label softwaresHeader = new Label("S");
        softwaresHeader.setToolTipText(Messages.getString("permission.softwares"));
        softwaresHeader.setStyleName("grid.header");
        accessesGrid.add(softwaresHeader);
        // softwares update permission
        Label softwaresUpdateHeader = new Label("SU");
        softwaresUpdateHeader.setToolTipText(Messages.getString("permission.softwaresupdate"));
        softwaresUpdateHeader.setStyleName("grid.header");
        accessesGrid.add(softwaresUpdateHeader);
        // release permission
        Label releaseHeader = new Label("Re");
        releaseHeader.setToolTipText(Messages.getString("permission.release"));
        releaseHeader.setStyleName("grid.header");
        accessesGrid.add(releaseHeader);
        // shell permission
        Label shellHeader = new Label("Sh");
        shellHeader.setToolTipText(Messages.getString("permission.shell"));
        shellHeader.setStyleName("grid.header");
        accessesGrid.add(shellHeader);
        // browser permission
        Label browserHeader = new Label("Br");
        browserHeader.setToolTipText(Messages.getString("permission.browser"));
        browserHeader.setStyleName("grid.header");
        accessesGrid.add(browserHeader);
        // homepage permission
        Label homepageHeader = new Label("Ho");
        homepageHeader.setToolTipText(Messages.getString("permission.homepage"));
        homepageHeader.setStyleName("grid.header");
        accessesGrid.add(homepageHeader);
        // load Kalumet configuration
        Kalumet kalumet = null;
        try {
            kalumet = ConfigurationManager.loadStore();
        } catch (Exception e) {
            KalumetConsoleApplication.getApplication().getLogPane().addError(Messages.getString("db.read") + ": " + e.getMessage());
            return;
        }
        for (Iterator accessIterator = parent.getEnvironment().getAccesses().iterator(); accessIterator.hasNext(); ) {
            Access access = (Access) accessIterator.next();
            // access group and action
            Row row = new Row();
            row.setInsets(new Insets(2));
            row.setCellSpacing(new Extent(2));
            accessesGrid.add(row);
            if (parent.adminPermission) {
                // delete
                Button deleteButton = new Button(Styles.DELETE);
                deleteButton.setToolTipText(Messages.getString("delete"));
                deleteButton.setActionCommand(access.getGroup());
                deleteButton.addActionListener(delete);
                row.add(deleteButton);
            }
            // group
            Label group = new Label(access.getGroup());
            group.setStyleName("default");
            accessesGrid.add(group);
            // admin permission
            Button adminButton;
            if (access.getProperty("admin").getValue().equals("true")) {
                adminButton = new Button(Styles.ACCEPT);
            } else {
                adminButton = new Button(Styles.DELETE);
            }
            adminButton.setStyleName("default");
            if (parent.adminPermission) {
                adminButton.setActionCommand(access.getGroup());
                adminButton.addActionListener(switchAdmin);
            }
            accessesGrid.add(adminButton);
            // update permission
            Button updateButton;
            if (access.getProperty("update").getValue().equals("true")) {
                updateButton = new Button(Styles.ACCEPT);
            } else {
                updateButton = new Button(Styles.DELETE);
            }
            updateButton.setStyleName("default");
            if (parent.adminPermission) {
                updateButton.setActionCommand(access.getGroup());
                updateButton.addActionListener(switchUpdate);
            }
            accessesGrid.add(updateButton);
            // JEE servers permission
            Button jeeServersButton;
            if (access.getProperty("jee_servers").getValue().equals("true")) {
                jeeServersButton = new Button(Styles.ACCEPT);
            } else {
                jeeServersButton = new Button(Styles.DELETE);
            }
            jeeServersButton.setStyleName("default");
            if (parent.adminPermission) {
                jeeServersButton.setActionCommand(access.getGroup());
                jeeServersButton.addActionListener(switchJeeServers);
            }
            accessesGrid.add(jeeServersButton);
            // JEE servers update permission
            Button jeeServersUpdateButton;
            if (access.getProperty("jee_servers_update").getValue().equals("true")) {
                jeeServersUpdateButton = new Button(Styles.ACCEPT);
            } else {
                jeeServersUpdateButton = new Button(Styles.DELETE);
            }
            jeeServersUpdateButton.setStyleName("default");
            if (parent.adminPermission) {
                jeeServersUpdateButton.setActionCommand(access.getGroup());
                jeeServersUpdateButton.addActionListener(switchJeeServersUpdate);
            }
            accessesGrid.add(jeeServersUpdateButton);
            // JEE servers control permission
            Button jeeServersControlButton;
            if (access.getProperty("jee_servers_control").getValue().equals("true")) {
                jeeServersControlButton = new Button(Styles.ACCEPT);
            } else {
                jeeServersControlButton = new Button(Styles.DELETE);
            }
            jeeServersControlButton.setStyleName("default");
            if (parent.adminPermission) {
                jeeServersControlButton.setActionCommand(access.getGroup());
                jeeServersControlButton.addActionListener(switchJeeServersControl);
            }
            accessesGrid.add(jeeServersControlButton);
            // JEE resources permission
            Button jeeResourcesButton;
            if (access.getProperty("jee_resources").getValue().equals("true")) {
                jeeResourcesButton = new Button(Styles.ACCEPT);
            } else {
                jeeResourcesButton = new Button(Styles.DELETE);
            }
            jeeResourcesButton.setStyleName("default");
            if (parent.adminPermission) {
                jeeResourcesButton.setActionCommand(access.getGroup());
                jeeResourcesButton.addActionListener(switchJeeResources);
            }
            accessesGrid.add(jeeResourcesButton);
            // JEE resources update permission
            Button jeeResourcesUpdateButton;
            if (access.getProperty("jee_resources_update").getValue().equals("true")) {
                jeeResourcesUpdateButton = new Button(Styles.ACCEPT);
            } else {
                jeeResourcesUpdateButton = new Button(Styles.DELETE);
            }
            jeeResourcesUpdateButton.setStyleName("default");
            if (parent.adminPermission) {
                jeeResourcesUpdateButton.setActionCommand(access.getGroup());
                jeeResourcesUpdateButton.addActionListener(switchJeeResourcesUpdate);
            }
            accessesGrid.add(jeeResourcesUpdateButton);
            // JEE applications permission
            Button jeeApplicationsButton;
            if (access.getProperty("jee_applications").getValue().equals("true")) {
                jeeApplicationsButton = new Button(Styles.ACCEPT);
            } else {
                jeeApplicationsButton = new Button(Styles.DELETE);
            }
            jeeApplicationsButton.setStyleName("default");
            if (parent.adminPermission) {
                jeeApplicationsButton.setActionCommand(access.getGroup());
                jeeApplicationsButton.addActionListener(switchJeeApplications);
            }
            accessesGrid.add(jeeApplicationsButton);
            // JEE applications update permission
            Button jeeApplicationsUpdateButton;
            if (access.getProperty("jee_applications_update").getValue().equals("true")) {
                jeeApplicationsUpdateButton = new Button(Styles.ACCEPT);
            } else {
                jeeApplicationsUpdateButton = new Button(Styles.DELETE);
            }
            jeeApplicationsUpdateButton.setStyleName("default");
            if (parent.adminPermission) {
                jeeApplicationsUpdateButton.setActionCommand(access.getGroup());
                jeeApplicationsUpdateButton.addActionListener(switchJeeApplicationsUpdate);
            }
            accessesGrid.add(jeeApplicationsUpdateButton);
            // softwares permission
            Button softwaresButton;
            if (access.getProperty("softwares").getValue().equals("true")) {
                softwaresButton = new Button(Styles.ACCEPT);
            } else {
                softwaresButton = new Button(Styles.DELETE);
            }
            softwaresButton.setStyleName("default");
            if (parent.adminPermission) {
                softwaresButton.setActionCommand(access.getGroup());
                softwaresButton.addActionListener(switchSoftwares);
            }
            accessesGrid.add(softwaresButton);
            // softwares update permission
            Button softwaresUpdateButton;
            if (access.getProperty("softwares_update").getValue().equals("true")) {
                softwaresUpdateButton = new Button(Styles.ACCEPT);
            } else {
                softwaresUpdateButton = new Button(Styles.DELETE);
            }
            softwaresUpdateButton.setStyleName("default");
            if (parent.adminPermission) {
                softwaresUpdateButton.setActionCommand(access.getGroup());
                softwaresUpdateButton.addActionListener(switchSoftwaresUpdate);
            }
            accessesGrid.add(softwaresUpdateButton);
            // release permission
            Button releaseButton;
            if (access.getProperty("release").getValue().equals("true")) {
                releaseButton = new Button(Styles.ACCEPT);
            } else {
                releaseButton = new Button(Styles.DELETE);
            }
            releaseButton.setStyleName("default");
            if (parent.adminPermission) {
                releaseButton.setActionCommand(access.getGroup());
                releaseButton.addActionListener(switchRelease);
            }
            accessesGrid.add(releaseButton);
            // shell permission
            Button shellButton;
            if (access.getProperty("shell").getValue().equals("true")) {
                shellButton = new Button(Styles.ACCEPT);
            } else {
                shellButton = new Button(Styles.DELETE);
            }
            shellButton.setStyleName("default");
            if (parent.adminPermission) {
                shellButton.setActionCommand(access.getGroup());
                shellButton.addActionListener(switchShell);
            }
            accessesGrid.add(shellButton);
            // browser permission
            Button browserButton;
            if (access.getProperty("browser").getValue().equals("true")) {
                browserButton = new Button(Styles.ACCEPT);
            } else {
                browserButton = new Button(Styles.DELETE);
            }
            browserButton.setStyleName("default");
            if (parent.adminPermission) {
                browserButton.setActionCommand(access.getGroup());
                browserButton.addActionListener(switchBrowser);
            }
            accessesGrid.add(browserButton);
            // homepage permission
            Button homepageButton;
            if (access.getProperty("homepage").getValue().equals("true")) {
                homepageButton = new Button(Styles.ACCEPT);
            } else {
                homepageButton = new Button(Styles.DELETE);
            }
            homepageButton.setStyleName("default");
            if (parent.adminPermission) {
                homepageButton.setActionCommand(access.getGroup());
                homepageButton.addActionListener(switchHomepage);
            }
            accessesGrid.add(homepageButton);
        }
    }

}
