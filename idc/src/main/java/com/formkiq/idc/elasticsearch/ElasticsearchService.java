package com.formkiq.idc.elasticsearch;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import co.elastic.clients.elasticsearch.core.UpdateResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;

@Singleton
public class ElasticsearchService {

	public static final String INDEX = "documents";

	private ElasticsearchClient esClient;

	@Value("${elasticsearch.url}")
	private String url;

	public ElasticsearchService() {
	}

	public boolean addDocument(String index, String id, Map<String, Object> document) throws IOException {
		try {

			IndexRequest<Map<?, ?>> request = IndexRequest.of(i -> i.index(index).id(id).document(document));

			return getClient().index(request).result() == Result.Created;

		} catch (ElasticsearchException e) {
			if (e.getMessage().contains("index_not_found_exception")) {
				return addDocument(index, id, document);
			} else {
				throw e;
			}
		}
	}

	public IndexResponse createIndex(String indexName) throws IOException {
		return getClient().index(IndexRequest.of(i -> i.index(indexName)));
	}

	private ElasticsearchClient getClient() throws IOException {

		if (this.esClient == null) {
			URL u = new URL(this.url);

			RestClient httpClient = RestClient.builder(new HttpHost(u.getHost(), u.getPort())).build();

			ElasticsearchTransport transport = new RestClientTransport(httpClient, new JacksonJsonpMapper());

			this.esClient = new ElasticsearchClient(transport);
		}

		return this.esClient;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> getDocument(String indexName, String documentId) throws IOException {

		try {

			GetRequest request = GetRequest.of(i -> i.index(indexName).id(documentId));
			return getClient().get(request, Map.class).source();

		} catch (ElasticsearchException e) {
			return null;
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<Map<String, Object>> search(String index, String searchText) throws IOException {

		SearchRequest searchRequest = SearchRequest
				.of(i -> i.index(index).query(q -> q.match(mq -> mq.field("content").query(searchText))));

		SearchResponse<Map> searchResponse = getClient().search(searchRequest, Map.class);

		List<Hit<Map>> hits = searchResponse.hits().hits();
		List<Map<String, Object>> list = hits.stream().map(m -> (Map<String, Object>) m.source())
				.collect(Collectors.toList());

		return list;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<Map<String, Object>> searchTags(String index, Map<String, String> keyValueMap) throws IOException {

		SearchResponse<Map> searchResponse = getClient().search(s -> {
			return s.index(index).query(q -> {

				BoolQuery bq = BoolQuery.of(qq -> {

					List<Query> tagQueries = new ArrayList<>();

					for (Map.Entry<String, String> entry : keyValueMap.entrySet()) {
						String key = entry.getKey();
						String value = entry.getValue();

						Query tagQuery = Query.of(tq -> tq.match(mq -> mq.field("tags." + key).query(value)));
						tagQueries.add(tagQuery);
					}

					return qq.must(tagQueries);
				});

				return q.bool(bq);
			});
		}, Map.class);

		List<Hit<Map>> hits = searchResponse.hits().hits();
		List<Map<String, Object>> list = hits.stream().map(m -> (Map<String, Object>) m.source())
				.collect(Collectors.toList());

		return list;
	}

	@SuppressWarnings("rawtypes")
	public UpdateResponse<Map> updateDocument(String index, String id, Map<String, Object> document)
			throws IOException {
		return getClient().update(UpdateRequest.of(i -> i.index(index).id(id).doc(document)), Map.class);
	}

}
