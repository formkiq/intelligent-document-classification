package com.formkiq.idc.kafka;

import static com.formkiq.idc.elasticsearch.ElasticsearchService.INDEX;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.elasticsearch.core.Map;

import com.formkiq.idc.elasticsearch.ElasticsearchService;

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
	OpenNlpProducer producer;

	@Inject
	ElasticsearchService elasticService;

	@Topic("tesseract")
	public void receive(@KafkaKey String key, final String path) throws IOException {

		String filename = Path.of(path).getFileName().toString();

		try {

			Path newFilePath = Path.of(storageDirectory, key, filename);
			Files.createDirectories(Path.of(storageDirectory, key));
			Files.copy(Path.of(path), newFilePath, StandardCopyOption.REPLACE_EXISTING);

			Path ocrFile = Path.of(storageDirectory, key, "ocr.txt");

			Tesseract tesseract = new Tesseract();
			tesseract.setDatapath("/usr/local/share/tessdata");
			tesseract.setLanguage("eng");
			tesseract.setPageSegMode(1);
			tesseract.setOcrEngineMode(1);

			String result = tesseract.doOCR(new File(path));
			elasticService.addDocument(INDEX, key, Map.of("content", result));

			try (BufferedWriter writer = new BufferedWriter(new FileWriter(ocrFile.toFile()))) {
				writer.write(result);
			}

			producer.sendOpenNlpRequest(key);

		} catch (TesseractException e) {
			e.printStackTrace();
		}
	}
}
