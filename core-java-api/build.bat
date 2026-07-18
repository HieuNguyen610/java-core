@echo off
REM =============================================================================
REM Build Script for Core Java API - No Maven required
REM =============================================================================
REM Usage:
REM   build.bat compile    - Compile Java sources
REM   build.bat package   - Build JAR with lib dependencies
REM   build.bat run [port] - Run the application
REM   build.bat clean     - Clean build outputs
REM =============================================================================

setlocal enabledelayedexpansion

set "PROJECT_DIR=%~dp0"
set "SRC_DIR=%PROJECT_DIR%src\main\java"
set "BUILD_DIR=%PROJECT_DIR%target\classes"
set "LIB_DIR=%PROJECT_DIR%lib"
set "MANIFEST_FILE=%PROJECT_DIR%MANIFEST.MF"
set "MAIN_CLASS=com.example.api.corejavaproject.server.Main"

REM Default port
set "PORT=8080"

REM Parse command
set "CMD=%1"

if "%CMD%"=="" goto usage
if "%CMD%"=="compile" goto compile
if "%CMD%"=="package" goto package
if "%CMD%"=="run" goto run
if "%CMD%"=="clean" goto clean
goto usage

:usage
echo Usage: build.bat [compile^|package^|run^|clean] [port]
echo.
echo Commands:
echo   compile   - Compile Java sources to target\classes
echo   package   - Create executable JAR in target
echo   run       - Run the application on port 8080
echo   run 9090  - Run on custom port (9090)
echo   clean     - Remove target directory
exit /b 1

:compile
echo Compiling Java sources...
if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"
if not exist "%LIB_DIR%\ojdbc8.jar" (
    echo ERROR: ojdbc8.jar not found in lib
    echo Please download Oracle JDBC driver and place it in lib
    exit /b 1
)
javac -d "%BUILD_DIR%" -cp "%LIB_DIR%\*" -sourcepath "%SRC_DIR%" "%SRC_DIR%\com\example\api\corejavaproject\**\*.java" 2>nul
if errorlevel 1 (
    echo Compilation failed!
    exit /b 1
)
echo Compilation successful.
echo Output: %BUILD_DIR%
exit /b 0

:package
echo Packaging application...
call :compile
if errorlevel 1 exit /b 1

if not exist "%PROJECT_DIR%target" mkdir "%PROJECT_DIR%target"

REM Create JAR with manifest using traditional cf syntax
echo Creating JAR file...
jar cfm "%PROJECT_DIR%target\core-java-api-1.0.0.jar" "%MANIFEST_FILE%" -C "%BUILD_DIR%" .

REM Copy lib for classpath reference
if not exist "%PROJECT_DIR%target\lib" mkdir "%PROJECT_DIR%target\lib"
copy "%LIB_DIR%\*.jar" "%PROJECT_DIR%target\lib\" >nul

echo Packaging complete.
echo Output: %PROJECT_DIR%target\core-java-api-1.0.0.jar
exit /b 0

:run
if "%2" neq "" set "PORT=%2"
echo Running Core Java API on port %PORT%...

REM Check if compiled classes exist
if not exist "%BUILD_DIR%\com\example\api\corejavaproject\server\Main.class" (
    echo Classes not found. Compiling first...
    call :compile
    if errorlevel 1 exit /b 1
)

java -cp "%BUILD_DIR%;%LIB_DIR%\*" %MAIN_CLASS% %PORT%
exit /b 0

:clean
echo Cleaning build outputs...
if exist "%PROJECT_DIR%target" (
    rmdir /s /q "%PROJECT_DIR%target"
    echo Cleaned target directory.
) else (
    echo Nothing to clean.
)
exit /b 0

endlocal