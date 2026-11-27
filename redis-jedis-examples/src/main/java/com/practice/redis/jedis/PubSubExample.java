package com.practice.redis.jedis;

import static com.practice.redis.jedis.CommandLogger.log;
import static com.practice.redis.jedis.CommandLogger.note;
import static com.practice.redis.jedis.CommandLogger.section;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

/**
 * PUB/SUB - publishers send messages to a CHANNEL; every client subscribed to that
 * channel at that moment receives them. Messages are not stored: a subscriber that
 * connects later never sees earlier messages.
 *
 * <p>Key point: SUBSCRIBE blocks its connection (it waits for messages forever), so the
 * subscriber needs its own connection on its own thread. That is why this example takes
 * a {@link JedisPool} instead of a single {@link Jedis}.
 */
public class PubSubExample {

    private static final long TIMEOUT_SECONDS = 5;

    private final JedisPool pool;

    public PubSubExample(JedisPool pool) {
        this.pool = pool;
    }

    public void run() throws InterruptedException {
        section("Pub/Sub");
        publishAndReceive("demo:news", List.of("Redis 101 starts now", "Lists are fun", "Goodbye!"));
    }

    /**
     * Subscribes to {@code channel} on a background thread, publishes {@code messages},
     * and returns the messages the subscriber received, in order.
     */
    public List<String> publishAndReceive(String channel, List<String> messages) throws InterruptedException {
        List<String> received = new CopyOnWriteArrayList<>();  // thread-safe: written by the subscriber thread
        CountDownLatch subscribed = new CountDownLatch(1);
        CountDownLatch allReceived = new CountDownLatch(messages.size());

        // JedisPubSub is a callback object: Jedis calls these methods as events arrive.
        JedisPubSub listener = new JedisPubSub() {
            @Override
            public void onSubscribe(String ch, int subscribedChannels) {
                note("subscriber joined channel '" + ch + "'");
                subscribed.countDown();
            }

            @Override
            public void onMessage(String ch, String message) {
                note("subscriber received on '" + ch + "': \"" + message + "\"");
                received.add(message);
                allReceived.countDown();
            }
        };

        // 1. Start the subscriber. subscribe() blocks this thread until we unsubscribe.
        Thread subscriber = new Thread(() -> {
            try (Jedis subscriberConnection = pool.getResource()) {
                subscriberConnection.subscribe(listener, channel);
            }
        }, "redis-subscriber");
        subscriber.start();

        // 2. Wait until the subscription is active, otherwise early messages would be lost.
        if (!subscribed.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Subscriber did not connect within " + TIMEOUT_SECONDS + "s");
        }

        // 3. Publish on a different connection. PUBLISH returns how many subscribers got the message.
        try (Jedis publisher = pool.getResource()) {
            for (String message : messages) {
                log("PUBLISH " + channel + " \"" + message + "\"", publisher.publish(channel, message));
            }
        }

        // 4. Wait for delivery, then unsubscribe so the subscriber thread can finish.
        allReceived.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        listener.unsubscribe();
        subscriber.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));

        return List.copyOf(received);
    }
}
