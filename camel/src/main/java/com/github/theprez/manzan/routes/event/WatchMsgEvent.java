package com.github.theprez.manzan.routes.event;

import java.io.IOException;
import java.util.List;
import com.github.theprez.manzan.ManzanEventType;
import com.github.theprez.manzan.routes.ManzanRoute;

public class WatchMsgEvent extends ManzanRoute {

    private final String m_schema;
    private final int m_interval;
    private final int m_numToProcess;
    private String m_sessionId;

    public WatchMsgEvent(String _name, String _session_id, List<String> _destinations, String _schema, int _interval, int _numToProcess) throws IOException {
        super(_name);
        m_interval=_interval;
        m_numToProcess=_numToProcess;
        m_schema=_schema;
        super.setRecipientList(_destinations);
        m_sessionId = _session_id.trim().toUpperCase();
    }

    @Override
    public void configure() {        
        from("timer://foo?synchronous=false&period=" + m_interval)
        .routeId("manzan_msg:"+m_name)
        .setHeader(EVENT_TYPE, constant(ManzanEventType.WATCH_MSG))
        .setBody(constant("SeLeCt * fRoM " + m_schema + ".mAnZaNmSg wHeRe SESSION_ID = '"+m_sessionId+"' limit " + m_numToProcess ))
        .to("stream:out")
        .to("jdbc:jt400?outputType=StreamList")
        .split(body()).streaming().parallelProcessing()
            .setHeader("id", simple("${body[ORDINAL_POSITION]}"))
            .setHeader("session_id", simple("${body[SESSION_ID]}"))
            .setHeader("data_map", simple("${body}"))
            .marshal().json(true)
            .setBody(simple("${body}\n"))
            .recipientList(constant(getRecipientList())).parallelProcessing().stopOnException().end()
            .setBody(simple("delete fRoM " + m_schema + ".mAnZaNmSg where ORDINAL_POSITION = ${header.id} WITH NC"))
            .to("jdbc:jt400").to("stream:err");
    }
    
}
