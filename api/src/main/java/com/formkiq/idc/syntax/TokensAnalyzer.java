package com.formkiq.idc.syntax;

import java.util.List;

public interface TokensAnalyzer {

	boolean isValid(List<Token> tokens);
}
