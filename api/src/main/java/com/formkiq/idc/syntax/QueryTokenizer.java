package com.formkiq.idc.syntax;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class QueryTokenizer implements Tokenizer {

	private static final List<String> KEYWORDS = Arrays.asList("and");

	private Tokenizer tokenizer = new BasicTokenizer(KEYWORDS);

	private List<Token> joinTokens(List<Token> tokens) {

		List<Token> list = new ArrayList<>();
		Iterator<Token> itr = tokens.iterator();

		while (itr.hasNext()) {

			Token token = itr.next();

			if (TokenType.SQUARE_BRACKET_LEFT.equals(token.getType())) {

				list.addAll(joinTokens(token, itr, TokenType.SQUARE_BRACKET_RIGHT));

			} else if (TokenType.QUOTE.equals(token.getType())) {

				list.addAll(joinTokens(token, itr, TokenType.QUOTE));

			} else {
				list.add(token);
			}
		}

		return list;
	}

	private List<Token> joinTokens(Token token, Iterator<Token> itr, TokenType endType) {
		List<Token> queue = new ArrayList<Token>();
		queue.add(token);
		
		token = itr.next();

		while (itr.hasNext() && !endType.equals(token.getType())) {
			queue.add(token);
			token = itr.next();
		}

		if (token != null) {
			queue.add(token);
		}

		if (endType.equals(token.getType())) {
			String s = queue.stream().map(t -> t.getValue()).collect(Collectors.joining()).trim();
			queue.clear();
			queue.add(new Token(TokenType.IDENTIFIER, s));
		}

		return queue;
	}

	@Override
	public List<Token> tokenize(String s) {

		List<Token> tokens = tokenizer.tokenize(s);

		tokens = joinTokens(tokens);

		tokens = tokens.stream().filter(t -> !TokenType.WHITESPACE.equals(t.getType())).collect(Collectors.toList());

		return tokens;
	}
}
