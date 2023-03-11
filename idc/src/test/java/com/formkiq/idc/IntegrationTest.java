package com.formkiq.idc;

import static com.formkiq.idc.elasticsearch.ElasticsearchService.INDEX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
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

		String id = "354b4ad9-d2ff-4596-9cf0-599c40d841f8";
		producer.sendTesseractRequest(id, file.toString());

		Map<String, Object> data = elasticService.getDocument(INDEX, id);
		while (data == null || !data.containsKey("content") || !data.containsKey("tags")) {
			data = elasticService.getDocument(INDEX, id);
			TimeUnit.SECONDS.sleep(1);
		}

		assertTrue(data.containsKey("content"));
		assertTrue(data.get("content").toString().contains("East Repair Inc"));

		Map<String, Object> tags = (Map<String, Object>) data.get("tags");
		assertEquals(3, tags.size());
		assertEquals("[invoice]", tags.get("category").toString());
		assertEquals("[East Repair Inc, East Repatr Inc]", tags.get("ORG").toString());

		Path path = Path.of(storageDirectory, id, resourceName);
		assertTrue(path.toFile().exists());
	}
}