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

import nextapp.echo2.app.ImageReference;
import nextapp.echo2.app.ResourceImageReference;
import nextapp.echo2.app.StyleSheet;
import nextapp.echo2.app.componentxml.ComponentXmlException;
import nextapp.echo2.app.componentxml.StyleSheetLoader;

/**
 * Define the look'n feel information.
 */
public class Styles
{

    public static final String ICONS_PATH = "/org/apache/kalumet/console/app/icons/";

    public static final String STYLE_PATH = "/org/apache/kalumet/console/app/style/";

    // default style sheet.
    public static final StyleSheet DEFAULT_STYLE_SHEET;

    static
    {
        try
        {
            DEFAULT_STYLE_SHEET = StyleSheetLoader.load( STYLE_PATH + "default.stylesheet",
                                                         Thread.currentThread().getContextClassLoader() );
        }
        catch ( ComponentXmlException exception )
        {
            throw new RuntimeException( exception );
        }
    }

    // icons
    public static final ImageReference ACCEPT = new ResourceImageReference( ICONS_PATH + "accept.png" );

    public static final ImageReference ADD = new ResourceImageReference( ICONS_PATH + "add.png" );

    public static final ImageReference APPLICATION = new ResourceImageReference( ICONS_PATH + "application.png" );

    public static final ImageReference APPLICATION_ADD =
        new ResourceImageReference( ICONS_PATH + "application_add.png" );

    public static final ImageReference APPLICATION_DELETE =
        new ResourceImageReference( ICONS_PATH + "application_delete.png" );

    public static final ImageReference ARROW_DOWN = new ResourceImageReference( ICONS_PATH + "arrow_down.png" );

    public static final ImageReference ARROW_LEFT = new ResourceImageReference( ICONS_PATH + "arrow_left.png" );

    public static final ImageReference ARROW_RIGHT = new ResourceImageReference( ICONS_PATH + "arrow_right.png" );

    public static final ImageReference ARROW_UP = new ResourceImageReference( ICONS_PATH + "arrow_up.png" );

    public static final ImageReference CROSS = new ResourceImageReference( ICONS_PATH + "cross.png" );

    public static final ImageReference BRICK = new ResourceImageReference( ICONS_PATH + "brick.png" );

    public static final ImageReference BRICK_ADD = new ResourceImageReference( ICONS_PATH + "brick_add.png" );

    public static final ImageReference BRICK_DELETE = new ResourceImageReference( ICONS_PATH + "brick_delete.png" );

    public static final ImageReference BOOK_NEXT = new ResourceImageReference( ICONS_PATH + "book_next.png" );

    public static final ImageReference BOOK_PREVIOUS = new ResourceImageReference( ICONS_PATH + "book_previous.png" );

    public static final ImageReference BUILDING = new ResourceImageReference( ICONS_PATH + "building.png" );

    public static final ImageReference CHART_BAR = new ResourceImageReference( ICONS_PATH + "chart_bar.png" );

    public static final ImageReference COG = new ResourceImageReference( ICONS_PATH + "cog.png" );

    public static final ImageReference COG_ADD = new ResourceImageReference( ICONS_PATH + "cog_add.png" );

    public static final ImageReference COG_DELETE = new ResourceImageReference( ICONS_PATH + "cog_delete.png" );

    public static final ImageReference COMPUTER = new ResourceImageReference( ICONS_PATH + "computer.png" );

    public static final ImageReference COMPUTER_EDIT = new ResourceImageReference( ICONS_PATH + "computer_edit.png" );

    public static final ImageReference DATABASE = new ResourceImageReference( ICONS_PATH + "database.png" );

    public static final ImageReference DATABASE_ADD = new ResourceImageReference( ICONS_PATH + "database_add.png" );

    public static final ImageReference DATABASE_GEAR = new ResourceImageReference( ICONS_PATH + "database_gear.png" );

    public static final ImageReference DATABASE_GO = new ResourceImageReference( ICONS_PATH + "database_go.png" );

    public static final ImageReference DATABASE_REFRESH =
        new ResourceImageReference( ICONS_PATH + "database_refresh.png" );

    public static final ImageReference DATABASE_SAVE = new ResourceImageReference( ICONS_PATH + "database_save.png" );

    public static final ImageReference DELETE = new ResourceImageReference( ICONS_PATH + "delete.png" );

    public static final ImageReference DISCONNECT = new ResourceImageReference( ICONS_PATH + "disconnect.png" );

    public static final ImageReference DOOR_IN = new ResourceImageReference( ICONS_PATH + "door_in.png" );

    public static final ImageReference DRIVE_WEB = new ResourceImageReference( ICONS_PATH + "drive_web.png" );

    public static final ImageReference ERROR = new ResourceImageReference( ICONS_PATH + "error.png" );

    public static final ImageReference EXCLAMATION = new ResourceImageReference( ICONS_PATH + "exclamation.png" );

    public static final ImageReference FLAG_GREEN = new ResourceImageReference( ICONS_PATH + "flag_green.png" );

    public static final ImageReference FLAG_RED = new ResourceImageReference( ICONS_PATH + "flag_red.png" );

    public static final ImageReference FOLDER_EXPLORE = new ResourceImageReference( ICONS_PATH + "folder_explore.png" );

    public static final ImageReference GROUP_ADD = new ResourceImageReference( ICONS_PATH + "group_add.png" );

    public static final ImageReference GROUP_DELETE = new ResourceImageReference( ICONS_PATH + "group_delete.png" );

    public static final ImageReference GROUP_EDIT = new ResourceImageReference( ICONS_PATH + "group_edit.png" );

    public static final ImageReference GROUP = new ResourceImageReference( ICONS_PATH + "group.png" );

    public static final ImageReference INFORMATION = new ResourceImageReference( ICONS_PATH + "information.png" );

    public static final ImageReference LIGHTBULB = new ResourceImageReference( ICONS_PATH + "lightbulb.png" );

    public static final ImageReference LIGHTBULB_OFF = new ResourceImageReference( ICONS_PATH + "lightbulb_off.png" );

    public static final ImageReference LORRY = new ResourceImageReference( ICONS_PATH + "lorry.png" );

    public static final ImageReference LOCK = new ResourceImageReference( ICONS_PATH + "lock.png" );

    public static final ImageReference PAGE_COPY = new ResourceImageReference( ICONS_PATH + "page_copy.png" );

    public static final ImageReference PAGE_PASTE = new ResourceImageReference( ICONS_PATH + "page_paste.png" );

    public static final ImageReference PLUGIN = new ResourceImageReference( ICONS_PATH + "plugin.png" );

    public static final ImageReference PLUGIN_DISABLED =
        new ResourceImageReference( ICONS_PATH + "plugin_disabled.png" );

    public static final ImageReference SCRIPT = new ResourceImageReference( ICONS_PATH + "script.png" );

    public static final ImageReference SERVER_ADD = new ResourceImageReference( ICONS_PATH + "server_add.png" );

    public static final ImageReference SERVER_DELETE = new ResourceImageReference( ICONS_PATH + "server_delete.png" );

    public static final ImageReference STOP = new ResourceImageReference( ICONS_PATH + "stop.png" );

    public static final ImageReference TIME = new ResourceImageReference( ICONS_PATH + "time.png" );

    public static final ImageReference USER_ADD = new ResourceImageReference( ICONS_PATH + "user_add.png" );

    public static final ImageReference USER_DELETE = new ResourceImageReference( ICONS_PATH + "user_delete.png" );

    public static final ImageReference USER_EDIT = new ResourceImageReference( ICONS_PATH + "user_edit.png" );

    public static final ImageReference USER = new ResourceImageReference( ICONS_PATH + "user.png" );

    public static final ImageReference WORLD = new ResourceImageReference( ICONS_PATH + "world.png" );

    public static final ImageReference WRENCH = new ResourceImageReference( ICONS_PATH + "wrench.png" );

}
