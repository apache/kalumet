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
package org.apache.kalumet.console.app;

import nextapp.echo2.app.Button;
import nextapp.echo2.app.ContentPane;
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.Grid;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import org.apache.kalumet.model.Cache;

import java.util.Iterator;

/**
 * JEE application server caches pane.
 */
public class ApplicationServerCachesPane
  extends ContentPane
{

  private ApplicationServerWindow parent;

  private Grid grid;

  private TextField newPathField;

  // delete
  private ActionListener delete = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if the user has the environment lock
      if ( !getEnvironmentWindow().getEnvironment().getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeServersPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restrictied" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // get the cache path
      final String cachePath = event.getActionCommand();
      // display confirm window
      KalumetConsoleApplication.getApplication().getDefaultWindow().getContent().add(
        new ConfirmWindow( new ActionListener()
        {
          public void actionPerformed( ActionEvent event )
          {
            // looking for the cache object
            Cache cache = parent.getApplicationServer().getCache( cachePath );
            if ( cache == null )
            {
              KalumetConsoleApplication.getApplication().getLogPane().addWarning(
                Messages.getString( "cache.notfound" ), getEnvironmentWindow().getEnvironmentName() );
              return;
            }
            // remove the cache
            parent.getApplicationServer().getCaches().remove( cache );
            // add a change event
            parent.getEnvironmentWindow().getChangeEvents().add( "Delete cache " + cache.getPath() );
            // change the updated flag
            parent.getEnvironmentWindow().setUpdated( true );
            // update the journal log tab pane
            parent.getEnvironmentWindow().updateJournalPane();
            // update the pane
            update();
          }
        } ) );
    }
  };

  // edit
  private ActionListener edit = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if the user has the environment lock
      if ( !getEnvironmentWindow().getEnvironment().getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeServersPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // get the cache path
      String cachePath = event.getActionCommand();
      // get the cache path field
      TextField cachePathField = (TextField) ApplicationServerCachesPane.this.getComponent(
        "pathfield_" + parent.getEnvironmentWindow().getEnvironmentName() + "_" + parent.getApplicationServerName()
          + "_" + cachePath );
      // get the cache path field value
      String cachePathFieldValue = cachePathField.getText();
      // check mandatory field
      if ( cachePathFieldValue == null || cachePathFieldValue.trim().length() < 1 )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "cache.mandatory" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // if the user change the cache path, check if the cache path doesn't
      // already exist
      if ( !cachePath.equals( cachePathFieldValue ) )
      {
        if ( parent.getApplicationServer().getCache( cachePathFieldValue ) != null )
        {
          KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "cache.exists" ),
                                                                              getEnvironmentWindow().getEnvironmentName() );
          return;
        }
      }
      // looking for the cache object
      Cache cache = parent.getApplicationServer().getCache( cachePath );
      if ( cache == null )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "cache.notfound" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // add a change event
      parent.getEnvironmentWindow().getChangeEvents().add( "Change cache " + cache.getPath() );
      // update the cache object
      cache.setPath( cachePathFieldValue );
      // change the updated flag
      parent.getEnvironmentWindow().setUpdated( true );
      // update the journal log tab pane
      parent.getEnvironmentWindow().updateJournalPane();
      // update the pane
      update();
    }
  };

  // create
  private ActionListener create = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if the user has the environment lock
      if ( !getEnvironmentWindow().getEnvironment().getLock().equals(
        KalumetConsoleApplication.getApplication().getUserid() ) )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "environment.locked" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // check if the user can do it
      if ( !getEnvironmentWindow().adminPermission && !getEnvironmentWindow().jeeServersPermission )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "action.restricted" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // get cache path value
      String newPathFieldValue = newPathField.getText();
      // check mandatory field
      if ( newPathFieldValue == null || newPathFieldValue.trim().length() < 1 )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "cache.mandatory" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // create a new cache object
      Cache cache = new Cache();
      cache.setPath( newPathFieldValue );
      try
      {
        parent.getApplicationServer().addCache( cache );
      }
      catch ( Exception e )
      {
        KalumetConsoleApplication.getApplication().getLogPane().addWarning( Messages.getString( "cache.exists" ),
                                                                            getEnvironmentWindow().getEnvironmentName() );
        return;
      }
      // add a change event
      parent.getEnvironmentWindow().getChangeEvents().add( "Add cache " + cache.getPath() );
      // change the updated flag
      parent.getEnvironmentWindow().setUpdated( true );
      // update the journal log tab pane
      parent.getEnvironmentWindow().updateJournalPane();
      // update the pane
      update();
    }
  };

  // copy
  private ActionListener copy = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // looking for the cache object
      Cache cache = parent.getApplicationServer().getCache( event.getActionCommand() );
      if ( cache == null )
      {
        return;
      }
      try
      {
        // put the cache clone in the copy component
        KalumetConsoleApplication.getApplication().setCopyComponent( cache.clone() );
      }
      catch ( Exception e )
      {
        return;
      }
    }
  };

  // paste
  private ActionListener paste = new ActionListener()
  {
    public void actionPerformed( ActionEvent event )
    {
      // check if the copy is correct
      Object copy = KalumetConsoleApplication.getApplication().getCopyComponent();
      if ( copy == null || !( copy instanceof Cache ) )
      {
        return;
      }
      // update the new fields
      newPathField.setText( ( (Cache) copy ).getPath() );
    }
  };

  /**
   * Create a new <code>ApplicationServerCachesPane</code>.
   *
   * @param parent the parent <code>ApplicationServerWindow</code>.
   */
  public ApplicationServerCachesPane( ApplicationServerWindow parent )
  {
    super();
    setStyleName( "tab.content" );

    // update parent
    this.parent = parent;

    // add the caches grid
    grid = new Grid( 2 );
    grid.setStyleName( "border.grid" );
    grid.setColumnWidth( 0, new Extent( 50, Extent.PX ) );
    add( grid );

    // update the pane
    update();
  }

  /**
   * Update the pane.
   */
  public void update()
  {
    // remove all caches grid children
    grid.removeAll();
    // action header
    Label actionHeader = new Label( " " );
    actionHeader.setStyleName( "grid.header" );
    grid.add( actionHeader );
    Label pathHeader = new Label( Messages.getString( "path" ) );
    pathHeader.setStyleName( "grid.header" );
    grid.add( pathHeader );
    // add cache
    for ( Iterator cacheIterator = parent.getApplicationServer().getCaches().iterator(); cacheIterator.hasNext(); )
    {
      Cache cache = (Cache) cacheIterator.next();
      // row
      Row row = new Row();
      row.setCellSpacing( new Extent( 2 ) );
      row.setInsets( new Insets( 2 ) );
      grid.add( row );
      // copy
      Button copyButton = new Button( Styles.PAGE_COPY );
      copyButton.setToolTipText( Messages.getString( "copy" ) );
      copyButton.setActionCommand( cache.getPath() );
      copyButton.addActionListener( copy );
      row.add( copyButton );
      // delete / edit
      if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeServersPermission )
      {
        // edit
        Button editButton = new Button( Styles.ACCEPT );
        editButton.setActionCommand( cache.getPath() );
        editButton.addActionListener( edit );
        row.add( editButton );
        // delete
        Button deleteButton = new Button( Styles.DELETE );
        deleteButton.setToolTipText( Messages.getString( "delete" ) );
        deleteButton.setActionCommand( cache.getPath() );
        deleteButton.addActionListener( delete );
        row.add( deleteButton );
      }
      // path
      TextField cachePathField = new TextField();
      cachePathField.setId(
        "pathfield_" + parent.getEnvironmentWindow().getEnvironmentName() + "_" + parent.getApplicationServerName()
          + "_" + cache.getPath() );
      cachePathField.setStyleName( "default" );
      cachePathField.setWidth( new Extent( 100, Extent.PERCENT ) );
      cachePathField.setText( cache.getPath() );
      grid.add( cachePathField );
    }
    // add cache adding row
    if ( getEnvironmentWindow().adminPermission || getEnvironmentWindow().jeeServersPermission )
    {
      // row
      Row row = new Row();
      row.setCellSpacing( new Extent( 2 ) );
      row.setInsets( new Insets( 2 ) );
      grid.add( row );
      // paste
      Button pasteButton = new Button( Styles.PAGE_PASTE );
      pasteButton.addActionListener( paste );
      row.add( pasteButton );
      // add
      Button addButton = new Button( Styles.ADD );
      addButton.addActionListener( create );
      row.add( addButton );
      // path
      newPathField = new TextField();
      newPathField.setStyleName( "default" );
      newPathField.setWidth( new Extent( 100, Extent.PERCENT ) );
      grid.add( newPathField );
    }
  }

  public EnvironmentWindow getEnvironmentWindow()
  {
    return parent.getEnvironmentWindow();
  }

}
