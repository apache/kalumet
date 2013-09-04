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

import org.apache.kalumet.model.Environment;
import org.apache.kalumet.model.JEEApplicationServer;
import org.apache.kalumet.utils.VariableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;

/**
 * Factory to get a <code>JEEApplicationServerController</code>.
 */
public class JEEApplicationServerControllerFactory
{

  private final static transient Logger LOGGER =
    LoggerFactory.getLogger( JEEApplicationServerControllerFactory.class );

  public static JEEApplicationServerController getController( Environment environment, JEEApplicationServer server )
    throws ControllerException
  {
    LOGGER.debug( "Connecting to {}", VariableUtils.replace( server.getJmxurl(), environment.getVariables() ) );
    String jmxUrl = VariableUtils.replace( server.getJmxurl(), environment.getVariables() );
    String adminUser = VariableUtils.replace( server.getAdminuser(), environment.getVariables() );
    String adminPassword = VariableUtils.replace( server.getAdminpassword(), environment.getVariables() );
    JEEApplicationServerController controller = null;
    try
    {
      Class controllerClass = Class.forName( server.getClassname() );
      Constructor controllerConstructor = controllerClass.getConstructor(
        new Class[]{ String.class, String.class, String.class, String.class, Boolean.class } );
      controller = (JEEApplicationServerController) controllerConstructor.newInstance(
        new Object[]{ jmxUrl, adminUser, adminPassword, server.getName(),
          new Boolean( environment.getJEEApplicationServers().isCluster() ) } );
    }
    catch ( Exception e )
    {
      LOGGER.error( "Can't initialize controller", e );
      if ( e != null )
      {
        throw new ControllerException( "Can't initialize controller", e );
      }
      else
      {
        throw new ControllerException(
          "Can't initialize controller. Check if the JEE application server libraries are present in the agent classpath and check the agent log" );
      }
    }
    return controller;

  }

}
