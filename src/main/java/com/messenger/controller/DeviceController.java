package com.messenger.controller;

import com.messenger.dto.DeviceDTOs;
import com.messenger.entity.Device;
import com.messenger.entity.User;
import com.messenger.repository.UserRepository;
import com.messenger.service.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;
    private final UserRepository userRepository;

    /**
     * Get all devices for current user
     */
    @GetMapping
    public ResponseEntity<DeviceDTOs.DeviceListResponse> getMyDevices(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Device> devices = deviceService.getUserDevices(user.getId());
        
        List<DeviceDTOs.DeviceInfo> deviceInfos = devices.stream()
                .map(this::convertToDeviceInfo)
                .collect(Collectors.toList());
        
        DeviceDTOs.DeviceListResponse response = DeviceDTOs.DeviceListResponse.builder()
                .userId(user.getId().toString())
                .devices(deviceInfos)
                .build();
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get specific device by ID
     */
    @GetMapping("/{deviceId}")
    public ResponseEntity<DeviceDTOs.DeviceInfo> getDevice(
            @PathVariable String deviceId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Optional<Device> device = deviceService.getDeviceById(deviceId);
        
        if (device.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        // Verify device belongs to current user
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!device.get().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }
        
        return ResponseEntity.ok(convertToDeviceInfo(device.get()));
    }

    /**
     * Deactivate/remove a device
     */
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> deactivateDevice(
            @PathVariable String deviceId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Optional<Device> device = deviceService.getDeviceById(deviceId);
        
        if (device.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        // Verify device belongs to current user
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!device.get().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }
        
        deviceService.deactivateDevice(deviceId);
        log.info("User {} deactivated device {}", userDetails.getUsername(), deviceId);
        
        return ResponseEntity.ok().build();
    }

    /**
     * Get online status of all user devices
     */
    @GetMapping("/online")
    public ResponseEntity<List<DeviceDTOs.DeviceInfo>> getOnlineDevices(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Device> onlineDevices = deviceService.getUserOnlineDevices(user.getId());
        
        List<DeviceDTOs.DeviceInfo> deviceInfos = onlineDevices.stream()
                .map(this::convertToDeviceInfo)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(deviceInfos);
    }

    /**
     * Force disconnect a device
     */
    @PostMapping("/{deviceId}/disconnect")
    public ResponseEntity<Void> forceDisconnect(
            @PathVariable String deviceId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Optional<Device> device = deviceService.getDeviceById(deviceId);
        
        if (device.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        // Verify device belongs to current user
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!device.get().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }
        
        deviceService.setDeviceOnline(deviceId, false);
        log.info("User {} forced disconnect of device {}", userDetails.getUsername(), deviceId);
        
        return ResponseEntity.ok().build();
    }

    /**
     * Get QR code with device settings for sharing
     */
    @GetMapping("/{deviceId}/qr-code")
    public ResponseEntity<Map<String, String>> getDeviceQRCode(
            @PathVariable String deviceId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Optional<Device> device = deviceService.getDeviceById(deviceId);
        
        if (device.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        // Verify device belongs to current user
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!device.get().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }
        
        String qrCodeBase64 = deviceService.generateSettingsQRCode(deviceId);
        
        Map<String, String> response = new HashMap<>();
        response.put("qrCode", qrCodeBase64);
        response.put("deviceId", deviceId);
        response.put("format", "base64/png");
        
        log.info("Generated QR code for device {} requested by user {}", deviceId, userDetails.getUsername());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Resend device settings to favorites chat
     */
    @PostMapping("/{deviceId}/resend-settings")
    public ResponseEntity<Void> resendSettingsToFavorites(
            @PathVariable String deviceId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Optional<Device> device = deviceService.getDeviceById(deviceId);
        
        if (device.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        // Verify device belongs to current user
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!device.get().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }
        
        Map<String, Object> settings = deviceService.generateDeviceSettings(deviceId);
        deviceService.sendSettingsToFavoritesChat(userDetails.getUsername(), settings);
        
        log.info("Resent settings to favorites for device {} by user {}", deviceId, userDetails.getUsername());
        
        return ResponseEntity.ok().build();
    }

    private DeviceDTOs.DeviceInfo convertToDeviceInfo(Device device) {
        String deviceTypeStr = device.getDeviceType() != null ? 
                device.getDeviceType().name() : "UNKNOWN";
        
        return DeviceDTOs.DeviceInfo.builder()
                .deviceId(device.getDeviceId())
                .deviceName(device.getDeviceName())
                .deviceType(deviceTypeStr)
                .osVersion(device.getOsVersion())
                .appVersion(device.getAppVersion())
                .isOnline(device.getIsOnline() != null ? device.getIsOnline() : false)
                .lastSeen(device.getLastSeen())
                .build();
    }
}
