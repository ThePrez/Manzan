package com.github.theprez.manzan.routes.event;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.manzan.ManzanEventType;
import com.github.theprez.manzan.ManzanMessageFormatter;
import com.github.theprez.manzan.routes.ManzanRoute;

public class WatchMsgEvent extends ManzanRoute {

    private final int m_interval;
    private final int m_numToProcess;
    private final String m_schema;
    private final String m_sessionId;
    private final ManzanMessageFormatter m_formatter;

    public WatchMsgEvent(final String _name, final String _session_id, final String _format,
            final List<String> _destinations, final String _schema, final int _interval, final int _numToProcess)
            throws IOException {
        super(_name);
        m_interval = _interval;
        m_numToProcess = _numToProcess;
        m_schema = _schema;
        m_formatter = StringUtils.isEmpty(_format) ? null : new ManzanMessageFormatter(_format);
        super.setRecipientList(_destinations);
        m_sessionId = _session_id.trim().toUpperCase();
    }

    @Override
    public void configure() {
        from("timer://foo?synchronous=true&period=" + m_interval)
                .routeId("manzan_msg:" + m_name)
                .process(exchange -> {
                    // Reset the list of ordinal positions at the start of each execution
                    exchange.setProperty("ordinalPositions", new ArrayList<Integer>());
                })
                .setHeader(EVENT_TYPE, constant(ManzanEventType.WATCH_MSG))
                .setBody(constant("SeLeCt * fRoM " + m_schema + ".mAnZaNmSg wHeRe SESSION_ID = '" + m_sessionId
                        + "' limit " + m_numToProcess))
                .to("jdbc:jt400?outputType=StreamList")
                .split(body()).streaming().parallelProcessing()
                .setHeader("id", simple("${body[ORDINAL_POSITION]}"))
                .setHeader("session_id", simple("${body[SESSION_ID]}"))
                .setHeader("data_map", simple("${body}"))
                .process(exchange -> {
                    Integer ordinalPosition = exchange.getIn().getHeader("id", Integer.class);
                    @SuppressWarnings("unchecked")
                    List<Integer> ordinalPositions = exchange.getProperty("ordinalPositions", List.class);

                    synchronized (ordinalPositions) {
                        ordinalPositions.add(ordinalPosition); // Accumulate ORDINAL_POSITION
                    }
                })
                .marshal().json(true) // TODO: skip this if we are applying a format
                .setBody(simple("${body}\n"))
                .process(exchange -> {
                    if (null != m_formatter) {
                        exchange.getIn().setBody(m_formatter.format(getDataMap(exchange)));
                    }
                })
                .recipientList(constant(getRecipientList()))
                .parallelProcessing()
                .stopOnException()
                .end()
                .end()
                .process(exchange -> {
                    // Constructing the WHERE clause for ORDINAL_POSITIONs
                    StringBuilder deleteQuery = new StringBuilder("DELETE FROM " + m_schema
                            + ".mAnZaNmSg WHERE SESSION_ID = '" + m_sessionId + "' AND ORDINAL_POSITION IN (");
                    @SuppressWarnings("unchecked")
                    List<Integer> ordinalPositions = exchange.getProperty("ordinalPositions", List.class);
                    if (ordinalPositions != null && !ordinalPositions.isEmpty()) {
                        synchronized (ordinalPositions) {
                            String positions = ordinalPositions.stream()
                                    .map(String::valueOf) // Convert to String
                                    .collect(Collectors.joining(","));
                            deleteQuery.append(positions).append(")");
                        }

                        // Set the DELETE query as the body
                        exchange.getIn().setBody(deleteQuery.toString());
                    } else {
                        // If no positions, set body to null, skip the DELETE
                        exchange.getIn().setBody(null);
                    }
                })
                .choice()
                .when(body().isNotNull())
                .to("jdbc:jt400")
                .to("stream:err");
    }
}
