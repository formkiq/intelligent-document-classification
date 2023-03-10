package com.formkiq.idc.kafka;

import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.Topic;

@KafkaClient
public interface TesseractProducer {

	@Topic("tesseract")
	void sendTesseractRequest(@KafkaKey String key, String path);
}
