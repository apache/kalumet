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
 * Define the access to a log file.
 */
public class LogFile implements Cloneable, Serializable, Comparable {

    private static final long serialVersionUID = -544824580684870083L;
    
    private String name;
    private String path;
    private String agent;
    
    public LogFile() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }
    
    /**
     * @see java.lang.Object#clone()
     */
    public Object clone() throws CloneNotSupportedException {
       LogFile logFile = new LogFile();
       logFile.setAgent(this.getAgent());
       logFile.setName(this.getName());
       logFile.setPath(this.getPath());
       return logFile;
    }
    
    /**
     * Transform a <code>logfile</code> into a DOM element.
     * 
     * @param document the DOM document.
     * @return the DOM element.
     */
    public Element toDOMElement(CoreDocumentImpl document) {
        ElementImpl element = new ElementImpl(document, "logfile");
        element.setAttribute("name", this.getName());
        element.setAttribute("path", this.getPath());
        element.setAttribute("agent", this.getAgent());
        return element;
    }
    
    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object anotherLogFile) {
       return this.getName().compareTo(((LogFile)anotherLogFile).getName()); 
    }
    
}