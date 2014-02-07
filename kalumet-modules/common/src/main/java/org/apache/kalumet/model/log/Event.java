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
package org.apache.kalumet.model.log;

import org.apache.xerces.dom.CDATASectionImpl;
import org.apache.xerces.dom.CoreDocumentImpl;
import org.apache.xerces.dom.ElementImpl;
import org.w3c.dom.Element;

/**
 * Represents the <code>event</code> tag in the journal DOM.
 */
public class Event
{

    private String date;

    private String severity;

    private String author;

    private String content;

    public Event()
    {
    }

    public String getDate()
    {
        return this.date;
    }

    public void setDate( String date )
    {
        this.date = date;
    }

    public String getSeverity()
    {
        return this.severity;
    }

    public void setSeverity( String severity )
    {
        this.severity = severity;
    }

    public String getAuthor()
    {
        return this.author;
    }

    public void setAuthor( String author )
    {
        this.author = author;
    }

    public String getContent()
    {
        return this.content;
    }

    public void setContent( String content )
    {
        this.content = content;
    }

    /**
     * Transforms the <code>Event</code> POJO to a DOM element.
     *
     * @param document the DOM document.
     * @return the DOM element.
     */
    protected Element toDOMElement( CoreDocumentImpl document )
    {
        ElementImpl element = new ElementImpl( document, "event" );
        element.setAttribute( "date", this.getDate() );
        element.setAttribute( "severity", this.getSeverity() );
        element.setAttribute( "author", this.getAuthor() );
        CDATASectionImpl content = new CDATASectionImpl( document, this.getContent() );
        element.appendChild( content );
        return element;
    }

}
