package com.github.theprez.manzan.configuration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.github.theprez.manzan.routes.dest.*;

import org.apache.camel.CamelContext;
import org.ini4j.InvalidFileFormatException;
import org.ini4j.Profile.Section;

import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.manzan.routes.ManzanRoute;

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
                    ret.put(name, new SlackDestination(name, webhook, channel, format));
                }
                    break;
                case "kafka":
                    final String topic = getRequiredString(name, "topic");
                    ret.put(name, new KafkaDestination(context, name, topic, format, componentOptions, getUriAndHeaderParameters(name, sectionObj, "topic")));
                    break;
                case "splunk-hec":
                    final String splunkUrl = getRequiredString(name, "splunkUrl");
                    final String token = getRequiredString(name, "token");
                    getRequiredString(name, "index"); // This ensures that the user sets the index query parameter
                    ret.put(name, new SplunkDestination(context, name, splunkUrl, token, format, componentOptions, getUriAndHeaderParameters(name, sectionObj, "splunkUrl", "token")));
                    break;
                case "elasticsearch":
                    final String endpoint = getRequiredString(name, "endpoint");
                    final String apiKey = getRequiredString(name, "apiKey");
                    final String index = getRequiredString(name, "index");
                    ret.put(name, new ElasticsearchDestination(name, endpoint, apiKey, index));
                    break;
                case "activemq":
                    final String destName = getRequiredString(name, "destinationName");
                    String destType = getOptionalString(name, "destinationType");
                    destType = (destType != null && destType.equals("topic")) ? "topic" : "queue";
                    ret.put(name, new ActiveMqDestination(context, name, destType, destName, format, componentOptions, getUriAndHeaderParameters(name, sectionObj, "destinationName", "destinationType")));
                    break;
                case "google-pubsub":
                    final String projectId = getRequiredString(name, "projectId");
                    final String topicName = getRequiredString(name, "topicName");
                    ret.put(name, new GooglePubSubDestination(context, name, projectId, topicName, format, componentOptions, getUriAndHeaderParameters(name, sectionObj, "projectId", "topicName")));
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
                    final int maxLabels = getOptionalInt(name, "maxLabels");
                    final String labels = getOptionalString(name, "labels");
                    ret.put(name, new GrafanaLokiDestination(name, url, username, password, maxLabels, labels));
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
                    ret.put(name, new TwilioDestination(context, name, format,
                    componentOptions,
                    getUriAndHeaderParameters(name, sectionObj, "sid", "token")));
                    break;
                case "pagerduty":
                    final String routingKey = getRequiredString(name, "routingKey");
                    final String component = getOptionalString(name, "component");
                    final String group = getOptionalString(name, "group");
                    final String classType = getOptionalString(name, "class");
                    ret.put(name, new PagerDutyDestination(context, name, routingKey, component, group, classType, format));
                    break;
                case "mezmo":
                    final String ingestionKey = getRequiredString(name, "apiKey");
                    final String tags = getOptionalString(name, "tags");
                    final String app = getOptionalString(name, "app");
                    ret.put(name, new MezmoDestination(context, name, ingestionKey, tags, app, format));
                    break;
                case "http":
                case "https":
                    String url = getRequiredString(name, "url");
                    ret.put(name, HttpDestination.get(context, name, type, url, format, componentOptions, getUriAndHeaderParameters(name, sectionObj, "url")));
                    break;
                case "otlp":
                    url = getRequiredString(name, "url");
                    String errorRegex = getOptionalString(name, "errorRegex");
                    ret.put(name, OpenTelemetryDestination.get(context, name, url, format, errorRegex));
                    break;
                default:
                    throw new RuntimeException("Unknown destination type: " + type);
            }
        }
        return m_routes = ret;
    }

}
