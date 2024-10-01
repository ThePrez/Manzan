package com.github.theprez.manzan.routes.dest;

import java.sql.Timestamp;

import com.github.theprez.manzan.ManzanEventType;
import com.github.theprez.manzan.routes.ManzanRoute;

import pl.mjaron.tinyloki.ILogStream;
import pl.mjaron.tinyloki.Labels;
import pl.mjaron.tinyloki.LogController;
import pl.mjaron.tinyloki.StreamBuilder;
import pl.mjaron.tinyloki.TinyLoki;

public class GrafanaLokiDestination extends ManzanRoute {
    private final LogController logController;

    public GrafanaLokiDestination(final String _name, final String _url, final String _username, final String _password,
            final String _format) {
        super(_name);

        logController = TinyLoki
                .withUrl(_url + "/loki/api/v1/push")
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

    //@formatter:off
    @Override
    public void configure() {
        from(getInUri())
        .routeId(m_name).process(exchange -> {
            final ManzanEventType type = (ManzanEventType) exchange.getIn().getHeader(EVENT_TYPE);
            if(ManzanEventType.WATCH_MSG == type) {
                StreamBuilder builder = logController
                        .stream()
                        .l("app", "manzan")
                        .l(Labels.LEVEL, ((Integer) get(exchange, MSG_SEVERITY)) > 29 ? Labels.FATAL : Labels.INFO)
                        .l(SESSION_ID, getWatchName(exchange));

                String[] keys = {
                    MSG_MESSAGE_ID,
                    MSG_MESSAGE_TYPE,
                    MSG_SEVERITY,
                    JOB,
                    MSG_SENDING_USRPRF,
                    MSG_SENDING_PROGRAM_NAME,
                    MSG_SENDING_MODULE_NAME,
                    MSG_SENDING_PROCEDURE_NAME
                };

                for (String key: keys) {
                    String value = getString(exchange, key);
                    if(!value.equals("")) {
                        builder.l(key, value);
                    }
                }

                ILogStream stream = builder.build();
                stream.log(Timestamp.valueOf(getString(exchange, MSG_MESSAGE_TIMESTAMP)).getTime(), getBody(exchange));
            } else {
                throw new RuntimeException("Grafana Loki route doesn't know how to process type "+type);
            }
        });
    }
    //@formatter:on
}
