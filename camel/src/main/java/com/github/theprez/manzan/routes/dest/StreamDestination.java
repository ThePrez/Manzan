package com.github.theprez.manzan.routes.dest;

import com.github.theprez.manzan.routes.ManzanRoute;

public class StreamDestination extends ManzanRoute {
    public StreamDestination(final String _name) {
        super(_name);
    }

    @Override
    public void configure() {
        from(getInUri())
        .routeId(m_name).to("stream:out");
    }

}
