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

import org.apache.kalumet.KalumetException;

/**
 * WebService client exception wrapper.
 */
public class ClientException
    extends KalumetException
{

    /**
     * Create a WebService client exception with the explanation message.
     *
     * @param message the explanation message.
     */
    public ClientException( String message )
    {
        super( message );
    }

    /**
     * Create a WebService client exception with the cause.
     *
     * @param cause the cause.
     */
    public ClientException( Throwable cause )
    {
        super( cause );
    }

    /**
     * Create a WebService client exception with the explanation message and the cause.
     *
     * @param message the explanation message.
     * @param cause   the cause.
     */
    public ClientException( String message, Throwable cause )
    {
        super( message, cause );
    }

}
