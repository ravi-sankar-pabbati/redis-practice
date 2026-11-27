package com.practice.redis.spring.cache;

/**
 * The value we cache. It's stored in Redis as JSON, e.g.
 * {@code {"currency":"USD","rate":1.08,"fetchedAt":"2026-11-27T10:15:30Z"}}.
 *
 * @param fetchedAt when the "remote" lookup actually ran; it stays the same while the value
 *                  comes from the cache, which makes cache hits easy to spot
 */
public record ExchangeRate(String currency, double rate, String fetchedAt) {
}
