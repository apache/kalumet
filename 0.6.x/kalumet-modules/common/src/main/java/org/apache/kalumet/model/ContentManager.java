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
 * Represent the <code>contentmanager</code> tag in the Kalumet DOM.
 */
public class ContentManager
  implements Serializable, Cloneable, Comparable
{

  private static final long serialVersionUID = -6772514401403559365L;

  private String name;

  private String classname;

  private boolean active;

  private boolean blocker;

  private String agent;

  private LinkedList properties;

  public ContentManager()
  {
    this.properties = new LinkedList();
  }

  public String getName()
  {
    return this.name;
  }

  public void setName( String name )
  {
    this.name = name;
  }

  public String getClassname()
  {
    return this.classname;
  }

  public void setClassname( String classname )
  {
    this.classname = classname;
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

  public String getAgent()
  {
    return agent;
  }

  public void setAgent( String agent )
  {
    this.agent = agent;
  }

  public void setBlocker( boolean blocker )
  {
    this.blocker = blocker;
  }

  /**
   * Add a new <code>Property</code> in the <code>ContentManager</code>
   * properties container.
   *
   * @param property the <code>Property</code> to add.
   */
  public void addProperty( Property property )
    throws ModelObjectAlreadyExistsException
  {
    if ( this.getProperty( property.getName() ) != null )
    {
      throw new ModelObjectAlreadyExistsException( "Property name already exists in content manager." );
    }
    this.properties.add( property );
  }

  /**
   * Get the <code>Property</code> list in the <code>ContentManager</code>
   * properties container.
   *
   * @return the <code>Property</code> list.
   */
  public List getProperties()
  {
    return this.properties;
  }

  /**
   * Set the <code>Property</code> list in the
   * <code>ContentManager</code> properties container.
   *
   * @param properties the new <code>Property</code> list.
   */
  public void setProperties( LinkedList properties )
  {
    this.properties = properties;
  }

  /**
   * Get the <code>Property</code> identified by a given name in the
   * <code>ContentManager</code> properties container.
   *
   * @param name the <code>Property</code> name.
   * @return the <code>Property</code> found or null if no <code>Property</code> found.
   */
  public Property getProperty( String name )
  {
    for ( Iterator propertyIterator = this.getProperties().iterator(); propertyIterator.hasNext(); )
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
    ContentManager clone = new ContentManager();
    clone.setName( this.getName() );
    clone.setClassname( this.getClassname() );
    clone.setActive( this.isActive() );
    clone.setBlocker( this.isBlocker() );
    clone.setAgent( this.getAgent() );
    for ( Iterator propertyIterator = this.properties.iterator(); propertyIterator.hasNext(); )
    {
      Property property = (Property) propertyIterator.next();
      clone.properties.add( (Property) property.clone() );
    }
    return clone;
  }

  /**
   * Transform the <code>ContentManager</code> POJO to a DOM element.
   *
   * @param document the core XML document.
   * @return the DOM element.
   */
  protected Element toDOMElement( CoreDocumentImpl document )
  {
    ElementImpl element = new ElementImpl( document, "contentmanager" );
    element.setAttribute( "name", this.getName() );
    element.setAttribute( "classname", this.getClassname() );
    element.setAttribute( "active", new Boolean( this.isActive() ).toString() );
    element.setAttribute( "blocker", new Boolean( this.isBlocker() ).toString() );
    element.setAttribute( "agent", this.getAgent() );
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

  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo( Object anotherContentManager )
  {
    return this.getName().compareTo( ( (ContentManager) anotherContentManager ).getName() );
  }

}