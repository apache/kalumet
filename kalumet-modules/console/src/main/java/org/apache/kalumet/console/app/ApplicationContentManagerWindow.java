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
import nextapp.echo2.app.SelectField;
import nextapp.echo2.app.SplitPane;
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.WindowPane;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.app.list.DefaultListModel;
import nextapp.echo2.extras.app.TabPane;
import nextapp.echo2.extras.app.layout.TabPaneLayoutData;
import org.apache.kalumet.console.configuration.ConfigurationManager;
import org.apache.kalumet.model.Agent;
import org.apache.kalumet.model.ContentManager;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.model.Property;
import org.apache.kalumet.ws.client.ContentManagerClient;

import java.util.Iterator;

/**
 * JEE application content manager window.
 */
public class ApplicationContentManagerWindow
    extends WindowPane
{

    private String contentManagerName;

    private ContentManager contentManager;

    private ApplicationContentManagersPane parent;

    private TextField nameField;

    private SelectField activeField;

    private SelectField blockerField;

    private TextField classnameField;

    private SelectField agentField;

    private Grid propertiesGrid;

    private TextField newPropertyNameField;

    private TextField newPropertyValueField;

    class UpdateThread
        extends Thread
    {

        public boolean ended = false;

        public boolean failure = false;

        public String message;

        public void run()
        {
            try
            {
                // load Kalumet configuration
                Kalumet kalumet = ConfigurationManager.loadStore();
                // looking for the agent
                Agent agent = kalumet.getAgent(
                    parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironment().getAgent() );
                if ( agent == null )
                {
                    throw new IllegalArgumentException( "agent not found." );
                }
                // call the webservice
                ContentManagerClient client = new ContentManagerClient( agent.getHostname(), agent.getPort() );
                client.update( parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName(),
                               parent.getParentPane().getServerName(), parent.getParentPane().getApplicationName(),
                               contentManagerName, false );
            }
            catch ( Exception e )
            {
                failure = true;
                message = "JEE application " + parent.getParentPane().getApplicationName() + " content manager "
                    + contentManagerName + " update failed: " + e.getMessage();
            }
            finally
            {
                ended = true;
            }
        }
    }

    // refresh
    private ActionListener refresh = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            // looking for the content manager object
            ApplicationContentManagerWindow.this.contentManager =
                parent.getParentPane().getApplication().getContentManager( contentManagerName );
            if ( ApplicationContentManagerWindow.this.contentManager == null )
            {
                ApplicationContentManagerWindow.this.contentManager = new ContentManager();
            }
            // update the window
            update();
        }
    };

    // close
    private ActionListener close = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            ApplicationContentManagerWindow.this.userClose();
        }
    };

    // delete
    private ActionListener delete = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            // check if the user has the environment lock
            if ( !parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironment().getLock().equals(
                KalumetConsoleApplication.getApplication().getUserid() ) )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "environment.locked" ),
                    parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // check if the user can do it
            if ( !parent.getParentPane().getParentPane().getEnvironmentWindow().adminPermission
                && !parent.getParentPane().getParentPane().getEnvironmentWindow().jeeApplicationsChangePermission )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "action.restricted" ),
                    parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // display confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
                new ConfirmWindow( new ActionListener()
                {
                    public void actionPerformed( ActionEvent event )
                    {
                        // delete the content manager
                        parent.getParentPane().getApplication().getContentManagers().remove( contentManager );
                        // add a change event
                        parent.getParentPane().getParentPane().getEnvironmentWindow().getChangeEvents().add(
                            "Delete JEE application " + parent.getParentPane().getApplicationName()
                                + " content manager " + contentManager.getName() );
                        // change the updated flag
                        parent.getParentPane().getParentPane().getEnvironmentWindow().setUpdated( true );
                        // update the journal log tab pane
                        parent.getParentPane().getParentPane().getEnvironmentWindow().updateJournalPane();
                        // update the parent pane
                        parent.update();
                        // close the window
                        ApplicationContentManagerWindow.this.userClose();
                    }
                } ) );
        }
    };

    // apply
    private ActionListener apply = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            // check if the user has the environment lock
            if ( !parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironment().getLock().equals(
                KalumetConsoleApplication.getApplication().getUserid() ) )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "environment.locked" ),
                    parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // check if the user can do it
            if ( !parent.getParentPane().getParentPane().getEnvironmentWindow().adminPermission
                && !parent.getParentPane().getParentPane().getEnvironmentWindow().jeeApplicationsChangePermission )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "action.restricted" ),
                    parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // get the fields value
            String nameFieldValue = nameField.getText();
            int activeFieldIndex = activeField.getSelectedIndex();
            int blockerFieldIndex = blockerField.getSelectedIndex();
            String classnameFieldValue = classnameField.getText();
            String agentFieldValue = (String) agentField.getSelectedItem();
            // check fields
            if ( nameFieldValue == null || nameFieldValue.trim().length() < 1 || classnameFieldValue == null
                || classnameFieldValue.trim().length() < 1 )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "contentmanager.mandatory" ),
                    parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // if the user change the content manager name, check if the new
            // content manager name doesn't already exist
            if ( contentManagerName == null || ( contentManagerName != null && !contentManagerName.equals(
                nameFieldValue ) ) )
            {
                if ( parent.getParentPane().getApplication().getContentManager( nameFieldValue ) != null )
                {
                    KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                        Messages.getString( "contentmanager.exists" ),
                        parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
                    return;
                }
            }
            // add a change event
            if ( contentManagerName != null )
            {
                parent.getParentPane().getParentPane().getEnvironmentWindow().getChangeEvents().add(
                    "Change JEE application " + parent.getParentPane().getApplicationName() + " content manager "
                        + contentManager.getName() );
            }
            // update the content manager object
            contentManager.setName( nameFieldValue );
            if ( activeFieldIndex == 0 )
            {
                contentManager.setActive( true );
            }
            else
            {
                contentManager.setActive( false );
            }
            if ( blockerFieldIndex == 0 )
            {
                contentManager.setBlocker( true );
            }
            else
            {
                contentManager.setBlocker( false );
            }
            contentManager.setClassname( classnameFieldValue );
            contentManager.setAgent( agentFieldValue );
            // add the content manager object if needed
            if ( contentManagerName == null )
            {
                try
                {
                    parent.getParentPane().getApplication().addContentManager( contentManager );
                    parent.getParentPane().getParentPane().getEnvironmentWindow().getChangeEvents().add(
                        "Add JEE application " + parent.getParentPane().getApplicationName() + " content manager "
                            + contentManager.getName() );
                }
                catch ( Exception e )
                {
                    KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                        Messages.getString( "contentmanager.exists" ),
                        parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
                    return;
                }
            }
            // update the window definition
            setTitle( Messages.getString( "contentmanager" ) + " " + contentManager.getName() );
            setId( "contentmanagerwindow_"
                       + parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() + "_"
                       + parent.getParentPane().getServerName() + "_" + parent.getParentPane().getApplicationName()
                       + "_" + contentManager.getName() );
            contentManagerName = contentManager.getName();
            // change the updated flag
            parent.getParentPane().getParentPane().getEnvironmentWindow().setUpdated( true );
            // update the journal log tab pane
            parent.getParentPane().getParentPane().getEnvironmentWindow().updateJournalPane();
            // update the parent window
            parent.update();
            // update the window
            update();
        }
    };

    // delete property
    private ActionListener deleteProperty = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            // check if the user has the environment lock
            if ( !parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironment().getLock().equals(
                KalumetConsoleApplication.getApplication().getUserid() ) )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "environment.locked" ),
                    parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // check if the user can do it
            if ( !parent.getParentPane().getParentPane().getEnvironmentWindow().adminPermission
                && !parent.getParentPane().getParentPane().getEnvironmentWindow().jeeApplicationsChangePermission )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "action.restricted" ),
                    parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // looking for the property object
            Property property = contentManager.getProperty( event.getActionCommand() );
            if ( property == null )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "property.notfound" ),
                    parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // delete the property object
            contentManager.getProperties().remove( property );
            // add a change event
            parent.getParentPane().getParentPane().getEnvironmentWindow().getChangeEvents().add(
                "Delete JEE application " + parent.getParentPane().getApplicationName() + " content manager "
                    + contentManager.getName() + " property " + property.getName() );
            // change the updated flag
            parent.getParentPane().getParentPane().getEnvironmentWindow().setUpdated( true );
            // update the journal log tab pane
            parent.getParentPane().getParentPane().getEnvironmentWindow().updateJournalPane();
            // update the window
            update();
        }
    };

    // edit property
    private ActionListener editProperty = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            // check if the user has the environment lock
            if ( !parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironment().getLock().equals(
                KalumetConsoleApplication.getApplication().getUserid() ) )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "environment.locked" ),
                    parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // check if the user can do it
            if ( !parent.getParentPane().getParentPane().getEnvironmentWindow().adminPermission
                && !parent.getParentPane().getParentPane().getEnvironmentWindow().jeeApplicationsChangePermission )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "action.restricted" ),
                    parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // get fields
            TextField propertyNameField = (TextField) ApplicationContentManagerWindow.this.getComponent(
                "propertyname_" + parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName()
                    + "_" + parent.getParentPane().getServerName() + "_" + parent.getParentPane().getApplicationName()
                    + "_" + contentManagerName + "_" + event.getActionCommand() );
            TextField propertyValueField = (TextField) ApplicationContentManagerWindow.this.getComponent(
                "propertyvalue_" + parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName()
                    + "_" + parent.getParentPane().getServerName() + "_" + parent.getParentPane().getApplicationName()
                    + "_" + contentManagerName + "_" + event.getActionCommand() );
            // get fields value
            String propertyNameFieldValue = propertyNameField.getText();
            String propertyValueFieldValue = propertyValueField.getText();
            // check fields
            if ( propertyNameFieldValue == null || propertyNameFieldValue.trim().length() < 1
                || propertyValueFieldValue == null || propertyValueFieldValue.trim().length() < 1 )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "contentmanager.mandatory" ),
                    parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // if the user change the property name, check if the name doens't
            // already exist
            if ( !propertyNameFieldValue.equals( event.getActionCommand() ) )
            {
                if ( contentManager.getProperty( propertyNameFieldValue ) != null )
                {
                    KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                        Messages.getString( "contentmanager.exists" ),
                        parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
                    return;
                }
            }
            // looking for the property object
            Property property = contentManager.getProperty( event.getActionCommand() );
            if ( property == null )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "property.notfound" ),
                    parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // add a change event
            parent.getParentPane().getParentPane().getEnvironmentWindow().getChangeEvents().add(
                "Change JEE application " + parent.getParentPane().getApplicationName() + " content manager "
                    + contentManagerName + " property " + property.getName() );
            // update the property
            property.setName( propertyNameFieldValue );
            property.setValue( propertyValueFieldValue );
            // change the updated flag
            parent.getParentPane().getParentPane().getEnvironmentWindow().setUpdated( true );
            // update the journal log tab pane
            parent.getParentPane().getParentPane().getEnvironmentWindow().updateJournalPane();
            // update the window
            update();
        }
    };

    // create property
    private ActionListener createProperty = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            // check if the user has the environment lock
            if ( !parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironment().getLock().equals(
                KalumetConsoleApplication.getApplication().getUserid() ) )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "environment.locked" ),
                    parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // check if the user can do it
            if ( !parent.getParentPane().getParentPane().getEnvironmentWindow().adminPermission
                && !parent.getParentPane().getParentPane().getEnvironmentWindow().jeeApplicationsChangePermission )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "action.restricted" ),
                    parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // get fields value
            String newPropertyNameFieldValue = newPropertyNameField.getText();
            String newPropertyValueFieldValue = newPropertyValueField.getText();
            // check fields
            if ( newPropertyNameFieldValue == null || newPropertyNameFieldValue.trim().length() < 1
                || newPropertyValueFieldValue == null || newPropertyValueFieldValue.trim().length() < 1 )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "property.mandatory" ),
                    parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // create the property object
            Property property = new Property();
            property.setName( newPropertyNameFieldValue );
            property.setValue( newPropertyValueFieldValue );
            try
            {
                contentManager.addProperty( property );
            }
            catch ( Exception e )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "property.exists" ),
                    parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // add a change event
            parent.getParentPane().getParentPane().getEnvironmentWindow().getChangeEvents().add(
                "Add JEE application " + parent.getParentPane().getApplicationName() + " content manager "
                    + contentManagerName + " property " + property.getName() );
            // change the updated flag
            parent.getParentPane().getParentPane().getEnvironmentWindow().setUpdated( true );
            // update the journal log tab pane
            parent.getParentPane().getParentPane().getEnvironmentWindow().updateJournalPane();
            // update the window
            update();
        }
    };

    // copy
    private ActionListener copy = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            try
            {
                KalumetConsoleApplication.getApplication().setCopyComponent( contentManager.clone() );
            }
            catch ( Exception e )
            {
                return;
            }
        }
    };

    // paste
    private ActionListener paste = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            Object copy = KalumetConsoleApplication.getApplication().getCopyComponent();
            // check if the copy is correct
            if ( copy == null || !( copy instanceof ContentManager ) )
            {
                return;
            }
            contentManager = (ContentManager) copy;
            contentManagerName = null;
            // update the parent pane
            parent.update();
            // upate the window
            update();
        }
    };

    // update
    private ActionListener update = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            // check if the user has the lock
            if ( !parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironment().getLock().equals(
                KalumetConsoleApplication.getApplication().getUserid() ) )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "environment.locked" ),
                    parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // check if the user can do it
            if ( !parent.getParentPane().getParentPane().getEnvironmentWindow().adminPermission
                && !parent.getParentPane().getParentPane().getEnvironmentWindow().jeeApplicationsUpdatePermission )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "action.restricted" ),
                    parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // check if a change has not been saved
            if ( parent.getParentPane().getParentPane().getEnvironmentWindow().isUpdated() )
            {
                KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                    Messages.getString( "environment.notsaved" ),
                    parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
                return;
            }
            // display confirm window
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
                new ConfirmWindow( new ActionListener()
                {
                    public void actionPerformed( ActionEvent event )
                    {
                        // add a message into the log pane and the journal
                        KalumetConsoleApplication.getApplication().getLogPane().addInfo(
                            "JEE application " + parent.getParentPane().getApplicationName() + " content manager "
                                + contentManagerName + " update in progress...",
                            parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
                        parent.getParentPane().getParentPane().getEnvironmentWindow().getChangeEvents().add(
                            "JEE application " + parent.getParentPane().getApplicationName() + " content manager "
                                + contentManagerName + " update requested." );
                        // start the update thread
                        final UpdateThread updateThread = new UpdateThread();
                        updateThread.start();
                        // sync with the client
                        KalumetConsoleApplication.getApplication().enqueueTask(
                            KalumetConsoleApplication.getApplication().getTaskQueue(), new Runnable()
                        {
                            public void run()
                            {
                                if ( updateThread.ended )
                                {
                                    if ( updateThread.failure )
                                    {
                                        KalumetConsoleApplication.getApplication().getLogPane().addError(
                                            updateThread.message,
                                            parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
                                        parent.getParentPane().getParentPane().getEnvironmentWindow().getChangeEvents().add(
                                            updateThread.message );
                                    }
                                    else
                                    {
                                        KalumetConsoleApplication.getApplication().getLogPane().addConfirm(
                                            "JEE application " + parent.getParentPane().getApplicationName()
                                                + " content manager " + contentManagerName + " updated.",
                                            parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
                                        parent.getParentPane().getParentPane().getEnvironmentWindow().getChangeEvents().add(
                                            "JEE application " + parent.getParentPane().getApplicationName()
                                                + " content manager " + contentManagerName + " updated." );
                                    }
                                }
                                else
                                {
                                    KalumetConsoleApplication.getApplication().enqueueTask(
                                        KalumetConsoleApplication.getApplication().getTaskQueue(), this );
                                }
                            }
                        } );
                    }
                } ) );
        }
    };

    // copy property
    private ActionListener copyProperty = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            // looking for the property object
            Property property = contentManager.getProperty( event.getActionCommand() );
            if ( property == null )
            {
                return;
            }
            try
            {
                KalumetConsoleApplication.getApplication().setCopyComponent( property );
            }
            catch ( Exception e )
            {
                return;
            }
        }
    };

    // paste property
    private ActionListener pasteProperty = new ActionListener()
    {
        public void actionPerformed( ActionEvent event )
        {
            Object copy = KalumetConsoleApplication.getApplication().getCopyComponent();
            // check if the copy is correct
            if ( copy == null || !( copy instanceof Property ) )
            {
                return;
            }
            // update the new fields
            newPropertyNameField.setText( ( (Property) copy ).getName() );
            newPropertyValueField.setText( ( (Property) copy ).getValue() );
        }
    };

    /**
     * Create a new <code>ApplicationContentManagerWindow</code>.
     *
     * @param parent             the <code>ApplicationContentManagersPane</code>.
     * @param contentManagerName the original <code>ContentManager</code> name.
     */
    public ApplicationContentManagerWindow( ApplicationContentManagersPane parent, String contentManagerName )
    {
        super();

        // update the parent pane
        this.parent = parent;

        // update the content manager name
        this.contentManagerName = contentManagerName;

        // update the content manager object from the parent pane
        this.contentManager = parent.getParentPane().getApplication().getContentManager( contentManagerName );
        if ( this.contentManager == null )
        {
            this.contentManager = new ContentManager();
        }

        if ( contentManagerName == null )
        {
            setTitle( Messages.getString( "contentmanager" ) );
        }
        else
        {
            setTitle( Messages.getString( "contentmanager" ) + " " + contentManagerName );
        }
        setId(
            "contentmanagerwindow_" + parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName()
                + "_" + parent.getParentPane().getServerName() + "_" + parent.getParentPane().getApplicationName() + "_"
                + contentManagerName );
        setStyleName( "default" );
        setWidth( new Extent( 450, Extent.PX ) );
        setHeight( new Extent( 300, Extent.PX ) );
        setModal( false );
        setDefaultCloseOperation( WindowPane.DISPOSE_ON_CLOSE );

        // create a split pane for the control buttons
        SplitPane splitPane = new SplitPane( SplitPane.ORIENTATION_VERTICAL_BOTTOM_TOP, new Extent( 32 ) );
        add( splitPane );

        // add the control pane
        Row controlRow = new Row();
        controlRow.setStyleName( "control" );
        splitPane.add( controlRow );
        // add the refresh button
        Button refreshButton = new Button( Messages.getString( "reload" ), Styles.DATABASE_REFRESH );
        refreshButton.setStyleName( "control" );
        refreshButton.addActionListener( refresh );
        controlRow.add( refreshButton );
        // add the copy button
        Button copyButton = new Button( Messages.getString( "copy" ), Styles.PAGE_COPY );
        copyButton.setStyleName( "control" );
        copyButton.addActionListener( copy );
        controlRow.add( copyButton );
        if ( parent.getParentPane().getParentPane().getEnvironmentWindow().adminPermission
            || parent.getParentPane().getParentPane().getEnvironmentWindow().jeeApplicationsChangePermission )
        {
            // add the paste button
            Button pasteButton = new Button( Messages.getString( "paste" ), Styles.PAGE_PASTE );
            pasteButton.setStyleName( "control" );
            pasteButton.addActionListener( paste );
            controlRow.add( pasteButton );
        }
        // add the update button
        if ( parent.getParentPane().getParentPane().getEnvironmentWindow().adminPermission
            || parent.getParentPane().getParentPane().getEnvironmentWindow().jeeApplicationsUpdatePermission )
        {
            Button updateButton = new Button( Messages.getString( "update" ), Styles.COG );
            updateButton.setStyleName( "control" );
            updateButton.addActionListener( update );
            controlRow.add( updateButton );
        }
        if ( parent.getParentPane().getParentPane().getEnvironmentWindow().adminPermission
            || parent.getParentPane().getParentPane().getEnvironmentWindow().jeeApplicationsChangePermission )
        {
            // add the apply button
            Button applyButton = new Button( Messages.getString( "apply" ), Styles.ACCEPT );
            applyButton.setStyleName( "control" );
            applyButton.addActionListener( apply );
            controlRow.add( applyButton );
            // add the delete button
            Button deleteButton = new Button( Messages.getString( "delete" ), Styles.DELETE );
            deleteButton.setStyleName( "control" );
            deleteButton.addActionListener( delete );
            controlRow.add( deleteButton );
        }
        // add the close button
        Button closeButton = new Button( Messages.getString( "close" ), Styles.CROSS );
        closeButton.setStyleName( "control" );
        closeButton.addActionListener( close );
        controlRow.add( closeButton );

        // add the main tab pane
        TabPane tabPane = new TabPane();
        tabPane.setStyleName( "default" );
        splitPane.add( tabPane );

        // add the general tab
        TabPaneLayoutData tabLayoutData = new TabPaneLayoutData();
        tabLayoutData.setTitle( Messages.getString( "general" ) );
        ContentPane generalTabPane = new ContentPane();
        generalTabPane.setStyleName( "tab.content" );
        generalTabPane.setLayoutData( tabLayoutData );
        tabPane.add( generalTabPane );
        Grid generalLayoutGrid = new Grid( 2 );
        generalLayoutGrid.setStyleName( "default" );
        generalLayoutGrid.setColumnWidth( 0, new Extent( 20, Extent.PERCENT ) );
        generalLayoutGrid.setColumnWidth( 1, new Extent( 80, Extent.PERCENT ) );
        generalTabPane.add( generalLayoutGrid );
        // name
        Label nameLabel = new Label( Messages.getString( "name" ) );
        nameLabel.setStyleName( "grid.cell" );
        generalLayoutGrid.add( nameLabel );
        nameField = new TextField();
        nameField.setStyleName( "default" );
        nameField.setWidth( new Extent( 100, Extent.PERCENT ) );
        generalLayoutGrid.add( nameField );
        // active
        Label activeLabel = new Label( Messages.getString( "active" ) );
        activeLabel.setStyleName( "grid.cell" );
        generalLayoutGrid.add( activeLabel );
        activeField = new SelectField( MainScreen.LABELS );
        activeField.setStyleName( "default" );
        activeField.setSelectedIndex( 0 );
        activeField.setWidth( new Extent( 10, Extent.EX ) );
        generalLayoutGrid.add( activeField );
        // blocker
        Label blockerLabel = new Label( Messages.getString( "blocker" ) );
        blockerLabel.setStyleName( "grid.cell" );
        generalLayoutGrid.add( blockerLabel );
        blockerField = new SelectField( MainScreen.LABELS );
        blockerField.setStyleName( "default" );
        blockerField.setSelectedIndex( 1 );
        blockerField.setWidth( new Extent( 10, Extent.EX ) );
        generalLayoutGrid.add( blockerField );
        // classname
        Label classnameLabel = new Label( Messages.getString( "classname" ) );
        classnameLabel.setStyleName( "grid.cell" );
        generalLayoutGrid.add( classnameLabel );
        classnameField = new TextField();
        classnameField.setStyleName( "default" );
        classnameField.setWidth( new Extent( 100, Extent.PERCENT ) );
        generalLayoutGrid.add( classnameField );
        // agent
        Label agentLabel = new Label( Messages.getString( "agent" ) );
        agentLabel.setStyleName( "grid.cell" );
        generalLayoutGrid.add( agentLabel );
        agentField = new SelectField();
        agentField.setStyleName( "default" );
        agentField.setWidth( new Extent( 50, Extent.EX ) );
        generalLayoutGrid.add( agentField );

        // add the properties tab
        tabLayoutData = new TabPaneLayoutData();
        tabLayoutData.setTitle( Messages.getString( "properties" ) );
        ContentPane propertiesTabPane = new ContentPane();
        propertiesTabPane.setStyleName( "tab.content" );
        propertiesTabPane.setLayoutData( tabLayoutData );
        tabPane.add( propertiesTabPane );
        propertiesGrid = new Grid( 3 );
        propertiesGrid.setStyleName( "grid.border" );
        propertiesGrid.setColumnWidth( 0, new Extent( 50, Extent.PX ) );
        propertiesGrid.setColumnWidth( 1, new Extent( 50, Extent.PERCENT ) );
        propertiesGrid.setColumnWidth( 2, new Extent( 50, Extent.PERCENT ) );
        propertiesTabPane.add( propertiesGrid );

        // update the window
        update();
    }

    /**
     * Update the window
     */
    public void update()
    {
        // update the content manager name field
        nameField.setText( contentManager.getName() );
        // update the content manager active field
        if ( contentManager.isActive() )
        {
            activeField.setSelectedIndex( 0 );
        }
        else
        {
            activeField.setSelectedIndex( 1 );
        }
        // update the content manager blocker field
        if ( contentManager.isBlocker() )
        {
            blockerField.setSelectedIndex( 0 );
        }
        else
        {
            blockerField.setSelectedIndex( 1 );
        }
        // update the classname field
        classnameField.setText( contentManager.getClassname() );
        // update the agent field
        DefaultListModel agentListModel = (DefaultListModel) agentField.getModel();
        agentListModel.removeAll();
        agentListModel.add( "" );
        try
        {
            Kalumet kalumet = ConfigurationManager.loadStore();
            for ( Iterator agentIterator = kalumet.getAgents().iterator(); agentIterator.hasNext(); )
            {
                Agent agent = (Agent) agentIterator.next();
                agentListModel.add( agent.getId() );
            }
            agentField.setSelectedItem( contentManager.getAgent() );
        }
        catch ( Exception e )
        {
            KalumetConsoleApplication.getApplication().getLogPane().addError(
                Messages.getString( "db.read" ) + ": " + e.getMessage(),
                parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName() );
        }
        // remove all properties grid children
        propertiesGrid.removeAll();
        // add properties grid header
        Label propertyActionHeader = new Label( " " );
        propertyActionHeader.setStyleName( "grid.header" );
        propertiesGrid.add( propertyActionHeader );
        Label propertyNameHeader = new Label( Messages.getString( "name" ) );
        propertyNameHeader.setStyleName( "grid.header" );
        propertiesGrid.add( propertyNameHeader );
        Label propertyValueHeader = new Label( Messages.getString( "value" ) );
        propertyValueHeader.setStyleName( "grid.header" );
        propertiesGrid.add( propertyValueHeader );
        // add properties
        for ( Iterator propertyIterator = contentManager.getProperties().iterator(); propertyIterator.hasNext(); )
        {
            Property property = (Property) propertyIterator.next();
            // row
            Row row = new Row();
            row.setCellSpacing( new Extent( 2 ) );
            row.setInsets( new Insets( 2 ) );
            propertiesGrid.add( row );
            // property copy
            Button copyButton = new Button( Styles.PAGE_COPY );
            copyButton.setToolTipText( Messages.getString( "copy" ) );
            copyButton.setActionCommand( property.getName() );
            copyButton.addActionListener( copyProperty );
            row.add( copyButton );
            // property delete / edit
            if ( parent.getParentPane().getParentPane().getEnvironmentWindow().adminPermission
                || parent.getParentPane().getParentPane().getEnvironmentWindow().jeeApplicationsChangePermission )
            {
                // property edit
                Button editButton = new Button( Styles.ACCEPT );
                editButton.setToolTipText( Messages.getString( "apply" ) );
                editButton.setActionCommand( property.getName() );
                editButton.addActionListener( editProperty );
                row.add( editButton );
                // property delete
                Button deleteButton = new Button( Styles.DELETE );
                deleteButton.setToolTipText( Messages.getString( "delete" ) );
                deleteButton.setActionCommand( property.getName() );
                deleteButton.addActionListener( deleteProperty );
                row.add( deleteButton );
            }
            // property name
            TextField propertyNameField = new TextField();
            propertyNameField.setStyleName( "default" );
            propertyNameField.setWidth( new Extent( 100, Extent.PERCENT ) );
            propertyNameField.setId(
                "propertyname_" + parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName()
                    + "_" + parent.getParentPane().getServerName() + "_" + parent.getParentPane().getApplicationName()
                    + "_" + contentManagerName + "_" + property.getName() );
            propertyNameField.setText( property.getName() );
            propertiesGrid.add( propertyNameField );
            // property value
            TextField propertyValueField = new TextField();
            propertyValueField.setStyleName( "default" );
            propertyValueField.setWidth( new Extent( 100, Extent.PERCENT ) );
            propertyValueField.setId(
                "propertyvalue_" + parent.getParentPane().getParentPane().getEnvironmentWindow().getEnvironmentName()
                    + "_" + parent.getParentPane().getServerName() + "_" + parent.getParentPane().getApplicationName()
                    + "_" + contentManagerName + "_" + property.getName() );
            propertyValueField.setText( property.getValue() );
            propertiesGrid.add( propertyValueField );
        }
        // add the adding property row
        if ( parent.getParentPane().getParentPane().getEnvironmentWindow().adminPermission
            || parent.getParentPane().getParentPane().getEnvironmentWindow().jeeApplicationsChangePermission )
        {
            // row
            Row row = new Row();
            row.setCellSpacing( new Extent( 2 ) );
            row.setInsets( new Insets( 2 ) );
            propertiesGrid.add( row );
            // paste
            Button pasteButton = new Button( Styles.PAGE_PASTE );
            pasteButton.setToolTipText( Messages.getString( "paste" ) );
            pasteButton.addActionListener( pasteProperty );
            row.add( pasteButton );
            // add
            Button addButton = new Button( Styles.ADD );
            addButton.setToolTipText( Messages.getString( "add" ) );
            addButton.addActionListener( createProperty );
            row.add( addButton );
            // new property name
            newPropertyNameField = new TextField();
            newPropertyNameField.setStyleName( "default" );
            newPropertyNameField.setWidth( new Extent( 100, Extent.PERCENT ) );
            propertiesGrid.add( newPropertyNameField );
            // new property value
            newPropertyValueField = new TextField();
            newPropertyValueField.setStyleName( "default" );
            newPropertyValueField.setWidth( new Extent( 100, Extent.PERCENT ) );
            propertiesGrid.add( newPropertyValueField );
        }
    }

}
