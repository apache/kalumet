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
 * Represent the <code>Email</code> tag in the Kalumet configuration DOM.
 */
public class Email implements Serializable, Cloneable, Comparable {

   private static final long serialVersionUID = -88087573038809801L;

   private String mailhost;
   private String from;
   private LinkedList destinations;

   public Email() {
      this.destinations = new LinkedList();
   }

   public String getMailhost() {
      return this.mailhost;
   }

   public void setMailhost(String mailhost) {
      this.mailhost = mailhost;
   }

   public String getFrom() {
      return this.from;
   }

   public void setFrom(String from) {
      this.from = from;
   }

   /**
    * Add a new <code>Destination</code> in the <code>Email</code>
    * destinations container.
    * 
    * @param destination the <code>Destination</code> to add.
    */
   public void addDestination(Destination destination) throws ModelObjectAlreadyExistsException {
      if (this.getDestination(destination.getAddress()) != null) {
         throw new ModelObjectAlreadyExistsException("Destination address already exists in the email destinations.");
      }
      this.destinations.add(destination);
   }

   /**
    * Get the <code>Destination</code> list in the <code>Email</code>
    * destinations container.
    * 
    * @return the <code>Destination</code> list.
    */
   public List getDestinations() {
      return this.destinations;
   }

   /**
    * Set the <code>Destination</code> list in the <code>Email</code>
    * destinations container.
    * 
    * @param destinations the new <code>Destination</code> list.
    */
   public void setDestinations(LinkedList destinations) {
      this.destinations = destinations;
   }

   /**
    * Get the <code>Destination</code> identified by a given address in the
    * <code>Email</code> destinations container.
    * 
    * @param address the <code>Destination</code> address.
    * @return the <code>Destination</code> found or null if not found.
    */
   public Destination getDestination(String address) {
      for (Iterator destinationIterator = this.getDestinations().iterator(); destinationIterator.hasNext();) {
         Destination destination = (Destination) destinationIterator.next();
         if (destination.getAddress().equals(address)) {
            return destination;
         }
      }
      return null;
   }

   /**
    * @see java.lang.Object#clone()
    */
   public Object clone() throws CloneNotSupportedException {
      Email clone = new Email();
      clone.setMailhost(this.getMailhost());
      clone.setFrom(this.getFrom());
      for (Iterator destinationIterator = this.destinations.iterator(); destinationIterator.hasNext(); ) {
          Destination destination = (Destination)destinationIterator.next();
          clone.destinations.add((Destination)destination.clone());
      }
      return clone;
   }

   /**
    * Transform the <code>Email</code> POJO to a DOM element.
    * 
    * @param document the DOM document.
    * @return the DOM element.
    */
   protected Element toDOMElement(CoreDocumentImpl document) {
      ElementImpl element = new ElementImpl(document, "email");
      element.setAttribute("mailhost", this.getMailhost());
      element.setAttribute("from", this.getFrom());
      // destinations
      ElementImpl destinations = new ElementImpl(document, "destinations");
      for (Iterator destinationIterator = this.getDestinations().iterator(); destinationIterator.hasNext();) {
         Destination destination = (Destination) destinationIterator.next();
         destinations.appendChild(destination.toDOMElement(document));
      }
      element.appendChild(destinations);
      return element;
   }
   
   /**
    * @see java.lang.Comparable#compareTo(java.lang.Object)
    */
   public int compareTo(Object anotherEmail) {
       return this.getMailhost().compareTo(((Email)anotherEmail).getMailhost());
   }

}