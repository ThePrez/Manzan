package com.github.theprez.manzan.routes.dest;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;

import com.github.theprez.manzan.routes.ManzanRoute;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;

public class ElasticsearchDestination extends ManzanRoute {
    String index;
    ElasticsearchClient esClient;

    public ElasticsearchDestination(final String _name, String _endpoint, String _apiKey, String _index) {
        super(_name);
        this.index = _index;

        // Create the low-level client
        RestClient restClient = RestClient
            .builder(HttpHost.create(_endpoint))
            .setDefaultHeaders(new Header[]{
                new BasicHeader("Authorization", "ApiKey " + _apiKey)
            })
            .build();

        // Create the transport with a Jackson mapper
        ElasticsearchTransport transport = new RestClientTransport(
            restClient, new JacksonJsonpMapper());

        // Create the API client
        this.esClient = new ElasticsearchClient(transport);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    esClient.close();
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void configure() {
        from(getInUri())
            .routeId(m_name).process(exchange -> {
            esClient.index(i -> i
                .index(this.index)
                .document(getDataMap(exchange))
            );
        });
    }
}