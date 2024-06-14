package RAF.KiDSDomaci1.model;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

public class ResultObject {

	// Koji je Cruncher povezan sa rezultatom
	private Cruncher cruncher;
	private boolean poison;
	
	private String resultFileName;
	
	// Ovde ce se nalaziti delovi rezultata za odredjeni fajl
	
	// List: Lista futura za odrejeni fileName
	// Map<Key: <lista reci>, Value: broj pojavljivanja>
	private List<Future<Map<List<String>, Integer>>> futureResults;
	
	public ResultObject(String resultFileName, Cruncher cruncher) {
		this.resultFileName = resultFileName;
		this.cruncher = cruncher;
		this.poison = false;
		this.futureResults = new CopyOnWriteArrayList<Future<Map<List<String>,Integer>>>();
	}
	
	// Exit
	public ResultObject(boolean poison) {
		this.resultFileName = "";
		this.cruncher = null;
		this.futureResults = null;
		this.poison = poison;
	}
	
	public Cruncher getCruncher() {
		return cruncher;
	}
	
	public String getResultFileName() {
		return resultFileName;
	}
	
	public boolean isPoison() {
		return poison;
	}
	
	public void addFutureResult(Future<Map<List<String>, Integer>> futureResult) {
		this.futureResults.add(futureResult);
	}
	
	@Override
	public String toString() {
		
		if(poison == false) {
			return "Result: [" + resultFileName + ", " + cruncher.getArity() + ", " + futureResults.size() + "]";
		}
		
		else {
			return "Result: poison: " + poison; 
		}
		
	}
	
}
