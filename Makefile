# Makefile for Secure Messenger
# Supports: Linux, macOS
# For Windows use: install.bat and start.bat

.PHONY: help install start stop restart logs status clean test build

# Default target
help:
	@echo "╔════════════════════════════════════════════════════════════╗"
	@echo "║         SECURE MESSENGER - MAKE COMMANDS                   ║"
	@echo "╚════════════════════════════════════════════════════════════╝"
	@echo ""
	@echo "Available commands:"
	@echo "  make install    - Install dependencies and build application"
	@echo "  make start      - Start all services with Docker Compose"
	@echo "  make stop       - Stop all services"
	@echo "  make restart    - Restart all services"
	@echo "  make status     - Check status of services"
	@echo "  make logs       - View logs in real-time"
	@echo "  make test       - Run all tests"
	@echo "  make build      - Build application (no tests)"
	@echo "  make clean      - Clean build artifacts and stop services"
	@echo "  make quickstart - Install and start in one command"
	@echo ""
	@echo "For Windows: use install.bat and start.bat"
	@echo ""

# Install and setup
install:
	@echo "🚀 Running installer..."
	@chmod +x install.sh
	@./install.sh

# Start services (with auto-install if needed)
start:
	@echo "▶️  Starting services..."
	@if [ ! -f .env ]; then \
		echo "📝 .env not found, running installation first..."; \
		$(MAKE) install; \
	fi
	@chmod +x start.sh
	@./start.sh

# Stop services
stop:
	@echo "🛑 Stopping services..."
	@if command -v docker-compose >/dev/null 2>&1; then \
		docker-compose down; \
	else \
		docker compose down; \
	fi
	@echo "✅ Services stopped"

# Restart services
restart:
	@echo "🔄 Restarting services..."
	@if command -v docker-compose >/dev/null 2>&1; then \
		docker-compose restart; \
	else \
		docker compose restart; \
	fi
	@echo "✅ Services restarted"

# Check status
status:
	@echo "📊 Service Status:"
	@if command -v docker-compose >/dev/null 2>&1; then \
		docker-compose ps; \
	else \
		docker compose ps; \
	fi

# View logs
logs:
	@if command -v docker-compose >/dev/null 2>&1; then \
		docker-compose logs -f; \
	else \
		docker compose logs -f; \
	fi

# Run tests
test:
	@echo "🧪 Running tests..."
	@if [ -f "./mvnw" ]; then \
		./mvnw test; \
	elif command -v mvn >/dev/null 2>&1; then \
		mvn test; \
	else \
		echo "❌ Maven not found. Please install Maven or use Docker."; \
		exit 1; \
	fi

# Build application
build:
	@echo "🔨 Building application..."
	@if [ -f "./mvnw" ]; then \
		./mvnw clean package -DskipTests; \
	elif command -v mvn >/dev/null 2>&1; then \
		mvn clean package -DskipTests; \
	else \
		echo "❌ Maven not found. Please install Maven or use Docker."; \
		exit 1; \
	fi
	@echo "✅ Build complete"

# Clean everything
clean:
	@echo "🧹 Cleaning up..."
	@if command -v docker-compose >/dev/null 2>&1; then \
		docker-compose down -v --remove-orphans 2>/dev/null || true; \
	else \
		docker compose down -v --remove-orphans 2>/dev/null || true; \
	fi
	@rm -rf target/ data/ logs/*.log 2>/dev/null || true
	@echo "✅ Cleanup complete"

# Quick start - auto-install if needed and run
quickstart: start
	@echo ""
	@echo "╔════════════════════════════════════════════════════════════╗"
	@echo "║         🎉 MESSENGER IS READY!                            ║"
	@echo "╚════════════════════════════════════════════════════════════╝"
	@echo ""
	@echo "🌐 Access URLs:"
	@echo "   • Application:     http://localhost:8080"
	@echo "   • Health Check:    http://localhost:8080/actuator/health"
	@echo "   • RabbitMQ:        http://localhost:15672"
	@echo "   • MinIO Console:   http://localhost:9001"
	@echo ""
	@echo "💡 Useful commands:"
	@echo "   make logs         - View logs"
	@echo "   make stop         - Stop services"
	@echo "   make restart      - Restart services"
	@echo ""

# Development mode - build and start locally
dev:
	@echo "🛠️  Starting development mode..."
	@if [ -f "./mvnw" ]; then \
		./mvnw spring-boot:run; \
	elif command -v mvn >/dev/null 2>&1; then \
		mvn spring-boot:run; \
	else \
		echo "❌ Maven not found. Please install Maven."; \
		exit 1; \
	fi
