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

import org.apache.xerces.dom.CDATASectionImpl;
import org.apache.xerces.dom.CoreDocumentImpl;
import org.apache.xerces.dom.ElementImpl;
import org.w3c.dom.Element;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents the <code>jeeapplicationserver</code> tag in the Kalumet DOM.
 */
public class JEEApplicationServer
  implements Serializable, Cloneable, Comparable
{

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

  private LinkedList jdbcConnectionPools;

  private LinkedList jdbcDataSources;

  private LinkedList jmsConnectionFactories;

  private LinkedList jmsServers;

  private LinkedList jndiBindings;

  private LinkedList sharedLibraries;

  private LinkedList jeeApplications;

  private LinkedList caches;

  private LinkedList logAccesses;

  /**
   * Default constructor to create a new <code>JEEApplicationServer</code>.
   */
  public JEEApplicationServer()
  {
    this.jdbcConnectionPools = new LinkedList();
    this.jdbcDataSources = new LinkedList();
    this.jmsConnectionFactories = new LinkedList();
    this.jmsServers = new LinkedList();
    this.jndiBindings = new LinkedList();
    this.sharedLibraries = new LinkedList();
    this.jeeApplications = new LinkedList();
    this.caches = new LinkedList();
    this.logAccesses = new LinkedList();
  }

  public String getName()
  {
    return this.name;
  }

  public void setName( String name )
  {
    this.name = name;
  }

  public boolean isActive()
  {
    return active;
  }

  public void setActive( boolean active )
  {
    this.active = active;
  }

  public boolean isBlocker()
  {
    return blocker;
  }

  public void setBlocker( boolean blocker )
  {
    this.blocker = blocker;
  }

  public String getClassname()
  {
    return this.classname;
  }

  public void setClassname( String classname )
  {
    this.classname = classname;
  }

  public String getJmxurl()
  {
    return this.jmxurl;
  }

  public void setJmxurl( String jmxurl )
  {
    this.jmxurl = jmxurl;
  }

  public String getAdminuser()
  {
    return this.adminuser;
  }

  public void setAdminuser( String adminuser )
  {
    this.adminuser = adminuser;
  }

  public String getAdminpassword()
  {
    return this.adminpassword;
  }

  public void setAdminpassword( String adminpassword )
  {
    this.adminpassword = adminpassword;
  }

  public boolean isUpdateRequireRestart()
  {
    return this.updateRequireRestart;
  }

  public void setUpdateRequireRestart( boolean updateRequireRestart )
  {
    this.updateRequireRestart = updateRequireRestart;
  }

  public boolean isUpdateRequireCacheCleaning()
  {
    return this.updateRequireCacheCleaning;
  }

  public void setUpdateRequireCacheCleaning( boolean updateRequireCacheCleaning )
  {
    this.updateRequireCacheCleaning = updateRequireCacheCleaning;
  }

  public boolean isUsejmxstop()
  {
    return this.usejmxstop;
  }

  public void setUsejmxstop( boolean usejmxstop )
  {
    this.usejmxstop = usejmxstop;
  }

  public boolean isDeletecomponents()
  {
    return this.deletecomponents;
  }

  public void setDeletecomponents( boolean deletecomponents )
  {
    this.deletecomponents = deletecomponents;
  }

  public String getStartupcommand()
  {
    return this.startupcommand;
  }

  public void setStartupcommand( String startupcommand )
  {
    this.startupcommand = startupcommand;
  }

  public String getShutdowncommand()
  {
    return this.shutdowncommand;
  }

  public void setShutdowncommand( String shutdowncommand )
  {
    this.shutdowncommand = shutdowncommand;
  }

  public String getAgent()
  {
    return agent;
  }

  public void setAgent( String agent )
  {
    this.agent = agent;
  }

  /**
   * Add a new <code>JDBCConnectionPool</code> in the
   * <code>JEEApplicationServer</code> connection pools container.
   *
   * @param JDBCConnectionPool the <code>JDBCConnectionPool</code> to add.
   * @throws ModelObjectAlreadyExistsException
   *          if the <code>JDBCConnectionPool</code> name already exists in the application server.
   */
  public void addJDBCConnectionPool( JDBCConnectionPool JDBCConnectionPool )
    throws ModelObjectAlreadyExistsException
  {
    if ( this.getJDBCConnectionPool( JDBCConnectionPool.getName() ) != null )
    {
      throw new ModelObjectAlreadyExistsException( "JDBC connection pool name already exists in the JEE server." );
    }
    this.jdbcConnectionPools.add( JDBCConnectionPool );
  }

  /**
   * Get the <code>JDBCConnectionPool</code> list in the
   * <code>JEEApplicationServer</code> connection pools container.
   *
   * @return the <code>JDBCConnectionPool</code> list.
   */
  public List getJDBCConnectionPools()
  {
    return this.jdbcConnectionPools;
  }

  /**
   * Overwrite the <code>JDBCConnectionPool</code> list in the
   * <code>JEEApplicationServer</code> connection pools container.
   *
   * @param jdbcConnectionPools the new <code>JDBCConnectionPool</code> list.
   */
  public void setJDBCConnectionPools( LinkedList jdbcConnectionPools )
  {
    this.jdbcConnectionPools = jdbcConnectionPools;
  }

  /**
   * Get the <code>JDBCConnectionPool</code> identified by a given name in the
   * <code>JEEApplicationServer</code> connection pools container.
   *
   * @param name the <code>JDBCConnectionPool</code> name.
   * @return the <code>JDBCConnectionPool</code> found or null if not found.
   */
  public JDBCConnectionPool getJDBCConnectionPool( String name )
  {
    for ( Iterator connectionPoolIterator = this.getJDBCConnectionPools().iterator();
          connectionPoolIterator.hasNext(); )
    {
      JDBCConnectionPool JDBCConnectionPool = (JDBCConnectionPool) connectionPoolIterator.next();
      if ( JDBCConnectionPool.getName().equals( name ) )
      {
        return JDBCConnectionPool;
      }
    }
    return null;
  }

  /**
   * Add a new <code>JDBCDataSource</code> in the <code>JEEApplicationServer</code>
   * data sources container.
   *
   * @param JDBCDataSource the <code>JDBCDataSource</code> to add.
   * @throws ModelObjectAlreadyExistsException
   *          if the <code>JDBCDataSource</code> name already exists in the application server.
   */
  public void addJDBCDataSource( JDBCDataSource JDBCDataSource )
    throws ModelObjectAlreadyExistsException
  {
    if ( this.getJDBCDataSource( JDBCDataSource.getName() ) != null )
    {
      throw new ModelObjectAlreadyExistsException( "JDBC data source name already exists in the JEE server." );
    }
    this.jdbcDataSources.add( JDBCDataSource );
  }

  /**
   * Get the <code>JDBCDataSource</code> list in the
   * <code>JEEApplicationServer</code> data sources container.
   *
   * @return the <code>JDBCDataSource</code> list.
   */
  public List getJDBCDataSources()
  {
    return this.jdbcDataSources;
  }

  /**
   * Overwrite the <code>JDBCDataSource</code> list in the
   * <code>JEEApplicationServer</code> data sources container.
   *
   * @param dataSources the new <code>JDBCDataSource</code> list.
   */
  public void setJDBCDataSources( LinkedList dataSources )
  {
    this.jdbcDataSources = dataSources;
  }

  /**
   * Get the <code>JDBCDataSource</code> identified by a given name in the
   * <code>JEEApplicationServer</code> data sources container.
   *
   * @param name the <code>JDBCDataSource</code> name.
   * @return the <code>JDBCDataSource</code> found or null if not found.
   */
  public JDBCDataSource getJDBCDataSource( String name )
  {
    for ( Iterator dataSourceIterator = this.getJDBCDataSources().iterator(); dataSourceIterator.hasNext(); )
    {
      JDBCDataSource JDBCDataSource = (JDBCDataSource) dataSourceIterator.next();
      if ( JDBCDataSource.getName().equals( name ) )
      {
        return JDBCDataSource;
      }
    }
    return null;
  }

  /**
   * Add a new <code>JMSConnectionFactory</code> in the
   * <code>JEEApplicationServer</code> JMS connection factories container.
   *
   * @param jmsConnectionFactory the <code>JMSConnectionFactory</code> to add.
   * @throws ModelObjectAlreadyExistsException
   *
   */
  public void addJMSConnectionFactory( JMSConnectionFactory jmsConnectionFactory )
    throws ModelObjectAlreadyExistsException
  {
    if ( this.getJMSConnectionFactory( jmsConnectionFactory.getName() ) != null )
    {
      throw new ModelObjectAlreadyExistsException( "JMS connection factory name already exists in the JEE server." );
    }
    this.jmsConnectionFactories.add( jmsConnectionFactory );
  }

  /**
   * Get the <code>JMSConnectionFactory</code> list in the
   * <code>JEEApplicationServer</code> JMS connection factories container.
   *
   * @return the <code>JMSConnectionFactory</code> list.
   */
  public List getJMSConnectionFactories()
  {
    return this.jmsConnectionFactories;
  }

  /**
   * Overwrites the <code>JMSConnectionFactory</code> list in the
   * <code>JEEApplicationServer</code> JMS connection factories container.
   *
   * @param jmsConnectionFactories the new <code>JMSConnectionFactory</code> list.
   */
  public void setJMSConnectionFactories( LinkedList jmsConnectionFactories )
  {
    this.jmsConnectionFactories = jmsConnectionFactories;
  }

  /**
   * Gets the <code>JMSConnectionFactory</code> identified by a given name in
   * the <code>JEEApplicationServer</code> JMS connection factories container.
   *
   * @param name the <code>JMSConnectionFactory</code> name.
   * @return the <code>JMSConnectionFactory</code> found or null if not found.
   */
  public JMSConnectionFactory getJMSConnectionFactory( String name )
  {
    for ( Iterator jmsConnectionFactoryIterator = this.getJMSConnectionFactories().iterator();
          jmsConnectionFactoryIterator.hasNext(); )
    {
      JMSConnectionFactory jmsConnectionFactory = (JMSConnectionFactory) jmsConnectionFactoryIterator.next();
      if ( jmsConnectionFactory.getName().equals( name ) )
      {
        return jmsConnectionFactory;
      }
    }
    return null;
  }

  /**
   * Adds a new <code>JMSServer</code> in the <code>JEEApplicationServer</code>
   * JMS servers container.
   *
   * @param jmsServer the <code>JMSServer</code> to add.
   * @throws ModelObjectAlreadyExistsException
   *          if the <code>JMSServer</code> name already exists in the application server.
   */
  public void addJMSServer( JMSServer jmsServer )
    throws ModelObjectAlreadyExistsException
  {
    if ( this.getJMSServer( jmsServer.getName() ) != null )
    {
      throw new ModelObjectAlreadyExistsException( "JMS server name already exists in the JEE application server." );
    }
    this.jmsServers.add( jmsServer );
  }

  /**
   * Gets the <code>JMSServer</code> list in the <code>JEEApplicationServer</code>
   * JMS servers container.
   *
   * @return the <code>JMSServer</code> list.
   */
  public List getJMSServers()
  {
    return this.jmsServers;
  }

  /**
   * Overwrites the <code>JMSServer</code> list in the
   * <code>JEEApplicationServer</code> JMS servers container.
   *
   * @param jmsServers the new <code>JMSServer</code> list.
   */
  public void setJMSServers( LinkedList jmsServers )
  {
    this.jmsServers = jmsServers;
  }

  /**
   * Gets the <code>JMSServer</code> identified by a given name in the
   * <code>JEEApplicationServer</code> JMS servers container.
   *
   * @param name the <code>JMSServer</code> name.
   * @return the <code>JMSServer</code> found or null if not found.
   */
  public JMSServer getJMSServer( String name )
  {
    for ( Iterator jmsServerIterator = this.getJMSServers().iterator(); jmsServerIterator.hasNext(); )
    {
      JMSServer jmsServer = (JMSServer) jmsServerIterator.next();
      if ( jmsServer.getName().equals( name ) )
      {
        return jmsServer;
      }
    }
    return null;
  }

  /**
   * Adds a new <code>JNDIBinding</code> in the
   * <code>JEEApplicationServer</code> name space bindings container.
   *
   * @param JNDIBinding the <code>JNDIBinding</code> to add.
   * @throws ModelObjectAlreadyExistsException
   *          if the <code>JNDIBinding</code> name already exists in the application server.
   */
  public void addJNDIBinding( JNDIBinding JNDIBinding )
    throws ModelObjectAlreadyExistsException
  {
    if ( this.getJNDIBinding( JNDIBinding.getName() ) != null )
    {
      throw new ModelObjectAlreadyExistsException( "Name space binding name already exists in the JEE server." );
    }
    this.jndiBindings.add( JNDIBinding );
  }

  /**
   * Gets the <code>JNDIBinding</code> list in the
   * <code>JEEApplicationServer</code> name space bindings container.
   *
   * @return the <code>JNDIBinding</code> list.
   */
  public List getJNDIBindings()
  {
    return this.jndiBindings;
  }

  /**
   * Overwrites the <code>JNDIBinding</code> list in the
   * <code>JEEApplicationServer</code> name space bindings container.
   *
   * @param jndiBindings the new <code>JNDIBinding</code> list.
   */
  public void setJNDIBindings( LinkedList jndiBindings )
  {
    this.jndiBindings = jndiBindings;
  }

  /**
   * Gets the <code>JNDIBinding</code> identified by a given name in the
   * <code>JEEApplicationServer</code> name space bindings container.
   *
   * @param name the <code>JNDIBinding</code> name.
   * @return the <code>JNDIBinding</code> found or null if not found.
   */
  public JNDIBinding getJNDIBinding( String name )
  {
    for ( Iterator jndiBindingIterator = this.getJNDIBindings().iterator(); jndiBindingIterator.hasNext(); )
    {
      JNDIBinding JNDIBinding = (JNDIBinding) jndiBindingIterator.next();
      if ( JNDIBinding.getName().equals( name ) )
      {
        return JNDIBinding;
      }
    }
    return null;
  }

  /**
   * Adds a new <code>SharedLibrary</code> in the
   * <code>JEEApplicationServer</code> shared libraries container.
   *
   * @param sharedLibrary the <code>SharedLibrary</code> to add.
   * @throws ModelObjectAlreadyExistsException
   *          if the <code>SharedLibrary</code> name already exists in the application server.
   */
  public void addSharedLibrary( SharedLibrary sharedLibrary )
    throws ModelObjectAlreadyExistsException
  {
    if ( this.getSharedLibrary( sharedLibrary.getName() ) != null )
    {
      throw new ModelObjectAlreadyExistsException( "Shared library name already exists in the JEE server." );
    }
    this.sharedLibraries.add( sharedLibrary );
  }

  /**
   * Gets the <code>SharedLibrary</code> list in the
   * <code>JEEApplicationServer</code> shared libraries container.
   *
   * @return the <code>SharedLibrary</code> list.
   */
  public List getSharedLibraries()
  {
    return this.sharedLibraries;
  }

  /**
   * Overwrites the <code>SharedLibrary</code> list in the
   * <code>JEEApplicationServer</code> shared libraries container.
   *
   * @param sharedLibraries the new <code>SharedLibrary</code> list.
   */
  public void setSharedLibraries( LinkedList sharedLibraries )
  {
    this.sharedLibraries = sharedLibraries;
  }

  /**
   * Gets the <code>SharedLibrary</code> identified by a given name in the
   * <code>JEEApplicationServer</code> shared libraries container.
   *
   * @param name the <code>SharedLibrary</code> name.
   * @return the <code>SharedLibrary</code> found or null if not found.
   */
  public SharedLibrary getSharedLibrary( String name )
  {
    for ( Iterator sharedLibraryIterator = this.getSharedLibraries().iterator(); sharedLibraryIterator.hasNext(); )
    {
      SharedLibrary sharedLibrary = (SharedLibrary) sharedLibraryIterator.next();
      if ( sharedLibrary.getName().equals( name ) )
      {
        return sharedLibrary;
      }
    }
    return null;
  }

  /**
   * Adds a new <code>JEEApplication</code> in the <code>JEEApplicationServer</code>
   * jeeApplications container.
   *
   * @param jeeApplication the <code>JEEApplication</code> to add.
   * @throws ModelObjectAlreadyExistsException
   *          if the <code>JEEApplication</code> name already exists in the jeeApplication server.
   */
  public void addJEEApplication( JEEApplication jeeApplication )
    throws ModelObjectAlreadyExistsException
  {
    if ( this.getJEEApplication(jeeApplication.getName()) != null )
    {
      throw new ModelObjectAlreadyExistsException(
        "JEE application name already exists in the JEE application server." );
    }
    this.jeeApplications.add(jeeApplication);
  }

  /**
   * Gets the <code>JEEApplication</code> list in the
   * <code>JEEApplicationServer</code> jeeApplications container.
   *
   * @return the <code>JEEApplication</code> list.
   */
  public List getJEEApplications()
  {
    return this.jeeApplications;
  }

  /**
   * Overwrites the <code>JEEApplication</code> list in the
   * <code>JEEApplicationServer</code> jeeApplications container.
   *
   * @param jeeApplications the new <code>JEEApplication</code> list.
   */
  public void setJEEApplications( LinkedList jeeApplications )
  {
    this.jeeApplications = jeeApplications;
  }

  /**
   * Gets the <code>JEEApplication</code> identified by a given name in the
   * <code>JEEApplicationServer</code> jeeApplications container.
   *
   * @param name the <code>JEEApplication</code> name.
   * @return the <code>JEEApplication</code> found or null if not found.
   */
  public JEEApplication getJEEApplication( String name )
  {
    for ( Iterator applicationIterator = this.getJEEApplications().iterator(); applicationIterator.hasNext(); )
    {
      JEEApplication jeeApplication = (JEEApplication) applicationIterator.next();
      if ( jeeApplication.getName().equals( name ) )
      {
        return jeeApplication;
      }
    }
    return null;
  }

  /**
   * Adds a new <code>Cache</code> in the <code>JEEApplicationServer</code> caches
   * container.
   *
   * @param cache the <code>Cache</code> to add.
   * @throws ModelObjectAlreadyExistsException
   *          if the <code>Cache</code> path already exists in the application server.
   */
  public void addCache( Cache cache )
    throws ModelObjectAlreadyExistsException
  {
    if ( this.getCache( cache.getPath() ) != null )
    {
      throw new ModelObjectAlreadyExistsException( "Cache path already exists in the JEE application server." );
    }
    this.caches.add( cache );
  }

  /**
   * Gets the <code>Cache</code> list in the <code>JEEApplicationServer</code>
   * caches container.
   *
   * @return the <code>Cache</code> list.
   */
  public List getCaches()
  {
    return this.caches;
  }

  /**
   * Overwrites the <code>Cache</code> list in the
   * <code>JEEApplicationServer</code> caches container.
   *
   * @param caches the new <code>Cache</code> list.
   */
  public void setCaches( LinkedList caches )
  {
    this.caches = caches;
  }

  /**
   * Gets the <code>Cache</code> identified by a given path in the
   * <code>JEEApplicationServer</code> caches container.
   *
   * @param path the <code>Cache</code> path.
   * @return the <code>Cache</code> found or null if not found.
   */
  public Cache getCache( String path )
  {
    for ( Iterator cacheIterator = this.getCaches().iterator(); cacheIterator.hasNext(); )
    {
      Cache cache = (Cache) cacheIterator.next();
      if ( cache.getPath().equals( path ) )
      {
        return cache;
      }
    }
    return null;
  }

  /**
   * @see java.lang.Object#clone()
   */
  public Object clone()
    throws CloneNotSupportedException
  {
    JEEApplicationServer clone = new JEEApplicationServer();
    clone.setName( this.getName() );
    clone.setActive( this.isActive() );
    clone.setBlocker( this.isBlocker() );
    clone.setClassname( this.getClassname() );
    clone.setJmxurl( this.getJmxurl() );
    clone.setAdminuser( this.getAdminuser() );
    clone.setAdminpassword( this.getAdminpassword() );
    clone.setUpdateRequireRestart( this.isUpdateRequireRestart() );
    clone.setUpdateRequireCacheCleaning( this.isUpdateRequireCacheCleaning() );
    clone.setUsejmxstop( this.isUsejmxstop() );
    clone.setDeletecomponents( this.isDeletecomponents() );
    clone.setStartupcommand( this.getStartupcommand() );
    clone.setShutdowncommand( this.getShutdowncommand() );
    clone.setAgent( this.getAgent() );
    for ( Iterator jdbcConnectionPoolIterator = this.jdbcConnectionPools.iterator();
          jdbcConnectionPoolIterator.hasNext(); )
    {
      JDBCConnectionPool JDBCConnectionPool = (JDBCConnectionPool) jdbcConnectionPoolIterator.next();
      clone.jdbcConnectionPools.add( (JDBCConnectionPool) JDBCConnectionPool.clone() );
    }
    for ( Iterator jdbcDataSourceIterator = this.jdbcDataSources.iterator(); jdbcDataSourceIterator.hasNext(); )
    {
      JDBCDataSource JDBCDataSource = (JDBCDataSource) jdbcDataSourceIterator.next();
      clone.jdbcDataSources.add( (JDBCDataSource) JDBCDataSource.clone() );
    }
    for ( Iterator jmsConnectionFactoryIterator = this.jmsConnectionFactories.iterator();
          jmsConnectionFactoryIterator.hasNext(); )
    {
      JMSConnectionFactory jmsConnectionFactory = (JMSConnectionFactory) jmsConnectionFactoryIterator.next();
      clone.jmsConnectionFactories.add( (JMSConnectionFactory) jmsConnectionFactory.clone() );
    }
    for ( Iterator jmsServerIterator = this.jmsServers.iterator(); jmsServerIterator.hasNext(); )
    {
      JMSServer jmsServer = (JMSServer) jmsServerIterator.next();
      clone.jmsServers.add( (JMSServer) jmsServer.clone() );
    }
    for ( Iterator jndiBindingIterator = this.jndiBindings.iterator(); jndiBindingIterator.hasNext(); )
    {
      JNDIBinding JNDIBinding = (JNDIBinding) jndiBindingIterator.next();
      clone.jndiBindings.add( (JNDIBinding) JNDIBinding.clone() );
    }
    for ( Iterator sharedLibraryIterator = this.sharedLibraries.iterator(); sharedLibraryIterator.hasNext(); )
    {
      SharedLibrary sharedLibrary = (SharedLibrary) sharedLibraryIterator.next();
      clone.sharedLibraries.add( (SharedLibrary) sharedLibrary.clone() );
    }
    for ( Iterator applicationIterator = this.jeeApplications.iterator(); applicationIterator.hasNext(); )
    {
      JEEApplication jeeApplication = (JEEApplication) applicationIterator.next();
      clone.jeeApplications.add( (JEEApplication) jeeApplication.clone() );
    }
    for ( Iterator cacheIterator = this.caches.iterator(); cacheIterator.hasNext(); )
    {
      Cache cache = (Cache) cacheIterator.next();
      clone.caches.add( (Cache) cache.clone() );
    }
    return clone;
  }

  /**
   * Transforms the <code>JEEApplicationServer</code> POJO to a DOM element.
   *
   * @param document the core XML document.
   * @return the DOM element.
   */
  protected Element toDOMElement( CoreDocumentImpl document )
  {
    ElementImpl element = new ElementImpl( document, "jeeapplicationserver" );
    element.setAttribute( "name", this.getName() );
    element.setAttribute( "active", new Boolean( this.isActive() ).toString() );
    element.setAttribute( "blocker", new Boolean( this.isBlocker() ).toString() );
    element.setAttribute( "classname", this.getClassname() );
    element.setAttribute( "jmxurl", this.getJmxurl() );
    element.setAttribute( "adminuser", this.getAdminuser() );
    element.setAttribute( "adminpassword", this.getAdminpassword() );
    element.setAttribute( "updateRequireRestart", new Boolean( this.isUpdateRequireRestart() ).toString() );
    element.setAttribute( "updateRequireCacheCleaning", new Boolean( this.isUpdateRequireCacheCleaning() ).toString() );
    element.setAttribute( "usejmxstop", new Boolean( this.isUsejmxstop() ).toString() );
    element.setAttribute( "deletecomponents", new Boolean( this.isDeletecomponents() ).toString() );
    element.setAttribute( "agent", this.getAgent() );
    // add startup command
    ElementImpl startupcommand = new ElementImpl( document, "startupcommand" );
    CDATASectionImpl startupcommandContent = new CDATASectionImpl( document, this.getStartupcommand() );
    startupcommand.appendChild( startupcommandContent );
    element.appendChild( startupcommand );
    // add shutdown command
    ElementImpl shutdowncommand = new ElementImpl( document, "shutdowncommand" );
    CDATASectionImpl shutdowncommandContent = new CDATASectionImpl( document, this.getShutdowncommand() );
    shutdowncommand.appendChild( shutdowncommandContent );
    element.appendChild( shutdowncommand );
    // connectionpools
    ElementImpl jdbcconnectionpools = new ElementImpl( document, "jdbcconnectionpools" );
    for ( Iterator jdbcConnectionPoolIterator = this.getJDBCConnectionPools().iterator();
          jdbcConnectionPoolIterator.hasNext(); )
    {
      JDBCConnectionPool JDBCConnectionPool = (JDBCConnectionPool) jdbcConnectionPoolIterator.next();
      jdbcconnectionpools.appendChild( JDBCConnectionPool.toDOMElement( document ) );
    }
    element.appendChild( jdbcconnectionpools );
    // jdbcdatasources
    ElementImpl jdbcdatasources = new ElementImpl( document, "jdbcdatasources" );
    for ( Iterator dataSourceIterator = this.getJDBCDataSources().iterator(); dataSourceIterator.hasNext(); )
    {
      JDBCDataSource JDBCDataSource = (JDBCDataSource) dataSourceIterator.next();
      jdbcdatasources.appendChild( JDBCDataSource.toDOMElement( document ) );
    }
    element.appendChild( jdbcdatasources );
    // jmsconnectionfactories
    ElementImpl jmsconnectionfactories = new ElementImpl( document, "jmsconnectionfactories" );
    for ( Iterator jmsConnectionFactoryIterator = this.getJMSConnectionFactories().iterator();
          jmsConnectionFactoryIterator.hasNext(); )
    {
      JMSConnectionFactory jmsConnectionFactory = (JMSConnectionFactory) jmsConnectionFactoryIterator.next();
      jmsconnectionfactories.appendChild( jmsConnectionFactory.toDOMElement( document ) );
    }
    element.appendChild( jmsconnectionfactories );
    // jmsservers
    ElementImpl jmsservers = new ElementImpl( document, "jmsservers" );
    for ( Iterator jmsServerIterator = this.getJMSServers().iterator(); jmsServerIterator.hasNext(); )
    {
      JMSServer jmsServer = (JMSServer) jmsServerIterator.next();
      jmsservers.appendChild( jmsServer.toDOMElement( document ) );
    }
    element.appendChild( jmsservers );
    // jndibindings
    ElementImpl jndiBindings = new ElementImpl( document, "jndibindings" );
    for ( Iterator jndiBindingIterator = this.getJNDIBindings().iterator(); jndiBindingIterator.hasNext(); )
    {
      JNDIBinding JNDIBinding = (JNDIBinding) jndiBindingIterator.next();
      jndiBindings.appendChild( JNDIBinding.toDOMElement( document ) );
    }
    element.appendChild( jndiBindings );
    // sharedlibraries
    ElementImpl sharedlibraries = new ElementImpl( document, "sharedlibrairies" );
    for ( Iterator sharedLibraryIterator = this.getSharedLibraries().iterator(); sharedLibraryIterator.hasNext(); )
    {
      SharedLibrary sharedLibrary = (SharedLibrary) sharedLibraryIterator.next();
      sharedlibraries.appendChild( sharedLibrary.toDOMElement( document ) );
    }
    element.appendChild( sharedlibraries );
    // jeeapplications
    ElementImpl applications = new ElementImpl( document, "jeeapplications" );
    for ( Iterator applicationIterator = this.getJEEApplications().iterator(); applicationIterator.hasNext(); )
    {
      JEEApplication jeeApplication = (JEEApplication) applicationIterator.next();
      applications.appendChild(jeeApplication.toDOMElement(document));
    }
    element.appendChild( applications );
    // caches
    ElementImpl caches = new ElementImpl( document, "caches" );
    for ( Iterator cacheIterator = this.getCaches().iterator(); cacheIterator.hasNext(); )
    {
      Cache cache = (Cache) cacheIterator.next();
      caches.appendChild( cache.toDOMElement( document ) );
    }
    element.appendChild( caches );
    return element;
  }

  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo( Object anotherApplicationServer )
  {
    return this.getName().compareTo( ( (JEEApplicationServer) anotherApplicationServer ).getName() );
  }

}