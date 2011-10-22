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

import org.apache.xerces.dom.CDATASectionImpl;
import org.apache.xerces.dom.CoreDocumentImpl;
import org.apache.xerces.dom.ElementImpl;
import org.w3c.dom.Element;

/**
 * Represent the <code>environment</code> tag in the Kalumet configuration DOM.
 */
public class Environment implements Serializable, Cloneable, Comparable {

   private static final long serialVersionUID = -5131247974934459040L;

   private String name;
   private String group;
   private String tag;
   private boolean autoupdate;
   private String agent;
   private String lock;
   private String releaseLocation;
   private String notes;
   private String weblinks;
   private LinkedList variables;
   private LinkedList freeFields;
   private LinkedList accesses;
   private ApplicationServers applicationServers;
   private LinkedList softwares;
   private LinkedList logFiles;
   private Notifiers notifiers;
   private LinkedList publishers;
   private Statistics statistics;

   public Environment() {
      this.variables = new LinkedList();
      this.freeFields = new LinkedList();
      this.accesses = new LinkedList();
      this.applicationServers = new ApplicationServers();
      this.softwares = new LinkedList();
      this.logFiles = new LinkedList();
      this.notifiers = new Notifiers();
      this.publishers = new LinkedList();
      this.statistics = new Statistics();
   }

   public String getName() {
      return this.name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getGroup() {
      return this.group;
   }

   public void setGroup(String group) {
      this.group = group;
   }
   
   public String getTag() {
       return this.tag;
   }
   
   public void setTag(String flag) {
       this.tag = flag;
   }

   public boolean getAutoupdate() {
      return this.autoupdate;
   }

   public void setAutoupdate(boolean autoupdate) {
      this.autoupdate = autoupdate;
   }

   public String getAgent() {
      return this.agent;
   }

   public void setAgent(String agent) {
      this.agent = agent;
   }

   public String getLock() {
      return this.lock;
   }

   public void setLock(String lock) {
      this.lock = lock;
   }
   
   public String getReleaseLocation() {
      return this.releaseLocation;
   }
   
   public void setReleaseLocation(String releaseLocation) {
      this.releaseLocation = releaseLocation;
   }

   public String getNotes() {
      return this.notes;
   }

   public void setNotes(String notes) {
      this.notes = notes;
   }

   public String getWeblinks() {
      return this.weblinks;
   }

   public void setWeblinks(String weblinks) {
      this.weblinks = weblinks;
   }

   /**
    * Add a new <code>Variable</code> in the <code>Environment</code>
    * variables container.
    * 
    * @param variable the <code>Variable</code> to add.
    */
   public void addVariable(Variable variable) throws ModelObjectAlreadyExistsException {
      if (this.getVariable(variable.getName()) != null) {
         throw new ModelObjectAlreadyExistsException("Variable name already exists in the environment variables.");
      }
      this.variables.add(variable);
   }

   /**
    * Get the <code>Variable</code> list in the <code>Environment</code>
    * variables container.
    * 
    * @return the <code>Variable</code> list.
    */
   public List getVariables() {
      return this.variables;
   }

   /**
    * Set the <code>Variable</code> list in the <code>Environment</code>
    * variables container.
    * 
    * @param variables the new <code>Variable</code> list.
    */
   public void setVariables(LinkedList variables) {
      this.variables = variables;
   }

   /**
    * Get the <code>Variable</code> identified by a given name in the
    * <code>Environment</code> variables container.
    * 
    * @param name the <code>Variable</code> name.
    * @return the <code>Variable</code> found or null if not found.
    */
   public Variable getVariable(String name) {
      for (Iterator variableIterator = this.getVariables().iterator(); variableIterator.hasNext();) {
         Variable variable = (Variable) variableIterator.next();
         if (variable.getName().equals(name)) {
            return variable;
         }
      }
      return null;
   }

   /**
    * Add a new <code>FreeField</code> in the <code>Environment</code>
    * free fields container.
    * 
    * @param freeField the <code>FreeField</code> to add.
    */
   public void addFreeField(FreeField freeField) throws ModelObjectAlreadyExistsException {
      if (this.getFreeField(freeField.getName()) != null) {
         throw new ModelObjectAlreadyExistsException("Freefield name already exists in the environment freefields.");
      }
      this.freeFields.add(freeField);
   }

   /**
    * Get the <code>FreeField</code> list in the <code>Environment</code>
    * free fields container.
    * 
    * @return the <code>FreeField</code> list.
    */
   public List getFreeFields() {
      return this.freeFields;
   }

   /**
    * Set the <code>FreeField<code> list in the <code>Environment</code> 
    * free fields container.
    *
    * @param freeFields the new <code>FreeField</code> list.
    */
   public void setFreeFields(LinkedList freeFields) {
      this.freeFields = freeFields;
   }

   /**
    * Get the <code>FreeField</code> identified by a given name in the
    * <code>Environment</code> free fields container.
    * 
    * @param name the <code>FreeField</code> name.
    * @return the <code>FreeField</code> found or null if not found.
    */
   public FreeField getFreeField(String name) {
      for (Iterator freeFieldIterator = this.getFreeFields().iterator(); freeFieldIterator.hasNext();) {
         FreeField freeField = (FreeField) freeFieldIterator.next();
         if (freeField.getName().equals(name)) {
            return freeField;
         }
      }
      return null;
   }

   /**
    * Add a new <code>Access</code> in the <code>Environment</code> accesses
    * container.
    * 
    * @param access the <code>Access</code> to add.
    */
   public void addAccess(Access access) throws ModelObjectAlreadyExistsException {
      if (this.getAccess(access.getGroup()) != null) {
         throw new ModelObjectAlreadyExistsException("Access group id already exists in the environnement accesses.");
      }
      this.accesses.add(access);
   }

   /**
    * Get the <code>Access</code> list in the <code>Environment</code>
    * accesses container.
    * 
    * @return the <code>Access</code> list.
    */
   public List getAccesses() {
      return this.accesses;
   }

   /**
    * Set the <code>Access</code> list in the <code>Environment</code>
    * accesses container.
    * 
    * @param accesses the new <code>Access</code> list.
    */
   public void setAccesses(LinkedList accesses) {
      this.accesses = accesses;
   }

   /**
    * Get the <code>Access</code> identified by a given group id in the
    * <code>Environment</code> accesses container.
    * 
    * @param group the group id.
    * @return the <code>Access</code> found or null if not found.
    */
   public Access getAccess(String group) {
      for (Iterator accessIterator = this.getAccesses().iterator(); accessIterator.hasNext();) {
         Access access = (Access) accessIterator.next();
         if (access.getGroup().equals(group)) {
            return access;
         }
      }
      return null;
   }

   /**
    * Set the <code>ApplicationServers</code> container in the
    * <code>Environment</code>.
    * 
    * @param applicationServers the <code>ApplicationServers</code> to set.
    */
   public void setApplicationServers(ApplicationServers applicationServers) {
      this.applicationServers = applicationServers;
   }

   /**
    * Get the <code>ApplicationServers</code> container in the
    * <code>Environment</code>.
    * 
    * @return the <code>ApplicationServers</code> container.
    */
   public ApplicationServers getApplicationServers() {
      return this.applicationServers;
   }

   /**
    * Add a new <code>Software</code> in the <code>Environment</code>
    * softwares container.
    * 
    * @param software the <code>Software</code> to add.
    */
   public void addSoftware(Software software) throws ModelObjectAlreadyExistsException {
      if (this.getSoftware(software.getName()) != null) {
         throw new ModelObjectAlreadyExistsException("Software name already exists in the environment.");
      }
      this.softwares.add(software);
   }

   /**
    * Get the <code>Software</code> list in the <code>Environment</code>
    * softwares container.
    * 
    * @return the <code>Software</code> list.
    */
   public List getSoftwares() {
      return this.softwares;
   }

   /**
    * Set the <code>Software</code> list in the <code>Environment</code>
    * softwares container.
    * 
    * @param softwares the new <code>Software</code> list.
    */
   public void setSoftwares(LinkedList softwares) {
      this.softwares = softwares;
   }

   /**
    * Get the <code>Software</code> identified by a given name in the
    * <code>Environment</code> softwares container.
    * 
    * @param name the <code>Software</code> name.
    * @return the <code>Software</code> found or null if not found.
    */
   public Software getSoftware(String name) {
      for (Iterator softwareIterator = this.getSoftwares().iterator(); softwareIterator.hasNext();) {
         Software software = (Software) softwareIterator.next();
         if (software.getName().equals(name)) {
            return software;
         }
      }
      return null;
   }
   
   /**
    * Get the environment log files list.
    * 
    * @return the log files list.
    */
   public List getLogFiles() {
       return this.logFiles;
   }
   
   /**
    * Set the environment log files list.
    * 
    * @param logFiles the log files list.
    */
   public void setLogFiles(LinkedList logFiles) {
       this.logFiles = logFiles;
   }
   
   /**
    * Get a log file identified by a name.
    * 
    * @param name the log file name.
    * @return the log file or null if not found.
    */
   public LogFile getLogFile(String name) {
       for (Iterator logFileIterator = this.getLogFiles().iterator(); logFileIterator.hasNext(); ) {
           LogFile logFile = (LogFile) logFileIterator.next();
           if (logFile.getName().equals(name)) {
               return logFile;
           }
       }
       return null;
   }
   
   /**
    * Add a new log file in the environment.
    * 
    * @param logFile the log file to add.
    * @throws ModelObjectAlreadyExistsException if the log file name already exists.
    */
   public void addLogFile(LogFile logFile) throws ModelObjectAlreadyExistsException {
       if (this.getLogFile(logFile.getName()) != null) {
           throw new ModelObjectAlreadyExistsException("Log file name already exists in environment.");
       }
       this.logFiles.add(logFile);
   }

   /**
    * Set the <code>Notifiers</code> in the <code>Environment</code>.
    * 
    * @param notifiers the <code>Notifiers</code> to set.
    */
   public void setNotifiers(Notifiers notifiers) {
      this.notifiers = notifiers;
   }

   /**
    * Get the <code>Notifiers</code> in the <code>Environment</code>.
    * 
    * @return the <code>Notifiers</code> in the <code>Environment</code>.
    */
   public Notifiers getNotifiers() {
      return this.notifiers;
   }

   /**
    * Add a new <code>Email</code> publisher in the <code>Environment</code>
    * publishers container.
    * 
    * @param email the <code>Email</code> to add.
    */
   public void addPublisher(Email email) throws ModelObjectAlreadyExistsException {
      if (this.getPublisher(email.getMailhost()) != null) {
         throw new ModelObjectAlreadyExistsException("Email publisher mailhost already exists in environment.");
      }
      this.publishers.add(email);
   }

   /**
    * Get the <code>Email</code> publisher list in the
    * <code>Environment</code> publishers container.
    * 
    * @return the <code>Email</code> publisher list.
    */
   public List getPublishers() {
      return this.publishers;
   }

   /**
    * Set the <code>Email</code> publisher list in the
    * <code>Environment</code> publishers container.
    * 
    * @param publishers the new <code>Email</code> publisher list.
    */
   public void setPublishers(LinkedList publishers) {
      this.publishers = publishers;
   }

   /**
    * Get the <code>Email</code> publisher identified by a given mail host
    * in the <code>Environment</code> publishers container.
    * 
    * @param mailhost the <code>Email</code> mail host.
    * @return the <code>Email</code> found or null if not found.
    */
   public Email getPublisher(String mailhost) {
      for (Iterator publisherIterator = this.getPublishers().iterator(); publisherIterator.hasNext();) {
         Email email = (Email) publisherIterator.next();
         if (email.getMailhost().equals(mailhost)) {
            return email;
         }
      }
      return null;
   }
   
   /**
    * Set the <code>Statistics</code> container.
    * 
    * @return statistics the new <code>Statistics</code> container.
    */
   public Statistics getStatistics() {
       return statistics;
   }

   /**
    * Get the <code>Statistics</code> container.
    * 
    * @param statistics the current <code>Statistics</code> container.
    */
   public void setStatistics(Statistics statistics) {
       this.statistics = statistics;
    }

    /**
    * @see java.lang.Object#clone()
    */
   public Object clone() throws CloneNotSupportedException {
      Environment clone = new Environment();
      clone.setName(this.getName());
      clone.setGroup(this.getGroup());
      clone.setTag(this.getTag());
      clone.setAutoupdate(this.getAutoupdate());
      clone.setAgent(this.getAgent());
      clone.setLock(this.getLock());
      clone.setReleaseLocation(this.getReleaseLocation());
      clone.setNotes(this.getNotes());
      clone.setWeblinks(this.getWeblinks());
      for (Iterator variableIterator = this.variables.iterator(); variableIterator.hasNext(); ) {
          Variable variable = (Variable)variableIterator.next();
          clone.variables.add((Variable)variable.clone());
      }
      for (Iterator freeFieldIterator = this.freeFields.iterator(); freeFieldIterator.hasNext(); ) {
          FreeField freeField = (FreeField)freeFieldIterator.next();
          clone.freeFields.add((FreeField)freeField.clone());
      }
      for (Iterator accessIterator = this.accesses.iterator(); accessIterator.hasNext(); ) {
          Access access = (Access)accessIterator.next();
          clone.accesses.add((Access)access.clone());
      }
      clone.setApplicationServers((ApplicationServers) this.getApplicationServers().clone());
      for (Iterator softwareIterator = this.softwares.iterator(); softwareIterator.hasNext(); ) {
          Software software = (Software)softwareIterator.next();
          clone.softwares.add((Software)software.clone());
      }
      for (Iterator logFilesIterator = this.logFiles.iterator(); logFilesIterator.hasNext(); ) {
          LogFile logFile = (LogFile) logFilesIterator.next();
          clone.softwares.add((LogFile) logFile.clone());
      }
      clone.setNotifiers((Notifiers) this.getNotifiers().clone());
      for (Iterator publisherIterator = this.publishers.iterator(); publisherIterator.hasNext(); ) {
          Email publisher = (Email)publisherIterator.next();
          clone.publishers.add((Email)publisher.clone());
      }
      clone.setStatistics((Statistics) this.getStatistics().clone());
      return clone;
   }

   /**
    * Transform the <code>Environment</code> POJO to a DOM element.
    * 
    * @param document the DOM document.
    * @return the DOM element.
    */
   protected Element toDOMElement(CoreDocumentImpl document) {
      ElementImpl element = new ElementImpl(document, "environment");
      element.setAttribute("name", this.getName());
      element.setAttribute("group", this.getGroup());
      element.setAttribute("tag", this.getTag());
      element.setAttribute("autoupdate", new Boolean(this.getAutoupdate()).toString());
      element.setAttribute("agent", this.getAgent());
      element.setAttribute("lock", this.getLock());
      element.setAttribute("releaseLocation", this.getReleaseLocation());
      // variables element
      ElementImpl variables = new ElementImpl(document, "variables");
      // add variable to variables
      for (Iterator variableIterator = this.getVariables().iterator(); variableIterator.hasNext();) {
         Variable variable = (Variable) variableIterator.next();
         variables.appendChild(variable.toDOMElement(document));
      }
      // add variables to environment
      element.appendChild(variables);
      // freefields element
      ElementImpl freefields = new ElementImpl(document, "freefields");
      // add freefield to freefields
      for (Iterator freeFieldIterator = this.getFreeFields().iterator(); freeFieldIterator.hasNext();) {
         FreeField freefield = (FreeField) freeFieldIterator.next();
         freefields.appendChild(freefield.toDOMElement(document));
      }
      // add freefields to environment
      element.appendChild(freefields);
      // accesses element
      ElementImpl accesses = new ElementImpl(document, "accesses");
      // add access to accesses
      for (Iterator accessIterator = this.getAccesses().iterator(); accessIterator.hasNext();) {
         Access access = (Access) accessIterator.next();
         accesses.appendChild(access.toDOMElement(document));
      }
      // add accesses to environment
      element.appendChild(accesses);
      // add notes
      ElementImpl notes = new ElementImpl(document, "notes");
      CDATASectionImpl notesContent = new CDATASectionImpl(document, this.getNotes());
      notes.appendChild(notesContent);
      element.appendChild(notes);
      // add weblinks
      ElementImpl weblinks = new ElementImpl(document, "weblinks");
      CDATASectionImpl weblinksContent = new CDATASectionImpl(document, this.getWeblinks());
      weblinks.appendChild(weblinksContent);
      element.appendChild(weblinks);
      // add J2EE servers
      element.appendChild(this.getApplicationServers().toDOMElement(document));
      // softwares element
      ElementImpl softwares = new ElementImpl(document, "softwares");
      // add software to softwares
      for (Iterator softwareIterator = this.getSoftwares().iterator(); softwareIterator.hasNext();) {
         Software software = (Software) softwareIterator.next();
         softwares.appendChild(software.toDOMElement(document));
      }
      // add softwares to environment
      element.appendChild(softwares);
      // logfiles element
      ElementImpl logfiles = new ElementImpl(document, "logfiles");
      // add logfile to logfiles
      for (Iterator logFileIterator = this.getLogFiles().iterator(); logFileIterator.hasNext(); ) {
          LogFile logFile = (LogFile) logFileIterator.next();
          logfiles.appendChild(logFile.toDOMElement(document));
      }
      // add logfiles to environment
      element.appendChild(logfiles);
      // add notifiers to environments
      element.appendChild(this.getNotifiers().toDOMElement(document));
      // publishers element
      ElementImpl publishers = new ElementImpl(document, "publishers");
      // add email publisher to publishers
      for(Iterator publisherIterator = this.getPublishers().iterator(); publisherIterator.hasNext();) {
         Email email = (Email) publisherIterator.next();
         publishers.appendChild(email.toDOMElement(document));
      }
      // add publishers to environment
      element.appendChild(publishers);
      // add statistics
      element.appendChild(this.getStatistics().toDOMElement(document));
      return element;
   }
   
   /**
    * @see java.lang.Comparable#compareTo(java.lang.Object)
    */
   public int compareTo(Object anotherEnvironment) {
       return this.getName().compareTo(((Environment)anotherEnvironment).getName());
   }

}