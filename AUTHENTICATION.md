# 🔐 Аутентификация и подключение - Документация

## 📋 Обзор

Secure Messenger использует **JWT (JSON Web Token)** для аутентификации пользователей. Система поддерживает:
- ✅ HTTP REST API с JWT токенами
- ✅ WebSocket с автоматической аутентификацией
- ✅ Управление устройствами и сессиями
- ✅ Рефреш токены для продления сессии

---

## 🔑 Процесс аутентификации

### 1. Регистрация пользователя

**Endpoint:** `POST /api/auth/register`

```json
{
  "username": "user123",
  "password": "securePassword123",
  "email": "user@example.com"
}
```

**Ответ:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "username": "user123"
}
```

### 2. Вход в систему

**Endpoint:** `POST /api/auth/login`

```json
{
  "username": "user123",
  "password": "securePassword123"
}
```

**Ответ:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "username": "user123"
}
```

### 3. Обновление токена

**Endpoint:** `POST /api/auth/refresh`

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Ответ:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

---

## 🔌 Подключение WebSocket

### 1. Подключение с токеном

WebSocket endpoint: `ws://localhost:8080/ws`

**Вариант 1: Токен в URL (для браузеров)**
```javascript
const socket = new SockJS('http://localhost:8080/ws?token=YOUR_JWT_TOKEN');
const stompClient = Stomp.over(socket);
```

**Вариант 2: Токен в заголовках (для мобильных приложений)**
```javascript
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect(
  { 'Authorization': 'Bearer YOUR_JWT_TOKEN' },
  function(frame) {
    console.log('Connected: ' + frame);
  }
);
```

### 2. Автоматическая аутентификация

После подключения, все сообщения автоматически ассоциируются с пользователем через Principal:

```java
@MessageMapping("/chat.send")
public void sendMessage(@Payload SendMessageRequest request, Principal principal) {
    String username = principal.getName(); // Получаем username из JWT
    // Обработка сообщения...
}
```

### 3. Обработка отключения

```javascript
stompClient.disconnect(function() {
    console.log("Disconnected");
});
```

---

## 📱 Типы конференций

### 1. Текстовый чат (по умолчанию)
- Подключается автоматически при входе в чат
- Не требует специальной аутентификации

### 2. Аудио-звонок
**Endpoint:** `POST /api/conferences/chats/{chatId}?type=audio`

```json
{
  "id": "uuid",
  "roomId": "room-uuid",
  "conferenceType": "AUDIO",
  "status": "ACTIVE"
}
```

### 3. Видео-звонок
**Endpoint:** `POST /api/conferences/chats/{chatId}?type=video`

```json
{
  "id": "uuid",
  "roomId": "room-uuid",
  "conferenceType": "VIDEO",
  "status": "ACTIVE"
}
```

### 4. Демонстрация экрана
**Endpoint:** `POST /api/conferences/chats/{chatId}?type=screen_share`

---

## 🔄 Жизненный цикл сессии

```
┌─────────────┐
│   Login     │ ← POST /api/auth/login
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  JWT Token  │ ← Сохраняем accessToken и refreshToken
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ WS Connect  │ ← Подключаем WebSocket с токеном
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  Activities │ ← Отправляем сообщения, звоним, etc.
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   Refresh   │ ← Обновляем токен при истечении
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ Disconnect  │ ← Отключаемся при выходе
└─────────────┘
```

---

## ⚡ Быстрый старт (Пример кода)

### JavaScript (Browser)

```javascript
class MessengerClient {
    constructor(baseUrl) {
        this.baseUrl = baseUrl;
        this.token = null;
        this.stompClient = null;
    }

    // 1. Аутентификация
    async login(username, password) {
        const response = await fetch(`${this.baseUrl}/api/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        
        const data = await response.json();
        this.token = data.accessToken;
        localStorage.setItem('refreshToken', data.refreshToken);
        return data;
    }

    // 2. Подключение WebSocket
    connectWebSocket() {
        const socket = new SockJS(`${this.baseUrl}/ws?token=${this.token}`);
        this.stompClient = Stomp.over(socket);
        
        this.stompClient.connect({}, (frame) => {
            console.log('Connected: ' + frame);
            
            // Подписываемся на сообщения чата
            this.stompClient.subscribe('/topic/chat/123', (message) => {
                console.log('New message:', JSON.parse(message.body));
            });
            
            // Уведомляем сервер о подключении
            this.stompClient.send('/app/user.connect', {}, {});
        });
    }

    // 3. Отправка сообщения
    sendMessage(chatId, content) {
        this.stompClient.send('/app/chat.send', {}, JSON.stringify({
            chatId: chatId,
            content: content
        }));
    }

    // 4. Создание аудио-звонка
    async createAudioCall(chatId) {
        const response = await fetch(
            `${this.baseUrl}/api/conferences/chats/${chatId}?type=audio`, 
            {
                method: 'POST',
                headers: { 
                    'Authorization': `Bearer ${this.token}`,
                    'Content-Type': 'application/json'
                }
            }
        );
        return await response.json();
    }

    // 5. Присоединение к конференции
    joinConference(conferenceId) {
        this.stompClient.send('/app/conference.join', {}, JSON.stringify({
            conferenceId: conferenceId,
            videoEnabled: false,
            audioEnabled: true,
            deviceId: 'browser'
        }));
    }
}

// Использование
const client = new MessengerClient('http://localhost:8080');
await client.login('user123', 'password');
client.connectWebSocket();
```

### Java (Android/Spring)

```java
@RestController
public class MessengerController {
    
    @Autowired
    private WebSocketClient webSocketClient;
    
    // Аутентификация
    public AuthResponse login(String username, String password) {
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
            "/api/auth/login",
            new LoginRequest(username, password),
            AuthResponse.class
        );
        return response.getBody();
    }
    
    // WebSocket подключение
    public void connectWebSocket(String token) {
        WebSocketStompClient stompClient = new WebSocketStompClient(
            new SockJsClient(Collections.singletonList(new WebSocketTransport(
                new StandardWebSocketClient())))
        );
        
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        
        stompClient.connect("ws://localhost:8080/ws", headers, sessionHandler);
    }
}
```

---

## 🔒 Безопасность

### Токены
- **Access Token:** Действителен 24 часа (86400 сек)
- **Refresh Token:** Действителен 7 дней (604800 сек)

### Rate Limiting
- **Auth endpoints:** 10 запросов в минуту
- **API endpoints:** 100 запросов в минуту

### Best Practices
1. ✅ Храните refresh token в безопасном месте (Keychain/Keystore)
2. ✅ Обновляйте access token до истечения срока
3. ✅ Используйте HTTPS в production
4. ✅ Всегда проверяйте отключение WebSocket при выходе

---

## 🐛 Устранение неполадок

### "Invalid token"
- Проверьте, что токен не истек
- Обновите токен через `/api/auth/refresh`

### "WebSocket connection failed"
- Проверьте, что токен передается правильно
- Убедитесь, что сервер доступен

### "Unauthorized"
- Проверьте заголовок Authorization: `Bearer TOKEN`
- Убедитесь, что токен валиден

---

## 📚 Дополнительные ресурсы

- [JWT Specification](https://tools.ietf.org/html/rfc7519)
- [Spring Security WebSocket](https://docs.spring.io/spring-security/reference/servlet/integrations/websocket.html)
- [SockJS Client](https://github.com/sockjs/sockjs-client)

---

**Версия:** 1.0.0  
**Обновлено:** 2026-02-16
