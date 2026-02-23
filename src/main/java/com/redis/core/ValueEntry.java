package com.redis.core;

public class ValueEntry {
    private String value;
    private long expiresAt;

    public ValueEntry() {
        this.value = null;
        this.expiresAt = -1;
    };

    public ValueEntry(String value, long expiresAt) {
        this.value = value;
        this.expiresAt = expiresAt;
    };

    public String getValue() {
        return value;
    };

    public long getExpiresAt() {
        return expiresAt;
    };

    public void setValue(String value) {
        this.value = value;
    };

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    };

}
