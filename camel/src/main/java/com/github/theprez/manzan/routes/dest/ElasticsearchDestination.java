package com.github.theprez.manzan.routes.dest;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;

import com.github.theprez.manzan.ManzanEventType;
import com.github.theprez.manzan.routes.ManzanRoute;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;

public class ElasticsearchDestination extends ManzanRoute {
    String index;
    ElasticsearchClient esClient;

    public ElasticsearchDestination(final String _name, String _endpoint, String _apiKey, String _index) {
        super(_name);
        this.index = _index;

        // Create the low-level client
        RestClient restClient = RestClient
            .builder(HttpHost.create(_endpoint))
            .setDefaultHeaders(new Header[]{
                new BasicHeader("Authorization", "ApiKey " + _apiKey)
            })
            .build();

        // Create the transport with a Jackson mapper
        ElasticsearchTransport transport = new RestClientTransport(
            restClient, new JacksonJsonpMapper());

        // Create the API client
        this.esClient = new ElasticsearchClient(transport);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    esClient.close();
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Converts numeric values that exceed Long.MAX_VALUE to strings to prevent
     * Elasticsearch indexing errors. This handles cases where database numeric
     * fields contain values larger than Java's long type can represent.
     *
     * @param dataMap The original data map from the exchange
     * @return A sanitized data map with large numbers converted to strings
     */
    private Map<String, Object> sanitizeDataMap(Map<String, Object> dataMap) {
        Map<String, Object> sanitized = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
            Object value = entry.getValue();
            
            // Check if the value is a numeric type that might exceed long range
            if (value instanceof Number) {
                Number numValue = (Number) value;
                
                // Convert to BigInteger to check if it exceeds long range
                BigInteger bigIntValue;
                if (value instanceof BigInteger) {
                    bigIntValue = (BigInteger) value;
                } else if (value instanceof Long || value instanceof Integer ||
                           value instanceof Short || value instanceof Byte) {
                    bigIntValue = BigInteger.valueOf(numValue.longValue());
                } else if (value instanceof Double || value instanceof Float) {
                    // For floating point, convert to string if it's too large
                    double doubleValue = numValue.doubleValue();
                    if (Math.abs(doubleValue) > Long.MAX_VALUE) {
                        sanitized.put(entry.getKey(), String.valueOf(value));
                        continue;
                    }
                    bigIntValue = BigInteger.valueOf(numValue.longValue());
                } else {
                    // For other numeric types, try to convert to string representation
                    try {
                        bigIntValue = new BigInteger(value.toString());
                    } catch (NumberFormatException e) {
                        // If conversion fails, keep original value
                        sanitized.put(entry.getKey(), value);
                        continue;
                    }
                }
                
                // Check if the value exceeds long range
                if (bigIntValue.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0 ||
                    bigIntValue.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) < 0) {
                    // Convert to string to preserve the value
                    sanitized.put(entry.getKey(), bigIntValue.toString());
                } else {
                    // Value is within long range, keep as-is
                    sanitized.put(entry.getKey(), value);
                }
            } else {
                // Non-numeric values pass through unchanged
                sanitized.put(entry.getKey(), value);
            }
        }
        
        return sanitized;
    }

    @Override
    public void configure() {
        from(getInUri())
            .routeId(m_name).process(exchange -> {
            Map<String, Object> dataMap = getDataMap(exchange);
            Map<String, Object> sanitizedData = sanitizeDataMap(dataMap);
            
            esClient.index(i -> i
                .index(this.index)
                .document(sanitizedData)
            );
        });
    }

    @Override
    protected void setEventType(ManzanEventType manzanEventType) {}
}