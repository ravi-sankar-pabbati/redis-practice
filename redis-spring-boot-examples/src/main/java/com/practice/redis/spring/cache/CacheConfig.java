package com.practice.redis.spring.cache;

import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

/**
 * Adjusts the Redis cache that Spring Boot auto-configures. TTL and key prefix still come from
 * application.properties ({@code spring.cache.redis.*}); this class changes two other things.
 */
@Configuration
public class CacheConfig {

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheCustomizer(RedisConnectionFactory connectionFactory) {
        return builder -> builder
                // 1. Write cache entries immediately.
                //    With Lettuce, Spring Data Redis 4 sends cache writes asynchronously by default:
                //    the method returns before Redis has stored the value. That's faster, but a read
                //    right after a @CachePut can still see the old value. For a learning project,
                //    predictable beats a few saved microseconds.
                .cacheWriter(RedisCacheWriter.create(connectionFactory, writer -> writer.immediateWrites()))

                // 2. Store the "rates" cache as JSON.
                //    By default values use Java serialization, which shows up in Redis Insight as
                //    unreadable bytes and breaks when the class changes. cacheDefaults() is the config
                //    Boot built from spring.cache.redis.*; keep it and only swap the value serializer.
                .withCacheConfiguration(ExchangeRateService.CACHE,
                        builder.cacheDefaults().serializeValuesWith(
                                SerializationPair.fromSerializer(new JacksonJsonRedisSerializer<>(ExchangeRate.class))));
    }
}
