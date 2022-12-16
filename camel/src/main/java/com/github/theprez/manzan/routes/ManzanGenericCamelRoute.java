package com.github.theprez.manzan.routes;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.manzan.ManzanMessageFormatter;

public abstract class ManzanGenericCamelRoute extends ManzanRoute{

    private final Map<String, String> m_uriParams;
    private Map<String, Object> m_headerParams;
    private String m_camelComponent;
    private String m_path;
    private String m_format;

    public ManzanGenericCamelRoute(String _name, String _camelComponent,String _path, String _format, Map<String, String> _uriParams, Map<String, Object> _headerParams) {
        super(_name);
        m_uriParams=null == _uriParams ? new HashMap<String,String>(1) : _uriParams;
        m_headerParams=null == _headerParams ? new HashMap<String,Object>(1) : _headerParams;
        m_camelComponent = _camelComponent;
        m_path = _path;
        m_format = _format;
    }
    
    private String getTargetUri() {
        String ret = m_camelComponent;
        ret += "://";
        ret += m_path;
        if(m_uriParams.isEmpty()) {
            return ret;
        }
        ret += "?";
        for(Entry<String, String> entry : m_uriParams.entrySet()) {
            ret += entry.getKey();
            ret += "=";
            try {
                ret += URLEncoder.encode(entry.getValue(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                ret += URLEncoder.encode(entry.getValue());
            }
            ret += "&";
        }
        ret=ret.replaceFirst("&$", "");
        return ret;
    }

    

    @Override
    public void configure() {
        from(getInUri())
        .process(exchange -> {
            if(StringUtils.isNonEmpty(m_format)) {
            ManzanMessageFormatter formatter = new ManzanMessageFormatter(exchange.getIn().getBody(String.class));
            String formatted = formatter.format(getDataMap(exchange));
            exchange.getIn().setBody(formatted);
        }
            for(Entry<String, Object> headerEntry : m_headerParams.entrySet()) {
                exchange.getIn().setHeader(headerEntry.getKey(), headerEntry.getValue());
            }
        })
        .to(getTargetUri());
    }
}
