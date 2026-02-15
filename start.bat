@echo off
:: Quick Start Script for Messenger Server

echo ========================================
echo    MESSENGER SERVER - QUICK START
echo ========================================
echo.

:: Check if .env exists
if not exist ".env" (
    echo [ERROR] .env file not found!
    echo Please run install.bat first to set up the environment.
    pause
    exit /b 1
)

:: Check Docker
docker info >nul 2>&1
if %errorLevel% neq 0 (
    echo [ERROR] Docker is not running!
    echo Please start Docker Desktop first.
    pause
    exit /b 1
)

echo [INFO] Starting Messenger Server...
echo.

:: Start services
docker-compose up -d

if %errorLevel% neq 0 (
    echo [ERROR] Failed to start services!
    pause
    exit /b 1
)

echo.
echo [INFO] Waiting for services to be ready...
timeout /t 10 /nobreak >nul

echo.
echo ========================================
echo    SERVER STARTED SUCCESSFULLY
echo ========================================
echo.
echo Access URLs:
echo   - Application:     http://localhost:8080
echo   - Health Check:    http://localhost:8080/actuator/health
echo   - RabbitMQ:        http://localhost:15672 (guest/guest)
echo   - MinIO Console:   http://localhost:9001 (minioadmin/minioadmin)
echo.
echo Useful commands:
echo   - View logs:       docker-compose logs -f
echo   - Stop server:     docker-compose down
echo   - Restart:         docker-compose restart
echo.
pause
