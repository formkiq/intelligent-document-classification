package com.formkiq.idc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import com.formkiq.idc.kafka.TesseractProducer;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.multipart.CompletedFileUpload;
import jakarta.inject.Inject;

@Controller
public class IndexController {

	@Value("${storage.directory}")
	private String storageDirectory;

	@Inject
	private TesseractProducer producer;

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