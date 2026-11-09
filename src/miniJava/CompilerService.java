package miniJava;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

public class CompilerService {
	// mJAM.Machine uses a process-wide static code store, so compiler execution must be serialized.
	private static final ReentrantLock COMPILATION_LOCK = new ReentrantLock();

	public CompilationResult compile(String sourceCode) throws IOException {
		Path compilationDirectory = Files.createTempDirectory("minijava-compile-");
		Path sourcePath = compilationDirectory.resolve("code.java");
		Path outputPath = compilationDirectory.resolve("compiler-output.txt");
		Path assemblyPath = compilationDirectory.resolve("code.asm");

		try {
			Files.write(sourcePath, sourceCode.getBytes(StandardCharsets.UTF_8));
			COMPILATION_LOCK.lock();
			try {
				boolean successful = Compiler.run(sourcePath, outputPath);
				String compilerOutput = new String(Files.readAllBytes(outputPath), StandardCharsets.UTF_8);
				byte[] assembly = successful && Files.exists(assemblyPath)
						? Files.readAllBytes(assemblyPath) : null;
				return new CompilationResult(compilerOutput, assembly, successful);
			} finally {
				COMPILATION_LOCK.unlock();
			}
		} finally {
			deleteRecursively(compilationDirectory);
		}
	}

	private void deleteRecursively(Path directory) throws IOException {
		if (!Files.exists(directory)) {
			return;
		}
		try (Stream<Path> paths = Files.walk(directory)) {
			paths.sorted(Comparator.reverseOrder()).forEach(path -> {
				try {
					Files.deleteIfExists(path);
				} catch (IOException e) {
					throw new CleanupException(e);
				}
			});
		} catch (CleanupException e) {
			throw e.getCause();
		}
	}

	private static class CleanupException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		CleanupException(IOException cause) {
			super(cause);
		}

		@Override
		public synchronized IOException getCause() {
			return (IOException) super.getCause();
		}
	}
}
