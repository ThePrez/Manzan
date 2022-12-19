package com.github.theprez.manzan.routes;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.camel.Exchange;

import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.manzan.ManzanMessageFormatter;


public abstract class ManzanGenericCamelRoute extends ManzanRoute {

    private final String m_camelComponent;
    private final String m_format;
    private final Map<String, Object> m_headerParams;
    private final String m_path;
    private final Map<String, String> m_uriParams;

    public ManzanGenericCamelRoute(final String _name, final String _camelComponent, final String _path,
            final String _format, final Map<String, String> _uriParams, final Map<String, Object> _headerParams) {
        super(_name);
        m_uriParams = null == _uriParams ? new HashMap<String, String>(1) : _uriParams;
        m_headerParams = null == _headerParams ? new HashMap<String, Object>(1) : _headerParams;
        m_camelComponent = _camelComponent;
        m_path = _path;
        m_format = _format;
    }

    protected abstract void customPostProcess(Exchange exchange);
    //@formatter:off
    @Override
    public void configure() {
        from(getInUri())
                .process(exchange -> {
                    if (StringUtils.isNonEmpty(m_format)) {
                        final ManzanMessageFormatter formatter = new ManzanMessageFormatter(m_format);
                        final String formatted = formatter.format(getDataMap(exchange));
                        exchange.getIn().setBody(formatted);
                    }
                    for (final Entry<String, Object> headerEntry : m_headerParams.entrySet()) {
                        exchange.getIn().setHeader(headerEntry.getKey(), headerEntry.getValue());
                    }
                })
                .setBody(simple("${body}\n"))
                .process(exchange -> {customPostProcess(exchange);})
                .to(getTargetUri());
    }
    //@formatter:on

    private String getTargetUri() {
        String ret = m_camelComponent;
        ret += "://";
        ret += m_path;
        if (m_uriParams.isEmpty()) {
            return ret;
        }
        ret += "?";
        for (final Entry<String, String> entry : m_uriParams.entrySet()) {
            ret += entry.getKey();
            ret += "=";
            try {
                ret += URLEncoder.encode(entry.getValue(), "UTF-8");
            } catch (final UnsupportedEncodingException e) {
                ret += URLEncoder.encode(entry.getValue());
            }
            ret += "&";
        }
        ret = ret.replaceFirst("&$", "");
        return ret;
    }
}
