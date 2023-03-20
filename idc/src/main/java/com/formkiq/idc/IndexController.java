package com.formkiq.idc;

import static com.formkiq.idc.elasticsearch.ElasticsearchService.INDEX;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.formkiq.idc.elasticsearch.Document;
import com.formkiq.idc.elasticsearch.ElasticsearchService;
import com.formkiq.idc.kafka.TesseractProducer;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Options;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.multipart.CompletedFileUpload;
import jakarta.inject.Inject;

@Controller
public class IndexController {

	@Inject
	private ElasticsearchService elasticService;

	@Inject
	private TesseractProducer producer;

	@Value("${storage.directory}")
	private String storageDirectory;

	@Get("/documents/{documentId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Document getDocument(@PathVariable String documentId) throws IOException {
		return this.elasticService.getDocument(INDEX, documentId);
	}

	@Options(value = "/documents/{documentId}", consumes = MediaType.APPLICATION_JSON)
	public HttpResponse<Object> getDocumentOptions(@PathVariable String documentId) {
		return options();
	}

	private HttpResponse<Object> options() {
		return HttpResponse.ok()
				.headers(Map.of("Access-Control-Allow-Credentials", "true", "Access-Control-Allow-Methods", "*",
						"Access-Control-Allow-Origin", "*", "Access-Control-Allow-Headers", "*"));
	}

	@Options(value = "/search", consumes = MediaType.APPLICATION_JSON)
	public HttpResponse<Object> search() {
		return options();
	}

	@Post(value = "/search", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
	public HttpResponse<SearchResponse> search(@Body SearchRequest request) {

		if (request.getText() == null && request.getTags().isEmpty()) {
			return HttpResponse.badRequest();
		}

		try {
			List<Document> documents = elasticService.search("documents", request.getText(), request.getTags());
			SearchResponse response = new SearchResponse();
			response.setDocuments(documents);
			return HttpResponse.ok(response);
		} catch (IOException e) {
			return HttpResponse.badRequest();
		}
	}

	@Options(value = "/upload", consumes = MediaType.APPLICATION_JSON)
	public HttpResponse<Object> upload() {
		return options();
	}

	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Post("/upload")
	@Produces(MediaType.APPLICATION_JSON)
	public MutableHttpResponse<Document> upload(CompletedFileUpload file) throws IOException {

		System.out.println("processing file: " + file.getFilename());
		if ((file.getFilename() == null || file.getFilename().equals(""))) {
			return HttpResponse.badRequest();
		}

		MediaType mediaType = file.getContentType().orElse(MediaType.APPLICATION_OCTET_STREAM_TYPE);

		String documentId = UUID.randomUUID().toString();

		Path filePath = Path.of(storageDirectory, documentId, "original", file.getFilename());
		Files.createDirectories(Path.of(storageDirectory, documentId, "original"));
		Files.write(filePath, file.getBytes());
		
		Document document = new Document();
		document.setDocumentId(documentId);
		document.setContentType(mediaType.getName());
		document.setFileLocation(filePath.toString());
		this.elasticService.addDocument(INDEX, documentId, document);

		producer.sendTesseractRequest(documentId, filePath.toString());
		return HttpResponse.created(document);
	}
}