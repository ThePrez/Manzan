package com.github.theprez.manzan.routes.dest;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import com.github.theprez.manzan.routes.ManzanGenericCamelRoute;

public class ActiveMqDestination extends ManzanGenericCamelRoute {
    public ActiveMqDestination(final CamelContext _context, final String _name, final String _destType, final String _destName, final String _format, final Map<String, String> _componentOptions, final Map<String, String> _uriParams) {
        super(_context, _name, "activemq", _destType + ":" + _destName, _format, _uriParams, null, _componentOptions);
    }

    @Override
    protected void customPostProcess(Exchange exchange) {
    }
}