/**
 * miniJava Contextual Analyser classes
 * @author Nicola Folchi
 */
package miniJava.ContextualAnalyser;

import java.util.HashMap;
import java.util.Stack;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenKind;

public class IdTable {

	private Stack<HashMap<String, Declaration>> idTablesStack;
	private HashMap<String, Declaration> currentIdTable;
	private Identifier printStreamIdentifier;

	public IdTable() {
		currentIdTable = null;
		idTablesStack = new Stack<>();
		standardEnvironmentInit();
	}

	public IdTable(HashMap<String, Declaration> idTable) {
		currentIdTable = idTable;
		idTablesStack = new Stack<>();
		idTablesStack.push(currentIdTable);
	}

	public Object enter(String id, Declaration decl) {
		if (currentIdTable.containsKey(id)) {
			return null; // error fund
		} else {
			currentIdTable.put(id, decl); 
		}
		return decl;
	}

	public Declaration retrieve(String id) {
		for (int i = (idTablesStack.size() - 1); i >= 0; i--) {
			HashMap<String, Declaration> hashMapIdTable = idTablesStack.get(i);
			if (hashMapIdTable.containsKey(id)) {
				return hashMapIdTable.get(id);
			}
		}
		return null;
	}

	public Declaration retrieveClass(String id) {
		for (int i = (idTablesStack.size() - 1); i >= 0; i--) {
			HashMap<String, Declaration> hashMapIdTable = idTablesStack.get(i);
			if (hashMapIdTable.containsKey(id) && hashMapIdTable.get(id) instanceof ClassDecl) {
				return hashMapIdTable.get(id);
			}
		}
		return null;
	}

	public Declaration retrieveMember(String id) {
		for (int i = (idTablesStack.size() - 1); i >= 0; i--) {
			HashMap<String, Declaration> hashMapIdTable = idTablesStack.get(i);
			if (hashMapIdTable.containsKey(id) && hashMapIdTable.get(id) instanceof MemberDecl) {
				return hashMapIdTable.get(id);
			}
		}
		return null;
	}

	public Declaration retrieveField(String id) {
		for (int i = (idTablesStack.size() - 1); i >= 0; i--) {
			HashMap<String, Declaration> hashMapIdTable = idTablesStack.get(i);
			if (hashMapIdTable.containsKey(id) && hashMapIdTable.get(id) instanceof FieldDecl) {
				return hashMapIdTable.get(id);
			}
		}
		return null;
	}

	public Declaration retrieveMethod(String id) {
		for (int i = (idTablesStack.size() - 1); i >= 0; i--) {
			HashMap<String, Declaration> hashMapIdTable = idTablesStack.get(i);
			if (hashMapIdTable.containsKey(id) && hashMapIdTable.get(id) instanceof MethodDecl) {
				return hashMapIdTable.get(id);
			}
		}
		return null;
	}

	public int getCurrentLevel() {
		return (idTablesStack.size() - 1);
	}

	public HashMap<String, Declaration> getCurrentIdTable() {
		return this.currentIdTable;
	}

	public boolean isInCurrLevel(String id) {
		if (currentIdTable.containsKey(id)) {
			return true;
		}
		return false;
	}

	public void openScope() {
		currentIdTable = new HashMap<>();
		idTablesStack.push(currentIdTable);
	}

	public void closeScope() {
		idTablesStack.pop();
		currentIdTable = idTablesStack.peek();
	}

	private void standardEnvironmentInit() {
		openScope();
		Declaration printlnMethod = createPrintStreamClass();
		Declaration outField = createSystemClass();
		createStringClass();
		openScope();
		enter("println", printlnMethod);
		enter("out", outField);
		openScope();
	}

	private Declaration createPrintStreamClass() {
		MethodDeclList printStreamMethods = new MethodDeclList();
		MemberDecl printlnField = new FieldDecl(false, false, new BaseType(TypeKind.VOID, null), "println", null);
		ParameterDeclList printlnParameters = new ParameterDeclList();
		ParameterDecl nParameter = new ParameterDecl(new BaseType(TypeKind.INT, null), "n", null);
		printlnParameters.add(nParameter);
		MethodDecl printlnMethod = new MethodDecl(printlnField, printlnParameters, new StatementList(), null);
		printStreamMethods.add(printlnMethod);
		ClassDecl printStreamClass = new ClassDecl("_PrintStream", new FieldDeclList(), printStreamMethods, null);
		printStreamIdentifier = new Identifier(new Token(TokenKind.ID, "_PrintStream", null));
		printStreamClass.type = new ClassType(printStreamIdentifier, new SourcePosition());
		enter("_PrintStream", printStreamClass);
		return printlnMethod;
	}

	private Declaration createSystemClass() {
		Identifier systemIdentifier = new Identifier(new Token(TokenKind.ID, "System", null));
		FieldDeclList systemFields = new FieldDeclList();
		FieldDecl outField = new FieldDecl(false, true, new ClassType(printStreamIdentifier, null), "out", null);
		systemFields.add(outField);
		ClassDecl systemClass = new ClassDecl("System", systemFields, new MethodDeclList(), null);
		systemClass.type = new ClassType(systemIdentifier, new SourcePosition());
		enter("System", systemClass);
		return outField;
	}

	private void createStringClass() {
		ClassDecl stringClass = new ClassDecl("String", new FieldDeclList(), new MethodDeclList(), null);
		stringClass.type = new BaseType(TypeKind.UNSUPPORTED, null);
		enter("String", stringClass);
	}

	public void printScopeIndex(int i) {
		for (String name : currentIdTable.keySet()) {
			String key = name.toString();
			String value = currentIdTable.get(name).toString();
			System.out.println(key + " " + value);
		}
	}

	public void printStack() {
		System.out.println(idTablesStack);
	}
}
