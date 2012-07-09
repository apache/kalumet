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
package org.apache.kalumet.controller.jboss;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.kalumet.FileManipulator;
import org.apache.kalumet.FileManipulatorException;
import org.apache.kalumet.controller.core.AbstractJ2EEApplicationServerController;
import org.apache.kalumet.controller.core.ControllerException;
import org.jboss.jmx.adaptor.rmi.RMIAdaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Attribute;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * JBoss4Controller is a controller to connect and manage a JBoss server.
 */
public class JBoss4Controller
  extends AbstractJ2EEApplicationServerController
{

  private final static transient Logger LOGGER = LoggerFactory.getLogger( JBoss4Controller.class );

  private RMIAdaptor server;

  private URL deployURL;

  /**
   * Default constructor.
   *
   * @param url        the JMX URL to connect to the JBoss MBean server.
   * @param username   the JMX username to connect to the JBoss MBean server.
   * @param password   the JMX password to connect to the JBoss MBean server.
   * @param serverName the server/cluster name to manage.
   * @param cluster    a flag indicating if we manage a cluster (true) or a single application server (false).
   */
  public JBoss4Controller( String url, String username, String password, String serverName, Boolean cluster )
    throws ControllerException
  {
    super( url, username, password, serverName, cluster );
  }

  /**
   * Initialize the connection to the JBoss MBean server.
   */
  protected void init()
    throws ControllerException
  {
    Properties properties = new Properties();
    properties.setProperty( Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory" );
    properties.setProperty( "java.naming.factory.url.pkgs", "org.jboss.naming:org.jnp.interfaces" );
    properties.setProperty( Context.PROVIDER_URL, this.getUrl() );
    try
    {
      InitialContext context = new InitialContext( properties );
      server = (RMIAdaptor) context.lookup( "jmx/invoker/RMIAdaptor" );
    }
    catch ( NamingException namingException )
    {
      LOGGER.error( "Can't connect to JBoss JMX RMI Adaptor", namingException );
      throw new ControllerException( "Can't connect to JBoss MBean RMI Adaptor", namingException );
    }
    // disable the deployment scanner and get the deploy directory
    ObjectName deploymentScannerMBean = null;
    try
    {
      deploymentScannerMBean = new ObjectName( "jboss.deployment:flavor=URL,type=DeploymentScanner" );
      server.setAttribute( deploymentScannerMBean, new Attribute( "ScanEnabled", Boolean.FALSE ) );
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't change JBoss deployment scanner", e );
      throw new ControllerException( "Can't change JBoss deployment scanner", e );
    }
    try
    {
      List urlList = (List) server.getAttribute( deploymentScannerMBean, "URLList" );
      if ( urlList.size() < 1 )
      {
        throw new ControllerException( "JBoss deploy URL list is empty" );
      }
      deployURL = (URL) urlList.get( 0 );
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't get JBoss deploy directory", e );
      throw new ControllerException( "Can't get JBoss deploy directory", e );
    }
  }

  public void shutdown()
    throws ControllerException
  {
    LOGGER.info( "Shutting down JBoss application server" );
    LOGGER.debug( "Get the JBoss application server MBean" );
    ObjectName mbean = null;
    try
    {
      mbean = new ObjectName( "jboss.system:type=Server" );
      LOGGER.debug( "Invoke the shutdown operation on the application server MBean" );
      server.invoke( mbean, "shutdown", null, null );
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't shutdown JBoss application server", e );
      throw new ControllerException( "Can't shutdown JBoss application server", e );
    }
  }

  public String status()
  {
    LOGGER.info( "Checking status of JBoss application server" );
    ObjectName mbean = null;
    try
    {
      mbean = new ObjectName( "jboss.system:type=Server" );
      LOGGER.debug( "Getting the server status property." );
      boolean started = false;
      started = ( (Boolean) server.getAttribute( mbean, "Started" ) ).booleanValue();
      if ( started )
      {
        LOGGER.debug( "JBoss server started." );
        return "JBoss application server started since " + (Date) server.getAttribute( mbean, "StartDate" );
      }
      else
      {
        LOGGER.debug( "JBoss server not started." );
        return "JBoss server not started.";
      }
    }
    catch ( Exception e )
    {
      LOGGER.warn( "Can't check status of JBoss application server. The server is probably down.", e );
      return "Can't get the JBoss application server status. The server is probably down.";
    }
  }

  public boolean isStopped()
  {
    LOGGER.info( "Checking if JBoss application server is stopped" );
    ObjectName mbean = null;
    try
    {
      mbean = new ObjectName( "jboss.system:type=Server" );
      LOGGER.debug( "Getting the Started attribute in the server MBean" );
      return !( ( (Boolean) server.getAttribute( mbean, "Started" ) ).booleanValue() );
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't check if JBoss server is stopped. The server is probably down.", e );
      return true;
    }
  }

  /**
   * Format an application path in a JBoss compatible URL.
   *
   * @param path the application path.
   * @return the JBoss compatible URL.
   */
  private static String formatPathToUrl( String path )
  {
    String trimPath = path.trim();
    if ( trimPath.startsWith( "http:" ) || path.startsWith( "file:" ) )
    {
      LOGGER.debug( "The path is already in a JBoss compatible URL format" );
      return trimPath;
    }
    else
    {
      LOGGER.debug( "The path is going to be formatted in a JBoss compatible URL" );
      return "file:" + trimPath;
    }
  }

  public boolean isJ2EEApplicationDeployed( String path, String name )
    throws ControllerException
  {
    LOGGER.info( "Checking if the J2EE application {} is deployed", name );
    String applicationUrl = JBoss4Controller.formatPathToUrl( path );
    boolean deployed = false;
    ObjectName mbean = null;
    try
    {
      mbean = new ObjectName( "jboss.system:service=MainDeployer" );
      deployed = ( (Boolean) server.invoke( mbean, "isDeployed", new Object[]{ applicationUrl },
                                            new String[]{ "java.lang.String" } ) ).booleanValue();
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't check if the J2EE application {} is deployed", name, e );
      throw new ControllerException( "Can't check if the J2EE application " + name + " is deployed", e );
    }
    return deployed;
  }

  public void deployJ2EEApplication( String path, String name, String classloaderorder, String classloaderpolicy,
                                     String vhost )
    throws ControllerException
  {
    LOGGER.info( "Deploying the J2EE application {} ({})", name, path );
    String applicationUrl = JBoss4Controller.formatPathToUrl( path );
    ObjectName mbean = null;
    try
    {
      mbean = new ObjectName( "jboss.system:service=MainDeployer" );
      server.invoke( mbean, "deploy", new Object[]{ applicationUrl }, new String[]{ "java.lang.String" } );
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't deploy the J2EE application {}", name, e );
      throw new ControllerException( "Can't deploy the J2EE application " + name, e );
    }
  }

  public void undeployJ2EEApplication( String path, String name )
    throws ControllerException
  {
    LOGGER.info( "Undeploying the J2EE application {} ({})", name, path );
    String applicationUrl = JBoss4Controller.formatPathToUrl( path );
    ObjectName mbean = null;
    try
    {
      mbean = new ObjectName( "jboss.system:service=MainDeployer" );
      server.invoke( mbean, "undeploy", new Object[]{ applicationUrl }, new String[]{ "java.lang.String" } );
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't undeploy the J2EE application {}", name, e );
      throw new ControllerException( "Can't undeploy the J2EE application " + name, e );
    }
  }

  public void redeployJ2EEApplication( String path, String name )
    throws ControllerException
  {
    LOGGER.info( "Redeploying the J2EE application {} ({})", name, path );
    String applicationUrl = JBoss4Controller.formatPathToUrl( path );
    ObjectName mbean = null;
    try
    {
      mbean = new ObjectName( "jboss.system:service=MainDeployer" );
      server.invoke( mbean, "redeploy", new Object[]{ applicationUrl }, new String[]{ "java.lang.String" } );
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't redeploy the J2EE application {}", name, e );
      throw new ControllerException( "Can't redeploy the J2EE application " + name, e );
    }
  }

  public boolean isJDBCConnectionPoolDeployed( String name )
    throws ControllerException
  {
    LOGGER.info( "Checking if JDBC connection pool {} is deployed", name );
    boolean deployed = false;
    ObjectName mbean = null;
    File file = new File( deployURL.getPath() + "/" + name + "-ds.xml" );
    try
    {
      mbean = new ObjectName( "jboss.system:service=MainDeployer" );
      LOGGER.info( "Getting the MainDeployer MBean" );
      deployed = ( (Boolean) server.invoke( mbean, "isDeployed", new Object[]{ file.toURL() },
                                            new String[]{ "java.net.URL" } ) ).booleanValue();
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
    LOGGER.debug( "Loading the file value" );
    File tempFile = new File( deployURL.getPath() + "/" + name + "-ds.xml.temp" );
    this.jdbcConnectionPoolWriteFile( tempFile, name, driver, increment, initial, maximal, user, password, url );
    FileManipulator fileManipulator = null;
    try
    {
      fileManipulator = new FileManipulator();
      if ( fileManipulator.contentEquals( deployURL.getPath() + "/" + name + "-ds.xml",
                                          deployURL.getPath() + "/" + name + "-ds.xml.temp" ) )
      {
        LOGGER.debug( "JDBC connection pool {} is already up to date", name );
        try
        {
          fileManipulator.delete( tempFile.getAbsolutePath() );
        }
        catch ( Exception e )
        {
          LOGGER.warn( "Can't delete the temp file {}", tempFile.getAbsolutePath(), e );
        }
        return true;
      }
    }
    catch ( FileManipulatorException fileManipulatorException )
    {
      LOGGER.error( "Can't check status of JDBC connection pool {}", name, fileManipulatorException );
      throw new ControllerException( "Can't check status of JDBC connection pool " + name, fileManipulatorException );
    }
    try
    {
      fileManipulator.delete( tempFile.getAbsolutePath() );
    }
    catch ( Exception e )
    {
      LOGGER.warn( "Can't delete the temp file {}", tempFile.getAbsolutePath(), e );
    }
    return false;
  }

  public boolean updateJDBCConnectionPool( String name, String driver, int increment, int initial, int maximal,
                                           String user, String password, String url, String classpath )
    throws ControllerException
  {
    LOGGER.info( "Updating JDBC connection pool {}", name );
    if ( !this.isJDBCConnectionPoolUpToDate( name, driver, increment, initial, maximal, user, password, url,
                                             classpath ) )
    {
      LOGGER.debug( "JDBC connection pool {} must be updated, redeploy it", name );
      this.undeployJDBCConnectionPool( name );
      this.deployJDBCConnectionPool( name, driver, increment, initial, maximal, user, password, url, null );
      return true;
    }
    return false;
  }

  public void deployJDBCConnectionPool( String name, String driver, int increment, int initial, int maximal,
                                        String user, String password, String url, String classpath )
    throws ControllerException
  {
    LOGGER.info( "Deploying the JDBC connection pool {}", name );
    LOGGER.debug( "Create the JBoss datasource XML file" );
    ObjectName mbean = null;
    File file = new File( deployURL.getPath() + "/" + name + "-ds.xml" );
    this.jdbcConnectionPoolWriteFile( file, name, driver, increment, initial, maximal, user, password, url );
    try
    {
      mbean = new ObjectName( "jboss.system:service=MainDeployer" );
      LOGGER.info( "Getting the MainDeployer MBean" );
      server.invoke( mbean, "deploy", new Object[]{ file.toURL() }, new String[]{ "java.net.URL" } );
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't deploy JDBC connection pool {}", name, e );
      throw new ControllerException( "Can't deploy JDBC connection pool " + name, e );
    }
  }

  /**
   * Create a JBoss connection pool/data source XML file.
   *
   * @param file      the JBoss connection pool/data source XML file.
   * @param name      the JDBC connection pool name.
   * @param driver    the JDBC connection pool JDBC driver.
   * @param increment the JDBC connection pool capacity increment.
   * @param initial   the JDBC connection pool initial capacity.
   * @param maximal   the JDBC connection pool maximal capacity.
   * @param user      the JDBC connection pool database user name.
   * @param password  the JDBC connection pool database password.
   * @param url       the JDBC connection pool JDBC URL.
   */
  private void jdbcConnectionPoolWriteFile( File file, String name, String driver, int increment, int initial,
                                            int maximal, String user, String password, String url )
    throws ControllerException
  {
    LOGGER.info( "Writing the JBoss JDBC connection pool/datasource XML file" );
    LOGGER.debug( "Constructing the replacement values" );
    LOGGER.debug( "Checking if we have XA driver or not" );
    InputStreamReader connectionPoolTemplate = null;
    Object[] values = new Object[7];
    values[0] = name;
    values[1] = driver;
    values[2] = url;
    values[3] = user;
    values[4] = password;
    values[5] = new Integer( initial ).toString();
    values[6] = new Integer( maximal ).toString();
    if ( StringUtils.containsIgnoreCase( driver, "xa" ) )
    {
      LOGGER.debug( "XA connection pool detected" );
      connectionPoolTemplate =
        new InputStreamReader( JBoss4Controller.class.getResourceAsStream( "/jboss/template-xa-ds.xml" ) );
    }
    else
    {
      LOGGER.debug( "Non XA connection pool detected" );
      connectionPoolTemplate =
        new InputStreamReader( JBoss4Controller.class.getResourceAsStream( "/jboss/template-ds.xml" ) );
    }
    String connectionPoolContent = JBoss4Controller.format( connectionPoolTemplate, values );
    try
    {
      FileUtils.writeStringToFile( file, connectionPoolContent );
    }
    catch ( IOException ioException )
    {
      LOGGER.error( "Can't write JBoss JDBC connection pool descriptor file", ioException );
      throw new ControllerException( "Can't write JBoss JDBC connection pool descriptor file", ioException );
    }
  }

  /**
   * Format a JBoss configuration file template (JDBC connection
   * pool/datasource, JMS connection factory, etc) with given values.
   *
   * @param templateReader the template reader.
   * @param values         the <code>Object[]</code> values.
   * @return the formatted string.
   */
  private static String format( Reader templateReader, Object[] values )
    throws ControllerException
  {
    try
    {
      BufferedReader templateBufferedReader = new BufferedReader( templateReader );
      StringWriter writer = new StringWriter();
      BufferedWriter buffer = new BufferedWriter( writer );
      String templateLine = templateBufferedReader.readLine();
      while ( templateLine != null )
      {
        buffer.write( MessageFormat.format( templateLine, values ) );
        buffer.newLine();
        templateLine = templateBufferedReader.readLine();
      }
      buffer.flush();
      return writer.toString();
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't format JBoss XML configuration file template", e );
      throw new ControllerException( "Can't format JBoss XML configuration file template", e );
    }
  }

  public void undeployJDBCConnectionPool( String name )
    throws ControllerException
  {
    LOGGER.info( "Undeploying the JDBC connection pool {}", name );
    ObjectName mbean = null;
    File file = new File( deployURL.getPath() + "/" + name + "-ds.xml" );
    try
    {
      mbean = new ObjectName( "jboss.system:service=MainDeployer" );
      server.invoke( mbean, "undeploy", new Object[]{ file.toURL() }, new String[]{ "java.net.URL" } );
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't undeploy the JDBC connection pool {}", name, e );
      throw new ControllerException( "Can't undeploy the JDBC connection pool " + name, e );
    }
  }

  public boolean isJDBCDataSourceDeployed( String name )
    throws ControllerException
  {
    LOGGER.warn( "JDBC data source is not available with JBoss server. Use JDBC connection pool instead." );
    return true;
  }

  public boolean isJDBCDataSourceUpToDate( String name, String jdbcConnectionPool, String jdbcUrl,
                                           String helperClassname )
    throws ControllerException
  {
    LOGGER.warn( "JDBC data source is not available with JBoss server. Use JDBC connection pool instead." );
    return true;
  }

  public void deployJDBCDataSource( String name, String jdbcConnectionPool, String jdbcUrl, String helperClassname )
    throws ControllerException
  {
    LOGGER.warn( "JDBC data source is not available with JBoss server. Use JDBC connection pool instead." );
  }

  public void undeployJDBCDataSource( String name )
    throws ControllerException
  {
    LOGGER.warn( "JDBC data source is not available with JBoss server. Use JDBC connection pool instead." );
  }

  public boolean updateJDBCDataSource( String name, String jdbcConnectionPool, String jdbcUrl, String helperClassname )
    throws ControllerException
  {
    LOGGER.warn( "JDBC data source is not available with JBoss server. Use JDBC connection pool instead." );
    return false;
  }

  public boolean isJMSConnectionFactoryDeployed( String name )
    throws ControllerException
  {
    LOGGER.info( "Checking if the JMS connection factory {} is deployed", name );
    boolean deployed = false;
    ObjectName mbean = null;
    File file = new File( deployURL.getPath() + "/jms/" + name + "-ds.xml" );
    try
    {
      mbean = new ObjectName( "jboss.system:service=MainDeployer" );
      deployed = ( (Boolean) server.invoke( mbean, "isDeployed", new Object[]{ file.toURL() },
                                            new String[]{ "java.net.URL" } ) ).booleanValue();
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't check if the JMS connection factory {} is deployed", name, e );
      throw new ControllerException( "Can't check if the JMS connection factory " + name + " is deployed", e );
    }
    return deployed;
  }

  public void deployJMSConnectionFactory( String name )
    throws ControllerException
  {
    LOGGER.info( "Deploying the JMS connection factory {}", name );
    ObjectName mbean = null;
    File file = new File( deployURL.getPath() + "/jms/" + name + "-ds.xml" );
    this.jmsConnectionFactoryWriteFile( file, name );
    try
    {
      mbean = new ObjectName( "jboss.system:service=MainDeployer" );
      server.invoke( mbean, "deploy", new Object[]{ file.toURL() }, new String[]{ "java.net.URL" } );
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't deploy the JMS connection factory {}", name, e );
      throw new ControllerException( "Can't deploy the JMS connection factory " + name, e );
    }
  }

  /**
   * Write a JBoss JMS connection factory XML file.
   *
   * @param file the JMS connection factory <code>File</code>.
   * @param name the JMS connection factory name.
   */
  private void jmsConnectionFactoryWriteFile( File file, String name )
    throws ControllerException
  {
    LOGGER.info( "Writing JBoss JMS connection factory XML file" );
    LOGGER.debug( "Constructing the replacement values" );
    InputStreamReader connectionFactoryTemplate =
      new InputStreamReader( JBoss4Controller.class.getResourceAsStream( "/jboss/template-jms-ds.xml" ) );
    Object[] values = new Object[1];
    values[0] = name;
    String connectionPoolContent = JBoss4Controller.format( connectionFactoryTemplate, values );
    try
    {
      FileUtils.writeStringToFile( file, connectionPoolContent );
    }
    catch ( IOException ioException )
    {
      LOGGER.error( "Can't write JBoss JMS connection factory descriptor file", ioException );
      throw new ControllerException( "Can't write JBoss JMS connection factory descriptor file", ioException );
    }
  }

  public void undeployJMSConnectionFactory( String name )
    throws ControllerException
  {
    LOGGER.info( "Undeploying the JMS connection factory {}", name );
    File file = new File( deployURL.getPath() + "/jms/" + name + "-ds.xml" );
    ObjectName mbean = null;
    try
    {
      mbean = new ObjectName( "jboss.system:service=MainDeployer" );
      server.invoke( mbean, "undeploy", new Object[]{ file.toURL() }, new String[]{ "java.net.URL" } );
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't undeploy the JMS connection factory {}", name, e );
      throw new ControllerException( "Can't undeploy JMS connection factory " + name, e );
    }
  }

  public boolean isJMSServerDeployed( String name )
    throws ControllerException
  {
    LOGGER.info( "Checking if JMS server {} is deployed", name );
    LOGGER.warn( "JMS server is embedded in the JBoss server" );
    return true;
  }

  /**
   * Write a JBoss JMS queue service file from the template.
   *
   * @param file the target file.
   * @param name the queue name.
   * @throws ControllerException in case of writing failure.
   */
  private void jmsQueueWriteFile( File file, String name )
    throws ControllerException
  {
    InputStreamReader jmsQueueTemplate =
      new InputStreamReader( JBoss4Controller.class.getResourceAsStream( "/jboss/template-jms-queue-service.xml" ) );
    Object[] values = new Object[1];
    values[0] = name;
    String jmsQueueContent = JBoss4Controller.format( jmsQueueTemplate, values );
    try
    {
      FileUtils.writeStringToFile( file, jmsQueueContent );
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't write JBoss JMS queue service file", e );
      throw new ControllerException( "Can't write JBoss JMS queue service file", e );
    }
  }

  /**
   * Write a JBoss JMS topic service file from the template.
   *
   * @param file the target file.
   * @param name the topic name.
   * @throws ControllerException in case of writing failure.
   */
  private void jmsTopicWriteFile( File file, String name )
    throws ControllerException
  {
    InputStreamReader jmsTopicTemplate =
      new InputStreamReader( JBoss4Controller.class.getResourceAsStream( "/jboss/template-jms-topic-service.xml" ) );
    Object[] values = new Object[1];
    values[0] = name;
    String jmsTopicContent = JBoss4Controller.format( jmsTopicTemplate, values );
    try
    {
      FileUtils.writeStringToFile( file, jmsTopicContent );
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't write JBoss JMS topic service file.", e );
      throw new ControllerException( "Can't write JBoss JMS topic service file", e );
    }
  }

  public void deployJMSServer( String name, List queues, List topics )
    throws ControllerException
  {
    LOGGER.info( "Deploying the JMS server {}", name );
    LOGGER.warn( "JMS server is embedded in the JBoss application server" );
    ObjectName mbean = null;
    try
    {
      mbean = new ObjectName( "jboss.system:service=MainDeployer" );
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't get JBoss MainDeployer", e );
      throw new ControllerException( "Can't get JBoss MainDeployer", e );
    }
    LOGGER.info( "Deploying JMS queues" );
    for ( Iterator queueIterator = queues.iterator(); queueIterator.hasNext(); )
    {
      String queue = (String) queueIterator.next();
      File file = new File( deployURL.getPath() + "/jms/" + queue + "-service.xml" );
      this.jmsQueueWriteFile( file, queue );
      LOGGER.info( "Deploying JMS queue {}", queue );
      try
      {
        server.invoke( mbean, "deploy", new Object[]{ file.toURL() }, new String[]{ "java.net.URL" } );
      }
      catch ( Exception e )
      {
        LOGGER.error( "Can't deploy JMS queue {}", queue, e );
        throw new ControllerException( "Can't deploy JMS queue " + queue, e );
      }
    }
    LOGGER.info( "Deploying JMS topics" );
    for ( Iterator topicIterator = topics.iterator(); topicIterator.hasNext(); )
    {
      String topic = (String) topicIterator.next();
      File file = new File( deployURL.getPath() + "/jms/" + topic + "-service.xml" );
      this.jmsTopicWriteFile( file, topic );
      LOGGER.info( "Deploying JMS topic {}", topic );
      try
      {
        server.invoke( mbean, "deploy", new Object[]{ file.toURL() }, new String[]{ "java.net.URL" } );
      }
      catch ( Exception e )
      {
        LOGGER.error( "Can't deploy JMS topic {}", topic, e );
        throw new ControllerException( "Can't deploy JMS topic " + topic, e );
      }
    }
  }

  public boolean isJMSServerUpToDate( String name, List queues, List topics )
    throws ControllerException
  {
    LOGGER.info( "Checking status of the JMS server {}", name );
    try
    {
      ObjectName mbean = new ObjectName( "jboss.mq:service=DestinationManager" );
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't check status of the JMS server {}", name, e );
      throw new ControllerException( "Can't check status of the JMS server " + name, e );
    }
    // check JMS queue
    for ( Iterator queueIterator = queues.iterator(); queueIterator.hasNext(); )
    {
      String queue = (String) queueIterator.next();
      ObjectName queueMBean = null;
      try
      {
        queueMBean = new ObjectName( "jboss.mq.destination:name=" + queue + ",service=Queue" );
        boolean started = false;
        started = ( (Boolean) server.getAttribute( queueMBean, "Started" ) ).booleanValue();
        if ( !started )
        {
          return false;
        }
      }
      catch ( Exception e )
      {
        LOGGER.error( "Can't check status of the JMS queue {}" + queue, e );
        throw new ControllerException( "Can't check status of the JMS queue " + queue, e );
      }
    }
    // check JMS topic
    for ( Iterator topicIterator = topics.iterator(); topicIterator.hasNext(); )
    {
      String topic = (String) topicIterator.next();
      ObjectName topicMBean = null;
      try
      {
        topicMBean = new ObjectName( "jboss.mq.destination:name=" + topic + ",service=Topic" );
        boolean started = false;
        started = ( (Boolean) server.getAttribute( topicMBean, "Started" ) ).booleanValue();
        if ( !started )
        {
          return false;
        }
      }
      catch ( Exception e )
      {
        LOGGER.error( "Can't check status of the JMS topic {}", topic, e );
        throw new ControllerException( "Can't check status of the JMS topic " + topic, e );
      }
    }
    return true;
  }

  public boolean updateJMSServer( String name, List queues, List topics )
    throws ControllerException
  {
    LOGGER.info( "Updating JMS server {}", name );
    LOGGER.info( "Check JMS queues" );
    boolean updated = false;
    for ( Iterator queueIterator = queues.iterator(); queueIterator.hasNext(); )
    {
      String queue = (String) queueIterator.next();
      boolean started = false;
      LOGGER.debug( "Check if the JMS queue {} is deployed", queue );
      ObjectName queueMBean = null;
      try
      {
        queueMBean = new ObjectName( "jboss.mq.destination:name=" + queue + ",service=Queue" );
      }
      catch ( MalformedObjectNameException malformedObjectNameException )
      {
        LOGGER.debug( "The JMS queue seems to not be deployed in the JMS server, deploy it" );
        try
        {
          ObjectName mbean = new ObjectName( "jboss.system:service=MainDeployer" );
          File file = new File( deployURL.getPath() + "/jms/" + queue + "-service.xml" );
          this.jmsQueueWriteFile( file, queue );
          server.invoke( mbean, "deploy", new Object[]{ file.toURL() }, new String[]{ "java.net.URL" } );
          updated = true;
        }
        catch ( Exception e )
        {
          LOGGER.error( "Can't deploy JMS queue {}", queue, e );
          throw new ControllerException( "Can't deploy JMS queue " + queue, e );
        }
      }
      try
      {
        started = ( (Boolean) server.getAttribute( queueMBean, "Started" ) ).booleanValue();
        if ( !started )
        {
          server.invoke( queueMBean, "start", null, null );
          updated = true;
        }
      }
      catch ( Exception e )
      {
        LOGGER.error( "Can't start JMS queue {}", queue, e );
        throw new ControllerException( "Can't start JMS queue " + queue, e );
      }
    }
    LOGGER.info( "Check JMS topics" );
    for ( Iterator topicIterator = topics.iterator(); topicIterator.hasNext(); )
    {
      String topic = (String) topicIterator.next();
      boolean started = false;
      LOGGER.debug( "Check if JMS topic {} is deployed", topic );
      ObjectName topicMBean = null;
      try
      {
        topicMBean = new ObjectName( "jboss.mq.destination:name=" + topic + ",service=Topic" );
      }
      catch ( MalformedObjectNameException malformedObjectNameException )
      {
        LOGGER.debug( "The JMS topic seems to not be deployed in the JMS server, deploy it" );
        try
        {
          ObjectName mbean = new ObjectName( "jboss.system:service=MainDeployer" );
          File file = new File( deployURL.getPath() + "/jms/" + topic + "-service.xml" );
          this.jmsTopicWriteFile( file, topic );
          server.invoke( mbean, "deploy", new Object[]{ file.toURL() }, new String[]{ "java.net.URL" } );
          updated = true;
        }
        catch ( Exception e )
        {
          LOGGER.error( "Can't deploy JMS topic {}", topic, e );
          throw new ControllerException( "Can't deploy JMS topic " + topic, e );
        }
      }
      try
      {
        started = ( (Boolean) server.getAttribute( topicMBean, "Started" ) ).booleanValue();
        if ( !started )
        {
          server.invoke( topicMBean, "start", null, null );
          updated = true;
        }
      }
      catch ( Exception e )
      {
        LOGGER.error( "Can't start JMS topic {}", topic, e );
        throw new ControllerException( "Can't start JMS topic " + topic, e );
      }
    }
    return updated;
  }

  public void undeployJMSServer( String name )
    throws ControllerException
  {
    LOGGER.info( "Undeploying the JMS server {}", name );
    LOGGER.warn( "The JMS server is embedded in JBoss application server" );
  }

  public boolean isJNDIBindingDeployed( String name )
  {
    LOGGER.info( "Checking if JNDI binding {} is deployed", name );
    ObjectName mbean = null;
    try
    {
      mbean = new ObjectName( "jboss:service=JNDIView" );
    }
    catch ( MalformedObjectNameException malformedObjectNameException )
    {
      LOGGER.warn( "Can't check if the JNDI binding {} is deployed", name, malformedObjectNameException );
      return false;
    }
    try
    {
      String output = (String) server.invoke( mbean, "list", new Object[]{ new Boolean( "false" ) },
                                              new String[]{ "java.lang.Boolean" } );
      if ( StringUtils.containsIgnoreCase( output, name ) )
      {
        LOGGER.debug( "The JNDI binding {} has been found", name );
        return true;
      }
    }
    catch ( Exception e )
    {
      LOGGER.warn( "Can't check if the JNDI binding {} is deployed", name, e );
      return false;
    }
    return false;
  }

  /**
   * Write a JBoss JNDI alias service file.
   *
   * @param file the target service file.
   * @param name the JNDI binding name.
   * @param from the JNDI alias from name.
   * @param to   the JNDI alias to name.
   * @throws ControllerException in case of file writing failure.
   */
  private void jndiAliasWriteFile( File file, String name, String from, String to )
    throws ControllerException
  {
    InputStreamReader jmsQueueTemplate =
      new InputStreamReader( JBoss4Controller.class.getResourceAsStream( "/jboss/template-jndi-alias-service.xml" ) );
    Object[] values = new Object[3];
    values[0] = name;
    values[1] = from;
    values[2] = to;
    String jndiAliasContent = JBoss4Controller.format( jmsQueueTemplate, values );
    try
    {
      FileUtils.writeStringToFile( file, jndiAliasContent );
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't write JBoss JNDI binding service file", e );
      throw new ControllerException( "Can't write JBoss JNDI binding service file", e );
    }
  }

  public void deployJNDIBinding( String name, String jndiName, String jndiAlias, String providerUrl )
    throws ControllerException
  {
    LOGGER.info( "Deploying JNDI binding {}", name );
    File file = new File( deployURL.getPath() + "/" + name + "-service.xml" );
    this.jndiAliasWriteFile( file, name, jndiName, jndiAlias );
    try
    {
      ObjectName mbean = new ObjectName( "jboss.system:service=MainDeployer" );
      server.invoke( mbean, "deploy", new Object[]{ file.toURL() }, new String[]{ "java.net.URL" } );
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
    try
    {
      ObjectName mbean = new ObjectName( "jboss:service=Naming" );
      server.invoke( mbean, "removeAlias", new Object[]{ name }, new String[]{ "java.lang.String" } );
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't undeploy JNDI binding {}", name, e );
      throw new ControllerException( "Can't undeploy JNDI binding " + name, e );
    }
  }

  public boolean isJNDIBindingUpToDate( String name, String jndiName, String jndiAlias, String providerUrl )
    throws ControllerException
  {
    LOGGER.info( "Checking status of JNDI binding {}", name );
    if ( isJNDIBindingDeployed( name ) )
    {
      return true;
    }
    return false;
  }

  public boolean updateJNDIBinding( String name, String jndiName, String jndiAlias, String providerUrl )
    throws ControllerException
  {
    LOGGER.info( "Updating JNDI binding {}", name );
    if ( isJNDIBindingDeployed( name ) )
    {
      this.undeployJNDIBinding( name );
      this.deployJNDIBinding( name, jndiName, jndiAlias, providerUrl );
      return true;
    }
    return false; // return false either if the name space binding is always updated
  }

  public boolean isSharedLibraryDeployed( String name )
    throws ControllerException
  {
    LOGGER.warn( "Shared libraries are not supported with JBoss application server" );
    return true;
  }

  public void deploySharedLibrary( String name, String classpath )
    throws ControllerException
  {
    LOGGER.warn( "Shared libraries are not supported with JBoss application server" );
  }

  public void undeploySharedLibrary( String name )
    throws ControllerException
  {
    LOGGER.warn( "Shared libraries are not supported with JBoss application server" );
  }

  public boolean isSharedLibraryUpToDate( String name, String classpath )
    throws ControllerException
  {
    LOGGER.warn( "Shared libraries are not supported with JBoss application server" );
    return false;
  }

  public boolean updateSharedLibrary( String name, String classpath )
    throws ControllerException
  {
    LOGGER.warn( "Shared libraries are not supported with JBoss application server" );
    return false;
  }

}
