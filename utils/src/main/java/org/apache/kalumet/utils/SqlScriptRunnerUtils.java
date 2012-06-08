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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SqlScriptRunnerUtils
{

  private final static transient Logger LOGGER = LoggerFactory.getLogger( SqlScriptRunnerUtils.class );

  private static final String DEFAULT_DELIMITER = ";";

  private Connection connection;

  private boolean stopOnError = false;

  private boolean autoCommit = false;

  private String delimiter = DEFAULT_DELIMITER;

  private boolean fullLineDelimiter = false;

  public SqlScriptRunnerUtils( Connection connection )
  {
    this.connection = connection;
  }

  public void setStopOnError( boolean stopOnError )
  {
    this.stopOnError = stopOnError;
  }

  public void setAutoCommit( boolean autoCommit )
  {
    this.autoCommit = autoCommit;
  }

  public void setDelimiter( String delimiter )
  {
    this.delimiter = delimiter;
  }

  public void setFullLineDelimiter( boolean fullLineDelimiter )
  {
    this.fullLineDelimiter = fullLineDelimiter;
  }

  public void runScript( Reader reader )
    throws Exception
  {
    runScriptWithConnection( connection, reader );
  }

  public void closeConnection()
  {
    try
    {
      connection.close();
    }
    catch ( Exception e )
    {
      // ignore
    }
  }

  /**
   * Execute an SQL script (read in using the Reader parameter) using the connection passed in.
   *
   * @param conn   the connection to use for the script.
   * @param reader the source of the script.
   * @throws java.sql.SQLException if any SQL errors occur.
   * @throws java.io.IOException   if there is an error reading from the Reader.
   */
  private void runScriptWithConnection( Connection conn, Reader reader )
    throws Exception
  {
    StringBuffer command = null;
    try
    {
      BufferedReader lineReader = new BufferedReader( reader );
      String line;
      while ( ( line = lineReader.readLine() ) != null )
      {
        if ( command == null )
        {
          command = new StringBuffer();
        }
        String trimmedLine = line.trim();
        if ( trimmedLine.length() < 1 )
        {
          // do nothing
        }
        else if ( trimmedLine.startsWith( "//" ) || trimmedLine.startsWith( "--" ) )
        {
          LOGGER.info( trimmedLine );
        }
        else if ( !fullLineDelimiter && trimmedLine.endsWith( delimiter ) || fullLineDelimiter && trimmedLine.equals(
          delimiter ) )
        {
          command.append( line.substring( 0, line.lastIndexOf( delimiter ) ) );
          command.append( " " );
          Statement statement = conn.createStatement();

          LOGGER.info( command.toString() );

          if ( stopOnError )
          {
            statement.execute( command.toString() );
          }
          else
          {
            try
            {
              statement.execute( command.toString() );
            }
            catch ( SQLException e )
            {
              e.fillInStackTrace();
              LOGGER.warn( "Error executing SQL {}", command );
            }
          }

          if ( autoCommit && !conn.getAutoCommit() )
          {
            conn.commit();
          }

          command = null;
          try
          {
            statement.close();
          }
          catch ( Exception e )
          {
            // ignore to workaround a bug in Jakarta DBCP
          }
          Thread.yield();
        }
        else
        {
          command.append( line );
          command.append( " " );
        }
      }
      if ( !autoCommit && !conn.getAutoCommit() )
      {
        conn.commit();
      }
    }
    catch ( Exception e )
    {
      LOGGER.error( "Error executing SQL {}", command, e );
      throw e;
    }
    finally
    {
      conn.rollback();
    }
  }

}

