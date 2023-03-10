package com.formkiq.idc;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.mock.action.ExpectationResponseCallback;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import io.micronaut.test.support.TestPropertyProvider;

abstract class AbstractTest implements TestPropertyProvider {

	static final KafkaContainer MY_KAFKA;
	static final ClientAndServer MOCK_SERVER;
	static final ElasticsearchContainer ELASTIC;

	static {
		MY_KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));
		MY_KAFKA.withExposedPorts(9092, 9093);
		MY_KAFKA.start();

		ELASTIC = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.17.7");
		ELASTIC.withExposedPorts(9200);
		ELASTIC.start();

		MOCK_SERVER = startClientAndServer(8080);
		MOCK_SERVER.when(request().withMethod("GET")).respond(new ExpectationResponseCallback() {

			@Override
			public HttpResponse handle(HttpRequest httpRequest) throws Exception {
				String documentId = httpRequest.getFirstQueryStringParameter("documentId");
				String resourceName = "response/" + documentId + ".txt";
				ClassLoader classLoader = getClass().getClassLoader();
				File file = new File(classLoader.getResource(resourceName).getFile());

				Path filePath = Path.of(file.toString());

				String content = Files.readString(filePath);

				return org.mockserver.model.HttpResponse.response(content);
			}
		});
	}

	@Override
	public Map<String, String> getProperties() {
		return Map.of("kafka.bootstrap.servers", MY_KAFKA.getBootstrapServers(), "storage.directory",
				System.getProperty("java.io.tmpdir"), "api.ml.url", "http://localhost:8080", "elasticsearch.url",
				"http://localhost:" + ELASTIC.getFirstMappedPort());
	}

	@AfterAll
	public static void afterClass() {
		MOCK_SERVER.stop();
		MY_KAFKA.stop();
		ELASTIC.stop();
	}
}
