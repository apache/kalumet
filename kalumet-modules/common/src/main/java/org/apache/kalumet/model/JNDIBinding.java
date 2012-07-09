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
 * Represent the <code>jndibinding</code> tag in the Kalumet configuration DOM.
 */
public class JNDIBinding
  implements Serializable, Cloneable, Comparable
{

  private static final long serialVersionUID = -2336476111740231781L;

  private String name;

  private String jndiname;

  private String jndialias;

  private String providerurl;

  private boolean active;

  private boolean blocker;

  public JNDIBinding()
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

  public String getJndiname()
  {
    return this.jndiname;
  }

  public void setJndiname( String jndiname )
  {
    this.jndiname = jndiname;
  }

  public String getJndialias()
  {
    return this.jndialias;
  }

  public void setJndialias( String jndialias )
  {
    this.jndialias = jndialias;
  }

  public String getProviderurl()
  {
    return this.providerurl;
  }

  public void setProviderurl( String providerurl )
  {
    this.providerurl = providerurl;
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
    JNDIBinding clone = new JNDIBinding();
    clone.setName( this.getName() );
    clone.setJndiname( this.getJndiname() );
    clone.setJndialias( this.getJndialias() );
    clone.setProviderurl( this.getProviderurl() );
    clone.setActive( this.isActive() );
    clone.setBlocker( this.isBlocker() );
    return clone;
  }

  /**
   * Transform the <code>JNDIBinding</code> POJO to a DOM element.
   *
   * @param document the DOM document.
   * @return the DOM element.
   */
  protected Element toDOMElement( CoreDocumentImpl document )
  {
    ElementImpl element = new ElementImpl( document, "jndibinding" );
    element.setAttribute( "name", this.getName() );
    element.setAttribute( "jndiname", this.getJndiname() );
    element.setAttribute( "jndialias", this.getJndialias() );
    element.setAttribute( "providerurl", this.getProviderurl() );
    element.setAttribute( "active", new Boolean( this.isActive() ).toString() );
    element.setAttribute( "blocker", new Boolean( this.isBlocker() ).toString() );
    return element;
  }

  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo( Object anotherNameSpaceBinding )
  {
    return this.getName().compareTo( ( (JNDIBinding) anotherNameSpaceBinding ).getName() );
  }

}