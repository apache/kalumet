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

/**
 * Exception class handler to manage when a model component already exists in
 * the Kalumet configuration.
 */
public final class ModelObjectAlreadyExistsException
  extends KalumetException
{

  private static final long serialVersionUID = -6461646659257275924L;

  /**
   * Creates a <code>ModelObjectAlreadyExistsException</code> with an explanation message.
   *
   * @param message the explanation message.
   */
  public ModelObjectAlreadyExistsException( String message )
  {
    super( message );
  }

  /**
   * Creates a <code>ModelObjectAlreadyExistsException</code> with the underlying cause.
   *
   * @param cause the underlying cause.
   */
  public ModelObjectAlreadyExistsException( Throwable cause )
  {
    super( cause );
  }

  /**
   * Creates a <code>ModelObjectAlreadyExistsException</code> with an explanation message and the underlying cause.
   *
   * @param message the explanation message.
   * @param cause   the underlying cause.
   */
  public ModelObjectAlreadyExistsException( String message, Throwable cause )
  {
    super( message, cause );
  }

}
