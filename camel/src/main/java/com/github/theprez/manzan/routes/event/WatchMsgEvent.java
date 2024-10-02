package com.github.theprez.manzan.routes.event;

import java.io.IOException;
import java.util.List;

import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.manzan.ManzanEventType;
import com.github.theprez.manzan.ManzanMessageFormatter;
import com.github.theprez.manzan.routes.ManzanRoute;

public class WatchMsgEvent extends ManzanRoute {

    private final String m_schema;
    private final String m_sessionId;
    private final ManzanEventType m_eventType;
    private final String m_table;
    private final int m_interval;
    private final int m_numToProcess;
    private final ManzanMessageFormatter m_formatter;

    public WatchMsgEvent(final String _name, final String _session_id, final String _format,
            final List<String> _destinations, final String _schema, final ManzanEventType _eventType,
            final int _interval, final int _numToProcess) throws IOException {
        super(_name);
        m_schema = _schema;
        m_sessionId = _session_id.trim().toUpperCase();
        m_eventType = _eventType;
        m_interval = _interval;
        m_numToProcess = _numToProcess;
        m_formatter = StringUtils.isEmpty(_format) ? null : new ManzanMessageFormatter(_format);
        super.setRecipientList(_destinations);

        if (m_eventType == ManzanEventType.WATCH_MSG) {
            m_table = "MANZANMSG";
        } else if (m_eventType == ManzanEventType.WATCH_VLOG) {
            m_table = "MANZANVLOG";
        } else {
            m_table = "MANZANPAL";
        }
    }

    //@formatter:off
    @Override
    public void configure() {
        from("timer://foo?synchronous=true&period=" + m_interval)
            .routeId("manzan_msg:" + m_name)
            .setHeader(EVENT_TYPE, constant(m_eventType))
            .setBody(constant("SELECT * FROM " + m_schema + "." + m_table + " WHERE SESSION_ID = '" + m_sessionId
                    + "' LIMIT " + m_numToProcess))
            // .to("stream:out")
            .to("jdbc:jt400?outputType=StreamList")
            .split(body()).streaming().parallelProcessing()
            .setHeader("id", simple("${body[ORDINAL_POSITION]}"))
            .setHeader("session_id", simple("${body[SESSION_ID]}"))
            .setHeader("data_map", simple("${body}"))
            .marshal().json(true) // TODO: skip this if we are applying a format
            .setBody(simple("${body}\n"))
            .process(exchange -> {
                if (null != m_formatter) {
                    exchange.getIn().setBody(m_formatter.format(getDataMap(exchange)));
                }
            })
            .recipientList(constant(getRecipientList())).parallelProcessing().stopOnException().end()
            .setBody(simple(
                    "DELETE FROM " + m_schema + "." + m_table + " WHERE ORDINAL_POSITION = ${header.id} WITH NC"))
            .to("jdbc:jt400").to("stream:err");
    }
    //@formatter:on
}
