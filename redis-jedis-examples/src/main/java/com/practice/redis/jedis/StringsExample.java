package com.practice.redis.jedis;

import static com.practice.redis.jedis.CommandLogger.log;
import static com.practice.redis.jedis.CommandLogger.note;
import static com.practice.redis.jedis.CommandLogger.section;

import java.util.List;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

/**
 * STRINGS - the simplest Redis type: one key holds one value (text, a number, or bytes).
 *
 * <p>Typical uses: caching a value, counters, feature flags, short-lived tokens.
 *
 * <p>Each method first deletes its keys so it gives the same result every time it runs.
 */
public class StringsExample {

    private final Jedis jedis;

    public StringsExample(Jedis jedis) {
        this.jedis = jedis;
    }

    public void run() {
        section("Strings");
        setAndGet();
        counter();
        expiringKey();
        multipleKeys();
    }

    /** SET stores a value, APPEND adds to it, GET reads it back. */
    public String setAndGet() {
        log("DEL demo:greeting", jedis.del("demo:greeting"));
        log("SET demo:greeting \"Hello\"", jedis.set("demo:greeting", "Hello"));
        log("APPEND demo:greeting \", Redis!\"", jedis.append("demo:greeting", ", Redis!")); // returns new length
        log("GET demo:missing", jedis.get("demo:missing"));                                 // missing key -> null
        return log("GET demo:greeting", jedis.get("demo:greeting"));
    }

    /**
     * INCR/DECR treat the string as an integer. They are atomic, so many clients
     * can update the same counter at once without losing updates.
     */
    public long counter() {
        log("DEL demo:page:views", jedis.del("demo:page:views"));
        log("INCR demo:page:views", jedis.incr("demo:page:views"));          // missing key starts at 0 -> 1
        log("INCRBY demo:page:views 10", jedis.incrBy("demo:page:views", 10)); // 11
        return log("DECR demo:page:views", jedis.decr("demo:page:views"));    // 10
    }

    /** SET ... EX gives a key a time-to-live; Redis deletes it automatically when it expires. */
    public long expiringKey() {
        log("SET demo:session:abc \"user-42\" EX 60",
                jedis.set("demo:session:abc", "user-42", SetParams.setParams().ex(60)));
        note("TTL = seconds left before Redis deletes the key (-1 = no expiry, -2 = key missing)");
        return log("TTL demo:session:abc", jedis.ttl("demo:session:abc"));
    }

    /** MSET / MGET read or write several keys in a single round trip. */
    public List<String> multipleKeys() {
        log("MSET demo:color red demo:size L",
                jedis.mset("demo:color", "red", "demo:size", "L"));
        return log("MGET demo:color demo:size demo:missing",
                jedis.mget("demo:color", "demo:size", "demo:missing"));
    }
}
