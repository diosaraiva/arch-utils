@echo off
setlocal EnableDelayedExpansion
REM =============================================================================
REM  PlantUML GUI - user launcher (Windows)
REM -----------------------------------------------------------------------------
REM  Double-click it: it just starts the app, no questions asked.
REM  Developers who want the build/clean menu should use dev_java_windows.bat.
REM =============================================================================

set "SCRIPT_DIR=%~dp0"
if "%SCRIPT_DIR:~-1%"=="\" set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"
set "JAR_NAME=plantuml-gui-java.jar"

where java >nul 2>&1
if errorlevel 1 (
    echo [FAIL]  'java' was not found on PATH. Install Java 17 or newer and try again.
    pause
    exit /b 1
)

REM Unzipped release first, then the developer build output one level up.
set "JAR_FILE="
if exist "%SCRIPT_DIR%\%JAR_NAME%" set "JAR_FILE=%SCRIPT_DIR%\%JAR_NAME%"
if not defined JAR_FILE if exist "%SCRIPT_DIR%\..\target\%JAR_NAME%" set "JAR_FILE=%SCRIPT_DIR%\..\target\%JAR_NAME%"

if not defined JAR_FILE (
    echo [FAIL]  %JAR_NAME% was not found next to this script.
    echo         Download the release zip from https://github.com/diosaraiva/plantuml-gui/releases
    pause
    exit /b 1
)

REM Keep java_config.ini beside the JAR; otherwise the app would create it one level up.
start "" javaw -Dplantumlgui.config="%SCRIPT_DIR%\java_config.ini" -jar "%JAR_FILE%" %*
exit /b 0
