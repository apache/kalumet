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
package org.apache.kalumet.controller.websphere;

import com.ibm.websphere.management.AdminClient;
import com.ibm.websphere.management.AdminClientFactory;
import com.ibm.websphere.management.AdminConstants;
import com.ibm.websphere.management.ObjectNameHelper;
import com.ibm.websphere.management.Session;
import com.ibm.websphere.management.application.AppConstants;
import com.ibm.websphere.management.application.AppManagement;
import com.ibm.websphere.management.application.AppManagementProxy;
import com.ibm.websphere.management.configservice.ConfigServiceHelper;
import com.ibm.websphere.management.configservice.ConfigServiceProxy;
import com.ibm.websphere.management.configservice.SystemAttributes;
import org.apache.kalumet.controller.core.AbstractJEEApplicationServerController;
import org.apache.kalumet.controller.core.ControllerException;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternCompiler;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.PatternMatcherInput;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

/**
 * WebsphereController is the controller for IBM WebSphere Server 5.x and 6.x.
 */
public class WebsphereController
  extends AbstractJEEApplicationServerController
{

  private final static transient Logger LOGGER = LoggerFactory.getLogger( WebsphereController.class );

  private static final String JMX_URL_REGEXP = "(.+)://(.+):(.+)";

  private String connector;

  private String host;

  private String port;

  /**
   * Default constructor.
   *
   * @param url        the JMX URL to connect to the IBM WebSphere server.
   * @param username   the admin user name to connect to the IBM WebSphere server.
   * @param password   the admin password to connect to the IBM WebSphere server
   * @param serverName the server/cluster name to manage.
   * @param cluster    a flag indicating if we manage a cluster (true) or a single server (false).
   */
  public WebsphereController( String url, String username, String password, String serverName, Boolean cluster )
    throws ControllerException
  {
    super( url, username, password, serverName, cluster );
  }

  public String getConnector()
  {
    return this.connector;
  }

  public void setConnector( String connector )
  {
    this.connector = connector;
  }

  public String getHost()
  {
    return this.host;
  }

  public void setHost( String host )
  {
    this.host = host;
  }

  public String getPort()
  {
    return this.port;
  }

  public void setPort( String port )
  {
    this.port = port;
  }

  protected void init()
    throws ControllerException
  {
    // make a regexp on the URL to get hostname and port
    LOGGER.debug( "Initializing ORO regexp objects to split the JMX URL" );
    PatternMatcher matcher = new Perl5Matcher();
    PatternCompiler compiler = new Perl5Compiler();
    Pattern pattern = null;
    try
    {
      pattern = compiler.compile( WebsphereController.JMX_URL_REGEXP );
      LOGGER.debug( "ORO regexp pattern is {}", WebsphereController.JMX_URL_REGEXP );
    }
    catch ( MalformedPatternException patternException )
    {
      LOGGER.error( "IBM WebSphere server JMX URL is not correct", patternException );
      throw new ControllerException( "IBM WebSphere JMX URL is not correct", patternException );
    }
    PatternMatcherInput input = new PatternMatcherInput( this.getUrl() );
    LOGGER.debug( "Initializing ORO regexp input matcher with the URL {}", this.getUrl() );
    if ( matcher.contains( input, pattern ) )
    {
      LOGGER.debug( "ORO regexp input matches the pattern, try to split" );
      MatchResult result = matcher.getMatch();
      this.setConnector( result.group( 1 ) );
      LOGGER.debug( "Connector isolated using the pattern: {}", this.getConnector() );
      this.setHost( result.group( 2 ) );
      LOGGER.debug( "Host isolated using the pattern: {}", this.getHost() );
      this.setPort( result.group( 3 ) );
      LOGGER.debug( "Port isolated using the pattern: {}", this.getPort() );
    }
    else
    {
      LOGGER.error( "IBM WebSphere server JMX URL is not correct" );
      throw new ControllerException( "IBM WebSphere server JMX URL is not correct" );
    }
  }

  /**
   * Get the WebSphere Config Service proxy.
   *
   * @return the WebSphere config service proxy.
   * @throws ControllerException in case of communication failure.
   */
  protected ConfigServiceProxy getConfigServiceProxy()
    throws ControllerException
  {
    AdminClient admin = null;
    ConfigServiceProxy configService = null;
    try
    {
      Properties properties = new Properties();
      properties.setProperty( AdminClient.CONNECTOR_HOST, this.getHost() );
      properties.setProperty( AdminClient.CONNECTOR_PORT, this.getPort() );
      if ( this.getConnector().equals( "iiop" ) )
      {
        properties.setProperty( AdminClient.CONNECTOR_TYPE, AdminClient.CONNECTOR_TYPE_RMI );
      }
      if ( this.getConnector().equals( "soap" ) )
      {
        properties.setProperty( AdminClient.CONNECTOR_TYPE, AdminClient.CONNECTOR_TYPE_SOAP );
      }
      // TODO add support for client security, add attribute in the Kalumet model is mandatory
      //properties.setProperty(AdminClient.CONNECTOR_SECURITY_ENABLED, "true");
      //properties.setProperty("javax.net.ssl.trustStore", "/opt/websphere/6.1/etc/trust.p12");
      //properties.setProperty("javax.net.ssl.keyStore", "/opt/websphere/6.1/etc/key.p12");
      //properties.setProperty("javax.net.ssl.trustStorePassword", "WebAS");
      //properties.setProperty("javax.net.ssl.keyStorePassword", "WebAS");
      //properties.setProperty("javax.net.ssl.trustStoreType", "PKCS12");
      //properties.setProperty("javax.net.ssk.keyStoreType", "PKCS12");
      //properties.setProperty(AdminClient.USERNAME, this.getUsername());
      //properties.setProperty(AdminClient.PASSWORD, this.getPassword());
      admin = AdminClientFactory.createAdminClient( properties );
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't connect to IBM WebSphere server", e );
      throw new ControllerException( "Can't connect to IBM WebSphere server", e );
    }
    // we have the admin client, we init the config service proxy
    try
    {
      configService = new ConfigServiceProxy( admin );
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't get IBM WebSphere server config service proxy", e );
      throw new ControllerException( "Can't get IBM WebSphere server config service proxy", e );
    }
    return configService;
  }

  public void shutdown()
    throws ControllerException
  {
    LOGGER.info( "Shutting down IBM WebSphere server {}", this.getServerName() );
    AdminClient admin = this.getConfigServiceProxy().getAdminClient();
    ObjectName server = null;
    try
    {
      Set servers = null;
      if ( this.isCluster() )
      {
        servers =
          admin.queryNames( new ObjectName( "WebSphere:type=Cluster,name=" + this.getServerName() + ",*" ), null );
      }
      else
      {
        servers =
          admin.queryNames( new ObjectName( "WebSphere:type=Server,name=" + this.getServerName() + ",*" ), null );
      }
      if ( !servers.isEmpty() )
      {
        server = (ObjectName) servers.iterator().next();
        admin.invoke( server, "stop", null, null );
      }
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't shutdown IBM WebSphere server {}", this.getServerName(), e );
      throw new ControllerException( "Can't shutdown IBM WebSphere server " + this.getServerName(), e );
    }
  }

  public String status()
  {
    LOGGER.info( "Check status of IBM WebSphere server {}", this.getServerName() );
    String state = "N/A";
    AdminClient admin = null;
    try
    {
      admin = this.getConfigServiceProxy().getAdminClient();
    }
    catch ( ControllerException controllerException )
    {
      LOGGER.warn( "Can't check status of IBM WebSphere server {}", this.getServerName(), controllerException );
      return "N/A";
    }
    ObjectName server = null;
    try
    {
      Set servers = null;
      if ( this.isCluster() )
      {
        servers =
          admin.queryNames( new ObjectName( "WebSphere:type=Cluster,name=" + this.getServerName() + ",*" ), null );
      }
      else
      {
        servers =
          admin.queryNames( new ObjectName( "WebSphere:type=Server,name=" + this.getServerName() + ",*" ), null );
      }
      if ( !servers.isEmpty() )
      {
        server = (ObjectName) servers.iterator().next();
        state = (String) admin.getAttribute( server, "state" );
      }
      else
      {
        state = "STOPPED";
      }
    }
    catch ( Exception e )
    {
      LOGGER.warn( "Can't check status of IBM WebSphere server {}", this.getServerName(), e );
      return "N/A";
    }
    LOGGER.debug( "IBM WebSphere server status: {}", state );
    return state;
  }

  public boolean isStopped()
  {
    LOGGER.info( "Check if IBM WebSphere server {} is stopped", this.getServerName() );
    boolean stopped = true;
    AdminClient admin = null;
    try
    {
      admin = this.getConfigServiceProxy().getAdminClient();
    }
    catch ( ControllerException controllerException )
    {
      LOGGER.warn( "Can't check if IBM WebSphere server is stopped", controllerException );
      return true;
    }
    ObjectName server = null;
    try
    {
      Set servers = null;
      if ( this.isCluster() )
      {
        servers =
          admin.queryNames( new ObjectName( "WebSphere:type=Cluster,name=" + this.getServerName() + ",*" ), null );
      }
      else
      {
        servers =
          admin.queryNames( new ObjectName( "WebSphere:type=Server,name=" + this.getServerName() + ",*" ), null );
      }
      if ( !servers.isEmpty() )
      {
        server = (ObjectName) servers.iterator().next();
        if ( ( (String) admin.getAttribute( server, "state" ) ).equals( "STARTED" ) )
        {
          stopped = false;
        }
      }
    }
    catch ( Exception e )
    {
      LOGGER.warn( "Can't check if IBM WebSphere server is stopped", e );
      return true;
    }
    return stopped;
  }

  public boolean isJEEApplicationDeployed(String path, String name)
    throws ControllerException
  {
    LOGGER.info( "Checking if JEE application {} is deployed", name );
    boolean deployed = false;
    AdminClient admin = this.getConfigServiceProxy().getAdminClient();
    try
    {
      AppManagement appManagement = AppManagementProxy.getJMXProxyForClient( admin );
      deployed = appManagement.checkIfAppExists( name, null, null );
    }
    catch ( Exception e )
    {
      LOGGER.warn( "Can't check if JEE application {} is deployed", name, e );
      return false;
    }
    return deployed;
  }

  public void deployJEEApplication(String path, String name, String classloaderorder, String classloaderpolicy,
                                   String vhost)
    throws ControllerException
  {
    LOGGER.info( "Deploying JEE application {}", name );
    AdminClient admin = this.getConfigServiceProxy().getAdminClient();
    ObjectName server = null;
    try
    {
      Session session = new Session();
      LOGGER.debug( "Looking for IBM WebSphere server MBean" );
      Set servers = null;
      if ( this.isCluster() )
      {
        servers =
          admin.queryNames( new ObjectName( "WebSphere:type=Cluster,name=" + this.getServerName() + ",*" ), null );
      }
      else
      {
        servers =
          admin.queryNames( new ObjectName( "WebSphere:type=Server,name=" + this.getServerName() + ",*" ), null );
      }
      if ( servers.isEmpty() )
      {
        LOGGER.error( "IBM WebSphere server {} not found, can't deploy JEE application {}", this.getServerName(),
                      name );
        throw new ControllerException(
          "IBM WebSphere server " + this.getServerName() + " not found, can't deploy JEE application " + name );
      }
      server = (ObjectName) servers.iterator().next();
      LOGGER.debug( "IBM WebSphere server {} MBean found", this.getServerName() );
      AppManagement appManagement = AppManagementProxy.getJMXProxyForClient( admin );
      // first, create the deployment controler and populate the archive file
      // with appropriate options
      LOGGER.debug( "Defining JEE application preferences" );
      Hashtable preferences = new Hashtable();
      preferences.put( AppConstants.APPDEPL_LOCALE, Locale.getDefault() );
      preferences.put( AppConstants.APPDEPL_APPNAME, name );
      if ( this.isCluster() )
      {
        preferences.put( AppConstants.APPDEPL_CLUSTER, this.getServerName() );
      }
      if ( classloaderorder.equals( "PARENT_LAST" ) )
      {
        classloaderorder = AppConstants.APPDEPL_CLASSLOADINGMODE_PARENTLAST;
      }
      else
      {
        classloaderorder = AppConstants.APPDEPL_CLASSLOADINGMODE_PARENTFIRST;
      }
      if ( classloaderpolicy.equals( "APPLICATION" ) )
      {
        classloaderpolicy = AppConstants.APPDEPL_CLASSLOADERPOLICY_SINGLE;
      }
      else
      {
        classloaderpolicy = AppConstants.APPDEPL_CLASSLOADERPOLICY_MULTIPLE;
      }
      preferences.put( AppConstants.APPDEPL_CLASSLOADINGMODE, classloaderorder );
      preferences.put( AppConstants.APPDEPL_CLASSLOADERPOLICY, classloaderpolicy );
      if ( vhost == null || vhost.trim().length() < 1 )
      {
        vhost = "default_host";
      }

      LOGGER.debug( "Creating JEE application default bindings" );
      Properties defaultBinding = new Properties();
      defaultBinding.put( AppConstants.APPDEPL_DFLTBNDG_VHOST, vhost );
      preferences.put( AppConstants.APPDEPL_DFLTBNDG, defaultBinding );
      LOGGER.debug( "Creating JEE application options" );
      Hashtable options = new Hashtable();
      options.put( AppConstants.APPDEPL_APPNAME, name );
      options.put( AppConstants.APPDEPL_LOCALE, Locale.getDefault() );
      if ( this.isCluster() )
      {
        options.put( AppConstants.APPDEPL_CLUSTER, this.getServerName() );
        // to use the Archive Upload options, we need to use WebSphere 6.1 libraries
        //options.put(AppConstants.APPDEPL_ARCHIVE_UPLOAD, new Boolean(true));
      }
      options.put( AppConstants.APPDEPL_CLASSLOADINGMODE, classloaderorder );
      options.put( AppConstants.APPDEPL_CLASSLOADERPOLICY, AppConstants.APPDEPL_CLASSLOADERPOLICY_SINGLE );
      options.put( AppConstants.APPDEPL_DFLTBNDG, defaultBinding );

      LOGGER.debug( "Defining JEE application targets" );
      Hashtable module2server = new Hashtable();
      if ( this.isCluster() )
      {
        module2server.put( "*", "WebSphere:cell=" + ObjectNameHelper.getCellName( server ) + ",cluster="
          + this.getServerName() );
      }
      else
      {
        module2server.put( "*", "WebSphere:cell=" + ObjectNameHelper.getCellName( server ) + ",node="
          + ObjectNameHelper.getNodeName( server ) + ",server=" + this.getServerName() );
      }
      options.put( AppConstants.APPDEPL_MODULE_TO_SERVER, module2server );
      // install the JEE application
      LOGGER.debug( "Installing JEE application {}", name );
      appManagement.installApplication( path, name, options, null );
      ConfigServiceProxy configService = this.getConfigServiceProxy();
      Thread.sleep( 5000 );
      configService.save( session, true );
      if ( this.isCluster() )
      {
        syncNodes();
      }
      // waiting for deployment
      int i = 0;
      while ( !this.isJEEApplicationDeployed(path, name) && i < 100 )
      {
        Thread.sleep( 10000 );
        i++;
      }
      Thread.sleep( 60000 );
      // start the JEE application
      LOGGER.debug( "Starting JEE application {}", name );
      appManagement.startApplication( name, preferences, null );
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't deploy JEE application {}", name, e );
      throw new ControllerException( "Can't deploy JEE application " + name, e );
    }
  }

  public void undeployJEEApplication(String path, String name)
    throws ControllerException
  {
    LOGGER.info( "Undeploying JEE application {}", name );
    AdminClient admin = this.getConfigServiceProxy().getAdminClient();
    try
    {
      AppManagement appManagement = AppManagementProxy.getJMXProxyForClient( admin );
      Hashtable preferences = new Hashtable();
      preferences.put( AppConstants.APPDEPL_LOCALE, Locale.getDefault() );
      preferences.put( AppConstants.APPDEPL_APPNAME, name );
      LOGGER.debug( "Stopping JEE application {}", name );
      appManagement.stopApplication( name, preferences, null );
      LOGGER.debug( "Uninstalling JEE application {}", name + " from IBM WebSphere server " + this.getServerName() );
      appManagement.uninstallApplication( name, preferences, null );
      // waiting for undeployment
      int i = 0;
      while ( this.isJEEApplicationDeployed(path, name) && i < 100 )
      {
        Thread.sleep( 10000 );
        i++;
      }
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't undeploy JEE application {}", name, e );
      throw new ControllerException( "Can't undeploy JEE application " + name, e );
    }
  }

  public void redeployJEEApplication(String path, String name)
    throws ControllerException
  {
    LOGGER.info( "Redeploying JEE application {}", name );
    AdminClient admin = this.getConfigServiceProxy().getAdminClient();
    try
    {
      AppManagement appManagement = AppManagementProxy.getJMXProxyForClient( admin );
      Hashtable preferences = new Hashtable();
      preferences.put( AppConstants.APPDEPL_LOCALE, Locale.getDefault() );
      preferences.put( AppConstants.APPDEPL_APPNAME, name );
      appManagement.redeployApplication( path, name, preferences, null );
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't redeploy JEE application {}", name, e );
      throw new ControllerException( "Can't redeploy JEE application " + name, e );
    }
  }

  public boolean isJDBCConnectionPoolDeployed( String name )
    throws ControllerException
  {
    LOGGER.info( "Check if JDBC connection pool {} is deployed", name );
    boolean deployed = false;
    ConfigServiceProxy configService = this.getConfigServiceProxy();
    Session session = new Session();
    // define the JDBC provider MBean
    ObjectName jdbcProvider = ConfigServiceHelper.createObjectName( null, "JDBCProvider", name );
    try
    {
      ObjectName[] jdbcProviders = configService.queryConfigObjects( session, null, jdbcProvider, null );
      if ( jdbcProviders.length > 0 )
      {
        LOGGER.debug( "JDBC Provider " + name + " found." );
        deployed = true;
      }
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't check if JDBC connection pool {} is deployed", name, e );
      throw new ControllerException( "Can't check if JDBC connection pool " + name + " is deployed", e );
    }
    return deployed;
  }

  public boolean isJDBCConnectionPoolUpToDate( String name, String driver, int increment, int initial, int maximal,
                                               String user, String password, String url, String classpath )
    throws ControllerException
  {
    LOGGER.info( "Checking status of JDBC connection pool {}", name );
    if ( !this.isJDBCConnectionPoolDeployed( name ) )
    {
      LOGGER.debug( "JDBC connection pool {} is not deployed", name );
      return false;
    }
    ConfigServiceProxy configService = this.getConfigServiceProxy();
    Session session = new Session();
    try
    {
      // looking for the JDBC provider MBean
      ObjectName jdbcProvider = ConfigServiceHelper.createObjectName( null, "JDBCProvider", name );
      jdbcProvider = configService.queryConfigObjects( session, null, jdbcProvider, null )[0];
      // check the driver attribute
      String currentDriver = (String) configService.getAttribute( session, jdbcProvider, "implementationClassName" );
      if ( !currentDriver.equals( driver ) )
      {
        LOGGER.debug( "JDBC connection pool {} driver is not up to date", name );
        return false;
      }
      // check the classpath
      List currentClasspath = (List) configService.getAttribute( session, jdbcProvider, "classpath" );
      if ( !currentClasspath.equals( classpath ) )
      {
        LOGGER.debug( "JDBC connection pool {} classpath is not up to date", name );
        return false;
      }
      // check J2C authentification data
      LOGGER.debug( "Looking for the JAASAuthData in IBM WebSphere server" );
      ObjectName authData = ConfigServiceHelper.createObjectName( null, "JAASAuthData", null );
      ObjectName[] authDatas = configService.queryConfigObjects( session, null, authData, null );
      authData = null;
      ObjectName currentAuthData = null;
      for ( int i = 0; i < authDatas.length; i++ )
      {
        currentAuthData = authDatas[i];
        String currentAlias = (String) configService.getAttribute( session, currentAuthData, "alias" );
        if ( currentAlias.equals( name + "Authentication" ) )
        {
          authData = currentAuthData;
          break;
        }
      }
      if ( authData == null )
      {
        // the authData doesn't exist
        return false;
      }
      // the authData exists, check user and password
      LOGGER.debug( "JAAS AuthData with the alias {}Authentication exists", name );
      String currentUser = (String) configService.getAttribute( session, authData, "userId" );
      if ( !currentUser.equals( user ) )
      {
        LOGGER.debug( "JDBC connection pool {} username is not up to date", name );
        return false;
      }
      String currentPassword = (String) configService.getAttribute( session, authData, "password" );
      if ( !currentPassword.equals( password ) )
      {
        LOGGER.debug( "JDBC connection pool {} password is not up to date", name );
        return false;
      }
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't check status of JDBC connection pool {}", name, e );
      throw new ControllerException( "Can't check status of JDBC connection pool " + name, e );
    }
    return true;
  }

  public boolean updateJDBCConnectionPool( String name, String driver, int increment, int initial, int maximal,
                                           String user, String password, String url, String classpath )
    throws ControllerException
  {
    LOGGER.info( "Updating JDBC connection pool {}", name );
    boolean updated = false;
    ConfigServiceProxy configService = this.getConfigServiceProxy();
    Session session = new Session();
    try
    {
      ObjectName jdbcProvider = ConfigServiceHelper.createObjectName( null, "JDBCProvider", name );
      jdbcProvider = configService.queryConfigObjects( session, null, jdbcProvider, null )[0];
      // create a new attribute list
      AttributeList attributes = new AttributeList();
      // update the driver attribute if required
      String currentDriver = (String) configService.getAttribute( session, jdbcProvider, "implementationClassName" );
      if ( !currentDriver.equals( driver ) )
      {
        attributes.add( new Attribute( "implementationClassName", driver ) );
        updated = true;
      }
      configService.setAttributes( session, jdbcProvider, attributes );
      // update the classpath if required
      List currentClasspath = (List) configService.getAttribute( session, jdbcProvider, "classpath" );
      if ( !currentClasspath.contains( classpath ) )
      {
        configService.addElement( session, jdbcProvider, "classpath", classpath, -1 );
        updated = true;
      }
      // update JAAS AuthData
      ObjectName authData = ConfigServiceHelper.createObjectName( null, "JAASAuthData", null );
      ObjectName[] authDatas = configService.queryConfigObjects( session, null, authData, null );
      authData = null;
      ObjectName currentAuthData = null;
      for ( int i = 0; i < authDatas.length; i++ )
      {
        currentAuthData = authDatas[i];
        String currentAlias = (String) configService.getAttribute( session, currentAuthData, "alias" );
        if ( currentAlias.equals( name + "Authentication" ) )
        {
          authData = currentAuthData;
          break;
        }
      }
      if ( authData == null )
      {
        // the JAASAuthData doesn't exist, must create it
        LOGGER.debug( "JAAS AuthData with the alias {}Authentication is not found, creating it", name );
        ObjectName security = ConfigServiceHelper.createObjectName( null, "Security", null );
        security = configService.queryConfigObjects( session, null, security, null )[0];
        // get the main security MBean
        LOGGER.debug( "Getting the main IBM WebSphere Security MBean" );
        AttributeList authAttributes = new AttributeList();
        authAttributes.add( new Attribute( "alias", name + "Authentication" ) );
        authAttributes.add( new Attribute( "userId", user ) );
        authAttributes.add( new Attribute( "password", password ) );
        authAttributes.add(
          new Attribute( "description", "Authentication for JDBC Provider " + name + " created by Apache Kalumet" ) );
        configService.createConfigData( session, security, "authDataEntries", "JAASAuthData", authAttributes );
        updated = true;
      }
      else
      {
        // the JAAS AuthData exists, check for update
        LOGGER.debug( "JAAS AuthData with the alias {}Authentication already exists, check for update", name );
        AttributeList authAttributes = new AttributeList();
        String currentUser = (String) configService.getAttribute( session, authData, "userId" );
        if ( !currentUser.equals( user ) )
        {
          authAttributes.add( new Attribute( "userId", user ) );
          updated = true;
        }
        String currentPassword = (String) configService.getAttribute( session, authData, "password" );
        if ( !currentPassword.equals( password ) )
        {
          authAttributes.add( new Attribute( "password", password ) );
          updated = true;
        }
        configService.setAttributes( session, authData, authAttributes );
      }
      if ( updated )
      {
        configService.save( session, true );
      }
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't update JDBC connection pool {}", name, e );
      throw new ControllerException( "Can't update JDBC connection pool " + name, e );
    }
    return updated;
  }

  public void deployJDBCConnectionPool( String name, String driver, int increment, int initial, int maximal,
                                        String user, String password, String url, String classpath )
    throws ControllerException
  {
    LOGGER.info( "Deploying JDBC connection pool {}", name );
    ObjectName jdbcProvider = null;
    ConfigServiceProxy configService = this.getConfigServiceProxy();
    Session session = new Session();
    try
    {
      // define the target scope
      ObjectName scope = null;
      if ( this.isCluster() )
      {
        scope = ConfigServiceHelper.createObjectName( null, "Cell", getCellName( configService.getAdminClient() ) );
      }
      else
      {
        scope = ConfigServiceHelper.createObjectName( null, "Server", this.getServerName() );
      }
      scope = configService.queryConfigObjects( session, null, scope, null )[0];
      ObjectName jdbcProviderObjectName = null;
      // create the JDBC Provider
      AttributeList attributes = new AttributeList();
      attributes.clear();
      attributes.add( new Attribute( "name", name ) );
      attributes.add( new Attribute( "implementationClassName", driver ) );
      attributes.add( new Attribute( "description", "JDBC Provider " + name + " created by Apache Kalumet" ) );
      jdbcProvider = configService.createConfigData( session, scope, "JDBCProvider", "JDBCProvider", attributes );
      // add the classpath
      configService.addElement( session, jdbcProvider, "classpath", classpath, -1 );
      // create the JASS AuthData
      ObjectName security = ConfigServiceHelper.createObjectName( null, "Security", null );
      security = configService.queryConfigObjects( session, null, security, null )[0];
      AttributeList authAttributes = new AttributeList();
      authAttributes.add( new Attribute( "alias", name + "Authentication" ) );
      authAttributes.add( new Attribute( "userId", user ) );
      authAttributes.add( new Attribute( "password", password ) );
      authAttributes.add(
        new Attribute( "description", "Authentication for JDBC Provider " + name + " created by Apache Kalumet" ) );
      configService.createConfigData( session, security, "authDataEntries", "JAASAuthData", authAttributes );
      // save the session
      configService.save( session, true );
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't deploy JDBC connection pool {}", name, e );
      throw new ControllerException( "Can't deploy JDBC connection pool " + name, e );
    }
  }

  public void undeployJDBCConnectionPool( String name )
    throws ControllerException
  {
    LOGGER.info( "Undeploying JDBC connection pool {}", name );
    ConfigServiceProxy configService = this.getConfigServiceProxy();
    Session session = new Session();
    try
    {
      // get the JDBC Provider MBean
      ObjectName jdbcProvider = ConfigServiceHelper.createObjectName( null, "JDBCProvider", name );
      jdbcProvider = configService.queryConfigObjects( session, null, jdbcProvider, null )[0];
      // delete the JDBC Provider
      configService.deleteConfigData( session, jdbcProvider );
      // get the JAAS AuthData MBean
      ObjectName authData = ConfigServiceHelper.createObjectName( null, "JAASAuthData", null );
      ObjectName[] authDatas = configService.queryConfigObjects( session, null, authData, null );
      authData = null;
      ObjectName currentAuthData = null;
      for ( int i = 0; i < authDatas.length; i++ )
      {
        currentAuthData = authDatas[i];
        String currentAlias = (String) configService.getAttribute( session, currentAuthData, "alias" );
        if ( currentAlias.equals( name + "Authentication" ) )
        {
          authData = currentAuthData;
          break;
        }
      }
      if ( authData != null )
      {
        configService.deleteConfigData( session, authData );
      }
      configService.save( session, true );
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't undeploy JDBC connection pool {}", name, e );
      throw new ControllerException( "Can't undeploy JDBC connection pool " + name, e );
    }
  }

  public boolean isJDBCDataSourceDeployed( String name )
    throws ControllerException
  {
    LOGGER.info( "Checking if JDBC data source {} is already deployed", name );
    ConfigServiceProxy configService = this.getConfigServiceProxy();
    Session session = new Session();
    try
    {
      // get the JDBC DataSource MBean
      ObjectName dataSource = ConfigServiceHelper.createObjectName( null, "DataSource", name );
      ObjectName[] dataSources = configService.queryConfigObjects( session, null, dataSource, null );
      if ( dataSources.length > 0 )
      {
        return true;
      }
    }
    catch ( Exception e )
    {
      LOGGER.warn( "Can't check if JDBC data source {} is arelady deployed", name, e );
      return false;
    }
    return false;
  }

  public void deployJDBCDataSource( String name, String jdbcConnectionPool, String jdbcUrl, String helperClassname )
    throws ControllerException
  {
    LOGGER.info( "Deploying JDBC data source {}", name );
    ConfigServiceProxy configService = this.getConfigServiceProxy();
    Session session = new Session();
    ObjectName dataSource = null;
    try
    {
      // define the scope
      ObjectName scope = null;
      if ( this.isCluster() )
      {
        scope = ConfigServiceHelper.createObjectName( null, "Cell", getCellName( configService.getAdminClient() ) );
      }
      else
      {
        scope = ConfigServiceHelper.createObjectName( null, "Server", this.getServerName() );
      }
      scope = configService.queryConfigObjects( session, null, scope, null )[0];
      // looking for the JDBC provider
      ObjectName jdbcProvider = ConfigServiceHelper.createObjectName( null, "JDBCProvider", jdbcConnectionPool );
      jdbcProvider = configService.queryConfigObjects( session, null, jdbcProvider, null )[0];
      // prepare the attribute list
      AttributeList attributes = new AttributeList();
      attributes.add( new Attribute( "name", name ) );
      attributes.add( new Attribute( "jndiName", name ) );
      attributes.add( new Attribute( "description", "DataSource " + name + " created by Apache Kalumet" ) );
      attributes.add( new Attribute( "datasourceHelperClassname", helperClassname ) );
      attributes.add( new Attribute( "authDataAlias", jdbcConnectionPool + "Authentication" ) );
      // create the datasource
      dataSource = configService.createConfigData( session, jdbcProvider, "DataSource", "DataSource", attributes );
      // create the corresponding J2CResourceAdapter connection factory
      // object
      ObjectName jra = ConfigServiceHelper.createObjectName( null, "J2CResourceAdapter", null );
      ObjectName[] jras = configService.queryConfigObjects( session, scope, jra, null );
      int i = 0;
      for (; i < jras.length; i++ )
      {
        // quit for the first builtin JRA found
        if ( jras[i].getKeyProperty( SystemAttributes._WEBSPHERE_CONFIG_DATA_DISPLAY_NAME ).equals(
          "WebSphere Relational Resource Adapter" ) )
        {
          break;
        }
      }
      // create the CMP engine mapping
      AttributeList cmpEngineAttributes = new AttributeList();
      cmpEngineAttributes.add( new Attribute( "name", name + "_CF" ) );
      cmpEngineAttributes.add( new Attribute( "authMechanismPreference", "BASIC_PASSWORD" ) );
      cmpEngineAttributes.add( new Attribute( "authDataAlias", jdbcConnectionPool + "Authentication" ) );
      cmpEngineAttributes.add( new Attribute( "cmpDatasource", dataSource ) );
      configService.createConfigData( session, jras[i], "CMPConnectorFactory", "CMPConnectorFactory",
                                      cmpEngineAttributes );
      // define special properties for this new datasource
      AttributeList propertySet = new AttributeList();
      ObjectName resourcePropertySet =
        configService.createConfigData( session, dataSource, "propertySet", "", propertySet );
      // add a resourceProperty URL
      AttributeList urlProperty = new AttributeList();
      urlProperty.add( new Attribute( "name", "URL" ) );
      urlProperty.add( new Attribute( "type", "java.lang.String" ) );
      urlProperty.add( new Attribute( "value", jdbcUrl ) );
      configService.addElement( session, resourcePropertySet, "resourceProperties", urlProperty, -1 );
      // save the configuration
      configService.save( session, true );
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't deploy JDBC data source {}", name, e );
      throw new ControllerException( "Can't deploy JDBC data source " + name, e );
    }
  }

  public void undeployJDBCDataSource( String name )
    throws ControllerException
  {
    LOGGER.info( "Undeploying JDBC data source {}", name );
    ConfigServiceProxy configService = this.getConfigServiceProxy();
    Session session = new Session();
    ObjectName dataSource = ConfigServiceHelper.createObjectName( null, "DataSource", name );
    try
    {
      // delete DataSource
      dataSource = configService.queryConfigObjects( session, null, dataSource, null )[0];
      configService.deleteConfigData( session, dataSource );
      // delete the CMP connector factory
      ObjectName cmpConnectorFactory =
        ConfigServiceHelper.createObjectName( null, "CMPConnectorFactory", name + "_CF" );
      cmpConnectorFactory = configService.queryConfigObjects( session, null, cmpConnectorFactory, null )[0];
      configService.deleteConfigData( session, cmpConnectorFactory );
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't undeploy JDBC data source {}", name, e );
      throw new ControllerException( "Can't undeploy JDBC data source " + name, e );
    }
  }

  public boolean isJDBCDataSourceUpToDate( String name, String jdbcConnectionPool, String jdbcUrl,
                                           String helperClassname )
    throws ControllerException
  {
    LOGGER.info( "Checking status of JDBC data source {}", name );
    if ( !this.isJDBCDataSourceDeployed( name ) )
    {
      LOGGER.debug( "JDBC data source {} is not deployed", name );
      return false;
    }
    ConfigServiceProxy configService = this.getConfigServiceProxy();
    Session session = new Session();
    ObjectName dataSource = ConfigServiceHelper.createObjectName( null, "DataSource", name );
    try
    {
      dataSource = configService.queryConfigObjects( session, null, dataSource, null )[0];
      // check the JDBC Provider
      ObjectName provider = (ObjectName) configService.getAttribute( session, dataSource, "provider" );
      String currentProvider = (String) configService.getAttribute( session, provider, "name" );
      if ( !currentProvider.equals( jdbcConnectionPool ) )
      {
        return false;
      }
      // check the authDataAlias
      String currentAuthDataAlias = (String) configService.getAttribute( session, dataSource, "authDataAlias" );
      if ( !currentAuthDataAlias.equals( jdbcConnectionPool + "Authentication" ) )
      {
        return false;
      }
      // check the helper classname
      String currentHelperclass =
        (String) configService.getAttribute( session, dataSource, "datasourceHelperClassname" );
      if ( !currentHelperclass.equals( helperClassname ) )
      {
        return false;
      }
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't check status of JDBC data source {}", name, e );
      throw new ControllerException( "Can't check JDBC data source " + name, e );
    }
    return true;
  }

  public boolean updateJDBCDataSource( String name, String jdbcConnectionPool, String jdbcUrl, String helperClassname )
    throws ControllerException
  {
    LOGGER.info( "Updating JDBC data source {}", name );
    boolean updated = false;
    ConfigServiceProxy configService = this.getConfigServiceProxy();
    Session session = new Session();
    ObjectName dataSource = ConfigServiceHelper.createObjectName( null, "DataSource", name );
    try
    {
      dataSource = configService.queryConfigObjects( session, null, dataSource, null )[0];
      AttributeList dataSourceAttributes = new AttributeList();
      ObjectName provider = (ObjectName) configService.getAttribute( session, dataSource, "provider" );
      String currentProvider = (String) configService.getAttribute( session, provider, "name" );
      // update the JDBC Provider if required
      if ( !currentProvider.equals( jdbcConnectionPool ) )
      {
        provider = ConfigServiceHelper.createObjectName( null, "JDBCProvider", jdbcConnectionPool );
        provider = configService.queryConfigObjects( session, null, provider, null )[0];
        dataSourceAttributes.add( new Attribute( "provider", provider ) );
        updated = true;
      }
      // update the authDataAlias
      String currentAuthDataAlias = (String) configService.getAttribute( session, dataSource, "authDataAlias" );
      if ( !currentAuthDataAlias.equals( jdbcConnectionPool + "Authentication" ) )
      {
        dataSourceAttributes.add( new Attribute( "authDataAlias", jdbcConnectionPool + "Authentication" ) );
        // update the CMP Engine
        ObjectName cmpEngine = ConfigServiceHelper.createObjectName( null, "CMPConnectorFactory", name + "_CF" );
        cmpEngine = configService.queryConfigObjects( session, null, cmpEngine, null )[0];
        AttributeList cmpEngineAttributes = new AttributeList();
        cmpEngineAttributes.add( new Attribute( "authDataAlias", jdbcConnectionPool + "Authentication" ) );
        configService.setAttributes( session, cmpEngine, cmpEngineAttributes );
        updated = true;
      }
      // update the helper classname
      String currentHelperclass =
        (String) configService.getAttribute( session, dataSource, "datasourceHelperClassname" );
      if ( !currentHelperclass.equals( helperClassname ) )
      {
        dataSourceAttributes.add( new Attribute( "datasourceHelperClassname", helperClassname ) );
        updated = true;
      }
      // save the attributes of the DataSource
      configService.setAttributes( session, dataSource, dataSourceAttributes );
      // update the JDBC URL in the DataSource property set (always)
      AttributeList value = configService.getAttributes( session, dataSource, new String[]{ "propertySet" }, false );
      ObjectName propertySet = (ObjectName) ConfigServiceHelper.getAttributeValue( value, "propertySet" );
      AttributeList urlProperty = new AttributeList();
      urlProperty.add( new Attribute( "name", "URL" ) );
      urlProperty.add( new Attribute( "type", "java.lang.String" ) );
      urlProperty.add( new Attribute( "value", jdbcUrl ) );
      configService.addElement( session, propertySet, "resourceProperties", urlProperty, -1 );
      // save the configuration
      configService.save( session, true );
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't update JDBC data source {}", name, e );
      throw new ControllerException( "Can't update JDBC data source " + name, e );
    }
    return updated;
  }

  public boolean isJMSConnectionFactoryDeployed( String name )
    throws ControllerException
  {
    LOGGER.info( "Checking if JMS connection factory {} is deployed", name );
    ConfigServiceProxy configService = this.getConfigServiceProxy();
    Session session = new Session();
    ObjectName jmsConnectionFactory = ConfigServiceHelper.createObjectName( null, "JMSConnectionFactory", name );
    try
    {
      ObjectName[] jmsConnectionFactories =
        configService.queryConfigObjects( session, null, jmsConnectionFactory, null );
      if ( jmsConnectionFactories.length > 0 )
      {
        return true;
      }
    }
    catch ( Exception e )
    {
      LOGGER.warn( "Can't check if JMS connection factory {} is deployed", name, e );
      return false;
    }
    return false;
  }

  public void deployJMSConnectionFactory( String name )
    throws ControllerException
  {
    LOGGER.info( "Deploying JMS connection factory {}", name );
    ConfigServiceProxy configService = this.getConfigServiceProxy();
    Session session = new Session();
    try
    {
      ObjectName scope = null;
      if ( this.isCluster() )
      {
        scope = ConfigServiceHelper.createObjectName( null, "Cell", getCellName( configService.getAdminClient() ) );
      }
      else
      {
        scope = ConfigServiceHelper.createObjectName( null, "Server", this.getServerName() );
      }
      scope = configService.queryConfigObjects( session, null, scope, null )[0];
      // looking for the builtin JMS provider
      ObjectName jmsProvider = ConfigServiceHelper.createObjectName( null, "JMSProvider", null );
      ObjectName[] jmsProviders = configService.queryConfigObjects( session, scope, jmsProvider, null );
      int i = 0;
      for (; i < jmsProviders.length; i++ )
      {
        if ( jmsProviders[i].getKeyProperty( SystemAttributes._WEBSPHERE_CONFIG_DATA_DISPLAY_NAME ).equals(
          "WebSphere JMS Provider" ) )
        {
          break;
        }
      }
      if ( i >= jmsProviders.length )
      {
        LOGGER.error( "IBM Websphere builtin JMS provider is not found" );
        throw new ControllerException( "IBM Websphere builtin JMS provider is not found" );
      }
      jmsProvider = jmsProviders[i];
      // create the JMS connection factory
      AttributeList jmsAttributes = new AttributeList();
      jmsAttributes.add( new Attribute( "name", name ) );
      jmsAttributes.add( new Attribute( "jndiName", name ) );
      jmsAttributes.add(
        new Attribute( "description", "JMS Connection Factory " + name + " created by Apache Kalumet" ) );
      jmsAttributes.add( new Attribute( "XAEnabled", new Boolean( true ) ) );
      configService.createConfigData( session, jmsProvider, "WASQueueConnectionFactory", "JMSConnectionFactory",
                                      jmsAttributes );
      // save the configuration
      configService.save( session, true );
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't deploy JMS connection factory {}", name, e );
      throw new ControllerException( "Can't deploy JMS connection factory " + name, e );
    }
  }

  public void undeployJMSConnectionFactory( String name )
    throws ControllerException
  {
    LOGGER.info( "Undeploying JMS connection factory {}", name );
    ConfigServiceProxy configService = this.getConfigServiceProxy();
    Session session = new Session();
    ObjectName jmsConnectionFactory = ConfigServiceHelper.createObjectName( null, "JMSConnectionFactory", name );
    try
    {
      jmsConnectionFactory = configService.queryConfigObjects( session, null, jmsConnectionFactory, null )[0];
      configService.deleteConfigData( session, jmsConnectionFactory );
      configService.save( session, true );
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't undeploy JMS connection factory {}", name, e );
      throw new ControllerException( "Can't undeploy JMS connection factory " + name, e );
    }
  }

  public boolean isJMSServerDeployed( String name )
    throws ControllerException
  {
    LOGGER.info( "Checking if JMS server {} is deployed", name );
    ConfigServiceProxy configService = this.getConfigServiceProxy();
    Session session = new Session();
    // lookup a JMS server
    ObjectName jmsServer = ConfigServiceHelper.createObjectName( null, "Server", name );
    try
    {
      ObjectName[] jmsServers = configService.queryConfigObjects( session, null, jmsServer, null );
      if ( jmsServers.length > 0 )
      {
        return true;
      }
    }
    catch ( Exception e )
    {
      LOGGER.warn( "Can't check if JMS server {} is deployed", name, e );
      return false;
    }
    return false;
  }

  /**
   * Deploy a JMS WAS Queue in the IBM WebSphere JMS built-in provider of a
   * given JMS server.
   *
   * @param jmsServerName the JMS server.
   * @param name          the JMS WAS Queue name.
   */
  private void jmsWasQueueDeploy( String jmsServerName, String name )
    throws ControllerException
  {
    LOGGER.debug( "Deploying JMS WAS Queue {}", name );
    ConfigServiceProxy configService = this.getConfigServiceProxy();
    Session session = new Session();
    try
    {
      // get the JMS Server
      ObjectName jmsServer = ConfigServiceHelper.createObjectName( null, "Server", jmsServerName );
      jmsServer = configService.queryConfigObjects( session, null, jmsServer, null )[0];
      // get the WebSphere Internal JMS Provider
      ObjectName jmsProvider = ConfigServiceHelper.createObjectName( null, "JMSProvider", null );
      ObjectName[] jmsProviders = configService.queryConfigObjects( session, jmsServer, jmsProvider, null );
      int i = 0;
      for (; i < jmsProviders.length; i++ )
      {
        if ( jmsProviders[i].getKeyProperty( SystemAttributes._WEBSPHERE_CONFIG_DATA_DISPLAY_NAME ).equals(
          "WebSphere JMS Provider" ) )
        {
          break;
        }
      }
      if ( i >= jmsProviders.length )
      {
        LOGGER.error( "IBM WebSphere Builtin JMS Provider not found" );
        throw new ControllerException( "IBM WebSphere Builtin JMS Provider not found" );
      }
      jmsProvider = jmsProviders[i];
      // create the JMS WAS Queue properties
      AttributeList queueAttributes = new AttributeList();
      queueAttributes.add( new Attribute( "name", name ) );
      queueAttributes.add( new Attribute( "jndiName", name ) );
      queueAttributes.add( new Attribute( "description", "JMS WAS Queue " + name + " deployed by Apache Kalumet" ) );
      queueAttributes.add( new Attribute( "queue", name ) );
      // create the queue
      configService.createConfigData( session, jmsProvider, "WASQueue", "WASQueue", queueAttributes );
      // save the configuration
      configService.save( session, true );
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't deploy JMS WAS Queue {}", name, e );
      throw new ControllerException( "Can't deploy JMS WAS Queue " + name, e );
    }
  }

  /**
   * Deploy a JMS WAS Topic in the IBM WebSphere JMS built-in provider of a given JMS server.
   *
   * @param jmsServerName the JMS server.
   * @param name          the JMS WAS Topic name.
   */
  private void jmsWasTopicDeploy( String jmsServerName, String name )
    throws ControllerException
  {
    LOGGER.debug( "Deploying JMS WAS Topic {}", name );
    ConfigServiceProxy configService = this.getConfigServiceProxy();
    Session session = new Session();
    try
    {
      // get the JMS Server
      ObjectName jmsServer = ConfigServiceHelper.createObjectName( null, "Server", jmsServerName );
      jmsServer = configService.queryConfigObjects( session, null, jmsServer, null )[0];
      // get the WebSphere Internal JMS Provider
      ObjectName jmsProvider = ConfigServiceHelper.createObjectName( null, "JMSProvider", null );
      ObjectName[] jmsProviders = configService.queryConfigObjects( session, jmsServer, jmsProvider, null );
      int i = 0;
      for (; i < jmsProviders.length; i++ )
      {
        if ( jmsProviders[i].getKeyProperty( SystemAttributes._WEBSPHERE_CONFIG_DATA_DISPLAY_NAME ).equals(
          "WebSphere JMS Provider" ) )
        {
          break;
        }
      }
      if ( i >= jmsProviders.length )
      {
        LOGGER.error( "IBM WebSphere Builtin JMS Provider not found" );
        throw new ControllerException( "IBM WebSphere Builtin JMS Provider not found" );
      }
      jmsProvider = jmsProviders[i];
      // create the JMS WAS Queue properties
      AttributeList topicAttributes = new AttributeList();
      topicAttributes.add( new Attribute( "name", name ) );
      topicAttributes.add( new Attribute( "jndiName", name ) );
      topicAttributes.add( new Attribute( "description", "JMS WAS Topic " + name + " deployed by Apache Kalumet" ) );
      topicAttributes.add( new Attribute( "topic", name ) );
      // create the queue
      configService.createConfigData( session, jmsProvider, "WASTopic", "WASTopic", topicAttributes );
      // save the configuration
      configService.save( session, true );
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't deploy JMS WAS Topic {}", name, e );
      throw new ControllerException( "Can't deploy JMS WAS Topic " + name, e );
    }
  }

  public void deployJMSServer( String name, List queues, List topics )
    throws ControllerException
  {
    LOGGER.info( "Deploying JMS server {}", name );
    // deploy the queues
    for ( Iterator queueIterator = queues.iterator(); queueIterator.hasNext(); )
    {
      String queue = (String) queueIterator.next();
      this.jmsWasQueueDeploy( name, queue );
    }
    // deploy the topics
    for ( Iterator topicIterator = topics.iterator(); topicIterator.hasNext(); )
    {
      String topic = (String) topicIterator.next();
      this.jmsWasTopicDeploy( name, topic );
    }
  }

  public boolean isJMSServerUpToDate( String name, List queues, List topics )
    throws ControllerException
  {
    LOGGER.info( "Checking status of JMS server {}", name );
    if ( !this.isJMSServerDeployed( name ) )
    {
      return false;
    }
    ConfigServiceProxy configService = this.getConfigServiceProxy();
    Session session = new Session();
    try
    {
      // get the JMS server
      ObjectName jmsServer = ConfigServiceHelper.createObjectName( null, "Server", name );
      jmsServer = configService.queryConfigObjects( session, null, jmsServer, null )[0];
      // looking for the IBM Websphere Internal JMS Provider
      ObjectName jmsProvider = ConfigServiceHelper.createObjectName( null, "JMSProvider", null );
      ObjectName[] jmsProviders = configService.queryConfigObjects( session, jmsServer, jmsProvider, null );
      int i = 0;
      for (; i < jmsProviders.length; i++ )
      {
        if ( jmsProviders[i].getKeyProperty( SystemAttributes._WEBSPHERE_CONFIG_DATA_DISPLAY_NAME ).equals(
          "WebSphere JMS Provider" ) )
        {
          break;
        }
      }
      if ( i >= jmsProviders.length )
      {
        LOGGER.error( "IBM WebSphere builtin JMS provider is not found" );
        throw new ControllerException( "IBM WebSphere builtin JMS provider is not found" );
      }
      jmsProvider = jmsProviders[i];
      // check the queues
      for ( Iterator queueIterator = queues.iterator(); queueIterator.hasNext(); )
      {
        String queueName = (String) queueIterator.next();
        ObjectName queue = ConfigServiceHelper.createObjectName( null, "WASQueue", queueName );
        ObjectName[] queueList = configService.queryConfigObjects( session, jmsProvider, queue, null );
        if ( queueList.length < 1 )
        {
          return false;
        }
      }
      // check the topics
      for ( Iterator topicIterator = topics.iterator(); topicIterator.hasNext(); )
      {
        String topicName = (String) topicIterator.next();
        ObjectName topic = ConfigServiceHelper.createObjectName( null, "WASTopic", topicName );
        ObjectName[] topicList = configService.queryConfigObjects( session, jmsProvider, topic, null );
        if ( topicList.length < 1 )
        {
          return false;
        }
      }
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't check status of JMS server {}", name, e );
      throw new ControllerException( "Can't check status of JMS server " + name, e );
    }
    return true;
  }

  public boolean updateJMSServer( String name, List queues, List topics )
    throws ControllerException
  {
    LOGGER.info( "Updating JMS server {}", name );
    boolean updated = false;
    ConfigServiceProxy configService = this.getConfigServiceProxy();
    Session session = new Session();
    try
    {
      // looking the JMS server
      ObjectName jmsServer = ConfigServiceHelper.createObjectName( null, "Server", name );
      jmsServer = configService.queryConfigObjects( session, null, jmsServer, null )[0];
      // looking for the IBM Websphere Internal JMS Provider
      ObjectName jmsProvider = ConfigServiceHelper.createObjectName( null, "JMSProvider", null );
      ObjectName[] jmsProviders = configService.queryConfigObjects( session, jmsServer, jmsProvider, null );
      int i = 0;
      for (; i < jmsProviders.length; i++ )
      {
        if ( jmsProviders[i].getKeyProperty( SystemAttributes._WEBSPHERE_CONFIG_DATA_DISPLAY_NAME ).equals(
          "WebSphere JMS Provider" ) )
        {
          break;
        }
      }
      if ( i >= jmsProviders.length )
      {
        LOGGER.error( "IBM WebSphere builtin JMS provider is not found" );
        throw new ControllerException( "IBM WebSphere builtin JMS provider is not found" );
      }
      jmsProvider = jmsProviders[i];
      // check the queues
      for ( Iterator queueIterator = queues.iterator(); queueIterator.hasNext(); )
      {
        String queueName = (String) queueIterator.next();
        ObjectName queue = ConfigServiceHelper.createObjectName( null, "WASQueue", queueName );
        ObjectName[] queueList = configService.queryConfigObjects( session, jmsProvider, queue, null );
        if ( queueList.length < 1 )
        {
          this.jmsWasQueueDeploy( name, queueName );
          updated = true;
        }
      }
      // check the topics
      for ( Iterator topicIterator = topics.iterator(); topicIterator.hasNext(); )
      {
        String topicName = (String) topicIterator.next();
        ObjectName topic = ConfigServiceHelper.createObjectName( null, "WASTopic", topicName );
        ObjectName[] topicList = configService.queryConfigObjects( session, jmsProvider, topic, null );
        if ( topicList.length < 1 )
        {
          this.jmsWasTopicDeploy( name, topicName );
          updated = true;
        }
      }
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't update JMS server {}", name, e );
      throw new ControllerException( "Can't update JMS server " + name, e );
    }
    return updated;
  }

  public void undeployJMSServer( String name )
    throws ControllerException
  {
    LOGGER.info( "Undeploying JMS server {}", name );
    LOGGER.warn( "In IBM WebSphere, the JMS server is builtin" );
  }

  public boolean isJNDIBindingDeployed( String name )
    throws ControllerException
  {
    LOGGER.info( "Checking if JNDI binding {} is deployed", name );
    ConfigServiceProxy configService = this.getConfigServiceProxy();
    Session session = new Session();
    try
    {
      // check string name space binding
      ObjectName nameSpaceBinding = ConfigServiceHelper.createObjectName( null, "StringNameSpaceBinding", name );
      ObjectName[] nameSpaceBindings = configService.queryConfigObjects( session, null, nameSpaceBinding, null );
      if ( nameSpaceBindings.length > 0 )
      {
        return true;
      }
      // check indirect lookup name space binding
      nameSpaceBinding = ConfigServiceHelper.createObjectName( null, "IndirectLookupNameSpaceBinding", name );
      nameSpaceBindings = configService.queryConfigObjects( session, null, nameSpaceBinding, null );
      if ( nameSpaceBindings.length > 0 )
      {
        return true;
      }
    }
    catch ( Exception e )
    {
      LOGGER.warn( "Can't check if JNDI binding {} is deployed", name, e );
      return false;
    }
    return false;
  }

  public void deployJNDIBinding( String name, String jndiName, String jndiAlias, String providerUrl )
    throws ControllerException
  {
    LOGGER.info( "Deploying JNDI binding {}", name );
    ConfigServiceProxy configService = this.getConfigServiceProxy();
    Session session = new Session();
    try
    {
      // get the scope
      ObjectName scope = null;
      if ( this.isCluster() )
      {
        scope = ConfigServiceHelper.createObjectName( null, "Cell", getCellName( configService.getAdminClient() ) );
      }
      else
      {
        scope = ConfigServiceHelper.createObjectName( null, "Server", this.getServerName() );
      }
      scope = configService.queryConfigObjects( session, null, scope, null )[0];
      // prepare the attributes list
      AttributeList attributes = new AttributeList();
      if ( providerUrl != null && providerUrl.trim().length() > 0 )
      {
        // create an IndirectLookupNameSpaceBinding
        attributes.add( new Attribute( "name", name ) );
        attributes.add( new Attribute( "nameInNameSpace", jndiName ) );
        attributes.add( new Attribute( "providerURL", providerUrl ) );
        attributes.add( new Attribute( "jndiName", jndiAlias ) );
        // create it
        configService.createConfigData( session, scope, "IndirectLookupNameSpaceBinding",
                                        "IndirectLookupNameSpaceBinding", attributes );
        // save the configuration
        configService.save( session, true );
        return;
      }
      // create a StringNameSpaceBinding
      attributes.add( new Attribute( "name", name ) );
      attributes.add( new Attribute( "nameInNameSpace", jndiName ) );
      attributes.add( new Attribute( "stringToBind", jndiAlias ) );
      // create it
      configService.createConfigData( session, scope, "StringNameSpaceBinding", "StringNameSpaceBinding", attributes );
      // save the configuration
      configService.save( session, true );
      return;
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't deploy JNDI binding {}", name, e );
      throw new ControllerException( "Can't deploy JNDI binding " + name, e );
    }
  }

  public void undeployJNDIBinding( String name )
    throws ControllerException
  {
    LOGGER.info( "Undeploying JNDI binding {}", name );
    ConfigServiceProxy configService = this.getConfigServiceProxy();
    Session session = new Session();
    try
    {
      ObjectName nameSpaceBinding = ConfigServiceHelper.createObjectName( null, "StringNameSpaceBinding", name );
      ObjectName[] nameSpaceBindings = configService.queryConfigObjects( session, null, nameSpaceBinding, null );
      if ( nameSpaceBindings.length > 0 )
      {
        // I have found a string name space binding
        nameSpaceBinding = nameSpaceBindings[0];
        // delete it
        configService.deleteConfigData( session, nameSpaceBinding );
        // save the configuration
        configService.save( session, true );
        return;
      }
      nameSpaceBinding = ConfigServiceHelper.createObjectName( null, "IndirectLookupNameSpaceBinding", name );
      nameSpaceBindings = configService.queryConfigObjects( session, null, nameSpaceBinding, null );
      if ( nameSpaceBindings.length > 0 )
      {
        // I have found a indirect lookup name space binding
        nameSpaceBinding = nameSpaceBindings[0];
        // delete it
        configService.deleteConfigData( session, nameSpaceBinding );
        // save the configuration
        configService.save( session, true );
        return;
      }
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't undeploy JNDI binding {}", name, e );
      throw new ControllerException( "Can't undeploy JNDI binding" + name, e );
    }
  }

  public boolean isJNDIBindingUpToDate( String name, String jndiName, String jndiAlias, String providerUrl )
    throws ControllerException
  {
    LOGGER.info( "Checking status of JNDI binding {}", name );
    ConfigServiceProxy configService = this.getConfigServiceProxy();
    Session session = new Session();
    try
    {
      ObjectName nameSpaceBinding = ConfigServiceHelper.createObjectName( null, "StringNameSpaceBinding", name );
      ObjectName[] nameSpaceBindings = configService.queryConfigObjects( session, null, nameSpaceBinding, null );
      if ( nameSpaceBindings.length > 0 )
      {
        // I have a StringNameSpaceBinding
        nameSpaceBinding = nameSpaceBindings[0];
        if ( providerUrl != null && providerUrl.trim().length() > 0 )
        {
          // I need a IndirectLookupNameSpaceBinding
          return false;
        }
        String currentJNDIName = (String) configService.getAttribute( session, nameSpaceBinding, "nameInNameSpace" );
        if ( !currentJNDIName.equals( jndiName ) )
        {
          return false;
        }
        String currentJNDIAlias = (String) configService.getAttribute( session, nameSpaceBinding, "stringToBind" );
        if ( !currentJNDIAlias.equals( jndiAlias ) )
        {
          return false;
        }
      }
      nameSpaceBinding = ConfigServiceHelper.createObjectName( null, "IndirectLookupNameSpaceBinding", name );
      nameSpaceBindings = configService.queryConfigObjects( session, null, nameSpaceBinding, null );
      if ( nameSpaceBindings.length > 0 )
      {
        // I have a IndirectLookupNameSpaceBinding
        nameSpaceBinding = nameSpaceBindings[0];
        if ( providerUrl == null || providerUrl.trim().length() < 1 )
        {
          // I need a StringNameSpaceBinding
          return false;
        }
        String currentJNDIName = (String) configService.getAttribute( session, nameSpaceBinding, "nameInNameSpace" );
        if ( !currentJNDIName.equals( jndiName ) )
        {
          return false;
        }
        String currentProviderUrl = (String) configService.getAttribute( session, nameSpaceBinding, "providerURL" );
        if ( !currentProviderUrl.equals( providerUrl ) )
        {
          return false;
        }
        String currentJNDIAlias = (String) configService.getAttribute( session, nameSpaceBinding, "jndiName" );
        if ( !currentJNDIAlias.equals( jndiAlias ) )
        {
          return false;
        }
      }
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't check status of JNDI binding {}", name, e );
      throw new ControllerException( "Can't check status of JNDI binding " + name, e );
    }
    return true;
  }

  public boolean updateJNDIBinding( String name, String jndiName, String jndiAlias, String providerUrl )
    throws ControllerException
  {
    LOGGER.info( "Updating JNDI binding {}", name );
    boolean updated = false;
    ConfigServiceProxy configService = this.getConfigServiceProxy();
    Session session = new Session();
    try
    {
      AttributeList attributes = new AttributeList();
      ObjectName nameSpaceBinding = ConfigServiceHelper.createObjectName( null, "StringNameSpaceBinding", name );
      ObjectName[] nameSpaceBindings = configService.queryConfigObjects( session, null, nameSpaceBinding, null );
      if ( nameSpaceBindings.length > 0 )
      {
        // I have found a StringNameSpaceBinding
        nameSpaceBinding = nameSpaceBindings[0];
        if ( providerUrl != null && providerUrl.trim().length() > 0 )
        {
          // I need a IndirectLookupNameSpaceBinding
          this.undeployJNDIBinding( name );
          this.deployJNDIBinding( name, jndiName, jndiAlias, providerUrl );
          return true;
        }
        String currentJNDIName = (String) configService.getAttribute( session, nameSpaceBinding, "nameInNameSpace" );
        if ( !currentJNDIName.equals( jndiName ) )
        {
          attributes.add( new Attribute( "nameInNameSpace", jndiName ) );
          updated = true;
        }
        String currentJNDIAlias = (String) configService.getAttribute( session, nameSpaceBinding, "stringToBind" );
        if ( !currentJNDIAlias.equals( jndiAlias ) )
        {
          attributes.add( new Attribute( "stringToBind", jndiAlias ) );
          updated = true;
        }
        if ( updated )
        {
          // save the attributes
          configService.setAttributes( session, nameSpaceBinding, attributes );
          // save the configuration
          configService.save( session, true );
          return true;
        }
      }
      nameSpaceBinding = ConfigServiceHelper.createObjectName( null, "IndirectLookupNameSpaceBinding", name );
      nameSpaceBindings = configService.queryConfigObjects( session, null, nameSpaceBinding, null );
      if ( nameSpaceBindings.length > 0 )
      {
        // I have found a IndirectLookupNameSpaceBinding
        nameSpaceBinding = nameSpaceBindings[0];
        if ( providerUrl == null || providerUrl.trim().length() < 1 )
        {
          // I need a StringNameSpaceBinding
          this.undeployJNDIBinding( name );
          this.deployJNDIBinding( name, jndiName, jndiAlias, providerUrl );
          return true;
        }
        String currentJNDIName = (String) configService.getAttribute( session, nameSpaceBinding, "nameInNameSpace" );
        if ( !currentJNDIName.equals( jndiName ) )
        {
          attributes.add( new Attribute( "nameInNameSpace", jndiName ) );
          updated = true;
        }
        String currentProviderUrl = (String) configService.getAttribute( session, nameSpaceBinding, "providerURL" );
        if ( !currentProviderUrl.equals( providerUrl ) )
        {
          attributes.add( new Attribute( "providerURL", providerUrl ) );
          updated = true;
        }
        String currentJNDIAlias = (String) configService.getAttribute( session, nameSpaceBinding, "jndiName" );
        if ( !currentJNDIAlias.equals( jndiAlias ) )
        {
          attributes.add( new Attribute( "jndiName", jndiAlias ) );
          updated = true;
        }
        if ( updated )
        {
          // save the attributes
          configService.setAttributes( session, nameSpaceBinding, attributes );
          // save the configuration
          configService.save( session, true );
          return true;
        }
      }
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't update JNDI binding {}", name, e );
      throw new ControllerException( "Can't update JNDI binding " + name, e );
    }
    return updated;
  }

  public boolean isSharedLibraryDeployed( String name )
    throws ControllerException
  {
    LOGGER.info( "Checking if shared library {} is deployed", name );
    ConfigServiceProxy configService = this.getConfigServiceProxy();
    Session session = new Session();
    try
    {
      ObjectName sharedLibrary = ConfigServiceHelper.createObjectName( null, "Library", name );
      ObjectName[] sharedLibraries = configService.queryConfigObjects( session, null, sharedLibrary, null );
      if ( sharedLibraries.length > 1 )
      {
        return true;
      }
    }
    catch ( Exception e )
    {
      LOGGER.warn( "Can't check if shared library {} is deployed", name, e );
      return false;
    }
    return false;
  }

  public void deploySharedLibrary( String name, String classpath )
    throws ControllerException
  {
    LOGGER.info( "Deploying shared library {}", name );
    ConfigServiceProxy configService = this.getConfigServiceProxy();
    Session session = new Session();
    try
    {
      // define the scope
      ObjectName scope = null;
      if ( this.isCluster() )
      {
        scope = ConfigServiceHelper.createObjectName( null, "Cell", getCellName( configService.getAdminClient() ) );
      }
      else
      {
        scope = ConfigServiceHelper.createObjectName( null, "Server", this.getServerName() );
      }
      scope = configService.queryConfigObjects( session, null, scope, null )[0];
      // prepare the attributes
      AttributeList attributes = new AttributeList();
      attributes.add( new Attribute( "name", name ) );
      attributes.add( new Attribute( "description", "Created by Apache Kalumet" ) );
      ArrayList classpaths = new ArrayList();
      classpaths.add( classpath );
      attributes.add( new Attribute( "classPath", classpaths ) );
      // create the shared library
      configService.createConfigData( session, scope, "Library", "Library", attributes );
      // save the configuration
      configService.save( session, true );
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't deploy shared library {}", name, e );
      throw new ControllerException( "Can't deploy shared library " + name, e );
    }
  }

  public void undeploySharedLibrary( String name )
    throws ControllerException
  {
    LOGGER.info( "Undeploying shared library {}", name );
    ConfigServiceProxy configService = this.getConfigServiceProxy();
    Session session = new Session();
    try
    {
      ObjectName sharedLibrary = ConfigServiceHelper.createObjectName( null, "Library", name );
      sharedLibrary = configService.queryConfigObjects( session, null, sharedLibrary, null )[0];
      configService.deleteConfigData( session, sharedLibrary );
      configService.save( session, true );
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't undeploy shared library " + name, e );
      throw new ControllerException( "Can't undeploy shared library " + name, e );
    }
  }

  public boolean isSharedLibraryUpToDate( String name, String classpath )
    throws ControllerException
  {
    LOGGER.info( "Checking status of shared library {}", name );
    if ( !this.isSharedLibraryDeployed( name ) )
    {
      return false;
    }
    ConfigServiceProxy configService = this.getConfigServiceProxy();
    Session session = new Session();
    try
    {
      ObjectName sharedLibrary = ConfigServiceHelper.createObjectName( null, "Library", name );
      sharedLibrary = configService.queryConfigObjects( session, null, sharedLibrary, null )[0];
      String currentClasspath = (String) configService.getAttribute( session, sharedLibrary, "classPath" );
      if ( !currentClasspath.equals( classpath ) )
      {
        return false;
      }
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't check status of the shared library {}", name, e );
      throw new ControllerException( "Can't check status of shared library " + name, e );
    }
    return true;
  }

  public boolean updateSharedLibrary( String name, String classpath )
    throws ControllerException
  {
    LOGGER.info( "Updating shared library {}", name );
    ConfigServiceProxy configService = this.getConfigServiceProxy();
    Session session = new Session();
    try
    {
      ObjectName sharedLibrary = ConfigServiceHelper.createObjectName( null, "Library", name );
      sharedLibrary = configService.queryConfigObjects( session, null, sharedLibrary, null )[0];
      List currentClasspath = (List) configService.getAttribute( session, sharedLibrary, "classPath" );
      if ( !currentClasspath.get( 0 ).equals( classpath ) )
      {
        AttributeList attributes = new AttributeList();
        AttributeList classpaths = new AttributeList();
        classpaths.add( classpath );
        attributes.add( new Attribute( "classPath", classpaths ) );
        configService.setAttributes( session, sharedLibrary, attributes );
        configService.save( session, true );
        return true;
      }
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't update shared library {}", name, e );
      throw new ControllerException( "Can't update shared library " + name, e );
    }
    return false;
  }

  /**
   * Synchronize all nodes in a WebSphere cluster.
   *
   * @throws ControllerException in case of sync failure.
   */
  private void syncNodes()
    throws ControllerException
  {
    LOGGER.info( "Synchonize IBM WebSphere server nodes" );
    AdminClient admin = this.getConfigServiceProxy().getAdminClient();
    try
    {
      List nodes = listNodes( admin );
      for ( Iterator nodeIterator = nodes.iterator(); nodeIterator.hasNext(); )
      {
        String nodeName = (String) nodeIterator.next();
        ObjectName nodeSync = new ObjectName( "WebSphere:*,type=NodeSync,node=" + nodeName );
        Set queryResult = admin.queryNames( nodeSync, null );
        ObjectName nodeSyncMBean = (ObjectName) queryResult.iterator().next();
        LOGGER.info( "Syncing {}", nodeSyncMBean );
        Object result = admin.invoke( nodeSyncMBean, "sync", null, null );
      }
    }
    catch ( Exception e )
    {
      LOGGER.error( "IBM WebSphere server {} sync failed", this.getServerName(), e );
      throw new ControllerException( "IBM WebSphere server " + this.getServerName() + " sync failed", e );
    }
  }

  /**
   * Get the list of running nodes in a WebSphere cluster.
   *
   * @throws ControllerException in case of listing failure.
   */
  private static List listNodes( AdminClient client )
    throws ControllerException
  {
    Set oSet = queryObjectNames( "WebSphere:*,type=Server", client );
    if ( oSet == null )
    {
      return null;
    }
    List nodes = new LinkedList();
    for ( Iterator it = oSet.iterator(); it.hasNext(); )
    {
      ObjectName on = (ObjectName) it.next();
      if ( !on.getKeyProperty( "processType" ).equals( AdminConstants.DEPLOYMENT_MANAGER_PROCESS ) )
      {
        nodes.add( on.getKeyProperty( "node" ) );
      }
    }
    return nodes;
  }

  /**
   * Request object name on the admin client.
   *
   * @param query  the object name query.
   * @param client the admin client.
   * @return the object name set.
   */
  private static Set queryObjectNames( String query, AdminClient client )
  {
    Set oSet = null;
    try
    {
      if ( client != null )
      {
        oSet = client.queryNames( new ObjectName( query ), null );
      }
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't get object name for query {}", query, e );
    }
    return oSet;
  }

  /**
   * Get the cell name linked to the admin client.
   *
   * @param client the admin client.
   * @return the cell name.
   * @throws Exception in case of query failure.
   */
  private String getCellName( AdminClient client )
    throws Exception
  {
    return client.getServerMBean().getKeyProperty( "cell" );
  }

  /**
   * Get the list of running servers in a WebSphere cluster/node.
   *
   * @param node   the WebSphere node.
   * @param client the admin client.
   * @return the list of running WebSphere servers.
   */
  private static List listServers( String node, AdminClient client )
  {
    List servers = new LinkedList();
    Set oSet = queryObjectNames( "WebSphere:*,type=Server,node=" + node, client );
    if ( oSet == null )
    {
      return null;
    }
    for ( Iterator it = oSet.iterator(); it.hasNext(); )
    {
      ObjectName on = (ObjectName) it.next();
      servers.add( on.getKeyProperty( "process" ) );
    }

    return servers;
  }

}
