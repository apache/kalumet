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

import org.apache.xerces.dom.CoreDocumentImpl;
import org.apache.xerces.dom.ElementImpl;
import org.w3c.dom.Element;

import java.io.Serializable;

/**
 * Represent the <code>jdbcconnectionpool</code> tag in the Kalumet XML
 * configuration file.
 */
public class JDBCConnectionPool
  implements Serializable, Cloneable, Comparable
{

  private static final long serialVersionUID = 8052573645587249685L;

  private String name;

  private String driver;

  private String helperclass;

  private int increment;

  private int initial;

  private int maximal;

  private String user;

  private String password;

  private String url;

  private String classpath;

  private boolean active;

  private boolean blocker;

  public JDBCConnectionPool()
  {
  }

  public String getName()
  {
    return this.name;
  }

  public void setName( String name )
  {
    this.name = name;
  }

  public String getDriver()
  {
    return this.driver;
  }

  public void setDriver( String driver )
  {
    this.driver = driver;
  }

  public String getHelperclass()
  {
    return this.helperclass;
  }

  public void setHelperclass( String helperclass )
  {
    this.helperclass = helperclass;
  }

  public int getIncrement()
  {
    return this.increment;
  }

  public void setIncrement( int increment )
  {
    this.increment = increment;
  }

  public int getInitial()
  {
    return this.initial;
  }

  public void setInitial( int initial )
  {
    this.initial = initial;
  }

  public int getMaximal()
  {
    return this.maximal;
  }

  public void setMaximal( int maximal )
  {
    this.maximal = maximal;
  }

  public String getUser()
  {
    return this.user;
  }

  public void setUser( String user )
  {
    this.user = user;
  }

  public String getPassword()
  {
    return this.password;
  }

  public void setPassword( String password )
  {
    this.password = password;
  }

  public String getUrl()
  {
    return this.url;
  }

  public void setUrl( String url )
  {
    this.url = url;
  }

  public String getClasspath()
  {
    return this.classpath;
  }

  public void setClasspath( String classpath )
  {
    this.classpath = classpath;
  }

  public boolean isActive()
  {
    return this.active;
  }

  public void setActive( boolean active )
  {
    this.active = active;
  }

  public boolean isBlocker()
  {
    return this.blocker;
  }

  public void setBlocker( boolean blocker )
  {
    this.blocker = blocker;
  }

  /**
   * @see java.lang.Object#clone()
   */
  public Object clone()
    throws CloneNotSupportedException
  {
    JDBCConnectionPool clone = new JDBCConnectionPool();
    clone.setName( this.getName() );
    clone.setDriver( this.getDriver() );
    clone.setHelperclass( this.getHelperclass() );
    clone.setIncrement( this.getIncrement() );
    clone.setInitial( this.getInitial() );
    clone.setMaximal( this.getMaximal() );
    clone.setUser( this.getUser() );
    clone.setPassword( this.getPassword() );
    clone.setUrl( this.getUrl() );
    clone.setClasspath( this.getClasspath() );
    clone.setActive( this.isActive() );
    clone.setBlocker( this.isBlocker() );
    return clone;
  }

  /**
   * Transform the <code>JDBCConnectionPool</code> POJO to a DOM element.
   *
   * @param document the core XML document.
   * @return the DOM element.
   */
  protected Element toDOMElement( CoreDocumentImpl document )
  {
    ElementImpl element = new ElementImpl( document, "jdbcconnectionpool" );
    element.setAttribute( "name", this.getName() );
    element.setAttribute( "driver", this.getDriver() );
    element.setAttribute( "helperclass", this.getHelperclass() );
    element.setAttribute( "increment", new Integer( this.getIncrement() ).toString() );
    element.setAttribute( "initial", new Integer( this.getInitial() ).toString() );
    element.setAttribute( "maximal", new Integer( this.getMaximal() ).toString() );
    element.setAttribute( "user", this.getUser() );
    element.setAttribute( "password", this.getPassword() );
    element.setAttribute( "url", this.getUrl() );
    element.setAttribute( "classpath", this.getClasspath() );
    element.setAttribute( "active", new Boolean( this.isActive() ).toString() );
    element.setAttribute( "blocker", new Boolean( this.isActive() ).toString() );
    return element;
  }

  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo( Object anotherConnectionPool )
  {
    return this.getName().compareTo( ( (JDBCConnectionPool) anotherConnectionPool ).getName() );
  }

}