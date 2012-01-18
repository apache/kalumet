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
import org.apache.kalumet.controller.core.AbstractJ2EEApplicationServerController;
import org.apache.kalumet.controller.core.ControllerException;
import org.jboss.mx.util.propertyeditor.ObjectNameEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.*;
import java.lang.reflect.MalformedParameterizedTypeException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * JBoss 6 controller.
 */
public class JBoss6Controller extends AbstractJ2EEApplicationServerController {
    
    private final static transient Logger LOGGER = LoggerFactory.getLogger(JBoss6Controller.class);

    private JMXServiceURL jmxServiceURL;
    private URL deployURL;

    /**
     * Default constructor.
     * NB: the JBoss application server should accept remote JMX connection. To do so,in the run.conf/run.bat of the JBoss application server,
     * you have to add:
     * JAVA_OPTS="$JAVA_OPTS
     *      -Djboss.platform.mbeanserver
     *      -Djavax.management.builder.initial=org.jboss.system.server.jmx.MBeanServerBuilderImpl
     *      -Dcom.sun.management.jmxremote.port=12345
     *      -Dcom.sun.management.jmxremote.authenticate=false
     *      -Dcom.sun.management.jmxremote.ssl=false
     *      -Djava.util.logging.manager=org.jboss.logmanager.LogManager
     *      -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
     *      -Dorg.jboss.logging.Logger.pluginClass=org.jboss.logging.logmanager.LoggerPluginImpl"
     *  JBOSS_CLASSPATH="/absolute/path/to/lib/jboss-logmanager.jar"
     *
     * @param url the JMX URL to connect to the JBoss application server (both pure JMX URL and JNP URLs are supported).
     * @param username the username to connect to the JBoss MBean server.
     * @param password the password to connect to the JBoss MBean server.
     * @param serverName the server name to manage (not used with JBoss).
     * @param cluster true to use a cluster topology, false else (not used with JBoss).
     * @throws ControllerException in case of connection failure.
     */
    public JBoss6Controller(String url, String username, String password, String serverName, Boolean cluster) throws ControllerException {
        super(url, username, password, serverName, cluster);
        if (url.startsWith("jnp://")) {
            url = url.substring(6);
            url = "service:jmx:rmi:///jndi/rmi://" + url + "/jmxrmi";
            this.setUrl(url);
        }
    }

    /**
     * Initialize the connection to the JBoss application server.
     * @throws ControllerException
     */
    protected void init() throws ControllerException {
        try {
            this.jmxServiceURL = new JMXServiceURL(this.getUrl());
        } catch (Exception e) {
            LOGGER.error("Can't connect to the JBoss application server", e);
            throw new ControllerException("Can't connect to the JBoss application server", e);
        }
        // disable the deployment scanner and get the deploy folder
        JMXConnector connector = null;
        try {
            connector = JMXConnectorFactory.connect(jmxServiceURL, null);
            MBeanServerConnection server = connector.getMBeanServerConnection();
            ObjectName deploymentScannerMBean = new ObjectName("jboss.deployment:flavor=URL,type=DeploymentScanner");
            server.invoke(deploymentScannerMBean, "stop", null, null);
            ObjectName serverConfigMBean = new ObjectName("jboss.system:type=ServerConfig");
            String deployFolder = ((URL) server.getAttribute(serverConfigMBean, "ServerHomeLocation")).toString() + "/deploy";
            this.deployURL = new URL(deployFolder);
        } catch (Exception e) {
            LOGGER.error("Can't stop the JBoss deployment scanner or get the deploy folder", e);
            throw new ControllerException("Can't stop the JBoss deployment scanner or get the deploy folder", e);
        } finally {
            if (connector != null) {
                try {
                    connector.close();
                } catch (Exception e) {
                    // nothing to do
                }
            }
        }
    }
    
    public void shutdown() throws ControllerException {
        LOGGER.info("Shutting down JBoss application server");
        JMXConnector connector = null;
        try {
            connector = JMXConnectorFactory.connect(jmxServiceURL, null);
            ObjectName serverMBean = new ObjectName("jboss.system:type=Server");
            MBeanServerConnection server = connector.getMBeanServerConnection();
            server.invoke(serverMBean, "shutdown", null, null);
        } catch (Exception e) {
            LOGGER.error("Can't shutdown JBoss application server", e);
            throw new ControllerException("Can't shutdown JBoss application server", e);
        } finally {
            if (connector != null) {
                try {
                    connector.close();
                } catch (Exception e) {
                    // nothing to do
                }
            }
        }
    }

    public String status() throws ControllerException {
        LOGGER.info("Checking status of JBoss application server");
        boolean stopped = isStopped();
        JMXConnector connector = null;
        try {
            connector = JMXConnectorFactory.connect(jmxServiceURL, null);
            ObjectName serverMBean = new ObjectName("jboss.system:type=Server");
            MBeanServerConnection server = connector.getMBeanServerConnection();
            if (!stopped) {
                LOGGER.debug("JBoss application server started");
                return "JBoss application server started since " + (Date) server.getAttribute(serverMBean, "StartDate");
            } else {
                LOGGER.debug("JBoss application server stopped");
                return "JBoss application server stopped";
            }
        } catch (Exception e) {
            LOGGER.warn("Can't check status of the JBoss application server", e);
            return "N/A";
        } finally {
            if (connector != null) {
                try {
                    connector.close();
                } catch (Exception e) {
                    // nothing to do
                }
            }
        }
    }
    
    public boolean isStopped() throws ControllerException {
        LOGGER.info("Checking if JBoss application server is stopped");
        JMXConnector connector = null;
        try {
            connector = JMXConnectorFactory.connect(jmxServiceURL, null);
            ObjectName serverMBean = new ObjectName("jboss.system:type=Server");
            MBeanServerConnection server = connector.getMBeanServerConnection();
            return !(((Boolean) server.getAttribute(serverMBean, "Started")).booleanValue());
        } catch (Exception e) {
            LOGGER.warn("Can't check if JBoss application server is stopped. The server is probably down.", e);
            return true;
        }
    }

    /**
     * Format an application path in a JBoss compliant URL.
     *
     * @param path the J2EE application path.
     * @return the JBoss application URL.
     */
    private static String  formatPathToUrl(String path) {
        String trimmed = path.trim();
        if (trimmed.startsWith("http:") || trimmed.startsWith("file:")) {
            LOGGER.debug("The path is already in a JBoss compliant URL");
            return trimmed;
        } else {
            LOGGER.debug("Prefixing path with file: protocol");
            return "file:" + trimmed;
        }
    }
    
    public boolean isJ2EEApplicationDeployed(String path, String name) throws ControllerException {
        LOGGER.info("Checking if J2EE application {} is deployed in the JBoss application server", name);
        String applicationUrl = JBoss6Controller.formatPathToUrl(path);
        boolean deployed = false;
        JMXConnector connector = null;
        try {
            connector = JMXConnectorFactory.connect(jmxServiceURL, null);
            MBeanServerConnection server = connector.getMBeanServerConnection();
            ObjectName mainDeployerMBean = new ObjectName("jboss.system:service=MainDeployer");
            deployed = ((Boolean) server.invoke(mainDeployerMBean, "isDeployed", new Object[]{ applicationUrl }, new String[]{ "java.lang.String" })).booleanValue();
        } catch (Exception e) {
            LOGGER.error("Can't check if J2EE application {} is deployed", name, e);
            throw new ControllerException("Can't check if J2EE application " + name + " is deployed", e);
        } finally {
            if (connector != null) {
                try {
                    connector.close();
                } catch (Exception e) {
                    // nothing to do
                }
            }
        }
        return deployed;
    }

    public void deployJ2EEApplication(String path, String name, String classloaderorder, String classloaderpolicy, String vhost) throws ControllerException {
        LOGGER.info("Deploying J2EE application {} located {}", name, path);
        String applicationUrl = JBoss6Controller.formatPathToUrl(path);
        JMXConnector connector = null;
        try {
            connector = JMXConnectorFactory.connect(jmxServiceURL, null);
            MBeanServerConnection server = connector.getMBeanServerConnection();
            ObjectName mainDeployerMBean = new ObjectName("jboss.system:service=MainDeployer");
            server.invoke(mainDeployerMBean, "deploy", new Object[]{ applicationUrl }, new String[]{ "java.lang.String"});
        } catch (Exception e) {
            LOGGER.error("Can't deploy J2EE application {}", name, e);
            throw new ControllerException("Can't deploy J2EE application " + name, e);
        } finally {
            if (connector != null) {
                try {
                    connector.close();
                } catch (Exception e) {
                    // nothing to do
                }
            }
        }
    }

    public void undeployJ2EEApplication(String path, String name) throws ControllerException {
        LOGGER.info("Undeploying J2EE application {} located {}", name, path);
        String applicationUrl = JBoss6Controller.formatPathToUrl(path);
        JMXConnector connector = null;
        try {
            connector = JMXConnectorFactory.connect(jmxServiceURL, null);
            MBeanServerConnection server = connector.getMBeanServerConnection();
            ObjectName mainDeployerMBean = new ObjectName("jboss.system:service=MainDeployer");
            server.invoke(mainDeployerMBean, "undeploy", new Object[]{ applicationUrl }, new String[] { "java.lang.String" });
        } catch (Exception e) {
            LOGGER.error("Can't undeploy J2EE application {}", name, e);
            throw new ControllerException("Can't undeploy J2EE application " + name, e);
        } finally {
            if (connector != null) {
                try {
                    connector.close();
                } catch (Exception e) {
                    // nothing to do
                }
            }
        }
    }

    public void redeployJ2EEApplication(String path, String name) throws ControllerException {
        LOGGER.info("Redeploying J2EE application {} located {}", name, path);
        String applicationUrl = JBoss6Controller.formatPathToUrl(path);
        JMXConnector connector = null;
        try {
            connector = JMXConnectorFactory.connect(jmxServiceURL, null);
            MBeanServerConnection server = connector.getMBeanServerConnection();
            ObjectName mainDeployerMBean = new ObjectName("jboss.system:service=MainDeployer");
            server.invoke(mainDeployerMBean, "redeploy", new Object[]{ applicationUrl }, new String[]{ "java.lang.String" });
        } catch (Exception e) {
            LOGGER.error("Can't redeploy J2EE application {}", name, e);
            throw new ControllerException("Can't redeploy J2EE application " + name, e); 
        } finally {
            if (connector != null) {
                try {
                    connector.close();
                } catch (Exception e) {
                    // nothing to do
                }
            }
        }
    }

    public boolean isJDBCConnectionPoolDeployed(String name) throws ControllerException {
        LOGGER.info("Checking if JDBC connection pool {} is deployed", name);
        boolean deployed = false;
        File file = new File(deployURL.getPath() + "/" + name + "-ds.xml");
        JMXConnector connector = null;
        try {
            connector = JMXConnectorFactory.connect(jmxServiceURL, null);
            MBeanServerConnection server = connector.getMBeanServerConnection();
            ObjectName mainDeployerMBean = new ObjectName("jboss.system:service=MainDeployer");
            deployed = ((Boolean) server.invoke(mainDeployerMBean, "isDeployed", new Object[]{ file.toURL() }, new String[]{ "java.net.URL" })).booleanValue();
        } catch (Exception e) {
            LOGGER.error("Can't check if JDBC connection pool {} is deployed", name, e);
            throw new ControllerException("Can't check if JDBC connection pool " + name + " is deployed", e);
        } finally {
            if (connector != null) {
                try {
                    connector.close();
                } catch (Exception e) {
                    // nothing to do
                }
            }
        }
        return deployed;
    }

    public boolean isJDBCConnectionPoolUpToDate(String name, String driver, int increment, int initial, int maximal, String user, String password, String url, String classpath) throws ControllerException {
        LOGGER.info("Checking if JDBC connection pool {} is up to date", name);
        if (!this.isJDBCConnectionPoolDeployed(name)) {
            LOGGER.debug("JDBC connection pool {} is not deployed", name);
            return false;
        }
        File tempFile = new File(deployURL.getPath() + "/" + name + "-ds.xml.temp");
        this.jdbcConnectionPoolWriteFile(tempFile, name, driver, increment, initial, maximal, user, password, url);
        FileManipulator fileManipulator = null;
        try {
            fileManipulator = new FileManipulator();
            if (fileManipulator.contentEquals(deployURL.getPath() + "/" + name + "-ds.xml", deployURL.getPath() + "/" + name + "-ds.xml.temp")) {
                LOGGER.debug("JDBC connection pool {} is already up to date", name);
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("Can't check status of JDBC connection pool {}", name, e);
            throw new ControllerException("Can't check status of JDBC connection pool " + name, e);
        } finally {
            if (fileManipulator != null) {
                try {
                    fileManipulator.delete(tempFile.getAbsolutePath());
                } catch (Exception e) {
                    // nothing to do
                }
            }
        }
        return false;
    }

    public boolean updateJDBCConnectionPool(String name, String driver, int increment, int initial, int maximal, String user, String password, String url, String classpath) throws ControllerException {
        LOGGER.info("Updating JDBC connection pool {}", name);
        if (!this.isJDBCConnectionPoolUpToDate(name, driver, increment, initial, maximal, user, password, url, classpath)) {
            LOGGER.debug("JDBC connection pool {} must be updated, redeploy it", name);
            this.undeployJDBCConnectionPool(name);
            this.deployJDBCConnectionPool(name, driver, increment, initial, maximal, user, password, url, classpath);
            return true;
        }
        return false;
    }

    public void deployJDBCConnectionPool(String name, String driver, int increment, int initial, int maximal, String user, String password, String url, String classpath) throws ControllerException {
        LOGGER.info("Deploying JDBC connection pool {}", name);
        LOGGER.debug("Creating the JBoss datasource XML file");
        File file = new File(deployURL.getPath() + "/" + name + "-ds.xml");
        this.jdbcConnectionPoolWriteFile(file, name, driver, increment, initial, maximal, user, password, url);
        JMXConnector connector = null;
        try {
            connector = JMXConnectorFactory.connect(jmxServiceURL, null);
            MBeanServerConnection server = connector.getMBeanServerConnection();
            ObjectName mainDeployerMBean = new ObjectName("jboss.system:service=MainDeployer");
            server.invoke(mainDeployerMBean, "deploy", new Object[]{ file.toURL() }, new String[]{ "java.net.URL" });
        } catch (Exception e) {
            LOGGER.error("Can't deploy JDBC connection pool {}", name, e);
            throw new ControllerException("Can't deploy JDBC connection pool " + name, e);
        } finally {
            if (connector != null) {
                try {
                    connector.close();
                } catch (Exception e) {
                    // nothing to do
                }
            }
        }
    }

    public void undeployJDBCConnectionPool(String name) throws ControllerException {
        LOGGER.info("Undeploying JDBC connection pool {}", name);
        File file = new File(deployURL.getPath() + "/" + name + "-ds.xml");
        JMXConnector connector = null;
        try {
            connector = JMXConnectorFactory.connect(jmxServiceURL, null);
            MBeanServerConnection server = connector.getMBeanServerConnection();
            ObjectName mainDeployerMBean = new ObjectName("jboss.system:service=MainDeployer");
            server.invoke(mainDeployerMBean, "undeploy", new Object[]{ file.toURL() }, new String[]{ "java.net.URL" });
        } catch (Exception e) {
            LOGGER.error("Can't undeploy JDBC connection pool {}", name, e);
            throw new ControllerException("Can't undeploy JDBC connection pool " + name, e);
        } finally {
            if (connector != null) {
                try {
                    connector.close();
                } catch (Exception e) {
                    // nothing to do
                }
            }
        }
    }

    public boolean isJDBCDataSourceDeployed(String name) throws ControllerException {
        LOGGER.warn("JDBC data source is not available with JBoss server. Use JDBC connection pool instead.");
        return true;
    }

    public boolean isJDBCDataSourceUpToDate(String name, String jdbcConnectionPool, String jdbcUrl, String helperClassname) throws ControllerException {
        LOGGER.warn("JDBC data source is not available with JBoss server. Use JDBC connection pool instead.");
        return true;
    }

    public void deployJDBCDataSource(String name, String jdbcConnectionPool, String jdbcUrl, String helperClassname) throws ControllerException {
        LOGGER.warn("JDBC data source is not available with JBoss server. Use JDBC connection pool instead.");
    }

    public void undeployJDBCDataSource(String name) throws ControllerException {
        LOGGER.warn("JDBC data source is not available with JBoss server. Use JDBC connection pool instead.");
    }

    public boolean updateJDBCDataSource(String name, String jdbcConnectionPool, String jdbcUrl, String helperClassname) throws ControllerException {
        LOGGER.warn("JDBC data source is not available with JBoss server. Use JDBC connection pool instead.");
        return false;
    }

    public boolean isJMSConnectionFactoryDeployed(String name) throws ControllerException {
        LOGGER.info("Checking if the JMS connection factory {} is already deployed");
        boolean deployed = false;
        File file = new File(deployURL.getPath() + "/jms/" + name + "-ds.xml");
        JMXConnector connector = null;
        try {
            connector = JMXConnectorFactory.connect(jmxServiceURL, null);
            MBeanServerConnection server = connector.getMBeanServerConnection();
            ObjectName mainDeployerMBean = new ObjectName("jboss.system:service=MainDeployer");
            deployed = ((Boolean) server.invoke(mainDeployerMBean, "isDeployed", new Object[]{ file.toURL() }, new String[]{ "java.net.URL" })).booleanValue();
        } catch (Exception e) {
            LOGGER.error("Can't check if the JMS connection {} is deployed", name, e);
            throw new ControllerException("Can't check if the JMS connection " + name + " is deployed", e);
        } finally {
            if (connector != null) {
                try {
                    connector.close();
                } catch (Exception e) {
                    // nothing to do
                }
            }
        }
        return deployed;
    }

    public void deployJMSConnectionFactory(String name) throws ControllerException {
        LOGGER.info("Deploying JMS connection factory {}", name);
        File file = new File(deployURL.getPath() + "/jms/" + name + "-ds.xml");
        jmsConnectionFactoryWriteFile(file, name);
        JMXConnector connector = null;
        try {
            connector = JMXConnectorFactory.connect(jmxServiceURL, null);
            MBeanServerConnection server = connector.getMBeanServerConnection();
            ObjectName mainDeployerMBean = new ObjectName("jboss.system:service=MainDeployer");
            server.invoke(mainDeployerMBean, "deploy", new Object[]{ file.toURL() }, new String[]{ "java.net.URL" });
        } catch (Exception e) {
            LOGGER.error("Can't deploy JMS connection factory {}", name, e);
            throw new ControllerException("Can't deploy JMS connection factory " + name, e);
        } finally {
            if (connector != null) {
                try {
                    connector.close();
                } catch (Exception e) {
                    // nothing to do
                }
            }
        }
    }

    public void undeployJMSConnectionFactory(String name) throws ControllerException {
        LOGGER.info("Undeploying JMS connection factory {}", name);
        File file = new File(deployURL.getPath() + "/jms/" + name + "-ds.xml");
        JMXConnector connector = null;
        try {
            connector = JMXConnectorFactory.connect(jmxServiceURL, null);
            MBeanServerConnection server = connector.getMBeanServerConnection();
            ObjectName mainDeployerMBean = new ObjectName("jboss.system:service=MainDeployer");
            server.invoke(mainDeployerMBean, "undeploy", new Object[]{ file.toURL() }, new String[]{ "java.net.URL" });
        } catch (Exception e) {
            LOGGER.error("Can't undeploy JMS connection factory {}", name, e);
            throw new ControllerException("Can't undeploy JMS connection factory " + name, e);
        } finally {
            if (connector != null) {
                try {
                    connector.close();
                } catch (Exception e) {
                    // nothing to do
                }
            }
        }
    }

    public boolean isJMSServerDeployed(String name) throws ControllerException {
        LOGGER.info("Checking if JMS server {} is deployed", name);
        LOGGER.warn("JMS server is embedded in the JBoss application server");
        return true;
    }

    public void deployJMSServer(String name, List queues, List topics) throws ControllerException {
        LOGGER.info("Deploying JMS server {}", name);
        LOGGER.warn("JMS server is embedded in the JBoss application server");
        JMXConnector connector = null;
        try {
            connector = JMXConnectorFactory.connect(jmxServiceURL, null);
            MBeanServerConnection server = connector.getMBeanServerConnection();
            ObjectName mainDeployerMBean = new ObjectName("jboss.system:service=MainDeployer");
            LOGGER.info("Deploying JMS queues");
            for (Iterator queueIterator = queues.iterator(); queueIterator.hasNext(); ) {
                String queue = (String) queueIterator.next();
                LOGGER.info("Deploying JMS queue {}", queue);
                File file = new File(deployURL.getPath() + "/jms/" + queue + "-service.xml");
                this.jmsQueueWriteFile(file, queue);
                server.invoke(mainDeployerMBean, "deploy", new Object[]{ file.toURL() }, new String[]{ "java.net.URL" });
            }
            LOGGER.info("Deploying JMS topics");
            for (Iterator topicIterator = topics.iterator(); topicIterator.hasNext(); ) {
                String topic = (String) topicIterator.next();
                LOGGER.info("Deploying JMS topic {}", topic);
                File file = new File(deployURL.getPath() + "/jms/" + topic + "-service.xml");
                this.jmsTopicWriteFile(file, topic);
                server.invoke(mainDeployerMBean, "deploy", new Object[]{ file.toURL() }, new String[] { "java.net.URL" });
            }
        } catch (Exception e) {
            LOGGER.error("Can't deploy JMS server {}", name, e);
            throw new ControllerException("Can't deploy JMS server " + name, e);
        } finally {
            if (connector != null) {
                try {
                    connector.close();
                } catch (Exception e) {
                    // nothing to do
                }
            }
        }
    }

    public boolean isJMSServerUpToDate(String name, List queues, List topics) throws ControllerException {
        LOGGER.info("Checking if the JMS server {} is up to date", name);
        JMXConnector connector = null;
        try {
            connector = JMXConnectorFactory.connect(jmxServiceURL, null);
            MBeanServerConnection server = connector.getMBeanServerConnection();
            for (Iterator queueIterator = queues.iterator(); queueIterator.hasNext(); ) {
                String queue = (String) queueIterator.next();
                ObjectName queueMBean = new ObjectName("jboss.mq.destination:name=" + queue + ",service=Queue");
                boolean started = ((Boolean) server.getAttribute(queueMBean, "Started")).booleanValue();
                if (!started) {
                    return false;
                }
            }
            for (Iterator topicIterator = queues.iterator(); topicIterator.hasNext(); ) {
                String topic = (String) topicIterator.next();
                ObjectName topicMBean = new ObjectName("jboss.mq.destination:name=" + topic + ",service=Topic");
                boolean started = ((Boolean) server.getAttribute(topicMBean, "Started")).booleanValue();
                if (!started) {
                    return false;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Can't check if JMS server {} is up to date", name, e);
            throw new ControllerException("Can't check if JMS server " + name + " is up to date", e);
        } finally {
            if (connector != null) {
                try {
                    connector.close();
                } catch (Exception e) {
                    // nothing to do
                }
            }
        }
        return true;
    }

    public boolean updateJMSServer(String name, List queues, List topics) throws ControllerException {
        LOGGER.info("Updating JMS server {}", name);
        boolean updated = false;
        JMXConnector connector = null;
        try {
            connector = JMXConnectorFactory.connect(jmxServiceURL, null);
            MBeanServerConnection server = connector.getMBeanServerConnection();
            LOGGER.info("Checking JMS queues");
            for (Iterator queueIterator = queues.iterator(); queueIterator.hasNext(); ) {
                String queue = (String) queueIterator.next();
                boolean started = false;
                LOGGER.debug("Checking if JMS queue {} is deployed", queue);
                ObjectName queueMBean = null;
                try {
                    queueMBean = new ObjectName("jboss.mq.destination:name=" + queue + ",service=Queue");
                } catch (MalformedObjectNameException malformedObjectNameException) {
                    LOGGER.debug("JMS queue {} doesn't seem to be deployed, deploy it", queue);
                    try {
                        ObjectName mainDeployerMBean = new ObjectName("jboss.system:service=MainDeployer");
                        File file = new File(deployURL.getPath() + "/jms/" + queue + "-service.xml");
                        this.jmsQueueWriteFile(file, queue);
                        server.invoke(mainDeployerMBean, "deploy", new Object[]{ file.toURL() }, new String[]{ "java.net.URL"} );
                        updated = true;
                    } catch (Exception e) {
                        LOGGER.error("Can't deploy JMS queue {}", name, e);
                        throw new ControllerException("Can't deploy JMS queue " + name, e);
                    }
                }
                try {
                    started = ((Boolean) server.getAttribute(queueMBean, "Started")).booleanValue();
                    if (!started) {
                        server.invoke(queueMBean, "start", null, null);
                        updated = true;
                    }
                } catch (Exception e) {
                    LOGGER.error("Can't start JMS queue {}", name, e);
                    throw new ControllerException("Can't start JMS queu " + name, e);
                }
            }
            LOGGER.info("Checking JMS topics");
            for (Iterator topicIterator = topics.iterator(); topicIterator.hasNext(); ) {
                String topic = (String) topicIterator.next();
                boolean started = false;
                LOGGER.debug("Check if JMS topic {} is deployed", topic);
                ObjectName topicMBean = null;
                try {
                    topicMBean = new ObjectName("jboss.mq.destination:name=" + topic + ",service=Topic");
                } catch (MalformedObjectNameException malformedObjectNameException) {
                    LOGGER.debug("JMS topic {} doesn't seem to be deployed, deploy it", topic);
                    try {
                        ObjectName mainDeployerMBean = new ObjectName("jboss.system:service=MainDeployer");
                        File file = new File(deployURL.getPath() + "/jms/" + topic + "-service.xml");
                        this.jmsTopicWriteFile(file, topic);
                        server.invoke(mainDeployerMBean, "deploy", new Object[]{ file.toURL() }, new String[]{ "java.net.URL" });
                        updated = true;
                    } catch (Exception e) {
                        LOGGER.error("Can't deploy JMS topic {}", topic, e);
                        throw new ControllerException("Can't deploy JMS topic " + name, e);
                    }
                }
                try {
                    started = ((Boolean) server.getAttribute(topicMBean, "Started")).booleanValue();
                    if (!started) {
                        server.invoke(topicMBean, "start", null, null);
                        updated = true;
                    }
                } catch (Exception e) {
                    LOGGER.error("Can't start JMS topic {}", topic, e);
                    throw new ControllerException("Can't start JMS topic " + topic, e);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Can't connect to the JBoss application server", e);
            throw new ControllerException("Can't connect to the JBoss application server", e);
        } finally {
            if (connector != null) {
                try {
                    connector.close();
                } catch (Exception e) {
                    // nothing to do
                }
            }
        }
        return updated;
    }

    public void undeployJMSServer(String name) throws ControllerException {
        LOGGER.info("Undeploying the JMS server {}", name);
        LOGGER.warn("The JMS server is embedded in JBoss application server");
    }

    public boolean isJNDIBindingDeployed(String name) {
        LOGGER.info("Checking if JNDI binding {} is deployed", name);
        JMXConnector connector = null;
        try {
            connector = JMXConnectorFactory.connect(jmxServiceURL, null);
            MBeanServerConnection server = connector.getMBeanServerConnection();
            ObjectName jndiViewMBean = new ObjectName("jboss:service=JNDIView");
            String output = (String) server.invoke(jndiViewMBean, "list", new Object[]{ new Boolean(false) }, new String[]{ "java.lang.Boolean" });
            if (StringUtils.containsIgnoreCase(output, name)) {
                LOGGER.debug("JNDI binding {} found", name);
                return true;
            }
        } catch (Exception e) {
            LOGGER.warn("Can't check if JNDI binding {} is deployed", name, e);
            return false;
        } finally {
            if (connector != null) {
                try {
                    connector.close();
                } catch (Exception e) {
                    // nothing to do
                }
            }
        }
        return false;
    }

    public void deployJNDIBinding(String name, String jndiName, String jndiAlias, String providerUrl) throws ControllerException {
        LOGGER.info("Deploying JNDI binding {}", name);
        File file = new File(deployURL.getPath() + "/" + name + "-service.xml");
        this.jndiAliasWriteFile(file, name, jndiName, jndiAlias);
        JMXConnector connector = null;
        try {
            connector = JMXConnectorFactory.connect(jmxServiceURL, null);
            MBeanServerConnection server = connector.getMBeanServerConnection();
            ObjectName mainDeployerMBean = new ObjectName("jboss.system:service=MainDeployer");
            server.invoke(mainDeployerMBean, "deploy", new Object[]{ file.toURL() }, new String[]{ "java.net.URL" });
        } catch (Exception e ) {
            LOGGER.error("Can't deploy JNDI binding {}", name, e);
            throw new ControllerException("Can't deploy JNDI binding " + name, e);
        } finally {
            if (connector != null) {
                try {
                    connector.close();
                } catch (Exception e) {
                    // nothing to do
                }
            }
        }
    }
    
    public void undeployJNDIBinding(String name) throws ControllerException {
        LOGGER.info("Undeploying JNDI binding {}", name);
        File file = new File(deployURL.getPath() + "/" + name + "-service.xml");
        JMXConnector connector = null;
        try {
            connector = JMXConnectorFactory.connect(jmxServiceURL, null);
            MBeanServerConnection server = connector.getMBeanServerConnection();
            ObjectName namingMBean = new ObjectName("jboss:service=Naming");
            server.invoke(namingMBean, "removeAlias", new Object[]{ name }, new String[]{ "java.lang.String" });
            file.delete();
        } catch (Exception e) {
            LOGGER.error("Can't undeploy JNDI binding {}", name, e);
            throw new ControllerException("Can't undeploy JNDI binding " + name, e);
        } finally {
            if (connector != null) {
                try {
                    connector.close();
                } catch (Exception e) {
                    // nothing to do
                }
            }
        }
    }

    public boolean isJNDIBindingUpToDate(String name, String jndiName, String jndiAlias, String providerUrl) throws ControllerException {
        LOGGER.info("Checking status of JNDI binding {}", name);
        if (isJNDIBindingDeployed(name)) {
            return true;
        }
        return false;
    }

    public boolean updateJNDIBinding(String name, String jndiName, String jndiAlias, String providerUrl) throws ControllerException {
        LOGGER.info("Updating JNDI binding {}", name);
        if (isJNDIBindingDeployed(name)) {
            this.undeployJNDIBinding(name);
            this.deployJNDIBinding(name, jndiName, jndiAlias, providerUrl);
            return true;
        }
        return false; // return false either if the name space binding is always updated
    }

    public boolean isSharedLibraryDeployed(String name) throws ControllerException {
        LOGGER.warn("Shared libraries are not supported with JBoss application server");
        return true;
    }

    public void deploySharedLibrary(String name, String classpath) throws ControllerException {
        LOGGER.warn("Shared libraries are not supported with JBoss application server");
    }

    public void undeploySharedLibrary(String name) throws ControllerException {
        LOGGER.warn("Shared libraries are not supported with JBoss application server");
    }

    public boolean isSharedLibraryUpToDate(String name, String classpath) throws ControllerException {
        LOGGER.warn("Shared libraries are not supported with JBoss application server");
        return false;
    }

    public boolean updateSharedLibrary(String name, String classpath) throws ControllerException {
        LOGGER.warn("Shared libraries are not supported with JBoss application server");
        return false;
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
    private void jdbcConnectionPoolWriteFile(File file, String name, String driver, int increment, int initial, int maximal, String user, String password, String url) throws ControllerException {
        LOGGER.info("Writing the JBoss JDBC connection pool/datasource XML file");
        InputStreamReader connectionPoolTemplate = null;
        Object[] values = new Object[7];
        values[0] = name;
        values[1] = driver;
        values[2] = url;
        values[3] = user;
        values[4] = password;
        values[5] = new Integer(initial).toString();
        values[6] = new Integer(maximal).toString();
        if (StringUtils.containsIgnoreCase(driver, "xa")) {
            LOGGER.debug("XA connection pool detected");
            connectionPoolTemplate = new InputStreamReader(JBoss6Controller.class.getResourceAsStream("/jboss/template-xa-ds.xml"));
        } else {
            LOGGER.debug("Non XA connection pool detected");
            connectionPoolTemplate = new InputStreamReader(JBoss6Controller.class.getResourceAsStream("/jboss/template-ds.xml"));
        }
        String connectionPoolContent = JBoss6Controller.format(connectionPoolTemplate, values);
        try {
            FileUtils.writeStringToFile(file, connectionPoolContent);
        } catch (IOException ioException) {
            LOGGER.error("Can't write JBoss JDBC connection pool descriptor file", ioException);
            throw new ControllerException("Can't write JBoss JDBC connection pool descriptor file", ioException);
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
    private static String format(Reader templateReader, Object[] values) throws ControllerException {
        try {
            BufferedReader templateBufferedReader = new BufferedReader(templateReader);
            StringWriter writer = new StringWriter();
            BufferedWriter buffer = new BufferedWriter(writer);
            String templateLine = templateBufferedReader.readLine();
            while (templateLine != null) {
                buffer.write(MessageFormat.format(templateLine, values));
                buffer.newLine();
                templateLine = templateBufferedReader.readLine();
            }
            buffer.flush();
            return writer.toString();
        } catch (Exception e) {
            LOGGER.error("Can't format JBoss XML configuration file template", e);
            throw new ControllerException("Can't format JBoss XML configuration file template", e);
        }
    }

    /**
     * Write a JBoss JMS connection factory XML file.
     *
     * @param file the JMS connection factory <code>File</code>.
     * @param name the JMS connection factory name.
     */
    private void jmsConnectionFactoryWriteFile(File file, String name) throws ControllerException {
        LOGGER.info("Writing JBoss JMS connection factory XML file");
        LOGGER.debug("Constructing the replacement values");
        InputStreamReader connectionFactoryTemplate = new InputStreamReader(JBoss4Controller.class.getResourceAsStream("/jboss/template-jms-ds.xml"));
        Object[] values = new Object[1];
        values[0] = name;
        String connectionPoolContent = JBoss6Controller.format(connectionFactoryTemplate, values);
        try {
            FileUtils.writeStringToFile(file, connectionPoolContent);
        } catch (IOException ioException) {
            LOGGER.error("Can't write JBoss JMS connection factory descriptor file", ioException);
            throw new ControllerException("Can't write JBoss JMS connection factory descriptor file", ioException);
        }
    }

    /**
     * Write a JBoss JMS queue service file from the template.
     *
     * @param file the target file.
     * @param name the queue name.
     * @throws ControllerException in case of writing failure.
     */
    private void jmsQueueWriteFile(File file, String name) throws ControllerException {
        InputStreamReader jmsQueueTemplate = new InputStreamReader(JBoss4Controller.class.getResourceAsStream("/jboss/template-jms-queue-service.xml"));
        Object[] values = new Object[1];
        values[0] = name;
        String jmsQueueContent = JBoss6Controller.format(jmsQueueTemplate, values);
        try {
            FileUtils.writeStringToFile(file, jmsQueueContent);
        } catch (Exception e) {
            LOGGER.error("Can't write JBoss JMS queue service file", e);
            throw new ControllerException("Can't write JBoss JMS queue service file", e);
        }
    }

    /**
     * Write a JBoss JMS topic service file from the template.
     *
     * @param file the target file.
     * @param name the topic name.
     * @throws ControllerException in case of writing failure.
     */
    private void jmsTopicWriteFile(File file, String name) throws ControllerException {
        InputStreamReader jmsTopicTemplate = new InputStreamReader(JBoss4Controller.class.getResourceAsStream("/jboss/template-jms-topic-service.xml"));
        Object[] values = new Object[1];
        values[0] = name;
        String jmsTopicContent = JBoss6Controller.format(jmsTopicTemplate, values);
        try {
            FileUtils.writeStringToFile(file, jmsTopicContent);
        } catch (Exception e) {
            LOGGER.error("Can't write JBoss JMS topic service file.", e);
            throw new ControllerException("Can't write JBoss JMS topic service file", e);
        }
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
    private void jndiAliasWriteFile(File file, String name, String from, String to) throws ControllerException {
        InputStreamReader jmsQueueTemplate = new InputStreamReader(JBoss4Controller.class.getResourceAsStream("/jboss/template-jndi-alias-service.xml"));
        Object[] values = new Object[3];
        values[0] = name;
        values[1] = from;
        values[2] = to;
        String jndiAliasContent = JBoss6Controller.format(jmsQueueTemplate, values);
        try {
            FileUtils.writeStringToFile(file, jndiAliasContent);
        } catch (Exception e) {
            LOGGER.error("Can't write JBoss JNDI binding service file", e);
            throw new ControllerException("Can't write JBoss JNDI binding service file", e);
        }
    }   

}
