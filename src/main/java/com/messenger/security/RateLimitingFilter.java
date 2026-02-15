package com.messenger.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate Limiting Filter - защита от DDoS и brute force атак
 * Ограничивает количество запросов с одного IP адреса
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // После MDC фильтра
public class RateLimitingFilter extends OncePerRequestFilter {

    // Конфигурация лимитов
    private static final int DEFAULT_MAX_REQUESTS = 100; // запросов
    private static final Duration DEFAULT_WINDOW = Duration.ofMinutes(1); // в минуту
    private static final int AUTH_MAX_REQUESTS = 10; // для auth endpoints
    private static final Duration AUTH_WINDOW = Duration.ofMinutes(1);
    private static final int BLOCK_DURATION_MINUTES = 15; // время блокировки

    // Хранилище счетчиков запросов по IP
    private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();
    
    // Список заблокированных IP
    private final Map<String, Instant> blockedIps = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String clientIp = getClientIP(request);
        String path = request.getRequestURI();
        
        // Проверяем, не заблокирован ли IP
        if (isBlocked(clientIp)) {
            log.warn("🚫 Blocked IP {} attempted access to {}", clientIp, path);
            sendErrorResponse(response, HttpStatus.FORBIDDEN, 
                    "Your IP has been temporarily blocked due to too many requests. " +
                    "Please try again in " + BLOCK_DURATION_MINUTES + " minutes.");
            return;
        }
        
        // Определяем лимиты в зависимости от endpoint
        int maxRequests = isAuthEndpoint(path) ? AUTH_MAX_REQUESTS : DEFAULT_MAX_REQUESTS;
        Duration window = isAuthEndpoint(path) ? AUTH_WINDOW : DEFAULT_WINDOW;
        
        // Проверяем лимит
        if (isRateLimitExceeded(clientIp, maxRequests, window)) {
            log.warn("⚠️ Rate limit exceeded for IP {} on endpoint {}", clientIp, path);
            
            // Блокируем IP если слишком много нарушений
            blockIp(clientIp);
            
            sendErrorResponse(response, HttpStatus.TOO_MANY_REQUESTS, 
                    "Too many requests. Please slow down. " +
                    "Your IP has been temporarily blocked.");
            return;
        }
        
        // Добавляем заголовки с информацией о лимите
        addRateLimitHeaders(response, clientIp, maxRequests);
        
        filterChain.doFilter(request, response);
    }

    /**
     * Проверяет, не превышен ли лимит запросов
     */
    private boolean isRateLimitExceeded(String clientIp, int maxRequests, Duration window) {
        Instant now = Instant.now();
        RequestCounter counter = requestCounts.computeIfAbsent(clientIp, 
                k -> new RequestCounter(now));
        
        synchronized (counter) {
            // Проверяем, не истекло ли окно
            if (Duration.between(counter.getWindowStart(), now).compareTo(window) > 0) {
                // Сбрасываем счетчик
                counter.reset(now);
            }
            
            // Увеличиваем счетчик
            int currentCount = counter.increment();
            
            // Проверяем лимит
            if (currentCount > maxRequests) {
                counter.incrementViolation();
                return true;
            }
        }
        
        return false;
    }

    /**
     * Проверяет, заблокирован ли IP
     */
    private boolean isBlocked(String clientIp) {
        Instant blockTime = blockedIps.get(clientIp);
        if (blockTime == null) {
            return false;
        }
        
        // Проверяем, не истекла ли блокировка
        if (Duration.between(blockTime, Instant.now()).toMinutes() > BLOCK_DURATION_MINUTES) {
            blockedIps.remove(clientIp);
            return false;
        }
        
        return true;
    }

    /**
     * Блокирует IP адрес
     */
    private void blockIp(String clientIp) {
        blockedIps.put(clientIp, Instant.now());
        requestCounts.remove(clientIp); // Очищаем счетчик
        log.warn("🔒 IP {} has been blocked for {} minutes due to rate limit violations", 
                clientIp, BLOCK_DURATION_MINUTES);
    }

    /**
     * Проверяет, является ли endpoint аутентификационным
     */
    private boolean isAuthEndpoint(String path) {
        return path.contains("/api/auth/") || 
               path.contains("/api/login") || 
               path.contains("/api/register");
    }

    /**
     * Добавляет заголовки с информацией о лимите
     */
    private void addRateLimitHeaders(HttpServletResponse response, String clientIp, int maxRequests) {
        RequestCounter counter = requestCounts.get(clientIp);
        if (counter != null) {
            int remaining = Math.max(0, maxRequests - counter.getCount());
            response.setHeader("X-RateLimit-Limit", String.valueOf(maxRequests));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
            response.setHeader("X-RateLimit-Window", "60");
        }
    }

    /**
     * Отправляет JSON ответ с ошибкой
     */
    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) 
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(String.format(
                "{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                status.value(),
                status.getReasonPhrase(),
                message,
                java.time.LocalDateTime.now()
        ));
    }

    /**
     * Получает реальный IP клиента (учитывая прокси)
     */
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Класс для хранения счетчика запросов
     */
    private static class RequestCounter {
        private Instant windowStart;
        private AtomicInteger count;
        private AtomicInteger violations;

        public RequestCounter(Instant windowStart) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(0);
            this.violations = new AtomicInteger(0);
        }

        public void reset(Instant newWindowStart) {
            this.windowStart = newWindowStart;
            this.count.set(0);
        }

        public int increment() {
            return count.incrementAndGet();
        }

        public void incrementViolation() {
            violations.incrementAndGet();
        }

        public Instant getWindowStart() {
            return windowStart;
        }

        public int getCount() {
            return count.get();
        }

        public int getViolations() {
            return violations.get();
        }
    }
}
