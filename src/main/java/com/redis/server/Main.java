package com.redis.server;

import java.io.IOException;

import com.redis.commands.CommandDispatcher;
import com.redis.core.ExpiryManager;
import com.redis.core.RedisStore;

public class Main {
    public static void main(String[] args) throws IOException {
        RedisStore store = new RedisStore();

        ExpiryManager expiryManager = new ExpiryManager(store);
        expiryManager.start();

        Runtime.getRuntime().addShutdownHook(new Thread(expiryManager::stop));

        CommandDispatcher dispatcher = new CommandDispatcher(store);
        RedisServer server = new RedisServer(dispatcher);

        System.out.println("Redis server starting on port 6379...");
        server.start();
    }
}