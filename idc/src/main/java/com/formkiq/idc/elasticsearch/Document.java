package com.formkiq.idc.elasticsearch;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class Document {

	private String content;
	private Map<String, Collection<String>> tags = Collections.emptyMap();

	public Document() {

	}

	public String getContent() {
		return content;
	}

	public Map<String, Collection<String>> getTags() {
		return tags;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public void setTags(Map<String, Collection<String>> tags) {
		this.tags = tags;
	}
}
