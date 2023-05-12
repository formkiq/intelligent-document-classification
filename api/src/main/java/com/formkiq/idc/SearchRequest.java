package com.formkiq.idc;

public class SearchRequest {

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
	 * Get Search text.
	 * 
	 * @return {@link String}
	 */
	public String getText() {
		return text;
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
