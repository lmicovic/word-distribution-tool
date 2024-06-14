package RAF.KiDSDomaci1.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import RAF.KiDSDomaci1.app.Config;
import RAF.KiDSDomaci1.view.MainView;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;



public class CruncherWorker implements Callable<Boolean>{
	
	private static AtomicInteger cruncherWorkerIdCounter = new AtomicInteger(0);
	private int id;
	
	private ExecutorService crucherTaskPool;
	
	private Cruncher cruncher;
	private CruncherJob currentCruncherJob;
	
	private FileOutputTask linkedFileOutputTask;
	
	public CruncherWorker(Cruncher cruncher, CruncherJob currentCruncherJob, FileOutputTask linkedFileOutputTask) {
		this.id = cruncherWorkerIdCounter.getAndIncrement();
		this.cruncher = cruncher;
		this.currentCruncherJob = currentCruncherJob;
		this.crucherTaskPool = Executors.newCachedThreadPool();
		this.linkedFileOutputTask = linkedFileOutputTask;
		
		
		
	}
	
	@Override
	public Boolean call() {
		try {
			
			// Dodamo u GUI da se obraduje file u Cruncher-u
			cruncher.getCruncherView().setStatus(currentCruncherJob.getDirectory().getFile().getName());
			
			// Prosledimo dobijeni CruncherJob a dobijemo podeljene poslove koje svaki posle CruncherTask treba da obradi
			List<CruncherJob> shareCruncherJobs = shareTasks(currentCruncherJob);
			List<Thread> cruncherTaskList = new ArrayList<Thread>(shareCruncherJobs.size());	// Umesto fork join pravimo iz ovog thread-a ostale thread-ove
			
			Result result = new Result("*" + currentCruncherJob.getDirectory().getFile().getName(), cruncher);
			linkedFileOutputTask.addResult(result);												// Dodajemo rezulat u FileoutputTask result.
			
			// Prolazimo kroz podeljene posove i dodeljujemo novim CruncherTask nititma.
			for (CruncherJob cruncherJob : shareCruncherJobs) {
				Thread cruncherTask = new Thread(new CruncherTask(cruncher, cruncherJob, result));
				cruncherTaskList.add(cruncherTask);
				cruncherTask.start();
			}
			
			// Cekamo da se zavrse svi tasovi - tako znamo da je neki fajl obradjen
			for (Thread cruncherTask : cruncherTaskList) {
				try {
					cruncherTask.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			// Kada se zavrse svi taskovi rezultat ce se nalaziti u Result
			System.err.println("Finished: " + currentCruncherJob.getDirectory().getFile().getName() + ": Result: " + result.getResult().size());
			
			// Zavrsena obrada rezultata
			result.setDone(true);
			
			Platform.runLater(new Runnable() {
				
				@Override
				public void run() {
					
					// Menjamo naziv rezultata u GUI
					MainView.results.getItems().set(MainView.results.getItems().indexOf(result.getResultName()), result.getResultName().substring(1));
					result.setResultName(result.getResultName().substring(1));
				}
			});
			
			cruncher.getCruncherView().removeStatus(currentCruncherJob.getDirectory().getFile().getName());

			
		} catch (OutOfMemoryError e) {
			e.printStackTrace();
		}
		
		cruncher.getActiveCruncherWorker().remove(getId());
		
		return true;
		
	}
	
	private List<CruncherJob> shareTasks(CruncherJob currentCruncherJob) {
		
		int task_limit = Integer.parseInt(Config.getProperty("counter_data_limit"));
		int limit_counter = task_limit;
		
		List<CruncherJob> cruncherJobs = new ArrayList<CruncherJob>();
		
		int cruncherCount = 0;
		
		int start = 0;
		for(int i = 0; i < currentCruncherJob.getData().length(); i++) {
			
			// Ako je doslo do kraja poslednji job moze da ima manje 
			if(i == currentCruncherJob.getData().length() - 1) {
				CruncherJob cruncherJob = new CruncherJob(cruncher, currentCruncherJob.getDirectory(), currentCruncherJob.getData(), start, i);
				cruncherJobs.add(cruncherJob);
				break;
			}
			
			// Ostali slucajevi trazi se okolina od task_limit
			if(limit_counter == 0) {
				
				// Ako je space onda netrazimo okolinu reci
				if(currentCruncherJob.getData().charAt(i) == ' ') {
					
					CruncherJob cruncherJob = new CruncherJob(cruncher, currentCruncherJob.getDirectory(), currentCruncherJob.getData(), start, i);
					cruncherJobs.add(cruncherJob);
					
					start = i;
					cruncherCount++;
					
				}
				
				// trazimo pocetak sledece reci
				else {
					for(int j = i; j < currentCruncherJob.getData().length(); j++) {
						if(currentCruncherJob.getData().charAt(j) == ' ') {
							
							i = j;
							
							CruncherJob cruncherJob = new CruncherJob(cruncher, currentCruncherJob.getDirectory(), currentCruncherJob.getData(), start, i);							cruncherJobs.add(cruncherJob);
							
							start = i;
							cruncherCount++;
							break;
						}
					}
				}
				
				limit_counter = task_limit;
			}
			
			limit_counter--;
			
		}
		
		return cruncherJobs;
	}
	
	public int getId() {
		return id;
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if(this.id == ((CruncherWorker)obj).getId()) {
			return true;
		}
		
		return false;
	}
	
}
