package RAF.KiDSDomaci1.model;

public class CruncherResult implements Comparable<CruncherResult>{
	
	private String word;
	private Integer count;
	
	public CruncherResult(String word, Integer count) {
		this.word = word;
		this.count = count;
	}
	
	public String getWord() {
		return word;
	}
	
	public  Integer getCount() {
		return count;
	}
	
	public void setCount(Integer count) {
		this.count = count;
	}
	
	@Override
	public int compareTo(CruncherResult o) {

		if(this.getCount() > o.getCount()) {
			return 1;
		}
		
		if(this.getCount() < o.getCount()) {
			return -1;
		}
		
		return 0;
	}
	
	@Override
	public String toString() {

		return word + ": " + count;
	}
	
	
}
