package com.practice.redis.spring.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.practice.redis.spring.RedisIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ExchangeRateServiceTest extends RedisIntegrationTest {

    private static final String KEY = "demo:cache:rates::USD";

    @Autowired
    ExchangeRateService service;

    @Test
    void secondLookupComesFromTheCache() {
        int before = service.remoteCalls(); // the counter is shared by every test, so compare differences

        ExchangeRate first = service.getRate("USD");   // miss: runs the method, stores the result
        ExchangeRate second = service.getRate("USD");  // hit: method body skipped

        assertThat(second).isEqualTo(first);
        assertThat(service.remoteCalls() - before).isEqualTo(1);

        // The entry is JSON with a TTL (spring.cache.redis.time-to-live=60s)
        assertThat(redis.opsForValue().get(KEY)).contains("\"currency\":\"USD\"");
        assertThat(redis.getExpire(KEY)).isBetween(1L, 60L);
    }

    @Test
    void evictForcesAFreshLookup() {
        service.getRate("GBP");
        service.evict("GBP");
        assertThat(redis.hasKey("demo:cache:rates::GBP")).isFalse();

        int before = service.remoteCalls();
        service.getRate("GBP");
        assertThat(service.remoteCalls() - before).isEqualTo(1);
    }

    @Test
    void cachePutAlwaysRunsAndReplacesTheEntry() {
        service.getRate("USD");
        int before = service.remoteCalls();

        ExchangeRate refreshed = service.refresh("USD");

        assertThat(service.remoteCalls() - before).isEqualTo(1);
        assertThat(service.getRate("USD")).isEqualTo(refreshed); // the cache now holds the new value
    }
}
