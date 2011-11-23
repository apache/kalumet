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

import nextapp.echo2.app.ApplicationInstance;
import nextapp.echo2.app.TaskQueueHandle;
import nextapp.echo2.app.Window;
import nextapp.echo2.webcontainer.ContainerContext;
import org.apache.kalumet.console.configuration.ConfigurationManager;
import org.apache.kalumet.console.utils.LdapUtils;
import org.apache.kalumet.console.utils.StackTraceUtils;
import org.apache.kalumet.model.Kalumet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the Kalumet Console Echo2 application.
 */
public class KalumetConsoleApplication extends ApplicationInstance {

    private final static transient Logger LOGGER = LoggerFactory.getLogger(KalumetConsoleApplication.class);

    // the user logged in
    private String userid;
    // the environments pane of the current user
    private EnvironmentsPane environmentsPane;
    // the log pane of the current user
    private LogPane logPane;
    // the current copy object
    private Object copyComponent = null;
    // the task queue handle to manage asynchronous task
    private TaskQueueHandle taskQueue;

    // define the async update interval (60 sec.).
    private final static int ASYNC_UPDATE_INTERVAL = 60000;

    /**
     * Convenience method to return the active Kalumet Console application as
     * <code>KalumetConsoleApplication</code>.
     *
     * @return the active <code>KalumetConsoleApplication</code>.
     */
    public static KalumetConsoleApplication getApplication() {
        return (KalumetConsoleApplication) getActive();
    }

    public String getUserid() {
        return this.userid;
    }

    public void setEnvironmentsPane(EnvironmentsPane environmentsPane) {
        this.environmentsPane = environmentsPane;
    }

    public EnvironmentsPane getEnvironmentsPane() {
        return this.environmentsPane;
    }

    public void setLogPane(LogPane logPane) {
        this.logPane = logPane;
    }

    public LogPane getLogPane() {
        return this.logPane;
    }

    public void setCopyComponent(Object copyComponent) {
        this.copyComponent = copyComponent;
    }

    public Object getCopyComponent() {
        return this.copyComponent;
    }

    public TaskQueueHandle getTaskQueue() {
        return this.taskQueue;
    }

    /**
     * Authenticate an user into Kalumet Console and display the main screen if success.
     *
     * @param userid   the user name.
     * @param password the user password.
     * @return true if the user is identified, false else.
     */
    public boolean connect(String userid, String password) {
        // check the userid and password
        if (userid == null || userid.trim().length() < 1 || password == null || password.trim().length() < 1) {
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new ErrorWindow(Messages.getString("error.authentication"), Messages.getString("error.authentication.badpassword")));
            return false;
        }
        // load the Kalumet configuration
        Kalumet kalumet = null;
        try {
            kalumet = ConfigurationManager.loadStore();
        } catch (Exception e) {
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new ErrorWindow(Messages.getString("db.read"), e.getMessage() + "\n\n" + StackTraceUtils.toString(e.getStackTrace())));
            return false;
        }

        try {
            boolean authenticated = false;
            if (userid.equals("admin")) {
                // if the user is the admin look for the authentication is Kalumet only
                if (kalumet.getSecurity().identifyUser(userid, password)) {
                    authenticated = true;
                }
            } else {
                // it's a "normal" user, check if I need to use a LDAP or not
                if (kalumet.getProperty("LdapAuthentication").getValue().equals("true")) {
                    // bind on a LDAP
                    if (LdapUtils.bind(userid, password)) {
                        authenticated = true;
                    }
                } else {
                    // use Kalumet internal authentication
                    if (kalumet.getSecurity().identifyUser(userid, password)) {
                        authenticated = true;
                    }
                }
            }
            if (authenticated) {
                // store the userid
                this.userid = userid;
                // display the main screen
                this.getDefaultWindow().setContent(new MainScreen());
                // init the application task handler
                this.taskQueue = this.createTaskQueue();
                // define the async update interval
                ContainerContext containerContext = (ContainerContext) getContextProperty(ContainerContext.CONTEXT_PROPERTY_NAME);
                containerContext.setTaskQueueCallbackInterval(this.taskQueue, ASYNC_UPDATE_INTERVAL);
                // the user is authenticated
                return true;
            }
        } catch (Exception e) {
            KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new ErrorWindow(Messages.getString("error.authentication"), e.getMessage() + "\n\n" + StackTraceUtils.toString(e.getStackTrace())));
            return false;
        }
        KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(new ErrorWindow(Messages.getString("error.authentication"), Messages.getString("error.authentication.badpassword")));
        return false;
    }

    /**
     * Disconnect the session and display the authentication screen.
     */
    public void disconnect() {
        this.userid = null;
        this.copyComponent = null;
        // delete the application task queue
        this.removeTaskQueue(this.taskQueue);
        getDefaultWindow().setContent(new LoginScreen());
    }

    /**
     * Initializes the Kalumet Console window.
     *
     * @return the login screen.
     */
    public Window init() {
        // load the default style sheet
        setStyleSheet(Styles.DEFAULT_STYLE_SHEET);

        // create the main window
        Window window = new Window();
        window.setTitle(Messages.getString("kalumet.console"));

        // load the login screen into the window
        window.setContent(new LoginScreen());

        return window;
    }

}
