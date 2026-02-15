package com.messenger.repository;

import com.messenger.entity.Device;
import com.messenger.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class DeviceRepositoryTest {

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should save and find device by ID")
    void shouldSaveAndFindDeviceById() {
        // Create user first
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .build();
        user = userRepository.save(user);

        // Create device
        Device device = Device.builder()
                .user(user)
                .deviceId("device-123")
                .deviceName("Test Device")
                .deviceType(Device.DeviceType.ANDROID)
                .isOnline(true)
                .isActive(true)
                .build();

        Device savedDevice = deviceRepository.save(device);

        assertNotNull(savedDevice.getId());
        
        Optional<Device> foundDevice = deviceRepository.findById(savedDevice.getId());
        assertTrue(foundDevice.isPresent());
        assertEquals("device-123", foundDevice.get().getDeviceId());
    }

    @Test
    @DisplayName("Should find device by device ID")
    void shouldFindDeviceByDeviceId() {
        User user = createTestUser("user1");
        
        Device device = Device.builder()
                .user(user)
                .deviceId("unique-device-id")
                .deviceName("My Phone")
                .deviceType(Device.DeviceType.IOS)
                .build();
        
        deviceRepository.save(device);
        
        Optional<Device> found = deviceRepository.findByDeviceId("unique-device-id");
        
        assertTrue(found.isPresent());
        assertEquals("My Phone", found.get().getDeviceName());
    }

    @Test
    @DisplayName("Should find all devices by user ID")
    void shouldFindAllDevicesByUserId() {
        User user = createTestUser("user2");
        
        Device device1 = Device.builder()
                .user(user)
                .deviceId("device-1")
                .deviceName("Phone")
                .deviceType(Device.DeviceType.ANDROID)
                .build();
        
        Device device2 = Device.builder()
                .user(user)
                .deviceId("device-2")
                .deviceName("Tablet")
                .deviceType(Device.DeviceType.IOS)
                .build();
        
        deviceRepository.save(device1);
        deviceRepository.save(device2);
        
        List<Device> devices = deviceRepository.findByUserId(user.getId());
        
        assertEquals(2, devices.size());
    }

    @Test
    @DisplayName("Should find online devices by user ID")
    void shouldFindOnlineDevicesByUserId() {
        User user = createTestUser("user3");
        
        Device onlineDevice = Device.builder()
                .user(user)
                .deviceId("online-device")
                .deviceName("Online Phone")
                .isOnline(true)
                .build();
        
        Device offlineDevice = Device.builder()
                .user(user)
                .deviceId("offline-device")
                .deviceName("Offline Phone")
                .isOnline(false)
                .build();
        
        deviceRepository.save(onlineDevice);
        deviceRepository.save(offlineDevice);
        
        List<Device> onlineDevices = deviceRepository.findByUserIdAndIsOnlineTrue(user.getId());
        
        assertEquals(1, onlineDevices.size());
        assertEquals("online-device", onlineDevices.get(0).getDeviceId());
    }

    @Test
    @DisplayName("Should find all online devices")
    void shouldFindAllOnlineDevices() {
        User user1 = createTestUser("user4");
        User user2 = createTestUser("user5");
        
        Device device1 = Device.builder()
                .user(user1)
                .deviceId("dev-1")
                .isOnline(true)
                .build();
        
        Device device2 = Device.builder()
                .user(user2)
                .deviceId("dev-2")
                .isOnline(true)
                .build();
        
        Device device3 = Device.builder()
                .user(user1)
                .deviceId("dev-3")
                .isOnline(false)
                .build();
        
        deviceRepository.save(device1);
        deviceRepository.save(device2);
        deviceRepository.save(device3);
        
        List<Device> onlineDevices = deviceRepository.findByIsOnlineTrue();
        
        assertEquals(2, onlineDevices.size());
    }

    @Test
    @DisplayName("Should find stale devices")
    void shouldFindStaleDevices() {
        User user = createTestUser("user6");
        
        LocalDateTime oldHeartbeat = LocalDateTime.now().minusMinutes(10);
        LocalDateTime recentHeartbeat = LocalDateTime.now();
        
        Device staleDevice = Device.builder()
                .user(user)
                .deviceId("stale-device")
                .lastHeartbeat(oldHeartbeat)
                .isOnline(true)
                .build();
        
        Device freshDevice = Device.builder()
                .user(user)
                .deviceId("fresh-device")
                .lastHeartbeat(recentHeartbeat)
                .isOnline(true)
                .build();
        
        deviceRepository.save(staleDevice);
        deviceRepository.save(freshDevice);
        
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        List<Device> staleDevices = deviceRepository.findStaleDevices(threshold);
        
        assertEquals(1, staleDevices.size());
        assertEquals("stale-device", staleDevices.get(0).getDeviceId());
    }

    @Test
    @DisplayName("Should count online devices by user ID")
    void shouldCountOnlineDevicesByUserId() {
        User user = createTestUser("user7");
        
        Device device1 = Device.builder()
                .user(user)
                .deviceId("dev-1")
                .isOnline(true)
                .build();
        
        Device device2 = Device.builder()
                .user(user)
                .deviceId("dev-2")
                .isOnline(true)
                .build();
        
        Device device3 = Device.builder()
                .user(user)
                .deviceId("dev-3")
                .isOnline(false)
                .build();
        
        deviceRepository.save(device1);
        deviceRepository.save(device2);
        deviceRepository.save(device3);
        
        long count = deviceRepository.countOnlineDevicesByUserId(user.getId());
        
        assertEquals(2, count);
    }

    @Test
    @DisplayName("Should check if device exists by device ID")
    void shouldCheckIfDeviceExistsByDeviceId() {
        User user = createTestUser("user8");
        
        Device device = Device.builder()
                .user(user)
                .deviceId("existing-device")
                .build();
        
        deviceRepository.save(device);
        
        assertTrue(deviceRepository.existsByDeviceId("existing-device"));
        assertFalse(deviceRepository.existsByDeviceId("non-existing-device"));
    }

    @Test
    @DisplayName("Should delete device by device ID")
    void shouldDeleteDeviceByDeviceId() {
        User user = createTestUser("user9");
        
        Device device = Device.builder()
                .user(user)
                .deviceId("device-to-delete")
                .build();
        
        deviceRepository.save(device);
        assertTrue(deviceRepository.existsByDeviceId("device-to-delete"));
        
        deviceRepository.deleteByDeviceId("device-to-delete");
        
        assertFalse(deviceRepository.existsByDeviceId("device-to-delete"));
    }

    @Test
    @DisplayName("Should find active devices by user ID")
    void shouldFindActiveDevicesByUserId() {
        User user = createTestUser("user10");
        
        Device activeDevice = Device.builder()
                .user(user)
                .deviceId("active-device")
                .isActive(true)
                .build();
        
        Device inactiveDevice = Device.builder()
                .user(user)
                .deviceId("inactive-device")
                .isActive(false)
                .build();
        
        deviceRepository.save(activeDevice);
        deviceRepository.save(inactiveDevice);
        
        List<Device> activeDevices = deviceRepository.findActiveDevicesByUserId(user.getId());
        
        assertEquals(1, activeDevices.size());
        assertEquals("active-device", activeDevices.get(0).getDeviceId());
    }

    private User createTestUser(String username) {
        User user = User.builder()
                .username(username)
                .email(username + "@example.com")
                .password("password")
                .build();
        return userRepository.save(user);
    }
}
