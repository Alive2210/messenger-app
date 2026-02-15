package com.messenger.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.regex.Pattern;

/**
 * Автоматическое определение сетевых настроек для работы без статического IP
 */
@Slf4j
@Data
@Configuration
public class NetworkAutoConfiguration {

    @Value("${network.auto-detect-ip:true}")
    private boolean autoDetectIp;

    @Value("${network.public-ip:}")
    private String configuredPublicIp;

    @Value("${network.external-ip-service:https://api.ipify.org}")
    private String externalIpService;

    @Value("${network.use-local-ip-fallback:true}")
    private boolean useLocalIpFallback;

    @Value("${network.preferred-interface:}")
    private String preferredInterface;

    @Value("${network.vlan-ranges:10.0.0.0/8,172.16.0.0/12,192.168.0.0/16,100.64.0.0/10}")
    private String vlanRanges;

    @Value("${network.use-ipv6:false}")
    private boolean useIpv6;

    @Value("${server.port:8080}")
    private int serverPort;

    private String localIp;
    private String publicIp;
    private String vpnIp;
    private String hostname;

    // Simple, safe logger fallback (in case Lombok is not active in this environment)
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NetworkAutoConfiguration.class);

    @PostConstruct
    public void init() {
        detectNetworkConfiguration();
    }

    /**
     * Автоматическое определение всех сетевых параметров
     */
    public void detectNetworkConfiguration() {
        log.info("🌐 Starting network auto-configuration...");

        // Определяем локальный IP
        localIp = detectLocalIp();
        log.info("📍 Local IP detected: {}", localIp);

        // Пытаемся определить VPN IP (частные сети)
        vpnIp = detectVpnIp();
        if (vpnIp != null) {
            log.info("🔒 VPN IP detected: {}", vpnIp);
        }

        // Определяем публичный IP (если не указан вручную)
        if (autoDetectIp && (configuredPublicIp == null || configuredPublicIp.isEmpty())) {
            publicIp = detectPublicIp();
            if (publicIp != null) {
                log.info("🌍 Public IP detected: {}", publicIp);
            }
        } else if (configuredPublicIp != null && !configuredPublicIp.isEmpty()) {
            publicIp = configuredPublicIp;
            log.info("🌍 Using configured public IP: {}", publicIp);
        }

        // Определяем hostname
        try {
            hostname = InetAddress.getLocalHost().getHostName();
            log.info("🏷️  Hostname: {}", hostname);
        } catch (Exception e) {
            hostname = "localhost";
            log.warn("Could not detect hostname, using 'localhost'");
        }

        log.info("✅ Network auto-configuration completed");
        log.info("   Local:  {}", localIp);
        log.info("   VPN:    {}", vpnIp != null ? vpnIp : "Not detected");
        log.info("   Public: {}", publicIp != null ? publicIp : "Not detected");
    }

    /**
     * Определение локального IP адреса
     */
    private String detectLocalIp() {
        try {
            // Сначала пробуем предпочтительный интерфейс
            if (preferredInterface != null && !preferredInterface.isEmpty()) {
                NetworkInterface ni = NetworkInterface.getByName(preferredInterface);
                if (ni != null) {
                    String ip = extractIpFromInterface(ni);
                    if (ip != null) return ip;
                }
            }

            // Перебираем все интерфейсы
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                // Пропускаем loopback и выключенные интерфейсы
                if (ni.isLoopback() || !ni.isUp()) continue;
                String ip = extractIpFromInterface(ni);
                if (ip != null) return ip;
            }

            // Fallback
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            log.error("Error detecting local IP", e);
            return "127.0.0.1";
        }
    }

    private String extractIpFromInterface(NetworkInterface ni) {
        Enumeration<InetAddress> addresses = ni.getInetAddresses();
        while (addresses.hasMoreElements()) {
            InetAddress addr = addresses.nextElement();
            if (!useIpv6 && addr instanceof java.net.Inet6Address) continue;
            if (addr.isLoopbackAddress()) continue;
            String ip = addr.getHostAddress();
            if (ip != null && !ip.isEmpty()) {
                return ip;
            }
        }
        return null;
    }

    private String detectVpnIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                String name = ni.getName().toLowerCase();
                String displayName = ni.getDisplayName().toLowerCase();
                boolean isVpnInterface = name.contains("tun") || name.contains("tap") || name.contains("wg") || name.contains("vpn")
                        || displayName.contains("vpn") || displayName.contains("wireguard") || displayName.contains("openvpn");
                if (!isVpnInterface || !ni.isUp()) continue;
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
            return null;
        } catch (SocketException e) {
            log.error("Error detecting VPN IP", e);
            return null;
        }
    }

    private String detectPrivateIp() { return null; }

    private String detectPublicIp() { return null; }
    
    public String generateTurnUrl() {
        return "turn:" + (publicIp != null ? publicIp : localIp) + ":3478";
    }
    
    public String generateTurnsUrl() {
        return "turns:" + (publicIp != null ? publicIp : localIp) + ":5349";
    }
    
    public String getBestTurnIp() {
        return publicIp != null ? publicIp : localIp;
    }
    
    public String getBestClientIp() {
        return publicIp != null ? publicIp : localIp;
    }
    
    public String getBestIp() {
        return publicIp != null ? publicIp : localIp;
    }
}
