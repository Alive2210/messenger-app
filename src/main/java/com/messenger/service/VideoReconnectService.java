package com.messenger.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Сервис управления видео сессиями с ожиданием реконнекта
 * Обеспечивает grace period для восстановления видео потока
 */
@Slf4j
@Component
public class VideoReconnectService {

    // Grace period для ожидания реконнекта (10 секунд)
    private static final long GRACE_PERIOD_MS = 10000;
    
    // Интервал проверки истекших сессий
    private static final long CLEANUP_INTERVAL_MS = 5000;

    private final VideoStreamBuffer videoStreamBuffer;
    private final Map<String, VideoSession> videoSessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public VideoReconnectService(VideoStreamBuffer videoStreamBuffer) {
        this.videoStreamBuffer = videoStreamBuffer;
        // Запускаем периодическую очистку
        scheduler.scheduleAtFixedRate(this::cleanupExpiredSessions, 
                CLEANUP_INTERVAL_MS, CLEANUP_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Регистрирует видео сессию участника
     */
    public void registerVideoSession(String sessionId, String conferenceId, 
                                     String username, String deviceId) {
        String key = buildSessionKey(conferenceId, username);
        
        VideoSession session = new VideoSession();
        session.setSessionId(sessionId);
        session.setConferenceId(conferenceId);
        session.setUsername(username);
        session.setDeviceId(deviceId);
        session.setActive(true);
        session.setStartTime(Instant.now());
        session.setLastActivity(Instant.now());
        
        videoSessions.put(key, session);
        log.info("🎥 Зарегистрирована видео сессия {} для {} в конференции {}", 
                sessionId, username, conferenceId);
    }

    /**
     * Отмечает отключение видео сессии и запускает grace period
     */
    public void handleVideoDisconnection(String sessionId, String conferenceId, 
                                         String username, String reason) {
        String key = buildSessionKey(conferenceId, username);
        VideoSession session = videoSessions.get(key);
        
        if (session == null) {
            log.warn("⚠️ Попытка отключения неизвестной видео сессии: {}", key);
            return;
        }

        if (session.isActive()) {
            session.setActive(false);
            session.setDisconnectTime(Instant.now());
            session.setDisconnectReason(reason);
            session.setDisconnectSessionId(sessionId);
            
            log.info("🔌 Видео сессия {} отключена: {}. Grace period: {} сек. Буфер сохраняется...", 
                    sessionId, reason, GRACE_PERIOD_MS / 1000);
            
            // Буфер НЕ очищаем - он сохраняется для возможного реконнекта
            // Очистка произойдет автоматически через cleanupExpiredSessions
        }
    }

    /**
     * Подтверждает реконнект видео сессии
     */
    public boolean confirmVideoReconnection(String oldSessionId, String newSessionId,
                                            String conferenceId, String username) {
        String key = buildSessionKey(conferenceId, username);
        VideoSession session = videoSessions.get(key);
        
        if (session == null) {
            log.warn("⚠️ Попытка реконнекта для неизвестной сессии: {}", key);
            return false;
        }

        // Проверяем, не истек ли grace period
        if (session.getDisconnectTime() != null) {
            long disconnectDuration = Instant.now().toEpochMilli() - 
                    session.getDisconnectTime().toEpochMilli();
            
            if (disconnectDuration > GRACE_PERIOD_MS) {
                log.warn("⏰ Grace period истек для сессии {} (прошло {} мс)", 
                        oldSessionId, disconnectDuration);
                
                // Очищаем буфер
                videoStreamBuffer.clearBuffer(conferenceId, username);
                videoSessions.remove(key);
                return false;
            }
        }

        // Восстанавливаем сессию
        session.setActive(true);
        session.setSessionId(newSessionId);
        session.setReconnectTime(Instant.now());
        session.setReconnectCount(session.getReconnectCount() + 1);
        session.setDisconnectTime(null);
        session.setDisconnectReason(null);
        session.setLastActivity(Instant.now());
        
        log.info("✅ Видео сессия {} успешно переподключена как {} (попытка #{})", 
                oldSessionId, newSessionId, session.getReconnectCount());
        
        return true;
    }

    /**
     * Обновляет активность сессии
     */
    public void updateActivity(String conferenceId, String username) {
        String key = buildSessionKey(conferenceId, username);
        VideoSession session = videoSessions.get(key);
        if (session != null) {
            session.setLastActivity(Instant.now());
        }
    }

    /**
     * Проверяет, находится ли сессия в grace period
     */
    public boolean isInGracePeriod(String conferenceId, String username) {
        String key = buildSessionKey(conferenceId, username);
        VideoSession session = videoSessions.get(key);
        
        if (session == null || session.isActive()) {
            return false;
        }

        if (session.getDisconnectTime() == null) {
            return false;
        }

        long disconnectDuration = Instant.now().toEpochMilli() - 
                session.getDisconnectTime().toEpochMilli();
        return disconnectDuration < GRACE_PERIOD_MS;
    }

    /**
     * Получает оставшееся время grace period
     */
    public long getRemainingGracePeriodMs(String conferenceId, String username) {
        String key = buildSessionKey(conferenceId, username);
        VideoSession session = videoSessions.get(key);
        
        if (session == null || session.isActive() || session.getDisconnectTime() == null) {
            return 0;
        }

        long disconnectDuration = Instant.now().toEpochMilli() - 
                session.getDisconnectTime().toEpochMilli();
        return Math.max(0, GRACE_PERIOD_MS - disconnectDuration);
    }

    /**
     * Полностью удаляет видео сессию
     */
    public void removeVideoSession(String conferenceId, String username) {
        String key = buildSessionKey(conferenceId, username);
        VideoSession session = videoSessions.remove(key);
        
        if (session != null) {
            videoStreamBuffer.clearBuffer(conferenceId, username);
            log.info("🗑️ Видео сессия удалена для {} в конференции {}", username, conferenceId);
        }
    }

    /**
     * Удаляет все сессии конференции
     */
    public void removeConferenceSessions(String conferenceId) {
        String prefix = conferenceId + ":";
        
        videoSessions.keySet().removeIf(key -> {
            if (key.startsWith(prefix)) {
                VideoSession session = videoSessions.get(key);
                if (session != null) {
                    videoStreamBuffer.clearBuffer(conferenceId, session.getUsername());
                    log.debug("🗑️ Удалена видео сессия для {} в конференции {}", 
                            session.getUsername(), conferenceId);
                }
                return true;
            }
            return false;
        });
        
        log.info("🗑️ Все видео сессии удалены для конференции {}", conferenceId);
    }

    /**
     * Получает статус видео сессии
     */
    public VideoSessionStatus getSessionStatus(String conferenceId, String username) {
        String key = buildSessionKey(conferenceId, username);
        VideoSession session = videoSessions.get(key);
        
        if (session == null) {
            return null;
        }

        VideoSessionStatus status = new VideoSessionStatus();
        status.setConferenceId(conferenceId);
        status.setUsername(username);
        status.setActive(session.isActive());
        status.setInGracePeriod(isInGracePeriod(conferenceId, username));
        status.setRemainingGracePeriodMs(getRemainingGracePeriodMs(conferenceId, username));
        status.setReconnectCount(session.getReconnectCount());
        
        if (session.getDisconnectTime() != null) {
            status.setDisconnectTime(session.getDisconnectTime().toEpochMilli());
        }
        
        // Получаем статус буфера
        VideoStreamBuffer.BufferStatus bufferStatus = videoStreamBuffer.getStatus(
                conferenceId, username);
        if (bufferStatus != null) {
            status.setBufferFrameCount(bufferStatus.getFrameCount());
            status.setBufferSizeBytes(bufferStatus.getTotalSizeBytes());
        }
        
        return status;
    }

    /**
     * Очистка истекших сессий
     */
    private void cleanupExpiredSessions() {
        long now = Instant.now().toEpochMilli();
        
        videoSessions.entrySet().removeIf(entry -> {
            VideoSession session = entry.getValue();
            
            // Если сессия неактивна и grace period истек
            if (!session.isActive() && session.getDisconnectTime() != null) {
                long disconnectDuration = now - session.getDisconnectTime().toEpochMilli();
                
                if (disconnectDuration > GRACE_PERIOD_MS) {
                    log.info("⏰ Grace period истек для {} в конференции {}. Очистка буфера.",
                            session.getUsername(), session.getConferenceId());
                    
                    videoStreamBuffer.clearBuffer(
                            session.getConferenceId(), 
                            session.getUsername()
                    );
                    return true;
                }
            }
            
            // Если сессия активна но неактивна более 10 минут
            if (session.isActive() && session.getLastActivity() != null) {
                long inactiveDuration = now - session.getLastActivity().toEpochMilli();
                if (inactiveDuration > 600000) { // 10 минут
                    log.info("⏰ Сессия {} неактивна более 10 минут. Удаление.", entry.getKey());
                    videoStreamBuffer.clearBuffer(
                            session.getConferenceId(), 
                            session.getUsername()
                    );
                    return true;
                }
            }
            
            return false;
        });
    }

    private String buildSessionKey(String conferenceId, String username) {
        return conferenceId + ":" + username;
    }

    /**
     * Данные видео сессии
     */
    public static class VideoSession {
        private String sessionId;
        private String conferenceId;
        private String username;
        private String deviceId;
        private volatile boolean active;
        private Instant startTime;
        private Instant lastActivity;
        private Instant disconnectTime;
        private String disconnectReason;
        private String disconnectSessionId;
        private int reconnectCount = 0;
        private Instant reconnectTime;

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getConferenceId() { return conferenceId; }
        public void setConferenceId(String conferenceId) { this.conferenceId = conferenceId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public Instant getStartTime() { return startTime; }
        public void setStartTime(Instant startTime) { this.startTime = startTime; }
        public Instant getLastActivity() { return lastActivity; }
        public void setLastActivity(Instant lastActivity) { this.lastActivity = lastActivity; }
        public Instant getDisconnectTime() { return disconnectTime; }
        public void setDisconnectTime(Instant disconnectTime) { this.disconnectTime = disconnectTime; }
        public String getDisconnectReason() { return disconnectReason; }
        public void setDisconnectReason(String disconnectReason) { this.disconnectReason = disconnectReason; }
        public String getDisconnectSessionId() { return disconnectSessionId; }
        public void setDisconnectSessionId(String disconnectSessionId) { this.disconnectSessionId = disconnectSessionId; }
        public int getReconnectCount() { return reconnectCount; }
        public void setReconnectCount(int reconnectCount) { this.reconnectCount = reconnectCount; }
        public Instant getReconnectTime() { return reconnectTime; }
        public void setReconnectTime(Instant reconnectTime) { this.reconnectTime = reconnectTime; }
    }

    /**
     * Статус видео сессии для API
     */
    public static class VideoSessionStatus {
        private String conferenceId;
        private String username;
        private boolean active;
        private boolean inGracePeriod;
        private long remainingGracePeriodMs;
        private int reconnectCount;
        private long disconnectTime;
        private int bufferFrameCount;
        private long bufferSizeBytes;

        public String getConferenceId() { return conferenceId; }
        public void setConferenceId(String conferenceId) { this.conferenceId = conferenceId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public boolean isInGracePeriod() { return inGracePeriod; }
        public void setInGracePeriod(boolean inGracePeriod) { this.inGracePeriod = inGracePeriod; }
        public long getRemainingGracePeriodMs() { return remainingGracePeriodMs; }
        public void setRemainingGracePeriodMs(long remainingGracePeriodMs) { this.remainingGracePeriodMs = remainingGracePeriodMs; }
        public int getReconnectCount() { return reconnectCount; }
        public void setReconnectCount(int reconnectCount) { this.reconnectCount = reconnectCount; }
        public long getDisconnectTime() { return disconnectTime; }
        public void setDisconnectTime(long disconnectTime) { this.disconnectTime = disconnectTime; }
        public int getBufferFrameCount() { return bufferFrameCount; }
        public void setBufferFrameCount(int bufferFrameCount) { this.bufferFrameCount = bufferFrameCount; }
        public long getBufferSizeBytes() { return bufferSizeBytes; }
        public void setBufferSizeBytes(long bufferSizeBytes) { this.bufferSizeBytes = bufferSizeBytes; }
    }
}
