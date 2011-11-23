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
import nextapp.echo2.app.Grid;
import nextapp.echo2.app.Label;

/**
 * Environment statistics pane.
 */
public class StatisticsPane extends ContentPane {

    private EnvironmentWindow parent;
    private Label updateCount;
    private Label lastChangeDate;
    private Label lastUpdateDate;


    /**
     * Create a new <code>StatisticsPane</code>.
     *
     * @param parent the parent <code>EnvironmentWindow</code>.
     */
    public StatisticsPane(EnvironmentWindow parent) {
        super();
        this.setStyleName("tab.content");

        // update the parent
        this.parent = parent;

        Grid layout = new Grid(2);
        layout.setStyleName("default");
        add(layout);

        Label updateCountLabel = new Label(Messages.getString("update.count"));
        layout.add(updateCountLabel);

        updateCount = new Label();
        layout.add(updateCount);

        Label lastChangeDateLabel = new Label(Messages.getString("last.change.date"));
        layout.add(lastChangeDateLabel);

        lastChangeDate = new Label();
        layout.add(lastChangeDate);

        Label lastUpLabelLabel = new Label(Messages.getString("last.update.date"));
        layout.add(lastUpLabelLabel);

        lastUpdateDate = new Label();
        layout.add(lastUpLabelLabel);

        update();
    }

    /**
     * Update the pane.
     */
    public void update() {

    }

}
