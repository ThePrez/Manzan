package com.github.theprez.manzan.routes.dest;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.component.es.ElasticsearchComponent;

import com.github.theprez.manzan.routes.ManzanGenericCamelRoute;

public class ElasticsearchDestination extends ManzanGenericCamelRoute {
    public ElasticsearchDestination(final CamelContext _context, final String _name, final String _clusterName, String _hostAddresses, String _user, String _password, final String _format, final Map<String, String> _componentOptions, final Map<String, String> _uriParams) {
        super(_context, _name, "elasticsearch", _clusterName + "?operation=Index", _format,  _uriParams, null, _componentOptions);
        ElasticsearchComponent elasticsearch = _context.getComponent("elasticsearch", ElasticsearchComponent.class);
        elasticsearch.setHostAddresses(_hostAddresses);
        elasticsearch.setUser(_user);
        elasticsearch.setPassword(_password);
    }

    @Override
    protected void customPostProcess(Exchange exchange) {
    }
}
