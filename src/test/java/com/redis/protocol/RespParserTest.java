package com.redis.protocol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RespParserTest {

    private RespParser parser;

    // helper — converts a RESP string into an InputStream the parser can read
    private InputStream toStream(String resp) {
        return new ByteArrayInputStream(resp.getBytes(StandardCharsets.UTF_8));
    }

    @BeforeEach
    void setUp() {
        parser = new RespParser();
    }

    // ── SIMPLE STRING ───────────────────────────────────────────────────

    @Test
    void parse_simple_string() throws IOException {
        RespValue result = parser.parse(toStream("+OK\r\n"));
        assertEquals(RespValue.Type.SIMPLE_STRING, result.getType());
        assertEquals("OK", result.getStrValue());
    }

    @Test
    void parse_simple_string_with_spaces() throws IOException {
        RespValue result = parser.parse(toStream("+hello world\r\n"));
        assertEquals(RespValue.Type.SIMPLE_STRING, result.getType());
        assertEquals("hello world", result.getStrValue());
    }

    // ── ERROR ───────────────────────────────────────────────────────────

    @Test
    void parse_error() throws IOException {
        RespValue result = parser.parse(toStream("-ERR unknown command\r\n"));
        assertEquals(RespValue.Type.ERROR, result.getType());
        assertEquals("ERR unknown command", result.getStrValue());
    }

    @Test
    void parse_error_wrong_type() throws IOException {
        RespValue result = parser.parse(toStream("-WRONGTYPE operation not permitted\r\n"));
        assertEquals(RespValue.Type.ERROR, result.getType());
        assertEquals("WRONGTYPE operation not permitted", result.getStrValue());
    }

    // ── INTEGER ─────────────────────────────────────────────────────────

    @Test
    void parse_integer() throws IOException {
        RespValue result = parser.parse(toStream(":42\r\n"));
        assertEquals(RespValue.Type.INTEGER, result.getType());
        assertEquals(42L, result.getLongValue());
    }

    @Test
    void parse_integer_zero() throws IOException {
        RespValue result = parser.parse(toStream(":0\r\n"));
        assertEquals(RespValue.Type.INTEGER, result.getType());
        assertEquals(0L, result.getLongValue());
    }

    @Test
    void parse_negative_integer() throws IOException {
        RespValue result = parser.parse(toStream(":-1\r\n"));
        assertEquals(RespValue.Type.INTEGER, result.getType());
        assertEquals(-1L, result.getLongValue());
    }

    @Test
    void parse_large_integer() throws IOException {
        RespValue result = parser.parse(toStream(":1000000\r\n"));
        assertEquals(RespValue.Type.INTEGER, result.getType());
        assertEquals(1000000L, result.getLongValue());
    }

    // ── BULK STRING ─────────────────────────────────────────────────────

    @Test
    void parse_bulk_string() throws IOException {
        RespValue result = parser.parse(toStream("$6\r\ndorian\r\n"));
        assertEquals(RespValue.Type.BULK_STRING, result.getType());
        assertEquals("dorian", result.getStrValue());
    }

    @Test
    void parse_bulk_string_empty() throws IOException {
        RespValue result = parser.parse(toStream("$0\r\n\r\n"));
        assertEquals(RespValue.Type.BULK_STRING, result.getType());
        assertEquals("", result.getStrValue());
    }

    @Test
    void parse_bulk_string_with_spaces() throws IOException {
        RespValue result = parser.parse(toStream("$11\r\nhello world\r\n"));
        assertEquals(RespValue.Type.BULK_STRING, result.getType());
        assertEquals("hello world", result.getStrValue());
    }

    @Test
    void parse_bulk_string_two_digit_length() throws IOException {
        // "hello world!" is 12 characters — tests that multi-digit lengths parse correctly
        RespValue result = parser.parse(toStream("$12\r\nhello world!\r\n"));
        assertEquals(RespValue.Type.BULK_STRING, result.getType());
        assertEquals("hello world!", result.getStrValue());
    }

    @Test
    void parse_null_bulk_string() throws IOException {
        RespValue result = parser.parse(toStream("$-1\r\n"));
        assertEquals(RespValue.Type.NULL, result.getType());
    }

    // ── ARRAY ───────────────────────────────────────────────────────────

    @Test
    void parse_empty_array() throws IOException {
        RespValue result = parser.parse(toStream("*0\r\n"));
        assertEquals(RespValue.Type.ARRAY, result.getType());
        assertTrue(result.getArrayValue().isEmpty());
    }

    @Test
    void parse_array_single_element() throws IOException {
        RespValue result = parser.parse(toStream("*1\r\n$4\r\nPING\r\n"));
        assertEquals(RespValue.Type.ARRAY, result.getType());
        assertEquals(1, result.getArrayValue().size());
        assertEquals("PING", result.getArrayValue().get(0).getStrValue());
    }

    @Test
    void parse_array_bulk_strings() throws IOException {
        // SET name dorian — the way redis-cli sends it
        String resp = "*3\r\n$3\r\nSET\r\n$4\r\nname\r\n$6\r\ndorian\r\n";
        RespValue result = parser.parse(toStream(resp));

        assertEquals(RespValue.Type.ARRAY, result.getType());

        List<RespValue> items = result.getArrayValue();
        assertEquals(3, items.size());
        assertEquals("SET",    items.get(0).getStrValue());
        assertEquals("name",   items.get(1).getStrValue());
        assertEquals("dorian", items.get(2).getStrValue());
    }

    @Test
    void parse_array_mixed_types() throws IOException {
        // array containing a bulk string and an integer
        String resp = "*2\r\n$5\r\nhello\r\n:99\r\n";
        RespValue result = parser.parse(toStream(resp));

        assertEquals(RespValue.Type.ARRAY, result.getType());

        List<RespValue> items = result.getArrayValue();
        assertEquals(2, items.size());
        assertEquals(RespValue.Type.BULK_STRING, items.get(0).getType());
        assertEquals("hello", items.get(0).getStrValue());
        assertEquals(RespValue.Type.INTEGER, items.get(1).getType());
        assertEquals(99L, items.get(1).getLongValue());
    }

    @Test
    void parse_nested_array() throws IOException {
        // outer array contains an inner array and a bulk string
        String resp = "*2\r\n*2\r\n$1\r\na\r\n$1\r\nb\r\n$5\r\nouter\r\n";
        RespValue result = parser.parse(toStream(resp));

        assertEquals(RespValue.Type.ARRAY, result.getType());
        assertEquals(2, result.getArrayValue().size());

        RespValue inner = result.getArrayValue().get(0);
        assertEquals(RespValue.Type.ARRAY, inner.getType());
        assertEquals(2, inner.getArrayValue().size());
        assertEquals("a", inner.getArrayValue().get(0).getStrValue());
        assertEquals("b", inner.getArrayValue().get(1).getStrValue());

        assertEquals("outer", result.getArrayValue().get(1).getStrValue());
    }

    // ── REAL REDIS COMMANDS ─────────────────────────────────────────────
    // These mirror exactly what redis-cli sends over the wire

    @Test
    void parse_ping_command() throws IOException {
        RespValue result = parser.parse(toStream("*1\r\n$4\r\nPING\r\n"));
        List<RespValue> items = result.getArrayValue();
        assertEquals(1, items.size());
        assertEquals("PING", items.get(0).getStrValue());
    }

    @Test
    void parse_get_command() throws IOException {
        RespValue result = parser.parse(toStream("*2\r\n$3\r\nGET\r\n$4\r\nname\r\n"));
        List<RespValue> items = result.getArrayValue();
        assertEquals(2, items.size());
        assertEquals("GET",  items.get(0).getStrValue());
        assertEquals("name", items.get(1).getStrValue());
    }

    @Test
    void parse_del_multiple_keys() throws IOException {
        String resp = "*4\r\n$3\r\nDEL\r\n$1\r\na\r\n$1\r\nb\r\n$1\r\nc\r\n";
        RespValue result = parser.parse(toStream(resp));
        List<RespValue> items = result.getArrayValue();
        assertEquals(4, items.size());
        assertEquals("DEL", items.get(0).getStrValue());
        assertEquals("a",   items.get(1).getStrValue());
        assertEquals("b",   items.get(2).getStrValue());
        assertEquals("c",   items.get(3).getStrValue());
    }

    // ── EDGE CASES ──────────────────────────────────────────────────────

    @Test
    void parse_returns_null_on_empty_stream() throws IOException {
        RespValue result = parser.parse(toStream(""));
        assertNull(result);
    }

    @Test
    void parse_unknown_type_treats_as_inline() throws IOException {
        RespValue result = parser.parse(toStream("!invalid\r\n"));
        assertEquals(RespValue.Type.ARRAY, result.getType());
        assertEquals("!invalid", result.getArrayValue().get(0).getStrValue());
    }

    // ── ROUND TRIP ──────────────────────────────────────────────────────
    // Encode a value, feed the result back into the parser, assert you get the same value

    @Test
    void round_trip_simple_string() throws IOException {
        RespEncoder encoder = new RespEncoder();
        RespValue original = RespValue.simpleString("OK");
        String encoded = encoder.encode(original);
        RespValue parsed = parser.parse(toStream(encoded));
        assertEquals(original.getStrValue(), parsed.getStrValue());
        assertEquals(original.getType(), parsed.getType());
    }

    @Test
    void round_trip_bulk_string() throws IOException {
        RespEncoder encoder = new RespEncoder();
        RespValue original = RespValue.bulkString("dorian");
        String encoded = encoder.encode(original);
        RespValue parsed = parser.parse(toStream(encoded));
        assertEquals(original.getStrValue(), parsed.getStrValue());
        assertEquals(original.getType(), parsed.getType());
    }

    @Test
    void round_trip_integer() throws IOException {
        RespEncoder encoder = new RespEncoder();
        RespValue original = RespValue.integer(2003);
        String encoded = encoder.encode(original);
        RespValue parsed = parser.parse(toStream(encoded));
        assertEquals(original.getLongValue(), parsed.getLongValue());
        assertEquals(original.getType(), parsed.getType());
    }

    @Test
    void round_trip_array() throws IOException {
        RespEncoder encoder = new RespEncoder();
        RespValue original = RespValue.array(List.of(
            RespValue.bulkString("SET"),
            RespValue.bulkString("name"),
            RespValue.bulkString("dorian")
        ));
        String encoded = encoder.encode(original);
        RespValue parsed = parser.parse(toStream(encoded));

        assertEquals(RespValue.Type.ARRAY, parsed.getType());
        assertEquals(3, parsed.getArrayValue().size());
        assertEquals("SET",    parsed.getArrayValue().get(0).getStrValue());
        assertEquals("name",   parsed.getArrayValue().get(1).getStrValue());
        assertEquals("dorian", parsed.getArrayValue().get(2).getStrValue());
    }
}