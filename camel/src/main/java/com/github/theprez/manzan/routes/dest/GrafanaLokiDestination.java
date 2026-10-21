package com.github.theprez.manzan.routes.dest;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Map;

import com.github.theprez.manzan.InstanceContext;
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
    private int maxLabels = 15;
    private String[] labels = {};

    public GrafanaLokiDestination(final InstanceContext _ctx, final String _name, final String _url, final String _username, final String _password,
                                  final int _maxLabels, final String _labels) {
        super(_ctx, _name);

        if (_maxLabels != -1){
            maxLabels = _maxLabels;
        }
        if (_labels != null){
            labels = _labels.split(" ");
        }

        logController = TinyLoki
                .withUrl(_url + endpoint)
                .withBasicAuth(_username, _password)
                .start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                logController
                        .softStop()
                        .hardStop();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }));
    }

    private static final DateTimeFormatter SQL_TIMESTAMP_FMT = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .optionalStart().appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true).optionalEnd()
            .toFormatter();

    private long timestampKeyToLong(String key, Exchange exchange) {
        return LocalDateTime.parse(getString(exchange, key), SQL_TIMESTAMP_FMT)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
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

    private void addLabels(StreamBuilder builder, Exchange exchange) {
        Map<String, Object> dataMap = getDataMap(exchange);
        String[] keys = labels.length == 0 ?
                dataMap.keySet().toArray(new String[0]) :
                labels;

        int labelsAdded = 0;
        int i = 0;
        // Subtract 1 because we've already added a label for appLabelName
        while (labelsAdded < maxLabels - 1 && i < keys.length){
            String key = keys[i];
            try {
                String value = getString(exchange, key);
                if (!value.equals("")) {
                    builder.l(key, value);
                    labelsAdded++;
                }
            } catch (Exception e) {
                // Don't need to do anything here. It's expected that some keys might have null values
            }
            i++;
        }
    }

    @Override
    public void configure() {
        from(getInUri())
                .routeId(getRouteId()).process(exchange -> {
                    StreamBuilder builder = logController
                            .stream()
                            .l(appLabelName, appLabelValue);
                    addLabels(builder, exchange);
                    ILogStream stream = builder.build();
                    long timestamp = getTimestamp(exchange);
                    stream.log(timestamp, getBody(exchange, String.class));
                });
    }

    @Override
    protected void setEventType(ManzanEventType manzanEventType) {
    }
}


