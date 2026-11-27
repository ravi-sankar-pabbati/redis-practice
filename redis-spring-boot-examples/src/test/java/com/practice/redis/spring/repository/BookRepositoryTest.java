package com.practice.redis.spring.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.practice.redis.spring.RedisIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.DataType;
import org.springframework.http.MediaType;

class BookRepositoryTest extends RedisIntegrationTest {

    @Autowired
    BookRepository books;

    @Test
    void savesBooksAsHashesAndFindsThemByIndexedField() {
        books.save(new Book("1", "Dune", "Frank Herbert", 1965));
        books.save(new Book("2", "Neuromancer", "William Gibson", 1984));

        assertThat(books.findById("1")).contains(new Book("1", "Dune", "Frank Herbert", 1965));
        assertThat(books.count()).isEqualTo(2);
        assertThat(books.findByAuthor("William Gibson")).extracting(Book::title).containsExactly("Neuromancer");

        // The keys Spring Data created, as described in Book's Javadoc
        assertThat(redis.type("demo:books:1")).isEqualTo(DataType.HASH);
        assertThat(redis.opsForHash().get("demo:books:1", "title")).isEqualTo("Dune");
        assertThat(redis.opsForSet().members("demo:books")).containsExactlyInAnyOrder("1", "2");
        assertThat(redis.opsForSet().members("demo:books:author:Frank Herbert")).containsExactly("1");

        // Deleting also cleans up the index entries
        books.deleteById("1");
        assertThat(redis.hasKey("demo:books:1")).isFalse();
        assertThat(books.findByAuthor("Frank Herbert")).isEmpty();
    }

    @Test
    void restEndpointGeneratesAnIdOnCreate() throws Exception {
        mvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Dune\",\"author\":\"Frank Herbert\",\"year\":1965}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty());

        mvc.perform(get("/books").param("author", "Frank Herbert"))
                .andExpect(jsonPath("$[0].title").value("Dune"));

        mvc.perform(get("/books/does-not-exist")).andExpect(status().isNotFound());
    }
}
