package com.formkiq.idc.syntax;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class TokenizerTest {

	private Tokenizer queryTokenizer = new QueryTokenizer();
	private QueryTokensAnalyzer tokensAnalyzer = new QueryTokensAnalyzer();

	void debug(List<Token> tokens) {
		for (Token token : tokens) {
			System.out.println(token.getType() + ": " + token.getValue());
		}
	}

	@Test
	void testQueryTokenizer01() {
		String input = "[category 2] = something and [category]=somethingelse";

		List<Token> tokens = queryTokenizer.tokenize(input);

		int i = 0;
		assertEquals(TokenType.IDENTIFIER, tokens.get(i).getType());
		assertEquals("[category 2]", tokens.get(i++).getValue());

		assertEquals(TokenType.ASSIGNMENT_OPERATOR, tokens.get(i).getType());
		assertEquals("=", tokens.get(i++).getValue());

		assertEquals(TokenType.IDENTIFIER, tokens.get(i).getType());
		assertEquals("something", tokens.get(i++).getValue());

		assertEquals(TokenType.KEYWORD, tokens.get(i).getType());
		assertEquals("and", tokens.get(i++).getValue());

		assertEquals(TokenType.IDENTIFIER, tokens.get(i).getType());
		assertEquals("[category]", tokens.get(i++).getValue());

		assertEquals(TokenType.ASSIGNMENT_OPERATOR, tokens.get(i).getType());
		assertEquals("=", tokens.get(i++).getValue());

		assertEquals(TokenType.IDENTIFIER, tokens.get(i).getType());
		assertEquals("somethingelse", tokens.get(i++).getValue());
		
		assertTrue(tokensAnalyzer.isValid(tokens));
	}

	@Test
	void testQueryTokenizer02() {
		String input = "if (x > 0) { y = x * 2; }";

		List<Token> tokens = queryTokenizer.tokenize(input);

		int i = 0;
		assertEquals(TokenType.IDENTIFIER, tokens.get(i).getType());
		assertEquals("if", tokens.get(i++).getValue());

		assertEquals(TokenType.LEXEMES, tokens.get(i).getType());
		assertEquals("(", tokens.get(i++).getValue());

		assertEquals(TokenType.IDENTIFIER, tokens.get(i).getType());
		assertEquals("x", tokens.get(i++).getValue());
		
		assertFalse(tokensAnalyzer.isValid(tokens));
	}
	
	@Test
	void testQueryTokenizer03() {
		String input = "something";

		List<Token> tokens = queryTokenizer.tokenize(input);

		int i = 0;
		assertEquals(TokenType.IDENTIFIER, tokens.get(i).getType());
		assertEquals("something", tokens.get(i++).getValue());
		
		assertTrue(tokensAnalyzer.isValid(tokens));
	}
	
	@Test
	void testQueryTokenizer04() {
		String input = "[category 2] = something ";

		List<Token> tokens = queryTokenizer.tokenize(input);

		int i = 0;
		assertEquals(TokenType.IDENTIFIER, tokens.get(i).getType());
		assertEquals("[category 2]", tokens.get(i++).getValue());

		assertEquals(TokenType.ASSIGNMENT_OPERATOR, tokens.get(i).getType());
		assertEquals("=", tokens.get(i++).getValue());

		assertEquals(TokenType.IDENTIFIER, tokens.get(i).getType());
		assertEquals("something", tokens.get(i++).getValue());
		
		assertTrue(tokensAnalyzer.isValid(tokens));
	}
	
	@Test
	void testQueryTokenizer05() {
		String input = null;
		List<Token> tokens = queryTokenizer.tokenize(input);		
		assertFalse(tokensAnalyzer.isValid(tokens));
	}
}