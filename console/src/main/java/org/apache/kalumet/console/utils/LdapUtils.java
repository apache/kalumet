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
package org.apache.kalumet.console.utils;

import org.apache.kalumet.console.configuration.ConfigurationManager;
import org.apache.kalumet.model.Kalumet;
import org.apache.kalumet.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;


/**
 * LDAP utility class.
 */
public class LdapUtils {

   private final static transient Logger LOGGER = LoggerFactory.getLogger(LdapUtils.class);

   /**
    * Try to bind a user id and password in a given LDAP directory.
    * 
    * @param user the user to bind.
    * @param password the password to bind.
    * @return true if the user is identified.
    */
   public static boolean bind(String user, String password) throws Exception {
      LOGGER.debug("Try to bind the user {}", user);
      // load Kalumet store
      Kalumet kalumet;
      kalumet = ConfigurationManager.loadStore();
      if (kalumet.getProperty("LdapAuthentication") == null || kalumet.getProperty("LdapServer") == null || kalumet.getProperty("LdapBaseDN") == null || kalumet.getProperty("LdapUidAttribute") == null
            || kalumet.getProperty("LdapMailAttribute") == null || kalumet.getProperty("LdapCnAttribute") == null) {
         LOGGER.error("All LDAP required properties are not present in Apache Kalumet configuration. Check if the properties LdapAuthentication, LdapServer, LdapBaseDN, LdapUidAttribute, LdapMailAttribute, LdapCnAttribute are presents in Apache Kalumet configuration.");
         throw new IllegalStateException("All LDAP required properties are not present in Apache Kalumet configuration. Check if the properties LdapAuthentication, LdapServer, LdapBaseDN, LdapUidAttribute, LdapMailAttribute, LdapCnAttribute are presents in Apache Kalumet configuration.");
      }
      if (kalumet.getProperty("LdapAuthentication").getValue().equals("false")) {
         LOGGER.error("The LDAP authentication is not active in Apache Kalumet configuration. Can't bind on LDAP.");
         throw new IllegalStateException("The LDAP authentication is not active in Apache Kalumet. Can't bind on LDAP.");
      }
      // step 1 : connect to the LDAP server (anonymous) to get the user DN
      LOGGER.debug("LDAP Authentification Backend Step 1: grab the user DN");
      LOGGER.debug("Create the LDAP initial context");
      Hashtable env = new Hashtable();
      // TODO use a generic LDAP Context Factory compliant with IBM JDK
      env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
      LOGGER.debug("Connect to the LDAP server ldap://{}", kalumet.getProperty("LdapServer").getValue());
      env.put(Context.PROVIDER_URL, "ldap://" + kalumet.getProperty("LdapServer").getValue());
      String userDN;
      String userName;
      String userEmail;
      try {
         LOGGER.debug("Init the JNDI LDAP Dir context ...");
         DirContext context = new InitialDirContext(env);
         LOGGER.debug("Define the subtree scope search control. ");
         SearchControls controls = new SearchControls();
         controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
         LOGGER.debug("Looking for the user in LDAP ...");
         LOGGER.debug("  Base DN: {}", kalumet.getProperty("LdapBaseDN").getValue());
         LOGGER.debug("  Filter:  ({}={})", kalumet.getProperty("LdapUidAttribute").getValue(), user);
         NamingEnumeration namingEnumeration = context.search(kalumet.getProperty("LdapBaseDN").getValue(), "(" + kalumet.getProperty("LdapUidAttribute").getValue() + "=" + user + ")", controls);
         if (!namingEnumeration.hasMore()) {
            LOGGER.warn("User {} not found in LDAP", user);
            return false;
         }
         LOGGER.debug("Get the user object");
         SearchResult result = (SearchResult) namingEnumeration.next();
         LOGGER.debug("Get the attributes set");
         Attributes attributes = result.getAttributes();
         LOGGER.debug("Trying to get the DN attribute");
         userDN = (String) result.getName();
         LOGGER.debug("Get the LDAP user DN: {}", userDN);
         userName = (String) attributes.get(kalumet.getProperty("LdapCnAttribute").getValue()).get();
         LOGGER.debug("Get the LDAP user name: {}", userName);
         userEmail = (String) attributes.get(kalumet.getProperty("LdapMailAttribute").getValue()).get();
         LOGGER.debug("Get the LDAP user e-mail: {}", userEmail);
         context.close();
      } catch (Exception e) {
         LOGGER.error("Can't connect to the LDAP server", e);
         throw new IllegalStateException("Can't connect to the LDAP server", e);
      }
      // step 2 : I have the DN, try to bind the user
      LOGGER.debug("LDAP Authentification Backend Step 2: bind the user with the DN/password");
      env = new Hashtable();
      env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
      LOGGER.debug("Connect to the LDAP server ldap://{}", kalumet.getProperty("LdapServer").getValue());
      env.put(Context.PROVIDER_URL, "ldap://" + kalumet.getProperty("LdapServer").getValue());
      LOGGER.debug("Define a simple authentication");
      env.put(Context.SECURITY_AUTHENTICATION, "simple");
      LOGGER.debug("Define the security principal to {},{}", userDN, kalumet.getProperty("LdapBaseDN").getValue());
      env.put(Context.SECURITY_PRINCIPAL, userDN + "," + kalumet.getProperty("LdapBaseDN").getValue());
      env.put(Context.SECURITY_CREDENTIALS, password);
      LOGGER.debug("Init the JNDI context ...");
      try {
         LOGGER.debug("Directory context init");
         DirContext context = new InitialDirContext(env);
         LOGGER.debug("LDAP user bind successful");
         context.close();
         if (kalumet.getSecurity().getUser(user) == null) {
            User securityUser = new User();
            securityUser.setId(user);
            securityUser.setName(userName);
            securityUser.setEmail(userEmail);
            kalumet.getSecurity().addUser(securityUser);
            try {
                ConfigurationManager.writeStore(kalumet);
            } catch (Exception e) {
               LOGGER.error("Can't write the LDAP user in Apache Kalumet configuration store", e);
            }
         }
      } catch(Exception e) {
         LOGGER.error("User authentication failure using LDAP server", e);
         return false;
      }
      return true;
   }

}
