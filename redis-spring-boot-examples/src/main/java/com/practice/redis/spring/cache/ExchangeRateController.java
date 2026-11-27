package com.practice.redis.spring.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for the caching example.
 *
 * <pre>
 *   GET    /rates/USD          cached lookup (slow the first time, instant until the TTL runs out)
 *   POST   /rates/USD/refresh  force a fresh lookup and overwrite the cache (@CachePut)
 *   DELETE /rates/USD          remove USD from the cache (@CacheEvict)
 *   DELETE /rates              clear the whole cache
 * </pre>
 *
 * <p>Each response includes how long the call took and how many real lookups have happened,
 * so you can see the cache working.
 */
@RestController
@RequestMapping("/rates")
public class ExchangeRateController {

    private final ExchangeRateService service;

    public ExchangeRateController(ExchangeRateService service) {
        this.service = service;
    }

    @GetMapping("/{currency}")
    public Map<String, Object> get(@PathVariable String currency) {
        long start = System.nanoTime();
        ExchangeRate rate = service.getRate(currency.toUpperCase());
        return response(rate, start);
    }

    @PostMapping("/{currency}/refresh")
    public Map<String, Object> refresh(@PathVariable String currency) {
        long start = System.nanoTime();
        ExchangeRate rate = service.refresh(currency.toUpperCase());
        return response(rate, start);
    }

    @DeleteMapping("/{currency}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void evict(@PathVariable String currency) {
        service.evict(currency.toUpperCase());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void evictAll() {
        service.evictAll();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> unknownCurrency(IllegalArgumentException e) {
        return Map.of("error", e.getMessage());
    }

    private Map<String, Object> response(ExchangeRate rate, long startNanos) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("rate", rate);
        body.put("tookMs", (System.nanoTime() - startNanos) / 1_000_000);
        body.put("remoteCallsSoFar", service.remoteCalls());
        return body;
    }
}
