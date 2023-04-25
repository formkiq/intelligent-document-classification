package com.formkiq.idc.kafka;

import static com.formkiq.idc.elasticsearch.ElasticsearchService.INDEX;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;

import com.formkiq.idc.elasticsearch.Document;
import com.formkiq.idc.elasticsearch.ElasticsearchService;
import com.formkiq.idc.elasticsearch.Status;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.OffsetReset;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.MediaType;
import jakarta.inject.Inject;

@KafkaListener(offsetReset = OffsetReset.LATEST)
public class DocumentTaggerConsumer {

	private static final double CATEGORY_MIN_SCORE = 0.60;
	private static final double ENTITY_MIN_SCORE = 0.90;

	@Value("${api.ml.url}")
	private String apiMlUrl;

	@Inject
	private ElasticsearchService elasticService;

	@Value("${storage.directory}")
	private String storageDirectory;

	private Path createPngImage(String key, Document document) {
		Path path;
		String pdfFilename = document.getFileLocation();

		path = Path.of(storageDirectory, key, "image.png");

		try {

			try (PDDocument pdDocument = PDDocument.load(new File(pdfFilename))) {
				PDFRenderer pdfRenderer = new PDFRenderer(pdDocument);
				BufferedImage bim = pdfRenderer.renderImageWithDPI(0, 300, ImageType.RGB);
				ImageIOUtil.writeImage(bim, path.toString(), 300);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return path;
	}

	@SuppressWarnings("unchecked")
	private void fetchTagsAndUpdateDocument(String key, Path path)
			throws URISyntaxException, IOException, InterruptedException {

		String urlEncode = URLEncoder.encode(path.toString(), StandardCharsets.UTF_8);
		HttpRequest request = HttpRequest.newBuilder()
				.uri(new URI(apiMlUrl + "?documentId=" + key + "&path=" + urlEncode)).timeout(Duration.ofMinutes(2))
				.GET().build();

		HttpClient client = HttpClient.newHttpClient();
		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		System.out.println("tags api response: " + response.body());

		Gson gson = new GsonBuilder().disableHtmlEscaping().create();
		Document document = new Document();

		Map<String, Collection<String>> tags = new HashMap<>();

		try {

			Map<String, Object> map = gson.fromJson(response.body(), Map.class);

			if (map != null) {

				if (map.containsKey("title")) {
					document.setTitle(map.get("title").toString());
				}

				if (map.containsKey("category")) {
					Map<String, String> category = (Map<String, String>) map.get("category");
					float score = Float.valueOf(category.get("score")).floatValue();

					if (score >= CATEGORY_MIN_SCORE) {
						tags.put("category", Set.of(category.get("label").toString()));
					} else {
						tags.put("category", Set.of("uncategorized"));
					}
				}

				if (map.containsKey("namedEntity")) {

					Map<String, List<Map<String, String>>> entities = (Map<String, List<Map<String, String>>>) map
							.get("namedEntity");

					for (Map.Entry<String, List<Map<String, String>>> e : entities.entrySet()) {
						Collection<String> values = e.getValue().stream().filter(m -> {
							float score = Float.valueOf(m.get("score")).floatValue();
							return score >= ENTITY_MIN_SCORE;
						}).map(m -> m.get("word")).collect(Collectors.toSet());

						if (!values.isEmpty()) {
							List<String> strs = new ArrayList<>(values);
							Collections.sort(strs);
							tags.put(e.getKey().toLowerCase(), strs);
						}
					}
				}
			}

			document.setStatus(Status.COMPLETE.name());

		} catch (JsonSyntaxException e) {
			System.out.println("invalid body: " + response.body());
			tags.put("category", Arrays.asList("unknown"));
			document.setStatus(Status.ML_FAILED.name());
		}

		if (!tags.isEmpty()) {
			System.out.println("key: " + key + " adding tags: " + tags);
			document.setTags(tags);
		}

		elasticService.updateDocument(INDEX, key, document);
	}

	@Topic("document_tagging")
	public void receive(String key) throws IOException, URISyntaxException, InterruptedException {

		boolean deletePath = false;
		Document document = elasticService.getDocumentWithoutContent(INDEX, key);
		Path path = Path.of(document.getFileLocation());

		MediaType contentType = MediaType.of(document.getContentType());
		if (MediaType.APPLICATION_PDF_TYPE.equals(contentType)) {
			path = createPngImage(key, document);
			deletePath = true;
		}

		try {
			fetchTagsAndUpdateDocument(key, path);
		} finally {
			if (deletePath) {
				Files.delete(path);
			}
		}
	}
}
