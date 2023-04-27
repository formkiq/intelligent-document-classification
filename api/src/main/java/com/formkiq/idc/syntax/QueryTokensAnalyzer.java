package com.formkiq.idc.syntax;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class QueryTokensAnalyzer implements TokensAnalyzer {

	@Override
	public boolean isValid(List<Token> tokens) {

		boolean valid = tokens.size() == 1 && TokenType.IDENTIFIER.equals(tokens.get(0).getType());

		if (!valid) {

			Deque<Token> list = new ArrayDeque<>();

			for (Token token : tokens) {
				if (TokenType.KEYWORD.equals(token.getType())) {

					valid = isValid(list);

					if (!valid) {
						break;
					}

				} else {
					list.push(token);
				}
			}

			if (!list.isEmpty()) {
				valid = isValid(list);
			}
		}

		return valid;
	}

	private boolean isValid(Deque<Token> list) {
		boolean valid;
		if (list.size() == 3) {

			Token t0 = list.pop();
			Token t1 = list.pop();
			Token t2 = list.pop();

			valid = TokenType.IDENTIFIER.equals(t0.getType()) && TokenType.ASSIGNMENT_OPERATOR.equals(t1.getType())
					&& TokenType.IDENTIFIER.equals(t2.getType());

		} else if (list.size() == 1) {
			valid = TokenType.IDENTIFIER.equals(list.pop().getType());
		} else {
			valid = false;
		}
		return valid;
	}

}
