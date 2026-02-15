package com.messenger.exception;

public class DeviceAlreadyExistsException extends RuntimeException {
    public DeviceAlreadyExistsException(String message) {
        super(message);
    }

    public static DeviceAlreadyExistsException forDeviceId(String deviceId) {
        return new DeviceAlreadyExistsException(String.format("Device with ID '%s' already exists", deviceId));
    }
}
