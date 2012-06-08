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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests on the <code>FileManipulator</code>.
 */
public class FileManipulatorTest
{

  private FileManipulator fileManipulator;

  @Before
  public void setUp()
    throws Exception
  {
    fileManipulator = new FileManipulator();
  }

  @Test
  public void testProtocolPrefix()
    throws Exception
  {
    String uri = "http://uri";
    assertEquals( true, FileManipulator.protocolExists( uri ) );
    uri = "uri";
    assertEquals( false, FileManipulator.protocolExists( uri ) );
    uri = "zip:file:/test";
    assertEquals( true, FileManipulator.protocolExists( uri ) );
  }

  @Test
  public void testVFSFormatting()
    throws Exception
  {
    String uri = FileManipulator.format( "http://uri/archive.tar.gz" ) + "!/lib";
    assertEquals( "tgz:http://uri/archive.tar.gz!/lib", uri );
  }

  @Test
  public void testContentCompare()
    throws Exception
  {
    assertEquals( true,
                  fileManipulator.contentEquals( "https://svn.apache.org/repos/asf/incubator/kalumet/trunk/LICENSE",
                                                 "https://svn.apache.org/repos/asf/incubator/kalumet/trunk/LICENSE" ) );
  }

  @Test
  public void testExists()
    throws Exception
  {
    assertEquals( true, fileManipulator.exists( "https://svn.apache.org/repos/asf/incubator/kalumet/trunk/LICENSE" ) );
    assertEquals( false,
                  fileManipulator.exists( "https://svn.apache.org/repos/asf/incubator/kalumet/trunk/NOT_FOUND" ) );
  }

}
