/**
 * miniJava Syntactic Analyser classes
 * @author Nicola Folchi
 */
package miniJava.SyntacticAnalyzer;

import miniJava.SyntacticAnalyzer.TokenKind;

/**
 *  A token has a kind and a spelling
 *  In a compiler it would also have a source position 
 */
public class Token {
	public TokenKind kind;
	public String spelling;
	public SourcePosition posn;

	public Token(TokenKind kind, String spelling, SourcePosition posn ) {
		this.kind = kind;
		this.spelling = spelling;
		this.posn = posn;
	}
}