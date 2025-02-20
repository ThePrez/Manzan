package com.github.theprez.manzan.routes.dest;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.component.google.pubsub.GooglePubsubComponent;
import org.apache.camel.component.google.pubsub.GooglePubsubConstants;
import org.apache.camel.model.RouteDefinition;

import com.github.theprez.manzan.ManzanEventType;
import com.github.theprez.manzan.routes.ManzanGenericCamelRoute;

public class GooglePubSubDestination extends ManzanGenericCamelRoute {
    public GooglePubSubDestination(final CamelContext _context, final String _name, final String _projectId, final String _topicName, final String _format, final Map<String, String> _componentOptions, final Map<String, String> _uriParams) {
        super(_context, _name, "google-pubsub", _projectId + ":" + _topicName, _format, _uriParams, null, _componentOptions);
        GooglePubsubComponent pubsub = _context.getComponent("google-pubsub", GooglePubsubComponent.class);
        pubsub.init();
        pubsub.start();
    }

    @Override
    protected void customPostProcess(Exchange exchange) {
        final ManzanEventType type = (ManzanEventType) exchange.getIn().getHeader(EVENT_TYPE);
        if (ManzanEventType.WATCH_MSG == type) {
            exchange.getIn().setHeader(GooglePubsubConstants.PUBLISH_TIME, getString(exchange, MSG_MESSAGE_TIMESTAMP));
        } else if (ManzanEventType.WATCH_VLOG == type) {
            // exchange.getIn().setHeader(GooglePubsubConstants.PUBLISH_TIME, getString(exchange, LOG_TIMESTAMP));
        } else if (ManzanEventType.WATCH_PAL == type) {
            // exchange.getIn().setHeader(GooglePubsubConstants.PUBLISH_TIME, getString(exchange, PAL_TIMESTAMP));
        }

        Map<String, String> map = new HashMap<>();
        for (Map.Entry<String, Object> entry : getDataMap(exchange).entrySet()) {
            map.put(entry.getKey(), entry.getValue().toString());
        }
        exchange.getIn().setHeader(GooglePubsubConstants.ATTRIBUTES, map);
    }

    @Override
    protected void customRouteDefinition(RouteDefinition routeDefinition) {
    }
}