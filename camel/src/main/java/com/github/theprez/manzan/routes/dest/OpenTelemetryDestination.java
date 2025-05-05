package com.github.theprez.manzan.routes.dest;

import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.theprez.manzan.ManzanEventType;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.opentelemetry.api.logs.Severity;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;


import com.github.theprez.manzan.routes.ManzanGenericCamelRoute;

public class OpenTelemetryDestination extends ManzanGenericCamelRoute {
    final String errorRegex;

    private OpenTelemetryDestination(final CamelContext _context, final String _name, final String _url, final String _format, final Map<String, Object> _headerParams, final String errorRegex) {
        super(_context, _name, _url.startsWith("https") ? "https" : "http", _url.replaceFirst("^http(s)?://", ""), _format, null, _headerParams, null);
        this.errorRegex = errorRegex;
    }

    public static OpenTelemetryDestination get(final CamelContext _context, final String _name, final String _url, final String _format, final String errorRegex) {
        Map<String, Object> headerParameters = new LinkedHashMap<String, Object>();
        String hostVal = _url.replaceFirst("^http(s)?://", "").replaceAll("\\/.*", "");
        headerParameters.put("Host", hostVal);
        headerParameters.put("User-Agent", "Manzan/1.0");
        return new OpenTelemetryDestination(_context, _name, _url, _format, headerParameters, errorRegex);
    }

    /**
     * Transforms a map into a KvList. Kvlist is a key value list used in open telemetry protocol to specify a body with multiple keys.
     *
     * @param inputMap of keys and values
     * @return A JsonObject representing the KvList
     */
    private JsonObject mapToKvlistValue(Map<String, Object> inputMap) {
        JsonArray valuesArray = new JsonArray();

        for (Map.Entry<String, Object> entry : inputMap.entrySet()) {
            JsonObject kvEntry = new JsonObject();
            kvEntry.addProperty("key", entry.getKey());

            Object value = entry.getValue();
            JsonObject jsonValue = new JsonObject();

            if (value instanceof String) {
                jsonValue.addProperty("stringValue", (String) value);
            } else if (value instanceof Boolean) {
                jsonValue.addProperty("boolValue", (Boolean) value);
            } else if (value instanceof Integer) {
                jsonValue.addProperty("intValue", (Integer) value);
            } else if (value instanceof Long) {
                jsonValue.addProperty("intValue", (Long) value);
            } else if (value instanceof Double) {
                jsonValue.addProperty("doubleValue", (Double) value);
            } else if (value instanceof Float) {
                jsonValue.addProperty("doubleValue", ((Float) value).doubleValue());
            } else if (value instanceof byte[]) {
                String base64 = Base64.getEncoder().encodeToString((byte[]) value);
                jsonValue.addProperty("bytesValue", base64);
            } else {
                throw new IllegalArgumentException("Unsupported value type for key '" + entry.getKey() +
                        "': " + value.getClass().getName());
            }

            kvEntry.add("value", jsonValue);
            valuesArray.add(kvEntry);
        }

        JsonObject kvlistValue = new JsonObject();
        kvlistValue.add("values", valuesArray);

        JsonObject root = new JsonObject();
        root.add("kvlistValue", kvlistValue);

        return root;
    }

    private long getNanoSecondsSinceEpoch() {
        return Instant.now().toEpochMilli() * 1_000_000;
    }

    private String constructOpenTelemetryLogMessage(long timestamp,
                                                    int severityNumber,
                                                    String severityText,
                                                    String formatBody) {
        String message = String.format(
                        "{\n" +
                        "  \"resourceLogs\": [{\n" +
                        "    \"resource\": {\n" +
                        "      \"attributes\": [\n" +
                        "        { \"key\": \"service.name\", \"value\": { \"stringValue\": \"camel-watcher\" } }\n" +
                        "      ]\n" +
                        "    },\n" +
                        "    \"scopeLogs\": [{\n" +
                        "      \"scope\": { \"name\": \"file-watcher\" },\n" +
                        "      \"logRecords\": [{\n" +
                        "        \"timeUnixNano\": %d,\n" +
                        "        \"severityNumber\": %d,\n" +
                        "        \"severityText\": \"%s\",\n" +
                        "        \"body\":" + "%s" +
                        "    }]\n" +
                        "  }]\n" +
                        "  }]\n" +
                        "}",
                timestamp,
                severityNumber,
                severityText,
                formatBody
        );
        return message;
    }

    private SeverityInfo getSeverityInfo(Exchange exchange, String formatBody){
        int severityNumber;
        String severityText;
        long timestamp;
        final ManzanEventType type = (ManzanEventType) exchange.getIn().getHeader(EVENT_TYPE);
        if (type == ManzanEventType.WATCH_MSG) {
            severityNumber = ((Integer) get(exchange, MSG_SEVERITY)) > SEVERITY_LIMIT ? Severity.ERROR.getSeverityNumber() : Severity.INFO.getSeverityNumber();
            severityText = ((Integer) get(exchange, MSG_SEVERITY)) > SEVERITY_LIMIT ? Severity.ERROR.name() : Severity.INFO.name();
            timestamp = Long.parseLong(getString(exchange, MSG_MESSAGE_TIMESTAMP));
        } else if (type == ManzanEventType.WATCH_VLOG) {
            severityNumber = Severity.FATAL.getSeverityNumber();
            severityText = ((Integer) get(exchange, MSG_SEVERITY)) > SEVERITY_LIMIT ? Severity.ERROR.name() : Severity.INFO.name();
            timestamp = Long.parseLong(getString(exchange, LOG_TIMESTAMP));
        } else if (type == ManzanEventType.WATCH_PAL) {
            severityNumber = Severity.FATAL.getSeverityNumber();
            severityText = ((Integer) get(exchange, MSG_SEVERITY)) > SEVERITY_LIMIT ? Severity.ERROR.name() : Severity.INFO.name();
            timestamp = Long.parseLong(getString(exchange, PAL_TIMESTAMP));
        } else {
            boolean errorPatternFound = isErrorRegexFoundInBody(formatBody);
            severityNumber = errorPatternFound ?  Severity.ERROR.getSeverityNumber() : Severity.INFO.getSeverityNumber();
            severityText = errorPatternFound ? Severity.ERROR.name() : Severity.INFO.name();
            timestamp = getNanoSecondsSinceEpoch();
        }
        return new SeverityInfo(severityNumber, severityText, timestamp);
    }

    private boolean isErrorRegexFoundInBody(String body){
        boolean errorPatternFound = false;
        if (this.errorRegex != null){
            try{
                Pattern errorPattern = Pattern.compile(this.errorRegex);
                Matcher matcher = errorPattern.matcher(body);
                errorPatternFound = matcher.find();
            } catch (PatternSyntaxException e){
                e.printStackTrace();
            }
        }
        return errorPatternFound;
    }

    private String constructOpenTelemetryBody(Exchange exchange){
        String formatBody;
        Object formatAppliedHeader = exchange.getIn().getHeader("format_applied");
        if (formatAppliedHeader instanceof Boolean && (boolean) formatAppliedHeader) {
            Object body = exchange.getIn().getBody();
            formatBody = String.format("{\"stringValue\": \"%s\"}", body);
        } else {
            Map<String, Object> dataMap = getDataMap(exchange);
            JsonObject formattedBody = mapToKvlistValue(dataMap);
            formatBody = formattedBody.toString();
        }
        return formatBody;
    }

    @Override
    protected void customPostProcess(Exchange exchange) throws JsonProcessingException {
        String formatBody = constructOpenTelemetryBody(exchange);
        SeverityInfo severityInfo = getSeverityInfo(exchange, formatBody);
        String message = constructOpenTelemetryLogMessage(severityInfo.getTimestamp(), severityInfo.getSeverityNumber(), severityInfo.getSeverityText(), formatBody);
        exchange.getIn().setBody(message);
    }
}

class SeverityInfo {
    private int severityNumber;
    private String severityText;
    private long timestamp;
    SeverityInfo(int severityNumber, String severityText, long timestamp) {
        this.severityNumber = severityNumber;
        this.severityText = severityText;
        this.timestamp = timestamp;
    }

    public int getSeverityNumber() {
        return severityNumber;
    }

    public String getSeverityText() {
        return severityText;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
