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
package org.apache.kalumet.console.app;

import nextapp.echo2.app.Button;
import nextapp.echo2.app.Column;
import nextapp.echo2.app.ContentPane;
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.Grid;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import org.apache.kalumet.console.configuration.ConfigurationManager;

import java.util.Iterator;

// Version Thread
class VersionThread
  extends Thread
{

  private String hostname;

  private int port;

  private boolean completed = false;

  private boolean failure = false;

  private String errorMessage;

  private String version;

  public VersionThread( String hostname, int port )
  {
    this.hostname = hostname;
    this.port = port;
  }

  public boolean getCompleted()
  {
    return this.completed;
  }

  public boolean getFailure()
  {
    return this.failure;
  }

  public String getErrorMessage()
  {
    return this.errorMessage;
  }

  public String getVersion()
  {
    return this.version;
  }

  public void run()
  {
    try
    {
      // call the webservice
      AgentClient webServiceClient = new AgentClient( hostname, port );
      version = webServiceClient.getVersion();
      completed = true;
    }
    catch ( Exception e )
    {
      errorMessage = e.getMessage();
      completed = true;
      failure = true;
    }
  }

}

// ApplicationServerStatusThread
class ApplicationServerStatusThread
  extends Thread
{

  private String hostname;

  private int port;

  private String environmentName;

  private String applicationServerName;

  private boolean completed = false;

  private boolean failure = false;

  private String errorMessage;

  private String status;

  public ApplicationServerStatusThread( String hostname, int port, String environmentName,
                                        String applicationServerName )
  {
    this.hostname = hostname;
    this.port = port;
    this.environmentName = environmentName;
    this.applicationServerName = applicationServerName;
  }

  public boolean getCompleted()
  {
    return this.completed;
  }

  public boolean getFailure()
  {
    return this.failure;
  }

  public String getErrorMessage()
  {
    return this.errorMessage;
  }

  public String getStatus()
  {
    return this.status;
  }

  public void run()
  {
    try
    {
      // call the webservice
      J2EEApplicationServerClient webServiceClient = new J2EEApplicationServerClient( hostname, port );
      status = webServiceClient.status( environmentName, applicationServerName );
      completed = true;
    }
    catch ( Exception e )
    {
      errorMessage = e.getMessage();
      completed = true;
      failure = true;
    }
  }

}

// ConnectionPoolCheckThread
class ConnectionPoolCheckThread
  extends Thread
{

  private String hostname;

  private int port;

  private String environmentName;

  private String applicationServerName;

  private String connectionPoolName;

  private boolean completed = false;

  private boolean failure = false;

  private String errorMessage;

  private boolean uptodate;

  public ConnectionPoolCheckThread( String hostname, int port, String environmentName, String applicationServerName,
                                    String connectionPoolName )
  {
    this.hostname = hostname;
    this.port = port;
    this.environmentName = environmentName;
    this.applicationServerName = applicationServerName;
    this.connectionPoolName = connectionPoolName;
  }

  public boolean getCompleted()
  {
    return this.completed;
  }

  public boolean getFailure()
  {
    return this.failure;
  }

  public String getErrorMessage()
  {
    return this.errorMessage;
  }

  public boolean getUptodate()
  {
    return this.uptodate;
  }

  public void run()
  {
    try
    {
      // call the webservice
      JDBCConnectionPoolClient webServiceClient = new JDBCConnectionPoolClient( hostname, port );
      uptodate = webServiceClient.check( environmentName, applicationServerName, connectionPoolName );
      completed = true;
    }
    catch ( Exception e )
    {
      errorMessage = e.getMessage();
      completed = true;
      failure = true;
    }
  }

}

// DataSourceCheckThread
class DataSourceCheckThread
  extends Thread
{

  private String hostname;

  private int port;

  private String environmentName;

  private String applicationServerName;

  private String dataSourceName;

  private boolean completed = false;

  private boolean failure = false;

  private String errorMessage;

  private boolean uptodate;

  public DataSourceCheckThread( String hostname, int port, String environmentName, String applicationServerName,
                                String dataSourceName )
  {
    this.hostname = hostname;
    this.port = port;
    this.environmentName = environmentName;
    this.applicationServerName = applicationServerName;
    this.dataSourceName = dataSourceName;
  }

  public boolean getCompleted()
  {
    return this.completed;
  }

  public boolean getFailure()
  {
    return this.failure;
  }

  public String getErrorMessage()
  {
    return this.errorMessage;
  }

  public boolean getUptodate()
  {
    return this.uptodate;
  }

  public void run()
  {
    try
    {
      // call the webservice
      JDBCDataSourceClient webServiceClient = new JDBCDataSourceClient( hostname, port );
      uptodate = webServiceClient.check( environmentName, applicationServerName, dataSourceName );
      completed = true;
    }
    catch ( Exception e )
    {
      errorMessage = e.getMessage();
      completed = true;
      failure = true;
    }
  }

}

// JMSConnectionFactoryCheckThread
class JMSConnectionFactoryCheckThread
  extends Thread
{

  private String hostname;

  private int port;

  private String environmentName;

  private String applicationServerName;

  private String jmsConnectionFactoryName;

  private boolean completed = false;

  private boolean failure = false;

  private String errorMessage;

  private boolean uptodate;

  public JMSConnectionFactoryCheckThread( String hostname, int port, String environmentName,
                                          String applicationServerName, String jmsConnectionFactoryName )
  {
    this.hostname = hostname;
    this.port = port;
    this.environmentName = environmentName;
    this.applicationServerName = applicationServerName;
    this.jmsConnectionFactoryName = jmsConnectionFactoryName;
  }

  public boolean getCompleted()
  {
    return this.completed;
  }

  public boolean getFailure()
  {
    return this.failure;
  }

  public String getErrorMessage()
  {
    return this.errorMessage;
  }

  public boolean getUptodate()
  {
    return this.uptodate;
  }

  public void run()
  {
    try
    {
      // call the webservice
      JMSConnectionFactoryClient webServiceClient = new JMSConnectionFactoryClient( hostname, port );
      uptodate = webServiceClient.check( environmentName, applicationServerName, jmsConnectionFactoryName );
      completed = true;
    }
    catch ( Exception e )
    {
      errorMessage = e.getMessage();
      completed = true;
      failure = true;
    }
  }

}

// JMSServerCheckThread
class JMSServerCheckThread
  extends Thread
{

  private String hostname;

  private int port;

  private String environmentName;

  private String applicationServerName;

  private String jmsServerName;

  private boolean completed = false;

  private boolean failure = false;

  private String errorMessage;

  private boolean uptodate;

  public JMSServerCheckThread( String hostname, int port, String environmentName, String applicationServerName,
                               String jmsServerName )
  {
    this.hostname = hostname;
    this.port = port;
    this.environmentName = environmentName;
    this.applicationServerName = applicationServerName;
    this.jmsServerName = jmsServerName;
  }

  public boolean getCompleted()
  {
    return this.completed;
  }

  public boolean getFailure()
  {
    return this.failure;
  }

  public String getErrorMessage()
  {
    return this.errorMessage;
  }

  public boolean getUptodate()
  {
    return this.uptodate;
  }

  public void run()
  {
    try
    {
      // call the webservice
      JMSServerClient webServiceClient = new JMSServerClient( hostname, port );
      uptodate = webServiceClient.check( environmentName, applicationServerName, jmsServerName );
      completed = true;
    }
    catch ( Exception e )
    {
      errorMessage = e.getMessage();
      completed = true;
      failure = true;
    }
  }

}

// JNDIBindingCheckThread
class JNDIBindingCheckThread
  extends Thread
{

  private String hostname;

  private int port;

  private String environmentName;

  private String applicationServerName;

  private String nameSpaceBindingName;

  private boolean completed = false;

  private boolean failure = false;

  private String errorMessage;

  private boolean uptodate;

  public JNDIBindingCheckThread( String hostname, int port, String environmentName, String applicationServerName,
                                 String nameSpaceBindingName )
  {
    this.hostname = hostname;
    this.port = port;
    this.environmentName = environmentName;
    this.applicationServerName = applicationServerName;
    this.nameSpaceBindingName = nameSpaceBindingName;
  }

  public boolean getCompleted()
  {
    return this.completed;
  }

  public boolean getFailure()
  {
    return this.failure;
  }

  public String getErrorMessage()
  {
    return this.errorMessage;
  }

  public boolean getUptodate()
  {
    return this.uptodate;
  }

  public void run()
  {
    try
    {
      // call the webservice
      JNDIBindingClient webServiceClient = new JNDIBindingClient( hostname, port );
      uptodate = webServiceClient.check( environmentName, applicationServerName, nameSpaceBindingName );
      completed = true;
    }
    catch ( Exception e )
    {
      errorMessage = e.getMessage();
      completed = true;
      failure = true;
    }
  }

}

// SharedLibraryCheckThread
class SharedLibraryCheckThread
  extends Thread
{

  private String hostname;

  private int port;

  private String environmentName;

  private String applicationServerName;

  private String sharedLibraryName;

  private boolean completed = false;

  private boolean failure = false;

  private String errorMessage;

  private boolean uptodate;

  public SharedLibraryCheckThread( String hostname, int port, String environmentName, String applicationServerName,
                                   String sharedLibraryName )
  {
    this.hostname = hostname;
    this.port = port;
    this.environmentName = environmentName;
    this.applicationServerName = applicationServerName;
    this.sharedLibraryName = sharedLibraryName;
  }

  public boolean getCompleted()
  {
    return this.completed;
  }

  public boolean getFailure()
  {
    return this.failure;
  }

  public String getErrorMessage()
  {
    return this.errorMessage;
  }

  public boolean getUptodate()
  {
    return this.uptodate;
  }

  public void run()
  {
    try
    {
      // call the webservice
      SharedLibraryClient webServiceClient = new SharedLibraryClient( hostname, port );
      uptodate = webServiceClient.check( environmentName, applicationServerName, sharedLibraryName );
      completed = true;
    }
    catch ( Exception e )
    {
      errorMessage = e.getMessage();
      completed = true;
      failure = true;
    }
  }

}

// J2EEApplicationArchiveCheckThread
class J2EEApplicationArchiveCheckThread
  extends Thread
{

  private String hostname;

  private int port;

  private String environmentName;

  private String applicationServerName;

  private String applicationName;

  private String archiveName;

  private boolean completed = false;

  private boolean failure = false;

  private String errorMessage;

  private boolean uptodate;

  public J2EEApplicationArchiveCheckThread( String hostname, int port, String environmentName,
                                            String applicationServerName, String applicationName, String archiveName )
  {
    this.hostname = hostname;
    this.port = port;
    this.environmentName = environmentName;
    this.applicationServerName = applicationServerName;
    this.applicationName = applicationName;
    this.archiveName = archiveName;
  }

  public boolean getCompleted()
  {
    return this.completed;
  }

  public boolean getFailure()
  {
    return this.failure;
  }

  public String getErrorMessage()
  {
    return this.errorMessage;
  }

  public boolean getUptodate()
  {
    return this.uptodate;
  }

  public void run()
  {
    try
    {
      // call the webservice
      ArchiveClient webServiceClient = new ArchiveClient( hostname, port );
      uptodate = webServiceClient.check( environmentName, applicationServerName, applicationName, archiveName );
      completed = true;
    }
    catch ( Exception e )
    {
      errorMessage = e.getMessage();
      completed = true;
      failure = true;
    }
  }
}

// J2EEApplicationConfigurationFileCheckThread
class J2EEApplicationConfigurationFileCheckThread
  extends Thread
{

  private String hostname;

  private int port;

  private String environmentName;

  private String applicationServerName;

  private String applicationName;

  private String configurationFileName;

  private boolean completed = false;

  private boolean failure = false;

  private String errorMessage;

  private boolean uptodate;

  public J2EEApplicationConfigurationFileCheckThread( String hostname, int port, String environmentName,
                                                      String applicationServerName, String applicationName,
                                                      String configurationFileName )
  {
    this.hostname = hostname;
    this.port = port;
    this.environmentName = environmentName;
    this.applicationServerName = applicationServerName;
    this.applicationName = applicationName;
    this.configurationFileName = configurationFileName;
  }

  public boolean getCompleted()
  {
    return this.completed;
  }

  public boolean getFailure()
  {
    return this.failure;
  }

  public String getErrorMessage()
  {
    return this.errorMessage;
  }

  public boolean getUptodate()
  {
    return this.uptodate;
  }

  public void run()
  {
    try
    {
      // call the webservice
      ConfigurationFileClient webServiceClient = new ConfigurationFileClient( hostname, port );
      uptodate =
        webServiceClient.check( environmentName, applicationServerName, applicationName, configurationFileName );
      completed = true;
    }
    catch ( Exception e )
    {
      errorMessage = e.getMessage();
      completed = true;
      failure = true;
    }
  }
}

/**
 * Environment checker pane.
 */
public class CheckerPane
  extends ContentPane
{

  private EnvironmentWindow parent;

  private Grid grid;

  // launch
  private ActionListener launch = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if no modifications are in progress
      if ( parent.isUpdated() )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning(
          Messages.getString( "environment.locked" ) );
        return;
      }
      // get the agent for the environment
      // load Kalumet configuration
      Kalumet kalumet;
      try
      {
        kalumet = ConfigurationManager.loadStore();
      }
      catch ( Exception e )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addError(
          Messages.getString( "db.read" ) + ": " + e.getMessage() );
        return;
      }
      // looking for the agent
      Agent agent = kalumet.getAgent( parent.getEnvironment().getAgent() );
      if ( agent == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "agent.notfound" ) );
        return;
      }
      // put message in the action log pane
      KalumetConsoleApplication.getApplication().getLogPane().addInfo( "Check in progress ..." );
      // clean the results grid
      grid.removeAll();
      // agent check
      Label agentCheck = new Label( "Kalumet agent check" );
      agentCheck.setStyleName( "default" );
      grid.add( agentCheck );
      final Label agentVersionLabel = new Label();
      agentVersionLabel.setStyleName( "default" );
      grid.add( agentVersionLabel );
      final Label agentButton = new Label();
      grid.add( agentButton );
      // launch the version thread
      final VersionThread versionThread = new VersionThread( agent.getHostname(), agent.getPort() );
      versionThread.start();
      // launch the synchronization task for the agent version
      KalumetConsoleApplication.getApplication().enqueueTask( KalumetConsoleApplication.getApplication().getTaskQueue(),
                                                              new Runnable()
                                                              {
                                                                public void run()
                                                                {
                                                                  if ( versionThread.getCompleted() )
                                                                  {
                                                                    if ( versionThread.getFailure() )
                                                                    {
                                                                      agentVersionLabel.setText( "Agent error: "
                                                                                                   + versionThread.getErrorMessage() );
                                                                      agentButton.setIcon( Styles.EXCLAMATION );
                                                                    }
                                                                    else
                                                                    {
                                                                      agentVersionLabel.setText(
                                                                        versionThread.getVersion() );
                                                                      agentButton.setIcon( Styles.ACCEPT );
                                                                    }
                                                                  }
                                                                  else
                                                                  {
                                                                    KalumetConsoleApplication.getApplication().enqueueTask(
                                                                      KalumetConsoleApplication.getApplication().getTaskQueue(),
                                                                      this );
                                                                  }
                                                                }
                                                              } );
      // application servers check
      for ( Iterator applicationServerIterator =
              parent.getEnvironment().getJ2EEApplicationServers().getJ2EEApplicationServers().iterator();
            applicationServerIterator.hasNext(); )
      {
        J2EEApplicationServer applicationServer = (J2EEApplicationServer) applicationServerIterator.next();
        Label applicationServerLabel = new Label( "JEE server " + applicationServer.getName() + " check" );
        applicationServerLabel.setStyleName( "default" );
        grid.add( applicationServerLabel );
        final Label applicationServerStatusLabel = new Label();
        applicationServerStatusLabel.setStyleName( "default" );
        grid.add( applicationServerStatusLabel );
        final Label applicationServerStatusButton = new Label();
        applicationServerStatusButton.setStyleName( "default" );
        grid.add( applicationServerStatusButton );
        // launch the application server status thread
        final ApplicationServerStatusThread applicationServerStatusThread =
          new ApplicationServerStatusThread( agent.getHostname(), agent.getPort(), parent.getEnvironmentName(),
                                             applicationServer.getName() );
        applicationServerStatusThread.start();
        // launch the synchronisation status thread
        KalumetConsoleApplication.getApplication().enqueueTask(
          KalumetConsoleApplication.getApplication().getTaskQueue(), new Runnable()
        {
          public void run()
          {
            if ( applicationServerStatusThread.getCompleted() )
            {
              if ( applicationServerStatusThread.getFailure() )
              {
                applicationServerStatusLabel.setText(
                  "J2EE application server error: " + applicationServerStatusThread.getErrorMessage() );
                applicationServerStatusButton.setIcon( Styles.EXCLAMATION );
              }
              else
              {
                applicationServerStatusLabel.setText( applicationServerStatusThread.getStatus() );
                applicationServerStatusButton.setIcon( Styles.ACCEPT );
              }
            }
            else
            {
              KalumetConsoleApplication.getApplication().enqueueTask(
                KalumetConsoleApplication.getApplication().getTaskQueue(), this );
            }
          }
        } );
        // check connection pool
        for ( Iterator connectionPoolIterator = applicationServer.getJDBCConnectionPools().iterator();
              connectionPoolIterator.hasNext(); )
        {
          JDBCConnectionPool connectionPool = (JDBCConnectionPool) connectionPoolIterator.next();
          Label connectionPoolLabel = new Label( " JDBC connection pool " + connectionPool.getName() + " check" );
          connectionPoolLabel.setStyleName( "default" );
          grid.add( connectionPoolLabel );
          final Label connectionPoolStatusLabel = new Label();
          connectionPoolStatusLabel.setStyleName( "default" );
          grid.add( connectionPoolStatusLabel );
          final Label connectionPoolButton = new Label();
          grid.add( connectionPoolButton );
          // launch the connection pool check thread
          final ConnectionPoolCheckThread connectionPoolCheckThread =
            new ConnectionPoolCheckThread( agent.getHostname(), agent.getPort(), parent.getEnvironmentName(),
                                           applicationServer.getName(), connectionPool.getName() );
          connectionPoolCheckThread.start();
          // launch the synchronisation thread
          KalumetConsoleApplication.getApplication().enqueueTask(
            KalumetConsoleApplication.getApplication().getTaskQueue(), new Runnable()
          {
            public void run()
            {
              if ( connectionPoolCheckThread.getCompleted() )
              {
                if ( connectionPoolCheckThread.getFailure() )
                {
                  connectionPoolStatusLabel.setText(
                    "JDBC connection pool check error: " + connectionPoolCheckThread.getErrorMessage() );
                  connectionPoolButton.setIcon( Styles.EXCLAMATION );
                }
                else
                {
                  if ( connectionPoolCheckThread.getUptodate() )
                  {
                    connectionPoolStatusLabel.setText( "OK" );
                    connectionPoolButton.setIcon( Styles.ACCEPT );
                  }
                  else
                  {
                    connectionPoolStatusLabel.setText( "JDBC Connection Pool is not deployed or not up to date" );
                    connectionPoolButton.setIcon( Styles.EXCLAMATION );
                  }
                }
              }
              else
              {
                KalumetConsoleApplication.getApplication().enqueueTask(
                  KalumetConsoleApplication.getApplication().getTaskQueue(), this );
              }
            }
          } );
        }
        // check datasources
        for ( Iterator dataSourceIterator = applicationServer.getJDBCDataSources().iterator();
              dataSourceIterator.hasNext(); )
        {
          JDBCDataSource dataSource = (JDBCDataSource) dataSourceIterator.next();
          Label dataSourceLabel = new Label( " JDBC data source " + dataSource.getName() + " check" );
          dataSourceLabel.setStyleName( "default" );
          grid.add( dataSourceLabel );
          final Label dataSourceStatusLabel = new Label();
          dataSourceStatusLabel.setStyleName( "Default" );
          grid.add( dataSourceStatusLabel );
          final Label dataSourceButton = new Label();
          grid.add( dataSourceButton );
          // launch the datasource check thread
          final DataSourceCheckThread dataSourceCheckThread =
            new DataSourceCheckThread( agent.getHostname(), agent.getPort(), parent.getEnvironmentName(),
                                       applicationServer.getName(), dataSource.getName() );
          dataSourceCheckThread.start();
          // launch the synchronisation thread
          KalumetConsoleApplication.getApplication().enqueueTask(
            KalumetConsoleApplication.getApplication().getTaskQueue(), new Runnable()
          {
            public void run()
            {
              if ( dataSourceCheckThread.getCompleted() )
              {
                if ( dataSourceCheckThread.getFailure() )
                {
                  dataSourceStatusLabel.setText(
                    "JDBC data source check error: " + dataSourceCheckThread.getErrorMessage() );
                  dataSourceButton.setIcon( Styles.EXCLAMATION );
                }
                else
                {
                  if ( dataSourceCheckThread.getUptodate() )
                  {
                    dataSourceStatusLabel.setText( "OK" );
                    dataSourceButton.setIcon( Styles.ACCEPT );
                  }
                  else
                  {
                    dataSourceStatusLabel.setText( "JDBC data source is not deployed or not up to date." );
                    dataSourceButton.setIcon( Styles.EXCLAMATION );
                  }
                }
              }
              else
              {
                KalumetConsoleApplication.getApplication().enqueueTask(
                  KalumetConsoleApplication.getApplication().getTaskQueue(), this );
              }
            }
          } );
        }
        // check JMS connection factories
        for ( Iterator jmsConnectionFactoryIterator = applicationServer.getJMSConnectionFactories().iterator();
              jmsConnectionFactoryIterator.hasNext(); )
        {
          JMSConnectionFactory jmsConnectionFactory = (JMSConnectionFactory) jmsConnectionFactoryIterator.next();
          Label jmsConnectionFactoryLabel =
            new Label( " JMS connection factory " + jmsConnectionFactory.getName() + " check" );
          jmsConnectionFactoryLabel.setStyleName( "Default" );
          grid.add( jmsConnectionFactoryLabel );
          final Label jmsConnectionFactoryStatusLabel = new Label();
          jmsConnectionFactoryStatusLabel.setStyleName( "Default" );
          grid.add( jmsConnectionFactoryStatusLabel );
          final Label jmsConnectionFactoryButton = new Label();
          grid.add( jmsConnectionFactoryButton );
          // launch the jms connection factory check thread
          final JMSConnectionFactoryCheckThread jmsConnectionFactoryCheckThread =
            new JMSConnectionFactoryCheckThread( agent.getHostname(), agent.getPort(), parent.getEnvironmentName(),
                                                 applicationServer.getName(), jmsConnectionFactory.getName() );
          jmsConnectionFactoryCheckThread.start();
          // launch the synchronisation thread
          KalumetConsoleApplication.getApplication().enqueueTask(
            KalumetConsoleApplication.getApplication().getTaskQueue(), new Runnable()
          {
            public void run()
            {
              if ( jmsConnectionFactoryCheckThread.getCompleted() )
              {
                if ( jmsConnectionFactoryCheckThread.getFailure() )
                {
                  jmsConnectionFactoryStatusLabel.setText(
                    "JMS connection factory check error: " + jmsConnectionFactoryCheckThread.getErrorMessage() );
                  jmsConnectionFactoryButton.setIcon( Styles.EXCLAMATION );
                }
                else
                {
                  if ( jmsConnectionFactoryCheckThread.getUptodate() )
                  {
                    jmsConnectionFactoryStatusLabel.setText( "OK" );
                    jmsConnectionFactoryButton.setIcon( Styles.ACCEPT );
                  }
                  else
                  {
                    jmsConnectionFactoryStatusLabel.setText(
                      "JMS connection factory is not deployed or not up to date" );
                    jmsConnectionFactoryButton.setIcon( Styles.EXCLAMATION );
                  }
                }
              }
              else
              {
                KalumetConsoleApplication.getApplication().enqueueTask(
                  KalumetConsoleApplication.getApplication().getTaskQueue(), this );
              }
            }
          } );
        }
        // check JMS servers
        for ( Iterator jmsServerIterator = applicationServer.getJMSServers().iterator(); jmsServerIterator.hasNext(); )
        {
          JMSServer jmsServer = (JMSServer) jmsServerIterator.next();
          Label jmsServerLabel = new Label( " JMS server " + jmsServer.getName() + " check" );
          jmsServerLabel.setStyleName( "Default" );
          grid.add( jmsServerLabel );
          final Label jmsServerStatusLabel = new Label();
          jmsServerStatusLabel.setStyleName( "Default" );
          grid.add( jmsServerStatusLabel );
          final Label jmsServerButton = new Label();
          grid.add( jmsServerButton );
          // launch the jms server check thread
          final JMSServerCheckThread jmsServerCheckThread =
            new JMSServerCheckThread( agent.getHostname(), agent.getPort(), parent.getEnvironmentName(),
                                      applicationServer.getName(), jmsServer.getName() );
          jmsServerCheckThread.start();
          // launch the synchronisation thread
          KalumetConsoleApplication.getApplication().enqueueTask(
            KalumetConsoleApplication.getApplication().getTaskQueue(), new Runnable()
          {
            public void run()
            {
              if ( jmsServerCheckThread.getCompleted() )
              {
                if ( jmsServerCheckThread.getFailure() )
                {
                  jmsServerStatusLabel.setText( "JMS server check error: " + jmsServerCheckThread.getErrorMessage() );
                  jmsServerButton.setIcon( Styles.EXCLAMATION );
                }
                else
                {
                  if ( jmsServerCheckThread.getUptodate() )
                  {
                    jmsServerStatusLabel.setText( "OK" );
                    jmsServerButton.setIcon( Styles.ACCEPT );
                  }
                  else
                  {
                    jmsServerStatusLabel.setText( "JMS server is not deployed or not up to date" );
                    jmsServerButton.setIcon( Styles.EXCLAMATION );
                  }
                }
              }
              else
              {
                KalumetConsoleApplication.getApplication().enqueueTask(
                  KalumetConsoleApplication.getApplication().getTaskQueue(), this );
              }
            }
          } );
        }
        // check JNDI bindings
        for ( Iterator jndiBindingIterator = applicationServer.getJNDIBindings().iterator();
              jndiBindingIterator.hasNext(); )
        {
          JNDIBinding jndiBinding = (JNDIBinding) jndiBindingIterator.next();
          Label nameSpaceBindingLabel = new Label( " JNDI binding " + jndiBinding.getName() + " check" );
          nameSpaceBindingLabel.setStyleName( "Default" );
          grid.add( nameSpaceBindingLabel );
          final Label nameSpaceBindingStatusLabel = new Label();
          nameSpaceBindingStatusLabel.setStyleName( "Default" );
          grid.add( nameSpaceBindingStatusLabel );
          final Label nameSpaceBindingButton = new Label();
          grid.add( nameSpaceBindingButton );
          // launch the name space binding check thread
          final JNDIBindingCheckThread JNDIBindingCheckThread =
            new JNDIBindingCheckThread( agent.getHostname(), agent.getPort(), parent.getEnvironmentName(),
                                        applicationServer.getName(), jndiBinding.getName() );
          JNDIBindingCheckThread.start();
          // launch the synchronisation thread
          KalumetConsoleApplication.getApplication().enqueueTask(
            KalumetConsoleApplication.getApplication().getTaskQueue(), new Runnable()
          {
            public void run()
            {
              if ( JNDIBindingCheckThread.getCompleted() )
              {
                if ( JNDIBindingCheckThread.getFailure() )
                {
                  nameSpaceBindingStatusLabel.setText(
                    "JNDI binding check error: " + JNDIBindingCheckThread.getErrorMessage() );
                  nameSpaceBindingButton.setIcon( Styles.EXCLAMATION );
                }
                else
                {
                  if ( JNDIBindingCheckThread.getUptodate() )
                  {
                    nameSpaceBindingStatusLabel.setText( "OK" );
                    nameSpaceBindingButton.setIcon( Styles.ACCEPT );
                  }
                  else
                  {
                    nameSpaceBindingStatusLabel.setText( "JNDI binding is not deployed or not up to date" );
                    nameSpaceBindingButton.setIcon( Styles.EXCLAMATION );
                  }
                }
              }
              else
              {
                KalumetConsoleApplication.getApplication().enqueueTask(
                  KalumetConsoleApplication.getApplication().getTaskQueue(), this );
              }
            }
          } );
        }
        // check shared libraries
        for ( Iterator sharedLibraryIterator = applicationServer.getSharedLibraries().iterator();
              sharedLibraryIterator.hasNext(); )
        {
          SharedLibrary sharedLibrary = (SharedLibrary) sharedLibraryIterator.next();
          Label sharedLibraryLabel = new Label( " Shared library " + sharedLibrary.getName() + " check" );
          sharedLibraryLabel.setStyleName( "Default" );
          grid.add( sharedLibraryLabel );
          final Label sharedLibraryStatusLabel = new Label();
          sharedLibraryStatusLabel.setStyleName( "Default" );
          grid.add( sharedLibraryLabel );
          final Label sharedLibraryButton = new Label();
          grid.add( sharedLibraryButton );
          // launch the shared library check thread
          final SharedLibraryCheckThread sharedLibraryCheckThread =
            new SharedLibraryCheckThread( agent.getHostname(), agent.getPort(), parent.getEnvironmentName(),
                                          applicationServer.getName(), sharedLibrary.getName() );
          sharedLibraryCheckThread.start();
          // launch the synchronisation thread
          KalumetConsoleApplication.getApplication().enqueueTask(
            KalumetConsoleApplication.getApplication().getTaskQueue(), new Runnable()
          {
            public void run()
            {
              if ( sharedLibraryCheckThread.getCompleted() )
              {
                if ( sharedLibraryCheckThread.getFailure() )
                {
                  sharedLibraryStatusLabel.setText(
                    "Shared library check error: " + sharedLibraryCheckThread.getErrorMessage() );
                  sharedLibraryButton.setIcon( Styles.EXCLAMATION );
                }
                else
                {
                  if ( sharedLibraryCheckThread.getUptodate() )
                  {
                    sharedLibraryStatusLabel.setText( "OK" );
                    sharedLibraryButton.setIcon( Styles.ACCEPT );
                  }
                  else
                  {
                    sharedLibraryStatusLabel.setText( "Shared library is not deployed or not up to date" );
                    sharedLibraryButton.setIcon( Styles.EXCLAMATION );
                  }
                }
              }
              else
              {
                KalumetConsoleApplication.getApplication().enqueueTask(
                  KalumetConsoleApplication.getApplication().getTaskQueue(), this );
              }
            }
          } );
        }
        // check J2EE applications
        for ( Iterator applicationIterator = applicationServer.getJ2EEApplications().iterator();
              applicationIterator.hasNext(); )
        {
          J2EEApplication application = (J2EEApplication) applicationIterator.next();
          Label applicationLabel = new Label( " J2EE application " + application.getName() + " check" );
          applicationLabel.setStyleName( "Default" );
          grid.add( applicationLabel );
          Label blankLabel = new Label( " " );
          grid.add( blankLabel );
          blankLabel = new Label( " " );
          grid.add( blankLabel );
          // check J2EE application archives
          for ( Iterator archiveIterator = application.getArchives().iterator(); archiveIterator.hasNext(); )
          {
            Archive archive = (Archive) archiveIterator.next();
            Label archiveLabel = new Label( " J2EE application archive " + archive.getName() + " check" );
            archiveLabel.setStyleName( "Default" );
            grid.add( archiveLabel );
            final Label archiveStatusLabel = new Label();
            archiveStatusLabel.setStyleName( "Default" );
            grid.add( archiveStatusLabel );
            final Label archiveButton = new Label();
            grid.add( archiveButton );
            // launch the application archive check thread
            final J2EEApplicationArchiveCheckThread j2EEApplicationArchiveCheckThread =
              new J2EEApplicationArchiveCheckThread( agent.getHostname(), agent.getPort(), parent.getEnvironmentName(),
                                                     applicationServer.getName(), application.getName(),
                                                     archive.getName() );
            j2EEApplicationArchiveCheckThread.start();
            // launch the synchronisation thread
            KalumetConsoleApplication.getApplication().enqueueTask(
              KalumetConsoleApplication.getApplication().getTaskQueue(), new Runnable()
            {
              public void run()
              {
                if ( j2EEApplicationArchiveCheckThread.getCompleted() )
                {
                  if ( j2EEApplicationArchiveCheckThread.getFailure() )
                  {
                    archiveStatusLabel.setText(
                      "J2EE application archive check error: " + j2EEApplicationArchiveCheckThread.getErrorMessage() );
                    archiveButton.setIcon( Styles.EXCLAMATION );
                  }
                  else
                  {
                    if ( j2EEApplicationArchiveCheckThread.getUptodate() )
                    {
                      archiveStatusLabel.setText( "OK" );
                      archiveButton.setIcon( Styles.ACCEPT );
                    }
                    else
                    {
                      archiveStatusLabel.setText( "J2EE application archive is not deployed or not up to date" );
                      archiveButton.setIcon( Styles.EXCLAMATION );
                    }
                  }
                }
                else
                {
                  KalumetConsoleApplication.getApplication().enqueueTask(
                    KalumetConsoleApplication.getApplication().getTaskQueue(), this );
                }
              }
            } );
          }
          // check J2EE application configuration files
          for ( Iterator configurationFileIterator = application.getConfigurationFiles().iterator();
                configurationFileIterator.hasNext(); )
          {
            ConfigurationFile configurationFile = (ConfigurationFile) configurationFileIterator.next();
            Label configurationFileLabel =
              new Label( " J2EE application configuration file " + configurationFile.getName() + " check" );
            configurationFileLabel.setStyleName( "Default" );
            grid.add( configurationFileLabel );
            final Label configurationFileStatusLabel = new Label();
            configurationFileStatusLabel.setStyleName( "Default" );
            grid.add( configurationFileStatusLabel );
            final Label configurationFileButton = new Label();
            grid.add( configurationFileButton );
            // launch the application configuration file check thread
            final J2EEApplicationConfigurationFileCheckThread j2EEApplicationConfigurationFileCheckThread =
              new J2EEApplicationConfigurationFileCheckThread( agent.getHostname(), agent.getPort(),
                                                               parent.getEnvironmentName(), applicationServer.getName(),
                                                               application.getName(), configurationFile.getName() );
            j2EEApplicationConfigurationFileCheckThread.start();
            // launch the synchronisation thread
            KalumetConsoleApplication.getApplication().enqueueTask(
              KalumetConsoleApplication.getApplication().getTaskQueue(), new Runnable()
            {
              public void run()
              {
                if ( j2EEApplicationConfigurationFileCheckThread.getCompleted() )
                {
                  if ( j2EEApplicationConfigurationFileCheckThread.getFailure() )
                  {
                    configurationFileStatusLabel.setText( "J2EE application configuration file check error: "
                                                            + j2EEApplicationConfigurationFileCheckThread.getErrorMessage() );
                    configurationFileButton.setIcon( Styles.EXCLAMATION );
                  }
                  else
                  {
                    if ( j2EEApplicationConfigurationFileCheckThread.getUptodate() )
                    {
                      configurationFileStatusLabel.setText( "OK" );
                      configurationFileButton.setIcon( Styles.ACCEPT );
                    }
                    else
                    {
                      configurationFileStatusLabel.setText( "J2EE application configuration file is not up to date" );
                      configurationFileButton.setIcon( Styles.EXCLAMATION );
                    }
                  }
                }
                else
                {
                  KalumetConsoleApplication.getApplication().enqueueTask(
                    KalumetConsoleApplication.getApplication().getTaskQueue(), this );
                }
              }
            } );
          }
        }
      }
    }
  };

  /**
   * Create a new <code>EnvironmentCheckerTabPane</code>
   *
   * @param parent the parent <code>EnvironmentWindow</code>
   */
  public CheckerPane( EnvironmentWindow parent )
  {
    super();
    setStyleName( "tab.content" );

    // update parent
    this.parent = parent;

    // column layout
    Column content = new Column();
    content.setCellSpacing( new Extent( 2 ) );
    content.setInsets( new Insets( 2 ) );
    add( content );

    // add the launch button
    Button launchButton = new Button( Messages.getString( "status" ), Styles.INFORMATION );
    launchButton.addActionListener( launch );
    content.add( launchButton );

    // add results grid
    grid = new Grid( 3 );
    grid.setStyleName( "border.grid" );
    content.add( grid );

    // update the pane
    update();
  }

  /**
   * Update the pane
   */
  public void update()
  {
    // nothing to do
  }

}