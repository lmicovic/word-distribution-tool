package RAF.KiDSDomaci1.model;

import java.io.File;

public class Directory {
	
	private File file;
	private Long currentLastModified;
	
	public Directory(File file) {
		this.file = file;
		this.currentLastModified = file.lastModified();
	}
	
	public File getFile() {
		return file;
	}
	
	public Long getCurrentLastModified() {
		return currentLastModified;
	}
	
	public boolean isModified() {
		
		if(currentLastModified != file.lastModified()) {
			return true;
		}
		
		return false;
	}
	
	public void setModified() {
		this.currentLastModified = file.lastModified();
	}
	
	@Override
	public String toString() {
		return file.getAbsolutePath();
	}
	
}
