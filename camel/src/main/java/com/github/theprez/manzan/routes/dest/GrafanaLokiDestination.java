package com.github.theprez.manzan.routes.dest;

import java.sql.Timestamp;

import com.github.theprez.manzan.ManzanEventType;
import com.github.theprez.manzan.routes.ManzanRoute;

import pl.mjaron.tinyloki.ILogStream;
import pl.mjaron.tinyloki.Labels;
import pl.mjaron.tinyloki.LogController;
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
                ILogStream stream = logController
                        .stream()
                        .l("app", "manzan")
                        .l(Labels.LEVEL, ((Integer) get(exchange, MSG_SEVERITY)) > 29 ? Labels.FATAL : Labels.INFO)
                        .l(SESSION_ID, getWatchName(exchange))
                        .l(MSG_MESSAGE_ID, getString(exchange, MSG_MESSAGE_ID))
                        .l(MSG_MESSAGE_TYPE, getString(exchange, MSG_MESSAGE_TYPE))
                        .l(MSG_SEVERITY, getString(exchange, MSG_SEVERITY))
                        .l(JOB, getString(exchange, JOB))
                        .l(MSG_SENDING_USRPRF, getString(exchange, MSG_SENDING_USRPRF))
                        .l(MSG_SENDING_PROGRAM_NAME, getString(exchange, MSG_SENDING_PROGRAM_NAME))
                        .l(MSG_SENDING_MODULE_NAME, getString(exchange, MSG_SENDING_MODULE_NAME))
                        // TODO: Uncomment once MSG_SENDING_PROCEDURE_NAME is not empty
                        // .l(MSG_SENDING_PROCEDURE_NAME, getString(exchange, MSG_SENDING_PROCEDURE_NAME))
                        .build();
                stream.log(Timestamp.valueOf(getString(exchange, MSG_MESSAGE_TIMESTAMP)).getTime(), getBody(exchange));
            } else {
                throw new RuntimeException("Grafana Loki route doesn't know how to process type "+type);
            }
        });
    }
    //@formatter:on
}
