@echo off

setlocal enabledelayedexpansion

TITLE #APPLICATION_NAME#

set MAIN_CLASS=#MAIN_CLASS#
set USED_CONFIG_LOCATION=#USED_CONFIG_LOCATION#
set USED_CONFIG_PROFILE=#USED_CONFIG_PROFILE#
set ADDITIONAL_VM_ARGUMENTS=#ADDITIONAL_VM_ARGUMENTS#
set APPLICATION_ARGUMENTS=#APPLICATION_ARGUMENTS#
set APPLICATION_NAME=#APPLICATION_NAME#


if "%OS%" == "Windows_NT" (
  set "DIR_PATH=%~dp0%"
) else (
  set DIR_PATH=.\
)

pushd "%DIR_PATH%.."
set "ROOT_DIR=%CD%"
popd

set LIB_DIR=%ROOT_DIR%\lib
set APP_DIR=%ROOT_DIR%\app
set CFG_DIR=%ROOT_DIR%\cfg
set LCF_URL=%CFG_DIR%\log4j2.xml

set CLASS_PATH=%CFG_DIR%\*;%APP_DIR%\*;%LIB_DIR%\*

set _JAVACMD=%JAVACMD%

if "%JAVA_HOME%" == "" goto NO_JAVA_HOME
if not exist "%JAVA_HOME%\bin\java.exe" goto NO_JAVA_HOME
if "%_JAVACMD%" == "" set _JAVACMD=%JAVA_HOME%\bin\java.exe
goto RUN_JAVA

:NO_JAVA_HOME
if "%_JAVACMD%" == "" set _JAVACMD=java.exe
echo.
echo Warning: JAVA_HOME environment variable is not set.
echo.

:RUN_JAVA

set "CFG_JVM_OPTS=%~dp0\..\cfg\jvm.options"
set CORANT_JVM_OPTS=
if NOT "%ADDITIONAL_VM_ARGUMENTS%" == "" set CORANT_JVM_OPTS=%CORANT_JVM_OPTS% %ADDITIONAL_VM_ARGUMENTS%

@setlocal
rem extract the options from the JVM options file %CFG_JVM_OPTS%
rem such options are the lines beginning with '-', thus "findstr /b"
for /F "usebackq delims=" %%a in (`findstr /b \- "%CFG_JVM_OPTS%"`) do set JVM_OPTIONS=!JVM_OPTIONS! %%a
@endlocal & set CORANT_JVM_OPTS=%JVM_OPTIONS% %CORANT_JVM_OPTS%

set CORANT_JVM_OPTS=%CORANT_JVM_OPTS% -classpath "%CLASS_PATH%"

if "%USED_CONFIG_LOCATION%" == "" set USED_CONFIG_LOCATION=filesystem:%CFG_DIR%

set CORANT_JVM_OPTS=%CORANT_JVM_OPTS% -Dcorant.config.location="%USED_CONFIG_LOCATION%"

if NOT "%USED_CONFIG_PROFILE%" == "" set CORANT_JVM_OPTS=%CORANT_JVM_OPTS% -Dcorant.config.profile="%USED_CONFIG_PROFILE%"

if NOT "%APPLICATION_NAME%" == "" set CORANT_JVM_OPTS=%CORANT_JVM_OPTS% -Dcorant.application-name="%APPLICATION_NAME%"

if exist "%LCF_URL%" set CORANT_JVM_OPTS=%CORANT_JVM_OPTS% -Dlog4j.configurationFile="%LCF_URL%"

set CORANT_JVM_OPTS=%CORANT_JVM_OPTS% -Dcorant.application.root-dir=filesystem:"%ROOT_DIR%"

if NOT "%DEBUG_ARGS%"=="" set JVM_ARGS=%JVM_ARGS% %DEBUG_ARGS%

"%_JAVACMD%" %CORANT_JVM_OPTS% %MAIN_CLASS% %APPLICATION_ARGUMENTS%

endlocal
GOTO :EOF

:EOF