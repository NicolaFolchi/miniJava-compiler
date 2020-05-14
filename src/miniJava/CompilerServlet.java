package miniJava;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CompilerServlet extends HttpServlet {
	
	public void service(HttpServletRequest req, HttpServletResponse res) throws IOException {
		System.out.println("found it");
		String projectDir = System.getProperty("user.dir");
//		System.out.println(projectDir);
		File codeToParse = new File("code.txt");
		if (codeToParse.exists()) {
			codeToParse.delete();
		}
		try {
			codeToParse.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			FileWriter myWriter = new FileWriter("code.txt");
			myWriter.write(req.getParameter("code"));
			myWriter.close();
			System.out.println("Successfully wrote to the file.");
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
		String codeFilePath = codeToParse.getAbsolutePath();
		System.out.println(codeFilePath);
		Compiler.run(codeFilePath);
		
		PrintWriter out = res.getWriter();
		File compilerOutput = new File("compilerOutput.txt");
		
		out.println(compilerOutput);
		out.close();
	}
	
	
}
