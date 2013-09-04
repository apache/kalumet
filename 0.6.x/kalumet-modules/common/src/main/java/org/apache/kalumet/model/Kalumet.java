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

import EDU.oswego.cs.dl.util.concurrent.WriterPreferenceReadWriteLock;
import org.apache.commons.digester.Digester;
import org.apache.kalumet.FileManipulator;
import org.apache.kalumet.KalumetException;
import org.apache.xerces.dom.CoreDocumentImpl;
import org.apache.xerces.dom.ElementImpl;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Element;

import java.io.FileOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Represents the <code>kalumet</code> root tag in the main Kalumet DOM.
 */
public class Kalumet
  implements Serializable, Cloneable
{

  private static final long serialVersionUID = -3237352886418250595L;

  private static WriterPreferenceReadWriteLock lock = new WriterPreferenceReadWriteLock();

  private LinkedList properties;

  private Security security;

  private LinkedList agents;

  private LinkedList environments;

  public Kalumet()
  {
    this.properties = new LinkedList();
    this.security = new Security();
    this.agents = new LinkedList();
    this.environments = new LinkedList();
  }

  /**
   * Adds a new <code>Property</code> in the <code>Kalumet</code>
   * container.
   *
   * @param property the <code>Property</code> to add.
   */
  public void addProperty( Property property )
    throws ModelObjectAlreadyExistsException
  {
    if ( this.getProperty( property.getName() ) != null )
    {
      throw new ModelObjectAlreadyExistsException( "Property name already exists in Kalumet configuration." );
    }
    this.properties.add( property );
  }

  /**
   * Gets the <code>Property</code> list in the <code>Kalumet</code>
   * container.
   *
   * @return the <code>Property</code> list.
   */
  public List getProperties()
  {
    return this.properties;
  }

  /**
   * Overwrites the <code>Property</code> list in the <code>Kalumet</code>
   * container.
   *
   * @param properties the new <code>Property</code> list.
   */
  public void setProperties( LinkedList properties )
  {
    this.properties = properties;
  }

  /**
   * Get the <code>Property</code> identified by a given name in the
   * <code>Kalumet</code> container.
   *
   * @param name the <code>Property</code> name.
   * @return the found <code>Property</code> or null if no <code>Property</code> found.
   */
  public Property getProperty( String name )
  {
    for ( Iterator propertyIterator = this.getProperties().iterator(); propertyIterator.hasNext(); )
    {
      Property property = (Property) propertyIterator.next();
      if ( property.getName().equals( name ) )
      {
        return property;
      }
    }
    return null;
  }

  /**
   * Set the <code>Security</code> definition of the <code>Kalumet</code>
   * container.
   *
   * @param security the <code>Security</code> definition.
   */
  public void setSecurity( Security security )
  {
    this.security = security;
  }

  /**
   * Get the <code>Security</code> definition of the <code>Kalumet</code>
   * container.
   *
   * @return the <code>Security</code> definition.
   */
  public Security getSecurity()
  {
    return this.security;
  }

  /**
   * Add a new <code>Agent</code> in the <code>Kalumet</code> container.
   *
   * @param agent the <code>Agent</code> to add.
   */
  public void addAgent( Agent agent )
    throws ModelObjectAlreadyExistsException
  {
    if ( this.getAgent( agent.getId() ) != null )
    {
      throw new ModelObjectAlreadyExistsException( "Agent id already exists in Kalumet configuration." );
    }
    this.agents.add( agent );
  }

  /**
   * Overwrite the <code>Agent</code> list in the <code>Kalumet</code>
   * container with a new list.
   *
   * @param agents the new <code>Agent</code> list.
   */
  public void setAgents( LinkedList agents )
  {
    this.agents = agents;
  }

  /**
   * Return the <code>Agent</code> list in the <code>Kalumet</code>
   * container.
   *
   * @return the <code>Agent</code> list.
   */
  public List getAgents()
  {
    return this.agents;
  }

  /**
   * Get the <code>Agent</code> identified by a given id in the
   * <code>Kalumet</code> container.
   *
   * @param id the <code>Agent</code> id.
   * @return the found <code>Agent</code> or null if no <code>Agent</code> found.
   */
  public Agent getAgent( String id )
  {
    for ( Iterator agentIterator = this.getAgents().iterator(); agentIterator.hasNext(); )
    {
      Agent agent = (Agent) agentIterator.next();
      if ( agent.getId().equals( id ) )
      {
        return agent;
      }
    }
    return null;
  }

  /**
   * Add a new <code>Environment</code> to the <code>Kalumet</code>
   * container.
   *
   * @param environment the <code>Environment</code> to add.
   */
  public void addEnvironment( Environment environment )
    throws ModelObjectAlreadyExistsException
  {
    if ( this.getEnvironment( environment.getName() ) != null )
    {
      throw new ModelObjectAlreadyExistsException( "Environment name already exists in Kalumet configuration." );
    }
    this.environments.add( environment );
  }

  /**
   * Get the <code>Environment</code> list in the <code>Kalumet</code>
   * container.
   *
   * @return the <code>Environment</code> list.
   */
  public List getEnvironments()
  {
    return this.environments;
  }

  /**
   * Overwrite the <code>Environment</code> list in the
   * <code>Kalumet</code> container.
   *
   * @param environments the new <code>Environment</code> list.
   */
  public void setEnvironments( LinkedList environments )
  {
    this.environments = environments;
  }

  /**
   * Get the <code>Environment</code> identified by the given name in the
   * <code>Kalumet</code> container.
   *
   * @param name the <code>Environment</code> name.
   * @return the found <code>Environment</code> or null if no <code>Environment</code> found.
   */
  public Environment getEnvironment( String name )
  {
    for ( Iterator environmentIterator = this.getEnvironments().iterator(); environmentIterator.hasNext(); )
    {
      Environment environment = (Environment) environmentIterator.next();
      if ( environment.getName().equals( name ) )
      {
        return environment;
      }
    }
    return null;
  }

  /**
   * Get the <code>Environment</code> map order by group.
   *
   * @return the groups/environments map.
   */
  public Map getEnvironmentsByGroups()
  {
    HashMap map = new HashMap();
    for ( Iterator environmentIterator = environments.iterator(); environmentIterator.hasNext(); )
    {
      Environment current = (Environment) environmentIterator.next();
      if ( !map.containsKey( current.getGroup() ) )
      {
        map.put( current.getGroup(), new LinkedList() );
      }
      ( (List) map.get( current.getGroup() ) ).add( current );
    }
    return map;
  }

  /**
   * Get the <code>Environment</code> list for a given <code>User</code> id.
   *
   * @param userid the <code>User</code> id.
   * @return the <code>Environment</code> list of the user.
   */
  public List getUserEnvironments( String userid )
  {
    LinkedList userEnvironments = new LinkedList();
    Security security = this.getSecurity();
    for ( Iterator environmentIterator = this.getEnvironments().iterator(); environmentIterator.hasNext(); )
    {
      Environment environment = (Environment) environmentIterator.next();
      if ( security.checkEnvironmentUserAccess( environment, userid, null ) )
      {
        userEnvironments.add( environment );
      }
    }
    return userEnvironments;
  }

  /**
   * Get the <code>Environment</code> user map order by group.
   *
   * @param userid the <code>User</code> id.
   * @return the groups/environments user map.
   */
  public Map getUserEnvironmentsByGroups( String userid )
  {
    HashMap map = new HashMap();
    for ( Iterator userEnvironmentIterator = this.getUserEnvironments( userid ).iterator();
          userEnvironmentIterator.hasNext(); )
    {
      Environment environment = (Environment) userEnvironmentIterator.next();
      if ( !map.containsKey( environment.getGroup() ) )
      {
        map.put( environment.getGroup(), new LinkedList() );
      }
      ( (List) map.get( environment.getGroup() ) ).add( environment );
    }
    return map;
  }

  /**
   * Get the <code>Environment</code> list for a given <code>Agent</code> id.
   *
   * @param id the <code>Agent</code> id.
   * @return the <code>Environment</code> list managed by the <code>Agent</code>.
   */
  public List getEnvironmentsByAgent( String id )
  {
    LinkedList list = new LinkedList();
    for ( Iterator environmentIterator = this.getEnvironments().iterator(); environmentIterator.hasNext(); )
    {
      Environment current = (Environment) environmentIterator.next();
      if ( current.getAgent().equals( id ) )
      {
        list.add( current );
      }
    }
    return list;
  }

  /**
   * Digeste a given XML file and return the main kalumet root tag.
   *
   * @param path the Kalumet XML file to parse.
   * @return the main <code>Kalumet</code> corresponding with the root tag.
   */
  public static Kalumet digeste( String path )
    throws KalumetException
  {
    if ( !path.startsWith( "http:" ) && !path.startsWith( "HTTP:" ) && !path.startsWith( "file:" ) && !path.startsWith(
      "FILE:" ) )
    {
      path = "file:" + path;
    }
    Kalumet kalumet = null;
    try
    {
      lock.readLock().acquire();

      // init the digester with no validation on the XML file (no DTD)
      Digester digester = new Digester();
      digester.setValidating( false );

      // kalumet tag rules
      digester.addObjectCreate( "kalumet", "org.apache.kalumet.model.Kalumet" );
      digester.addSetProperties( "kalumet" );

      // properties/property tag rules
      digester.addObjectCreate( "kalumet/properties/property", "org.apache.kalumet.model.Property" );
      digester.addSetProperties( "kalumet/properties/property" );

      // add property in the kalumet tag rule
      digester.addSetNext( "kalumet/properties/property", "addProperty", "org.apache.kalumet.model.Property" );

      // security tag rules
      digester.addObjectCreate( "kalumet/security", "org.apache.kalumet.model.Security" );
      digester.addSetProperties( "kalumet/security" );

      // user tag rules
      digester.addObjectCreate( "kalumet/security/users/user", "org.apache.kalumet.model.User" );
      digester.addSetProperties( "kalumet/security/users/user" );

      // add user to security tag rule
      digester.addSetNext( "kalumet/security/users/user", "addUser", "org.apache.kalumet.model.User" );

      // group tag rules
      digester.addObjectCreate( "kalumet/security/groups/group", "org.apache.kalumet.model.Group" );
      digester.addSetProperties( "kalumet/security/groups/group" );

      // user group tag rules
      digester.addObjectCreate( "kalumet/security/groups/group/users/user", "org.apache.kalumet.model.User" );
      digester.addSetProperties( "kalumet/security/groups/group/users/user" );

      // add user in group tag rule
      digester.addSetNext( "kalumet/security/groups/group/users/user", "addUser", "org.apache.kalumet.model.User" );

      // add group to security tag rule
      digester.addSetNext( "kalumet/security/groups/group", "addGroup", "org.apache.kalumet.model.Group" );

      // add security to kalumet tag rule
      digester.addSetNext( "kalumet/security", "setSecurity", "org.apache.kalumet.model.Security" );

      // agent tag rules
      digester.addObjectCreate( "kalumet/agents/agent", "org.apache.kalumet.model.Agent" );
      digester.addSetProperties( "kalumet/agents/agent" );

      // add agent to kalumet tag rule
      digester.addSetNext( "kalumet/agents/agent", "addAgent", "org.apache.kalumet.model.Agent" );

      // environment tag rules
      digester.addObjectCreate( "kalumet/environments/environment", "org.apache.kalumet.model.Environment" );
      digester.addSetProperties( "kalumet/environments/environment" );

      // variables tag rules
      digester.addObjectCreate( "kalumet/environments/environment/variables/variable",
                                "org.apache.kalumet.model.Variable" );
      digester.addSetProperties( "kalumet/environments/environment/variables/variable" );

      // add variable to environment tag rule
      digester.addSetNext( "kalumet/environments/environment/variables/variable", "addVariable",
                           "org.apache.kalumet.model.Variable" );

      // freefield tag rules
      digester.addObjectCreate( "kalumet/environments/environment/freefields/freefield",
                                "org.apache.kalumet.model.FreeField" );
      digester.addSetProperties( "kalumet/environments/environment/freefields/freefield" );
      // add freefield content
      digester.addCallMethod( "kalumet/environments/environment/freefields/freefield", "setContent", 0 );

      // add freefield to environment tag rule
      digester.addSetNext( "kalumet/environments/environment/freefields/freefield", "addFreeField",
                           "org.apache.kalumet.model.FreeField" );

      // access tag rules
      digester.addObjectCreate( "kalumet/environments/environment/accesses/access", "org.apache.kalumet.model.Access" );
      digester.addSetProperties( "kalumet/environments/environment/accesses/access" );

      // access properties rules
      digester.addObjectCreate( "kalumet/environments/environment/accesses/access/properties/property",
                                "org.apache.kalumet.model.Property" );
      digester.addSetProperties( "kalumet/environments/environment/accesses/access/properties/property" );

      // add property in access tag rule
      digester.addSetNext( "kalumet/environments/environment/accesses/access/properties/property", "addProperty",
                           "org.apache.kalumet.model.Property" );

      // add access to environment tag rule
      digester.addSetNext( "kalumet/environments/environment/accesses/access", "addAccess",
                           "org.apache.kalumet.model.Access" );

      // environment notes and weblinks tag rules
      digester.addCallMethod( "kalumet/environments/environment/notes", "setNotes", 0 );
      digester.addCallMethod( "kalumet/environments/environment/weblinks", "setWeblinks", 0 );

      // jeeapplicationservers tag rules
      digester.addObjectCreate( "kalumet/environments/environment/jeeapplicationservers",
                                "org.apache.kalumet.model.JEEApplicationServers" );
      digester.addSetProperties( "kalumet/environments/environment/jeeapplicationservers" );

      // jeeapplicationserver tag rules
      digester.addObjectCreate( "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver",
                                "org.apache.kalumet.model.JEEApplicationServer" );
      digester.addSetProperties( "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver" );

      // jeeapplicationserver startupcommand and shutdowncommand tag rules
      digester.addCallMethod(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/startupcommand",
        "setStartupcommand", 0 );
      digester.addCallMethod(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/shutdowncommand",
        "setShutdowncommand", 0 );

      // jdbcconnectionpool tag rules
      digester.addObjectCreate(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jdbcconnectionpools/jdbcconnectionpool",
        "org.apache.kalumet.model.JDBCConnectionPool" );
      digester.addSetProperties(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jdbcconnectionpools/jdbcconnectionpool" );

      // add jdbcconnectionpool to jeeapplicationserver
      digester.addSetNext(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jdbcconnectionpools/jdbcconnectionpool",
        "addJDBCConnectionPool", "org.apache.kalumet.model.JDBCConnectionPool" );

      // jdbcdatasource tag rules
      digester.addObjectCreate(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jdbcdatasources/jdbcdatasource",
        "org.apache.kalumet.model.JDBCDataSource" );
      digester.addSetProperties(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jdbcdatasources/jdbcdatasource" );

      // add jdbcdatasource to jeeapplicationserver
      digester.addSetNext(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jdbcdatasources/jdbcdatasource",
        "addJDBCDataSource", "org.apache.kalumet.model.JDBCDataSource" );

      // jmsconnectionfactory tag rules
      digester.addObjectCreate(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jmsconnectionfactories/jmsconnectionfactory",
        "org.apache.kalumet.model.JMSConnectionFactory" );
      digester.addSetProperties(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jmsconnectionfactories/jmsconnectionfactory" );

      // add jmsconnectionfactory to jeeapplicationserver
      digester.addSetNext(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jmsconnectionfactories/jmsconnectionfactory",
        "addJMSConnectionFactory", "org.apache.kalumet.model.JMSConnectionFactory" );

      // jmsserver tag rules
      digester.addObjectCreate(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jmsservers/jmsserver",
        "org.apache.kalumet.model.JMSServer" );
      digester.addSetProperties(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jmsservers/jmsserver" );

      // jmsqueue tag rules
      digester.addObjectCreate(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jmsservers/jmsserver/jmsqueues/jmsqueue",
        "org.apache.kalumet.model.JMSQueue" );
      digester.addSetProperties(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jmsservers/jmsserver/jmsqueues/jmsqueue" );

      // add jmsqueue to jmsserver
      digester.addSetNext(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jmsservers/jmsserver/jmsqueues/jmsqueue",
        "addJMSQueue", "org.apache.kalumet.model.JMSQueue" );

      // jmstopic tag rules
      digester.addObjectCreate(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jmsservers/jmsserver/jmstopics/jmstopic",
        "org.apache.kalumet.model.JMSTopic" );
      digester.addSetProperties(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jmsservers/jmsserver/jmstopics/jmstopic" );

      // add jmstopic to jmsserver
      digester.addSetNext(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jmsservers/jmsserver/jmstopics/jmstopic",
        "addJMSTopic", "org.apache.kalumet.model.JMSTopic" );

      // add jmsserver to jeeapplicationserver
      digester.addSetNext(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jmsservers/jmsserver",
        "addJMSServer", "org.apache.kalumet.model.JMSServer" );

      // jndibinding tag rules
      digester.addObjectCreate(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jndibindings/jndibinding",
        "org.apache.kalumet.model.JNDIBinding" );
      digester.addSetProperties(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jndibindings/jndibinding" );

      // add jndibinding to jeeapplicationserver
      digester.addSetNext(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jndibindings/jndibinding",
        "addJNDIBinding", "org.apache.kalumet.model.JNDIBinding" );

      // sharedlibrary tag rules
      digester.addObjectCreate(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/sharedlibrairies/sharedlibrary",
        "org.apache.kalumet.model.SharedLibrary" );
      digester.addSetProperties(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/sharedlibrairies/sharedlibrary" );

      // add sharedlibrary to jeeapplicationserver
      digester.addSetNext(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/sharedlibrairies/sharedlibrary",
        "addSharedLibrary", "org.apache.kalumet.model.SharedLibrary" );

      // application tag rules
      digester.addObjectCreate(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jeeapplications/jeeapplication",
        "org.apache.kalumet.model.JEEApplication" );
      digester.addSetProperties(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jeeapplications/jeeapplication" );

      // archive tag rules
      digester.addObjectCreate(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jeeapplications/jeeapplication/archives/archive",
        "org.apache.kalumet.model.Archive" );
      digester.addSetProperties(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jeeapplications/jeeapplication/archives/archive" );

      // add archive archive to application
      digester.addSetNext(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jeeapplications/jeeapplication/archives/archive",
        "addArchive", "org.apache.kalumet.model.Archive" );

      // contentmanager tag rules
      digester.addObjectCreate(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jeeapplications/jeeapplication/contentmanagers/contentmanager",
        "org.apache.kalumet.model.ContentManager" );
      digester.addSetProperties(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jeeapplications/jeeapplication/contentmanagers/contentmanager" );

      // contentmanager property tag rules
      digester.addObjectCreate(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jeeapplications/jeeapplication/contentmanagers/contentmanager/properties/property",
        "org.apache.kalumet.model.Property" );
      digester.addSetProperties(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jeeapplications/jeeapplication/contentmanagers/contentmanager/properties/property" );

      // add property in contentmanager
      digester.addSetNext(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jeeapplications/jeeapplication/contentmanagers/contentmanager/properties/property",
        "addProperty", "org.apache.kalumet.model.Property" );

      // add contentmanager to application
      digester.addSetNext(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jeeapplications/jeeapplication/contentmanagers/contentmanager",
        "addContentManager", "org.apache.kalumet.model.ContentManager" );

      // configurationfile tag rules
      digester.addObjectCreate(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jeeapplications/jeeapplication/configurationfiles/configurationfile",
        "org.apache.kalumet.model.ConfigurationFile" );
      digester.addSetProperties(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jeeapplications/jeeapplication/configurationfiles/configurationfile" );

      // mapping tag rules
      digester.addObjectCreate(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jeeapplications/jeeapplication/configurationfiles/configurationfile/mappings/mapping",
        "org.apache.kalumet.model.Mapping" );
      digester.addSetProperties(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jeeapplications/jeeapplication/configurationfiles/configurationfile/mappings/mapping" );

      // add mapping to configurationfile
      digester.addSetNext(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jeeapplications/jeeapplication/configurationfiles/configurationfile/mappings/mapping",
        "addMapping", "org.apache.kalumet.model.Mapping" );

      // add configurationfile to application
      digester.addSetNext(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jeeapplications/jeeapplication/configurationfiles/configurationfile",
        "addConfigurationFile", "org.apache.kalumet.model.ConfigurationFile" );

      // database tag rules
      digester.addObjectCreate(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jeeapplications/jeeapplication/databases/database",
        "org.apache.kalumet.model.Database" );
      digester.addSetProperties(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jeeapplications/jeeapplication/databases/database" );

      // sqlscript tag rules
      digester.addObjectCreate(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jeeapplications/jeeapplication/databases/database/sqlscripts/sqlscript",
        "org.apache.kalumet.model.SqlScript" );
      digester.addSetProperties(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jeeapplications/jeeapplication/databases/database/sqlscripts/sqlscript" );

      // sqlscript mapping tag rules
      digester.addObjectCreate(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jeeapplications/jeeapplication/databases/database/sqlscripts/sqlscript/mappings/mapping",
        "org.apache.kalumet.model.Mapping" );
      digester.addSetProperties(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jeeapplications/jeeapplication/databases/database/sqlscripts/sqlscript/mappings/mapping" );

      // add mapping to sqlscript
      digester.addSetNext(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jeeapplications/jeeapplication/databases/database/sqlscripts/sqlscript/mappings/mapping",
        "addMapping", "org.apache.kalumet.model.Mapping" );

      // add sqlscript to database
      digester.addSetNext(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jeeapplications/jeeapplication/databases/database/sqlscripts/sqlscript",
        "addSqlScript", "org.apache.kalumet.model.SqlScript" );

      // add database to application
      digester.addSetNext(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jeeapplications/jeeapplication/databases/database",
        "addDatabase", "org.apache.kalumet.model.Database" );

      // add application to applicationserver
      digester.addSetNext(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/jeeapplications/application",
        "addApplication", "org.apache.kalumet.model.JEEApplication" );

      // cache tag rules
      digester.addObjectCreate(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/caches/cache",
        "org.apache.kalumet.model.Cache" );
      digester.addSetProperties(
        "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/caches/cache" );

      // add cache to applicationserver
      digester.addSetNext( "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver/caches/cache",
                           "addCache", "org.apache.kalumet.model.Cache" );

      // add applicationserver to applicationservers tag rule
      digester.addSetNext( "kalumet/environments/environment/jeeapplicationservers/jeeapplicationserver",
                           "addJEEApplicationServer", "org.apache.kalumet.model.JEEApplicationServer" );

      // add applicationservers to environment tag rule
      digester.addSetNext( "kalumet/environments/environment/jeeapplicationservers", "setJEEApplicationServers",
                           "org.apache.kalumet.model.JEEApplicationServers" );

      // logfile tag rules
      digester.addObjectCreate( "kalumet/environments/environment/logfiles/logfile",
                                "org.apache.kalumet.model.LogFile" );
      digester.addSetProperties( "kalumet/environments/environment/logfiles/logfile" );

      // add logfile to environment tag rule
      digester.addSetNext( "kalumet/environments/environment/logfiles/logfile", "addLogFile",
                           "org.apache.kalumet.model.LogFile" );

      // software tag rules
      digester.addObjectCreate( "kalumet/environments/environment/softwares/software",
                                "org.apache.kalumet.model.Software" );
      digester.addSetProperties( "kalumet/environments/environment/softwares/software" );

      // software update plan command item
      digester.addObjectCreate( "kalumet/environments/environment/softwares/software/updateplan/command",
                                "org.apache.kalumet.model.Command" );
      digester.addSetProperties( "kalumet/environments/environment/softwares/software/updateplan/command" );
      digester.addCallMethod( "kalumet/environments/environment/softwares/software/updateplan/command", "setCommand",
                              0 );
      digester.addSetNext( "kalumet/environments/environment/softwares/software/updateplan/command", "addCommand",
                           "org.apache.kalumet.model.Command" );

      // software update plan location item
      digester.addObjectCreate( "kalumet/environments/environment/softwares/software/updateplan/location",
                                "org.apache.kalumet.model.Location" );
      digester.addSetProperties( "kalumet/environments/environment/softwares/software/updateplan/location" );
      digester.addSetNext( "kalumet/environments/environment/softwares/software/updateplan/location", "addLocation",
                           "org.apache.kalumet.model.Location" );

      // software update plan configuration file item
      digester.addObjectCreate( "kalumet/environments/environment/softwares/software/updateplan/configurationfile",
                                "org.apache.kalumet.model.ConfigurationFile" );
      digester.addSetProperties( "kalumet/environments/environment/softwares/software/updateplan/configurationfile" );
      digester.addObjectCreate(
        "kalumet/environments/environment/softwares/software/updateplan/configurationfile/mappings/mapping",
        "org.apache.kalumet.model.Mapping" );
      digester.addSetProperties(
        "kalumet/environments/environment/softwares/software/updateplan/configurationfile/mappings/mapping" );
      digester.addSetNext(
        "kalumet/environments/environment/softwares/software/updateplan/configurationfile/mappings/mapping",
        "addMapping", "org.apache.kalumet.model.Mapping" );
      digester.addSetNext( "kalumet/environments/environment/softwares/software/updateplan/configurationfile",
                           "addConfigurationFile", "org.apache.kalumet.model.ConfigurationFile" );

      // software update plan database item
      digester.addObjectCreate( "kalumet/environments/environment/softwares/software/updateplan/database",
                                "org.apache.kalumet.model.Database" );
      digester.addSetProperties( "kalumet/environments/environment/softwares/software/updateplan/database" );
      digester.addObjectCreate(
        "kalumet/environments/environment/softwares/software/updateplan/database/sqlscripts/sqlscript",
        "org.apache.kalumet.model.SqlScript" );
      digester.addSetProperties(
        "kalumet/environments/environment/softwares/software/updateplan/database/sqlscripts/sqlscript" );
      digester.addObjectCreate(
        "kalumet/environments/environment/softwares/software/updateplan/database/sqlscripts/sqlscript/mappings/mapping",
        "org.apache.kalumet.model.Mapping" );
      digester.addSetProperties(
        "kalumet/environments/environment/softwares/software/updateplan/database/sqlscripts/sqlscript/mappings/mapping" );
      digester.addSetNext(
        "kalumet/environments/environment/softwares/software/updateplan/database/sqlscripts/sqlscript/mappings/mapping",
        "addMapping", "org.apache.kalumet.model.Mapping" );
      digester.addSetNext(
        "kalumet/environments/environment/softwares/software/updateplan/database/sqlscripts/sqlscript", "addSqlScript",
        "org.apache.kalumet.model.SqlScript" );
      digester.addSetNext( "kalumet/environments/environment/softwares/software/updateplan/database", "addDatabase",
                           "org.apache.kalumet.model.Database" );

      // add software to environment
      digester.addSetNext( "kalumet/environments/environment/softwares/software", "addSoftware",
                           "org.apache.kalumet.model.Software" );

      // notifiers tag rules
      digester.addObjectCreate( "kalumet/environments/environment/notifiers", "org.apache.kalumet.model.Notifiers" );
      digester.addSetProperties( "kalumet/environments/environment/notifiers" );

      // email tag rules
      digester.addObjectCreate( "kalumet/environments/environment/notifiers/email", "org.apache.kalumet.model.Email" );
      digester.addSetProperties( "kalumet/environments/environment/notifiers/email" );

      // destination tag rules
      digester.addObjectCreate( "kalumet/environments/environment/notifiers/email/destinations/destination",
                                "org.apache.kalumet.model.Destination" );
      digester.addSetProperties( "kalumet/environments/environment/notifiers/email/destinations/destination" );

      // add destination to email notifier
      digester.addSetNext( "kalumet/environments/environment/notifiers/email/destinations/destination",
                           "addDestination", "org.apache.kalumet.model.Destination" );

      // add email to notifiers
      digester.addSetNext( "kalumet/environments/environment/notifiers/email", "addNotifier",
                           "org.apache.kalumet.model.Email" );

      // add notifiers to environment
      digester.addSetNext( "kalumet/environments/environment/notifiers", "setNotifiers",
                           "org.apache.kalumet.model.Notifiers" );

      // email publisher tag rules
      digester.addObjectCreate( "kalumet/environments/environment/publishers/email", "org.apache.kalumet.model.Email" );
      digester.addSetProperties( "kalumet/environments/environment/publishers/email" );

      // destination email publisher tag rules
      digester.addObjectCreate( "kalumet/environments/environment/publishers/email/destinations/destination",
                                "org.apache.kalumet.model.Destination" );
      digester.addSetProperties( "kalumet/environments/environment/publishers/email/destinations/destination" );

      // add destination to email publisher
      digester.addSetNext( "kalumet/environments/environment/publishers/email/destinations/destination",
                           "addDestination", "org.apache.kalumet.model.Destination" );

      // add email publisher to environment
      digester.addSetNext( "kalumet/environments/environment/publishers/email", "addPublisher",
                           "org.apache.kalumet.model.Email" );

      // statistics tag rules
      digester.addObjectCreate( "kalumet/environments/environment/statistics", "org.apache.kalumet.model.Statistics" );
      digester.addSetProperties( "kalumet/environments/environment/statistics" );

      // add statistics to environment
      digester.addSetNext( "kalumet/environments/environment/statistics", "setStatistics",
                           "org.apache.kalumet.model.Statistics" );

      // add environment to kalumet tag rule
      digester.addSetNext( "kalumet/environments/environment", "addEnvironment",
                           "org.apache.kalumet.model.Environment" );

      // parse the XML file
      kalumet = (Kalumet) digester.parse( path );
    }
    catch ( Exception e )
    {
      throw new KalumetException( "Can't read Kalumet configuration.", e );
    }
    finally
    {
      lock.readLock().release();
    }
    return kalumet;
  }

  /**
   * Transform the <code>Kalumet</code> POJO to a DOM Element.
   *
   * @param document the XML core document.
   * @return the DOM element.
   */
  protected Element toDOMElement( CoreDocumentImpl document )
  {
    ElementImpl element = new ElementImpl( document, "kalumet" );
    // properties element
    ElementImpl properties = new ElementImpl( document, "properties" );
    // add property in properties container
    for ( Iterator propertyIterator = this.getProperties().iterator(); propertyIterator.hasNext(); )
    {
      Property property = (Property) propertyIterator.next();
      properties.appendChild( property.toDOMElement( document ) );
    }
    // add properties in kalumet
    element.appendChild( properties );
    // add security in kalumet
    element.appendChild( this.getSecurity().toDOMElement( document ) );
    // agents element
    ElementImpl agents = new ElementImpl( document, "agents" );
    // add agent in agents container
    for ( Iterator agentIterator = this.getAgents().iterator(); agentIterator.hasNext(); )
    {
      Agent agent = (Agent) agentIterator.next();
      agents.appendChild( agent.toDOMElement( document ) );
    }
    // add agents in kalumet
    element.appendChild( agents );
    // environments element
    ElementImpl environments = new ElementImpl( document, "environments" );
    // add environment in environments container
    for ( Iterator environmentIterator = this.getEnvironments().iterator(); environmentIterator.hasNext(); )
    {
      Environment environment = (Environment) environmentIterator.next();
      environments.appendChild( environment.toDOMElement( document ) );
    }
    // add environments in kalumet
    element.appendChild( environments );
    return element;
  }

  /*
  * (non-Javadoc)
  * @see java.lang.Object#clone()
  */
  public Object clone()
    throws CloneNotSupportedException
  {
    Kalumet clone = new Kalumet();
    for ( Iterator propertyIterator = this.properties.iterator(); propertyIterator.hasNext(); )
    {
      Property property = (Property) propertyIterator.next();
      clone.properties.add( (Property) property.clone() );
    }
    clone.setSecurity( (Security) this.getSecurity().clone() );
    for ( Iterator agentIterator = this.agents.iterator(); agentIterator.hasNext(); )
    {
      Agent agent = (Agent) agentIterator.next();
      clone.agents.add( (Agent) agent.clone() );
    }
    for ( Iterator environmentIterator = this.environments.iterator(); environmentIterator.hasNext(); )
    {
      Environment environment = (Environment) environmentIterator.next();
      clone.environments.add( (Environment) environment.clone() );
    }
    return clone;
  }

  /**
   * Write a Kalumet XML file with the content of the in-memory
   * configuration.
   *
   * @param path   the path to the file to write.
   * @param backup a flag indicated if a previous backup must be copied before writing.
   */
  public synchronized void writeXMLFile( String path, boolean backup )
    throws KalumetException
  {
    if ( backup )
    {
      this.backupXMLFile( path );
    }
    try
    {
      lock.writeLock().acquire();
      OutputFormat format = new OutputFormat();
      format.setLineWidth( 72 );
      format.setIndenting( true );
      format.setIndent( 3 );
      format.setEncoding( "ISO-8859-1" );
      if ( path.startsWith( "http:" ) || path.startsWith( "http:" ) )
      {
        throw new KalumetException( "Can't write Kalumet XML file over a HTTP URL." );
      }
      if ( path.startsWith( "file:" ) || path.startsWith( "FILE:" ) )
      {
        path = path.substring( 5 );
      }
      XMLSerializer serializer = new XMLSerializer( new FileOutputStream( path ), format );
      serializer.serialize( this.toDOMElement( new CoreDocumentImpl( true ) ) );
    }
    catch ( Exception e )
    {
      throw new KalumetException( "Can't write Kalumet XML file.", e );
    }
    finally
    {
      lock.writeLock().release();
    }
  }

  /**
   * Write a Kalumet XML file with the content of the in-memory
   * configuration.
   *
   * @param path the path to the file to write.
   */
  public void writeXMLFile( String path )
    throws KalumetException
  {
    this.writeXMLFile( path, false );
  }

  /**
   * Make a backup of the old Kalumet XML configuration to avoid file
   * corruption (for example when disk full).
   *
   * @param path the path to the Kalumet XML file.
   */
  public void backupXMLFile( String path )
    throws KalumetException
  {
    FileManipulator fileManipulator = null;
    try
    {
      fileManipulator = new FileManipulator();
      fileManipulator.copy( path, path + ".backup" );
    }
    finally
    {
      if ( fileManipulator != null )
      {
        fileManipulator.close();
      }
    }
  }

  public static void writeDefault( String path )
    throws KalumetException
  {
    Kalumet kalumet = new Kalumet();
    Security security = kalumet.getSecurity();
    User adminUser = new User();
    adminUser.setId( "admin" );
    adminUser.setName( "admin" );
    adminUser.setPassword( "21232f297a57a5a743894a0e4a801fc3" );
    Group adminGroup = new Group();
    adminGroup.setId( "admin" );
    adminGroup.setName( "admin" );
    adminGroup.addUser( adminUser );
    security.addGroup( adminGroup );
    security.addUser( adminUser );
    kalumet.writeXMLFile( path );
  }

}