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
 * Represent the <code>jmsqueue</code> tag in the Kalumet configuration DOM.
 */
public class JMSQueue
    implements Serializable, Cloneable, Comparable
{

    private static final long serialVersionUID = -5239170363052872967L;

    private String name;

    public JMSQueue()
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

    /**
     * @see java.lang.Object#clone()
     */
    public Object clone()
        throws CloneNotSupportedException
    {
        JMSQueue clone = new JMSQueue();
        clone.setName( this.getName() );
        return clone;
    }

    /**
     * Transform the <code>JMSQueue</code> POJO to a DOM element.
     *
     * @param document the DOM document.
     * @return the DOM element.
     */
    protected Element toDOMElement( CoreDocumentImpl document )
    {
        ElementImpl element = new ElementImpl( document, "jmsqueue" );
        element.setAttribute( "name", this.getName() );
        return element;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo( Object anotherJMSQueue )
    {
        return this.getName().compareTo( ( (JMSQueue) anotherJMSQueue ).getName() );
    }

}