package miniJava;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * reports errors from different phases of compilation
 * and maintains a count of total errors for use in 
 * the compiler driver
 *
 */
public class ErrorReporter {

		private int numErrors;
		public File compilerOutput = new File("CompilerOutputFiles/compilerOutput.txt");
		protected BufferedWriter writer;
		
		public ErrorReporter() {
			numErrors = 0;
			try {
				writer = new BufferedWriter(new FileWriter("CompilerOutputFiles/compilerOutput.txt", true));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public boolean hasErrors() {
			return numErrors > 0;
		}

		public void reportError(String message) {
			try {
				writer.write(message);
				writer.write("\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println(message);
			numErrors++;
		}	
}
