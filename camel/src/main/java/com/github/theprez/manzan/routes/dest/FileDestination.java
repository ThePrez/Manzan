package com.github.theprez.manzan.routes.dest;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.model.RouteDefinition;

import com.github.theprez.manzan.routes.ManzanGenericCamelRoute;

public class FileDestination extends ManzanGenericCamelRoute {
    public FileDestination(final CamelContext _context, final String _name, final String _file, final String _format, final Map<String, String> _componentOptions, final Map<String, String> _uriParams) {
        super(_context, _name, "stream", "file", _format, addToMap(_uriParams, "fileName", _file), null, _componentOptions);
    }

    private static Map<String, String> addToMap(Map<String, String> _uriParams, String _key, String _val) {
        Map<String, String> ret = null == _uriParams ? new LinkedHashMap<String,String>(): _uriParams;
        ret.put(_key, _val);
        return ret;
    }

    @Override
    protected void customPostProcess(Exchange exchange) {
    }

    @Override
    protected void customRouteDefinition(RouteDefinition routeDefinition) {
    }
}
