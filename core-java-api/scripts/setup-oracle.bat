@echo off
REM =============================================================================
REM Setup script for Oracle 19c XE Docker
REM =============================================================================

echo ================================================
echo   Oracle 19c XE Docker Setup
echo   (Express Edition - FREE, no license required)
echo ================================================
echo.

REM Check if Docker is running
docker version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Docker is not running or not installed.
    echo Please install Docker Desktop from: https://docker.com
    exit /b 1
)

echo [1/3] Starting Oracle 19c container...
docker-compose up -d

echo.
echo [2/3] Waiting for Oracle to be ready...
echo This may take 5-10 minutes on first start...
echo.

REM Wait for Oracle to be ready
docker exec oracle19c healthcheck.sh >nul 2>&1
if errorlevel 1 (
    echo Waiting for Oracle database to initialize...
    echo Please wait...
)

REM Wait up to 10 minutes for Oracle to be ready
set /a counter=0
:wait_loop
timeout /t 30 /nobreak >nul
docker exec oracle19c healthcheck.sh >nul 2>&1
if errorlevel 1 (
    set /a counter+=1
    if %counter% LSS 20 (
        echo Still initializing... (%counter%/20)
        goto wait_loop
    )
)

echo.
echo [3/3] Running schema.sql...
echo.

REM Run schema.sql
docker exec -i oracle19c sqlplus system/oracle19c_password@//localhost:1521/ORCL @/opt/oracle/scripts/startup/schema.sql

echo.
echo ================================================
echo   Oracle 19c XE is ready!
echo ================================================
echo.
echo Connection details:
echo   Host: localhost
echo   Port: 1521
echo   SID/Service: ORCL
echo   Username: system
echo   Password: oracle19c_password
echo.
echo JDBC Connection String:
echo   jdbc:oracle:thin:@//localhost:1521/ORCL
echo.
echo Environment variables for the app:
echo   DB_HOST=localhost
echo   DB_PORT=1521
echo   DB_NAME=ORCL
echo   DB_USER=system
echo   DB_PASSWORD=oracle19c_password
echo.
echo ================================================
echo   To stop Oracle: docker-compose down
echo   To remove all data: docker-compose down -v
echo ================================================
echo.