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

import org.apache.axis.encoding.ser.BeanDeserializerFactory;
import org.apache.axis.encoding.ser.BeanSerializerFactory;

import javax.xml.namespace.QName;

/**
 * File WS client.
 */
public class FileClient
    extends AbstractClient
{

    /**
     * Default constructor.
     *
     * @param host the hostname or IP address of the Kalumet agent WS server.
     * @param port the port number of the Kalumet agent WS server.
     * @throws ClientException in case of communication failure.
     */
    public FileClient( String host, int port )
        throws ClientException
    {
        super( "http://" + host + ":" + port + "/axis/services/FileService" );
    }

    /**
     * Wrapper method to view the content of a VFS file.
     *
     * @param path the VFS path.
     * @return the file content.
     * @throws ClientException in case of viewing failure.
     */
    public String view( String path )
        throws ClientException
    {
        String content = null;
        try
        {
            content = ( (String) call.invoke( "view", new Object[]{ path } ) );
        }
        catch ( Exception e )
        {
            throw new ClientException( "Can't view the file " + path + " content", e );
        }
        return content;
    }

    public SimplifiedFileObject[] browse( String path )
        throws ClientException
    {
        call.registerTypeMapping( SimplifiedFileObject.class,
                                  new QName( "http://kalumet.apache.org", "SimplifiedFileObject" ),
                                  BeanSerializerFactory.class, BeanDeserializerFactory.class );
        SimplifiedFileObject[] children = null;
        try
        {
            children = ( (SimplifiedFileObject[]) call.invoke( "browse", new Object[]{ path } ) );
        }
        catch ( Exception e )
        {
            throw new ClientException( "Can't browse " + path, e );
        }
        return children;
    }

}
