package com.github.theprez.manzan.routes;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.Exchange;

import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.manzan.ManzanMessageFormatter;
import org.apache.camel.spi.PropertyConfigurer;


public abstract class ManzanGenericCamelRoute extends ManzanRoute {

    private final String m_camelComponent;
    private final ManzanMessageFormatter m_formatter;
    private final Map<String, Object> m_headerParams;
    private final String m_path;
    protected final Map<String, String> m_uriParams;

    public ManzanGenericCamelRoute(final CamelContext _context, final String _name, final String _camelComponent, final String _path,
                                       final String _format, final Map<String, String> _uriParams, final Map<String, Object> _headerParams,
                                       final Map<String, String> componentOptions) {
        super(_name);
        m_uriParams = null == _uriParams ? new HashMap<String, String>(1) : _uriParams;
        m_headerParams = null == _headerParams ? new HashMap<String, Object>(1) : _headerParams;
        m_camelComponent = _camelComponent;
        m_path = _path;
        m_formatter = StringUtils.isEmpty(_format) ? null : new ManzanMessageFormatter(_format);

        Component component = _context.getComponent(_camelComponent, true, false);
        PropertyConfigurer configurer = component.getComponentPropertyConfigurer();
        componentOptions.forEach((key, value) -> {
            configurer.configure(_context, component, key, value,  true);
        });

    }

    protected abstract void customPostProcess(Exchange exchange);

    @Override
    public void configure() {
        from(getInUri())
                .process(exchange -> {
                    if (null != m_formatter) {
                        exchange.getIn().setBody(m_formatter.format(getDataMap(exchange)));
                    }
                    for (final Entry<String, Object> headerEntry : m_headerParams.entrySet()) {
                        exchange.getIn().setHeader(headerEntry.getKey(), headerEntry.getValue());
                    }
                })
                .setBody(simple("${body}\n"))
                // .wireTap("stream:out")
                .process(exchange -> {
                    customPostProcess(exchange);
                })
                .to(getTargetUri());
    }

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
            ret += entry.getValue();
            // TODO: what's right here? with "file://" targets Camel wants real paths
            // try {
            // ret += URLEncoder.encode(entry.getValue(), "UTF-8");
            // } catch (final UnsupportedEncodingException e) {
            // ret += URLEncoder.encode(entry.getValue());
            // }
            ret += "&";
        }
        ret = ret.replaceFirst("&$", "");
        System.out.println("target URI: " + ret);
        return ret;
    }
}
