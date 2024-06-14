package RAF.KiDSDomaci1.model;

import java.io.File;

/**
 * Predstavlja disk koji treba da se procita: Disk1(A,B), Disk2(C,D)
 */
public class Disk {
	
	private File directory;
	
	public Disk(File directory) {
		this.directory = directory;
	}
	
	public File getDirectory() {
		return directory;
	}
	
	@Override
	public String toString() {
		return directory.toPath().toString();
	}
}

