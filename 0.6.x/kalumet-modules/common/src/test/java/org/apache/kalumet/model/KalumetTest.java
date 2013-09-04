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

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests on the Kalumet model.
 */
public class KalumetTest
{

  private final static transient Logger LOGGER = LoggerFactory.getLogger( KalumetTest.class );

  private Kalumet kalumetModel;

  @Before
  public void setUp()
    throws Exception
  {
    kalumetModel = Kalumet.digeste( "file:./src/test/resources/kalumet.xml" );
  }

  @Test
  public void testPropertiesUnmarshalling()
  {
    LOGGER.info( "Get Kalumet test property" );
    Property testProperty = kalumetModel.getProperty( "test" );
    assertEquals( "test", testProperty.getValue() );
  }

  @Test
  public void testUserUnmarshalling()
  {
    LOGGER.info( "Get Kalumet test user" );
    User user = kalumetModel.getSecurity().getUser( "test" );
    assertEquals( "test", user.getId() );
    assertEquals( "Test Test", user.getName() );
    assertEquals( "test@example.com", user.getEmail() );
  }

  @Test
  public void testGroupUnmarshalling()
  {
    LOGGER.info( "Get Kalumet test group" );
    Group group = kalumetModel.getSecurity().getGroup( "test" );
    assertEquals( "Test Group", group.getName() );
    assertEquals( "test", group.getUser( "test" ).getId() );
  }

  @Test
  public void testAgentUnmarshalling()
  {
    LOGGER.info( "Get Kalumet test agent" );
    Agent agent = kalumetModel.getAgent( "test" );
    assertEquals( "localhost", agent.getHostname() );
    assertEquals( 5000, agent.getPort() );
    assertEquals( "0 * * * * *", agent.getCron() );
    assertEquals( 5, agent.getMaxmanagedenvironments() );
    assertEquals( 3, agent.getMaxjeeapplicationserversstarted() );
  }

  @Test
  public void testEnvironmentUnmarshalling()
  {
    LOGGER.info( "Get Kalumet test_auto environment" );
    Environment environment = kalumetModel.getEnvironment( "test_auto" );
    assertEquals( "test_auto", environment.getName() );
    assertEquals( "TEST", environment.getGroup() );
    assertEquals( "test", environment.getTag() );
    assertEquals( false, environment.isAutoupdate() );
    assertEquals( "test", environment.getAgent() );
    assertEquals( "test", environment.getLock() );
    assertEquals( "ftp://remote/release/${RELEASE_VERSION}", environment.getReleaseLocation() );
    assertEquals( "Notes test.", environment.getNotes() );
    assertEquals( "Weblinks test.", environment.getWeblinks() );
  }

  @Test
  public void testVariableUnmarshalling()
  {
    LOGGER.info( "Get Kalumet test_auto environment RELEASE_VERSION variable" );
    Variable variable = kalumetModel.getEnvironment( "test_auto" ).getVariable( "RELEASE_VERSION" );
    assertEquals( "test", variable.getValue() );
  }

  @Test
  public void testFreeFieldUnmarshalling()
  {
    LOGGER.info( "Get Kalumet test_auto environment FREE freefield" );
    FreeField freeField = kalumetModel.getEnvironment( "test_auto" ).getFreeField( "FREE" );
    assertEquals( "field", freeField.getContent() );
  }

  @Test
  public void testAccessUnmarshalling()
  {
    LOGGER.info( "Get Kalumet test_auto environment access for the test group" );
    Access access = kalumetModel.getEnvironment( "test_auto" ).getAccess( "test" );
    assertEquals( "false", access.getProperty( "admin" ).getValue() );
    assertEquals( "true", access.getProperty( "update" ).getValue() );
    assertEquals( "true", access.getProperty( "jee_application_servers_change" ).getValue() );
    assertEquals( "true", access.getProperty( "jee_application_servers_update" ).getValue() );
    assertEquals( "true", access.getProperty( "jee_application_servers_control" ).getValue() );
    assertEquals( "true", access.getProperty( "jee_resources_change" ).getValue() );
    assertEquals( "true", access.getProperty( "jee_resources_update" ).getValue() );
    assertEquals( "true", access.getProperty( "jee_applications_change" ).getValue() );
    assertEquals( "true", access.getProperty( "jee_applications_update" ).getValue() );
    assertEquals( "true", access.getProperty( "softwares_change" ).getValue() );
    assertEquals( "true", access.getProperty( "softwares_update" ).getValue() );
    assertEquals( "true", access.getProperty( "release" ).getValue() );
    assertEquals( "true", access.getProperty( "shell" ).getValue() );
    assertEquals( "true", access.getProperty( "browser" ).getValue() );
    assertEquals( "true", access.getProperty( "homepage" ).getValue() );
  }

  @Test
  public void testLogFileUnmarshalling()
  {
    LOGGER.info( "Get Kalumet test_auto environment test log file" );
    LogFile logFile = kalumetModel.getEnvironment( "test_auto" ).getLogFile( "test" );
    assertEquals( "test", logFile.getName() );
    assertEquals( "/tmp", logFile.getPath() );
    assertEquals( "test", logFile.getAgent() );
  }

  @Test
  public void testJEEApplicationServersUnmarshalling()
  {
    LOGGER.info( "Get Kalumet test_auto environment JEE application servers" );
    JEEApplicationServers applicationServers = kalumetModel.getEnvironment( "test_auto" ).getJEEApplicationServers();
    assertEquals( false, applicationServers.isCluster() );
  }

  @Test
  public void testJEEApplicationServerUnmarshalling()
  {
    LOGGER.info( "Get Kalumet test_auto environment as_test JEE application server" );
    JEEApplicationServer applicationServer =
      kalumetModel.getEnvironment( "test_auto" ).getJEEApplicationServers().getJEEApplicationServer("as_test");
    assertEquals( "org.apache.kalumet.jmx.plugins.DummyPlugin", applicationServer.getClassname() );
    assertEquals( "dummy://localhost:1099", applicationServer.getJmxurl() );
    assertEquals( "admin_user", applicationServer.getAdminuser() );
    assertEquals( "admin_password", applicationServer.getAdminpassword() );
    assertEquals( true, applicationServer.isUpdateRequireRestart() );
    assertEquals( true, applicationServer.isUpdateRequireCacheCleaning() );
    assertEquals( false, applicationServer.isUsejmxstop() );
    assertEquals( false, applicationServer.isDeletecomponents() );
    assertEquals( "test", applicationServer.getAgent() );
    assertEquals( "echo \"Startup\"", applicationServer.getStartupcommand() );
    assertEquals( "echo \"Shutdown\"", applicationServer.getShutdowncommand() );
  }

  @Test
  public void testJDBCConnectionPoolUnmarshalling()
  {
    LOGGER.info( "Get Kalumet test_auto environment, as_test JEE application server, JDBC connection pool test" );
    JDBCConnectionPool connectionPool =
      kalumetModel.getEnvironment( "test_auto" ).getJEEApplicationServers().getJEEApplicationServer(
              "as_test").getJDBCConnectionPool( "test" );
    assertEquals( "test", connectionPool.getName() );
  }

  @Test
  public void testJDBCDataSourceUnmarshalling()
  {
    LOGGER.info( "Get Kalumet test_auto environment, as_test JEE application server, JDBC data source test" );
    JDBCDataSource dataSource =
      kalumetModel.getEnvironment( "test_auto" ).getJEEApplicationServers().getJEEApplicationServer(
              "as_test").getJDBCDataSource( "test" );
    assertEquals( "test", dataSource.getName() );
  }

  @Test
  public void testSoftwareUnmarshalling()
  {
    LOGGER.info( "Get Kalumet test_auto environment test software" );
    Software software = kalumetModel.getEnvironment( "test_auto" ).getSoftware( "test" );
    assertEquals( "test", software.getName() );
    assertEquals( "http://www.example.com/software", software.getUri() );
    assertEquals( "test", software.getAgent() );
    assertEquals( true, software.isActive() );
    assertEquals( false, software.isBlocker() );
    assertEquals( false, software.isBeforejee() );
  }

  @Test
  public void testSoftwareUpdatePlanCommandUnmarshalling()
  {
    LOGGER.info( "Get Kalumet test_auto environment test software, command_test command" );
    Command command = kalumetModel.getEnvironment( "test_auto" ).getSoftware( "test" ).getCommand( "command_test" );
    assertEquals( "command_test", command.getName() );
    assertEquals( true, command.isActive() );
    assertEquals( false, command.isBlocker() );
    assertEquals( "ls /tmp", command.getCommand() );
    assertEquals( "test", command.getAgent() );
  }

  @Test
  public void testSoftwareUpdatePlanLocationUnmarshalling()
  {
    LOGGER.info( "Get Kalumet test_auto environment test software, location_test location" );
    Location location = kalumetModel.getEnvironment( "test_auto" ).getSoftware( "test" ).getLocation( "location_test" );
    assertEquals( "location_test", location.getName() );
    assertEquals( true, location.isActive() );
    assertEquals( false, location.isBlocker() );
    assertEquals( "http://www.example.com/location", location.getUri() );
    assertEquals( "/tmp/location", location.getPath() );
    assertEquals( "test", location.getAgent() );
  }

  @Test
  public void testSoftwareUpdatePlanConfigurationFileUnmarshalling()
  {
    LOGGER.info( "Get Kalumet test_auto environment test software, configurationfile_test configuration file" );
    ConfigurationFile configurationFile =
      kalumetModel.getEnvironment( "test_auto" ).getSoftware( "test" ).getConfigurationFile( "configurationfile_test" );
    assertEquals( "configurationfile_test", configurationFile.getName() );
    assertEquals( "http://www.example.com/configurationfile", configurationFile.getUri() );
    assertEquals( "/tmp/configurationfile", configurationFile.getPath() );
    assertEquals( true, configurationFile.isActive() );
    assertEquals( false, configurationFile.isBlocker() );
    assertEquals( "test", configurationFile.getAgent() );
  }

  @Test
  public void testSoftwareUpdatePlanDatabaseUnmarshaling()
  {
    LOGGER.info( "Get Kalumet environment test_auto test software db_test database update plan item." );
    Database database = kalumetModel.getEnvironment( "test_auto" ).getSoftware( "test" ).getDatabase( "db_test" );
    assertEquals( "db_test", database.getName() );
    assertEquals( true, database.isActive() );
    assertEquals( false, database.isBlocker() );
    assertEquals( "com.example.test.Driver", database.getDriver() );
    assertEquals( "user_test", database.getUser() );
    assertEquals( "password_test", database.getPassword() );
    assertEquals( "jdbc://example.com:3306/test", database.getJdbcurl() );
    assertEquals( "test", database.getAgent() );
    assertEquals( "", database.getSqlCommand() );
    assertEquals( "", database.getConnectionPool() );
  }

  @Test
  public void testSoftwareUpdatePlanDatabaseSqlScriptUnmarshaling()
  {
    LOGGER.info(
      "Get Kalumet environment test_auto test software db_test database update plan sqlscript_test SQL script." );
    SqlScript sqlScript =
      kalumetModel.getEnvironment( "test_auto" ).getSoftware( "test" ).getDatabase( "db_test" ).getSqlScript(
        "sqlscript_test" );
    assertEquals( "sqlscript_test", sqlScript.getName() );
    assertEquals( true, sqlScript.isActive() );
    assertEquals( false, sqlScript.isBlocker() );
    assertEquals( true, sqlScript.isForce() );
    assertEquals( "http://www.example.com/sqlscript", sqlScript.getUri() );
  }

  @Test
  public void testKalumetMarshalling()
    throws Exception
  {
    Kalumet kalumet = new Kalumet();
    Property testProperty = new Property();
    testProperty.setName( "test" );
    testProperty.setValue( "test" );
    kalumet.getProperties().add( testProperty );
    Environment environment = new Environment();
    environment.setName( "test" );
    environment.setNotes( "Only for test" );
    environment.setWeblinks( "Only for test" );
    Software software = new Software();
    software.setName( "test" );
    software.setUri( "http://www.example.com/test?test=test&other=other" );
    environment.addSoftware( software );
    kalumet.addEnvironment( environment );
    kalumet.writeXMLFile( "file:./target/kalumet.xml" );
  }

  @Test
  public void testKalumetUnmarshalling()
    throws Exception
  {
    Kalumet kalumet = Kalumet.digeste( "file:./target/kalumet.xml" );
    Software software = kalumet.getEnvironment( "test" ).getSoftware( "test" );
    assertEquals( "http://www.example.com/test?test=test&other=other", software.getUri() );
  }

}
