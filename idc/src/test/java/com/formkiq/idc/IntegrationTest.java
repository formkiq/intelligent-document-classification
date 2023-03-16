package com.formkiq.idc;

import static com.formkiq.idc.elasticsearch.ElasticsearchService.INDEX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;

import com.formkiq.idc.elasticsearch.ElasticsearchService;
import com.formkiq.idc.kafka.TesseractMessageConsumer;
import com.formkiq.idc.kafka.TesseractProducer;

import io.micronaut.context.annotation.Value;
import io.micronaut.runtime.EmbeddedApplication;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;

@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntegrationTest extends AbstractTest {

	@Inject
	EmbeddedApplication<?> application;

	@Inject
	TesseractMessageConsumer consumer;

	@Inject
	ElasticsearchService elasticService;

	@Inject
	TesseractProducer producer;

	@Value("${storage.directory}")
	private String storageDirectory;

	private File getFile(final String resourceName) {
		ClassLoader classLoader = getClass().getClassLoader();
		File file = new File(classLoader.getResource(resourceName).getFile());
		return file;
	}

	@SuppressWarnings("unchecked")
	@Test
	@Timeout(unit = TimeUnit.MINUTES, value = 1)
	void testProcessImage() throws Exception {

		Assertions.assertTrue(application.isRunning());
		Assertions.assertNotNull(consumer);
		Assertions.assertNotNull(producer);

		String resourceName = "receipt.png";
		File file = getFile(resourceName);

		String documentId = "354b4ad9-d2ff-4596-9cf0-599c40d841f8";

		Path newFilePath = Path.of(storageDirectory, documentId, resourceName);
		Files.createDirectories(Path.of(storageDirectory, documentId));
		Files.copy(Path.of(file.toString()), newFilePath, StandardCopyOption.REPLACE_EXISTING);

		producer.sendTesseractRequest(documentId, file.toString());

		Map<String, Object> data = elasticService.getDocument(INDEX, documentId);
		while (data == null || !data.containsKey("content") || !data.containsKey("tags")) {
			data = elasticService.getDocument(INDEX, documentId);
			TimeUnit.SECONDS.sleep(1);
		}

		assertTrue(data.containsKey("content"));
		assertTrue(data.get("content").toString().contains("East Repair Inc"));

		Map<String, Object> tags = (Map<String, Object>) data.get("tags");
		assertEquals(3, tags.size());
		assertEquals("[invoice]", tags.get("category").toString());
		assertEquals("[East Repair Inc, East Repatr Inc]", tags.get("ORG").toString());
		
		List<Map<String, Object>> list = elasticService.search(INDEX, "Repair Inc");
		assertEquals(1, list.size());

		list = elasticService.searchTags(INDEX, Map.of("category", "invoice"));
		assertEquals(1, list.size());
		
		list = elasticService.searchTags(INDEX, Map.of("LOC", "New York"));
		assertEquals(1, list.size());
		
		list = elasticService.searchTags(INDEX, Map.of("LOC", "Chicago"));
		assertEquals(0, list.size());

		Path path = Path.of(storageDirectory, documentId, resourceName);
		assertTrue(path.toFile().exists());
	}
}