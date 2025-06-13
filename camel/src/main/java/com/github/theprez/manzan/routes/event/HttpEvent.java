package com.github.theprez.manzan.routes.event;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
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
    private int m_interval;

    public HttpEvent(final String _name, final String _url, final String _format, final List<String> _destinations,
                     final String _filter, final int _interval) throws IOException {
        super(_name);
        super.setRecipientList(_destinations);
        m_url = _url;
        m_formatter = StringUtils.isEmpty(_format) ? null : new ManzanMessageFormatter(_format);
        m_filter = new ManzanMessageFilter(_filter);
        m_interval = _interval;
    }

    @Override
    public void configure() {

        String ABORT = "ABORT";
        String CONTINUE = "CONTINUE";

       from("timer://foo?period=" + m_interval + "&synchronous=true")
                .routeId(m_name)
                .setHeader(EVENT_TYPE, constant(ManzanEventType.HTTP))
                .to(m_url)
                .unmarshal().json(JsonLibrary.Jackson)
                .setHeader("data_map", simple("${body}"))
                .marshal().json(true) // TODO: skip this if we are applying a format
                .convertBodyTo(String.class)// Need to convert it to string, otherwise it will just be a byte sequence
                .setBody(simple("${body}"))
                .process(exchange -> {
                    if (null != m_formatter) {
                        exchange.getIn().setBody(m_formatter.format(getDataMap(exchange)));
                    }
                })
                .convertBodyTo(String.class, "UTF-8")
                .recipientList(constant(getRecipientList())).stopOnException();
    }
}
