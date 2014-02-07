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
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import org.apache.kalumet.model.JMSTopic;

import java.util.Iterator;

/**
 * JMS topics pane.
 */
public class JmsTopicsPane
    extends ContentPane
{

    private JmsServerWindow parent;

    private Grid grid;

    private TextField newTopicNameField;

    // delete
    private ActionListener delete = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            // check if the user has the environment lock
            if ( !getEnvironmentWindow().getEnvironment().getLock().equals(
                KalumetConsoleApplication.getApplication().getUserid() ) )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "environment.locked" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // check if the user can do it
            if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeResourcesChangePermission )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "action.restricted" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // looking for the jms topic object
            final JMSTopic jmsTopic = parent.getJMSServer().getJMSTopic( event.getActionCommand() );
            if ( jmsTopic == null )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "jmstopic.notfound" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // display confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
                new ConfirmWindow( new ActionListener()
                {
                    public void actionPerformed( ActionEvent event )
                    {
                        // delete the jms topic
                        parent.getJMSServer().getJMSTopics().remove( jmsTopic );
                        // add a change event
                        parent.getEnvironmentWindow().getChangeEvents().add( "Delete JMS topic " + jmsTopic.getName() );
                        // change the updated flag
                        parent.getEnvironmentWindow().setUpdated( true );
                        // update the whole environment window
                        parent.getEnvironmentWindow().update();
                        // update the pane
                        update();
                    }
                } ) );
        }
    };

    // edit
    private ActionListener edit = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            // check if the user has the environment lock
            if ( !getEnvironmentWindow().getEnvironment().getLock().equals(
                KalumetConsoleApplication.getApplication().getUserid() ) )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "environment.locked" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // check if the user can do it
            if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeResourcesChangePermission )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "action.restricted" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // get the field
            TextField topicNameField = (TextField) JmsTopicsPane.this.getComponent(
                "jmsTopicNameField_" + parent.getEnvironmentWindow().getEnvironmentName() + "_" + parent.getServerName()
                    + "_" + parent.getJMSServerName() + "_" + event.getActionCommand() );
            // get the field value
            String topicNameFieldValue = topicNameField.getText();
            // check field
            if ( topicNameFieldValue == null || topicNameFieldValue.trim().length() < 1 )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "jmstopic.mandatory" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // looking for the JMS topic object
            JMSTopic jmsTopic = parent.getJMSServer().getJMSTopic( event.getActionCommand() );
            if ( jmsTopic == null )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "jmstopic.notfound" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // if the user change the JMS topic name, check if the JMS topic name
            // doesn't already exist
            if ( !topicNameFieldValue.equals( jmsTopic.getName() ) )
            {
                if ( parent.getJMSServer().getJMSTopic( topicNameFieldValue ) != null )
                {
                    KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                        Messages.getString( "jmstopic.exists" ), getEnvironmentWindow().getEnvironmentName() );
                    return;
                }
            }
            // add a change event
            parent.getEnvironmentWindow().getChangeEvents().add( "Change JMS topic " + jmsTopic.getName() );
            // update the jms topic object
            jmsTopic.setName( topicNameFieldValue );
            // change the updated flag
            parent.getEnvironmentWindow().setUpdated( true );
            // update the whole parent window
            parent.getEnvironmentWindow().update();
            // update the pane
            update();
        }
    };

    // create
    private ActionListener create = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            // check if the user has the environment lock
            if ( !getEnvironmentWindow().getEnvironment().getLock().equals(
                KalumetConsoleApplication.getApplication().getUserid() ) )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "environment.locked" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // check if the user can do it
            if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeResourcesChangePermission )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "environment.locked" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // get the field value
            String newTopicNameFieldValue = newTopicNameField.getText();
            // check the field
            if ( newTopicNameFieldValue == null || newTopicNameFieldValue.trim().length() < 1 )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "jmstopic.mandatory" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // create the jms topic object
            JMSTopic jmsTopic = new JMSTopic();
            jmsTopic.setName( newTopicNameFieldValue );
            // add the jms topic in the jms server
            try
            {
                parent.getJMSServer().addJMSTopic( jmsTopic );
            }
            catch ( Exception e )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "jmstopic.exists" ), getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // add a change event
            parent.getEnvironmentWindow().getChangeEvents().add( "Add JMS topic " + jmsTopic.getName() );
            // change the updated flag
            parent.getEnvironmentWindow().setUpdated( true );
            // update the whole window
            parent.getEnvironmentWindow().update();
            // update the pane
            update();
        }
    };

    /**
     * Create a new <code>JmsTopicsPane</code>.
     *
     * @param parent the parent <code>JmsServerWindow</code>.
     */
    public JmsTopicsPane( JmsServerWindow parent )
    {
        super();
        setStyleName( "tab.content" );

        // update parent
        this.parent = parent;

        // add topics grid
        grid = new Grid( 2 );
        grid.setStyleName( "border.grid" );
        grid.setColumnWidth( 0, new Extent( 50, Extent.PX ) );
        add( grid );

        // update the pane
        update();
    }

    /**
     * Update the pane.
     */
    public void update()
    {
        // remove topics grid children
        grid.removeAll();
        // add topics header
        Label actionHeader = new Label( " " );
        actionHeader.setStyleName( "grid.header" );
        grid.add( actionHeader );
        Label nameHeader = new Label( Messages.getString( "name" ) );
        nameHeader.setStyleName( "grid.header" );
        grid.add( nameHeader );
        // add topic
        for ( Iterator topicIterator = parent.getJMSServer().getJMSTopics().iterator(); topicIterator.hasNext(); )
        {
            JMSTopic topic = (JMSTopic) topicIterator.next();
            // row
            Row row = new Row();
            row.setCellSpacing( new Extent( 2 ) );
            row.setInsets( new Insets( 2 ) );
            grid.add( row );
            if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeResourcesChangePermission )
            {
                // add edit button
                Button editButton = new Button( Styles.ACCEPT );
                editButton.setToolTipText( Messages.getString( "apply" ) );
                editButton.setActionCommand( topic.getName() );
                editButton.addActionListener( edit );
                row.add( editButton );
                // delete
                Button deleteButton = new Button( Styles.DELETE );
                deleteButton.setToolTipText( Messages.getString( "delete" ) );
                deleteButton.setActionCommand( topic.getName() );
                deleteButton.addActionListener( delete );
                row.add( deleteButton );
            }
            // name
            TextField nameField = new TextField();
            nameField.setStyleName( "default" );
            nameField.setWidth( new Extent( 100, Extent.PERCENT ) );
            nameField.setId(
                "topicname_" + parent.getEnvironmentWindow().getEnvironmentName() + "_" + parent.getServerName() + "_"
                    + parent.getJMSServerName() + "_" + topic.getName() );
            nameField.setText( topic.getName() );
            grid.add( nameField );
        }
        if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeResourcesChangePermission )
        {
            // row
            Row row = new Row();
            row.setCellSpacing( new Extent( 2 ) );
            row.setInsets( new Insets( 2 ) );
            grid.add( row );
            // add
            Button addButton = new Button( Styles.ADD );
            addButton.setToolTipText( Messages.getString( "add" ) );
            addButton.addActionListener( create );
            row.add( addButton );
            // name
            newTopicNameField = new TextField();
            newTopicNameField.setStyleName( "default" );
            newTopicNameField.setWidth( new Extent( 100, Extent.PERCENT ) );
            grid.add( newTopicNameField );
        }
    }

    public EnvironmentWindow getEnvironmentWindow()
    {
        return parent.getEnvironmentWindow();
    }

}