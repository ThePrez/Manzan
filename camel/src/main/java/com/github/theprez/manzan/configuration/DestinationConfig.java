package com.github.theprez.manzan.configuration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.ini4j.InvalidFileFormatException;
import org.ini4j.Profile.Section;

import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.manzan.routes.ManzanRoute;
import com.github.theprez.manzan.routes.dest.EmailDestination;
import com.github.theprez.manzan.routes.dest.FluentDDestination;
import com.github.theprez.manzan.routes.dest.KafkaDestination;
import com.github.theprez.manzan.routes.dest.SentryDestination;
import com.github.theprez.manzan.routes.dest.SlackDestination;
import com.github.theprez.manzan.routes.dest.StreamDestination;

public class DestinationConfig extends Config {
    public static DestinationConfig get() throws InvalidFileFormatException, IOException {
        return new DestinationConfig(getConfigFile("dests.ini"));
    }

    private Map<String, ManzanRoute> m_routes = null;

    private DestinationConfig(final File _f) throws InvalidFileFormatException, IOException {
        super(_f);
    }

    public synchronized Map<String, ManzanRoute> getRoutes() {
        if (null != m_routes) {
            return m_routes;
        }
        final Map<String, ManzanRoute> ret = new LinkedHashMap<String, ManzanRoute>();
        for (final String section : getIni().keySet()) {
            final String type = getIni().get(section, "type");
            if (StringUtils.isEmpty(type)) {
                throw new RuntimeException("type not specified for destination [" + section + "]");
            }
            if ("false".equalsIgnoreCase(getIni().get(section, "enabled"))) {
                continue;
            }
            final String name = section;
            final Section sectionObj = getIni().get(name);
            final String format = getOptionalString(name, "format");
            switch (type) {
                case "stdout":
                    ret.put(name, new StreamDestination(name, getOptionalString(name, "format")));
                    break;
                case "slack": {
                    final String webhook = getRequiredString(name, "webhook");
                    final String channel = getRequiredString(name, "channel");
                    ret.put(name, new SlackDestination(name, webhook, channel, format));
                }
                    break;
                case "kafka":
                    final String topic = getRequiredString(name, "topic");
                    ret.put(name, new KafkaDestination(name, topic, format, getUriParameters(name, sectionObj, "topic")));
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
                case "smtp":
                    final String server = getRequiredString(name, "server");
                    final EmailDestination d = new EmailDestination(name, server, format, getUriParameters(name, sectionObj, "server"), null);
                    ret.put(name, d);
                    break;
                default:
                    throw new RuntimeException("Unknown destination type: " + type);
            }
        }
        return m_routes = ret;
    }

    private Map<String, String> getUriParameters(final String _name, Section sectionObj, String... _exclusions) {
        final Map<String, String> pathParameters = new LinkedHashMap<String, String>();
        List<String> exclusions = new LinkedList<>(Arrays.asList(_exclusions));
        exclusions.addAll(Arrays.asList("type", "filter", "format"));
        for (final String sectionKey : sectionObj.keySet()) {
            if (exclusions.contains(sectionKey)) {
                continue;
            }
            pathParameters.put(sectionKey, getRequiredString(_name, sectionKey));
        }
        return pathParameters;
    }

}
