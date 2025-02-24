package com.github.theprez.manzan.routes.dest;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.theprez.manzan.LocalHostResolver;
import com.github.theprez.manzan.ManzanEventType;
import com.github.theprez.manzan.routes.ManzanGenericCamelRoute;
import com.github.theprez.manzan.routes.ManzanRoute;
import com.github.theprez.manzan.routes.event.FileEvent;

public class PagerDutyDestination extends ManzanGenericCamelRoute {
    private final String m_routingKey;
    private final String m_component;
    private final String m_group;
    private final String m_classType;

    public PagerDutyDestination(final CamelContext _context, final String _name, final String routingKey, final String _component, final String _group, final String _classType, final String _format) {
        super(_context, _name, "https", "events.pagerduty.com/v2/enqueue", _format, null, null, null);
        this.m_routingKey = routingKey;
        this.m_component = _component;
        this.m_group = _group;
        this.m_classType = _classType;
        this.m_headerParams.put("Accept", "application/json");
        this.m_headerParams.put(Exchange.CONTENT_TYPE, "application/json");
        this.m_headerParams.put(Exchange.HTTP_METHOD, "POST");
    }

    @Override
    protected void customPostProcess(Exchange exchange) throws JsonProcessingException {
        Map<String, Object> dataMap = getDataMap(exchange);

        String defaultSummary;
        String severity;
        String timestamp;
        final ManzanEventType type = (ManzanEventType) exchange.getIn().getHeader(EVENT_TYPE);
        if (type == ManzanEventType.WATCH_MSG) {
            defaultSummary = getString(exchange, ManzanRoute.MSG_MESSAGE);
            severity = ((Integer) get(exchange, MSG_SEVERITY)) > SEVERITY_LIMIT ? "error" : "info";
            timestamp = getString(exchange, MSG_MESSAGE_TIMESTAMP);
        } else if (type == ManzanEventType.WATCH_VLOG) {
            defaultSummary = getString(exchange, ManzanRoute.LOG_ID);
            severity = "critical";
            timestamp = getString(exchange, LOG_TIMESTAMP);
        } else if (type == ManzanEventType.WATCH_PAL) {
            defaultSummary = getString(exchange, ManzanRoute.LOG_ID);
            severity = "critical";
            timestamp = getString(exchange, PAL_TIMESTAMP);
        } else {
            defaultSummary = getString(exchange, FileEvent.FILE_DATA);
            severity = "info";
            timestamp = null;
        }

        // Add required fields to payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("summary", this.m_formatter != null ? this.m_formatter.format(getDataMap(exchange)) : defaultSummary);
        payload.put("severity", severity);
        try {
            payload.put("source", LocalHostResolver.getFQDN());
        } catch (Exception e) {
            payload.put("source", "IBM i");
        }

        // Add optional fields to payload
        if(timestamp != null) {
            payload.put("timestamp", timestamp);
        }
        if(this.m_component != null) {
            payload.put("component", this.m_component);
        }
        if(this.m_group != null) {
            payload.put("group", this.m_group);
        }
        if(this.m_classType != null) {
            payload.put("class", this.m_classType);
        }
        payload.put("custom_details", dataMap);

        // Construct request data
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("payload", payload);
        requestData.put("routing_key", this.m_routingKey);
        requestData.put("event_action", "trigger");
        requestData.put("client", "Manzan");

        // Convert map to JSON and set as body
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonBody = objectMapper.writeValueAsString(requestData);
        exchange.getIn().setBody(jsonBody);
    }
}
