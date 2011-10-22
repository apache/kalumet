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

import java.io.File;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * Managed files by reference.
 */
public class WeakFileReference extends WeakReference {

    private String absolutePath;

    /**
     * Default constructor.
     *
     * @param file the file to manage reference.
     * @param queue the references queue.
     */
    public WeakFileReference(File file, ReferenceQueue queue) {
        super(file, queue);
        this.absolutePath = file.getAbsolutePath();
    }

    /**
     * Gets the absolute path of the file.
     *
     * @return the absolute path of the file.
     */
    public String getPath() {
        return this.absolutePath;
    }
}
