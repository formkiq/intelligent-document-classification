package com.formkiq.idc.elasticsearch;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import org.apache.http.HttpHost;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.xcontent.XContentType;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;

@Singleton
public class ElasticsearchService {

	public static final String INDEX = "documents";

	private RestHighLevelClient client;

	@Value("${elasticsearch.url}")
	private String url;

	public ElasticsearchService() {
	}

	public boolean addDocument(String index, String id, Map<String, Object> document) throws IOException {
		try {
			IndexRequest request = new IndexRequest(index).id(id).source(document, XContentType.JSON);
			IndexResponse response = getClient().index(request, RequestOptions.DEFAULT);
			return response.getResult() == IndexResponse.Result.CREATED;
		} catch (ElasticsearchStatusException e) {
			if (e.getMessage().contains("index_not_found_exception")) {
				return addDocument(index, id, document);
			} else {
				throw e;
			}
		}
	}

	public CreateIndexResponse createIndex(String indexName) throws IOException {
		CreateIndexRequest request = new CreateIndexRequest(indexName);
		CreateIndexResponse createIndexResponse = client.indices().create(request, RequestOptions.DEFAULT);
		return createIndexResponse;
	}

	private RestHighLevelClient getClient() throws IOException {
		if (this.client == null) {
			URL u = new URL(this.url);
			this.client = new RestHighLevelClient(RestClient.builder(new HttpHost(u.getHost(), u.getPort(), "http")));
		}

		return this.client;
	}

	public Map<String, Object> getDocument(String indexName, String documentId) throws IOException {
		try {
			GetRequest getRequest = new GetRequest(indexName, documentId);
			GetResponse getResponse = getClient().get(getRequest, RequestOptions.DEFAULT);
			return getResponse.getSourceAsMap();
		} catch (ElasticsearchStatusException e) {
			return null;
		}
	}

	public UpdateResponse updateDocument(String index, String id, Map<String, Object> document) throws IOException {
		UpdateRequest request = new UpdateRequest(index, id).doc(document);

		UpdateResponse updateResponse = client.update(request, RequestOptions.DEFAULT);
		return updateResponse;
	}
}
