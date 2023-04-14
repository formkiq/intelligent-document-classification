package com.formkiq.idc.kafka;

import static com.formkiq.idc.elasticsearch.ElasticsearchService.INDEX;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.formkiq.idc.elasticsearch.Document;
import com.formkiq.idc.elasticsearch.ElasticsearchService;
import com.formkiq.idc.elasticsearch.Status;

import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.OffsetReset;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

@KafkaListener(offsetReset = OffsetReset.EARLIEST)
public class TesseractMessageConsumer {

	@Value("${storage.directory}")
	private String storageDirectory;

	@Inject
	DocumentTaggerProducer producer;

	@Inject
	ElasticsearchService elasticService;

	@Topic("tesseract")
	public void receive(@KafkaKey String key, final String path) throws IOException {

		System.out.println("processing ocr: " + path + " (" + key + ")");

		Document document = elasticService.getDocumentWithoutContent(INDEX, key);
		String contentType = document.getContentType();

		try {

			Path ocrFile = Path.of(storageDirectory, key, "ocr.txt");

			Tesseract tesseract = new Tesseract();

			if (Path.of("/usr/local/share/tessdata").toFile().exists()) {
				tesseract.setDatapath("/usr/local/share/tessdata");
			} else {
				tesseract.setDatapath("/usr/share/tessdata");
			}

			tesseract.setLanguage("eng");
			tesseract.setPageSegMode(1);
			tesseract.setOcrEngineMode(1);

			document = new Document();

			String result = null;

			if (!contentType.startsWith("text/")) {

				result = tesseract.doOCR(new File(path));

			} else {
				result = Files.readString(Path.of(path));
			}

			document.setContent(result);
			document.setStatus(Status.OCR_COMPLETE.name());
			elasticService.updateDocument(INDEX, key, document);

			try (BufferedWriter writer = new BufferedWriter(new FileWriter(ocrFile.toFile()))) {
				writer.write(result);
			}

			producer.createDocumentTagsRequest(key);

		} catch (TesseractException e) {
			e.printStackTrace();

			document = new Document();
			document.setStatus(Status.OCR_FAILED.name());
			elasticService.updateDocument(INDEX, key, document);
		}
	}
}
