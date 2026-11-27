package com.practice.redis.jedis;

import static com.practice.redis.jedis.CommandLogger.log;
import static com.practice.redis.jedis.CommandLogger.note;
import static com.practice.redis.jedis.CommandLogger.section;

import java.util.List;
import java.util.Map;
import redis.clients.jedis.Jedis;

/**
 * SORTED SETS - like a set (unique members), but every member has a numeric SCORE
 * and Redis keeps the members ordered by that score.
 *
 * <p>Typical uses: leaderboards, priority queues, time-ordered items (score = timestamp).
 */
public class SortedSetsExample {

    private final Jedis jedis;

    public SortedSetsExample(Jedis jedis) {
        this.jedis = jedis;
    }

    public void run() {
        section("Sorted Sets");
        leaderboard();
        rangeByScore();
    }

    /** Build a leaderboard and read the top players, highest score first. */
    public List<String> leaderboard() {
        createLeaderboard();
        log("ZINCRBY demo:leaderboard 30 bob", jedis.zincrby("demo:leaderboard", 30, "bob"));  // 85 -> 115
        log("ZSCORE demo:leaderboard carol", jedis.zscore("demo:leaderboard", "carol"));
        note("ZREVRANK = position counted from the highest score (0 = first place)");
        log("ZREVRANK demo:leaderboard bob", jedis.zrevrank("demo:leaderboard", "bob"));
        log("ZREVRANGE demo:leaderboard 0 2 WITHSCORES", jedis.zrevrangeWithScores("demo:leaderboard", 0, 2));
        return log("ZREVRANGE demo:leaderboard 0 2", jedis.zrevrange("demo:leaderboard", 0, 2));
    }

    /** Query by score instead of by position. */
    public List<String> rangeByScore() {
        createLeaderboard();
        log("ZCOUNT demo:leaderboard 90 130", jedis.zcount("demo:leaderboard", 90, 130));
        return log("ZRANGEBYSCORE demo:leaderboard 90 130", jedis.zrangeByScore("demo:leaderboard", 90, 130));
    }

    private void createLeaderboard() {
        log("DEL demo:leaderboard", jedis.del("demo:leaderboard"));
        log("ZADD demo:leaderboard 100 alice 85 bob 120 carol 70 dave",
                jedis.zadd("demo:leaderboard", Map.of("alice", 100.0, "bob", 85.0, "carol", 120.0, "dave", 70.0)));
    }
}
