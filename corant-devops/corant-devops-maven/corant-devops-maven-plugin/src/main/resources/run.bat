@echo off

setlocal enabledelayedexpansion

TITLE #APPLICATION_NAME#

set MAIN_CLASS=#MAIN_CLASS#

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

@setlocal
rem extract the options from the JVM options file %CFG_JVM_OPTS%
rem such options are the lines beginning with '-', thus "findstr /b"
for /F "usebackq delims=" %%a in (`findstr /b \- "%CFG_JVM_OPTS%"`) do set JVM_OPTIONS=!JVM_OPTIONS! %%a
@endlocal & set CORANT_JVM_OPTS=%JVM_OPTIONS% %CORANT_JVM_OPTS%


set CORANT_JVM_OPTS=%CORANT_JVM_OPTS% -classpath %CLASS_PATH%
set CORANT_JVM_OPTS=%CORANT_JVM_OPTS% -Dcorant.config.location=%CFG_DIR%

if not "%DEBUG_ARGS%"=="" set JVM_ARGS=%JVM_ARGS% %DEBUG_ARGS%

"%_JAVACMD%" %CORANT_JVM_OPTS% %MAIN_CLASS%

endlocal
GOTO :EOF

:EOF