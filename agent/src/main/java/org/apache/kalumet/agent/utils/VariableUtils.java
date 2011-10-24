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

import org.apache.commons.lang.StringUtils;
import org.apache.kalumet.model.Variable;

import java.util.Iterator;
import java.util.List;

/**
 * Util class to search and replace variables in string.
 */
public class VariableUtils {

    /**
     * Replace all given variables in a string.
     *
     * @param source    the source string.
     * @param variables the variables list.
     * @return the string with variables replaced.
     */
    public final static String replace(String source, List variables) {
        String replaced = source;
        for (Iterator variableIterator = variables.iterator(); variableIterator.hasNext(); ) {
            Variable variable = (Variable) variableIterator.next();
            replaced = StringUtils.replace(replaced, "${" + variable.getName() + "}", variable.getValue());
        }
        return replaced;
    }

}
