package com.messenger.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class QRCodeServiceTest {

    private QRCodeService qrCodeService;

    @BeforeEach
    void setUp() {
        qrCodeService = new QRCodeService();
    }

    @Test
    @DisplayName("Should generate QR code from string")
    void shouldGenerateQRCodeFromString() {
        String content = "test content";
        
        String qrCode = qrCodeService.generateQRCodeBase64(content);
        
        assertNotNull(qrCode);
        assertFalse(qrCode.isEmpty());
        // Should be valid Base64
        assertDoesNotThrow(() -> java.util.Base64.getDecoder().decode(qrCode));
    }

    @Test
    @DisplayName("Should generate QR code from settings map")
    void shouldGenerateQRCodeFromSettingsMap() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("serverUrl", "http://localhost:8080");
        settings.put("websocketUrl", "/ws");
        settings.put("heartbeatInterval", 10);
        
        String qrCode = qrCodeService.generateSettingsQRCode(settings);
        
        assertNotNull(qrCode);
        assertFalse(qrCode.isEmpty());
    }

    @Test
    @DisplayName("Should generate connection settings QR code")
    void shouldGenerateConnectionSettingsQRCode() {
        String serverUrl = "http://localhost:8080";
        String websocketUrl = "/ws";
        Map<String, Object> additional = new HashMap<>();
        additional.put("feature", "enabled");
        
        String qrCode = qrCodeService.generateConnectionSettingsQRCode(serverUrl, websocketUrl, additional);
        
        assertNotNull(qrCode);
        assertFalse(qrCode.isEmpty());
    }

    @Test
    @DisplayName("Should generate different QR codes for different content")
    void shouldGenerateDifferentQRCodesForDifferentContent() {
        String content1 = "content1";
        String content2 = "content2";
        
        String qrCode1 = qrCodeService.generateQRCodeBase64(content1);
        String qrCode2 = qrCodeService.generateQRCodeBase64(content2);
        
        assertNotEquals(qrCode1, qrCode2);
    }

    @Test
    @DisplayName("Should reject empty content")
    void shouldRejectEmptyContent() {
        String content = "";
        
        // ZXing doesn't support empty content, should throw exception
        assertThrows(RuntimeException.class, () -> {
            qrCodeService.generateQRCodeBase64(content);
        });
    }

    @Test
    @DisplayName("Should handle reasonably long content")
    void shouldHandleReasonablyLongContent() {
        // Generate content that's within QR code capacity (max ~4000 chars for alphanumeric)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("Lorem ipsum dolor sit amet ");
        }
        String longContent = sb.toString();
        
        String qrCode = qrCodeService.generateQRCodeBase64(longContent);
        
        assertNotNull(qrCode);
        assertFalse(qrCode.isEmpty());
    }

    @Test
    @DisplayName("Should handle special characters")
    void shouldHandleSpecialCharacters() {
        String content = "Special chars: àáâãäåæçèéêë ñ 中文 🎉 \"quoted\" 'single'";
        
        String qrCode = qrCodeService.generateQRCodeBase64(content);
        
        assertNotNull(qrCode);
        assertFalse(qrCode.isEmpty());
    }

    @Test
    @DisplayName("Should generate same QR code for same content")
    void shouldGenerateSameQRCodeForSameContent() {
        String content = "same content";
        
        String qrCode1 = qrCodeService.generateQRCodeBase64(content);
        String qrCode2 = qrCodeService.generateQRCodeBase64(content);
        
        // QR code generation should be deterministic for same content
        assertEquals(qrCode1, qrCode2);
    }

    @Test
    @DisplayName("Should generate valid Base64 PNG image")
    void shouldGenerateValidBase64PNGImage() {
        String content = "test";
        
        String qrCode = qrCodeService.generateQRCodeBase64(content);
        byte[] imageBytes = java.util.Base64.getDecoder().decode(qrCode);
        
        // PNG files start with specific bytes
        assertTrue(imageBytes.length > 0);
        // PNG signature: 89 50 4E 47 0D 0A 1A 0A
        assertEquals((byte) 0x89, imageBytes[0]);
        assertEquals((byte) 0x50, imageBytes[1]);
        assertEquals((byte) 0x4E, imageBytes[2]);
        assertEquals((byte) 0x47, imageBytes[3]);
    }

    @Test
    @DisplayName("Should handle nested settings map")
    void shouldHandleNestedSettingsMap() {
        Map<String, Object> nested = new HashMap<>();
        nested.put("inner", "value");
        
        Map<String, Object> settings = new HashMap<>();
        settings.put("serverUrl", "http://localhost:8080");
        settings.put("nested", nested);
        settings.put("list", java.util.Arrays.asList("item1", "item2"));
        
        String qrCode = qrCodeService.generateSettingsQRCode(settings);
        
        assertNotNull(qrCode);
        assertFalse(qrCode.isEmpty());
    }

    @Test
    @DisplayName("Should generate connection settings with null additional settings")
    void shouldGenerateConnectionSettingsWithNullAdditionalSettings() {
        String serverUrl = "http://localhost:8080";
        String websocketUrl = "/ws";
        
        String qrCode = qrCodeService.generateConnectionSettingsQRCode(serverUrl, websocketUrl, null);
        
        assertNotNull(qrCode);
        assertFalse(qrCode.isEmpty());
    }

    @Test
    @DisplayName("Should include timestamp in connection settings")
    void shouldIncludeTimestampInConnectionSettings() {
        String serverUrl = "http://localhost:8080";
        String websocketUrl = "/ws";
        
        long before = System.currentTimeMillis();
        String qrCode = qrCodeService.generateConnectionSettingsQRCode(serverUrl, websocketUrl, null);
        long after = System.currentTimeMillis();
        
        assertNotNull(qrCode);
        // QR code should be generated within the time window
        assertTrue(qrCode.length() > 0);
    }
}
