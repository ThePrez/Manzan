package com.github.theprez.manzan.routes.dest;

import java.util.Map;

import com.github.theprez.manzan.routes.ManzanGenericCamelRoute;

public class EmailDestination extends ManzanGenericCamelRoute {

    public EmailDestination(final String _name, final String _smtpServer, final String _format, final Map<String, String> _uriParams, final Map<String, Object> _headerParams) {
        super(_name, "smtp", _smtpServer, _format, _uriParams, _headerParams);
    }
}
