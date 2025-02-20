package com.github.theprez.manzan.routes.dest;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.model.RouteDefinition;

import com.github.theprez.manzan.routes.ManzanGenericCamelRoute;

public class EmailDestination extends ManzanGenericCamelRoute {

    public EmailDestination(final CamelContext _context, final String _name, final String _type,
                            final String _smtpServer, final String _format, final int _port,
                            final Map<String, String> _componentOptions, final Map<String, String> _uriParams,
                            final Map<String, Object> _headerParams
    ) {
        super(_context, _name, _type, (_port == -1) ? _smtpServer : _smtpServer + ":" + _port, _format, _uriParams, _headerParams, _componentOptions);
    }

    @Override
    protected void customPostProcess(Exchange exchange) {
        exchange.getIn().removeHeaders("*");
        exchange.getIn().setHeader("contentType", "text/plain");
    }

    @Override
    protected void customRouteDefinition(RouteDefinition routeDefinition) {
    }
}
