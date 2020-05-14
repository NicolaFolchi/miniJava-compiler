/**
 * miniJava Compiler class
 * @author Nicola Folchi
 */
package miniJava;

import java.io.*;

import mJAM.Disassembler;
import mJAM.Interpreter;
import mJAM.ObjectFile;
import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.CodeGenerator.CodeGeneration;
import miniJava.ContextualAnalyser.Identification;
import miniJava.ContextualAnalyser.TypeChecking;

public class Compiler {
//	public static void main(String args[]) {
//		InputStream inputStream = null;
//		try {
//			inputStream = new FileInputStream(args[0]);
//		} catch (FileNotFoundException e) {
//			System.out.println("Input file " + args[0] + " not found");
//			System.exit(3);
//		} 
//		
//		ErrorReporter errorReporter = new ErrorReporter();
//		Scanner scanner = new Scanner(inputStream, errorReporter);
//		Parser parser = new Parser(scanner, errorReporter);
//		Package packageAST = null; 
//		Identification idChecker; 
//		TypeChecking typeChecker;
//		CodeGeneration codeGeneration;
//		
//		System.out.println("Syntactic analysis ... ");
//		try {
//			packageAST = parser.parse();
//			new ASTDisplay().showTree(packageAST);
//		} catch (Exception e) {}
//		System.out.print("Syntactic analysis complete:  ");
//		
//		if (errorReporter.hasErrors()) {
//			System.out.println("INVALID arithmetic expression");
//			// return code for invalid input
//			System.exit(4);
//		} else {
//			System.out.println("Valid arithmetic expression");
////			System.exit(0);
//		}
//		
//		try {
//			idChecker = new Identification(packageAST, errorReporter);
//			idChecker.beginCheck();
//			if (errorReporter.hasErrors()) {
//				System.out.println("INVALID arithmetic expression");
//				System.exit(4);
//			}
//			typeChecker = new TypeChecking(packageAST, errorReporter); 
//			typeChecker.beginCheck();
//		} catch (Exception e) {}
//		
//		if (errorReporter.hasErrors()) {
//			System.out.println("INVALID arithmetic expression");
//			// return code for invalid input
////			System.exit(4);
//		} else {
//			System.out.println("Valid arithmetic expression");
////			System.exit(0);
//		}
//		
//		
//		try {
//			codeGeneration = new CodeGeneration(packageAST, errorReporter);
//			codeGeneration.begin();
//		} catch (Exception e) {}
//		
//		
//		String objectCodeFileName = args[0].replace(".java", ".mJAM");
//		ObjectFile objF = new ObjectFile(objectCodeFileName);
//		System.out.print("Writing object code file " + objectCodeFileName + " ... ");
//		if (objF.write()) {
//			errorReporter.reportError("*** Code Generation error: Writing object code file FAILED");
//			return;
//		} else {
//			System.out.println("SUCCEEDED");
//		}
//
//		String asmCodeFileName = objectCodeFileName.replace(".mJAM", ".asm");
//		System.out.print("Writing assembly file " + asmCodeFileName + " ... ");
//		Disassembler d = new Disassembler(objectCodeFileName);
//		if (d.disassemble()) {
//			errorReporter.reportError("***Code Generation error: Writing assembly file FAILED");
//			return;
//		} else
//			System.out.println("SUCCEEDED");
//
//		// Debugger 
//		boolean runInterpreter = false;
//		if (runInterpreter ) {
//			System.out.println("Running code in debugger ... ");
//			Interpreter.debug(objectCodeFileName, asmCodeFileName);
//			System.out.println("*** mJAM execution completed");
//		}
//			
//		if (errorReporter.hasErrors()) {
//			System.out.println("INVALID arithmetic expression");
//			// return code for invalid input
////			System.exit(4);
//		} else {
//			System.out.println("Valid arithmetic expression");
////			System.exit(0);
//		}
//	}
	public static void run(String args) {
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(args);
		} catch (FileNotFoundException e) {
			System.out.println("Input file " + args + " not found");
			System.exit(3);
		} 
		OutputWriter writeOutput = new OutputWriter();
		ErrorReporter errorReporter = new ErrorReporter();
		Scanner scanner = new Scanner(inputStream, errorReporter);
		Parser parser = new Parser(scanner, errorReporter, writeOutput);
		Package packageAST = null; 
		Identification idChecker; 
		TypeChecking typeChecker;
		CodeGeneration codeGeneration;
		
		System.out.println("Syntactic analysis ... ");
		try {
			packageAST = parser.parse();
			new ASTDisplay().showTree(packageAST);
		} catch (Exception e) {}
		System.out.print("Syntactic analysis complete:  ");
		
		if (errorReporter.hasErrors()) {
			System.out.println("INVALID arithmetic expression");
			// return code for invalid input
//			System.exit(4);
		} else {
			System.out.println("Valid arithmetic expression");
//				System.exit(0);
		}
		
		try {
			idChecker = new Identification(packageAST, errorReporter);
			idChecker.beginCheck();
			if (errorReporter.hasErrors()) {
				System.out.println("INVALID arithmetic expression");
//				System.exit(4);
			}
			typeChecker = new TypeChecking(packageAST, errorReporter); 
			typeChecker.beginCheck();
		} catch (Exception e) {}
		
		if (errorReporter.hasErrors()) {
			System.out.println("INVALID arithmetic expression");
			// return code for invalid input
//			System.exit(4);
		} else {
			System.out.println("Valid arithmetic expression");
//				System.exit(0);
		}
		
		
		try {
			codeGeneration = new CodeGeneration(packageAST, errorReporter);
			codeGeneration.begin();
		} catch (Exception e) {}
		
		
		String objectCodeFileName = args.replace(".java", ".mJAM");
		ObjectFile objF = new ObjectFile(objectCodeFileName);
		System.out.print("Writing object code file " + objectCodeFileName + " ... ");
		if (objF.write()) {
			errorReporter.reportError("*** Code Generation error: Writing object code file FAILED");
			return;
		} else {
			System.out.println("SUCCEEDED");
		}

		String asmCodeFileName = objectCodeFileName.replace(".mJAM", ".asm");
		System.out.print("Writing assembly file " + asmCodeFileName + " ... ");
		Disassembler d = new Disassembler(objectCodeFileName);
		if (d.disassemble()) {
			errorReporter.reportError("***Code Generation error: Writing assembly file FAILED");
			return;
		} else
			System.out.println("SUCCEEDED");

		// Debugger 
		boolean runInterpreter = false;
		if (runInterpreter ) {
			System.out.println("Running code in debugger ... ");
			Interpreter.debug(objectCodeFileName, asmCodeFileName);
			System.out.println("*** mJAM execution completed");
		}
			
		if (errorReporter.hasErrors()) {
			System.out.println("INVALID arithmetic expression");
			// return code for invalid input
//			System.exit(4);
		} else {
			System.out.println("Valid arithmetic expression");
//			System.exit(0);
		}
		try { // closing writer to compilerOutput file
			writeOutput.writer.close();
			errorReporter.writer.close();
//			errorReporter.codeToParse.delete();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}



