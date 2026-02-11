# 🚀 Полное развертывание Secure Messenger

## Быстрый старт (3 команды)

```bash
# 1. Делаем скрипты исполняемыми
chmod +x full-deploy.sh stop.sh logs.sh

# 2. Запускаем полный деплой
./full-deploy.sh

# 3. Открываем в браузере
open https://localhost
```

## Что установится автоматически:

### ✅ Инфраструктура (Docker)
- **PostgreSQL 16** - база данных
- **RabbitMQ 3.12** - брокер сообщений
- **MinIO** - хранилище файлов (S3-совместимое)
- **Redis 7** - кэш и сессии
- **Nginx** - reverse proxy с SSL
- **Coturn** - TURN/STUN сервер для WebRTC через VPN

### ✅ Мониторинг
- **Prometheus** - метрики (http://localhost:9090)
- **Grafana** - дашборды (http://localhost:3000, admin/admin)

### ✅ Spring Boot приложение
- Собирается из исходников
- Запускается с профилем prod
- Подключается к инфраструктуре

## Доступы после установки:

```
🌐 Messenger API:     https://localhost
📱 WebSocket:          wss://localhost/ws
📊 RabbitMQ:           http://localhost:15672
📈 Prometheus:         http://localhost:9090
📉 Grafana:            http://localhost:3000
🗄️  MinIO Console:     http://localhost:9001
🔄 TURN Server:        turn:localhost:3478
```

### WebRTC - Видеоконференции через VPN (БЕЗ статического IP!)

**🎯 Работает без статического IP адреса!**

```bash
# Автоматическое определение сетевых настроек
curl http://localhost:8080/api/network/info

{
  "localIp": "192.168.1.100",
  "vpnIp": "10.0.0.5",
  "publicIp": "203.0.113.42",
  "turnUrl": "turn:10.0.0.5:3478"
}

# Получить конфигурацию WebRTC (автоматически определяет IP)
curl http://localhost:8080/api/webrtc/config

# Конфигурация включает:
# - TURN сервер с автоопределенным IP
# - VP9 кодек (4 Mbps) для Full HD без потерь
# - Opus аудио (128 kbps, 48 kHz) CD качество
```

**Настройка DuckDNS (динамический DNS):**
```bash
# Настроить автоматическое обновление DNS
./setup-duckdns.sh

# Или вручную
curl -X POST "http://localhost:8080/api/network/dns/duckdns" \
  -d "domain=your-domain" \
  -d "token=your-token"

# Добавить в crontab для автоматического обновления
*/5 * * * * /path/to/messenger-app/update-ip.sh
```

## Структура проекта после деплоя:

```
messenger-app/
├── docker-compose.prod.yml    # Production Docker конфиг
├── full-deploy.sh             # Скрипт деплоя
├── stop.sh                    # Остановка
├── logs.sh                    # Просмотр логов
├── view-logs.sh               # Утилита просмотра логов
├── .env                       # Переменные окружения (создается автоматически)
├── .env.example               # Пример .env файла
├── app.pid                    # PID процесса (создается автоматически)
├── ssl/                       # SSL сертификаты
│   ├── server.crt
│   └── server.key
├── nginx/
│   └── nginx.conf             # Nginx конфиг с WebSocket
├── rabbitmq/
│   └── rabbitmq.conf          # Конфиг RabbitMQ
├── coturn/                    # TURN/STUN сервер конфиг
│   └── turnserver.conf        # Конфиг для WebRTC через VPN
├── monitoring/
│   ├── prometheus/
│   │   └── prometheus.yml     # Конфиг Prometheus
│   └── grafana/
│       └── datasources/
│           └── datasources.yml # Источники данных Grafana
├── logs/                      # Логи приложения
└── data/                      # Persistent данные
    ├── postgres/
    ├── rabbitmq/
    ├── minio/
    ├── redis/
    ├── prometheus/
    └── grafana/
```

## Управление:

### Перезапуск:
```bash
./stop.sh && ./full-deploy.sh
```

### Логи:
```bash
./logs.sh          # Логи приложения
./logs.sh docker   # Логи Docker
./logs.sh nginx    # Логи Nginx
```

### Проверка статуса:
```bash
# Health check
curl https://localhost/health

# Actuator
curl https://localhost/actuator/health
curl https://localhost/actuator/info

# Docker
docker-compose -f docker-compose.prod.yml ps
```

## API Endpoints:

### Аутентификация
```bash
# Регистрация
curl -X POST https://localhost/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user1",
    "email": "user1@test.com",
    "password": "password123",
    "publicKey": "..."
  }'

# Логин
curl -X POST https://localhost/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user1",
    "password": "password123"
  }'
```

### WebSocket (wscat или аналог)
```javascript
// Подключение
const ws = new WebSocket('wss://localhost/ws');

// Отправка сообщения
ws.send(JSON.stringify({
  destination: '/app/chat.send',
  body: JSON.stringify({
    chatId: '...',
    encryptedContent: '...',
    messageType: 'TEXT'
  })
}));
```

## Продакшен настройки:

### Собственный домен:
1. Получите SSL сертификат (Let's Encrypt)
2. Замените файлы в `ssl/`
3. Обновите `nginx/nginx.conf` - замените `localhost` на ваш домен

### Настройка .env:
```bash
# Сгенерируйте свои секреты
DB_PASSWORD=$(openssl rand -base64 24)
JWT_SECRET=$(openssl rand -base64 32)
MINIO_SECRET_KEY=$(openssl rand -base64 24)
RABBITMQ_PASS=$(openssl rand -base64 24)
```

### Бэкапы:
```bash
# Бэкап PostgreSQL
docker-compose -f docker-compose.prod.yml exec postgres pg_dump -U messenger_user messenger_db > backup.sql

# Бэкап данных
tar -czf messenger-backup-$(date +%Y%m%d).tar.gz data/
```

## Устранение проблем:

### Порт занят:
```bash
# Найти процесс на порту 8080
sudo lsof -i :8080

# Или изменить порт в .env
SERVER_PORT=8081
```

### Очистка:
```bash
# Остановка и удаление данных
./stop.sh
docker-compose -f docker-compose.prod.yml down -v
sudo rm -rf data/
```

### Пересборка:
```bash
# Пересобрать приложение без кэша
./stop.sh
docker-compose -f docker-compose.prod.yml build --no-cache
./full-deploy.sh
```

## Производительность:

### Масштабирование:
```yaml
# docker-compose.prod.yml
deploy:
  replicas: 3
  resources:
    limits:
      cpus: '4'
      memory: 4G
```

### Оптимизация PostgreSQL:
```sql
-- После первого запуска
ALTER SYSTEM SET max_connections = '500';
ALTER SYSTEM SET shared_buffers = '1GB';
ALTER SYSTEM SET effective_cache_size = '3GB';
SELECT pg_reload_conf();
```

## 🎥 WebRTC через VPN - Видео без потерь качества

### Настройка для VPN сети:

```bash
# 1. Узнайте IP вашего сервера в VPN
ip addr show

# 2. Обновите .env
TURN_SERVER_HOST=10.0.0.5  # Ваш VPN IP
TURN_RELAY_IP=10.0.0.5
TURN_EXTERNAL_IP=10.0.0.5

# 3. Перезапустите
./stop.sh
./full-deploy.sh
```

### Качество видео (без потерь):
- **Кодек**: VP9
- **Битрейт**: 4 Mbps (Full HD 1080p)
- **Разрешение**: 1920x1080
- **FPS**: 30

### Качество аудио:
- **Кодек**: Opus
- **Битрейт**: 128 kbps
- **Sample Rate**: 48 kHz
- **Каналы**: Stereo

### Документация WebRTC:
Подробная настройка в файле: `WEBRTC_VPN.md`

## 🌐 Работа без статического IP (автоматически!)

Мессенджер работает **без статического IP адреса**!

### Автоматическое определение сети:
```bash
# Посмотреть определенные IP адреса
curl http://localhost:8080/api/network/info

# Получить рекомендацию по подключению
curl http://localhost:8080/api/network/connection-recommendation
```

**Поддерживаемые сценарии:**
- ✅ **Локальная сеть** - автоопределение IP (192.168.x.x)
- ✅ **VPN сеть** - обнаружение VPN интерфейсов (tun, tap, wg)
- ✅ **Интернет** - определение через внешние сервисы
- ✅ **Динамический DNS** - автоматическое обновление (DuckDNS, No-IP)

**Настройка DuckDNS:**
```bash
# Быстрая настройка автоматического DNS
./setup-duckdns.sh

# Добавить в crontab
*/5 * * * * /path/to/messenger-app/update-ip.sh
```

Подробная документация: `NO_STATIC_IP.md`

## Готово к работе! 🎉

После `./full-deploy.sh` ваш мессенджер будет доступен по HTTPS с:
- ✅ End-to-end шифрованием
- ✅ WebSocket real-time сообщениями
- ✅ Голосовыми сообщениями
- ✅ Видеоконференциями (WebRTC) **через VPN с Full HD качеством**
- ✅ **Работа без статического IP** (автоопределение + динамический DNS)
- ✅ Загрузкой файлов
- ✅ Полным мониторингом
- ✅ Продвинутым логированием
- ✅ TURN сервером для работы через NAT
