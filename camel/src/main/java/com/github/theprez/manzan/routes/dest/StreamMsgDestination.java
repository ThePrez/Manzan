package com.github.theprez.manzan.routes.dest;

import com.github.theprez.manzan.routes.ManzanRoute;

public class StreamMsgDestination extends ManzanRoute {
    public StreamMsgDestination(final String _name) {
        super(_name);
    }

    @Override
    public void configure() {
        from(getInUri()).to("stream:out");
    }

}
