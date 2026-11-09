package miniJava;

public final class CompilationResult {
	private final String compilerOutput;
	private final byte[] assembly;
	private final boolean successful;

	public CompilationResult(String compilerOutput, byte[] assembly, boolean successful) {
		this.compilerOutput = compilerOutput;
		this.assembly = assembly == null ? null : assembly.clone();
		this.successful = successful;
	}

	public String getCompilerOutput() {
		return compilerOutput;
	}

	public boolean hasAssembly() {
		return successful && assembly != null && assembly.length > 0;
	}

	public boolean isSuccessful() {
		return successful;
	}

	public byte[] getAssembly() {
		return assembly == null ? null : assembly.clone();
	}
}
