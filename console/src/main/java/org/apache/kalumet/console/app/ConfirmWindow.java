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
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.SplitPane;
import nextapp.echo2.app.WindowPane;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;

/**
 * Confirm window to interact with the user.
 */
public class ConfirmWindow extends WindowPane {

    public ConfirmWindow(final ActionListener callback) {
        super();

        this.setStyleName("default");
        this.setTitle(Messages.getString("confirm"));
        this.setWidth(new Extent(220, Extent.PX));
        this.setHeight(new Extent(120, Extent.PX));
        this.setModal(true);

        SplitPane content = new SplitPane(SplitPane.ORIENTATION_VERTICAL_BOTTOM_TOP, new Extent(32));
        this.add(content);

        // control row
        Row controlRow = new Row();
        controlRow.setStyleName("control");
        content.add(controlRow);

        // add yes button
        Button yesButton = new Button(Messages.getString("yes"), Styles.ACCEPT);
        yesButton.setStyleName("control");
        yesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                // call the callback
                callback.actionPerformed(null);
                // close the confirm window
                ConfirmWindow.this.userClose();
            }
        });
        controlRow.add(yesButton);

        // add the no button
        Button noButton = new Button(Messages.getString("no"), Styles.CROSS);
        noButton.setStyleName("control");
        noButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                ConfirmWindow.this.userClose();
            }
        });
        controlRow.add(noButton);

        // add the main label
        Label mainLabel = new Label(Messages.getString("sure.question"));
        content.add(mainLabel);

    }

}