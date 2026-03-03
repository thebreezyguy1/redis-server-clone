package com.redis.protocol;

import java.util.List;

public class RespValue {
    public enum Type {SIMPLE_STRING, ERROR, INTEGER, BULK_STRING, ARRAY, NULL}

    private final Type type;
    private final String strValue;
    private final long longValue;
    private final List<RespValue> arrayValue;

    private RespValue(Type type, String strValue, long longValue, List<RespValue> arrayValue) {
        this.type = type;
        this.strValue = strValue;
        this.longValue = longValue;
        this.arrayValue = arrayValue;
    }

    public static RespValue simpleString(String s) {
        return new RespValue(Type.SIMPLE_STRING, s, 0, null);
    }

    public static RespValue error(String msg) {
        return new RespValue(Type.ERROR, msg, 0, null);
    }

    public static RespValue integer(long n) {
        return new RespValue(Type.INTEGER, null, n, null);
    }

    public static RespValue bulkString(String s) {
        return new RespValue(Type.BULK_STRING, s, 0, null);
    }

    public static RespValue nullValue() {
        return new RespValue(Type.NULL, null, 0, null);
    }

    public static RespValue array(List<RespValue> items) {
        return new RespValue(Type.ARRAY, null, 0, items);
    }

    public Type getType() {
        return type;
    }

    public String getStrValue() {
        return strValue;
    }

    public long getLongValue() {
        return longValue;
    }

    public List<RespValue> getArrayValue() {
        return arrayValue;
    }

    @Override
    public String toString() {
        return switch (type) {
            case SIMPLE_STRING  -> "SIMPLE_STRING(" + strValue + ")";
            case ERROR          -> "ERROR(" + strValue + ")";
            case INTEGER        -> "INTEGER(" + strValue + ")";
            case BULK_STRING    -> "BULK_STRING(" + strValue + ")";
            case NULL           -> "NULL";
            case ARRAY          -> "ARRAY(" + arrayValue + ")";
        };
    }
 
}
