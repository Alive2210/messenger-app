package com.messenger.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Менеджер переподключения WebSocket соединений
 * Автоматически переподключает клиентов при разрыве связи
 */
@Slf4j
@Component
public class WebSocketReconnectManager {

    // Максимальное время ожидания переподключения (20 секунд)
    private static final long MAX_RECONNECT_TIMEOUT_MS = 20000;
    
    // Интервал между попытками переподключения (начальный)
    private static final long INITIAL_RETRY_INTERVAL_MS = 1000;
    
    // Максимальный интервал между попытками
    private static final long MAX_RETRY_INTERVAL_MS = 5000;
    
    // Максимальное количество попыток
    private static final int MAX_RETRY_ATTEMPTS = 10;

    private final Map<String, ReconnectSession> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    /**
     * Регистрирует сессию для отслеживания
     */
    public void registerSession(String sessionId, String username, String deviceId, 
                                ReconnectCallback callback) {
        ReconnectSession session = new ReconnectSession();
        session.setSessionId(sessionId);
        session.setUsername(username);
        session.setDeviceId(deviceId);
        session.setCallback(callback);
        session.setConnected(true);
        session.setLastHeartbeat(Instant.now());
        
        sessions.put(sessionId, session);
        log.info("✅ Зарегистрирована сессия {} для пользователя {}", sessionId, username);
    }

    /**
     * Отмечает отключение сессии и запускает попытки переподключения
     */
    public void handleDisconnection(String sessionId, String reason) {
        ReconnectSession session = sessions.get(sessionId);
        if (session == null) {
            log.warn("⚠️ Попытка отключения неизвестной сессии: {}", sessionId);
            return;
        }

        if (session.isConnected()) {
            session.setConnected(false);
            session.setDisconnectTime(Instant.now());
            session.setDisconnectReason(reason);
            session.setReconnectAttempts(0);
            
            log.info("🔌 Сессия {} отключена: {}. Запуск переподключения...", 
                    sessionId, reason);
            
            // Запускаем попытки переподключения
            scheduleReconnect(session);
        }
    }

    /**
     * Подтверждает успешное переподключение
     */
    public void confirmReconnection(String sessionId, String newSessionId) {
        ReconnectSession session = sessions.get(sessionId);
        if (session == null) {
            return;
        }

        // Отменяем будущие попытки
        if (session.getScheduledFuture() != null) {
            session.getScheduledFuture().cancel(false);
        }

        session.setConnected(true);
        session.setReconnectAttempts(0);
        session.setSessionId(newSessionId); // Обновляем ID сессии
        session.setLastHeartbeat(Instant.now());
        
        log.info("✅ Сессия {} успешно переподключена как {}", sessionId, newSessionId);
        
        // Уведомляем callback
        if (session.getCallback() != null) {
            session.getCallback().onReconnected(sessionId, newSessionId);
        }
    }

    /**
     * Обновляет heartbeat для сессии
     */
    public void updateHeartbeat(String sessionId) {
        ReconnectSession session = sessions.get(sessionId);
        if (session != null) {
            session.setLastHeartbeat(Instant.now());
        }
    }

    /**
     * Удаляет сессию (при полном выходе)
     */
    public void removeSession(String sessionId) {
        ReconnectSession session = sessions.remove(sessionId);
        if (session != null && session.getScheduledFuture() != null) {
            session.getScheduledFuture().cancel(false);
        }
        log.info("🗑️ Сессия {} удалена", sessionId);
    }

    /**
     * Планирует попытку переподключения
     */
    private void scheduleReconnect(ReconnectSession session) {
        // Проверяем, не истек ли таймаут (20 секунд)
        long disconnectTime = Instant.now().toEpochMilli() - session.getDisconnectTime().toEpochMilli();
        if (disconnectTime > MAX_RECONNECT_TIMEOUT_MS) {
            log.warn("⏰ Таймаут переподключения для сессии {} (20 сек истекло)", 
                    session.getSessionId());
            handleReconnectFailure(session, "Таймаут (20 сек)");
            return;
        }

        // Проверяем количество попыток
        if (session.getReconnectAttempts() >= MAX_RETRY_ATTEMPTS) {
            log.error("❌ Исчерпаны попытки переподключения для сессии {}", 
                    session.getSessionId());
            handleReconnectFailure(session, "Исчерпаны попытки");
            return;
        }

        // Вычисляем интервал с экспоненциальным backoff
        long retryInterval = calculateRetryInterval(session.getReconnectAttempts());
        session.setReconnectAttempts(session.getReconnectAttempts() + 1);

        log.info("🔄 Попытка переподключения {}/{} для сессии {} через {} мс", 
                session.getReconnectAttempts(), MAX_RETRY_ATTEMPTS, 
                session.getSessionId(), retryInterval);

        // Запускаем задачу переподключения
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            attemptReconnect(session);
        }, retryInterval, TimeUnit.MILLISECONDS);

        session.setScheduledFuture(future);
    }

    /**
     * Выполняет попытку переподключения
     */
    private void attemptReconnect(ReconnectSession session) {
        if (session.isConnected()) {
            // Уже подключились
            return;
        }

        try {
            log.debug("🔄 Выполняется попытка переподключения для сессии {}", 
                    session.getSessionId());

            // Вызываем callback для попытки переподключения
            if (session.getCallback() != null) {
                boolean success = session.getCallback().attemptReconnect(
                        session.getSessionId(),
                        session.getUsername(),
                        session.getDeviceId()
                );

                if (success) {
                    // Переподключение будет подтверждено через confirmReconnection
                    return;
                }
            }

            // Если не удалось, планируем следующую попытку
            scheduleReconnect(session);

        } catch (Exception e) {
            log.error("❌ Ошибка при попытке переподключения сессии {}: {}", 
                    session.getSessionId(), e.getMessage());
            scheduleReconnect(session);
        }
    }

    /**
     * Обрабатывает неудачу переподключения
     */
    private void handleReconnectFailure(ReconnectSession session, String reason) {
        log.error("❌ Переподключение невозможно для сессии {}: {}", 
                session.getSessionId(), reason);
        
        if (session.getCallback() != null) {
            session.getCallback().onReconnectFailed(session.getSessionId(), reason);
        }
        
        sessions.remove(session.getSessionId());
    }

    /**
     * Вычисляет интервал с экспоненциальным backoff
     */
    private long calculateRetryInterval(int attempt) {
        long interval = INITIAL_RETRY_INTERVAL_MS * (long) Math.pow(2, attempt);
        return Math.min(interval, MAX_RETRY_INTERVAL_MS);
    }

    /**
     * Получает статус сессии
     */
    public ReconnectStatus getSessionStatus(String sessionId) {
        ReconnectSession session = sessions.get(sessionId);
        if (session == null) {
            return null;
        }

        ReconnectStatus status = new ReconnectStatus();
        status.setSessionId(sessionId);
        status.setConnected(session.isConnected());
        status.setReconnectAttempts(session.getReconnectAttempts());
        
        if (!session.isConnected() && session.getDisconnectTime() != null) {
            long elapsed = Instant.now().toEpochMilli() - session.getDisconnectTime().toEpochMilli();
            status.setTimeSinceDisconnectMs(elapsed);
            status.setRemainingTimeMs(Math.max(0, MAX_RECONNECT_TIMEOUT_MS - elapsed));
        }

        return status;
    }

    /**
     * Callback для переподключения
     */
    public interface ReconnectCallback {
        /**
         * Вызывается для попытки переподключения
         * @return true если переподключение начато
         */
        boolean attemptReconnect(String oldSessionId, String username, String deviceId);

        /**
         * Вызывается при успешном переподключении
         */
        void onReconnected(String oldSessionId, String newSessionId);

        /**
         * Вызывается при невозможности переподключения
         */
        void onReconnectFailed(String sessionId, String reason);
    }

    /**
     * Данные сессии переподключения
     */
    @Data
    private static class ReconnectSession {
        private String sessionId;
        private String username;
        private String deviceId;
        private ReconnectCallback callback;
        private volatile boolean connected;
        private Instant lastHeartbeat;
        private Instant disconnectTime;
        private String disconnectReason;
        private int reconnectAttempts;
        private ScheduledFuture<?> scheduledFuture;
    }

    /**
     * Статус переподключения для API
     */
    @Data
    public static class ReconnectStatus {
        private String sessionId;
        private boolean connected;
        private int reconnectAttempts;
        private long timeSinceDisconnectMs;
        private long remainingTimeMs;
    }
}
