package RAF.KiDSDomaci1.model;

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
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class OutputSumResultWorker implements Runnable {
	
	private String resultName;
	private FileOutputTask fileOutputTask;
	private List<String> selectedResulstList;
	
	private double jobSize;
	private volatile double progress;
	private int counter, updateCounter;
	
	public OutputSumResultWorker(String resultName, FileOutputTask fileOutputTask, List<String> selectedResultList) {
		this.resultName = resultName;
		this.fileOutputTask = fileOutputTask;
		this.selectedResulstList = selectedResultList;
		
	}
	
	@Override
	public void run() {
		
		try {
			
			double jobIncrement = 1.0 / selectedResulstList.size();
			progress = 0;
			
			Result sumResult = new Result("*" + resultName);
			
			// Add result in list GUI
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					MainView.results.getItems().add(sumResult.getResultName());	
				}
			});
			
			showProgress("Summing results");
			
			// Povezujemo Rezultate
			for (String resultName : selectedResulstList) {
				
				Map<String, Integer> map;
				
				// Uzima rezultat ako nije spreman onda ceka
				Result selectedResult = fileOutputTask.take(resultName);
				
				// Ako rezultat ne postoji
				if(selectedResult == null) {
					alert("Result with name " + resultName + " doesnt exist.");
					return;
				}
				
				map = selectedResult.getResult();
				
				//-----------------------------------------------
				// MergeMap
				//-----------------------------------------------
				try {
					for (Entry<String, Integer> entity : map.entrySet()) {
						if(sumResult.getResult().containsKey(entity.getKey())) {
							sumResult.getResult().put(entity.getKey(), sumResult.getResult().get(entity.getKey()) + entity.getValue());
						}
						else {
							sumResult.getResult().put(entity.getKey(), entity.getValue());
						}
					}
					
				} catch (OutOfMemoryError e) {
					MainView.OutOfMemory(e);
				}
				
				// update progress bar
				progress += jobIncrement;
				Platform.runLater(new Runnable() {
					
					@Override
					public void run() {
						
						MainView.progressBar.setProgress(progress);
						
					}
				});
				
				
			}
			
			// Upisi rezultat u mapu
			sumResult.setDone(true);
			sumResult.setResultName(sumResult.getResultName().substring(1));
			fileOutputTask.addResult(sumResult);
			
			hideProgress();

			// Rename resultName in list GUI
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					MainView.results.getItems().set(MainView.results.getItems().indexOf("*" + sumResult.getResultName()), sumResult.getResultName());;	
				}
			});
			//-----------------------------------------------
			
			
			//-----------------------------------------------
			// Sort Result
			//-----------------------------------------------
			showProgress("Sorting result");

			jobSize = sumResult.getResult().size() * Math.log(sumResult.getResult().size());
			
			List<Map.Entry<String, Integer>> list = new LinkedList<>(sumResult.getResult().entrySet());

			progress = 0;
			
			Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
				
				@Override
				public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
					updateCounter += 1;
					counter += 1;
					if (updateCounter == Integer.parseInt(Config.getProperty("sort_progress_limit"))) {
						updateCounter = 0;
						progress = ((100.0 * counter) / jobSize)/100.0;
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
			
			hideProgress();
			//-----------------------------------------------
			
			
			//-----------------------------------------------
			// Print result
			//-----------------------------------------------
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
			
			printGraph(newMap);
			
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
	
	private void showProgress(String text) {
		
		Platform.runLater(new Runnable() {
			@Override
			public void run() {				
				MainView.progressBarText.setText(text);
				MainView.progressBarText.setVisible(true);
				MainView.progressBar.setVisible(true);

			}
		});
		
	}
	
	private void hideProgress() {
		
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				MainView.progressBar.setVisible(false);
				MainView.progressBar.setProgress(0);
				MainView.progressBarText.setVisible(false);
				MainView.progressBarText.setText("");
			}
		});
		
	}
	
	private void alert(String text) {
		
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Information");
		alert.setHeaderText(text);
		alert.show();
		
	}
	
}
