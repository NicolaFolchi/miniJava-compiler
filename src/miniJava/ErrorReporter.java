package miniJava;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * reports errors from different phases of compilation
 * and maintains a count of total errors for use in 
 * the compiler driver
 *
 */
public class ErrorReporter implements AutoCloseable {

		private int numErrors;
		protected BufferedWriter writer;
		private boolean ownsWriter;
		
		public ErrorReporter() {
			try {
				initialize(Paths.get("CompilerOutputFiles/compilerOutput.txt"));
			} catch (IOException e) {
				throw new UncheckedIOException("Unable to open compiler error output", e);
			}
		}

		public ErrorReporter(Path outputPath) throws IOException {
			initialize(outputPath);
		}

		public ErrorReporter(OutputWriter outputWriter) {
			numErrors = 0;
			writer = outputWriter.writer;
			ownsWriter = false;
		}

		private void initialize(Path outputPath) throws IOException {
			numErrors = 0;
			ownsWriter = true;
			Path parent = outputPath.toAbsolutePath().getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}
			writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8,
					StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		}

		public boolean hasErrors() {
			return numErrors > 0;
		}

		public void reportError(String message) {
			try {
				writer.write(message);
				writer.write("\n");
				writer.flush();
			} catch (IOException e) {
				throw new UncheckedIOException("Unable to write compiler error output", e);
			}
			System.out.println(message);
			numErrors++;
		}

		@Override
		public void close() throws IOException {
			if (ownsWriter) {
				writer.close();
			} else {
				writer.flush();
			}
		}
}
