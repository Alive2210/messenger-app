#!/bin/bash
# Quick Start Script for Messenger Server

echo "========================================"
echo "   MESSENGER SERVER - QUICK START"
echo "========================================"
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Check if .env exists - if not, run install first
if [ ! -f .env ]; then
    echo -e "${YELLOW}[INFO] .env file not found! Running installation first...${NC}"
    echo ""
    if [ -f ./install.sh ]; then
        chmod +x ./install.sh
        ./install.sh
        if [ $? -ne 0 ]; then
            echo -e "${RED}[ERROR] Installation failed!${NC}"
            exit 1
        fi
    else
        echo -e "${RED}[ERROR] install.sh not found!${NC}"
        exit 1
    fi
    echo ""
fi

# Check Docker
if ! docker info >/dev/null 2>&1; then
    echo -e "${RED}[ERROR] Docker is not running!${NC}"
    echo "Please start Docker first."
    exit 1
fi

echo -e "${GREEN}[INFO] Starting Messenger Server...${NC}"
echo ""

# Start services
if command -v docker-compose &> /dev/null; then
    docker-compose up -d
else
    docker compose up -d
fi

if [ $? -ne 0 ]; then
    echo -e "${RED}[ERROR] Failed to start services!${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}[INFO] Waiting for services to be ready...${NC}"
sleep 10

echo ""
echo "========================================"
echo "   SERVER STARTED SUCCESSFULLY"
echo "========================================"
echo ""
echo "Access URLs:"
echo "  - Application:     http://localhost:8080"
echo "  - Health Check:    http://localhost:8080/actuator/health"
echo "  - RabbitMQ:        http://localhost:15672 (guest/guest)"
echo "  - MinIO Console:   http://localhost:9001 (minioadmin/minioadmin)"
echo ""
echo "Useful commands:"
echo "  - View logs:       docker-compose logs -f"
echo "  - Stop server:     docker-compose down"
echo "  - Restart:         docker-compose restart"
echo ""
