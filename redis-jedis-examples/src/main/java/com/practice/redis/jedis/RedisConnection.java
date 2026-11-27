package com.practice.redis.jedis;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

/**
 * Creates and shares a {@link JedisPool} connected to the local Redis (localhost:6379).
 *
 * <p>Why a pool? A single {@code Jedis} object is one TCP connection and is NOT thread-safe.
 * A pool keeps a few connections open and hands one out per task:
 *
 * <pre>{@code
 * try (Jedis jedis = RedisConnection.getPool().getResource()) {
 *     jedis.set("key", "value");
 * } // close() returns the connection to the pool (it does not disconnect)
 * }</pre>
 */
public final class RedisConnection {

    public static final String HOST = "localhost";
    public static final int PORT = 6379;

    /** Redis has 16 numbered databases (0-15) by default. Examples use 0. */
    public static final int DEFAULT_DATABASE = 0;

    private static JedisPool pool;

    private RedisConnection() {
        // static helper, not meant to be instantiated
    }

    /** Returns the shared pool for database 0, creating it on first use. */
    public static synchronized JedisPool getPool() {
        if (pool == null || pool.isClosed()) {
            pool = createPool(DEFAULT_DATABASE);
        }
        return pool;
    }

    /**
     * Creates a new pool for the given database number.
     * Tests use this to work in a separate database so they never touch your example data.
     */
    public static JedisPool createPool(int database) {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(8);         // at most 8 connections open at the same time
        config.setMaxIdle(8);          // keep up to 8 idle connections ready for reuse
        config.setMinIdle(0);          // don't open connections until they are needed
        config.setTestOnBorrow(true);  // PING a connection before handing it out
        config.setJmxEnabled(false);   // skip JMX registration; not needed for learning

        // Arguments: config, host, port, timeout (ms), password (none), database number
        return new JedisPool(config, HOST, PORT, Protocol.DEFAULT_TIMEOUT, null, database);
    }

    /** Closes the shared pool and all of its connections. */
    public static synchronized void close() {
        if (pool != null) {
            pool.close();
            pool = null;
        }
    }
}
