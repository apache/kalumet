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
 * Represent the <code>mapping</code> tag in the Kalumet configuration DOM.
 */
public class Mapping
    implements Serializable, Cloneable, Comparable
{

    private static final long serialVersionUID = 6313869273116904013L;

    private String key;

    private String value;

    public Mapping()
    {
    }

    public String getKey()
    {
        return this.key;
    }

    public void setKey( String key )
    {
        this.key = key;
    }

    public String getValue()
    {
        return this.value;
    }

    public void setValue( String value )
    {
        this.value = value;
    }

    /**
     * @see java.lang.Object#clone()
     */
    public Object clone()
        throws CloneNotSupportedException
    {
        Mapping clone = new Mapping();
        clone.setKey( this.getKey() );
        clone.setValue( this.getValue() );
        return clone;
    }

    /**
     * Transform the <code>Mapping</code> POJO to a DOM element.
     *
     * @param document the DOM document.
     * @return the DOM element.
     */
    protected Element toDOMElement( CoreDocumentImpl document )
    {
        ElementImpl element = new ElementImpl( document, "mapping" );
        element.setAttribute( "key", this.getKey() );
        element.setAttribute( "value", this.getValue() );
        return element;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo( Object anotherMapping )
    {
        return this.getKey().compareTo( ( (Mapping) anotherMapping ).getKey() );
    }

}