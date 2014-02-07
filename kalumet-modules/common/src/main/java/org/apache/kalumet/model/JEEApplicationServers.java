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
package org.apache.kalumet.model;

import org.apache.xerces.dom.CoreDocumentImpl;
import org.apache.xerces.dom.ElementImpl;
import org.w3c.dom.Element;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents the <code>jeeapplicationservers</code> tag in the Kalumet DOM.
 */
public class JEEApplicationServers
    implements Serializable, Cloneable
{

    private static final long serialVersionUID = -4940898204749451109L;

    private boolean cluster;

    private LinkedList jeeApplicationServers;

    public JEEApplicationServers()
    {
        this.jeeApplicationServers = new LinkedList();
    }

    public boolean isCluster()
    {
        return this.cluster;
    }

    public void setCluster( boolean cluster )
    {
        this.cluster = cluster;
    }

    /**
     * Adds a new <code>JEEApplicationServer</code> in the
     * <code>JEEApplicationServers</code> container.
     *
     * @param jeeApplicationServer the <code>JEEApplicationServer</code> to add.
     */
    public void addJEEApplicationServer( JEEApplicationServer jeeApplicationServer )
        throws ModelObjectAlreadyExistsException
    {
        if ( this.getJEEApplicationServer( jeeApplicationServer.getName() ) != null )
        {
            throw new ModelObjectAlreadyExistsException(
                "JEE application server name already exists in the environment." );
        }
        this.jeeApplicationServers.add( jeeApplicationServer );
    }

    /**
     * Gets the <code>JEEApplicationServer</code> list in the
     * <code>JEEApplicationServers</code> container.
     *
     * @return the <code>JEEApplicationServer</code> list.
     */
    public List getJEEApplicationServers()
    {
        return this.jeeApplicationServers;
    }

    /**
     * Overwrites the <code>JEEApplicationServer</code> list in the
     * <code>JEEApplicationServers</code> container.
     *
     * @param jeeApplicationServers the new <code>JEEApplicationServer</code> list.
     */
    public void setJEEApplicationServers( LinkedList jeeApplicationServers )
    {
        this.jeeApplicationServers = jeeApplicationServers;
    }

    /**
     * Gets the <code>JEEApplicationServer</code> identified by a given name in the
     * <code>JEEApplicationServers</code> container.
     *
     * @param name the <code>JEEApplicationServer</code> name.
     * @return the <code>JEEApplicationServer</code> found or null if no found.
     */
    public JEEApplicationServer getJEEApplicationServer( String name )
    {
        for ( Iterator applicationServerIterator = this.getJEEApplicationServers().iterator();
              applicationServerIterator.hasNext(); )
        {
            JEEApplicationServer jeeApplicationServer = (JEEApplicationServer) applicationServerIterator.next();
            if ( jeeApplicationServer.getName().equals( name ) )
            {
                return jeeApplicationServer;
            }
        }
        return null;
    }

    /**
     * @see java.lang.Object#clone()
     */
    public Object clone()
        throws CloneNotSupportedException
    {
        JEEApplicationServers clone = new JEEApplicationServers();
        clone.setCluster( this.isCluster() );
        for ( Iterator applicationServerIterator = this.jeeApplicationServers.iterator();
              applicationServerIterator.hasNext(); )
        {
            JEEApplicationServer jeeApplicationServer = (JEEApplicationServer) applicationServerIterator.next();
            clone.jeeApplicationServers.add( (JEEApplicationServer) jeeApplicationServer.clone() );
        }
        return clone;
    }

    /**
     * Transforms the <code>JEEApplicationServers</code> POJO to a DOM element.
     *
     * @param document the DOM document.
     * @return the DOM element.
     */
    protected Element toDOMElement( CoreDocumentImpl document )
    {
        ElementImpl element = new ElementImpl( document, "jeeapplicationservers" );
        element.setAttribute( "cluster", new Boolean( this.isCluster() ).toString() );
        // add JEE application server child nodes
        for ( Iterator applicationServerIterator = this.getJEEApplicationServers().iterator();
              applicationServerIterator.hasNext(); )
        {
            JEEApplicationServer jeeApplicationServer = (JEEApplicationServer) applicationServerIterator.next();
            element.appendChild( jeeApplicationServer.toDOMElement( document ) );
        }
        return element;
    }

}