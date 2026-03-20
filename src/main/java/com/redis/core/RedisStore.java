package com.redis.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RedisStore {
    private ConcurrentHashMap<String, ValueEntry> store;

    public RedisStore() {
        store = new ConcurrentHashMap<>();
    };

    public void set(String key, String value) {
        if (key != null) {
            ValueEntry valueEntry = new ValueEntry(value, -1, DataType.STRING);
            store.put(key, valueEntry);
        }
    };

    public void set(String key, String value, long ttlMillis) {
        if (key != null) {
            long expiresAt = System.currentTimeMillis() + ttlMillis;
            ValueEntry valueEntry = new ValueEntry(value, expiresAt, DataType.STRING);
            store.put(key, valueEntry);
        }
    }

    public String get(String key) {
        if (key == null) return null;
        ValueEntry entry = store.get(key);
        if (entry == null) return null;
        if (isExpired(key)) {
            store.remove(key);
            return null;
        }
        return entry.getValue();
    }

    public int del(String... keys) {
        int count = 0;
        for (int i = 0; i < keys.length; i++) {
            if (store.remove(keys[i]) != null) count++;
        }
        return count;
    }

    public boolean exists(String key) {
        if (key == null) return false;
        if (!store.containsKey(key)) return false;
        if (isExpired(key)) {
            store.remove(key);
            return false;
        }
        return true;
    }

    public List<String> keys(String pattern) {
        String regex = globToRegex(pattern);
        Pattern compiled = Pattern.compile(regex);

        return new ArrayList<>(store.keySet())
                .stream()
                .filter(key -> !isExpired(key))
                .filter(key -> compiled.matcher(key).matches())
                .collect(Collectors.toList());
    }

    private String globToRegex(String glob) {
        StringBuilder sb = new StringBuilder("^"); // anchor to start
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);

            switch (c) {
                case '*' -> sb.append(".*");
                case '?' -> sb.append(".");
                case '.', '(', ')', '[', ']', '{', '}', '^', '$', '|', '+', '\\' -> sb.append("\\").append(c);
                default -> sb.append(c);
            }
        }
        sb.append("$"); // anchor to end
        return sb.toString();
    }

    private boolean isExpired(String key) {
        ValueEntry entry = store.get(key);
        if (entry == null) return true;
        if (entry.getExpiresAt() == -1) return false;
        return System.currentTimeMillis() > entry.getExpiresAt();
    }

    public boolean expire(String key, long seconds) {
        ValueEntry entry = store.get(key);
        if (entry == null) return false;
        if (entry.getExpiresAt() != -1 && System.currentTimeMillis() > entry.getExpiresAt()) {
            store.remove(key);
            return false;
        }
        entry.setExpiresAt(System.currentTimeMillis() + (seconds * 1000));
        return true;
    }

    public long ttl(String key) {
        ValueEntry entry = store.get(key);
        if (entry == null) return -2;
        if (entry.getExpiresAt() == -1) return -1;
        if (isExpired(key)) {
            store.remove(key);
            return -2;
        }  
        return (entry.getExpiresAt() - System.currentTimeMillis()) / 1000;
    }

    public DataType type(String key) {
        ValueEntry entry = store.get(key);
        if (entry == null || isExpired(key)) return DataType.NONE;
        return entry.getType();
    }

    public void evictExpiredKeys() {
        long now = System.currentTimeMillis();

        for (Map.Entry<String, ValueEntry> entry: store.entrySet()) {
            if (entry.getValue().getExpiresAt() != -1 && now > entry.getValue().getExpiresAt()) {
                store.remove(entry.getKey());
            }
        }
    }

    public Map<String, ValueEntry> getSnapshot() {
        return new HashMap<>(store);
    }

    public void flushAll() {
        store.clear();
    }

    public int dbSize() {
        return store.size();
    }
}
