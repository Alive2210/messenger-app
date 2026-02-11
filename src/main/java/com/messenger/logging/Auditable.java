package com.messenger.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация для аудит-логирования важных операций
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    /**
     * Описание действия для аудит-лога
     */
    String action();
    
    /**
     * Уровень важности
     */
    AuditLevel level() default AuditLevel.INFO;
    
    enum AuditLevel {
        INFO, WARN, CRITICAL
    }
}
