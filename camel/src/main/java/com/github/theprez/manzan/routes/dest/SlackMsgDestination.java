package com.github.theprez.manzan.routes.dest;

import com.github.theprez.manzan.routes.ManzanRoute;

public class SlackMsgDestination extends ManzanRoute {
    private final String m_channel;
    private final String m_webhook;

    public SlackMsgDestination(final String _name, final String _webhook, final String _channel) {
        super(_name);
        m_webhook = _webhook;
        m_channel = _channel;
    }

    @Override
    public void configure() {
        from(getInUri()).to("slack:" + m_channel + "?webhookUrl=" + m_webhook);

    }

}
