package com.practice.redis.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Entry point for the Spring Boot examples.
 *
 * <p>Spring Boot sees spring-boot-starter-data-redis on the classpath and auto-configures:
 * <ul>
 *   <li>a Lettuce connection factory for localhost:6379 (see application.properties)</li>
 *   <li>{@code StringRedisTemplate} and {@code RedisTemplate} beans (template package)</li>
 *   <li>Redis repositories for {@code @RedisHash} classes (repository package)</li>
 *   <li>a Redis-backed CacheManager, because of {@code @EnableCaching} below (cache package)</li>
 *   <li>Spring Session, which stores every HttpSession in Redis (session package)</li>
 * </ul>
 *
 * <p>Run it with {@code mvn -pl redis-spring-boot-examples spring-boot:run}. The app listens on
 * http://localhost:8080 (try the curl commands in README.md). On startup, {@code StartupTour}
 * prints the RedisTemplate example.
 */
@SpringBootApplication
@EnableCaching // turns on @Cacheable / @CachePut / @CacheEvict
public class RedisSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(RedisSpringApplication.class, args);
    }
}
