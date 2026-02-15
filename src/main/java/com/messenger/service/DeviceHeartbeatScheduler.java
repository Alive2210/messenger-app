package com.messenger.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceHeartbeatScheduler {

    private final DeviceService deviceService;

    @Value("${device.heartbeat.timeout:5}")
    private int heartbeatTimeoutMinutes;

    /**
     * Run every minute to check for stale devices
     */
    @Scheduled(fixedRate = 10000) // Every 10 seconds
    public void cleanupStaleDevices() {
        log.debug("Running scheduled stale device cleanup");
        deviceService.cleanupStaleDevices(heartbeatTimeoutMinutes);
    }
}
