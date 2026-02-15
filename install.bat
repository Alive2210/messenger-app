@echo off
setlocal EnableDelayedExpansion

echo ========================================
echo    MESSENGER SERVER AUTO-INSTALLER
echo ========================================
echo.

:: Colors
set "GREEN=[92m"
set "RED=[91m"
set "YELLOW=[93m"
set "NC=[0m"

:: Check for admin privileges
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo %RED%This script requires administrator privileges.%NC%
    echo Please run as administrator.
    pause
    exit /b 1
)

echo [INFO] Checking prerequisites...

:: Check Java
echo [INFO] Checking Java installation...
java -version >nul 2>&1
if %errorLevel% neq 0 (
    echo %YELLOW%Java not found. Installing Java 17...%NC%
    
    :: Download and install Java 17
    set "JAVA_INSTALLER=jdk-17_windows-x64_bin.exe"
    set "JAVA_URL=https://download.oracle.com/java/17/latest/%JAVA_INSTALLER%"
    
    echo [INFO] Downloading Java 17...
    powershell -Command "Invoke-WebRequest -Uri '%JAVA_URL%' -OutFile '%TEMP%\%JAVA_INSTALLER%'" 2>nul
    
    if exist "%TEMP%\%JAVA_INSTALLER%" (
        echo [INFO] Installing Java 17...
        start /wait "" "%TEMP%\%JAVA_INSTALLER%" /s
        del "%TEMP%\%JAVA_INSTALLER%"
        echo %GREEN%Java 17 installed successfully%NC%
    ) else (
        echo %RED%Failed to download Java. Please install manually from:%NC%
        echo https://www.oracle.com/java/technologies/downloads/#java17
        pause
        exit /b 1
    )
) else (
    for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
        set "JAVA_VERSION=%%g"
        set "JAVA_VERSION=!JAVA_VERSION:"=!"
    )
    echo %GREEN%Java found: !JAVA_VERSION!%NC%
)

:: Check Maven
echo [INFO] Checking Maven installation...
mvn -version >nul 2>&1
if %errorLevel% neq 0 (
    echo %YELLOW%Maven not found. Installing Maven...%NC%
    
    set "MAVEN_VERSION=3.9.6"
    set "MAVEN_ZIP=apache-maven-%MAVEN_VERSION%-bin.zip"
    set "MAVEN_URL=https://dlcdn.apache.org/maven/maven-3/%MAVEN_VERSION%/binaries/%MAVEN_ZIP%"
    
    echo [INFO] Downloading Maven...
    powershell -Command "Invoke-WebRequest -Uri '%MAVEN_URL%' -OutFile '%TEMP%\%MAVEN_ZIP%'" 2>nul
    
    if exist "%TEMP%\%MAVEN_ZIP%" (
        echo [INFO] Installing Maven...
        powershell -Command "Expand-Archive -Path '%TEMP%\%MAVEN_ZIP%' -DestinationPath 'C:\' -Force"
        
        :: Add to PATH
        setx /M PATH "C:\apache-maven-%MAVEN_VERSION%\bin;%PATH%" >nul 2>&1
        set "PATH=C:\apache-maven-%MAVEN_VERSION%\bin;%PATH%"
        
        del "%TEMP%\%MAVEN_ZIP%"
        echo %GREEN%Maven installed successfully%NC%
    ) else (
        echo %RED%Failed to download Maven. Please install manually.%NC%
        pause
        exit /b 1
    )
) else (
    for /f "tokens=3" %%g in ('mvn -version 2^>^&1 ^| findstr /i "Apache Maven"') do (
        echo %GREEN%Maven found: %%g%NC%
    )
)

:: Check Docker
echo [INFO] Checking Docker installation...
docker --version >nul 2>&1
if %errorLevel% neq 0 (
    echo %YELLOW%Docker not found. Please install Docker Desktop:%NC%
    echo https://www.docker.com/products/docker-desktop
    echo.
    echo After installing Docker, please run this script again.
    pause
    exit /b 1
) else (
    for /f "tokens=3" %%g in ('docker --version') do (
        echo %GREEN%Docker found: %%g%NC%
    )
)

:: Check Docker Compose
docker-compose --version >nul 2>&1
if %errorLevel% neq 0 (
    docker compose version >nul 2>&1
    if %errorLevel% neq 0 (
        echo %RED%Docker Compose not found. Please install Docker Desktop properly.%NC%
        pause
        exit /b 1
    ) else (
        echo %GREEN%Docker Compose found (plugin)%NC%
    )
) else (
    echo %GREEN%Docker Compose found%NC%
)

:: Check if Docker is running
docker info >nul 2>&1
if %errorLevel% neq 0 (
    echo %RED%Docker is not running. Please start Docker Desktop.%NC%
    pause
    exit /b 1
)

echo.
echo ========================================
echo   PREREQUISITES CHECK PASSED
echo ========================================
echo.

:: Create directories
echo [INFO] Creating directories...
if not exist "logs" mkdir logs
if not exist "data\postgres" mkdir data\postgres
if not exist "data\minio" mkdir data\minio
if not exist "data\rabbitmq" mkdir data\rabbitmq
echo %GREEN%Directories created%NC%

:: Generate .env file if not exists
if not exist ".env" (
    echo [INFO] Generating .env file with secure passwords...
    
    :: Generate random passwords using PowerShell
    for /f %%a in ('powershell -Command "-join ((65..90) + (97..122) + (48..57) | Get-Random -Count 32 | %% {[char]$_})"') do set "DB_PASSWORD=%%a"
    for /f %%a in ('powershell -Command "-join ((65..90) + (97..122) + (48..57) | Get-Random -Count 64 | %% {[char]$_})"') do set "JWT_SECRET=%%a"
    for /f %%a in ('powershell -Command "-join ((65..90) + (97..122) + (48..57) | Get-Random -Count 24 | %% {[char]$_})"') do set "MINIO_ACCESS_KEY=%%a"
    for /f %%a in ('powershell -Command "-join ((65..90) + (97..122) + (48..57) | Get-Random -Count 48 | %% {[char]$_})"') do set "MINIO_SECRET_KEY=%%a"
    for /f %%a in ('powershell -Command "-join ((65..90) + (97..122) + (48..57) | Get-Random -Count 24 | %% {[char]$_})"') do set "RABBITMQ_PASSWORD=%%a"
    
    (
        echo # Database Configuration
        echo DB_USERNAME=messenger_user
        echo DB_PASSWORD=!DB_PASSWORD!
        echo DB_NAME=messenger_db
        echo.
        echo # JWT Configuration
        echo JWT_SECRET=!JWT_SECRET!
        echo.
        echo # MinIO Configuration
        echo MINIO_ACCESS_KEY=!MINIO_ACCESS_KEY!
        echo MINIO_SECRET_KEY=!MINIO_SECRET_KEY!
        echo.
        echo # RabbitMQ Configuration
        echo RABBITMQ_USER=messenger
        echo RABBITMQ_PASS=!RABBITMQ_PASSWORD!
        echo.
        echo # Application Configuration
        echo SERVER_URL=http://localhost:8080
        echo WEBSOCKET_URL=/ws
        echo.
        echo # Generated on %date% %time%
    ) > .env
    
    echo %GREEN%.env file generated with secure passwords%NC%
    echo [INFO] Please review .env file and modify settings if needed
) else (
    echo %GREEN%.env file already exists%NC%
)

echo.
echo ========================================
echo   READY TO INSTALL
echo ========================================
echo.
echo This will:
echo   1. Build the application with Maven
echo   2. Run all tests
echo   3. Start all services with Docker Compose
echo.
set /p CONTINUE="Continue? (y/n): "
if /i not "%CONTINUE%"=="y" (
    echo Installation cancelled.
    pause
    exit /b 0
)

echo.
echo ========================================
echo   BUILDING APPLICATION
echo ========================================
echo.

:: Build application
call mvn clean package -DskipTests
if %errorLevel% neq 0 (
    echo %RED%Build failed!%NC%
    pause
    exit /b 1
)

echo.
echo %GREEN%Build successful!%NC%
echo.

:: Run tests
echo ========================================
echo   RUNNING TESTS
echo ========================================
echo.
call mvn test
if %errorLevel% neq 0 (
    echo %YELLOW%Some tests failed!%NC%
    set /p CONTINUE="Continue anyway? (y/n): "
    if /i not "!CONTINUE!"=="y" (
        exit /b 1
    )
) else (
    echo %GREEN%All tests passed!%NC%
)

echo.
echo ========================================
echo   STARTING SERVICES
echo ========================================
echo.

:: Start Docker Compose
docker-compose up -d
if %errorLevel% neq 0 (
    echo %RED%Failed to start services!%NC%
    pause
    exit /b 1
)

echo.
echo [INFO] Waiting for services to be ready...
timeout /t 15 /nobreak >nul

:: Check if services are running
docker-compose ps | findstr "Up" >nul
if %errorLevel% neq 0 (
    echo %YELLOW%Some services may still be starting...%NC%
    echo [INFO] Check status with: docker-compose ps
)

echo.
echo ========================================
echo    INSTALLATION COMPLETE
echo ========================================
echo.
echo %GREEN%Application is running!%NC%
echo.
echo Access URLs:
echo   - Application: http://localhost:8080
echo   - Health Check: http://localhost:8080/actuator/health
echo   - RabbitMQ Management: http://localhost:15672 (guest/guest)
echo   - MinIO Console: http://localhost:9001 (minioadmin/minioadmin)
echo.
echo To stop services: docker-compose down
echo To view logs: docker-compose logs -f
echo.
pause
