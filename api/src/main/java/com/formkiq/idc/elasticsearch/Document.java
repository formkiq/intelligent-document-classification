package com.formkiq.idc.elasticsearch;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class Document {

	private String content;
	private String contentType;
	private String documentId;
	private String fileLocation;
	private String filename;
	private String insertedDate;
	private String status;
	private Map<String, Collection<String>> tags = Collections.emptyMap();
	private String title;

	public Document() {

	}

	public String getContent() {
		return content;
	}

	public String getContentType() {
		return contentType;
	}

	public String getDocumentId() {
		return documentId;
	}

	public String getFileLocation() {
		return fileLocation;
	}

	public String getFilename() {
		return filename;
	}

	public String getInsertedDate() {
		return insertedDate;
	}

	public String getStatus() {
		return status;
	}

	public Map<String, Collection<String>> getTags() {
		return tags;
	}

	public String getTitle() {
		return title;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}

	public void setFileLocation(String fileLocation) {
		this.fileLocation = fileLocation;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public void setInsertedDate(String insertedDate) {
		this.insertedDate = insertedDate;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public void setTags(Map<String, Collection<String>> tags) {
		this.tags = tags;
	}

	public void setTitle(String title) {
		this.title = title;
	}
}
