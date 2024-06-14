package RAF.KiDSDomaci1.view;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import RAF.KiDSDomaci1.app.Config;
import RAF.KiDSDomaci1.model.Cruncher;
import RAF.KiDSDomaci1.model.Disk;
import RAF.KiDSDomaci1.model.FileInput;
import RAF.KiDSDomaci1.model.FileOutput;
import RAF.KiDSDomaci1.model.FileOutputTask;
import RAF.KiDSDomaci1.model.Result;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class MainView {
	private Stage stage;
	private ComboBox<Disk> disks;
	private HBox left;
	private VBox fileInput, cruncher;
	private static Pane center, right;
	public static ListView<String> results;
	private Button addFileInput, singleResult, sumResult;
	private ArrayList<FileInputView> fileInputViews;
	public static LineChart<Number, Number> lineChart;
	private ArrayList<Cruncher> availableCrunchers;
	
	// [Change]
	//------------------------------------------------------------------
	public static Map<FileInput, FileInputTask> fileInputTasks = new ConcurrentHashMap<FileInput, FileInputTask>();
	public static ExecutorService fileInputPool = Executors.newCachedThreadPool();
	
	public static List<Cruncher> cruncherTasks = new ArrayList<Cruncher>();
	public static ExecutorService cruncherPool = Executors.newCachedThreadPool();
	
	public static ExecutorService fileOutputPool = Executors.newCachedThreadPool();
	public static FileOutputTask fileOutputTask;

	public static Text progressBarText = new Text("");
	public static ProgressBar progressBar = new ProgressBar();
	// [Change - END]
	
	
	public static AtomicInteger activeInputTasks = new AtomicInteger(0);
	public static AtomicInteger activeCruncherTasks = new AtomicInteger(0);
	public static AtomicInteger activeOutputTasks = new AtomicInteger(0);
	
	
	
	private Button addCruncher;

	public void initMainView(BorderPane borderPane, Stage stage) {

		this.stage = stage;
		
		// [Change]
		//------------------------------------------------------------------------------------------------
		
		// Application On Exit
		closeApplication();
		
		// [Change - END]
		
		fileInputViews = new ArrayList<FileInputView>();
		availableCrunchers = new ArrayList<Cruncher>();

		left = new HBox();

		borderPane.setLeft(left);

		initFileInput();

		initCruncher();

		initCenter(borderPane);

		initRight(borderPane);
		
		// [Change]
		//------------------------------------------
		fileOutputTask = new FileOutputTask(new FileOutput());
		fileOutputPool.execute(fileOutputTask);
		
		
		// [Change - END]
		
	}

	private void initFileInput() {
		fileInput = new VBox();

		fileInput.getChildren().add(new Text("File inputs:"));
		VBox.setMargin(fileInput.getChildren().get(0), new Insets(0, 0, 10, 0));

		disks = new ComboBox<Disk>();
		disks.getSelectionModel().selectedItemProperty().addListener(e -> updateEnableAddFileInput());
		disks.setMinWidth(120);
		disks.setMaxWidth(120);
		fileInput.getChildren().add(disks);

		addFileInput = new Button("Add FileInput");
		// [Change]
		//----------------------------------------------------------
		addFileInput.setOnAction((e) -> {
			addFileInput(new FileInput(disks.getSelectionModel().getSelectedItem()));
		});
		// [Change - END]
		VBox.setMargin(addFileInput, new Insets(5, 0, 10, 0));
		addFileInput.setMinWidth(120);
		addFileInput.setMaxWidth(120);
		fileInput.getChildren().add(addFileInput);

		int width = 210;

		VBox divider = new VBox();
		divider.getStyleClass().add("divider");
		divider.setMinWidth(width);
		divider.setMaxWidth(width);
		fileInput.getChildren().add(divider);
		VBox.setMargin(divider, new Insets(0, 0, 15, 0));

		Insets insets = new Insets(10);
		ScrollPane scrollPane = new ScrollPane(fileInput);
		scrollPane.setMinWidth(width + 35);
		fileInput.setPadding(insets);
		fileInput.getChildren().add(scrollPane);

		left.getChildren().add(scrollPane);

		
		try {
			String[] disksArray = Config.getProperty("disks").split(";");
			for (String disk : disksArray) {
				File file = new File(disk);
				if(!file.exists() || !file.isDirectory()) {
					throw new Exception("Bad directory path");
				}
				disks.getItems().add(new Disk(file));
			}
			if (disksArray.length > 0) {
				disks.getSelectionModel().select(0);
			}
		} catch (Exception e) {
			Platform.runLater(new Runnable() {
				
				@Override
				public void run() {
					Alert alert = new Alert(AlertType.ERROR);
					alert.setTitle("Closing");
					alert.setHeaderText("Bad config disks");
					alert.setContentText(null);

					alert.showAndWait();
					System.exit(0);
				}
			});
		}

		updateEnableAddFileInput();
	}

	private void initCruncher() {
		cruncher = new VBox();

		Text text = new Text("Crunchers");
		cruncher.getChildren().add(text);
		VBox.setMargin(text, new Insets(0, 0, 5, 0));

		addCruncher = new Button("Add cruncher");
		addCruncher.setOnAction(e -> addCruncher());
		cruncher.getChildren().add(addCruncher);
		VBox.setMargin(addCruncher, new Insets(0, 0, 15, 0));

		int width = 110;

		Insets insets = new Insets(10);
		ScrollPane scrollPane = new ScrollPane(cruncher);
		scrollPane.setMinWidth(width + 35);
		cruncher.setPadding(insets);
		left.getChildren().add(scrollPane);
	}

	private void initCenter(BorderPane borderPane) {
		center = new HBox();

		final NumberAxis xAxis = new NumberAxis();
		final NumberAxis yAxis = new NumberAxis();
		xAxis.setLabel("Bag of words");
		yAxis.setLabel("Frequency");
		lineChart = new LineChart<Number, Number>(xAxis, yAxis);
		lineChart.setMinWidth(700);
		lineChart.setMinHeight(600);
		center.getChildren().add(lineChart);
				
		borderPane.setCenter(center);
	}

	private void initRight(BorderPane borderPane) {
		right = new VBox();
		right.setPadding(new Insets(10));
		right.setMaxWidth(200);

		results = new ListView<String>();
		right.getChildren().add(results);
		VBox.setMargin(results, new Insets(0, 0, 10, 0));
		results.getSelectionModel().selectedItemProperty().addListener(e -> updateResultButtons());
		results.getSelectionModel().selectedIndexProperty().addListener(e -> updateResultButtons());
		results.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		singleResult = new Button("Single result");
		singleResult.setOnAction(e -> getSingleResult());
		singleResult.setDisable(true);
		right.getChildren().add(singleResult);
		VBox.setMargin(singleResult, new Insets(0, 0, 5, 0));

		sumResult = new Button("Sum results");
		sumResult.setDisable(true);
		sumResult.setOnAction(e -> sumResults());
		right.getChildren().add(sumResult);
		VBox.setMargin(sumResult, new Insets(0, 0, 10, 0));
		
		right.getChildren().add(progressBarText);
		right.getChildren().add(progressBar);
		progressBar.setVisible(false);
		progressBarText.setVisible(false);
		
		borderPane.setRight(right);
	}

	public void updateEnableAddFileInput() {
		Disk disk = disks.getSelectionModel().getSelectedItem();
		if (disk != null) {
			for (FileInputView fileInputView : fileInputViews) {
				if (fileInputView.getFileInput().getDisk() == disk) {
					addFileInput.setDisable(true);
					return;
				}
			}
			addFileInput.setDisable(false);
		} else {
			addFileInput.setDisable(true);
		}
	}

	public void updateResultButtons() {
		if (results.getSelectionModel().getSelectedItems() == null
				|| results.getSelectionModel().getSelectedItems().size() == 0) {
			singleResult.setDisable(true);
			sumResult.setDisable(true);
		} else if (results.getSelectionModel().getSelectedItems().size() == 1) {
			singleResult.setDisable(false);
			sumResult.setDisable(true);
		} else {
			singleResult.setDisable(true);
			sumResult.setDisable(false);
		}
	}

	private void getSingleResult() {
		
		String selectedResultName = results.getSelectionModel().getSelectedItem();
		fileOutputTask.getSingleResult(selectedResultName);
		
	}
	
	private void sumResults() {
		
		TextInputDialog td = new TextInputDialog();
		td.setHeaderText("Enter the name of Sum Result:");
		
		Optional<String> tdResult = td.showAndWait();
		tdResult.ifPresent(res -> {
			
			// Selected ResultNames
			List<String> selectedResultNames = results.getSelectionModel().getSelectedItems();

			fileOutputTask.getSumResult(res, selectedResultNames);
			
		});
		
	}
	
	
	public void addFileInput(FileInput fileInput) {
		FileInputView fileInputView = new FileInputView(fileInput, this);
		this.fileInput.getChildren().add(fileInputView.getFileInputView());
		VBox.setMargin(fileInputView.getFileInputView(), new Insets(0, 0, 30, 0));
		fileInputView.getFileInputView().getStyleClass().add("file-input");
		fileInputViews.add(fileInputView);
		if (availableCrunchers != null) {
			fileInputView.updateAvailableCrunchers(availableCrunchers);
		}
		updateEnableAddFileInput();
		
		// [Change]
		//-------------------------------
		FileInputTask fileInputTask = new FileInputTask(fileInputView);
		fileInputTasks.putIfAbsent(fileInput, fileInputTask);
		fileInputView.setFileInputTask(fileInputTask);
		//[Change - END]
	}

	public void removeFileInputView(FileInputView fileInputView) {
		fileInput.getChildren().remove(fileInputView.getFileInputView());
		fileInputViews.remove(fileInputView);
		updateEnableAddFileInput();
	}

	public void updateCrunchers(ArrayList<Cruncher> crunchers) {
		for (FileInputView fileInputView : fileInputViews) {
			fileInputView.updateAvailableCrunchers(crunchers);
		}
		this.availableCrunchers = crunchers;
	}

	public Stage getStage() {
		return stage;
	}

	private void addCruncher() {
		TextInputDialog dialog = new TextInputDialog("1");
		dialog.setTitle("Add cruncher");
		dialog.setHeaderText("Enter cruncher arity");

		Optional<String> result = dialog.showAndWait();
		result.ifPresent(res -> {
			try {
				int arity = Integer.parseInt(res);
				for (Cruncher cruncher : availableCrunchers) {
					if (cruncher.getArity() == arity) {
						Alert alert = new Alert(AlertType.WARNING);
						alert.setTitle("Error");
						alert.setHeaderText("Cruncher with this arity already exists.");
						alert.setContentText(null);
						alert.showAndWait();
						return;
					}
				}
				Cruncher cruncher = new Cruncher(arity, fileOutputTask);
				
				// [Change]
				//----------------------------------------------------------------------------
				if(!cruncherTasks.contains(cruncher)) {
					cruncherTasks.add(cruncher);
					MainView.cruncherPool.execute(cruncher);		// Start cruncherTask - listening cruncherJobQueue
				}	
				// [Change - END]
				
				CruncherView cruncherView = new CruncherView(this, cruncher);
				this.cruncher.getChildren().add(cruncherView.getCruncherView());
				availableCrunchers.add(cruncher);
				updateCrunchers(availableCrunchers);
				
				cruncher.setCruncherView(cruncherView);
				
			} catch (NumberFormatException e) {
				Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("Wrong input");
				alert.setHeaderText("Arity must be a number");
				alert.showAndWait();
			}
		});
	}
	
	
	
	private AtomicBoolean close = new AtomicBoolean(false);
	public void closeApplication() {
		
		stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			
			@Override
			public void handle(WindowEvent event) {
				
				Alert alert = new Alert(AlertType.INFORMATION);
				
				alert.setTitle("Information");
				alert.setHeaderText("Closing...");
				alert.show();
						
				// Stop all FileInput Tasks
				stopFileInputTasks();
				stopCruncherTasks();
				stopFileOutputTasks();
				
				
				while(MainView.activeInputTasks.get() > 0 && MainView.activeCruncherTasks.get() > 0 && MainView.activeOutputTasks.get() > 0) {
					
				}
				
				Platform.exit();
				System.exit(0);
				
			}
		});
		
	}
	
	public static void shutdownApplication() {
		
		stopFileInputTasks();
		stopCruncherTasks();
		stopFileOutputTasks();
		
		Platform.exit();
		System.exit(0);
	}
	
	public static void stopFileOutputTasks() {
		fileOutputTask.stop();
	}
	
	public static void stopCruncherTasks() {
		
		// Stop All Active CruncherTasks
		for (Cruncher cruncher : cruncherTasks) {
			cruncher.stop();
		}
		
		// Stop CruncherTasksPool
		cruncherPool.shutdown();
		
	}

	public static void stopFileInputTasks() {
		
		// Stop All Active FileInputTasks
		for (Entry<FileInput, FileInputTask> entry : fileInputTasks.entrySet()) {
			entry.getValue().stop();
		}
		
		// Stop FileInputTasksPool
		fileInputPool.shutdown();
		
	}

	public void removeCruncher(CruncherView cruncherView) {
		for (FileInputView fileInputView : fileInputViews) {
			fileInputView.removeLinkedCruncher(cruncherView.getCruncher());
		}
		availableCrunchers.remove(cruncherView.getCruncher());
		updateCrunchers(availableCrunchers);
		cruncher.getChildren().remove(cruncherView.getCruncherView());
	}
	
	
	public static AtomicBoolean memory = new AtomicBoolean(false);
	public static void OutOfMemory(OutOfMemoryError e) {
		
		if(memory.getAndSet(true)) {
			
			Platform.runLater(new Runnable() {
				
				@Override
				public void run() {
						
					Alert alert = new Alert(AlertType.ERROR);
					alert.setTitle("Error");
					alert.setHeaderText("Program run out of memory.");
					alert.showAndWait();
					
					shutdownApplication();
					
				}
			});
			
			e.printStackTrace();
			
		}
		
		
		
	}
	
	public static Pane getRight() {
		return right;
	}

	public static ProgressBar getProgressBar() {
		return progressBar;
	}
	
}
