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

import org.apache.commons.io.FileUtils;
import org.apache.kalumet.FileManipulator;
import org.apache.kalumet.model.Destination;
import org.apache.kalumet.model.Email;
import org.apache.kalumet.model.Environment;
import org.apache.kalumet.model.update.UpdateLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Util to publish update results.
 */
public class PublisherUtils
{

    private final static transient Logger LOGGER = LoggerFactory.getLogger( PublisherUtils.class );

    private final static String XSL_LOCATION = "/templates/publisher.xsl";

    /**
     * Send an e-mail with the result of the update.
     *
     * @param environment the target environment.
     */
    public static void publish( Environment environment )
    {
        LOGGER.debug( "Publish update result for environment {}", environment.getName() );
        try
        {
            LOGGER.debug( "Iterate in the publishers list" );
            for ( Iterator publisherIterator = environment.getPublishers().iterator(); publisherIterator.hasNext(); )
            {
                Email email = (Email) publisherIterator.next();
                LOGGER.debug( "Construct the addresses list" );
                LinkedList addresses = new LinkedList();
                for ( Iterator destinationIterator = email.getDestinations().iterator();
                      destinationIterator.hasNext(); )
                {
                    Destination destination = (Destination) destinationIterator.next();
                    addresses.add( VariableUtils.replace( destination.getAddress(), environment.getVariables() ) );
                }
                LOGGER.debug( "Generate the publish e-mail content" );
                String xslFile = null;
                try
                {
                    xslFile = PublisherUtils.class.getResource( XSL_LOCATION ).toString();
                }
                catch ( Exception e )
                {
                    LOGGER.warn( "Can't load publisher XSL file from {}", XSL_LOCATION, e );
                    xslFile = null;
                }
                String environmentCacheDir = FileManipulator.createEnvironmentCacheDir( environment );
                if ( xslFile != null && xslFile.trim().length() > 0 )
                {
                    LOGGER.debug( "XSL transformation file found, generate and send a HTML e-mail" );
                    // create the XSL transformer
                    String inputFile = environmentCacheDir + "/" + UpdateLog.MAIN_LOG_FILE;
                    LOGGER.debug( "XSL input file: {}", inputFile );
                    String outputFile = environmentCacheDir + "/cache.html";
                    LOGGER.debug( "XSL output file: {}", outputFile );
                    TransformerFactory transformerFactory = TransformerFactory.newInstance();
                    Transformer transformer = transformerFactory.newTransformer( new StreamSource( xslFile ) );
                    transformer.transform( new StreamSource( inputFile ),
                                           new StreamResult( new FileOutputStream( outputFile ) ) );
                    EmailUtils.sendHTMLEmail( VariableUtils.replace( email.getMailhost(), environment.getVariables() ),
                                              VariableUtils.replace( email.getFrom(), environment.getVariables() ),
                                              "Apache Kalumet Report - Environment " + environment.getName(), addresses,
                                              (String) FileUtils.readFileToString( new File( outputFile ), null ) );
                }
                else
                {
                    LOGGER.debug( "No XSL transformation file found, send a text e-mail" );
                    EmailUtils.sendTextEmail( VariableUtils.replace( email.getMailhost(), environment.getVariables() ),
                                              VariableUtils.replace( email.getFrom(), environment.getVariables() ),
                                              "Apache Kalumet Report - Environmment " + environment.getName(),
                                              addresses, (String) FileUtils.readFileToString(
                        new File( environmentCacheDir + "/" + UpdateLog.MAIN_LOG_FILE ) ) );
                }
            }
        }
        catch ( Exception e )
        {
            LOGGER.warn( "Can't publish update report", e );
        }
    }

}
