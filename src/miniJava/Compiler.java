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
		
		outputWriter.writeOutput("Initiating Compiler ... \n");
		outputWriter.writeOutput("Syntactic analysis ... ");
		System.out.println("Syntactic analysis ... ");
		try {
			packageAST = parser.parse();
			outputWriter.writeOutput("\n");
			outputWriter.writeOutput("Program Abstract Syntax Tree: ");
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
				outputWriter.writeOutput("\n");
				outputWriter.writeOutput("Contextual analysis ... ");
				System.out.println("Contextual analysis ... ");
				
				idChecker = new Identification(packageAST, errorReporter);
				idChecker.beginCheck();
				
				outputWriter.writeOutput("Contextual analysis complete:  ");
				System.out.print("Contextual analysis complete:  ");
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
							outputWriter.writeOutput("\n");
							outputWriter.writeOutput("Code Generation ... ");
							System.out.println("Code Generation ... ");
							codeGeneration = new CodeGeneration(packageAST, errorReporter);
							codeGeneration.begin();
							outputWriter.writeOutput("Code Generation complete:  ");
							System.out.print("Code Generation complete:  ");
						} catch (Exception e) {}
						
						outputWriter.writeOutput("\n");
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
					}
					
				}
			} catch (Exception e) {}
			
		}
		outputWriter.writeOutput("\n");
		outputWriter.writeOutput("Result: \n");
		
		if (errorReporter.hasErrors()) {
			outputWriter.writeOutput("INVALID MiniJava Program!");
			System.out.println("INVALID MiniJava Program!");
		} else {
			outputWriter.writeOutput("Valid MiniJava Program!");
			System.out.println("Valid MiniJava Program!");
		}
		try { // closing writer to compilerOutput file
			outputWriter.writer.close();
			errorReporter.writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
}



