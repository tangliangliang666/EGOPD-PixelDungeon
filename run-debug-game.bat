@echo off
setlocal
rem ============================================================
rem  My Pixel Dungeon - launch the game in DEBUG mode (fast test)
rem  In game: press F2 to open the spawn window.
rem  Double-click this file to start; close the game window to quit.
rem ============================================================

set "JAVA_HOME=D:\PD\tools\jdk-21.0.12.1+1"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "GRADLE_USER_HOME=D:\PD\.gradle"

cd /d "%~dp0"
echo [MyPD] Launching game in debug mode...
echo [MyPD] In-game hotkey: F2 = spawn window
call "D:\PD\tools\gradle-9.4.0\bin\gradle.bat" :desktop:debug
echo [MyPD] Game closed, exit code: %ERRORLEVEL%
endlocal
