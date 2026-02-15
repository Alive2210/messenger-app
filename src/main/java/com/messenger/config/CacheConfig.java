package com.messenger.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Конфигурация кэширования с Redis
 */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    /**
     * Конфигурация кэш менеджера
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)) // TTL по умолчанию - 10 минут
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .withCacheConfiguration("userChats", 
                        config.entryTtl(Duration.ofMinutes(5))) // Чаты - 5 минут
                .withCacheConfiguration("chatMessages", 
                        config.entryTtl(Duration.ofMinutes(2))) // Сообщения - 2 минуты
                .withCacheConfiguration("userDevices", 
                        config.entryTtl(Duration.ofMinutes(10))) // Устройства - 10 минут
                .withCacheConfiguration("userProfile", 
                        config.entryTtl(Duration.ofHours(1))) // Профиль - 1 час
                .withCacheConfiguration("webrtcConfig", 
                        config.entryTtl(Duration.ofMinutes(30))) // WebRTC конфиг - 30 минут
                .build();
    }

    /**
     * Обработчик ошибок кэша - логирует но не падает
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }
}
