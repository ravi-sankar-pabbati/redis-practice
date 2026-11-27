package com.practice.redis.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.junit.jupiter.api.Test;

class SetsExampleTest extends RedisTestSupport {

    @Test
    void setsKeepUniqueMembersAndSupportSetOperations() {
        SetsExample example = new SetsExample(jedis);

        // "java" was added twice but is stored once (compare as Sets: order is not guaranteed)
        assertEquals(Set.of("java", "redis", "docker"), example.basics());

        // Skills Alice and Bob have in common
        assertEquals(Set.of("redis", "docker"), example.setOperations());
        assertEquals(Set.of("java"), jedis.sdiff("demo:skills:alice", "demo:skills:bob"));
    }
}
