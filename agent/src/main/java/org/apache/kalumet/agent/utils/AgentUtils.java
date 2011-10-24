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
package org.apache.kalumet.agent.utils;

/**
 * Util class to get version from the MANIFEST package specification.
 */
public class AgentUtils {

    /**
     * Get the version from Kalumet package.
     *
     * @return the Kalumet version.
     */
    public static String getVersion() {
        String version = "";
        Package p = Package.getPackage("org.apache.kalumet.agent");
        if (p != null) {
            version = p.getImplementationVersion();
        }
        return version;
    }

}
