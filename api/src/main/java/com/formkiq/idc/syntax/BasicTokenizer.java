package com.formkiq.idc.syntax;

import java.util.ArrayList;
import java.util.List;

public class BasicTokenizer implements Tokenizer {

	private static final String QUOTES = "'\"";
	private static final String OPERATORS = "=<>";
	private int currentPosition;

	private String input;
	private List<String> keywords;

	public BasicTokenizer(List<String> tokenizerKeywords) {
		this.keywords = tokenizerKeywords;
	}

	private TokenType defaultToken(String s) {
		switch (s) {
		case "[":
			return TokenType.SQUARE_BRACKET_LEFT;
		case "]":
			return TokenType.SQUARE_BRACKET_RIGHT;
		default:
			return TokenType.LEXEMES;
		}
	}

	private Token parseNumber() {
		StringBuilder sb = new StringBuilder();
		while (currentPosition < input.length() && Character.isDigit(input.charAt(currentPosition))) {
			sb.append(input.charAt(currentPosition));
			currentPosition++;
		}
		return new Token(TokenType.NUMBER, sb.toString());
	}

	private Token parseOperator() {
		StringBuilder sb = new StringBuilder();

		while (currentPosition < input.length() && sb.length() < 3
				&& OPERATORS.indexOf(input.charAt(currentPosition)) != -1) {
			sb.append(input.charAt(currentPosition));
			currentPosition++;
		}

		String word = sb.toString();
		TokenType type = TokenType.RELATIONAL_OPERATOR;
		if ("=".equals(word)) {
			type = TokenType.ASSIGNMENT_OPERATOR;
		}

		return new Token(type, word);
	}

	private Token parseWord() {
		StringBuilder sb = new StringBuilder();
		while (currentPosition < input.length() && Character.isLetterOrDigit(input.charAt(currentPosition))) {
			sb.append(input.charAt(currentPosition));
			currentPosition++;
		}

		String word = sb.toString();
		if (keywords.contains(word)) {
			return new Token(TokenType.KEYWORD, word);
		} else {
			return new Token(TokenType.IDENTIFIER, word);
		}
	}

	public List<Token> tokenize(String input) {

		this.input = input;
		this.currentPosition = 0;

		List<Token> tokens = new ArrayList<>();

		if (input != null) {
			while (currentPosition < input.length()) {
				char currentChar = input.charAt(currentPosition);

				if (Character.isWhitespace(currentChar)) {
					tokens.add(new Token(TokenType.WHITESPACE, String.valueOf(currentChar)));
					currentPosition++;
				} else if (Character.isDigit(currentChar)) {
					tokens.add(parseNumber());
				} else if (Character.isLetter(currentChar)) {
					tokens.add(parseWord());
				} else if (OPERATORS.indexOf(currentChar) != -1) {
					tokens.add(parseOperator());
				} else {
					String s = String.valueOf(currentChar);

					if (QUOTES.indexOf(currentChar) != -1) {
						tokens.add(new Token(TokenType.QUOTE, s));
					} else {
						tokens.add(new Token(defaultToken(s), s));
					}
					currentPosition++;
				}
			}
		}

		return tokens;
	}
}
