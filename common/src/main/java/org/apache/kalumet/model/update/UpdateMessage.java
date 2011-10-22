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
package org.apache.kalumet.model.update;

import org.apache.xerces.dom.CDATASectionImpl;
import org.apache.xerces.dom.CoreDocumentImpl;
import org.apache.xerces.dom.ElementImpl;
import org.w3c.dom.Element;

/**
 * Represents a <code>updatemessage</code> tag in the Kalumet update log DOM.
 */
public class UpdateMessage {

   private String priority;
   private String message;

   public UpdateMessage() { }

   /**
    * Creates a UpdateMessage with a defined message content.
    * 
    * @param priority the message priority (info, error, ...).
    * @param message the message content.
    */
   public UpdateMessage(String priority, String message) {
      this.priority = priority;
      this.message = message;
   }

   public String getPriority() {
      return this.priority;
   }

   public void setPriority(String priority) {
      this.priority = priority;
   }

   public String getMessage() {
      return this.message;
   }

   public void setMessage(String message) {
      this.message = message;
   }

   /**
    * Transforms the <code>UpdateMessage</code> POJO to a DOM element.
    * 
    * @param document the DOM document.
    * @return the DOM element.
    */
   protected Element toDOMElement(CoreDocumentImpl document) {
      ElementImpl element = new ElementImpl(document, "updatemessage");
      element.setAttribute("priority", this.getPriority());
      CDATASectionImpl message = new CDATASectionImpl(document, this.getMessage());
      element.appendChild(message);
      return element;
   }

}
