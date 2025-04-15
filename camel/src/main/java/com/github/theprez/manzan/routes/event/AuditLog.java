package com.github.theprez.manzan.routes.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.manzan.ManzanMessageFormatter;
import com.github.theprez.manzan.routes.ManzanRoute;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;

public class AuditLog extends ManzanRoute {
    final private String m_sql;
    final private String m_auditLogType;
    final int m_interval;
    final ManzanMessageFormatter m_formatter;

    public AuditLog(final String _name, final String _format,
                    final List<String> _destinations,
                    final int _interval,
                    final int _numToProcess,
                    final AuditType _auditType,
                    final int _fallbackStartTime) throws IOException {
        super(_name);
        super.setRecipientList(_destinations);

        m_interval = _interval;
        m_formatter = StringUtils.isEmpty(_format) ? null : new ManzanMessageFormatter(_format);
        String audit_table = null;
        switch (_auditType){
            case PASSWORD:
                audit_table = "SYSTOOLS.AUDIT_JOURNAL_PW ()";
                m_auditLogType = "PASSWORD";
                break;
            default:
                throw new RuntimeException("Invalid audit log type: " + _auditType);
        }
        m_sql = String.format("SELECT * FROM TABLE ( %s" +
                " ) as x where x.ENTRY_TIMESTAMP > (SELECT COALESCE(MAX(TIME)," +
                " CURRENT_TIMESTAMP - %d HOURS) AS result_time FROM MANZAN.AUDJRNTS where AUDTYPE='%s')" +
                " order by x.ENTRY_TIMESTAMP ASC limit %d", audit_table, _fallbackStartTime, _auditType, _numToProcess);
    }

    @Override
    public void configure() {
        from("timer://foo?synchronous=true&period=" + m_interval)
                .routeId("audit_table:" + m_name)
                .setBody(constant(m_sql))
                .to("jdbc:jt400?outputType=StreamList")
                .process(exchange -> {
                    List<Map<String, Object>> rows = exchange.getIn().getBody(List.class);
                    exchange.setProperty("resultSet", rows);

                    // Find max ENTRY_TIMESTAMP
                    Optional<Timestamp> maxTimestamp = rows.stream()
                            .map(row -> (Timestamp) row.get("ENTRY_TIMESTAMP"))
                            .filter(Objects::nonNull)
                            .max(Comparator.naturalOrder());

                    maxTimestamp.ifPresent(ts -> {
                        String updateQuery = String.format(
                                "insert into MANZAN.AUDJRNTS (AUDTYPE, TIME) values ('%s', TIMESTAMP('%s'))",
                                m_auditLogType,
                                ts
                        );
                        exchange.getIn().setBody(updateQuery);
                    });
                    // If maxTimestamp is not present, set the body to null
                    if (!maxTimestamp.isPresent()) {
                        exchange.getIn().setBody(null);
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
                    if (null != m_formatter) {
                        exchange.getIn().setBody(m_formatter.format(dataMap));
                    } else {
                        // Use Jackson for pretty-printing
                        ObjectMapper mapper = new ObjectMapper();
                        mapper.enable(SerializationFeature.INDENT_OUTPUT); // pretty print
                        String json = mapper.writeValueAsString(dataMap);
                        exchange.getIn().setBody(json);
                    }
                })
                .recipientList(constant(getRecipientList()))
                .parallelProcessing()
                .stopOnException()
                .end() // end recipientList
                .end(); // end split
    }
}

