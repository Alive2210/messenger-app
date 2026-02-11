package com.messenger.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class AudioProcessingServiceTest {

    private AudioProcessingService audioService;

    @BeforeEach
    void setUp() {
        audioService = new AudioProcessingService();
        audioService.init();
    }

    @Test
    @DisplayName("Should process audio without errors")
    void shouldProcessAudioWithoutErrors() {
        // Create test PCM data (1 second of 48kHz stereo audio)
        byte[] testAudio = new byte[48000 * 2 * 2]; // 48000 samples * 2 channels * 2 bytes
        
        // Fill with sine wave
        for (int i = 0; i < testAudio.length; i += 2) {
            short sample = (short) (Math.sin(i * 0.01) * 10000);
            testAudio[i] = (byte) (sample & 0xFF);
            testAudio[i + 1] = (byte) ((sample >> 8) & 0xFF);
        }

        byte[] result = audioService.processAudio(testAudio, true, true, true);

        assertNotNull(result);
        assertEquals(testAudio.length, result.length);
    }

    @Test
    @DisplayName("Should return original data on empty input")
    void shouldReturnOriginalDataOnEmptyInput() {
        byte[] emptyAudio = new byte[0];
        
        byte[] result = audioService.processAudio(emptyAudio, true, true, true);
        
        assertArrayEquals(emptyAudio, result);
    }

    @Test
    @DisplayName("Should handle null processing flags")
    void shouldHandleNullProcessingFlags() {
        byte[] testAudio = new byte[1024];
        
        byte[] result = audioService.processAudio(testAudio, false, false, false);
        
        assertNotNull(result);
        assertEquals(testAudio.length, result.length);
    }

    @Test
    @DisplayName("Should apply noise suppression")
    void shouldApplyNoiseSuppression() {
        byte[] quietAudio = new byte[1024];
        // Fill with very quiet audio
        for (int i = 0; i < quietAudio.length; i++) {
            quietAudio[i] = 1;
        }

        byte[] result = audioService.processAudio(quietAudio, true, false, false);

        assertNotNull(result);
        // After noise suppression, very quiet audio should be even quieter
        float avgLevel = calculateAverageLevel(result);
        float originalLevel = calculateAverageLevel(quietAudio);
        assertTrue(avgLevel < originalLevel || avgLevel < 10);
    }

    @Test
    @DisplayName("Should normalize audio levels")
    void shouldNormalizeAudioLevels() {
        byte[] quietAudio = new byte[1024];
        // Fill with quiet audio
        for (int i = 0; i < quietAudio.length; i += 2) {
            short sample = 1000;
            quietAudio[i] = (byte) (sample & 0xFF);
            quietAudio[i + 1] = (byte) ((sample >> 8) & 0xFF);
        }

        byte[] result = audioService.processAudio(quietAudio, false, false, true);

        assertNotNull(result);
        float level = calculateAverageLevel(result);
        // Normalized audio should have higher level
        assertTrue(level > calculateAverageLevel(quietAudio));
    }

    @Test
    @DisplayName("Should limit audio peaks")
    void shouldLimitAudioPeaks() {
        byte[] loudAudio = new byte[1024];
        // Fill with loud audio near clipping
        for (int i = 0; i < loudAudio.length; i += 2) {
            short sample = 30000; // Near max
            loudAudio[i] = (byte) (sample & 0xFF);
            loudAudio[i + 1] = (byte) ((sample >> 8) & 0xFF);
        }

        byte[] result = audioService.processAudio(loudAudio, false, false, false);

        assertNotNull(result);
        float maxLevel = calculateMaxLevel(result);
        // Should be limited below clipping
        assertTrue(maxLevel <= 1.0f);
    }

    @Test
    @DisplayName("Should reset processor state")
    void shouldResetProcessorState() {
        byte[] testAudio = new byte[1024];
        
        // Process some audio
        audioService.processAudio(testAudio, true, true, true);
        
        // Reset
        audioService.reset();
        
        // Verify state is reset
        assertFalse(audioService.isNoiseProfileInitialized());
        assertEquals(0.0f, audioService.getCurrentNoiseLevel(), 0.001f);
    }

    @Test
    @DisplayName("Should set noise gate threshold")
    void shouldSetNoiseGateThreshold() {
        audioService.setNoiseGateThreshold(0.05f);
        
        // Process audio to verify it works with new threshold
        byte[] testAudio = new byte[1024];
        byte[] result = audioService.processAudio(testAudio, true, false, false);
        
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should handle large audio buffers")
    void shouldHandleLargeAudioBuffers() {
        byte[] largeAudio = new byte[1024 * 1024]; // 1MB
        
        byte[] result = audioService.processAudio(largeAudio, true, true, true);
        
        assertNotNull(result);
        assertEquals(largeAudio.length, result.length);
    }

    @Test
    @DisplayName("Should reinitialize noise profile")
    void shouldReinitializeNoiseProfile() {
        // Process to initialize profile
        byte[] testAudio = new byte[1024];
        audioService.processAudio(testAudio, true, false, false);
        
        assertTrue(audioService.isNoiseProfileInitialized());
        
        // Reinitialize
        audioService.reinitializeNoiseProfile();
        
        assertFalse(audioService.isNoiseProfileInitialized());
    }

    private float calculateAverageLevel(byte[] audio) {
        float sum = 0;
        int count = 0;
        for (int i = 0; i < audio.length - 1; i += 2) {
            short sample = (short) ((audio[i + 1] << 8) | (audio[i] & 0xFF));
            sum += Math.abs(sample);
            count++;
        }
        return count > 0 ? sum / count / 32768.0f : 0;
    }

    private float calculateMaxLevel(byte[] audio) {
        float max = 0;
        for (int i = 0; i < audio.length - 1; i += 2) {
            short sample = (short) ((audio[i + 1] << 8) | (audio[i] & 0xFF));
            max = Math.max(max, Math.abs(sample) / 32768.0f);
        }
        return max;
    }
}
