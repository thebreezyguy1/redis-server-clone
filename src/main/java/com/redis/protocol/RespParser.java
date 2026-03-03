package com.redis.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class RespParser {

    public RespValue parse(InputStream inputStream) throws IOException {
        int data = inputStream.read();
        if (data == -1) return null;
        return switch ((char) data) {
            case '+' -> RespValue.simpleString(readline(inputStream));
            
            case '-' -> RespValue.error(readline(inputStream));
            
            case ':' -> RespValue.integer(Long.parseLong(readline(inputStream)));
            
            case '$' -> {
                int length = Integer.parseInt(readline(inputStream));
                if (length == -1) yield RespValue.nullValue();
                byte[] bytes = inputStream.readNBytes(length);
                inputStream.read(); inputStream.read();
                yield RespValue.bulkString(new String(bytes));
            }
            case '*' -> {
                int count = Integer.parseInt(readline(inputStream));
                List<RespValue> arr = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    arr.add(parse(inputStream));
                }
                yield RespValue.array(arr);
            }
            default -> throw new IOException("Unknown RESP type: " + (char) data);
        };
    }

    private String readline(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        while (true) {
            int data = in.read();
            if ((char) data == '\r') {
                in.read();
                break;
            }
            sb.append((char) data);
        }
        return sb.toString();
    }
}
