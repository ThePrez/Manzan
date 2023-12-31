package com.github.theprez.manzan.routes.dest;

import java.util.Map;

import org.apache.camel.Exchange;

import com.github.theprez.manzan.routes.ManzanGenericCamelRoute;

public class DirDestination extends ManzanGenericCamelRoute {
    public DirDestination(final String _name, final String _file, final String _format, final Map<String, String> _uriParams) {
        super(_name, "file", _file, _format, _uriParams, null);
    }

    @Override
    protected void customPostProcess(Exchange exchange) {
    }
}
