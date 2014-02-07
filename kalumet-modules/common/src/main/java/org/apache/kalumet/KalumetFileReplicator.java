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
package org.apache.kalumet;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSelector;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.Selectors;
import org.apache.commons.vfs.provider.AbstractVfsComponent;
import org.apache.commons.vfs.provider.FileReplicator;
import org.apache.commons.vfs.provider.TemporaryFileStore;
import org.apache.commons.vfs.provider.UriParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.List;

/**
 * VFS file replicator to avoid huge space usage in the VFS cache.
 */
public class KalumetFileReplicator
  extends AbstractVfsComponent
  implements FileReplicator, TemporaryFileStore
{

  private final static transient Logger LOGGER = LoggerFactory.getLogger( KalumetFileReplicator.class );

  private final List<WeakFileReference> copies = new ArrayList<WeakFileReference>();

  private File tmpDir;

  private ReferenceQueue<WeakFileReference> queue = new ReferenceQueue<WeakFileReference>();

  private char[] TMP_RESERVED_CHARS =
    new char[]{ '?', '/', '\\', ' ', '&', '"', '\'', '*', '#', ';', ':', '<', '>', '|' };

  /**
   * Constructor to set the location of the temporary directory.
   *
   * @param tmpDir the temporary cache directory.
   */
  public KalumetFileReplicator( final File tmpDir )
  {
    this.tmpDir = tmpDir;
  }

  /**
   * Default constructor.
   */
  public KalumetFileReplicator()
  {

  }

  /**
   * Initialize the VFS component.
   *
   * @throws FileSystemException in case of init failure.
   */
  public void init()
    throws FileSystemException
  {
    if ( tmpDir == null )
    {
      tmpDir = new File( "kalumet_cache" ).getAbsoluteFile();
    }
  }

  /**
   * Close the replication class, deleting all temporary files.
   */
  public void close()
  {
    // delete the temporary files
    while ( copies.size() > 0 )
    {
      WeakFileReference fileReference = copies.remove( 0 );
      try
      {
        File file = new File( fileReference.getPath() );
        FileObject fileObject = getContext().toFileObject( file );
        fileObject.delete( Selectors.SELECT_ALL );
      }
      catch ( FileSystemException fileSystemException )
      {
        LOGGER.error( "Can't delete temporary files", fileSystemException );
      }
    }
    // clean the tmp directory, if it's empty
    if ( tmpDir != null && tmpDir.exists() && tmpDir.list().length == 0 )
    {
      tmpDir.delete();
      tmpDir = null;
    }
  }

  /**
   * Allocates a new temporary file.
   *
   * @param baseName the file base name.
   * @return the temporary file.
   * @throws FileSystemException if the allocation failed.
   */
  public File allocateFile( final String baseName )
    throws FileSystemException
  {
    WeakFileReference fileReference = queue.poll().get();
    while ( fileReference != null )
    {
      File toDelete = new File( fileReference.getPath() );
      if ( toDelete.exists() )
      {
        toDelete.delete();
      }
      copies.remove( fileReference );
      fileReference = queue.poll().get();
    }
    // create the filename
    final String baseNamePath = createFileName( baseName );
    final File file = createFile( tmpDir, baseNamePath );
    // keep track to delete later
    copies.add( new WeakFileReference( file, queue ) );
    return file;
  }

  /**
   * Create the temporary file name.
   *
   * @param baseName the temporary file base name.
   * @return the temporary file name.
   */
  protected String createFileName( final String baseName )
  {
    String safeBaseName = UriParser.encode( baseName, TMP_RESERVED_CHARS ).replace( '%', '_' );
    return "tmp_" + safeBaseName;
  }

  /**
   * Create a temporary file.
   *
   * @param parent the parent file.
   * @param name   the file name.
   * @return the file.
   * @throws FileSystemException in case of creation failure.
   */
  protected File createFile( final File parent, final String name )
    throws FileSystemException
  {
    return new File( parent, UriParser.decode( name ) );
  }

  /**
   * Create a local (in cache) copy of the file, and all its descendants.
   *
   * @param src      the source file.
   * @param selector the file selector.
   * @return the replicate file.
   * @throws FileSystemException in case of replication failure.
   */
  public File replicateFile( final FileObject src, final FileSelector selector )
    throws FileSystemException
  {
    final String baseName = src.getName().getBaseName();
    final File file = allocateFile( baseName );
    // copy from the source file
    final FileObject dest = getContext().toFileObject( file );
    dest.copyFrom( src, selector );
    return file;
  }

}
