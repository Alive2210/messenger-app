package com.messenger.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DeviceTest {

    @Test
    @DisplayName("Should create device with builder")
    void shouldCreateDeviceWithBuilder() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        
        Device device = Device.builder()
                .id(UUID.randomUUID())
                .user(user)
                .deviceId("device-123")
                .deviceName("Test Device")
                .deviceType(Device.DeviceType.ANDROID)
                .osVersion("13")
                .appVersion("1.0.0")
                .pushToken("push-token-123")
                .isOnline(true)
                .lastSeen(LocalDateTime.now())
                .lastHeartbeat(LocalDateTime.now())
                .ipAddress("192.168.1.1")
                .connectionSettings("{\"key\": \"value\"}")
                .isActive(true)
                .build();
        
        assertNotNull(device);
        assertEquals("device-123", device.getDeviceId());
        assertEquals("Test Device", device.getDeviceName());
        assertEquals(Device.DeviceType.ANDROID, device.getDeviceType());
        assertEquals("13", device.getOsVersion());
        assertEquals("1.0.0", device.getAppVersion());
        assertTrue(device.getIsOnline());
        assertTrue(device.getIsActive());
    }

    @Test
    @DisplayName("Should create device with all device types")
    void shouldCreateDeviceWithAllDeviceTypes() {
        User user = new User();
        user.setId(UUID.randomUUID());
        
        for (Device.DeviceType type : Device.DeviceType.values()) {
            Device device = Device.builder()
                    .user(user)
                    .deviceId("device-" + type.name())
                    .deviceType(type)
                    .build();
            
            assertEquals(type, device.getDeviceType());
        }
    }

    @Test
    @DisplayName("Should have default values")
    void shouldHaveDefaultValues() {
        Device device = new Device();
        
        assertNotNull(device);
        // Default values from entity definition
        assertNull(device.getId());
        assertEquals(Boolean.FALSE, device.getIsOnline());
        assertEquals(Boolean.TRUE, device.getIsActive());
    }

    @Test
    @DisplayName("Should update device fields")
    void shouldUpdateDeviceFields() {
        User user = new User();
        user.setId(UUID.randomUUID());
        
        Device device = Device.builder()
                .user(user)
                .deviceId("device-123")
                .deviceName("Initial Name")
                .isOnline(false)
                .build();
        
        // Update fields
        device.setDeviceName("Updated Name");
        device.setIsOnline(true);
        device.setOsVersion("14");
        device.setAppVersion("2.0.0");
        device.setIpAddress("10.0.0.1");
        
        assertEquals("Updated Name", device.getDeviceName());
        assertTrue(device.getIsOnline());
        assertEquals("14", device.getOsVersion());
        assertEquals("2.0.0", device.getAppVersion());
        assertEquals("10.0.0.1", device.getIpAddress());
    }

    @Test
    @DisplayName("Should handle null values")
    void shouldHandleNullValues() {
        User user = new User();
        user.setId(UUID.randomUUID());
        
        Device device = Device.builder()
                .user(user)
                .deviceId("device-123")
                .deviceType(null)
                .osVersion(null)
                .appVersion(null)
                .pushToken(null)
                .build();
        
        assertNull(device.getDeviceType());
        assertNull(device.getOsVersion());
        assertNull(device.getAppVersion());
        assertNull(device.getPushToken());
    }

    @Test
    @DisplayName("Should handle timestamps")
    void shouldHandleTimestamps() {
        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setId(UUID.randomUUID());
        
        Device device = Device.builder()
                .user(user)
                .deviceId("device-123")
                .lastSeen(now)
                .lastHeartbeat(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
        
        assertEquals(now, device.getLastSeen());
        assertEquals(now, device.getLastHeartbeat());
        assertEquals(now, device.getCreatedAt());
        assertEquals(now, device.getUpdatedAt());
    }

    @Test
    @DisplayName("Should create device with empty no-args constructor")
    void shouldCreateDeviceWithEmptyConstructor() {
        Device device = new Device();
        
        assertNotNull(device);
        device.setDeviceId("test-device");
        device.setDeviceName("Test");
        device.setDeviceType(Device.DeviceType.WEB);
        
        assertEquals("test-device", device.getDeviceId());
        assertEquals("Test", device.getDeviceName());
        assertEquals(Device.DeviceType.WEB, device.getDeviceType());
    }
}
