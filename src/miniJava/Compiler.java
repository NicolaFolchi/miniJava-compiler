/**
 * miniJava Compiler class
 * @author Nicola Folchi
 */
package miniJava;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

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
		try {
			run(Paths.get(args), Paths.get("CompilerOutputFiles/compilerOutput.txt"));
		} catch (IOException e) {
			throw new UncheckedIOException("Compiler execution failed", e);
		}
	}

	public static boolean run(Path sourcePath, Path outputPath) throws IOException {
		try (InputStream inputStream = new FileInputStream(sourcePath.toFile());
				OutputWriter outputWriter = new OutputWriter(outputPath);
				ErrorReporter errorReporter = new ErrorReporter(outputWriter)) {
		return run(sourcePath, inputStream, outputWriter, errorReporter);
		}
	}

	private static boolean run(Path sourcePath, InputStream inputStream, OutputWriter outputWriter,
			ErrorReporter errorReporter) {
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
		} catch (UncheckedIOException e) {
			throw e;
		} catch (Exception e) {
			if (!errorReporter.hasErrors()) {
				errorReporter.reportError("*** Internal compiler error during syntactic analysis");
			}
		}
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
						} catch (UncheckedIOException e) {
							throw e;
						} catch (Exception e) {
							errorReporter.reportError("*** Internal compiler error during code generation");
						}
						
						if (!errorReporter.hasErrors()) {
							outputWriter.writeOutput("\n");
							String objectCodeFileName = sourcePath.toString().replace(".java", ".mJAM");
							ObjectFile objF = new ObjectFile(objectCodeFileName);
							outputWriter.writeOutput("Writing object code file " + Paths.get(objectCodeFileName).getFileName() + " ... ");
							System.out.print("Writing object code file " + objectCodeFileName + " ... ");
							if (objF.write()) {
								errorReporter.reportError("*** Code Generation error: Writing object code file FAILED");
								return false;
							} else {
								outputWriter.writeOutput("SUCCEEDED");
								System.out.println("SUCCEEDED");
							}

							String asmCodeFileName = objectCodeFileName.replace(".mJAM", ".asm");
							outputWriter.writeOutput("Writing assembly file " + Paths.get(asmCodeFileName).getFileName() + " ... ");
							System.out.print("Writing assembly file " + asmCodeFileName + " ... ");
							Disassembler d = new Disassembler(objectCodeFileName);
							if (d.disassemble()) {
								errorReporter.reportError("***Code Generation error: Writing assembly file FAILED");
								return false;
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
					
				}
			} catch (UncheckedIOException e) {
				throw e;
			} catch (Exception e) {
				errorReporter.reportError("*** Internal compiler error during contextual analysis");
			}
			
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
		return !errorReporter.hasErrors();
	}
	
}
