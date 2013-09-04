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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents the <code>access</code> tag in the Kalumet XML DOM.
 */
public class Access
  implements Serializable, Cloneable
{

  private static final long serialVersionUID = -3233389055334111823L;

  private String group;

  private List properties;

  public Access()
  {
    properties = new LinkedList();
  }

  public String getGroup()
  {
    return this.group;
  }

  public void setGroup( String group )
  {
    this.group = group;
  }

  public List getProperties()
  {
    return properties;
  }

  public void setProperties( List properties )
  {
    this.properties = properties;
  }

  /**
   * Add a new <code>Property</code> in the <code>Access</code>.
   *
   * @param property the <code>Property</code> to add.
   */
  public void addProperty( Property property )
    throws ModelObjectAlreadyExistsException
  {
    if ( this.getProperty( property.getName() ) != null )
    {
      throw new ModelObjectAlreadyExistsException( "Property name already exists in access." );
    }
    this.properties.add( property );
  }

  /**
   * Get the <code>Property</code> identified by a given name in the <code>Access</code>.
   *
   * @param name the <code>Property</code> name.
   * @return the <code>Property</code> found or null if no <code>Property</code> found.
   */
  public Property getProperty( String name )
  {
    for ( Iterator propertyIterator = this.properties.iterator(); propertyIterator.hasNext(); )
    {
      Property property = (Property) propertyIterator.next();
      if ( property.getName().equals( name ) )
      {
        return property;
      }
    }
    return null;
  }

  /**
   * @see java.lang.Object#clone()
   */
  public Object clone()
    throws CloneNotSupportedException
  {
    Access clone = new Access();
    clone.setGroup( this.getGroup() );
    for ( Iterator propertyIterator = this.getProperties().iterator(); propertyIterator.hasNext(); )
    {
      Property property = (Property) propertyIterator.next();
      clone.properties.add( (Property) property.clone() );
    }
    return clone;
  }

  /**
   * Transforms the <code>Access</code> POJO into a DOM element.
   *
   * @param document the core DOM document.
   * @return the DOM element.
   */
  protected Element toDOMElement( CoreDocumentImpl document )
  {
    ElementImpl element = new ElementImpl( document, "access" );
    element.setAttribute( "group", this.getGroup() );
    // properties
    ElementImpl properties = new ElementImpl( document, "properties" );
    for ( Iterator propertyIterator = this.getProperties().iterator(); propertyIterator.hasNext(); )
    {
      Property property = (Property) propertyIterator.next();
      properties.appendChild( property.toDOMElement( document ) );
    }
    element.appendChild( properties );
    return element;
  }

}
