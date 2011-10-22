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
package org.apache.kalumet.model.log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import EDU.oswego.cs.dl.util.concurrent.WriterPreferenceReadWriteLock;
import org.apache.kalumet.KalumetException;

import org.apache.commons.digester.Digester;
import org.apache.xerces.dom.CoreDocumentImpl;
import org.apache.xerces.dom.ElementImpl;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Manages the environment log file and represents the <code>journal</code>
 * root tag.
 */
public class Journal {

   private LinkedList events;

   private WriterPreferenceReadWriteLock lock = new WriterPreferenceReadWriteLock();

   public Journal() {
      this.events = new LinkedList();
   }

   /**
    * Adds a new <code>Event</code> in the <code>Journal</code> container.
    * 
    * @param event the <code>Event</code> to add.
    */
   public void addEvent(Event event) {
      this.events.add(event);
   }

   /**
    * Gets the <code>Event</code> list in the <code>Journal</code>
    * container.
    * 
    * @return the <code>Event</code> list.
    */
   public List getEvents() {
      return this.events;
   }

   /**
    * Transforms the <code>Journal</code> POJO to a DOM element.
    * 
    * @param document the DOM document.
    * @return the DOM element.
    */
   protected Element toDOMElement(CoreDocumentImpl document) {
      ElementImpl element = new ElementImpl(document, "journal");
      // events
      for(Iterator eventIterator = this.getEvents().iterator(); eventIterator.hasNext();) {
         Event event = (Event) eventIterator.next();
         element.appendChild(event.toDOMElement(document));
      }
      return element;
   }

   /**
    * Parses and loads a given XML log file and return the environment journal root tag.
    * 
    * @param path the environment log XML to parse.
    * @return the environment <code>Journal</code> corresponding with the journal root tag.
    */
   public static Journal digeste(String path) throws KalumetException {
      if (!path.startsWith("http:") && !path.startsWith("HTTP:") && !path.startsWith("file:") && !path.startsWith("FILE:")) {
         path = "file:" + path;
      }
      Journal journal = null;
      try {

         // init the digester with no validation on the XML file (no DTD)
         Digester digester = new Digester();
         digester.setValidating(false);

         // journal tag rules
         digester.addObjectCreate("journal", "org.apache.kalumet.model.log.Journal");
         digester.addSetProperties("journal");

         // event tag rules
         digester.addObjectCreate("journal/event", "org.apache.kalumet.model.log.Event");
         digester.addSetProperties("journal/event");

         // event content
         digester.addCallMethod("journal/event", "setContent", 0);

         // add event to journal
         digester.addSetNext("journal/event", "addEvent", "org.apache.kalumet.model.log.Event");

         // parse the XML file
         journal = (Journal) digester.parse(path);
      } 
      catch (IOException ioException) {
         // most of the time IOException occurs because the journal file path
         // doesn't exist, try to create it
         journal = new Journal();
         journal.writeXMLFile(path);
      } 
      catch (Exception e) {
         throw new KalumetException("Can't read journal", e);
      }
      return journal;
   }

   /**
    * Writes the environment journal log XML file with the in-memory DOM.
    * 
    * @param path the path to the file to write.
    */
   public void writeXMLFile(String path) throws KalumetException {
      try {
         lock.writeLock().acquire();
         OutputFormat format = new OutputFormat();
         format.setLineWidth(72);
         format.setIndenting(true);
         format.setIndent(3);
         format.setEncoding("ISO-8859-1");
         if (path.startsWith("http:") || path.startsWith("http:")) {
            throw new KalumetException("Can't write journal file over a HTTP URL");
         }
         if (path.startsWith("file:") || path.startsWith("FILE:")) {
            path = path.substring(5);
         }
         XMLSerializer serializer = new XMLSerializer(new FileOutputStream(path), format);
         serializer.serialize(this.toDOMElement(new CoreDocumentImpl(true)));
      } 
      catch (Exception e) {
         throw new KalumetException("Can't write journal", e);
      } finally {
          lock.writeLock().release();
      }
   }

}