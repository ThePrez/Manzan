package com.github.theprez.manzan.routes.dest;

import org.apache.commons.lang3.StringUtils;

import com.github.theprez.manzan.ManzanMessageFormatter;
import com.github.theprez.manzan.routes.ManzanRoute;

public class SlackDestination extends ManzanRoute {
    private final String m_channel;
    private final String m_webhook;
    private final ManzanMessageFormatter m_format;

    public SlackDestination(final String _name, final String _webhook, final String _channel, String _format) {
        super(_name);
        m_webhook = _webhook;
        m_channel = _channel;
        m_format = StringUtils.isEmpty(_format) ? null : new ManzanMessageFormatter(_format);
    }

    @Override
    public void configure() {
        if (null == m_format) {
            from(getInUri())
            .routeId(m_name)
            .to("slack:" + m_channel + "?webhookUrl=" + m_webhook);
        } else {
            from(getInUri())
        .routeId(m_name).convertBodyTo(String.class, "UTF-8")
                    .process(exchange -> {
                        String formatted = m_format.format(getDataMap(exchange));
                        exchange.getIn().setBody(formatted);

                    })
                    .to("slack:" + m_channel + "?webhookUrl=" + m_webhook);
        }
    }

}
