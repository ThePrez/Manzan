package com.github.theprez.manzan.configuration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.kafka.common.protocol.types.Field;
import org.ini4j.InvalidFileFormatException;
import org.ini4j.Profile.Section;

import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.manzan.routes.ManzanRoute;
import com.github.theprez.manzan.routes.dest.DirDestination;
import com.github.theprez.manzan.routes.dest.EmailDestination;
import com.github.theprez.manzan.routes.dest.FileDestination;
import com.github.theprez.manzan.routes.dest.FluentDDestination;
import com.github.theprez.manzan.routes.dest.GrafanaLokiDestination;
import com.github.theprez.manzan.routes.dest.HttpDestination;
import com.github.theprez.manzan.routes.dest.KafkaDestination;
import com.github.theprez.manzan.routes.dest.ActiveMqDestination;
import com.github.theprez.manzan.routes.dest.SentryDestination;
import com.github.theprez.manzan.routes.dest.SlackDestination;
import com.github.theprez.manzan.routes.dest.StreamDestination;
import com.github.theprez.manzan.routes.dest.TwilioDestination;

public class DestinationConfig extends Config {
    public static DestinationConfig get() throws InvalidFileFormatException, IOException {
        return new DestinationConfig(getConfigFile("dests.ini"));
    }

    private Map<String, ManzanRoute> m_routes = null;

    private DestinationConfig(final File _f) throws InvalidFileFormatException, IOException {
        super(_f);
    }

    public synchronized Map<String, ManzanRoute> getRoutes(CamelContext context) {
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
            final Map<String, String> componentOptions = getComponentOptions(name);
            switch (type) {
                case "stdout":
                    ret.put(name, new StreamDestination(context, name, format, componentOptions));
                    break;
                case "slack": {
                    final String webhook = getRequiredString(name, "webhook");
                    final String channel = getRequiredString(name, "channel");
                    ret.put(name, new SlackDestination( name, webhook, channel, format));
                }
                    break;
                case "kafka":
                    final String topic = getRequiredString(name, "topic");
                    ret.put(name, new KafkaDestination(context, name, topic, format, componentOptions, getUriAndHeaderParameters(name, sectionObj, "topic")));
                    break;
                case "activemq":
                    final String destName = getRequiredString(name, "destName");
                    String destType = getOptionalString(name, "destType");
                    destType = destType.equals("topic") ? "topic" : "queue";
                    ret.put(name, new ActiveMqDestination(context, name, destType, destName, format, componentOptions, getUriAndHeaderParameters(name, sectionObj, "destName", "destType")));
                    break;
                case "file":
                    final String file = getRequiredString(name, "file");
                    ret.put(name, new FileDestination(context, name, file, format, componentOptions, getUriAndHeaderParameters(name, sectionObj, "file")));
                    break;
                case "dir":
                    final String dir = getRequiredString(name, "dir");
                    ret.put(name, new DirDestination(context, name, dir, format, componentOptions, getUriAndHeaderParameters(name, sectionObj, "dir")));
                    break;
                case "sentry":
                    final String dsn = getRequiredString(name, "dsn");
                    ret.put(name, new SentryDestination(name, dsn));
                    break;
                case "fluentd": {
                    final String tag = getRequiredString(name, "tag");
                    final String host = getRequiredString(name, "host");
                    final int port = getRequiredInt(name, "port");
                    ret.put(name, new FluentDDestination(name, tag, host, port));
                }
                case "loki": {
                    final String url = getRequiredString(name, "url");
                    final String username = getRequiredString(name, "username");
                    final String password = getRequiredString(name, "password");
                    ret.put(name, new GrafanaLokiDestination( name, url, username, password, format));
                }
                    break;
                case "smtp":
                case "smtps":
                    final String server = getRequiredString(name, "server");
                    final int port = getOptionalInt(name, "port");
                    final EmailDestination d = new EmailDestination(context, name, type, server, format, port, componentOptions, getUriAndHeaderParameters(name, sectionObj, "server", "port"), null);
                    ret.put(name, d);
                    break;
                case "twilio":
                    // final String sid = getRequiredString(name, "sid");
                    // final String token = getRequiredString(name, "token");
                    ret.put(name, new TwilioDestination(context, name, format,
                    componentOptions,
                    getUriAndHeaderParameters(name, sectionObj, "sid", "token")));
                    break;
                case "http":
                case "https":
                    final String url = getRequiredString(name, "url");
                    ret.put(name, HttpDestination.get(context, name, type, url, format, componentOptions, getUriAndHeaderParameters(name, sectionObj, "url")));
                    break;
                default:
                    throw new RuntimeException("Unknown destination type: " + type);
            }
        }
        return m_routes = ret;
    }

    private Map<String, String> getUriAndHeaderParameters(final String _name, Section sectionObj, String... _exclusions) {
        final Map<String, String> pathParameters = new LinkedHashMap<String, String>();
        List<String> exclusions = new LinkedList<>(Arrays.asList(_exclusions));
        exclusions.addAll(Arrays.asList("type", "filter", "format"));
        for (final String sectionKey : sectionObj.keySet()) {
            if (exclusions.contains(sectionKey) || sectionKey.startsWith("componentOptions.")) {
                continue;
            }
            pathParameters.put(sectionKey, getRequiredString(_name, sectionKey));
        }
        return pathParameters;
    }

}
