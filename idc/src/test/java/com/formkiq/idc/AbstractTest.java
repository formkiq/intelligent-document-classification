package com.formkiq.idc;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;

import java.io.File;
import java.net.URL;
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
		MY_KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.3.2"));
		MY_KAFKA.withExposedPorts(9092, 9093);
		MY_KAFKA.start();

		ELASTIC = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.6.2");
		ELASTIC.withExposedPorts(9200);
		ELASTIC.getEnvMap().put("xpack.security.enabled", "false");
		ELASTIC.start();

		MOCK_SERVER = startClientAndServer(9000);
		MOCK_SERVER.when(request().withMethod("GET")).respond(new ExpectationResponseCallback() {

			@Override
			public HttpResponse handle(HttpRequest httpRequest) throws Exception {
				String documentId = httpRequest.getFirstQueryStringParameter("documentId");
				String resourceName = "response/" + documentId + ".txt";
				ClassLoader classLoader = getClass().getClassLoader();
				URL url = classLoader.getResource(resourceName);

				String content = "";
				if (url != null) {
					File file = new File(url.getFile());

					Path filePath = Path.of(file.toString());

					content = Files.readString(filePath);
				}

				return org.mockserver.model.HttpResponse.response(content);
			}
		});
	}

	@Override
	public Map<String, String> getProperties() {
		return Map.of("kafka.bootstrap.servers", MY_KAFKA.getBootstrapServers(), "storage.directory",
				System.getProperty("java.io.tmpdir"), "api.ml.url", "http://localhost:9000", "elasticsearch.url",
				"http://localhost:" + ELASTIC.getMappedPort(9200));
	}

	@AfterAll
	public static void afterClass() {
		MOCK_SERVER.stop();
		MY_KAFKA.stop();
		ELASTIC.stop();
	}
}
