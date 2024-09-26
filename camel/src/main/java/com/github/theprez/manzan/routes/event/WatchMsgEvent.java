package com.github.theprez.manzan.routes.event;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.camel.model.dataformat.JsonLibrary;

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

//@formatter:off
    @Override
    public void configure() {
        from("netty:tcp://0.0.0.0:8080?sync=false")
            .log("Received raw message: ${body}")
            .unmarshal().json(JsonLibrary.Jackson, Map.class)
            .routeId("manzan_msg:"+m_name)
            .setHeader(EVENT_TYPE, constant(ManzanEventType.WATCH_MSG))
            .setHeader("session_id", simple("${body[sessionId]}"))
            .setHeader("data_map", simple("${body}"))
            .marshal().json(true) //TODO: skip this if we are applying a format
            .setBody(simple("${body}\n"))
            .process(exchange -> {
                if (null != m_formatter) {
                    exchange.getIn().setBody(m_formatter.format(getDataMap(exchange)));
                }
            })
            .recipientList(constant(getRecipientList())).parallelProcessing().stopOnException().end();
    }
    //@formatter:on

}
