package com.ety.natively.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.ety.natively.properties.ElasticProperties;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ElasticConfig {

	@Bean
	public ElasticsearchClient elasticsearchClient(ElasticProperties elasticProperties){
		RestClient restClient = RestClient
				.builder(HttpHost.create(elasticProperties.getServerUrl()))
				.build();
		RestClientTransport transport = new RestClientTransport(
				restClient, new JacksonJsonpMapper());
		return new ElasticsearchClient(transport);
	}

}
