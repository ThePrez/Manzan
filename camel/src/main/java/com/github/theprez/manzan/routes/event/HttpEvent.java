package com.github.theprez.manzan.routes.event;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.camel.model.dataformat.JsonLibrary;

import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.manzan.ManzanEventType;
import com.github.theprez.manzan.ManzanMessageFilter;
import com.github.theprez.manzan.ManzanMessageFormatter;
import com.github.theprez.manzan.routes.ManzanRoute;

public class HttpEvent extends ManzanRoute {
    private final ManzanMessageFilter m_filter;
    private final String m_url;
    private final ManzanMessageFormatter m_formatter;
    private final int m_interval;
    private final Map<String, String> m_headerParams;

    public HttpEvent(final String _name, final String _url, final String _format, final List<String> _destinations,
                     final String _filter, final int _interval, Map<String, String> _headerParams) throws IOException {
        super(_name);
        super.setRecipientList(_destinations);
        m_url = _url;
        m_formatter = StringUtils.isEmpty(_format) ? null : new ManzanMessageFormatter(_format);
        m_filter = new ManzanMessageFilter(_filter);
        m_interval = _interval;
        m_headerParams = _headerParams;
    }

    @Override
    public void configure() {
       from("timer://foo?period=" + m_interval + "&synchronous=true")
                .routeId(m_name)
                .setHeader(EVENT_TYPE, constant(ManzanEventType.HTTP))
                 .process(exchange -> {
                     for (Map.Entry<String, String> header: m_headerParams.entrySet()){
                         exchange.getIn().setHeader(header.getKey(), header.getValue());
                     }
                 })
                .to(m_url)
                .unmarshal().json(JsonLibrary.Jackson)
                .setHeader("data_map", simple("${body}"))
                .marshal().json(true) // TODO: skip this if we are applying a format
               .convertBodyTo(String.class, "UTF-8") // Need to convert it to string, otherwise it will just be a byte sequence
                .filter(exchange -> {
                    String body = exchange.getIn().getBody().toString();
                    return m_filter.matches(body);
                })
                .process(exchange -> {
                    if (null != m_formatter) {
                        exchange.getIn().setBody(m_formatter.format(getDataMap(exchange)));
                    }
                })
                .recipientList(constant(getRecipientList())).stopOnException();
    }
}
