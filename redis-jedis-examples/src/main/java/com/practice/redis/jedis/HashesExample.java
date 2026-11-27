package com.practice.redis.jedis;

import static com.practice.redis.jedis.CommandLogger.log;
import static com.practice.redis.jedis.CommandLogger.section;

import java.util.LinkedHashMap;
import java.util.Map;
import redis.clients.jedis.Jedis;

/**
 * HASHES - one key holding a map of field -> value pairs, like a small object or a row.
 *
 * <p>Typical uses: user profiles, product details, settings. You can read or update
 * one field without loading the whole object.
 */
public class HashesExample {

    private final Jedis jedis;

    public HashesExample(Jedis jedis) {
        this.jedis = jedis;
    }

    public void run() {
        section("Hashes");
        storeObject();
        updateFields();
    }

    /** HSET writes several fields at once; HGET reads one; HGETALL reads them all. */
    public Map<String, String> storeObject() {
        log("DEL demo:user:1", jedis.del("demo:user:1"));

        Map<String, String> user = new LinkedHashMap<>();
        user.put("name", "Alice");
        user.put("email", "alice@example.com");
        user.put("age", "30");
        log("HSET demo:user:1 name Alice email alice@example.com age 30",
                jedis.hset("demo:user:1", user));  // returns number of NEW fields added

        log("HGET demo:user:1 name", jedis.hget("demo:user:1", "name"));
        return log("HGETALL demo:user:1", jedis.hgetAll("demo:user:1"));
    }

    /** Change individual fields: HINCRBY for numbers, HDEL to remove a field. */
    public long updateFields() {
        storeObjectQuietly();
        long age = log("HINCRBY demo:user:1 age 1", jedis.hincrBy("demo:user:1", "age", 1));
        log("HEXISTS demo:user:1 email", jedis.hexists("demo:user:1", "email"));
        log("HDEL demo:user:1 email", jedis.hdel("demo:user:1", "email"));
        log("HKEYS demo:user:1", jedis.hkeys("demo:user:1"));
        log("HLEN demo:user:1", jedis.hlen("demo:user:1"));
        return age;
    }

    /** Resets the user hash without printing, so updateFields() always starts from age 30. */
    private void storeObjectQuietly() {
        jedis.del("demo:user:1");
        jedis.hset("demo:user:1", Map.of("name", "Alice", "email", "alice@example.com", "age", "30"));
    }
}
