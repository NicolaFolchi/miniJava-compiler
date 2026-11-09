package miniJava;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class OutputWriter implements AutoCloseable {
	protected BufferedWriter writer;

	public OutputWriter() {
		try {
			writer = open(Paths.get("CompilerOutputFiles/compilerOutput.txt"), true);
		} catch (IOException e) {
			throw new UncheckedIOException("Unable to open compiler output", e);
		}
	}

	public OutputWriter(Path outputPath) throws IOException {
		writer = open(outputPath, false);
	}

	private BufferedWriter open(Path outputPath, boolean append) throws IOException {
		Path parent = outputPath.toAbsolutePath().getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}
		if (append) {
			return Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8,
					StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		}
		return Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8,
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
	}
	
	public void writeOutput(String message) {
		try {
			writer.write(message);
			writer.write("\n");
			writer.flush();
		} catch (IOException e) {
			throw new UncheckedIOException("Unable to write compiler output", e);
		}
	}

	@Override
	public void close() throws IOException {
		writer.close();
	}
}
