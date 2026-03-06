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
    private static final int DEFAULT_MAX_REQUESTS = 10000; // запросов
    private static final Duration DEFAULT_WINDOW = Duration.ofMinutes(1); // в минуту
    private static final int AUTH_MAX_REQUESTS = 1000; // для auth endpoints
    private static final Duration AUTH_WINDOW = Duration.ofMinutes(1);
    private static final int BLOCK_DURATION_MINUTES = 1; // время блокировки

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
        String method = request.getMethod();
        
        // ИСКЛЮЧЕНИЯ - не проверяем rate limit для:
        // 1. Health checks и actuator
        // 2. Preflight (OPTIONS) запросов
        // 3. Статических ресурсов
        // 4. WebSocket соединений
        // 5. Локальных IP
        if (isExcluded(path, method)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Разрешаем localhost без ограничений
        if ("0:0:0:0:0:0:0:1".equals(clientIp) || "127.0.0.1".equals(clientIp) || 
            "localhost".equals(clientIp) || clientIp.startsWith("192.168.") ||
            clientIp.startsWith("10.") || clientIp.startsWith("172.16.") || clientIp.startsWith("172.17.") || 
            clientIp.startsWith("172.18.") || clientIp.startsWith("172.19.") || clientIp.startsWith("172.20.") ||
            clientIp.startsWith("172.30.") || clientIp.startsWith("172.31.")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Проверяем, не заблокирован ли IP - НЕ БЛОКИРУЕМ, просто ограничиваем
        if (isBlocked(clientIp)) {
            // Не блокируем, а просто замедляем
            log.debug("Rate limited IP {} - allowing with warning", clientIp);
        }
        
        // Определяем лимиты в зависимости от endpoint
        int maxRequests = isAuthEndpoint(path) ? AUTH_MAX_REQUESTS : DEFAULT_MAX_REQUESTS;
        Duration window = isAuthEndpoint(path) ? AUTH_WINDOW : DEFAULT_WINDOW;
        
        // Проверяем лимит
        if (isRateLimitExceeded(clientIp, maxRequests, window)) {
            log.warn("Rate limit exceeded for IP {} on endpoint {} (limit: {}/{}s)", 
                clientIp, path, maxRequests, window.getSeconds());
            
            // НЕ БЛОКИРУЕМ IP - только возвращаем 429
            // addRateLimitHeaders(response, clientIp, maxRequests); // REMOVED - causes issues
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too Many Requests\",\"message\":\"Please try again later\",\"retryAfter\":60}");
            return;
        }
        
        // Добавляем заголовки с информацией о лимите
        addRateLimitHeaders(response, clientIp, maxRequests);
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Проверяет, нужно ли исключить запрос из rate limiting
     */
    private boolean isExcluded(String path, String method) {
        // Preflight OPTIONS - всегда разрешаем
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }
        
        // Health checks
        if (path.startsWith("/actuator/health") || path.equals("/health") || path.equals("/actuator/info")) {
            return true;
        }
        
        // Статические ресурсы
        if (path.startsWith("/static/") || path.startsWith("/assets/") || 
            path.startsWith("/images/") || path.startsWith("/css/") || 
            path.startsWith("/js/") || path.endsWith(".html") || 
            path.endsWith(".css") || path.endsWith(".js") || path.endsWith(".png") ||
            path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".gif") ||
            path.endsWith(".ico") || path.endsWith(".svg") || path.endsWith(".woff") ||
            path.endsWith(".woff2") || path.endsWith(".ttf")) {
            return true;
        }
        
        // WebSocket
        if (path.startsWith("/ws") || path.startsWith("/websocket")) {
            return true;
        }
        
        return false;
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
