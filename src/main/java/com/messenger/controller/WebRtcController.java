package com.messenger.controller;

import com.messenger.service.WebRtcConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Контроллер для предоставления WebRTC конфигурации клиентам
 * Включает настройки ICE серверов, видео и аудио без потерь качества
 */
@Slf4j
@RestController
@RequestMapping("/api/webrtc")
@RequiredArgsConstructor
public class WebRtcController {

    private final WebRtcConfigurationService webRtcConfigurationService;

    /**
     * Получить полную конфигурацию WebRTC
     * Доступно без авторизации для первоначальной настройки клиента
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getWebRtcConfiguration() {
        log.debug("Providing WebRTC configuration to client");

        Map<String, Object> config = webRtcConfigurationService.getFullConfiguration();

        return ResponseEntity.ok(config);
    }

    /**
     * Обновить сетевую конфигурацию (пересоздать ICE серверы)
     * Используется при изменении сетевого окружения
     */
    @PostMapping("/refresh-network")
    public ResponseEntity<Map<String, Object>> refreshNetworkConfig() {
        log.info("🔄 Refreshing network configuration via API");
        webRtcConfigurationService.refreshNetworkConfig();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "refreshed");
        response.put("config", webRtcConfigurationService.getFullConfiguration());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Получить только ICE серверы
     */
    @GetMapping("/ice-servers")
    public ResponseEntity<Map<String, Object>> getIceServers() {
        return ResponseEntity.ok(webRtcConfigurationService.getIceServersConfiguration());
    }

    /**
     * Получить конфигурацию видео
     */
    @GetMapping("/video-config")
    public ResponseEntity<Map<String, Object>> getVideoConfig() {
        return ResponseEntity.ok(webRtcConfigurationService.getVideoConfiguration());
    }

    /**
     * Получить конфигурацию аудио
     */
    @GetMapping("/audio-config")
    public ResponseEntity<Map<String, Object>> getAudioConfig() {
        return ResponseEntity.ok(webRtcConfigurationService.getAudioConfiguration());
    }
}
