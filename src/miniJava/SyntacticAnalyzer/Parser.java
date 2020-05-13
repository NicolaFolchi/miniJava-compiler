/**
 * miniJava Syntactic Analyser classes
 * @author Nicola Folchi
 */
package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenKind;
import miniJava.SyntacticAnalyzer.Parser.SyntaxError;

public class Parser {

	private Scanner scanner;
	private ErrorReporter reporter;
	private Token token;
	private boolean trace = true;
	private SourcePosition position; 

	public Parser(Scanner scanner, ErrorReporter reporter) {
		this.scanner = scanner; 
		this.reporter = reporter;
		this.position = new SourcePosition();
	}

	/**
	 * SyntaxError is used to unwind parse stack when parse fails
	 *
	 */
	class SyntaxError extends Error {
		private static final long serialVersionUID = 1L;
	}

	/**
	 * parse input, catch possible parse error
	 */
	public Package parse() throws SyntaxError {
		Package packageAST = null;
		ClassDeclList classes = new ClassDeclList();
		SourcePosition posn = new SourcePosition();
		token = scanner.scan();
		beginPosn(posn);
		try {
			while (token.kind == TokenKind.CLASS) {
				ClassDecl classDeclarationAST = parseClassDeclaration();
				classes.add(classDeclarationAST);
			}
			packageAST = new Package(classes, posn);
			accept(TokenKind.EOT);
			endPosn(posn);
		} catch (SyntaxError e) {
			return null;
		}
		return packageAST;
	}

	private ClassDecl parseClassDeclaration() throws SyntaxError {
		ClassDecl classDeclarationAST = null;
		
		FieldDeclList fieldDeclarationList = new FieldDeclList();
		MethodDeclList methodDeclarationList = new MethodDeclList();
		SourcePosition classPosn = new SourcePosition();
		beginPosn(classPosn);
//		MemberDecl memberDecl = null;
//		FieldDecl fieldDecl = null; 
//		MethodDecl methodDecl = null; 
		
		if (token.kind == TokenKind.CLASS) {
			acceptIt();
			Identifier className = new Identifier(token);
			String classSpelling = token.spelling; 
			accept(TokenKind.ID);
			accept(TokenKind.LCURLYBRACKET);
			while (token.kind != TokenKind.RCURLYBRACKET) {
				SourcePosition memberPosn = new SourcePosition();
				beginPosn(memberPosn);
				boolean isPrivate = false;
				boolean isStatic = false;
				if (token.kind == TokenKind.PUBLIC)
					acceptIt();
				else if (token.kind == TokenKind.PRIVATE) {
					acceptIt();
					isPrivate = true;
				}
				if (token.kind == TokenKind.STATIC) {
					acceptIt();
					isStatic = true;
				}
				if (token.kind == TokenKind.VOID) { // method
					TypeDenoter type = new BaseType(TypeKind.VOID, token.posn);
					acceptIt();
					Identifier methodName = new Identifier(token);
					String methodSpelling = token.spelling; 
					accept(TokenKind.ID);
					accept(TokenKind.LPAREN);
					methodDeclarationList.add(parseMethod(isPrivate, isStatic, type, methodSpelling, memberPosn));
				} else { // method is of Type || filed declaration
					TypeDenoter type = parseType();
					Identifier id = new Identifier(token);
					String idSpelling = token.spelling; 
					accept(TokenKind.ID);
					if (token.kind == TokenKind.LPAREN) { // method
						acceptIt();
						methodDeclarationList.add(parseMethod(isPrivate, isStatic, type, idSpelling, memberPosn));
					} else { // field declaration
						accept(TokenKind.SEMICOLON);
						endPosn(memberPosn);
						fieldDeclarationList.add(new FieldDecl(isPrivate, isStatic, type, idSpelling, memberPosn));
					}
				}
			}
			accept(TokenKind.RCURLYBRACKET);
			endPosn(classPosn);
			classDeclarationAST = new ClassDecl(classSpelling, fieldDeclarationList, methodDeclarationList, classPosn);
		}
		return classDeclarationAST;
	}

	private TypeDenoter parseType() throws SyntaxError {
		SourcePosition typePosn = new SourcePosition();
		beginPosn(typePosn);
		switch (token.kind) {
		case INT:
			acceptIt();
			if (token.kind == TokenKind.LSQUAREBRACKET) {
				acceptIt();
				accept(TokenKind.RSQUAREBRACKET);
				endPosn(typePosn);
				return new ArrayType(new BaseType(TypeKind.INT, typePosn), typePosn);
			}
			endPosn(typePosn);
			return new BaseType(TypeKind.INT, typePosn);
		case BOOLEAN:
			acceptIt();
			endPosn(typePosn);
			return new BaseType(TypeKind.BOOLEAN, typePosn);
		case ID:
			Token id = token;
			acceptIt();
			if (token.kind == TokenKind.LSQUAREBRACKET) {
				acceptIt();
				accept(TokenKind.RSQUAREBRACKET);
				endPosn(typePosn);
				return new ArrayType(new ClassType(new Identifier(id), typePosn), typePosn);
			}
			endPosn(typePosn);
			return new ClassType(new Identifier(id), typePosn);

		default:
			endPosn(typePosn);
			return new BaseType(TypeKind.ERROR, typePosn);
		}
	}

	private MethodDecl parseMethod(boolean isPrivate, boolean isStatic, TypeDenoter type, String id, SourcePosition posn) throws SyntaxError {
		ParameterDeclList parameterDeclList = new ParameterDeclList();
		StatementList statementList = new StatementList();

		if (token.kind == TokenKind.INT || token.kind == TokenKind.BOOLEAN || token.kind == TokenKind.ID) {
			parameterDeclList = parseParameterList();
		}
		accept(TokenKind.RPAREN);
		accept(TokenKind.LCURLYBRACKET);
		while (token.kind != TokenKind.RCURLYBRACKET) {
			statementList.add(parseStatement());
		}
		accept(TokenKind.RCURLYBRACKET);
		endPosn(posn);
		return new MethodDecl(new FieldDecl(isPrivate, isStatic, type, id, posn), parameterDeclList, statementList, posn);
	}

	private Statement parseStatement() throws SyntaxError {
		SourcePosition statementPosn = new SourcePosition();
		beginPosn(statementPosn);
		Statement statement = null;
		switch (token.kind) {
		case LCURLYBRACKET: // { Statement* }
			acceptIt();
			StatementList statementList = new StatementList();
			while (token.kind != TokenKind.RCURLYBRACKET) {
				statementList.add(parseStatement());
			}
			accept(TokenKind.RCURLYBRACKET);
			endPosn(statementPosn);
			statement = new BlockStmt(statementList, statementPosn);
			break;
		// case TYPE Excluding ID
		case INT:
		case BOOLEAN:
			TypeDenoter type = parseType();
			Identifier id = new Identifier(token);
			String idSpelling = token.spelling;
			accept(TokenKind.ID);
			accept(TokenKind.EQUALS);
			Expression varDeclarationExpression = parseExpression();
			accept(TokenKind.SEMICOLON);
			endPosn(statementPosn);
			statement = new VarDeclStmt(new VarDecl(type, idSpelling, id.posn), varDeclarationExpression, statementPosn);
			break;

		case ID:
			Identifier idType = new Identifier(token);
			accept(TokenKind.ID);
			Identifier idNameReference = null;
			VarDecl varDeclaration = null;
			Reference reference = null;
			Expression expression = null;
			boolean isTypeId = false;
			SourcePosition variablePosn = new SourcePosition();

			if (token.kind == TokenKind.ID) { // Having a type ID, followed by its id === SomeType myVar =
				idNameReference = new Identifier(token);
				String idSpelling1 = token.spelling;
				accept(TokenKind.ID);
				isTypeId = true;
				variablePosn.start = idType.posn.start;
				variablePosn.end = idNameReference.posn.end;
				varDeclaration = new VarDecl(new ClassType(idType, idType.posn), idSpelling1, variablePosn);
			} else if (token.kind == TokenKind.LSQUAREBRACKET) { // having a typeId[]
				acceptIt();
				if (token.kind == TokenKind.RSQUAREBRACKET) {
					acceptIt();
					idNameReference = new Identifier(token);
					String idSpelling2 = token.spelling;
					accept(TokenKind.ID);
					isTypeId = true;
					variablePosn.start = idType.posn.start;
					variablePosn.end = idNameReference.posn.end;
					varDeclaration = new VarDecl(new ArrayType(new ClassType(idType, idType.posn), idType.posn), idSpelling2, variablePosn);
				} else { // having a typeId[expression] ===> reference (IxAssignStmt)
					reference = new IdRef(idType, idType.posn);
					Expression referenceExpression = parseExpression();
					accept(TokenKind.RSQUAREBRACKET);
					expression = referenceExpression;
				}
			} else if (token.kind == TokenKind.DOT) { // having a reference of typeID.something else here
				SourcePosition idPosn = new SourcePosition();
				idPosn.start = idType.posn.start;
				reference = new IdRef(idType, idType.posn);
				while (token.kind == TokenKind.DOT) { // ref.ref[]
					acceptIt();
					Identifier identifier = new Identifier(token);
					accept(TokenKind.ID);
					endPosn(idPosn);
					reference = new QualRef(reference, identifier, idPosn);
				}
				if (token.kind == TokenKind.LSQUAREBRACKET) {
					acceptIt();
					Expression referenceExpression = parseExpression();
					accept(TokenKind.RSQUAREBRACKET);
					expression = referenceExpression;
				} else if (token.kind == TokenKind.LPAREN && expression == null && isTypeId == false) { // reference (argumentList?)
					acceptIt();
					ExprList argumentList = new ExprList();
					if (token.kind == TokenKind.RPAREN) {
						acceptIt();
						accept(TokenKind.SEMICOLON);
					} else {
						argumentList = parseArgumentList();
						accept(TokenKind.RPAREN);
						accept(TokenKind.SEMICOLON);
					}
					endPosn(idPosn);
					statement = new CallStmt(reference, argumentList, idPosn);
				}
			} else {
				reference = new IdRef(idType, idType.posn);
			}
			
			// deciding on missing part of expression
			if (token.kind == TokenKind.EQUALS) {
				acceptIt();
				Expression assignmentExpression = parseExpression(); 
				accept(TokenKind.SEMICOLON);
				endPosn(statementPosn);
				if (reference == null) {
					statement = new VarDeclStmt(varDeclaration, assignmentExpression, statementPosn);
				} else {
					if (expression != null) {
						statement = new IxAssignStmt(reference, expression, assignmentExpression, statementPosn);
					} else {
						statement = new AssignStmt(reference, assignmentExpression, statementPosn);
					}
				}
			} else if (token.kind == TokenKind.LPAREN && expression == null && isTypeId == false) { // reference (argumentList? )
				acceptIt();
				ExprList argumentList = new ExprList();
				reference = new IdRef(idType, idType.posn);
				if (token.kind == TokenKind.RPAREN) {
					acceptIt();
					accept(TokenKind.SEMICOLON);
				} else {
					argumentList = parseArgumentList();
					accept(TokenKind.RPAREN);
					accept(TokenKind.SEMICOLON);
				}
				endPosn(statementPosn);
				statement = new CallStmt(reference, argumentList, statementPosn);
			} else {
				reference = new IdRef(idType, idType.posn);
			}
			break;
			
		case THIS: // having a reference that starts with this. ===> this.id || this[e]
			SourcePosition thisPosn = token.posn;
			accept(TokenKind.THIS);
			Reference thisReference = new ThisRef(thisPosn);
			Expression thisExpression = null;
			if (token.kind == TokenKind.DOT) { // having a reference of typeID.something else here
				while (token.kind == TokenKind.DOT) { // ref.ref[] this.soemtehding ()
					acceptIt();
					Identifier identifier = new Identifier(token);
					accept(TokenKind.ID);
					endPosn(thisPosn);
					thisReference = new QualRef(thisReference, identifier, thisPosn);
				}
				if (token.kind == TokenKind.LSQUAREBRACKET) {
					acceptIt();
					Expression referenceExpression = parseExpression();
					accept(TokenKind.RSQUAREBRACKET);
					thisExpression = referenceExpression;
				}
			} else if (token.kind == TokenKind.LSQUAREBRACKET) {
				acceptIt();
				Expression referenceExpression = parseExpression();
				accept(TokenKind.RSQUAREBRACKET);
				thisExpression = referenceExpression;
			}
			if (token.kind == TokenKind.EQUALS) {
				acceptIt();
				Expression thisAssignExpression = parseExpression();
				accept(TokenKind.SEMICOLON);
				endPosn(statementPosn);
				if (thisExpression != null) {
					statement = new IxAssignStmt(thisReference, thisExpression, thisAssignExpression, statementPosn);
				} else {
					statement = new AssignStmt(thisReference, thisAssignExpression, statementPosn);
				}
			} else if (token.kind == TokenKind.LPAREN) {
				acceptIt();
				ExprList argumentList = new ExprList();
				if (token.kind == TokenKind.RPAREN) {
					acceptIt();
					accept(TokenKind.SEMICOLON);
				} else {
					argumentList = parseArgumentList();
					accept(TokenKind.RPAREN);
					accept(TokenKind.SEMICOLON);
				}
				endPosn(statementPosn);
				statement = new CallStmt(thisReference, argumentList, statementPosn);
			} else {
				parseError("An error was found while trying to parse a statement");
			}
			break;
			
		case RETURN: // return Expression? ;
			acceptIt();
			if (token.kind != TokenKind.SEMICOLON) {
				Expression returnExpression = parseExpression();
				accept(TokenKind.SEMICOLON);
				endPosn(statementPosn);
				statement = new ReturnStmt(returnExpression, statementPosn);
				break;
			}
			accept(TokenKind.SEMICOLON);
			endPosn(statementPosn);
			statement = new ReturnStmt(null, statementPosn);
			break;

		case IF: // if ( Expression ) Statement (else Statement)?
			acceptIt();
			accept(TokenKind.LPAREN);
			Expression ifCondition = parseExpression();
			accept(TokenKind.RPAREN);
			Statement thenStatement = parseStatement();
			if (token.kind == TokenKind.ELSE) {
				acceptIt();
				Statement elseStatement = parseStatement();
				statement = new IfStmt(ifCondition, thenStatement, elseStatement, statementPosn);
			} else {
				statement = new IfStmt(ifCondition, thenStatement, statementPosn);
			}
			endPosn(statementPosn);
			break;

		case WHILE: // while ( Expression ) Statement
			acceptIt();
			accept(TokenKind.LPAREN);
			Expression whileExpression = parseExpression();
			accept(TokenKind.RPAREN);
			Statement body = parseStatement();
			endPosn(statementPosn);
			statement = new WhileStmt(whileExpression, body, statementPosn);
			break;

		default:
			parseError("An error was found while trying to parse a statement");
		}
		return statement;
	}

	private ExprList parseArgumentList() throws SyntaxError {
		ExprList expressionList = new ExprList();
		expressionList.add(parseExpression());

		while (token.kind == TokenKind.COMMA) {
			acceptIt();
			expressionList.add(parseExpression());
		}
		return expressionList;
	}

	private ParameterDeclList parseParameterList() throws SyntaxError {
		ParameterDeclList parameterDeclarationList = new ParameterDeclList();
		SourcePosition parameterPosn = new SourcePosition();
		beginPosn(parameterPosn);
		TypeDenoter type = parseType();
		Identifier id = new Identifier(token);
		String idSpelling = token.spelling; 
		accept(TokenKind.ID);
		endPosn(parameterPosn);
		ParameterDecl parameterDeclaration = new ParameterDecl(type, idSpelling, parameterPosn);
		parameterDeclarationList.add(parameterDeclaration);
		while (token.kind == TokenKind.COMMA) {
			acceptIt();
			type = parseType();
			id = new Identifier(token);
			idSpelling = token.spelling; 
			accept(TokenKind.ID);
			endPosn(parameterPosn);
			parameterDeclaration = new ParameterDecl(type, idSpelling, parameterPosn);
			parameterDeclarationList.add(parameterDeclaration);
		}
		return parameterDeclarationList;
	}
	
	 private Expression parseExpression() throws SyntaxError {
	    	return parseDisjunction();
	    }

	    private Expression parseDisjunction() throws SyntaxError {
	        Expression expression;
	        SourcePosition disjuncPosn = new SourcePosition();
	        beginPosn(disjuncPosn);
	        expression = parseConjunction();
	        while (token.kind == TokenKind.DISJUNCTION) {
	            Operator operator = new Operator(token);
	            acceptIt();
	            Expression secondExpression = parseConjunction();
	            endPosn(disjuncPosn);
	            expression = new BinaryExpr(operator, expression, secondExpression, disjuncPosn);
	        }
	        return expression;
	    }

	    private Expression parseConjunction() throws SyntaxError {
	        Expression expression;
	        SourcePosition conjuncPosn = new SourcePosition();
	        beginPosn(conjuncPosn);
	        expression = parseEquality();
	        while (token.kind == TokenKind.CONJUNCTION) {
	            Operator operator = new Operator(token);
	            acceptIt();
	            Expression secondExpression = parseEquality();
	            endPosn(conjuncPosn);
	            expression = new BinaryExpr(operator, expression, secondExpression, conjuncPosn);
	        }
	        return expression;
	    }

	    private Expression parseEquality() throws SyntaxError {
	        Expression expression;
	        SourcePosition equalityPosn = new SourcePosition();
	        beginPosn(equalityPosn);
	        expression = parseRelational();
	        while (token.kind == TokenKind.EQUALITY) {
	            Operator operator = new Operator(token);
	            acceptIt();
	            Expression secondExpression = parseRelational();
	            endPosn(equalityPosn);
	            expression = new BinaryExpr(operator, expression, secondExpression, equalityPosn);
	        }
	        return expression;
	    }

	    private Expression parseRelational() throws SyntaxError {
	        Expression expression;
	        SourcePosition relationalPosn = new SourcePosition();
	        beginPosn(relationalPosn);
	        expression = parseAdditive();
	        while (token.kind == TokenKind.RELATIONAL) {
	            Operator operator = new Operator(token);
	            acceptIt();
	            Expression secondExpression = parseAdditive();
	            endPosn(relationalPosn);
	            expression = new BinaryExpr(operator, expression, secondExpression, relationalPosn);
	        }
	        return expression;
	    }

	    private Expression parseAdditive() throws SyntaxError {
	        Expression expression;
	        SourcePosition additivePosn = new SourcePosition();
	        beginPosn(additivePosn);
	        expression = parseMultiplicative();
	        while (token.kind == TokenKind.ADDITIVE || token.kind == TokenKind.NEGATIVE) {
	            Operator operator = new Operator(token);
	            acceptIt();
	            Expression secondExpression = parseMultiplicative();
	            endPosn(additivePosn);
	            expression = new BinaryExpr(operator, expression, secondExpression, additivePosn);
	        }
	        return expression;
	    }
	    private Expression parseMultiplicative() throws SyntaxError {
	        Expression expression;
	        SourcePosition multiPosn = new SourcePosition();
	        beginPosn(multiPosn);
	        expression = parseUnaryExpression();
	        while (token.kind == TokenKind.MULTIPLICATIVE) {
	            Operator operator = new Operator(token);
	            acceptIt();
	            Expression secondExpression = parseUnaryExpression();
	            endPosn(multiPosn);
	            expression = new BinaryExpr(operator, expression, secondExpression, multiPosn);
	        } 
	        return expression;
	    }

	    private Expression parseUnaryExpression() throws SyntaxError {
	        Expression expression;
	        SourcePosition unaryPosn = new SourcePosition();
	        beginPosn(unaryPosn);
	        if (token.kind ==  TokenKind.NOT || token.kind == TokenKind.NEGATIVE) {
	            Operator op = new Operator(token);
	            acceptIt();
	            Expression unaryExpression = parseUnaryExpression();
	            endPosn(unaryPosn);
	            expression = new UnaryExpr(op, unaryExpression, unaryPosn);
	        } else {
	            expression = parseRegularExpression();
	        }
	        return expression;
	    }

	private Expression parseRegularExpression() throws SyntaxError { 
		Expression expression = null;
		SourcePosition regularExprPosn = new SourcePosition();
		beginPosn(regularExprPosn);
		switch (token.kind) {
		case NEW:
			acceptIt();
            if (token.kind == TokenKind.ID) {
                Identifier id = new Identifier(token);
                acceptIt();
                if (token.kind == TokenKind.LPAREN) {
                    acceptIt();
                    accept(TokenKind.RPAREN);
                    endPosn(regularExprPosn);
                    expression = new NewObjectExpr(new ClassType(id, id.posn), regularExprPosn);
                } else if (token.kind == TokenKind.LSQUAREBRACKET) {
                    acceptIt();
                    Expression exp = parseExpression();
                    accept(TokenKind.RSQUAREBRACKET);
                    endPosn(regularExprPosn);
                    expression = new NewArrayExpr(new ClassType(id, id.posn), exp, regularExprPosn);
                } else {
                	parseError("Error, wrong usage of the keyword \"new\"");
                }
            } else if (token.kind == TokenKind.INT) {
                SourcePosition intPos = token.posn;
                acceptIt();
                accept(TokenKind.LSQUAREBRACKET);
                Expression exp = parseExpression();
                accept(TokenKind.RSQUAREBRACKET);
                endPosn(regularExprPosn);
                expression = new NewArrayExpr(new BaseType(TypeKind.INT, intPos), exp, regularExprPosn);
            } else {
                parseError("Error when trying to parse single expression.");
            }
            break;
            
		case NUM:
			IntLiteral initialLiteral = new IntLiteral(token);
			acceptIt();
			endPosn(regularExprPosn);
			expression = new LiteralExpr(initialLiteral, regularExprPosn);
			break;

		case TRUE:
		case FALSE:
			BooleanLiteral boolLiteral = new BooleanLiteral(token);
			acceptIt();
			endPosn(regularExprPosn);
			expression = new LiteralExpr(boolLiteral, regularExprPosn);
			break;

		case LPAREN:
			acceptIt();
			expression = parseExpression();
			accept(TokenKind.RPAREN);
			break;
			
		case NULL:
			NullLiteral nullExpr = new NullLiteral(token);
			acceptIt();
			endPosn(regularExprPosn);
			expression =  new LiteralExpr(nullExpr, regularExprPosn);
			break;

		case ID:
		case THIS:
			// parsing a reference
			if (token.kind == TokenKind.ID) {
				Identifier idType = new Identifier(token);
				accept(TokenKind.ID);
				Reference reference = new IdRef(idType, idType.posn);

				if (token.kind == TokenKind.LSQUAREBRACKET) { // having a typeId[expression] ===> reference  (IxAssignStmt)
					acceptIt();
					if (token.kind != TokenKind.RSQUAREBRACKET) {
						Expression referenceExpression = parseExpression();
						accept(TokenKind.RSQUAREBRACKET);
						endPosn(regularExprPosn);
						expression = new IxExpr(reference, referenceExpression, regularExprPosn);
					}
				} else if (token.kind == TokenKind.DOT) { // having a reference of typeID.something else here
					while (token.kind == TokenKind.DOT) { // ref.ref()
						acceptIt();
						Identifier identifier = new Identifier(token);
						accept(TokenKind.ID);
						endPosn(regularExprPosn);
						reference = new QualRef(reference, identifier, regularExprPosn);
					}
					if (token.kind == TokenKind.LSQUAREBRACKET) {
						acceptIt();
						Expression referenceExpression = parseExpression();
						accept(TokenKind.RSQUAREBRACKET);
						endPosn(regularExprPosn);
						expression = new IxExpr(reference, referenceExpression, regularExprPosn);
					}  else if (token.kind == TokenKind.LPAREN) { // reference ( argumentList? )
						acceptIt();
						ExprList argumentList = new ExprList();
						if (token.kind == TokenKind.RPAREN) {
							acceptIt();
						} else {
							argumentList = parseArgumentList();
							accept(TokenKind.RPAREN);
						}
						endPosn(regularExprPosn);
						expression = new CallExpr(reference, argumentList, regularExprPosn);
					} else {
						endPosn(regularExprPosn);
						expression = new RefExpr(reference, regularExprPosn);
					}
				}
				// deciding how not to have the case ref[]()
				else if (token.kind == TokenKind.LPAREN) { // reference ( argumentList? )
					acceptIt();
					ExprList argumentList = new ExprList();
					if (token.kind == TokenKind.RPAREN) {
						acceptIt();
					} else {
						argumentList = parseArgumentList();
						accept(TokenKind.RPAREN);
					}
					endPosn(regularExprPosn);
					expression = new CallExpr(reference, argumentList, regularExprPosn);
				} else {
					endPosn(regularExprPosn);
					expression = new RefExpr(reference, regularExprPosn);
				}
				break;
			}
				else if (token.kind == TokenKind.THIS) { // having a reference that starts with this. ===> this.id ||
					accept(TokenKind.THIS);
					// need to send this to parseReference
					Reference thisReference = new ThisRef(regularExprPosn);
					if (token.kind == TokenKind.DOT) { // having a reference of typeID.something else here
						while (token.kind == TokenKind.DOT) { // ref.ref[]
							acceptIt();
							Identifier identifier = new Identifier(token);
							accept(TokenKind.ID);
							endPosn(regularExprPosn);
							thisReference = new QualRef(thisReference, identifier, regularExprPosn);
						}
						if (token.kind == TokenKind.LSQUAREBRACKET) {
							acceptIt();
							Expression referenceExpression = parseExpression();
							accept(TokenKind.RSQUAREBRACKET);
							endPosn(regularExprPosn);
							expression = new IxExpr(thisReference, referenceExpression, regularExprPosn);
						} else if (token.kind == TokenKind.LPAREN) { // reference ( argumentList? )
							acceptIt();
							ExprList argumentList = new ExprList();
							if (token.kind == TokenKind.RPAREN) {
								acceptIt();
							} else {
								argumentList = parseArgumentList();
								accept(TokenKind.RPAREN);
							}
							endPosn(regularExprPosn);
							expression = new CallExpr(thisReference, argumentList, regularExprPosn);
						} else {
							endPosn(regularExprPosn);
							expression = new RefExpr(thisReference, regularExprPosn);
						}
					}
					else if (token.kind == TokenKind.LPAREN) {
						acceptIt();
						ExprList argumentList = new ExprList();
						if (token.kind == TokenKind.RPAREN) {
							acceptIt();
							accept(TokenKind.SEMICOLON);
						} else {
							argumentList = parseArgumentList();
							accept(TokenKind.RPAREN);
							accept(TokenKind.SEMICOLON);
						}
						endPosn(regularExprPosn);
						expression = new CallExpr(thisReference, argumentList, regularExprPosn);
					} else if (token.kind == TokenKind.LSQUAREBRACKET) {
						acceptIt();
						Expression referenceExpression = parseExpression();
						accept(TokenKind.RSQUAREBRACKET);
						endPosn(regularExprPosn);
						expression = new IxExpr(thisReference, referenceExpression, regularExprPosn);
					}
					else {
						endPosn(regularExprPosn);
						expression = new RefExpr(thisReference, regularExprPosn);
					}
					break;
				}
			default:
				parseError("We found an unaccepted token: '" + token.kind + "'");
		}
		return expression;
	}
	
	public void beginPosn(SourcePosition pos) {
		pos.start = token.posn.start;
	}
	
	public void endPosn(SourcePosition pos) {
		pos.end = token.posn.end;
	}
	
	
	/**
	 * accept current token and advance to next token
	 */
	private void acceptIt() throws SyntaxError {
		accept(token.kind);
	}

	/**
	 * verify that current token in input matches expected token and advance to next
	 * token
	 * 
	 * @param expectedToken
	 * @throws SyntaxError if match fails
	 */
	private void accept(TokenKind expectedTokenKind) throws SyntaxError {
		if (token.kind == expectedTokenKind) {
			if (trace)
				pTrace();
			token = scanner.scan();
		} else
			parseError("expecting '" + expectedTokenKind + "' but found '" + token.kind + "'");
	}

	/**
	 * report parse error and unwind call stack to start of parse
	 * 
	 * @param e string with error detail
	 * @throws SyntaxError
	 */
	private void parseError(String e) throws SyntaxError {
		reporter.reportError("Parse error: " + e);
		throw new SyntaxError();
	}

	// show parse stack whenever terminal is accepted
	private void pTrace() {
		StackTraceElement[] stl = Thread.currentThread().getStackTrace();
		for (int i = stl.length - 1; i > 0; i--) {
			if (stl[i].toString().contains("parse"))
				System.out.println(stl[i]);
		}
		System.out.println("accepting: " + token.kind + " (\"" + token.spelling + "\")");
		System.out.println();
	}

}
