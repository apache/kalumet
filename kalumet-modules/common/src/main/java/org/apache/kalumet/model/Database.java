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
 * Represent the <code>database</code> tag in the Kalumet configuration DOM.
 */
public class Database
    implements Serializable, Cloneable, Comparable
{

    private static final long serialVersionUID = 119112072290707974L;

    private String name;

    private String driver;

    private String user;

    private String password;

    private String jdbcurl;

    private String connectionPool;

    private String sqlCommand;

    private String agent;

    private boolean active;

    private boolean blocker;

    private LinkedList sqlScripts;

    public Database()
    {
        this.sqlScripts = new LinkedList();
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

    public String getJdbcurl()
    {
        return this.jdbcurl;
    }

    public void setJdbcurl( String jdbcurl )
    {
        this.jdbcurl = jdbcurl;
    }

    public String getConnectionPool()
    {
        return connectionPool;
    }

    public void setConnectionPool( String connectionPool )
    {
        this.connectionPool = connectionPool;
    }

    public String getSqlCommand()
    {
        return sqlCommand;
    }

    public void setSqlCommand( String sqlCommand )
    {
        this.sqlCommand = sqlCommand;
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
     * Add a new <code>SqlScript</code> in the <code>Database</code> sqlscripts
     * container.
     *
     * @param sqlScript the <code>SqlScript</code> to add.
     */
    public void addSqlScript( SqlScript sqlScript )
        throws ModelObjectAlreadyExistsException
    {
        if ( this.getSqlScript( sqlScript.getName() ) != null )
        {
            throw new ModelObjectAlreadyExistsException( "SQL script name already exists in database." );
        }
        this.sqlScripts.add( sqlScript );
    }

    /**
     * Get the <code>SqlScript</code> list in the <code>Database</code>
     * sqlscripts container.
     *
     * @return the <code>SqlScript</code> list.
     */
    public List getSqlScripts()
    {
        return this.sqlScripts;
    }

    /**
     * Set the <code>SqlScript</code> list in the <code>Database</code>
     * sqlscripts container.
     *
     * @param sqlScripts the new <code>SqlScript</code> list.
     */
    public void setSqlScripts( LinkedList sqlScripts )
    {
        this.sqlScripts = sqlScripts;
    }

    /**
     * Get the <code>SqlScript</code> identified by a given name in the
     * <code>Database</code> sqlscripts container.
     *
     * @param name the <code>SqlScript</code> name.
     * @return the <code>SqlScript</code> found or null if no <code>SqlScript</code> found.
     */
    public SqlScript getSqlScript( String name )
    {
        for ( Iterator sqlScriptIterator = this.getSqlScripts().iterator(); sqlScriptIterator.hasNext(); )
        {
            SqlScript sqlScript = (SqlScript) sqlScriptIterator.next();
            if ( sqlScript.getName().equals( name ) )
            {
                return sqlScript;
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
        Database clone = new Database();
        clone.setName( this.getName() );
        clone.setDriver( this.getDriver() );
        clone.setUser( this.getUser() );
        clone.setPassword( this.getPassword() );
        clone.setJdbcurl( this.getJdbcurl() );
        clone.setConnectionPool( this.getConnectionPool() );
        clone.setSqlCommand( this.getSqlCommand() );
        clone.setAgent( this.getAgent() );
        clone.setActive( this.isActive() );
        clone.setBlocker( this.isBlocker() );
        for ( Iterator sqlScriptIterator = this.sqlScripts.iterator(); sqlScriptIterator.hasNext(); )
        {
            SqlScript sqlScript = (SqlScript) sqlScriptIterator.next();
            clone.sqlScripts.add( (SqlScript) sqlScript.clone() );
        }
        return clone;
    }

    /**
     * Transform the <code>Database</code> POJO to a DOM element.
     *
     * @param document the core XML document.
     * @return the DOM element.
     */
    protected Element toDOMElement( CoreDocumentImpl document )
    {
        ElementImpl element = new ElementImpl( document, "database" );
        element.setAttribute( "name", this.getName() );
        element.setAttribute( "driver", this.getDriver() );
        element.setAttribute( "user", this.getUser() );
        element.setAttribute( "password", this.getPassword() );
        element.setAttribute( "jdbcurl", this.getJdbcurl() );
        element.setAttribute( "connectionPool", this.getConnectionPool() );
        element.setAttribute( "sqlCommand", this.getSqlCommand() );
        element.setAttribute( "agent", this.getAgent() );
        element.setAttribute( "active", new Boolean( this.isActive() ).toString() );
        element.setAttribute( "blocker", new Boolean( this.isBlocker() ).toString() );
        // sqlscripts
        ElementImpl sqlscripts = new ElementImpl( document, "sqlscripts" );
        for ( Iterator sqlScriptIterator = this.getSqlScripts().iterator(); sqlScriptIterator.hasNext(); )
        {
            SqlScript sqlScript = (SqlScript) sqlScriptIterator.next();
            sqlscripts.appendChild( sqlScript.toDOMElement( document ) );
        }
        element.appendChild( sqlscripts );
        return element;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo( Object anotherDatabase )
    {
        return this.getName().compareTo( ( (Database) anotherDatabase ).getName() );
    }

}