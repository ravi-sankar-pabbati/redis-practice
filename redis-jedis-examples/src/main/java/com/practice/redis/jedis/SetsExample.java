package com.practice.redis.jedis;

import static com.practice.redis.jedis.CommandLogger.log;
import static com.practice.redis.jedis.CommandLogger.note;
import static com.practice.redis.jedis.CommandLogger.section;

import java.util.Set;
import redis.clients.jedis.Jedis;

/**
 * SETS - an unordered collection of UNIQUE strings. Adding a duplicate does nothing.
 *
 * <p>Typical uses: tags, unique visitors, "who liked this", and comparing groups
 * (intersection, union, difference).
 */
public class SetsExample {

    private final Jedis jedis;

    public SetsExample(Jedis jedis) {
        this.jedis = jedis;
    }

    public void run() {
        section("Sets");
        basics();
        setOperations();
    }

    /** SADD ignores duplicates; SISMEMBER checks membership quickly. */
    public Set<String> basics() {
        log("DEL demo:tags", jedis.del("demo:tags"));
        note("'java' is passed twice, but a set stores it only once");
        log("SADD demo:tags java redis java docker", jedis.sadd("demo:tags", "java", "redis", "java", "docker"));
        log("SISMEMBER demo:tags redis", jedis.sismember("demo:tags", "redis"));
        log("SISMEMBER demo:tags python", jedis.sismember("demo:tags", "python"));
        log("SCARD demo:tags", jedis.scard("demo:tags"));  // set size
        return log("SMEMBERS demo:tags", jedis.smembers("demo:tags"));  // order is not guaranteed
    }

    /** Compare two sets: what they share, what either has, and what only one has. */
    public Set<String> setOperations() {
        log("DEL demo:skills:alice demo:skills:bob", jedis.del("demo:skills:alice", "demo:skills:bob"));
        log("SADD demo:skills:alice java redis docker", jedis.sadd("demo:skills:alice", "java", "redis", "docker"));
        log("SADD demo:skills:bob python redis docker", jedis.sadd("demo:skills:bob", "python", "redis", "docker"));

        Set<String> shared = log("SINTER demo:skills:alice demo:skills:bob",
                jedis.sinter("demo:skills:alice", "demo:skills:bob"));
        log("SUNION demo:skills:alice demo:skills:bob", jedis.sunion("demo:skills:alice", "demo:skills:bob"));
        log("SDIFF demo:skills:alice demo:skills:bob", jedis.sdiff("demo:skills:alice", "demo:skills:bob"));
        return shared;
    }
}
