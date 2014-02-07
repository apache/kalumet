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

import org.apache.commons.vfs.FileSelectInfo;
import org.apache.commons.vfs.FileSelector;
import org.apache.oro.text.GlobCompiler;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternCompiler;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.Perl5Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * VFS file selector based on name regex.
 */
public class FileNameRegexSelector
    implements FileSelector
{

    private static final transient Logger LOGGER = LoggerFactory.getLogger( FileNameRegexSelector.class );

    private Pattern pattern;

    private PatternMatcher matcher;

    /**
     * Default constructor with the matcher pattern.
     *
     * @param pattern the file name regex pattern to use.
     * @throws FileManipulatorException if the regex pattern is malformed.
     */
    public FileNameRegexSelector( String pattern )
        throws FileManipulatorException
    {
        LOGGER.debug( "Creating the glob regex" );
        PatternCompiler compiler = new GlobCompiler();
        try
        {
            this.pattern = compiler.compile( pattern );
            this.matcher = new Perl5Matcher();
        }
        catch ( MalformedPatternException malformedPatternException )
        {
            LOGGER.error( "Invalid regex pattern " + pattern, malformedPatternException );
            throw new FileManipulatorException( "Invalid regex pattern " + pattern, malformedPatternException );
        }
    }

    /**
     * @see org.apache.commons.vfs.FileSelector#includeFile(org.apache.commons.vfs.FileSelectInfo)
     */
    public boolean includeFile( FileSelectInfo fileInfo )
    {
        String fileName = fileInfo.getFile().getName().getBaseName();
        return matcher.matches( fileName, pattern );
    }

    /**
     * @see org.apache.commons.vfs.FileSelector#traverseDescendents(org.apache.commons.vfs.FileSelectInfo)
     */
    public boolean traverseDescendents( FileSelectInfo fileInfo )
    {
        return true;
    }

}
