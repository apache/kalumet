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
import java.security.MessageDigest;

/**
 * Represent the <code>user</code> tag in the Kalumet configuration DOM.
 */
public class User
    implements Serializable, Cloneable, Comparable
{

    private static final long serialVersionUID = -1628759131745053332L;

    private String id;

    private String name;

    private String email;

    private String password;

    public User()
    {
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

    public String getEmail()
    {
        return this.email;
    }

    public void setEmail( String email )
    {
        this.email = email;
    }

    /**
     * <b>Warning : this method returns the encrypted password</b>
     */
    public String getPassword()
    {
        return this.password;
    }

    /**
     * <b>Warning : this method is expecting for an encrypted password</b>
     */
    public void setPassword( String password )
    {
        this.password = password;
    }

    /**
     * Encrypts MD5 of a given password.
     *
     * @param password the password to encrypt.
     * @return the MD5 encrypted password.
     */
    public static String md5PasswordCrypt( String password )
        throws KalumetException
    {
        try
        {
            byte[] hash = MessageDigest.getInstance( "MD5" ).digest( password.getBytes() );
            StringBuffer hashString = new StringBuffer();
            for ( int i = 0; i < hash.length; i++ )
            {
                String hex = Integer.toHexString( hash[i] );
                if ( hex.length() == 1 )
                {
                    hashString.append( '0' );
                    hashString.append( hex.charAt( hex.length() - 1 ) );
                }
                else
                {
                    hashString.append( hex.substring( hex.length() - 2 ) );
                }
            }
            return hashString.toString();
        }
        catch ( Exception e )
        {
            throw new KalumetException( "Cant' crypt password.", e );
        }
    }

    /**
     * Check if a given password match the <code>User</code> password.
     *
     * @param password the given password.
     * @return true of the password match the <code>User</code> password, false else.
     */
    public boolean checkPassword( String password )
        throws KalumetException
    {
        String crypt = User.md5PasswordCrypt( password );
        if ( this.getPassword().equals( crypt ) )
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * @see java.lang.Object#clone()
     */
    public Object clone()
        throws CloneNotSupportedException
    {
        User clone = new User();
        clone.setId( this.getId() );
        clone.setName( this.getName() );
        clone.setEmail( this.getEmail() );
        clone.setPassword( this.getPassword() );
        return clone;
    }

    /**
     * Transform the <code>User</code> POJO to a DOM element.
     *
     * @param document the DOM document.
     * @return the DOM element.
     */
    protected Element toDOMElement( CoreDocumentImpl document )
    {
        ElementImpl element = new ElementImpl( document, "user" );
        element.setAttribute( "id", this.getId() );
        element.setAttribute( "name", this.getName() );
        element.setAttribute( "email", this.getEmail() );
        element.setAttribute( "password", this.getPassword() );
        return element;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo( Object anotherUser )
    {
        return this.getId().compareTo( ( (User) anotherUser ).getId() );
    }

}