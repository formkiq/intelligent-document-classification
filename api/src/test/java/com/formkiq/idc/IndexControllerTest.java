package com.formkiq.idc;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.formkiq.idc.elasticsearch.ElasticSeachRequest;
import com.formkiq.idc.syntax.QueryTokenizer;
import com.formkiq.idc.syntax.QueryTokensAnalyzer;
import com.formkiq.idc.syntax.Token;
import com.formkiq.idc.syntax.Tokenizer;

class IndexControllerTest {

	private IndexController controller = new IndexController();
	private Tokenizer queryTokenizer = new QueryTokenizer();
	private QueryTokensAnalyzer tokensAnalyzer = new QueryTokensAnalyzer();

	@Test
	void testText01() {
		String text = "[loc]=MB and [per]=\"john smith\"";
		List<Token> tokens = queryTokenizer.tokenize(text);

		boolean valid = tokensAnalyzer.isValid(tokens);
		assertTrue(valid);
		
		ElasticSeachRequest request = controller.createElasticSearchRequest(tokens);
		assertNull(request.getText());
		assertEquals(2, request.getTags().size());
		assertEquals("MB", request.getTags().get("loc"));
		assertEquals("john smith", request.getTags().get("per"));
	}

	@Test
	void testText02() {
		String text = "[loc]=MB and 'some text'";
		List<Token> tokens = queryTokenizer.tokenize(text);

		boolean valid = tokensAnalyzer.isValid(tokens);
		assertTrue(valid);
		
		ElasticSeachRequest request = controller.createElasticSearchRequest(tokens);
		assertEquals("some text", request.getText());
		assertEquals(1, request.getTags().size());
		assertEquals("MB", request.getTags().get("loc"));
	}
}
