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

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A global job listener.
 */
public class SchedulerJobListener implements JobListener {

    private final static transient Logger LOGGER = LoggerFactory.getLogger(SchedulerJobListener.class);

    public SchedulerJobListener() { }

    /**
     * Get the listener name.
     *
     * @return the listener name.
     */
    public String getName() {
        return "AgentJobListener";
    }

    /**
     * @see org.quartz.JobListener#jobToBeExecuted(org.quartz.JobExecutionContext)
     */
    public void jobToBeExecuted(JobExecutionContext jobExecutionContext) {
        LOGGER.info("Executing scheduler job ...");
    }

    /**
     * @see org.quartz.JobListener#jobExecutionVetoed(org.quartz.JobExecutionContext)
     */
    public void jobExecutionVetoed(JobExecutionContext jobExecutionContext) { }

    /**
     * @see org.quartz.JobListener#jobWasExecuted(org.quartz.JobExecutionContext, org.quartz.JobExecutionException)
     */
    public void jobWasExecuted(JobExecutionContext jobExecutionContext, JobExecutionException jobExecutionException) {
        if (jobExecutionException != null) {
            LOGGER.warn("Scheduler job executed with error", jobExecutionException);
        } else {
            LOGGER.info("Scheduler job executed");
        }
    }

}
