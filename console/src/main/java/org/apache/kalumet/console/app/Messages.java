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

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import nextapp.echo2.app.ApplicationInstance;

/**
 * A utility class that provides resources for obtaining localized messages.
 */
public class Messages {

    private static final String BUNDLE_NAME = "org.apache.kalumet.console.app.locales.Messages";

    // a map which contains DateFormat objects for various locales.
    private static final Map DATE_FORMAT_MEDIUM_MAP = new HashMap();

    /**
     * Formats a date with the specified locale.
     *
     * @param date the date to be formatted.
     * @return a localized String representation of the date.
     */
    public static final String formatDateTimeMedium(Date date) {
        Locale locale = ApplicationInstance.getActive().getLocale();
        DateFormat df = (DateFormat) DATE_FORMAT_MEDIUM_MAP.get(locale);
        if (df == null) {
            df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale);
            DATE_FORMAT_MEDIUM_MAP.put(locale, df);
        }
        return date == null ? null : df.format(date);
    }

    /**
     * Returns a localized formatted message. This method conveniently wraps a
     * call to a MessageFormat object.
     *
     * @param key       the key of the message to be returned.
     * @param arguments an array of arguments to be inserted into the message.
     */
    public static String getFormattedString(String key, Object[] arguments) {
        Locale locale = ApplicationInstance.getActive().getLocale();
        String template = getString(key);
        MessageFormat messageFormat = new MessageFormat(template);
        messageFormat.setLocale(locale);
        return messageFormat.format(arguments, new StringBuffer(), null).toString();
    }

    /**
     * Returns localized text.
     *
     * @param key the key of the text to be returned.
     * @return the appropriate localized text (if the key is not defined, the string "!key!" is returned).
     */
    public static String getString(String key) {
        try {
            Locale locale = ApplicationInstance.getActive().getLocale();
            ResourceBundle resource = ResourceBundle.getBundle(BUNDLE_NAME, locale);
            return resource.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    /**
     * Non-instantiable class.
     */
    private Messages() {
    }

}
