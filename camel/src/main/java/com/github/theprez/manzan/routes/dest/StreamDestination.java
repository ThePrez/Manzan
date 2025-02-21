package com.github.theprez.manzan.routes.dest;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.model.RouteDefinition;

import com.github.theprez.manzan.routes.ManzanGenericCamelRoute;

import java.util.Map;

public class StreamDestination extends ManzanGenericCamelRoute {
    public StreamDestination(final CamelContext _context, final String _name, final String _format, final Map<String, String> _componentOptions) {
        super(_context, _name, "stream", "out", _format, null, null, _componentOptions);
    }

    @Override
    protected void customPostProcess(Exchange exchange) {
    }
}
