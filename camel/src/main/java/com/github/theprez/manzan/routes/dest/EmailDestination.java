package com.github.theprez.manzan.routes.dest;

import java.util.Map;

import com.github.theprez.manzan.routes.ManzanGenericCamelRoute;

public class EmailDestination extends ManzanGenericCamelRoute {

    public EmailDestination(String _name, String _smtpServer, String _format, Map<String,String> _uriParams, Map<String,Object> _headerParams) {
        super(_name, "smtp",  _smtpServer,_format, _uriParams, _headerParams);
    }    
}


