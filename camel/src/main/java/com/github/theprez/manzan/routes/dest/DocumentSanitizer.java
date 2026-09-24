package com.github.theprez.manzan.routes.dest;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * Shared utility that prevents oversized numeric values from causing index-rejection
 * errors in document stores (Elasticsearch, OpenSearch).
 *
 * <p>IBM i numeric columns can produce {@link BigInteger} or other values that exceed
 * {@link Long#MAX_VALUE}. Both the Elasticsearch and OpenSearch clients serialise
 * document fields as JSON numbers, which will be rejected if the value is out of the
 * {@code long} range. This class converts such values to their string representation
 * while leaving everything else unchanged.
 *
 * <p>This class is package-private — it is an implementation detail shared between
 * {@link ElasticsearchDestination} and {@link OpenSearchDestination} only.
 */
final class DocumentSanitizer {

    // Utility class — no instances
    private DocumentSanitizer() {}

    /**
     * Returns a copy of {@code input} with any numeric value outside the
     * {@code [Long.MIN_VALUE, Long.MAX_VALUE]} range replaced by its
     * {@link Object#toString()} representation.
     *
     * @param input the raw data map from a Camel exchange
     * @return a new map safe for indexing; the original map is not modified
     */
    static Map<String, Object> sanitize(Map<String, Object> input) {
        final Map<String, Object> sanitized = new HashMap<>(input.size());

        for (Map.Entry<String, Object> entry : input.entrySet()) {
            sanitized.put(entry.getKey(), sanitizeValue(entry.getValue()));
        }

        return sanitized;
    }

    private static Object sanitizeValue(Object value) {
        if (!(value instanceof Number)) {
            return value;
        }

        final BigInteger bigInt = toBigInteger((Number) value);
        if (bigInt == null) {
            // Could not convert — return as-is and let the client handle it
            return value;
        }

        if (bigInt.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0
                || bigInt.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) < 0) {
            // Out of long range — stringify to preserve the value without truncation
            return bigInt.toString();
        }

        return value;
    }

    /**
     * Converts a {@link Number} to a {@link BigInteger} for range comparison.
     * Returns {@code null} if the conversion is not applicable (e.g. NaN, Infinity).
     */
    private static BigInteger toBigInteger(Number value) {
        if (value instanceof BigInteger) {
            return (BigInteger) value;
        }
        if (value instanceof Long || value instanceof Integer
                || value instanceof Short || value instanceof Byte) {
            return BigInteger.valueOf(value.longValue());
        }
        if (value instanceof Double || value instanceof Float) {
            double d = value.doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                return null;
            }
            if (Math.abs(d) > Long.MAX_VALUE) {
                // Use string conversion path in sanitizeValue
                return BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
            }
            return BigInteger.valueOf((long) d);
        }
        try {
            return new BigInteger(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
