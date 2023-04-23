package com.formkiq.idc.syntax;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class QueryTokenizer implements Tokenizer {

	private static final List<String> KEYWORDS = Arrays.asList("and");

	private Tokenizer tokenizer = new BasicTokenizer(KEYWORDS);

	private List<Token> joinCategory(List<Token> tokens) {

		List<Token> list = new ArrayList<>();
		Iterator<Token> itr = tokens.iterator();

		List<Token> queue = null;

		while (itr.hasNext()) {

			Token token = itr.next();

			if (TokenType.SQUARE_BRACKET_LEFT.equals(token.getType())) {

				queue = new ArrayList<Token>();

			} else if (queue != null) {

				if (TokenType.SQUARE_BRACKET_RIGHT.equals(token.getType())) {

					String s = "[" + queue.stream().map(t -> t.getValue()).collect(Collectors.joining()).trim() + "]";
					list.add(new Token(TokenType.IDENTIFIER, s));

					queue = null;

				} else {
					queue.add(token);
				}

			} else {
				list.add(token);
			}
		}

		return list;
	}

	@Override
	public List<Token> tokenize(String s) {

		List<Token> tokens = tokenizer.tokenize(s);

		tokens = joinCategory(tokens);

		tokens = tokens.stream().filter(t -> !TokenType.WHITESPACE.equals(t.getType())).collect(Collectors.toList());

		return tokens;
	}
}
