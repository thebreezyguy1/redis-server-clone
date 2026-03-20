package com.redis.protocol;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class RespEncoder {
    private static final byte[] CRLF = {'\r', '\n'};
    private static final byte[] NULL_BULK = "$-1\r\n".getBytes(StandardCharsets.UTF_8);

    public void encodeTo(RespValue value, OutputStream out) throws IOException {
        switch (value.getType()) {
            case SIMPLE_STRING -> {
                out.write('+');
                out.write(value.getStrValue().getBytes(StandardCharsets.UTF_8));
                out.write(CRLF);
            }
            case ERROR -> {
                out.write('-');
                out.write(value.getStrValue().getBytes(StandardCharsets.UTF_8));
                out.write(CRLF);
            }
            case INTEGER -> {
                out.write(':');
                out.write(Long.toString(value.getLongValue()).getBytes(StandardCharsets.UTF_8));
                out.write(CRLF);
            }
            case BULK_STRING -> {
                String s = value.getStrValue() == null ? "" : value.getStrValue();
                byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
                out.write('$');
                out.write(Integer.toString(bytes.length).getBytes(StandardCharsets.UTF_8));
                out.write(CRLF);
                out.write(bytes);
                out.write(CRLF);
            }
            case NULL -> out.write(NULL_BULK);
            case ARRAY -> {
                out.write('*');
                out.write(Integer.toString(value.getArrayValue().size()).getBytes(StandardCharsets.UTF_8));
                out.write(CRLF);
                for (RespValue item : value.getArrayValue()) {
                    encodeTo(item, out);
                }
            }
        }
    }

    public byte[] encodeToBytes(RespValue value) {
        try {
            java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
            encodeTo(value, buf);
            return buf.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String encode(RespValue value) {
        return new String(encodeToBytes(value), StandardCharsets.UTF_8);
    }
}
