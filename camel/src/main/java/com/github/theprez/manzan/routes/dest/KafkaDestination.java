package com.github.theprez.manzan.routes.dest;

import java.util.Map;

import org.apache.camel.Exchange;

import com.github.theprez.manzan.routes.ManzanGenericCamelRoute;

public class KafkaDestination extends ManzanGenericCamelRoute {
    public KafkaDestination(final String _name, final String _topic, final String _format, final Map<String, String> _uriParams) {
        super(_name, "kafka", _topic, _format, _uriParams, null);
    }

    @Override
    protected void customPostProcess(Exchange exchange) {
    }
}
