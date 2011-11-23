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

import nextapp.echo2.app.ApplicationInstance;
import nextapp.echo2.webcontainer.WebContainerServlet;

/**
 * This is the echo-specific of a HttpServlet. This servlet is responsible for
 * processing all requests from the client-side Echo engine, including rendering
 * the initial HTML page, handling XML sync services, and sending graphic
 * images to the client. All such client interaction work is done behind the scene.
 */
public class KalumetConsoleServlet extends WebContainerServlet {

    public ApplicationInstance newApplicationInstance() {
        return new KalumetConsoleApplication();
    }

}
