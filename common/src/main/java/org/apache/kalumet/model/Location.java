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

import java.io.Serializable;

import org.apache.xerces.dom.CoreDocumentImpl;
import org.apache.xerces.dom.ElementImpl;
import org.w3c.dom.Element;

/**
 * A <code>location</code> is a general wrapper for files and directories.
 */
public class Location implements Cloneable, Serializable, Comparable {

    private static final long serialVersionUID = 3632838715316673949L;
    
    private String name;
    private boolean active;
    private boolean blocker;
    private String uri;
    private String path;
    private String agent;
    
    public Location() { }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isBlocker() {
        return blocker;
    }

    public void setBlocker(boolean blocker) {
        this.blocker = blocker;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getPath() {
        return path;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @see java.lang.Object#clone()
     */
    public Object clone() throws CloneNotSupportedException {
        Location clone = new Location();
        clone.setActive(this.isActive());
        clone.setBlocker(this.isBlocker());
        clone.setName(this.getName());
        clone.setUri(this.getUri());
        clone.setPath(this.getPath());
        clone.setAgent(this.getAgent());
        return clone;
    }
    
    /**
     * Transform a <code>location</code> into a DOM element.
     * 
     * @param document the DOM document.
     * @return the DOM element.
     */
    public Element toDOMElement(CoreDocumentImpl document) {
        ElementImpl element = new ElementImpl(document, "location");
        element.setAttribute("name", this.getName());
        element.setAttribute("active", new Boolean(this.isActive()).toString());
        element.setAttribute("blocker", new Boolean(this.isBlocker()).toString());
        element.setAttribute("uri", this.getUri());
        element.setAttribute("path", this.getPath());
        element.setAttribute("agent", this.getAgent());
        return element;
    }
    
    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object anotherLocation) {
        return this.getName().compareTo(((Location)anotherLocation).getName());
    }
    
}