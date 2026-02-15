package com.messenger.service;

import com.messenger.entity.Device;
import com.messenger.entity.User;
import com.messenger.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private UserChatRepository userChatRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private QRCodeService qrCodeService;

    @InjectMocks
    private DeviceService deviceService;

    private User testUser;
    private Device testDevice;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .build();

        testDevice = Device.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .deviceId("device-123")
                .deviceName("Test Device")
                .deviceType(Device.DeviceType.ANDROID)
                .isOnline(true)
                .isActive(true)
                .build();

        ReflectionTestUtils.setField(deviceService, "serverUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(deviceService, "websocketUrl", "/ws");
    }

    @Test
    @DisplayName("Should register new device")
    void shouldRegisterNewDevice() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(deviceRepository.findByDeviceId("device-123")).thenReturn(Optional.empty());
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        Device result = deviceService.registerOrUpdateDevice(
                "testuser",
                "device-123",
                "Test Device",
                Device.DeviceType.ANDROID,
                "13",
                "1.0.0",
                "192.168.1.1"
        );

        assertNotNull(result);
        assertEquals("device-123", result.getDeviceId());
        verify(deviceRepository).save(any(Device.class));
    }

    @Test
    @DisplayName("Should update existing device")
    void shouldUpdateExistingDevice() {
        Device existingDevice = Device.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .deviceId("device-123")
                .deviceName("Old Name")
                .deviceType(Device.DeviceType.IOS)
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(deviceRepository.findByDeviceId("device-123")).thenReturn(Optional.of(existingDevice));
        when(deviceRepository.save(any(Device.class))).thenReturn(existingDevice);

        Device result = deviceService.registerOrUpdateDevice(
                "testuser",
                "device-123",
                "New Name",
                Device.DeviceType.ANDROID,
                "14",
                "2.0.0",
                "10.0.0.1"
        );

        assertNotNull(result);
        assertEquals("New Name", existingDevice.getDeviceName());
        assertEquals(Device.DeviceType.ANDROID, existingDevice.getDeviceType());
        assertEquals("14", existingDevice.getOsVersion());
        assertEquals("2.0.0", existingDevice.getAppVersion());
        assertEquals("10.0.0.1", existingDevice.getIpAddress());
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            deviceService.registerOrUpdateDevice(
                    "nonexistent",
                    "device-123",
                    "Test Device",
                    Device.DeviceType.ANDROID,
                    null, null, null
            );
        });
    }

    @Test
    @DisplayName("Should update device heartbeat")
    void shouldUpdateDeviceHeartbeat() {
        when(deviceRepository.findByDeviceId("device-123")).thenReturn(Optional.of(testDevice));
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        deviceService.updateDeviceHeartbeat("device-123");

        assertNotNull(testDevice.getLastHeartbeat());
        assertTrue(testDevice.getIsOnline());
        verify(deviceRepository).save(testDevice);
    }

    @Test
    @DisplayName("Should set device online")
    void shouldSetDeviceOnline() {
        Device offlineDevice = Device.builder()
                .id(UUID.randomUUID())
                .deviceId("device-123")
                .isOnline(false)
                .build();

        when(deviceRepository.findByDeviceId("device-123")).thenReturn(Optional.of(offlineDevice));
        when(deviceRepository.save(any(Device.class))).thenReturn(offlineDevice);

        deviceService.setDeviceOnline("device-123", true);

        assertTrue(offlineDevice.getIsOnline());
        assertNotNull(offlineDevice.getLastSeen());
        assertNotNull(offlineDevice.getLastHeartbeat());
    }

    @Test
    @DisplayName("Should set device offline")
    void shouldSetDeviceOffline() {
        when(deviceRepository.findByDeviceId("device-123")).thenReturn(Optional.of(testDevice));
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        deviceService.setDeviceOnline("device-123", false);

        assertFalse(testDevice.getIsOnline());
    }

    @Test
    @DisplayName("Should update connection settings")
    void shouldUpdateConnectionSettings() {
        when(deviceRepository.findByDeviceId("device-123")).thenReturn(Optional.of(testDevice));
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        deviceService.updateConnectionSettings("device-123", "{\"key\": \"value\"}");

        assertEquals("{\"key\": \"value\"}", testDevice.getConnectionSettings());
    }

    @Test
    @DisplayName("Should get device by ID")
    void shouldGetDeviceById() {
        when(deviceRepository.findByDeviceId("device-123")).thenReturn(Optional.of(testDevice));

        Optional<Device> result = deviceService.getDeviceById("device-123");

        assertTrue(result.isPresent());
        assertEquals("device-123", result.get().getDeviceId());
    }

    @Test
    @DisplayName("Should get user devices")
    void shouldGetUserDevices() {
        UUID userId = testUser.getId();
        List<Device> devices = Arrays.asList(testDevice);

        when(deviceRepository.findActiveDevicesByUserId(userId)).thenReturn(devices);

        List<Device> result = deviceService.getUserDevices(userId);

        assertEquals(1, result.size());
        assertEquals("device-123", result.get(0).getDeviceId());
    }

    @Test
    @DisplayName("Should get user online devices")
    void shouldGetUserOnlineDevices() {
        UUID userId = testUser.getId();
        List<Device> devices = Arrays.asList(testDevice);

        when(deviceRepository.findByUserIdAndIsOnlineTrue(userId)).thenReturn(devices);

        List<Device> result = deviceService.getUserOnlineDevices(userId);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should deactivate device")
    void shouldDeactivateDevice() {
        when(deviceRepository.findByDeviceId("device-123")).thenReturn(Optional.of(testDevice));
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        deviceService.deactivateDevice("device-123");

        assertFalse(testDevice.getIsActive());
        assertFalse(testDevice.getIsOnline());
    }

    @Test
    @DisplayName("Should cleanup stale devices")
    void shouldCleanupStaleDevices() {
        Device staleDevice = Device.builder()
                .id(UUID.randomUUID())
                .deviceId("stale-device")
                .isOnline(true)
                .lastHeartbeat(LocalDateTime.now().minusMinutes(10))
                .build();

        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        when(deviceRepository.findStaleDevices(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(staleDevice));
        when(deviceRepository.save(any(Device.class))).thenReturn(staleDevice);

        deviceService.cleanupStaleDevices(5);

        assertFalse(staleDevice.getIsOnline());
        verify(deviceRepository).save(staleDevice);
    }

    @Test
    @DisplayName("Should check if device is online")
    void shouldCheckIfDeviceIsOnline() {
        when(deviceRepository.findByDeviceId("device-123")).thenReturn(Optional.of(testDevice));

        boolean result = deviceService.isDeviceOnline("device-123");

        assertTrue(result);
    }

    @Test
    @DisplayName("Should return false for non-existent device")
    void shouldReturnFalseForNonExistentDevice() {
        when(deviceRepository.findByDeviceId("nonexistent")).thenReturn(Optional.empty());

        boolean result = deviceService.isDeviceOnline("nonexistent");

        assertFalse(result);
    }

    @Test
    @DisplayName("Should generate device settings")
    void shouldGenerateDeviceSettings() {
        when(deviceRepository.findByDeviceId("device-123")).thenReturn(Optional.of(testDevice));

        Map<String, Object> settings = deviceService.generateDeviceSettings("device-123");

        assertNotNull(settings);
        assertEquals("device-123", settings.get("deviceId"));
        assertEquals("Test Device", settings.get("deviceName"));
        assertEquals("http://localhost:8080", settings.get("serverUrl"));
        assertEquals("/ws", settings.get("websocketUrl"));
        assertEquals(10, settings.get("heartbeatInterval"));
        assertTrue(settings.containsKey("rtcConfig"));
        assertTrue(settings.containsKey("features"));
        assertTrue(settings.containsKey("timestamp"));
    }

    @Test
    @DisplayName("Should generate settings QR code")
    void shouldGenerateSettingsQRCode() {
        when(deviceRepository.findByDeviceId("device-123")).thenReturn(Optional.of(testDevice));
        when(qrCodeService.generateSettingsQRCode(any())).thenReturn("base64-qr-code");

        String qrCode = deviceService.generateSettingsQRCode("device-123");

        assertEquals("base64-qr-code", qrCode);
        verify(qrCodeService).generateSettingsQRCode(any());
    }

    @Test
    @DisplayName("Should handle device not found gracefully")
    void shouldHandleDeviceNotFoundGracefully() {
        when(deviceRepository.findByDeviceId("nonexistent")).thenReturn(Optional.empty());

        // These methods should not throw exceptions for non-existent devices
        deviceService.updateDeviceHeartbeat("nonexistent");
        deviceService.setDeviceOnline("nonexistent", true);
        deviceService.updateConnectionSettings("nonexistent", "{}");
        deviceService.deactivateDevice("nonexistent");

        // No exceptions should be thrown
        verify(deviceRepository, never()).save(any());
    }
}
