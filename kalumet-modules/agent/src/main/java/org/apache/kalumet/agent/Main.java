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
package org.apache.kalumet.agent;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.utils.AgentUtils;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class for Kalumet agent launch.
 */
public final class Main
{

    private final static transient Logger LOGGER = LoggerFactory.getLogger( Main.class );

    /**
     * Main agent launcher.
     *
     * @param args
     */
    public final static void main( String[] args )
    {
        System.out.println( "Starting Apache Kalumet agent " + AgentUtils.getVersion() );
        System.out.println();

        Options options = new Options();
        Option config = OptionBuilder.withArgName( "config" ) //
            .hasArg() //
            .withDescription( "The location URL (local: or http:) to the Kalumet configuration (e.g. http://hostname/kalumet/ConfigurationWrapper)" ) //
            .isRequired() //
            .create( "config" );
        options.addOption( config );

        Option agentid = OptionBuilder.withArgName( "id" ) //
            .hasArg() //
            .withDescription( "The Kalumet agent identification as defined in the configuration" ) //
            .isRequired() //
            .create( "id" );
        options.addOption( agentid );
        CommandLineParser parser = new GnuParser();
        CommandLine cmd = null;

        try
        {
            // parse the command line
            cmd = parser.parse( options, args );
        }
        catch ( ParseException parseException )
        {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "Apache Kalumet", options );
            System.exit( 1 );
        }

        String configLocation = cmd.getOptionValue( "config" );
        LOGGER.info( "Loading configuration from {}", configLocation );
        String agentId = cmd.getOptionValue( "id" );
        LOGGER.info( "Agent ID is {}", agentId );

        // parse the Kalumet configuration to get the the agent cron
        Kalumet kalumet = null;
        String cronString = null;
        try
        {
            kalumet = Kalumet.digeste( configLocation );
        }
        catch ( Exception e )
        {
            LOGGER.error( "Can't load Apache Kalumet configuration", e );
            System.err.println( "Can't load Apache Kalumet configuration" );
            e.printStackTrace();
            System.exit( 1 );
        }

        if ( kalumet.getAgent( agentId ) == null )
        {
            LOGGER.error( "Agent ID {} is not found in the Kalumet configuration", agentId );
            System.err.println( "Agent ID " + agentId + " is not found in the Kalumet configuration" );
            System.exit( 1 );
        }

        // init the agent configuration store
        Configuration.CONFIG_LOCATION = configLocation;
        Configuration.AGENT_ID = agentId;

        cronString = kalumet.getAgent( agentId ).getCron();
        LOGGER.debug( "Cron definition: " + cronString );

        // start the WS server
        try
        {
            int port = kalumet.getAgent( agentId ).getPort();
            WsServer wsServer = new WsServer( port, "/apache-kalumet.wsdd" );
            wsServer.start();
            LOGGER.info( "WS server started on {}", port );
        }
        catch ( Exception e )
        {
            LOGGER.error( "Can't start WS server", e );
            System.err.println( "Can't start WS server" );
            e.printStackTrace();
            System.exit( 2 );
        }

        // start the scheduler
        try
        {
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            Scheduler scheduler = schedulerFactory.getScheduler();
            scheduler.addGlobalJobListener( new SchedulerJobListener() );
            LOGGER.debug( "Scheduler job listener plugged" );
            scheduler.start();
            JobDetail job = new JobDetail( "Apache Kalumet agent job", Scheduler.DEFAULT_GROUP, SchedulerJob.class );
            CronTrigger cron = new CronTrigger( "Apache Kalumet agent trigger", Scheduler.DEFAULT_GROUP, cronString );
            LOGGER.debug( "{} cron created", cronString );
            scheduler.scheduleJob( job, cron );
            LOGGER.info( "Scheduler started with {} trigger", cronString );
        }
        catch ( Exception e )
        {
            LOGGER.error( "Can't start scheduler", e );
            System.err.println( "Can't start scheduler" );
            e.printStackTrace();
            System.exit( 3 );
        }

        LOGGER.info( "Apache Kalumet agent started" );
    }

}
