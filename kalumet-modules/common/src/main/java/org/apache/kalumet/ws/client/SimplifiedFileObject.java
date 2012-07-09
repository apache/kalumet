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
package org.apache.kalumet.ws.client;

import java.io.Serializable;
import java.util.Date;

/**
 * Simplified VFS file object to be used via WS.
 */
public class SimplifiedFileObject
  implements Serializable
{

  private String name;

  private String path;

  private boolean file;

  private long size;

  private Date lastModificationDate;

  public String getName()
  {
    return this.name;
  }

  public void setName( String name )
  {
    this.name = name;
  }

  public String getPath()
  {
    return this.path;
  }

  public void setPath( String path )
  {
    this.path = path;
  }

  public boolean isFile()
  {
    return this.file;
  }

  public void setFile( boolean file )
  {
    this.file = file;
  }

  public Date getLastModificationDate()
  {
    return this.lastModificationDate;
  }

  public void setLastModificationDate( Date lastModificationDate )
  {
    this.lastModificationDate = lastModificationDate;
  }

  public long getSize()
  {
    return this.size;
  }

  public void setSize( long size )
  {
    this.size = size;
  }

}
