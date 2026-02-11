package com.messenger.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class LoggingService {

    /**
     * Получить список всех логгеров
     */
    public List<LoggerInfo> getAllLoggers() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        List<LoggerInfo> loggers = new ArrayList<>();
        
        for (Logger logger : context.getLoggerList()) {
            loggers.add(LoggerInfo.builder()
                    .name(logger.getName())
                    .level(logger.getLevel() != null ? logger.getLevel().toString() : "INHERITED")
                    .effectiveLevel(logger.getEffectiveLevel().toString())
                    .build());
        }
        
        return loggers;
    }

    /**
     * Получить логгер по имени
     */
    public LoggerInfo getLogger(String loggerName) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = context.exists(loggerName);
        
        if (logger == null) {
            return null;
        }
        
        return LoggerInfo.builder()
                .name(logger.getName())
                .level(logger.getLevel() != null ? logger.getLevel().toString() : "INHERITED")
                .effectiveLevel(logger.getEffectiveLevel().toString())
                .build();
    }

    /**
     * Установить уровень логирования
     */
    public boolean setLogLevel(String loggerName, String level) {
        try {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            Logger logger = context.getLogger(loggerName);
            
            if (logger == null) {
                log.warn("Logger not found: {}", loggerName);
                return false;
            }
            
            Level newLevel = Level.toLevel(level, null);
            if (newLevel == null) {
                log.error("Invalid log level: {}", level);
                return false;
            }
            
            logger.setLevel(newLevel);
            log.info("Changed log level for '{}' to '{}'", loggerName, level);
            
            return true;
        } catch (Exception e) {
            log.error("Failed to set log level", e);
            return false;
        }
    }

    /**
     * Сбросить уровень логирования к значению по умолчанию
     */
    public boolean resetLogLevel(String loggerName) {
        try {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            Logger logger = context.getLogger(loggerName);
            
            if (logger == null) {
                log.warn("Logger not found: {}", loggerName);
                return false;
            }
            
            logger.setLevel(null); // Наследовать от родителя
            log.info("Reset log level for '{}' to inherited", loggerName);
            
            return true;
        } catch (Exception e) {
            log.error("Failed to reset log level", e);
            return false;
        }
    }

    /**
     * Получить предопределенные логгеры с описаниями
     */
    public List<LoggerDescription> getPredefinedLoggers() {
        List<LoggerDescription> loggers = new ArrayList<>();
        
        loggers.add(new LoggerDescription("ROOT", "Root logger for all application logs"));
        loggers.add(new LoggerDescription("com.messenger", "Application main logger"));
        loggers.add(new LoggerDescription("com.messenger.controller", "REST API controllers"));
        loggers.add(new LoggerDescription("com.messenger.service", "Business logic services"));
        loggers.add(new LoggerDescription("com.messenger.repository", "Database repositories"));
        loggers.add(new LoggerDescription("com.messenger.websocket", "WebSocket handlers"));
        loggers.add(new LoggerDescription("com.messenger.security", "Security components"));
        loggers.add(new LoggerDescription("AUDIT", "Audit events logger"));
        loggers.add(new LoggerDescription("SECURITY", "Security events logger"));
        loggers.add(new LoggerDescription("WEBSOCKET", "WebSocket events logger"));
        loggers.add(new LoggerDescription("PERFORMANCE", "Performance metrics logger"));
        loggers.add(new LoggerDescription("org.springframework.web", "Spring Web logs"));
        loggers.add(new LoggerDescription("org.springframework.security", "Spring Security logs"));
        loggers.add(new LoggerDescription("org.hibernate.SQL", "SQL queries logging"));
        
        return loggers;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LoggerInfo {
        private String name;
        private String level;
        private String effectiveLevel;
    }

    @Data
    @AllArgsConstructor
    public static class LoggerDescription {
        private String name;
        private String description;
    }
}
