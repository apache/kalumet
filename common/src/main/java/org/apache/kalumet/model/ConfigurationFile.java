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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.xerces.dom.CoreDocumentImpl;
import org.apache.xerces.dom.ElementImpl;
import org.w3c.dom.Element;

/**
 * Represents the <code>configurationfile</code> tag in the Kalumet DOM.
 */
public class ConfigurationFile implements Serializable, Cloneable, Comparable {

    private static final long serialVersionUID = -1898011382653346087L;

    private String name;
    private String uri;
    private String path;
    private boolean active;
    private boolean blocker;
    private String agent;
    private LinkedList mappings;

    public ConfigurationFile() {
        this.mappings = new LinkedList();
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUri() {
        return this.uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isBlocker() {
        return this.blocker;
    }

    public void setBlocker(boolean blocker) {
        this.blocker = blocker;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    /**
     * Add a new <code>Mapping</code> in the <code>ConfigurationFile</code>
     * mappings container.
     * 
     * @param mapping the <code>Mapping</code> to add.
     */
    public void addMapping(Mapping mapping) throws ModelObjectAlreadyExistsException {
        if (this.getMapping(mapping.getKey()) != null) {
            throw new ModelObjectAlreadyExistsException("Mapping key already exists in the configuration file.");
        }
        this.mappings.add(mapping);
    }

    /**
     * Get the <code>Mapping</code> list in the <code>ConfigurationFile</code>
     * mappings container.
     * 
     * @return the <code>Mapping</code> list.
     */
    public List getMappings() {
        return this.mappings;
    }

    /**
     * Set the <code>Mapping</code> list in the
     * <code>ConfigurationFile</code> mappings container.
     * 
     * @param mappings the new <code>Mapping</code> list.
     */
    public void setMappings(LinkedList mappings) {
        this.mappings = mappings;
    }

    /**
     * Get the <code>Mapping</code> identified by a given key in the
     * <code>ConfigurationFile</code> mappings container.
     * 
     * @param key the <code>Mapping</code> key.
     * @return the <code>Mapping</code> found or null if no <code>Mapping</code> found.
     */
    public Mapping getMapping(String key) {
        for (Iterator mappingIterator = this.getMappings().iterator(); mappingIterator.hasNext();) {
            Mapping mapping = (Mapping) mappingIterator.next();
            if (mapping.getKey().equals(key)) {
                return mapping;
            }
        }
        return null;
    }

    /**
     * @see java.lang.Object#clone()
     */
    public Object clone() throws CloneNotSupportedException {
        ConfigurationFile clone = new ConfigurationFile();
        clone.setName(this.getName());
        clone.setUri(this.getUri());
        clone.setPath(this.getPath());
        clone.setActive(this.isActive());
        clone.setBlocker(this.isBlocker());
        clone.setAgent(this.getAgent());
        for (Iterator mappingIterator = this.mappings.iterator(); mappingIterator.hasNext();) {
            Mapping mapping = (Mapping) mappingIterator.next();
            clone.mappings.add((Mapping) mapping.clone());
        }
        return clone;
    }

    /**
     * Transform the <code>ConfigurationFile</code> POJO to a DOM element.
     * 
     * @param document the core XML document.
     * @return the DOM element.
     */
    protected Element toDOMElement(CoreDocumentImpl document) {
        ElementImpl element = new ElementImpl(document, "configurationfile");
        element.setAttribute("name", this.getName());
        element.setAttribute("uri", this.getUri());
        element.setAttribute("path", this.getPath());
        element.setAttribute("active", new Boolean(this.isActive()).toString());
        element.setAttribute("blocker", new Boolean(this.isBlocker()).toString());
        element.setAttribute("agent", this.getAgent());
        // mappings
        ElementImpl mappings = new ElementImpl(document, "mappings");
        for (Iterator mappingIterator = this.getMappings().iterator(); mappingIterator.hasNext();) {
            Mapping mapping = (Mapping) mappingIterator.next();
            mappings.appendChild(mapping.toDOMElement(document));
        }
        element.appendChild(mappings);
        return element;
    }
    
    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object anotherConfigurationFile) {
        return this.getName().compareTo(((ConfigurationFile)anotherConfigurationFile).getName());
    }

}