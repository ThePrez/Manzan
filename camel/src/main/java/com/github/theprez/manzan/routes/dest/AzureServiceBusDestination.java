package com.github.theprez.manzan.routes.dest;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;

import com.github.theprez.manzan.routes.ManzanGenericCamelRoute;

public class AzureServiceBusDestination extends ManzanGenericCamelRoute {
    public AzureServiceBusDestination(final CamelContext _context, final String _name, final String _topicOrQueueName,
            final String _serviceBusType, final String _connectionString, final String _tokenCredential,
            final String _format, final Map<String, String> _componentOptions, final Map<String, String> _uriParams) {
        super(_context, _name, "azure-servicebus", _topicOrQueueName, _format, _uriParams, null, _componentOptions);

        if (!_serviceBusType.equals("queue") && !_serviceBusType.equals("topic")) {
            throw new RuntimeException("Invalid service bus type: " + _serviceBusType + ". Must be 'queue' or 'topic'.");
        } else if (_connectionString == null && _tokenCredential == null) {
            throw new RuntimeException("Connection string or token credential must be provided.");
        }
    }

    @Override
    protected void customPostProcess(Exchange exchange) {
    }
}
