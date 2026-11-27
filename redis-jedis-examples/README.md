# redis-jedis-examples: a guided tour

This module teaches the core Redis data types by running real commands from Java with the **Jedis** client. Each example prints the Redis command it runs and the result Redis sent back:

```
redis> INCRBY demo:page:views 10                          -> 11
       # a line starting with '#' is an explanation, not a command
```

Read this guide alongside the code. Each section below explains one example class: what the data type is, when to use it, what each command does, and the output you should see.

---

## Contents

1. [Quick start](#1-quick-start)
2. [Redis in 2 minutes](#2-redis-in-2-minutes)
3. [How the code is organized](#3-how-the-code-is-organized)
4. [Connecting: RedisConnection](#4-connecting-redisconnection)
5. [Strings: StringsExample](#5-strings-stringsexample)
6. [Lists: ListsExample](#6-lists-listsexample)
7. [Hashes: HashesExample](#7-hashes-hashesexample)
8. [Sets: SetsExample](#8-sets-setsexample)
9. [Sorted Sets: SortedSetsExample](#9-sorted-sets-sortedsetsexample)
10. [Pub/Sub: PubSubExample](#10-pubsub-pubsubexample)
11. [How the tests work](#11-how-the-tests-work)
12. [Which data type should I use?](#12-which-data-type-should-i-use)
13. [Exercises](#13-exercises)
14. [Troubleshooting](#14-troubleshooting)

---

## 1. Quick start

Run these from the project root (`redis-practice/`):

```bash
docker compose up -d                       # start Redis and Redis Insight
mvn -pl redis-jedis-examples exec:java     # run all the examples
mvn -pl redis-jedis-examples test          # run the tests
```

Then open **Redis Insight** at <http://localhost:5540>, click the **redis-practice** database, and you'll see the `demo:*` keys the examples created. Click a key to see its value.

---

## 2. Redis in 2 minutes

- **Redis is an in-memory key-value store.** Every piece of data lives under a **key** (a string such as `demo:user:1`). Because data is kept in RAM, reads and writes take well under a millisecond.
- **Each key has a type.** The value can be a String, List, Hash, Set, Sorted Set, and more. The type decides which commands you can use on the key. For example, `LPUSH` only works on lists.
- **Keys are created on first write and removed when empty.** You never "create a list" first. `RPUSH demo:queue job1` creates the list if it doesn't exist. If you pop the last item, the key disappears.
- **Key naming convention:** use `:` to build a hierarchy, like `object-type:id:field`. Redis doesn't care, but tools like Redis Insight group keys by `:` in tree view. All examples here use the `demo:` prefix.
- **Commands are atomic.** Each command runs completely before the next one starts. Redis runs commands one at a time, so `INCR` never loses an update, even with many clients.
- **Numbered databases:** a Redis server has databases `0` to `15`. The examples use **0**, and the tests use **15** so they don't interfere with each other.

---

## 3. How the code is organized

```
src/main/java/com/practice/redis/jedis/
├── RedisConnection.java     creates the connection pool (localhost:6379)
├── CommandLogger.java       prints "redis> COMMAND -> result"
├── StringsExample.java      ┐
├── ListsExample.java        │
├── HashesExample.java       │ one class per data type
├── SetsExample.java         │
├── SortedSetsExample.java   │
├── PubSubExample.java       ┘
└── Main.java                runs every example in order

src/test/java/com/practice/redis/jedis/
├── RedisTestSupport.java    shared test setup (database 15, cleanup)
└── *ExampleTest.java        one test per example
```

**Every example class follows the same pattern:**

```java
public class StringsExample {
    private final Jedis jedis;                 // one connection, passed in

    public StringsExample(Jedis jedis) { ... }

    public void run() { ... }                  // calls every demo method; used by Main

    public String setAndGet() { ... }          // one small demo per method,
    public long counter() { ... }              // each RETURNS a value so the
                                               // tests can check it
}
```

**Every command is wrapped in `log(...)`:**

```java
String value = log("GET demo:greeting", jedis.get("demo:greeting"));
//                 ^ text to print        ^ the real Jedis call
```

`log` prints the command and its result, then returns the result unchanged. The first argument is only a label for display, and the second is the actual call to Redis.

**Each demo method begins with `DEL`** on the keys it uses. That way it gives the same output however many times you run it. Without it, a list would grow longer on every run.

**Jedis method names match Redis command names:** `SET` → `jedis.set`, `HGETALL` → `jedis.hgetAll`, `ZREVRANGE` → `jedis.zrevrange`. If you know the Redis command, you can usually guess the Java method.

---

## 4. Connecting: `RedisConnection`

```java
try (Jedis jedis = RedisConnection.getPool().getResource()) {
    jedis.set("key", "value");
}   // close() gives the connection BACK to the pool; it doesn't disconnect
```

**Why a pool?** A `Jedis` object is one TCP connection, and it is **not thread-safe**: two threads must never share it. Opening a new connection for every command would be slow. A **`JedisPool`** keeps a few connections open and lends one out whenever you ask.

| Setting | Value | Meaning |
|---|---|---|
| `maxTotal` | 8 | At most 8 connections open at once |
| `maxIdle` | 8 | Keep up to 8 unused connections ready for reuse |
| `minIdle` | 0 | Don't open connections before they're needed |
| `testOnBorrow` | true | Ping a connection before lending it out, so you never get a dead one |

`getPool()` returns one shared pool for database 0. `createPool(15)` makes a separate pool for database 15, which the tests use.

**Rule of thumb:** always borrow a connection with `try (Jedis jedis = pool.getResource())` so it is returned to the pool automatically.

---

## 5. Strings: `StringsExample`

**What it is:** the simplest type. One key holds one value, which can be text, a number, or binary data up to 512 MB.

**Use it for:** caching (`user:42:profile` → JSON), counters, feature flags, session tokens, rate limits.

### `setAndGet()`: store, extend, and read a value

```
redis> DEL demo:greeting                                  -> 0
redis> SET demo:greeting "Hello"                          -> OK
redis> APPEND demo:greeting ", Redis!"                    -> 13
redis> GET demo:missing                                   -> (nil)
redis> GET demo:greeting                                  -> "Hello, Redis!"
```

- `DEL` returns how many keys it deleted. It returns `0` the first time because the key didn't exist yet.
- `APPEND` returns the **new length** of the string (13 characters).
- `GET` on a key that doesn't exist returns `(nil)`, which is `null` in Java.

### `counter()`: atomic counters

```
redis> INCR demo:page:views                               -> 1
redis> INCRBY demo:page:views 10                          -> 11
redis> DECR demo:page:views                               -> 10
```

- `INCR` on a missing key treats it as `0`, so the first call returns `1`.
- The value is stored as a string, but Redis treats it as an integer for these commands.
- **Why this matters:** in plain Java, `count = get(); count++; set(count)` loses updates when two servers do it at the same time. `INCR` does the read and the write as one atomic step.

### `expiringKey()`: keys that delete themselves

```
redis> SET demo:session:abc "user-42" EX 60               -> OK
redis> TTL demo:session:abc                               -> 60
```

- `EX 60` means "delete this key automatically after 60 seconds". In Java this is `SetParams.setParams().ex(60)`.
- `TTL` shows the seconds left. `-1` means the key has no expiry, and `-2` means the key doesn't exist.
- This is why **you won't find `demo:session:abc` in Redis Insight** a minute after running the examples. Redis already removed it.
- **Use it for:** login sessions, one-time passwords, cache entries that must refresh.

### `multipleKeys()`: several keys in one round trip

```
redis> MSET demo:color red demo:size L                    -> OK
redis> MGET demo:color demo:size demo:missing             -> [red, L, null]
```

- One `MGET` is one network round trip instead of three separate `GET` calls.
- Missing keys come back as `null` in the same position.

---

## 6. Lists: `ListsExample`

**What it is:** an ordered sequence of strings. Adding or removing at either end (**left** = head, **right** = tail) is very fast. Index `0` is the first element and `-1` is the last.

**Use it for:** job queues, stacks, activity feeds, "last N" lists.

```
 LPUSH / LPOP  →  [ head ... tail ]  ←  RPUSH / RPOP
   (left)                                  (right)
```

### `queue()`: first in, first out (FIFO)

```
redis> RPUSH demo:queue job1 job2 job3                    -> 3
redis> LRANGE demo:queue 0 -1                             -> [job1, job2, job3]
redis> LPOP demo:queue                                    -> "job1"
redis> LLEN demo:queue                                    -> 2
```

- Add at the **right** with `RPUSH` and take from the **left** with `LPOP`, so jobs come out in the order they went in.
- `RPUSH` returns the new length of the list.
- `LRANGE key 0 -1` means "from the first element to the last", in other words the whole list.

### `stack()`: last in, first out (LIFO)

```
redis> LPUSH demo:stack a b c                             -> 3
redis> LRANGE demo:stack 0 -1                             -> [c, b, a]
redis> LPOP demo:stack                                    -> "c"
```

- `LPUSH a b c` inserts `a`, then `b` in front of it, then `c` in front of that. So the list reads `c, b, a`.
- Pushing and popping on the **same** side gives a stack: the last item in is the first out.

### `latestItems()`: a capped list of recent items

```
redis> LPUSH demo:recent home                             -> 1
redis> LPUSH demo:recent about                            -> 2
...
redis> LPUSH demo:recent blog                             -> 5
redis> LTRIM demo:recent 0 2                              -> OK
redis> LRANGE demo:recent 0 -1                            -> [blog, docs, pricing]
```

- New items go to the head, so the newest is always first.
- `LTRIM 0 2` keeps only indexes 0 to 2 and deletes everything else.
- **Use it for:** "recently viewed products" or "last 100 log lines". Push, then trim, and the list never grows past its limit.

---

## 7. Hashes: `HashesExample`

**What it is:** one key holding a small map of **field → value** pairs, like a Java object or a database row.

**Use it for:** user profiles, product details, settings. You can read or change **one field** without loading or rewriting the whole object, which you can't do if you store the object as one JSON string.

```
demo:user:1  ──►  { name: "Alice", email: "alice@example.com", age: "30" }
```

### `storeObject()`: write and read an object

```
redis> HSET demo:user:1 name Alice email alice@example.com age 30 -> 3
redis> HGET demo:user:1 name                              -> "Alice"
redis> HGETALL demo:user:1                                -> {name=Alice, email=alice@example.com, age=30}
```

- `HSET` with a Java `Map` writes several fields at once. It returns how many **new** fields were added (3).
- `HGET` reads one field, and `HGETALL` returns every field as a `Map<String, String>`.
- All values are strings. `age` is stored as `"30"`.

### `updateFields()`: change parts of the object

```
redis> HINCRBY demo:user:1 age 1                          -> 31
redis> HEXISTS demo:user:1 email                          -> true
redis> HDEL demo:user:1 email                             -> 1
redis> HKEYS demo:user:1                                  -> [name, age]
redis> HLEN demo:user:1                                   -> 2
```

- `HINCRBY` is an atomic counter inside a hash, like `INCR` for a single field.
- `HDEL` removes a field. `HKEYS` lists the field names, and `HLEN` counts them.

---

## 8. Sets: `SetsExample`

**What it is:** an **unordered** collection of **unique** strings. Adding something already there does nothing.

**Use it for:** tags, unique visitors, "users who liked this post", and comparing groups.

### `basics()`: uniqueness and membership

```
       # 'java' is passed twice, but a set stores it only once
redis> SADD demo:tags java redis java docker              -> 3
redis> SISMEMBER demo:tags redis                          -> true
redis> SISMEMBER demo:tags python                         -> false
redis> SCARD demo:tags                                    -> 3
redis> SMEMBERS demo:tags                                 -> [java, redis, docker]
```

- `SADD` returns how many members were **actually added** (3, not 4) because the duplicate `java` was ignored.
- `SISMEMBER` checks membership very quickly, no matter how big the set is.
- `SCARD` returns the size of the set ("cardinality").
- `SMEMBERS` may return members in **any order**. Never rely on set order. That's why the test compares the result as a `Set`.

### `setOperations()`: compare two sets

```
demo:skills:alice = { java, redis, docker }
demo:skills:bob   = { python, redis, docker }
```

```
redis> SINTER demo:skills:alice demo:skills:bob           -> [redis, docker]
redis> SUNION demo:skills:alice demo:skills:bob           -> [python, java, redis, docker]
redis> SDIFF demo:skills:alice demo:skills:bob            -> [java]
```

| Command | Question it answers | Result |
|---|---|---|
| `SINTER` (intersection) | What do **both** have? | redis, docker |
| `SUNION` (union) | What does **either** have? | python, java, redis, docker |
| `SDIFF` (difference) | What does Alice have that Bob **doesn't**? | java |

**Real-world use:** "mutual friends" with `SINTER`, or "recommend skills Bob doesn't have yet" with `SDIFF`.

---

## 9. Sorted Sets: `SortedSetsExample`

**What it is:** a set of unique members where each member has a numeric **score**. Redis always keeps the members **sorted by score**.

**Use it for:** leaderboards, priority queues, rankings, and time-ordered data (use a timestamp as the score).

### `leaderboard()`: rank players by score

```
redis> ZADD demo:leaderboard 100 alice 85 bob 120 carol 70 dave -> 4
redis> ZINCRBY demo:leaderboard 30 bob                    -> 115.0
redis> ZSCORE demo:leaderboard carol                      -> 120.0
       # ZREVRANK = position counted from the highest score (0 = first place)
redis> ZREVRANK demo:leaderboard bob                      -> 1
redis> ZREVRANGE demo:leaderboard 0 2 WITHSCORES          -> [[carol,120.0], [bob,115.0], [alice,100.0]]
redis> ZREVRANGE demo:leaderboard 0 2                     -> [carol, bob, alice]
```

- `ZADD` adds members with scores. In Java you pass a `Map<String, Double>`.
- `ZINCRBY` adds to a member's score atomically. Bob goes from 85 to 115, and the order updates automatically.
- Scores are doubles, which is why you see `115.0`.
- Commands come in pairs: the plain version counts **low → high**, and the `REV` version counts **high → low**. For a leaderboard you usually want `ZREVRANGE` and `ZREVRANK`, so the highest score comes first.
- `ZREVRANK bob` → `1` means second place, because ranks start at 0.
- `ZREVRANGE 0 2` returns the **top 3**. Add `WITHSCORES` to get the scores too.

Leaderboard after `ZINCRBY`:

| Rank (ZREVRANK) | Player | Score |
|---|---|---|
| 0 | carol | 120 |
| 1 | bob | 115 |
| 2 | alice | 100 |
| 3 | dave | 70 |

### `rangeByScore()`: query by score instead of position

This method rebuilds the leaderboard first, so Bob is back to 85.

```
redis> ZCOUNT demo:leaderboard 90 130                     -> 2
redis> ZRANGEBYSCORE demo:leaderboard 90 130              -> [alice, carol]
```

- "Who scored between 90 and 130?" Alice (100) and Carol (120). Bob (85) and Dave (70) are outside the range.
- **Use it for:** "events in the last hour", with timestamps as scores and `ZRANGEBYSCORE now-3600 now`.

---

## 10. Pub/Sub: `PubSubExample`

**What it is:** messaging, not storage. **Publishers** send messages to a **channel**, and every client **subscribed** to that channel at that moment receives them.

```
                  PUBLISH demo:news "hi"
  Publisher  ─────────────────────────────►  Redis  ───►  Subscriber(s)
                                         (channel: demo:news)
```

**Important properties:**
- **Fire and forget.** Messages are not saved. A subscriber that connects later never sees earlier messages. That's why you won't find any `demo:news` key in Redis Insight.
- **`SUBSCRIBE` blocks its connection.** Once a connection subscribes, it sits waiting for messages and can't run other commands. So the subscriber needs **its own connection on its own thread**. That's why this class takes a `JedisPool` instead of a single `Jedis`.

### `publishAndReceive(channel, messages)`: step by step

```java
// 1. Start a subscriber thread with its own connection
//    (subscribe() blocks until we unsubscribe)
// 2. Wait until the subscription is active, using a CountDownLatch.
//    Otherwise the first messages could be published before anyone is listening, and be lost.
// 3. Publish each message using a different connection
// 4. Wait until all messages arrive, then unsubscribe so the thread can finish
```

Output:

```
       # subscriber joined channel 'demo:news'
redis> PUBLISH demo:news "Redis 101 starts now"           -> 1
       # subscriber received on 'demo:news': "Redis 101 starts now"
redis> PUBLISH demo:news "Lists are fun"                  -> 1
       # subscriber received on 'demo:news': "Lists are fun"
       # subscriber received on 'demo:news': "Goodbye!"
redis> PUBLISH demo:news "Goodbye!"                       -> 1
```

- `PUBLISH` returns **how many subscribers received** the message (1 here). `0` means nobody was listening, and the message is gone.
- `JedisPubSub` is a callback object. Jedis calls `onSubscribe` and `onMessage` on the subscriber thread as events arrive.
- **Why "received" sometimes prints before "PUBLISH":** two threads print at the same time. The subscriber can print its line before the publisher prints the `PUBLISH` result. This is normal concurrency, not a bug.

**Use it for:** live notifications, chat, telling other servers to clear a cache. If messages must not be lost, look at **Redis Streams** instead. Streams store messages and let consumers catch up.

---

## 11. How the tests work

Each example has one JUnit 5 test, such as `StringsExampleTest`. They all extend **`RedisTestSupport`**, which:

1. **Uses database 15**, so tests never touch the `demo:*` keys from `Main` in database 0.
2. **Runs `FLUSHDB` before every test.** This empties database 15 only, so each test starts clean and tests don't affect each other.
3. **Skips instead of failing** when Redis isn't running, using a JUnit `Assumption`. The skip message tells you to run `docker compose up -d`.

The tests call the same demo methods that `Main` calls and check the returned values:

```java
assertEquals("Hello, Redis!", example.setAndGet());
assertEquals(List.of("carol", "bob", "alice"), example.leaderboard());
```

Run a single test class:

```bash
mvn -pl redis-jedis-examples test -Dtest=SortedSetsExampleTest
```

---

## 12. Which data type should I use?

| I need to... | Use | Key commands |
|---|---|---|
| Cache a value or JSON blob | **String** | `SET` (with `EX`), `GET` |
| Count things (views, likes, rate limits) | **String** | `INCR`, `INCRBY` |
| Expire data automatically | any type + TTL | `SET ... EX`, `EXPIRE`, `TTL` |
| Process jobs in order | **List** | `RPUSH` + `LPOP` |
| Keep the latest N items | **List** | `LPUSH` + `LTRIM` |
| Store an object and update single fields | **Hash** | `HSET`, `HGET`, `HINCRBY` |
| Track unique items / membership | **Set** | `SADD`, `SISMEMBER` |
| Find common or different members | **Set** | `SINTER`, `SUNION`, `SDIFF` |
| Rank things by a number | **Sorted Set** | `ZADD`, `ZINCRBY`, `ZREVRANGE` |
| Query by a numeric or time range | **Sorted Set** | `ZRANGEBYSCORE` |
| Broadcast live messages | **Pub/Sub** | `PUBLISH`, `SUBSCRIBE` |

---

## 13. Exercises

Try these in Redis Insight's **CLI** or **Workbench**, or add them to an example class and run `Main` again.

1. **Strings:** `SET demo:otp 123456 EX 10`, then run `TTL demo:otp` every few seconds until it returns `-2`.
2. **Lists:** make the queue blocking. In one CLI window run `BLPOP demo:jobs 0`, which waits forever. In another, run `RPUSH demo:jobs hello` and watch the first window receive it.
3. **Hashes:** add a `city` field to `demo:user:1` with `HSET`, then read only `name` and `city` with `HMGET`.
4. **Sets:** create `demo:skills:carol` and find skills that **all three** people share with `SINTER` on three keys.
5. **Sorted sets:** give `dave` 60 points with `ZINCRBY`. What is his `ZREVRANK` now?
6. **Pub/Sub:** in one CLI window, run `SUBSCRIBE demo:news`, then run `Main` and watch the messages arrive there too. `PUBLISH` will now return `2`.
7. **Java:** add a method to `StringsExample` that uses `SETNX` (`jedis.setnx`) to set a key only if it doesn't exist yet. This is the basis of a simple lock. Then write a test for it.

---

## 14. Troubleshooting

| Problem | Fix |
|---|---|
| `Could not connect to Redis at localhost:6379` | Start Redis with `docker compose up -d` from the project root. Check with `docker compose ps`. |
| Tests show as **skipped** | Same as above: Redis isn't running. |
| No database listed in Redis Insight | Click **+ Add Redis database** and use host **`redis`**, port `6379`. Don't use `localhost`, because Redis Insight runs inside Docker. |
| `demo:session:abc` is missing | Expected: it expires 60 seconds after `Main` runs. |
| Can't find test keys in Redis Insight | Tests use **database 15**. Add a connection with database index 15 to see them. |
| Want a completely fresh start | `docker compose down -v`, then `docker compose up -d`. This **deletes all data**. |
