package com.messenger.controller;

import com.messenger.config.NetworkAutoConfiguration;
import com.messenger.service.DynamicDNSService;
import com.messenger.service.WebRtcConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Контроллер для управления сетевой конфигурацией
 * Работает без статического IP
 */
@Slf4j
@RestController
@RequestMapping("/api/network")
@RequiredArgsConstructor
public class NetworkController {

    private final NetworkAutoConfiguration networkConfig;
    private final DynamicDNSService dynamicDNSService;
    private final WebRtcConfigurationService webRtcConfigService;

    /**
     * Получить текущую сетевую конфигурацию
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getNetworkInfo() {
        Map<String, Object> info = new HashMap<>();
        
        info.put("localIp", networkConfig.getLocalIp());
        info.put("vpnIp", networkConfig.getVpnIp());
        info.put("publicIp", networkConfig.getPublicIp());
        info.put("hostname", networkConfig.getHostname());
        info.put("turnUrl", networkConfig.generateTurnUrl());
        info.put("turnsUrl", networkConfig.generateTurnsUrl());
        info.put("bestTurnIp", networkConfig.getBestTurnIp());
        info.put("bestClientIp", networkConfig.getBestClientIp());
        
        return ResponseEntity.ok(info);
    }

    /**
     * Обновить сетевую конфигурацию (переопределить IP)
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshNetworkConfig() {
        log.info("Manual network configuration refresh requested");
        
        webRtcConfigService.refreshNetworkConfig();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Network configuration refreshed");
        response.put("localIp", networkConfig.getLocalIp());
        response.put("vpnIp", networkConfig.getVpnIp());
        response.put("publicIp", networkConfig.getPublicIp());
        response.put("turnUrl", networkConfig.generateTurnUrl());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Обновить DuckDNS
     */
    @PostMapping("/dns/duckdns")
    public ResponseEntity<Map<String, Object>> updateDuckDNS(
            @RequestParam String domain,
            @RequestParam String token) {
        
        log.info("Updating DuckDNS for domain: {}", domain);
        
        boolean success = dynamicDNSService.updateDuckDNS(domain, token);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("domain", domain);
        response.put("ip", networkConfig.getPublicIp());
        
        if (success) {
            response.put("message", "DuckDNS updated successfully");
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "Failed to update DuckDNS");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Обновить No-IP
     */
    @PostMapping("/dns/noip")
    public ResponseEntity<Map<String, Object>> updateNoIP(
            @RequestParam String hostname,
            @RequestParam String username,
            @RequestParam String password) {
        
        log.info("Updating No-IP for hostname: {}", hostname);
        
        boolean success = dynamicDNSService.updateNoIP(hostname, username, password);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("hostname", hostname);
        response.put("ip", networkConfig.getPublicIp());
        
        if (success) {
            response.put("message", "No-IP updated successfully");
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "Failed to update No-IP");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Обновить Dynu DNS
     */
    @PostMapping("/dns/dynu")
    public ResponseEntity<Map<String, Object>> updateDynu(
            @RequestParam String hostname,
            @RequestParam String password) {
        
        log.info("Updating Dynu DNS for hostname: {}", hostname);
        
        boolean success = dynamicDNSService.updateDynu(hostname, password);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("hostname", hostname);
        response.put("ip", networkConfig.getPublicIp());
        
        if (success) {
            response.put("message", "Dynu DNS updated successfully");
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "Failed to update Dynu DNS");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Обновить Cloudflare DNS
     */
    @PostMapping("/dns/cloudflare")
    public ResponseEntity<Map<String, Object>> updateCloudflare(
            @RequestParam String zoneId,
            @RequestParam String recordId,
            @RequestParam String token,
            @RequestParam String name) {
        
        log.info("Updating Cloudflare DNS for: {}", name);
        
        boolean success = dynamicDNSService.updateCloudflare(zoneId, recordId, token, name);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("name", name);
        response.put("ip", networkConfig.getPublicIp());
        
        if (success) {
            response.put("message", "Cloudflare DNS updated successfully");
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "Failed to update Cloudflare DNS");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Проверить, изменился ли IP с последнего обновления
     */
    @PostMapping("/check-ip-change")
    public ResponseEntity<Map<String, Object>> checkIpChange(@RequestParam String lastKnownIp) {
        boolean changed = dynamicDNSService.hasIpChanged(lastKnownIp);
        String currentIp = networkConfig.getPublicIp();
        
        Map<String, Object> response = new HashMap<>();
        response.put("changed", changed);
        response.put("lastKnownIp", lastKnownIp);
        response.put("currentIp", currentIp);
        
        if (changed) {
            response.put("message", "IP has changed from " + lastKnownIp + " to " + currentIp);
        } else {
            response.put("message", "IP unchanged: " + currentIp);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Получить рекомендуемый способ подключения для клиента
     */
    @GetMapping("/connection-recommendation")
    public ResponseEntity<Map<String, Object>> getConnectionRecommendation() {
        Map<String, Object> recommendation = new HashMap<>();
        
        String vpnIp = networkConfig.getVpnIp();
        String publicIp = networkConfig.getPublicIp();
        String localIp = networkConfig.getLocalIp();
        
        if (vpnIp != null) {
            recommendation.put("recommended", "VPN");
            recommendation.put("ip", vpnIp);
            recommendation.put("description", "Use VPN IP for best performance (P2P)");
            recommendation.put("turnRequired", false);
        } else if (publicIp != null) {
            recommendation.put("recommended", "Public");
            recommendation.put("ip", publicIp);
            recommendation.put("description", "Use Public IP with TURN relay");
            recommendation.put("turnRequired", true);
        } else {
            recommendation.put("recommended", "Local");
            recommendation.put("ip", localIp);
            recommendation.put("description", "Local network only");
            recommendation.put("turnRequired", false);
        }
        
        recommendation.put("turnUrl", networkConfig.generateTurnUrl());
        recommendation.put("webrtcConfigUrl", "/api/webrtc/config");
        
        return ResponseEntity.ok(recommendation);
    }
}
