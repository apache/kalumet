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
 * Represent the <code>jmsserver</code> tag in the Kalumet configuration DOM.
 */
public class JMSServer implements Serializable, Cloneable, Comparable {

   private static final long serialVersionUID = -6330087943208308843L;

   private String name;
   private boolean active;
   private boolean blocker;
   private LinkedList jmsQueues;
   private LinkedList jmsTopics;

   public JMSServer() {
      this.jmsQueues = new LinkedList();
      this.jmsTopics = new LinkedList();
   }

   public String getName() {
      return this.name;
   }

   public void setName(String name) {
      this.name = name;
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

   /**
    * Add a new <code>JMSQueue</code> in the <code>JMSServer</code>
    * JMS queues container.
    * 
    * @param jmsQueue the <code>JMSQueue</code> to add.
    */
   public void addJMSQueue(JMSQueue jmsQueue) throws ModelObjectAlreadyExistsException {
      if (this.getJMSQueue(jmsQueue.getName()) != null) {
         throw new ModelObjectAlreadyExistsException("JMS queue name already exists in JMS server.");
      }
      this.jmsQueues.add(jmsQueue);
   }

   /**
    * Get the <code>JMSQueue</code> list in the <code>JMSServer</code>
    * JMS queues container.
    * 
    * @return the <code>JMSQueue</code> list.
    */
   public List getJMSQueues() {
      return this.jmsQueues;
   }

   /**
    * Set the <code>JMSQueue</code> list in the <code>JMSServer</code>
    * JMS queues container.
    * 
    * @param jmsQueues the new <code>JMSQueue</code> list.
    */
   public void setJMSQueues(LinkedList jmsQueues) {
      this.jmsQueues = jmsQueues;
   }

   /**
    * Get the <code>JMSQueue</code> identified by a given name in the
    * <code>JMSServer</code> JMS queues container.
    * 
    * @param name the <code>JMSQueue</code> name.
    * @return the <code>JMSQueue</code> found or null if not found.
    */
   public JMSQueue getJMSQueue(String name) {
      for (Iterator jmsQueueIterator = this.getJMSQueues().iterator(); jmsQueueIterator.hasNext();) {
         JMSQueue jmsQueue = (JMSQueue) jmsQueueIterator.next();
         if (jmsQueue.getName().equals(name)) {
            return jmsQueue;
         }
      }
      return null;
   }

   /**
    * Add a new <code>JMSTopic</code> in the <code>JMSServer</code>
    * JMS topics container.
    * 
    * @param jmsTopic the <code>JMSTopic</code> to add.
    */
   public void addJMSTopic(JMSTopic jmsTopic) throws ModelObjectAlreadyExistsException {
      if (this.getJMSTopic(jmsTopic.getName()) != null) {
         throw new ModelObjectAlreadyExistsException("JMS topic name already exists in JMS server.");
      }
      this.jmsTopics.add(jmsTopic);
   }

   /**
    * Get the <code>JMSTopic</code> list in the <code>JMSServer</code>
    * JMS topics container.
    * 
    * @return the <code>JMSTopic</code> list.
    */
   public List getJMSTopics() {
      return this.jmsTopics;
   }

   /**
    * Set the <code>JMSTopic</code> list in the <code>JMSServer</code>
    * JMS topics container.
    * 
    * @param jmsTopics the new <code>JMSTopic</code> list.
    */
   public void setJMSTopics(LinkedList jmsTopics) {
      this.jmsTopics = jmsTopics;
   }

   /**
    * Get the <code>JMSTopic</code> identified by a given name in the
    * <code>JMSServer</code> JMS topics container.
    * 
    * @param name the <code>JMSTopic</code> name.
    * @return the <code>JMSTopic</code> found or null if not found.
    */
   public JMSTopic getJMSTopic(String name) {
      for (Iterator jmsTopicIterator = this.getJMSTopics().iterator(); jmsTopicIterator.hasNext();) {
         JMSTopic jmsTopic = (JMSTopic) jmsTopicIterator.next();
         if (jmsTopic.getName().equals(name)) {
            return jmsTopic;
         }
      }
      return null;
   }

   /**
    * @see java.lang.Object#clone()
    */
   public Object clone() throws CloneNotSupportedException {
      JMSServer clone = new JMSServer();
      clone.setName(this.getName());
      clone.setActive(this.isActive());
      clone.setBlocker(this.isBlocker());
      for (Iterator jmsQueueIterator = this.jmsQueues.iterator(); jmsQueueIterator.hasNext(); ) {
          JMSQueue jmsQueue = (JMSQueue)jmsQueueIterator.next();
          clone.jmsQueues.add((JMSQueue)jmsQueue.clone());
      }
      for (Iterator jmsTopicIterator = this.jmsTopics.iterator(); jmsTopicIterator.hasNext(); ) {
          JMSTopic jmsTopic = (JMSTopic)jmsTopicIterator.next();
          clone.jmsTopics.add((JMSTopic)jmsTopic.clone());
      }
      return clone;
   }

   /**
    * Transform the <code>JMSServer</code> POJO to a DOM element.
    * 
    * @param document the DOM document.
    * @return the DOM element.
    */
   protected Element toDOMElement(CoreDocumentImpl document) {
      ElementImpl element = new ElementImpl(document, "jmsserver");
      element.setAttribute("name", this.getName());
      element.setAttribute("active", new Boolean(this.isActive()).toString());
      element.setAttribute("blocker", new Boolean(this.isBlocker()).toString());
      // jmsqueues
      ElementImpl jmsqueues = new ElementImpl(document, "jmsqueues");
      for (Iterator jmsQueueIterator = this.getJMSQueues().iterator(); jmsQueueIterator.hasNext();) {
         JMSQueue jmsQueue = (JMSQueue) jmsQueueIterator.next();
         jmsqueues.appendChild(jmsQueue.toDOMElement(document));
      }
      element.appendChild(jmsqueues);
      // jmstopics
      ElementImpl jmstopics = new ElementImpl(document, "jmstopics");
      for (Iterator jmsTopicIterator = this.getJMSTopics().iterator(); jmsTopicIterator.hasNext();) {
         JMSTopic jmsTopic = (JMSTopic) jmsTopicIterator.next();
         jmstopics.appendChild(jmsTopic.toDOMElement(document));
      }
      element.appendChild(jmstopics);
      return element;
   }
   
   /**
    * @see java.lang.Comparable#compareTo(java.lang.Object)
    */
   public int compareTo(Object anotherJMSServer) {
       return this.getName().compareTo(((JMSServer)anotherJMSServer).getName());
   }

}