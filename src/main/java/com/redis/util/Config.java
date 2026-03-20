package com.redis.util;

import java.util.ArrayList;
import java.util.List;

public class Config {
    private final int port;
    private final String rdbFilename;
    private final List<SaveRule> saveRules;

    public Config() {
        this.port = 6379;
        this.rdbFilename = "dump.rdb";
        this.saveRules = new ArrayList<>();
        saveRules.add(new SaveRule(900, 1));
        saveRules.add(new SaveRule(300, 10));
        saveRules.add(new SaveRule(60, 100000));
    }

    public int getPort() { return port; }
    public String getRdbFilename() { return rdbFilename; }
    public List<SaveRule> getSaveRules() { return saveRules; }

    public record SaveRule(int seconds, int changes) {}
}
