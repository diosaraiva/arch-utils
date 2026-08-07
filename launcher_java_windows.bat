@echo off
setlocal EnableDelayedExpansion
REM =============================================================================
REM  PlantUML GUI - Windows launcher
REM -----------------------------------------------------------------------------
REM  Same behaviour as launcher_java_unix.sh:
REM    1) Run without compiling (java source-code launcher, no .class files)
REM    2) Compile (only if needed) then run from compiled classes
REM    3) Clean build artifacts (also restores the default configuration)
REM    4) Restore default configuration
REM    5) Exit
REM  Every setting is read from java_config.ini, shared with the application.
REM =============================================================================

set "SCRIPT_DIR=%~dp0"
if "%SCRIPT_DIR:~-1%"=="\" set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"
set "CONFIG_FILE=%SCRIPT_DIR%\java_config.ini"

REM Missing active config? Recreate it from the bundled default.
REM Both branches end with the launcher settings loaded.
if exist "%CONFIG_FILE%" (
    call :load_settings
) else (
    call :restore_config quiet
    if errorlevel 1 call :load_settings
)

cd /d "%PROJECT_DIR%" || (
    echo [FAIL]  Cannot enter project directory: %PROJECT_DIR%
    exit /b 1
)

where java >nul 2>&1
if errorlevel 1 (
    echo [FAIL]  'java' was not found on PATH. Install a JDK ^(17+ recommended^) and retry.
    exit /b 1
)

REM =============================================================================
REM  Menu loop
REM =============================================================================
:menu
call :java_major_version JAVA_MAJOR
call :build_state BUILD_STATE
echo.
echo ========================================
echo          PlantUML GUI - Launcher
echo ========================================
echo   project : %PROJECT_DIR%
echo   config  : %CONFIG_FILE%
echo   java    : major %JAVA_MAJOR%
echo   build   : %BUILD_STATE%
echo ----------------------------------------
echo   1^) Run without compiling ^(source mode^)
echo   2^) Compile ^(if needed^) and run
echo   3^) Clean build artifacts + reset config
echo   4^) Restore default configuration
echo   5^) Exit
echo ----------------------------------------
set "CHOICE="
set /p "CHOICE=Choose an option [1-5]: "
set "CHOICE=%CHOICE: =%"

if "%CHOICE%"=="1" ( call :run_from_source          & call :pause_menu & goto :menu )
if "%CHOICE%"=="2" ( call :compile_and_run          & call :pause_menu & goto :menu )
if "%CHOICE%"=="3" ( call :clean_artifacts          & call :pause_menu & goto :menu )
if "%CHOICE%"=="4" ( call :restore_config_interactive & call :pause_menu & goto :menu )
if "%CHOICE%"=="5" ( echo [INFO]  Bye^! & exit /b 0 )
if "%CHOICE%"==""  ( echo [FAIL]  No option entered. Please type a number between 1 and 5. & goto :menu )
echo [FAIL]  Invalid option: '%CHOICE%'. Please type a number between 1 and 5.
goto :menu

REM =============================================================================
REM  Option 1 - Run directly from sources (no javac, no .class files)
REM =============================================================================
REM   * Java 11+ -> single-file source programs (JEP 330)
REM   * Java 22+ -> multi-file source programs (JEP 458), required here because
REM                 the project spans many classes.
:run_from_source
if %JAVA_MAJOR% EQU 0 (
    echo [WARN]  Could not determine the Java version - continuing anyway.
) else if %JAVA_MAJOR% LSS 11 (
    echo [FAIL]  Java %JAVA_MAJOR% detected. Source-code execution requires Java 11+ ^(22+ for this project^).
    echo [FAIL]  Use option 2 ^(compile then run^) instead.
    exit /b 1
) else if %JAVA_MAJOR% LSS 22 (
    echo [WARN]  Java %JAVA_MAJOR% detected. Multi-file source execution needs Java 22+ ^(JEP 458^).
    echo [WARN]  This project has multiple classes, so the run will most likely fail.
    set "ANSWER="
    set /p "ANSWER=Try anyway? [y/N]: "
    if /i not "!ANSWER!"=="y" if /i not "!ANSWER!"=="yes" (
        echo [INFO]  Aborted. Use option 2 ^(compile then run^).
        exit /b 0
    )
)
if not exist "%MAIN_SRC_FILE%" (
    echo [FAIL]  Main source not found: %PROJECT_DIR%\%MAIN_SRC_FILE%
    exit /b 1
)
REM In source-file mode the launcher looks for the other .java files on the class
REM path, so the source root must be on -cp; the resource dir is appended so
REM classpath resources (i18n, plantuml jar) resolve too.
set "CP=%SRC_DIR%"
if exist "%RES_DIR%\" set "CP=%SRC_DIR%;%RES_DIR%"
echo [INFO]  Running from sources ^(Java %JAVA_MAJOR%, no .class files generated^)...
echo [INFO]  Any javac warnings below come from the in-memory compilation step.
java -cp "%CP%" "%MAIN_SRC_FILE%"
set "RC=!errorlevel!"
if "!RC!"=="0" ( echo [ OK ]  Application exited normally. ) else ( echo [FAIL]  Application exited with code !RC!. )
exit /b 0

REM =============================================================================
REM  Option 2 - Compile if needed, then run from compiled classes
REM =============================================================================
:compile_and_run
call :is_build_fresh FRESH
if "%FRESH%"=="1" (
    echo [ OK ]  Compiled classes are up to date - skipping javac.
) else (
    echo [INFO]  Compiled classes are missing or outdated.
    call :compile_project
    if errorlevel 1 exit /b 1
)
set "CP=%OUT_DIR%"
if exist "%RES_DIR%\" set "CP=%OUT_DIR%;%RES_DIR%"
echo [INFO]  Running %MAIN_CLASS% from compiled classes...
java -cp "%CP%" %MAIN_CLASS%
set "RC=!errorlevel!"
if "!RC!"=="0" ( echo [ OK ]  Application exited normally. ) else ( echo [FAIL]  Application exited with code !RC!. )
exit /b 0

REM Freshness check (mirrors is_build_fresh in the shell script):
REM   fresh = output dir exists AND holds .class files AND the main .class exists
REM           AND no *.java is newer than that main .class file.
REM   Windows has no 'find -newer', and %%~tF is locale dependent, so the
REM   comparison is delegated to PowerShell. Without PowerShell the build is
REM   reported as outdated, which is the safe answer (it just recompiles).
:is_build_fresh
set "%~1=0"
if not exist "%OUT_DIR%\" exit /b 0
if not exist "%MAIN_CLASS_FILE%" exit /b 0
set "ANY_CLASS="
for /r "%OUT_DIR%" %%F in (*.class) do ( set "ANY_CLASS=1" & goto :have_class )
:have_class
if not defined ANY_CLASS exit /b 0

where powershell >nul 2>&1
if errorlevel 1 exit /b 0
set "PS_FRESH=$r=(Get-Item '%MAIN_CLASS_FILE%').LastWriteTime; if (Get-ChildItem -Path '%SRC_DIR%' -Recurse -Filter *.java ^| Where-Object { $_.LastWriteTime -gt $r } ^| Select-Object -First 1) { 0 } else { 1 }"
for /f %%R in ('powershell -NoProfile -Command "!PS_FRESH!"') do set "%~1=%%R"
exit /b 0

:compile_project
where javac >nul 2>&1
if errorlevel 1 (
    echo [FAIL]  'javac' was not found on PATH. A full JDK ^(not just a JRE^) is required.
    exit /b 1
)
echo [INFO]  Compiling sources into '%OUT_DIR%'...
if not exist "%OUT_DIR%\" mkdir "%OUT_DIR%"
set "SOURCES_FILE=%TEMP%\plantumlgui-sources.txt"
if exist "%SOURCES_FILE%" del /q "%SOURCES_FILE%"
for /r "%SRC_DIR%" %%F in (*.java) do echo %%~fF>>"%SOURCES_FILE%"
if not exist "%SOURCES_FILE%" (
    echo [FAIL]  No .java files found under '%SRC_DIR%'.
    exit /b 1
)
if defined JAVAC_RELEASE (
    javac --release %JAVAC_RELEASE% -encoding UTF-8 -d "%OUT_DIR%" "@%SOURCES_FILE%"
) else (
    javac -encoding UTF-8 -d "%OUT_DIR%" "@%SOURCES_FILE%"
)
set "RC=%errorlevel%"
del /q "%SOURCES_FILE%" >nul 2>&1
if not "%RC%"=="0" (
    echo [FAIL]  Compilation failed ^(exit code %RC%^).
    exit /b 1
)
set "CLASS_COUNT=0"
for /r "%OUT_DIR%" %%F in (*.class) do set /a CLASS_COUNT+=1
echo [ OK ]  Compilation finished ^(%CLASS_COUNT% class files^).
exit /b 0

REM =============================================================================
REM  Option 3 - Clean build artifacts + restore the default configuration
REM =============================================================================
:clean_artifacts
set "FOUND="
for %%D in (%CLEAN_DIRS%) do if exist "%%D\" set "FOUND=!FOUND! %%D"
set "STRAY=0"
for /r "%SRC_DIR%" %%F in (*.class) do set /a STRAY+=1
echo.
echo The following will be removed ^(under %PROJECT_DIR%^):
if not defined FOUND if "%STRAY%"=="0" echo   - nothing, no build artifacts found
for %%D in (%FOUND%) do (
    set "N=0"
    for /r "%%D" %%F in (*) do set /a N+=1
    echo   - %%D\ ^(!N! files^)
)
if not "%STRAY%"=="0" echo   - %STRAY% stray .class file^(s^) under %SRC_DIR%
echo.
echo And this file will be reset to the bundled defaults:
echo   - %CONFIG_FILE%
echo.
set "ANSWER="
set /p "ANSWER=Confirm clean? [y/N]: "
if /i not "%ANSWER%"=="y" if /i not "%ANSWER%"=="yes" (
    echo [INFO]  Clean cancelled - nothing was removed or reset.
    exit /b 0
)
set "REMOVED_DIRS=0"
set "REMOVED_FILES=0"
for %%D in (%FOUND%) do (
    set "N=0"
    for /r "%%D" %%F in (*) do set /a N+=1
    rd /s /q "%%D"
    if not exist "%%D\" (
        set /a REMOVED_DIRS+=1
        set /a REMOVED_FILES+=!N!
        echo   removed %%D\
    ) else (
        echo [FAIL]  Could not remove %%D
    )
)
if not "%STRAY%"=="0" (
    del /s /q "%SRC_DIR%\*.class" >nul 2>&1
    set /a REMOVED_FILES+=%STRAY%
    echo   removed %STRAY% stray .class file^(s^)
)
echo [ OK ]  Clean complete: %REMOVED_DIRS% directory^(ies^), !REMOVED_FILES! file^(s^) removed.
REM A clean also puts the configuration back to its factory state.
call :restore_config
cd /d "%PROJECT_DIR%" 2>nul
exit /b 0

REM =============================================================================
REM  Option 4 - Restore the default configuration only
REM =============================================================================
:restore_config_interactive
call :config_template TEMPLATE
if not defined TEMPLATE (
    echo [FAIL]  Bundled default not found: ^<module^>\src\main\resources\java_config.ini
    exit /b 1
)
echo.
echo Overwrite the active configuration with the bundled defaults?
echo   from %TEMPLATE%
echo   to   %CONFIG_FILE%
echo.
set "ANSWER="
set /p "ANSWER=Confirm restore? [y/N]: "
if /i not "%ANSWER%"=="y" if /i not "%ANSWER%"=="yes" (
    echo [INFO]  Restore cancelled - the configuration was left untouched.
    exit /b 0
)
call :restore_config
cd /d "%PROJECT_DIR%" 2>nul
exit /b 0

REM =============================================================================
REM  Helpers
REM =============================================================================
REM Locates the bundled factory default shipped in a module's resources dir.
:config_template
set "%~1="
for /d %%D in ("%SCRIPT_DIR%\*") do (
    if exist "%%~fD\src\main\resources\java_config.ini" (
        set "%~1=%%~fD\src\main\resources\java_config.ini"
        exit /b 0
    )
)
exit /b 0

REM Overwrites the active java_config.ini with the bundled default - the same
REM behaviour as the 'Default' button in the app's Config tab.
REM Pass "quiet" to suppress the message during start-up bootstrap.
:restore_config
call :config_template _template
if not defined _template (
    if not "%~1"=="quiet" echo [FAIL]  Bundled default not found: ^<module^>\src\main\resources\java_config.ini
    exit /b 1
)
copy /y "%_template%" "%CONFIG_FILE%" >nul
if errorlevel 1 (
    if not "%~1"=="quiet" echo [FAIL]  Could not write %CONFIG_FILE%
    exit /b 1
)
if not "%~1"=="quiet" echo [ OK ]  Configuration restored from %_template%
call :load_settings
exit /b 0

REM (Re)reads every launcher setting; called at start-up and after a restore.
:load_settings
call :ini_get launcher.projectDir  "plantuml-gui-java"                PROJECT_DIR_NAME
call :ini_get launcher.srcDir      "src\main\java"                    SRC_DIR
call :ini_get launcher.resDir      "src\main\resources"               RES_DIR
call :ini_get launcher.outDir      "bin"                              OUT_DIR
call :ini_get launcher.mainClass   "com.diosaraiva.plantumlgui.Main"  MAIN_CLASS
call :ini_get launcher.cleanDirs   "bin out build target temp output" CLEAN_DIRS
call :ini_get launcher.javacRelease ""                                JAVAC_RELEASE

REM INI paths use forward slashes; Windows tools accept both, but normalise anyway.
set "SRC_DIR=%SRC_DIR:/=\%"
set "RES_DIR=%RES_DIR:/=\%"
set "OUT_DIR=%OUT_DIR:/=\%"

REM The app resolves resources relative to user.dir, so always run from the module
set "PROJECT_DIR=%SCRIPT_DIR%\%PROJECT_DIR_NAME%"
if not exist "%PROJECT_DIR%\" set "PROJECT_DIR=%SCRIPT_DIR%"

set "MAIN_REL_PATH=%MAIN_CLASS:.=\%"
set "MAIN_SRC_FILE=%SRC_DIR%\%MAIN_REL_PATH%.java"
set "MAIN_CLASS_FILE=%OUT_DIR%\%MAIN_REL_PATH%.class"
exit /b 0

REM Reads one key from java_config.ini: call :ini_get <key> "<fallback>" <var>
:ini_get
setlocal EnableDelayedExpansion
set "_ini_value="
if exist "%CONFIG_FILE%" (
    for /f "usebackq tokens=1,* delims==" %%A in ("%CONFIG_FILE%") do (
        set "_k=%%A"
        set "_v=%%B"
        set "_k=!_k: =!"
        REM skip comment lines and keep the first match only
        if not "!_k:~0,1!"=="#" if not "!_k:~0,1!"==";" (
            if /i "!_k!"=="%~1" if not defined _ini_value (
                REM strip the single leading space left by 'key = value'
                if "!_v:~0,1!"==" " set "_v=!_v:~1!"
                set "_ini_value=!_v!"
            )
        )
    )
)
if not defined _ini_value set "_ini_value=%~2"
endlocal & set "%~3=%_ini_value%"
exit /b 0

REM Major Java version (8, 11, 21, 26...); 0 when it cannot be determined.
:java_major_version
set "_raw="
for /f "tokens=2 delims==" %%V in ('java -XshowSettings:properties -version 2^>^&1 ^| findstr /c:"java.specification.version"') do set "_raw=%%V"
if not defined _raw (
    for /f "tokens=3" %%V in ('java -version 2^>^&1 ^| findstr /i "version"') do set "_raw=%%~V"
)
set "_raw=%_raw: =%"
set "_major=0"
if defined _raw (
    if "%_raw:~0,2%"=="1." (
        for /f "tokens=2 delims=." %%M in ("%_raw%") do set "_major=%%M"
    ) else (
        for /f "tokens=1 delims=." %%M in ("%_raw%") do set "_major=%%M"
    )
)
set "%~1=%_major%"
exit /b 0

:build_state
call :is_build_fresh _fresh
if "%_fresh%"=="1" (
    set "%~1=up to date"
) else if exist "%MAIN_CLASS_FILE%" (
    set "%~1=outdated"
) else (
    set "%~1=not compiled"
)
exit /b 0

:pause_menu
echo.
set "DUMMY="
set /p "DUMMY=Press ENTER to return to the menu..."
echo.
exit /b 0
