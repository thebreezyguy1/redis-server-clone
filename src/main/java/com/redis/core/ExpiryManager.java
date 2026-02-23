package com.redis.core;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ExpiryManager {
    private final RedisStore store;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> task;
    
    public ExpiryManager(RedisStore store) {
        this.store = store;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "expiry-sweeper");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        task = scheduler.scheduleAtFixedRate(
            this::sweep, 
            0, 
            0, 
            TimeUnit.MILLISECONDS
        );
    }

    public void stop() {
        if (task != null) task.cancel(false);
        scheduler.shutdown();
    }

    private void sweep() {
        try {
            store.evictExpiredKeys();
        } catch (Exception e) {
            System.err.println("[ExpiryManager] sweep error: " + e.getMessage());
        }
    }
}
