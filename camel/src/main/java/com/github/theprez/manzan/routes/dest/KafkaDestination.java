package com.github.theprez.manzan.routes.dest;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.model.RouteDefinition;

import com.github.theprez.manzan.routes.ManzanGenericCamelRoute;

public class KafkaDestination extends ManzanGenericCamelRoute {
    public KafkaDestination(final CamelContext _context, final String _name, final String _topic, final String _format, final Map<String, String> _componentOptions, final Map<String, String> _uriParams) {
        super(_context, _name, "kafka", _topic, _format,  _uriParams, null, _componentOptions);
    }

    @Override
    protected void customPostProcess(Exchange exchange) {
    }
}
