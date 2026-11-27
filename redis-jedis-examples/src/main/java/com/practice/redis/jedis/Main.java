package com.practice.redis.jedis;

import static com.practice.redis.jedis.CommandLogger.log;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 * Runs every example in order and prints each Redis command with its result.
 *
 * <p>Start Redis first ({@code docker compose up -d} in the project root), then run:
 * {@code mvn -pl redis-jedis-examples exec:java}
 *
 * <p>All keys start with "demo:", so you can find them easily in Redis Insight
 * (http://localhost:5540).
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        JedisPool pool = RedisConnection.getPool();
        try {
            // Borrow one connection for all the single-connection examples.
            try (Jedis jedis = pool.getResource()) {
                log("PING", jedis.ping());

                new StringsExample(jedis).run();
                new ListsExample(jedis).run();
                new HashesExample(jedis).run();
                new SetsExample(jedis).run();
                new SortedSetsExample(jedis).run();
            }

            // Pub/Sub needs two connections (subscriber + publisher), so it gets the pool.
            new PubSubExample(pool).run();

            System.out.println();
            System.out.println("Done. Browse the demo:* keys in Redis Insight at http://localhost:5540");
        } catch (JedisConnectionException e) {
            System.err.println("Could not connect to Redis at "
                    + RedisConnection.HOST + ":" + RedisConnection.PORT
                    + ". Is it running? Start it with: docker compose up -d");
            System.err.println("Cause: " + e.getMessage());
            System.exit(1);
        } finally {
            RedisConnection.close();
        }
    }
}
