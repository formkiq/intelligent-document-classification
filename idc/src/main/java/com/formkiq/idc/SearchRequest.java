package com.formkiq.idc;

import java.util.Collections;
import java.util.Map;

public class SearchRequest {

	/** Search for Tags. */
	private Map<String, String> tags = Collections.emptyMap();
	/**
	 * 'text' string to search for.
	 */
	private String text;

	/**
	 * constructor.
	 */
	public SearchRequest() {

	}

	/**
	 * Get Tags.
	 * @return {@link Map}
	 */
	public Map<String, String> getTags() {
		return tags;
	}

	/**
	 * Get Search text.
	 * 
	 * @return {@link String}
	 */
	public String getText() {
		return text;
	}

	/**
	 * Set Tags.
	 * @param tags {@link Map}
	 */
	public void setTags(Map<String, String> tags) {
		this.tags = tags;
	}

	/**
	 * Set Search text.
	 * 
	 * @param text {@link String}
	 */
	public void setText(String text) {
		this.text = text;
	}
}
