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
package org.apache.kalumet;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests on the <code>FileManipulator</code>.
 */
public class FileManipulatorTest
{

    private FileManipulator fileManipulator;

    private Tomcat tomcat;

    private int port;

    @Before
    public void setUp()
        throws Exception
    {
        fileManipulator = new FileManipulator();
        tomcat = new Tomcat();
        tomcat.setBaseDir( System.getProperty( "java.io.tmpdir" ) );
        tomcat.setPort( 0 );

        Context context = tomcat.addContext( "", System.getProperty( "java.io.tmpdir" ) );

        Tomcat.addServlet( context, "foo", new SimpleGetServlet() );
        context.addServletMapping( "/repos/*", "foo" );

        tomcat.start();

        this.port = tomcat.getConnector().getLocalPort();

    }

    private static class SimpleGetServlet
        extends HttpServlet
    {
        @Override
        protected void doGet( HttpServletRequest req, HttpServletResponse resp )
            throws ServletException, IOException
        {
            if ( req.getPathInfo().endsWith( "LICENSE" ) )
            {
                String basedir = System.getProperty( "basedir" );
                File f = new File( basedir, "src/test/resources/LICENSE" );
                IOUtils.copy( new FileInputStream( f ), resp.getOutputStream() );
                return;
            }

            resp.setStatus( 404 );
        }
    }

    @After
    public void shutdown()
        throws Exception
    {

        tomcat.stop();

    }

    @Test
    public void testProtocolPrefix()
        throws Exception
    {
        String uri = "http://uri";
        assertEquals( true, FileManipulator.protocolExists( uri ) );
        uri = "uri";
        assertEquals( false, FileManipulator.protocolExists( uri ) );
        uri = "zip:file:/test";
        assertEquals( true, FileManipulator.protocolExists( uri ) );
    }

    @Test
    public void testVFSFormatting()
        throws Exception
    {
        String uri = FileManipulator.format( "http://uri/archive.tar.gz" ) + "!/lib";
        assertEquals( "tgz:http://uri/archive.tar.gz!/lib", uri );
    }

    @Test
    public void testContentCompare()
        throws Exception
    {
        assertEquals( true, fileManipulator.contentEquals(
            "http://localhost:" + port + "/repos/asf/incubator/kalumet/trunk/LICENSE",
            "http://localhost:" + port + "/repos/asf/incubator/kalumet/trunk/LICENSE" ) );
    }

    @Test
    public void testExists()
        throws Exception
    {
        assertEquals( true, fileManipulator.exists(
            "http://localhost:" + port + "/repos/asf/incubator/kalumet/trunk/LICENSE" ) );
        assertEquals( false, fileManipulator.exists(
            "http://localhost:" + port + "/repos/asf/incubator/kalumet/trunk/NOT_FOUND" ) );
    }

}
