package com.formkiq.idc;

import java.util.Collections;
import java.util.Map;

import org.apache.kafka.common.Uuid;

import com.formkiq.idc.kafka.TesseractProducer;

import io.micronaut.context.annotation.Bean;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller
public class HelloController {

	@Bean
	private TesseractProducer producer;

	@Get
	public Map<String, Object> index() {
		producer.sendTesseractRequest(Uuid.randomUuid().toString(), "asdad");
		return Collections.singletonMap("message", "Hello World123");
	}
}