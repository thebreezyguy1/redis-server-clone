package com.redis.core;

public enum DataType {
    STRING,
    LIST,
    SET,
    HASH,
    ZSET,
    NONE;

    public String toRedisString() {
        return this.name().toLowerCase();
    }
}
