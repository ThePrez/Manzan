


package com.github.theprez.manzan.routes.event;

import java.io.IOException;
import java.util.List;

import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.manzan.ManzanMessageFormatter;
import com.github.theprez.manzan.routes.ManzanRoute;

public class WatchSql extends ManzanRoute {
    private final int m_interval;
    private final ManzanMessageFormatter m_formatter;
    private final String m_sql;

    public WatchSql(final String _name, final String _sql, final String _format,
                    final List<String> _destinations,
                    final int _interval)
            throws IOException {
        super(_name);
        m_interval = _interval;
        m_formatter = StringUtils.isEmpty(_format) ? null : new ManzanMessageFormatter(_format);
        m_sql = _sql;
        super.setRecipientList(_destinations);
    }

    @Override
    public void configure() {
        from("timer://foo?synchronous=true&period=" + m_interval)
                .routeId("manzan_sql:" + m_name)
                .setBody(constant(m_sql))
                .to("jdbc:jt400?outputType=StreamList")
                .split(body()).streaming().parallelProcessing()
                .setHeader("data_map", simple("${body}"))
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