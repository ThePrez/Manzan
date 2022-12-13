package com.github.theprez.manzan.routes.event;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.theprez.manzan.ManzanEventType;
import com.github.theprez.manzan.ManzanMessageFilter;
import com.github.theprez.manzan.routes.ManzanRoute;

import io.github.theprez.jfiletail.FileNewContentsReader;

public class FileEvent extends ManzanRoute{

    private static final String FILE_NAME = "FILE_NAME";
    private static final String FILE_PATH = "FILE_PATH";
    private static final String FILE_DATA = "FILE_DATA";
    private final File m_file;
    private final ManzanMessageFilter m_filter;

    public FileEvent(String _name, File _f, List<String> _destinations, final String _filter) throws IOException {
        super(_name);
        m_file=_f;
        super.setRecipientList(_destinations);
        m_filter = new ManzanMessageFilter(_filter);
    }

    public FileEvent(String _name, String _f, List<String> _destinations, final String _filter) throws IOException {
        this(_name, new File(_f), _destinations, _filter);
    }

    @Override
    public void configure() {
        from("timer://foo?period=5000&synchronous=true")
        .routeId("file://"+m_file.getAbsolutePath())
        .setHeader(EVENT_TYPE, constant(ManzanEventType.FILE))
        .setBody(constant(m_file.getAbsolutePath()))
        .process((exchange) -> {
            File f = new File(exchange.getIn().getBody().toString());
            exchange.getIn().setBody(new FileNewContentsReader(f, "*TAG"));
        })
        .split(body().tokenize("\n")).streaming().parallelProcessing(false).stopOnException()
        .convertBodyTo(String.class)
        .process(exchange -> {
                exchange.getIn().setHeader("abort", m_filter.matches(exchange.getIn().getBody(String.class))?"continue":"abort");
                System.out.println("abort="+exchange.getIn().getHeader("abort"));
        })
        .choice().when(simple("${header.abort} != 'abort'"))
        .process(exchange -> {
            Map<String,Object> data_map = new LinkedHashMap<String,Object>();
            data_map.put(FILE_NAME, m_file.getName());
            data_map.put(FILE_PATH, m_file.getAbsolutePath());
            data_map.put(FILE_DATA, exchange.getIn().getBody(String.class));
            exchange.getIn().setHeader("data_map", data_map);
        })
        .recipientList(constant(getRecipientList())).stopOnException()
        ;
    }
}
