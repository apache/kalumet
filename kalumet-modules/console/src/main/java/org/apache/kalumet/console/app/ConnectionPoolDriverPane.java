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
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.Grid;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.SelectField;
import nextapp.echo2.app.TextField;
import org.apache.commons.lang.StringUtils;

/**
 * JDBC connection pool driver tab <code>ContentPane</code>
 */
public class ConnectionPoolDriverPane
    extends ContentPane
{

    // constants
    private static String[] DRIVERS =
        new String[]{ Messages.getString( "jdbc.driver.oracle.thin" ), Messages.getString( "jdbc.driver.oracle.xa" ),
            Messages.getString( "jdbc.driver.ibm.db2" ), Messages.getString( "jdbc.driver.mysql" ),
            Messages.getString( "jdbc.driver.postgresql" ) };

    private static String[] HELPERS = new String[]{ " ", Messages.getString( "jdbc.helper.websphere.generic" ),
        Messages.getString( "jdbc.helper.websphere.oracle" ) };

    // attributes
    private ConnectionPoolWindow parent;

    private SelectField driverField;

    private SelectField helperField;

    private TextField classpathField;

    /**
     * Create a new <code>JDBCConnectionPoolDriverTabPane</code>.
     *
     * @param parent the parent <code>JDBCConnectionPoolWindow</code>.
     */
    public ConnectionPoolDriverPane( ConnectionPoolWindow parent )
    {
        super();
        setStyleName( "tab;content" );

        // update parent
        this.parent = parent;

        // add the driver layout grid
        Grid layout = new Grid( 2 );
        layout.setStyleName( "default" );
        layout.setColumnWidth( 0, new Extent( 20, Extent.PERCENT ) );
        layout.setColumnWidth( 1, new Extent( 80, Extent.PERCENT ) );
        add( layout );

        // add the driver field
        Label driverLabel = new Label( Messages.getString( "jdbc.driver" ) );
        driverLabel.setStyleName( "grid.cell" );
        layout.add( driverLabel );
        driverField = new SelectField( ConnectionPoolDriverPane.DRIVERS );
        driverField.setStyleName( "default" );
        driverField.setWidth( new Extent( 50, Extent.EX ) );
        driverField.setSelectedIndex( 0 );
        layout.add( driverField );

        // add the helper field
        Label helperLabel = new Label( Messages.getString( "jdbc.helper" ) );
        helperLabel.setStyleName( "grid.cell" );
        layout.add( helperLabel );
        helperField = new SelectField( ConnectionPoolDriverPane.HELPERS );
        helperField.setStyleName( "default" );
        helperField.setWidth( new Extent( 50, Extent.EX ) );
        helperField.setSelectedIndex( 0 );
        layout.add( helperField );

        // add the classpath field
        Label classpathLabel = new Label( Messages.getString( "classpath" ) );
        classpathLabel.setStyleName( "grid.cell" );
        layout.add( classpathLabel );
        classpathField = new TextField();
        classpathField.setStyleName( "default" );
        classpathField.setWidth( new Extent( 100, Extent.PERCENT ) );
        layout.add( classpathField );

        // update the pane
        update();
    }

    /**
     * Update the pane
     */
    public void update()
    {
        // update the JDBC connection pool driver field
        if ( StringUtils.containsIgnoreCase( parent.getConnectionPool().getDriver(), "oracle" ) )
        {
            if ( StringUtils.containsIgnoreCase( parent.getConnectionPool().getDriver(), "xa" ) )
            {
                driverField.setSelectedIndex( 1 );
            }
            else
            {
                driverField.setSelectedIndex( 0 );
            }
        }
        if ( StringUtils.containsIgnoreCase( parent.getConnectionPool().getDriver(), "db2" ) )
        {
            driverField.setSelectedIndex( 2 );
        }
        if ( StringUtils.containsIgnoreCase( parent.getConnectionPool().getDriver(), "mysql" ) )
        {
            driverField.setSelectedIndex( 3 );
        }
        if ( StringUtils.containsIgnoreCase( parent.getConnectionPool().getDriver(), "postgres" ) )
        {
            driverField.setSelectedIndex( 4 );
        }
        // update the JDBC connection pool helper field
        helperField.setSelectedIndex( 0 );
        if ( StringUtils.containsIgnoreCase( parent.getConnectionPool().getHelperclass(), "generic" ) )
        {
            helperField.setSelectedIndex( 1 );
        }
        if ( StringUtils.containsIgnoreCase( parent.getConnectionPool().getHelperclass(), "oracle" ) )
        {
            helperField.setSelectedIndex( 2 );
        }
        // update the JDBC connection pool classpath field
        classpathField.setText( parent.getConnectionPool().getClasspath() );
    }

    public SelectField getDriverField()
    {
        return this.driverField;
    }

    public SelectField getHelperField()
    {
        return this.helperField;
    }

    public TextField getClasspathField()
    {
        return this.classpathField;
    }

}