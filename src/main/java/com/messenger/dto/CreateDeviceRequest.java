package com.messenger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDeviceRequest {
    
    @NotBlank(message = "Device ID is required")
    @Size(min = 3, max = 255, message = "Device ID must be between 3 and 255 characters")
    private String deviceId;
    
    @NotBlank(message = "Device name is required")
    @Size(min = 1, max = 255, message = "Device name must be between 1 and 255 characters")
    private String deviceName;
    
    @NotNull(message = "Device type is required")
    private String deviceType; // ANDROID, IOS, WEB, DESKTOP
    
    @Size(max = 50, message = "OS version must not exceed 50 characters")
    private String osVersion;
    
    @Size(max = 50, message = "App version must not exceed 50 characters")
    private String appVersion;
}
