package com.formkiq.idc;

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
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Options;
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

	@Options(value = "/search", consumes = MediaType.APPLICATION_JSON)
	public HttpResponse<SearchResponse> search() {
		return HttpResponse.ok();
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
			return HttpResponse.ok(new SearchResponse());
		} catch (IOException e) {
			return HttpResponse.badRequest();
		}
	}
	
	@Options(value = "/upload", consumes = MediaType.APPLICATION_JSON)
	public HttpResponse<SearchResponse> upload() {
		return HttpResponse.ok();
	}
	
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Post("/upload")
	@Produces(MediaType.TEXT_PLAIN)
	public HttpResponse<String> upload(CompletedFileUpload file) throws IOException {

		System.out.println("processing file: " + file.getFilename());
		if ((file.getFilename() == null || file.getFilename().equals(""))) {
			return HttpResponse.badRequest();
		}

		String documentId = UUID.randomUUID().toString();

		Path filePath = Path.of(storageDirectory, documentId, file.getFilename());
		Files.createDirectories(Path.of(storageDirectory, documentId));
		Files.write(filePath, file.getBytes());

		producer.sendTesseractRequest(documentId, filePath.toString());
		return HttpResponse.created("uploaded");
	}
}