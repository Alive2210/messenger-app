package com.messenger.service;

import com.messenger.dto.DeviceDTOs;
import com.messenger.entity.*;
import com.messenger.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final UserChatRepository userChatRepository;
    private final MessageRepository messageRepository;
    private final QRCodeService qrCodeService;

    @Value("${app.server.url:http://localhost:8080}")
    private String serverUrl;

    @Value("${app.websocket.url:/ws}")
    private String websocketUrl;

    @Transactional
    public Device registerOrUpdateDevice(String username, String deviceId, String deviceName, 
                                         Device.DeviceType deviceType, String osVersion, 
                                         String appVersion, String ipAddress) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        Optional<Device> existingDevice = deviceRepository.findByDeviceId(deviceId);
        
        Device device;
        if (existingDevice.isPresent()) {
            device = existingDevice.get();
            device.setDeviceName(deviceName);
            device.setDeviceType(deviceType);
            device.setOsVersion(osVersion);
            device.setAppVersion(appVersion);
            device.setIpAddress(ipAddress);
            device.setIsActive(true);
            device.setLastSeen(LocalDateTime.now());
            
            log.info("Updated device {} for user {}", deviceId, username);
            device = deviceRepository.save(device);
        } else {
            device = Device.builder()
                    .user(user)
                    .deviceId(deviceId)
                    .deviceName(deviceName)
                    .deviceType(deviceType != null ? deviceType : Device.DeviceType.UNKNOWN)
                    .osVersion(osVersion)
                    .appVersion(appVersion)
                    .ipAddress(ipAddress)
                    .isActive(true)
                    .isOnline(false)
                    .lastSeen(LocalDateTime.now())
                    .build();
            
            log.info("Registered new device {} for user {}", deviceId, username);
            device = deviceRepository.save(device);
            
            // Create favorites chat for new device
            createFavoritesChatIfNotExists(user);
        }
        
        return device;
    }

    @Transactional
    public void sendSettingsToFavoritesChat(String username, Map<String, Object> settings) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            
            Chat favoritesChat = getOrCreateFavoritesChat(user);
            
            // Generate QR code with settings
            String qrCodeBase64 = qrCodeService.generateSettingsQRCode(settings);
            
            // Create message content with settings and QR code
            String messageContent = buildSettingsMessageContent(settings, qrCodeBase64);
            
            // Save message to favorites chat
            Message message = Message.builder()
                    .chat(favoritesChat)
                    .sender(user)
                    .messageType(Message.MessageType.SYSTEM)
                    .encryptedContent(messageContent)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            messageRepository.save(message);
            
            log.info("Settings sent to favorites chat for user {}", username);
        } catch (Exception e) {
            log.error("Error sending settings to favorites chat", e);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> generateDeviceSettings(String deviceId) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found: " + deviceId));
        
        Map<String, Object> settings = new HashMap<>();
        settings.put("deviceId", deviceId);
        settings.put("deviceName", device.getDeviceName());
        settings.put("serverUrl", serverUrl);
        settings.put("websocketUrl", websocketUrl);
        settings.put("heartbeatInterval", 10); // seconds
        settings.put("timestamp", System.currentTimeMillis());
        
        // Add RTC configuration
        Map<String, Object> rtcSettings = new HashMap<>();
        rtcSettings.put("iceServers", Arrays.asList(
            Map.of("urls", "stun:stun.l.google.com:19302"),
            Map.of("urls", "stun:stun1.l.google.com:19302")
        ));
        settings.put("rtcConfig", rtcSettings);
        
        // Add features
        Map<String, Object> features = new HashMap<>();
        features.put("videoCalls", true);
        features.put("voiceMessages", true);
        features.put("fileSharing", true);
        features.put("reactions", true);
        settings.put("features", features);
        
        return settings;
    }

    @Transactional(readOnly = true)
    public String generateSettingsQRCode(String deviceId) {
        Map<String, Object> settings = generateDeviceSettings(deviceId);
        return qrCodeService.generateSettingsQRCode(settings);
    }

    private Chat getOrCreateFavoritesChat(User user) {
        // Try to find existing favorites chat
        List<Chat> userChats = chatRepository.findByUserIdAndChatType(user.getId(), Chat.ChatType.FAVORITES);
        
        if (!userChats.isEmpty()) {
            return userChats.get(0);
        }
        
        return createFavoritesChat(user);
    }

    private void createFavoritesChatIfNotExists(User user) {
        List<Chat> existing = chatRepository.findByUserIdAndChatType(user.getId(), Chat.ChatType.FAVORITES);
        if (existing.isEmpty()) {
            createFavoritesChat(user);
        }
    }

    private Chat createFavoritesChat(User user) {
        Chat favoritesChat = Chat.builder()
                .chatType(Chat.ChatType.FAVORITES)
                .chatName("Избранное")
                .createdBy(user)
                .isEncrypted(true)
                .build();
        
        favoritesChat = chatRepository.save(favoritesChat);
        
        // Add user to chat
        UserChat userChat = UserChat.builder()
                .user(user)
                .chat(favoritesChat)
                .isAdmin(true)
                .joinedAt(LocalDateTime.now())
                .build();
        
        userChatRepository.save(userChat);
        
        log.info("Created favorites chat for user {}", user.getUsername());
        return favoritesChat;
    }

    private String buildSettingsMessageContent(Map<String, Object> settings, String qrCodeBase64) {
        StringBuilder content = new StringBuilder();
        content.append("📱 Настройки устройства\n\n");
        content.append("Device ID: ").append(settings.get("deviceId")).append("\n");
        content.append("Server: ").append(settings.get("serverUrl")).append("\n");
        content.append("WebSocket: ").append(settings.get("websocketUrl")).append("\n");
        content.append("Heartbeat: ").append(settings.get("heartbeatInterval")).append(" сек\n\n");
        content.append("QR-код для обмена настройками:\n");
        content.append("data:image/png;base64,").append(qrCodeBase64);
        
        return content.toString();
    }

    @Transactional
    @CacheEvict(value = "deviceById", key = "#deviceId")
    public void updateDeviceHeartbeat(String deviceId) {
        deviceRepository.findByDeviceId(deviceId).ifPresent(device -> {
            device.setLastHeartbeat(LocalDateTime.now());
            device.setIsOnline(true);
            deviceRepository.save(device);
            log.debug("Heartbeat received from device {}", deviceId);
        });
    }

    @Transactional
    @CacheEvict(value = {"deviceById", "userDevices"}, allEntries = true)
    public void setDeviceOnline(String deviceId, boolean online) {
        deviceRepository.findByDeviceId(deviceId).ifPresent(device -> {
            device.setIsOnline(online);
            if (online) {
                device.setLastSeen(LocalDateTime.now());
                device.setLastHeartbeat(LocalDateTime.now());
            }
            deviceRepository.save(device);
            log.info("Device {} is now {}", deviceId, online ? "online" : "offline");
        });
    }

    @Transactional
    @CacheEvict(value = "deviceById", key = "#deviceId")
    public void updateConnectionSettings(String deviceId, String settings) {
        deviceRepository.findByDeviceId(deviceId).ifPresent(device -> {
            device.setConnectionSettings(settings);
            deviceRepository.save(device);
            log.info("Updated connection settings for device {}", deviceId);
        });
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "deviceById", key = "#deviceId")
    public Optional<Device> getDeviceById(String deviceId) {
        log.debug("Fetching device {} from database (not cached)", deviceId);
        return deviceRepository.findByDeviceId(deviceId);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "userDevices", key = "#userId.toString()")
    public List<Device> getUserDevices(UUID userId) {
        log.debug("Fetching devices for user {} from database (not cached)", userId);
        return deviceRepository.findActiveDevicesByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Device> getUserOnlineDevices(UUID userId) {
        return deviceRepository.findByUserIdAndIsOnlineTrue(userId);
    }

    @Transactional
    public void deactivateDevice(String deviceId) {
        deviceRepository.findByDeviceId(deviceId).ifPresent(device -> {
            device.setIsActive(false);
            device.setIsOnline(false);
            deviceRepository.save(device);
            log.info("Deactivated device {}", deviceId);
        });
    }

    @Transactional
    public void cleanupStaleDevices(int timeoutMinutes) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<Device> staleDevices = deviceRepository.findStaleDevices(threshold);
        
        for (Device device : staleDevices) {
            device.setIsOnline(false);
            deviceRepository.save(device);
            log.info("Marked device {} as offline due to missed heartbeats", device.getDeviceId());
        }
        
        log.info("Cleaned up {} stale devices", staleDevices.size());
    }

    @Transactional(readOnly = true)
    public boolean isDeviceOnline(String deviceId) {
        return deviceRepository.findByDeviceId(deviceId)
                .map(Device::getIsOnline)
                .orElse(false);
    }
}
