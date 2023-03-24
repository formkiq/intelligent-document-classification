package com.formkiq.idc;

import static com.formkiq.idc.elasticsearch.ElasticsearchService.INDEX;
import static io.micronaut.http.HttpStatus.BAD_REQUEST;
import static io.micronaut.http.HttpStatus.OK;
import static io.micronaut.http.HttpStatus.UNAUTHORIZED;
import static io.micronaut.http.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;

import com.formkiq.idc.elasticsearch.Document;
import com.formkiq.idc.elasticsearch.ElasticsearchService;
import com.formkiq.idc.kafka.TesseractMessageConsumer;
import com.formkiq.idc.kafka.TesseractProducer;
import com.nimbusds.jose.util.StandardCharset;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.http.server.netty.multipart.NettyCompletedFileUpload;
import io.micronaut.runtime.EmbeddedApplication;
import io.micronaut.security.authentication.UsernamePasswordCredentials;
import io.micronaut.security.token.jwt.render.BearerAccessRefreshToken;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.MemoryFileUpload;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;

@MicronautTest(environments = "integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntegrationTest extends AbstractTest {

	@Inject
	EmbeddedApplication<?> application;

	@Inject
	@Client("/")
	HttpClient client;

	@Inject
	TesseractMessageConsumer consumer;

	@Inject
	ElasticsearchService elasticService;

	@Inject
	IndexController indexController;

	@Inject
	TesseractProducer producer;

	@Value("${storage.directory}")
	private String storageDirectory;

	private String getAccessToken() {
		UsernamePasswordCredentials creds = new UsernamePasswordCredentials("sherlock", "password");
		HttpRequest<?> request = HttpRequest.POST("/login", creds);
		HttpResponse<BearerAccessRefreshToken> rsp = client.toBlocking().exchange(request,
				BearerAccessRefreshToken.class);
		assertEquals(OK, rsp.getStatus());

		return rsp.body().getAccessToken();
	}

	private File getFile(final String resourceName) {
		ClassLoader classLoader = getClass().getClassLoader();
		File file = new File(classLoader.getResource(resourceName).getFile());
		assertTrue(file.exists());
		return file;
	}

	@Test
	@Timeout(unit = TimeUnit.MINUTES, value = 1)
	public void testElasticSearch(RequestSpecification spec) throws IOException {
		assertNull(elasticService.getDocument(INDEX, UUID.randomUUID().toString()));
	}

	@Test
	@Timeout(unit = TimeUnit.MINUTES, value = 1)
	public void testOptionsSearch(RequestSpecification spec) throws IOException {
		SearchRequest search = new SearchRequest();
		spec.when().contentType(ContentType.JSON)
				.headers(Map.of("Access-Control-Request-Method", "POST", "Origin", "http://localhost:8080"))
				.body(search).options("/search").then().statusCode(200);
	}

	@Test
	@Timeout(unit = TimeUnit.MINUTES, value = 1)
	public void testOptionsUpload(RequestSpecification spec) throws IOException {
		SearchRequest search = new SearchRequest();
		spec.when().contentType(ContentType.JSON)
				.headers(Map.of("Access-Control-Request-Method", "POST", "Origin", "http://localhost:8080"))
				.body(search).options("/upload").then().statusCode(200);
	}

	@Test
	@Timeout(unit = TimeUnit.MINUTES, value = 1)
	public void testPostSearch() {

		String accessToken = getAccessToken();
		SearchRequest search = new SearchRequest();
		search.setText("canada");

		HttpRequest<?> requestWithAuthorization = HttpRequest.POST("/search", search).accept(APPLICATION_JSON_TYPE)
				.bearerAuth(accessToken);
		HttpResponse<String> response = client.toBlocking().exchange(requestWithAuthorization, String.class);

		assertEquals(OK, response.getStatus());
		assertEquals("{\"documents\":[]}", response.body());
	}

	@Test
	@Timeout(unit = TimeUnit.MINUTES, value = 1)
	public void testPostSearchInvalid() throws IOException {
		String accessToken = getAccessToken();
		SearchRequest search = new SearchRequest();

		HttpRequest<?> requestWithAuthorization = HttpRequest.POST("/search", search).accept(APPLICATION_JSON_TYPE)
				.bearerAuth(accessToken);

		try {
			client.toBlocking().exchange(requestWithAuthorization, String.class);
			fail();
		} catch (HttpClientResponseException e) {
			assertEquals(BAD_REQUEST, e.getResponse().getStatus());
		}
	}

	@Test
	@Timeout(unit = TimeUnit.MINUTES, value = 1)
	public void testPostSearchMissingAccessToken() throws IOException {
		SearchRequest search = new SearchRequest();

		HttpRequest<?> requestWithAuthorization = HttpRequest.POST("/search", search).accept(APPLICATION_JSON_TYPE);

		try {
			client.toBlocking().exchange(requestWithAuthorization, String.class);
			fail();
		} catch (HttpClientResponseException e) {
			assertEquals(UNAUTHORIZED, e.getResponse().getStatus());
		}

	}

	@Test
	@Timeout(unit = TimeUnit.MINUTES, value = 1)
	void testProcessPdf01() throws Exception {

		String resourceName = "example.pdf";
		String documentId = upload(resourceName, MediaType.APPLICATION_PDF_TYPE);

		Document data = elasticService.getDocument(INDEX, documentId);
		while (!"COMPLETE".equals(data.getStatus())) {
			data = elasticService.getDocument(INDEX, documentId);
			TimeUnit.SECONDS.sleep(1);
		}

		assertTrue(data.getContent().toString().contains("Output Designer"));
		assertEquals(documentId, data.getDocumentId());
		assertNotNull(data.getInsertedDate());
		assertNotNull(data.getFileLocation());
		assertEquals("application/pdf", data.getContentType());

		Path path = Path.of(storageDirectory, documentId, "original", resourceName);
		assertTrue(path.toFile().exists());

		path = Path.of(storageDirectory, documentId, "ocr.txt");
		assertTrue(path.toFile().exists());

		path = Path.of(storageDirectory, documentId, "image.png");
		assertTrue(path.toFile().exists());
	}

	@Test
	@Timeout(unit = TimeUnit.MINUTES, value = 1)
	void testProcessPng01() throws Exception {

		String resourceName = "receipt.png";
		String documentId = upload(resourceName, MediaType.IMAGE_PNG_TYPE);
		assertNotNull(documentId);

		String readResource = readResource("response/354b4ad9-d2ff-4596-9cf0-599c40d841f8.txt");
		addContent(documentId, readResource);

		Document data = elasticService.getDocument(INDEX, documentId);
		while (!"COMPLETE".equals(data.getStatus())) {
			data = elasticService.getDocument(INDEX, documentId);
			TimeUnit.SECONDS.sleep(1);
		}

		assertEquals(documentId, data.getDocumentId());
		assertNotNull(data.getInsertedDate());
		assertEquals("COMPLETE", data.getStatus());
		assertEquals("image/png", data.getContentType());
		assertNotNull(data.getFileLocation());
		assertTrue(data.getContent().toString().contains("East Repair Inc"));

		Map<String, Collection<String>> tags = data.getTags();
		assertEquals(3, tags.size());
		assertEquals("[invoice]", tags.get("category").toString());
		assertEquals("[East Repair Inc, East Repatr Inc]", tags.get("ORG").toString());

		List<Document> list = elasticService.search(INDEX, "Repair Inc", null);
		assertEquals(1, list.size());

		list = elasticService.search(INDEX, null, Map.of("category", "invoice"));
		assertEquals(1, list.size());

		list = elasticService.search(INDEX, null, Map.of("LOC", "New York"));
		assertEquals(1, list.size());

		list = elasticService.search(INDEX, null, Map.of("LOC", "Chicago"));
		assertEquals(0, list.size());

		Path path = Path.of(storageDirectory, documentId, "original", resourceName);
		assertTrue(path.toFile().exists());

		path = Path.of(storageDirectory, documentId, "ocr.txt");
		assertTrue(path.toFile().exists());
	}

	private String upload(String resourceName, MediaType mediaType) throws IOException {
		File file = getFile(resourceName);

		FileUpload fileUpload = new MemoryFileUpload(resourceName, resourceName, mediaType.getName(), "base64",
				StandardCharset.UTF_8, file.length());
		fileUpload.setContent(file);
		CompletedFileUpload completedFileUpload = new NettyCompletedFileUpload(fileUpload);

		MutableHttpResponse<Document> response = indexController.upload(completedFileUpload);
		String documentId = response.getBody().get().getDocumentId();
		return documentId;
	}
}