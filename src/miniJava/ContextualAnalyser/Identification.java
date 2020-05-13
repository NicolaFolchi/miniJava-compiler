/**
 * miniJava Contextual Analyser classes
 * @author Nicola Folchi
 */
package miniJava.ContextualAnalyser;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

public class Identification implements Visitor<Object, Object> {
	 
	public Package packageAST;
	public IdTable idTable;
	public HashMap<ClassDecl, Integer> classContents;
	public ClassDecl currentClass;
	public ErrorReporter errorReporter;
	public boolean isStatic;
	public HashSet<String> variables;
	public String currentVariableDecl;
	private boolean existsMain = false;
	private int mainCounter = 0;
	public HashMap<String, Reference> arrayDecls;
	private Declaration obj22;
 
	public Identification(Package packageAST, ErrorReporter err) {
		this.packageAST = packageAST;
		idTable = new IdTable();
		errorReporter = err;
		arrayDecls = new HashMap<String, Reference>();
	}

	public Package beginCheck() {
		packageAST.visit(this, null);
		idTable.printStack();
		System.out.println(obj22);
		arrayDecls.forEach((k, v) -> System.out.println(k + " : " + (v)));
		return packageAST;
	}

	@Override
	public Object visitPackage(Package prog, Object obj) {
		ClassDeclList classes = prog.classDeclList;
		idTable.openScope();

		// checking validity of ALL classes first 
		for (ClassDecl singleClass : classes) {
			if (idTable.enter(singleClass.name, singleClass) == null) {
				// report error cause it is duplicate
				logError("*** line " + singleClass.posn.start + ": Identification error - ClassDecl: class already exists on idTable");
			}
		}
		for (ClassDecl singleClass : classes) {
			idTable.openScope(); // level 2 - members
			for (FieldDecl fieldDecl : singleClass.fieldDeclList) {
				if (idTable.enter(fieldDecl.name, fieldDecl) == null) {
					logError("*** line " + fieldDecl.posn.start + ": Identification error - FieldDecl: field already exists on idTable");
				}
				fieldDecl.classDecl = singleClass; // Associating every field with its class
				fieldDecl.visit(this, null);
			}
			for (MethodDecl methodDecl : singleClass.methodDeclList) { 
				if (isMain(methodDecl)) {
					if (mainCounter > 1) {
						logError("*** line " + methodDecl.posn.start + ": Identification error - Cannot start program: multiple main methods found");
					}
				}
				if (idTable.enter(methodDecl.name, methodDecl) == null) {
					logError("*** line " + methodDecl.posn.start + ": Identification error - MethodDecl: method already exists on idTable");
				}
				methodDecl.classDecl = singleClass; // Associating every method with its class
			}
		}
		if (existsMain == false) {
			logError("*** line " + prog.posn.end + ": Identification error - Package: Method main is not defined in package");
		}
		for (ClassDecl singleClass : classes) {
			currentClass = singleClass;
			singleClass.visit(this, null);
		}
		idTable.closeScope();
		return null;
	}

	@Override
	public Object visitClassDecl(ClassDecl cd, Object obj) {
		for (MethodDecl singleMethod : cd.methodDeclList) {
			singleMethod.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitFieldDecl(FieldDecl fd, Object obj) {
		fd.type.visit(this, null);
		return null;
	}

	@Override
	public Object visitMethodDecl(MethodDecl md, Object obj) {
		md.type.visit(this, null);
		idTable.openScope();
		isStatic = md.isStatic;
		variables = new HashSet<>(); 
		for (ParameterDecl parameterDecl : md.parameterDeclList) {
			parameterDecl.visit(this, null);
		}
		for (Statement statement : md.statementList) {
			statement.visit(this, null);
		}
		idTable.closeScope();
		return null;
	}

	@Override
	public Object visitParameterDecl(ParameterDecl pd, Object obj) {
		pd.type.visit(this, null);
		if (idTable.enter(pd.name, pd) == null) {
			logError("*** line " + pd.posn.start + ": Identification error - ParameterDecl: parameter already exists on idTable");
		}
		if (variables.contains(pd.name)) {
			logError("*** line " + pd.posn.start + ": Identification error - ParameterDecl: parameter name is being used by another variable");
		} else {
			variables.add(pd.name);
		}
		return null;
	}

	@Override
	public Object visitVarDecl(VarDecl decl, Object obj) {
		decl.type.visit(this, null);
		if (idTable.enter(decl.name, decl) == null) {
			logError("*** line " + decl.posn.start + ": Identification error - VarDecl: variable already exists on idTable");
		}
		if (variables.contains(decl.name)) {
			logError("*** line " + decl.posn.start + ": Identification error - VarDecl: variable name is being used by another variable");
		} else {
			variables.add(decl.name);
		}
		return null;
	}

	@Override
	public Object visitBaseType(BaseType type, Object obj) {
		return null;
	}

	@Override
	public Object visitClassType(ClassType type, Object obj) {
		Declaration classDecl =  idTable.retrieveClass(type.className.spelling);
		if (classDecl == null) {
			logError("*** line " + type.posn.end + ": Identification error - ClassDecl: " + type.className.spelling + " not found");
		} else {
			type.className.decl = classDecl;
		}
		return null;
	}

	@Override
	public Object visitArrayType(ArrayType type, Object obj) {
		type.eltType.visit(this, null);
		return null;
	}

	@Override
	public Object visitBlockStmt(BlockStmt stmt, Object obj) { 
		idTable.openScope();
		for (Statement singleStatement : stmt.sl) {
			singleStatement.visit(this, null);
		}
		HashMap<String, Declaration> currentTable = idTable.getCurrentIdTable();
		for (String key : currentTable.keySet()) {
			if (variables.contains(key)) {
				variables.remove(key);
			}
		}
		idTable.closeScope();
		return null;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, Object obj) {
		currentVariableDecl = stmt.varDecl.name;
		stmt.initExp.visit(this, null);
		currentVariableDecl = "";
		stmt.varDecl.visit(this, null);
		return null;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, Object obj) {
		if (stmt.ref instanceof ThisRef) {
			logError("*** line " + stmt.posn.start + ": Identification error - AssignStmt: This can't be used in the left hand side of an assignment");
			return null;
		} else {
			stmt.ref.visit(this, null);
			if (stmt.ref.decl instanceof FieldDecl) {
				FieldDecl fieldDecl = (FieldDecl)stmt.ref.decl;
	        	if (fieldDecl.isStatic) {
	        		stmt.ref.decl.isStatic = true;
	        	}
			}
		}
		stmt.val.visit(this, null);
		return null;
	}

	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, Object obj) {
		if (stmt.ref instanceof ThisRef) {
			logError("*** line " + stmt.posn.start + ": Identification error - IxAssignStmt: This can't be used in the left hand side of an assignment");
			return null;
		} else {
			stmt.ref.visit(this, null);
		}
		stmt.ix.visit(this, null);
		stmt.exp.visit(this, null);
		return null;
	}

	@Override
	public Object visitCallStmt(CallStmt stmt, Object obj) {
		stmt.methodRef.visit(this, null);
		if (!(stmt.methodRef.decl instanceof MethodDecl)) {
			logError("*** line " + stmt.posn.end + ": Identification error - CallStmt: this can't be a method");
			return null;
		}
		MethodDecl methodDecl = (MethodDecl) stmt.methodRef.decl;
		
		if (methodDecl.parameterDeclList.size() != stmt.argList.size()) {
			logError("*** line " + stmt.posn.start + ": Identification error - CallStmt: Number of arguments doesn't match with the number of parameters");
            return null;
        }

        for (Expression expression : stmt.argList) {
            expression.visit(this, null);
        }
		return null;
	}

	@Override
	public Object visitReturnStmt(ReturnStmt stmt, Object obj) {
		if (stmt.returnExpr != null) {
			stmt.returnExpr.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitIfStmt(IfStmt stmt, Object obj) {
		stmt.cond.visit(this, null);
        if (stmt.thenStmt instanceof VarDeclStmt) { 
            logError("*** line " + stmt.thenStmt.posn.start + ": Identification error - IfStmt: A variable declaration cannot be the only statement in a branch of an if statement");
        } else {
            stmt.thenStmt.visit(this, null);
        }
        if (stmt.elseStmt != null) {
            if (stmt.elseStmt instanceof VarDeclStmt) {
                logError("*** line " + stmt.elseStmt.posn.start + ": Identification error - IfStmt: A variable declaration cannot be the only statement in a branch of an if statement");
            } else {
                stmt.elseStmt.visit(this, null);
            }
        }
		return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, Object obj) {
		stmt.cond.visit(this, null);
        if (stmt.body != null) {
            if (stmt.body instanceof VarDeclStmt) {
                logError("*** line " + stmt.body.posn.start + ": Identification error - WhileStmt: A variable declaration cannot be the only statement in the body of a while loop");
            } else {
                stmt.body.visit(this, null);
            }
        }
		return null;
	}

	@Override
	public Object visitUnaryExpr(UnaryExpr expr, Object obj) {
        expr.expr.visit(this, null);
		return null;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr expr, Object obj) {
		expr.left.visit(this, null);
        expr.right.visit(this, null);
        return null;
	}

	@Override
	public Object visitRefExpr(RefExpr expr, Object obj) {
		 expr.ref.visit(this, null);
	        if (expr.ref instanceof IdRef && (expr.ref.decl instanceof ClassDecl || expr.ref.decl instanceof MethodDecl)) {
	            logError("*** line " + expr.ref.posn.start + ": Identification error - RefExpr: Cannot reference class/method name only in a RefExpr");
	        }
		return null;
	}

	@Override
	public Object visitIxExpr(IxExpr expr, Object obj) { // x[e]
		expr.ixExpr.visit(this, null);
		if (expr.ref instanceof IdRef && (expr.ref.decl instanceof ClassDecl || expr.ref.decl instanceof MethodDecl)) {
			logError("*** line " + expr.ref.posn.start + ": Identification error - IxExpr: Cannot reference class/method name only in a RefExpr");
		}
		 return null;
	}

	@Override
	public Object visitCallExpr(CallExpr expr, Object obj) {
		expr.functionRef.visit(this, new BaseType(TypeKind.INT, null)); // creating a BaseType object to make difference of cases on visitIdentifier
        MethodDecl methodDecl = (MethodDecl) (expr.functionRef.decl);
        if (methodDecl.parameterDeclList.size() != expr.argList.size()) {
            logError("*** line " + expr.posn.start + ": Identification error - CallExpr: number of arguments does not agree with number of parameters");
            return null;
        }
//        System.out.println(methodDecl);
        for (Expression expression : expr.argList) {
            expression.visit(this, null);
        }
		return null;
	}

	@Override
	public Object visitLiteralExpr(LiteralExpr expr, Object obj) {
        expr.lit.visit(this, null);
		return null;
	}

	@Override
	public Object visitNewObjectExpr(NewObjectExpr expr, Object obj) {
        expr.classtype.className.visit(this, null);
		return null;
	}

	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, Object obj) {
		expr.eltType.visit(this, null);
        expr.sizeExpr.visit(this, null);
		return null;
	}

	@Override
	public Object visitThisRef(ThisRef ref, Object obj) {
		ref.decl = currentClass;
		if (isStatic) {
            logError("*** line " + ref.posn.end + ": Identification error - ThisRef: \"this\" can't be used within a static context.");
        }
        ref.isStatic = false;
		return null;
	}

	@Override
	public Object visitIdRef(IdRef ref, Object obj) {
		ref.id.visit(this, obj); //-HERE
		ref.isStatic = ref.id.isStatic;
		ref.decl = ref.id.decl;
	
		if (ref.decl != null && ref.decl.type != null) {
			if (ref.decl.type.typeKind == TypeKind.ARRAY) {
				arrayDecls.put(ref.id.spelling, ref);
			}
		}
		
		if (ref.id.spelling.equals(currentVariableDecl)) {
            logError( "*** line " + ref.posn.start + ": Identification error - IdRef: " + currentVariableDecl + " is not initialized");
            return null;
        }
		if (ref.decl instanceof MemberDecl) {
			ClassDecl classDecl = ((MemberDecl) ref.decl).classDecl; // declaring a classDecl from a (member Declaration).classDecl
//			System.out.println(ref.decl);
//			if (classDecl != currentClass) {
//				logError("*** line " + ref.id.decl.posn.start + ": Identification error - IdRef: Could not find symbol: " + ref.id.spelling + " from scope of the class: " + classDecl.name);
//			}
		}
		
		for(Map.Entry<String, Reference> entry : arrayDecls.entrySet()) {
		    String key = entry.getKey();
		    Reference value = entry.getValue();
		    if (ref.id.spelling.equals(key)) {
		    	ref.decl = value.decl;
		    	ref.id.decl = value.decl;
		    }
		}
		
//		if (ref.decl.type.typeKind == TypeKind.ARRAY) {
//			System.out.println(ref.id.spelling);
//			obj22 = ref.decl;
//		}
		return null;
	}

	@Override
	public Object visitQRef(QualRef ref, Object obj) {
		ref.ref.visit(this, null);
        boolean prevStatic = isStatic;
        isStatic = ref.ref.isStatic;
        Declaration prevDecl = ref.ref.decl;
        if (prevDecl == null) return null;
        if (prevDecl instanceof MethodDecl) {
        	logError("*** line " + ref.ref.posn.start + ": Identification error - QRef: Having a method name in the middle of a QRef is not valid");
        }
        if (prevDecl instanceof ClassDecl) 
        	qRefClassDecl(ref, prevDecl);
        else {
        	qRefHelper(ref, prevDecl);
        }
		if (ref.ref instanceof IdRef) {
			if (((IdRef) ref.ref).isStatic) {
				ref.ref.decl.isStatic = true;
				ref.decl.isStatic = true;
			}
		}
        ref.decl = ref.id.decl;
        isStatic = prevStatic;
		return null;
	}

	@Override
	public Object visitIdentifier(Identifier id, Object obj) {
        id.isStatic = false;
        
        if (id.spelling.equals("length")) {
            id.decl = new FieldDecl(false, false, new BaseType(TypeKind.INT, id.posn), id.spelling, id.posn);
            return null;
        }
        
        if (obj instanceof FieldDecl || obj instanceof TypeDenoter) {
        	Declaration decl = checkMemberOrMethod(obj, id);
        	if (decl != null) {
        		id.decl = decl;
        	}
        	return null;
        }
        // we have reached the base case for identifier
        Declaration decl = idTable.retrieve(id.spelling);
        identifierBaseCase(id, decl);
		return null;
	}

	@Override
	public Object visitOperator(Operator op, Object obj) {
		return null;
	}

	@Override
	public Object visitIntLiteral(IntLiteral num, Object obj) {
		return null;
	}

	@Override
	public Object visitBooleanLiteral(BooleanLiteral bool, Object obj) {
		return null;
	}

	@Override
	public Object visitNullLiteral(NullLiteral nullLiteral, Object obj) {
		return null;
	}
	
	private void qRefHelper (QualRef ref, Declaration prevDecl) {
		ref.id.visit(this, new FieldDecl(false, false, null, null, null)); // creating a FieldDeck object to make difference of cases on visitIdentifier
        ref.isStatic = ref.id.isStatic;
        ref.decl = ref.id.decl;
        Declaration declaration = ref.decl;
        if (prevDecl.type.typeKind == TypeKind.CLASS) {
        	prevDecl.type.decl = ((ClassType) ref.ref.decl.type).className.decl;
            ClassDecl classDecl = (ClassDecl) (prevDecl.type.decl);
            System.out.println("decl " + ((ClassType) ref.ref.decl.type).className.decl);
            if (prevDecl instanceof ClassDecl) classDecl = (ClassDecl)prevDecl;
            boolean sameClass = classDecl == currentClass;

            if (declaration instanceof MemberDecl) {
                if (!sameClass) {
                    // Check visible
//                	System.out.println(ref.ref.decl.type.decl);
                    String checkName = declaration.name; 
                    if (classDecl == null) {}
                    else if (!hasMember(classDecl, checkName, false, true)) {
                        logError("*** line " + ref.ref.posn.start + ": Identification error - QRef: public member was not found: " + declaration.name); 
                    }
                } else {
                	 if (!hasMember(classDecl, declaration.name, false, false)) {
                        logError("*** line " + ref.ref.posn.start + ": Identification error - QRef: The \"" + declaration.name + "\" member was not found as declared");
                    }
                }
            } else {
                logError("*** line " + ref.posn.start + ": Identification error - QRef: Expecting \"" + declaration.name + "\" to be a member declaration");
            }
        } else if (prevDecl.type.typeKind == TypeKind.ARRAY) {
        	if (!ref.id.spelling.equals("length")) {
        		logError("*** line " + ref.id.posn.start + ": Identification error - QRef: Can't reference a member of an ARRAY unless is a \".length\"");            		
        	}  else {
    			ref.id.decl = new FieldDecl(false, false, new BaseType(TypeKind.INT, ref.ref.posn), "length", ref.ref.posn);
    			ref.id.decl.isAttributeLength = true;
        	}
        } else {
            logError("*** line " + ref.posn.start + ": Identification error - QRef: Trying to reference member of primitive types");
        }
	}
	
	private void qRefClassDecl(QualRef ref, Declaration prevDecl) {
		ClassDecl classDecl = (ClassDecl) prevDecl;
        ref.id.visit(this, new FieldDecl(false, false, null, null, null)); // creating a FieldDeck object to make difference of cases on visitIdentifier
        ref.isStatic = ref.id.isStatic;
        ref.decl = ref.id.decl;
        Declaration declaration = ref.decl;
        boolean sameClass = classDecl == currentClass;

        if (declaration instanceof MemberDecl) {
            if (!sameClass) {
//            	System.out.println(sameClass);
                String checkName = declaration.name;
                if (!hasMember(classDecl, checkName, isStatic, true)) {
                    logError("*** line " + ref.posn.start + ": Identification error - QRef: public and static member was not found: " + declaration.name);
                } 
//                else if (((MemberDecl) declaration).isStatic == true && ref.id.isStatic == false) {
//                	logError("wrong");
//                }
            } else {
                if (!hasMember(classDecl, declaration.name, isStatic, false)) {
                    logError("*** line " + ref.ref.posn.start + ": Identification error - QRef: The \"" + declaration.name + "\" member was not found");
                }
            }
        } else {
            logError("*** line " + ref.posn.start + ": Identification error - QRef: Expecting \"" + declaration.name + "\" to be a member declaration");
        }
	}
	private boolean hasMember(ClassDecl classDecl, String name, boolean expectStatic, boolean expectPublic) {
		for (FieldDecl fieldDecl : classDecl.fieldDeclList) {
			if (name.equals(fieldDecl.name)) {
				if (expectStatic && !fieldDecl.isStatic) continue;
				if (expectPublic && fieldDecl.isPrivate) continue;
				return true;
			}
		}
		
		for (MethodDecl methodDecl : classDecl.methodDeclList) {
			if (name.equals(methodDecl.name)) {
				if (expectStatic && !methodDecl.isStatic) continue;
				if (expectPublic && methodDecl.isPrivate) continue; 
				return true;
			}
		}
		return false;
	}
	
	private Declaration checkMemberOrMethod(Object obj, Identifier id) {
		if (obj instanceof FieldDecl) {
        	Declaration declaration = idTable.retrieveMember(id.spelling);
        	if (declaration == null) {
        		logError("*** line " + id.posn.start + ": Identification error - Identifier: Member \"" + id.spelling + "\" has not been declared");
        		return null;
        	} else {
        		return declaration;
        	}
        }

        if (obj instanceof TypeDenoter) {
            Declaration declaration = idTable.retrieveMethod(id.spelling);
            if (declaration == null) {
                logError("*** line " + id.posn.start + ": Identification error - Identifier Method \"" + id.spelling + "\" has not been declared");
                return null;
            } else {
                return declaration;
            }
        }
        return null;
	}
	
	private void identifierBaseCase(Identifier id, Declaration decl) {
		if (decl == null) {
            logError("*** line " + id.posn.start + ": Identification error - Identifier: Can't find: " + id.spelling);
        } else {
            if (decl instanceof ClassDecl) {
                id.decl = decl;
                id.isStatic = true;
            } else {
                if (decl instanceof MemberDecl) { 
                    MemberDecl memberDecl = (MemberDecl) decl;
                    if ((isStatic && !memberDecl.isStatic)) {
                        logError("*** line " + id.posn.start + ": Identification error - Identifier: Can't make reference to non-static variables in static methods");
                    }
                    boolean sameClass = currentClass == memberDecl.classDecl;
                    if (!sameClass && !isStatic && memberDecl.isStatic) {
                        logError("*** line " + id.posn.start + ": Identification error - Identifier: Can't make reference to static variables in non-static methods");
                    }
                }
                decl.type.visit(this, null);
                id.decl = decl;
            }
        }
	}
	
	private boolean isMain(MethodDecl methodDecl) {
		if (methodDecl.name.equals("main")) {
			if (methodDecl.isPrivate || !methodDecl.isStatic || methodDecl.type.typeKind != TypeKind.VOID) {
				return false;
			}
			if (methodDecl.parameterDeclList.size() == 1) {
				if (methodDecl.parameterDeclList.get(0).type instanceof ArrayType) {
					ArrayType arrType = (ArrayType) methodDecl.parameterDeclList.get(0).type;
					if ((arrType.eltType instanceof ClassType) && (((ClassType) arrType.eltType).className.spelling.equals("String"))) {
							existsMain = true;
							mainCounter += 1;
							return true;
					} else {
						return false;
					}
				} else {
					return false;
				}
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	public void logError(String err) {
		errorReporter.reportError(err);
	}
	
}