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
 * Represent the <code>notifiers</code> tag in the Kalumet configuration DOM.
 */
public class Notifiers
  implements Serializable, Cloneable
{

  private static final long serialVersionUID = -5087839972754579270L;

  private int countdown;

  private LinkedList notifiers;

  public Notifiers()
  {
    this.notifiers = new LinkedList();
  }

  public int getCountdown()
  {
    return this.countdown;
  }

  public void setCountdown( int countdown )
  {
    this.countdown = countdown;
  }

  /**
   * Add a new <code>Email</code> notifier in the <code>Notifiers</code>
   * notifiers container.
   *
   * @param email the <code>Email</code> to add.
   */
  public void addNotifier( Email email )
    throws ModelObjectAlreadyExistsException
  {
    if ( this.getNotifier( email.getMailhost() ) != null )
    {
      throw new ModelObjectAlreadyExistsException( "Email notifier mailhost already exists in notifiers." );
    }
    this.notifiers.add( email );
  }

  /**
   * Get the <code>Email</code> notifier list in the <code>Notifiers</code>
   * notifiers container.
   *
   * @return the <code>Email</code> notifier list.
   */
  public List getNotifiers()
  {
    return this.notifiers;
  }

  /**
   * Set the <code>Email</code> notifier list in the
   * <code>Notifiers</code> notifiers container.
   *
   * @param notifiers the new <code>Email</code> notifier list.
   */
  public void setNotifiers( LinkedList notifiers )
  {
    this.notifiers = notifiers;
  }

  /**
   * Get the <code>Email</code> notifier identified by a given mail host in
   * the <code>Notifiers</code> notifiers container.
   *
   * @param mailhost the <code>Email</code> notifier mail host.
   * @return the <code>Email</code> found or null if not found.
   */
  public Email getNotifier( String mailhost )
  {
    for ( Iterator notifierIterator = this.getNotifiers().iterator(); notifierIterator.hasNext(); )
    {
      Email email = (Email) notifierIterator.next();
      if ( email.getMailhost().equals( mailhost ) )
      {
        return email;
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
    Notifiers clone = new Notifiers();
    clone.setCountdown( this.getCountdown() );
    for ( Iterator notifierIterator = this.notifiers.iterator(); notifierIterator.hasNext(); )
    {
      Email notifier = (Email) notifierIterator.next();
      clone.notifiers.add( (Email) notifier.clone() );
    }
    return clone;
  }

  /**
   * Transform the <code>Notifiers</code> POJO to a DOM element.
   *
   * @param document the DOM document.
   * @return the DOM element.
   */
  protected Element toDOMElement( CoreDocumentImpl document )
  {
    ElementImpl element = new ElementImpl( document, "notifiers" );
    element.setAttribute( "countdown", new Integer( this.getCountdown() ).toString() );
    // email notifier child nodes
    for ( Iterator notifierIterator = this.getNotifiers().iterator(); notifierIterator.hasNext(); )
    {
      Email email = (Email) notifierIterator.next();
      element.appendChild( email.toDOMElement( document ) );
    }
    return element;
  }

}