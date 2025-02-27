package com.github.theprez.manzan.routes.dest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;

import com.github.theprez.manzan.routes.ManzanGenericCamelRoute;

public class HttpDestination extends ManzanGenericCamelRoute {

    public static HttpDestination get(final CamelContext _context, final String _name, String _type, final String _url, final String _format, final Map<String, String> _componentOptions, Map<String, String> _parameters) {
        Map<String, Object> headerParameters = new LinkedHashMap<String,Object>();
        Map<String, String> uriParameters = new LinkedHashMap<String,String>();
        String hostVal = _url.replaceFirst("^http(s)?://", "").replaceAll("\\/.*","");
        headerParameters.put("Host", hostVal);
        headerParameters.put("User-Agent", "Manzan/1.0");
        for(Entry<String, String> parmEntry : _parameters.entrySet()) {
            if("Host".equalsIgnoreCase(parmEntry.getKey())) {
                headerParameters.put("Host", parmEntry.getValue());
            }else if(Exchange.CONTENT_TYPE.equalsIgnoreCase(parmEntry.getKey())) {
                headerParameters.put(Exchange.CONTENT_TYPE, parmEntry.getValue());
            } else if(Exchange.CONTENT_ENCODING.equalsIgnoreCase(parmEntry.getKey())) {
                headerParameters.put(Exchange.CONTENT_ENCODING, parmEntry.getValue());
            } else if (parmEntry.getKey().startsWith("Camel")) {
                headerParameters.put(parmEntry.getKey(), parmEntry.getValue());
            } else {
                uriParameters.put(parmEntry.getKey(), parmEntry.getValue());
            }
        }
        return new HttpDestination(_context, _name, _type, _url, _format, _componentOptions, uriParameters, headerParameters);
    }
    private HttpDestination(final CamelContext _context, final String _name, String _type, final String _url, final String _format, final Map<String, String> _componentOptions, Map<String, String> _uriParams, Map<String, Object> _headerParams) {
        super(_context, _name, _type, _url.replaceFirst("^http(s)?://", ""), _format, _uriParams, _headerParams,_componentOptions);
    }

    @Override
    protected void customPostProcess(Exchange exchange) {
    }
}
