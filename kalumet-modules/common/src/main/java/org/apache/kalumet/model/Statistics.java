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
 * Store environment statistics (number of update, etc).
 */
public class Statistics
    implements Serializable, Cloneable
{

    private static final long serialVersionUID = -6110824574514994557L;

    private int updateCount;

    private String lastUpdateDate;

    private String lastChangeDate;

    public Statistics()
    {
        updateCount = 0;
    }

    public int getUpdateCount()
    {
        return updateCount;
    }

    public void setUpdateCount( int updateCount )
    {
        this.updateCount = updateCount;
    }

    public String getLastUpdateDate()
    {
        return lastUpdateDate;
    }

    public void setLastUpdateDate( String lastUpdateDate )
    {
        this.lastUpdateDate = lastUpdateDate;
    }

    public String getLastChangeDate()
    {
        return lastChangeDate;
    }

    public void setLastChangeDate( String lastChangeDate )
    {
        this.lastChangeDate = lastChangeDate;
    }

    /**
     * @see java.lang.Object#clone()
     */
    public Object clone()
        throws CloneNotSupportedException
    {
        Statistics clone = new Statistics();
        clone.setUpdateCount( this.getUpdateCount() );
        clone.setLastChangeDate( this.getLastChangeDate() );
        clone.setLastUpdateDate( this.getLastUpdateDate() );
        return clone;
    }

    /**
     * Transform the <code>Statistics</code> POJO to a DOM element.
     *
     * @param document the DOM document.
     * @return the DOM element.
     */
    protected Element toDOMElement( CoreDocumentImpl document )
    {
        ElementImpl element = new ElementImpl( document, "statistics" );
        element.setAttribute( "updatecount", new Integer( this.getUpdateCount() ).toString() );
        element.setAttribute( "lastupdatedate", this.getLastUpdateDate() );
        element.setAttribute( "lastchangedate", this.getLastChangeDate() );
        return element;
    }

}