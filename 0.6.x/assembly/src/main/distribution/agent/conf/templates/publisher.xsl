<?xml version="1.0"?>
<!--

    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements.  See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to You under the Apache License, Version 2.0
    (the "License"); you may not use this file except in compliance with
    the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->
<!--
   *************************************
   * Apache Kalumet Mail Publisher XSL *
   * Author : Jean-Baptiste ONOFRE       *
   * E-mail : jb@nanthrax.net                *
   *************************************
   
   This XSL is optionnal. It's used to send a HTML e-mail
   when an Apache Kalumet agent publish an update result.
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <xsl:output method="html"/>

  <xsl:template match="/">
    <html>
      <head>
        <title>Apache Kalumet Agent Update Report</title>
        <xsl:call-template name="style"/>
      </head>
      <body>
        <div id="header">
          Apache Kalumet Agent Update Report
          <xsl:value-of select="updatelog/@title"/>
        </div>
        <div id="central">
          <br/>
          <table width="90%" border="1" cellspacing="0" cellpadding="2">
            <tr>
              <td>
                <b>Updated Environment</b>
              </td>
              <td>
                <xsl:value-of select="updatelog/@title"/>
              </td>
            </tr>
            <tr>
              <td>
                <b>Final Status</b>
              </td>
              <td>
                <xsl:value-of select="updatelog/@status"/>
              </td>
            </tr>
            <tr>
              <td>
                <b>Update Date</b>
              </td>
              <td>
                <xsl:value-of select="updatelog/@time"/>
              </td>
            </tr>
            <tr>
              <td>
                <b>Errors</b>
              </td>
              <td>
                <font color="red">
                  <xsl:apply-templates select="updatelog/updatemessage[@priority = 'error']"/>
                </font>
                <br/>
              </td>
            </tr>
            <tr>
              <td>
                <b>Warnings</b>
              </td>
              <td>
                <font color="red">
                  <xsl:apply-templates select="updatelog/updatemessage[@priority = 'warn']"/>
                </font>
                <br/>
              </td>
            </tr>
            <tr>
              <td>
                <b>Applications Summary</b>
              </td>
              <td>
                <xsl:apply-templates select="updatelog/updatemessage[@priority = 'summary']"/>
              </td>
            </tr>
            <tr>
              <td>
                <b>Output Log</b>
              </td>
              <td>
                <font size="1">
                  <xsl:apply-templates select="updatelog/updatemessage"/>
                </font>
              </td>
            </tr>
          </table>
        </div>
        <div id="footer">
          <hr/>
          Copyright (c) 2011 - Apache Kalumet - All Rights Reserved
        </div>
      </body>
    </html>
  </xsl:template>

  <xsl:template match="updatemessage">
    <ul>
      <li>
        <xsl:value-of select="."/>
      </li>
    </ul>
  </xsl:template>

  <xsl:template name="style">
    <style>
         <xsl:text>
body {
  margin: 0;
  padding: 0;
  background-color: #ffffff;
}

#header {
  background-color: #f0f0f0;
  width: 100%;
  font: bold 14px helvetica, sans-serif;
  color: black;
}

#central {
  width: 100%;
  font: 10px helvetica, sans-serif;
  align: center;
}

#central table {
  background-color: #bfbfbf;
  margin-left: 5%;
}

#central tr {
  vertical-align: top;
}

#footer {
  width: 100%;
  clear: both;
  margin-top: 10px;
  padding-top: 10px;
  position: relative;
  font: 8px gill sans, helvetica, sans-serif;
  text-align: center;
}

         </xsl:text>
    </style>
  </xsl:template>

</xsl:stylesheet>
