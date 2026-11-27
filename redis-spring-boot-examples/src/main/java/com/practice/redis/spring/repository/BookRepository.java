package com.practice.redis.spring.repository;

import java.util.List;
import org.springframework.data.repository.CrudRepository;

/**
 * Spring Data writes the implementation of this interface at startup.
 *
 * <p>{@link CrudRepository} gives us save, findById, findAll, count, deleteById, ... for free.
 * Extra finders are derived from the method name. Redis has no query language, so derived
 * queries only work on {@code @Indexed} fields and only for equality (no "greater than", no LIKE).
 */
public interface BookRepository extends CrudRepository<Book, String> {

    /** Reads the index set "demo:books:author:<author>" and then loads each book hash. */
    List<Book> findByAuthor(String author);
}
