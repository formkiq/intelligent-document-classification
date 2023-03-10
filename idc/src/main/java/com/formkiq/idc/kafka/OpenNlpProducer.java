package com.formkiq.idc.kafka;

import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.Topic;

@KafkaClient
public interface OpenNlpProducer {

	@Topic("opennlp")
	void sendOpenNlpRequest(String key);
}
