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
 * Represent the <code>destination</code> tag in the Kalumet configuration DOM.
 */
public class Destination
    implements Serializable, Cloneable, Comparable
{

    private static final long serialVersionUID = 1088692045286398988L;

    private String address;

    public Destination()
    {
    }

    public String getAddress()
    {
        return this.address;
    }

    public void setAddress( String address )
    {
        this.address = address;
    }

    /**
     * @see java.lang.Object#clone()
     */
    public Object clone()
        throws CloneNotSupportedException
    {
        Destination clone = new Destination();
        clone.setAddress( this.getAddress() );
        return clone;
    }

    /**
     * Transform the <code>Destination</code> POJO to a DOM element.
     *
     * @param document the DOM document.
     * @return the DOM element.
     */
    protected Element toDOMElement( CoreDocumentImpl document )
    {
        ElementImpl element = new ElementImpl( document, "destination" );
        element.setAttribute( "address", this.getAddress() );
        return element;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo( Object anotherDestination )
    {
        return this.getAddress().compareTo( ( (Destination) anotherDestination ).getAddress() );
    }

}