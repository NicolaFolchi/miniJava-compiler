/**
 * miniJava Contextual Analyser classes
 * @author Nicola Folchi
 */
package miniJava.ContextualAnalyser;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenKind;

public class TypeChecking implements Visitor<TypeDenoter, TypeDenoter> {
	public Package packageAST;
	public ErrorReporter errorReporter;
	private TypeDenoter returnType;
	private ClassDecl thisDecl;
	
	public TypeChecking(Package packageAST, ErrorReporter errorReporter) {
		this.packageAST = packageAST;
		this.errorReporter = errorReporter;
	} 
	
	public void beginCheck() {
		packageAST.visit(this, null); 
	}

	@Override
	public TypeDenoter visitPackage(Package prog, TypeDenoter arg) {
		ClassDeclList classes = prog.classDeclList;
		for (ClassDecl singleClass : classes) { 
			singleClass.visit(this, null);
		}
		return null;
	}

	@Override
	public TypeDenoter visitClassDecl(ClassDecl cd, TypeDenoter arg) {
		for (FieldDecl fieldDecl : cd.fieldDeclList) {
			fieldDecl.visit(this, null);
		}
		for (MethodDecl methodDecl : cd.methodDeclList) {
			methodDecl.visit(this, null);
		}
		return null;
	}

	@Override
	public TypeDenoter visitFieldDecl(FieldDecl fd, TypeDenoter arg) {
		return fd.type.visit(this, null);
	}

	@Override
	public TypeDenoter visitMethodDecl(MethodDecl md, TypeDenoter arg) {
		TypeDenoter methodType = md.type.visit(this, null);
		for (ParameterDecl parameter : md.parameterDeclList) {
			parameter.type.visit(this, null);
		}
		for (Statement statement : md.statementList) {
			TypeDenoter statementType = statement.visit(this, null);
			if (statement instanceof ReturnStmt) {
//				System.out.println(statementType.typeKind);
				if (statementType.typeKind != methodType.typeKind) {
					logError("*** line " + statement.posn.start + ": TypeChecking Error - MethodDecl: Return statement type \"" + statementType.typeKind + "\" does not match method return type " + returnType.typeKind);

				}
			}
		}
		int methodStatementsSize = md.statementList.size()-1;
//		if (!(md.statementList.get(methodStatementsSize) instanceof ReturnStmt)) {
//			if (methodType.typeKind != TypeKind.VOID) {
//				logError("*** line " + md.statementList.get(methodStatementsSize).posn.start + ": TypeChecking Error - MethodDecl: Last statement on method should be a return statement");
//			}
//		}
		return methodType;
	}

	@Override
	public TypeDenoter visitParameterDecl(ParameterDecl pd, TypeDenoter arg) {
		// never reaching this
		return null;
	}

	@Override
	public TypeDenoter visitVarDecl(VarDecl decl, TypeDenoter arg) {
		return decl.type.visit(this, null);
	}

	@Override
	public TypeDenoter visitBaseType(BaseType type, TypeDenoter arg) {
		return type;
	}

	@Override
	public TypeDenoter visitClassType(ClassType type, TypeDenoter arg) {
		return type;
	}

	@Override
	public TypeDenoter visitArrayType(ArrayType type, TypeDenoter arg) {
		TypeDenoter arrayType = type.eltType.visit(this, null);
		return new ArrayType(arrayType, null);
	}

	@Override
	public TypeDenoter visitBlockStmt(BlockStmt stmt, TypeDenoter arg) {
		for (Statement statement : stmt.sl) {
			statement.visit(this, null);
		}
		return null;
	}

	@Override
	public TypeDenoter visitVardeclStmt(VarDeclStmt stmt, TypeDenoter arg) { 
		TypeDenoter varType = stmt.varDecl.visit(this, null);
		TypeDenoter exprType = stmt.initExp.visit(this, null);
		if (varType == null) {
			varType = new BaseType(TypeKind.ERROR, stmt.posn);
		}
		if (exprType == null) {
			exprType = new BaseType(TypeKind.ERROR, stmt.posn);
		}

		if (stmt.initExp instanceof RefExpr) {
			RefExpr refExpr = (RefExpr) stmt.initExp;
			if (refExpr.ref.decl instanceof ClassDecl || refExpr.ref.decl instanceof MethodDecl) {
				logError("*** line " + stmt.initExp.posn.start + ": TypeChecking error - VarDeclStmt: Invalid assignment for a class/method");
			}
		}

		if (!checkEqualClassOrArray(varType, exprType)) {
			if (varType instanceof ClassType && exprType instanceof ClassType) {
				System.out.println("got here22");
				if (stmt.initExp instanceof NewObjectExpr) {
					Identifier className1 = ((ClassType) varType).className;
					Identifier className2 = ((ClassType) exprType).className;
					if (className1.decl == null || className2.decl == null) {
						logError("*** line " + stmt.posn.start
								+ ": TypeChecking error - VarDeclStmt: Invalid assignment of ClassType");
					} else if (!className1.spelling.equals(className2.spelling)) {
						logError("*** line " + stmt.initExp.posn.start + ": TypeChecking error - VarDeclStmt: Classes names do not match");
					}
				}
			} else if (exprType.typeKind == TypeKind.ERROR) {
				if (stmt.initExp instanceof IxExpr) {
					if (!(((IxExpr) stmt.initExp).ref instanceof QualRef)) {
						if (varType.typeKind != TypeKind.CLASS && varType.typeKind != TypeKind.ARRAY) {
							logError("*** line " + stmt.initExp.posn.start + ": TypeChecking error - VarDeclStmt: Invalid assignment INT type from an array index");
						}
					}
				}

			} else if (exprType.typeKind != TypeKind.NULL) {
				if (varType.typeKind != exprType.typeKind) {
					logError("*** line " + stmt.posn.start + ": TypeChecking error - VarDeclStmt: Types do not match");
					return new BaseType(TypeKind.ERROR, stmt.posn);
				}
			}
		}
		return varType;
	}

	@Override
	public TypeDenoter visitAssignStmt(AssignStmt stmt, TypeDenoter arg) {
		TypeDenoter varType = stmt.ref.visit(this, null);
		TypeDenoter exprType = stmt.val.visit(this, null);

//		if (exprType ) { //  <= DON'T THINK THIS IS CORREC
//		System.out.println(varType.typeKind);
			if (exprType.typeKind != TypeKind.NULL) {
				if (!isValid(varType) || !isValid(exprType)) {
					logError("*** line " + stmt.val.posn.start + ": TypeChecking error - AssignStmt: Assign expression(s) cannot be null");
					return new BaseType(TypeKind.ERROR, null);
				}
				if (!varType.typeKind.equals(exprType.typeKind)) {
					logError("*** line " + stmt.val.posn.start + ": TypeChecking error - AssignStmt: Variable assignment type and variable type do not match");
					return new BaseType(TypeKind.ERROR, null);
				}
			if (stmt.ref instanceof QualRef) {
				QualRef qualRef = (QualRef) stmt.ref;
				if (qualRef.ref.decl != null) {
					if (qualRef.ref.decl.type.typeKind == TypeKind.ARRAY && qualRef.id.spelling.equals("length")) {
						logError("*** line " + stmt.val.posn.start + ": TypeChecking error - AssignStmt: Assignment to attribute \"length\" of array is invalid.");
						return new BaseType(TypeKind.ERROR, stmt.posn);
					}
				}
			}
		}
		return null;
	}

	@Override
	public TypeDenoter visitIxAssignStmt(IxAssignStmt stmt, TypeDenoter arg) {
		TypeDenoter varType = stmt.ref.visit(this, null);
		TypeDenoter exprType = stmt.exp.visit(this, null);
		if (exprType.typeKind != TypeKind.NULL) {
			System.out.println("IxAssignment: " + varType + " " + exprType);
			if(!isValid(varType) || !isValid(exprType)){
				logError("*** line " + stmt.posn.start + ": TypeChecking error - IxAssignStmt: Assign expression(s) cannot be null");
				return new BaseType(TypeKind.ERROR, null);
			}
//			if ((varType.typeKind == TypeKind.ARRAY || exprType.typeKind == TypeKind.ARRAY)
//					&& (varType.typeKind == TypeKind.ARRAY || exprType.typeKind == TypeKind.ARRAY)) {
//
//			} 
			if(!checkEqualClassOrArray(varType, exprType)) {
				System.out.println("got here");
				return new BaseType(TypeKind.ERROR, null);
			}else if (!varType.typeKind.equals(exprType.typeKind)) {
				logError("*** line " + stmt.posn.start + ": TypeChecking error - IxAssignStmt: Variable assignment type and variable type do not match");
			}
		}
		return null;
	}

	@Override
	public TypeDenoter visitCallStmt(CallStmt stmt, TypeDenoter arg) {
		MethodDecl calledMethod = (MethodDecl) stmt.methodRef.decl;
		if (!(stmt.methodRef.decl instanceof MethodDecl)) {
			logError("*** line " + stmt.posn.start + ": TypeChecking error - CallStmt: Call statement is not of method type");
		} else {
			if (stmt.argList.size() != calledMethod.parameterDeclList.size()) {
				logError("*** line " + stmt.posn.start + ": TypeChecking error - CallStmt: Call statement arguments do not match in size");
			} else {
				if (calledMethod.name.equals("println")) {
					for (int i = 0; i < stmt.argList.size(); i++) {
						TypeDenoter argumentType = stmt.argList.get(i).visit(this, null);
						System.out.println(argumentType.typeKind + " " + calledMethod.parameterDeclList.get(0).type.typeKind);
						if (argumentType.typeKind != TypeKind.ERROR) { // TODO CHECK IF THIS IS GOOD ---------------------------------
							if (!argumentType.typeKind.equals(calledMethod.parameterDeclList.get(0).type.typeKind)) {
								logError("*** line " + stmt.posn.start + ": TypeChecking error - CallStmt: Call statement arguments are not the correct type..");
							}
						}
					}
				} else {
					for (int i = 0; i < stmt.argList.size(); i++) {
						TypeDenoter argumentType = stmt.argList.get(i).visit(this, null);
						TypeDenoter methodArgumentType = calledMethod.parameterDeclList.get(i).visit(this, null);
						System.out.println(methodArgumentType);
						if (!argumentType.typeKind.equals(methodArgumentType.typeKind)) {
							logError("*** line " + stmt.posn.start + ": TypeChecking error - CallStmt: Call statement arguments are not the correct type");
						}
					}
				}
			}
		}
		return null;
	}

	@Override
	public TypeDenoter visitReturnStmt(ReturnStmt stmt, TypeDenoter arg) {
		if (stmt.returnExpr != null) {
			returnType = stmt.returnExpr.visit(this, null);
			return returnType;
		} else {
			return new BaseType(TypeKind.VOID, stmt.posn);
		}
	}

	@Override
	public TypeDenoter visitIfStmt(IfStmt stmt, TypeDenoter arg) {
		TypeDenoter conditionType = stmt.cond.visit(this, null);
		if (conditionType.typeKind != TypeKind.BOOLEAN) {
			logError("*** line " + stmt.cond.posn.start + ": TypeChecking error - IfStmt: condition is not of boolean type");
		}
		TypeDenoter thenType = stmt.thenStmt.visit(this, null);
		if (stmt.elseStmt != null) {
			TypeDenoter elseType = stmt.elseStmt.visit(this, null);
		}
		return null;
	}

	@Override
	public TypeDenoter visitWhileStmt(WhileStmt stmt, TypeDenoter arg) {
		TypeDenoter conditionType = stmt.cond.visit(this, null);
		if (conditionType.typeKind != TypeKind.BOOLEAN) {
			logError("*** line " + stmt.cond.posn.start + ": TypeChecking error - WhileStmt: condition is not of boolean type");
		}
		TypeDenoter thenType = stmt.body.visit(this, null);
		return null;
	}

	@Override
	public TypeDenoter visitUnaryExpr(UnaryExpr expr, TypeDenoter arg) {
		String unaryOperator = expr.operator.spelling;
		TypeDenoter expressionType = expr.expr.visit(this, null);
		if (unaryOperator.equals("!")) {
			if (expressionType.typeKind == TypeKind.BOOLEAN) {
				return new BaseType(TypeKind.BOOLEAN, null);
			} else {
				logError("*** line " + expr.posn.start + ": TypeChecking error - UnaryExpr: condition is not of boolean type");
				return new BaseType(TypeKind.ERROR, null);
			}
		}
		else if (unaryOperator.equals("-")) {
			if (expressionType.typeKind == TypeKind.INT) {
				return new BaseType(TypeKind.INT, null);
			} else {
				logError("*** line " + expr.posn.start + ": TypeChecking error - UnaryExpr: condition is not of int type");
				return new BaseType(TypeKind.ERROR, null);
			}
		} else {
			logError("*** line " + expr.posn.start + ": TypeChecking error - UnaryExpr: operator is not unary");
			return new BaseType(TypeKind.ERROR, null);
		}
//		return null;
	}

	@Override
	public TypeDenoter visitBinaryExpr(BinaryExpr expr, TypeDenoter arg) {
		TypeDenoter leftExprType = expr.left.visit(this, null);
		TypeDenoter rightExprType = expr.right.visit(this, null);
		String binaryOperator = expr.operator.spelling;
		if (binaryOperator.equals("+") || binaryOperator.equals("-") || binaryOperator.equals("*") || binaryOperator.equals("/")) {
			System.out.println("Arithmetic types: " + leftExprType + " " + rightExprType);

			if (leftExprType.typeKind == TypeKind.INT && rightExprType.typeKind == TypeKind.INT) {
				return new BaseType(TypeKind.INT, null);
			}
			if (leftExprType.typeKind == TypeKind.ERROR && rightExprType.typeKind == TypeKind.INT) {
				return new BaseType(TypeKind.INT, null);
			}
			if (leftExprType.typeKind == TypeKind.INT && rightExprType.typeKind == TypeKind.ERROR) {
				return new BaseType(TypeKind.INT, null);
			} 
			if (leftExprType.typeKind == TypeKind.ERROR && rightExprType.typeKind == TypeKind.ERROR) {
				return new BaseType(TypeKind.BOOLEAN, null);
			} else {
				logError("*** line " + expr.posn.start + ": TypeChecking error - BinaryExpr (Arithmetic): Expressions type do not match");
				return new BaseType(TypeKind.ERROR, null);
			}
		} else if (binaryOperator.equals("<=") || binaryOperator.equals("<") || binaryOperator.equals(">") || binaryOperator.equals(">=")) {
			if (leftExprType.typeKind == TypeKind.INT && rightExprType.typeKind == TypeKind.INT) {
				return new BaseType(TypeKind.BOOLEAN, null);
			} 
			if (leftExprType.typeKind == TypeKind.ERROR && rightExprType.typeKind == TypeKind.INT) {
				return new BaseType(TypeKind.BOOLEAN, null);
			}
			if (leftExprType.typeKind == TypeKind.INT && rightExprType.typeKind == TypeKind.ERROR) {
				return new BaseType(TypeKind.BOOLEAN, null);
			} 
			if (leftExprType.typeKind == TypeKind.ERROR && rightExprType.typeKind == TypeKind.ERROR) {
				return new BaseType(TypeKind.BOOLEAN, null);
			} else {
				logError("*** line " + expr.posn.start + ": TypeChecking error - BinaryExpr (</>): Expressions type do not match");
				return new BaseType(TypeKind.ERROR, null);
			}
		} else if (binaryOperator.equals("==") || binaryOperator.equals("!=")) {
			if (!isValid(leftExprType) || !isValid(rightExprType) || !checkEqualClassOrArray(leftExprType, rightExprType)) {
				if (leftExprType instanceof ClassType && rightExprType instanceof ClassType) {
					logError("*** line " + expr.posn.start + ": TypeChecking error - BinaryExpr (Comparison): Expression type do not match");
				} else {
					logError("*** line " + expr.posn.start + ": TypeChecking error - BinaryExpr (Comparison): Expressions type do not match");
				}
                return new BaseType(TypeKind.ERROR, expr.posn);
            }
            return new BaseType(TypeKind.BOOLEAN, expr.posn);
		} else if (binaryOperator.equals("||") || binaryOperator.equals("&&")) {
			if (leftExprType.typeKind == TypeKind.BOOLEAN && rightExprType.typeKind == TypeKind.BOOLEAN) {
				return new BaseType(TypeKind.BOOLEAN, null);
			} else {
				logError("*** line " + expr.posn.start + ": TypeChecking error - BinaryExpr (Conditions): Expressions type do not match");
				return new BaseType(TypeKind.ERROR, null);
			}
		}
		return null;
	}

	@Override
	public TypeDenoter visitRefExpr(RefExpr expr, TypeDenoter arg) {
		return expr.ref.visit(this, null);
	}

	@Override
	public TypeDenoter visitIxExpr(IxExpr expr, TypeDenoter arg) { // we visit the reference since it is the reference the one that defines the type
		return expr.ref.visit(this, null);
	}

	@Override
	public TypeDenoter visitCallExpr(CallExpr expr, TypeDenoter arg) {
		MethodDecl calledMethod = (MethodDecl) expr.functionRef.decl;
		TypeDenoter callMethodType = null;
		if (!(expr.functionRef.decl instanceof MethodDecl)) {
			logError("*** line " + expr.posn.start + ": TypeChecking error - CallExpr: Call statement is not of method type");
			return new BaseType(TypeKind.ERROR, null);
		} else {
			if (expr.argList.size() != calledMethod.parameterDeclList.size()) {
				logError("*** line " + expr.posn.start + ": TypeChecking error - CallExpr: Call statement arguments do not match in size");
				return new BaseType(TypeKind.ERROR, null);
			} else {
				for (int i = 0; i < expr.argList.size(); i++) {
					TypeDenoter argumentType = expr.argList.get(i).visit(this, null);
					TypeDenoter methodArgumentType = calledMethod.parameterDeclList.get(0).type;
					System.out.println(calledMethod.parameterDeclList.get(0).type);
					if(!isValid(argumentType) || !isValid(methodArgumentType)){
						logError("*** line " + expr.posn.start + ": TypeChecking error - CallExpr: Call statement argument(s) cannot be null");
						return new BaseType(TypeKind.ERROR, null);
					}
					if (!argumentType.typeKind.equals(methodArgumentType.typeKind)) {
						logError("*** line " + expr.posn.start + ": TypeChecking error - CallExpr: Call statement arguments are not the correct type");
						return new BaseType(TypeKind.ERROR, null);
					}
				}
				callMethodType = calledMethod.type;
			}
		}
		return callMethodType;
	}

	@Override
	public TypeDenoter visitLiteralExpr(LiteralExpr expr, TypeDenoter arg) { 
		return expr.lit.visit(this, null);
	}

	@Override
	public TypeDenoter visitNewObjectExpr(NewObjectExpr expr, TypeDenoter arg) {
		return expr.classtype.visit(this, null);
	}

	@Override
	public TypeDenoter visitNewArrayExpr(NewArrayExpr expr, TypeDenoter arg) { 
		return new ArrayType(expr.eltType, null);
	}

	@Override
	public TypeDenoter visitThisRef(ThisRef ref, TypeDenoter arg) {
		if (ref.decl != null) {
			thisDecl = (ClassDecl) ref.decl;
			return new ClassType(new Identifier(new Token(TokenKind.ID, thisDecl.name, ref.posn)), ref.posn);
		} else if (ref.decl == null) {
			return new BaseType(TypeKind.ERROR, ref.posn);
		}
//		else if (ref.decl.type == null) {
//			return new BaseType(TypeKind.THISCASE, ref.posn);
//		}
		return ref.decl.type;
	}

	@Override
	public TypeDenoter visitIdRef(IdRef ref, TypeDenoter arg) {
		System.out.println(ref.id.spelling);
		System.out.println("dsd " + ref.decl);

		if (ref.decl == null ) {
			return new BaseType(TypeKind.ERROR, new SourcePosition());
		}
//		}
////		if (ref.id.decl.type == null) {
////			logError("*** line " + ref.posn.start + ": TypeChecking error - IdRef: id can't be null here");
////			return null;
//		} else {			
//			return ref.id.decl.type;
//		}
		return ref.decl.type;
	}

	@Override
	public TypeDenoter visitQRef(QualRef ref, TypeDenoter arg) {
		ref.ref.visit(this, null);
		System.out.println(ref.ref.decl);
		if (ref.ref instanceof ThisRef && thisDecl != null) { 
			for (FieldDecl fieldDecl : thisDecl.fieldDeclList) {
				if (fieldDecl.name.equals(ref.id.spelling)) {
					return fieldDecl.type;
				}
			}
		}
		if (ref.id.decl == null ) { // TODO need to fix when var decl in thix.var equals null, cause then I return an error that may exist in the left hand side of an assignment or something like that
			return new BaseType(TypeKind.ERROR, new SourcePosition());
		}
		return ref.id.decl.type;
	}

	@Override
	public TypeDenoter visitIdentifier(Identifier id, TypeDenoter arg) {
		System.out.println(id.spelling);
//		if (id.type == null ) {
//			return new BaseType(TypeKind.ERROR, new SourcePosition());
//		}
		return id.decl.type;
	}

	@Override
	public TypeDenoter visitOperator(Operator op, TypeDenoter arg) {
		return null;
	}

	@Override
	public TypeDenoter visitIntLiteral(IntLiteral num, TypeDenoter arg) {
		return new BaseType(TypeKind.INT, null);
	}

	@Override
	public TypeDenoter visitBooleanLiteral(BooleanLiteral bool, TypeDenoter arg) {
		return new BaseType(TypeKind.BOOLEAN, null);
	}

	@Override
	public TypeDenoter visitNullLiteral(NullLiteral nullLiteral, TypeDenoter arg) { 
		return new BaseType(TypeKind.NULL, null);
	}
	
	public void logError(String err) {
		errorReporter.reportError(err);
	}
	
	private boolean isValid(TypeDenoter tp) {
		if (tp == null) {
			return false;
		} else if (tp.typeKind == TypeKind.UNSUPPORTED) {
			return false;
		} else if (tp.typeKind == TypeKind.ERROR) {
			return true;
		}
		return true;
	}
	private boolean checkEqualClassOrArray(TypeDenoter tp1, TypeDenoter tp2) {
		if (tp1 instanceof ArrayType || tp2 instanceof ArrayType) {
        	// arrays can be assigned and compared to null
        	if (tp1.typeKind == TypeKind.NULL || tp2.typeKind == TypeKind.NULL) {
        		return true;
        	}
        	
            if (!(tp1 instanceof ArrayType) || !(tp2 instanceof ArrayType)) {
                return false;
            }
            return checkEqualClassOrArray(((ArrayType) tp1).eltType, ((ArrayType) tp2).eltType);
            
        } else if (tp1 instanceof ClassType || tp2 instanceof ClassType) {            
            // Class objects can be assigned to null
            if (tp1.typeKind == TypeKind.CLASS && tp2.typeKind == TypeKind.NULL) {
                return true;
            } else if (tp1.typeKind == TypeKind.NULL && tp2.typeKind == TypeKind.CLASS) {
                return true;
            } else if (!(tp1 instanceof ClassType) || !(tp2 instanceof ClassType)) {
                return false;
            }

            Identifier className1 = ((ClassType) tp1).className;
            Identifier className2 = ((ClassType) tp2).className;
            
            if (className1.decl != null && className2.decl != null) {
            	if (className1.decl.type == null || className2.decl.type == null) {
            		return false;
            	}
            	else if (className1.decl.type.typeKind == TypeKind.UNSUPPORTED || className2.decl.type.typeKind == TypeKind.UNSUPPORTED) {
    				return false;
    			}
            }
            return className1.spelling.equals(className2.spelling); // name equivalence
        }
		return tp1.typeKind == tp2.typeKind;
	}
	
	public void isError(TypeDenoter tp) {
		if (tp.typeKind == TypeKind.ERROR) {
//			logError("TypeChecking: variable with type ERROR");
		}
	}
	
	public void isUnsupported(TypeDenoter tp) {
		if (tp.typeKind == TypeKind.UNSUPPORTED) {
			logError("TypeChecking: variable with type UNSUPPORTED");
		}
	}

}