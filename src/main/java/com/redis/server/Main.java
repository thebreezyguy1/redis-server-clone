package com.redis.server;

import java.io.IOException;

import com.redis.commands.CommandDispatcher;
import com.redis.core.ExpiryManager;
import com.redis.core.RedisStore;
import com.redis.persistence.RdbSnapshot;
import com.redis.util.Config;

public class Main {
    public static void main(String[] args) throws IOException {
        RedisStore store = new RedisStore();

        ExpiryManager expiryManager = new ExpiryManager(store);
        expiryManager.start();

        Runtime.getRuntime().addShutdownHook(new Thread(expiryManager::stop));

        Config config = new Config();
        RdbSnapshot rdb = new RdbSnapshot(store, config);
        rdb.start();

        Runtime.getRuntime().addShutdownHook(new Thread(rdb::stop));

        CommandDispatcher dispatcher = new CommandDispatcher(store, rdb);
        RedisServer server = new RedisServer(dispatcher);

        System.out.println("Redis server starting on port 6379...");
        server.start();
    }
}