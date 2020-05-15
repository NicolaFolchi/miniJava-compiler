package miniJava;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CompilerServlet extends HttpServlet {
	private static final int BUFSIZE = 4096;

	public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
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
		// Starting the compiler execution
		Compiler.run(codeFilePath);

		File compilerOutput = new File("compilerOutput.txt");
		
		/**
		 * The following commented code would be used if I want my http response to be provided in a separate page
		 */
//		int length = 0;
//		ServletOutputStream outStream = res.getOutputStream();
//		ServletContext context = getServletConfig().getServletContext();
//
//		// sets HTTP header
//		res.setHeader("Content-Disposition", "inline");
//
//		byte[] byteBuffer = new byte[BUFSIZE];
//		DataInputStream in = new DataInputStream(new FileInputStream(compilerOutput));
//
//		// reads the file's bytes and writes them to the response stream
//		while ((in != null) && ((length = in.read(byteBuffer)) != -1)) {
//			outStream.write(byteBuffer, 0, length);
//		}
//
//		
//		in.close();
//		outStream.close();

		req.setAttribute("compilerOutput", compilerOutput);
		
		req.getRequestDispatcher("/index.jsp").forward(req, res);
	}
	
	public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
		File mjamInstructions = new File("code.txt.asm");
		int length = 0;
		ServletOutputStream outStream = res.getOutputStream();
		ServletContext context = getServletConfig().getServletContext();

		// sets HTTP header
		res.setHeader("Content-Disposition", "attachment;filename=code.txt.asm");

		byte[] byteBuffer = new byte[BUFSIZE];
		DataInputStream in = new DataInputStream(new FileInputStream(mjamInstructions));

		// reads the file's bytes and writes them to the response stream
		while ((in != null) && ((length = in.read(byteBuffer)) != -1)) {
			outStream.write(byteBuffer, 0, length);
		}

		
		in.close();
		outStream.close();
	}
	
	
}
