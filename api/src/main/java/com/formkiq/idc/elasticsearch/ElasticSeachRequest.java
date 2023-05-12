package com.formkiq.idc.elasticsearch;

import java.util.Map;

public class ElasticSeachRequest {

	private Map<String, String> tags;
	private String text;
	
	public ElasticSeachRequest(String searchText, Map<String, String> searchTags) {
		this.text = searchText;
		this.tags = searchTags;
	}

	public Map<String, String> getTags() {
		return tags;
	}

	public String getText() {
		return text;
	}
}
