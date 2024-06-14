package RAF.KiDSDomaci1.model;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import RAF.KiDSDomaci1.app.App;
import RAF.KiDSDomaci1.app.Config;
import RAF.KiDSDomaci1.view.CruncherView;
import RAF.KiDSDomaci1.view.MainView;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;


public class Cruncher implements Runnable {
	
	private static AtomicInteger counter = new AtomicInteger(1);
	
	private int arity;
	private String cruncherName;
	
	private ExecutorService cruncherWorkerPool;
	
	private BlockingQueue<CruncherJob> cruncherJobs;		// Jobs for This cruncher
	
	private CruncherView cruncherView;
	
	private FileOutputTask linkedFileOutputTask;
	
	private volatile List<CruncherWorker> activeCruncherWorker;
	
	public Cruncher(int arity, FileOutputTask linkedFileOutputTask) {
		this.arity = arity;
		this.cruncherName = "Cruncher " + String.valueOf(counter.getAndIncrement());
		this.linkedFileOutputTask = linkedFileOutputTask;
		this.cruncherWorkerPool = Executors.newCachedThreadPool();
		this.cruncherJobs = new LinkedBlockingQueue<CruncherJob>();
		this.activeCruncherWorker = new CopyOnWriteArrayList<CruncherWorker>();
		
		MainView.activeCruncherTasks.incrementAndGet();
		
	}
	
	@Override
	public void run() {
		
		startCruncherLog();
		
		while(true) {
			
			try {
				
				CruncherJob currentCruncherJob = cruncherJobs.take();
				
				// Exit
				if(currentCruncherJob.getPoison()) {
					break;
				}
				
				CruncherWorker cruncherWorker = new CruncherWorker(this, currentCruncherJob, linkedFileOutputTask);
				activeCruncherWorker.add(cruncherWorker);
				
				cruncherWorkerPool.submit(cruncherWorker);
					
			} catch (Exception e) {
				e.printStackTrace();
			}	
		}
		
		System.err.println(activeCruncherWorker.size());
		MainView.activeCruncherTasks.decrementAndGet();
		

	}
	
	
	
	private void stopCruncherLog() {
		
		String log = "";
		log += "-----------------------------------------------------------------\n";
		log += "|Cruncher| - STOP -  " + this.getName() + " Arity: " + this.getArity() + "     " + App.getCurrentDate() + "\n";
		log += "-----------------------------------------------------------------\n";
		
		writeLog(log);
		
	}
	
	public void startCruncherLog() {
		
		File logFile = new File("./log/" + this.getName().toLowerCase().replace(" ", "-") + ".txt");
		
		try {
			if(logFile.exists()) {
				logFile.delete();
				logFile.createNewFile();
			}
			else {
				logFile.createNewFile();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		String log = "";
		log += "-----------------------------------------------------------------\n";
		log += "|Cruncher| - START - " + this.getName() + " Arity: " + this.getArity() + "     " + App.getCurrentDate() + "\n";
		log += "-----------------------------------------------------------------\n\n";
		
		writeLog(log);
		
	}
	
	private void writeLog(String log) {
			
		try {
			
			File logFile = new File("./log/" + this.getName().toLowerCase().replace(" ", "-") + ".txt");
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
	
	public void addCruncherJob(CruncherJob cruncherJob) {
		this.cruncherJobs.add(cruncherJob);
	}
	
	public BlockingQueue<CruncherJob> getCruncherJobs() {
		return cruncherJobs;
	}
	
	public String getName() {
		return cruncherName;
	}
	
	@Override
	public String toString() {
		return cruncherName;
	}
	
	public int getArity() {
		return arity;
	}
	
	public void setCruncherView(CruncherView cruncherView) {
		this.cruncherView = cruncherView;
	}
	
	public List<CruncherWorker> getActiveCruncherWorker() {
		return activeCruncherWorker;
	}
	
	public CruncherView getCruncherView() {
		return cruncherView;
	}
	
	
	
	public void stop() {
		this.cruncherJobs.add(new CruncherJob(true));
		
		if(!MainView.memory.get()) {
			while(activeCruncherWorker.size() > 0) {
				
			}
		}
			
		this.cruncherWorkerPool.shutdown();
		stopCruncherLog();
	}
	
}
