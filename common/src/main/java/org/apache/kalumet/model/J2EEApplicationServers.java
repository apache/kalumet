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
 * Represents the <code>j2eeapplicationservers</code> tag in the Kalumet DOM.
 */
public class J2EEApplicationServers implements Serializable, Cloneable {

   private static final long serialVersionUID = -4940898204749451109L;

   private boolean cluster;
   private LinkedList applicationServers;

   public J2EEApplicationServers() {
      this.applicationServers = new LinkedList();
   }

   public boolean isCluster() {
      return this.cluster;
   }

   public void setCluster(boolean cluster) {
      this.cluster = cluster;
   }

   /**
    * Adds a new <code>J2EEApplicationServer</code> in the
    * <code>J2EEApplicationServers</code> container.
    * 
    * @param j2EEApplicationServer the <code>J2EEApplicationServer</code> to add.
    */
   public void addApplicationServer(J2EEApplicationServer j2EEApplicationServer) throws ModelObjectAlreadyExistsException {
      if (this.getApplicationServer(j2EEApplicationServer.getName()) != null) {
         throw new ModelObjectAlreadyExistsException("J2EE application server name already exists in the environment.");
      }
      this.applicationServers.add(j2EEApplicationServer);
   }

   /**
    * Gets the <code>J2EEApplicationServer</code> list in the
    * <code>J2EEApplicationServers</code> container.
    * 
    * @return the <code>J2EEApplicationServer</code> list.
    */
   public List getApplicationServers() {
      return this.applicationServers;
   }

   /**
    * Overwrites the <code>J2EEApplicationServer</code> list in the
    * <code>J2EEApplicationServers</code> container.
    * 
    * @param applicationServers the new <code>J2EEApplicationServer</code> list.
    */
   public void setApplicationServers(LinkedList applicationServers) {
      this.applicationServers = applicationServers;
   }

   /**
    * Gets the <code>J2EEApplicationServer</code> identified by a given name in the
    * <code>J2EEApplicationServers</code> container.
    * 
    * @param name the <code>J2EEApplicationServer</code> name.
    * @return the <code>J2EEApplicationServer</code> found or null if no found.
    */
   public J2EEApplicationServer getApplicationServer(String name) {
      for (Iterator applicationServerIterator = this.getApplicationServers().iterator(); applicationServerIterator.hasNext();) {
         J2EEApplicationServer j2EEApplicationServer = (J2EEApplicationServer) applicationServerIterator.next();
         if (j2EEApplicationServer.getName().equals(name)) {
            return j2EEApplicationServer;
         }
      }
      return null;
   }

   /**
    * @see java.lang.Object#clone()
    */
   public Object clone() throws CloneNotSupportedException {
      J2EEApplicationServers clone = new J2EEApplicationServers();
      clone.setCluster(this.isCluster());
      for (Iterator applicationServerIterator = this.applicationServers.iterator(); applicationServerIterator.hasNext(); ) {
          J2EEApplicationServer j2EEApplicationServer = (J2EEApplicationServer) applicationServerIterator.next();
          clone.applicationServers.add((J2EEApplicationServer) j2EEApplicationServer.clone());
      }
      return clone;
   }

   /**
    * Transforms the <code>J2EEApplicationServers</code> POJO to a DOM element.
    * 
    * @param document the DOM document.
    * @return the DOM element.
    */
   protected Element toDOMElement(CoreDocumentImpl document) {
      ElementImpl element = new ElementImpl(document, "j2eeapplicationservers");
      element.setAttribute("cluster", new Boolean(this.isCluster()).toString());
      // add applicationserver child nodes
      for (Iterator applicationServerIterator = this.getApplicationServers().iterator(); applicationServerIterator.hasNext();) {
         J2EEApplicationServer j2EEApplicationServer = (J2EEApplicationServer) applicationServerIterator.next();
         element.appendChild(j2EEApplicationServer.toDOMElement(document));
      }
      return element;
   }

}