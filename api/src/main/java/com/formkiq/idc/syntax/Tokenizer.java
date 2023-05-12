package com.formkiq.idc.syntax;

import java.util.List;

public interface Tokenizer {
	List<Token> tokenize(String s);
}
