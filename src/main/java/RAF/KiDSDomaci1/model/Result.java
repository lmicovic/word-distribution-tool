package RAF.KiDSDomaci1.model;

import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class Result {
	
	private Cruncher cruncher;
	private String resultName;
	private Map<String, Integer> result;
	private Object resultLock = new Object();
	
	private boolean poison = false;
	
	private volatile AtomicBoolean done = new AtomicBoolean(false);

	
	public Result(boolean poison) {
		this.poison = poison;
	}
	
	public Result(String resultFileName) {
		this.resultName = resultFileName;
		this.result = new ConcurrentHashMap<String, Integer>();
	}
	
	public Result(String resultFileName, Cruncher cruncher) {
		this.resultName = resultFileName + "-arity" + cruncher.getArity();
		this.cruncher = cruncher;
		this.result = new ConcurrentHashMap<String, Integer>();
		this.done = new AtomicBoolean(false);
	}
	
	
	
	
	
	// Mozda ne radi ???
//	public static Result mergeResults(Result result1, Result result2) {
//		
//		Map<List<String>, Integer> mergedResult = mergeAndAdd(result1.getResult(), result2.getResult());
//		Result newResult = new Result(result1.getResultName(), result1.getCruncher(), mergedResult);
//		
//		return newResult;
//		
//	}
	
	public static <K> Map<K, Integer> mergeAndAdd(Map<K, Integer>... maps) {
	    Map<K, Integer> result = new HashMap<>();
	    for (Map<K, Integer> map : maps) {
	        for (Map.Entry<K, Integer> entry : map.entrySet()) {
	            K key = entry.getKey();
	            Integer current = result.get(key);
	            result.put(key, current == null ? entry.getValue() : entry.getValue() + current);
	        }
	    }
	    return result;
	}
	
	public void setResultName(String resultName) {
		this.resultName = resultName;
	}
	
	public String getResultName() {
		return resultName;
	}
	
	public Cruncher getCruncher() {
		return cruncher;
	}
	
	public Map<String, Integer> getResult() {
		return result;
	}
	
	public void setResult(Map<String, Integer> result) {
		this.result = result;
	}
	
	public boolean getPoison() {
		return poison;
	}
	
	@Override
	public boolean equals(Object other) {
		return this.resultName.equals(((Result)other).getResultName());
	}
	
	public boolean isDone() {
		return done.get();
	}
	
	public void setDone(boolean done) {
		this.done.set(done);
	}
	
	
	
	
	public static Map<List<String>, Integer> sortResult(Map<List<String>, Integer> result, int limit) {
		Map<List<String>, Integer> finalRresult =
				result.entrySet().stream()
			       .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
			       .limit(limit)	// Top 10 result
			       .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		
		return finalRresult;
	}
	
	public Object getResultLock() {
		return resultLock;
	}
	
	
}