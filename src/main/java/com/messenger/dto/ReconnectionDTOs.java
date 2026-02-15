package com.messenger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO для управления переподключением WebSocket
 */
public class ReconnectionDTOs {

    /**
     * Запрос на переподключение от клиента
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReconnectRequest {
        private String oldSessionId;      // ID предыдущей сессии
        private String username;           // Имя пользователя
        private String deviceId;           // ID устройства
        private String lastSequenceNumber; // Последний полученный sequence number
        private String reconnectToken;     // Токен для авторизации переподключения
    }

    /**
     * Ответ на запрос переподключения
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReconnectResponse {
        private boolean success;           // Успешно ли переподключение
        private String newSessionId;       // ID новой сессии
        private String message;            // Сообщение
        private ConnectionStatus status;   // Статус соединения
        private long serverTime;           // Время сервера
    }

    /**
     * Статус соединения
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConnectionStatus {
        private boolean connected;         // Подключено ли
        private int reconnectAttempts;     // Количество попыток
        private long timeSinceDisconnect;  // Время с момента отключения (мс)
        private long remainingTime;        // Оставшееся время для переподключения (мс)
        private ConnectionQuality quality; // Качество соединения
    }

    /**
     * Отчет о качестве соединения
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QualityReport {
        private String sessionId;
        private long timestamp;
        private int rtt;                   // Round-trip time (ms)
        private double packetLoss;         // Потеря пакетов (%)
        private double jitter;             // Джиттер (ms)
        private int bandwidth;             // Пропускная способность (kbps)
        private ConnectionQuality quality; // Оценка качества
    }

    /**
     * Команда серверу при обрыве связи
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConnectionInterrupted {
        private String sessionId;
        private String reason;
        private long timestamp;
        private boolean willReconnect;     // Будет ли клиент пытаться переподключиться
    }

    /**
     * Запрос на восстановление видео потока
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VideoRecoveryRequest {
        private String conferenceId;
        private String participantId;
        private long fromSequence;         // С какого sequence number восстановить
    }

    /**
     * Ответ с буферизированными видео фреймами
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VideoRecoveryResponse {
        private boolean success;
        private String conferenceId;
        private String participantId;
        private byte[][] frames;           // Буферизированные фреймы
        private long lastSequence;         // Последний sequence number
        private int totalFrames;           // Общее количество фреймов
    }

    /**
     * Уведомление об обрыве соединения
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConnectionLostEvent {
        private String sessionId;
        private String username;
        private String reason;
        private long timestamp;
        private boolean autoReconnectEnabled; // Включено ли авто-переподключение
    }

    /**
     * Уведомление об успешном переподключении
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReconnectionSuccessEvent {
        private String oldSessionId;
        private String newSessionId;
        private String username;
        private long reconnectTime;        // Время переподключения (мс)
        private boolean missedMessages;    // Были ли пропущены сообщения
    }

    public enum ConnectionQuality {
        EXCELLENT(5),    // > 90% - Отлично
        GOOD(4),         // 70-90% - Хорошо
        FAIR(3),         // 50-70% - Средне
        POOR(2),         // 30-50% - Плохо
        CRITICAL(1),     // 10-30% - Критично
        DISCONNECTED(0); // < 10% - Отключено

        private final int level;

        ConnectionQuality(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }

        public boolean isUsable() {
            return level >= 3; // FAIR или лучше
        }
    }
}
