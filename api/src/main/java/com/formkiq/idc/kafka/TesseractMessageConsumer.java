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

@KafkaListener(offsetReset = OffsetReset.LATEST)
public class TesseractMessageConsumer {

	@Inject
	ElasticsearchService elasticService;

	@Inject
	DocumentTaggerProducer producer;

	@Value("${storage.directory}")
	private String storageDirectory;

	private String dataPath;

	public TesseractMessageConsumer() {

		if (Path.of("/opt/homebrew/lib").toFile().exists()) {
			System.setProperty("jna.library.path", "/opt/homebrew/lib");
		}

		if (Path.of("/opt/homebrew/share/tessdata").toFile().exists()) {
			this.dataPath = "/opt/homebrew/share/tessdata";
		} else if (Path.of("/usr/local/share/tessdata").toFile().exists()) {
			this.dataPath = "/usr/local/share/tessdata";
		} else {
			this.dataPath = "/usr/share/tessdata";
		}
	}

	@Topic("tesseract")
	public void receive(@KafkaKey String key, final String path) throws IOException {

		System.out.println("processing ocr: " + path + " (" + key + ")");

		updateStatus(key, Status.OCR_IN_PROGRESS);
		Document document = elasticService.getDocumentWithoutContent(INDEX, key);
		String contentType = document.getContentType();

		try {

			Path ocrFile = Path.of(storageDirectory, key, "ocr.txt");

			Tesseract tesseract = new Tesseract();
			tesseract.setDatapath(this.dataPath);
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

			updateStatus(key, Status.OCR_FAILED);
		}
	}

	private void updateStatus(String key, Status status) throws IOException {
		Document document;
		document = new Document();
		document.setStatus(status.name());
		elasticService.updateDocument(INDEX, key, document);
	}
}
