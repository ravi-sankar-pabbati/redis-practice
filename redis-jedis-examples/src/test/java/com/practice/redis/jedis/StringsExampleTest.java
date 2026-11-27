package com.practice.redis.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class StringsExampleTest extends RedisTestSupport {

    @Test
    void stringsCommandsBehaveAsExpected() {
        StringsExample example = new StringsExample(jedis);

        // SET + APPEND + GET
        assertEquals("Hello, Redis!", example.setAndGet());

        // INCR -> 1, INCRBY 10 -> 11, DECR -> 10
        assertEquals(10L, example.counter());

        // The key was set with EX 60, so its TTL is somewhere in (0, 60]
        long ttl = example.expiringKey();
        assertTrue(ttl > 0 && ttl <= 60, "TTL should be between 1 and 60 but was " + ttl);

        // MGET returns null for keys that do not exist
        assertEquals(Arrays.asList("red", "L", null), example.multipleKeys());
    }
}
