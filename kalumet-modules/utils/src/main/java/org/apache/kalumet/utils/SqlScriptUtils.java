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
package org.apache.kalumet.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.kalumet.KalumetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;

/**
 * SQL script execution utils.
 */
public class SqlScriptUtils
{

    private static final transient Logger LOGGER = LoggerFactory.getLogger( SqlScriptUtils.class );

    /**
     * Execute a SQL script using a system command.
     *
     * @param path    the SQL script path.
     * @param command the system command to use for SQL script execution.
     * @return the SQL script execution output.
     * @throws KalumetException in case of error during the SQL script execution.
     */
    public static String executeUsingCommand( String path, String command )
        throws KalumetException
    {
        LOGGER.info( "Executing SQL script {} using command {}", path, command );
        // replace %s by the SQL script path in the command
        LOGGER.debug( "Replacing %s by the SQL script path in the command" );
        String execCommand = StringUtils.replace( command, "%s", path );
        LOGGER.debug( "Executing the SQL command" );
        String output = null;
        try
        {
            output = CommandUtils.execute( execCommand );
        }
        catch ( KalumetException kalumetException )
        {
            LOGGER.error( "SQL script {} execution failed", path, kalumetException );
            throw new KalumetException( "SQL script " + path + " execution failed", kalumetException );
        }
        return output;
    }

    /**
     * Execute a SQL script using a JDBC connection.
     *
     * @param path     the SQL script path.
     * @param driver   the JDBC connection driver class name.
     * @param user     the JDBC connection user name.
     * @param password the JDBC connection user password.
     * @param url      the JDBC connection URL.
     * @throws KalumetException in case of error during the SQL script execution.
     */
    public static void executeUsingJdbc( String path, String driver, String user, String password, String url )
        throws KalumetException
    {
        LOGGER.info( "Executing SQL script {} using JDBC connection {}", path, url );
        Connection connection = null;
        try
        {
            // creates JDBC connection.
            LOGGER.debug( "Creating JDBC connection" );
            connection = SqlScriptUtils.getConnection( driver, user, password, url );
            // creates the SQL script buffered reader.
            LOGGER.debug( "Creating the SQL script buffered reader" );
            BufferedReader reader =
                new BufferedReader( new InputStreamReader( new BufferedInputStream( new FileInputStream( path ) ) ) );
            // uses the SQL script runner
            LOGGER.debug( "Call SQL script runner" );
            SqlScriptRunnerUtils sqlScriptRunner = new SqlScriptRunnerUtils( connection );
            sqlScriptRunner.setAutoCommit( true );
            sqlScriptRunner.setStopOnError( true );
            sqlScriptRunner.runScript( reader );
        }
        catch ( Exception e )
        {
            LOGGER.error( "SQL script {} execution failed", path, e );
            throw new KalumetException( "SQL script " + path + " execution failed", e );
        }
        finally
        {
            if ( connection != null )
            {
                try
                {
                    connection.close();
                }
                catch ( Exception e )
                {
                    LOGGER.warn( "Can't close the JDBC connection", e );
                }
            }
        }
    }

    /**
     * Create a JDBC connection.
     *
     * @param driver   the JDBC driver class name.
     * @param user     the JDBC user name.
     * @param password the JDBC user password.
     * @param url      the JDBC URL.
     * @return the JDBC connection.
     */
    private static Connection getConnection( String driver, String user, String password, String url )
        throws KalumetException
    {
        try
        {
            Class.forName( driver );
            return DriverManager.getConnection( url, user, password );
        }
        catch ( Exception e )
        {
            LOGGER.error( "Can't create JDBC connection", e );
            throw new KalumetException( "Can't create JDBC connection", e );
        }
    }

}
