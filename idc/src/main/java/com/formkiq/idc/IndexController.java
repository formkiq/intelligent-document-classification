package com.formkiq.idc;

import static com.formkiq.idc.elasticsearch.ElasticsearchService.INDEX;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.inject.Inject;

@Controller
@Secured(SecurityRule.IS_AUTHENTICATED)
public class IndexController {

	@Inject
	private ElasticsearchService elasticService;

	@Inject
	private TesseractProducer producer;

	@Value("${storage.directory}")
	private String storageDirectory;

	@Delete("/documents/{documentId}")
	@Produces(MediaType.APPLICATION_JSON)
	public HttpResponse<?> deleteDocument(@PathVariable String documentId) throws IOException {
		return this.elasticService.deleteDocument(INDEX, documentId) ? HttpResponse.ok() : HttpResponse.notFound();
	}

	@Get("/documents/{documentId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Document getDocument(@PathVariable String documentId) throws IOException {
		return this.elasticService.getDocument(INDEX, documentId);
	}

	@Post(value = "/search", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
	public HttpResponse<SearchResponse> search(@Body SearchRequest request) {

		try {
			List<Document> documents = elasticService.search("documents", request.getText(), request.getTags());
			documents.forEach(doc -> doc.setContent(null));
			SearchResponse response = new SearchResponse();
			response.setDocuments(documents);
			return HttpResponse.ok(response);
		} catch (IOException e) {
			return HttpResponse.badRequest();
		}
	}

	@Post("/upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
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

		Document doc = new Document();
		doc.setDocumentId(documentId);
		return HttpResponse.created(doc);
	}
}