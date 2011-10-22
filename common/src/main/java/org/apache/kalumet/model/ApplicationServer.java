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
 * Represents the <code>applicationserver</code> tag in the Kalumet DOM.
 */
public class ApplicationServer implements Serializable, Cloneable, Comparable {

    private static final long serialVersionUID = 2272703476099937797L;

    private String name;
    private boolean active;
    private boolean blocker;
    private String classname;
    private String jmxurl;
    private String adminuser;
    private String adminpassword;
    private boolean updateRequireRestart;
    private boolean updateRequireCacheCleaning;
    private boolean usejmxstop;
    private boolean deletecomponents;
    private String startupcommand;
    private String shutdowncommand;
    private String agent;
    private LinkedList connectionPools;
    private LinkedList dataSources;
    private LinkedList jmsConnectionFactories;
    private LinkedList jmsServers;
    private LinkedList nameSpaceBindings;
    private LinkedList sharedLibraries;
    private LinkedList applications;
    private LinkedList caches;
    private LinkedList logAccesses;

    /**
     * Default constructor to create a new <code>ApplicationServer</code>.
     */
    public ApplicationServer() {
        this.connectionPools = new LinkedList();
        this.dataSources = new LinkedList();
        this.jmsConnectionFactories = new LinkedList();
        this.jmsServers = new LinkedList();
        this.nameSpaceBindings = new LinkedList();
        this.sharedLibraries = new LinkedList();
        this.applications = new LinkedList();
        this.caches = new LinkedList();
        this.logAccesses = new LinkedList();
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isBlocker() {
        return blocker;
    }

    public void setBlocker(boolean blocker) {
        this.blocker = blocker;
    }

    public String getClassname() {
        return this.classname;
    }

    public void setClassname(String classname) {
        this.classname = classname;
    }

    public String getJmxurl() {
        return this.jmxurl;
    }

    public void setJmxurl(String jmxurl) {
        this.jmxurl = jmxurl;
    }

    public String getAdminuser() {
        return this.adminuser;
    }

    public void setAdminuser(String adminuser) {
        this.adminuser = adminuser;
    }

    public String getAdminpassword() {
        return this.adminpassword;
    }

    public void setAdminpassword(String adminpassword) {
        this.adminpassword = adminpassword;
    }

    public boolean getUpdateRequireRestart() {
        return this.updateRequireRestart;
    }

    public void setUpdateRequireRestart(boolean updateRequireRestart) {
        this.updateRequireRestart = updateRequireRestart;
    }

    public boolean isUpdateRequireCacheCleaning() {
        return this.updateRequireCacheCleaning;
    }

    public void setUpdateRequireCacheCleaning(boolean updateRequireCacheCleaning) {
        this.updateRequireCacheCleaning = updateRequireCacheCleaning;
    }

    public boolean isUsejmxstop() {
        return this.usejmxstop;
    }

    public void setUsejmxstop(boolean usejmxstop) {
        this.usejmxstop = usejmxstop;
    }

    public boolean isDeletecomponents() {
        return this.deletecomponents;
    }

    public void setDeletecomponents(boolean deletecomponents) {
        this.deletecomponents = deletecomponents;
    }

    public String getStartupcommand() {
        return this.startupcommand;
    }

    public void setStartupcommand(String startupcommand) {
        this.startupcommand = startupcommand;
    }

    public String getShutdowncommand() {
        return this.shutdowncommand;
    }

    public void setShutdowncommand(String shutdowncommand) {
        this.shutdowncommand = shutdowncommand;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    /**
     * Add a new <code>ConnectionPool</code> in the
     * <code>ApplicationServer</code> connection pools container.
     * 
     * @param connectionPool the <code>ConnectionPool</code> to add.
     * @throws ModelObjectAlreadyExistsException if the <code>ConnectionPool</code> name already exists in the application server.
     */
    public void addConnectionPool(ConnectionPool connectionPool) throws ModelObjectAlreadyExistsException {
        if (this.getConnectionPool(connectionPool.getName()) != null) {
            throw new ModelObjectAlreadyExistsException("Connection pool name already exists in the JZEE server.");
        }
        this.connectionPools.add(connectionPool);
    }

    /**
     * Get the <code>ConnectionPool</code> list in the
     * <code>ApplicationServer</code> connection pools container.
     * 
     * @return the <code>ConnectionPool</code> list.
     */
    public List getConnectionPools() {
        return this.connectionPools;
    }

    /**
     * Overwrite the <code>ConnectionPool</code> list in the
     * <code>ApplicationServer</code> connection pools container.
     * 
     * @param connectionPools the new <code>ConnectionPool</code> list.
     */
    public void setConnectionPools(LinkedList connectionPools) {
        this.connectionPools = connectionPools;
    }

    /**
     * Get the <code>ConnectionPool</code> identified by a given name in the
     * <code>ApplicationServer</code> connection pools container.
     * 
     * @param name the <code>ConnectionPool</code> name.
     * @return the <code>ConnectionPool</code> found or null if not found.
     */
    public ConnectionPool getConnectionPool(String name) {
        for (Iterator connectionPoolIterator = this.getConnectionPools().iterator(); connectionPoolIterator.hasNext();) {
            ConnectionPool connectionPool = (ConnectionPool) connectionPoolIterator.next();
            if (connectionPool.getName().equals(name)) {
                return connectionPool;
            }
        }
        return null;
    }

    /**
     * Add a new <code>DataSource</code> in the <code>ApplicationServer</code>
     * data sources container.
     * 
     * @param dataSource the <code>DataSource</code> to add.
     * @throws ModelObjectAlreadyExistsException if the <code>DataSource</code> name already exists in the application server.
     */
    public void addDataSource(DataSource dataSource) throws ModelObjectAlreadyExistsException {
        if (this.getDataSource(dataSource.getName()) != null) {
            throw new ModelObjectAlreadyExistsException("Datasource name already exists in the JZEE server.");
        }
        this.dataSources.add(dataSource);
    }

    /**
     * Get the <code>DataSource</code> list in the
     * <code>ApplicationServer</code> data sources container.
     * 
     * @return the <code>DataSource</code> list.
     */
    public List getDataSources() {
        return this.dataSources;
    }

    /**
     * Overwrite the <code>DataSource</code> list in the
     * <code>ApplicationServer</code> data sources container.
     * 
     * @param dataSources the new <code>DataSource</code> list.
     */
    public void setDataSources(LinkedList dataSources) {
        this.dataSources = dataSources;
    }

    /**
     * Get the <code>DataSource</code> identified by a given name in the
     * <code>ApplicationServer</code> data sources container.
     * 
     * @param name the <code>DataSource</code> name.
     * @return the <code>DataSource</code> found or null if not found.
     */
    public DataSource getDataSource(String name) {
        for (Iterator dataSourceIterator = this.getDataSources().iterator(); dataSourceIterator.hasNext();) {
            DataSource dataSource = (DataSource) dataSourceIterator.next();
            if (dataSource.getName().equals(name)) {
                return dataSource;
            }
        }
        return null;
    }

    /**
     * Add a new <code>JMSConnectionFactory</code> in the
     * <code>ApplicationServer</code> JMS connection factories container.
     * 
     * @param jmsConnectionFactory the <code>JMSConnectionFactory</code> to add.
     * @throws ModelObjectAlreadyExistsException 
     */
    public void addJMSConnectionFactory(JMSConnectionFactory jmsConnectionFactory) throws ModelObjectAlreadyExistsException {
        if (this.getJMSConnectionFactory(jmsConnectionFactory.getName()) != null) {
            throw new ModelObjectAlreadyExistsException("JMS connection factory name already exists in the JZEE server.");
        }
        this.jmsConnectionFactories.add(jmsConnectionFactory);
    }

    /**
     * Get the <code>JMSConnectionFactory</code> list in the
     * <code>ApplicationServer</code> JMS connection factories container.
     * 
     * @return the <code>JMSConnectionFactory</code> list.
     */
    public List getJMSConnectionFactories() {
        return this.jmsConnectionFactories;
    }

    /**
     * Overwrites the <code>JMSConnectionFactory</code> list in the
     * <code>ApplicationServer</code> JMS connection factories container.
     * 
     * @param jmsConnectionFactories the new <code>JMSConnectionFactory</code> list.
     */
    public void setJMSConnectionFactories(LinkedList jmsConnectionFactories) {
        this.jmsConnectionFactories = jmsConnectionFactories;
    }

    /**
     * Gets the <code>JMSConnectionFactory</code> identified by a given name in
     * the <code>ApplicationServer</code> JMS connection factories container.
     * 
     * @param name the <code>JMSConnectionFactory</code> name.
     * @return the <code>JMSConnectionFactory</code> found or null if not found.
     */
    public JMSConnectionFactory getJMSConnectionFactory(String name) {
        for (Iterator jmsConnectionFactoryIterator = this.getJMSConnectionFactories().iterator(); jmsConnectionFactoryIterator.hasNext();) {
            JMSConnectionFactory jmsConnectionFactory = (JMSConnectionFactory) jmsConnectionFactoryIterator.next();
            if (jmsConnectionFactory.getName().equals(name)) {
                return jmsConnectionFactory;
            }
        }
        return null;
    }

    /**
     * Adds a new <code>JMSServer</code> in the <code>ApplicationServer</code>
     * JMS servers container.
     * 
     * @param jmsServer the <code>JMSServer</code> to add.
     * @throws ModelObjectAlreadyExistsException if the <code>JMSServer</code> name already exists in the application server.
     */
    public void addJMSServer(JMSServer jmsServer) throws ModelObjectAlreadyExistsException {
        if (this.getJMSServer(jmsServer.getName()) != null) {
            throw new ModelObjectAlreadyExistsException("JMS server name already exists in the JZEE server.");
        }
        this.jmsServers.add(jmsServer);
    }

    /**
     * Gets the <code>JMSServer</code> list in the <code>ApplicationServer</code>
     * JMS servers container.
     * 
     * @return the <code>JMSServer</code> list.
     */
    public List getJMSServers() {
        return this.jmsServers;
    }

    /**
     * Overwrites the <code>JMSServer</code> list in the
     * <code>ApplicationServer</code> JMS servers container.
     * 
     * @param jmsServers the new <code>JMSServer</code> list.
     */
    public void setJMSServers(LinkedList jmsServers) {
        this.jmsServers = jmsServers;
    }

    /**
     * Gets the <code>JMSServer</code> identified by a given name in the
     * <code>ApplicationServer</code> JMS servers container.
     * 
     * @param name the <code>JMSServer</code> name.
     * @return the <code>JMSServer</code> found or null if not found.
     */
    public JMSServer getJMSServer(String name) {
        for (Iterator jmsServerIterator = this.getJMSServers().iterator(); jmsServerIterator.hasNext();) {
            JMSServer jmsServer = (JMSServer) jmsServerIterator.next();
            if (jmsServer.getName().equals(name)) {
                return jmsServer;
            }
        }
        return null;
    }

    /**
     * Adds a new <code>NameSpaceBinding</code> in the
     * <code>ApplicationServer</code> name space bindings container.
     * 
     * @param nameSpaceBinding the <code>NameSpaceBinding</code> to add.
     * @throws ModelObjectAlreadyExistsException if the <code>NameSpaceBinding</code> name already exists in the application server.
     */
    public void addNameSpaceBinding(NameSpaceBinding nameSpaceBinding) throws ModelObjectAlreadyExistsException {
        if (this.getNameSpaceBinding(nameSpaceBinding.getName()) != null) {
            throw new ModelObjectAlreadyExistsException("Name space binding name already exists in the JZEE server.");
        }
        this.nameSpaceBindings.add(nameSpaceBinding);
    }

    /**
     * Gets the <code>NameSpaceBinding</code> list in the
     * <code>ApplicationServer</code> name space bindings container.
     * 
     * @return the <code>NameSpaceBinding</code> list.
     */
    public List getNameSpaceBindings() {
        return this.nameSpaceBindings;
    }

    /**
     * Overwrites the <code>NameSpaceBinding</code> list in the
     * <code>ApplicationServer</code> name space bindings container.
     * 
     * @param nameSpaceBindings the new <code>NameSpaceBinding</code> list.
     */
    public void setNameSpaceBindings(LinkedList nameSpaceBindings) {
        this.nameSpaceBindings = nameSpaceBindings;
    }

    /**
     * Gets the <code>NameSpaceBinding</code> identified by a given name in the
     * <code>ApplicationServer</code> name space bindings container.
     * 
     * @param name the <code>NameSpaceBinding</code> name.
     * @return the <code>NameSpaceBinding</code> found or null if not found.
     */
    public NameSpaceBinding getNameSpaceBinding(String name) {
        for (Iterator nameSpaceBindingIterator = this.getNameSpaceBindings().iterator(); nameSpaceBindingIterator.hasNext();) {
            NameSpaceBinding nameSpaceBinding = (NameSpaceBinding) nameSpaceBindingIterator.next();
            if (nameSpaceBinding.getName().equals(name)) {
                return nameSpaceBinding;
            }
        }
        return null;
    }

    /**
     * Adds a new <code>SharedLibrary</code> in the
     * <code>ApplicationServer</code> shared libraries container.
     * 
     * @param sharedLibrary the <code>SharedLibrary</code> to add.
     * @throws ModelObjectAlreadyExistsException if the <code>SharedLibrary</code> name already exists in the application server.
     */
    public void addSharedLibrary(SharedLibrary sharedLibrary) throws ModelObjectAlreadyExistsException {
        if (this.getSharedLibrary(sharedLibrary.getName()) != null) {
            throw new ModelObjectAlreadyExistsException("Shared library name already exists in the JZEE server.");
        }
        this.sharedLibraries.add(sharedLibrary);
    }

    /**
     * Gets the <code>SharedLibrary</code> list in the
     * <code>ApplicationServer</code> shared libraries container.
     * 
     * @return the <code>SharedLibrary</code> list.
     */
    public List getSharedLibraries() {
        return this.sharedLibraries;
    }

    /**
     * Overwrites the <code>SharedLibrary</code> list in the
     * <code>ApplicationServer</code> shared libraries container.
     * 
     * @param sharedLibraries the new <code>SharedLibrary</code> list.
     */
    public void setSharedLibraries(LinkedList sharedLibraries) {
        this.sharedLibraries = sharedLibraries;
    }

    /**
     * Gets the <code>SharedLibrary</code> identified by a given name in the
     * <code>ApplicationServer</code> shared libraries container.
     * 
     * @param name the <code>SharedLibrary</code> name.
     * @return the <code>SharedLibrary</code> found or null if not found.
     */
    public SharedLibrary getSharedLibrary(String name) {
        for (Iterator sharedLibraryIterator = this.getSharedLibraries().iterator(); sharedLibraryIterator.hasNext();) {
            SharedLibrary sharedLibrary = (SharedLibrary) sharedLibraryIterator.next();
            if (sharedLibrary.getName().equals(name)) {
                return sharedLibrary;
            }
        }
        return null;
    }

    /**
     * Adds a new <code>Application</code> in the <code>ApplicationServer</code>
     * applications container.
     * 
     * @param application the <code>Application</code> to add.
     * @throws ModelObjectAlreadyExistsException if the <code>Application</code> name already exists in the application server.
     */
    public void addApplication(Application application) throws ModelObjectAlreadyExistsException {
        if (this.getApplication(application.getName()) != null) {
            throw new ModelObjectAlreadyExistsException("JZEE application name already exists in the JZEE server.");
        }
        this.applications.add(application);
    }

    /**
     * Gets the <code>Application</code> list in the
     * <code>ApplicationServer</code> applications container.
     * 
     * @return the <code>Application</code> list.
     */
    public List getApplications() {
        return this.applications;
    }

    /**
     * Overwrites the <code>Application</code> list in the
     * <code>ApplicationServer</code> applications container.
     * 
     * @param applications the new <code>Application</code> list.
     */
    public void setApplications(LinkedList applications) {
        this.applications = applications;
    }

    /**
     * Gets the <code>Application</code> identified by a given name in the
     * <code>ApplicationServer</code> applications container.
     * 
     * @param name the <code>Application</code> name.
     * @return the <code>Application</code> found or null if not found.
     */
    public Application getApplication(String name) {
        for (Iterator applicationIterator = this.getApplications().iterator(); applicationIterator.hasNext();) {
            Application application = (Application) applicationIterator.next();
            if (application.getName().equals(name)) {
                return application;
            }
        }
        return null;
    }

    /**
     * Adds a new <code>Cache</code> in the <code>ApplicationServer</code> caches
     * container.
     * 
     * @param cache the <code>Cache</code> to add.
     * @throws ModelObjectAlreadyExistsException if the <code>Cache</code> path already exists in the application server.
     */
    public void addCache(Cache cache) throws ModelObjectAlreadyExistsException {
        if (this.getCache(cache.getPath()) != null) {
            throw new ModelObjectAlreadyExistsException("Cache path already exists in the J2EE server.");
        }
        this.caches.add(cache);
    }

    /**
     * Gets the <code>Cache</code> list in the <code>ApplicationServer</code>
     * caches container.
     * 
     * @return the <code>Cache</code> list.
     */
    public List getCaches() {
        return this.caches;
    }

    /**
     * Overwrites the <code>Cache</code> list in the
     * <code>ApplicationServer</code> caches container.
     * 
     * @param caches the new <code>Cache</code> list.
     */
    public void setCaches(LinkedList caches) {
        this.caches = caches;
    }

    /**
     * Gets the <code>Cache</code> identified by a given path in the
     * <code>ApplicationServer</code> caches container.
     * 
     * @param path the <code>Cache</code> path.
     * @return the <code>Cache</code> found or null if not found.
     */
    public Cache getCache(String path) {
        for (Iterator cacheIterator = this.getCaches().iterator(); cacheIterator.hasNext();) {
            Cache cache = (Cache) cacheIterator.next();
            if (cache.getPath().equals(path)) {
                return cache;
            }
        }
        return null;
    }

    /**
     * @see java.lang.Object#clone()
     */
    public Object clone() throws CloneNotSupportedException {
        ApplicationServer clone = new ApplicationServer();
        clone.setName(this.getName());
        clone.setActive(this.isActive());
        clone.setBlocker(this.isBlocker());
        clone.setClassname(this.getClassname());
        clone.setJmxurl(this.getJmxurl());
        clone.setAdminuser(this.getAdminuser());
        clone.setAdminpassword(this.getAdminpassword());
        clone.setUpdateRequireRestart(this.getUpdateRequireRestart());
        clone.setUpdateRequireCacheCleaning(this.isUpdateRequireCacheCleaning());
        clone.setUsejmxstop(this.isUsejmxstop());
        clone.setDeletecomponents(this.isDeletecomponents());
        clone.setStartupcommand(this.getStartupcommand());
        clone.setShutdowncommand(this.getShutdowncommand());
        clone.setAgent(this.getAgent());
        for (Iterator connectionPoolIterator = this.connectionPools.iterator(); connectionPoolIterator.hasNext();) {
            ConnectionPool connectionPool = (ConnectionPool) connectionPoolIterator.next();
            clone.connectionPools.add((ConnectionPool) connectionPool.clone());
        }
        for (Iterator dataSourceIterator = this.dataSources.iterator(); dataSourceIterator.hasNext();) {
            DataSource dataSource = (DataSource) dataSourceIterator.next();
            clone.dataSources.add((DataSource) dataSource.clone());
        }
        for (Iterator jmsConnectionFactoryIterator = this.jmsConnectionFactories.iterator(); jmsConnectionFactoryIterator
                .hasNext();) {
            JMSConnectionFactory jmsConnectionFactory = (JMSConnectionFactory) jmsConnectionFactoryIterator.next();
            clone.jmsConnectionFactories.add((JMSConnectionFactory) jmsConnectionFactory.clone());
        }
        for (Iterator jmsServerIterator = this.jmsServers.iterator(); jmsServerIterator.hasNext();) {
            JMSServer jmsServer = (JMSServer) jmsServerIterator.next();
            clone.jmsServers.add((JMSServer) jmsServer.clone());
        }
        for (Iterator nameSpaceBindingIterator = this.nameSpaceBindings.iterator(); nameSpaceBindingIterator.hasNext();) {
            NameSpaceBinding nameSpaceBinding = (NameSpaceBinding) nameSpaceBindingIterator.next();
            clone.nameSpaceBindings.add((NameSpaceBinding) nameSpaceBinding.clone());
        }
        for (Iterator sharedLibraryIterator = this.sharedLibraries.iterator(); sharedLibraryIterator.hasNext();) {
            SharedLibrary sharedLibrary = (SharedLibrary) sharedLibraryIterator.next();
            clone.sharedLibraries.add((SharedLibrary) sharedLibrary.clone());
        }
        for (Iterator applicationIterator = this.applications.iterator(); applicationIterator.hasNext();) {
            Application application = (Application) applicationIterator.next();
            clone.applications.add((Application) application.clone());
        }
        for (Iterator cacheIterator = this.caches.iterator(); cacheIterator.hasNext();) {
            Cache cache = (Cache) cacheIterator.next();
            clone.caches.add((Cache) cache.clone());
        }
        return clone;
    }

    /**
     * Transforms the <code>ApplicationServer</code> POJO to a DOM element.
     * 
     * @param document the core XML document.
     * @return the DOM element.
     */
    protected Element toDOMElement(CoreDocumentImpl document) {
        ElementImpl element = new ElementImpl(document, "applicationserver");
        element.setAttribute("name", this.getName());
        element.setAttribute("active", new Boolean(this.isActive()).toString());
        element.setAttribute("blocker", new Boolean(this.isBlocker()).toString());
        element.setAttribute("classname", this.getClassname());
        element.setAttribute("jmxurl", this.getJmxurl());
        element.setAttribute("adminuser", this.getAdminuser());
        element.setAttribute("adminpassword", this.getAdminpassword());
        element.setAttribute("updateRequireRestart", new Boolean(this.getUpdateRequireRestart()).toString());
        element.setAttribute("updateRequireCacheCleaning", new Boolean(this.isUpdateRequireCacheCleaning()).toString());
        element.setAttribute("usejmxstop", new Boolean(this.isUsejmxstop()).toString());
        element.setAttribute("deletecomponents", new Boolean(this.isDeletecomponents()).toString());
        element.setAttribute("agent", this.getAgent());
        // add startup command
        ElementImpl startupcommand = new ElementImpl(document, "startupcommand");
        CDATASectionImpl startupcommandContent = new CDATASectionImpl(document, this.getStartupcommand());
        startupcommand.appendChild(startupcommandContent);
        element.appendChild(startupcommand);
        // add shutdown command
        ElementImpl shutdowncommand = new ElementImpl(document, "shutdowncommand");
        CDATASectionImpl shutdowncommandContent = new CDATASectionImpl(document, this.getShutdowncommand());
        shutdowncommand.appendChild(shutdowncommandContent);
        element.appendChild(shutdowncommand);
        // connectionpools
        ElementImpl connectionpools = new ElementImpl(document, "connectionpools");
        for (Iterator connectionPoolIterator = this.getConnectionPools().iterator(); connectionPoolIterator.hasNext();) {
            ConnectionPool connectionPool = (ConnectionPool) connectionPoolIterator.next();
            connectionpools.appendChild(connectionPool.toDOMElement(document));
        }
        element.appendChild(connectionpools);
        // datasources
        ElementImpl datasources = new ElementImpl(document, "datasources");
        for (Iterator dataSourceIterator = this.getDataSources().iterator(); dataSourceIterator.hasNext();) {
            DataSource dataSource = (DataSource) dataSourceIterator.next();
            datasources.appendChild(dataSource.toDOMElement(document));
        }
        element.appendChild(datasources);
        // jmsconnectionfactories
        ElementImpl jmsconnectionfactories = new ElementImpl(document, "jmsconnectionfactories");
        for (Iterator jmsConnectionFactoryIterator = this.getJMSConnectionFactories().iterator(); jmsConnectionFactoryIterator
                .hasNext();) {
            JMSConnectionFactory jmsConnectionFactory = (JMSConnectionFactory) jmsConnectionFactoryIterator.next();
            jmsconnectionfactories.appendChild(jmsConnectionFactory.toDOMElement(document));
        }
        element.appendChild(jmsconnectionfactories);
        // jmsservers
        ElementImpl jmsservers = new ElementImpl(document, "jmsservers");
        for (Iterator jmsServerIterator = this.getJMSServers().iterator(); jmsServerIterator.hasNext();) {
            JMSServer jmsServer = (JMSServer) jmsServerIterator.next();
            jmsservers.appendChild(jmsServer.toDOMElement(document));
        }
        element.appendChild(jmsservers);
        // namespacebindings
        ElementImpl namespacebindings = new ElementImpl(document, "namespacebindings");
        for (Iterator nameSpaceBindingIterator = this.getNameSpaceBindings().iterator(); nameSpaceBindingIterator
                .hasNext();) {
            NameSpaceBinding nameSpaceBinding = (NameSpaceBinding) nameSpaceBindingIterator.next();
            namespacebindings.appendChild(nameSpaceBinding.toDOMElement(document));
        }
        element.appendChild(namespacebindings);
        // sharedlibraries
        ElementImpl sharedlibraries = new ElementImpl(document, "sharedlibrairies");
        for (Iterator sharedLibraryIterator = this.getSharedLibraries().iterator(); sharedLibraryIterator.hasNext();) {
            SharedLibrary sharedLibrary = (SharedLibrary) sharedLibraryIterator.next();
            sharedlibraries.appendChild(sharedLibrary.toDOMElement(document));
        }
        element.appendChild(sharedlibraries);
        // applications
        ElementImpl applications = new ElementImpl(document, "applications");
        for (Iterator applicationIterator = this.getApplications().iterator(); applicationIterator.hasNext();) {
            Application application = (Application) applicationIterator.next();
            applications.appendChild(application.toDOMElement(document));
        }
        element.appendChild(applications);
        // caches
        ElementImpl caches = new ElementImpl(document, "caches");
        for (Iterator cacheIterator = this.getCaches().iterator(); cacheIterator.hasNext();) {
            Cache cache = (Cache) cacheIterator.next();
            caches.appendChild(cache.toDOMElement(document));
        }
        element.appendChild(caches);
        return element;
    }
    
    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object anotherApplicationServer) {
        return this.getName().compareTo(((ApplicationServer)anotherApplicationServer).getName());
    }

}