package com.messenger.service;

import com.messenger.config.NetworkAutoConfiguration;
import com.messenger.config.WebRtcConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Сервис для предоставления WebRTC конфигурации клиентам
 * Работает без статического IP - автоматически определяет сетевые настройки
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebRtcConfigurationService {

    private final WebRtcConfig webRtcConfig;
    private final NetworkAutoConfiguration networkConfig;

    @Value("${TURN_USER:messenger}")
    private String turnUser;

    @Value("${TURN_PASS:secure_password_123}")
    private String turnPass;

    @PostConstruct
    public void init() {
        log.info("🎥 WebRTC Configuration Service initialized");
        log.info("   Best IP for TURN: {}", networkConfig.getBestTurnIp());
        log.info("   Best IP for clients: {}", networkConfig.getBestClientIp());
    }

    /**
     * Получить конфигурацию ICE серверов для клиента
     * Автоматически определяет IP сервера
     */
    public Map<String, Object> getIceServersConfiguration() {
        Map<String, Object> config = new HashMap<>();
        
        // Динамически создаем список ICE серверов с актуальными IP
        List<Map<String, String>> iceServers = new ArrayList<>();
        
        // 1. Локальный TURN сервер (автоопределение IP)
        String turnIp = networkConfig.getBestTurnIp();
        
        // TURN UDP
        Map<String, String> turnUdp = new HashMap<>();
        turnUdp.put("urls", "turn:" + turnIp + ":3478");
        turnUdp.put("username", turnUser);
        turnUdp.put("credential", turnPass);
        iceServers.add(turnUdp);
        
        // TURNS TLS
        Map<String, String> turnTls = new HashMap<>();
        turnTls.put("urls", "turns:" + turnIp + ":5349");
        turnTls.put("username", turnUser);
        turnTls.put("credential", turnPass);
        iceServers.add(turnTls);
        
        // STUN сервер (опционально, для внешних соединений)
        if (networkConfig.getPublicIp() != null) {
            Map<String, String> stun = new HashMap<>();
            stun.put("urls", "stun:" + turnIp + ":3478");
            iceServers.add(stun);
        }
        
        // Резервные публичные STUN серверы (для интернет-соединений)
        Map<String, String> stunGoogle1 = new HashMap<>();
        stunGoogle1.put("urls", "stun:stun.l.google.com:19302");
        iceServers.add(stunGoogle1);
        
        Map<String, String> stunGoogle2 = new HashMap<>();
        stunGoogle2.put("urls", "stun:stun1.l.google.com:19302");
        iceServers.add(stunGoogle2);
        
        config.put("iceServers", iceServers);
        
        // Настройки пула кандидатов
        config.put("iceCandidatePoolSize", webRtcConfig.getNetwork().getIceCandidatePoolSize());
        
        // Приоритет соединений (сначала P2P, потом TURN)
        config.put("bundlePolicy", webRtcConfig.getNetwork().getBundlePolicy());
        config.put("rtcpMuxPolicy", webRtcConfig.getNetwork().getRtcpMuxPolicy());
        
        log.debug("Providing ICE configuration with {} servers, TURN IP: {}", 
                iceServers.size(), turnIp);
        
        return config;
    }

    /**
     * Получить полную конфигурацию видео
     */
    public Map<String, Object> getVideoConfiguration() {
        Map<String, Object> config = new HashMap<>();
        
        WebRtcConfig.VideoSettings video = webRtcConfig.getVideo();
        
        // Конфигурация для getUserMedia
        Map<String, Object> videoConstraints = new HashMap<>();
        
        Map<String, Object> width = new HashMap<>();
        width.put("ideal", video.getWidth());
        width.put("min", 1280);
        
        Map<String, Object> height = new HashMap<>();
        height.put("ideal", video.getHeight());
        height.put("min", 720);
        
        Map<String, Object> frameRate = new HashMap<>();
        frameRate.put("ideal", video.getFramerate());
        frameRate.put("min", 15);
        
        videoConstraints.put("width", width);
        videoConstraints.put("height", height);
        videoConstraints.put("frameRate", frameRate);
        
        // Кодек и битрейт
        config.put("codec", video.getCodec());
        config.put("bitrate", video.getBitrate());
        // Дополнительные видеокодеки (поддержка мобильных клиентов)
        if (video.getPreferredCodecs() != null && !video.getPreferredCodecs().isEmpty()) {
            config.put("preferredCodecs", video.getPreferredCodecs());
        }
        config.put("constraints", videoConstraints);
        config.put("degradationPreference", video.getDegradationPreference());
        
        return config;
    }

    /**
     * Получить конфигурацию аудио
     */
    public Map<String, Object> getAudioConfiguration() {
        Map<String, Object> config = new HashMap<>();
        
        WebRtcConfig.AudioSettings audio = webRtcConfig.getAudio();
        
        // Конфигурация для getUserMedia
        Map<String, Object> audioConstraints = new HashMap<>();
        
        Map<String, Object> sampleRate = new HashMap<>();
        sampleRate.put("ideal", audio.getSampleRate());
        
        Map<String, Object> sampleSize = new HashMap<>();
        sampleSize.put("ideal", 16);
        
        Map<String, Object> channelCount = new HashMap<>();
        channelCount.put("ideal", 2);
        
        audioConstraints.put("sampleRate", sampleRate);
        audioConstraints.put("sampleSize", sampleSize);
        audioConstraints.put("channelCount", channelCount);
        audioConstraints.put("echoCancellation", audio.isEchoCancellation());
        audioConstraints.put("noiseSuppression", audio.isNoiseSuppression());
        audioConstraints.put("autoGainControl", audio.isAutoGainControl());
        
        // Кодек и битрейт
        config.put("codec", audio.getCodec());
        config.put("bitrate", audio.getBitrate());
        config.put("constraints", audioConstraints);
        
        return config;
    }

    /**
     * Получить полную конфигурацию WebRTC для клиента
     */
    public Map<String, Object> getFullConfiguration() {
        Map<String, Object> config = new HashMap<>();
        
        config.put("iceServers", getIceServersConfiguration());
        config.put("video", getVideoConfiguration());
        config.put("audio", getAudioConfiguration());
        config.put("p2p", getP2PConfiguration());
        config.put("network", getNetworkInfo());
        
        // Флаги качества
        config.put("highQuality", true);
        config.put("vpnMode", networkConfig.getVpnIp() != null);
        config.put("autoDetectedIp", true);
        
        log.info("Generated WebRTC configuration:");
        log.info("   Server IP: {}", networkConfig.getBestTurnIp());
        log.info("   Codec: {} @ {} Mbps", 
                webRtcConfig.getVideo().getCodec(), 
                webRtcConfig.getVideo().getBitrate() / 1000000);
        
        return config;
    }

    /**
     * Получить информацию о сети
     */
    private Map<String, Object> getNetworkInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("localIp", networkConfig.getLocalIp());
        info.put("vpnIp", networkConfig.getVpnIp());
        info.put("publicIp", networkConfig.getPublicIp());
        info.put("hostname", networkConfig.getHostname());
        info.put("turnUrl", networkConfig.generateTurnUrl());
        return info;
    }

    /**
     * Получить конфигурацию P2P
     */
    public Map<String, Object> getP2PConfiguration() {
        Map<String, Object> config = new HashMap<>();
        
        WebRtcConfig.P2PSettings p2p = webRtcConfig.getP2p();
        
        config.put("enabled", p2p.isEnabled());
        config.put("stunRetries", p2p.getStunRetries());
        config.put("turnRetries", p2p.getTurnRetries());
        config.put("localIp", networkConfig.getLocalIp());
        config.put("vpnIp", networkConfig.getVpnIp());
        
        return config;
    }

    /**
     * Получить SDP constraints для setLocalDescription
     */
    public Map<String, Object> getSdpConstraints() {
        Map<String, Object> sdpConstraints = new HashMap<>();
        
        Map<String, Boolean> mandatory = new HashMap<>();
        mandatory.put("OfferToReceiveAudio", true);
        mandatory.put("OfferToReceiveVideo", true);
        
        sdpConstraints.put("mandatory", mandatory);
        sdpConstraints.put("optional", new HashMap<>());
        
        return sdpConstraints;
    }

    /**
     * Получить RTCOfferOptions для createOffer
     */
    public Map<String, Object> getOfferOptions() {
        Map<String, Object> options = new HashMap<>();
        
        options.put("offerToReceiveAudio", true);
        options.put("offerToReceiveVideo", true);
        options.put("voiceActivityDetection", false);  // Отключаем для постоянного битрейта
        options.put("iceRestart", false);
        
        return options;
    }

    /**
     * Обновить сетевую конфигурацию (вызывать при изменении IP)
     */
    public void refreshNetworkConfig() {
        log.info("🔄 Refreshing network configuration...");
        networkConfig.detectNetworkConfiguration();
        log.info("✅ Network configuration refreshed");
        log.info("   New TURN IP: {}", networkConfig.getBestTurnIp());
    }
}
