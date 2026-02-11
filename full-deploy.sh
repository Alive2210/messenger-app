#!/bin/bash
set -e

echo "╔════════════════════════════════════════════════════════════╗"
echo "║     🚀 SECURE MESSENGER - FULL PRODUCTION DEPLOY          ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Цвета
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Функция логирования
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Проверка root прав
if [[ $EUID -eq 0 ]]; then
   error "Не запускайте от root для безопасности"
   exit 1
fi

# Создание директорий
log "📁 Creating directories..."
mkdir -p logs
mkdir -p logs/coturn
mkdir -p data/postgres
mkdir -p data/rabbitmq
mkdir -p data/minio
mkdir -p data/redis
mkdir -p ssl
mkdir -p nginx
mkdir -p coturn
mkdir -p monitoring/prometheus
mkdir -p monitoring/grafana

# Генерация SSL сертификатов (для разработки)
if [ ! -f ssl/server.crt ]; then
    log "🔐 Generating SSL certificates..."
    openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
        -keyout ssl/server.key \
        -out ssl/server.crt \
        -subj "/C=RU/ST=Moscow/L=Moscow/O=SecureMessenger/CN=localhost" \
        2>/dev/null
    log "✅ SSL certificates generated"
fi

# Генерация JWT секрета
if [ ! -f .env ]; then
    log "🔑 Generating environment configuration..."
    JWT_SECRET=$(openssl rand -base64 32)
    cat > .env << EOF
# Database
DB_HOST=postgres
DB_PORT=5432
DB_NAME=messenger_db
DB_USERNAME=messenger_user
DB_PASSWORD=$(openssl rand -base64 24)

# JWT
JWT_SECRET=${JWT_SECRET}
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# MinIO
MINIO_ENDPOINT=http://minio:9000
MINIO_ACCESS_KEY=$(openssl rand -base64 12)
MINIO_SECRET_KEY=$(openssl rand -base64 24)
MINIO_BUCKET_NAME=messenger-files

# RabbitMQ
RABBITMQ_HOST=rabbitmq
RABBITMQ_PORT=5672
RABBITMQ_USER=messenger
RABBITMQ_PASS=$(openssl rand -base64 24)

# Redis
REDIS_HOST=redis
REDIS_PORT=6379

# Application
SERVER_PORT=8080
LOG_LEVEL=INFO
MAX_FILE_SIZE=100MB
MAX_REQUEST_SIZE=100MB

# TURN/STUN Server for WebRTC through VPN
TURN_SERVER_HOST=coturn
TURN_SERVER_PORT=3478
TURN_SERVER_TLS_PORT=5349
TURN_REALM=messenger.local
TURN_SERVER_NAME=turn.messenger.local
TURN_USER=messenger
TURN_PASS=$(openssl rand -base64 24)
TURN_RELAY_IP=0.0.0.0
TURN_EXTERNAL_IP=0.0.0.0

# WebRTC High Quality Settings (No Loss)
VIDEO_CODEC=VP9
VIDEO_BITRATE=4000000
VIDEO_FRAMERATE=30
VIDEO_WIDTH=1920
VIDEO_HEIGHT=1080
AUDIO_BITRATE=128000
EOF
    log "✅ Environment configuration created (.env)"
fi

# Загрузка .env
set -a
source .env
set +a

log "🐳 Starting Docker infrastructure..."
docker-compose -f docker-compose.prod.yml up -d --build

# Ожидание готовности сервисов
log "⏳ Waiting for services to be healthy..."
sleep 5

# Проверка PostgreSQL
log "🔍 Checking PostgreSQL..."
until docker-compose -f docker-compose.prod.yml exec -T postgres pg_isready -U ${DB_USERNAME} -d ${DB_NAME} > /dev/null 2>&1; do
    echo -n "."
    sleep 2
done
echo ""
log "✅ PostgreSQL is ready"

# Проверка RabbitMQ
log "🔍 Checking RabbitMQ..."
until curl -s http://localhost:15672/api/overview -u ${RABBITMQ_USER}:${RABBITMQ_PASS} > /dev/null 2>&1; do
    echo -n "."
    sleep 2
done
echo ""
log "✅ RabbitMQ is ready"

# Проверка MinIO
log "🔍 Checking MinIO..."
until curl -s http://localhost:9000/minio/health/live > /dev/null 2>&1; do
    echo -n "."
    sleep 2
done
echo ""
log "✅ MinIO is ready"

# Сборка приложения
log "🔨 Building Spring Boot application..."
./mvnw clean package -DskipTests -q

# Запуск приложения в фоне
log "🚀 Starting Spring Boot application..."
nohup java -Xms512m -Xmx2g \
    -Dspring.profiles.active=prod \
    -Dserver.port=${SERVER_PORT} \
    -Dlogging.file.name=logs/application.log \
    -jar target/*.jar > logs/startup.log 2>&1 &

APP_PID=$!
echo $APP_PID > app.pid

# Ожидание запуска приложения
log "⏳ Waiting for application to start..."
sleep 10

# Проверка здоровья приложения
for i in {1..30}; do
    if curl -s http://localhost:${SERVER_PORT}/actuator/health | grep -q "UP"; then
        log "✅ Application is UP and running!"
        break
    fi
    if [ $i -eq 30 ]; then
        error "Application failed to start. Check logs/startup.log"
        exit 1
    fi
    echo -n "."
    sleep 2
done

echo ""
echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║                    🎉 DEPLOY SUCCESS!                      ║"
echo "╠════════════════════════════════════════════════════════════╣"
echo "║                                                            ║"
echo "║  🌐 Application URL:                                       ║"
echo "║     HTTP:  http://localhost:${SERVER_PORT}                              ║"
echo "║     HTTPS: https://localhost:${SERVER_PORT}                             ║"
echo "║                                                            ║"
echo "║  📊 Monitoring:                                            ║"
echo "║     RabbitMQ:  http://localhost:15672                      ║"
echo "║     Prometheus: http://localhost:9090                      ║"
echo "║     Grafana:    http://localhost:3000                      ║"
echo "║                                                            ║"
echo "║  🎥 WebRTC / Video Conferencing:                           ║"
echo "║     TURN Server: turn:localhost:3478                       ║"
echo "║     Video Quality: Full HD 1080p (VP9, 4 Mbps)             ║"
echo "║     Audio Quality: CD Quality (Opus, 128 kbps)             ║"
echo "║                                                            ║"
echo "║  🔑 Access Credentials:                                    ║"
echo "║     RabbitMQ:  ${RABBITMQ_USER} / [hidden]                      ║"
echo "║     MinIO:     ${MINIO_ACCESS_KEY} / [hidden]                 ║"
echo "║     Grafana:   admin / admin                               ║"
echo "║     TURN:      ${TURN_USER} / [hidden]                     ║"
echo "║                                                            ║"
echo "║  📚 API Documentation:                                     ║"
echo "║     Register:   POST http://localhost:${SERVER_PORT}/api/auth/register        ║"
echo "║     Login:      POST http://localhost:${SERVER_PORT}/api/auth/login           ║"
echo "║     WebSocket:  ws://localhost:${SERVER_PORT}/ws                              ║"
echo "║     WebRTC Config: GET  http://localhost:${SERVER_PORT}/api/webrtc/config     ║"
echo "║                                                            ║"
echo "║  📁 Logs: logs/application.log                             ║"
echo "║  🛑 Stop: ./stop.sh                                        ║"
echo "║                                                            ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""
log "Deployment completed successfully! 🚀"
