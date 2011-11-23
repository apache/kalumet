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
import nextapp.echo2.app.ContentPane;
import nextapp.echo2.app.Grid;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import org.apache.kalumet.model.LogFile;

/**
 * Environment log viewer pane.
 */
public class LogViewerPane extends ContentPane {

    private EnvironmentWindow parent;
    private Grid grid;

    // view
    private ActionListener view = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            String logFileName = event.getActionCommand();
            // looking for the log file
            LogFile logFile = parent.getEnvironment().getLogFile(logFileName);
            if (logFile == null) {
                KalumetConsoleApplication.getApplication().getLogPane().addError(Messages.getString("logfile.notfound"), parent.getEnvironmentName());
                return;
            }
            // define which agent to use
            String agentId;
            if (logFile.getAgent() != null && logFile.getAgent().trim().length() > 0) {
                agentId = logFile.getAgent();
            } else {
                agentId = parent.getEnvironment().getAgent();
            }
            // open a view file window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new ViewFileWindow(logFile.getPath(), agentId));
        }
    };

    /**
     * Create a new <code>LogViewerPane</code>.
     *
     * @param parent the parent <code>EnvironmentWindow</code>.
     */
    public LogViewerPane(EnvironmentWindow parent) {
        super();
        this.setStyleName("tab.content");

        // update the parent
        this.parent = parent;

        grid = new Grid(3);
        grid.setStyleName("border.grid");
        add(grid);

        // update the pane
        update();
    }

    /**
     * Update the pane.
     */
    public void update() {
        grid.removeAll();
        // add grid headers
        // name header
        Label nameHeader = new Label(Messages.getString("name"));
        nameHeader.setStyleName("grid.header");
        grid.add(nameHeader);
        // path header
        Label pathHeader = new Label(Messages.getString("path"));
        pathHeader.setStyleName("grid.header");
        grid.add(pathHeader);
        // agent header
        Label agentHeader = new Label(Messages.getString("agent"));
        agentHeader.setStyleName("grid.header");
        grid.add(agentHeader);
        // iterator in the log pane
        for (Iterator logFileIterator = parent.getEnvironment().getLogFiles().iterator(); logFileIterator.hasNext(); ) {
            LogFile logFile = (LogFile) logFileIterator.next();
            // name
            Button name = new Button(logFile.getName());
            name.setActionCommand(logFile.getName());
            name.addActionListener(view);
            grid.add(name);
            // path
            Label path = new Label(logFile.getPath());
            grid.add(path);
            // agent
            Label agent = new Label(logFile.getAgent());
            grid.add(agent);
        }
    }

}