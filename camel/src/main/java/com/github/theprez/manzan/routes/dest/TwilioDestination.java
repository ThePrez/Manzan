package com.github.theprez.manzan.routes.dest;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.model.RouteDefinition;

import com.github.theprez.manzan.routes.ManzanGenericCamelRoute;

public class TwilioDestination extends ManzanGenericCamelRoute {
    public TwilioDestination(final CamelContext _context, final String _name, final String _format, Map<String, String> componentOptions, final Map<String, String> _uriParams) {
        super(_context, _name, "twilio", "message/create", _format, _uriParams, null, componentOptions);
    }

    @Override
    protected void customPostProcess(Exchange exchange) {
        exchange.getIn().setHeader("CamelTwilio.body", getBody(exchange, String.class));
    }

    @Override
    protected void customRouteDefinition(RouteDefinition routeDefinition) {
    }
}
