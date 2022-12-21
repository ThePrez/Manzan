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

    private static final String FILE_DATA = "FILE_DATA";
    private static final String FILE_NAME = "FILE_NAME";
    private static final String FILE_PATH = "FILE_PATH";
    private final File m_file;
    private final ManzanMessageFilter m_filter;
    private final ManzanMessageFormatter m_formatter;

    public FileEvent(final String _name, final File _f, final String _format, final List<String> _destinations, final String _filter) throws IOException {
        super(_name);
        m_file = _f;
        super.setRecipientList(_destinations);
        m_formatter = StringUtils.isEmpty(_format) ? null: new ManzanMessageFormatter(_format);
        m_filter = new ManzanMessageFilter(_filter);
    }

    public FileEvent(final String _name, final String _f, final String _format, final List<String> _destinations, final String _filter) throws IOException {
        this(_name, new File(_f), _format, _destinations, _filter);
    }

//@formatter:off
    @Override
    public void configure() {
        from("timer://foo?period=5000&synchronous=true")
        .routeId(m_name)
        .setHeader(EVENT_TYPE, constant(ManzanEventType.FILE))
        .setBody(constant(m_file.getAbsolutePath()))
        .process((exchange) -> {
            final File f = new File(exchange.getIn().getBody().toString());
            exchange.getIn().setBody(new FileNewContentsReader(f, "*TAG"));
        })
        .split(body().tokenize("\n")).streaming().parallelProcessing(false).stopOnException()
        .convertBodyTo(String.class)
        .process(exchange -> {
                exchange.getIn().setHeader("abort", m_filter.matches(exchange.getIn().getBody(String.class))?"continue":"abort");
        })
        .choice().when(simple("${header.abort} != 'abort'"))
        .process(exchange -> {
            final Map<String,Object> data_map = new LinkedHashMap<String,Object>();
            data_map.put(FILE_NAME, m_file.getName());
            data_map.put(FILE_PATH, m_file.getAbsolutePath());
            data_map.put(FILE_DATA, exchange.getIn().getBody(String.class).replace("\r",""));
            exchange.getIn().setHeader("data_map", data_map);
            exchange.getIn().setBody(data_map);
        })
        .marshal().json(true)  //TODO: skip this if we are applying a format
        .setBody(simple("${body}\n"))
        .process(exchange -> {
            if (null != m_formatter) {
                exchange.getIn().setBody(m_formatter.format(getDataMap(exchange)));
            }
        })
        .convertBodyTo(String.class,"UTF-8")
        .recipientList(constant(getRecipientList())).stopOnException()
        ;
    }
    //@formatter:on
}
