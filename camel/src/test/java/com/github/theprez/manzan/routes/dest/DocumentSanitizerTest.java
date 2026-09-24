package com.github.theprez.manzan.routes.dest;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DocumentSanitizer}.
 *
 * <p>Must live in the same package as the production code because
 * {@code DocumentSanitizer} is package-private. No Camel context or
 * network connection is required — all tests are pure in-memory.
 */
class DocumentSanitizerTest {

    @Test
    void passesThroughStrings() {
        Map<String, Object> input = map("key", "value");
        assertEquals("value", DocumentSanitizer.sanitize(input).get("key"),
                "String values must pass through unchanged");
    }

    @Test
    void passesThroughInRangeLong() {
        Map<String, Object> input = map("num", 42L);
        assertEquals(42L, DocumentSanitizer.sanitize(input).get("num"),
                "In-range long must pass through unchanged");
    }

    @Test
    void passesThroughInRangeInteger() {
        Map<String, Object> input = map("num", 99);
        assertEquals(99, DocumentSanitizer.sanitize(input).get("num"),
                "In-range int must pass through unchanged");
    }

    @Test
    void passesThroughLongMaxValue() {
        Map<String, Object> input = map("max", Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, DocumentSanitizer.sanitize(input).get("max"),
                "Long.MAX_VALUE is in range and must not be stringified");
    }

    @Test
    void passesThroughLongMinValue() {
        Map<String, Object> input = map("min", Long.MIN_VALUE);
        assertEquals(Long.MIN_VALUE, DocumentSanitizer.sanitize(input).get("min"),
                "Long.MIN_VALUE is in range and must not be stringified");
    }

    @Test
    void stringifiesBigIntegerAboveMax() {
        BigInteger huge = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
        Map<String, Object> input = map("big", huge);
        assertEquals(huge.toString(), DocumentSanitizer.sanitize(input).get("big"),
                "BigInteger exceeding Long.MAX_VALUE must be stringified");
    }

    @Test
    void stringifiesBigIntegerBelowMin() {
        BigInteger tiny = BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE);
        Map<String, Object> input = map("tiny", tiny);
        assertEquals(tiny.toString(), DocumentSanitizer.sanitize(input).get("tiny"),
                "BigInteger below Long.MIN_VALUE must be stringified");
    }

    @Test
    void passesThroughNullValues() {
        Map<String, Object> input = new HashMap<>();
        input.put("nullable", null);
        assertNull(DocumentSanitizer.sanitize(input).get("nullable"),
                "Null values must pass through without NullPointerException");
    }

    @Test
    void doesNotMutateInputMap() {
        BigInteger huge = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
        Map<String, Object> input = map("big", huge);

        DocumentSanitizer.sanitize(input);

        assertSame(huge, input.get("big"),
                "sanitize() must return a new map and must not modify the original");
    }

    @Test
    void returnsNewMap() {
        Map<String, Object> input = map("k", "v");
        Map<String, Object> result = DocumentSanitizer.sanitize(input);
        assertNotSame(input, result,
                "sanitize() must return a new map, not the same instance");
    }

    @Test
    void handlesNaNDoubleWithoutThrowing() {
        Map<String, Object> input = map("nan", Double.NaN);
        assertDoesNotThrow(() -> DocumentSanitizer.sanitize(input),
                "NaN double must not cause an exception");
    }

    @Test
    void handlesPositiveInfiniteDoubleWithoutThrowing() {
        Map<String, Object> input = map("inf", Double.POSITIVE_INFINITY);
        assertDoesNotThrow(() -> DocumentSanitizer.sanitize(input),
                "POSITIVE_INFINITY must not cause an exception");
    }

    @Test
    void handlesNegativeInfiniteDoubleWithoutThrowing() {
        Map<String, Object> input = map("inf", Double.NEGATIVE_INFINITY);
        assertDoesNotThrow(() -> DocumentSanitizer.sanitize(input),
                "NEGATIVE_INFINITY must not cause an exception");
    }

    @Test
    void handlesInRangeDouble() {
        Map<String, Object> input = map("d", 3.14);
        // In-range double: should pass through as-is (not stringified)
        Object result = DocumentSanitizer.sanitize(input).get("d");
        assertEquals(3.14, result, "In-range double must pass through unchanged");
    }

    @Test
    void handlesMixedMapCorrectly() {
        BigInteger huge = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
        Map<String, Object> input = new HashMap<>();
        input.put("normal_str",  "text");
        input.put("normal_long", 100L);
        input.put("normal_int",  42);
        input.put("oversized",   huge);
        input.put("nullable",    null);

        Map<String, Object> result = DocumentSanitizer.sanitize(input);

        assertEquals("text",          result.get("normal_str"));
        assertEquals(100L,            result.get("normal_long"));
        assertEquals(42,              result.get("normal_int"));
        assertEquals(huge.toString(), result.get("oversized"),
                "Only the oversized value should be stringified");
        assertNull(result.get("nullable"));
        // Unmodified entries count must match
        assertEquals(input.size(), result.size(),
                "Output map must have the same number of keys as the input");
    }

    @Test
    void handlesEmptyMap() {
        Map<String, Object> result = DocumentSanitizer.sanitize(new HashMap<>());
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Empty input must produce empty output");
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private static Map<String, Object> map(String key, Object value) {
        Map<String, Object> m = new HashMap<>();
        m.put(key, value);
        return m;
    }
}
