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
 * Represents the <code>cache</code> tag in the Kalumet XML configuration
 * file.
 */
public class Cache
    implements Serializable, Cloneable, Comparable
{

    private static final long serialVersionUID = 1931779797709903317L;

    private String path;

    public Cache()
    {
    }

    public String getPath()
    {
        return this.path;
    }

    public void setPath( String path )
    {
        this.path = path;
    }

    /**
     * @see java.lang.Object#clone()
     */
    public Object clone()
        throws CloneNotSupportedException
    {
        Cache clone = new Cache();
        clone.setPath( this.getPath() );
        return clone;
    }

    /**
     * Transforms the <code>Cache</code> POJO to a DOM element.
     *
     * @param document the core XML document.
     * @return the DOM element.
     */
    protected Element toDOMElement( CoreDocumentImpl document )
    {
        ElementImpl element = new ElementImpl( document, "cache" );
        element.setAttribute( "path", this.getPath() );
        return element;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo( Object anotherCache )
    {
        return this.getPath().compareTo( ( (Cache) anotherCache ).getPath() );
    }

}