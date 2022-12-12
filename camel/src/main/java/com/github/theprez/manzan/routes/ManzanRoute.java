package com.github.theprez.manzan.routes;

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

public abstract class ManzanRoute extends RouteBuilder {

    protected static String getWatchName(final Exchange exchange) {
        final Object watchNameObject = exchange.getIn().getHeader("session_id");
        if (null == watchNameObject) {
            throw new RuntimeException("Couldn't figure out watch ID");
        }
        return watchNameObject.toString().toLowerCase().trim();
    }

    protected final String m_name;

    public ManzanRoute(final String _name) {
        m_name = _name;
    }

    @Override
    public abstract void configure();

    protected Object get(final Exchange _exchange, final String _attr) {
        final Map<String, Object> data = getDataMap(_exchange);
        final Object val = data.get(_attr);
        if (null == val) {
            throw new RuntimeException("Missing value for " + _attr);
        }
        return val;
    }

    protected Map<String, Object> getDataMap(final Exchange _exchange) {
        return (Map<String, Object>) _exchange.getIn().getHeader("data_map");
    }

    protected String getInUri() {
        return "direct:" + m_name;
    }

    protected String getString(final Exchange _exchange, final String _attr) {
        return "" + get(_exchange, _attr);
    }
}
