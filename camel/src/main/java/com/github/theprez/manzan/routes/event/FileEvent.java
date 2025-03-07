package com.github.theprez.manzan.routes.event;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.manzan.ManzanEventType;
import com.github.theprez.manzan.ManzanMessageFilter;
import com.github.theprez.manzan.ManzanMessageFormatter;
import com.github.theprez.manzan.routes.ManzanRoute;

import io.github.theprez.jfiletail.FileNewContentsReader;

public class FileEvent extends ManzanRoute {

    public static final String FILE_DATA = "FILE_DATA";
    public static final String FILE_NAME = "FILE_NAME";
    public static final String FILE_PATH = "FILE_PATH";
    private final File m_file;
    private final int m_interval;
    private final ManzanMessageFilter m_filter;
    private final ManzanMessageFormatter m_formatter;

    public FileEvent(final String _name, final File _f, final String _format, final List<String> _destinations,
            final String _filter, final int _interval) throws IOException {
        super(_name);
        m_file = _f;
        super.setRecipientList(_destinations);
        m_interval = _interval;
        m_formatter = StringUtils.isEmpty(_format) ? null : new ManzanMessageFormatter(_format);
        m_filter = new ManzanMessageFilter(_filter);
    }

    public FileEvent(final String _name, final String _f, final String _format, final List<String> _destinations,
            final String _filter, final int _interval) throws IOException {
        this(_name, new File(_f), _format, _destinations, _filter, _interval);
    }

    @Override
    public void configure() {
        from("timer://foo?period=" + m_interval + "&synchronous=true")
                .routeId(m_name)
                .setHeader(EVENT_TYPE, constant(ManzanEventType.FILE))
                .setBody(constant(m_file.getAbsolutePath()))
                .process((exchange) -> {
                    final File f = new File(getBody(exchange, String.class));
                    exchange.getIn().setBody(new FileNewContentsReader(f, "*TAG"));
                })
                .split(body().tokenize("\n")).streaming().parallelProcessing(false).stopOnException()
                .convertBodyTo(String.class)
                .process(exchange -> {
                    String bodyStr = getBody(exchange, String.class);
                    exchange.getIn().setHeader("abort",
                            m_filter.matches(bodyStr) && StringUtils.isNonEmpty(bodyStr) ? "continue" : "abort");
                })
                .choice().when(simple("${header.abort} != 'abort'"))
                .process(exchange -> {
                    final Map<String, Object> data_map = new LinkedHashMap<String, Object>();
                    data_map.put(FILE_NAME, m_file.getName());
                    data_map.put(FILE_PATH, m_file.getAbsolutePath());
                    data_map.put(FILE_DATA, getBody(exchange, String.class).replace("\r", ""));
                    exchange.getIn().setHeader("data_map", data_map);
                    exchange.getIn().setBody(data_map);
                })
                .marshal().json(true) // TODO: skip this if we are applying a format
                .setBody(simple("${body}\n"))
                .process(exchange -> {
                    if (null != m_formatter) {
                        exchange.getIn().setBody(m_formatter.format(getDataMap(exchange)));
                    }
                })
                .convertBodyTo(String.class, "UTF-8")
                .recipientList(constant(getRecipientList())).stopOnException();
    }
}
