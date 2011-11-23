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
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.app.list.DefaultListModel;
import org.apache.kalumet.model.Destination;
import org.apache.kalumet.model.Email;

/**
 * Environment notifiers pane.
 */
public class NotifiersPane extends ContentPane {

    private EnvironmentWindow parent;
    private TextField countDownField;
    private SelectField scopeField;
    private Grid grid;
    private TextField newDestinationField;

    // scope select
    private ActionListener scopeSelect = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            update();
        }
    };
    // edit destination
    private ActionListener editDestination = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the environment lock
            if (!getEnvironmentWindow().getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!getEnvironmentWindow().adminPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // get the destination address
            String destinationAddress = event.getActionCommand();
            // get field
            TextField destinationAddressField = (TextField) NotifiersPane.this.getComponent("notifierdestination_" + parent.getEnvironmentName() + "_" + (String) scopeField.getSelectedItem() + "_" + destinationAddress);
            String destinationAddressFieldValue = destinationAddressField.getText();
            // check the field value
            if (destinationAddressFieldValue == null || destinationAddressFieldValue.trim().length() < 1) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("notifier.destination.mandatory"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // if the user change the destination address, check if the address is
            // not already in use
            if (!destinationAddress.equals(destinationAddressFieldValue)) {
                if (parent.getEnvironment().getNotifiers().getNotifier((String) scopeField.getSelectedItem()).getDestination(destinationAddressFieldValue) != null) {
                    KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("notifier.destination.exists"), getEnvironmentWindow().getEnvironmentName());
                    return;
                }
            }
            // looking for the destination object
            Destination destination = parent.getEnvironment().getNotifiers().getNotifier((String) scopeField.getSelectedItem()).getDestination(destinationAddress);
            if (destination == null) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("notifier.destination.notfound"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // add a change event
            parent.getChangeEvents().add("Change notifier destination to " + destination.getAddress());
            // update the destination address object
            destination.setAddress(destinationAddressFieldValue);
            // change the updated flag
            parent.setUpdated(true);
            // update the journal log tab pane
            parent.updateJournalPane();
            // update only the tab
            update();
        }
    };
    // create destination
    private ActionListener createDestination = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the environment lock
            if (!getEnvironmentWindow().getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!getEnvironmentWindow().adminPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // get the new destination address field value
            String newDestinationAddressFieldValue = newDestinationField.getText();
            // check mandatory field
            if (newDestinationAddressFieldValue == null || newDestinationAddressFieldValue.trim().length() < 1) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("notifier.destination.mandatory"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // create a new destination object
            Destination destination = new Destination();
            destination.setAddress(newDestinationAddressFieldValue);
            // add the destination
            try {
                parent.getEnvironment().getNotifiers().getNotifier((String) scopeField.getSelectedItem()).addDestination(destination);
            } catch (Exception e) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("notifier.destination.exists"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // add a change event
            parent.getChangeEvents().add("Create notifier destination " + destination.getAddress());
            // change the updated flag
            parent.setUpdated(true);
            // update the journal log tab pane
            parent.updateJournalPane();
            // update only the tab
            update();
        }
    };
    // delete destination
    private ActionListener deleteDestination = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the environment lock
            if (!getEnvironmentWindow().getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!getEnvironmentWindow().adminPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // get the destination address
            String destinationAddress = event.getActionCommand();
            // looking for the destination object
            final Destination destination = parent.getEnvironment().getNotifiers().getNotifier((String) scopeField.getSelectedItem()).getDestination(destinationAddress);
            if (destination == null) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("notifier.destination.notfound"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // display confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new ConfirmWindow(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    // remove the destination
                    parent.getEnvironment().getNotifiers().getNotifier((String) scopeField.getSelectedItem()).getDestinations().remove(destination);
                    // add a change event
                    parent.getChangeEvents().add("Delete notifier destination " + destination.getAddress());
                    // change the updated flag
                    parent.setUpdated(true);
                    // update the journal log tab pane
                    parent.updateJournalPane();
                    // update only the tab
                    update();
                }
            }));
        }
    };
    // delete notifier
    private ActionListener deleteNotifier = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // check if the user has the environment lock
            if (!getEnvironmentWindow().getEnvironment().getLock().equals(KalumetConsoleApplication.getApplication().getUserid())) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("environment.locked"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // check if the user can do it
            if (!getEnvironmentWindow().adminPermission) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("action.restricted"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // looking for the notifier object
            final Email notifier = parent.getEnvironment().getNotifiers().getNotifier((String) scopeField.getSelectedItem());
            if (notifier == null) {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(Messages.getString("notifier.notfound"), getEnvironmentWindow().getEnvironmentName());
                return;
            }
            // display confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new ConfirmWindow(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    // remove the notifier object
                    parent.getEnvironment().getNotifiers().getNotifiers().remove(notifier);
                    // add a change event
                    parent.getChangeEvents().add("Delete notifier " + notifier.getMailhost());
                    // change the updated flag
                    parent.setUpdated(true);
                    // update the journal log tab pane
                    parent.updateJournalPane();
                    // update the pane
                    update();
                }
            }));
        }
    };
    // edit notifier
    private ActionListener editNotifier = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            if (KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().getComponent("notifierwindow_" + parent.getEnvironmentName() + "_" + event.getActionCommand()) == null) {
                KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new NotifierWindow(NotifiersPane.this, (String) scopeField.getSelectedItem()));
            }
        }
    };
    // create notifier
    private ActionListener createNotifier = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new NotifierWindow(NotifiersPane.this, null));
        }
    };
    // copy notifier
    private ActionListener copyNotifier = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // looking for the notifier object
            Email notifier = parent.getEnvironment().getNotifiers().getNotifier((String) scopeField.getSelectedItem());
            if (notifier == null) {
                return;
            }
            try {
                KalumetConsoleApplication.getApplication().setCopyComponent(notifier.clone());
            } catch (Exception e) {
                return;
            }
        }
    };
    // copy destination
    private ActionListener copyDestination = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            // looking for the destination object
            Destination destination = parent.getEnvironment().getNotifiers().getNotifier((String) scopeField.getSelectedItem()).getDestination(event.getActionCommand());
            if (destination == null) {
                return;
            }
            try {
                KalumetConsoleApplication.getApplication().setCopyComponent(destination.clone());
            } catch (Exception e) {
                return;
            }
        }
    };
    // paste destination
    private ActionListener pasteDestination = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            Object copy = KalumetConsoleApplication.getApplication().getCopyComponent();
            // check the copy object
            if (copy == null || !(copy instanceof Destination)) {
                return;
            }
            // update the new fields
            newDestinationField.setText(((Destination) copy).getAddress());
        }
    };

    /**
     * Create a new <code>NotifiersPane</code>.
     *
     * @param parent the parent <code>EnvironmentWindow</code>.
     */
    public NotifiersPane(EnvironmentWindow parent) {
        super();
        setStyleName("tab.content");

        // update parent
        this.parent = parent;

        // column layout
        Column content = new Column();
        content.setInsets(new Insets(2));
        content.setCellSpacing(new Extent(2));
        add(content);

        // general grid
        Grid generalGrid = new Grid(2);
        generalGrid.setStyleName("default");
        generalGrid.setWidth(new Extent(100, Extent.PERCENT));
        generalGrid.setColumnWidth(0, new Extent(20, Extent.PERCENT));
        generalGrid.setColumnWidth(1, new Extent(80, Extent.PERCENT));
        content.add(generalGrid);

        // countdown field
        Label countDownLabel = new Label(Messages.getString("countdown"));
        countDownLabel.setStyleName("default");
        generalGrid.add(countDownLabel);
        countDownField = new TextField();
        countDownField.setStyleName("default");
        countDownField.setWidth(new Extent(50, Extent.EX));
        generalGrid.add(countDownField);

        // scope row
        Row scopeRow = new Row();
        scopeRow.setInsets(new Insets(2));
        scopeRow.setCellSpacing(new Extent(4));
        content.add(scopeRow);
        // scope select field
        scopeField = new SelectField();
        scopeField.setStyleName("default");
        scopeField.setWidth(new Extent(50, Extent.EX));
        scopeField.addActionListener(scopeSelect);
        scopeRow.add(scopeField);
        DefaultListModel scopeListModel = (DefaultListModel) scopeField.getModel();
        scopeListModel.removeAll();
        // add notifiers in the scope select field
        for (Iterator notifierIterator = parent.getEnvironment().getNotifiers().getNotifiers().iterator(); notifierIterator.hasNext(); ) {
            Email email = (Email) notifierIterator.next();
            scopeListModel.add(email.getMailhost());
        }
        if (scopeListModel.size() > 0) {
            scopeField.setSelectedIndex(0);
        }

        // copy notifier button
        Button copyNotifierButton = new Button(Styles.PAGE_COPY);
        copyNotifierButton.setToolTipText(Messages.getString("copy"));
        copyNotifierButton.addActionListener(copyNotifier);
        scopeRow.add(copyNotifierButton);
        // edit notifier button
        Button editNotifierButton = new Button(Styles.ACCEPT);
        editNotifierButton.setToolTipText(Messages.getString("edit"));
        editNotifierButton.addActionListener(editNotifier);
        scopeRow.add(editNotifierButton);
        if (getEnvironmentWindow().adminPermission) {
            // delete
            Button deleteNotifierButton = new Button(Styles.DELETE);
            deleteNotifierButton.setToolTipText(Messages.getString("delete"));
            deleteNotifierButton.addActionListener(deleteNotifier);
            scopeRow.add(deleteNotifierButton);
            // add
            Button createNotifierButton = new Button(Styles.ADD);
            createNotifierButton.setToolTipText(Messages.getString("notifier.add"));
            createNotifierButton.addActionListener(createNotifier);
            scopeRow.add(createNotifierButton);
        }

        // add the destinations grid
        grid = new Grid(2);
        grid.setStyleName("border.grid");
        grid.setColumnWidth(0, new Extent(50, Extent.PX));
        grid.setColumnWidth(1, new Extent(100, Extent.PERCENT));
        content.add(grid);

        // update the pane
        update();
    }

    /**
     * Update the pane.
     */
    public void update() {
        // update the count down field
        countDownField.setText(new Integer(parent.getEnvironment().getNotifiers().getCountdown()).toString());

        // update the scope select field
        String notifierMailhost = null;
        DefaultListModel scopeListModel = (DefaultListModel) scopeField.getModel();
        if (scopeListModel.size() > 0) {
            notifierMailhost = (String) scopeField.getSelectedItem();
        }
        scopeListModel.removeAll();
        int scopeIndex = 0;
        int found = -1;
        for (Iterator notifierIterator = parent.getEnvironment().getNotifiers().getNotifiers().iterator(); notifierIterator.hasNext(); ) {
            Email notifier = (Email) notifierIterator.next();
            scopeListModel.add(notifier.getMailhost());
            if (notifier.getMailhost().equals(notifierMailhost)) {
                found = scopeIndex;
            }
            scopeIndex++;
        }

        // remove all destinations grid children
        grid.removeAll();
        // check if at least one notifier is present
        if (scopeListModel.size() < 1) {
            return;
        }
        if (found == -1) {
            scopeField.setSelectedIndex(0);
        } else {
            scopeField.setSelectedIndex(found);
        }
        // update the notifier from the scope (in case of deletion)
        notifierMailhost = (String) scopeField.getSelectedItem();

        // add destinations grid header
        Label destinationActionHeader = new Label(" ");
        destinationActionHeader.setStyleName("grid.header");
        grid.add(destinationActionHeader);
        Label destinationAddressHeader = new Label(Messages.getString("destination"));
        destinationAddressHeader.setStyleName("grid.header");
        grid.add(destinationAddressHeader);
        // add the destinations e-mails
        for (Iterator destinationIterator = parent.getEnvironment().getNotifiers().getNotifier(notifierMailhost).getDestinations().iterator(); destinationIterator.hasNext(); ) {
            Destination destination = (Destination) destinationIterator.next();
            // row
            Row row = new Row();
            row.setInsets(new Insets(2));
            row.setCellSpacing(new Extent(2));
            grid.add(row);
            // copy
            Button copyButton = new Button(Styles.PAGE_COPY);
            copyButton.setToolTipText(Messages.getString("copy"));
            copyButton.setActionCommand(destination.getAddress());
            copyButton.addActionListener(copyDestination);
            row.add(copyButton);
            if (getEnvironmentWindow().adminPermission) {
                // edit
                Button editButton = new Button(Styles.ACCEPT);
                editButton.setToolTipText(Messages.getString("apply"));
                editButton.setActionCommand(destination.getAddress());
                editButton.addActionListener(editDestination);
                row.add(editButton);
                // delete
                Button deleteButton = new Button(Styles.DELETE);
                deleteButton.setToolTipText(Messages.getString("delete"));
                deleteButton.setActionCommand(destination.getAddress());
                deleteButton.addActionListener(deleteDestination);
                row.add(deleteButton);
            }
            // destination
            TextField destinationAddress = new TextField();
            destinationAddress.setId("notifierdestination_" + parent.getEnvironmentName() + "_" + notifierMailhost + "_" + destination.getAddress());
            destinationAddress.setText(destination.getAddress());
            destinationAddress.setStyleName("default");
            destinationAddress.setWidth(new Extent(100, Extent.PERCENT));
            grid.add(destinationAddress);
        }
        // add create destination row in the destinations grid
        if (getEnvironmentWindow().adminPermission) {
            // row
            Row row = new Row();
            row.setInsets(new Insets(2));
            row.setCellSpacing(new Extent(2));
            grid.add(row);
            // paste
            Button pasteButton = new Button(Styles.PAGE_PASTE);
            pasteButton.setToolTipText(Messages.getString("paste"));
            pasteButton.addActionListener(pasteDestination);
            row.add(pasteButton);
            // add
            Button addButton = new Button(Styles.ADD);
            addButton.setToolTipText(Messages.getString("destination.add"));
            addButton.addActionListener(createDestination);
            row.add(addButton);
            // destination
            newDestinationField = new TextField();
            newDestinationField.setStyleName("default");
            newDestinationField.setWidth(new Extent(100, Extent.PERCENT));
            grid.add(newDestinationField);
        }
    }

    /**
     * Get the parent <code>EnvironmentWindow</code>.
     *
     * @return the parent <code>EnvironmentWindow</code>.
     */
    public EnvironmentWindow getEnvironmentWindow() {
        return parent;
    }

    /**
     * Get the count down <code>TextField</code>.
     *
     * @return the count down <code>TextField</code>.
     */
    public TextField getCountDownField() {
        return this.countDownField;
    }

}