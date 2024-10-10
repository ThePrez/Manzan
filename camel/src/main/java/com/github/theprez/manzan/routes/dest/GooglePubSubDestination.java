package com.github.theprez.manzan.routes.dest;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.component.google.pubsub.GooglePubsubComponent;
import org.apache.camel.component.google.pubsub.GooglePubsubConstants;

import com.github.theprez.manzan.ManzanEventType;
import com.github.theprez.manzan.routes.ManzanGenericCamelRoute;

public class GooglePubSubDestination extends ManzanGenericCamelRoute {
    public GooglePubSubDestination(CamelContext context, final String _name, final String _format, final String _projectId, final String _topicName, final String _serviceAccountKey, final Map<String, String> _uriParams) {
        super(_name, "google-pubsub", _projectId + ":" + _topicName, _format, _uriParams, null);
        GooglePubsubComponent pubsub = context.getComponent("google-pubsub", GooglePubsubComponent.class);
        pubsub.setServiceAccountKey(_serviceAccountKey);
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
}