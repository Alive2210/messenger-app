#!/bin/bash

echo "╔════════════════════════════════════════════════════════════╗"
echo "║         🚀 SECURE MESSENGER - AUTO INSTALLER               ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Цвета
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

error_exit() {
    echo -e "${RED}❌ Error: $1${NC}" >&2
    exit 1
}

log() {
    echo -e "${GREEN}✅ $1${NC}"
}

warn() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# Проверка ОС
info "Checking operating system..."
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    OS="linux"
elif [[ "$OSTYPE" == "darwin"* ]]; then
    OS="mac"
elif [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" ]]; then
    OS="windows"
else
    error_exit "Unsupported OS: $OSTYPE"
fi
log "OS: $OS"

# Проверка Docker
info "Checking Docker..."
if ! command -v docker &> /dev/null; then
    error_exit "Docker is not installed. Please install Docker first:\n   https://docs.docker.com/get-docker/"
fi
DOCKER_VERSION=$(docker --version | cut -d ' ' -f3 | cut -d ',' -f1)
log "Docker version: $DOCKER_VERSION"

# Проверка Docker Compose
info "Checking Docker Compose..."
if docker compose version &> /dev/null; then
    COMPOSE_CMD="docker compose"
    COMPOSE_VERSION=$(docker compose version --short)
elif command -v docker-compose &> /dev/null; then
    COMPOSE_CMD="docker-compose"
    COMPOSE_VERSION=$(docker-compose --version | cut -d ' ' -f3 | cut -d ',' -f1)
else
    error_exit "Docker Compose is not installed"
fi
log "Docker Compose: $COMPOSE_VERSION"

# Проверка Java
info "Checking Java..."
if ! command -v java &> /dev/null; then
    warn "Java not found in PATH. Will use Docker to build."
    USE_DOCKER_BUILD=1
else
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d '"' -f2 | cut -d '.' -f1)
    if [ "$JAVA_VERSION" -ge 17 ]; then
        log "Java version: $(java -version 2>&1 | head -n 1 | cut -d '"' -f2)"
        USE_DOCKER_BUILD=0
    else
        warn "Java 17+ required. Will use Docker to build."
        USE_DOCKER_BUILD=1
    fi
fi

# Проверка Maven
info "Checking Maven..."
if [ -f "./mvnw" ]; then
    log "Maven Wrapper found"
    MVN_CMD="./mvnw"
elif command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn -version | head -n 1 | cut -d ' ' -f3)
    log "Maven version: $MVN_VERSION"
    MVN_CMD="mvn"
else
    warn "Maven not found. Will use Docker to build."
    USE_DOCKER_BUILD=1
fi

# Проверка памяти
info "Checking available memory..."
if [ "$OS" == "linux" ]; then
    MEM_AVAILABLE=$(free -m | awk '/^Mem:/{print $7}')
elif [ "$OS" == "mac" ]; then
    MEM_AVAILABLE=$(vm_stat | grep "Pages free" | awk '{print $3}' | sed 's/\.//')
    MEM_AVAILABLE=$((MEM_AVAILABLE * 4096 / 1024 / 1024))
else
    MEM_AVAILABLE=4096  # Default for Windows
fi

if [ "$MEM_AVAILABLE" -lt 2048 ]; then
    error_exit "Insufficient memory. At least 2GB RAM required. Available: ${MEM_AVAILABLE}MB"
fi
log "Available memory: ${MEM_AVAILABLE}MB"

# Проверка дискового пространства
info "Checking disk space..."
DISK_AVAILABLE=$(df -m . | tail -1 | awk '{print $4}')
if [ "$DISK_AVAILABLE" -lt 5120 ]; then
    error_exit "Insufficient disk space. At least 5GB required. Available: ${DISK_AVAILABLE}MB"
fi
log "Available disk space: ${DISK_AVAILABLE}MB"

# Проверка портов
info "Checking ports..."
PORTS="80 443 8080 5432 5672 15672 9000 9001 9090 3000"
for PORT in $PORTS; do
    if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null 2>&1 || netstat -tuln 2>/dev/null | grep -q ":$PORT "; then
        warn "Port $PORT is already in use"
    fi
done
log "Port check passed"

echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║                  🧪 SYSTEM CHECK PASSED                    ║"
echo "╠════════════════════════════════════════════════════════════╣"
echo "║                                                            ║"
echo "║  Docker:           $DOCKER_VERSION                             ║"
echo "║  Docker Compose:   $COMPOSE_VERSION                             ║"
echo "║  Memory:           ${MEM_AVAILABLE}MB available                           ║"
echo "║  Disk Space:       ${DISK_AVAILABLE}MB available                          ║"
echo "║  Build Method:     $([ $USE_DOCKER_BUILD -eq 1 ] && echo "Docker" || echo "Local Maven")                     ║"
echo "║                                                            ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

read -p "🚀 Proceed with deployment? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Deployment cancelled."
    exit 0
fi

echo ""
echo "🎬 Starting deployment..."
echo ""

# Делаем скрипты исполняемыми
chmod +x full-deploy.sh stop.sh logs.sh 2>/dev/null || true
chmod +x mvnw 2>/dev/null || true

# Запускаем деплой
./full-deploy.sh "$@"
