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

                byte[] bytes = new byte[length];
                int offset = 0;
                while (offset < length) {
                    int read = inputStream.read(bytes, offset, length - offset);
                    if (read == -1) throw new IOException("Unexpected end of stream");
                    offset += read;
                }
                inputStream.read(); // \r
                inputStream.read(); // \n
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
            default -> {
                StringBuilder sb = new StringBuilder();
                sb.append((char) data);
                while (true) {
                    int b = inputStream.read();
                    if (b == -1 || (char) b == '\r') break;
                    sb.append((char) b);
                }
                inputStream.read(); // consume \n

                // convert inline to an array of bulk strings
                String[] parts = sb.toString().trim().split(" ");
                List<RespValue> items = new ArrayList<>();
                for (String part : parts) {
                    items.add(RespValue.bulkString(part));
                }
                yield RespValue.array(items);
            }
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
