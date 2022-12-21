package com.github.theprez.manzan.routes.dest;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.component.twilio.TwilioComponent;

import com.github.theprez.manzan.routes.ManzanGenericCamelRoute;

public class TwilioDestination extends ManzanGenericCamelRoute {
    public TwilioDestination(CamelContext context, final String _name, final String _format, String sid, String token, final Map<String, String> _uriParams) {
        super(_name, "twilio", "message/create", _format, _uriParams, null);

        TwilioComponent twilio = context.getComponent("twilio", TwilioComponent.class);
        twilio.setUsername(sid);
        twilio.setPassword(token);
    }

    @Override
    protected void customPostProcess(Exchange exchange) {
        exchange.getIn().setHeader("CamelTwilio.body", exchange.getIn().getBody(String.class));
    }
}
