package com.messenger.controller;

import com.messenger.dto.DeviceDTOs;
import com.messenger.entity.Device;
import com.messenger.service.DeviceService;
import com.messenger.service.WebRtcConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DeviceWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final DeviceService deviceService;
    private final WebRtcConfigurationService webRtcConfigurationService;

    /**
     * Handle device registration - sent when client connects
     */
    @MessageMapping("/device.register")
    public void registerDevice(@Payload DeviceDTOs.DeviceRegistrationRequest request, 
                               Principal principal,
                               SimpMessageHeaderAccessor headerAccessor) {
        try {
            String username = principal.getName();
            String deviceId = request.getDeviceId();
            
            log.info("Registering device {} for user {}", deviceId, username);

            // Parse device type
            Device.DeviceType deviceType = parseDeviceType(request.getDeviceType());
            
            // Get IP address from session
            String ipAddress = getClientIpAddress(headerAccessor);
            
            // Register or update device
            Device device = deviceService.registerOrUpdateDevice(
                    username,
                    deviceId,
                    request.getDeviceName(),
                    deviceType,
                    request.getOsVersion(),
                    request.getAppVersion(),
                    ipAddress
            );

            // Store device ID in session
            headerAccessor.getSessionAttributes().put("deviceId", deviceId);
            
            // Set device online
            deviceService.setDeviceOnline(deviceId, true);

            // Build connection settings
            Map<String, Object> deviceSettings = deviceService.generateDeviceSettings(deviceId);
            DeviceDTOs.ConnectionSettings connectionSettings = buildConnectionSettings();

            // Send settings to favorites chat
            deviceService.sendSettingsToFavoritesChat(username, deviceSettings);

            // Generate QR code for settings sharing
            String qrCodeBase64 = deviceService.generateSettingsQRCode(deviceId);

            // Send response
            DeviceDTOs.DeviceRegistrationResponse response = DeviceDTOs.DeviceRegistrationResponse.builder()
                    .success(true)
                    .deviceId(deviceId)
                    .message("Device registered successfully")
                    .serverSettings(connectionSettings)
                    .build();

            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/device/registered",
                    response
            );

            // Send connection settings
            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/device/settings",
                    connectionSettings
            );

            // Send QR code for sharing settings
            Map<String, String> qrResponse = new HashMap<>();
            qrResponse.put("type", "SETTINGS_QR");
            qrResponse.put("qrCode", qrCodeBase64);
            qrResponse.put("deviceId", deviceId);
            
            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/device/qr-code",
                    qrResponse
            );

            log.info("Device {} registered successfully for user {}. Settings sent to favorites chat.", deviceId, username);

        } catch (Exception e) {
            log.error("Error registering device", e);
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/device/errors",
                    new ErrorDTO("Failed to register device: " + e.getMessage())
            );
        }
    }

    /**
     * Handle heartbeat from client devices
     */
    @MessageMapping("/device.heartbeat")
    public void handleHeartbeat(@Payload DeviceDTOs.HeartbeatRequest request, Principal principal) {
        try {
            String deviceId = request.getDeviceId();
            
            log.debug("Heartbeat received from device {} for user {}", deviceId, principal.getName());

            // Update device heartbeat
            deviceService.updateDeviceHeartbeat(deviceId);

            // Build response with current settings
            DeviceDTOs.ConnectionSettings currentSettings = buildConnectionSettings();

            DeviceDTOs.HeartbeatResponse response = DeviceDTOs.HeartbeatResponse.builder()
                    .acknowledged(true)
                    .serverTimestamp(System.currentTimeMillis())
                    .deviceId(deviceId)
                    .updatedSettings(currentSettings)
                    .build();

            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/device/heartbeat",
                    response
            );

        } catch (Exception e) {
            log.error("Error processing heartbeat", e);
            // Don't send error for heartbeat to avoid spam
        }
    }

    /**
     * Handle device disconnection
     */
    @MessageMapping("/device.disconnect")
    public void disconnectDevice(@Payload Map<String, String> request, Principal principal) {
        try {
            String deviceId = request.get("deviceId");
            
            log.info("Device {} disconnecting for user {}", deviceId, principal.getName());

            deviceService.setDeviceOnline(deviceId, false);

            Map<String, Object> response = new HashMap<>();
            response.put("deviceId", deviceId);
            response.put("status", "disconnected");
            response.put("timestamp", LocalDateTime.now().toString());

            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/device/disconnected",
                    response
            );

        } catch (Exception e) {
            log.error("Error disconnecting device", e);
        }
    }

    /**
     * Handle settings update from client
     */
    @MessageMapping("/device.update-settings")
    public void updateSettings(@Payload DeviceDTOs.SettingsUpdateRequest request, Principal principal) {
        try {
            String deviceId = request.getDeviceId();
            
            log.info("Settings update received from device {} for user {}", deviceId, principal.getName());

            // Store client settings
            String settingsJson = convertMapToJson(request.getSettings());
            deviceService.updateConnectionSettings(deviceId, settingsJson);

            DeviceDTOs.SettingsUpdateResponse response = DeviceDTOs.SettingsUpdateResponse.builder()
                    .success(true)
                    .message("Settings updated successfully")
                    .confirmedSettings(request.getSettings())
                    .build();

            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/device/settings-updated",
                    response
            );

        } catch (Exception e) {
            log.error("Error updating device settings", e);
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/device/errors",
                    new ErrorDTO("Failed to update settings: " + e.getMessage())
            );
        }
    }

    /**
     * Request current device list
     */
    @MessageMapping("/device.list")
    public void getDeviceList(Principal principal) {
        try {
            log.info("Device list requested by user {}", principal.getName());

            // Get devices for user - we'll need to get user ID from username
            // For now, send empty list with success message
            DeviceDTOs.DeviceListResponse response = DeviceDTOs.DeviceListResponse.builder()
                    .userId(principal.getName())
                    .devices(java.util.Collections.emptyList())
                    .build();

            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/device/list",
                    response
            );

        } catch (Exception e) {
            log.error("Error getting device list", e);
        }
    }

    private Device.DeviceType parseDeviceType(String type) {
        if (type == null) return Device.DeviceType.UNKNOWN;
        try {
            return Device.DeviceType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Device.DeviceType.UNKNOWN;
        }
    }

    private String getClientIpAddress(SimpMessageHeaderAccessor headerAccessor) {
        // Try to get IP from WebSocket session
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes != null && sessionAttributes.containsKey("ipAddress")) {
            return (String) sessionAttributes.get("ipAddress");
        }
        return "unknown";
    }

    private DeviceDTOs.ConnectionSettings buildConnectionSettings() {
        Map<String, Object> rtcConfig = webRtcConfigurationService.getFullConfiguration();

        Map<String, Object> features = new HashMap<>();
        features.put("videoCalls", true);
        features.put("voiceMessages", true);
        features.put("fileSharing", true);
        features.put("reactions", true);
        features.put("typing", true);
        features.put("readReceipts", true);

        return DeviceDTOs.ConnectionSettings.builder()
                .websocketUrl("/ws")
                .heartbeatInterval(30) // seconds
                .reconnectInterval(5) // seconds
                .maxReconnectAttempts(10)
                .rtcConfig(rtcConfig)
                .features(features)
                .preferredCodec("VP8")
                .maxFileSize(100 * 1024 * 1024) // 100MB
                .maxMessageLength(4096)
                .encryptionEnabled(true)
                .build();
    }

    private String convertMapToJson(Map<String, Object> map) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(map);
        } catch (Exception e) {
            log.error("Error converting map to JSON", e);
            return "{}";
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ErrorDTO {
        private String error;
    }
}
