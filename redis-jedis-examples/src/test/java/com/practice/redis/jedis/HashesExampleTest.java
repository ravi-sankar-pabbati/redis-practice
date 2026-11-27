package com.practice.redis.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;
import org.junit.jupiter.api.Test;

class HashesExampleTest extends RedisTestSupport {

    @Test
    void hashesStoreAndUpdateFields() {
        HashesExample example = new HashesExample(jedis);

        assertEquals(Map.of("name", "Alice", "email", "alice@example.com", "age", "30"),
                example.storeObject());

        // HINCRBY age 1 -> 31, and HDEL removed the email field
        assertEquals(31L, example.updateFields());
        assertFalse(jedis.hexists("demo:user:1", "email"));
        assertEquals("Alice", jedis.hget("demo:user:1", "name"));
    }
}
