package com.practice.redis.spring;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Shared setup for every test in this module.
 *
 * <ul>
 *   <li>Starts the whole application once (Spring caches the context, so all test classes share it).</li>
 *   <li>Uses Redis database 15, so the example data in database 0 is never touched.</li>
 *   <li>Empties database 15 before each test, so tests don't affect each other.</li>
 *   <li>Skips (instead of failing) when Redis isn't running.</li>
 * </ul>
 */
@SpringBootTest(properties = {
        "spring.data.redis.database=15",
        "demo.startup-tour.enabled=false",   // no console tour during tests
        "demo.rates.simulated-delay=0s"      // keep the cache tests fast
})
@AutoConfigureMockMvc // MockMvc sends HTTP requests to the controllers without starting a real server
public abstract class RedisIntegrationTest {

    @Autowired
    protected StringRedisTemplate redis;

    @Autowired
    protected MockMvc mvc;

    /** Runs before Spring starts the application, so a missing Redis skips the class cleanly. */
    @BeforeAll
    static void requireRedis() {
        assumeTrue(isRedisReachable(), "Redis is not running on localhost:6379; start it with: docker compose up -d");
    }

    @BeforeEach
    void emptyTestDatabase() {
        redis.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushDb(); // only database 15
            return null;
        });
    }

    private static boolean isRedisReachable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 6379), 500);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
