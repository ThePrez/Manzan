package com.github.theprez.manzan.routes.dest;

import org.apache.commons.lang3.StringUtils;

import com.github.theprez.manzan.ManzanMessageFormatter;
import com.github.theprez.manzan.routes.ManzanRoute;

public class SlackMsgDestination extends ManzanRoute {
    private final String m_channel;
    private final String m_webhook;
    private final String m_format;

    public SlackMsgDestination(final String _name, final String _webhook, final String _channel, String _format) {
        super(_name);
        m_webhook = _webhook;
        m_channel = _channel;
        m_format = _format;
    }

    @Override
    public void configure() {
        if (StringUtils.isEmpty(m_format)) {
            from(getInUri()).to("slack:" + m_channel + "?webhookUrl=" + m_webhook);
        } else {
            from(getInUri()).convertBodyTo(String.class, "UTF-8")
                    .process(exchange -> {
                        ManzanMessageFormatter formatter = new ManzanMessageFormatter(getDataMap(exchange));
                        String formatted = formatter.format(m_format);
                        exchange.getIn().setBody(formatted);

                    })
                    .to("slack:" + m_channel + "?webhookUrl=" + m_webhook);
        }
    }

}
