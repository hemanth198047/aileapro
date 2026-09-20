@echo off
setlocal EnableExtensions
cd /d "%~dp0"

rem AILEAPRO — stop the local stack (Windows)
rem Stops web, API, worker, executor, and Docker containers.
rem Volumes are kept so the database survives.
rem
rem macOS / Linux: ./stop.sh

where powershell >nul 2>&1
if errorlevel 1 (
  echo PowerShell was not found. It is required to run infra\scripts\dev.ps1.
  pause
  exit /b 1
)

echo.
echo == AILEAPRO stop
echo    repo: %CD%
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0infra\scripts\dev.ps1" down
set "ERR=%ERRORLEVEL%"

if not "%ERR%"=="0" (
  echo.
  echo Stop failed ^(exit %ERR%^).
  pause
  exit /b %ERR%
)

echo.
echo AILEAPRO is down. Start again with start.bat
echo.
pause
exit /b 0
