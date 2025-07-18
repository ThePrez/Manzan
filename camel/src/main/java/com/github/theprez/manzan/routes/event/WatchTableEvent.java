package com.github.theprez.manzan.routes.event;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.manzan.ManzanEventType;
import com.github.theprez.manzan.ManzanMessageFormatter;
import com.github.theprez.manzan.routes.ManzanRoute;

public class WatchTableEvent extends ManzanRoute {

    private final String m_schema;
    private final String m_table;
    private final int m_interval;
    private final int m_numToProcess;
    private final ManzanMessageFormatter m_formatter;
    final Map<String, String> dataMapInjection;


    public WatchTableEvent(final String _name, final String _format,
            final List<String> _destinations, final String _schema, final String _table, 
            final int _interval, final int _numToProcess, final Map<String, String> _dataMapInjection)
            throws IOException {
        super(_name);
        m_schema = _schema;
        m_table = _table;
        m_interval = _interval;
        m_numToProcess = _numToProcess;
        m_formatter = StringUtils.isEmpty(_format) ? null : new ManzanMessageFormatter(_format);
        dataMapInjection = _dataMapInjection;
        super.setRecipientList(_destinations);
        setEventType(ManzanEventType.TABLE);
    }

    protected void setEventType(ManzanEventType eventType){
        m_eventType = eventType;
    };

    @Override
    public void configure() {
        from("timer://foo?synchronous=true&period=" + m_interval)
                .routeId("manzan_table:" + m_name)
                .process(exchange -> {
                    // Reset the list of ordinal positions at the start of each execution
                    exchange.setProperty("ordinalPositions", new ArrayList<Integer>());
                })
                .setBody(constant("SELECT * FROM " + m_schema + "." + m_table
                        + " LIMIT " + m_numToProcess))
                .to("jdbc:jt400?outputType=StreamList")
                .split(body()).streaming().parallelProcessing()
                .setHeader("id", simple("${body[ID]}"))
                .process(exchange -> {
                    Map<String, Object> dataMap = exchange.getIn().getBody(Map.class);
                    injectIntoDataMap(dataMap, dataMapInjection);
                    exchange.getIn().setHeader("data_map", dataMap);
                    exchange.getIn().setBody(dataMap);
                })
                .setHeader(EVENT_TYPE, constant(m_eventType))
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
                        exchange.getIn().setHeader("format_applied", true);
                    }
                })
                .recipientList(constant(getRecipientList()))
                .parallelProcessing()
                .stopOnException()
                .end()
                .end()
                .process(exchange -> {
                    // Constructing the WHERE clause for ORDINAL_POSITIONs
                    StringBuilder deleteQuery = new StringBuilder("DELETE FROM " + m_schema + "." + m_table
                            + " WHERE ID IN (");
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