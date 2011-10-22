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
 * Represents the <code>applicationservers</code> tag in the Kalumet DOM.
 */
public class ApplicationServers implements Serializable, Cloneable {

   private static final long serialVersionUID = -4940898204749451109L;

   private boolean cluster;
   private LinkedList applicationServers;

   public ApplicationServers() {
      this.applicationServers = new LinkedList();
   }

   public boolean isCluster() {
      return this.cluster;
   }

   public void setCluster(boolean cluster) {
      this.cluster = cluster;
   }

   /**
    * Adds a new <code>ApplicationServer</code> in the
    * <code>ApplicationServers</code> container.
    * 
    * @param applicationServer the <code>ApplicationServer</code> to add.
    */
   public void addApplicationServer(ApplicationServer applicationServer) throws ModelObjectAlreadyExistsException {
      if (this.getApplicationServer(applicationServer.getName()) != null) {
         throw new ModelObjectAlreadyExistsException("J2EE server name already exists in the environment.");
      }
      this.applicationServers.add(applicationServer);
   }

   /**
    * Gets the <code>ApplicationServer</code> list in the
    * <code>ApplicationServers</code> container.
    * 
    * @return the <code>ApplicationServer</code> list.
    */
   public List getApplicationServers() {
      return this.applicationServers;
   }

   /**
    * Overwrites the <code>ApplicationServer</code> list in the
    * <code>ApplicationServers</code> container.
    * 
    * @param applicationServers the new <code>ApplicationServer</code> list.
    */
   public void setApplicationServers(LinkedList applicationServers) {
      this.applicationServers = applicationServers;
   }

   /**
    * Gets the <code>ApplicationServer</code> identified by a given name in the
    * <code>ApplicationServers</code> container.
    * 
    * @param name the <code>ApplicationServer</code> name.
    * @return the <code>ApplicationServer</code> found or null if no found.
    */
   public ApplicationServer getApplicationServer(String name) {
      for (Iterator applicationServerIterator = this.getApplicationServers().iterator(); applicationServerIterator.hasNext();) {
         ApplicationServer applicationServer = (ApplicationServer) applicationServerIterator.next();
         if (applicationServer.getName().equals(name)) {
            return applicationServer;
         }
      }
      return null;
   }

   /**
    * @see java.lang.Object#clone()
    */
   public Object clone() throws CloneNotSupportedException {
      ApplicationServers clone = new ApplicationServers();
      clone.setCluster(this.isCluster());
      for (Iterator applicationServerIterator = this.applicationServers.iterator(); applicationServerIterator.hasNext(); ) {
          ApplicationServer applicationServer = (ApplicationServer) applicationServerIterator.next();
          clone.applicationServers.add((ApplicationServer)applicationServer.clone());
      }
      return clone;
   }

   /**
    * Transforms the <code>ApplicationServers</code> POJO to a DOM element.
    * 
    * @param document the DOM document.
    * @return the DOM element.
    */
   protected Element toDOMElement(CoreDocumentImpl document) {
      ElementImpl element = new ElementImpl(document, "applicationservers");
      element.setAttribute("cluster", new Boolean(this.isCluster()).toString());
      // add applicationserver child nodes
      for (Iterator applicationServerIterator = this.getApplicationServers().iterator(); applicationServerIterator.hasNext();) {
         ApplicationServer applicationServer = (ApplicationServer) applicationServerIterator.next();
         element.appendChild(applicationServer.toDOMElement(document));
      }
      return element;
   }

}