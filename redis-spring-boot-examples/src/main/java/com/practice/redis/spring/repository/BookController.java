package com.practice.redis.spring.repository;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for the repository example.
 *
 * <pre>
 *   POST   /books                  create a book (JSON body), returns it with its generated id
 *   GET    /books                  all books
 *   GET    /books?author=Frank...  books by one author (uses the @Indexed field)
 *   GET    /books/{id}             one book, or 404
 *   DELETE /books/{id}             delete a book and its index entries
 * </pre>
 */
@RestController
@RequestMapping("/books")
public class BookController {

    private final BookRepository books;

    public BookController(BookRepository books) {
        this.books = books;
    }

    @PostMapping
    public Book create(@RequestBody Book book) {
        return books.save(book);
    }

    @GetMapping
    public Iterable<Book> list(@RequestParam(required = false) String author) {
        return author == null ? books.findAll() : books.findByAuthor(author);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Book> get(@PathVariable String id) {
        return ResponseEntity.of(books.findById(id)); // 200 with the book, or 404 if it's missing
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        books.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /** Handy for trying things out: loads three sample books. */
    @PostMapping("/samples")
    public List<Book> samples() {
        return (List<Book>) books.saveAll(List.of(
                new Book("1", "Dune", "Frank Herbert", 1965),
                new Book("2", "Children of Dune", "Frank Herbert", 1976),
                new Book("3", "Neuromancer", "William Gibson", 1984)));
    }
}
