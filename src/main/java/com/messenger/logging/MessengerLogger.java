package com.messenger.logging;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Утилитный класс для структурированного логирования
 */
@Slf4j
public class MessengerLogger {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("AUDIT");
    private static final Logger SECURITY_LOG = LoggerFactory.getLogger("SECURITY");
    private static final Logger WEBSOCKET_LOG = LoggerFactory.getLogger("WEBSOCKET");
    private static final Logger PERFORMANCE_LOG = LoggerFactory.getLogger("PERFORMANCE");

    // ==================== Audit Logging ====================

    public static void audit(String action, String userId, String details) {
        AUDIT_LOG.info("[AUDIT] Action: {} | User: {} | Details: {}", action, userId, details);
    }

    public static void auditLogin(String userId, String ipAddress, boolean success) {
        if (success) {
            AUDIT_LOG.info("[AUDIT] LOGIN_SUCCESS | User: {} | IP: {}", userId, ipAddress);
        } else {
            AUDIT_LOG.warn("[AUDIT] LOGIN_FAILURE | User: {} | IP: {}", userId, ipAddress);
        }
    }

    public static void auditLogout(String userId, String ipAddress) {
        AUDIT_LOG.info("[AUDIT] LOGOUT | User: {} | IP: {}", userId, ipAddress);
    }

    public static void auditMessageSent(String messageId, String senderId, String chatId) {
        AUDIT_LOG.info("[AUDIT] MESSAGE_SENT | Message: {} | Sender: {} | Chat: {}", 
                messageId, senderId, chatId);
    }

    public static void auditFileUploaded(String fileId, String userId, long fileSize) {
        AUDIT_LOG.info("[AUDIT] FILE_UPLOADED | File: {} | User: {} | Size: {} bytes", 
                fileId, userId, fileSize);
    }

    // ==================== Security Logging ====================

    public static void securityAuthFailure(String username, String reason, String ipAddress) {
        SECURITY_LOG.warn("[SECURITY] AUTH_FAILURE | User: {} | Reason: {} | IP: {}", 
                username, reason, ipAddress);
    }

    public static void securityAccessDenied(String userId, String resource, String ipAddress) {
        SECURITY_LOG.warn("[SECURITY] ACCESS_DENIED | User: {} | Resource: {} | IP: {}", 
                userId, resource, ipAddress);
    }

    public static void securityTokenInvalid(String token, String reason) {
        SECURITY_LOG.warn("[SECURITY] INVALID_TOKEN | Reason: {}", reason);
    }

    public static void securitySuspiciousActivity(String userId, String activity, String ipAddress) {
        SECURITY_LOG.error("[SECURITY] SUSPICIOUS_ACTIVITY | User: {} | Activity: {} | IP: {}", 
                userId, activity, ipAddress);
    }

    // ==================== WebSocket Logging ====================

    public static void wsConnection(String userId, String sessionId) {
        WEBSOCKET_LOG.info("[WS] CONNECTED | User: {} | Session: {}", userId, sessionId);
    }

    public static void wsDisconnection(String userId, String sessionId, String reason) {
        WEBSOCKET_LOG.info("[WS] DISCONNECTED | User: {} | Session: {} | Reason: {}", 
                userId, sessionId, reason);
    }

    public static void wsMessageReceived(String userId, String destination, String messageType) {
        WEBSOCKET_LOG.debug("[WS] MESSAGE_RECEIVED | User: {} | Dest: {} | Type: {}", 
                userId, destination, messageType);
    }

    public static void wsSignalSent(String fromUser, String toUser, String signalType) {
        WEBSOCKET_LOG.debug("[WS] SIGNAL | From: {} | To: {} | Type: {}", 
                fromUser, toUser, signalType);
    }

    // ==================== Performance Logging ====================

    public static void perfDatabaseQuery(String query, long durationMs) {
        if (durationMs > 1000) {
            PERFORMANCE_LOG.warn("[PERF] SLOW_QUERY | Time: {}ms | Query: {}", durationMs, query);
        } else {
            PERFORMANCE_LOG.debug("[PERF] QUERY | Time: {}ms | Query: {}", durationMs, query);
        }
    }

    public static void perfApiCall(String endpoint, String method, long durationMs, int statusCode) {
        if (durationMs > 5000) {
            PERFORMANCE_LOG.warn("[PERF] SLOW_API | {} {} | Status: {} | Time: {}ms", 
                    method, endpoint, statusCode, durationMs);
        } else {
            PERFORMANCE_LOG.debug("[PERF] API | {} {} | Status: {} | Time: {}ms", 
                    method, endpoint, statusCode, durationMs);
        }
    }

    public static void perfWebSocketOperation(String operation, long durationMs) {
        if (durationMs > 100) {
            PERFORMANCE_LOG.warn("[PERF] SLOW_WS | Operation: {} | Time: {}ms", operation, durationMs);
        } else {
            PERFORMANCE_LOG.debug("[PERF] WS | Operation: {} | Time: {}ms", operation, durationMs);
        }
    }

    // ==================== Application Logging ====================

    public static void appInfo(String component, String message) {
        log.info("[APP:{}] {}", component, message);
    }

    public static void appWarn(String component, String message) {
        log.warn("[APP:{}] {}", component, message);
    }

    public static void appError(String component, String message, Throwable throwable) {
        log.error("[APP:{}] {}", component, message, throwable);
    }

    public static void appDebug(String component, String message) {
        log.debug("[APP:{}] {}", component, message);
    }
}
