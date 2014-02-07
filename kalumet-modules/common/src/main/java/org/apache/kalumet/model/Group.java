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
 * Represent the <code>group</code> tag in the Kalumet configuration DOM.
 */
public class Group
    implements Serializable, Cloneable, Comparable
{

    private static final long serialVersionUID = 5188524193501221530L;

    private String id;

    private String name;

    private LinkedList users;

    public Group()
    {
        this.users = new LinkedList();
    }

    public String getId()
    {
        return this.id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public String getName()
    {
        return this.name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    /**
     * Add a new <code>User</code> in the <code>Group</code> container.
     *
     * @param user the <code>User</code> to add.
     */
    public void addUser( User user )
        throws ModelObjectAlreadyExistsException
    {
        if ( this.getUser( user.getId() ) != null )
        {
            throw new ModelObjectAlreadyExistsException( "User id already exists in group." );
        }
        this.users.add( user );
    }

    /**
     * Get the <code>User</code> list in the <code>Group</code> container.
     *
     * @return the <code>User</code> list.
     */
    public List getUsers()
    {
        return this.users;
    }

    /**
     * Set the <code>User</code> list in the <code>Group</code>
     * container.
     *
     * @param users the new <code>User</code> list.
     */
    public void setUsers( LinkedList users )
    {
        this.users = users;
    }

    /**
     * Get a <code>User</code> identified by a given id in the
     * <code>Group</code> container;
     *
     * @param id the <code>User</code> id.
     * @return the found <code>User</code> or null if not found.
     */
    public User getUser( String id )
    {
        for ( Iterator userIterator = this.getUsers().iterator(); userIterator.hasNext(); )
        {
            User user = (User) userIterator.next();
            if ( user.getId().equals( id ) )
            {
                return user;
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
        Group clone = new Group();
        clone.setId( this.getId() );
        clone.setName( this.getName() );
        for ( Iterator userIterator = this.users.iterator(); userIterator.hasNext(); )
        {
            User user = (User) userIterator.next();
            clone.users.add( (User) user.clone() );
        }
        return clone;
    }

    /**
     * Transform the <code>Group</code> POJO to a DOM element.
     *
     * @param document the DOM document.
     * @return the DOM element.
     */
    protected Element toDOMElement( CoreDocumentImpl document )
    {
        ElementImpl element = new ElementImpl( document, "group" );
        element.setAttribute( "id", this.getId() );
        element.setAttribute( "name", this.getName() );
        // users element
        ElementImpl users = new ElementImpl( document, "users" );
        // add user in the users element
        for ( Iterator userIterator = this.getUsers().iterator(); userIterator.hasNext(); )
        {
            User user = (User) userIterator.next();
            users.appendChild( user.toDOMElement( document ) );
        }
        // add users to group element
        element.appendChild( users );
        return element;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo( Object anotherGroup )
    {
        return this.getId().compareTo( ( (Group) anotherGroup ).getId() );
    }

}