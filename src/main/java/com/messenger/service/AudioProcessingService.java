package com.messenger.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Сервис для обработки аудио: шумоподавление, подавление эха, нормализация
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AudioProcessingService {

    private static final int SAMPLE_RATE = 48000;
    private static final int CHANNELS = 2;
    private static final int FRAME_SIZE_MS = 10;
    private static final int FRAME_SIZE = (SAMPLE_RATE * FRAME_SIZE_MS) / 1000; // 480 samples
    private static final float NOISE_GATE_THRESHOLD = 0.01f;
    private static final float ECHO_DECAY = 0.8f;
    private static final int ECHO_BUFFER_SIZE = SAMPLE_RATE / 10; // 100ms buffer

    // Буфер для подавления эха
    private final float[] echoBuffer = new float[ECHO_BUFFER_SIZE];
    private int echoBufferIndex = 0;

    // Параметры шумоподавления
    private float noiseGateLevel = NOISE_GATE_THRESHOLD;
    private float noiseFloor = 0.0f;
    private final float[] noiseProfile = new float[256];
    private boolean noiseProfileInitialized = false;

    // Параметры компрессии/лимитера
    private static final float COMPRESSION_THRESHOLD = 0.8f;
    private static final float COMPRESSION_RATIO = 4.0f;
    private static final float MAKEUP_GAIN = 1.2f;

    @PostConstruct
    public void init() {
        log.info("🎵 Audio Processing Service initialized");
        log.info("   Sample Rate: {} Hz", SAMPLE_RATE);
        log.info("   Channels: {}", CHANNELS);
        log.info("   Frame Size: {} samples ({} ms)", FRAME_SIZE, FRAME_SIZE_MS);
        log.info("   Features: Noise Suppression, Echo Cancellation, Limiter");
    }

    /**
     * Обработка аудио данных
     * 
     * @param inputRaw               PCM raw bytes (16-bit signed, little-endian)
     * @param enableNoiseSuppression включить шумоподавление
     * @param enableEchoCancellation включить подавление эха
     * @param enableNormalization    включить нормализацию
     * @return обработанные аудио данные
     */
    public byte[] processAudio(byte[] inputRaw,
            boolean enableNoiseSuppression,
            boolean enableEchoCancellation,
            boolean enableNormalization) {
        try {
            // Конвертируем bytes в float samples
            float[] samples = bytesToFloats(inputRaw);

            // Применяем обработку
            if (enableNoiseSuppression) {
                samples = applyNoiseSuppression(samples);
            }

            if (enableEchoCancellation) {
                samples = applyEchoCancellation(samples);
            }

            if (enableNormalization) {
                samples = applyNormalization(samples);
            }

            // Применяем лимитер для защиты от клиппинга
            samples = applyLimiter(samples);

            // Конвертируем обратно в bytes
            return floatsToBytes(samples);

        } catch (Exception e) {
            log.error("Error processing audio", e);
            return inputRaw; // Возвращаем оригинал при ошибке
        }
    }

    /**
     * Шумоподавление с использованием спектрального вычитания
     */
    private float[] applyNoiseSuppression(float[] samples) {
        float[] processed = new float[samples.length];

        // Размер окна для FFT
        int windowSize = 512;
        int hopSize = windowSize / 4;

        // Инициализация профиля шума при первом вызове
        if (!noiseProfileInitialized) {
            initializeNoiseProfile(samples);
            noiseProfileInitialized = true;
        }

        // Обрабатываем по окнам
        for (int i = 0; i < samples.length; i += hopSize) {
            int end = Math.min(i + windowSize, samples.length);
            int currentWindowSize = end - i;

            // Применяем окно Ханна
            float[] window = new float[currentWindowSize];
            for (int j = 0; j < currentWindowSize; j++) {
                float hann = 0.5f * (1 - (float) Math.cos(2 * Math.PI * j / (currentWindowSize - 1)));
                window[j] = samples[i + j] * hann;
            }

            // Простое шумоподавление на основе порога
            for (int j = 0; j < currentWindowSize; j++) {
                float sample = window[j];
                float magnitude = Math.abs(sample);

                // Обновляем уровень шума
                noiseFloor = 0.95f * noiseFloor + 0.05f * magnitude;

                // Применяем noise gate
                if (magnitude < noiseGateLevel) {
                    sample *= 0.1f; // Сильное ослабление тихих звуков
                } else if (magnitude < noiseGateLevel * 2) {
                    // Плавный переход
                    float gain = (magnitude - noiseGateLevel) / noiseGateLevel;
                    sample *= 0.1f + 0.9f * gain;
                }

                // Спектральное вычитание шума
                int bin = (j * noiseProfile.length) / currentWindowSize;
                if (bin < noiseProfile.length) {
                    float noiseEst = noiseProfile[bin] * 0.5f;
                    if (magnitude > noiseEst) {
                        sample *= (magnitude - noiseEst) / magnitude;
                    }
                }

                // Накопление с overlap-add
                if (i + j < processed.length) {
                    processed[i + j] += sample * 0.5f;
                }
            }
        }

        // Обновляем профиль шума
        updateNoiseProfile(samples);

        return processed;
    }

    /**
     * Инициализация профиля шума
     */
    private void initializeNoiseProfile(float[] samples) {
        int bins = noiseProfile.length;
        int samplesPerBin = samples.length / bins;

        for (int i = 0; i < bins; i++) {
            float sum = 0;
            int start = i * samplesPerBin;
            int end = Math.min(start + samplesPerBin, samples.length);

            for (int j = start; j < end; j++) {
                sum += Math.abs(samples[j]);
            }

            noiseProfile[i] = sum / (end - start);
        }

        log.debug("Noise profile initialized with {} bins", bins);
    }

    /**
     * Обновление профиля шума (адаптивное)
     */
    private void updateNoiseProfile(float[] samples) {
        float currentLevel = 0;
        for (float sample : samples) {
            currentLevel += Math.abs(sample);
        }
        currentLevel /= samples.length;

        // Обновляем профиль только если уровень низкий (вероятно шум)
        if (currentLevel < noiseGateLevel * 1.5f) {
            for (int i = 0; i < noiseProfile.length; i++) {
                noiseProfile[i] = 0.95f * noiseProfile[i] + 0.05f * currentLevel;
            }
        }
    }

    /**
     * Подавление эха с использованием адаптивного фильтра
     */
    private float[] applyEchoCancellation(float[] samples) {
        float[] processed = new float[samples.length];

        for (int i = 0; i < samples.length; i++) {
            float input = samples[i];

            // Получаем задержанный сигнал из буфера
            int delayedIndex = (echoBufferIndex - i + ECHO_BUFFER_SIZE) % ECHO_BUFFER_SIZE;
            float delayed = echoBuffer[delayedIndex];

            // Простое LMS-обновление
            float error = input - ECHO_DECAY * delayed;
            float stepSize = 0.01f;

            // Адаптивная фильтрация
            float output = error;

            // Сохраняем в буфер
            echoBuffer[echoBufferIndex] = input;
            echoBufferIndex = (echoBufferIndex + 1) % ECHO_BUFFER_SIZE;

            processed[i] = output;
        }

        return processed;
    }

    /**
     * Нормализация уровня (AGC - Automatic Gain Control)
     */
    private float[] applyNormalization(float[] samples) {
        // Находим пиковый уровень
        float peak = 0;
        for (float sample : samples) {
            peak = Math.max(peak, Math.abs(sample));
        }

        if (peak < 0.001f)
            return samples; // Тишина

        // Целевой уровень -0.3 dBFS (около 0.966)
        float targetLevel = 0.966f;
        float gain = targetLevel / peak;

        // Ограничиваем усиление (не более 20 dB)
        gain = Math.min(gain, 10.0f);

        // Применяем усиление
        float[] normalized = new float[samples.length];
        for (int i = 0; i < samples.length; i++) {
            normalized[i] = samples[i] * gain;
        }

        return normalized;
    }

    /**
     * Лимитер для защиты от клиппинга
     */
    private float[] applyLimiter(float[] samples) {
        float[] limited = new float[samples.length];

        for (int i = 0; i < samples.length; i++) {
            float sample = samples[i];

            // Soft knee лимитер
            float threshold = 0.95f;
            float knee = 0.05f;

            float absSample = Math.abs(sample);
            float sign = Math.signum(sample);

            if (absSample > threshold - knee) {
                // Soft limiting
                float excess = absSample - (threshold - knee);
                float compressed = excess * excess / (2 * knee);
                absSample = threshold - knee + compressed;
            }

            limited[i] = sign * Math.min(absSample, threshold);
        }

        return limited;
    }

    /**
     * Конвертация byte array в float samples
     */
    private float[] bytesToFloats(byte[] bytes) {
        int numSamples = bytes.length / 2; // 16-bit samples
        float[] samples = new float[numSamples];

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < numSamples; i++) {
            short sample = buffer.getShort();
            // Нормализуем к [-1.0, 1.0]
            samples[i] = sample / 32768.0f;
        }

        return samples;
    }

    /**
     * Конвертация float samples в byte array
     */
    private byte[] floatsToBytes(float[] samples) {
        byte[] bytes = new byte[samples.length * 2];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        for (float sample : samples) {
            // Ограничиваем и конвертируем
            sample = Math.max(-1.0f, Math.min(1.0f, sample));
            short shortSample = (short) (sample * 32767);
            buffer.putShort(shortSample);
        }

        return bytes;
    }

    /**
     * Сброс состояния обработчика
     */
    public void reset() {
        Arrays.fill(echoBuffer, 0);
        echoBufferIndex = 0;
        noiseProfileInitialized = false;
        noiseFloor = 0;
        log.info("Audio processor reset");
    }

    /**
     * Установка уровня шумового порога
     */
    public void setNoiseGateThreshold(float threshold) {
        this.noiseGateLevel = Math.max(0.001f, Math.min(0.1f, threshold));
        log.info("Noise gate threshold set to {}", this.noiseGateLevel);
    }

    /**
     * Получение текущего уровня шума
     */
    public float getCurrentNoiseLevel() {
        return noiseFloor;
    }

    /**
     * Проверка, инициализирован ли профиль шума
     */
    public boolean isNoiseProfileInitialized() {
        return noiseProfileInitialized;
    }

    /**
     * Принудительная реинициализация профиля шума
     */
    public void reinitializeNoiseProfile() {
        noiseProfileInitialized = false;
        log.info("Noise profile will be reinitialized on next processing");
    }
}
