package com.redis.persistence;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.redis.core.RedisStore;
import com.redis.core.ValueEntry;
import com.redis.util.Config;

public class RdbSnapshot {
    private final RedisStore store;
    private final Config config;
    private final AtomicLong writesSinceLastSave = new AtomicLong(0);
    private volatile long lastSaveTime = System.currentTimeMillis();
    private ScheduledExecutorService scheduler;

    public RdbSnapshot(RedisStore store, Config config) {
        this.store = store;
        this.config = config;
    }

    public void start() {
        load();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rdb-snapshotter");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::checkAndSave, 1, 1, TimeUnit.SECONDS);
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        save();
    }

    public void incrementWriteCount() {
        writesSinceLastSave.incrementAndGet();
    }

    private void checkAndSave() {
        long writes = writesSinceLastSave.get();
        long elapsed = (System.currentTimeMillis() - lastSaveTime) / 1000;

        for (Config.SaveRule rule: config.getSaveRules()) {
            if (elapsed >= rule.seconds() && writes >= rule.changes()) {
                save();
                return;
            }
        }
    }

    public synchronized void save() {
        String tmpFile = config.getRdbFilename() + ".tmp";
        try (ObjectOutputStream oos = new ObjectOutputStream(
            new BufferedOutputStream(new FileOutputStream(tmpFile)))) {
                Map<String, ValueEntry> snapshot = store.getSnapshot();
                oos.writeInt(snapshot.size());

                for (Map.Entry<String, ValueEntry> entry: snapshot.entrySet()) {
                    oos.writeUTF(entry.getKey());
                    oos.writeUTF(entry.getValue().getValue());
                    oos.writeLong(entry.getValue().getExpiresAt());
                    oos.writeUTF(entry.getValue().getType().name());
                }

                oos.flush();
        } catch (IOException e) {
            System.err.println("RDB save failed: " + e.getMessage());
            return;
        }

        File tmp = new File(tmpFile);
        File target = new File(config.getRdbFilename());
        if (!tmp.renameTo(target)) {
            System.err.println("RDB rename failed");
        }
        
        writesSinceLastSave.set(0);
        lastSaveTime = System.currentTimeMillis();
    }

    public void load() {
        File file = new File(config.getRdbFilename());
        if (!file.exists()) return;

        try (ObjectInputStream ois = new ObjectInputStream(
            new BufferedInputStream(new FileInputStream(file))
        )) {
            int size = ois.readInt();

            for (int i = 0; i < size; i++) {
                String key = ois.readUTF();
                String value = ois.readUTF();
                long expiresAt = ois.readLong();
                String typeName = ois.readUTF();

                if (expiresAt != -1 && System.currentTimeMillis() > expiresAt) {
                    continue;
                }

                if (expiresAt == -1) {
                    store.set(key, value);
                } else {
                    long remainingMillis = expiresAt - System.currentTimeMillis();
                    store.set(key, value, remainingMillis);
                }
            }
        } catch (Exception e) {
            System.err.println("RDB load failed: " + e.getMessage());
        }
    }
}
