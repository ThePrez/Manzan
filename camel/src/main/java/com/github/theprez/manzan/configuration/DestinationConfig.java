package com.github.theprez.manzan.configuration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.ini4j.InvalidFileFormatException;

import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.manzan.routes.ManzanRoute;
import com.github.theprez.manzan.routes.dest.FluentDDestination;
import com.github.theprez.manzan.routes.dest.SentryDestination;
import com.github.theprez.manzan.routes.dest.SlackDestination;
import com.github.theprez.manzan.routes.dest.StreamDestination;

public class DestinationConfig extends Config{
    private Map<String, ManzanRoute> m_routes=null;
    public DestinationConfig(final File _f) throws InvalidFileFormatException, IOException {
        super(_f);
    }
    public synchronized Map<String, ManzanRoute> getRoutes() {
        if(null != m_routes) {
            return m_routes;
        }
        final Map<String, ManzanRoute> ret = new LinkedHashMap<String, ManzanRoute>();
        for (final String section : getIni().keySet()) {
            final String type = getIni().get(section, "type");
            if (StringUtils.isEmpty(type)) {
                throw new RuntimeException("type not specified for destination [" + section + "]");
            }
            if("false".equalsIgnoreCase(getIni().get(section, "enabled"))) {
                continue;
            }
            final String name = section;
            switch (type) {
                case "stdout":
                    ret.put(name, new StreamDestination(name));
                    break;
                case "slack":
                    final String webhook = getRequiredString(name, "webhook");
                    final String channel = getRequiredString(name, "channel");
                    final String format = getOptionalString(name, "format");
                    ret.put(name, new SlackDestination(name, webhook, channel, format));
                    break;
                case "sentry":
                    final String dsn = getRequiredString(name, "dsn");
                    ret.put(name, new SentryDestination(name, dsn));
                    break;
                case "fluentd":
                    final String tag = getRequiredString(name, "tag");
                    final String host = getRequiredString(name, "host");
                    final int port = getRequiredInt(name, "port");
                    ret.put(name, new FluentDDestination(name, tag, host, port));
                    break;
                default:
                    throw new RuntimeException("Unknown destination type: " + type);
            }
        }
        return m_routes = ret;
    }

}
