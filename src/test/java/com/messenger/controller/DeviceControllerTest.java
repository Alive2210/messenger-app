package com.messenger.controller;

import com.messenger.entity.Device;
import com.messenger.entity.User;
import com.messenger.repository.UserRepository;
import com.messenger.service.DeviceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeviceService deviceService;

    @MockBean
    private UserRepository userRepository;

    private User createTestUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .build();
    }

    private Device createTestDevice(User user, String deviceId) {
        return Device.builder()
                .id(UUID.randomUUID())
                .user(user)
                .deviceId(deviceId)
                .deviceName("Test Device")
                .deviceType(Device.DeviceType.ANDROID)
                .isOnline(true)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Should get all devices for authenticated user")
    @WithMockUser(username = "testuser")
    void shouldGetAllDevicesForAuthenticatedUser() throws Exception {
        User user = createTestUser();
        Device device = createTestDevice(user, "device-1");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(deviceService.getUserDevices(user.getId())).thenReturn(Arrays.asList(device));

        mockMvc.perform(get("/api/devices")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId().toString()))
                .andExpect(jsonPath("$.devices", hasSize(1)))
                .andExpect(jsonPath("$.devices[0].deviceId").value("device-1"));
    }

    @Test
    @DisplayName("Should get specific device by ID")
    @WithMockUser(username = "testuser")
    void shouldGetSpecificDeviceById() throws Exception {
        User user = createTestUser();
        Device device = createTestDevice(user, "device-1");

        when(deviceService.getDeviceById("device-1")).thenReturn(Optional.of(device));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/devices/device-1")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value("device-1"))
                .andExpect(jsonPath("$.deviceName").value("Test Device"));
    }

    @Test
    @DisplayName("Should return 404 for non-existent device")
    @WithMockUser(username = "testuser")
    void shouldReturn404ForNonExistentDevice() throws Exception {
        when(deviceService.getDeviceById("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/devices/nonexistent")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 403 for device belonging to another user")
    @WithMockUser(username = "testuser")
    void shouldReturn403ForDeviceBelongingToAnotherUser() throws Exception {
        User user = createTestUser();
        User otherUser = User.builder()
                .id(UUID.randomUUID())
                .username("otheruser")
                .build();
        Device device = createTestDevice(otherUser, "device-1");

        when(deviceService.getDeviceById("device-1")).thenReturn(Optional.of(device));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/devices/device-1")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should delete device")
    @WithMockUser(username = "testuser")
    void shouldDeleteDevice() throws Exception {
        User user = createTestUser();
        Device device = createTestDevice(user, "device-1");

        when(deviceService.getDeviceById("device-1")).thenReturn(Optional.of(device));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        mockMvc.perform(delete("/api/devices/device-1"))
                .andExpect(status().isOk());

        verify(deviceService).deactivateDevice("device-1");
    }

    @Test
    @DisplayName("Should get online devices")
    @WithMockUser(username = "testuser")
    void shouldGetOnlineDevices() throws Exception {
        User user = createTestUser();
        Device device = createTestDevice(user, "device-1");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(deviceService.getUserOnlineDevices(user.getId())).thenReturn(Arrays.asList(device));

        mockMvc.perform(get("/api/devices/online")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].deviceId").value("device-1"));
    }

    @Test
    @DisplayName("Should force disconnect device")
    @WithMockUser(username = "testuser")
    void shouldForceDisconnectDevice() throws Exception {
        User user = createTestUser();
        Device device = createTestDevice(user, "device-1");

        when(deviceService.getDeviceById("device-1")).thenReturn(Optional.of(device));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/devices/device-1/disconnect"))
                .andExpect(status().isOk());

        verify(deviceService).setDeviceOnline("device-1", false);
    }

    @Test
    @DisplayName("Should get QR code for device")
    @WithMockUser(username = "testuser")
    void shouldGetQRCodeForDevice() throws Exception {
        User user = createTestUser();
        Device device = createTestDevice(user, "device-1");

        when(deviceService.getDeviceById("device-1")).thenReturn(Optional.of(device));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(deviceService.generateSettingsQRCode("device-1")).thenReturn("base64-qr-code");

        mockMvc.perform(get("/api/devices/device-1/qr-code")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qrCode").value("base64-qr-code"))
                .andExpect(jsonPath("$.deviceId").value("device-1"))
                .andExpect(jsonPath("$.format").value("base64/png"));
    }

    @Test
    @DisplayName("Should resend settings to favorites")
    @WithMockUser(username = "testuser")
    void shouldResendSettingsToFavorites() throws Exception {
        User user = createTestUser();
        Device device = createTestDevice(user, "device-1");

        when(deviceService.getDeviceById("device-1")).thenReturn(Optional.of(device));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/devices/device-1/resend-settings"))
                .andExpect(status().isOk());

        verify(deviceService).sendSettingsToFavoritesChat(eq("testuser"), any());
    }

    @Test
    @DisplayName("Should return 403 for unauthenticated user")
    void shouldReturn403ForUnauthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/devices"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should handle empty device list")
    @WithMockUser(username = "testuser")
    void shouldHandleEmptyDeviceList() throws Exception {
        User user = createTestUser();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(deviceService.getUserDevices(user.getId())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/devices")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.devices", hasSize(0)));
    }
}
