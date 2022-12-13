package com.github.theprez.manzan.routes;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

import com.github.theprez.jcmdutils.StringUtils;

public abstract class ManzanRoute extends RouteBuilder {

    protected static final String EVENT_TYPE = "event_type";

    protected static String getWatchName(final Exchange exchange) {
        final Object watchNameObject = exchange.getIn().getHeader("session_id");
        if (null == watchNameObject) {
            throw new RuntimeException("Couldn't figure out watch ID");
        }
        return watchNameObject.toString().toLowerCase().trim();
    }

    protected final String m_name;
    private String m_recipientList="";

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

    protected void setRecipientList(List<String> _destinations) throws IOException {
        String destinationsStr="";
        for(String dest : _destinations) {
            if(StringUtils.isEmpty(dest)) {
                continue;
            }
            destinationsStr+= "direct:"+dest.toLowerCase().trim()+",";
        }
        m_recipientList = destinationsStr.replaceFirst(",$", "").trim();
        
       if(StringUtils.isEmpty(m_recipientList)) {
        throw new IOException("Message watch for '"+m_name+"' has no valid destinations");
    }   
    }
    protected String getRecipientList() {
        return m_recipientList;
    }

}
