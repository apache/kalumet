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
package org.apache.kalumet.controller.core;

import java.util.List;

/**
 * J2EE application server controller interface.
 */
public interface J2EEApplicationServerController
{

  /**
   * Shutdown a J2EE application server.
   *
   * @throws ControllerException in case of shutdown failure.
   */
  public void shutdown()
    throws ControllerException;

  /**
   * Get the current status of a J2EE application server.
   *
   * @return the current human readable status.
   * @throws ControllerException in case of status check failure.
   */
  public String status()
    throws ControllerException;

  /**
   * Check if an J2EE application server is stopped or not.
   *
   * @return true if the J2EE application server is stopped, false if it's running.
   * @throws ControllerException in case of status check failure.
   */
  public boolean isStopped()
    throws ControllerException;

  /**
   * Check if a J2EE application is deployed.
   *
   * @param path the application local path.
   * @param name the application name.
   * @return true if the J2EE application is deployed, false else.
   * @throws ControllerException in case of status check failure.
   */
  public boolean isJ2EEApplicationDeployed( String path, String name )
    throws ControllerException;

  /**
   * Deploy a J2EE application.
   *
   * @param path              the J2EE application local path.
   * @param name              the J2EE application name.
   * @param classLoaderOrder  the J2EE application class loader order (PARENT_FIRST/PARENT_LAST).
   * @param classLoaderPolicy the J2EE application class loader policy (single/multiple).
   * @param virtualHost       the J2EE application virtual host (if applicable).
   * @throws ControllerException in case of deployment failure.
   */
  public void deployJ2EEApplication( String path, String name, String classLoaderOrder, String classLoaderPolicy,
                                     String virtualHost )
    throws ControllerException;

  /**
   * Undeploy a J2EE application.
   *
   * @param path the J2EE application local path.
   * @param name the J2EE application name.
   * @throws ControllerException in case of undeployment failure.
   */
  public void undeployJ2EEApplication( String path, String name )
    throws ControllerException;

  /**
   * Redeploy a J2EE application.
   *
   * @param path the J2EE application local path.
   * @param name the J2EE application name.
   * @throws ControllerException in case of redeployment failure.
   */
  public void redeployJ2EEApplication( String path, String name )
    throws ControllerException;

  /**
   * Check a JDBC connection pool is deployed.
   *
   * @param name the name of the JDBC connection pool.
   * @return true if the JDBC connection pool is deployed, false else.
   * @throws ControllerException in case of status check failure.
   */
  public boolean isJDBCConnectionPoolDeployed( String name )
    throws ControllerException;

  /**
   * Check if a JDBC connection pool attributes are up to date.
   *
   * @param name                the name of the JDBC connection pool.
   * @param jdbcDriverClassName the JDBC driver class name of the connection pool.
   * @param capacityIncrement   the capacity increment of the JDBC connection pool.
   * @param initialCapacity     the initial capacity of the JDBC connection pool.
   * @param maxCapacity         the max capacity of the JDBC connection pool.
   * @param username            the database username.
   * @param password            the database password.
   * @param jdbcURL             the JDBC URL
   * @param classPath           the class path where to look for the driver
   * @return true if all attributes are up to date, false else.
   * @throws ControllerException in case of status check failure.
   */
  public boolean isJDBCConnectionPoolUpToDate( String name, String jdbcDriverClassName, int capacityIncrement,
                                               int initialCapacity, int maxCapacity, String username, String password,
                                               String jdbcURL, String classPath )
    throws ControllerException;

  /**
   * Update a JDBC connection pool.
   *
   * @param name                the name of the JDBC connection pool.
   * @param jdbcDriverClassName the JDBC driver of the connection pool.
   * @param capacityIncrement   the capacity increment of the JDBC connection pool.
   * @param initialCapacity     the initial capacity of the JDBC connection pool.
   * @param maxCapacity         the max capacity of the JDBC connection pool.
   * @param username            the database username.
   * @param password            the database password.
   * @param jdbcURL             the JDBC URL of the database.
   * @param classPath           the class path of JDBC driver.
   * @return true if the JDBC connection pool has been updated, false else.
   * @throws ControllerException in case of update failure.
   */
  public boolean updateJDBCConnectionPool( String name, String jdbcDriverClassName, int capacityIncrement,
                                           int initialCapacity, int maxCapacity, String username, String password,
                                           String jdbcURL, String classPath )
    throws ControllerException;

  /**
   * Deploy a JDBC connection pool.
   *
   * @param name                the name of the JDBC connection pool.
   * @param jdbcDriverClassName the JDBC driver of the connection pool.
   * @param capacityIncrement   the capacity increment of the JDBC connection pool.
   * @param initialCapacity     the initial capacity of the JDBC connection pool.
   * @param maxCapacity         the max capacity of the JDBC connection pool.
   * @param username            the database username.
   * @param password            the database password.
   * @param jdbcURL             the database JDBC URL.
   * @param classPath           the class path of the JDBC driver.
   * @throws ControllerException in case of deployment failure.
   */
  public void deployJDBCConnectionPool( String name, String jdbcDriverClassName, int capacityIncrement,
                                        int initialCapacity, int maxCapacity, String username, String password,
                                        String jdbcURL, String classPath )
    throws ControllerException;

  /**
   * Undeploy a JDBC connection pool.
   *
   * @param name the name of the JDBC connection pool.
   * @throws ControllerException in case of an undeployment failure.
   */
  public void undeployJDBCConnectionPool( String name )
    throws ControllerException;

  /**
   * Check if a JDBC data source is deployed.
   *
   * @param name the name of the JDBC data source.
   * @return true if the JDBC data source is deployed, false else.
   * @throws ControllerException in case of status check failure.
   */
  public boolean isJDBCDataSourceDeployed( String name )
    throws ControllerException;

  /**
   * Check if a JDBC data source is up to date.
   *
   * @param name               the name of the JDBC data source.
   * @param jdbcConnectionPool the name of the JDBC connection pool used by the data source.
   * @param jdbcURL            the JDBC URL of the data source.
   * @param helpClassName      the helper class name of the JDBC data source.
   * @return true if the JDBC data source is up to date, false else.
   * @throws ControllerException in case of status check failure.
   */
  public boolean isJDBCDataSourceUpToDate( String name, String jdbcConnectionPool, String jdbcURL,
                                           String helpClassName )
    throws ControllerException;

  /**
   * Deploy a JDBC data source.
   *
   * @param name               the name of the JDBC data source.
   * @param jdbcConnectionPool the name of the JDBC connection pool used by the data source.
   * @param jdbcURL            the JDBC URL of the data source.
   * @param helpClassName      the helper class name of the JDBC data source.
   * @throws ControllerException in case of deployment failure.
   */
  public void deployJDBCDataSource( String name, String jdbcConnectionPool, String jdbcURL, String helpClassName )
    throws ControllerException;

  /**
   * Undeploy a JDBC data source.
   *
   * @param name the name of the JDBC data source.
   * @throws ControllerException in case of undeployment failure.
   */
  public void undeployJDBCDataSource( String name )
    throws ControllerException;

  /**
   * Update a JDBC data source.
   *
   * @param name               the name of the JDBC data source.
   * @param jdbcConnectionPool the name of the JDBC connection pool used by the data source.
   * @param jdbcURL            the JDBC URL of the data source.
   * @param helperClassName    the helper class name of the data source.
   * @return true if the JDBC data source has been updated, false else.
   * @throws ControllerException in case of update failure.
   */
  public boolean updateJDBCDataSource( String name, String jdbcConnectionPool, String jdbcURL, String helperClassName )
    throws ControllerException;

  /**
   * Check if a JMS connection factory is deployed.
   *
   * @param name the name of the JMS connection factory.
   * @return true if the JMS connection factory is deployed, false else.
   * @throws ControllerException in case of status check failure.
   */
  public boolean isJMSConnectionFactoryDeployed( String name )
    throws ControllerException;

  /**
   * Deploy a JMS connection factory.
   *
   * @param name the name of the JMS connection factory.
   * @throws ControllerException in case of deployment failure.
   */
  public void deployJMSConnectionFactory( String name )
    throws ControllerException;

  /**
   * Undeploy a JMS connection factory.
   *
   * @param name the name of the JMS connection factory.
   * @throws ControllerException in case of undeployment failure.
   */
  public void undeployJMSConnectionFactory( String name )
    throws ControllerException;

  /**
   * Check if a JMS server is deployed.
   *
   * @param name the name of the JMS server.
   * @return true if the JMS server is deployed, false else.
   * @throws ControllerException in case of status check failure.
   */
  public boolean isJMSServerDeployed( String name )
    throws ControllerException;

  /**
   * Check if a JMS server is up to date.
   *
   * @param name   the name of the JMS server.
   * @param queues the queues deployed in the JMS server.
   * @param topics the topics deployed in the JMS server.
   * @return true if the JMS server is up to date, false else.
   * @throws ControllerException in case of status check failure.
   */
  public boolean isJMSServerUpToDate( String name, List queues, List topics )
    throws ControllerException;

  /**
   * Deploy a JMS server.
   *
   * @param name   the name of the JMS server.
   * @param queues the queues to deploy in the JMS server.
   * @param topics the topics to deploy in the JMS server.
   * @throws ControllerException in case of deployment failure.
   */
  public void deployJMSServer( String name, List queues, List topics )
    throws ControllerException;

  /**
   * Update a JMS server.
   *
   * @param name   the name of the JMS server.
   * @param queues the queues in the JMS server.
   * @param topics the topics in the JMS server.
   * @return true if the JMS server has been updated, false else.
   * @throws ControllerException in case of update failure.
   */
  public boolean updateJMSServer( String name, List queues, List topics )
    throws ControllerException;

  /**
   * Undeploy a JMS server.
   *
   * @param name the name of the JMS server.
   * @throws ControllerException in case of undeployment failure.
   */
  public void undeployJMSServer( String name )
    throws ControllerException;

  /**
   * Check if a JNDI binding is deployed.
   *
   * @param name the name of the JNDI binding
   * @return true if the JNDI binding is deployed, false else.
   * @throws ControllerException in case of status check failure.
   */
  public boolean isJNDIBindingDeployed( String name )
    throws ControllerException;

  /**
   * Deploy a JNDI binding.
   *
   * @param name        the name of the JNDI binding.
   * @param jndiName    the name of the JNDI resources.
   * @param jndiAlias   the alias name to the JNDI resources.
   * @param providerUrl the URL provider of the JNDI binding.
   * @throws ControllerException in case of deployment failure.
   */
  public void deployJNDIBinding( String name, String jndiName, String jndiAlias, String providerUrl )
    throws ControllerException;

  /**
   * Undeploy a JNDI binding.
   *
   * @param name the name of the JNDI binding.
   * @throws ControllerException in case of undeployment failure.
   */
  public void undeployJNDIBinding( String name )
    throws ControllerException;

  /**
   * Check if a JNDI binding is up to date.
   *
   * @param name        the name of the JNDI binding.
   * @param jndiName    the name of the JNDI resources.
   * @param jndiAlias   the alias name to the JNDI resources.
   * @param providerUrl the URL provider of the JNDI binding.
   * @return true if the JNDI binding is up to date, false else.
   * @throws ControllerException in case of status check failure.
   */
  public boolean isJNDIBindingUpToDate( String name, String jndiName, String jndiAlias, String providerUrl )
    throws ControllerException;

  /**
   * Update a JNDI binding.
   *
   * @param name        the name of the JNDI binding.
   * @param jndiName    the name of the JNDI resources.
   * @param jndiAlias   the alias name to the JNDI resources.
   * @param providerUrl the URL provider of the JNDI binding.
   * @return true if the JNDI binding has been updated, false else.
   * @throws ControllerException in case of update failure.
   */
  public boolean updateJNDIBinding( String name, String jndiName, String jndiAlias, String providerUrl )
    throws ControllerException;

  /**
   * Check if a shared library is deployed.
   *
   * @param name the name of the shared library.
   * @return true if the shared library is deployed, false else.
   * @throws ControllerException
   */
  public boolean isSharedLibraryDeployed( String name )
    throws ControllerException;

  /**
   * Deploy a shared library.
   *
   * @param name      the name of the shared library.
   * @param classpath the class path of the shared library.
   * @throws ControllerException in case of deployment failure.
   */
  public void deploySharedLibrary( String name, String classpath )
    throws ControllerException;

  /**
   * Undeploy a shared library.
   *
   * @param name the name of the shared library.
   * @throws ControllerException in case of undeployment failure.
   */
  public void undeploySharedLibrary( String name )
    throws ControllerException;

  /**
   * Check if a shared library is up to date.
   *
   * @param name      the name of the shared library.
   * @param classpath the class path of the shared library.
   * @return true if the shared library is up to date, false else.
   * @throws ControllerException in case of status check failure.
   */
  public boolean isSharedLibraryUpToDate( String name, String classpath )
    throws ControllerException;

  /**
   * Update a shared library.
   *
   * @param name      the name of the shared library.
   * @param classpath the class path of the shared library.
   * @return true if the shared library has been updated, false else.
   * @throws ControllerException in case of update failure.
   */
  public boolean updateSharedLibrary( String name, String classpath )
    throws ControllerException;

}
