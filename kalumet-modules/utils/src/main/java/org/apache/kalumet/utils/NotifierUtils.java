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

import org.apache.kalumet.model.Destination;
import org.apache.kalumet.model.Email;
import org.apache.kalumet.model.Environment;
import org.apache.kalumet.model.Notifiers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Util class to notify that an update will start.
 */
public class NotifierUtils
{

    private final static transient Logger LOGGER = LoggerFactory.getLogger( NotifierUtils.class );

    /**
     * Wait the count down and send e-mail to notifiers.
     *
     * @param environment the target environment.
     */
    public static void waitAndNotify( Environment environment )
    {
        Notifiers notifiers = environment.getNotifiers();
        LOGGER.debug(
            "Send e-mail to notify people for the update of the environment {} and wait the count down ({} minute(s)).",
            environment.getName(), notifiers.getCountdown() );
        LOGGER.debug( "Construct the e-mail content." );
        LOGGER.debug( "Load the e-mail template." );
        InputStreamReader notifyTemplate =
            new InputStreamReader( NotifierUtils.class.getResourceAsStream( "/templates/notifier.html" ) );
        Object[] values = new Object[2];
        values[0] = environment.getName();
        values[1] = new Integer( notifiers.getCountdown() ).toString();
        String notifyContent = null;
        try
        {
            notifyContent = EmailUtils.format( notifyTemplate, values );
            // send the notification
            LOGGER.debug( "Send the notification." );
            LOGGER.debug( "Iterator on the notifier list." );
            for ( Iterator notifierIterator = notifiers.getNotifiers().iterator(); notifierIterator.hasNext(); )
            {
                Email email = (Email) notifierIterator.next();
                LOGGER.debug( "Construct the address list." );
                LinkedList addresses = new LinkedList();
                for ( Iterator destinationIterator = email.getDestinations().iterator();
                      destinationIterator.hasNext(); )
                {
                    Destination destination = (Destination) destinationIterator.next();
                    addresses.add( VariableUtils.replace( destination.getAddress(), environment.getVariables() ) );
                }
                EmailUtils.sendHTMLEmail( VariableUtils.replace( email.getMailhost(), environment.getVariables() ),
                                          VariableUtils.replace( email.getFrom(), environment.getVariables() ),
                                          "Apache Kalumet Notification - Environment " + environment.getName(),
                                          addresses, notifyContent );
            }
        }
        catch ( Exception e )
        {
            LOGGER.warn( "Can't send notification.", e );
        }
        LOGGER.debug( "Waiting for the countdown (" + notifiers.getCountdown() + " minute(s)) ..." );
        try
        {
            Thread.sleep( notifiers.getCountdown() * 60 * 1000 );
        }
        catch ( InterruptedException interruptedException )
        {
            LOGGER.warn( "Can't process notification count down.", interruptedException );
        }
    }

}
