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
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.SplitPane;
import nextapp.echo2.app.TextArea;
import nextapp.echo2.app.WindowPane;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.app.layout.SplitPaneLayoutData;
import org.apache.kalumet.console.configuration.ConfigurationManager;
import org.apache.kalumet.model.Agent;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.ws.client.FileClient;

/**
 * Window displaying the content of a file.
 */
public class ViewFileWindow extends WindowPane {

    private String path;
    private String agentId;
    private Label statusLabel;
    private TextArea contentArea;

    // view thread
    class ViewThread extends Thread {

        public boolean ended = false;
        public boolean failure = false;
        public String path;
        public String agentId;
        public String message;

        public void run() {
            try {
                // load Kalumet configuration
                Kalumet kalumet = ConfigurationManager.loadStore();
                // looking for the agent
                Agent agent = kalumet.getAgent(agentId);
                if (agent == null) {
                    throw new IllegalArgumentException("agent " + agentId + " not found.");
                }
                // call the WebService
                FileClient client = new FileClient(agent.getHostname(), agent.getPort());
                message = client.view(path);
            } catch (Exception e) {
                failure = true;
                message = "Can't view " + path + ": " + e.getMessage();
            } finally {
                ended = true;
            }
        }

    }

    // refresh
    private ActionListener refresh = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            update();
        }
    };

    /**
     * Create a window to display the content of the given file.
     *
     * @param path    the file path to display content.
     * @param agentId the agent id to use to view the file.
     */
    public ViewFileWindow(String path, String agentId) {
        super();

        this.path = path;
        this.agentId = agentId;

        this.setStyleName("default");
        this.setTitle("View file " + path);
        this.setIcon(Styles.SCRIPT);
        this.setWidth(new Extent(600, Extent.PX));
        this.setHeight(new Extent(400, Extent.PX));

        // add the split pane
        SplitPane splitPane = new SplitPane(SplitPane.ORIENTATION_VERTICAL_BOTTOM_TOP, new Extent(32));
        add(splitPane);

        // add the control row
        Row controlRow = new Row();
        controlRow.setStyleName("control");
        splitPane.add(controlRow);

        // add the refresh button
        Button refreshButton = new Button(Messages.getString("reload"), Styles.DATABASE_REFRESH);
        refreshButton.setStyleName("control");
        refreshButton.addActionListener(refresh);
        controlRow.add(refreshButton);

        // add the close button
        Button closeButton = new Button(Messages.getString("close"), Styles.CROSS);
        closeButton.setStyleName("control");
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                ViewFileWindow.this.userClose();
            }
        });
        controlRow.add(closeButton);

        // add the content column
        SplitPane content = new SplitPane(SplitPane.ORIENTATION_VERTICAL_TOP_BOTTOM, new Extent(20));
        splitPane.add(content);

        // add the status label
        statusLabel = new Label();
        statusLabel.setStyleName("default");
        SplitPaneLayoutData layoutData = new SplitPaneLayoutData();
        layoutData.setInsets(new Insets(4));
        statusLabel.setLayoutData(layoutData);
        content.add(statusLabel);

        // add the file content area
        contentArea = new TextArea();
        contentArea.setStyleName("default");
        contentArea.setLayoutData(layoutData);
        contentArea.setWidth(new Extent(100, Extent.PERCENT));
        contentArea.setHeight(new Extent(98, Extent.PERCENT));
        contentArea.setEnabled(false);
        content.add(contentArea);

        // update the pane
        update();
    }

    public void update() {
        statusLabel.setText("Please wait ...");
        statusLabel.setIcon(Styles.ERROR);

        // launch the view thread
        final ViewThread viewThread = new ViewThread();
        viewThread.agentId = agentId;
        viewThread.path = path;
        viewThread.start();

        // sync with the client
        KalumetConsoleApplication.getApplication().enqueueTask(KalumetConsoleApplication.getApplication().getTaskQueue(), new Runnable() {
            public void run() {
                if (viewThread.ended) {
                    if (viewThread.failure) {
                        statusLabel.setText(viewThread.message);
                        statusLabel.setIcon(Styles.EXCLAMATION);
                    } else {
                        statusLabel.setText("File loaded.");
                        statusLabel.setIcon(Styles.ACCEPT);
                        contentArea.setText(viewThread.message);
                    }
                } else {
                    KalumetConsoleApplication.getApplication().enqueueTask(KalumetConsoleApplication.getApplication().getTaskQueue(), this);
                }
            }
        });
    }

}
