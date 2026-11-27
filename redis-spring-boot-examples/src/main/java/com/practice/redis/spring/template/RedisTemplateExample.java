package com.practice.redis.spring.template;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * The same Redis commands as the Jedis examples, written with Spring's {@link StringRedisTemplate}.
 *
 * <p>Differences from Jedis worth noticing:
 * <ul>
 *   <li>No pool to borrow from or close: the template gets a connection for each call and gives it back.</li>
 *   <li>Commands are grouped by data type: {@code opsForValue()} (strings), {@code opsForList()},
 *       {@code opsForHash()}, {@code opsForSet()}, {@code opsForZSet()}.</li>
 *   <li>Generic key commands (DEL, EXPIRE, TTL) are directly on the template.</li>
 * </ul>
 *
 * <p>{@code StringRedisTemplate} stores keys and values as plain strings, so everything you write
 * here is readable in Redis Insight. (A plain {@code RedisTemplate<Object, Object>} uses Java
 * serialization by default, which shows up as unreadable bytes.)
 */
@Component
public class RedisTemplateExample {

    private final StringRedisTemplate redis;

    /** Spring Boot creates the StringRedisTemplate bean; constructor injection hands it to us. */
    public RedisTemplateExample(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Runs every example in order, printing each command and its result. */
    public void runAll() {
        System.out.println("=== RedisTemplate tour ===");
        strings();
        counter();
        expiringKey();
        list();
        hash();
        sortedSet();
        System.out.println();
    }

    /** SET / GET with opsForValue(). */
    public String strings() {
        redis.delete("demo:spring:greeting");
        redis.opsForValue().set("demo:spring:greeting", "Hello from Spring");
        log("SET demo:spring:greeting \"Hello from Spring\"", "OK");
        return log("GET demo:spring:greeting", redis.opsForValue().get("demo:spring:greeting"));
    }

    /** INCR / INCRBY: atomic counters. increment() returns the new value. */
    public Long counter() {
        redis.delete("demo:spring:counter");
        log("INCR demo:spring:counter", redis.opsForValue().increment("demo:spring:counter"));
        return log("INCRBY demo:spring:counter 5", redis.opsForValue().increment("demo:spring:counter", 5));
    }

    /** SET ... EX: the Duration argument becomes the key's time-to-live. */
    public Long expiringKey() {
        redis.opsForValue().set("demo:spring:otp", "493817", Duration.ofSeconds(30));
        log("SET demo:spring:otp \"493817\" EX 30", "OK");
        return log("TTL demo:spring:otp", redis.getExpire("demo:spring:otp"));
    }

    /** RPUSH / LRANGE: a list used as a queue. */
    public List<String> list() {
        redis.delete("demo:spring:tasks");
        log("RPUSH demo:spring:tasks email resize upload",
                redis.opsForList().rightPushAll("demo:spring:tasks", "email", "resize", "upload"));
        log("LPOP demo:spring:tasks", redis.opsForList().leftPop("demo:spring:tasks"));
        return log("LRANGE demo:spring:tasks 0 -1", redis.opsForList().range("demo:spring:tasks", 0, -1));
    }

    /** HSET / HGETALL: a hash used as a small object. */
    public Map<Object, Object> hash() {
        Map<String, String> user = new LinkedHashMap<>();
        user.put("name", "Bob");
        user.put("city", "Berlin");
        redis.delete("demo:spring:user:2");
        redis.opsForHash().putAll("demo:spring:user:2", user);
        log("HSET demo:spring:user:2 name Bob city Berlin", "OK");
        log("HINCRBY demo:spring:user:2 logins 1", redis.opsForHash().increment("demo:spring:user:2", "logins", 1));
        return log("HGETALL demo:spring:user:2", redis.opsForHash().entries("demo:spring:user:2"));
    }

    /** ZADD / ZREVRANGE: a sorted set used as a leaderboard. */
    public Set<String> sortedSet() {
        redis.delete("demo:spring:scores");
        redis.opsForZSet().add("demo:spring:scores", "alice", 100);
        redis.opsForZSet().add("demo:spring:scores", "bob", 140);
        redis.opsForZSet().add("demo:spring:scores", "carol", 120);
        log("ZADD demo:spring:scores 100 alice 140 bob 120 carol", 3);
        return log("ZREVRANGE demo:spring:scores 0 1", redis.opsForZSet().reverseRange("demo:spring:scores", 0, 1));
    }

    /** Prints "redis> COMMAND -> result" and returns the result, like the Jedis module's CommandLogger. */
    private static <T> T log(String command, T result) {
        System.out.printf("redis> %-50s -> %s%n", command, result);
        return result;
    }
}
