package com.github.theprez.manzan.routes.dest;

import java.util.Map;

import org.apache.camel.Exchange;
import com.github.theprez.manzan.routes.ManzanGenericCamelRoute;

public class EmailDestination extends ManzanGenericCamelRoute {

    public EmailDestination(final String _name, final String _type, final String _smtpServer, final int _port, final String _format, final Map<String, String> _uriParams, final Map<String, Object> _headerParams) {
        super(_name, _type, (_port == -1) ? _smtpServer : _smtpServer + ":" + _port, _format, _uriParams, _headerParams);
    }

    @Override
    protected void customPostProcess(Exchange exchange) {
        exchange.getIn().removeHeaders("*");
        exchange.getIn().setHeader("contentType", "text/plain");
    }
}
