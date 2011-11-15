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
package org.apache.kalumet.controller.weblogic;

import org.apache.kalumet.controller.core.AbstractJ2EEApplicationServerController;
import org.apache.kalumet.controller.core.ControllerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weblogic.jndi.Environment;
import weblogic.management.MBeanHome;
import weblogic.management.configuration.*;
import weblogic.management.deploy.DeployerRuntime;
import weblogic.management.deploy.DeploymentData;
import weblogic.management.runtime.ClusterRuntimeMBean;
import weblogic.management.runtime.DeployerRuntimeMBean;
import weblogic.management.runtime.DeploymentTaskRuntimeMBean;
import weblogic.management.runtime.ServerRuntimeMBean;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.naming.Context;

/**
 * WeblogicController is a controller  to manage a Oracle/BEA WebLogic server.
 */
public class WeblogicController extends AbstractJ2EEApplicationServerController {

    private final static transient Logger LOGGER = LoggerFactory.getLogger(WeblogicController.class);

    private static MBeanHome home = null;

    /**
     * Default constructor.
     *
     * @param url        the JMX URL to connect to WebLogic server.
     * @param username   the admin username to connect to the WebLogic server.
     * @param password   the admin password to connect to the WebLogic server.
     * @param serverName the server/cluster name to manage.
     * @param cluster    a flag indicating if we manage a cluster (true) or a single server (false).
     */
    public WeblogicController(String url, String username, String password, String serverName, Boolean cluster) throws ControllerException {
        super(url, username, password, serverName, cluster);
    }

    protected void init() throws ControllerException {
        LOGGER.info("Connecting to the WebLogic JMX layer");
        try {
            LOGGER.debug("Creating a WebLogic Environment JNDI container");
            Environment environment = new Environment();
            environment.setProviderUrl(this.getUrl());
            environment.setSecurityPrincipal(this.getUsername());
            environment.setSecurityCredentials(this.getPassword());
            LOGGER.debug("Creating the JNDI initial context");
            Context context = environment.getInitialContext();
            LOGGER.debug("Getting WebLogic JMX MBean connector");
            home = (MBeanHome) context.lookup(MBeanHome.ADMIN_JNDI_NAME);
            LOGGER.info("WebLogic server JMX connection initialized");
        } catch (Exception e) {
            LOGGER.error("Can't connect to WebLogic server. The server is probably down.", e);
            throw new ControllerException("Can't connect to WebLogic server. The server is probably down.", e);
        }
    }

    public void shutdown() throws ControllerException {
        try {
            if (this.isCluster()) {
                LOGGER.info("Shutting down WebLogic cluster {}", this.getServerName());
                ClusterRuntimeMBean clusterRuntime = (ClusterRuntimeMBean) home.getRuntimeMBean(this.getServerName(), "ClusterRuntime");
                String[] servers = clusterRuntime.getServerNames();
                for (int i = 0; i < servers.length; i++) {
                    LOGGER.info("Shutting down WebLogic server {}", servers[i]);
                    ServerRuntimeMBean serverRuntime = (ServerRuntimeMBean) home.getRuntimeMBean(servers[i], "ServerRuntime");
                    serverRuntime.shutdown();
                }
            } else {
                LOGGER.info("Shutting down WebLogic server");
                ServerRuntimeMBean serverRuntime = (ServerRuntimeMBean) home.getRuntimeMBean(this.getServerName(), "ServerRuntime");
                serverRuntime.shutdown();
            }
        } catch (Exception e) {
            LOGGER.error("Can't shutdown WebLogic server/cluster {}", this.getServerName(), e);
            throw new ControllerException("Can't shutdown WebLogic server/cluster " + this.getServerName(), e);
        }
    }

    public String status() {
        String status = "N/A";
        try {
            if (this.isCluster()) {
                LOGGER.info("Checking status of WebLogic cluster {}", this.getServerName());
                ClusterRuntimeMBean clusterRuntime = (ClusterRuntimeMBean) home.getRuntimeMBean(this.getServerName(), "ClusterRuntime");
                status = "Weblogic cluster ";
                String[] servers = clusterRuntime.getServerNames();
                for (int i = 0; i < servers.length; i++) {
                    LOGGER.info("Checking status of WebLogic server {}", servers[i]);
                    try {
                        ServerRuntimeMBean serverRuntime = (ServerRuntimeMBean) home.getRuntimeMBean(servers[i], "ServerRuntime");
                        status = status + " (WebLogic " + servers[i] + " " + serverRuntime.getWeblogicVersion() + " " + serverRuntime.getState() + ")";
                    } catch (Exception e) {
                        status = status + " (WebLogic " + servers[i] + " N/A)";
                    }
                }
            } else {
                LOGGER.info("Checking status of WebLogic server {}", this.getServerName());
                ServerRuntimeMBean serverRuntime = (ServerRuntimeMBean) home.getRuntimeMBean(this.getServerName(), "ServerRuntime");
                status = "WebLogic " + serverRuntime.getWeblogicVersion() + " " + serverRuntime.getState();
            }
        } catch (Exception e) {
            LOGGER.warn("Can't check status of WebLogic server/cluster status", e);
        }
        return status;
    }

    public boolean isStopped() {
        if (this.isCluster()) {
            LOGGER.info("Checking if WebLogic cluster {} is stopped", this.getServerName());
            LOGGER.warn("With WebLogic cluster, if I can connect to the cluster manager, the cluster is considered up");
            try {
                home.getRuntimeMBean(this.getServerName(), "ClusterRuntime");
                return false;
            } catch (Exception e) {
                LOGGER.warn("Can't connect to WebLogic cluster {}", this.getServerName(), e);
                return true;
            }
        } else {
            LOGGER.info("Checking if WebLogic server {} is stopped", this.getServerName());
            try {
                ServerRuntimeMBean serverRuntime = (ServerRuntimeMBean) home.getRuntimeMBean(this.getServerName(), "ServerRuntime");
                if (serverRuntime.getState().equals(ServerRuntimeMBean.RUNNING))
                    return false;
                else
                    return true;
            } catch (Exception e) {
                LOGGER.warn("Can't check status of WebLogic server", e);
                return true;
            }
        }
    }

    public boolean isJ2EEApplicationDeployed(String path, String name) throws ControllerException {
        LOGGER.info("Checking if the J2EE application {} is deployed", name);
        try {
            home.getAdminMBean(name, "Application", home.getDomainName());
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public void deployJ2EEApplication(String path, String name, String classloaderorder, String classloaderpolicy, String vhost) throws ControllerException {
        LOGGER.info("Deploying J2EE application {} in WebLogic server/cluster {}", name, this.getServerName());
        try {
            DeployerRuntimeMBean deployerRuntime = DeployerRuntime.getDeployerRuntime(home);
            DeploymentData info = new DeploymentData();
            info.addTarget(this.getServerName(), null);
            if (this.isCluster()) {
                info.setTargetType(this.getServerName(), DeploymentData.CLUSTER);
            } else {
                info.setTargetType(this.getServerName(), DeploymentData.SERVER);
            }
            LOGGER.debug("Launching the deployment task ...");
            DeploymentTaskRuntimeMBean task = deployerRuntime.deploy(path, name, null, info, null);
            while (task.isRunning()) {
                Thread.sleep(200);
            }
        } catch (Exception e) {
            LOGGER.error("Can't deploy J2EE application {}", name, e);
            throw new ControllerException("Can't deploy J2EE application " + name, e);
        }
    }

    public void undeployJ2EEApplication(String path, String name) throws ControllerException {
        LOGGER.info("Undeploying J2EE application {} from WebLogic server/cluster {}", name, this.getServerName());
        try {
            DeployerRuntimeMBean deployerRuntime = DeployerRuntime.getDeployerRuntime(home);
            DeploymentData info = new DeploymentData();
            info.addTarget(this.getServerName(), null);
            if (this.isCluster()) {
                info.setTargetType(this.getServerName(), DeploymentData.CLUSTER);
            } else {
                info.setTargetType(this.getServerName(), DeploymentData.SERVER);
            }
            LOGGER.debug("Launching the undeployment task ...");
            DeploymentTaskRuntimeMBean task = deployerRuntime.undeploy(name, info, null);
            while (task.isRunning()) {
                Thread.sleep(200);
            }
        } catch (Exception e) {
            LOGGER.error("Can't undeploy J2EE application {}", name, e);
            throw new ControllerException("Can't undeploy J2EE application " + name, e);
        }
    }

    public void redeployJ2EEApplication(String path, String name) throws ControllerException {
        LOGGER.info("Redeploying J2EE application {} in WebLogic server/cluster {}", name, this.getServerName());
        try {
            DeployerRuntimeMBean deployerRuntime = DeployerRuntime.getDeployerRuntime(home);
            DeploymentData info = new DeploymentData();
            info.addTarget(this.getServerName(), null);
            if (this.isCluster()) {
                info.setTargetType(this.getServerName(), DeploymentData.CLUSTER);
            } else {
                info.setTargetType(this.getServerName(), DeploymentData.SERVER);
            }
            LOGGER.debug("Launching the redeploy task ...");
            DeploymentTaskRuntimeMBean task = deployerRuntime.redeploy(name, info, null);
            while (task.isRunning()) {
                Thread.sleep(200);
            }
        } catch (Exception e) {
            LOGGER.error("Can't redeploy J2EE application {}", name, e);
            throw new ControllerException("Can't redeploy J2EE application " + name, e);
        }
    }

    public boolean isJDBCConnectionPoolDeployed(String name) throws ControllerException {
        LOGGER.info("Checking if JDBC connection pool {} is deployed in WebLogic server/cluster {}", name, this.getServerName());
        try {
            home.getAdminMBean(name, "JDBCConnectionPool", home.getDomainName());
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public boolean isJDBCConnectionPoolUpToDate(String name, String driver, int increment, int initial, int maximal, String user, String password, String url, String classpath) throws ControllerException {
        LOGGER.info("Checking status of JDBC connection pool {} in WebLogic server/cluster {}", name, this.getServerName());
        if (!this.isJDBCConnectionPoolDeployed(name)) {
            LOGGER.debug("JDBC connection pool {} is not deployed in the WebLogic server/cluster {}", name, this.getServerName());
            return false;
        }
        LOGGER.debug("Looking for JDBC connection pool MBean");
        try {
            JDBCConnectionPoolMBean connectionPool = (JDBCConnectionPoolMBean) home.getAdminMBean(name, "JDBCConnectionPool", home.getDomainName());
            if (connectionPool.getDriverName().equals(driver) && connectionPool.getCapacityIncrement() == increment && connectionPool.getInitialCapacity() == initial && connectionPool.getMaxCapacity() == maximal
                    && ((String) connectionPool.getProperties().getProperty("user")).equals(user) && connectionPool.getPassword().equals(password) && connectionPool.getURL().equals(url)) {
                LOGGER.debug("JDBC connection pool {} is up to date", name);
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("Can't check status of JBDC connection pool {}", name, e);
            throw new ControllerException("Can't check status of JDBC connection pool " + name, e);
        }
        return false;
    }

    public boolean updateJDBCConnectionPool(String name, String driver, int increment, int initial, int maximal, String user, String password, String url, String classpath) throws ControllerException {
        LOGGER.info("Updating JDBC connection pool {} in WebLogic server/cluster {}", name, this.getServerName());
        try {
            LOGGER.debug("Looking for the JDBC connection pool MBean");
            JDBCConnectionPoolMBean connectionPool = (JDBCConnectionPoolMBean) home.getAdminMBean(name, "JDBCConnectionPool", home.getDomainName());
            if (connectionPool.getDriverName().equals(driver) && connectionPool.getCapacityIncrement() == increment && connectionPool.getInitialCapacity() == initial && connectionPool.getMaxCapacity() == maximal
                    && ((String) connectionPool.getProperties().getProperty("user")).equals(user) && connectionPool.getPassword().equals(password) && connectionPool.getURL().equals(url)) {
                LOGGER.debug("JDBC connection pool {} is already up to date, nothing to do", name);
                return false;
            }
            connectionPool.setDriverName(driver);
            connectionPool.setCapacityIncrement(increment);
            connectionPool.setInitialCapacity(initial);
            connectionPool.setMaxCapacity(maximal);
            connectionPool.setShrinkingEnabled(true);
            Properties properties = new Properties();
            properties.put("user", user);
            connectionPool.setProperties(properties);
            connectionPool.setPassword(password);
            connectionPool.setURL(url);
            return true;
        } catch (Exception e) {
            LOGGER.error("Can't update JDBC connection pool {}", name, e);
            throw new ControllerException("Can't update JDBC connection pool " + name, e);
        }
    }

    public void deployJDBCConnectionPool(String name, String driver, int increment, int initial, int maximal, String user, String password, String url, String classpath) throws ControllerException {
        LOGGER.info("Deploying JDBC connection pool {} in WebLogic server/cluster {}", name, this.getServerName());
        try {
            TargetMBean target;
            if (this.isCluster()) {
                target = (ClusterMBean) home.getAdminMBean(this.getServerName(), "Cluster");
            } else {
                target = (ServerMBean) home.getAdminMBean(this.getServerName(), "Server");
            }
            JDBCConnectionPoolMBean connectionPool = (JDBCConnectionPoolMBean) home.createAdminMBean(name, "JDBCConnectionPool", home.getDomainName());
            connectionPool.setDriverName(driver);
            connectionPool.setCapacityIncrement(increment);
            connectionPool.setInitialCapacity(initial);
            connectionPool.setMaxCapacity(maximal);
            connectionPool.setURL(url);
            connectionPool.setShrinkingEnabled(true);
            Properties properties = new Properties();
            properties.put("user", user);
            connectionPool.setProperties(properties);
            connectionPool.setPassword(password);
            connectionPool.addTarget(target);
        } catch (Exception e) {
            LOGGER.error("Can't deploy JBDC connection pool {}", name, e);
            throw new ControllerException("Can't deploy JDBC connection pool " + name, e);
        }
    }

    public void undeployJDBCConnectionPool(String name) throws ControllerException {
        LOGGER.info("Undeploying JDBC connection pool {} from WebLogic server/cluster {}", name, this.getServerName());
        try {
            TargetMBean target;
            if (this.isCluster()) {
                target = (ClusterMBean) home.getAdminMBean(this.getServerName(), "Cluster");
            } else {
                target = (ServerMBean) home.getAdminMBean(this.getServerName(), "Cluster");
            }
            JDBCConnectionPoolMBean connectionPool = (JDBCConnectionPoolMBean) home.getMBean(name, "JDBCConnectionPool", home.getDomainName());
            connectionPool.removeTarget(target);
            home.deleteMBean(connectionPool);
        } catch (Exception e) {
            LOGGER.error("Can't undeploy JBDC connection pool {}", name, e);
            throw new ControllerException("Can't undeploy JDBC connection pool " + name, e);
        }
    }

    public boolean isJDBCDataSourceDeployed(String name) throws ControllerException {
        LOGGER.info("Checking if JDBC data source {} is already deployed in WebLogic server/cluster {}", name, this.getServerName());
        try {
            home.getMBean(name, "JDBCTxDataSource", home.getDomainName());
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public void deployJDBCDataSource(String name, String jdbcConnectionPool, String jdbcUrl, String helperClassname) throws ControllerException {
        LOGGER.info("Deploying JDBC data source {} in WebLogic server/cluster {}", name, this.getServerName());
        try {
            TargetMBean target;
            if (this.isCluster()) {
                target = (ClusterMBean) home.getAdminMBean(this.getServerName(), "Cluster");
            } else {
                target = (ServerMBean) home.getAdminMBean(this.getServerName(), "Server");
            }
            JDBCTxDataSourceMBean dataSource = (JDBCTxDataSourceMBean) home.createAdminMBean(name, "JDBCTxDataSource", home.getDomainName());
            dataSource.setJNDIName(name);
            dataSource.setPoolName(jdbcConnectionPool);
            dataSource.setRowPrefetchEnabled(true);
            dataSource.addTarget(target);
        } catch (Exception e) {
            LOGGER.error("Can't deploy JDBC data source {}", name, e);
            throw new ControllerException("Can't deploy JDBC data source " + name, e);
        }
    }

    public void undeployJDBCDataSource(String name) throws ControllerException {
        LOGGER.info("Undeploying JDBC data source {} from WebLogic {}", name, this.getServerName());
        try {
            TargetMBean target;
            if (this.isCluster()) {
                target = (ClusterMBean) home.getAdminMBean(this.getServerName(), "Cluster");
            } else {
                target = (ServerMBean) home.getAdminMBean(this.getServerName(), "Server");
            }
            JDBCTxDataSourceMBean dataSource = (JDBCTxDataSourceMBean) home.getMBean(name, "JDBCTxDataSource", home.getDomainName());
            dataSource.removeTarget(target);
            home.deleteMBean(dataSource);
        } catch (Exception e) {
            LOGGER.error("Can't undeploy JDBC data source {}", name, e);
            throw new ControllerException("Can't undeploy JDBC data source " + name, e);
        }
    }

    public boolean isJDBCDataSourceUpToDate(String name, String jdbcConnectionPool, String jdbcUrl, String helperClassname) throws ControllerException {
        LOGGER.info("Checking status of JDBC data source {} in WebLogic server/cluster {}", name, this.getServerName());
        if (!this.isJDBCDataSourceDeployed(name)) {
            LOGGER.debug("JDBC data source {} is not deployed in WebLogic server/cluster {}", name, this.getServerName());
            return false;
        }
        try {
            JDBCTxDataSourceMBean dataSource = (JDBCTxDataSourceMBean) home.getAdminMBean(name, "JDBCTxDataSource", home.getDomainName());
            if (dataSource.getPoolName().equals(jdbcConnectionPool)) {
                LOGGER.debug("JDBC data source {} is up to date", name);
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("Can't check status of JDBC data source {}", name, e);
            throw new ControllerException("Can't check status of JDBC data source " + name, e);
        }
        return false;
    }

    public boolean updateJDBCDataSource(String name, String jdbcConnectionPool, String jdbcUrl, String helperClassname) throws ControllerException {
        LOGGER.info("Updating JDBC data source {} in WebLogic server/cluster {}", name, this.getServerName());
        try {
            if (!this.isJDBCDataSourceDeployed(name)) {
                LOGGER.debug("JDBC data source {} is not deployed in WebLogic server/cluster {}", name, this.getServerName());
                return false;
            }
            JDBCTxDataSourceMBean dataSource = (JDBCTxDataSourceMBean) home.getAdminMBean(name, "JDBCTxDataSource", home.getDomainName());
            if (dataSource.getName().equals(name) && dataSource.getPoolName().equals(jdbcConnectionPool)) {
                LOGGER.debug("JDBC data source {} is already up to date, nothing to do", name);
                return false;
            } else {
                dataSource.setPoolName(jdbcConnectionPool);
                dataSource.setRowPrefetchEnabled(true);
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("Can't update JDBC data source {}", name, e);
            throw new ControllerException("Can't update JDBC data source " + name, e);
        }
    }

    public boolean isJMSConnectionFactoryDeployed(String name) throws ControllerException {
        LOGGER.info("Check if JMS connection factory {} is already deployed in WebLogic server/cluster {}", name, this.getServerName());
        try {
            home.getAdminMBean(name, "JMSConnectionFactory", home.getDomainName());
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public void deployJMSConnectionFactory(String name) throws ControllerException {
        LOGGER.info("Deploying JMS connection factory {} in WebLogic server/cluster {}", name, this.getServerName());
        try {
            TargetMBean target;
            if (this.isCluster()) {
                target = (ClusterMBean) home.getAdminMBean(this.getServerName(), "Cluster");
            } else {
                target = (ServerMBean) home.getAdminMBean(this.getServerName(), "Server");
            }
            JMSConnectionFactoryMBean connectionFactory = (JMSConnectionFactoryMBean) home.createAdminMBean(name, "JMSConnectionFactory", home.getDomainName());
            connectionFactory.setJNDIName(name);
            connectionFactory.addTarget(target);
        } catch (Exception e) {
            LOGGER.error("Can't deploy JMS connection factory {}", name, e);
            throw new ControllerException("Can't deploy JMS connection factory " + name, e);
        }
    }

    public void undeployJMSConnectionFactory(String name) throws ControllerException {
        LOGGER.info("Undeploying JMS connection factory {} from WebLogic server/cluster {}", name, this.getServerName());
        try {
            TargetMBean target;
            if (this.isCluster()) {
                target = (ClusterMBean) home.getAdminMBean(this.getServerName(), "Cluster");
            } else {
                target = (ServerMBean) home.getAdminMBean(this.getServerName(), "Server");
            }
            JMSConnectionFactoryMBean connectionFactory = (JMSConnectionFactoryMBean) home.getAdminMBean(name, "JMSConnectionFactory", home.getDomainName());
            connectionFactory.removeTarget(target);
            home.deleteMBean(connectionFactory);
        } catch (Exception e) {
            LOGGER.error("Can't undeploy JMS connection factory {}", name, e);
            throw new ControllerException("Can't undeploy JMS connection factory " + name, e);
        }
    }

    public boolean isJMSServerDeployed(String name) throws ControllerException {
        LOGGER.info("Checking if JMS server {} is already deployed in WebLogic server/cluster {}", name, this.getServerName());
        try {
            home.getAdminMBean(name, "JMSServer", home.getDomainName());
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public void deployJMSServer(String name, List queues, List topics) throws ControllerException {
        LOGGER.info("Deploying JMS server {} in WebLogic server/cluster {}", name, this.getServerName());
        try {
            TargetMBean target;
            if (this.isCluster()) {
                target = (ClusterMBean) home.getAdminMBean(this.getServerName(), "Cluster");
            } else {
                target = (ServerMBean) home.getAdminMBean(this.getServerName(), "Server");
            }
            JMSServerMBean jmsServer = (JMSServerMBean) home.createAdminMBean(name, "JMSServer", home.getDomainName());
            LOGGER.debug("Create the JMS queue destinations");
            for (Iterator queueIterator = queues.iterator(); queueIterator.hasNext(); ) {
                String queueName = (String) queueIterator.next();
                JMSQueueMBean queue = (JMSQueueMBean) home.createAdminMBean(queueName, "JMSQueue");
                queue.setJNDIName(queueName);
                queue.setParent(jmsServer);
                jmsServer.addDestination(queue);
            }
            LOGGER.debug("Create the JMS topic destinations");
            for (Iterator topicIterator = topics.iterator(); topicIterator.hasNext(); ) {
                String topicName = (String) topicIterator.next();
                JMSTopicMBean topic = (JMSTopicMBean) home.createAdminMBean(topicName, "JMSTopic");
                topic.setJNDIName(topicName);
                topic.setParent(jmsServer);
                jmsServer.addDestination(topic);
            }
            jmsServer.addTarget(target);
            LOGGER.debug("JMS Server deployed");
        } catch (Exception e) {
            LOGGER.error("Can't deploy JMS server {}", name, e);
            throw new ControllerException("Can't deploy JMS server " + name, e);
        }
    }

    public boolean isJMSServerUpToDate(String name, List queues, List topics) throws ControllerException {
        LOGGER.info("Checking status of JMS server {} in WebLogic server/cluster {}", name, this.getServerName());
        if (!this.isJMSServerDeployed(name)) {
            LOGGER.debug("JMS server is not deployed in WebLogic server/cluster {}", this.getServerName());
            return false;
        }
        try {
            // get the JMS server MBean
            JMSServerMBean jmsServer = (JMSServerMBean) home.getAdminMBean(name, "JMSServer", home.getDomainName());
            // get the JMS server destinations
            JMSDestinationMBean[] destinations = jmsServer.getDestinations();
            // check JMS queues
            for (Iterator queueIterator = queues.iterator(); queueIterator.hasNext(); ) {
                String queueName = (String) queueIterator.next();
                boolean found = false;
                for (int i = 0; i < destinations.length; i++) {
                    JMSDestinationMBean destination = destinations[i];
                    if (destination.getJNDIName().equals(queueName)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return false;
                }
            }
            // check JMS topic
            for (Iterator topicIterator = topics.iterator(); topicIterator.hasNext(); ) {
                String topicName = (String) topicIterator.next();
                boolean found = false;
                for (int i = 0; i < destinations.length; i++) {
                    JMSDestinationMBean destination = destinations[i];
                    if (destination.getJNDIName().equals(topicName)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return false;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Can't check status of JMS server {}", name, e);
            throw new ControllerException("Can't check status of JMS server " + name, e);
        }
        return true;
    }

    public boolean updateJMSServer(String name, List queues, List topics) throws ControllerException {
        LOGGER.info("Updating JMS server {} in WebLogic server/cluster {}", name, this.getServerName());
        boolean updated = false;
        try {
            JMSServerMBean jmsServer = (JMSServerMBean) home.getAdminMBean(name, "JMSServer", home.getDomainName());
            LOGGER.debug("Getting JMS server destinations");
            JMSDestinationMBean[] destinations = jmsServer.getDestinations();
            LOGGER.debug("Checking if all JMS queues are present");
            for (Iterator queueIterator = queues.iterator(); queueIterator.hasNext(); ) {
                String queueName = (String) queueIterator.next();
                boolean found = false;
                for (int i = 0; i < destinations.length; i++) {
                    JMSDestinationMBean destination = destinations[i];
                    if (destination.getJNDIName().equals(queueName)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    LOGGER.debug("JMS queue {} is not present in the JMS server, deploy it", name);
                    updated = true;
                    JMSQueueMBean queue = (JMSQueueMBean) home.createAdminMBean(queueName, "JMSQueue", home.getDomainName());
                    queue.setJNDIName(queueName);
                    queue.setParent(jmsServer);
                    jmsServer.addDestination(queue);
                }
            }
            LOGGER.debug("Check if all JMS topics are present");
            for (Iterator topicIterator = topics.iterator(); topicIterator.hasNext(); ) {
                String topicName = (String) topicIterator.next();
                boolean found = false;
                for (int i = 0; i < destinations.length; i++) {
                    JMSDestinationMBean destination = destinations[i];
                    if (destination.getJNDIName().equals(topicName)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    LOGGER.debug("JMS topic {} is not present in the JMS server, deploy it", name);
                    updated = true;
                    JMSTopicMBean topic = (JMSTopicMBean) home.createAdminMBean(topicName, "JMSTopic", home.getDomainName());
                    topic.setJNDIName(topicName);
                    topic.setParent(jmsServer);
                    jmsServer.addDestination(topic);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Can't update JMS server {}" + name, e);
            throw new ControllerException("Can't update JMS server " + name, e);
        }
        return updated;
    }

    public void undeployJMSServer(String name) throws ControllerException {
        LOGGER.info("Undeploying JMS server {} from WebLogic server/cluster {}", name, this.getServerName());
        try {
            TargetMBean target;
            if (this.isCluster()) {
                target = (ClusterMBean) home.getAdminMBean(this.getServerName(), "Cluster");
            } else {
                target = (ServerMBean) home.getAdminMBean(this.getServerName(), "Server");
            }
            JMSServerMBean jmsServer = (JMSServerMBean) home.getAdminMBean(name, "JMSServer", home.getDomainName());
            jmsServer.removeTarget(target);
            home.deleteMBean(jmsServer);
        } catch (Exception e) {
            LOGGER.error("Can't undeploy JMS server {}", name, e);
            throw new ControllerException("Can't undeploy JMS server " + name, e);
        }
    }

    public boolean isJNDIBindingDeployed(String name) {
        LOGGER.warn("JNDI bindings are not supported on WebLogic server/cluster");
        return true;
    }

    public void deployJNDIBinding(String name, String jndiName, String jndiAlias, String providerUrl) throws ControllerException {
        LOGGER.warn("JNDI bindings are not supported on WebLogic server/cluster");
    }

    public void undeployJNDIBinding(String name) throws ControllerException {
        LOGGER.warn("JNDI bindings are not supported on WebLogic server/cluster");
    }

    public boolean isJNDIBindingUpToDate(String name, String jndiName, String jndiAlias, String providerUrl) throws ControllerException {
        LOGGER.warn("JNDI bindings are not supported on WebLogic server/cluster");
        return false;
    }

    public boolean updateJNDIBinding(String name, String jndiName, String jndiAlias, String providerUrl) throws ControllerException {
        LOGGER.warn("JNDI bindings are not supported on WebLogic server/cluster");
        return false;
    }

    public boolean isSharedLibraryDeployed(String name) throws ControllerException {
        LOGGER.warn("Shared libraries are not supported on WebLogic server/cluster");
        return true;
    }

    public void deploySharedLibrary(String name, String classpath) throws ControllerException {
        LOGGER.warn("Shared libraries are not supported on WebLogic server/cluster");
    }

    public void undeploySharedLibrary(String name) throws ControllerException {
        LOGGER.warn("Shared libraries are not supported on WebLogic server/cluster");
    }

    public boolean isSharedLibraryUpToDate(String name, String classpath) throws ControllerException {
        LOGGER.warn("Shared libraries are not supported on WebLogic server/cluster");
        return false;
    }

    public boolean updateSharedLibrary(String name, String classpath) throws ControllerException {
        LOGGER.warn("Shared libraries are not supported on WebLogic server/cluster");
        return false;
    }

}
