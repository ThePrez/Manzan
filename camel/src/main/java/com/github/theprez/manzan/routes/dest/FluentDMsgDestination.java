package com.github.theprez.manzan.routes.dest;

import java.util.HashMap;
import java.util.Map;

import org.fluentd.logger.FluentLogger;

import com.github.theprez.manzan.routes.ManzanRoute;

public class FluentDMsgDestination extends ManzanRoute {

    private final Map<String, FluentLogger> m_fluentLoggers = new HashMap<String, FluentLogger>();
    private final String m_host;
    private final FluentLogger m_logger;
    private final int m_port;
    private final String m_tag;

    public FluentDMsgDestination(final String _name, final String _tag, final String _host, final int _port) {
        super(_name);
        m_tag = _tag;
        m_host = _host;
        m_port = _port;
        m_logger = FluentLogger.getLogger(_tag, _host, _port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    m_logger.close();
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void configure() {
        from("direct:msg_fluentd").process(exchange -> {
            m_logger.log(m_tag, getDataMap(exchange));
        });
    }

}
