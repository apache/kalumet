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

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;

import java.net.URL;

/**
 * Abstract WS client.
 */
public abstract class AbstractClient
{

    protected Call call;

    public AbstractClient( String url )
        throws ClientException
    {
        try
        {
            Service service = new Service();
            this.call = (Call) service.createCall();
            call.setTimeout( new Integer( Integer.MAX_VALUE ) );
            call.setTargetEndpointAddress( new URL( url ) );
        }
        catch ( Exception e )
        {
            throw new ClientException( "Can't to the Kalumet agent WS server", e );
        }
    }

}
