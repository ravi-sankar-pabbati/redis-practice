package com.practice.redis.spring.template;

import static org.assertj.core.api.Assertions.assertThat;

import com.practice.redis.spring.RedisIntegrationTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RedisTemplateExampleTest extends RedisIntegrationTest {

    @Autowired
    RedisTemplateExample example;

    @Test
    void stringsAndCounters() {
        assertThat(example.strings()).isEqualTo("Hello from Spring");
        assertThat(example.counter()).isEqualTo(6L); // INCR -> 1, INCRBY 5 -> 6
    }

    @Test
    void expiringKeyHasATtl() {
        assertThat(example.expiringKey()).isBetween(1L, 30L);
    }

    @Test
    void listHashAndSortedSet() {
        assertThat(example.list()).isEqualTo(List.of("resize", "upload")); // "email" was popped
        assertThat(example.hash()).isEqualTo(Map.of("name", "Bob", "city", "Berlin", "logins", "1"));
        assertThat(example.sortedSet()).containsExactly("bob", "carol"); // top 2, highest score first
    }
}
