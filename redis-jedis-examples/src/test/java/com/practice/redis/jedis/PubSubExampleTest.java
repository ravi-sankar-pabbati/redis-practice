package com.practice.redis.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class PubSubExampleTest extends RedisTestSupport {

    @Test
    void subscriberReceivesEveryPublishedMessageInOrder() throws InterruptedException {
        PubSubExample example = new PubSubExample(pool);
        List<String> messages = List.of("first", "second", "third");

        // A unique channel name keeps this test isolated from anything else publishing
        List<String> received = example.publishAndReceive("test:pubsub:" + System.nanoTime(), messages);

        assertEquals(messages, received);
    }
}
