@echo off
rem
rem
rem    Licensed to the Apache Software Foundation (ASF) under one or more
rem    contributor license agreements.  See the NOTICE file distributed with
rem    this work for additional information regarding copyright ownership.
rem    The ASF licenses this file to You under the Apache License, Version 2.0
rem    (the "License"); you may not use this file except in compliance with
rem    the License.  You may obtain a copy of the License at
rem
rem       http://www.apache.org/licenses/LICENSE-2.0
rem
rem    Unless required by applicable law or agreed to in writing, software
rem    distributed under the License is distributed on an "AS IS" BASIS,
rem    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem    See the License for the specific language governing permissions and
rem    limitations under the License.
rem

if not "%ECHO%" == "" echo %ECHO%

setlocal
set DIRNAME=%~dp0%
set PROGNAME=%~nx0%

set KALUMET_AGENT_HOME=%DIRNAME%..
cd %KALUMET_AGENT_HOME%

if "%JAVA_MIN_MEM%" == "" set JAVA_MIN_MEM=64m

if "%JAVA_MAX_MEM%" == "" set JAVA_MAX_MEM=128m

set DEFAULT_JAVA_OPTS=-server -Xms%JAVA_MIN_MEM% -Xmx%JAVA_MAX_MEM% -Dcom.sun.management.jmxremote

rem Load configuration file
if exist "%KALUMET_AGENT_HOME%\conf\agent-rc.cmd" call %KALUMET_AGENT_HOME%\conf\agent-rc.cmd

rem Support for loading native libraries
set PATH=%PATH%;%KALUMET_AGENT_HOME\lib

title Apache Kalumet Agent

rem Setup the Java Virtual Machine
if not "%JAVA%" == "" goto :Check_JAVA_END
    set JAVA=java
    if "%JAVA_HOME%" == "" call :warn JAVA_HOME not set; results may vary
    if not "%JAVA_HOME%" == "" set JAVA=%JAVA_HOME%\bin\java
    if not exist "%JAVA_HOME%" (
        call :warn JAVA_HOME is not valid: %JAVA_HOME%
        goto END
    )
:Check_JAVA_END

rem Setup the classworlds
set CLASSPATH=%KALUMET_HOME%\lib\classworlds-1.1.jar
set CLASSWORLDS_CONF=%KALUMET_HOME%\conf\agent.conf

if "%JAVA_OPTS%" == "" set JAVA_OPTS=%DEFAULT_JAVA_OPTS%

rem Execute the JVM
"%JAVA%" %JAVA_OPTS% -classpath "%CLASSPATH%" -Dclassworlds.conf="%CLASSWORLDS_CONF%" -Dkalumet.agent.home="%KALUMET_AGENT_HOME%" org.codehaus.classworlds.Launcher %*