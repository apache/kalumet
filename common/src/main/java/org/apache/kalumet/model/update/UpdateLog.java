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

import java.io.FileOutputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.kalumet.KalumetException;
import org.apache.kalumet.FileManipulator;
import org.apache.kalumet.model.Environment;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xerces.dom.CoreDocumentImpl;
import org.apache.xerces.dom.ElementImpl;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Element;

/**
 * Represents the main tag of the Kalumet update log DOM.
 */
public class UpdateLog {

   private final static transient Log LOG = LogFactory.getLog(UpdateLog.class);
   
   public final static String MAIN_LOG_FILE = "log.xml";

   private String status;
   private String time;
   private String title;
   private String basedir;
   private boolean updated = false;
   private LinkedList updateMessages;
   
   public UpdateLog() {
       this.updateMessages = new LinkedList();
   }

   /**
    * Create a <code>UpdateLog</code> defining basic attributes.
    * 
    * @param status the status.
    * @param time the time.
    * @param title the title.
    * @param environment the environment associated to this update log.
    * @param updated the updated flag.
    */
   public UpdateLog(String status, String time, String title, Environment environment, boolean updated) throws KalumetException {
      this.updateMessages = new LinkedList();
      this.status = status;
      this.time = time;
      this.title = title;
      this.basedir = FileManipulator.createEnvironmentCacheDir(environment);
      this.updated = updated;
   }
   
   /**
    * Create a <code>UpdateLog</code>.
    * 
    * @param status the current update log status.
    * @param title the current update log title.
    * @param environment the update log environment linked.
    * @throws KalumetException in case of update log creation failure.
    */
   public UpdateLog(String status, String title, Environment environment) throws KalumetException {
       this(status, ((FastDateFormat) DateFormatUtils.ISO_DATETIME_FORMAT).format(new Date()), title, environment, false);
   }

   public String getStatus() {
      return this.status;
   }

   public void setStatus(String status) {
      this.status = status;
   }

   public String getTime() {
      return this.time;
   }

   public void setTime(String time) {
      this.time = time;
   }

   public String getTitle() {
      return this.title;
   }

   public void setTitle(String title) {
      this.title = title;
   }

   public boolean isUpdated() {
      return this.updated;
   }

   public void setUpdated(boolean updated) {
      this.updated = updated;
   }

   /**
    * Adds a new <code>UpdateMessage</code> in the <code>UpdateLog</code>.
    * 
    * @param updateMessage the <code>UpdateMessage</code> to add.
    */
   public void addUpdateMessage(UpdateMessage updateMessage) {
      this.updateMessages.add(updateMessage);
      this.writeXMLFile();
   }

   /**
    * Gets <code>UpdateMessage</code> list in the <code>UpdateLog</code>.
    * 
    * @return the <code>UpdateMessage</code> list.
    */
   public List getUpdateMessages() {
      return this.updateMessages;
   }

   /**
    * Transforms the <code>UpdateLog</code> POJO to a DOM element.
    * 
    * @param document the DOM document.
    * @return the DOM element.
    */
   protected Element toDOMElement(CoreDocumentImpl document) {
      ElementImpl element = new ElementImpl(document, "updatelog");
      element.setAttribute("status", this.getStatus());
      element.setAttribute("time", this.getTime());
      element.setAttribute("title", this.getTitle());
      element.setAttribute("updated", new Boolean(this.isUpdated()).toString());
      // add update message child nodes
      for (Iterator updateMessageIterator = this.getUpdateMessages().iterator(); updateMessageIterator.hasNext();) {
         UpdateMessage updateMessage = (UpdateMessage) updateMessageIterator.next();
         element.appendChild(updateMessage.toDOMElement(document));
      }
      return element;
   }

   /**
    * Writes the Kalumet agent XML log file using in-memory DOM.
    */
   public synchronized void writeXMLFile() {
      try {
         OutputFormat format = new OutputFormat();
         format.setLineWidth(72);
         format.setIndenting(true);
         format.setIndent(3);
         format.setEncoding("ISO-8859-1");
         XMLSerializer serializer = new XMLSerializer(new FileOutputStream(this.basedir + "/" + MAIN_LOG_FILE), format);
         serializer.serialize(this.toDOMElement(new CoreDocumentImpl(true)));
      } catch (Exception e) {
         LOG.error("Can't write update log file.", e);
      }
   }

}
