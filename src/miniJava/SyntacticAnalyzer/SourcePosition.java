/**
 * miniJava Syntactic Analyser classes
 * @author Nicola Folchi
 */
package miniJava.SyntacticAnalyzer;

public class SourcePosition {
	SourcePosition posn;
	
	public int start;
	public int end;
	
	public SourcePosition() {
		start = 0;
		end = 0;
	}
}
