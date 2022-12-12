package com.github.theprez.manzan.configuration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;

import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.manzan.routes.ManzanRoute;
import com.github.theprez.manzan.routes.dest.FluentDMsgDestination;
import com.github.theprez.manzan.routes.dest.SentryMsgDestination;
import com.github.theprez.manzan.routes.dest.SlackMsgDestination;
import com.github.theprez.manzan.routes.dest.StreamMsgDestination;

public class DestinationConfig {
    private final Ini m_ini;

    public DestinationConfig(final File _f) throws InvalidFileFormatException, IOException {
        m_ini = new Ini(_f);
    }

    private int getRequiredInt(final String _name, final String _key) {
        return Integer.valueOf(getRequiredString(_name, _key));
    }

    private String getRequiredString(final String _name, final String _key) {
        final String ret = m_ini.get(_name, _key);
        if (StringUtils.isEmpty(ret)) {
            throw new RuntimeException("Required value for '" + _key + "' in [" + _name + "] not found");
        }
        return ret;
    }

    public Map<String, ManzanRoute> getRoutes() {
        final Map<String, ManzanRoute> ret = new LinkedHashMap<String, ManzanRoute>();
        for (final String section : m_ini.keySet()) {
            final String type = m_ini.get(section, "type");
            if (StringUtils.isEmpty(type)) {
                throw new RuntimeException("type not specified for destination [" + section + "]");
            }
            final String name = section;
            switch (type) {
                case "stdout":
                    ret.put(name, new StreamMsgDestination(name));
                    break;
                case "slack":
                    final String webhook = getRequiredString(name, "webhook");
                    final String channel = getRequiredString(name, "channel");
                    ret.put(name, new SlackMsgDestination(name, webhook, channel));
                    break;
                case "sentry":
                    final String dsn = getRequiredString(name, "dsn");
                    ret.put(name, new SentryMsgDestination(name, dsn));
                    break;
                case "fluentd":
                    final String tag = getRequiredString(name, "tag");
                    final String host = getRequiredString(name, "host");
                    final int port = getRequiredInt(name, "port");
                    ret.put(name, new FluentDMsgDestination(name, tag, host, port));
                    break;
                default:
                    throw new RuntimeException("Unknown destination type: " + type);
            }
        }
        return ret;
    }

}
