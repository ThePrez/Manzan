package com.github.theprez.manzan.routes.dest;

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

//@formatter:off
    @Override
    public void configure() {
        from(getInUri())
        .routeId(m_name)
        .convertBodyTo(String.class)
        .process(exchange -> {
            final ManzanEventType type = (ManzanEventType) exchange.getIn().getHeader(EVENT_TYPE);
            if(ManzanEventType.WATCH_MSG == type) {
            System.out.println("sentry");
            final SentryEvent event = new SentryEvent();
            final String watch = getWatchName(exchange);
            final SentryId id = new SentryId(UUID.randomUUID());
            event.setTag("session id", watch);
            event.setEventId(id);
            event.setExtras(getDataMap(exchange));
            final User user = new User();
            user.setUsername(getString(exchange, MSG_SENDING_USRPRF));
            event.setUser(user);
            event.setPlatform("IBM i");
            event.setTag("runtime", "IBM i");
            event.setTag("runtime.name", "IBM i");
            event.setDist("PASE");
            event.setTransaction(getString(exchange, MSG_ORDINAL_POSITION));

            SentryLevel level;
            final int sev = (Integer) get(exchange, MSG_SEVERITY);
            if (sev > SEVERITY_LIMIT) {
                level = SentryLevel.ERROR;
            } else {
                level = SentryLevel.INFO;
            }

            event.setLevel(level);
            final Message message = new Message();
            final String messageStr = getString(exchange, MSG_MESSAGE_ID) + ": " + getString(exchange, MSG_MESSAGE);
            message.setMessage(messageStr);
            final List<String> fingerprints = new LinkedList<String>();
            fingerprints.add(getString(exchange, MSG_MESSAGE_ID));
            fingerprints.add(getString(exchange, MSG_SENDING_PROCEDURE_NAME));
            fingerprints.add(getString(exchange, MSG_SENDING_MODULE_NAME));
            fingerprints.add(getString(exchange, MSG_SENDING_PROGRAM_NAME));
            event.setFingerprints(fingerprints);
            event.setMessage(message);
            Sentry.captureEvent(event);
            } else {
                throw new RuntimeException("Sentry route doesn't know how to process type "+type);
            }
        });
    }
    //@formatter:on
}
