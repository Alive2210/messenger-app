package com.messenger.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Конфигурация WebRTC для высококачественного видео без потерь
 */
    @Data
    @Configuration
    @ConfigurationProperties(prefix = "webrtc")
    public class WebRtcConfig {
    
    private List<IceServer> iceServers;
    private VideoSettings video;
    private AudioSettings audio;
    private P2PSettings p2p;
    private NetworkSettings network;
    
    @Data
    public static class IceServer {
        private String url;
        private String username;
        private String credential;
    }
    
    @Data
    public static class VideoSettings {
        private String codec = "VP9";
        private int bitrate = 4000000;  // 4 Mbps
        private int framerate = 30;
        private int width = 1920;
        private int height = 1080;
        private String degradationPreference = "maintain-resolution";
        // Preferred/available codecs for mobile clients (ordered by preference)
        private java.util.List<String> preferredCodecs = java.util.Arrays.asList("VP9", "AV1", "H264");
    }
    
    @Data
    public static class AudioSettings {
        private String codec = "opus";
        private int bitrate = 128000;  // 128 kbps
        private int sampleRate = 48000;
        private boolean echoCancellation = true;
        private boolean noiseSuppression = true;
        private boolean autoGainControl = false;
    }
    
    @Data
    public static class P2PSettings {
        private boolean enabled = true;
        private int stunRetries = 3;
        private int turnRetries = 2;
    }
    
    @Data
    public static class NetworkSettings {
        private int iceCandidatePoolSize = 10;
        private String bundlePolicy = "balanced";
        private String rtcpMuxPolicy = "require";
        private String tcpCandidatePolicy = "enabled";
    }
}
