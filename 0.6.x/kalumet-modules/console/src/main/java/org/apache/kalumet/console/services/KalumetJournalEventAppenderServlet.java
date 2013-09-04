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
package org.apache.kalumet.console.services;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.kalumet.console.configuration.ConfigurationManager;
import org.apache.kalumet.model.log.Event;
import org.apache.kalumet.model.log.Journal;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

/**
 * This servlet appends a new event in a environment journal file.
 */
public class KalumetJournalEventAppenderServlet
  extends HttpServlet
{

  private static final long serialVersionUID = -8539990024742515658L;

  public void doGet( HttpServletRequest req, HttpServletResponse resp )
    throws ServletException, IOException
  {
    // get the parameter
    String environment = req.getParameter( "environment" );
    String author = req.getParameter( "author" );
    String severity = req.getParameter( "severity" );
    String event = req.getParameter( "event" );
    // check if the parameters are corrects
    if ( environment == null || environment.trim().length() < 1 || author == null || author.trim().length() < 1
      || severity == null || severity.trim().length() < 1 || event == null || event.trim().length() < 1 )
    {
      throw new ServletException(
        "The Apache Kalumet Console journal event appender needs the environment, author, severity and event HTTP parameters." );
    }
    // load the environment journal
    Journal journal = null;
    try
    {
      journal = ConfigurationManager.loadEnvironmentJournal( environment );
    }
    catch ( Exception e )
    {
      throw new ServletException( "Can't read the environment journal", e );
    }
    // create a new event
    Event journalEvent = new Event();
    journalEvent.setDate( ( (FastDateFormat) DateFormatUtils.ISO_DATETIME_FORMAT ).format( new Date() ) );
    journalEvent.setSeverity( severity );
    journalEvent.setAuthor( author );
    journalEvent.setContent( event );
    journal.addEvent( journalEvent );
    // save the journal
    try
    {
      journal.writeXMLFile( ConfigurationManager.getEnvironmentJournalFile( environment ) );
    }
    catch ( Exception e )
    {
      throw new ServletException( "Can't write the environment journal", e );
    }
    // send OK
    PrintWriter writer = resp.getWriter();
    writer.print( "Environment " + environment + " journal updated" );
    writer.flush();
    writer.close();
  }

  public void doPost( HttpServletRequest req, HttpServletResponse resp )
    throws ServletException, IOException
  {
    doGet( req, resp );
  }

}
