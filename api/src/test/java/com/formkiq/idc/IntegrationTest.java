package com.formkiq.idc;

import static com.formkiq.idc.elasticsearch.ElasticsearchService.INDEX;
import static io.micronaut.http.HttpStatus.OK;
import static io.micronaut.http.HttpStatus.UNAUTHORIZED;
import static io.micronaut.http.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;

import com.formkiq.idc.elasticsearch.Document;
import com.formkiq.idc.elasticsearch.ElasticsearchService;
import com.formkiq.idc.kafka.TesseractMessageConsumer;
import com.formkiq.idc.kafka.TesseractProducer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nimbusds.jose.util.StandardCharset;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.http.server.netty.multipart.NettyCompletedFileUpload;
import io.micronaut.http.server.types.files.StreamedFile;
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

	Gson gson = new GsonBuilder().disableHtmlEscaping().create();

	@Inject
	IndexController indexController;

	@Inject
	TesseractProducer producer;

	@Value("${storage.directory}")
	String storageDirectory;

	@BeforeAll
	public void beforeEach() {
		System.setProperty("api.username", "admin");
		System.setProperty("api.password", "password");
	}

	private HttpResponse<?> delete(String documentId) throws IOException {
		HttpResponse<?> response = indexController.deleteDocument(documentId);
		return response;
	}

	private void deleteTag(String documentId, String tagKey, String tagValue) throws IOException {
		HttpResponse<?> response = indexController.deleteTagKeyValue(documentId, tagKey, tagValue);
		assertEquals(OK, response.getStatus());
	}

	private HttpResponse<StreamedFile> download(String documentId) throws IOException {
		return indexController.download(documentId);
	}

	private Document get(String documentId) throws IOException {
		return indexController.getDocument(documentId);
	}

	private String getAccessToken() {
		UsernamePasswordCredentials creds = new UsernamePasswordCredentials(System.getProperty("api.username"),
				System.getProperty("api.password"));

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

	private HttpResponse<String> search(SearchRequest search) {
		String accessToken = getAccessToken();
		HttpRequest<?> requestWithAuthorization = HttpRequest.POST("/search", search).accept(APPLICATION_JSON_TYPE)
				.bearerAuth(accessToken);
		HttpResponse<String> response = client.toBlocking().exchange(requestWithAuthorization, String.class);
		return response;
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> searchResults(SearchRequest search) {
		HttpResponse<String> response = search(search);
		assertEquals(OK, response.getStatus());

		Map<String, Object> documents = gson.fromJson(response.body(), Map.class);
		assertTrue(documents.containsKey("documents"));
		List<Map<String, Object>> list = (List<Map<String, Object>>) documents.get("documents");
		return list;
	}

	@Test
	@Timeout(unit = TimeUnit.MINUTES, value = 1)
	public void testElasticSearch(RequestSpecification spec) throws IOException {
		assertNull(elasticService.getDocument(INDEX, UUID.randomUUID().toString()));
	}

	@Test
	@Timeout(unit = TimeUnit.MINUTES, value = 1)
	public void testLogin() {

		HttpRequest<?> requestWithAuthorization = HttpRequest.POST("/login",
				Map.of("username", System.getProperty("api.username"), "password", System.getProperty("api.password")))
				.accept(APPLICATION_JSON_TYPE);
		HttpResponse<String> response = client.toBlocking().exchange(requestWithAuthorization, String.class);

		assertEquals(OK, response.getStatus());
		assertTrue(response.body().contains("access_token"));
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
	public void testPostSearch01() {

		SearchRequest search = new SearchRequest();
		search.setText("canada");

		HttpResponse<String> response = search(search);

		assertEquals(OK, response.getStatus());
		assertEquals("{\"documents\":[]}", response.body());
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

		Path path = Path.of(storageDirectory, documentId, resourceName);
		assertTrue(path.toFile().exists());

		path = Path.of(storageDirectory, documentId, "ocr.txt");
		assertTrue(path.toFile().exists());

		path = Path.of(storageDirectory, documentId, "image.png");
		assertFalse(path.toFile().exists());

		SearchRequest search = new SearchRequest();
		search.setText("");
		List<Map<String, Object>> list = searchResults(search);
		assertFalse(list.isEmpty());

		assertNotNull(list.get(0).get("documentId"));
		assertNotNull(list.get(0).get("insertedDate"));
		assertEquals("example.pdf", list.get(0).get("filename"));

		assertNotNull(get(documentId));
		assertEquals(HttpStatus.OK, delete(documentId).getStatus());
		assertNull(get(documentId));

		path = Path.of(storageDirectory, documentId);
		assertFalse(path.toFile().exists());
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
		assertEquals("[East Repair Inc, East Repatr Inc, Smith Job Smith]", tags.get("org").toString());

		List<Document> list = elasticService.search(INDEX, "Repair Inc", null);
		assertEquals(1, list.size());

		list = elasticService.search(INDEX, null, Map.of("category", "invoice"));
		assertEquals(1, list.size());

		list = elasticService.search(INDEX, null, Map.of("loc", "New York"));
		assertEquals(1, list.size());

		list = elasticService.search(INDEX, null, Map.of("loc", "Chicago"));
		assertEquals(0, list.size());

		SearchRequest req = new SearchRequest();
		req.setText(" [category] = invoice ");
		List<Map<String, Object>> search = searchResults(req);
		assertEquals(1, search.size());

		req.setText(" [category] = invoice and [loc]=new york");
		search = searchResults(req);
		assertEquals(1, search.size());

		req.setText(" [category] = invoice and [loc] = Florida");
		search = searchResults(req);
		assertEquals(0, search.size());

		Path path = Path.of(storageDirectory, documentId, resourceName);
		assertTrue(path.toFile().exists());

		path = Path.of(storageDirectory, documentId, "ocr.txt");
		assertTrue(path.toFile().exists());

		deleteTag(documentId, "loc", "New York");

		list = elasticService.search(INDEX, null, Map.of("category", "invoice"));
		assertEquals(1, list.size());

		list = elasticService.search(INDEX, null, Map.of("loc", "New York"));
		assertEquals(0, list.size());
	}

	@Test
	@Timeout(unit = TimeUnit.MINUTES, value = 1)
	void testProcessTxt01() throws Exception {

		String resourceName = "test.txt";
		String documentId = upload(resourceName, MediaType.TEXT_PLAIN_TYPE);
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
		assertEquals("This is a sample category", data.getTitle());
		assertEquals("COMPLETE", data.getStatus());
		assertEquals("text/plain", data.getContentType());
		assertNotNull(data.getFileLocation());
		assertTrue(data.getContent().toString().contains("test document"));

		Map<String, Collection<String>> tags = data.getTags();
		assertEquals(3, tags.size());
		assertEquals("[invoice]", tags.get("category").toString());
		assertEquals("[East Repair Inc, East Repatr Inc, Smith Job Smith]", tags.get("org").toString());

		List<Document> list = elasticService.search(INDEX, "test document", null);
		assertEquals(1, list.size());

		list = elasticService.search(INDEX, null, Map.of("loc", "Chicago"));
		assertEquals(0, list.size());

		Path path = Path.of(storageDirectory, documentId, resourceName);
		assertTrue(path.toFile().exists());

		path = Path.of(storageDirectory, documentId, "ocr.txt");
		assertTrue(path.toFile().exists());

		HttpResponse<StreamedFile> download = download(documentId);
		InputStream inputStream = download.body().getInputStream();
		String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		assertEquals("This is a test document!", text);

		String title = "this is new title";
		updateDocument(documentId, Map.of("title", title));
		assertEquals(title, elasticService.getDocument(INDEX, documentId).getTitle());
	}

	private HttpResponse<Void> updateDocument(String documentId, Map<String, Object> data) {
		HttpResponse<Void> response = indexController.updateDocument(documentId, data);
		assertEquals(OK, response.getStatus());
		return response;
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