package com.github.theprez.manzan.routes.dest;

import org.fluentd.logger.FluentLogger;

import com.github.theprez.manzan.routes.ManzanRoute;

public class FluentDDestination extends ManzanRoute {
    private final String m_host;
    private final FluentLogger m_logger;
    private final int m_port;
    private final String m_tag;

    public FluentDDestination(final String _name, final String _tag, final String _host, final int _port) {
        super(_name);
        m_tag = _tag;
        m_host = _host;
        m_port = _port;
        m_logger = FluentLogger.getLogger(m_tag, m_host, m_port);
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

//@formatter:off
    @Override
    public void configure() {
        from(getInUri())
        .routeId(m_name).process(exchange -> {
            m_logger.log(m_tag, getDataMap(exchange));
        });
    }

//@formatter:on

}
