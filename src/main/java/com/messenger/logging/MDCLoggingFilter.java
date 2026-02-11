package com.messenger.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Фильтр для добавления контекста в логи через MDC
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MDCLoggingFilter extends OncePerRequestFilter {

    private static final String TRACE_ID = "traceId";
    private static final String USER_ID = "userId";
    private static final String REQUEST_URI = "requestUri";
    private static final String REQUEST_METHOD = "requestMethod";
    private static final String CLIENT_IP = "clientIp";

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // Генерируем traceId
            String traceId = UUID.randomUUID().toString();
            MDC.put(TRACE_ID, traceId);
            
            // Добавляем в заголовок ответа для трейсинга
            response.setHeader("X-Trace-Id", traceId);
            
            // Получаем пользователя
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                MDC.put(USER_ID, authentication.getName());
            } else {
                MDC.put(USER_ID, "anonymous");
            }
            
            // Информация о запросе
            MDC.put(REQUEST_URI, request.getRequestURI());
            MDC.put(REQUEST_METHOD, request.getMethod());
            MDC.put(CLIENT_IP, getClientIP(request));
            
            filterChain.doFilter(request, response);
        } finally {
            // Очищаем MDC после запроса
            MDC.clear();
        }
    }

    /**
     * Получить IP клиента
     */
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    /**
     * Получить текущий traceId
     */
    public static String getCurrentTraceId() {
        return MDC.get(TRACE_ID);
    }

    /**
     * Получить текущего пользователя
     */
    public static String getCurrentUserId() {
        return MDC.get(USER_ID);
    }
}
