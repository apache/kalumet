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
 * Represent a <code>variable</code> tag in the Kalumet configuration DOM.
 */
public class Variable
    implements Serializable, Cloneable, Comparable
{

    private static final long serialVersionUID = -7869565872871570323L;

    private String name;

    private String value;

    public Variable()
    {
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getName()
    {
        return this.name;
    }

    public void setValue( String value )
    {
        this.value = value;
    }

    public String getValue()
    {
        return this.value;
    }

    /**
     * @see java.lang.Object#clone()
     */
    public Object clone()
        throws CloneNotSupportedException
    {
        Variable clone = new Variable();
        clone.setName( this.getName() );
        clone.setValue( this.getValue() );
        return clone;
    }

    /**
     * Transform the <code>Variable</code> POJO to a DOM Element.
     *
     * @param document the DOM document.
     * @return the DOM element.
     */
    protected Element toDOMElement( CoreDocumentImpl document )
    {
        ElementImpl element = new ElementImpl( document, "variable" );
        element.setAttribute( "name", this.getName() );
        element.setAttribute( "value", this.getValue() );
        return element;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo( Object anotherVariable )
    {
        return this.getName().compareTo( ( (Variable) anotherVariable ).getName() );
    }

}