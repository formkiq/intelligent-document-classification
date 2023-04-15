package com.formkiq.idc.elasticsearch;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.http.HttpHost;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
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

	public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

	public static final String INDEX = "documents";

	private ElasticsearchClient esClient;
	private SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);

	@Value("${elasticsearch.url}")
	private String url;

	public ElasticsearchService() {
	}

	public boolean addDocument(String index, String id, Document document) throws IOException {

		document.setInsertedDate(formatter.format(new Date()));

		IndexResponse response = getClient().index(i -> i.index(index).id(id).document(document));
		return response.result() == Result.Created;
	}

	public boolean deleteDocument(String index, String id) throws ElasticsearchException, IOException {
		DeleteResponse response = getClient()
				.delete(DeleteRequest.of(i -> i.index(index).id(id).refresh(Refresh.True)));
		return response.result() == Result.Deleted;
	}

	public boolean deleteDocumentTag(String index, String documentId, String tagKey, String tagValue)
			throws IOException {

		boolean deleted = false;

		Document document = getDocumentWithoutContent(index, documentId);

		if (document.getTags().containsKey(tagKey)) {
			deleted = document.getTags().get(tagKey).remove(tagValue);
			getClient().update(UpdateRequest.of(i -> i.index(index).id(documentId).doc(document).refresh(Refresh.True)),
					Map.class);
		}

		return deleted;
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

	public Document getDocument(String indexName, String documentId) throws IOException {

		try {

			GetRequest request = GetRequest.of(i -> i.index(indexName).id(documentId));
			Document document = getClient().get(request, Document.class).source();

			if (document != null) {
				document.setDocumentId(documentId);
			}

			return document;

		} catch (ResponseException | ElasticsearchException e) {
			e.printStackTrace();
			return null;
		}
	}

	public Document getDocumentWithoutContent(String indexName, String documentId) throws IOException {

		try {

			GetRequest request = GetRequest.of(i -> i.index(indexName).id(documentId)
					.sourceIncludes(Arrays.asList("contentType", "fileLocation", "tags")));
			Document document = getClient().get(request, Document.class).source();

			return document;

		} catch (ResponseException | ElasticsearchException e) {
			e.printStackTrace();
			return null;
		}
	}

	public List<Document> search(String index, String searchText, Map<String, String> tags) throws IOException {

		Integer limit = Integer.valueOf(20);

		try {
			SearchResponse<Document> searchResponse = getClient().search(s -> {
				return s.index(index).size(limit).query(q -> {

					BoolQuery bq = BoolQuery.of(qq -> {

						List<Query> queries = new ArrayList<>();

						if (searchText != null && searchText.trim().length() > 0) {
							Query textQuery = Query.of(tq -> tq.match(mq -> mq.field("content").query(searchText)));
							queries.add(textQuery);
						}

						if (tags != null) {
							for (Map.Entry<String, String> entry : tags.entrySet()) {
								String key = entry.getKey();
								String value = entry.getValue();

								if (value != null && value.trim().length() > 0) {
									Query tagQuery = Query
											.of(tq -> tq.match(mq -> mq.field("tags." + key).query(value)));
									queries.add(tagQuery);
								}
							}
						}

						return qq.must(queries);
					});

					return q.bool(bq);
				});
			}, Document.class);

			List<Hit<Document>> hits = searchResponse.hits().hits();
			List<Document> list = hits.stream().map(m -> {
				Document doc = m.source();
				doc.setDocumentId(m.id());
				return doc;
			}).collect(Collectors.toList());

			return list;
		} catch (ElasticsearchException e) {
			if (e.getMessage().contains("index_not_found_exception")) {
				return Collections.emptyList();
			} else {
				throw e;
			}
		}
	}

	@SuppressWarnings("rawtypes")
	public UpdateResponse<Map> updateDocument(String index, String id, Document document) throws IOException {
		return getClient().update(UpdateRequest.of(i -> i.index(index).id(id).doc(document).refresh(Refresh.True)),
				Map.class);
	}

}
