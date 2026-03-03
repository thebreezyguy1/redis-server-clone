package com.redis.protocol;

import java.nio.charset.StandardCharsets;

public class RespEncoder {
    
    public String encode(RespValue value) {
        return switch(value.getType()) {
            case SIMPLE_STRING  -> "+" + value.getStrValue() + "\r\n";
            case ERROR -> "-" + value.getStrValue() + "\r\n";
            case INTEGER -> ":" + value.getLongValue() + "\r\n";
            case BULK_STRING -> {
                String s = value.getStrValue() == null ? "": value.getStrValue();
                byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
                yield "$" + bytes.length + "\r\n" + s + "\r\n";
            }
            case NULL -> "$-1\r\n";
            case ARRAY -> {
                StringBuilder sb = new StringBuilder();
                sb.append("*").append(value.getArrayValue().size()).append("\r\n");
                for (RespValue item: value.getArrayValue()) {
                    sb.append(encode(item));
                }
                yield sb.toString();
            }    
        };
    }

    public byte[] encodeToBytes(RespValue value) {
        return encode(value).getBytes(StandardCharsets.UTF_8);
    }
}
