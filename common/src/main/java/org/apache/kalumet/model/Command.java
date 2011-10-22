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

import org.apache.xerces.dom.CDATASectionImpl;
import org.apache.xerces.dom.CoreDocumentImpl;
import org.apache.xerces.dom.ElementImpl;
import org.w3c.dom.Element;

/**
 * Represent a <code>command</code> tag, mainly inside <code>software</code>.
 */
public class Command implements Serializable, Cloneable, Comparable {

    private static final long serialVersionUID = -3671135569540426579L;
    
    private String name;
    private boolean active;
    private boolean blocker;
    private String agent;
    private String command;
    
    public Command() { }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isBlocker() {
        return blocker;
    }

    public void setBlocker(boolean blocker) {
        this.blocker = blocker;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }
    
    /**
     * @see java.lang.Object#clone()
     */
    public Object clone() throws CloneNotSupportedException {
        Command clone = new Command();
        clone.setName(this.getName());
        clone.setActive(this.isActive());
        clone.setBlocker(this.isBlocker());
        clone.setAgent(this.getAgent());
        clone.setCommand(this.getCommand());
        return clone;
    }
    
    /**
     * Transforms a <code>command</code> into a XML DOM element.
     * 
     * @param document the DOM document.
     * @return the <code>command</code> DOM element.
     */
    protected Element toDOMElement(CoreDocumentImpl document) {
        ElementImpl element = new ElementImpl(document, "command");
        element.setAttribute("name", this.getName());
        element.setAttribute("active", new Boolean(this.isActive()).toString());
        element.setAttribute("blocker", new Boolean(this.isBlocker()).toString());
        element.setAttribute("agent", this.getAgent());
        CDATASectionImpl content = new CDATASectionImpl(document, this.getCommand());
        element.appendChild(content);
        return element;
    }
    
    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object anotherCommand) {
        return this.getName().compareTo(((Command)anotherCommand).getName());
    }
    
}