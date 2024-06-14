package RAF.KiDSDomaci1.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import RAF.KiDSDomaci1.app.App;
import RAF.KiDSDomaci1.view.MainView;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;

public class CruncherTask implements Runnable {
	
	private Cruncher cruncher;
	private CruncherJob cruncherJob;
	
	private Result result;
	
	public CruncherTask(Cruncher cruncher, CruncherJob cruncherJob, Result result) {
		this.cruncher = cruncher;
		this.cruncherJob = cruncherJob;
		this.result = result;
	}
	
	@Override
	public void run() {

		Map<String, Integer> resultMap = processData(cruncherJob.getData(), cruncherJob.getStart(), cruncherJob.getEnd(), cruncher.getArity());		
		
	}
	
	private Map<String, Integer> processData(String data, int begin, int end, int arity) {
		
		try {
			
			// Referenca na result u Result objektu
			Map<String, Integer> resultMap = result.getResult();

			int count = arity;
			
			// i - trenutna pozicija karaktera
			for(int i = begin; arity == 1? i <= end : i <= end - arity; i++) {
				
				int j;
				int start = i;
				
				// Trazimo substring po airtiju
				for(j = i; i <= end && count > 0; j++) {
					
					// Ako je j dosao do kraja
					if(j == data.length()-1) {
						count--;
						break;
					}
					
					// Ako je j space
					if(data.charAt(j) == ' ') {
						
						if(count == arity) {
							start = j;
						}	
						count--;
					}
					
				}
				
				// Pronasli smo pozicije - start, end
				
				// Bag
				List<String> words = Arrays.asList((data.substring(i,j)).replace("\n", "").split(" "));
				
				// Treba lock zato sto je radimo sa mapom Result sa kojom i rade druge niti. Ta mapa se vec nalazi u FileOutputTask results
				synchronized (result.getResultLock()) {
					
					if(arity == 1) {
						// Process words	
						resultMap = processWords(resultMap, words);
					}
					// Poslednji ce da preskoci
					else if(i + arity < data.length() && count == 0) {
//						System.out.println(words);
						resultMap = processWords(resultMap, words);
					}
					
				}

				i = start;
				count = arity;		
				
			}
			
			return resultMap;
			
		} catch (OutOfMemoryError e) {
			MainView.OutOfMemory(e);
		}
		
		return null;
		
		
	}
	
	
	// Result: map
	// List: reci
	// Proverava da li se u mapi nalaze reci iz list i dodaje rezultat
	// Spajaju se rezultati sa rezultatima drugih niti odjenom zato sto je prosledjena referenca iz Result
	public Map<String, Integer> processWords(Map<String, Integer> map, List<String> list) { 
		
		Collections.sort(list);		// sortiramo reci
		
		String key = String.join(" ", list); // Pravimo String od reci

			if(map.containsKey(key)) {
				map.put(key, map.get(key) + 1);
			}
			else {
				map.put(key, 1);
			}
			
		return map;
	}
	
	private synchronized void writeLog(String log) {
		
		try {
			
			File logFile = new File("./log/" + cruncher.toString().toLowerCase().replace(" ", "-") + ".txt");
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
	
	private synchronized void startCruncherTaskLog() {
		
		String log = "";
		log += "		-----------------------------------------------------------------\n";
		log += "		|CruncherTask| - START - " + cruncher + " Arity: " + cruncher.getArity() + "     " + App.getCurrentDate() + "\n";
		log += "		-----------------------------------------------------------------\n";
		log += "		Cruncher: " + cruncher.getName() + "\n";
		log += "		Arity: " + cruncher.getArity() + "\n";
		log += "		CruncherTaskID: " + Thread.currentThread().getName() + "\n";
		log += "		Processing File: " + cruncherJob.getDirectory().getFile().getName() + "\n";
		log += "		Data Characters: " + cruncherJob.getData().length() + "\n";
		log += "		Processing FROM: " + cruncherJob.getStart() + "\n";
		log += "		Processing TO: " + cruncherJob.getEnd() + "\n";
		log += "		-----------------------------------------------------------------\n\n";
		
		writeLog(log);
	}
	
	private synchronized void stopCruncherLog() {
		
		String log = "";
		log += "		-----------------------------------------------------------------\n";
		log += "		|Cruncher| - STOP -  " + cruncher + " Arity: " + cruncher.getArity() + "     " + App.getCurrentDate() + "\n";
		log += "		-----------------------------------------------------------------\n";
		log += "		CruncherTaskID: " + Thread.currentThread().getName() + "\n";
		log += "		-----------------------------------------------------------------\n";
		
		writeLog(log);
		
	}
	
	// Posion pill
	public void stop() {
		try {
			cruncher.getCruncherJobs().put(new CruncherJob(true));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public Cruncher getCruncher() {
		return cruncher;
	}	
	
}
