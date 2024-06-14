package RAF.KiDSDomaci1.view;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

import RAF.KiDSDomaci1.app.App;
import RAF.KiDSDomaci1.model.Cruncher;
import RAF.KiDSDomaci1.model.Directory;
import RAF.KiDSDomaci1.model.FileInput;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;

public class FileInputView {
	MainView mainView;
	Pane main;
	FileInput fileInput;
	ListView<Cruncher> linkedCrunchers;
	ListView<Directory> directories;
	ComboBox<Cruncher> availableCrunchers;
	Button linkCrucher;
	Button unlinkCruncher;
	Button addDirectory;
	Button removeDirectory;
	Button start;
	Button removeDiskInput;
	Text status;
	
	private FileInputTask fileInputTask;
	
	private boolean running = false;
	
	public FileInputView(FileInput fileInput, MainView mainView) {
		this.mainView = mainView;
		this.fileInput = fileInput;

		
		main = new VBox();
		main.getChildren().add(new Text("File input " + fileInput.toString() + ": " + fileInput.getDisk().toString()));
		VBox.setMargin(main.getChildren().get(0), new Insets(0, 0, 10, 0));
		main.getChildren().add(new Text("Crunchers:"));

		int width = 210;

		linkedCrunchers = new ListView<Cruncher>();
		linkedCrunchers.setMinWidth(width);
		linkedCrunchers.setMaxWidth(width);
		linkedCrunchers.setMinHeight(150);
		linkedCrunchers.setMaxHeight(150);
		linkedCrunchers.getSelectionModel().selectedItemProperty().addListener(e -> updateUnlinkCruncherButtonEnabled());
		main.getChildren().add(linkedCrunchers);

		availableCrunchers = new ComboBox<Cruncher>();
		availableCrunchers.setMinWidth(width / 2 - 10);
		availableCrunchers.setMaxWidth(width / 2 - 10);
		availableCrunchers.getSelectionModel().selectedItemProperty().addListener(e -> updateLinkCruncherButtonEnabled());

		linkCrucher = new Button("Link cruncher");
		linkCrucher.setOnAction(e -> linkCruncher(availableCrunchers.getSelectionModel().getSelectedItem()));
		linkCrucher.setMinWidth(width / 2 - 10);
		linkCrucher.setMaxWidth(width / 2 - 10);
		linkCrucher.setDisable(true);

		HBox hBox = new HBox();
		hBox.getChildren().addAll(availableCrunchers, linkCrucher);
		HBox.setMargin(availableCrunchers, new Insets(0, 20, 0, 0));
		VBox.setMargin(hBox, new Insets(5, 0, 0, 0));
		main.getChildren().add(hBox);

		unlinkCruncher = new Button("Unlink cruncher");
		unlinkCruncher.setOnAction(e -> unlinkCruncher(linkedCrunchers.getSelectionModel().getSelectedItem()));
		unlinkCruncher.setMinWidth(width);
		unlinkCruncher.setMaxWidth(width);
		unlinkCruncher.setDisable(true);
		VBox.setMargin(unlinkCruncher, new Insets(5, 0, 0, 0));
		main.getChildren().add(unlinkCruncher);

		Text dirTitle = new Text("Dirs:");
		main.getChildren().add(dirTitle);
		VBox.setMargin(dirTitle, new Insets(10, 0, 0, 0));

		directories = new ListView<Directory>();
		directories.setMinWidth(width);
		directories.setMaxWidth(width);
		directories.setMinHeight(150);
		directories.setMaxHeight(150);
		directories.getSelectionModel().selectedItemProperty().addListener(e -> updateRemoveDirectoryButtonEnabled());
		main.getChildren().add(directories);

		addDirectory = new Button("Add dir");
		addDirectory.setOnAction(e -> addDirectory());
		addDirectory.setMinWidth(width / 2 - 10);
		addDirectory.setMaxWidth(width / 2 - 10);

		removeDirectory = new Button("Remove dir");
		removeDirectory.setOnAction(e -> removeDirectory(directories.getSelectionModel().getSelectedItem()));
		removeDirectory.setMinWidth(width / 2 - 10);
		removeDirectory.setMaxWidth(width / 2 - 10);
		removeDirectory.setDisable(true);

		hBox = new HBox();
		hBox.getChildren().addAll(addDirectory, removeDirectory);
		HBox.setMargin(addDirectory, new Insets(0, 20, 0, 0));
		VBox.setMargin(hBox, new Insets(5, 0, 0, 0));
		main.getChildren().add(hBox);

		start = new Button("Start");
		start.setOnAction(e -> start());
		start.setMinWidth(width);
		start.setMaxWidth(width);
		VBox.setMargin(start, new Insets(15, 0, 0, 0));
		main.getChildren().add(start);

		removeDiskInput = new Button("Remove disk input");
		removeDiskInput.setOnAction(e -> removeDiskInput());
		removeDiskInput.setMinWidth(width);
		removeDiskInput.setMaxWidth(width);
		VBox.setMargin(removeDiskInput, new Insets(5, 0, 0, 0));
		main.getChildren().add(removeDiskInput);

		status = new Text("Idle");
		VBox.setMargin(status, new Insets(5, 0, 0, 0));
		main.getChildren().add(status);
	}

	private void updateRemoveDirectoryButtonEnabled() {
		removeDirectory.setDisable(directories.getSelectionModel().getSelectedItem() == null);
	}

	public Pane getFileInputView() {
		return main;
	}
	
	private void updateLinkCruncherButtonEnabled() {
		Cruncher cruncher =  availableCrunchers.getSelectionModel().getSelectedItem();
		if(cruncher != null) {
			for(Cruncher linkedCruncher: linkedCrunchers.getItems()) {
				if(cruncher == linkedCruncher) {
					linkCrucher.setDisable(true);
					return;
				}
			}
			linkCrucher.setDisable(false);
		} else {
			linkCrucher.setDisable(true);
		}
	}
	
	private void updateUnlinkCruncherButtonEnabled() {
		unlinkCruncher.setDisable(linkedCrunchers.getSelectionModel().getSelectedItem() == null);
	}
	

	public void updateAvailableCrunchers(ArrayList<Cruncher> crunchers) {
		availableCrunchers.getItems().clear();
		if (crunchers == null || crunchers.size() == 0) {
			return;
		}
		availableCrunchers.getItems().addAll(crunchers);
		availableCrunchers.getSelectionModel().select(0);
	}

	private void linkCruncher(Cruncher cruncher) {
		linkedCrunchers.getItems().add(cruncher);
		updateLinkCruncherButtonEnabled();
	}
	
	public void removeLinkedCruncher(Cruncher cruncher) {
		linkedCrunchers.getItems().remove(cruncher);
		updateLinkCruncherButtonEnabled();
	}

	private void unlinkCruncher(Cruncher cruncher) {
		linkedCrunchers.getItems().remove(cruncher);
		updateLinkCruncherButtonEnabled();
	}

	private void addDirectory() {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setInitialDirectory(fileInput.getDisk().getDirectory());
		File fileDirectory = directoryChooser.showDialog(mainView.getStage());
		if (fileDirectory != null && fileDirectory.exists() && fileDirectory.isDirectory()) {
			for(Directory directory: directories.getItems()) {
				if(directory.toString().equals(fileDirectory.getPath())) {
					Alert alert = new Alert(AlertType.WARNING);
					alert.setTitle("Error");
					alert.setHeaderText("Directory: " + fileDirectory.getPath() + " is already added.");
					alert.setContentText(null);
					alert.showAndWait();
					return;
				}
			}
			Directory directory = new Directory(fileDirectory);
			directories.getItems().add(directory);
		}
	}

	private void removeDirectory(Directory directory) {
		directories.getItems().remove(directory);
	}

	private void start() {
		
		if(linkedCrunchers.getItems().isEmpty()) {
			showAlert("Please add Cruncher.");
			return;
		}
		
		if(directories.getItems().isEmpty()) {
			showAlert("Please add Directory.");
			return;
		}
		
		
		if(running == false) {
			
			running = true;
			
			// Start fileInputTask in mainFileInputTaskPool
			//--------------------------------------------------------------------------------
			FileInputTask fileInputTask = MainView.fileInputTasks.get(fileInput); 
			if(fileInputTask != null) {
				// Start fileInputTask in ThreadPool
				MainView.fileInputPool.execute(MainView.fileInputTasks.get(fileInput));
			}
			else {
				try {
					throw new Exception("|FileInputView|start():  FileInputName: " +  fileInput.getName() + " is not present MainView.fileInputTasks List...");
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(0);
				}
			}
		
		}
		
		
		start.setText(start.getText().equals("Start") ? "Pause" : "Start");
		
	}
	
	private void showAlert(String message) {
		
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Information");
		alert.setHeaderText(message);
		
		alert.show();
		
		
	}
	
	private void logFileInputStart(boolean consoleLog, boolean fileLog) {
		
		String log = "----------------------------------------------------------\n";
		log += "|FileInput| - start                   " + App.getCurrentDate() + "\n";
		log += "----------------------------------------------------------\n";
		log += "FileInput: FileInput-" + fileInput.getName() + "\n";
		log += "Disk:      " + fileInput.getDisk() + "\n"; 
		log += "Linked Directories: ";
		for (Directory directory : directories.getItems()) {
			log += directory.getFile().getName() + ", ";
		}
		log += "\n";
		log += "Linked Crunchers: ";
		for (Cruncher cruncher : linkedCrunchers.getItems()) {
			log += cruncher + ", ";
		}
		log += "\n----------------------------------------------------------\n\n";
		
		if(consoleLog) {
			System.out.println(log);
		}
		
		if(fileLog) {
			
			try {
				
				File file = new File("./log/fileinput-" + fileInput.getName() + ".txt");
				if(file.exists()) {
					file.delete();
				}
				file.createNewFile();
				
				// Write Log to File
				FileWriter fw = new FileWriter(file, true);
				BufferedWriter bw = new BufferedWriter(fw); 
				
				bw.write(log);
				
				bw.close();
				fw.close();
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		
	}
	
	private void removeDiskInput() {
		mainView.removeFileInputView(this);
		
		// [Change]
		//---------------------------------
		
		FileInputTask task = MainView.fileInputTasks.get(this.fileInput);
		if(task != null) {
			task.stop();
			MainView.fileInputTasks.remove(this.fileInput);
		}
		else if(task == null) {
			try {
				throw new Exception("FileInputTask[FileInputView|removeDiskInput()]: " + task + " is not present in file MainView fileInputTasks list.");
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(0);
			}
		}
		// [Change - END]
		
	
		
		
	} 
		

		
	
	
	public void setStatus(String status) {
		this.status.setText(status);
	}
	
	public FileInput getFileInput() {
		return fileInput;
	}
	
	public ListView<Directory> getDirectories() {
		return directories;
	}
	
	public ListView<Cruncher> getLinkedCrunchers() {
		return linkedCrunchers;
	}
	
	public void setFileInputTask(FileInputTask fileInputTask) {
		this.fileInputTask = fileInputTask;
	}
	
	public Button getStart() {
		return start;
	}
	
}
