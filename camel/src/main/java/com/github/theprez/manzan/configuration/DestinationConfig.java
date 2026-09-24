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
import com.github.theprez.manzan.InstanceContext;
import com.github.theprez.manzan.routes.ManzanRoute;

public class DestinationConfig extends Config {

    /**
     * Get DestinationConfig for a specific instance.
     */
    public static DestinationConfig get(final InstanceContext ctx) throws InvalidFileFormatException, IOException {
        return new DestinationConfig(ctx, getConfigFile(ctx, "dests.ini"));
    }

    /**
     * Get DestinationConfig for the default instance.
     *
     * @deprecated Use {@link #get(InstanceContext)} for multi-instance support.
     */
    @Deprecated
    public static DestinationConfig get() throws InvalidFileFormatException, IOException {
        return get(InstanceContext.getDefault());
    }

    private final InstanceContext m_ctx;
    private Map<String, ManzanRoute> m_routes = null;

    private DestinationConfig(final InstanceContext ctx, final File _f) throws InvalidFileFormatException, IOException {
        super(_f);
        m_ctx = ctx;
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
                    ret.put(name, new StreamDestination(context, m_ctx, name, format, componentOptions));
                    break;
                case "slack": {
                    final String webhook = getRequiredString(name, "webhook");
                    final String channel = getRequiredString(name, "channel");
                    ret.put(name, new SlackDestination(m_ctx, name, webhook, channel, format));
                }
                    break;
                case "kafka":
                    final String topic = getRequiredString(name, "topic");
                    ret.put(name, new KafkaDestination(context, m_ctx, name, topic, format, componentOptions, getUriAndHeaderParameters(name, sectionObj, "topic")));
                    break;
                case "splunk-hec":
                    final String splunkUrl = getRequiredString(name, "splunkUrl");
                    final String token = getRequiredString(name, "token");
                    getRequiredString(name, "index");
                    ret.put(name, new SplunkDestination(context, m_ctx, name, splunkUrl, token, format, componentOptions, getUriAndHeaderParameters(name, sectionObj, "splunkUrl", "token")));
                    break;
                case "azure-servicebus":
                    final String topicOrQueueName = getRequiredString(name, "topicOrQueueName");
                    final String serviceBusType = getRequiredString(name, "serviceBusType");
                    final String connectionString = getOptionalString(name, "connectionString");
                    final String tokenCredential = getOptionalString(name, "tokenCredential");
                    ret.put(name, new AzureServiceBusDestination(context, m_ctx, name, topicOrQueueName, serviceBusType, connectionString, tokenCredential, format, componentOptions, getUriAndHeaderParameters(name, sectionObj, "topicOrQueueName")));
                    break;
                case "elasticsearch":
                    final String endpoint = getRequiredString(name, "endpoint");
                    final String apiKey = getRequiredString(name, "apiKey");
                    final String index = getRequiredString(name, "index");
                    ret.put(name, new ElasticsearchDestination(m_ctx, name, endpoint, apiKey, index));
                    break;
                case "opensearch": {
                    final String osEndpoint  = getRequiredString(name, "endpoint");
                    final String osIndex     = getRequiredString(name, "index");
                    final String osAuthType  = getOptionalString(name, "authType");
                    final String osApiKey    = getOptionalString(name, "apiKey");
                    final String osUsername  = getOptionalString(name, "username");
                    final String osPassword  = getOptionalString(name, "password");
                    final String osRegion    = getOptionalString(name, "awsRegion");
                    final String osService   = getOptionalString(name, "awsService");
                    ret.put(name, new OpenSearchDestination(
                            m_ctx, name, osEndpoint, osIndex,
                            osAuthType, osApiKey, osUsername, osPassword,
                            osRegion, osService));
                    break;
                }
                case "activemq":
                    final String destName = getRequiredString(name, "destinationName");
                    String destType = getOptionalString(name, "destinationType");
                    destType = (destType != null && destType.equals("topic")) ? "topic" : "queue";
                    ret.put(name, new ActiveMqDestination(context, m_ctx, name, destType, destName, format, componentOptions, getUriAndHeaderParameters(name, sectionObj, "destinationName", "destinationType")));
                    break;
                case "google-pubsub":
                    final String projectId = getRequiredString(name, "projectId");
                    final String topicName = getRequiredString(name, "topicName");
                    ret.put(name, new GooglePubSubDestination(context, m_ctx, name, projectId, topicName, format, componentOptions, getUriAndHeaderParameters(name, sectionObj, "projectId", "topicName")));
                    break;
                case "file":
                    final String file = getRequiredString(name, "file");
                    ret.put(name, new FileDestination(context, m_ctx, name, file, format, componentOptions, getUriAndHeaderParameters(name, sectionObj, "file")));
                    break;
                case "dir":
                    final String dir = getRequiredString(name, "dir");
                    ret.put(name, new DirDestination(context, m_ctx, name, dir, format, componentOptions, getUriAndHeaderParameters(name, sectionObj, "dir")));
                    break;
                case "sentry":
                    final String dsn = getRequiredString(name, "dsn");
                    ret.put(name, new SentryDestination(m_ctx, name, dsn));
                    break;
                case "fluentd": {
                    final String tag = getRequiredString(name, "tag");
                    final String host = getRequiredString(name, "host");
                    final int port = getRequiredInt(name, "port");
                    ret.put(name, new FluentDDestination(m_ctx, name, tag, host, port));
                }
                case "loki": {
                    final String url = getRequiredString(name, "url");
                    final String username = getRequiredString(name, "username");
                    final String password = getRequiredString(name, "password");
                    final int maxLabels = getOptionalInt(name, "maxLabels");
                    final String labels = getOptionalString(name, "labels");
                    ret.put(name, new GrafanaLokiDestination(m_ctx, name, url, username, password, maxLabels, labels));
                }
                    break;
                case "smtp":
                case "smtps":
                    final String server = getRequiredString(name, "server");
                    final int port = getOptionalInt(name, "port");
                    final EmailDestination d = new EmailDestination(context, m_ctx, name, type, server, format, port, componentOptions, getUriAndHeaderParameters(name, sectionObj, "server", "port"), null);
                    ret.put(name, d);
                    break;
                case "twilio":
                    ret.put(name, new TwilioDestination(context, m_ctx, name, format,
                    componentOptions,
                    getUriAndHeaderParameters(name, sectionObj, "sid", "token")));
                    break;
                case "pagerduty":
                    final String routingKey = getRequiredString(name, "routingKey");
                    final String component = getOptionalString(name, "component");
                    final String group = getOptionalString(name, "group");
                    final String classType = getOptionalString(name, "class");
                    ret.put(name, new PagerDutyDestination(context, m_ctx, name, routingKey, component, group, classType, format));
                    break;
                case "mezmo":
                    final String ingestionKey = getRequiredString(name, "apiKey");
                    final String tags = getOptionalString(name, "tags");
                    final String app = getOptionalString(name, "app");
                    ret.put(name, new MezmoDestination(context, m_ctx, name, ingestionKey, tags, app, format));
                    break;
                case "http":
                case "https":
                    String url = getRequiredString(name, "url");
                    ret.put(name, HttpDestination.get(context, m_ctx, name, type, url, format, componentOptions, getUriAndHeaderParameters(name, sectionObj, "url")));
                    break;
                case "otlp": {
                    final String otlpEndpoint    = getRequiredString(name, "endpoint");
                    final String otlpHeaders     = getOptionalString(name, "headers");
                    final String otlpCaCert      = getOptionalString(name, "caCertPath");
                    final String otlpClientCert  = getOptionalString(name, "clientCertPath");
                    final String otlpClientKey   = getOptionalString(name, "clientKeyPath");
                    final int    otlpBatch       = getOptionalInt(name, "batchSize");
                    final int    otlpBatchTimeout = getOptionalInt(name, "batchTimeoutMs");
                    final int    otlpTimeout     = getOptionalInt(name, "timeoutMs");
                    final String otlpErrorRegex  = getOptionalString(name, "errorRegex");
                    ret.put(name, new OpenTelemetryDestination(
                            m_ctx, name, otlpEndpoint, otlpHeaders,
                            otlpCaCert, otlpClientCert, otlpClientKey,
                            otlpBatch      > 0 ? otlpBatch      : 512,
                            otlpBatchTimeout > 0 ? otlpBatchTimeout : 5000,
                            otlpTimeout    > 0 ? otlpTimeout    : 10000,
                            otlpErrorRegex));
                    break;
                }
                case "prometheus":
                    final int prometheusPort = getOptionalInt(name, "port");
                    final String prometheusPath = getOptionalString(name, "path");
                    final String metricPrefix = getOptionalString(name, "metricPrefix");
                    final String prometheusUsername = getOptionalString(name, "username");
                    final String prometheusPassword = getOptionalString(name, "password");
                    ret.put(name, new PrometheusDestination(m_ctx, name, prometheusPort != -1 ? prometheusPort : 9090, prometheusPath, metricPrefix, prometheusUsername, prometheusPassword));
                    break;
                default:
                    throw new RuntimeException("Unknown destination type: " + type);
            }
        }
        return m_routes = ret;
    }

}
