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
		OutputWriter outputWriter = new OutputWriter();
		ErrorReporter errorReporter = new ErrorReporter();
		Scanner scanner = new Scanner(inputStream, errorReporter);
		Parser parser = new Parser(scanner, errorReporter, outputWriter);
		Package packageAST = null; 
		Identification idChecker; 
		TypeChecking typeChecker;
		CodeGeneration codeGeneration;
		
		outputWriter.writeOutput("Syntactic analysis ... ");
		System.out.println("Syntactic analysis ... ");
		try {
			packageAST = parser.parse();
			ASTDisplay astDisplay = new ASTDisplay();
			astDisplay.instantiateOutputWriter(outputWriter);
			astDisplay.showTree(packageAST);
//			new ASTDisplay().showTree(packageAST);
		} catch (Exception e) {}
		outputWriter.writeOutput("Syntactic analysis complete:  ");
		System.out.print("Syntactic analysis complete:  ");
		
		if (errorReporter.hasErrors()) {
			outputWriter.writeOutput("INVALID arithmetic expression");
			System.out.println("INVALID arithmetic expression");
			// return code for invalid input
//			System.exit(4);
		} else {
			outputWriter.writeOutput("Valid arithmetic expression");
			System.out.println("Valid arithmetic expression");
//				System.exit(0);
			try {
				idChecker = new Identification(packageAST, errorReporter);
				idChecker.beginCheck();
				if (errorReporter.hasErrors()) {
					outputWriter.writeOutput("INVALID arithmetic expression");
					System.out.println("INVALID arithmetic expression");
//				System.exit(4);
				} else {
					typeChecker = new TypeChecking(packageAST, errorReporter); 
					typeChecker.beginCheck();
					if (errorReporter.hasErrors()) {
						outputWriter.writeOutput("INVALID arithmetic expression");
						System.out.println("INVALID arithmetic expression");
						// return code for invalid input
//			System.exit(4);
					} else {
						outputWriter.writeOutput("Valid arithmetic expression");
						System.out.println("Valid arithmetic expression");
//				System.exit(0);
						try {
							codeGeneration = new CodeGeneration(packageAST, errorReporter);
							codeGeneration.begin();
						} catch (Exception e) {}
						
						
						String objectCodeFileName = args.replace(".java", ".mJAM");
						ObjectFile objF = new ObjectFile(objectCodeFileName);
						outputWriter.writeOutput("Writing object code file " + objectCodeFileName + " ... ");
						System.out.print("Writing object code file " + objectCodeFileName + " ... ");
						if (objF.write()) {
							errorReporter.reportError("*** Code Generation error: Writing object code file FAILED");
							return;
						} else {
							outputWriter.writeOutput("SUCCEEDED");
							System.out.println("SUCCEEDED");
						}
						
						String asmCodeFileName = objectCodeFileName.replace(".mJAM", ".asm");
						outputWriter.writeOutput("Writing assembly file " + asmCodeFileName + " ... ");
						System.out.print("Writing assembly file " + asmCodeFileName + " ... ");
						Disassembler d = new Disassembler(objectCodeFileName);
						if (d.disassemble()) {
							errorReporter.reportError("***Code Generation error: Writing assembly file FAILED");
							return;
						} else {
							outputWriter.writeOutput("SUCCEEDED");
							System.out.println("SUCCEEDED");
						}
						
						// Debugger 
						boolean runInterpreter = false;
						if (runInterpreter ) {
							System.out.println("Running code in debugger ... ");
							Interpreter.debug(objectCodeFileName, asmCodeFileName);
							System.out.println("*** mJAM execution completed");
						}
						
						if (errorReporter.hasErrors()) {
							outputWriter.writeOutput("INVALID arithmetic expression");
							System.out.println("INVALID arithmetic expression");
							// return code for invalid input
//			System.exit(4);
						} else {
							outputWriter.writeOutput("Valid arithmetic expression");
							System.out.println("Valid arithmetic expression");
//			System.exit(0);
						}
					}
					
				}
			} catch (Exception e) {}
			
		}
		try { // closing writer to compilerOutput file
			outputWriter.writer.close();
			errorReporter.writer.close();
//			errorReporter.codeToParse.delete();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
}



