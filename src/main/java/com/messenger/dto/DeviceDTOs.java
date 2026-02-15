package com.messenger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

public class DeviceDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceRegistrationRequest {
        private String deviceId;
        private String deviceName;
        private String deviceType;
        private String osVersion;
        private String appVersion;
        private String pushToken;
        private Map<String, Object> clientSettings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceRegistrationResponse {
        private boolean success;
        private String deviceId;
        private String message;
        private ConnectionSettings serverSettings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeartbeatRequest {
        private String deviceId;
        private long timestamp;
        private Map<String, Object> clientStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeartbeatResponse {
        private boolean acknowledged;
        private long serverTimestamp;
        private String deviceId;
        private ConnectionSettings updatedSettings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConnectionSettings {
        private String websocketUrl;
        private int heartbeatInterval;
        private int reconnectInterval;
        private int maxReconnectAttempts;
        private Map<String, Object> rtcConfig;
        private Map<String, Object> features;
        private String preferredCodec;
        private int maxFileSize;
        private int maxMessageLength;
        private boolean encryptionEnabled;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceInfo {
        private String deviceId;
        private String deviceName;
        private String deviceType;
        private String osVersion;
        private String appVersion;
        private boolean isOnline;
        private LocalDateTime lastSeen;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceListResponse {
        private String userId;
        private java.util.List<DeviceInfo> devices;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettingsUpdateRequest {
        private String deviceId;
        private Map<String, Object> settings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettingsUpdateResponse {
        private boolean success;
        private String message;
        private Map<String, Object> confirmedSettings;
    }
}
