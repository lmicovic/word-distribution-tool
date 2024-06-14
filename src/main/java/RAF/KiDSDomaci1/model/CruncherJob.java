package RAF.KiDSDomaci1.model;

public class CruncherJob {
	
	private Cruncher cruncher;
	private Directory directory;
	private String data;
	
	private Integer start;
	private Integer end;
	
	private boolean poison;
	
	public CruncherJob(Directory directory, String data) {
		this.directory = directory;
		this.data = data;
		this.poison = false;
	}

	public CruncherJob(boolean poison) {
		this.poison = poison;
	}
	
	public CruncherJob(Cruncher cruncher, Directory directory, String data, Integer start, Integer end) {
		this.cruncher = cruncher;
		this.directory = directory;
		this.data = data;
		this.start = start;
		this.end = end;
	}
	
	public Directory getDirectory() {
		return directory;
	}
	
	public String getData() {
		return data;
	}
	
	@Override
	public String toString() {
		return directory.getFile().getName();
	}
	
	public boolean getPoison() {
		return poison;
				
	}
	
	public Integer getStart() {
		return start;
	}
	
	public Integer getEnd() {
		return end;
	}
	
	public void setStart(int start) {
		this.start = start;
	}
	
	public void setEnd(int end) {
		this.end = end;
	}
	
	public void setCruncher(Cruncher cruncher) {
		this.cruncher = cruncher;
	}
	
	public Cruncher getCruncher() {
		return cruncher;
	}
	
}
