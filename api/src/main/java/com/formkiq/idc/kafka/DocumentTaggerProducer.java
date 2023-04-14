package com.formkiq.idc.kafka;

import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.Topic;

@KafkaClient
public interface DocumentTaggerProducer {

	@Topic("document_tagging")
	void createDocumentTagsRequest(String key);
}
