package com.messenger.controller;

import com.messenger.service.LoggingService;
import com.messenger.service.LoggingService.LoggerInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class LoggingController {

    private final LoggingService loggingService;

    /**
     * Получить список всех логгеров
     */
    @GetMapping
    public ResponseEntity<List<LoggerInfo>> getAllLoggers() {
        log.debug("Fetching all loggers");
        return ResponseEntity.ok(loggingService.getAllLoggers());
    }

    /**
     * Получить информацию о конкретном логгере
     */
    @GetMapping("/{loggerName}")
    public ResponseEntity<LoggerInfo> getLogger(@PathVariable String loggerName) {
        log.debug("Fetching logger: {}", loggerName);
        LoggerInfo logger = loggingService.getLogger(loggerName);
        if (logger == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(logger);
    }

    /**
     * Установить уровень логирования
     */
    @PostMapping("/{loggerName}")
    public ResponseEntity<Map<String, Object>> setLogLevel(
            @PathVariable String loggerName,
            @RequestBody Map<String, String> request) {
        
        String level = request.get("level");
        log.info("Setting log level for '{}' to '{}'", loggerName, level);
        
        boolean success = loggingService.setLogLevel(loggerName, level);
        
        Map<String, Object> response = new HashMap<>();
        response.put("logger", loggerName);
        response.put("level", level);
        response.put("success", success);
        
        if (success) {
            log.info("Successfully changed log level for '{}' to '{}'", loggerName, level);
            return ResponseEntity.ok(response);
        } else {
            log.error("Failed to change log level for '{}' to '{}'", loggerName, level);
            response.put("error", "Failed to change log level");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Сбросить уровень логирования
     */
    @DeleteMapping("/{loggerName}")
    public ResponseEntity<Map<String, Object>> resetLogLevel(@PathVariable String loggerName) {
        log.info("Resetting log level for '{}'", loggerName);
        
        boolean success = loggingService.resetLogLevel(loggerName);
        
        Map<String, Object> response = new HashMap<>();
        response.put("logger", loggerName);
        response.put("success", success);
        
        if (success) {
            return ResponseEntity.ok(response);
        } else {
            response.put("error", "Failed to reset log level");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Получить предопределенные логгеры
     */
    @GetMapping("/predefined")
    public ResponseEntity<?> getPredefinedLoggers() {
        return ResponseEntity.ok(loggingService.getPredefinedLoggers());
    }

    /**
     * Получить доступные уровни логирования
     */
    @GetMapping("/levels")
    public ResponseEntity<List<String>> getLogLevels() {
        return ResponseEntity.ok(List.of("TRACE", "DEBUG", "INFO", "WARN", "ERROR", "OFF"));
    }
}
