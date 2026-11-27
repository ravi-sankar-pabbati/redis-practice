package com.practice.redis.spring.template;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Runs the RedisTemplate tour once the application has started.
 *
 * <p>A {@link CommandLineRunner} bean runs after the Spring context is ready. It's switched off
 * in the tests with {@code demo.startup-tour.enabled=false}, so test output stays clean.
 */
@Component
@ConditionalOnProperty(name = "demo.startup-tour.enabled", havingValue = "true", matchIfMissing = true)
public class StartupTour implements CommandLineRunner {

    private final RedisTemplateExample example;

    public StartupTour(RedisTemplateExample example) {
        this.example = example;
    }

    @Override
    public void run(String... args) {
        example.runAll();
        System.out.println("Try the REST examples (see README.md):");
        System.out.println("  curl -X POST localhost:8080/books -H 'Content-Type: application/json' "
                + "-d '{\"title\":\"Dune\",\"author\":\"Frank Herbert\",\"year\":1965}'");
        System.out.println("  curl localhost:8080/rates/USD      # slow the first time, instant after");
        System.out.println("  curl -c c.txt -b c.txt localhost:8080/session/visits");
        System.out.println();
    }
}
