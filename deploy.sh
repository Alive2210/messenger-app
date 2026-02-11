#!/bin/bash

# Deploy Messenger Script
set -e

echo "🚀 Starting Secure Messenger Deployment..."

# Check prerequisites
echo "📋 Checking prerequisites..."
command -v docker >/dev/null 2>&1 || { echo "❌ Docker is required but not installed. Aborting." >&2; exit 1; }
command -v docker-compose >/dev/null 2>&1 || { echo "❌ Docker Compose is required but not installed. Aborting." >&2; exit 1; }
command -v mvn >/dev/null 2>&1 || { echo "❌ Maven is required but not installed. Aborting." >&2; exit 1; }

echo "✅ All prerequisites met!"

# Start infrastructure
echo ""
echo "🐳 Starting infrastructure services..."
cd docker
docker-compose up -d

# Wait for services to be ready
echo ""
echo "⏳ Waiting for services to be healthy..."
sleep 10

# Check PostgreSQL
echo "🔍 Checking PostgreSQL..."
until docker-compose exec -T postgres pg_isready -U messenger_user -d messenger_db > /dev/null 2>&1; do
    echo "  PostgreSQL is not ready yet, waiting..."
    sleep 2
done
echo "✅ PostgreSQL is ready!"

# Check RabbitMQ
echo "🔍 Checking RabbitMQ..."
until curl -s http://localhost:15672/api/overview -u guest:guest > /dev/null 2>&1; do
    echo "  RabbitMQ is not ready yet, waiting..."
    sleep 2
done
echo "✅ RabbitMQ is ready!"

# Check MinIO
echo "🔍 Checking MinIO..."
until curl -s http://localhost:9000/minio/health/live > /dev/null 2>&1; do
    echo "  MinIO is not ready yet, waiting..."
    sleep 2
done
echo "✅ MinIO is ready!"

cd ..

# Build application
echo ""
echo "🔨 Building application..."
mvn clean package -DskipTests

# Run application
echo ""
echo "🚀 Starting application..."
echo ""
echo "=================================="
echo "🎉 Messenger is starting up!"
echo ""
echo "📱 Application: http://localhost:8080"
echo "📊 RabbitMQ UI: http://localhost:15672 (guest/guest)"
echo "🗄️  MinIO Console: http://localhost:9001 (minioadmin/minioadmin)"
echo ""
echo "API Documentation:"
echo "  POST http://localhost:8080/api/auth/register"
echo "  POST http://localhost:8080/api/auth/login"
echo "  WebSocket: ws://localhost:8080/ws"
echo ""
echo "Press Ctrl+C to stop"
echo "=================================="
echo ""

java -jar target/*.jar
