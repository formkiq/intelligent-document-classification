package com.formkiq.idc;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.formkiq.idc.elasticsearch.Document;

public class SearchResponse {

	@JsonInclude(Include.ALWAYS)	
	private List<Document> documents = Collections.emptyList();

	public SearchResponse() {

	}

	public List<Document> getDocuments() {
		return documents;
	}

	public void setDocuments(List<Document> documents) {
		this.documents = documents;
	}

}
