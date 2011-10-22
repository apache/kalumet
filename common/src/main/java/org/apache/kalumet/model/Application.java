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
 * Represents the <code>application</code> tag in the Kalumet DOM.
 */
public class Application implements Serializable, Cloneable, Comparable {

   private static final long serialVersionUID = -1198170476993837094L;

   private String name;
   private String uri;
   private boolean active;
   private boolean blocker;
   private String agent;
   private LinkedList archives;
   private LinkedList contentManagers;
   private LinkedList configurationFiles;
   private LinkedList databases;

   /**
    * Default constructor to create a <code>Application</code>
    */
   public Application() {
      this.archives = new LinkedList();
      this.contentManagers = new LinkedList();
      this.configurationFiles = new LinkedList();
      this.databases = new LinkedList();
   }

   public String getName() {
      return this.name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getUri() {
      return this.uri;
   }

   public void setUri(String uri) {
      this.uri = uri;
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

   public String getAgent() {
       return agent;
   }

   public void setAgent(String agent) {
       this.agent = agent;
   }

   /**
    * Add a new <code>Archive</code> in the <code>Application</code> archives container.
    * 
    * @param archive the <code>Archive</code> to add.
    * @throws ModelObjectAlreadyExistsException if the archive name already exists in the application.
    */
   public void addArchive(Archive archive) throws ModelObjectAlreadyExistsException {
      if (this.getArchive(archive.getName()) != null) {
         throw new ModelObjectAlreadyExistsException("Archive name already exists in the JZEE application.");
      }
      this.archives.add(archive);
   }

   /**
    * Get the <code>Archive</code> list in the <code>Application</code> archives container.
    * 
    * @return the <code>Archive</code> list
    */
   public List getArchives() {
      return this.archives;
   }

   /**
    * Overwrite the <code>Archive</code> list in the <code>Application</code> archives container.
    * 
    * @param archives the new <code>Archive</code> list.
    */
   public void setArchives(LinkedList archives) {
      this.archives = archives;
   }

   /**
    * Get the <code>Archive</code> identified by a given name in the <code>Application</code> archives container.
    * 
    * @param name the <code>Archive</code> name.
    * @return the <code>Archive</code> found or null if not found.
    */
   public Archive getArchive(String name) {
      for (Iterator archiveIterator = this.getArchives().iterator(); archiveIterator.hasNext();) {
         Archive archive = (Archive) archiveIterator.next();
         if (archive.getName().equals(name)) {
            return archive;
         }
      }
      return null;
   }

   /**
    * Add a new <code>ContentManager</code> in the <code>Application</code> content managers container.
    * 
    * @param contentManager the <code>ContentManager</code> to add.
    * @throws ModelObjectAlreadyExistsException if the <code>ContentManager</code> already exists in the application.
    */
   public void addContentManager(ContentManager contentManager) throws ModelObjectAlreadyExistsException {
      if (this.getContentManager(contentManager.getName()) != null) {
         throw new ModelObjectAlreadyExistsException("Content manager name already exists in the JZEE application.");
      }
      this.contentManagers.add(contentManager);
   }

   /**
    * Get the <code>ContentManager</code> list in the <code>Application</code> content managers container.
    * 
    * @return the <code>ContentManager</code> list.
    */
   public List getContentManagers() {
      return this.contentManagers;
   }

   /**
    * Overwrite the <code>ContentManager</code> list in the <code>Application</code> content managers container.
    * 
    * @param contentManagers the new <code>ContentManagers</code> list.
    */
   public void setContentManagers(LinkedList contentManagers) {
      this.contentManagers = contentManagers;
   }

   /**
    * Return the <code>ContentManager</code> identified by a given name in the <code>Application</code> content managers container.
    * 
    * @return the <code>ContentManager</code> found or null if not found.
    */
   public ContentManager getContentManager(String name) {
      for (Iterator contentManagerIterator = this.getContentManagers().iterator(); contentManagerIterator.hasNext();) {
         ContentManager contentManager = (ContentManager) contentManagerIterator.next();
         if (contentManager.getName().equals(name)) {
            return contentManager;
         }
      }
      return null;
   }

   /**
    * Add a new <code>ConfigurationFile</code> in the <code>Application</code>
    * configuration files container.
    * 
    * @param configurationFile the <code>ConfigurationFile</code> to add.
    * @throws ModelObjectAlreadyExistsException if the <code>ConfigurationFile</code> name already exists in the application.
    */
   public void addConfigurationFile(ConfigurationFile configurationFile) throws ModelObjectAlreadyExistsException {
      if (this.getConfigurationFile(configurationFile.getName()) != null) {
         throw new ModelObjectAlreadyExistsException("Configuration file name already exists in the JZEE application.");
      }
      this.configurationFiles.add(configurationFile);
   }

   /**
    * Get the <code>ConfigurationFile</code> list in the
    * <code>Application</code> configuration files container.
    * 
    * @return the <code>ConfigurationFile</code> list.
    */
   public List getConfigurationFiles() {
      return this.configurationFiles;
   }

   /**
    * Overwrite the <code>ConfigurationFile</code> list in the
    * <code>Application</code> configuration files container.
    * 
    * @param configurationFiles the new <code>ConfigurationFile</code> list.
    */
   public void setConfigurationFiles(LinkedList configurationFiles) {
      this.configurationFiles = configurationFiles;
   }

   /**
    * Get the <code>ConfigurationFile</code> identified by a given name in the
    * <code>Application</code> configuration files container.
    * 
    * @param name the <code>ConfigurationFile</code> name.
    * @return the <code>ConfigurationFile</code> found or null if not found.
    */
   public ConfigurationFile getConfigurationFile(String name) {
      for (Iterator configurationFileIterator = this.getConfigurationFiles().iterator(); configurationFileIterator.hasNext();) {
         ConfigurationFile configurationFile = (ConfigurationFile) configurationFileIterator.next();
         if (configurationFile.getName().equals(name)) {
            return configurationFile;
         }
      }
      return null;
   }

   /**
    * Add a new <code>Database</code> in the <code>Application</code>
    * databases container.
    * 
    * @param database the <code>Database</code> to add.
    * @throws ModelObjectAlreadyExistsException if the <code>Database</code> name already exists in the application.
    */
   public void addDatabase(Database database) throws ModelObjectAlreadyExistsException {
      if (this.getDatabase(database.getName()) != null) {
         throw new ModelObjectAlreadyExistsException("Database name already exists in the J2EE application.");
      }
      this.databases.add(database);
   }

   /**
    * Get the <code>Database</code> list in the <code>Application</code>
    * databases container.
    * 
    * @return the <code>Database</code> list.
    */
   public List getDatabases() {
      return this.databases;
   }

   /**
    * Overwrite the <code>Database</code> list in the <code>Application</code>
    * databases container.
    * 
    * @param databases the new <code>Database</code> list.
    */
   public void setDatabases(LinkedList databases) {
      this.databases = databases;
   }

   /**
    * Get the <code>Database</code> identified by a given name in the
    * <code>Application</code> databases container.
    * 
    * @param name the <code>Database</code> name.
    * @return the <code>Database</code> found or null if not found.
    */
   public Database getDatabase(String name) {
      for (Iterator databaseIterator = this.getDatabases().iterator(); databaseIterator.hasNext();) {
         Database database = (Database) databaseIterator.next();
         if (database.getName().equals(name)) {
            return database;
         }
      }
      return null;
   }

   /**
    * @see java.lang.Object#clone()
    */
   public Object clone() throws CloneNotSupportedException {
      Application clone = new Application();
      clone.setName(this.getName());
      clone.setUri(this.getUri());
      clone.setActive(this.isActive());
      clone.setBlocker(this.isBlocker());
      clone.setAgent(this.getAgent());
      for (Iterator archiveIterator = this.archives.iterator(); archiveIterator.hasNext(); ) {
          Archive archive = (Archive)archiveIterator.next();
          clone.archives.add((Archive)archive.clone());
      }
      for (Iterator contentManagerIterator = this.contentManagers.iterator(); contentManagerIterator.hasNext(); ) {
          ContentManager contentManager = (ContentManager)contentManagerIterator.next();
          clone.contentManagers.add((ContentManager)contentManager.clone());
      }
      for (Iterator configurationFileIterator = this.configurationFiles.iterator(); configurationFileIterator.hasNext(); ) {
          ConfigurationFile configurationFile = (ConfigurationFile)configurationFileIterator.next();
          clone.configurationFiles.add((ConfigurationFile)configurationFile.clone());
      }
      for (Iterator databaseIterator = this.databases.iterator(); databaseIterator.hasNext(); ) {
          Database database = (Database)databaseIterator.next();
          clone.databases.add((Database)database.clone());
      }
      return clone;
   }

   /**
    * Transforms the <code>Application</code> POJO to a DOM element.
    * 
    * @param document the core DOM document.
    * @return the DOM element.
    */
   protected Element toDOMElement(CoreDocumentImpl document) {
      ElementImpl element = new ElementImpl(document, "application");
      element.setAttribute("name", this.getName());
      element.setAttribute("uri", this.getUri());
      element.setAttribute("active", new Boolean(this.isActive()).toString());
      element.setAttribute("blocker", new Boolean(this.isBlocker()).toString());
      element.setAttribute("agent", this.getAgent());
      // archives
      ElementImpl archives = new ElementImpl(document, "archives");
      for (Iterator archiveIterator = this.getArchives().iterator(); archiveIterator.hasNext();) {
         Archive archive = (Archive) archiveIterator.next();
         archives.appendChild(archive.toDOMElement(document));
      }
      element.appendChild(archives);
      // contentmanagers
      ElementImpl contentmanagers = new ElementImpl(document, "contentmanagers");
      for (Iterator contentManagerIterator = this.getContentManagers().iterator(); contentManagerIterator.hasNext();) {
         ContentManager contentManager = (ContentManager) contentManagerIterator.next();
         contentmanagers.appendChild(contentManager.toDOMElement(document));
      }
      element.appendChild(contentmanagers);
      // configurationfiles
      ElementImpl configurationfiles = new ElementImpl(document, "configurationfiles");
      for (Iterator configurationFileIterator = this.getConfigurationFiles().iterator(); configurationFileIterator.hasNext();) {
         ConfigurationFile configurationFile = (ConfigurationFile) configurationFileIterator.next();
         configurationfiles.appendChild(configurationFile.toDOMElement(document));
      }
      element.appendChild(configurationfiles);
      // databases
      ElementImpl databases = new ElementImpl(document, "databases");
      for (Iterator databaseIterator = this.getDatabases().iterator(); databaseIterator.hasNext();) {
         Database database = (Database) databaseIterator.next();
         databases.appendChild(database.toDOMElement(document));
      }
      element.appendChild(databases);
      return element;
   }
   
   /**
    * @see java.lang.Comparable#compareTo(java.lang.Object)
    */
   public int compareTo(Object anotherApplication) {
       return this.getName().compareTo(((Application)anotherApplication).getName());
   }

}