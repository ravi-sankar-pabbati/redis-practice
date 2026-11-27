# redis-practice

Hands-on examples for learning Redis from Java, first with the plain Jedis client and then inside a Spring Boot app. The command-line examples print every Redis command they run and the result, so you can follow along the way you would in `redis-cli`. The Spring Boot examples add REST endpoints you can call with curl.

## Prerequisites

- Java 17 or newer
- Maven 3.9 or newer
- Docker (with Docker Compose)

## Start Redis

From the project root:

```bash
docker compose up -d
```

This starts two containers:

| Service       | URL / port              | Purpose                                 |
|---------------|-------------------------|-----------------------------------------|
| Redis 7.4     | `localhost:6379`        | The database the examples connect to    |
| Redis Insight | <http://localhost:5540> | Web UI for browsing keys and running commands |

Redis Insight is set up to connect to the `redis-practice` database automatically. If it isn't listed, add a database with host `redis` and port `6379`. Use `redis`, not `localhost`, because Redis Insight runs inside the Docker network.

Other useful commands:

```bash
docker compose exec redis redis-cli   # open a redis-cli shell
docker compose down                   # stop (data is kept)
docker compose down -v                # stop and delete all data
```

## Build and run

```bash
mvn test                                             # build every module and run all tests
mvn -pl redis-jedis-examples exec:java               # run the Jedis examples
mvn -pl redis-spring-boot-examples spring-boot:run   # start the Spring Boot app on http://localhost:8080
```

Example output from the Jedis examples:

```
=== Strings ===
redis> DEL demo:greeting                                   -> 1
redis> SET demo:greeting "Hello"                           -> OK
redis> APPEND demo:greeting ", Redis!"                     -> 13
redis> GET demo:missing                                    -> (nil)
redis> GET demo:greeting                                   -> "Hello, Redis!"
```

The examples in both modules write keys starting with `demo:` to database 0, so you can look at them in Redis Insight afterwards. The tests use database 15 and empty it before each test, so they never touch your example data. If Redis isn't running, the tests are skipped instead of failing.

## Modules

New to Redis? Start with the guided tour in [redis-jedis-examples/README.md](redis-jedis-examples/README.md). It explains every example command by command. Then move on to [redis-spring-boot-examples/README.md](redis-spring-boot-examples/README.md) to see how a Spring Boot app uses the same ideas.

| Module | What it teaches |
|--------|-----------------|
| [redis-jedis-examples](redis-jedis-examples) | Core Redis data types with the Jedis client: connection pooling, **Strings** (SET/GET, counters, expiry), **Lists** (queues, stacks, capped feeds), **Hashes** (objects with fields), **Sets** (unique members, intersection/union/difference), **Sorted Sets** (leaderboards, score ranges) and **Pub/Sub** (publishing and subscribing on separate connections). |
| [redis-spring-boot-examples](redis-spring-boot-examples) | Redis in a Spring Boot 4 app: **StringRedisTemplate** (the same commands, Spring style), **Spring Data repositories** (`@RedisHash` objects with indexed finders), **caching** (`@Cacheable`/`@CachePut`/`@CacheEvict` with a TTL and JSON values) and **sessions** (Spring Session storing `HttpSession` in Redis). Each one has a REST endpoint you can try with curl. |

## Project layout

```
redis-practice/
├── pom.xml                        parent POM: modules + managed versions
├── docker-compose.yml             Redis + Redis Insight, shared by all modules
├── redis-jedis-examples/
│   └── src/
│       ├── main/java/com/practice/redis/jedis/
│       │   ├── RedisConnection.java   JedisPool helper (localhost:6379)
│       │   ├── CommandLogger.java     prints "redis> COMMAND -> result"
│       │   ├── *Example.java          one class per data type
│       │   └── Main.java              runs all examples
│       └── test/java/...              one JUnit 5 test per example
└── redis-spring-boot-examples/
    └── src/
        ├── main/java/com/practice/redis/spring/
        │   ├── RedisSpringApplication.java   Spring Boot entry point (@EnableCaching)
        │   ├── template/                     StringRedisTemplate tour, printed on startup
        │   ├── repository/                   @RedisHash Book + BookRepository + REST API
        │   ├── cache/                        @Cacheable exchange-rate service + REST API
        │   └── session/                      HttpSession stored in Redis + REST API
        ├── main/resources/application.properties   Redis, cache and session settings
        └── test/java/...                     one Spring Boot test per example
```
