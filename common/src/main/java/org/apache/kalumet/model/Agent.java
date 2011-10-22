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
 * Represents the <code>agent</code> tag in the Kalumet DOM.
 */
public class Agent implements Serializable, Cloneable, Comparable {

   private static final long serialVersionUID = -2827134650188545192L;

   private String id;
   private String hostname;
   private int port;
   private String cron;
   private int maxenvironments;
   private int maxapplicationserverssactive;

   public Agent() { }

   public String getId() {
      return this.id;
   }

   public void setId(String id) {
      this.id = id;
   }

   public String getHostname() {
      return this.hostname;
   }

   public void setHostname(String hostname) {
      this.hostname = hostname;
   }

   public int getPort() {
      return this.port;
   }

   public void setPort(int port) {
      this.port = port;
   }

   public String getCron() {
       return cron;
   }

   public void setCron(String cron) {
       this.cron = cron;
   }

   public int getMaxenvironments() {
      return this.maxenvironments;
   }

   public void setMaxenvironments(int maxenvironments) {
      this.maxenvironments = maxenvironments;
   }

   public int getMaxapplicationserversactive() {
      return this.maxapplicationserverssactive;
   }

   public void setMaxapplicationserversactive(int maxenvironmentsactive) {
      this.maxapplicationserverssactive = maxenvironmentsactive;
   }

   /**
    * @see java.lang.Object#clone()
    */
   public Object clone() throws CloneNotSupportedException {
      Agent clone = new Agent();
      clone.setId(this.getId());
      clone.setHostname(this.getHostname());
      clone.setPort(this.getPort());
      clone.setCron(this.getCron());
      clone.setMaxenvironments(this.getMaxenvironments());
      clone.setMaxapplicationserversactive(this.getMaxapplicationserversactive());
      return clone;
   }

   /**
    * Transforms the <code>Agent</code> POJO into a DOM element.
    * 
    * @param document the DOM core document.
    * @return the DOM element.
    */
   protected Element toDOMElement(CoreDocumentImpl document) {
      ElementImpl element = new ElementImpl(document, "agent");
      element.setAttribute("id", this.getId());
      element.setAttribute("hostname", this.getHostname());
      element.setAttribute("port", new Integer(this.getPort()).toString());
      element.setAttribute("cron", this.getCron());
      element.setAttribute("maxenvironments", new Integer(this.getMaxenvironments()).toString());
      element.setAttribute("maxenvironmentsactive", new Integer(this.getMaxapplicationserversactive()).toString());
      return element;
   }
   
   /**
    * @see java.lang.Comparable#compareTo(java.lang.Object)
    */
   public int compareTo(Object anotherAgent) {
       return this.getId().compareTo(((Agent)anotherAgent).getId());
   }

}
