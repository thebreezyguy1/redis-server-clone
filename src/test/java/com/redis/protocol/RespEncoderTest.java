package com.redis.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


class RespEncoderTest {

    private RespEncoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new RespEncoder();
    }

    @Test
    void encode_simple_string() {
        String result = encoder.encode(RespValue.simpleString("dorian"));
        assertEquals("+dorian\r\n", result);
    }

    @Test
    void encode_error() {
        String result = encoder.encode(RespValue.error("ERR 404"));
        assertEquals("-ERR 404\r\n", result);
    }
    
    @Test
    void encode_integer() {
        String result = encoder.encode(RespValue.integer(2003));
        assertEquals(":2003\r\n", result);
    }

    @Test 
    void encode_bulk_string() {
        String result = encoder.encode(RespValue.bulkString("Hello"));
        assertEquals("$5\r\nHello\r\n", result);
    }

    @Test 
    void encode_null() {
        String result = encoder.encode(RespValue.nullValue());
        assertEquals("$-1\r\n", result);
    }

    @Test
    void encode_empty_string() {
        String result = encoder.encode(RespValue.bulkString(""));
        assertEquals("$0\r\n\r\n", result);
    }

    @Test
    void encode_array() {
        List<RespValue> arr = new ArrayList<>();
        arr.add(RespValue.integer(45));
        arr.add(RespValue.bulkString("dorian"));
        arr.add(RespValue.bulkString("hello"));
        String result = encoder.encode(RespValue.array(arr));
        assertEquals("*3\r\n:45\r\n$6\r\ndorian\r\n$5\r\nhello\r\n", result);
    }

    @Test
    void encode_nested_array() {
        List<RespValue> inner = List.of(RespValue.bulkString("a"), RespValue.bulkString("b"));
        List<RespValue> outer = List.of(RespValue.array(inner), RespValue.integer(1));
        String result = encoder.encode(RespValue.array(outer));
        assertEquals("*2\r\n*2\r\n$1\r\na\r\n$1\r\nb\r\n:1\r\n", result);
    }

}
