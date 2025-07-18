


package com.github.theprez.manzan.routes.event;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.manzan.ManzanEventType;
import com.github.theprez.manzan.ManzanMessageFormatter;
import com.github.theprez.manzan.routes.ManzanRoute;

public class WatchSql extends ManzanRoute {
    private final int m_interval;
    private final ManzanMessageFormatter m_formatter;
    private final String m_sql;
    final Map<String, String> dataMapInjection;


    public WatchSql(final String _name, final String _sql, final String _format,
                    final List<String> _destinations,
                    final int _interval, final Map<String, String> _dataMapInjection)
            throws IOException {
        super(_name);
        m_interval = _interval;
        m_formatter = StringUtils.isEmpty(_format) ? null : new ManzanMessageFormatter(_format);
        m_sql = _sql;
        dataMapInjection = _dataMapInjection;
        super.setRecipientList(_destinations);
        setEventType(ManzanEventType.SQL);
    }

    protected void setEventType(ManzanEventType eventType){
        m_eventType = eventType;
    }

    @Override
    public void configure() {
        from("timer://foo?synchronous=true&period=" + m_interval)
                .routeId("manzan_sql:" + m_name)
                .setBody(constant(m_sql))
                .to("jdbc:jt400?outputType=StreamList")
                .split(body()).streaming().parallelProcessing()
                .process(exchange -> {
                    Map<String, Object> dataMap = exchange.getIn().getBody(Map.class);
                    injectIntoDataMap(dataMap, dataMapInjection);
                    exchange.getIn().setHeader("data_map", dataMap);
                    exchange.getIn().setBody(dataMap);
                })
                .setHeader(EVENT_TYPE, constant(m_eventType))
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
                .end();
    }

}