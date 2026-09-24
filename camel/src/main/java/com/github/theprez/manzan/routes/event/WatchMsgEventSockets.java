package com.github.theprez.manzan.routes.event;

import java.util.Map;

import org.apache.camel.model.dataformat.JsonLibrary;

import com.github.theprez.manzan.ManzanEventType;
import com.github.theprez.manzan.ManzanMessageFormatter;
import com.github.theprez.manzan.routes.ManzanRoute;

public class WatchMsgEventSockets extends ManzanRoute {

    private final Map<String, String> m_formatMap;
    private final Map<String, String> m_destMap;
    private final Map<String, ManzanEventType> m_eventMap;
    final Map<String, Map<String, String>> m_dataMapInjectionsMap;

    private final String m_socketIp = "0.0.0.0";
    private final int m_socketPort;

    /**
     * Create a socket-listener route for watch events.
     *
     * @param _name                 Route name.
     * @param _socketPort           TCP port this instance listens on.  Must be
     *                              unique per host when multiple instances are
     *                              running.  Read from {@code [watch] socketPort}
     *                              in {@code app.ini} via
     *                              {@link com.github.theprez.manzan.configuration.ApplicationConfig#getSocketPort()}.
     * @param _formatMap            Session-ID → format-string map.
     * @param _destMap              Session-ID → recipient-list map.
     * @param _eventMap             Session-ID → event-type map.
     * @param _dataMapInjectionsMap Session-ID → injection key/value pairs.
     */
    public WatchMsgEventSockets(final String _name,
                                final int _socketPort,
                                final Map<String, String> _formatMap,
                                final Map<String, String> _destMap,
                                final Map<String, ManzanEventType> _eventMap,
                                final Map<String, Map<String, String>> _dataMapInjectionsMap) {
        super(_name);
        m_socketPort = _socketPort;
        m_formatMap = _formatMap;
        m_destMap = _destMap;
        m_eventMap = _eventMap;
        m_dataMapInjectionsMap = _dataMapInjectionsMap;
    }

    /** Returns the TCP port this route's socket listener is bound to. */
    public int getSocketPort() {
        return m_socketPort;
    }

    protected void setEventType(ManzanEventType eventType) {
        m_eventType = eventType;
    }

    //@formatter:off
    @Override
    public void configure() {
        from(String.format("netty:tcp://%s:%d?sync=false", m_socketIp, m_socketPort))
            .unmarshal().json(JsonLibrary.Jackson, Map.class)
            .routeId("manzan_msg:"+m_name)
            .setHeader("session_id", simple("${body[SESSION_ID]}"))
                .process(exchange -> {
                    String sessionId = exchange.getIn().getHeader("session_id", String.class);
                    String normalizedSessionId = sessionId == null ? null : sessionId.trim().toUpperCase();
                    exchange.getIn().setHeader("session_id", normalizedSessionId);

                    // warn and drop messages whose SESSION_ID is not owned by
                    // this instance.  Prevents silent message loss and makes cross-
                    // instance mis-delivery visible in the logs.
                    if (normalizedSessionId == null || !m_destMap.containsKey(normalizedSessionId)) {
                        System.err.println("WatchMsgEventSockets: received message with unknown SESSION_ID '"
                                + normalizedSessionId + "' — discarding. Known sessions: "
                                + m_destMap.keySet());
                        exchange.getIn().setHeader("destinations", (Object) null);
                        return;
                    }

                    Map<String, String> dataMapInjection = m_dataMapInjectionsMap.get(normalizedSessionId);
                    Map<String, Object> dataMap = exchange.getIn().getBody(Map.class);
                    injectIntoDataMap(dataMap, dataMapInjection);
                    exchange.getIn().setHeader("data_map", dataMap);
                    exchange.getIn().setBody(dataMap);
                })
            .filter(exchange -> exchange.getIn().getHeader("destinations") != null
                    || m_destMap.containsKey(
                            exchange.getIn().getHeader("session_id", String.class)))
            .marshal().json(true) //TODO: skip this if we are applying a format
            .setBody(simple("${body}\n"))
            .process(exchange -> {
                String sessionId = exchange.getIn().getHeader("session_id", String.class);
                String format = m_formatMap.get(sessionId);
                if (format != null) {
                    ManzanMessageFormatter m_formatter = new ManzanMessageFormatter(format);
                    exchange.getIn().setBody(m_formatter.format(getDataMap(exchange)));
                    exchange.getIn().setHeader("format_applied", true);
                }
                String destinations = m_destMap.get(sessionId);
                exchange.getIn().setHeader("destinations", destinations);

                ManzanEventType eventType = m_eventMap.get(sessionId);
                setEventType(eventType);
                exchange.getIn().setHeader(EVENT_TYPE, m_eventType);
            })
                .recipientList(header("destinations"))
                .parallelProcessing().stopOnException().end();
    }
    //@formatter:on
}