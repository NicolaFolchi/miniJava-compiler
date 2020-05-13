/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.CodeGenerator.RuntimeEntityDesc;
import miniJava.SyntacticAnalyzer.SourcePosition;

public abstract class Declaration extends AST {
	
	public Declaration(String name, TypeDenoter type, SourcePosition posn) {
		super(posn);
		this.name = name;
		this.type = type;
		this.runtimeEntityDes = null;
	}
	
	public String name;
	public TypeDenoter type;
	
	// PA4 Addition
	public RuntimeEntityDesc runtimeEntityDes;
	public boolean isStatic = false;
	public boolean isAttributeLength = false;
}
