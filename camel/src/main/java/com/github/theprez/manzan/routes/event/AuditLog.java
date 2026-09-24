package com.github.theprez.manzan.routes.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.manzan.InstanceContext;
import com.github.theprez.manzan.ManzanEventType;
import com.github.theprez.manzan.ManzanMessageFormatter;
import com.github.theprez.manzan.routes.ManzanRoute;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;

public class AuditLog extends ManzanRoute {
    final private String m_sql;
    final private String m_auditType;
    final private String m_sessionId;
    final int m_interval;
    final ManzanMessageFormatter m_formatter;
    final Map<String, String> dataMapInjection;

    /**
     * Create an AuditLog route for a specific instance.
     *
     * The watermark SELECT and MERGE are scoped to the instance's session ID so
     * that multiple concurrent instances each maintain an independent high-water
     * mark in AUDJRNTS without interfering with one another.
     *
     * @param ctx               Instance context — used for session-ID scoping and route ID generation.
     * @param _name             Section name from data.ini (used as route name).
     * @param _format           Optional message format string; null for JSON output.
     * @param _destinations     Destination names to route matching audit events to.
     * @param _interval         Polling interval in milliseconds.
     * @param _numToProcess     Maximum rows to consume per poll cycle.
     * @param _auditType        Audit journal type key (must match an {@link AuditType} name).
     * @param _fallbackStartTime Hours to look back when no prior watermark exists.
     * @param _dataMapInjection Static key/value pairs to inject into every event data map.
     */
    public AuditLog(final InstanceContext ctx,
                    final String _name, final String _format,
                    final List<String> _destinations,
                    final int _interval,
                    final int _numToProcess,
                    final String _auditType,
                    final int _fallbackStartTime,
                    final Map<String, String> _dataMapInjection) throws IOException {
        super(ctx, _name);
        super.setRecipientList(_destinations);
        m_auditType = _auditType;
        m_sessionId = ctx.generateSessionId(_name);
        m_interval = _interval;
        m_formatter = StringUtils.isEmpty(_format) ? null : new ManzanMessageFormatter(_format);
        dataMapInjection = _dataMapInjection;

        String audit_table = AuditType.fromValue(_auditType).getValue();

        // Watermark lookup is scoped to this instance's session ID so that
        // concurrent instances each maintain an independent high-water mark.
        m_sql = String.format(
                "SELECT * FROM TABLE ( %s ) as x" +
                " WHERE x.ENTRY_TIMESTAMP > (" +
                "  SELECT COALESCE(MAX(TIME), CURRENT_TIMESTAMP - %d HOURS) AS result_time" +
                "  FROM MANZAN.AUDJRNTS WHERE SESSION_ID='%s' AND AUDTYPE='%s'" +
                ") ORDER BY x.ENTRY_TIMESTAMP ASC LIMIT %d",
                audit_table, _fallbackStartTime, m_sessionId, _auditType, _numToProcess);
        setEventType(ManzanEventType.AUDIT);
    }

    /**
     * Create an AuditLog route for the default instance.
     *
     * @deprecated Use {@link #AuditLog(InstanceContext, String, String, List, int, int, String, int, Map)}
     *             for multi-instance support.
     */
    @Deprecated
    public AuditLog(final String _name, final String _format,
                    final List<String> _destinations,
                    final int _interval,
                    final int _numToProcess,
                    final String _auditType,
                    final int _fallbackStartTime,
                    final Map<String, String> _dataMapInjection) throws IOException {
        this(InstanceContext.getDefault(), _name, _format, _destinations,
                _interval, _numToProcess, _auditType, _fallbackStartTime, _dataMapInjection);
    }

    protected void setEventType(ManzanEventType eventType) {
        m_eventType = eventType;
    }

    @Override
    public void configure() {
        from("timer://foo?synchronous=true&period=" + m_interval)
                .routeId(getRouteId())
                .setBody(constant(m_sql))
                .to("jdbc:jt400?outputType=StreamList")
                .process(exchange -> {
                    List<Map<String, Object>> rows = exchange.getIn().getBody(List.class);
                    if (rows.isEmpty()) {
                        exchange.getIn().setBody(null);
                    } else {
                        exchange.setProperty("resultSet", rows);

                        // Find max ENTRY_TIMESTAMP
                        Optional<Timestamp> maxTimestamp = rows.stream()
                                .map(row -> (Timestamp) row.get("ENTRY_TIMESTAMP"))
                                .filter(Objects::nonNull)
                                .max(Comparator.naturalOrder());

                        maxTimestamp.ifPresent(ts -> {
                            // Watermark write is scoped to this instance's session ID so that
                            // concurrent instances each upsert their own row in AUDJRNTS.
                            String mergeQuery = String.format(
                                    "MERGE INTO MANZAN.AUDJRNTS tgt " +
                                    "USING (VALUES('%s', '%s')) src(SESSION_ID, AUDTYPE) " +
                                    "ON tgt.SESSION_ID = src.SESSION_ID AND tgt.AUDTYPE = src.AUDTYPE " +
                                    "WHEN MATCHED THEN " +
                                    "  UPDATE SET TIME = TIMESTAMP('%s') " +
                                    "WHEN NOT MATCHED THEN " +
                                    "  INSERT (SESSION_ID, AUDTYPE, TIME) VALUES('%s', '%s', TIMESTAMP('%s'))",
                                    m_sessionId, m_auditType, ts,
                                    m_sessionId, m_auditType, ts
                            );
                            exchange.getIn().setBody(mergeQuery);
                        });
                        if (!maxTimestamp.isPresent()) {
                            exchange.getIn().setBody(null);
                        }
                    }
                })
                .choice()
                .when(body().isNotNull())
                .to("jdbc:jt400")
                .to("stream:err")
                .process(exchange -> {
                    List<Map<String, Object>> rows = (List<Map<String, Object>>) exchange.getProperty("resultSet");
                    exchange.getIn().setBody(rows);
                })
                .split(body()).streaming().parallelProcessing()
                .process(exchange -> {
                    Map<String, Object> dataMap = exchange.getIn().getBody(Map.class);
                    injectIntoDataMap(dataMap, dataMapInjection);
                    exchange.getIn().setHeader("data_map", dataMap);
                    if (null != m_formatter) {
                        exchange.getIn().setBody(m_formatter.format(dataMap));
                        exchange.getIn().setHeader("format_applied", true);
                    } else {
                        ObjectMapper mapper = new ObjectMapper();
                        mapper.enable(SerializationFeature.INDENT_OUTPUT);
                        String json = mapper.writeValueAsString(dataMap);
                        exchange.getIn().setBody(json);
                    }
                })
                .setHeader(EVENT_TYPE, constant(m_eventType))
                .recipientList(constant(getRecipientList()))
                .parallelProcessing()
                .stopOnException()
                .end() // end recipientList
                .end(); // end split
    }
}
