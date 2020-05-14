package miniJava;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class OutputWriter {
	protected BufferedWriter writer;

	public OutputWriter() {
		File compilerOutput = new File("compilerOutput.txt");
		if (compilerOutput.exists()) {
			compilerOutput.delete();
		}
		try {
			compilerOutput.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			writer = new BufferedWriter(new FileWriter("compilerOutput.txt", true));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeOutput(String message) {
		try {
			writer.write(message);
			writer.write("\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
//		System.out.println(message);
	}
}
