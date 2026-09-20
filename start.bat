@echo off
setlocal EnableExtensions
cd /d "%~dp0"

rem AILEAPRO — one-click local start (Windows)
rem Starts Docker (Postgres, Redis, MinIO), seeds if needed, API :8090, web :3000,
rem and the lab executor + worker skeletons.
rem
rem Layout: frontend/  backend/  infra/
rem
rem Usage:
rem   start.bat              start everything
rem   start.bat down         stop everything
rem   start.bat restart      stop then start
rem   start.bat status       show what is running
rem   start.bat logs         follow API/web logs
rem   start.bat seed         force re-seed curriculum
rem
rem macOS / Linux: ./start.sh   (and ./stop.sh)

where docker >nul 2>&1
if errorlevel 1 (
  echo Docker is not on PATH. Install Docker Desktop and open it, then run start.bat again.
  pause
  exit /b 1
)

where powershell >nul 2>&1
if errorlevel 1 (
  echo PowerShell was not found. It is required to run infra\scripts\dev.ps1.
  pause
  exit /b 1
)

set "ACTION=%~1"
if "%ACTION%"=="" set "ACTION=up"
set "DEV=%~dp0infra\scripts\dev.ps1"

echo.
echo == AILEAPRO %ACTION%
echo    repo: %CD%
echo.

if /I "%ACTION%"=="up" goto :with_skeletons
if /I "%ACTION%"=="restart" goto :with_skeletons

powershell -NoProfile -ExecutionPolicy Bypass -File "%DEV%" %*
goto :after

:with_skeletons
powershell -NoProfile -ExecutionPolicy Bypass -File "%DEV%" %ACTION% -WithSkeletons
goto :after

:after
set "ERR=%ERRORLEVEL%"
if not "%ERR%"=="0" (
  echo.
  echo Start failed ^(exit %ERR%^). Check Docker Desktop is running, then try again.
  echo Logs: .run\logs\
  pause
  exit /b %ERR%
)

if /I "%ACTION%"=="up" goto :done_open
if /I "%ACTION%"=="restart" goto :done_open
exit /b 0

:done_open
echo.
echo AILEAPRO is up.
echo   Web  http://localhost:3000
echo   API  http://localhost:8090/actuator/health
echo   Docs http://localhost:8090/swagger-ui.html
echo.
echo Stop with:  stop.bat
echo.
pause
exit /b 0
