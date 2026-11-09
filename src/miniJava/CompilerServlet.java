package miniJava;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class CompilerServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final int MAX_SOURCE_LENGTH = 256 * 1024;
	private static final String ASSEMBLY_SESSION_ATTRIBUTE = "mjamInstructions";
	private final CompilerService compilerService = new CompilerService();

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		request.setCharacterEncoding(StandardCharsets.UTF_8.name());
		String sourceCode = request.getParameter("code");
		request.setAttribute("sourceCode", sourceCode);
		HttpSession existingSession = request.getSession(false);
		if (existingSession != null) {
			existingSession.removeAttribute(ASSEMBLY_SESSION_ATTRIBUTE);
		}

		if (sourceCode == null) {
			forwardError(request, response, HttpServletResponse.SC_BAD_REQUEST,
					"No MiniJava source code was submitted.");
			return;
		}
		if (sourceCode.trim().isEmpty()) {
			forwardError(request, response, HttpServletResponse.SC_BAD_REQUEST,
					"MiniJava source code must not be empty.");
			return;
		}
		if (sourceCode.length() > MAX_SOURCE_LENGTH) {
			forwardError(request, response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
					"MiniJava source code exceeds the 256 KiB limit.");
			return;
		}

		HttpSession session = request.getSession();
		try {
			CompilationResult result = compilerService.compile(sourceCode);
			request.setAttribute("compilerOutput", result.getCompilerOutput());
			if (result.hasAssembly()) {
				session.setAttribute(ASSEMBLY_SESSION_ATTRIBUTE, result.getAssembly());
			}
			request.getRequestDispatcher("/index.jsp").forward(request, response);
		} catch (IOException | RuntimeException e) {
			getServletContext().log("MiniJava compilation failed", e);
			forwardError(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"The compiler could not process this request. Please try again.");
		}
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		HttpSession session = request.getSession(false);
		byte[] assembly = session == null ? null : (byte[]) session.getAttribute(ASSEMBLY_SESSION_ATTRIBUTE);
		if (assembly == null || assembly.length == 0) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND,
					"No generated mJAM instructions are available for this session.");
			return;
		}

		response.setContentType("text/plain");
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setHeader("Content-Disposition", "attachment; filename=\"code.asm\"");
		response.setContentLength(assembly.length);
		session.removeAttribute(ASSEMBLY_SESSION_ATTRIBUTE);
		response.getOutputStream().write(assembly);
	}

	private void forwardError(HttpServletRequest request, HttpServletResponse response, int status,
			String message) throws ServletException, IOException {
		response.setStatus(status);
		request.setAttribute("compilerOutput", message);
		request.getRequestDispatcher("/index.jsp").forward(request, response);
	}
}
