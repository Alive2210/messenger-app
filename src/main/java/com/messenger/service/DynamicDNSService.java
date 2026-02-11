package com.messenger.service;

import com.messenger.config.NetworkAutoConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Сервис для работы с динамическим DNS
 * Поддерживает DuckDNS, No-IP, Dynu и другие провайдеры
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicDNSService {

    private final NetworkAutoConfiguration networkConfig;

    /**
     * Обновление DuckDNS
     */
    public boolean updateDuckDNS(String domain, String token) {
        try {
            String ip = networkConfig.getPublicIp();
            if (ip == null) {
                log.warn("Cannot update DuckDNS: public IP not detected");
                return false;
            }

            String url = String.format("https://www.duckdns.org/update/%s/%s/%s", 
                    domain, token, ip);

            String response = httpGet(url);
            boolean success = "OK".equals(response.trim());
            
            if (success) {
                log.info("✅ DuckDNS updated: {} -> {}", domain, ip);
            } else {
                log.error("❌ DuckDNS update failed: {}", response);
            }
            
            return success;
        } catch (Exception e) {
            log.error("Error updating DuckDNS", e);
            return false;
        }
    }

    /**
     * Обновление No-IP
     */
    public boolean updateNoIP(String hostname, String username, String password) {
        try {
            String ip = networkConfig.getPublicIp();
            if (ip == null) {
                log.warn("Cannot update No-IP: public IP not detected");
                return false;
            }

            String url = String.format("https://%s:%s@dynupdate.no-ip.com/nic/update?hostname=%s&myip=%s",
                    URLEncoder.encode(username, StandardCharsets.UTF_8),
                    URLEncoder.encode(password, StandardCharsets.UTF_8),
                    hostname, ip);

            String response = httpGet(url);
            boolean success = response.startsWith("good") || response.startsWith("nochg");
            
            if (success) {
                log.info("✅ No-IP updated: {} -> {}", hostname, ip);
            } else {
                log.error("❌ No-IP update failed: {}", response);
            }
            
            return success;
        } catch (Exception e) {
            log.error("Error updating No-IP", e);
            return false;
        }
    }

    /**
     * Обновление Dynu DNS
     */
    public boolean updateDynu(String hostname, String password) {
        try {
            String ip = networkConfig.getPublicIp();
            if (ip == null) {
                log.warn("Cannot update Dynu: public IP not detected");
                return false;
            }

            String url = String.format("https://api.dynu.com/nic/update?hostname=%s&myip=%s&password=%s",
                    hostname, ip, URLEncoder.encode(password, StandardCharsets.UTF_8));

            String response = httpGet(url);
            boolean success = response.contains("good") || response.contains("nochg");
            
            if (success) {
                log.info("✅ Dynu updated: {} -> {}", hostname, ip);
            } else {
                log.error("❌ Dynu update failed: {}", response);
            }
            
            return success;
        } catch (Exception e) {
            log.error("Error updating Dynu", e);
            return false;
        }
    }

    /**
     * Обновление Cloudflare DNS
     */
    public boolean updateCloudflare(String zoneId, String recordId, String token, String name) {
        try {
            String ip = networkConfig.getPublicIp();
            if (ip == null) {
                log.warn("Cannot update Cloudflare: public IP not detected");
                return false;
            }

            String url = String.format("https://api.cloudflare.com/client/v4/zones/%s/dns_records/%s",
                    zoneId, recordId);

            String json = String.format("{\"type\":\"A\",\"name\":\"%s\",\"content\":\"%s\",\"ttl\":120}",
                    name, ip);

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));

            int responseCode = conn.getResponseCode();
            boolean success = responseCode == 200;
            
            if (success) {
                log.info("✅ Cloudflare updated: {} -> {}", name, ip);
            } else {
                log.error("❌ Cloudflare update failed: HTTP {}", responseCode);
            }
            
            return success;
        } catch (Exception e) {
            log.error("Error updating Cloudflare", e);
            return false;
        }
    }

    /**
     * Получить текущий публичный IP
     */
    public Map<String, String> getCurrentNetworkInfo() {
        Map<String, String> info = new HashMap<>();
        
        info.put("localIp", networkConfig.getLocalIp());
        info.put("vpnIp", networkConfig.getVpnIp());
        info.put("publicIp", networkConfig.getPublicIp());
        info.put("hostname", networkConfig.getHostname());
        info.put("turnUrl", networkConfig.generateTurnUrl());
        
        return info;
    }

    /**
     * Проверить, изменился ли IP
     */
    public boolean hasIpChanged(String lastKnownIp) {
        String currentIp = networkConfig.getPublicIp();
        if (currentIp == null) return false;
        return !currentIp.equals(lastKnownIp);
    }

    /**
     * HTTP GET запрос
     */
    private String httpGet(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }
}
