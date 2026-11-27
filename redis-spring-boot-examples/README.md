# redis-spring-boot-examples

How Spring Boot apps use Redis, in four small examples:

| # | Example | Package | What it shows |
|---|---------|---------|---------------|
| 1 | [RedisTemplate](#1-redistemplate) | `template` | The same commands as the Jedis module, written with Spring's `StringRedisTemplate` |
| 2 | [Repositories](#2-spring-data-repositories) | `repository` | Saving Java objects as hashes with `@RedisHash` and a `CrudRepository` |
| 3 | [Caching](#3-caching) | `cache` | `@Cacheable`: storing slow method results in Redis with a TTL |
| 4 | [Sessions](#4-sessions) | `session` | Spring Session: `HttpSession` data stored in Redis instead of server memory |

Built with Spring Boot 4.2 (Spring Data Redis 4.2, the Lettuce client, Jackson 3, JUnit 6).

If Redis is new to you, read the [Jedis guided tour](../redis-jedis-examples/README.md) first. This module assumes you know what strings, hashes and TTLs are.

---

## Quick start

```bash
# from the project root
docker compose up -d                                   # start Redis + Redis Insight
mvn -pl redis-spring-boot-examples spring-boot:run     # start the app on http://localhost:8080
```

On startup the app prints the RedisTemplate tour, then keeps running so you can call the REST endpoints. Stop it with `Ctrl+C`.

> **Port 8080 already in use?** Another app (kafka-ui, for example) may have it. Stop that app, or change `server.port` in `application.properties`.

All examples write keys starting with `demo:` to database 0, so you can watch them appear in Redis Insight (<http://localhost:5540>) while you try things.

---

## How Spring Boot connects to Redis

There's no `RedisConnection` helper class here like in the Jedis module. Spring Boot sees `spring-boot-starter-data-redis` on the classpath and builds everything from [application.properties](src/main/resources/application.properties):

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.database=0
```

From that it creates a connection factory (using **Lettuce**, Spring's default Redis client, instead of Jedis) and beans you can inject: `StringRedisTemplate`, `RedisTemplate`, the repositories, a cache manager and the session store.

| Jedis module | This module |
|---|---|
| You create a `JedisPool` | Spring Boot creates a connection factory |
| `try (Jedis jedis = pool.getResource())` | Inject `StringRedisTemplate`; it gets and returns connections for you |
| `jedis.set(...)`, `jedis.hset(...)` | `redis.opsForValue().set(...)`, `redis.opsForHash().put(...)` |
| You close the pool on exit | Spring closes everything on shutdown |

---

## 1. RedisTemplate

[RedisTemplateExample.java](src/main/java/com/practice/redis/spring/template/RedisTemplateExample.java), run at startup by [StartupTour.java](src/main/java/com/practice/redis/spring/template/StartupTour.java).

`StringRedisTemplate` groups commands by data type:

| Method | Data type | Example |
|---|---|---|
| `opsForValue()` | Strings | `set`, `get`, `increment` |
| `opsForList()` | Lists | `rightPushAll`, `leftPop`, `range` |
| `opsForHash()` | Hashes | `putAll`, `increment`, `entries` |
| `opsForSet()` | Sets | `add`, `members`, `intersect` |
| `opsForZSet()` | Sorted sets | `add`, `reverseRange`, `incrementScore` |
| on the template itself | any key | `delete`, `hasKey`, `getExpire`, `expire` |

Startup output:

```
=== RedisTemplate tour ===
redis> SET demo:spring:greeting "Hello from Spring"       -> OK
redis> GET demo:spring:greeting                           -> Hello from Spring
redis> INCR demo:spring:counter                           -> 1
redis> INCRBY demo:spring:counter 5                       -> 6
redis> SET demo:spring:otp "493817" EX 30                 -> OK
redis> TTL demo:spring:otp                                -> 30
redis> RPUSH demo:spring:tasks email resize upload        -> 3
redis> LPOP demo:spring:tasks                             -> email
redis> LRANGE demo:spring:tasks 0 -1                      -> [resize, upload]
redis> HSET demo:spring:user:2 name Bob city Berlin       -> OK
redis> HINCRBY demo:spring:user:2 logins 1                -> 1
redis> HGETALL demo:spring:user:2                         -> {name=Bob, city=Berlin, logins=1}
redis> ZADD demo:spring:scores 100 alice 140 bob 120 carol -> 3
redis> ZREVRANGE demo:spring:scores 0 1                   -> [bob, carol]
```

Things to notice:

- **The expiry is a `Duration`:** `opsForValue().set(key, value, Duration.ofSeconds(30))` sends a single `SET ... EX 30`, so setting the value and its TTL is atomic.
- **Missing keys come back as `null`:** a `get` on a key that doesn't exist returns `null`, just like Jedis.
- **`StringRedisTemplate` vs `RedisTemplate`:**
  - The string template stores everything as plain text, so you can read it in Redis Insight.
  - A raw `RedisTemplate<Object, Object>` uses Java serialization by default, so keys show up as `\xac\xed\x00\x05t\x00...`.
  - Use `StringRedisTemplate` unless you've set up serializers on purpose.

---

## 2. Spring Data repositories

[Book.java](src/main/java/com/practice/redis/spring/repository/Book.java), [BookRepository.java](src/main/java/com/practice/redis/spring/repository/BookRepository.java), [BookController.java](src/main/java/com/practice/redis/spring/repository/BookController.java)

You write an annotated class and an interface. Spring Data writes the Redis code.

```java
@RedisHash("demo:books")                 // key prefix
public record Book(@Id String id, String title, @Indexed String author, int year) {}

public interface BookRepository extends CrudRepository<Book, String> {
    List<Book> findByAuthor(String author); // works because author is @Indexed
}
```

### Try it

```bash
curl -X POST localhost:8080/books/samples                       # load 3 sample books
curl localhost:8080/books                                       # all books
curl "localhost:8080/books?author=Frank%20Herbert"              # find by indexed field
curl localhost:8080/books/1                                     # one book (404 if missing)
curl -X POST localhost:8080/books -H 'Content-Type: application/json' \
     -d '{"title":"Snow Crash","author":"Neal Stephenson","year":1992}'   # id generated
curl -X DELETE localhost:8080/books/1
```

### What it stores

Saving one book creates **four** keys:

| Key | Type | Contents |
|---|---|---|
| `demo:books:1` | hash | `_class`, `id`, `title`, `author`, `year` (the object itself) |
| `demo:books` | set | every book id, used by `findAll()` and `count()` |
| `demo:books:author:Frank Herbert` | set | ids of books by that author, created because of `@Indexed` |
| `demo:books:1:idx` | set | which index sets book 1 is in, so a delete can clean them up |

```
redis> HGETALL demo:books:1
_class  com.practice.redis.spring.repository.Book
id      1
title   Dune
author  Frank Herbert
year    1965
```

### Limits worth knowing

- **Limited queries:** Redis has no query language, so derived finders only work on `@Indexed` fields and only test equality. `findByYearGreaterThan` won't work. For real search and filtering you'd use the Redis Query Engine (Redis Stack / Redis 8), which this project doesn't cover.
- **IDs:** if `id` is `null`, `save()` generates a random UUID.
- **Expiry:** to make entities expire automatically, use `@RedisHash(value = "...", timeToLive = 3600)`.

---

## 3. Caching

[ExchangeRateService.java](src/main/java/com/practice/redis/spring/cache/ExchangeRateService.java), [ExchangeRateController.java](src/main/java/com/practice/redis/spring/cache/ExchangeRateController.java), [CacheConfig.java](src/main/java/com/practice/redis/spring/cache/CacheConfig.java)

The service pretends to call a slow remote API (2 seconds). One annotation puts Redis in front of it:

```java
@Cacheable("rates")
public ExchangeRate getRate(String currency) { ... slow lookup ... }
```

When something calls `getRate("USD")`, Spring checks Redis for `demo:cache:rates::USD` first:

```
            ┌── key exists (hit)  ──► return cached value, method never runs
getRate ──► │
            └── key missing (miss) ──► run method ──► store result with TTL ──► return it
```

### Try it

```bash
curl localhost:8080/rates/USD            # miss: ~2000 ms
curl localhost:8080/rates/USD            # hit:  a few ms, same fetchedAt
curl -X POST localhost:8080/rates/USD/refresh   # @CachePut: always runs, overwrites the entry
curl -X DELETE localhost:8080/rates/USD  # @CacheEvict: next GET is a miss again
curl -X DELETE localhost:8080/rates      # clear the whole cache
```

Real output:

```json
{"rate":{"currency":"USD","rate":0.92,"fetchedAt":"2026-11-27T12:28:36.969007Z"},"tookMs":2042,"remoteCallsSoFar":1}
{"rate":{"currency":"USD","rate":0.92,"fetchedAt":"2026-11-27T12:28:36.969007Z"},"tookMs":5,"remoteCallsSoFar":1}
```

The second call took 5 ms, and `remoteCallsSoFar` stayed at 1 because the method didn't run.

### The annotations

| Annotation | Runs the method? | Effect on the cache |
|---|---|---|
| `@Cacheable` | only on a miss | stores the result |
| `@CachePut` | always | overwrites the entry with the new result |
| `@CacheEvict` | always | deletes the entry (or all entries with `allEntries = true`) |

### Configuration

In `application.properties`:

```properties
spring.cache.type=redis
spring.cache.redis.time-to-live=60s          # every entry gets a 60 s TTL
spring.cache.redis.key-prefix=demo:cache:    # keys look like demo:cache:rates::USD
```

`CacheConfig` changes two defaults.

**1. Values are stored as JSON** instead of with Java serialization, so they're readable in Redis Insight:

```
redis> GET demo:cache:rates::USD
{"currency":"USD","rate":0.92,"fetchedAt":"2026-11-27T12:28:36.969007Z"}
```

**2. Writes are immediate.**
- **The default:** with the Lettuce client, Spring Data Redis 4 sends cache writes *asynchronously*. The method returns before Redis has stored the value.
- **The trade-off:** that's a little faster, but a read straight after a `@CachePut` can still return the old value.
- **How it was found:** this project's `@CachePut` test hit exactly that, failing about 1 run in 3, until `immediateWrites()` was turned on.
- **In production:** async writes are usually fine for a cache, because a briefly stale value is acceptable. Choose deliberately.

### Common gotchas

- **Self-calls skip the cache.** `@Cacheable` works through a proxy around the bean. A call from another method *inside the same class* doesn't go through the proxy, so it's never cached.
- **Stale data.** Cached values stay until their TTL runs out or you evict them. Choose a TTL you can live with, and evict entries when the underlying data changes.
- **Async writes (Spring Data Redis 4).** Without `immediateWrites()`, a value you just cached may not be in Redis yet when the next line reads it. See Configuration above.
- **`@EnableCaching` is required.** Without it ([RedisSpringApplication.java](src/main/java/com/practice/redis/spring/RedisSpringApplication.java)), the annotations do nothing.

---

## 4. Sessions

[VisitController.java](src/main/java/com/practice/redis/spring/session/VisitController.java)

The controller uses the ordinary servlet `HttpSession` and never mentions Redis:

```java
Integer visits = (Integer) session.getAttribute("visits");
session.setAttribute("visits", visits == null ? 1 : visits + 1);
```

Because `spring-boot-starter-session-data-redis` is on the classpath, Spring Session adds a filter that replaces Tomcat's in-memory sessions with sessions stored in Redis. The browser gets a `SESSION` cookie holding the session id.

### Try it

`-c` saves cookies to a file and `-b` sends them back, the way a browser would:

```bash
curl -c c.txt -b c.txt localhost:8080/session/visits       # visits: 1
curl -c c.txt -b c.txt localhost:8080/session/visits       # visits: 2  (same session)
curl localhost:8080/session/visits                         # visits: 1  (no cookie = new session)
curl -c c.txt -b c.txt -X POST "localhost:8080/session/name?name=Ada"
curl -c c.txt -b c.txt -X DELETE localhost:8080/session    # log out: session deleted from Redis
```

```json
{"sessionId":"3cd92bd7-...","redisKey":"demo:sessions:sessions:3cd92bd7-...","visits":2,"name":null,"expiresAfterIdleSeconds":1800}
```

**The interesting experiment:** call `/session/visits` a few times, restart the app (`Ctrl+C`, then `spring-boot:run` again), and call it again with the same cookie file. The count continues, because the session lives in Redis rather than in the app. This is also how several app instances behind a load balancer can share sessions.

### What it stores

| Key | Type | Contents |
|---|---|---|
| `demo:sessions:sessions:<id>` | hash | `creationTime`, `lastAccessedTime`, `maxInactiveInterval`, and one `sessionAttr:<name>` field per attribute |

- **TTL:** the key's TTL is reset on every request, so idle sessions expire after `server.servlet.session.timeout` (30 minutes here).
- **Attribute values aren't readable:** they're stored with Java serialization, so Redis Insight shows them as bytes.
- **Namespace:** the `demo:sessions` prefix comes from `spring.session.data.redis.namespace`.

---

## Tests

```bash
mvn -pl redis-spring-boot-examples test
```

| Test | Checks |
|---|---|
| `RedisTemplateExampleTest` | each template method returns what Redis returned |
| `BookRepositoryTest` | save, find by id, find by indexed author, the four keys created, cleanup on delete, REST create/404 |
| `ExchangeRateServiceTest` | second call is a cache hit, entry is JSON with a TTL, evict and `@CachePut` behave as described |
| `VisitControllerTest` | session hash is created in Redis, the cookie continues the session, logout deletes it |

All tests extend [RedisIntegrationTest](src/test/java/com/practice/redis/spring/RedisIntegrationTest.java):

- **Real Redis:** they start the full application against the real Redis, on **database 15**, with the startup tour off and the fake 2-second delay set to 0.
- **Clean start:** database 15 is emptied before each test.
- **Shared context:** every class uses the same Spring settings, so Spring starts the app once and reuses it for all of them.
- **Skipped if Redis is down:** if Redis isn't running, the tests are skipped instead of failing.
- **HTTP without a server:** `MockMvc` sends requests to the controllers without opening a port, so the tests work even while the app is running on 8080.

---

## Exercises

1. **Entity expiry:** add `timeToLive = 120` to `@RedisHash` on `Book`. Save a book and watch its TTL count down in Redis Insight. Which keys get the TTL and which don't?
2. **A second index:** make `year` `@Indexed` and add `findByYear(int year)`. Check which new keys appear.
3. **Cache a list:** add `@Cacheable("allBooks")` to a service method that returns all books. Then work out when to evict it. *Hint:* `@CacheEvict(cacheNames = "allBooks", allEntries = true)` on save and delete.
4. **Prove the self-call gotcha:** add a method to `ExchangeRateService` that calls `getRate` internally, then watch `remoteCallsSoFar` go up on every call.
5. **Shopping cart:** store a `Map<String, Integer>` cart in the session, with endpoints to add items and view the cart. Restart the app and check that the cart survives.
6. **Rate limiter:** use `opsForValue().increment(key)` plus `expire(key, Duration.ofMinutes(1))` to allow at most 5 requests per minute per IP. Return HTTP 429 after that.

---

## Troubleshooting

| Problem | Fix |
|---|---|
| `Port 8080 was already in use` | Another app is running on that port; stop it or change `server.port` |
| `Unable to connect to Redis` on startup | Start Redis: `docker compose up -d` |
| Tests show as **skipped** | Same: Redis isn't reachable on `localhost:6379` |
| Session or cache values look like `\xac\xed...` in Redis Insight | That's Java serialization (sessions use it by default); see `CacheConfig` for switching to JSON |
| `@Cacheable` has no effect | Check `@EnableCaching`, and make sure the call comes from another bean |
