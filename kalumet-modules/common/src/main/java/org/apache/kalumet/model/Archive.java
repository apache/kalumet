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

/**
 * Represents the <code>archive</code> tag in the Kalumet DOM.
 */
public class Archive
    implements Serializable, Cloneable, Comparable
{

    private static final long serialVersionUID = 7125281479795326133L;

    private String name;

    private String uri;

    private String classloaderorder;

    private String classloaderpolicy;

    private String vhost;

    private String path;

    private String context;

    private boolean active;

    private boolean blocker;

    private String agent;

    public Archive()
    {
    }

    public String getName()
    {
        return this.name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getUri()
    {
        return this.uri;
    }

    public void setUri( String uri )
    {
        this.uri = uri;
    }

    public String getClassloaderorder()
    {
        return classloaderorder;
    }

    public void setClassloaderorder( String classloaderorder )
    {
        this.classloaderorder = classloaderorder;
    }

    public String getClassloaderpolicy()
    {
        return classloaderpolicy;
    }

    public void setClassloaderpolicy( String classloaderpolicy )
    {
        this.classloaderpolicy = classloaderpolicy;
    }

    public String getVhost()
    {
        return this.vhost;
    }

    public void setVhost( String vhost )
    {
        this.vhost = vhost;
    }

    public String getPath()
    {
        return this.path;
    }

    public void setPath( String path )
    {
        this.path = path;
    }

    public String getContext()
    {
        return this.context;
    }

    public void setContext( String context )
    {
        this.context = context;
    }

    public boolean isActive()
    {
        return this.active;
    }

    public void setActive( boolean active )
    {
        this.active = active;
    }

    public boolean isBlocker()
    {
        return this.blocker;
    }

    public void setBlocker( boolean blocker )
    {
        this.blocker = blocker;
    }

    public String getAgent()
    {
        return agent;
    }

    public void setAgent( String agent )
    {
        this.agent = agent;
    }

    /**
     * @see java.lang.Object#clone()
     */
    public Object clone()
        throws CloneNotSupportedException
    {
        Archive clone = new Archive();
        clone.setName( this.getName() );
        clone.setUri( this.getUri() );
        clone.setClassloaderorder( this.getClassloaderorder() );
        clone.setClassloaderpolicy( this.getClassloaderpolicy() );
        clone.setVhost( this.getVhost() );
        clone.setPath( this.getPath() );
        clone.setContext( this.getContext() );
        clone.setActive( this.isActive() );
        clone.setBlocker( this.isBlocker() );
        clone.setAgent( this.getAgent() );
        return clone;
    }

    /**
     * Transforms the <code>Archive</code> specific POJO to a DOM element.
     *
     * @param document the DOM document.
     * @return the DOM element.
     */
    protected Element toDOMElement( CoreDocumentImpl document )
    {
        ElementImpl element = new ElementImpl( document, "archive" );
        element.setAttribute( "name", this.getName() );
        element.setAttribute( "uri", this.getUri() );
        element.setAttribute( "classloaderorder", this.getClassloaderorder() );
        element.setAttribute( "classloaderpolicy", this.getClassloaderpolicy() );
        element.setAttribute( "vhost", this.getVhost() );
        element.setAttribute( "path", this.getPath() );
        element.setAttribute( "context", this.getContext() );
        element.setAttribute( "active", new Boolean( this.isActive() ).toString() );
        element.setAttribute( "blocker", new Boolean( this.isBlocker() ).toString() );
        element.setAttribute( "agent", this.getAgent() );
        return element;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo( Object anotherArchive )
    {
        return this.getName().compareTo( ( (Archive) anotherArchive ).getName() );
    }

}