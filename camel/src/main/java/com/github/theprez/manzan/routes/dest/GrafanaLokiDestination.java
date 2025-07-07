package com.github.theprez.manzan.routes.dest;

import java.sql.Timestamp;
import java.util.Map;

import com.github.theprez.manzan.ManzanEventType;
import com.github.theprez.manzan.routes.ManzanRoute;

import org.apache.camel.Exchange;
import pl.mjaron.tinyloki.ILogStream;
import pl.mjaron.tinyloki.LogController;
import pl.mjaron.tinyloki.StreamBuilder;
import pl.mjaron.tinyloki.TinyLoki;

public class GrafanaLokiDestination extends ManzanRoute {
    private final LogController logController;
    private final static String endpoint = "/loki/api/v1/push";
    private final static String appLabelName = "app";
    private final static String appLabelValue = "manzan";

    public GrafanaLokiDestination(final String _name, final String _url, final String _username, final String _password,
                                  final String _format) {
        super(_name);

        logController = TinyLoki
                .withUrl(_url + endpoint)
                .withBasicAuth(_username, _password)
                .start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    logController
                            .softStop()
                            .hardStop();
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private long timestampKeyToLong(String key, Exchange exchange) {
        return Timestamp.valueOf(getString(exchange, key)).getTime();
    }

    private long getTimestamp(Exchange exchange) {
        long timestamp;
        final ManzanEventType type = (ManzanEventType) exchange.getIn().getHeader(EVENT_TYPE);
        switch (type) {
            case WATCH_MSG:
                timestamp = timestampKeyToLong(MSG_MESSAGE_TIMESTAMP, exchange);
                break;
            case WATCH_PAL:
                timestamp = timestampKeyToLong(PAL_TIMESTAMP, exchange);
                break;
            case WATCH_VLOG:
                timestamp = timestampKeyToLong(LOG_TIMESTAMP, exchange);
                break;
            case AUDIT:
                timestamp = timestampKeyToLong(ENTRY_TIMESTAMP, exchange);
                break;
            default:
                timestamp = java.time.Instant.now().toEpochMilli();

        }
        return timestamp;
    }

    private void addKeyValuePairsToBuilder(StreamBuilder builder, Exchange exchange) {
        Map<String, Object> dataMap = getDataMap(exchange);
        String[] keys = dataMap.keySet().toArray(new String[0]);

        for (String key : keys) {
            try {
                String value = getString(exchange, key);
                if (!value.equals("")) {
                    builder.l(key, value);
                }
            } catch (Exception e) {
                // Don't need to do anything here. It's expected that some keys might have null values
            }
        }
    }

    @Override
    public void configure() {
        from(getInUri())
                .routeId(m_name).process(exchange -> {
                    StreamBuilder builder = logController
                            .stream()
                            .l(appLabelName, appLabelValue);
                    addKeyValuePairsToBuilder(builder, exchange);
                    ILogStream stream = builder.build();
                    long timestamp = getTimestamp(exchange);
                    stream.log(timestamp, getBody(exchange, String.class));
                });
    }

    @Override
    protected void setEventType(ManzanEventType manzanEventType) {
    }
}