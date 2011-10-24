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

import org.apache.commons.mail.HtmlEmail;
import org.apache.kalumet.KalumetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;

/**
 * Util class to send e-mails.
 */
public class EmailUtils {

    private final static transient Logger LOGGER = LoggerFactory.getLogger(EmailUtils.class);

    /**
     * Send a HTML email to a list of destination.
     *
     * @param host      the mail host name or IP address.
     * @param from      the from e-mail address.
     * @param subject   the e-mail subject.
     * @param addresses the e-mail address list.
     * @param content   the e-mail content.
     */
    public static void sendHTMLEmail(String host, String from, String subject, List addresses, String content) throws KalumetException {
        try {
            HtmlEmail email = new HtmlEmail();
            email.setHostName(host);
            email.setFrom(from);
            email.setSubject(subject);
            for (Iterator addressIterator = addresses.iterator(); addressIterator.hasNext(); ) {
                String address = (String) addressIterator.next();
                email.addTo(address);
            }
            email.setHtmlMsg(content);
            email.send();
        } catch (Exception e) {
            throw new KalumetException(e);
        }
    }

    /**
     * Send a text email to a list of destination.
     *
     * @param host      the mail host name or IP address.
     * @param from      the from e-mail address.
     * @param subject   the e-mail subject.
     * @param addresses the e-mail address list.
     * @param content   the e-mail content.
     */
    public static void sendTextEmail(String host, String from, String subject, List addresses, String content) throws KalumetException {
        try {
            HtmlEmail email = new HtmlEmail();
            email.setHostName(host);
            email.setFrom(from);
            email.setSubject(subject);
            for (Iterator addressIterator = addresses.iterator(); addressIterator.hasNext(); ) {
                String address = (String) addressIterator.next();
                email.addTo(address);
            }
            email.setTextMsg(content);
            email.send();
        } catch (Exception e) {
            throw new KalumetException(e);
        }
    }

    /**
     * Format a given e-mail template with value.
     *
     * @param template the template filename path/location.
     * @param values   the <code>Object[]</code> values.
     * @return the formatted string.
     */
    public static String format(String template, Object[] values) throws KalumetException {
        try {
            return EmailUtils.format(new FileReader(template), values);
        } catch (FileNotFoundException fileNotFoundException) {
            LOGGER.error("Can't format the e-mail template", fileNotFoundException);
            throw new KalumetException("Can't format the e-mail template", fileNotFoundException);
        }
    }

    /**
     * Format a given e-mail template with value.
     *
     * @param values the <code>Object[]</code> values.
     * @return the formatted string.
     * @pmram template the template reader.
     */
    public static String format(Reader templateReader, Object[] values) throws KalumetException {
        try {
            BufferedReader templateBufferedReader = new BufferedReader(templateReader);
            StringWriter writer = new StringWriter();
            BufferedWriter buffer = new BufferedWriter(writer);
            String templateLine = templateBufferedReader.readLine();
            while (templateLine != null) {
                buffer.write(MessageFormat.format(templateLine, values));
                buffer.newLine();
                templateLine = templateBufferedReader.readLine();
            }
            buffer.flush();
            return writer.toString();
        } catch (Exception e) {
            LOGGER.error("Can't format the e-mail template", e);
            throw new KalumetException("Can't format the e-mail template", e);
        }
    }

}
