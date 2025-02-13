package com.github.theprez.manzan.routes.dest;

import java.sql.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.github.theprez.manzan.ManzanEventType;
import com.github.theprez.manzan.routes.ManzanRoute;

import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.SentryLevel;
import io.sentry.protocol.Message;
import io.sentry.protocol.SentryId;
import io.sentry.protocol.User;

public class SentryDestination extends ManzanRoute {
    private static SentryDestination m_singleton = null;

    public SentryDestination(final String _name, final String _dsn) {
        super(_name);
        synchronized (SentryDestination.class) {
            if (null != m_singleton) {
                throw new RuntimeException("Only one Sentry configuration is allowed");
            }
            m_singleton = this;
        }

        Sentry.init(options -> {
            options.setDsn(_dsn);
        });
    }

    @Override
    public void configure() {
        from(getInUri())
                .routeId(m_name)
                .convertBodyTo(String.class)
                .process(exchange -> {
                    final SentryEvent event = new SentryEvent();
                    event.setTag(SESSION_ID, getWatchName(exchange)); // TODO: Check if SESSION_ID or just session id
                    event.setEventId(new SentryId(UUID.randomUUID()));
                    event.setExtras(getDataMap(exchange));
                    event.setPlatform("IBM i");
                    event.setTag("runtime", "IBM i");
                    event.setTag("runtime.name", "IBM i");
                    event.setDist("PASE");
                    event.setTransaction(getString(exchange, MSG_ORDINAL_POSITION));

                    final User user = new User();
                    user.setUsername(getString(exchange, MSG_SENDING_USRPRF));
                    event.setUser(user);

                    final Message message = new Message();
                    message.setMessage(getBody(exchange, String.class));
                    event.setMessage(message);

                    SentryLevel severity;
                    String timestamp;
                    final List<String> fingerprints = new LinkedList<String>();

                    final ManzanEventType type = (ManzanEventType) exchange.getIn().getHeader(EVENT_TYPE);
                    if (ManzanEventType.WATCH_MSG == type) {
                        severity = ((Integer) get(exchange, MSG_SEVERITY)) > SEVERITY_LIMIT ? SentryLevel.ERROR : SentryLevel.INFO;
                        timestamp = MSG_MESSAGE_TIMESTAMP;
                        fingerprints.add(getString(exchange, MSG_MESSAGE_ID));
                        fingerprints.add(getString(exchange, MSG_SENDING_PROCEDURE_NAME));
                        fingerprints.add(getString(exchange, MSG_SENDING_MODULE_NAME));
                        fingerprints.add(getString(exchange, MSG_SENDING_PROGRAM_NAME));
                    } else if (type == ManzanEventType.WATCH_VLOG) {
                        severity = SentryLevel.INFO;
                        timestamp = LOG_TIMESTAMP;
                        fingerprints.add(getString(exchange, MAJOR_CODE));
                        fingerprints.add(getString(exchange, MINOR_CODE));
                    } else if (type == ManzanEventType.WATCH_PAL) {
                        severity = SentryLevel.INFO;
                        timestamp = PAL_TIMESTAMP;
                        fingerprints.add(getString(exchange, SYSTEM_REFERENCE_CODE));
                    } else {
                        throw new RuntimeException("Sentry route doesn't know how to process type " + type);
                    }

                    event.setLevel(severity);
                    event.setTimestamp(Date.valueOf(getString(exchange, timestamp))); // TODO: Verify date is valid
                    event.setFingerprints(fingerprints);
                    Sentry.captureEvent(event);
                });
    }
}