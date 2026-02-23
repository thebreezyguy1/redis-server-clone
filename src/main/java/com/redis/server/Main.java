package com.redis.server;

import com.redis.core.ExpiryManager;
import com.redis.core.RedisStore;

public class Main {
    public static void main(String[] args) {
        RedisStore store = new RedisStore();

        ExpiryManager expiryManager = new ExpiryManager(store);
        expiryManager.start();

        Runtime.getRuntime().addShutdownHook(new Thread(expiryManager::stop));

        System.out.println("Redis server starting on port 6379...");
    }
}