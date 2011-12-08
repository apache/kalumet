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
package org.apache.kalumet.utils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileType;
import org.apache.kalumet.FileManipulator;
import org.apache.kalumet.ws.client.SimplifiedFileObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Date;

/**
 * Utils to manipulate file (view, browse)
 */
public class FileUtils {

    private final static transient Logger LOGGER = LoggerFactory.getLogger(FileUtils.class);

    /**
     * Wrapper method to read a file.
     *
     * @param path the file VFS path.
     * @return the file content.
     */
    public static String view(String path) {
        String content = null;
        InputStream stream = null;
        try  {
            // get a file manipulator instance
            FileManipulator fileManipulator = FileManipulator.getInstance();
            // get the file content
            stream = fileManipulator.read(path);
            // populate the content string
            content = IOUtils.toString(stream);
        }
        catch (Exception e) {
            LOGGER.warn("Can't view {}", path, e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {
                    // nothing to do
                }
            }
        }
        return content;
    }

    /**
     * Wrapper method to browse a path.
     *
     * @param path the path to browse.
     * @return the list of children.
     */
    public static SimplifiedFileObject[] browse(String path) {
        SimplifiedFileObject[] children = null;
        try {
            // get a file manipulator instance
            FileManipulator fileManipulator = FileManipulator.getInstance();
            // get the path children
            FileObject[] fileObjects = fileManipulator.browse(path);
            children = new SimplifiedFileObject[fileObjects.length];
            for (int i = 0; i < fileObjects.length; i++) {
                SimplifiedFileObject file = new SimplifiedFileObject();
                file.setName(fileObjects[i].getName().getBaseName());
                file.setPath(fileObjects[i].getName().getPath());
                file.setFile(fileObjects[i].getType().equals(FileType.FILE));
                file.setLastModificationDate(new Date(fileObjects[i].getContent().getLastModifiedTime()));
                if (fileObjects[i].getType().equals(FileType.FILE)){
                    file.setSize(fileObjects[i].getContent().getSize());
                } else {
                    file.setSize(0);
                }
                children[i] = file;
            }
        } catch (Exception e) {
            LOGGER.warn("Can't browse {}", path, e);
        }
        return children;
    }

}
