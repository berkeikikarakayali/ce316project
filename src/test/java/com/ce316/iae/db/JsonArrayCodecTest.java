package com.ce316.iae.db;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonArrayCodecTest {

    @Test
    void emptyAndNullRoundTrip() {
        assertEquals("[]", JsonArrayCodec.encode(null));
        assertEquals("[]", JsonArrayCodec.encode(Collections.emptyList()));
        assertTrue(JsonArrayCodec.decode("[]").isEmpty());
        assertTrue(JsonArrayCodec.decode(null).isEmpty());
    }

    @Test
    void simpleRoundTrip() {
        List<String> in = Arrays.asList("-d", ".", "Main");
        String encoded = JsonArrayCodec.encode(in);
        assertEquals("[\"-d\",\".\",\"Main\"]", encoded);
        assertEquals(in, JsonArrayCodec.decode(encoded));
    }

    @Test
    void escapesQuotesBackslashesAndNewlines() {
        List<String> in = Arrays.asList("she said \"hi\"", "a\\b", "line1\nline2");
        String encoded = JsonArrayCodec.encode(in);
        assertEquals(in, JsonArrayCodec.decode(encoded));
    }

    @Test
    void rejectsNonArrayInput() {
        assertThrows(IllegalArgumentException.class, () -> JsonArrayCodec.decode("not an array"));
    }
}
