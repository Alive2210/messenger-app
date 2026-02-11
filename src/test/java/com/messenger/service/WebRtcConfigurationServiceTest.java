package com.messenger.service;

import com.messenger.config.NetworkAutoConfiguration;
import com.messenger.config.WebRtcConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebRtcConfigurationServiceTest {

    @Mock
    private WebRtcConfig webRtcConfig;

    @Mock
    private NetworkAutoConfiguration networkConfig;

    private WebRtcConfigurationService service;

    @BeforeEach
    void setUp() {
        service = new WebRtcConfigurationService(webRtcConfig, networkConfig);
        
        // Setup WebRtcConfig
        WebRtcConfig.VideoSettings videoSettings = new WebRtcConfig.VideoSettings();
        videoSettings.setCodec("VP9");
        videoSettings.setBitrate(4000000);
        videoSettings.setFramerate(30);
        videoSettings.setWidth(1920);
        videoSettings.setHeight(1080);
        videoSettings.setDegradationPreference("maintain-resolution");
        when(webRtcConfig.getVideo()).thenReturn(videoSettings);
        
        WebRtcConfig.AudioSettings audioSettings = new WebRtcConfig.AudioSettings();
        audioSettings.setCodec("opus");
        audioSettings.setBitrate(128000);
        audioSettings.setSampleRate(48000);
        audioSettings.setEchoCancellation(true);
        audioSettings.setNoiseSuppression(true);
        audioSettings.setAutoGainControl(false);
        when(webRtcConfig.getAudio()).thenReturn(audioSettings);
        
        WebRtcConfig.P2PSettings p2pSettings = new WebRtcConfig.P2PSettings();
        p2pSettings.setEnabled(true);
        p2pSettings.setStunRetries(3);
        p2pSettings.setTurnRetries(2);
        when(webRtcConfig.getP2p()).thenReturn(p2pSettings);
        
        WebRtcConfig.NetworkSettings networkSettings = new WebRtcConfig.NetworkSettings();
        networkSettings.setIceCandidatePoolSize(10);
        networkSettings.setBundlePolicy("balanced");
        networkSettings.setRtcpMuxPolicy("require");
        when(webRtcConfig.getNetwork()).thenReturn(networkSettings);
        
        // Setup NetworkConfig
        when(networkConfig.getBestTurnIp()).thenReturn("10.0.0.5");
        when(networkConfig.getLocalIp()).thenReturn("192.168.1.100");
        when(networkConfig.getVpnIp()).thenReturn("10.0.0.5");
        when(networkConfig.getPublicIp()).thenReturn("203.0.113.42");
        when(networkConfig.getHostname()).thenReturn("messenger-server");
        when(networkConfig.generateTurnUrl()).thenReturn("turn:10.0.0.5:3478");
    }

    @Test
    @DisplayName("Should generate ICE servers configuration")
    void shouldGenerateIceServersConfiguration() {
        Map<String, Object> config = service.getIceServersConfiguration();
        
        assertNotNull(config);
        assertTrue(config.containsKey("iceServers"));
        assertTrue(config.containsKey("iceCandidatePoolSize"));
        assertTrue(config.containsKey("bundlePolicy"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, String>> iceServers = (List<Map<String, String>>) config.get("iceServers");
        assertFalse(iceServers.isEmpty());
        
        // Check first server is TURN
        Map<String, String> turnServer = iceServers.get(0);
        assertTrue(turnServer.get("urls").startsWith("turn:"));
    }

    @Test
    @DisplayName("Should generate video configuration")
    void shouldGenerateVideoConfiguration() {
        Map<String, Object> config = service.getVideoConfiguration();
        
        assertNotNull(config);
        assertEquals("VP9", config.get("codec"));
        assertEquals(4000000, config.get("bitrate"));
        assertEquals("maintain-resolution", config.get("degradationPreference"));
        assertTrue(config.containsKey("constraints"));
    }

    @Test
    @DisplayName("Should generate audio configuration")
    void shouldGenerateAudioConfiguration() {
        Map<String, Object> config = service.getAudioConfiguration();
        
        assertNotNull(config);
        assertEquals("opus", config.get("codec"));
        assertEquals(128000, config.get("bitrate"));
        assertTrue(config.containsKey("constraints"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> constraints = (Map<String, Object>) config.get("constraints");
        assertEquals(true, constraints.get("echoCancellation"));
        assertEquals(true, constraints.get("noiseSuppression"));
    }

    @Test
    @DisplayName("Should generate full configuration")
    void shouldGenerateFullConfiguration() {
        Map<String, Object> config = service.getFullConfiguration();
        
        assertNotNull(config);
        assertTrue(config.containsKey("iceServers"));
        assertTrue(config.containsKey("video"));
        assertTrue(config.containsKey("audio"));
        assertTrue(config.containsKey("p2p"));
        assertTrue(config.containsKey("network"));
        assertEquals(true, config.get("highQuality"));
        assertEquals(true, config.get("autoDetectedIp"));
    }

    @Test
    @DisplayName("Should refresh network configuration")
    void shouldRefreshNetworkConfiguration() {
        assertDoesNotThrow(() -> service.refreshNetworkConfig());
        verify(networkConfig, atLeastOnce()).detectNetworkConfiguration();
    }

    @Test
    @DisplayName("Should generate P2P configuration")
    void shouldGenerateP2PConfiguration() {
        Map<String, Object> config = service.getP2PConfiguration();
        
        assertNotNull(config);
        assertEquals(true, config.get("enabled"));
        assertEquals(3, config.get("stunRetries"));
        assertEquals(2, config.get("turnRetries"));
        assertEquals("192.168.1.100", config.get("localIp"));
    }

    @Test
    @DisplayName("Should generate SDP constraints")
    void shouldGenerateSdpConstraints() {
        Map<String, Object> config = service.getSdpConstraints();
        
        assertNotNull(config);
        assertTrue(config.containsKey("mandatory"));
        assertTrue(config.containsKey("optional"));
    }

    @Test
    @DisplayName("Should generate offer options")
    void shouldGenerateOfferOptions() {
        Map<String, Object> options = service.getOfferOptions();
        
        assertNotNull(options);
        assertEquals(true, options.get("offerToReceiveAudio"));
        assertEquals(true, options.get("offerToReceiveVideo"));
        assertEquals(false, options.get("voiceActivityDetection"));
    }
}
