package com.github.theprez.manzan.routes.dest;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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

public class MezmoDestination extends ManzanGenericCamelRoute {
    private final String m_app;

    public MezmoDestination(final CamelContext _context, final String _name, final String _apiKey, final String _tags, final String _app, final String _format) {
        super(_context, _name, "https", "logs.mezmo.com/logs/ingest", _format, null, null, null);
        this.m_app = _app;
        this.m_headerParams.put(Exchange.CONTENT_TYPE, "application/json");
        this.m_headerParams.put(Exchange.HTTP_METHOD, "POST");
        this.m_headerParams.put("apikey", _apiKey);
        if(_tags != null) {
            this.m_uriParams.put("tags", _tags);
        }
        try {
            this.m_uriParams.put("hostname", LocalHostResolver.getFQDN());
        } catch (Exception e) {
            this.m_uriParams.put("hostname", "IBM i");
        }
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
            severity = ((Integer) get(exchange, MSG_SEVERITY)) > SEVERITY_LIMIT ? "ERROR" : "INFO";
            timestamp = getString(exchange, MSG_MESSAGE_TIMESTAMP);
        } else if (type == ManzanEventType.WATCH_VLOG) {
            defaultSummary = getString(exchange, ManzanRoute.LOG_ID);
            severity = "ERROR";
            timestamp = getString(exchange, LOG_TIMESTAMP);
        } else if (type == ManzanEventType.WATCH_PAL) {
            defaultSummary = getString(exchange, ManzanRoute.LOG_ID);
            severity = "ERROR";
            timestamp = getString(exchange, PAL_TIMESTAMP);
        } else {
            defaultSummary = getString(exchange, FileEvent.FILE_DATA);
            severity = "INFO";
            timestamp = new Date().toString();
        }

        // Construct log line
        Map<String, Object> line = new HashMap<>();
        line.put("timestamp", timestamp);
        line.put("line", this.m_formatter != null ? this.m_formatter.format(getDataMap(exchange)) : defaultSummary);
        line.put("level", severity);
        line.put("meta", dataMap);
        if(this.m_app != null) {
            line.put("app", this.m_app);
        }

        // Construct log lines
        final List<Map<String, Object>> lines = new ArrayList<>();
        lines.add(line);

        // Construct request data
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("lines", lines);

        // Convert map to JSON and set as body
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonBody = objectMapper.writeValueAsString(requestData);
        exchange.getIn().setBody(jsonBody);
    }
}
