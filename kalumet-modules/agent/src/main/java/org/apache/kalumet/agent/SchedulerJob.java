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

import org.apache.kalumet.agent.updater.EnvironmentUpdater;
import org.apache.kalumet.model.Environment;
import org.apache.kalumet.model.Kalumet;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * Kalumet job in the quartz scheduler.
 */
public class SchedulerJob
    implements StatefulJob
{

    private final static transient Logger LOGGER = LoggerFactory.getLogger( SchedulerJob.class );

    public SchedulerJob()
    {
    }

    /**
     * Launch the main agent job.
     *
     * @param path    the Kalumet configuration file location.
     * @param agentId the Kalumet agent ID.
     */
    public static void perform( String path, String agentId )
    {
        Kalumet kalumet = null;
        try
        {
            LOGGER.debug( "Loading Kalumet configuration" );
            kalumet = Kalumet.digeste( path );
        }
        catch ( Exception e )
        {
            LOGGER.error( "Can't load Apache Kalumet configuration from {}", path, e );
            throw new RuntimeException( "Can't load Apache Kalumet configuration from " + path, e );
        }
        // loop to update all environments managed by the agent
        for ( Iterator environmentIterator = kalumet.getEnvironmentsByAgent( agentId ).iterator();
              environmentIterator.hasNext(); )
        {
            try
            {
                EnvironmentUpdater.update( (Environment) environmentIterator.next() );
            }
            catch ( Exception e )
            {
                // ignore
            }
        }
    }

    /**
     * @see org.quartz.StatefulJob#execute(org.quartz.JobExecutionContext)
     */
    public void execute( JobExecutionContext context )
        throws JobExecutionException
    {
        SchedulerJob.perform( Configuration.CONFIG_LOCATION, Configuration.AGENT_ID );
    }

}