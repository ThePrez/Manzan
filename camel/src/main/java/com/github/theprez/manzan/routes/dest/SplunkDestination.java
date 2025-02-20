package com.github.theprez.manzan.routes.dest;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.model.RouteDefinition;

import com.github.theprez.manzan.routes.ManzanGenericCamelRoute;

public class SplunkDestination extends ManzanGenericCamelRoute {
    public SplunkDestination(final CamelContext _context, final String _name, final String _splunkUrl, final String _token, final String _format, final Map<String, String> _componentOptions, final Map<String, String> _uriParams) {
        super(_context, _name, "splunk-hec", _splunkUrl + "/" + _token, _format,  _uriParams, null, _componentOptions);
    }

    @Override
    protected void customPostProcess(Exchange exchange) {
    }

    @Override
    protected void customRouteDefinition(RouteDefinition routeDefinition) {
    }
}
