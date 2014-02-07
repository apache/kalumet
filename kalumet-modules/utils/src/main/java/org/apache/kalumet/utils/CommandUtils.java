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

import org.apache.kalumet.KalumetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Util to execute system commands.
 */
public class CommandUtils
{

    private final static transient Logger LOGGER = LoggerFactory.getLogger( CommandUtils.class );

    /**
     * Execute a system command and return the output.
     *
     * @param command the system command to execute.
     * @return he command execution output.
     * @throws org.apache.kalumet.KalumetException in case of execution failure.
     */
    public static String execute( String command )
        throws KalumetException
    {
        LOGGER.info( "Executing {}", command );
        String[] shellCommand = null;
        LOGGER.debug( "Create the shell depending the shell" );
        String osName = System.getProperty( "os.name" );
        if ( osName.startsWith( "Windows" ) )
        {
            LOGGER.debug( "MS Windows platform detected" );
            String comSpec = System.getProperty( "ComSpec" );
            if ( comSpec != null )
            {
                LOGGER.debug( "ComSpec MS Windows environment variable found" );
                shellCommand = new String[]{ comSpec, "/C", command };
            }
            else
            {
                LOGGER.debug(
                    "ComSpec MS Windows environment variable is not defined, found the shell command depending of the MS Windows version." );
                if ( osName.startsWith( "Windows 3" ) || osName.startsWith( "Windows 95" ) || osName.startsWith(
                    "Windows 98" ) || osName.startsWith( "Windows ME" ) )
                {
                    LOGGER.debug( "MS Windows 3.1/95/98/Me detected, using: command.com /C " + command );
                    shellCommand = new String[]{ "command.com", "/C", command };
                }
                else
                {
                    LOGGER.debug( "MS Windows NT/XP/Vista detected, using: cmd.exe /C " + command );
                    shellCommand = new String[]{ "cmd.exe", "/C", command };
                }
            }
        }
        else
        {
            LOGGER.debug( "Unix platform detected." );
            String shell = System.getProperty( "SHELL" );
            if ( shell != null )
            {
                LOGGER.debug( "SHELL Unix environment variable is defined, using it: " + shell + " -c " + command );
                shellCommand = new String[]{ shell, "-c", command };
            }
            else
            {
                LOGGER.debug(
                    "SHELL Unix environment variable is not defined, using the default Unix shell: /bin/sh -c "
                        + command );
                shellCommand = new String[]{ "/bin/sh", "-c", command };
            }

        }
        try
        {
            Runtime runtime = Runtime.getRuntime();
            // launch the system command
            Process process = runtime.exec( shellCommand );
            // get the error stream gobbler
            StringBuffer errorBuffer = new StringBuffer();
            StreamGobbler errorGobbler = new StreamGobbler( process.getErrorStream(), errorBuffer );
            // get the output stream gobbler
            StringBuffer outputBuffer = new StringBuffer();
            StreamGobbler outputGobbler = new StreamGobbler( process.getInputStream(), outputBuffer );
            // start both gobblers
            errorGobbler.start();
            outputGobbler.start();
            // wait the end of the process
            int exitValue = process.waitFor();
            if ( exitValue != 0 )
            {
                // an error occurs
                LOGGER.error( "Command {} execution failed: {}", command, errorBuffer.toString() );
                throw new KalumetException( "Command " + command + " execution failed: " + errorBuffer.toString() );
            }
            // command is OK
            LOGGER.info( "Command {} has been executed successfully", command );
            LOGGER.debug( outputBuffer.toString() );
            return outputBuffer.toString();
        }
        catch ( Exception exception )
        {
            LOGGER.error( "Command {} execution failed", command, exception );
            throw new KalumetException( "Command " + command + " execution failed", exception );
        }

    }

}

/**
 * Inner class to glob stream with a thread.
 */
class StreamGobbler
    extends Thread
{

    private final static transient Logger LOGGER = LoggerFactory.getLogger( StreamGobbler.class );

    InputStream in;

    StringBuffer response;

    StreamGobbler( InputStream in, StringBuffer response )
    {
        this.in = in;
        this.response = response;
    }

    /**
     * @see java.lang.Thread#run()
     */
    public void run()
    {
        try
        {
            BufferedReader reader = new BufferedReader( new InputStreamReader( in ) );
            String row = null;
            while ( ( row = reader.readLine() ) != null )
            {
                response.append( row + "\n" );
            }
        }
        catch ( IOException ioException )
        {
            LOGGER.warn( "System command stream gobbler error", ioException );
        }
    }

}
