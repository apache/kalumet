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
 * Abstract JEE application server controller.
 */
public abstract class AbstractJEEApplicationServerController
    implements JEEApplicationServerController
{

    private String url;

    private String username;

    private String password;

    private String serverName;

    private boolean cluster;

    /**
     * Default constructor.
     *
     * @param url        JMX URL of the JEE application server.
     * @param username   the administrative user.
     * @param password   the administrative password.
     * @param serverName the JEE application server name.
     * @param cluster    true means that the server is a cluster, or single.
     * @throws ControllerException in case of connection failure.
     */
    public AbstractJEEApplicationServerController( String url, String username, String password, String serverName,
                                                   Boolean cluster )
        throws ControllerException
    {
        this.url = url;
        this.username = username;
        this.password = password;
        this.serverName = serverName;
        this.cluster = cluster.booleanValue();
        this.init();
    }

    /**
     * Abstract method to initialize a specific JEE application server.
     *
     * @throws ControllerException in case of initialization error.
     */
    protected abstract void init()
        throws ControllerException;

    public String getUrl()
    {
        return this.url;
    }

    public void setUrl( String url )
    {
        this.url = url;
    }

    public String getUsername()
    {
        return this.username;
    }

    public void setUsername( String username )
    {
        this.username = username;
    }

    public String getPassword()
    {
        return this.password;
    }

    public void setPassword( String password )
    {
        this.password = password;
    }

    public String getServerName()
    {
        return this.serverName;
    }

    public void setServerName( String serverName )
    {
        this.serverName = serverName;
    }

    public boolean isCluster()
    {
        return this.cluster;
    }

    public void setCluster( boolean cluster )
    {
        this.cluster = cluster;
    }

    public abstract void shutdown()
        throws ControllerException;

    public abstract String status()
        throws ControllerException;

    public abstract boolean isStopped()
        throws ControllerException;

    public abstract boolean isJEEApplicationDeployed( String path, String name )
        throws ControllerException;

    public abstract void deployJEEApplication( String path, String name, String classLoaderOrder,
                                               String classLoaderPolicy, String virtualHost )
        throws ControllerException;

    public abstract void undeployJEEApplication( String path, String name )
        throws ControllerException;

    public abstract void redeployJEEApplication( String path, String name )
        throws ControllerException;

    public abstract boolean isJDBCConnectionPoolDeployed( String name )
        throws ControllerException;

    public abstract boolean isJDBCConnectionPoolUpToDate( String name, String jdbcDriverClassName,
                                                          int capacityIncrement, int initialCapacity, int maxCapacity,
                                                          String username, String password, String jdbcURL,
                                                          String classPath )
        throws ControllerException;

    public abstract boolean updateJDBCConnectionPool( String name, String jdbcDriverClassName, int capacityIncrement,
                                                      int initialCapacity, int maxCapacity, String username,
                                                      String password, String jdbcURL, String classPath )
        throws ControllerException;

    public abstract void deployJDBCConnectionPool( String name, String jdbcDriverClassName, int capacityIncrement,
                                                   int initialCapacity, int maxCapacity, String username,
                                                   String password, String jdbcURL, String classPath )
        throws ControllerException;

    public abstract void undeployJDBCConnectionPool( String name )
        throws ControllerException;

    public abstract boolean isJDBCDataSourceDeployed( String name )
        throws ControllerException;

    public abstract boolean isJDBCDataSourceUpToDate( String name, String jdbcConnectionPool, String jdbcURL,
                                                      String helpClassName )
        throws ControllerException;

    public abstract void deployJDBCDataSource( String name, String jdbcConnectionPool, String jdbcURL,
                                               String helpClassName )
        throws ControllerException;

    public abstract void undeployJDBCDataSource( String name )
        throws ControllerException;

    public abstract boolean updateJDBCDataSource( String name, String jdbcConnectionPool, String jdbcURL,
                                                  String helperClassName )
        throws ControllerException;

    public abstract boolean isJMSConnectionFactoryDeployed( String name )
        throws ControllerException;

    public abstract void deployJMSConnectionFactory( String name )
        throws ControllerException;

    public abstract void undeployJMSConnectionFactory( String name )
        throws ControllerException;

    public abstract boolean isJMSServerDeployed( String name )
        throws ControllerException;

    public abstract boolean isJMSServerUpToDate( String name, List queues, List topics )
        throws ControllerException;

    public abstract void deployJMSServer( String name, List queues, List topics )
        throws ControllerException;

    public abstract boolean updateJMSServer( String name, List queues, List topics )
        throws ControllerException;

    public abstract void undeployJMSServer( String name )
        throws ControllerException;

    public abstract boolean isJNDIBindingDeployed( String name )
        throws ControllerException;

    public abstract void deployJNDIBinding( String name, String jndiName, String jndiAlias, String providerUrl )
        throws ControllerException;

    public abstract void undeployJNDIBinding( String name )
        throws ControllerException;

    public abstract boolean isJNDIBindingUpToDate( String name, String jndiName, String jndiAlias, String providerUrl )
        throws ControllerException;

    public abstract boolean updateJNDIBinding( String name, String jndiName, String jndiAlias, String providerUrl )
        throws ControllerException;

    public abstract boolean isSharedLibraryDeployed( String name )
        throws ControllerException;

    public abstract void deploySharedLibrary( String name, String classpath )
        throws ControllerException;

    public abstract void undeploySharedLibrary( String name )
        throws ControllerException;

    public abstract boolean isSharedLibraryUpToDate( String name, String classpath )
        throws ControllerException;

    public abstract boolean updateSharedLibrary( String name, String classpath )
        throws ControllerException;

}
