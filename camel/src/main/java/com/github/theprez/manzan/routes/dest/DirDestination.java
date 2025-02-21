package com.github.theprez.manzan.routes.dest;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.model.RouteDefinition;

import com.github.theprez.manzan.routes.ManzanGenericCamelRoute;

public class DirDestination extends ManzanGenericCamelRoute {
    public DirDestination(final CamelContext _context, final String _name, final String _file, final String _format, final Map<String, String> _componentOptions, final Map<String, String> _uriParams) {
        super(_context, _name, "file", _file, _format, _uriParams, null, _componentOptions);
    }

    @Override
    protected void customPostProcess(Exchange exchange) {
    }
}
