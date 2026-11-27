package com.practice.redis.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ListsExampleTest extends RedisTestSupport {

    @Test
    void listsWorkAsQueueStackAndCappedFeed() {
        ListsExample example = new ListsExample(jedis);

        // Queue: first in, first out
        assertEquals("job1", example.queue());
        assertEquals(List.of("job2", "job3"), jedis.lrange("demo:queue", 0, -1));

        // Stack: last in, first out
        assertEquals("c", example.stack());

        // LTRIM kept only the 3 most recently pushed pages, newest first
        assertEquals(List.of("blog", "docs", "pricing"), example.latestItems());
    }
}
