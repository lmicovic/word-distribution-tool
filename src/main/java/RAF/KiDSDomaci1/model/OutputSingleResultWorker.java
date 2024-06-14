package RAF.KiDSDomaci1.model;

import java.lang.annotation.Retention;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import RAF.KiDSDomaci1.app.Config;
import RAF.KiDSDomaci1.view.MainView;
import javafx.application.Platform;
import javafx.scene.chart.XYChart;

public class OutputSingleResultWorker implements Runnable {

	private FileOutputTask fileOutputTask;
	
	private Result result;					// Selektovani rezultat iz GUI
	private boolean processingResult;
	
	private double jobSize;
	private volatile double progress;
	private int counter;
	private int updateCounter;
	
	public OutputSingleResultWorker(FileOutputTask fileOutputTask, Result result) {
		this.fileOutputTask = fileOutputTask;
		this.result = result;
		this.processingResult = false;
	}
	
	@Override
	public void run() {
		
		try {
			
			this.processingResult = true;
			showProgressBar();
			
			// Uzmemo rezultate
			List<Entry<String, Integer>> list = new LinkedList<Entry<String,Integer>>(result.getResult().entrySet());
			
			// Sortiramo rezultate

			jobSize = result.getResult().size() * Math.log(result.getResult().size());
			
			Collections.sort(list, new Comparator<Entry<String, Integer>>() {
			
				@Override
				public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
					
					updateCounter += 1;
					counter += 1;
					if (updateCounter == Integer.parseInt(Config.getProperty("sort_progress_limit"))) {
						updateCounter = 0;
						
						progress = ((100.0 * counter * 2) / jobSize)/100.0;
						Platform.runLater(new Runnable() {
							
							@Override
							public void run() {
								MainView.progressBar.setProgress(progress);
							}
							
						});
					}
					
					
					return (o2.getValue().compareTo(o1.getValue()));
				}
				
			});
			
			// Top 100 Rezultat
			Map<Integer, Integer> newMap = new HashMap<Integer, Integer>();
			int count = 0;
			for (Entry<String, Integer> entry : list) {
				if(count == 100) {
					break;
				}
//				System.out.println(entry.getKey() + ": " + entry.getValue());
				newMap.put(count, entry.getValue());
				count++;
			}
			
			// Nacrtati Graf
			printGraph(newMap);
			
			this.processingResult = false;
			hideProgressBar();
			
		} catch (OutOfMemoryError e) {
			MainView.OutOfMemory(e);
		}
		
	}
	
	private void printGraph(Map<Integer, Integer> result) {
		
		Platform.runLater(new Runnable(){
			
			@Override
			public void run() {
				
				// Delete previouse Chart Data
				MainView.lineChart.getData().clear();
				
				// Fill LineChart
				int i = 0;
				XYChart.Series series = new XYChart.Series();
				for (Entry<Integer, Integer> entity : result.entrySet()) {
					series.getData().add(new XYChart.Data(i, entity.getValue()));
					i++;
				}
				MainView.lineChart.getData().add(series);
			}
			
		});
		
	}
	
	private void showProgressBar() {
		
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				MainView.progressBar.setVisible(true);
				MainView.progressBarText.setText("Sorting result");
				MainView.progressBarText.setVisible(true);
			}
		});
		
	}
	
	private void hideProgressBar() {

		Platform.runLater(new Runnable() {
			
			@Override
			public void run() {
				MainView.progressBar.setVisible(false);
				MainView.progressBarText.setVisible(false);
				MainView.progressBarText.setText("");
			}
		});
		
	}
	
	public boolean isProcessingResult() {
		return processingResult;
	}
	
	public Result getResult() {
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if(this.result.getResultName().equals(((Result)obj).getResultName())) {
			return true;
		}
		
		return false;
	}
	
}
