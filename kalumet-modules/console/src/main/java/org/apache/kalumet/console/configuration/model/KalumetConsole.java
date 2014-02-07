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
package org.apache.kalumet.console.configuration.model;

import org.apache.commons.digester.Digester;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents the <code>kalumet-console</code> root tag of the
 * Kalumet Console configuration.
 */
public class KalumetConsole
{

    private LinkedList properties;

    public KalumetConsole()
    {
        this.properties = new LinkedList();
    }

    /**
     * Add a new property.
     *
     * @param property the <code>Property</code> to add.
     */
    public void addProperty( Property property )
        throws Exception
    {
        if ( this.getProperty( property.getName() ) != null )
        {
            throw new IllegalArgumentException(
                "The property name " + property.getName() + " already exists in the console configuration" );
        }
        this.properties.add( property );
    }

    /**
     * Return the property list.
     *
     * @return the <code>Property</code> list.
     */
    public List getProperties()
    {
        return this.properties;
    }

    /**
     * Return the property identified by a given name.
     *
     * @param name the property name.
     * @return the <code>Property</code> found or null if no <code>Property</code> found.
     */
    public Property getProperty( String name )
    {
        for ( Iterator propertyIterator = this.getProperties().iterator(); propertyIterator.hasNext(); )
        {
            Property property = (Property) propertyIterator.next();
            if ( property.getName().equals( name ) )
            {
                return property;
            }
        }
        return null;
    }

    /**
     * Digest the Kalumet Console configuration.
     *
     * @param path the Kalumet Console location.
     * @return the Kalumet Console configuration object.
     */
    public static KalumetConsole digeste( String path )
        throws Exception
    {
        KalumetConsole kalumetConsole = null;
        try
        {
            Digester digester = new Digester();
            digester.setValidating( false );
            digester.addObjectCreate( "kalumet-console",
                                      "org.apache.kalumet.console.configuration.model.KalumetConsole" );

            digester.addObjectCreate( "kalumet-console/property",
                                      "org.apache.kalumet.console.configuration.model.Property" );
            digester.addSetProperties( "kalumet-console/property" );

            digester.addSetNext( "kalumet-console/property", "addProperty",
                                 "org.apache.kalumet.console.configuration.model.Property" );

            kalumetConsole = (KalumetConsole) digester.parse( path );
        }
        catch ( Exception e )
        {
            IOException ioe = new IOException( "Can't read the Apache Kalumet console configuration" );
            ioe.initCause( e );
            throw ioe;
        }
        return kalumetConsole;
    }

}
