package com.github.theprez.manzan.routes.event;

import java.util.Map;

import org.apache.camel.model.dataformat.JsonLibrary;

import com.github.theprez.manzan.ManzanEventType;
import com.github.theprez.manzan.ManzanMessageFormatter;
import com.github.theprez.manzan.routes.ManzanRoute;

public class WatchMsgEventSockets extends ManzanRoute {

//    private final ManzanMessageFormatter m_formatter;
    private final Map<String, String> m_formatMap;
    final Map<String, String> m_destMap;
    private final String m_socketIp = "0.0.0.0";
    private final String m_socketPort = "8080";

    public WatchMsgEventSockets(final String _name, final Map<String, String> _formatMap,
            final Map<String, String> _destMap) {
        super(_name);
        m_formatMap = _formatMap;
        m_destMap = _destMap;
    }

//@formatter:off
    @Override
    public void configure() {
        from(String.format("netty:tcp://%s:%s?sync=false", m_socketIp, m_socketPort))
            .unmarshal().json(JsonLibrary.Jackson, Map.class)
            .routeId("manzan_msg:"+m_name)
            .setHeader(EVENT_TYPE, constant(ManzanEventType.WATCH_MSG))
            .setHeader("session_id", simple("${body[SESSION_ID]}"))
            .setHeader("data_map", simple("${body}"))
            .marshal().json(true) //TODO: skip this if we are applying a format
            .setBody(simple("${body}\n"))
            .process(exchange -> {
                String sessionId = exchange.getIn().getHeader("session_id", String.class);
                String format = m_formatMap.get(sessionId);
                if (format != null) {
                    ManzanMessageFormatter m_formatter = new ManzanMessageFormatter(format);
                    exchange.getIn().setBody(m_formatter.format(getDataMap(exchange)));
                    String destinations = m_destMap.get(sessionId); // Get destinations from m_destMap
                    exchange.getIn().setHeader("destinations", destinations);
                }
            })
                .recipientList(header("destinations"))
                .parallelProcessing().stopOnException();
    }
    //@formatter:on
}