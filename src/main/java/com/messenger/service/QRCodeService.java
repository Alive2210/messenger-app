package com.messenger.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class QRCodeService {

    private static final int QR_CODE_SIZE = 300;

    /**
     * Generate QR code as Base64 string
     */
    public String generateQRCodeBase64(String content) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE, hints);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", outputStream);
            byte[] imageBytes = outputStream.toByteArray();

            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            log.error("Error generating QR code", e);
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    /**
     * Generate QR code with connection settings
     */
    public String generateSettingsQRCode(Map<String, Object> settings) {
        try {
            // Convert settings to JSON string
            String settingsJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(settings);
            return generateQRCodeBase64(settingsJson);
        } catch (Exception e) {
            log.error("Error generating settings QR code", e);
            throw new RuntimeException("Failed to generate settings QR code", e);
        }
    }

    /**
     * Generate connection settings QR code for device sharing
     */
    public String generateConnectionSettingsQRCode(String serverUrl, String websocketUrl, 
                                                    Map<String, Object> additionalSettings) {
        Map<String, Object> settings = new HashMap<>();
        settings.put("serverUrl", serverUrl);
        settings.put("websocketUrl", websocketUrl);
        settings.put("timestamp", System.currentTimeMillis());
        
        if (additionalSettings != null) {
            settings.putAll(additionalSettings);
        }
        
        return generateSettingsQRCode(settings);
    }
}
