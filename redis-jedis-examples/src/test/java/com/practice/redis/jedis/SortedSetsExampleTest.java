package com.practice.redis.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class SortedSetsExampleTest extends RedisTestSupport {

    @Test
    void sortedSetsOrderMembersByScore() {
        SortedSetsExample example = new SortedSetsExample(jedis);

        // Scores after ZINCRBY: carol 120, bob 115, alice 100, dave 70
        assertEquals(List.of("carol", "bob", "alice"), example.leaderboard());
        assertEquals(115.0, jedis.zscore("demo:leaderboard", "bob"));

        // Fresh leaderboard (bob back to 85): members scoring 90..130, lowest first
        assertEquals(List.of("alice", "carol"), example.rangeByScore());
    }
}
