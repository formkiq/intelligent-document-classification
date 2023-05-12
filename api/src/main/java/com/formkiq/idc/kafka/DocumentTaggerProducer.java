package com.formkiq.idc.kafka;

import org.apache.kafka.clients.producer.ProducerConfig;

import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.micronaut.context.annotation.Property;

@KafkaClient(acks = KafkaClient.Acknowledge.ALL, properties = {
		@Property(name = ProducerConfig.RETRIES_CONFIG, value = "0") })
public interface DocumentTaggerProducer {

	@Topic("document_tagging")
	void createDocumentTagsRequest(String key);
}
