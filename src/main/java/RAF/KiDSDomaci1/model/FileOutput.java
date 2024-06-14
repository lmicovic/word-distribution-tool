package RAF.KiDSDomaci1.model;

import java.util.concurrent.atomic.AtomicInteger;

public class FileOutput {
	
	private static AtomicInteger counter = new AtomicInteger(1);
	private String outputName;
	
	
	
	public FileOutput() {
		this.outputName = "Output-" + counter.getAndIncrement();
	}
	
	public String getFileOutputName() {
		return outputName;
	}
	
	
}
