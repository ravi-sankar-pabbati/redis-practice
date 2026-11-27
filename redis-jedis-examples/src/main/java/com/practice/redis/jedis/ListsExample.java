package com.practice.redis.jedis;

import static com.practice.redis.jedis.CommandLogger.log;
import static com.practice.redis.jedis.CommandLogger.note;
import static com.practice.redis.jedis.CommandLogger.section;

import java.util.List;
import redis.clients.jedis.Jedis;

/**
 * LISTS - an ordered sequence of strings. You can push/pop at both ends quickly.
 *
 * <p>Typical uses: job queues (push right, pop left), stacks, "latest N items" feeds.
 * Index 0 is the first (left) element; -1 is the last.
 */
public class ListsExample {

    private final Jedis jedis;

    public ListsExample(Jedis jedis) {
        this.jedis = jedis;
    }

    public void run() {
        section("Lists");
        queue();
        stack();
        latestItems();
    }

    /** Queue (FIFO): add on the right with RPUSH, take from the left with LPOP. */
    public String queue() {
        log("DEL demo:queue", jedis.del("demo:queue"));
        log("RPUSH demo:queue job1 job2 job3", jedis.rpush("demo:queue", "job1", "job2", "job3"));
        log("LRANGE demo:queue 0 -1", jedis.lrange("demo:queue", 0, -1));  // 0 -1 = whole list
        String first = log("LPOP demo:queue", jedis.lpop("demo:queue"));   // job1: first in, first out
        log("LLEN demo:queue", jedis.llen("demo:queue"));
        return first;
    }

    /** Stack (LIFO): add and take on the same side (left). */
    public String stack() {
        log("DEL demo:stack", jedis.del("demo:stack"));
        log("LPUSH demo:stack a b c", jedis.lpush("demo:stack", "a", "b", "c"));
        note("LPUSH inserts each value at the head, so the list is now c, b, a");
        log("LRANGE demo:stack 0 -1", jedis.lrange("demo:stack", 0, -1));
        return log("LPOP demo:stack", jedis.lpop("demo:stack"));            // c: last in, first out
    }

    /** "Latest N": push new items at the head, then LTRIM keeps only the first 3. */
    public List<String> latestItems() {
        log("DEL demo:recent", jedis.del("demo:recent"));
        for (String page : List.of("home", "about", "pricing", "docs", "blog")) {
            log("LPUSH demo:recent " + page, jedis.lpush("demo:recent", page));
        }
        log("LTRIM demo:recent 0 2", jedis.ltrim("demo:recent", 0, 2));      // keep indexes 0..2
        return log("LRANGE demo:recent 0 -1", jedis.lrange("demo:recent", 0, -1));
    }
}
