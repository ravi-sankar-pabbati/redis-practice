package com.practice.redis.spring.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Pretends to call a slow remote API for exchange rates, with Redis caching in front of it.
 *
 * <p>How the annotations work: Spring wraps this bean in a proxy. When a controller calls
 * {@code getRate("USD")}, the proxy first looks for the key {@code demo:cache:rates::USD} in Redis.
 * <ul>
 *   <li>Found (cache hit): returns the cached value; the method body never runs.</li>
 *   <li>Missing (cache miss): runs the method, stores the result in Redis with a TTL, returns it.</li>
 * </ul>
 *
 * <p>Gotcha: the proxy only sees calls coming from <em>outside</em> the bean. If another method
 * in this class called {@code getRate(...)} directly, the cache would be skipped.
 */
@Service
public class ExchangeRateService {

    public static final String CACHE = "rates";

    /** Fake "remote" data: euros per unit of each currency. */
    private static final Map<String, Double> RATES = Map.of("USD", 0.92, "GBP", 1.17, "JPY", 0.0061, "INR", 0.011);

    private final Duration simulatedDelay;
    private final AtomicInteger remoteCalls = new AtomicInteger();

    public ExchangeRateService(@Value("${demo.rates.simulated-delay:2s}") Duration simulatedDelay) {
        this.simulatedDelay = simulatedDelay;
    }

    /** Cached: the key is the method argument, so each currency gets its own cache entry. */
    @Cacheable(CACHE)
    public ExchangeRate getRate(String currency) {
        return fetchFromRemote(currency);
    }

    /** Always runs the method and overwrites the cache entry with the fresh result. */
    @CachePut(cacheNames = CACHE, key = "#currency")
    public ExchangeRate refresh(String currency) {
        return fetchFromRemote(currency);
    }

    /** Deletes one entry; the next getRate(currency) will be a cache miss. */
    @CacheEvict(cacheNames = CACHE, key = "#currency")
    public void evict(String currency) {
    }

    /** Deletes every entry in the "rates" cache. */
    @CacheEvict(cacheNames = CACHE, allEntries = true)
    public void evictAll() {
    }

    /** How many times the slow lookup actually ran: cache hits don't count. */
    public int remoteCalls() {
        return remoteCalls.get();
    }

    private ExchangeRate fetchFromRemote(String currency) {
        remoteCalls.incrementAndGet();
        sleep(simulatedDelay); // the cost caching saves us
        Double rate = RATES.get(currency.toUpperCase());
        if (rate == null) {
            throw new IllegalArgumentException("Unknown currency: " + currency);
        }
        return new ExchangeRate(currency.toUpperCase(), rate, Instant.now().toString());
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
