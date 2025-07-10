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
    private final String m_socketPort;


    public WatchMsgEventSockets(final String _name,
                                final Map<String, String> _formatMap,
                                final Map<String, String> _destMap,
                                final Map<String, ManzanEventType> _eventMap,
                                final Map<String, Map<String, String>> _dataMapInjectionsMap) {
        super(_name);
        m_formatMap = _formatMap;
        m_destMap = _destMap;
        m_eventMap = _eventMap;
        m_dataMapInjectionsMap = _dataMapInjectionsMap;
        m_socketPort = getSocketPort();
    }

    protected void setEventType(ManzanEventType eventType) {
        m_eventType = eventType;
    }

    ;

    private String getSocketPort() {
        String portEnv = System.getenv("MANZAN_SOCKET_PORT");
        if (portEnv != null && portEnv.matches("\\d+")) {
            return portEnv;
        }
        return "8080";
    }

    //@formatter:off
    @Override
    public void configure() {
        from(String.format("netty:tcp://%s:%s?sync=false", m_socketIp, m_socketPort))
            .unmarshal().json(JsonLibrary.Jackson, Map.class)
            .routeId("manzan_msg:"+m_name)
            .setHeader("session_id", simple("${body[SESSION_ID]}"))
                .process(exchange -> {
                    String sessionId = exchange.getIn().getHeader("session_id", String.class);
                    Map<String, String> dataMapInjection = m_dataMapInjectionsMap.get(sessionId);
                    Map<String, Object> dataMap = exchange.getIn().getBody(Map.class);
                    injectIntoDataMap(dataMap, dataMapInjection);
                    exchange.getIn().setHeader("data_map", dataMap);
                    exchange.getIn().setBody(dataMap);
                })
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
                String destinations = m_destMap.get(sessionId); // Get destinations from m_destMap
                exchange.getIn().setHeader("destinations", destinations);

                ManzanEventType eventType = m_eventMap.get(sessionId);
                setEventType(eventType);

            })
                .setHeader(EVENT_TYPE, constant(m_eventType))
                .recipientList(header("destinations"))
                .parallelProcessing().stopOnException().end();
    }
    //@formatter:on
}