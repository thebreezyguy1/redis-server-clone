package com.redis.core;

public class ValueEntry {
    private String value;
    private long expiresAt;
    private DataType type;

    public ValueEntry() {
        this.value = null;
        this.expiresAt = -1;
        this.type = DataType.STRING;
    };

    public ValueEntry(String value, long expiresAt, DataType type) {
        this.value = value;
        this.expiresAt = expiresAt;
        this.type = type;
    };

    public String getValue() {
        return value;
    };

    public long getExpiresAt() {
        return expiresAt;
    };

    public DataType getType() {
        return type;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public void setType(DataType type) {
        this.type = type;
    }

}
