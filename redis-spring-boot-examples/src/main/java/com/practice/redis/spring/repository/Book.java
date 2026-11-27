package com.practice.redis.spring.repository;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

/**
 * A book stored as a Redis hash by Spring Data.
 *
 * <p>Saving {@code Book("1", "Dune", "Frank Herbert", 1965)} creates these keys:
 * <pre>
 *   demo:books:1                        HASH  {_class, id, title, author, year}
 *   demo:books                          SET   every book id (used by findAll / count)
 *   demo:books:author:Frank Herbert     SET   ids of books by that author (because of @Indexed)
 *   demo:books:1:idx                    SET   which index sets this book is in (for cleanup)
 * </pre>
 *
 * <p>A Java record works as an entity: Spring Data reads and writes it through its constructor.
 */
@RedisHash("demo:books") // the key prefix; each book is stored at "demo:books:<id>"
public record Book(
        @Id String id,             // left null on create: Spring Data generates a random UUID
        String title,
        @Indexed String author,    // @Indexed makes findByAuthor(...) possible
        int year) {
}
