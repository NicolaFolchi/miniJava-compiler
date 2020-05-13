/**
 * miniJava Code Generation class
 * @author Nicola Folchi
 */
package miniJava.CodeGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import mJAM.Disassembler;
import mJAM.Interpreter;
import mJAM.Machine;
import mJAM.Machine.Op;
import mJAM.Machine.Prim;
import mJAM.Machine.Reg;
import mJAM.ObjectFile;
import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.ArrayType;
import miniJava.AbstractSyntaxTrees.AssignStmt;
import miniJava.AbstractSyntaxTrees.BaseType;
import miniJava.AbstractSyntaxTrees.BinaryExpr;
import miniJava.AbstractSyntaxTrees.BlockStmt;
import miniJava.AbstractSyntaxTrees.BooleanLiteral;
import miniJava.AbstractSyntaxTrees.CallExpr;
import miniJava.AbstractSyntaxTrees.CallStmt;
import miniJava.AbstractSyntaxTrees.ClassDecl;
import miniJava.AbstractSyntaxTrees.ClassType;
import miniJava.AbstractSyntaxTrees.Expression;
import miniJava.AbstractSyntaxTrees.FieldDecl;
import miniJava.AbstractSyntaxTrees.IdRef;
import miniJava.AbstractSyntaxTrees.Identifier;
import miniJava.AbstractSyntaxTrees.IfStmt;
import miniJava.AbstractSyntaxTrees.IntLiteral;
import miniJava.AbstractSyntaxTrees.IxAssignStmt;
import miniJava.AbstractSyntaxTrees.IxExpr;
import miniJava.AbstractSyntaxTrees.LiteralExpr;
import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.AbstractSyntaxTrees.NewArrayExpr;
import miniJava.AbstractSyntaxTrees.NewObjectExpr;
import miniJava.AbstractSyntaxTrees.NullLiteral;
import miniJava.AbstractSyntaxTrees.Operator;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.ParameterDecl;
import miniJava.AbstractSyntaxTrees.QualRef;
import miniJava.AbstractSyntaxTrees.RefExpr;
import miniJava.AbstractSyntaxTrees.Reference;
import miniJava.AbstractSyntaxTrees.ReturnStmt;
import miniJava.AbstractSyntaxTrees.Statement;
import miniJava.AbstractSyntaxTrees.StatementList;
import miniJava.AbstractSyntaxTrees.ThisRef;
import miniJava.AbstractSyntaxTrees.TypeKind;
import miniJava.AbstractSyntaxTrees.UnaryExpr;
import miniJava.AbstractSyntaxTrees.VarDecl;
import miniJava.AbstractSyntaxTrees.VarDeclStmt;
import miniJava.AbstractSyntaxTrees.Visitor;
import miniJava.AbstractSyntaxTrees.WhileStmt;
import miniJava.SyntacticAnalyzer.TokenKind;

public class CodeGeneration implements Visitor<Object, Object> {
	
	public Package packageAST;
	public ErrorReporter errorReporter;
	private ArrayList<PatchJump> patchList; // prolly have to change
	private HashMap<String, Reference> idRefDecls;
	private HashMap<String, Reference> qRefDecls;
	int patchAddrCallmain;
	int codeAddr_main;
	int staticOffset;
	int methodOffset = 0;
	int staticFieldsAddr;
	int localDisplacement; 
	int numOfMethodParams;
	int varDeclStmtTracker = 0;
	
	
	public CodeGeneration(Package packageAST, ErrorReporter err) {
		this.packageAST = packageAST;
		this.errorReporter = err;
		this.patchList = new ArrayList<>();
		idRefDecls = new HashMap<String, Reference>();
		qRefDecls = new HashMap<String, Reference>();
		Machine.initCodeGen();
	}
	
	public void begin() {
		staticFieldsAddr = Machine.nextInstrAddr();
		Machine.emit(Op.PUSH, 0); // allocating space for static variables
		Machine.emit(Op.LOADL,0);            // array length 0
		Machine.emit(Prim.newarr);           // empty String array argument
		patchAddrCallmain = Machine.nextInstrAddr();  // record instr addr where main is called                                                // "main" is called
		Machine.emit(Op.CALL,Reg.CB,0);     // static call main (address to be patched)
		Machine.emit(Op.HALT,0,0,0);         // end execution
		packageAST.visit(this, null);
	}

	@Override
	public Object visitPackage(Package prog, Object arg) {
		staticOffset = 0;
		for (ClassDecl classDecl : prog.classDeclList) {
			int instanceOffset = 0;
			for (FieldDecl fieldDecl : classDecl.fieldDeclList) {
				if (fieldDecl.isStatic) {
					fieldDecl.runtimeEntityDes = new RuntimeEntityDesc(staticOffset++);
				} else {
					fieldDecl.runtimeEntityDes = new RuntimeEntityDesc(instanceOffset++);
				}
			}
			classDecl.runtimeEntityDes = new RuntimeEntityDesc(instanceOffset);
		}
		
		Machine.patch(staticFieldsAddr, staticOffset);
		Machine.patch(patchAddrCallmain, Machine.nextInstrAddr());
		
		// finding main method
		for(ClassDecl singleClass : prog.classDeclList) {
			for (MethodDecl methodDecl : singleClass.methodDeclList) {
				if (methodDecl.name.equals("main")) {
					patchList.add(new PatchJump(patchAddrCallmain, methodDecl));
				}
			}
		}
		
		// adding a return stmt at the end of the method
		for (ClassDecl classDecl : prog.classDeclList) {
			for (MethodDecl methodDecl : classDecl.methodDeclList) {
				StatementList statementList = methodDecl.statementList;
				if (statementList.size() == 0) {
					methodDecl.statementList.add(new ReturnStmt(null, methodDecl.posn));
				}
				Statement lastStatement = statementList.get(statementList.size()-1);
				if (methodDecl.type.typeKind != TypeKind.VOID) {
					if (!(lastStatement instanceof ReturnStmt)) {
						logError("*** non-void method '" + methodDecl.name + "' does not return at end.");
					}
				} else {
					methodDecl.statementList.add(new ReturnStmt(null, lastStatement.posn));
				}
			}
		}
		
		for (ClassDecl c : prog.classDeclList)
			c.visit(this, null);
		
		// final pass to patch function call addresses
		for (PatchJump fp : patchList) {
			System.out.println("Patching method " + fp.methodDeclaration.name + " at addr " + fp.patchAddress);
			Machine.patch(fp.patchAddress, fp.methodDeclaration.runtimeEntityDes.offset);
		}
		return null;
	}

	@Override
	public Object visitClassDecl(ClassDecl cd, Object arg) {
		for (FieldDecl fieldDecl : cd.fieldDeclList) {
			fieldDecl.visit(this, null);
		}
		for (MethodDecl singleMethod : cd.methodDeclList) {
			singleMethod.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitFieldDecl(FieldDecl fd, Object arg) {
		fd.type.visit(this, null);
		return null;
	}

	@Override
	public Object visitMethodDecl(MethodDecl md, Object arg) {
		localDisplacement = 3;
		numOfMethodParams = md.parameterDeclList.size();
		md.type.visit(this, null);
		int parameterOffset = -md.parameterDeclList.size();
		for (ParameterDecl parameter : md.parameterDeclList) {
			parameter.visit(this, null);
			parameter.runtimeEntityDes = new RuntimeEntityDesc(parameterOffset++);
		}
		// offset of method to store its start on memory location
//		if (md.name.equals("main")) { 
//			codeAddr_main = Machine.nextInstrAddr(); // saving the start of main method
//			patchList.put(codeAddr_main, md); // saving main address to be later on patched
//		}
		md.runtimeEntityDes = new RuntimeEntityDesc(Machine.nextInstrAddr());
		for (Statement statement : md.statementList) {
			statement.visit(this, null);
		}

		return null;
	}

	@Override
	public Object visitParameterDecl(ParameterDecl pd, Object arg) {
		pd.type.visit(this, null);
		return null;
	}

	@Override
	public Object visitVarDecl(VarDecl decl, Object arg) {
		decl.runtimeEntityDes = new RuntimeEntityDesc(localDisplacement++);
		decl.type.visit(this, null);
		return null;
	}

	@Override
	public Object visitBaseType(BaseType type, Object arg) {
		return null;
	}

	@Override
	public Object visitClassType(ClassType type, Object arg) {
		type.className.visit(this, null);
		return null;
	}

	@Override
	public Object visitArrayType(ArrayType type, Object arg) {
		type.eltType.visit(this, null);
		return null;
	}

	@Override
	public Object visitBlockStmt(BlockStmt stmt, Object arg) { // check this
		this.varDeclStmtTracker = 0;
		for (Statement statement : stmt.sl) {
			statement.visit(this, null);
		}
		if (this.varDeclStmtTracker > 0) { // getting rid of the varDeclStmts on the stack when the block stmt closes. setting the LB offset back to normal
			this.localDisplacement -= this.varDeclStmtTracker;
			Machine.emit(Op.POP, this.varDeclStmtTracker);
		}
//		varDeclStmtTracker = 0;
		return null;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		this.varDeclStmtTracker++;
		stmt.varDecl.visit(this, null);
		stmt.initExp.visit(this, null);
		// increase frame by one, push local var to stack, use loada
		return null;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, Object arg) {
		System.out.println(stmt.ref);
		if (stmt.ref.decl == null) {
			if (stmt.ref instanceof QualRef) {
				for(Map.Entry<String, Reference> entry : idRefDecls.entrySet()) {
					String key = entry.getKey();
					Reference value = entry.getValue();
					System.out.println(key + " "+ value);
					if (((QualRef) stmt.ref).id.spelling.equals(key)) {
						stmt.ref.decl = value.decl;
						((QualRef) stmt.ref).id.decl = value.decl;
					}
				}
				for(Map.Entry<String, Reference> entry : qRefDecls.entrySet()) {
					String key = entry.getKey();
					Reference value = entry.getValue();
					System.out.println(key + " "+ value);
					if (((QualRef) stmt.ref).id.spelling.equals(key)) {
						stmt.ref.decl = value.decl;
						((QualRef) stmt.ref).id.decl = value.decl;
					}
				}
			}
		}
		if (stmt.ref.decl.isStatic) {
			stmt.val.visit(this, null);
			Machine.emit(Op.STORE, Machine.Reg.SB, stmt.ref.decl.runtimeEntityDes.offset);
		} else if (stmt.ref instanceof IdRef) {
			IdRef idReference = (IdRef) stmt.ref;
			if (idReference.decl instanceof FieldDecl) {// it is a fieldDecl, need to load the value from stmt.ref.decl.runtimeEntityDesc
				if (stmt.ref.isStatic) { // fieldDecl is static then go to SB
					stmt.val.visit(this, null);
					Machine.emit(Op.STORE, Reg.SB, idReference.id.decl.runtimeEntityDes.offset);
				} else { // non-static
					Machine.emit(Op.LOADA, Machine.Reg.OB, 0);
					Machine.emit(Op.LOADL, idReference.id.decl.runtimeEntityDes.offset);
					stmt.val.visit(this, null);
					Machine.emit(Prim.fieldupd);
				}
			} else { // if it's something else than a fielddecl (parameter decl) 
				stmt.val.visit(this, null);
				if (idReference.decl instanceof FieldDecl) {
					FieldDecl fieldDecl = (FieldDecl) idReference.decl;
					if (fieldDecl.isStatic) {
						Machine.emit(Op.STORE, Machine.Reg.SB, idReference.id.decl.runtimeEntityDes.offset);
					} else {

					}
				} else {
					if (idReference.id.decl.isStatic) {
						Machine.emit(Op.STORE, Machine.Reg.SB, idReference.id.decl.runtimeEntityDes.offset);
					} else {
						Machine.emit(Op.STORE, Machine.Reg.LB, idReference.id.decl.runtimeEntityDes.offset);
					}
				}
			}
			
		} else if (stmt.ref instanceof QualRef) {
			QualRef qRef = (QualRef) stmt.ref;
//			if (stmt.ref.isStatic) {
//				Machine.emit(Op.LOAD, Machine.Reg.SB, qRef.id.decl.runtimeEntityDes.offset);
//			} else {
				if (qRef.id.decl.runtimeEntityDes != null) {
					Stack<Integer> offsetStackTracker = new Stack<Integer>();
					offsetStackTracker.push(qRef.id.decl.runtimeEntityDes.offset);
					while (qRef.ref instanceof QualRef) {
						qRef = (QualRef) qRef.ref;
						offsetStackTracker.push(qRef.decl.runtimeEntityDes.offset);
					}
					qRef.ref.visit(this, null);
					int stackSize = offsetStackTracker.size();
					for (int i = 0; i < stackSize; i++) {
						int fieldOffset = offsetStackTracker.pop();
						Machine.emit(Op.LOADL, fieldOffset);
						if (i + 1 < stackSize) {
							Machine.emit(Prim.fieldref);
						}
					}
				}
				stmt.val.visit(this, null);
				Machine.emit(Prim.fieldupd);
			}
//		}
		return null;
	}

	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		stmt.ref.visit(this, null);
		stmt.ix.visit(this, null);
		stmt.exp.visit(this, null);
		Machine.emit(Prim.arrayupd);
		return null;
	}

	@Override
	public Object visitCallStmt(CallStmt stmt, Object arg) {
		for (Expression expression : stmt.argList) {
			expression.visit(this, null);
		}
		System.out.println(stmt.posn.start+ "-------------------");
		if (stmt.methodRef.decl.name.equals("println")) {
			Machine.emit(Prim.putintnl);
//			if (stmt.argList.size() == 1) {
//				Machine.emit(Prim.putintnl);
//			}
		} else {
			if (((MethodDecl) stmt.methodRef.decl).isStatic) { // call is static
				int callStmtAddr = Machine.nextInstrAddr();
				Machine.emit(Op.CALL, Machine.Reg.CB, 0);
				patchList.add(new PatchJump(callStmtAddr, (MethodDecl) stmt.methodRef.decl));
			} else { // non-static call
				if (stmt.methodRef instanceof QualRef) { // we visit the left hand side of the method call
					stmt.methodRef.visit(this, null);
					((QualRef) stmt.methodRef).ref.visit(this, null);
				} else { // case were we have a this.method();
					stmt.methodRef.visit(this, null);
					visitThisRef(null, null);
				}
				int callStmtAddr = Machine.nextInstrAddr();
				Machine.emit(Op.CALLI, Machine.Reg.CB, 0);
				patchList.add(new PatchJump(callStmtAddr, (MethodDecl) stmt.methodRef.decl));
			}
		}
		if (stmt.methodRef.decl.type.typeKind != TypeKind.VOID) { // if method is not assigned nor acting on an obj, then we pop it
			Machine.emit(Op.POP, 1);
		}
		return null;
	}

	@Override
	public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
		if (stmt.returnExpr != null) stmt.returnExpr.visit(this, null);
		Machine.emit(Op.RETURN, stmt.returnExpr == null ? 0 : 1, 0, numOfMethodParams);
		return null;
	}

	@Override
	public Object visitIfStmt(IfStmt stmt, Object arg) {
		int thenAddr, elseAddr;
		stmt.cond.visit(this, null);
		
		thenAddr = Machine.nextInstrAddr();
		Machine.emit(Op.JUMPIF, 0, Machine.Reg.CB, 0); // we jump to the then if and only if the cond is met
		stmt.thenStmt.visit(this, null);
		elseAddr = Machine.nextInstrAddr();
		Machine.emit(Op.JUMP, 0, Machine.Reg.CB, 0); // make a regular jump
		
		Machine.patch(thenAddr, Machine.nextInstrAddr());
		if (stmt.elseStmt != null) {
			stmt.elseStmt.visit(this, null);
		}
		
		Machine.patch(elseAddr, Machine.nextInstrAddr());
		return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, Object arg) {
		int condAddr, bodyAddr;
		
		bodyAddr = Machine.nextInstrAddr();
		Machine.emit(Op.JUMP, 0, Machine.Reg.CB, 0);
		stmt.body.visit(this, null);
		
		condAddr = Machine.nextInstrAddr();
		stmt.cond.visit(this, null);
		Machine.emit(Op.JUMPIF, 1, Machine.Reg.CB, bodyAddr + 1);
		
		Machine.patch(bodyAddr, condAddr);
		return null;
	}

	@Override
	public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
		// converting -x to 0-x
		if (expr.operator.kind == TokenKind.NEGATIVE) { Machine.emit(Op.LOADL, 0); }
		expr.expr.visit(this, null);
		expr.operator.visit(this, null);
		return null;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
		int firstAddress;
		int endAddress;

		if (expr.operator.kind == TokenKind.CONJUNCTION) {
			expr.left.visit(this, null);
			firstAddress = Machine.nextInstrAddr();

			Machine.emit(Machine.Op.JUMPIF, 0, Machine.Reg.CB, -1);
			Machine.emit(Machine.Op.LOADL, 1);

			expr.right.visit(this, null);

			Machine.emit(Machine.Prim.and);
			endAddress = Machine.nextInstrAddr();
			Machine.emit(Op.JUMP, Machine.Reg.CB, -1);
			Machine.patch(firstAddress, Machine.nextInstrAddr());
			Machine.emit(Machine.Op.LOADL, 0);
			Machine.patch(endAddress, Machine.nextInstrAddr());
			return null;
		} else if (expr.operator.kind == TokenKind.DISJUNCTION) {
			expr.left.visit(this, null);
			firstAddress = Machine.nextInstrAddr();

			Machine.emit(Machine.Op.JUMPIF, 1, Machine.Reg.CB, -1);
			Machine.emit(Machine.Op.LOADL, 0);

			expr.right.visit(this, null);

			Machine.emit(Machine.Prim.or);
			endAddress = Machine.nextInstrAddr();
			Machine.emit(Op.JUMP, Machine.Reg.CB, -1);
			Machine.patch(firstAddress, Machine.nextInstrAddr());
			Machine.emit(Op.LOADL, 1);
			Machine.patch(endAddress, Machine.nextInstrAddr());
			return null;
		} else {
			expr.left.visit(this, null);
			expr.right.visit(this, null);
			expr.operator.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitRefExpr(RefExpr expr, Object arg) {
		System.out.println(expr.ref);
		if (expr.ref instanceof ThisRef) { // since this ref doesnt have a static we check for it first
			Machine.emit(Op.LOADA, Machine.Reg.OB, 0);
		}
		else if (expr.ref.decl.isStatic) {
			Machine.emit(Op.LOAD, Machine.Reg.SB, expr.ref.decl.runtimeEntityDes.offset);
		} else if (expr.ref instanceof IdRef) {
			IdRef idReference = (IdRef) expr.ref;
			if (expr.ref.isStatic) {
				Machine.emit(Op.LOAD, Machine.Reg.SB, idReference.id.decl.runtimeEntityDes.offset);
			} else {
				expr.ref.visit(this, null);
			}
		} else if (expr.ref instanceof QualRef) {
			QualRef qRef = (QualRef) expr.ref;
//			if (expr.ref.isStatic) {
//				Machine.emit(Op.LOAD, Machine.Reg.SB, qRef.id.decl.runtimeEntityDes.offset);
//			} else {
				expr.ref.visit(this, null);
//			}
		} else {
			expr.ref.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitIxExpr(IxExpr expr, Object arg) {
		expr.ref.visit(this, null);
		expr.ixExpr.visit(this, null);
		Machine.emit(Prim.arrayref);
		return null;
	}

	@Override
	public Object visitCallExpr(CallExpr expr, Object arg) {
		int exprCallAddr;
		for (Expression expression : expr.argList) {
			expression.visit(this, null);
		}
		if (!(expr.functionRef.decl.name.equals("println"))) {
			expr.functionRef.visit(this, null);
			exprCallAddr = Machine.nextInstrAddr();
			if (((MethodDecl) expr.functionRef.decl).isStatic)  { // ref is static
				Machine.emit(Op.CALL, Machine.Reg.CB, 0);
				patchList.add(new PatchJump(exprCallAddr, (MethodDecl) expr.functionRef.decl));
			} else { // non-staic ref
				if (expr.functionRef instanceof QualRef) { // if has another reference, visit it
					((QualRef) expr.functionRef).ref.visit(this, null);
				} else {
					Machine.emit(Op.LOADA, Machine.Reg.OB, 0);
				}
				exprCallAddr = Machine.nextInstrAddr();
				Machine.emit(Op.CALLI, Machine.Reg.CB, 0);
				patchList.add(new PatchJump(exprCallAddr, (MethodDecl) expr.functionRef.decl));
			}
		}
		return null;
	}

	@Override
	public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
		expr.lit.visit(this, null);
		return null;
	}

	@Override
	public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		Machine.emit(Op.LOADL, -1);
		Machine.emit(Op.LOADL, expr.classtype.className.decl.runtimeEntityDes.offset);
		Machine.emit(Prim.newobj);
		return null;
	}

	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		expr.sizeExpr.visit(this, null);
		Machine.emit(Prim.newarr);
		return null;
	}

	@Override
	public Object visitThisRef(ThisRef ref, Object arg) {
//		System.out.println(ref);
		Machine.emit(Op.LOADA, Machine.Reg.OB, 0);
		return null;
	}

	@Override
	public Object visitIdRef(IdRef ref, Object arg) {
		if (ref.decl != null) {
			idRefDecls.put(ref.id.spelling, ref);
		}
		if (ref.decl == null) {
			for(Map.Entry<String, Reference> entry : idRefDecls.entrySet()) {
			    String key = entry.getKey();
			    Reference value = entry.getValue();
			    System.out.println(key + " "+ value);
			    if (ref.id.spelling.equals(key)) {
			    	ref.decl = value.decl;
			    	ref.id.decl = value.decl;
			    }
			}
		}
//		ref.id.visit(this, null);
		getIdRefFromStack(ref);
		return null;
	}

	@Override
	public Object visitQRef(QualRef ref, Object arg) {
		if (ref.decl != null) {
				qRefDecls.put(ref.id.spelling, ref);
		}
		if (ref.decl == null) {
			for(Map.Entry<String, Reference> entry : qRefDecls.entrySet()) {
			    String key = entry.getKey();
			    Reference value = entry.getValue();
			    if (ref.id.spelling.equals(key)) {
			    	ref.decl = value.decl;
			    	ref.id.decl = value.decl;
			    }
			}
		}
		if (ref.id.decl.isAttributeLength) {
//			System.out.println(ref.id.spelling + " ----------");
			if (ref.ref instanceof IdRef)
				getIdRefFromStack((IdRef) ref.ref);
			Machine.emit(Prim.arraylen);
		} else {
//			QualRef qRef = (QualRef) ref;
			if (ref.id.decl.runtimeEntityDes != null) {
				Stack<Integer> offsetStackTracker = new Stack<Integer>();
				offsetStackTracker.push(ref.id.decl.runtimeEntityDes.offset);
				while (ref.ref instanceof QualRef) {
					ref = (QualRef) ref.ref;
					offsetStackTracker.push(ref.decl.runtimeEntityDes.offset);
				}
				ref.ref.visit(this, null);
				int stackSize = offsetStackTracker.size();
				for (int i = 0; i < stackSize; i++) {
					int fieldOffset = offsetStackTracker.pop();
					Machine.emit(Op.LOADL, fieldOffset);
					if (i + 1 < stackSize) {
						Machine.emit(Prim.fieldref);
					}
				}
				Machine.emit(Prim.fieldref);
			}
		}
		return null;
	}

	@Override
	public Object visitIdentifier(Identifier id, Object arg) {
		return null;
	}

	@Override
	public Object visitOperator(Operator op, Object arg) {
		switch (op.kind) {
		
		case ADDITIVE:
			Machine.emit(Prim.add);
			break;
			
		case NEGATIVE:
			Machine.emit(Prim.sub);
			break;
			
		case MULTIPLICATIVE: // multiplication and division
			if (op.spelling.equals("*")) {
				Machine.emit(Prim.mult);
				break;
			} else if (op.spelling.equals("/")){
				Machine.emit(Prim.div);
				break;
			}
			
		case DISJUNCTION:
			Machine.emit(Prim.or);
			break;
			
		case CONJUNCTION:
			Machine.emit(Prim.and);
			break;
			
		case NOT: 
			Machine.emit(Prim.neg);
			break;
			
		case EQUALITY:
			if (op.spelling.equals("==")) {
				Machine.emit(Prim.eq);
				break;
			} else if (op.spelling.equals("!=")) {
				Machine.emit(Prim.ne); // check
				break;
			}
			
		case RELATIONAL:
			if (op.spelling.equals(">")) {
				Machine.emit(Prim.gt);
				break;
			} else if (op.spelling.equals("<")) {
				Machine.emit(Prim.lt);
				break;
			} else if (op.spelling.equals("<=")) {
				Machine.emit(Prim.le);
				break;
			} else if (op.spelling.equals(">=")) {
				Machine.emit(Prim.ge);
				break;
			}
			
		default:
			logError("*** Code Generation error: Unrecognized character '" + op + "' in input");
			return (TokenKind.ERROR);
		}
		return null;
	}

	@Override
	public Object visitIntLiteral(IntLiteral num, Object arg) {
		int integerBitValue = Integer.parseInt(num.spelling);
//		System.out.println("got here  with " + integerBitValue);
//		System.out.println(integerBitValue);
		Machine.emit(Op.LOADL, integerBitValue);
		return null;
	}

	@Override
	public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		if (bool.spelling.equals("false")) {
			Machine.emit(Op.LOADL, Machine.falseRep);
		}	else if (bool.spelling.equals("true")) {
			Machine.emit(Op.LOADL, Machine.trueRep);
		} 
		return null;
	}

	@Override
	public Object visitNullLiteral(NullLiteral nullLiteral, Object arg) {
		Machine.emit(Op.LOADL, Machine.nullRep);
		return null;
	}
	
	public void getIdRefFromStack(IdRef ref) {
		if (ref.decl instanceof FieldDecl) {
			getFieldDeclIdRef(ref);
		} else if (ref.id.decl.runtimeEntityDes != null) {
			getNewIdRef(ref);
		}

	}
	
	public void getFieldDeclIdRef(IdRef ref) {
		FieldDecl fieldDeclaration = (FieldDecl) ref.decl;
		if (fieldDeclaration.isStatic) {
			Machine.emit(Op.LOAD, Machine.Reg.SB, ref.id.decl.runtimeEntityDes.offset);
		} else {
			Machine.emit(Op.LOAD, Machine.Reg.OB, ref.id.decl.runtimeEntityDes.offset);
		}
	}
	
	public void getNewIdRef(IdRef ref) {
		if (ref.id.decl.isStatic) {
			Machine.emit(Op.LOAD, Machine.Reg.SB, ref.id.decl.runtimeEntityDes.offset);
		} else if (!(ref.id.decl instanceof MethodDecl)) {
			Machine.emit(Op.LOAD, Machine.Reg.LB, ref.id.decl.runtimeEntityDes.offset);
		}
	}
	
	
	public void logError(String err) {
		errorReporter.reportError(err);
	}

}
