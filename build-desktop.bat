@echo off
setlocal EnableDelayedExpansion
rem ============================================================
rem  My Pixel Dungeon - desktop build helper (Windows)
rem  Usage:
rem    build-desktop.bat              -> run the game in debug mode (fast iteration)
rem    build-desktop.bat :desktop:release   -> build a distributable JAR
rem    build-desktop.bat :desktop:jpackageImage -> build a standalone .exe (needs JDK17 jpackage)
rem  NOTE: keep this file ASCII-only (no Chinese) to avoid GBK codepage parse bugs.
rem ============================================================

set "JAVA_HOME=D:\PD\tools\jdk-21.0.12.1+1"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "GRADLE_USER_HOME=D:\PD\.gradle"

rem [MyPD-DEBUG] game data dir (same path as in the crash report) + full log to file
set "MYPD_DATA_DIR=C:\Users\14675\AppData\Roaming\.mypd\My Pixel Dungeon"
set "MYPD_LOG=%~dp0desktop-debug.log"

cd /d "%~dp0"
if "%~1"=="" (set "TASK=:desktop:debug") else (set "TASK=%~1")

echo [MyPD] JAVA_HOME=%JAVA_HOME%
echo [MyPD] Gradle task: %TASK%
echo.

rem ==================== [MyPD-DEBUG] preflight self-check ====================
echo [MyPD-DEBUG] 1. game data dir: %MYPD_DATA_DIR%
if not exist "%MYPD_DATA_DIR%\" (
    echo [MyPD-DEBUG]    result: dir does not exist - OK, will be created on first run
    goto :after_check
)
echo [MyPD-DEBUG]    result: dir exists
echo [MyPD-DEBUG] 2. attrib of dir / settings.xml (R = read-only = access denied):
attrib "%MYPD_DATA_DIR%"
if exist "%MYPD_DATA_DIR%\settings.xml" attrib "%MYPD_DATA_DIR%\settings.xml"
echo [MyPD-DEBUG] 3. write test (simulates the game saving settings.xml):
(echo mypd-write-test>"%MYPD_DATA_DIR%\_write_test.tmp") 2>nul
if exist "%MYPD_DATA_DIR%\_write_test.tmp" (
    echo [MyPD-DEBUG]    result: WRITE OK - dir is writable, permission is fine
    del /q "%MYPD_DATA_DIR%\_write_test.tmp" >nul 2>&1
) else (
    echo [MyPD-DEBUG]    result: WRITE FAILED - dir not writable for current user
    echo [MyPD-DEBUG]    suspects: read-only attr / ACL / file locked / antivirus
)
echo [MyPD-DEBUG] 4. leftover game process check (may lock settings.xml):
set "FOUND=0"
for /f "delims=" %%P in ('powershell -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { $_.CommandLine -like '*shatteredpixeldungeon*' } | ForEach-Object { $_.ProcessId }" 2^>nul') do (
    echo [MyPD-DEBUG]    leftover game process PID=%%P - kill it first
    set "FOUND=1"
)
if "!FOUND!"=="0" echo [MyPD-DEBUG]    no leftover game process
:after_check
echo [MyPD-DEBUG] self-check done. Gradle output below (also saved to %MYPD_LOG%):
echo [MyPD-DEBUG] note: no scrolling output while the game is running is normal.
echo ================================================================
echo.

call "D:\PD\tools\gradle-9.4.0\bin\gradle.bat" %TASK% > "%MYPD_LOG%" 2>&1
set "EC=%ERRORLEVEL%"
type "%MYPD_LOG%"
echo.
echo ================================================================
echo [MyPD] exit code: %EC%
echo [MyPD-DEBUG] full output saved to: %MYPD_LOG%
endlocal
pause
