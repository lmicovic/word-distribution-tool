package RAF.KiDSDomaci1.model;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;



import RAF.KiDSDomaci1.app.App;
import RAF.KiDSDomaci1.view.MainView;
import javafx.application.Platform;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Alert.AlertType;

public class FileOutputTask implements Runnable{

	private FileOutput fileOutput;
	private LinkedBlockingQueue<Result> queue;	
	private List<Result> results;
	
	public FileOutputTask(FileOutput fileOutput) {
		this.fileOutput = fileOutput;
		this.queue = new LinkedBlockingQueue<Result>();
		this.results = new CopyOnWriteArrayList<Result>();
		
		MainView.activeOutputTasks.incrementAndGet();
		
	}
	
	@Override
	public void run() {

		while(true) {
			try {
				
				Result result = queue.take();
				
				if(result.getPoison()) {
					break;
				}
				
				results.add(result);
				
			} catch (InterruptedException e) {
				e.printStackTrace();
			}	
		}
		
		
		MainView.activeOutputTasks.decrementAndGet();
	}
	
	// Nije blokirajuci
	public void getSingleResult(String resultName) {
		
		if(resultName.startsWith("*")) {
			alert("Selected result is in processing.");	
			return;
		}
		else {
			
			// Ako vrati null rezultat ne postoji.
			// Ako vrati rezultat rezultat je gotov
			Result result = poll(resultName);
			
			// Ako selektovani rezultat ne postoji
			if(result == null) {
				alert("Result " + resultName + " doesnt exist");
				return;
			}
			
			// Ako postoji 
			MainView.fileOutputPool.execute(new OutputSingleResultWorker(this, result));
	
		}
	}
	
	// Blokirajuci
	public void getSumResult(String resultName, List<String> selectedResults) {
		
		// Proveravamo ako postoji rezultat vec sa prosledjenim imenom
		Result newResult = new Result(resultName);
		if(results.contains(newResult)) {
			alert("Result with name " + resultName + " already exist.\nTry other name.");
			return;
		}
		
		MainView.fileOutputPool.execute(new OutputSumResultWorker(resultName, this, selectedResults));
		
		
	}
	
	// Ne blokirajuci
	public Result poll(String resultName) {
		
		Result selectedResult = new Result(resultName);
		Result result = null;
		
		// Ako ne postoji rezultat
		if(!results.contains(selectedResult)) {
			return null;
		}
		else {
			result = results.get(results.indexOf(selectedResult));
			if(result.isDone()) {
				return result;
			}
		}
		
		return null;
		
	}

	// Blokirajuci
	public Result take(String resultName) {
		
		Result selectedResult = new Result(resultName);
		Result result = null;
		
		if(!results.contains(selectedResult)) {
			return null;
		}
		else {
			result = results.get(results.indexOf(selectedResult));
			
			// Ako rezulat nije spreman cekamo
			while(!result.isDone()) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			return result;
		}
		
	}
	
	private void alert(String text) {
		
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				
				Alert alert = new Alert(AlertType.INFORMATION);
				alert.setTitle("Information");
				alert.setHeaderText(text);
				
				alert.show();
			}
		});
		
	}
	
	private void writeLog(Result result) {
		
		String log = "----------------------------------------------------------\n";
		log += "|FileOutput| - Task - " + result.getResultName() + "       " + App.getCurrentDate() + "\n";
		log += "----------------------------------------------------------\n";
		log += "CruncherName: " + result.getCruncher() + "\n";
		log += "Cruncher Arity: " + result.getCruncher().getArity() + "\n\n";
		
		Map<String, Integer> res = result.getResult();
		// Top 10 results
		res =
				res.entrySet().stream()
			       .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
			       .limit(10)	// Top 10 result
			       .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		
		log += "Top 10 result:\n";
		
		int printTo = results.size() > 10? 10: results.size();
		for (Entry<String, Integer> entity : res.entrySet()) {
			if(printTo <= 0) {
				break;
			}
			log += entity.getKey() + ": " + entity.getValue() + "\n";
			printTo--;
		}
		
		log += "----------------------------------------------------------\n\n";
		
		writeFile(log);
		
	}
	
	private void writeStopLog() {
		
		String log = "----------------------------------------------------------\n";
		log += "|FileOutput| - STOP - " + fileOutput.getFileOutputName() + "       " + App.getCurrentDate() + "\n";
		log += "----------------------------------------------------------\n\n";
		
		writeFile(log);
		
	}
	
	private void writeStartLog() {
		
		try {

			File file = new File("log\\" + fileOutput.getFileOutputName().toLowerCase() + ".txt");
			if(file.exists()) {
				file.delete();
			}
			
			file.createNewFile();
			
			String log = "----------------------------------------------------------\n";
			log += "|FileOutput| - START - " + fileOutput.getFileOutputName() + "       " + App.getCurrentDate() + "\n";
			log += "----------------------------------------------------------\n\n";
			
			writeFile(log);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}
	
	private void writeFile(String log) {
		
		try {
			
			File logFile = new File("log\\" + fileOutput.getFileOutputName().toLowerCase() + ".txt");
			if(!logFile.exists()) {
				logFile.createNewFile();
			}
			
			
			
			FileWriter fw = new FileWriter(logFile, true);
			BufferedWriter bw = new BufferedWriter(fw);
			
			bw.write(log);
			
			bw.close();
			fw.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public List<Result> getResults() {
		return results;
	}

	public void addToResults(Result result)  {
		results.add(result);			
	}
	
	public void removeResultByName(Result result) {
		
		try {
			
			if(results.contains(result)) {
				for (Result r : results) {
					if(r.getResultName().equals(result.getResultName())) {
						
						// Remove from Model
						int resultIdx = results.indexOf(r);
						results.remove(resultIdx);
					}
				}
			}
			else {
				throw new Exception("Result: " + result.getResultName() + " doesnt exist.");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	
	public void addResult(Result result) {
		this.queue.add(result);
	}
	
	public void stop() {
		this.queue.add(new Result(true));
	}
	
	
}
