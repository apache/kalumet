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
package org.apache.kalumet.console.utils;

/**
 * Util class to manipulate exception stack trace.
 */
public class StackTraceUtils
{

    /**
     * Convert a stack trace elements into a string.
     * WARNING: depending of the exception chain, this method call consume heap memory.
     *
     * @param elements the stack trace elements.
     * @return the string representation of the stack trace.
     */
    public static String toString( StackTraceElement[] elements )
    {
        StringBuilder builder = new StringBuilder();
        for ( int i = 0, size = elements.length; i < size; i++ )
        {
            builder.append( elements[i].toString() );
            builder.append( '\n' );
        }
        return builder.toString();
    }

}
