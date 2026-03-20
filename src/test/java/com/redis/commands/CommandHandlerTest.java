package com.redis.commands;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redis.core.RedisStore;
import com.redis.protocol.RespValue;

public class CommandHandlerTest {
    private RedisStore store;
    private CommandDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        store = new RedisStore();
        dispatcher = new CommandDispatcher(store, null);
    }

    private RespValue dispatch(String... parts) {
        List<RespValue> array = java.util.Arrays.stream(parts)
                .map(RespValue::bulkString)
                .toList();
        return dispatcher.dispatch(RespValue.array(array));
    }

    // ── PING ──

    @Test
    void pingReturnsOk() {
        RespValue result = dispatch("PING");
        assertEquals(RespValue.Type.SIMPLE_STRING, result.getType());
        assertEquals("PONG", result.getStrValue());
    }

    @Test
    void pingWithMessageEchoes() {
        RespValue result = dispatch("PING", "hello");
        assertEquals("hello", result.getStrValue());
    }

    // ── ECHO ──

    @Test
    void echoReturnsMessage() {
        RespValue result = dispatch("ECHO", "hello");
        assertEquals(RespValue.Type.BULK_STRING, result.getType());
        assertEquals("hello", result.getStrValue());
    }

    @Test
    void echoNoArgsReturnsError() {
        RespValue result = dispatch("ECHO");
        assertEquals(RespValue.Type.ERROR, result.getType());
    }

    // ── SET / GET ──

    @Test
    void setAndGet() {
        dispatch("SET", "foo", "bar");
        RespValue result = dispatch("GET", "foo");
        assertEquals(RespValue.Type.BULK_STRING, result.getType());
        assertEquals("bar", result.getStrValue());
    }

    @Test
    void getReturnsNullForMissingKey() {
        RespValue result = dispatch("GET", "missing");
        assertEquals(RespValue.Type.NULL, result.getType());
    }

    @Test
    void setOverwritesExistingValue() {
        dispatch("SET", "key", "first");
        dispatch("SET", "key", "second");
        RespValue result = dispatch("GET", "key");
        assertEquals("second", result.getStrValue());
    }

    @Test
    void setWithExExpiry() throws InterruptedException {
        dispatch("SET", "temp", "value", "EX", "1");
        assertEquals("value", dispatch("GET", "temp").getStrValue());
        Thread.sleep(1100);
        assertEquals(RespValue.Type.NULL, dispatch("GET", "temp").getType());
    }

    @Test
    void setWithPxExpiry() throws InterruptedException {
        dispatch("SET", "temp", "value", "PX", "500");
        assertEquals("value", dispatch("GET", "temp").getStrValue());
        Thread.sleep(600);
        assertEquals(RespValue.Type.NULL, dispatch("GET", "temp").getType());
    }

    // ── DEL ──

    @Test
    void delExistingKeys() {
        dispatch("SET", "a", "1");
        dispatch("SET", "b", "2");
        RespValue result = dispatch("DEL", "a", "b", "c");
        assertEquals(RespValue.Type.INTEGER, result.getType());
        assertEquals(2, result.getLongValue());
    }

    @Test
    void delNonExistentKey() {
        RespValue result = dispatch("DEL", "ghost");
        assertEquals(0, result.getLongValue());
    }

    // ── EXISTS ──

    @Test
    void existsCountsExistingKeys() {
        dispatch("SET", "a", "1");
        dispatch("SET", "b", "2");
        RespValue result = dispatch("EXISTS", "a", "b", "c");
        assertEquals(2, result.getLongValue());
    }

    @Test
    void existsReturnZeroForMissingKey() {
        RespValue result = dispatch("EXISTS", "nope");
        assertEquals(0, result.getLongValue());
    }

    // ── EXPIRE / TTL ──

    @Test
    void expireOnExistingKey() {
        dispatch("SET", "key", "val");
        RespValue result = dispatch("EXPIRE", "key", "10");
        assertEquals(1, result.getLongValue());
    }

    @Test
    void expireOnMissingKey() {
        RespValue result = dispatch("EXPIRE", "missing", "10");
        assertEquals(0, result.getLongValue());
    }

    @Test
    void expireInvalidSeconds() {
        dispatch("SET", "key", "val");
        RespValue result = dispatch("EXPIRE", "key", "notanumber");
        assertEquals(RespValue.Type.ERROR, result.getType());
    }

    @Test
    void ttlWithExpiry() {
        dispatch("SET", "key", "val");
        dispatch("EXPIRE", "key", "100");
        RespValue result = dispatch("TTL", "key");
        assertTrue(result.getLongValue() > 0 && result.getLongValue() <= 100);
    }

    @Test
    void ttlNoExpiry() {
        dispatch("SET", "key", "val");
        RespValue result = dispatch("TTL", "key");
        assertEquals(-1, result.getLongValue());
    }

    @Test
    void ttlMissingKey() {
        RespValue result = dispatch("TTL", "missing");
        assertEquals(-2, result.getLongValue());
    }

    // ── KEYS ──

    @Test
    void keysWildcard() {
        dispatch("SET", "foo", "1");
        dispatch("SET", "bar", "2");
        dispatch("SET", "baz", "3");
        RespValue result = dispatch("KEYS", "*");
        assertEquals(RespValue.Type.ARRAY, result.getType());
        assertEquals(3, result.getArrayValue().size());
    }

    @Test
    void keysWithPattern() {
        dispatch("SET", "hello", "1");
        dispatch("SET", "hallo", "2");
        dispatch("SET", "world", "3");
        RespValue result = dispatch("KEYS", "h?llo");
        assertEquals(2, result.getArrayValue().size());
    }

    @Test
    void keysNoMatch() {
        dispatch("SET", "foo", "1");
        RespValue result = dispatch("KEYS", "zzz*");
        assertEquals(0, result.getArrayValue().size());
    }

    // ── TYPE ──

    @Test
    void typeReturnsString() {
        dispatch("SET", "key", "val");
        RespValue result = dispatch("TYPE", "key");
        assertEquals("string", result.getStrValue());
    }

    // ── FLUSHALL ──

    @Test
    void flushAllClearsStore() {
        dispatch("SET", "a", "1");
        dispatch("SET", "b", "2");
        dispatch("FLUSHALL");
        RespValue result = dispatch("DBSIZE");
        assertEquals(0, result.getLongValue());
    }

    // ── DBSIZE ──

    @Test
    void dbSizeReturnsCount() {
        dispatch("SET", "a", "1");
        dispatch("SET", "b", "2");
        RespValue result = dispatch("DBSIZE");
        assertEquals(2, result.getLongValue());
    }

    @Test
    void dbSizeEmptyStore() {
        RespValue result = dispatch("DBSIZE");
        assertEquals(0, result.getLongValue());
    }

    // ── COMMAND ──

    @Test
    void commandReturnsArray() {
        RespValue result = dispatch("COMMAND");
        assertEquals(RespValue.Type.ARRAY, result.getType());
        assertTrue(result.getArrayValue().size() > 0);
    }

    @Test
    void commandCountReturnsInteger() {
        RespValue result = dispatch("COMMAND", "COUNT");
        assertEquals(RespValue.Type.INTEGER, result.getType());
        assertTrue(result.getLongValue() > 0);
    }

    // ── Unknown command ──

    @Test
    void unknownCommandReturnsError() {
        RespValue result = dispatch("FOOBAR");
        assertEquals(RespValue.Type.ERROR, result.getType());
    }

    // ── Case insensitivity ──

    @Test
    void commandsAreCaseInsensitive() {
        dispatch("set", "key", "val");
        RespValue result = dispatch("get", "key");
        assertEquals("val", result.getStrValue());
    }
}