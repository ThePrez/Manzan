package com.github.theprez.manzan.routes.dest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.github.theprez.manzan.InstanceContext;
import com.github.theprez.manzan.ManzanEventType;
import com.github.theprez.manzan.routes.ManzanRoute;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.LogRecordBuilder;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporterBuilder;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessorBuilder;
import io.opentelemetry.sdk.resources.Resource;

import org.apache.camel.Exchange;

/**
 * OTLP/HTTP destination for Manzan events.
 *
 * <p>Uses the OpenTelemetry SDK's {@link OtlpHttpLogRecordExporter} as the transport, giving
 * built-in batching, retry, connection pooling, and TLS/mTLS — none of which are available
 * through the Camel HTTP component approach this class previously used.
 *
 * <p>The previous implementation extended {@code ManzanGenericCamelRoute} and delegated the
 * HTTP call to {@code camel-http}. That inheritance is intentionally removed here because
 * the SDK exporter owns its own transport stack; the Camel pipeline was only used as a
 * pass-through, adding overhead and preventing batching.
 *
 * <p><b>Configuration keys (dests.ini):</b>
 * <pre>
 * [my-otel]
 * type             = otlp
 * endpoint         = https://collector.example.com:4318/v1/logs   # required
 * headers          = Authorization: Bearer xyz, X-Tenant: acme    # optional, comma-separated
 * caCertPath       = /etc/manzan/certs/ca.crt                      # optional PEM
 * clientCertPath   = /etc/manzan/certs/client.crt                  # optional PEM (mTLS)
 * clientKeyPath    = /etc/manzan/certs/client.key                  # optional PEM (mTLS)
 * batchSize        = 512                                            # optional, default 512
 * batchTimeoutMs   = 5000                                          # optional, default 5000
 * timeoutMs        = 10000                                         # optional, default 10000
 * errorRegex       = (SEVERE|MSGTYPE\s*=\s*S)                      # optional
 * format           = ${MESSAGE}                                    # optional
 * </pre>
 */
public class OpenTelemetryDestination extends ManzanRoute {

    private final SdkLoggerProvider m_loggerProvider;
    private final Logger m_logger;
    private final Pattern m_errorPattern;

    /**
     * Constructor. All TLS/auth setup happens here so failures are surfaced at
     * startup rather than at first message delivery.
     *
     * @param ctx            instance context (for route ID scoping)
     * @param name           destination name from dests.ini
     * @param endpoint       full OTLP/HTTP endpoint, e.g. {@code https://host:4318/v1/logs}
     * @param headers        optional comma-separated {@code "Key: Value"} pairs
     * @param caCertPath     optional path to PEM CA certificate for custom trust anchor
     * @param clientCertPath optional path to PEM client certificate (mTLS)
     * @param clientKeyPath  optional path to PEM client private key (mTLS)
     * @param batchSize      max records per export batch
     * @param batchTimeoutMs max ms between forced flushes
     * @param timeoutMs      per-request timeout in ms
     * @param errorRegex     optional regex; body matches → Severity.ERROR, else INFO
     */
    public OpenTelemetryDestination(
            final InstanceContext ctx,
            final String name,
            final String endpoint,
            final String headers,
            final String caCertPath,
            final String clientCertPath,
            final String clientKeyPath,
            final int batchSize,
            final int batchTimeoutMs,
            final int timeoutMs,
            final String errorRegex) {

        super(ctx, name);

        // --- Compile errorRegex once at construction, not per-message ---
        Pattern compiled = null;
        if (errorRegex != null && !errorRegex.isEmpty()) {
            try {
                compiled = Pattern.compile(errorRegex);
            } catch (PatternSyntaxException e) {
                System.err.println("[OTLP:" + name + "] Invalid errorRegex '" + errorRegex + "': " + e.getMessage());
            }
        }
        m_errorPattern = compiled;

        // --- Build the HTTP exporter ---
        OtlpHttpLogRecordExporterBuilder exporterBuilder = OtlpHttpLogRecordExporter.builder()
                .setEndpoint(endpoint)
                .setTimeout(timeoutMs, TimeUnit.MILLISECONDS);

        // Auth / custom headers
        if (headers != null && !headers.isEmpty()) {
            for (String pair : headers.split(",")) {
                int colon = pair.indexOf(':');
                if (colon > 0) {
                    String key = pair.substring(0, colon).trim();
                    String value = pair.substring(colon + 1).trim();
                    exporterBuilder.addHeader(key, value);
                }
            }
        }

        // Custom CA trust anchor (self-signed / enterprise CA)
        if (caCertPath != null && !caCertPath.isEmpty()) {
            exporterBuilder.setTrustedCertificates(readPemBytes(name, "caCertPath", caCertPath));
        }

        // mTLS — both cert and key must be provided together
        if (clientCertPath != null && !clientCertPath.isEmpty()) {
            if (clientKeyPath == null || clientKeyPath.isEmpty()) {
                throw new IllegalArgumentException(
                        "[OTLP:" + name + "] clientCertPath requires clientKeyPath to also be set");
            }
            exporterBuilder.setClientTls(
                    readPemBytes(name, "clientKeyPath",  clientKeyPath),
                    readPemBytes(name, "clientCertPath", clientCertPath));
        }

        OtlpHttpLogRecordExporter exporter = exporterBuilder.build();

        // --- Batch processor: SDK handles queue, retry, connection pool ---
        // scheduleDelay is set to a very large value so the background scheduler
        // never races with the explicit forceFlush() call in emit(). On IBM i PASE
        // the background scheduler thread does not run reliably anyway —
        // forceFlush() in emit() is the sole export driver.
        BatchLogRecordProcessorBuilder processorBuilder = BatchLogRecordProcessor.builder(exporter)
                .setMaxExportBatchSize(batchSize)
                .setScheduleDelay(24, TimeUnit.HOURS);

        // --- Logger provider wired to the batch processor ---
        m_loggerProvider = SdkLoggerProvider.builder()
                .setResource(Resource.getDefault()
                        .merge(Resource.builder()
                                .put("service.name", "manzan")
                                .put("manzan.instance", ctx != null ? ctx.getInstanceName() : "default")
                                .build()))
                .addLogRecordProcessor(processorBuilder.build())
                .build();

        m_logger = m_loggerProvider.get("manzan." + name);

        // Flush and shut down cleanly when the JVM exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            m_loggerProvider.forceFlush().join(5, TimeUnit.SECONDS);
            m_loggerProvider.shutdown().join(5, TimeUnit.SECONDS);
        }));
    }

    @Override
    public void configure() {
        from(getInUri())
                .routeId(getRouteId())
                .process(exchange -> emit(exchange));
    }

    @Override
    protected void setEventType(ManzanEventType manzanEventType) {
        // not used — event type is read from the exchange header at emit time
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void emit(Exchange exchange) {
        final ManzanEventType type =
                (ManzanEventType) exchange.getIn().getHeader(EVENT_TYPE);
        final Map<String, Object> dataMap = getDataMap(exchange);

        // Resolve severity and timestamp from the event type
        final SeverityInfo si = resolveSeverity(exchange, type, dataMap);

        LogRecordBuilder builder = m_logger.logRecordBuilder()
                .setTimestamp(si.timestampNanos, TimeUnit.NANOSECONDS)
                .setSeverity(si.severity)
                .setSeverityText(si.severity.name());

        // Emit every data-map field as a typed attribute
        for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
            setTypedAttribute(builder, entry.getKey(), entry.getValue());
        }

        builder.emit();

        // Force an immediate flush so the export happens synchronously on the
        // Camel thread rather than relying on the SDK's background scheduler,
        // which may not run reliably on IBM i PASE.
        io.opentelemetry.sdk.common.CompletableResultCode result =
                m_loggerProvider.forceFlush();
        result.join(30, TimeUnit.SECONDS);
        if (result.isSuccess()) {
            System.out.println("[OTLP:" + m_name + "] export succeeded");
        } else {
            System.err.println("[OTLP:" + m_name + "] export failed (see errors above)");
        }
    }

    /**
     * Sets a typed OTel attribute so the backend receives the correct field type
     * rather than everything coerced to a string.
     */
    @SuppressWarnings("unchecked")
    private static void setTypedAttribute(LogRecordBuilder builder, String key, Object value) {
        if (value instanceof String) {
            builder.setAttribute(AttributeKey.stringKey(key), (String) value);
        } else if (value instanceof Boolean) {
            builder.setAttribute(AttributeKey.booleanKey(key), (Boolean) value);
        } else if (value instanceof Integer) {
            builder.setAttribute(AttributeKey.longKey(key), ((Integer) value).longValue());
        } else if (value instanceof Long) {
            builder.setAttribute(AttributeKey.longKey(key), (Long) value);
        } else if (value instanceof Double) {
            builder.setAttribute(AttributeKey.doubleKey(key), (Double) value);
        } else if (value instanceof Float) {
            builder.setAttribute(AttributeKey.doubleKey(key), ((Float) value).doubleValue());
        } else if (value != null) {
            // Fallback: stringify unknown types rather than silently dropping them
            builder.setAttribute(AttributeKey.stringKey(key), value.toString());
        }
    }

    private SeverityInfo resolveSeverity(Exchange exchange, ManzanEventType type,
                                          Map<String, Object> dataMap) {
        if (type == ManzanEventType.WATCH_MSG) {
            int sev = (Integer) dataMap.get(MSG_SEVERITY);
            long ts  = Long.parseLong(getString(exchange, MSG_MESSAGE_TIMESTAMP));
            Severity severity = sev > SEVERITY_LIMIT ? Severity.ERROR : Severity.INFO;
            return new SeverityInfo(severity, ts);

        } else if (type == ManzanEventType.WATCH_VLOG) {
            // VLOG entries are always fatal-level system events
            long ts = Long.parseLong(getString(exchange, LOG_TIMESTAMP));
            return new SeverityInfo(Severity.FATAL, ts);

        } else if (type == ManzanEventType.WATCH_PAL) {
            // PAL (Product Activity Log) entries are always fatal-level
            long ts = Long.parseLong(getString(exchange, PAL_TIMESTAMP));
            return new SeverityInfo(Severity.FATAL, ts);

        } else {
            // For all other event types use wall-clock and regex-based error detection
            long tsNanos = System.currentTimeMillis() * 1_000_000L;
            boolean isError = isErrorInDataMap(dataMap);
            return new SeverityInfo(isError ? Severity.ERROR : Severity.INFO, tsNanos);
        }
    }

    private boolean isErrorInDataMap(Map<String, Object> dataMap) {
        if (m_errorPattern == null) {
            return false;
        }
        // Test the pattern against each string value in the map
        for (Object value : dataMap.values()) {
            if (value != null) {
                Matcher m = m_errorPattern.matcher(value.toString());
                if (m.find()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static byte[] readPemBytes(String destName, String paramName, String path) {
        try {
            return Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "[OTLP:" + destName + "] Cannot read " + paramName + " from '" + path + "': " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Private inner class — replaces the former package-private SeverityInfo
    // -------------------------------------------------------------------------

    private static final class SeverityInfo {
        final Severity severity;
        final long timestampNanos;

        SeverityInfo(Severity severity, long timestampNanos) {
            this.severity      = severity;
            this.timestampNanos = timestampNanos;
        }
    }
}
