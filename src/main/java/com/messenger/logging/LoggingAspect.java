package com.messenger.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    /**
     * Pointcut для всех контроллеров
     */
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void restController() {}

    /**
     * Pointcut для всех сервисов
     */
    @Pointcut("within(@org.springframework.stereotype.Service *)")
    public void service() {}

    /**
     * Pointcut для репозиториев
     */
    @Pointcut("within(@org.springframework.stereotype.Repository *)")
    public void repository() {}

    /**
     * Pointcut для WebSocket контроллеров
     */
    @Pointcut("within(com.messenger.controller.WebSocketController)")
    public void websocketController() {}

    /**
     * Логирование входа в метод контроллера
     */
    @Before("restController() && !loggingExcluded()")
    public void logControllerEntry(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String user = getCurrentUser();
        
        log.debug("[API] User: {} | {}.{}() called", user, className, methodName);
    }

    /**
     * Логирование выхода из метода контроллера
     */
    @AfterReturning(pointcut = "restController() && !loggingExcluded()", returning = "result")
    public void logControllerExit(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        log.debug("[API] {}.{}() completed successfully", className, methodName);
    }

    /**
     * Логирование исключений в контроллерах
     */
    @AfterThrowing(pointcut = "restController() || service()", throwing = "exception")
    public void logException(JoinPoint joinPoint, Exception exception) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String user = getCurrentUser();
        
        log.error("[ERROR] User: {} | {}.{}() threw exception: {}", 
                user, className, methodName, exception.getMessage(), exception);
    }

    /**
     * Логирование времени выполнения сервисных методов
     */
    @Around("service() && execution(* com.messenger.service.*.*(..))")
    public Object logServiceExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            
            // Логируем PERFORMANCE для медленных операций (> 1 сек)
            if (duration > 1000) {
                org.slf4j.Logger perfLog = org.slf4j.LoggerFactory.getLogger("PERFORMANCE");
                perfLog.warn("Slow operation: {}.{}() took {}ms", className, methodName, duration);
            }
            
            log.debug("[SERVICE] {}.{}() executed in {}ms", className, methodName, duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[SERVICE] {}.{}() failed after {}ms: {}", 
                    className, methodName, duration, e.getMessage());
            throw e;
        }
    }

    /**
     * Логирование WebSocket операций
     */
    @Around("websocketController()")
    public Object logWebSocketOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        org.slf4j.Logger wsLog = org.slf4j.LoggerFactory.getLogger("WEBSOCKET");
        
        wsLog.debug("WebSocket operation: {} started", methodName);
        
        try {
            Object result = joinPoint.proceed();
            wsLog.debug("WebSocket operation: {} completed", methodName);
            return result;
        } catch (Exception e) {
            wsLog.error("WebSocket operation: {} failed: {}", methodName, e.getMessage());
            throw e;
        }
    }

    /**
     * Аудит логирование для важных операций
     */
    @AfterReturning("@annotation(com.messenger.logging.Auditable)")
    public void auditLog(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Auditable auditable = signature.getMethod().getAnnotation(Auditable.class);
        String user = getCurrentUser();
        String action = auditable.action();
        
        org.slf4j.Logger auditLog = org.slf4j.LoggerFactory.getLogger("AUDIT");
        auditLog.info("AUDIT: User '{}' performed action '{}' in {}.{}()",
                user, action, 
                joinPoint.getTarget().getClass().getSimpleName(),
                signature.getName());
    }

    /**
     * Получить текущего пользователя
     */
    private String getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "anonymous";
    }

    /**
     * Исключить из логирования (для health check и т.д.)
     */
    @Pointcut("execution(* org.springframework.boot.actuate..*.*(..))")
    public void loggingExcluded() {}
}
