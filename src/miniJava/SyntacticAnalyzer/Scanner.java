/**
 * miniJava Syntactic Analyser classes
 * @author Nicola Folchi
 */
package miniJava.SyntacticAnalyzer;

import java.io.*;
import miniJava.ErrorReporter;

public class Scanner {

	private InputStream inputStream;
	private ErrorReporter reporter;

	private char currentChar;
	private StringBuilder currentSpelling;
	private int currentLine;

	// true when end of line is found
	private boolean eot = false;  

	public Scanner(InputStream inputStream, ErrorReporter reporter) { 
		this.inputStream = inputStream;
		this.reporter = reporter;
		this.currentLine = 1;

		// initialize scanner state
		readChar();
	}

	/**
	 * skip whitespace and scan next token
	 */
	public Token scan() {
		SourcePosition posn = new SourcePosition();
		posn.start = currentLine;
		posn.end = currentLine;
		// skip whitespace
		while (!eot && (currentChar == ' ' || currentChar == '\t' || currentChar == '\n' || currentChar == '\r'))
			skipIt();

		// start of a token: collect spelling and identify token kind
		currentSpelling = new StringBuilder();
		TokenKind kind = scanToken();
		String spelling = currentSpelling.toString();

		// return new token
		return new Token(kind, spelling, posn);
	}

	/**
	 * determine token kind
	 */
	public TokenKind scanToken() {
		// skipping comments
		while (currentChar == '/') {
			if (currentChar == '/') {
				skipIt();
				if (currentChar == '/' || currentChar == '*') {
					if (currentChar == '/') {
						skipIt();
						while (true) {
							if (eot) {
								// if reached here, then just exit from it so the program can terminate
								break;
							}
							if (currentChar == '\n' || currentChar == '\r') {
								skipIt();
								break;
							} else {
								skipIt();
							}
						}
					} else if (currentChar == '*') {
						skipIt();
						while (true) {
							if (eot) {
								scanError("Unrecognized character '" + currentChar + "' in input");
								return (TokenKind.ERROR);
							}
							if (currentChar == '*') {
								skipIt();
								if (currentChar == '/') {
									skipIt();
									break;
								}
							}
							if (currentChar != '*') {
								skipIt();
							}
						}
					}
				} else {
					currentSpelling.append('/');
					return TokenKind.MULTIPLICATIVE;
				}
			}
			while (!eot && (currentChar == ' ' || currentChar == '\t' || currentChar == '\n' || currentChar == '\r')) {
				skipIt();
			}
		}
//		// skipping new lines, tabs and carriage returns
		while (currentChar == '\\') {
			skipIt();
			if (currentChar == 't' || currentChar == 'n' || currentChar == 'r') {
				skipIt();
			}

		}
		// skipping all kinds of spaces after searching for comments
		while (!eot && (currentChar == ' ' || currentChar == '\t' || currentChar == '\n' || currentChar == '\r')) {
			skipIt();
		}

		if (eot)
			return (TokenKind.EOT);
		// scan Token
		switch (currentChar) {

		case '(':
			takeIt();
			return (TokenKind.LPAREN);

		case ')':
			takeIt();
			return (TokenKind.RPAREN);

		case '{':
			takeIt();
			return (TokenKind.LCURLYBRACKET);

		case '}':
			takeIt();
			return (TokenKind.RCURLYBRACKET);

		case '[':
			takeIt();
			return (TokenKind.LSQUAREBRACKET);

		case ']':
			takeIt();
			return (TokenKind.RSQUAREBRACKET);

		case ';':
			takeIt();
			return (TokenKind.SEMICOLON);

		case '.':
			takeIt();
			return (TokenKind.DOT);
			
		case '<':
		case '>':
			takeIt();
			if (currentChar == '=')
				takeIt();
			return (TokenKind.RELATIONAL);
			
		case '-':
			takeIt();
			return (TokenKind.NEGATIVE);
			
		case '+':
			takeIt();
			return (TokenKind.ADDITIVE);
			
		case '*':
		case '/':
			takeIt();
			return (TokenKind.MULTIPLICATIVE);
			
		case '&':
			takeIt();
			if (currentChar == '&') {
				takeIt();
				return (TokenKind.CONJUNCTION);
			} else {
				scanError("Unrecognized character '" + currentChar + "' in input");
				return (TokenKind.ERROR);
			}
			
		case '|':
			takeIt();
			if (currentChar == '|') {
				takeIt();
				return (TokenKind.DISJUNCTION);
			} else {
				scanError("Unrecognized character '" + currentChar + "' in input");
				return (TokenKind.ERROR);
			}
			
		case '=':
			takeIt();
			if (currentChar == '=') {
				takeIt();
				return (TokenKind.EQUALITY);
			}
			return (TokenKind.EQUALS);
			
		case '!':
			takeIt();
			if (currentChar == '=') {
				takeIt();
				return (TokenKind.EQUALITY);
			} else {
				return (TokenKind.NOT);
			}
			
		case ',':
			takeIt();
			return (TokenKind.COMMA);
			
		case 'a': case 'b': case 'c': case 'd': case 'e': case 'f': case 'g': case 'h':
		case 'i': case 'j': case 'k': case 'm': case 'n': case 'o': case 'p': case 'q':
		case 'r': case 's': case 't': case 'u': case 'v': case 'w': case 'x': case 'y':
		case 'z': case 'A': case 'B': case 'C': case 'D': case 'E': case 'F': case 'G':
		case 'H': case 'I': case 'J': case 'K': case 'L': case 'M': case 'N': case 'O':
		case 'P': case 'Q': case 'R': case 'S': case 'T': case 'U': case 'V': case 'W':
		case 'X': case 'Y': case 'Z': case 'l':
			if (isLetter(currentChar) == true || currentChar == '_') {
				while (isValidID(currentChar) == true) {
					takeIt();
				}
				String spelling = currentSpelling.toString();

				if (spelling.equals("int")) {
					return (TokenKind.INT);
				}
				if (spelling.equals("class")) {
					return (TokenKind.CLASS);
				}
				if (spelling.equals("public")) {
					return (TokenKind.PUBLIC);
				}
				if (spelling.equals("private")) {
					return (TokenKind.PRIVATE);
				}
				if (spelling.equals("static")) {
					return (TokenKind.STATIC);
				}
				if (spelling.equals("boolean")) {
					return (TokenKind.BOOLEAN);
				}
				if (spelling.equals("void")) {
					return (TokenKind.VOID);
				}
				if (spelling.equals("return")) {
					return (TokenKind.RETURN);
				}
				if (spelling.equals("if")) {
					return (TokenKind.IF);
				}
				if (spelling.equals("else")) {
					return (TokenKind.ELSE);
				}
				if (spelling.equals("while")) {
					return (TokenKind.WHILE);
				}
				if (spelling.equals("this")) {
					return (TokenKind.THIS);
				}
				if (spelling.equals("true")) {
					return (TokenKind.TRUE);
				}
				if (spelling.equals("false")) {
					return (TokenKind.FALSE);
				}
				if (spelling.equals("new")) {
					return (TokenKind.NEW);
				} 
				if (spelling.equals("null")){
					return (TokenKind.NULL);
				}else {
					return (TokenKind.ID);
				}
			}
	
		case '0': case '1': case '2': case '3': case '4':
		case '5': case '6': case '7': case '8': case '9':
			while (isDigit(currentChar))
				takeIt();
			return (TokenKind.NUM);

		default:
			scanError("Unrecognized character '" + currentChar + "' in input");
			return (TokenKind.ERROR);
		}
	}

	private boolean isLetter(char c) {
		return (Character.isLetter(c));
	}

	private boolean isValidID(char c) {
		if (Character.isDigit(c) == true || Character.isLetter(c) == true || c == '_') {
			return true;
		}
		return false;
	}

	private void takeIt() {
		currentSpelling.append(currentChar);
		nextChar();
	}

	private void skipIt() {
		nextChar();
	}

	private boolean isDigit(char c) {
		return (c >= '0') && (c <= '9');
	}

	private void scanError(String m) {
		reporter.reportError("Scan Error:  " + m);
	}

	/**
	 * advance to next char in inputstream detect end of file or end of line as end
	 * of input
	 */
	private void nextChar() {
		if (!eot)
			readChar();
	}

	private void readChar() {
		try {
			int c = inputStream.read();
			currentChar = (char) c;
			if ((char)c == '\n') {
				currentLine++;
			}
			if (c == -1) {
				eot = true;
			}
		} catch (IOException e) {
			scanError("I/O Exception!");
			eot = true;
		}
	}
}
