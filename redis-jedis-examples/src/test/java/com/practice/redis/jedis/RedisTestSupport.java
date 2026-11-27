package com.practice.redis.jedis;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 * Shared setup for tests that talk to the local Redis (started with docker compose).
 *
 * <ul>
 *   <li>Tests use database 15, so they never touch the example data in database 0.</li>
 *   <li>The test database is emptied (FLUSHDB) before every test, so tests are independent.</li>
 *   <li>If Redis is not running, the tests are SKIPPED with a hint instead of failing.</li>
 * </ul>
 */
abstract class RedisTestSupport {

    private static final int TEST_DATABASE = 15;

    protected static JedisPool pool;
    protected Jedis jedis;

    @BeforeAll
    static void connect() {
        pool = RedisConnection.createPool(TEST_DATABASE);
        Assumptions.assumeTrue(isRedisRunning(),
                "Redis is not reachable on localhost:6379 - start it with: docker compose up -d");
    }

    @BeforeEach
    void openConnectionAndClearDatabase() {
        jedis = pool.getResource();
        jedis.flushDB();  // only clears database 15
    }

    @AfterEach
    void returnConnection() {
        if (jedis != null) {
            jedis.close();
        }
    }

    @AfterAll
    static void closePool() {
        pool.close();
    }

    private static boolean isRedisRunning() {
        try (Jedis probe = pool.getResource()) {
            return "PONG".equals(probe.ping());
        } catch (JedisConnectionException e) {
            return false;
        }
    }
}
