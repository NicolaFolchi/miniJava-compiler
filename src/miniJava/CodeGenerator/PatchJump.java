/**
 * miniJava Code Generation class - Patching 
 * @author Nicola Folchi
 */
package miniJava.CodeGenerator;

import miniJava.AbstractSyntaxTrees.MethodDecl;

public class PatchJump {
	MethodDecl methodDeclaration;
	int patchAddress;
	
	public PatchJump(int address, MethodDecl methodDeclaration) {
		this.methodDeclaration = methodDeclaration;
		this.patchAddress = address;
	}
}
