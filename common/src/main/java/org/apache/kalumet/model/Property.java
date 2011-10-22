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
 * Represent a <code>property</code> tag in the Kalumet configuration DOM.
 */
public class Property implements Serializable, Cloneable, Comparable {

   private static final long serialVersionUID = -1044229953052391950L;

   private String name;
   private String value;

   public Property() { }

   public void setName(String name) {
      this.name = name;
   }

   public String getName() {
      return this.name;
   }

   public void setValue(String value) {
      this.value = value;
   }

   public String getValue() {
      return this.value;
   }

   /**
    * @see java.lang.Object#clone()
    */
   public Object clone() throws CloneNotSupportedException {
      Property clone = new Property();
      clone.setName(this.getName());
      clone.setValue(this.getValue());
      return clone;
   }

   /**
    * Transform the <code>Property</code> POJO to a DOM Element.
    * 
    * @param document the DOM document.
    * @return the DOM element.
    */
   protected Element toDOMElement(CoreDocumentImpl document) {
      ElementImpl element = new ElementImpl(document, "property");
      element.setAttribute("name", this.getName());
      element.setAttribute("value", this.getValue());
      return element;
   }
   
   /**
    * @see java.lang.Comparable#compareTo(java.lang.Object)
    */
   public int compareTo(Object anotherProperty) {
       return this.getName().compareTo(((Property)anotherProperty).getName());
   }

}