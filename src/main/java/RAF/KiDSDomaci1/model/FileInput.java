package RAF.KiDSDomaci1.model;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import RAF.KiDSDomaci1.view.FileInputTask;

public class FileInput {
	
	public static AtomicInteger counter = new AtomicInteger(1);		// Koristi se za naziv FileInput-a.
	
	private Disk disk;
	private String name;
	
	public FileInput(Disk disk) {
		this.name = String.valueOf(counter.getAndIncrement());
		this.disk = disk;
	}
	
	public Disk getDisk() {
		return disk;
	}
	public String getName() {
		return name;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
}
