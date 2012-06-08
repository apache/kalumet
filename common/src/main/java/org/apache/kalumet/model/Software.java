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

/**
 * Represent the <code>software</code> tag in the Kalumet DOM.
 */
public class Software
  implements Serializable, Cloneable, Comparable
{

  private static final long serialVersionUID = 1464721106305749412L;

  private String name;

  private String uri;

  private String agent;

  private boolean active;

  private boolean blocker;

  private boolean beforej2ee;

  private LinkedList updatePlan;

  public Software()
  {
    this.updatePlan = new LinkedList();
  }

  public String getName()
  {
    return name;
  }

  public void setName( String name )
  {
    this.name = name;
  }

  public String getUri()
  {
    return uri;
  }

  public void setUri( String uri )
  {
    this.uri = uri;
  }

  public String getAgent()
  {
    return agent;
  }

  public void setAgent( String agent )
  {
    this.agent = agent;
  }

  public boolean isActive()
  {
    return active;
  }

  public void setActive( boolean active )
  {
    this.active = active;
  }

  public boolean isBlocker()
  {
    return blocker;
  }

  public void setBlocker( boolean blocker )
  {
    this.blocker = blocker;
  }

  public boolean isBeforej2ee()
  {
    return beforej2ee;
  }

  public void setBeforej2ee( boolean beforej2ee )
  {
    this.beforej2ee = beforej2ee;
  }

  public LinkedList getUpdatePlan()
  {
    return updatePlan;
  }

  public void setUpdatePlan( LinkedList updatePlan )
  {
    this.updatePlan = updatePlan;
  }

  /**
   * Get the software component with the given name.
   *
   * @param name the software component name.
   * @return the component <code>Object</code> or <code>null</code> if not found.
   */
  public Object getComponent( String name )
  {
    Object component = this.getLocation( name );
    if ( component != null )
    {
      return component;
    }
    component = this.getCommand( name );
    if ( component != null )
    {
      return component;
    }
    component = this.getConfigurationFile( name );
    if ( component != null )
    {
      return component;
    }
    component = this.getDatabase( name );
    if ( component != null )
    {
      return component;
    }
    return null;
  }

  /**
   * Add a system command into the software update plan.
   *
   * @param command the system command
   */
  public void addCommand( Command command )
    throws ModelObjectAlreadyExistsException
  {
    if ( this.getComponent( command.getName() ) != null )
    {
      throw new ModelObjectAlreadyExistsException( "Software component " + command.getName() + " already exists." );
    }
    updatePlan.add( command );
  }

  /**
   * Get the command identified by <code>name</code> in the software update plan..
   *
   * @param name the command name.
   * @return the <code>Command</code> or <code>null</code> if not found.
   */
  public Command getCommand( String name )
  {
    for ( Iterator updatePlanIterator = updatePlan.iterator(); updatePlanIterator.hasNext(); )
    {
      Object item = updatePlanIterator.next();
      if ( item instanceof Command )
      {
        Command command = (Command) item;
        if ( command.getName().equals( name ) )
        {
          return command;
        }
      }
    }
    return null;
  }

  /**
   * Add a <code>Location</code> into the software update plan.
   *
   * @param location the files/directories location
   */
  public void addLocation( Location location )
    throws ModelObjectAlreadyExistsException
  {
    if ( this.getComponent( location.getName() ) != null )
    {
      throw new ModelObjectAlreadyExistsException( "Software component " + location.getName() + " already exists." );
    }
    updatePlan.add( location );
  }

  /**
   * Get the <code>Location</code> identified by <code>name</code> in the software update plan..
   *
   * @param name the location name.
   * @return the <code>Location</code> or <code>null</code> if not found.
   */
  public Location getLocation( String name )
  {
    for ( Iterator updatePlanIterator = updatePlan.iterator(); updatePlanIterator.hasNext(); )
    {
      Object item = updatePlanIterator.next();
      if ( item instanceof Location )
      {
        Location location = (Location) item;
        if ( location.getName().equals( name ) )
        {
          return location;
        }
      }
    }
    return null;
  }

  /**
   * Add a <code>ConfigurationFile</code> into the software update plan.
   *
   * @param configurationFile the configuration file.
   */
  public void addConfigurationFile( ConfigurationFile configurationFile )
    throws ModelObjectAlreadyExistsException
  {
    if ( this.getComponent( configurationFile.getName() ) != null )
    {
      throw new ModelObjectAlreadyExistsException(
        "Software component " + configurationFile.getName() + " already exists." );
    }
    updatePlan.add( configurationFile );
  }

  /**
   * Get the <code>ConfigurationFile</code> identified by <code>name</code> in the software update plan.
   *
   * @param name the configuration file name.
   * @return the <code>ConfigurationFile</code> or <code>null</code> if not found.
   */
  public ConfigurationFile getConfigurationFile( String name )
  {
    for ( Iterator updatePlanIterator = updatePlan.iterator(); updatePlanIterator.hasNext(); )
    {
      Object item = updatePlanIterator.next();
      if ( item instanceof ConfigurationFile )
      {
        ConfigurationFile configurationFile = (ConfigurationFile) item;
        if ( configurationFile.getName().equals( name ) )
        {
          return configurationFile;
        }
      }
    }
    return null;
  }

  /**
   * Add a database (including SQL scripts) into the software update plan.
   *
   * @param database the database
   */
  public void addDatabase( Database database )
    throws ModelObjectAlreadyExistsException
  {
    if ( this.getComponent( database.getName() ) != null )
    {
      throw new ModelObjectAlreadyExistsException( "Software component " + database.getName() + " already exists." );
    }
    updatePlan.add( database );
  }

  /**
   * Get the <code>Database</code> identified by <code>name</code> in the software update plan.
   *
   * @param name the database name.
   * @return the <code>Database</code> or <code>null</code> if not found.
   */
  public Database getDatabase( String name )
  {
    for ( Iterator updatePlanIterator = updatePlan.iterator(); updatePlanIterator.hasNext(); )
    {
      Object item = updatePlanIterator.next();
      if ( item instanceof Database )
      {
        Database database = (Database) item;
        if ( database.getName().equals( name ) )
        {
          return database;
        }
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
    Software clone = new Software();
    clone.setName( this.getName() );
    clone.setUri( this.getUri() );
    clone.setActive( this.isActive() );
    clone.setBlocker( this.isBlocker() );
    clone.setBeforej2ee( this.isBeforej2ee() );
    for ( Iterator updatePlanIterator = this.getUpdatePlan().iterator(); updatePlanIterator.hasNext(); )
    {
      Object item = updatePlanIterator.next();
      if ( item instanceof Command )
      {
        clone.getUpdatePlan().add( ( (Command) item ).clone() );
      }
      if ( item instanceof Location )
      {
        clone.getUpdatePlan().add( ( (Location) item ).clone() );
      }
      if ( item instanceof ConfigurationFile )
      {
        clone.getUpdatePlan().add( ( (ConfigurationFile) item ).clone() );
      }
      if ( item instanceof Database )
      {
        clone.getUpdatePlan().add( ( (Database) item ).clone() );
      }
    }
    return clone;
  }

  /**
   * Transform a <code>software</code> into a DOM element.
   *
   * @param document the DOM document.
   * @return the DOM element.
   */
  protected Element toDOMElement( CoreDocumentImpl document )
  {
    ElementImpl element = new ElementImpl( document, "software" );
    element.setAttribute( "name", this.getName() );
    element.setAttribute( "uri", this.getUri() );
    element.setAttribute( "agent", this.getAgent() );
    element.setAttribute( "active", new Boolean( this.isActive() ).toString() );
    element.setAttribute( "blocker", new Boolean( this.isBlocker() ).toString() );
    element.setAttribute( "beforej2ee", new Boolean( this.isBeforej2ee() ).toString() );
    ElementImpl updateplan = new ElementImpl( document, "updateplan" );
    element.appendChild( updateplan );
    for ( Iterator updatePlanIterator = this.getUpdatePlan().iterator(); updatePlanIterator.hasNext(); )
    {
      Object item = updatePlanIterator.next();
      if ( item instanceof Command )
      {
        updateplan.appendChild( ( (Command) item ).toDOMElement( document ) );
      }
      if ( item instanceof Location )
      {
        updateplan.appendChild( ( (Location) item ).toDOMElement( document ) );
      }
      if ( item instanceof ConfigurationFile )
      {
        updateplan.appendChild( ( (ConfigurationFile) item ).toDOMElement( document ) );
      }
      if ( item instanceof Database )
      {
        updateplan.appendChild( ( (Database) item ).toDOMElement( document ) );
      }
    }
    return element;
  }

  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo( Object anotherSoftware )
  {
    return this.getName().compareTo( ( (Software) anotherSoftware ).getName() );
  }

}