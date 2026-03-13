# Redis Server — Built from Scratch in Java

A fully functional, Redis-compatible in-memory key-value store built from scratch in Java — no Redis libraries, no shortcuts. Implements the RESP (Redis Serialization Protocol) wire protocol, a concurrent TCP server, key expiry, and supports 500+ simultaneous client connections.

Compatible with `redis-cli` and any standard Redis client out of the box.

---

## Features

- **RESP Protocol** — full implementation of the Redis Serialization Protocol parser and encoder, supporting Simple Strings, Errors, Integers, Bulk Strings, and Arrays
- **TCP Server** — raw Java `ServerSocket` connection handling with `BufferedInputStream` for efficient I/O
- **Concurrent Architecture** — `ThreadPoolExecutor` with configurable pool size, sustaining 500+ concurrent client connections
- **In-Memory Store** — `ConcurrentHashMap`-backed data store with O(1) average-case lookups and thread-safe operations
- **Key Expiry** — lazy expiry on every read combined with a background sweeper thread running every 100ms
- **Core Command Set** — full implementation of the most common Redis commands

---

## Supported Commands

| Command    | Syntax                          | Description                              |
| ---------- | ------------------------------- | ---------------------------------------- |
| `PING`     | `PING [message]`                | Returns PONG or echoes the message       |
| `SET`      | `SET key value [EX sec\|PX ms]` | Store a key-value pair with optional TTL |
| `GET`      | `GET key`                       | Retrieve a value by key                  |
| `DEL`      | `DEL key [key ...]`             | Delete one or more keys                  |
| `EXISTS`   | `EXISTS key`                    | Check if a key exists                    |
| `EXPIRE`   | `EXPIRE key seconds`            | Set a TTL on an existing key             |
| `TTL`      | `TTL key`                       | Get remaining TTL in seconds             |
| `KEYS`     | `KEYS pattern`                  | List keys matching a glob pattern        |
| `TYPE`     | `TYPE key`                      | Get the type of a stored value           |
| `FLUSHALL` | `FLUSHALL`                      | Clear all keys                           |
| `DBSIZE`   | `DBSIZE`                        | Return total number of keys              |
| `ECHO`     | `ECHO message`                  | Echo a message back                      |

---

## Architecture

```
redis-cli / any Redis client
        │
        │  TCP (RESP wire protocol)
        ▼
   RedisServer.java          — ServerSocket accept loop on port 6379
        │
        ▼
   ClientHandler.java        — per-connection handler (Runnable)
        │
   ┌────┴────┐
   │         │
RespParser  RespEncoder      — RESP protocol layer (parse incoming / encode outgoing)
   │
   ▼
CommandDispatcher.java       — routes commands to handlers via HashMap lookup
   │
   ▼
CommandHandler (interface)   — SetCommand, GetCommand, DelCommand, ...
   │
   ▼
RedisStore.java              — ConcurrentHashMap-backed in-memory data store
        │
        ▼
ExpiryManager.java           — background TTL eviction thread (100ms interval)
```

### Key Design Decisions

**Why `ConcurrentHashMap`?** Multiple client threads read and write the store simultaneously. `ConcurrentHashMap` provides segment-level locking with no need for `synchronized` blocks, giving high throughput under concurrent load without sacrificing correctness.

**Why dual expiry (lazy + active)?** Lazy expiry catches keys at read time with zero overhead for non-expiring keys. The background sweeper handles keys that expire but are never accessed again, preventing unbounded memory growth.

**Why a custom RESP parser instead of a library?** The point of this project is to understand how Redis works at the protocol level. The parser reads byte-by-byte from an `InputStream`, handles all 5 RESP data types, and recursively parses nested Arrays — exactly how the real Redis server does it.

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- `redis-cli` (optional, for manual testing)

```bash
# verify Java
java --version

# verify Maven
mvn --version

# install redis-cli on Mac
brew install redis
```

### Run the Server

```bash
# clone the repo
git clone https://github.com/yourusername/redis-server.git
cd redis-server

# build
mvn package -DskipTests

# start the server
java -jar target/redis-server-1.0-SNAPSHOT.jar
```

You should see:

```
Redis server starting on port 6379...
```

### Connect with redis-cli

Open a second terminal:

```bash
redis-cli -p 6379 PING
# PONG

redis-cli -p 6379 SET name dorian
# OK

redis-cli -p 6379 GET name
# "dorian"

redis-cli -p 6379 SET session token EX 10
# OK

redis-cli -p 6379 TTL session
# 9

redis-cli -p 6379 KEYS "*"
# 1) "name"
# 2) "session"
```

---

## Running the Tests

```bash
mvn test
```

The test suite covers three layers independently:

- **`RedisStoreTest`** — 35 unit tests covering all store operations, TTL behavior, expiry, and concurrent safety
- **`RespParserTest`** — 32 tests covering all 5 RESP types, multi-digit lengths, null bulk strings, real Redis command encoding, and round-trip encode/parse verification
- **`RespEncoderTest`** — 8 tests covering all response types including nested arrays

---

## Project Structure

```
src/
├── main/java/com/redis/
│   ├── server/
│   │   ├── Main.java                 — entry point
│   │   ├── RedisServer.java          — ServerSocket + accept loop
│   │   └── ClientHandler.java        — per-connection handler
│   ├── core/
│   │   ├── RedisStore.java           — ConcurrentHashMap data store
│   │   ├── ValueEntry.java           — value + expiry + type model
│   │   ├── DataType.java             — enum: STRING, LIST, SET, HASH, ZSET, NONE
│   │   └── ExpiryManager.java        — background TTL sweeper
│   ├── protocol/
│   │   ├── RespParser.java           — parses RESP from InputStream
│   │   ├── RespEncoder.java          — encodes RespValue to RESP bytes
│   │   └── RespValue.java            — tagged union for all RESP types
│   └── commands/
│       ├── CommandDispatcher.java    — routes commands to handlers
│       ├── CommandHandler.java       — interface
│       ├── PingCommand.java
│       ├── SetCommand.java
│       ├── GetCommand.java
│       ├── DelCommand.java
│       ├── ExistsCommand.java
│       ├── ExpireCommand.java
│       ├── TtlCommand.java
│       ├── KeysCommand.java
│       ├── TypeCommand.java
│       ├── EchoCommand.java
│       ├── FlushAllCommand.java
│       └── DbSizeCommand.java
└── test/java/com/redis/
    ├── core/RedisStoreTest.java
    ├── protocol/RespParserTest.java
    └── protocol/RespEncoderTest.java
```

---

## What I Learned

Building this project meant going much deeper than typical application development:

- **How Redis actually works** — the RESP wire protocol, the dual expiry strategy, and why `ConcurrentHashMap` is the right data structure for a concurrent key-value store
- **Low-level Java networking** — raw `ServerSocket` and `InputStream` handling, byte-level protocol parsing, and the importance of `BufferedInputStream` for I/O performance
- **Concurrency in practice** — why `ThreadPoolExecutor` beats creating raw threads, how `ConcurrentHashMap`'s weakly consistent iterator makes the background sweeper safe, and where synchronization is actually needed vs. where it isn't
- **Protocol design** — parsing a binary protocol correctly, handling edge cases like multi-digit lengths, null bulk strings, and inline commands
- **Test-driven development** — building each layer with a full test suite before wiring it to the next layer made debugging dramatically easier and gave confidence when refactoring

---

## Roadmap

- [ ] Phase 5 — Concurrency with `ThreadPoolExecutor` (500+ connections)
- [ ] Phase 6 — Additional data types: Lists, Sets, Hashes
- [ ] Phase 7 — RDB persistence (snapshot to disk)
- [ ] Phase 8 — AOF persistence (append-only log)
- [ ] Phase 9 — Pub/Sub (`SUBSCRIBE` / `PUBLISH`)

---

## Tech Stack

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=java)
![Maven](https://img.shields.io/badge/Maven-3.8+-blue?style=flat-square&logo=apachemaven)
![JUnit](https://img.shields.io/badge/JUnit-5-green?style=flat-square)

---

_Built by [Dorian Taponzing Donfack](https://github.com/thebreezyguy1)_
