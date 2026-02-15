# Кэширование видео потока - Документация

## Обзор

Система кэширования видео потока предназначена для повышения стабильности видеозвонков за счет буферизации видео фреймов на сервере. Это позволяет восстановить поток после кратковременных обрывов соединения.

## Архитектура

```
Клиент A (Отправитель) 
    ↓ Отправляет фреймы через /app/video.frame
Сервер (VideoStreamBuffer)
    ↓ Буферизирует (60 фреймов ≈ 2 секунды, макс 10MB на поток)
Клиент B (Получатель)
    ↓ При потере соединения запрашивает /app/video.recover
Восстановленный поток
```

## Возможности

### 1. Буферизация видео фреймов
- Хранит последние 60 фреймов (~2 секунды при 30 FPS)
- Максимальный размер буфера: 10 MB на участника
- Автоматическая очистка старых фреймов
- Автоматическая очистка неиспользуемых буферов (5 минут)

### 2. Восстановление потока
- При обрыве связи клиент может запросить пропущенные фреймы
- Возможность получить фреймы с определенного sequence number
- Fallback: получение последних 30 фреймов если запрошенные не найдены

### 3. Интеграция с WebRTC
- Работает поверх существующей WebRTC сигнализации
- Не заменяет WebRTC, а дополняет для стабильности
- Совместим с существующей системой переподключения

### 4. Grace Period (Ожидание реконнекта)
- При отключении участника видео сессия переходит в режим ожидания
- Grace period: **10 секунд** - время для восстановления соединения
- В течение grace period буфер видео сохраняется
- После успешного реконнекта видео поток восстанавливается автоматически
- После истечения grace period буфер очищается

## WebSocket Endpoints

### Отправка видео фрейма
**Endpoint:** `/app/video.frame`
**Тип:** @MessageMapping

```json
{
  "conferenceId": "uuid-conference-id",
  "targetUserId": "username-target", // Опционально - для прямой отправки
  "frameData": "base64-encoded-frame-data",
  "timestamp": 1700000000000,
  "sequenceNumber": 12345,
  "codec": "VP8"
}
```

### Восстановление видео потока
**Endpoint:** `/app/video.recover`
**Тип:** @MessageMapping

```json
{
  "conferenceId": "uuid-conference-id",
  "participantId": "username",
  "fromSequence": 12340
}
```

**Ответ** (`/user/queue/video-recovery`):
```json
{
  "success": true,
  "conferenceId": "uuid-conference-id",
  "participantId": "username",
  "frames": ["base64-frame-1", "base64-frame-2", ...],
  "totalFrames": 30,
  "lastSequence": 12370
}
```

### Получение статуса буфера
**Endpoint:** `/app/video.buffer-status`
**Тип:** @MessageMapping

```json
{
  "conferenceId": "uuid-conference-id",
  "participantId": "username"
}
```

**Ответ** (`/user/queue/buffer-status`):
```json
{
  "conferenceId": "uuid-conference-id",
  "participantId": "username",
  "frameCount": 45,
  "totalSizeBytes": 5242880,
  "lastSequenceNumber": 12345
}
```

### Уведомление об обрыве соединения
**Endpoint:** `/app/connection.interrupted`
**Тип:** @MessageMapping

```json
{
  "sessionId": "websocket-session-id",
  "reason": "network-loss",
  "timestamp": 1700000000000,
  "willReconnect": true
}
```

### Реконнект видео сессии
**Endpoint:** `/app/video.reconnect`
**Тип:** @MessageMapping

Запрос на восстановление видео сессии после обрыва связи (в течение grace period).

```json
{
  "oldSessionId": "previous-session-id",
  "conferenceId": "uuid-conference-id",
  "deviceId": "device-id"
}
```

**Ответы:**
- Успех: `/topic/conference/{conferenceId}` событие `PARTICIPANT_RECONNECTED`
- Автоматическая отправка восстановленных фреймов в `/user/queue/video-recovery`
- Неудача (grace period истек): `/user/queue/video-reconnect-failed`

### Получение статуса видео сессии
**Endpoint:** `/app/video.buffer-status`
**Тип:** @MessageMapping

```json
{
  "conferenceId": "uuid-conference-id",
  "participantId": "username"
}
```

**Ответ** (`/user/queue/buffer-status`):
```json
{
  "conferenceId": "uuid-conference-id",
  "username": "username",
  "active": false,
  "inGracePeriod": true,
  "remainingGracePeriodMs": 25000,
  "reconnectCount": 0,
  "bufferFrameCount": 45,
  "bufferSizeBytes": 5242880
}
```

## Пример использования на клиенте (JavaScript)

```javascript
// Отправка видео фрейма
function sendVideoFrame(frameData, conferenceId) {
    const frame = {
        conferenceId: conferenceId,
        frameData: btoa(String.fromCharCode(...frameData)), // Base64 encode
        timestamp: Date.now(),
        sequenceNumber: currentSequence++,
        codec: 'VP8'
    };
    
    stompClient.send('/app/video.frame', {}, JSON.stringify(frame));
}

// Обработка обрыва соединения
function handleConnectionInterrupted(conferenceId) {
    // Уведомляем сервер
    stompClient.send('/app/connection.interrupted', {}, JSON.stringify({
        sessionId: stompClient.sessionId,
        reason: 'network-loss',
        timestamp: Date.now(),
        willReconnect: true
    }));
}

// Восстановление после переподключения
function recoverVideoStream(conferenceId, participantId, lastKnownSequence) {
    const request = {
        conferenceId: conferenceId,
        participantId: participantId,
        fromSequence: lastKnownSequence
    };
    
    stompClient.send('/app/video.recover', {}, JSON.stringify(request));
}

// Получение восстановленных фреймов
stompClient.subscribe('/user/queue/video-recovery', (message) => {
    const response = JSON.parse(message.body);
    if (response.success) {
        response.frames.forEach(base64Frame => {
            const frameData = Uint8Array.from(atob(base64Frame), c => c.charCodeAt(0));
            // Декодируем и отображаем фрейм
            decodeAndRenderFrame(frameData);
        });
    }
});

// Пример с Grace Period и автоматическим реконнектом
class VideoCallManager {
    constructor(stompClient) {
        this.stompClient = stompClient;
        this.currentConferenceId = null;
        this.sessionId = null;
        this.oldSessionId = null;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        
        this.setupSubscriptions();
    }
    
    setupSubscriptions() {
        // Слушаем события реконнекта
        this.stompClient.subscribe('/topic/conference/' + this.currentConferenceId, (message) => {
            const event = JSON.parse(message.body);
            if (event.type === 'PARTICIPANT_RECONNECTED') {
                console.log('✅ Участник восстановил соединение:', event.username);
            }
        });
        
        // Автоматическое восстановление видео
        this.stompClient.subscribe('/user/queue/video-recovery', (message) => {
            const response = JSON.parse(message.body);
            if (response.success) {
                console.log(`📹 Восстановлено ${response.totalFrames} фреймов`);
                this.handleRecoveredFrames(response.frames);
            }
        });
        
        // Ошибка реконнекта (grace period истек)
        this.stompClient.subscribe('/user/queue/video-reconnect-failed', (message) => {
            const error = JSON.parse(message.body);
            console.error('❌ Grace period истек:', error.message);
            this.handleReconnectFailed();
        });
    }
    
    joinConference(conferenceId) {
        this.currentConferenceId = conferenceId;
        this.sessionId = this.stompClient.sessionId;
        this.reconnectAttempts = 0;
        
        this.stompClient.send('/app/conference.join', {}, JSON.stringify({
            conferenceId: conferenceId,
            videoEnabled: true,
            audioEnabled: true,
            deviceId: 'my-device'
        }));
    }
    
    handleConnectionLost() {
        console.log('🔌 Соединение потеряно, сохраняем sessionId...');
        this.oldSessionId = this.sessionId;
        this.reconnectAttempts = 0;
        
        // Уведомляем сервер о разрыве
        this.stompClient.send('/app/connection.interrupted', {}, JSON.stringify({
            sessionId: this.oldSessionId,
            reason: 'network_loss',
            timestamp: Date.now(),
            willReconnect: true
        }));
        
        // Запускаем попытки реконнекта
        this.attemptReconnect();
    }
    
    attemptReconnect() {
        if (this.reconnectAttempts >= this.maxReconnectAttempts) {
            console.error('❌ Превышено количество попыток реконнекта');
            this.handleReconnectFailed();
            return;
        }
        
        this.reconnectAttempts++;
        console.log(`🔄 Попытка реконнекта ${this.reconnectAttempts}/${this.maxReconnectAttempts}...`);
        
        // Пытаемся восстановить WebSocket соединение
        setTimeout(() => {
            this.stompClient.reconnect(() => {
                // После успешного WebSocket реконнекта, восстанавливаем видео сессию
                this.sessionId = this.stompClient.sessionId;
                
                this.stompClient.send('/app/video.reconnect', {}, JSON.stringify({
                    oldSessionId: this.oldSessionId,
                    conferenceId: this.currentConferenceId,
                    deviceId: 'my-device'
                }));
            });
        }, 1000 * this.reconnectAttempts); // Экспоненциальная задержка
    }
    
    handleRecoveredFrames(base64Frames) {
        base64Frames.forEach(base64Frame => {
            const frameData = Uint8Array.from(atob(base64Frame), c => c.charCodeAt(0));
            // Воспроизводим фрейм
            this.videoDecoder.decode(frameData);
        });
    }
    
    handleReconnectFailed() {
        console.log('⚠️ Необходимо перезайти в конференцию');
        // Показываем UI с кнопкой "Переподключиться"
        this.showReconnectDialog();
    }
    
    leaveConference() {
        if (this.currentConferenceId) {
            this.stompClient.send('/app/conference.leave', {}, JSON.stringify({
                conferenceId: this.currentConferenceId
            }));
            this.currentConferenceId = null;
            this.sessionId = null;
            this.oldSessionId = null;
        }
    }
}
```

## Интеграция с существующей системой

### При выходе из конференции
Автоматически очищается буфер участника:
- WebSocket: `/app/conference.leave` → очищает буфер пользователя
- REST: `POST /api/conferences/{id}/end` → очищает все буферы конференции

### Очистка ресурсов
- **Автоматическая**: Буферы старше 5 минут автоматически удаляются
- **При выходе**: Очистка при выходе участника из конференции
- **При завершении**: Полная очистка при завершении конференции

## Конфигурация

### Настройки буфера (VideoStreamBuffer.java)
```java
private static final int BUFFER_SIZE = 60; // Количество фреймов (~2 сек)
private static final long MAX_BUFFER_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB
private static final long CLEANUP_INTERVAL_MS = 30000; // 30 секунд
private static final long BUFFER_TTL_MS = 300000; // 5 минут
```

### Redis кэш
Конфигурация WebRTC кэшируется в Redis (TTL: 30 минут):
```java
.withCacheConfiguration("webrtcConfig", 
        config.entryTtl(Duration.ofMinutes(30)))
```

### Настройки Grace Period (VideoReconnectService.java)
```java
private static final long GRACE_PERIOD_MS = 10000; // 10 секунд ожидания
private static final long CLEANUP_INTERVAL_MS = 5000; // Проверка каждые 5 сек
```

## Тестирование

### Unit тесты

Созданы тесты для всех компонентов системы:

1. **VideoStreamBufferTest** - тесты буфера видео фреймов:
   - Добавление фреймов
   - Ограничение буфера (60 фреймов, 10MB)
   - Получение фреймов с sequence
   - Получение последних N фреймов
   - Воспроизведение буфера
   - Конкурентный доступ
   - Очистка буферов

2. **VideoReconnectServiceTest** - тесты сервиса реконнекта:
   - Регистрация сессии
   - Отключение и grace period
   - Успешный реконнект внутри grace period
   - Отслеживание количества реконнектов
   - Очистка сессий
   - Работа с несколькими конференциями

3. **WebSocketControllerVideoTest** - тесты WebSocket эндпоинтов:
   - Обработка видео фреймов
   - Регистрация при входе в конференцию
   - Grace period при выходе
   - Восстановление видео потока
   - Реконнект сессии

### Запуск тестов

```bash
# Запуск всех тестов
mvn test

# Запуск конкретного тест-класса
mvn test -Dtest=VideoStreamBufferTest
mvn test -Dtest=VideoReconnectServiceTest
mvn test -Dtest=WebSocketControllerVideoTest

# Запуск с покрытием кода
mvn jacoco:report
```

### Интеграционное тестирование

```bash
# Запуск интеграционных тестов
mvn verify -P integration-tests
```

## Мониторинг

### Логи
- `log.trace`: Добавление фреймов в буфер
- `log.info`: Воспроизведение буфера, очистка буферов
- `log.debug`: Очистка старых буферов

### Метрики буфера
Используйте endpoint `/app/video.buffer-status` для получения:
- Количества фреймов в буфере
- Размера буфера в байтах
- Последнего sequence number

## Ограничения

1. **Размер буфера**: Максимум 60 фреймов (~2 секунды) на участника
2. **Память**: Максимум 10 MB на видео поток
3. **Время жизни**: Буферы удаляются после 5 минут неактивности
4. **Формат**: Поддерживаются кодеки VP8, VP9, H.264 (зависит от клиента)

## Безопасность

- Буферизируются только фреймы активных конференций
- Доступ к буферу только для участников конференции
- Автоматическая очистка при выходе пользователя

## Устранение неполадок

### Проблема: Буфер пуст при восстановлении
**Решение**: Проверьте, что фреймы отправляются через `/app/video.frame` и не превышают размер буфера.

### Проблема: Высокое использование памяти
**Решение**: Уменьшите `BUFFER_SIZE` или `MAX_BUFFER_SIZE_BYTES` в VideoStreamBuffer.java

### Проблема: Задержка видео
**Решение**: Кэширование добавляет минимальную задержку. Для real-time приложений можно отключить восстановление.

### Проблема: Grace period истекает слишком быстро
**Решение**: Увеличьте `GRACE_PERIOD_MS` в `VideoReconnectService.java` (по умолчанию 10 секунд). Рекомендуется 10-30 секунд в зависимости от стабильности сети.

### Проблема: Реконнект не работает
**Решение**: 
1. Проверьте, что отправляется `oldSessionId` при реконнекте
2. Убедитесь, что grace period не истек
3. Проверьте логи на наличие ошибок
4. Используйте `/app/video.buffer-status` для проверки статуса сессии

### Проблема: Тесты падают
**Решение**: 
1. Убедитесь, что все зависимости загружены: `mvn clean install`
2. Проверьте, что Lombok работает (может потребоваться включить annotation processing)
3. Для конкурентных тестов может потребоваться увеличить таймаут

## Дальнейшее развитие

- [ ] Адаптивное качество видео на основе качества соединения
- [ ] Предиктивная буферизация
- [ ] Интеграция с CDN для распределенного кэширования
- [ ] Поддержка SVC (Scalable Video Coding)
- [ ] Метрики и мониторинг через Micrometer/Prometheus
- [ ] Автоматическое тестирование с эмуляцией packet loss

## Дальнейшее развитие

- [ ] Адаптивное качество видео на основе качества соединения
- [ ] Предиктивная буферизация
- [ ] Интеграция с CDN для распределенного кэширования
- [ ] Поддержка SVC (Scalable Video Coding)
