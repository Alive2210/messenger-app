# 🌐 Messenger без статического IP

Мессенджер теперь работает **без статического IP адреса**! Система автоматически определяет сетевые настройки и подстраивается под любую сеть.

## ✨ Автоматические возможности

### 🎯 Автоопределение IP
- **Локальный IP** - автоматически находит IP в локальной сети
- **VPN IP** - обнаруживает VPN интерфейсы (tun, tap, wg, vpn)
- **Публичный IP** - определяет через внешние сервисы (api.ipify.org)
- **Hostname** - автоматически определяет имя хоста

### 🔄 Динамический DNS
Поддержка обновления DNS при смене IP:
- ✅ DuckDNS
- ✅ No-IP
- ✅ Dynu
- ✅ Cloudflare

## 🚀 Быстрый старт

### 1. Простой запуск (автоматически)

```bash
cd messenger-app
docker-compose -f docker-compose.prod.yml up -d
```

Система автоматически:
- Определит ваш IP адрес
- Настроит TURN сервер
- Сгенерирует WebRTC конфигурацию

### 2. Проверка работы

```bash
# Посмотреть определенные IP
curl http://localhost:8080/api/network/info

# Результат:
{
  "localIp": "192.168.1.100",
  "vpnIp": "10.0.0.5",
  "publicIp": "203.0.113.42",
  "hostname": "messenger-server",
  "turnUrl": "turn:10.0.0.5:3478",
  "turnsUrl": "turns:10.0.0.5:5349",
  "bestTurnIp": "10.0.0.5",
  "bestClientIp": "203.0.113.42"
}

# Получить рекомендацию по подключению
curl http://localhost:8080/api/network/connection-recommendation

# Результат:
{
  "recommended": "VPN",
  "ip": "10.0.0.5",
  "description": "Use VPN IP for best performance (P2P)",
  "turnRequired": false,
  "turnUrl": "turn:10.0.0.5:3478",
  "webrtcConfigUrl": "/api/webrtc/config"
}
```

### 3. Получить WebRTC конфигурацию

```bash
curl http://localhost:8080/api/webrtc/config

# Результат:
{
  "iceServers": {
    "iceServers": [
      {
        "urls": "turn:10.0.0.5:3478",
        "username": "messenger",
        "credential": "secure_password_123"
      },
      {
        "urls": "turns:10.0.0.5:5349",
        "username": "messenger",
        "credential": "secure_password_123"
      }
    ],
    "iceCandidatePoolSize": 10,
    "bundlePolicy": "balanced",
    "rtcpMuxPolicy": "require"
  },
  "video": {
    "codec": "VP9",
    "bitrate": 4000000,
    "constraints": {
      "width": {"ideal": 1920, "min": 1280},
      "height": {"ideal": 1080, "min": 720},
      "frameRate": {"ideal": 30, "min": 15}
    },
    "degradationPreference": "maintain-resolution"
  },
  "audio": {
    "codec": "opus",
    "bitrate": 128000,
    "constraints": {
      "sampleRate": {"ideal": 48000},
      "channelCount": {"ideal": 2}
    }
  },
  "network": {
    "localIp": "192.168.1.100",
    "vpnIp": "10.0.0.5",
    "publicIp": "203.0.113.42"
  }
}
```

## 🎥 Как это работает

### Сценарий 1: Все в одной VPN сети

```
┌─────────────┐         ┌─────────────┐
│  Client 1   │ ←─────→ │  Client 2   │
│  10.0.0.10  │   P2P   │  10.0.0.20  │
└─────────────┘         └─────────────┘
       │                       │
       └──────────┬────────────┘
                  │
           ┌─────────────┐
           │   Server    │
           │  10.0.0.5   │ ← Автоопределен
           │  (TURN)     │
           └─────────────┘
                  │
                  ↓
        WebSocket сигналинг
        Видео идет напрямую (P2P)
```

✅ **Результат**: Лучшая производительность, видео без потерь

### Сценарий 2: Разные сети (через интернет)

```
┌─────────────┐         ┌─────────────┐
│  Client 1   │         │  Client 2   │
│  NAT/Home   │ ←─────→ │  NAT/Office │
│  192.168.1.5│   TURN  │  10.0.0.20  │
└─────────────┘  Relay  └─────────────┘
       │                       │
       └──────────┬────────────┘
                  │
           ┌─────────────┐
           │   Server    │
           │  Public IP  │ ← Автоопределен
           │  (TURN)     │
           └─────────────┘
```

✅ **Результат**: Работает через любые NAT/Firewall

### Сценарий 3: Смешанная сеть

```
┌─────────────┐         ┌─────────────┐
│  Client 1   │ ← P2P → │  Client 2   │
│  VPN:       │         │  VPN:       │
│  10.0.0.10  │         │  10.0.0.20  │
└─────────────┘         └─────────────┘
       │
       │ P2P не работает
       ↓
┌─────────────┐
│  Client 3   │
│  Internet   │ ← TURN Relay → Server
│  203.0.113.5│
└─────────────┘
```

✅ **Результат**: Гибридный режим, оптимальный для каждого клиента

## 🔄 Динамический DNS

### Настройка DuckDNS (бесплатно)

1. Зарегистрируйтесь на https://www.duckdns.org
2. Создайте домен (например, `messenger-home`)
3. Получите токен

```bash
# Обновить DNS
curl -X POST "http://localhost:8080/api/network/dns/duckdns" \
  -d "domain=messenger-home" \
  -d "token=your-token-here"

# Теперь используйте:
# turn:messenger-home.duckdns.org:3478
```

### Автоматическое обновление DNS

```bash
#!/bin/bash
# /usr/local/bin/update-ddns.sh

LAST_IP_FILE="/tmp/last_ip.txt"
CURRENT_IP=$(curl -s http://localhost:8080/api/network/info | grep -o '"publicIp":"[^"]*"' | cut -d'"' -f4)

if [ -f "$LAST_IP_FILE" ]; then
    LAST_IP=$(cat "$LAST_IP_FILE")
    if [ "$CURRENT_IP" != "$LAST_IP" ]; then
        curl -X POST "http://localhost:8080/api/network/dns/duckdns" \
          -d "domain=messenger-home" \
          -d "token=your-token"
        echo "$CURRENT_IP" > "$LAST_IP_FILE"
        echo "IP changed from $LAST_IP to $CURRENT_IP, DNS updated"
    fi
else
    echo "$CURRENT_IP" > "$LAST_IP_FILE"
fi
```

Добавьте в crontab:
```bash
# Проверять каждые 5 минут
*/5 * * * * /usr/local/bin/update-ddns.sh
```

## 📱 Подключение клиентов

### JavaScript клиент (автоконфигурация)

```javascript
// 1. Получаем конфигурацию с сервера (автоматически определяет IP)
const response = await fetch('http://YOUR_SERVER:8080/api/webrtc/config');
const config = await response.json();

// 2. Создаем PeerConnection с автонастройками
const pc = new RTCPeerConnection({
    iceServers: config.iceServers.iceServers,
    iceCandidatePoolSize: config.iceServers.iceCandidatePoolSize,
    bundlePolicy: config.iceServers.bundlePolicy,
    rtcpMuxPolicy: config.iceServers.rtcpMuxPolicy
});

// 3. Настройки видео (Full HD без потерь)
const stream = await navigator.mediaDevices.getUserMedia({
    video: config.video.constraints,
    audio: config.audio.constraints
});

// 4. Настройка кодеков (VP9 для видео, Opus для аудио)
const videoTransceiver = pc.getTransceivers()
    .find(t => t.receiver.track.kind === 'video');
if (videoTransceiver) {
    const codecs = RTCRtpSender.getCapabilities('video').codecs;
    const vp9Codec = codecs.find(c => c.mimeType === 'video/VP9');
    if (vp9Codec) {
        videoTransceiver.setCodecPreferences([vp9Codec]);
    }
}

// 5. Настройка битрейта (4 Mbps)
const sender = pc.getSenders().find(s => s.track.kind === 'video');
const params = sender.getParameters();
params.encodings[0].maxBitrate = config.video.bitrate;  // 4000000
params.encodings[0].scaleResolutionDownBy = 1;
await sender.setParameters(params);
```

### React Native / Мобильные клиенты

```javascript
// Автоматическое подключение без хардкода IP
const getServerConfig = async () => {
    // Пробуем обнаружить сервер через mDNS/Bonjour
    // или используем ранее сохраненный IP
    
    const savedIp = await AsyncStorage.getItem('server_ip');
    
    if (savedIp) {
        try {
            const response = await fetch(`http://${savedIp}:8080/api/network/info`);
            if (response.ok) return savedIp;
        } catch (e) {
            console.log('Saved IP not reachable');
        }
    }
    
    // Ищем сервер в локальной сети
    const localIp = await getLocalIp();  // 192.168.1.x
    const subnet = localIp.replace(/\.\d+$/, '');
    
    for (let i = 1; i < 255; i++) {
        const testIp = `${subnet}.${i}`;
        try {
            const response = await fetch(`http://${testIp}:8080/api/network/info`, {
                timeout: 500
            });
            if (response.ok) {
                await AsyncStorage.setItem('server_ip', testIp);
                return testIp;
            }
        } catch (e) {}
    }
    
    return null;
};
```

## 🔧 Конфигурация

### Переменные окружения (.env)

```env
# Автоопределение сети
NETWORK_AUTO_DETECT_IP=true
NETWORK_EXTERNAL_IP_SERVICE=https://api.ipify.org
NETWORK_USE_LOCAL_IP_FALLBACK=true
NETWORK_PREFERRED_INTERFACE=eth0  # или wlan0, vpn0 и т.д.
NETWORK_USE_IPV6=false

# TURN сервер (автоматически заполняется)
TURN_SERVER_HOST=auto  # или конкретный IP
TURN_USER=messenger
TURN_PASS=auto_generated
TURN_REALM=messenger.local

# Динамический DNS (опционально)
DUCKDNS_DOMAIN=your-domain
DUCKDNS_TOKEN=your-token
```

### Ручное управление IP

```bash
# Обновить сетевую конфигурацию вручную
curl -X POST http://localhost:8080/api/network/refresh

# Проверить, изменился ли IP
curl -X POST "http://localhost:8080/api/network/check-ip-change?lastKnownIp=203.0.113.42"

# Обновить конкретный DNS
curl -X POST "http://localhost:8080/api/network/dns/noip" \
  -d "hostname=messenger-home.no-ip.biz" \
  -d "username=your-email@example.com" \
  -d "password=your-password"
```

## 🐛 Решение проблем

### Проблема: Не определяется IP

```bash
# Проверить логи
docker-compose -f docker-compose.prod.yml logs app | grep "Network auto-configuration"

# Ручное обновление
curl -X POST http://localhost:8080/api/network/refresh

# Проверить текущие настройки
curl http://localhost:8080/api/network/info
```

### Проблема: TURN сервер не доступен

```bash
# Проверить статус coturn
docker-compose -f docker-compose.prod.yml logs coturn

# Проверить порты
netstat -tlnp | grep turn

# Проверить из клиента
curl http://YOUR_IP:8080/api/network/connection-recommendation
```

### Проблема: IP сменился, DNS не обновился

```bash
# Проверить текущий IP
curl http://localhost:8080/api/network/info

# Обновить DNS вручную
curl -X POST "http://localhost:8080/api/network/dns/duckdns" \
  -d "domain=your-domain" \
  -d "token=your-token"

# Настроить автоматическое обновление через cron
```

## 📊 Мониторинг

```bash
# Смотреть сетевые события
tail -f logs/application.log | grep "Network"

# Проверка ICE кандидатов
tail -f logs/websocket.log | grep "ICE"

# Статистика соединений
curl http://localhost:8080/api/admin/logs
```

## 🎯 Рекомендации

### Для домашней сети:
1. Используйте DuckDNS (бесплатно)
2. Откройте порты 3478, 5349, 10000-20000/UDP
3. Настройте автоматическое обновление DNS

### Для VPN сети:
1. Используйте VPN IP (автоопределяется)
2. Не нужен публичный IP
3. Лучшая производительность (P2P)

### Для мобильных клиентов:
1. Реализуйте автообнаружение сервера
2. Сохраняйте последний рабочий IP
3. Используйте fallback на публичный DNS

## ✅ Преимущества

- ✅ **Нет привязки к статическому IP**
- ✅ **Работает в любой сети** (дом, офис, VPN)
- ✅ **Автоматическое обновление DNS** при смене IP
- ✅ **Оптимальный выбор маршрута** (P2P или TURN)
- ✅ **Full HD видео** без потерь качества
- ✅ **CD-качество аудио** (Opus 128kbps)
- ✅ **Простая настройка** - просто запустите!

Всё работает автоматически! 🚀
