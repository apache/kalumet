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
 * Represent the <code>jdbcdatasource</code> tag in the Kalumet configuration DOM.
 */
public class JDBCDataSource
  implements Serializable, Cloneable, Comparable
{

  private static final long serialVersionUID = -6850328756411047364L;

  private String name;

  private String pool;

  private boolean active;

  private boolean blocker;

  public JDBCDataSource()
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

  public String getPool()
  {
    return this.pool;
  }

  public void setPool( String pool )
  {
    this.pool = pool;
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
    JDBCDataSource clone = new JDBCDataSource();
    clone.setName( this.getName() );
    clone.setPool( this.getPool() );
    clone.setActive( this.isActive() );
    clone.setBlocker( this.isBlocker() );
    return clone;
  }

  /**
   * Transforms the <code>JDBCDataSource</code> POJO to a DOM element.
   *
   * @param document the DOM document.
   * @return the DOM element.
   */
  protected Element toDOMElement( CoreDocumentImpl document )
  {
    ElementImpl element = new ElementImpl( document, "jdbcdatasource" );
    element.setAttribute( "name", this.getName() );
    element.setAttribute( "pool", this.getPool() );
    element.setAttribute( "active", new Boolean( this.isActive() ).toString() );
    element.setAttribute( "blocker", new Boolean( this.isBlocker() ).toString() );
    return element;
  }

  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo( Object anotherDataSource )
  {
    return this.getName().compareTo( ( (JDBCDataSource) anotherDataSource ).getName() );
  }

}