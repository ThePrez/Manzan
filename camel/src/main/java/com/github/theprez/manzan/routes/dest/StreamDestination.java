package com.github.theprez.manzan.routes.dest;

import org.apache.camel.Exchange;

import com.github.theprez.manzan.routes.ManzanGenericCamelRoute;

public class StreamDestination extends ManzanGenericCamelRoute {
    public StreamDestination(final String _name, final String _format) {
        super(_name, "stream", "out", _format, null, null);
    }

    @Override
    protected void customPostProcess(Exchange exchange) {
    }
}
