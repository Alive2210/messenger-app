# Secure Messenger - Backend

Полноценный бэкенд мессенджера с end-to-end шифрованием, голосовыми сообщениями, отправкой файлов и видеоконференциями.

## Стек технологий

- **Java 17**
- **Spring Boot 3.2**
- **Spring Security** + JWT
- **Spring WebSocket** (STOMP)
- **PostgreSQL 16**
- **RabbitMQ** (брокер сообщений)
- **MinIO** (хранилище файлов)
- **Coturn** (TURN/STUN сервер для WebRTC через VPN)
- **WebRTC** (видеоконференции Full HD без потерь)

## Архитектура

```
┌─────────────────┐
│  REST API       │  Auth, CRUD операции
├─────────────────┤
│  WebSocket      │  Real-time сообщения
├─────────────────┤
│  WebRTC Signal  │  Видеоконференции (TURN)
├─────────────────┤
│  RabbitMQ       │  Очереди, уведомления
├─────────────────┤
│  PostgreSQL     │  Данные пользователей, чатов
├─────────────────┤
│  MinIO/S3       │  Файлы, голосовые, видео
├─────────────────┤
│  Coturn         │  TURN/STUN сервер
└─────────────────┘
```

## Основные функции

### 1. End-to-End Шифрование
- AES-256-GCM для шифрования сообщений
- RSA-2048 для обмена ключами
- Signal Protocol-ready архитектура

### 2. Real-time Messaging
- WebSocket + STOMP протокол
- Тайпинг индикаторы
- Статусы доставки (SENT, DELIVERED, READ)
- Ответы на сообщения

### 3. Голосовые сообщения
- Загрузка аудио файлов
- Визуализация waveform
- Отметка о прослушивании

### 4. Файлы
- Поддержка любых типов файлов
- Превью для изображений
- Шифрование на стороне клиента

### 5. Видеоконференции через VPN
- **TURN/STUN сервер** (Coturn) для работы через VPN/NAT
- **Full HD видео** (1920x1080, 4 Mbps, VP9) без потерь качества
- **CD-качество аудио** (Opus, 128 kbps, 48 kHz)
- WebRTC для P2P соединений внутри VPN
- Групповые звонки до 50 участников
- Демонстрация экрана
- Запись конференций
- Mute/unmute, вкл/выкл камеры

## Запуск проекта

### 1. Запуск инфраструктуры

```bash
cd docker
docker-compose up -d
```

Запустит:
- PostgreSQL на порту 5432
- RabbitMQ на порту 5672 (UI: http://localhost:15672)
- MinIO на порту 9000 (Console: http://localhost:9001)
- Redis на порту 6379
- TURN Server на порту 3478 (UDP/TCP) - для WebRTC через VPN

### 2. Сборка и запуск приложения

```bash
# Сборка
mvn clean package

# Запуск
mvn spring-boot:run
```

Или с Docker:
```bash
docker build -t secure-messenger .
docker run -p 8080:8080 secure-messenger
```

## API Endpoints

### Аутентификация
```
POST /api/auth/register          # Регистрация
POST /api/auth/login             # Вход
POST /api/auth/refresh           # Обновление токена
POST /api/auth/logout            # Выход
GET  /api/auth/public-key/{user} # Получить публичный ключ
```

### WebSocket
```
/ws                              # WebSocket endpoint

# Отправка сообщений:
/app/chat.send                   # Отправить сообщение
/app/chat.typing                 # Индикатор печати
/app/chat.read                   # Прочитано

# Получение:
/topic/chat/{chatId}             # Сообщения чата
/topic/chat/{chatId}/typing      # Тайпинг статус
/topic/chat/{chatId}/read        # Прочитанные сообщения
/user/queue/message-status       # Статус своих сообщений
/topic/user/{username}/status    # Онлайн статус

# WebRTC (видеозвонки через VPN):
GET /api/webrtc/config           # Получить конфигурацию WebRTC
                                 # (ICE серверы, настройки видео/аудио)
/app/webrtc.offer                # Отправить offer
/app/webrtc.answer               # Отправить answer
/app/webrtc.ice-candidate        # ICE кандидаты
/app/webrtc.config               # Запросить конфигурацию WebRTC
/user/queue/webrtc               # Получение сигналов
/user/queue/webrtc-config        # Получение конфигурации

# Видеоконференции:
/app/conference.join             # Присоединиться
/app/conference.leave            # Выйти
/app/conference.media-state      # Изменить медиа
/topic/conference/{id}           # События конференции
/topic/conference/{id}/media     # Изменения медиа
```

## Структура БД

### Основные таблицы:
- **users** - Пользователи с публичными ключами
- **chats** - Чаты (личные, групповые, каналы)
- **user_chats** - Связь пользователей с чатами
- **messages** - Зашифрованные сообщения
- **file_attachments** - Файлы
- **voice_messages** - Голосовые сообщения
- **message_status** - Статусы доставки
- **video_conferences** - Видеоконференции
- **conference_participants** - Участники конференций

## Безопасность

1. **Аутентификация**: JWT токены (24ч access, 7 дней refresh)
2. **Шифрование**: 
   - Клиент шифрует сообщения AES-256
   - Ключи шифруются RSA публичным ключом получателя
   - Сервер хранит только зашифрованные данные
3. **Файлы**: Шифруются клиентом перед загрузкой
4. **CORS**: Настроен для веб-клиентов

## WebRTC Flow (через VPN с Full HD, без статического IP!)

### Работа без статического IP

Мессенджер автоматически определяет сетевые настройки и работает без статического IP!

**Автоматическое определение:**
- ✅ Локальный IP в сети
- ✅ VPN IP (tun, tap, wg интерфейсы)
- ✅ Публичный IP через внешние сервисы
- ✅ Динамический DNS (DuckDNS, No-IP, Cloudflare)

**Получение конфигурации:**
```bash
# Сервер автоматически определяет все IP адреса
curl http://localhost:8080/api/network/info

{
  "localIp": "192.168.1.100",
  "vpnIp": "10.0.0.5",
  "publicIp": "203.0.113.42",
  "turnUrl": "turn:10.0.0.5:3478"
}
```

### Установление соединения:

1. **Получение конфигурации**:
   ```
   Client → GET /api/webrtc/config
   Response: ICE серверы с автоопределенными IP, VP9/Opus настройки
   ```

2. **Создание конференции**:
   ```
   Client → POST /api/conferences/{chatId}
   Server → Возвращает roomId и ICE конфигурацию с актуальными IP
   ```

3. **Обмен сигналами** (через WebSocket):
   ```
   /app/webrtc.offer      → SDP Offer с VP9 codec
   /app/webrtc.answer     → SDP Answer
   /app/webrtc.ice-candidate → ICE candidates (host, srflx, relay)
   ```

4. **Установление соединения**:
   - Сначала пробуем **P2P** (если в одной VPN сети)
   - Если P2P не работает → **TURN relay** (через Coturn)
   - Видео: VP9, 4 Mbps, 1080p
   - Аудио: Opus, 128 kbps, 48kHz

5. **Передача медиа**:
   - DTLS шифрование
   - TURN relay (если нужен)
   - Сервер не видит медиа-трафик

### Качество без потерь:

```javascript
// VP9 кодек для видео (лучше H264)
const codecs = RTCRtpSender.getCapabilities('video').codecs;
const vp9Codec = codecs.find(c => c.mimeType === 'video/VP9');
transceiver.setCodecPreferences([vp9Codec]);

// Битрейт 4 Mbps для Full HD
params.encodings[0].maxBitrate = 4000000;
params.encodings[0].scaleResolutionDownBy = 1;  // Без уменьшения

// Opus аудио с высоким качеством
const opusCodec = codecs.find(c => c.mimeType === 'audio/opus');
audioTransceiver.setCodecPreferences([opusCodec]);
```

### Работа через VPN:

```
┌─────────────┐                    ┌─────────────┐
│  Client 1   │                    │  Client 2   │
│  VPN:       │ ←── P2P (VPN) ───→ │  VPN:       │
│  10.0.0.10  │                    │  10.0.0.20  │
└─────────────┘                    └─────────────┘
       │                                  │
       └────────── TURN Relay ───────────┘
              (если P2P не работает)
              Coturn: 10.0.0.5:3478
```

## Разработка

### Добавление новых фич:
1. Сущности → `entity/`
2. Репозитории → `repository/`
3. Сервисы → `service/`
4. DTOs → `dto/`
5. Контроллеры → `controller/`
6. Миграции → `resources/db/changelog/`

### Тестирование:
```bash
mvn test
```

## Лицензия

MIT
