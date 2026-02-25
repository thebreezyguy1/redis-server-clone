package com.redis.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RedisStoreTest {

    private RedisStore store;

    @BeforeEach
    void setUp() {
        store = new RedisStore();
    }

    // ── SET / GET ───────────────────────────────────────────────────────

    @Test
    void set_and_get_basic_value() {
        store.set("name", "dorian");
        assertEquals("dorian", store.get("name"));
    }

    @Test
    void get_returns_null_for_missing_key() {
        assertNull(store.get("ghost"));
    }

    @Test
    void set_overwrites_existing_value() {
        store.set("name", "dorian");
        store.set("name", "donfack");
        assertEquals("donfack", store.get("name"));
    }

    @Test
    void set_ignores_null_key() {
        assertDoesNotThrow(() -> store.set(null, "value"));
        assertNull(store.get(null));
    }

    @Test
    void set_allows_empty_string_value() {
        store.set("empty", "");
        assertEquals("", store.get("empty"));
    }

    // ── TTL / EXPIRY ────────────────────────────────────────────────────

    @Test
    void get_returns_null_after_ttl_expires() throws InterruptedException {
        store.set("temp", "value", 100);
        Thread.sleep(200);
        assertNull(store.get("temp"));
    }

    @Test
    void get_returns_value_before_ttl_expires() throws InterruptedException {
        store.set("temp", "alive", 500);
        Thread.sleep(100);
        assertEquals("alive", store.get("temp"));
    }

    @Test
    void set_without_ttl_never_expires() throws InterruptedException {
        store.set("persistent", "here");
        Thread.sleep(200);
        assertEquals("here", store.get("persistent"));
    }

    // ── DEL ─────────────────────────────────────────────────────────────

    @Test
    void del_removes_existing_key() {
        store.set("a", "1");
        store.del("a");
        assertNull(store.get("a"));
    }

    @Test
    void del_returns_count_of_deleted_keys() {
        store.set("a", "1");
        store.set("b", "2");
        int deleted = store.del("a", "b", "missing");
        assertEquals(2, deleted);
    }

    @Test
    void del_on_missing_key_returns_zero() {
        assertEquals(0, store.del("ghost"));
    }

    @Test
    void del_multiple_keys_removes_all() {
        store.set("x", "1");
        store.set("y", "2");
        store.set("z", "3");
        store.del("x", "y", "z");
        assertNull(store.get("x"));
        assertNull(store.get("y"));
        assertNull(store.get("z"));
    }

    // ── EXISTS ──────────────────────────────────────────────────────────

    @Test
    void exists_returns_true_for_present_key() {
        store.set("here", "yes");
        assertTrue(store.exists("here"));
    }

    @Test
    void exists_returns_false_for_missing_key() {
        assertFalse(store.exists("ghost"));
    }

    @Test
    void exists_returns_false_for_null_key() {
        assertFalse(store.exists(null));
    }

    @Test
    void exists_returns_false_for_expired_key() throws InterruptedException {
        store.set("temp", "value", 100);
        Thread.sleep(200);
        assertFalse(store.exists("temp"));
    }

    // ── EXPIRE / TTL ────────────────────────────────────────────────────

    @Test
    void expire_sets_ttl_on_existing_key() throws InterruptedException {
        store.set("key", "value");
        boolean result = store.expire("key", 1);
        assertTrue(result);
        Thread.sleep(1100);
        assertNull(store.get("key"));
    }

    @Test
    void expire_returns_false_for_missing_key() {
        assertFalse(store.expire("ghost", 10));
    }

    @Test
    void ttl_returns_minus_one_for_key_with_no_expiry() {
        store.set("persistent", "value");
        assertEquals(-1, store.ttl("persistent"));
    }

    @Test
    void ttl_returns_minus_two_for_missing_key() {
        assertEquals(-2, store.ttl("ghost"));
    }

    @Test
    void ttl_returns_remaining_seconds() throws InterruptedException {
        store.set("key", "value", 5000);
        Thread.sleep(100);
        long remaining = store.ttl("key");
        assertTrue(remaining >= 3 && remaining <= 5);
    }

    @Test
    void ttl_returns_minus_two_after_expiry() throws InterruptedException {
        store.set("temp", "value", 100);
        Thread.sleep(200);
        assertEquals(-2, store.ttl("temp"));
    }

    // ── KEYS ────────────────────────────────────────────────────────────

    @Test
    void keys_star_returns_all_keys() {
        store.set("a", "1");
        store.set("b", "2");
        store.set("c", "3");
        List<String> result = store.keys("*");
        assertEquals(3, result.size());
        assertTrue(result.containsAll(List.of("a", "b", "c")));
    }

    @Test
    void keys_with_prefix_wildcard() {
        store.set("hello", "1");
        store.set("help", "2");
        store.set("world", "3");
        List<String> result = store.keys("hel*");
        assertEquals(2, result.size());
        assertTrue(result.containsAll(List.of("hello", "help")));
        assertFalse(result.contains("world"));
    }

    @Test
    void keys_question_mark_matches_single_char() {
        store.set("hello", "1");
        store.set("hallo", "2");
        store.set("heello", "3");
        List<String> result = store.keys("h?llo");
        assertEquals(2, result.size());
        assertTrue(result.containsAll(List.of("hello", "hallo")));
        assertFalse(result.contains("heello"));
    }

    @Test
    void keys_exact_match() {
        store.set("hello", "1");
        store.set("world", "2");
        List<String> result = store.keys("hello");
        assertEquals(1, result.size());
        assertEquals("hello", result.get(0));
    }

    @Test
    void keys_excludes_expired_keys() throws InterruptedException {
        store.set("live", "1");
        store.set("dead", "2", 100);
        Thread.sleep(200);
        List<String> result = store.keys("*");
        assertTrue(result.contains("live"));
        assertFalse(result.contains("dead"));
    }

    @Test
    void keys_returns_empty_list_when_no_match() {
        store.set("hello", "1");
        List<String> result = store.keys("xyz*");
        assertTrue(result.isEmpty());
    }

    @Test
    void keys_handles_regex_special_chars_in_pattern() {
        store.set("file.txt", "1");
        store.set("filetxt", "2");
        List<String> result = store.keys("file.txt");
        assertEquals(1, result.size());
        assertEquals("file.txt", result.get(0));
    }

    // ── TYPE ────────────────────────────────────────────────────────────

    @Test
    void type_returns_string_for_existing_key() {
        store.set("name", "dorian");
        assertEquals(DataType.STRING, store.type("name"));
    }

    @Test
    void type_returns_none_for_missing_key() {
        assertEquals(DataType.NONE, store.type("ghost"));
    }

    @Test
    void type_returns_none_for_expired_key() throws InterruptedException {
        store.set("temp", "value", 100);
        Thread.sleep(200);
        assertEquals(DataType.NONE, store.type("temp"));
    }

    // ── EVICTION ────────────────────────────────────────────────────────

    @Test
    void evictExpiredKeys_removes_expired_entries() throws InterruptedException {
        store.set("live", "1");
        store.set("dead", "2", 100);
        Thread.sleep(200);
        store.evictExpiredKeys();
        assertNull(store.get("dead"));
        assertEquals("1", store.get("live"));
    }

    @Test
    void evictExpiredKeys_does_not_remove_live_keys() throws InterruptedException {
        store.set("a", "1");
        store.set("b", "2");
        Thread.sleep(50);
        store.evictExpiredKeys();
        assertEquals("1", store.get("a"));
        assertEquals("2", store.get("b"));
    }

    // ── FLUSHALL / DBSIZE ───────────────────────────────────────────────

    @Test
    void flushAll_clears_all_keys() {
        store.set("a", "1");
        store.set("b", "2");
        store.flushAll();
        assertEquals(0, store.dbSize());
        assertNull(store.get("a"));
    }

    @Test
    void dbSize_returns_correct_count() {
        assertEquals(0, store.dbSize());
        store.set("a", "1");
        store.set("b", "2");
        assertEquals(2, store.dbSize());
    }

    @Test
    void dbSize_does_not_count_expired_keys() throws InterruptedException {
        store.set("live", "1");
        store.set("dead", "2", 100);
        Thread.sleep(200);
        store.evictExpiredKeys();
        assertEquals(1, store.dbSize());
    }
}