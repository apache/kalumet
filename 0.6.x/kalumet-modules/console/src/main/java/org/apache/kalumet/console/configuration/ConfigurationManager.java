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
package org.apache.kalumet.console.configuration;

import org.apache.commons.io.FileUtils;
import org.apache.kalumet.console.configuration.model.KalumetConsole;
import org.apache.kalumet.console.configuration.model.Property;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.model.log.Journal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Calendar;
import java.util.Date;

/**
 * Load and manipulate the Kalumet configuration store.
 */
public class ConfigurationManager
{

  private final static transient Logger LOGGER = LoggerFactory.getLogger( ConfigurationManager.class );

  private final static String KALUMET_CONSOLE_CONFIGURATION_FILE = "/kalumet-console-config.xml";

  private final static String ENVIRONMENT_JOURNAL_FILE_EXTENSION = ".log";

  private static KalumetConsole KALUMET_CONSOLE_CACHE = null;

  private static Kalumet KALUMET_CACHE = null;

  private static int KALUMET_CACHE_TIMEOUT_MINUTES = 5;

  private static Date KALUMET_CACHE_DEPRECATION_DATE = null;

  /**
   * Load the Kalumet Console configuration.
   *
   * @return the Kalumet configuration object.
   */
  public final static KalumetConsole loadConfiguration()
    throws Exception
  {
    if ( KALUMET_CONSOLE_CACHE == null )
    {
      LOGGER.debug( "Loading Apache Kalumet console configuration from {}", KALUMET_CONSOLE_CONFIGURATION_FILE );
      String configurationFile = null;
      try
      {
        configurationFile =
          ConfigurationManager.class.getResource( ConfigurationManager.KALUMET_CONSOLE_CONFIGURATION_FILE ).toString();
      }
      catch ( NullPointerException nullPointerException )
      {
        LOGGER.error( "Apache Kalumet configuration file is not found in the server classpath" );
        throw new IllegalStateException( "Apache Kalumet configuration file is not found in the server classpath" );
      }
      KALUMET_CONSOLE_CACHE = KalumetConsole.digeste( configurationFile );
    }
    return KALUMET_CONSOLE_CACHE;
  }

  /**
   * Get the Kalumet XML configuration location.
   *
   * @return the Kalumet XML configuration location.
   */
  private final static String getStoreFile()
    throws Exception
  {
    KalumetConsole kalumetConsole = ConfigurationManager.loadConfiguration();
    Property kalumetConsoleProperty = kalumetConsole.getProperty( "ConfigurationLocation" );
    if ( kalumetConsoleProperty == null )
    {
      throw new IllegalStateException(
        "The property ConfigurationLocation is not found in the Apache Kalumet Console configuration. This property is required to use Apache Kalumet Console and must contains the location (file: or http:) to the Kalumet configuration store" );
    }
    if ( System.getProperty( "kalumet.home" ) != null )
    {
      return System.getProperty( "kalumet.home" ) + "/" + kalumetConsoleProperty.getValue();
    }
    return kalumetConsoleProperty.getValue();
  }

  /**
   * Load the Kalumet configuration.
   *
   * @return the Kalumet configuration.
   */
  public final static Kalumet loadStore()
    throws Exception
  {
    if ( KALUMET_CACHE == null || KALUMET_CACHE_DEPRECATION_DATE.after( Calendar.getInstance().getTime() ) )
    {
      String kalumetConfigurationPath = ConfigurationManager.getStoreFile();
      File kalumetConfigurationFile = new File( kalumetConfigurationPath );
      if ( !kalumetConfigurationFile.exists() )
      {
        kalumetConfigurationFile.createNewFile();
        // init with a default file
        Kalumet.writeDefault( kalumetConfigurationPath );
      }
      KALUMET_CACHE = Kalumet.digeste( kalumetConfigurationPath );
      // update the deprecation date
      Calendar timeout = Calendar.getInstance();
      timeout.set( Calendar.MINUTE, timeout.get( Calendar.MINUTE ) + KALUMET_CACHE_TIMEOUT_MINUTES );
      KALUMET_CACHE_DEPRECATION_DATE = timeout.getTime();
    }
    return KALUMET_CACHE;
  }

  /**
   * Read the Kalumet configuration store and return the XML raw content.
   *
   * @return the Kalumet XML raw content.
   */
  public final static String readStore()
    throws Exception
  {
    String content = null;
    return FileUtils.readFileToString( new File( ConfigurationManager.getStoreFile() ), "ISO-8859-1" );
  }

  /**
   * Write the Kalumet configuration.
   *
   * @param kalumet the Kalumet configuration object to store.
   */
  public final static void writeStore( Kalumet kalumet )
    throws Exception
  {
    // get the kalumet configuration store location
    String kalumetConfigurationLocation = ConfigurationManager.getStoreFile();
    // write the file
    kalumet.writeXMLFile( kalumetConfigurationLocation );
    // update the cache
    KALUMET_CACHE = kalumet;
    // update the deprecation date
    Calendar timeout = Calendar.getInstance();
    timeout.set( Calendar.MINUTE, timeout.get( Calendar.MINUTE ) + KALUMET_CACHE_TIMEOUT_MINUTES );
    KALUMET_CACHE_DEPRECATION_DATE = timeout.getTime();
  }

  /**
   * Get the Environment Journal location.
   *
   * @param environment the Environment name.
   * @return the Environment Journal location.
   */
  public final static String getEnvironmentJournalFile( String environment )
    throws Exception
  {
    KalumetConsole kalumetConsole = ConfigurationManager.loadConfiguration();
    Property kalumetConsoleProperty = kalumetConsole.getProperty( "JournalsLocation" );
    if ( kalumetConsoleProperty == null )
    {
      throw new IllegalArgumentException(
        "The property JournalsLocation is not found in the Apache Kalumet Console configuration. This property is required to store the environment journals and must contain the directory path for the journal files." );
    }
    String journalPath;
    if ( System.getProperty( "kalumet.home" ) != null )
    {
      journalPath = System.getProperty( "kalumet.home" ) + "/" + kalumetConsoleProperty.getValue() + "/" + environment
        + ConfigurationManager.ENVIRONMENT_JOURNAL_FILE_EXTENSION;
    }
    else
    {
      journalPath =
        kalumetConsoleProperty.getValue() + "/" + environment + ConfigurationManager.ENVIRONMENT_JOURNAL_FILE_EXTENSION;
    }

    File journalDir = new File( journalPath );
    journalDir.getParentFile().mkdirs();
    return journalPath;
  }

  /**
   * Read the environment journal.
   *
   * @param environment the <code>Environment</code> name.
   * @return the environment journal object.
   */
  public final static Journal loadEnvironmentJournal( String environment )
    throws Exception
  {
    KalumetConsole kalumetConsole = ConfigurationManager.loadConfiguration();
    String journalPath = ConfigurationManager.getEnvironmentJournalFile( environment );
    Journal journal = null;
    return Journal.digeste( journalPath );
  }

}
