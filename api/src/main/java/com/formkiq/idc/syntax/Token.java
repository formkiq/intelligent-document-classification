package com.formkiq.idc.syntax;

public class Token {

	private TokenType type;
	private String value;

	public Token(TokenType tokenType, String tokenValue) {
		this.type = tokenType;
		this.value = tokenValue;
	}

	public TokenType getType() {
		return type;
	}

	public String getValue() {
		return value;
	}
}
