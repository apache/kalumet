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

import org.apache.kalumet.KalumetException;
import org.apache.xerces.dom.CoreDocumentImpl;
import org.apache.xerces.dom.ElementImpl;
import org.w3c.dom.Element;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Represent the <code>security</code> tag in the Kalumet configuration DOM.
 */
public class Security
    implements Serializable, Cloneable
{

    private static final long serialVersionUID = 1323976117053191122L;

    private LinkedList users;

    private LinkedList groups;

    public Security()
    {
        this.users = new LinkedList();
        this.groups = new LinkedList();
    }

    /**
     * Add a new <code>User</code> in the <code>Security</code> container.
     *
     * @param user the <code>User</code> to add.
     */
    public void addUser( User user )
        throws ModelObjectAlreadyExistsException
    {
        if ( this.getUser( user.getId() ) != null )
        {
            throw new ModelObjectAlreadyExistsException( "User id already exists in the security user configuration." );
        }
        this.users.add( user );
    }

    /**
     * Get the <code>User</code> list in the <code>Security</code>
     * container.
     *
     * @return the <code>User</code> list.
     */
    public List getUsers()
    {
        return this.users;
    }

    /**
     * Set the <code>User</code> list in the <code>Security</code>
     * container.
     *
     * @param users the new <code>User</code> list.
     */
    public void setUsers( LinkedList users )
    {
        this.users = users;
    }

    /**
     * Get the <code>User</code> identified by a given id in the
     * <code>Security</code> container.
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
     * Identify a user.
     *
     * @param id       the user id.
     * @param password the user password (in clear).
     * @return true if the user is identified, false else.
     */
    public boolean identifyUser( String id, String password )
        throws KalumetException
    {
        String encryptedPassword = User.md5PasswordCrypt( password );
        User user = this.getUser( id );
        if ( user == null )
        {
            return false;
        }
        if ( !user.getPassword().equals( encryptedPassword ) )
        {
            return false;
        }
        return true;
    }

    /**
     * Add a new <code>Group</code> in the <code>Security</code> container.
     *
     * @param group the <code>Group</code> to add.
     */
    public void addGroup( Group group )
        throws ModelObjectAlreadyExistsException
    {
        if ( this.getGroup( group.getId() ) != null )
        {
            throw new ModelObjectAlreadyExistsException( "Group id already exists in security definition." );
        }
        this.groups.add( group );
    }

    /**
     * Get the <code>Group</code> list in the <code>Security</code> container.
     *
     * @return the <code>Group</code> list.
     */
    public List getGroups()
    {
        return this.groups;
    }

    /**
     * Set the <code>Group</code> list in the <code>Security</code>
     * container.
     *
     * @param groups the new <code>Group</code>list.
     */
    public void setGroups( LinkedList groups )
    {
        this.groups = groups;
    }

    /**
     * Get a <code>Group</code> identified by a given id in the
     * <code>Security</code> container.
     *
     * @param id the <code>Group</code> id.
     * @return the found <code>Group</code> or null if not found.
     */
    public Group getGroup( String id )
    {
        for ( Iterator groupIterator = this.getGroups().iterator(); groupIterator.hasNext(); )
        {
            Group group = (Group) groupIterator.next();
            if ( group.getId().equals( id ) )
            {
                return group;
            }
        }
        return null;
    }

    /**
     * Get all groups of a user.
     *
     * @param userid the user id.
     * @return the user groups.
     */
    public List getUserGroups( String userid )
    {
        if ( userid.equals( "admin" ) )
        {
            return this.getGroups();
        }
        LinkedList userGroups = new LinkedList();
        for ( Iterator groupIterator = this.getGroups().iterator(); groupIterator.hasNext(); )
        {
            Group group = (Group) groupIterator.next();
            if ( group.getUser( userid ) != null )
            {
                userGroups.add( group );
            }
        }
        return userGroups;
    }

    /**
     * Check user in group.
     *
     * @param userid  the user id.
     * @param groupid the group id.
     * @return true if the user is a member of the group, false else.
     */
    public boolean checkUserInGroup( String userid, String groupid )
    {
        if ( userid.equals( "admin" ) )
        {
            return true;
        }
        for ( Iterator userGroupIterator = this.getUserGroups( userid ).iterator(); userGroupIterator.hasNext(); )
        {
            Group group = (Group) userGroupIterator.next();
            if ( group.getId().equals( groupid ) )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a user has an access to a given environment.
     *
     * @param environment the <code>Environment</code>.
     * @param userid      the <code>User</code> id.
     * @param property    the <code>Access</code> property.
     * @return true if the user has access to the environment, false else.
     */
    public boolean checkEnvironmentUserAccess( Environment environment, String userid, String property )
    {
        if ( this.checkUserInGroup( userid, "admin" ) )
        {
            return true;
        }
        for ( Iterator accessIterator = environment.getAccesses().iterator(); accessIterator.hasNext(); )
        {
            Access access = (Access) accessIterator.next();
            if ( property == null )
            {
                if ( this.checkUserInGroup( userid, access.getGroup() ) )
                {
                    return true;
                }
            }
            else
            {
                if ( access.getProperty( property ) != null && access.getProperty( property ).getValue().equals(
                    "true" ) )
                {
                    if ( this.checkUserInGroup( userid, access.getGroup() ) )
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * @see java.lang.Object#clone()
     */
    public Object clone()
        throws CloneNotSupportedException
    {
        Security clone = new Security();
        for ( Iterator userIterator = this.users.iterator(); userIterator.hasNext(); )
        {
            User user = (User) userIterator.next();
            clone.users.add( (User) user.clone() );
        }
        for ( Iterator groupIterator = this.groups.iterator(); groupIterator.hasNext(); )
        {
            Group group = (Group) groupIterator.next();
            clone.groups.add( (Group) group.clone() );
        }
        return clone;
    }

    /**
     * Transform the <code>Security</code> POJO to a DOM element.
     *
     * @param document the DOM document.
     * @return the DOM element.
     */
    protected Element toDOMElement( CoreDocumentImpl document )
    {
        ElementImpl element = new ElementImpl( document, "security" );
        // users element
        ElementImpl users = new ElementImpl( document, "users" );
        // add user in the users container
        for ( Iterator userIterator = this.getUsers().iterator(); userIterator.hasNext(); )
        {
            User user = (User) userIterator.next();
            users.appendChild( user.toDOMElement( document ) );
        }
        // add users in security
        element.appendChild( users );
        // groups element
        ElementImpl groups = new ElementImpl( document, "groups" );
        // add group in the groups container
        for ( Iterator groupIterator = this.getGroups().iterator(); groupIterator.hasNext(); )
        {
            Group group = (Group) groupIterator.next();
            groups.appendChild( group.toDOMElement( document ) );
        }
        // add groups in security
        element.appendChild( groups );
        return element;
    }

}