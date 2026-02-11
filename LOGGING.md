# Логирование в Secure Messenger

## 📁 Структура логов

Приложение создает следующие файлы логов в директории `logs/`:

### Основные логи
- **`application.log`** - Основные логи приложения (текстовый формат)
- **`application.json`** - Основные логи в JSON формате (для ELK/Splunk)
- **`error.log`** - Только ошибки (ERROR level)

### Специализированные логи
- **`audit.log`** - Аудит событий (JSON)
  - Регистрация пользователей
  - Вход/выход в систему
  - Отправка сообщений
  - Загрузка файлов
  
- **`security.log`** - События безопасности (JSON)
  - Неудачные попытки аутентификации
  - Доступ запрещен
  - Невалидные токены
  - Подозрительная активность
  
- **`websocket.log`** - WebSocket события
  - Подключения/отключения
  - Отправка сообщений
  - WebRTC сигналы
  
- **`performance.log`** - Производительность (JSON)
  - Медленные запросы к БД (>1s)
  - Медленные API вызовы (>5s)
  - WebSocket операции

## 🎚️ Уровни логирования

| Уровень | Описание | Использование |
|---------|----------|---------------|
| **TRACE** | Детальная отладка | Разработка, отладка |
| **DEBUG** | Отладочная информация | Разработка, тестирование |
| **INFO** | Информационные события | Production, нормальная работа |
| **WARN** | Предупреждения | Нештатные ситуации |
| **ERROR** | Ошибки | Критические проблемы |
| **OFF** | Отключено | Не логировать |

## 🔧 Управление логами через API

### Получить список всех логгеров
```bash
curl -X GET http://localhost:8080/api/admin/logs \
  -H "Authorization: Bearer <token>"
```

### Получить информацию о конкретном логгере
```bash
curl -X GET http://localhost:8080/api/admin/logs/com.messenger.service \
  -H "Authorization: Bearer <token>"
```

### Изменить уровень логирования
```bash
curl -X POST http://localhost:8080/api/admin/logs/com.messenger.service \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"level": "DEBUG"}'
```

### Сбросить к уровню по умолчанию
```bash
curl -X DELETE http://localhost:8080/api/admin/logs/com.messenger.service \
  -H "Authorization: Bearer <token>"
```

### Получить доступные уровни
```bash
curl -X GET http://localhost:8080/api/admin/logs/levels \
  -H "Authorization: Bearer <token>"
```

## 🎯 Предопределенные логгеры

| Логгер | Описание | Рекомендуемый уровень |
|--------|----------|---------------------|
| `ROOT` | Корневой логгер | INFO |
| `com.messenger` | Основной логгер приложения | DEBUG |
| `com.messenger.controller` | REST API контроллеры | DEBUG |
| `com.messenger.service` | Бизнес-логика | DEBUG |
| `com.messenger.repository` | Репозитории БД | WARN |
| `com.messenger.websocket` | WebSocket обработчики | DEBUG |
| `com.messenger.security` | Компоненты безопасности | INFO |
| `AUDIT` | Аудит событий | INFO |
| `SECURITY` | События безопасности | INFO |
| `WEBSOCKET` | WebSocket события | DEBUG |
| `PERFORMANCE` | Метрики производительности | INFO |
| `org.springframework.web` | Spring Web | WARN |
| `org.springframework.security` | Spring Security | INFO |
| `org.hibernate.SQL` | SQL запросы | WARN |

## 🔧 Конфигурация через переменные окружения

```bash
# Корневой уровень логирования
ROOT_LOG_LEVEL=INFO

# Уровень приложения
APP_LOG_LEVEL=DEBUG

# Уровень WebSocket
WS_LOG_LEVEL=DEBUG

# Уровень SQL
SQL_LOG_LEVEL=WARN

# Уровень Spring
SPRING_LOG_LEVEL=WARN

# Уровень Spring Security
SECURITY_LOG_LEVEL=INFO

# Уровень консоли (цветной вывод)
CONSOLE_LOG_LEVEL=INFO

# Настройки файлов
LOG_PATH=logs
LOG_MAX_HISTORY=30
LOG_MAX_SIZE=100MB
LOG_TOTAL_SIZE_CAP=10GB
```

## 📊 Примеры логов

### Успешный вход
```json
{
  "timestamp": "2024-01-15T10:30:45.123Z",
  "level": "INFO",
  "logger": "AUDIT",
  "message": "AUDIT: User 'john_doe' performed action 'USER_LOGIN' in AuthService.login()",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "john_doe",
  "service": "messenger",
  "log_type": "audit"
}
```

### Ошибка безопасности
```json
{
  "timestamp": "2024-01-15T10:31:12.456Z",
  "level": "WARN",
  "logger": "SECURITY",
  "message": "[SECURITY] AUTH_FAILURE | User: hacker | Reason: INVALID_PASSWORD | IP: 192.168.1.100",
  "traceId": "550e8400-e29b-41d4-a716-446655440001",
  "service": "messenger",
  "log_type": "security"
}
```

### Медленный запрос
```json
{
  "timestamp": "2024-01-15T10:32:01.789Z",
  "level": "WARN",
  "logger": "PERFORMANCE",
  "message": "[PERF] SLOW_QUERY | Time: 2500ms | Query: SELECT ...",
  "traceId": "550e8400-e29b-41d4-a716-446655440002",
  "service": "messenger",
  "log_type": "performance"
}
```

### WebSocket событие
```
2024-01-15 10:33:15.234 [websocket-1] DEBUG [550e8400-e29b-41d4-a716-446655440003] [john_doe] WEBSOCKET - [WS] MESSAGE_RECEIVED | User: john_doe | Dest: /app/chat.send | Type: TEXT
```

## 🎨 Использование в коде

### Базовое логирование
```java
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MyService {
    public void doSomething() {
        log.trace("Trace message");
        log.debug("Debug message: {}", variable);
        log.info("Info message");
        log.warn("Warning: {}", warningMessage);
        log.error("Error occurred", exception);
    }
}
```

### Структурированное логирование (MessengerLogger)
```java
import com.messenger.logging.MessengerLogger;

// Аудит
MessengerLogger.audit("ACTION_NAME", userId, "details");
MessengerLogger.auditLogin(username, ipAddress, success);
MessengerLogger.auditLogout(userId, ipAddress);

// Безопасность
MessengerLogger.securityAuthFailure(username, reason, ipAddress);
MessengerLogger.securityAccessDenied(userId, resource, ipAddress);

// WebSocket
MessengerLogger.wsConnection(userId, sessionId);
MessengerLogger.wsMessageReceived(userId, destination, messageType);

// Производительность
MessengerLogger.perfDatabaseQuery(query, durationMs);
MessengerLogger.perfApiCall(endpoint, method, durationMs, statusCode);
```

### Аудит аннотация
```java
import com.messenger.logging.Auditable;

@Service
public class UserService {
    
    @Auditable(action = "USER_UPDATE")
    public User updateUser(UserDTO dto) {
        // Автоматически логируется в audit.log
        return userRepository.save(user);
    }
}
```

## 🔍 Просмотр логов в реальном времени

### Все логи
```bash
tail -f logs/application.log
```

### Только ошибки
```bash
tail -f logs/error.log
```

### Аудит
```bash
tail -f logs/audit.log | jq '.'  # с форматированием JSON
```

### Производительность
```bash
tail -f logs/performance.log | jq '.'
```

### Поиск по traceId
```bash
grep "550e8400-e29b-41d4-a716-446655440000" logs/application.json | jq '.'
```

## 📈 Мониторинг логов

Логи в JSON формате можно отправлять в:
- **ELK Stack** (Elasticsearch, Logstash, Kibana)
- **Splunk**
- **Datadog**
- **Grafana Loki**

Настройка Filebeat для ELK:
```yaml
filebeat.inputs:
- type: log
  enabled: true
  paths:
    - /path/to/messenger/logs/*.json
  json.keys_under_root: true
  json.add_error_key: true

output.elasticsearch:
  hosts: ["localhost:9200"]
```

## 🧹 Ротация и очистка

Логи автоматически ротируются:
- По размеру: 100MB
- По времени: ежедневно
- Хранение: 30 дней
- Архивация: gzip

Ручная очистка:
```bash
# Удалить старые логи
find logs/ -name "*.log.*" -mtime +30 -delete
find logs/ -name "*.json.*" -mtime +30 -delete
```
