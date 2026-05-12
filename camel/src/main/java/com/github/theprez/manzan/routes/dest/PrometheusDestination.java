package com.github.theprez.manzan.routes.dest;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.theprez.manzan.ManzanEventType;
import com.github.theprez.manzan.routes.ManzanRoute;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;

/**
 * Prometheus metrics exporter destination for Manzan.
 * Exposes IBM i events as Prometheus metrics via HTTP endpoint.
 */
public class PrometheusDestination extends ManzanRoute {
    
    private final int port;
    private final String path;
    private final String metricPrefix;
    private final String username;
    private final String password;
    private final CollectorRegistry registry;
    
    // Metric collectors
    private final ConcurrentHashMap<String, Counter> counters;
    private final ConcurrentHashMap<String, Gauge> gauges;
    private final ConcurrentHashMap<String, Histogram> histograms;
    
    // Default metric names
    private static final String EVENT_COUNTER_NAME = "events_total";
    private static final String EVENT_COUNTER_HELP = "Total number of events processed";
    
    private static final String MESSAGE_COUNTER_NAME = "messages_total";
    private static final String MESSAGE_COUNTER_HELP = "Total number of messages processed";
    
    private static final String JOB_DURATION_NAME = "job_duration_seconds";
    private static final String JOB_DURATION_HELP = "Job execution duration in seconds";
    
    private static final String QUEUE_DEPTH_NAME = "queue_depth";
    private static final String QUEUE_DEPTH_HELP = "Current message queue depth";

    public PrometheusDestination(final String _name, final int _port, final String _path, 
                                  final String _metricPrefix, final String _username, 
                                  final String _password) {
        super(_name);
        this.port = _port;
        this.path = _path != null ? _path : "/metrics";
        this.metricPrefix = _metricPrefix != null ? _metricPrefix : "manzan_";
        this.username = _username;
        this.password = _password;
        this.registry = new CollectorRegistry();
        this.counters = new ConcurrentHashMap<>();
        this.gauges = new ConcurrentHashMap<>();
        this.histograms = new ConcurrentHashMap<>();
        
        // Initialize default metrics
        initializeDefaultMetrics();
    }

    private void initializeDefaultMetrics() {
        // Event counter
        getOrCreateCounter(EVENT_COUNTER_NAME, EVENT_COUNTER_HELP, "event_type", "system");
        
        // Message counter
        getOrCreateCounter(MESSAGE_COUNTER_NAME, MESSAGE_COUNTER_HELP, "message_type", "severity", "system");
        
        // Job duration histogram
        getOrCreateHistogram(JOB_DURATION_NAME, JOB_DURATION_HELP, "job_name", "status", "system");
        
        // Queue depth gauge
        getOrCreateGauge(QUEUE_DEPTH_NAME, QUEUE_DEPTH_HELP, "queue_name", "system");
    }

    private Counter getOrCreateCounter(String name, String help, String... labelNames) {
        String fullName = metricPrefix + name;
        return counters.computeIfAbsent(fullName, k -> 
            Counter.build()
                .name(fullName)
                .help(help)
                .labelNames(labelNames)
                .register(registry)
        );
    }

    private Gauge getOrCreateGauge(String name, String help, String... labelNames) {
        String fullName = metricPrefix + name;
        return gauges.computeIfAbsent(fullName, k -> 
            Gauge.build()
                .name(fullName)
                .help(help)
                .labelNames(labelNames)
                .register(registry)
        );
    }

    private Histogram getOrCreateHistogram(String name, String help, String... labelNames) {
        String fullName = metricPrefix + name;
        return histograms.computeIfAbsent(fullName, k -> 
            Histogram.build()
                .name(fullName)
                .help(help)
                .labelNames(labelNames)
                .register(registry)
        );
    }

    @Override
    public void configure() {
        // Process incoming events and update metrics
        from(getInUri())
            .routeId(m_name)
            .process(new MetricsProcessor());
        
        // HTTP endpoint for Prometheus scraping
        from("netty-http:http://0.0.0.0:" + port + path + "?matchOnUriPrefix=true")
            .routeId(m_name + "_http_endpoint")
            .process(exchange -> {
                // Basic authentication if configured
                if (username != null && password != null) {
                    String authHeader = exchange.getIn().getHeader("Authorization", String.class);
                    if (!isValidAuth(authHeader)) {
                        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 401);
                        exchange.getIn().setHeader("WWW-Authenticate", "Basic realm=\"Prometheus Metrics\"");
                        exchange.getIn().setBody("Unauthorized");
                        return;
                    }
                }
                
                // Export metrics in Prometheus format
                StringWriter writer = new StringWriter();
                try {
                    TextFormat.write004(writer, registry.metricFamilySamples());
                    exchange.getIn().setHeader(Exchange.CONTENT_TYPE, TextFormat.CONTENT_TYPE_004);
                    exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
                    exchange.getIn().setBody(writer.toString());
                } catch (IOException e) {
                    exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 500);
                    exchange.getIn().setBody("Error generating metrics: " + e.getMessage());
                }
            });
    }

    private boolean isValidAuth(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return false;
        }
        
        String base64Credentials = authHeader.substring("Basic ".length()).trim();
        String credentials = new String(java.util.Base64.getDecoder().decode(base64Credentials));
        String[] values = credentials.split(":", 2);
        
        return values.length == 2 && 
               username.equals(values[0]) && 
               password.equals(values[1]);
    }

    @Override
    protected void setEventType(ManzanEventType manzanEventType) {
        // Not needed for Prometheus destination
    }

    /**
     * Processor that converts Manzan events to Prometheus metrics
     */
    private class MetricsProcessor implements Processor {
        @Override
        public void process(Exchange exchange) throws Exception {
            ManzanEventType eventType = (ManzanEventType) exchange.getIn().getHeader(EVENT_TYPE);
            Map<String, Object> dataMap = getDataMap(exchange);
            String system = getString(exchange, "SYSTEM_NAME", "unknown");
            
            // Increment event counter
            Counter eventCounter = getOrCreateCounter(EVENT_COUNTER_NAME, EVENT_COUNTER_HELP, "event_type", "system");
            eventCounter.labels(eventType != null ? eventType.name() : "UNKNOWN", system).inc();
            
            // Process specific event types
            if (eventType != null) {
                switch (eventType) {
                    case WATCH_MSG:
                        processMessageEvent(exchange, dataMap, system);
                        break;
                    case WATCH_VLOG:
                        processJobLogEvent(exchange, dataMap, system);
                        break;
                    case WATCH_PAL:
                        processJobLogEvent(exchange, dataMap, system);
                        break;
                    case AUDIT:
                        processAuditEvent(exchange, dataMap, system);
                        break;
                    case FILE:
                        processFileEvent(exchange, dataMap, system);
                        break;
                    case SQL:
                        processSqlEvent(exchange, dataMap, system);
                        break;
                    case TABLE:
                        processSqlEvent(exchange, dataMap, system);
                        break;
                    case JOBLOG:
                        processJobLogEvent(exchange, dataMap, system);
                        break;
                    default:
                        // Generic event processing
                        break;
                }
            }
        }

        private void processMessageEvent(Exchange exchange, Map<String, Object> dataMap, String system) {
            String messageType = getString(exchange, "MSG_TYPE", "unknown");
            String severity = getString(exchange, "MSG_SEVERITY", "unknown");
            
            Counter msgCounter = getOrCreateCounter(MESSAGE_COUNTER_NAME, MESSAGE_COUNTER_HELP, 
                "message_type", "severity", "system");
            msgCounter.labels(messageType, severity, system).inc();
            
            // Update queue depth if available
            String queueName = getString(exchange, "MSG_QUEUE", "unknown");
            if (dataMap.containsKey("QUEUE_DEPTH")) {
                try {
                    double depth = Double.parseDouble(dataMap.get("QUEUE_DEPTH").toString());
                    Gauge queueGauge = getOrCreateGauge(QUEUE_DEPTH_NAME, QUEUE_DEPTH_HELP, 
                        "queue_name", "system");
                    queueGauge.labels(queueName, system).set(depth);
                } catch (NumberFormatException e) {
                    // Ignore invalid depth values
                }
            }
        }

        private void processJobLogEvent(Exchange exchange, Map<String, Object> dataMap, String system) {
            String jobName = getString(exchange, "JOB_NAME", "unknown");
            String status = getString(exchange, "JOB_STATUS", "unknown");
            
            // Track job duration if available
            if (dataMap.containsKey("JOB_DURATION") || dataMap.containsKey("ELAPSED_TIME")) {
                try {
                    String durationKey = dataMap.containsKey("JOB_DURATION") ? "JOB_DURATION" : "ELAPSED_TIME";
                    double duration = Double.parseDouble(dataMap.get(durationKey).toString());
                    
                    Histogram jobDuration = getOrCreateHistogram(JOB_DURATION_NAME, JOB_DURATION_HELP, 
                        "job_name", "status", "system");
                    jobDuration.labels(jobName, status, system).observe(duration);
                } catch (NumberFormatException e) {
                    // Ignore invalid duration values
                }
            }
            
            // Count job completions
            Counter jobCounter = getOrCreateCounter("jobs_total", "Total number of jobs processed", 
                "job_name", "status", "system");
            jobCounter.labels(jobName, status, system).inc();
        }

        private void processAuditEvent(Exchange exchange, Map<String, Object> dataMap, String system) {
            String auditType = getString(exchange, "AUDIT_TYPE", "unknown");
            String user = getString(exchange, "USER_NAME", "unknown");
            
            Counter auditCounter = getOrCreateCounter("audit_events_total", 
                "Total number of audit events", "audit_type", "user", "system");
            auditCounter.labels(auditType, user, system).inc();
        }

        private void processFileEvent(Exchange exchange, Map<String, Object> dataMap, String system) {
            String operation = getString(exchange, "FILE_OPERATION", "unknown");
            String path = getString(exchange, "FILE_PATH", "unknown");
            
            Counter fileCounter = getOrCreateCounter("file_operations_total", 
                "Total number of file operations", "operation", "system");
            fileCounter.labels(operation, system).inc();
        }

        private void processSqlEvent(Exchange exchange, Map<String, Object> dataMap, String system) {
            String operation = getString(exchange, "SQL_OPERATION", "unknown");
            String table = getString(exchange, "TABLE_NAME", "unknown");
            
            Counter sqlCounter = getOrCreateCounter("sql_operations_total", 
                "Total number of SQL operations", "operation", "table", "system");
            sqlCounter.labels(operation, table, system).inc();
        }

        private String getString(Exchange exchange, String key, String defaultValue) {
            try {
                String value = PrometheusDestination.this.getString(exchange, key);
                return value != null && !value.isEmpty() ? value : defaultValue;
            } catch (Exception e) {
                return defaultValue;
            }
        }
    }
}
