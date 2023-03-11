package com.formkiq.idc.kafka;

import static com.formkiq.idc.elasticsearch.ElasticsearchService.INDEX;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.formkiq.idc.elasticsearch.ElasticsearchService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.OffsetReset;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;

@KafkaListener(offsetReset = OffsetReset.EARLIEST)
public class DocumentTaggerConsumer {

	private static final double ENTITY_MIN_SCORE = 0.90;

	@Value("${storage.directory}")
	private String storageDirectory;

	@Value("${api.ml.url}")
	private String apiMlUrl;

	@Inject
	private ElasticsearchService elasticService;

	@SuppressWarnings("unchecked")
	@Topic("document_tagging")
	public void receive(String key) throws IOException, URISyntaxException, InterruptedException {

		HttpRequest request = HttpRequest.newBuilder().uri(new URI(apiMlUrl + "?documentId=" + key))
				.timeout(Duration.ofMinutes(2)).GET().build();

		HttpClient client = HttpClient.newHttpClient();
		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

		Gson gson = new GsonBuilder().disableHtmlEscaping().create();

		Map<String, Object> map = gson.fromJson(response.body(), Map.class);

		Map<String, Set<String>> tags = new HashMap<>();

		if (map.containsKey("category")) {
			tags.put("category", Set.of(map.get("category").toString()));
		}

		if (map.containsKey("namedEntity")) {
			List<Map<String, String>> entities = (List<Map<String, String>>) map.get("namedEntity");

			Map<String, Set<String>> list = entities.stream().filter(e -> {
				float score = Float.valueOf(e.get("score")).floatValue();
				return score >= ENTITY_MIN_SCORE;
			}).map(e -> Map.of(e.get("entity_group"), e.get("word"))).flatMap(e -> e.entrySet().stream())
					.collect(Collectors.groupingBy(Map.Entry::getKey,
							Collectors.mapping(Map.Entry::getValue, Collectors.toSet())));
			tags.putAll(list);
		}

		if (!tags.isEmpty()) {
			System.out.println("key: " + key + " adding tags: " + tags);
			elasticService.updateDocument(INDEX, key, Map.of("tags", tags));
		} else {
			System.out.println("key: " + key + " no tags");
		}
	}
}
