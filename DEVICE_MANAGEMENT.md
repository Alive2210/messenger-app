# Device Management Implementation Summary

## Overview
Implemented a comprehensive device management system with heartbeat mechanism for the messenger application. This allows the backend to identify client devices, track their online status, and exchange connection settings.

## New Components Created

### 1. Entity Layer
- **Device.java** - Device entity to track all user devices
  - Stores device ID, name, type (ANDROID/IOS/WEB/DESKTOP), OS version, app version
  - Tracks online status, last seen, last heartbeat timestamps
  - Stores connection settings and IP address
  - Links to User entity (many-to-one relationship)

### 2. Repository Layer
- **DeviceRepository.java** - Repository interface with custom queries:
  - Find device by device ID
  - Find all devices for a user
  - Find online devices
  - Find stale devices (by heartbeat threshold)
  - Count online devices per user

### 3. Service Layer
- **DeviceService.java** - Business logic for device management:
  - Register/update devices
  - Update heartbeat timestamps
  - Set device online/offline status
  - Store and retrieve connection settings
  - Cleanup stale devices
  - **NEW:** Create favorites chat for each user
  - **NEW:** Send device settings to favorites chat
  - **NEW:** Generate QR codes for settings sharing

- **QRCodeService.java** - QR code generation:
  - Generate QR codes as Base64 images
  - Generate settings QR codes for device sharing

- **DeviceHeartbeatScheduler.java** - Scheduled task to:
  - Run every 10 seconds (previously 60 seconds) to check for stale devices
  - Mark devices offline if heartbeat not received within 2 minutes

### 4. Controller Layer

#### WebSocket Controllers
- **DeviceWebSocketController.java** - WebSocket endpoints for:
  - `/device.register` - Register device, receive connection settings and QR code
  - `/device.heartbeat` - Send/receive heartbeat with settings updates
  - `/device.disconnect` - Handle device disconnection
  - `/device.update-settings` - Update device settings
  - `/device.list` - Get list of user devices

#### REST Controllers
- **DeviceController.java** - REST API for:
  - `GET /api/devices` - Get all user devices
  - `GET /api/devices/{deviceId}` - Get specific device
  - `DELETE /api/devices/{deviceId}` - Deactivate/remove device
  - `GET /api/devices/online` - Get online devices
  - `POST /api/devices/{deviceId}/disconnect` - Force disconnect
  - **NEW:** `GET /api/devices/{deviceId}/qr-code` - Get QR code with settings
  - **NEW:** `POST /api/devices/{deviceId}/resend-settings` - Resend settings to favorites

### 5. Data Transfer Objects
- **DeviceDTOs.java** - Contains all DTOs:
  - DeviceRegistrationRequest/Response
  - HeartbeatRequest/Response
  - ConnectionSettings
  - DeviceInfo
  - DeviceListResponse
  - SettingsUpdateRequest/Response

### 6. Security Updates
- **JwtTokenProvider.java** - Enhanced to:
  - Generate device-specific tokens with `generateTokenForDevice()`
  - Extract device ID from tokens with `extractDeviceId()`

- **WebSocketConfig.java** - Updated to:
  - Extract device ID from JWT during WebSocket connection
  - Store device ID in session attributes
  - Log device connections/disconnections

### 7. Authentication Updates
- **AuthDTOs.java** - LoginRequestDTO now includes:
  - deviceId, deviceName, deviceType
  - osVersion, appVersion

- **AuthService.java** - Updated to:
  - Accept device information during login
  - Generate device-specific tokens
  - Register device on login

### 8. Database Migration
- **db.changelog-master.xml** - Changes:
  - ChangeSet 10: Added `devices` table with all necessary columns
  - **NEW:** Added FAVORITES to Chat.ChatType enum

### 9. Dependencies
- **pom.xml** - Added:
  - ZXing core and javase for QR code generation (version 3.5.2)

### 10. Configuration
- **application.yml** - Added device management settings:
  ```yaml
  device:
    heartbeat:
      timeout: 2  # Minutes (reduced for 10s interval)
      interval: 10  # Seconds
    settings:
      max-devices-per-user: 10
      allow-multiple-devices: true
  
  app:
    server:
      url: ${SERVER_URL:http://localhost:8080}
    websocket:
      url: ${WEBSOCKET_URL:/ws}
  ```

## Special Feature: Favorites Chat

When a device is registered:
1. System creates a special "Избранное" (Favorites) chat for the user if it doesn't exist
2. Device settings are automatically sent to this chat as a system message
3. Message includes:
   - Device information (ID, name, server URLs)
   - Connection settings (heartbeat interval, WebRTC config)
   - **QR code** for sharing settings with other devices

This allows users to:
- Access their connection settings from any device
- Share settings by scanning QR code
- Have a backup of configuration in the chat

## QR Code for Settings Sharing

### Generation
- QR code is generated as Base64 PNG image
- Contains JSON with all connection settings:
  ```json
  {
    "deviceId": "unique-device-id",
    "deviceName": "Device Name",
    "serverUrl": "http://localhost:8080",
    "websocketUrl": "/ws",
    "heartbeatInterval": 10,
    "timestamp": 1234567890,
    "rtcConfig": { ... },
    "features": { ... }
  }
  ```

### Access Methods
1. **WebSocket:** Received automatically on `/user/queue/device/qr-code` after registration
2. **REST API:** `GET /api/devices/{deviceId}/qr-code`
3. **Favorites Chat:** QR code embedded in settings message

## WebSocket Communication Flow

### Device Registration
1. Client connects to WebSocket with JWT token (containing device ID)
2. Client sends `/app/device.register` message with device info
3. Server registers/updates device in database
4. Server creates favorites chat if needed
5. Server sends settings to favorites chat
6. Server generates QR code
7. Server sends responses:
   - `/user/queue/device/registered` - Registration confirmation
   - `/user/queue/device/settings` - Connection settings
   - `/user/queue/device/qr-code` - QR code for sharing

### Heartbeat
1. Client sends `/app/device.heartbeat` every 10 seconds
2. Server updates `last_heartbeat` timestamp
3. Server responds with acknowledgment and updated settings
4. If heartbeat not received within 2 minutes, device marked offline

### Settings Exchange
1. Client can send updated settings via `/app/device.update-settings`
2. Server stores settings in database
3. Server confirms with updated settings
4. Server can push settings updates via heartbeat responses

## REST API Endpoints

### Device Management
```
GET    /api/devices                    - List all user devices
GET    /api/devices/{id}               - Get device details
DELETE /api/devices/{id}               - Deactivate device
GET    /api/devices/online             - List online devices
POST   /api/devices/{id}/disconnect    - Force disconnect
GET    /api/devices/{id}/qr-code       - Get QR code with settings
POST   /api/devices/{id}/resend-settings - Resend settings to favorites
```

### Authentication (Enhanced)
```
POST /api/auth/login
Body: {
  "username": "...",
  "password": "...",
  "deviceId": "unique-device-id",
  "deviceName": "My Phone",
  "deviceType": "ANDROID",
  "osVersion": "13",
  "appVersion": "1.2.3"
}
```

## Client Implementation Guide

### 1. Initial Login
```javascript
const loginData = {
  username: "user123",
  password: "password",
  deviceId: "device-uuid-generated-client-side",
  deviceName: "Chrome on Windows",
  deviceType: "WEB",
  osVersion: "Windows 11",
  appVersion: "1.0.0"
};

const response = await fetch('/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(loginData)
});

const { accessToken } = await response.json();
```

### 2. WebSocket Connection with Favorites
```javascript
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect(
  { 'Authorization': 'Bearer ' + accessToken },
  function(frame) {
    console.log('Connected');
    
    // Register device
    stompClient.send('/app/device.register', {}, JSON.stringify({
      deviceId: 'device-uuid',
      deviceName: 'Chrome on Windows',
      deviceType: 'WEB',
      osVersion: 'Windows 11',
      appVersion: '1.0.0'
    }));
    
    // Subscribe to settings
    stompClient.subscribe('/user/queue/device/settings', function(message) {
      const settings = JSON.parse(message.body);
      console.log('Received settings:', settings);
    });
    
    // Subscribe to QR code
    stompClient.subscribe('/user/queue/device/qr-code', function(message) {
      const qrData = JSON.parse(message.body);
      console.log('Received QR code:', qrData.qrCode);
      // Display QR code: <img src="data:image/png;base64,${qrData.qrCode}">
    });
  }
);
```

### 3. Heartbeat (Every 10 Seconds)
```javascript
// Send heartbeat every 10 seconds
setInterval(() => {
  stompClient.send('/app/device.heartbeat', {}, JSON.stringify({
    deviceId: 'device-uuid',
    timestamp: Date.now(),
    clientStatus: { batteryLevel: 85, networkType: 'wifi' }
  }));
}, 10000);

// Subscribe to heartbeat responses
stompClient.subscribe('/user/queue/device/heartbeat', function(message) {
  const response = JSON.parse(message.body);
  if (response.updatedSettings) {
    // Apply updated settings
  }
});
```

### 4. Access Favorites Chat
The favorites chat is automatically created and settings are sent there.
To retrieve settings from favorites:
```javascript
// Get messages from favorites chat
const response = await fetch('/api/chats/favorites/messages', {
  headers: { 'Authorization': 'Bearer ' + token }
});
const messages = await response.json();
// Find system messages with settings
```

### 5. Get QR Code via REST
```javascript
const response = await fetch(`/api/devices/${deviceId}/qr-code`, {
  headers: { 'Authorization': 'Bearer ' + token }
});
const { qrCode } = await response.json();
// Display: <img src="data:image/png;base64,${qrCode}">
```

### 6. Resend Settings to Favorites
```javascript
await fetch(`/api/devices/${deviceId}/resend-settings`, {
  method: 'POST',
  headers: { 'Authorization': 'Bearer ' + token }
});
```

## Configuration Options

### Heartbeat Settings
```yaml
device:
  heartbeat:
    timeout: 2  # Minutes - device marked offline after this time
    interval: 10  # Seconds - expected heartbeat interval
```

### Device Limits
```yaml
device:
  settings:
    max-devices-per-user: 10
    allow-multiple-devices: true
```

### Server URLs for QR Codes
```yaml
app:
  server:
    url: https://your-server.com  # Used in QR codes
  websocket:
    url: /ws
```

## Benefits

1. **Multi-device Support**: Users can have multiple devices simultaneously
2. **Device Tracking**: Know which devices are online and when they were last seen
3. **Settings Synchronization**: Push configuration updates to all devices
4. **Favorites Chat**: Automatic backup of settings in special chat
5. **QR Code Sharing**: Easy sharing of connection settings between devices
6. **Session Management**: Force disconnect devices remotely
7. **Security**: Device-specific tokens and ability to revoke device access
8. **Heartbeat Monitoring**: Detect disconnected or unresponsive devices (every 10s)

## Notes

- Device ID should be generated client-side and persisted (e.g., in localStorage)
- The system automatically cleans up stale devices every 10 seconds
- Favorites chat is created automatically when first device is registered
- Settings in favorites include QR code for easy sharing
- All device operations are tied to the authenticated user for security
- QR codes contain all necessary connection information for quick setup