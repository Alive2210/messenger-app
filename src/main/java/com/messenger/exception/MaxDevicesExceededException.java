package com.messenger.exception;

public class MaxDevicesExceededException extends RuntimeException {
    public MaxDevicesExceededException(String message) {
        super(message);
    }

    public MaxDevicesExceededException(int maxDevices) {
        super(String.format("Maximum number of devices (%d) exceeded. Please remove an existing device first.", maxDevices));
    }
}
