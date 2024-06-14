package RAF.KiDSDomaci1.view;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import RAF.KiDSDomaci1.app.App;
import RAF.KiDSDomaci1.app.Config;
import RAF.KiDSDomaci1.model.Cruncher;
import RAF.KiDSDomaci1.model.CruncherJob;
import RAF.KiDSDomaci1.model.Directory;
import RAF.KiDSDomaci1.model.Disk;
import RAF.KiDSDomaci1.model.FileInput;
import RAF.KiDSDomaci1.model.Result;
import javafx.application.Platform;
import javafx.collections.ObservableList;

public class FileInputTask implements Runnable{

	private FileInputView fileInputView;
	private FileInput fileInput;
	private Disk disk;
	private ObservableList<Directory> directoriesToRead;
	
	public List<File> readFiles = new CopyOnWriteArrayList<File>();
	public List<Directory> processedFiles;
	public List<CruncherJob> cruncherJobs; 		// ???  DODATI da dodaje u LinkedBlocingQ
	
	private ObservableList<Cruncher> linkedCrunchers;
	
	private boolean working;
	
	public FileInputTask(FileInputView fileInputView) {
		
		this.fileInput = fileInputView.getFileInput();
		this.fileInputView = fileInputView;
		this.disk = fileInput.getDisk();
		this.directoriesToRead = this.fileInputView.getDirectories().getItems();
		this.readFiles = new CopyOnWriteArrayList<File>();
		this.processedFiles = new CopyOnWriteArrayList<Directory>();
		this.cruncherJobs = new CopyOnWriteArrayList<CruncherJob>();
		this.linkedCrunchers = fileInputView.getLinkedCrunchers().getItems();
		this.working = true;
		
		MainView.activeInputTasks.incrementAndGet();
		
	}
	
	@Override
	public void run() {
		
		logFileInputStart(true, true);
		
		String log = "";
		while(working) {
			
			// Pause FileInput Thread
			if(!fileInputView.getStart().getText().equals("Pause")) {
				while(fileInputView.getStart().getText().equals("Start")) {
				
				}
			}
			//-----------------------------------------------------------
			
			try {
				
				// Logs
				//-------------------------------------------------------------------
//				log += "----------------------------------------------------------\n";
//				log += "|Task| - " + "FileInput-" + fileInput + "                  " + App.getCurrentDate() + "\n";
//				log += "----------------------------------------------------------\n";
//				log += "Disk: " + disk.getDirectory().getAbsolutePath() + "\n";
//				log += "\nFolders: ";
//				
//				System.out.println("----------------------------------------------------------");
//				System.out.println("|Task| - " + "FileInput-" + fileInput + "                  " + App.getCurrentDate());
//				System.out.println("----------------------------------------------------------");
//				System.out.println("Disk: " + disk.getDirectory().getAbsolutePath());
//				System.out.println("\nFolders: "); 
				
				//-------------------------------------------------------------------
				
				// Scan Directories - loading file from Directories added from GUI
				for (Directory directory: directoriesToRead) {
					
					// Log
					// --------------------------------------------------------------
					System.out.println(directory.getFile().getAbsolutePath());
					log += directory.getFile().getAbsolutePath() + "\n";
					// --------------------------------------------------------------
					
					// Skeniramo direktoriju
					scanDirectory(directory.getFile().listFiles());
				}
				System.out.println();
				
				// Log
				// --------------------------------------------------------------
//				System.out.println("Read Filse: " + readFiles);
//				System.out.println("Processed Filse: " + processedFiles);
//				System.out.println("Jobs: " + cruncherJobs);
				// --------------------------------------------------------------
				
				// Load Files
				loadFiles(readFiles);
				
				// Logs
				//-------------------------------------------------------------------
				log += "Processed Files: " + processedFiles + "\n"; 
				log += "Jobs: " + cruncherJobs + "\n";
				log +=  "----------------------------------------------------------\n\n";
				
				
				
//				System.out.println("Processed Files: " + processedFiles);
//				System.out.println("\nJobs: " + cruncherJobs);
//				System.out.println("----------------------------------------------------------\n");
				//-------------------------------------------------------------------
				
				logFileInput(false, true, log);
				log = "";
				
				readFiles.clear();		// Reset
				Thread.sleep(Integer.parseInt(Config.getProperty("file_input_sleep_time")));
				
				
			} catch (Exception e) {
				e.printStackTrace();
			} catch (OutOfMemoryError e) {
				MainView.OutOfMemory(e);
			}
		}
		
		
		
		
		
	}
	
	// Files: list of Files in given Directory
	// Ucitava fajlove koji nisu ucitani u readFileList
	private void scanDirectory(File[] files) {
		
		for (File file : files) {
			
			// If file is Folder
			if(file.isDirectory()) {
				scanDirectory(file.listFiles());
			}
			
			// File is file
			else {
				
				// Ako se ucitani fajl ne nalazi u vec ucitanim fajlovima, onda dodajemo fajl u ucitane fajlove.
				if(!contains(readFiles, file)) {
					readFiles.add(file);
				}
//				System.out.println(directory.getName());
			}
			
		}
		
	}
	 
	
	
	public void loadFiles(List<File> readFiles) {
		
		// Logs
		//------------------------------------------------------
		System.out.println("Read Files: ");
		for (File file : readFiles) {
			System.out.println(file.getName());
		}
		System.out.println();
		//------------------------------------------------------
		
		
		
		
		//----------------------------------------------------------------------------------------
		// Initial - ako je processed prazan
		//----------------------------------------------------------------------------------------
		
		// Ako je processedFiles prazan
		if(processedFiles.isEmpty()) {
			
			// Prolazimo kroz ucitane fajlove i dodajemo ih u listu procesiranih fajlova.
			for (File readFile : readFiles) {
				
				Directory newDirectory = new Directory(readFile);	// Pravimo novi direktorijum od ucitanih fajlova.
				processedFiles.add(newDirectory);					// Dodajemo ucitani fajl u procesirane fajlove
				CruncherJob cruncherJob = readFile(newDirectory);	// Pravimo cruncherJob od ucitanog Fajla
				cruncherJobs.add(cruncherJob);						
				for (Cruncher cruncher : linkedCrunchers) {			// Prolazimo kroz povezane Crunchere koji su povezani sa ovim FileInput.
					Platform.runLater(new Runnable(){
						@Override
						public void run() {
							
							// Dodajemo naziv rezultata u resultList u GUI-u 
							MainView.results.getItems().add("*" + cruncherJob.getDirectory().getFile().getName() + "-arity" + cruncher.getArity());
							
						}
					});
					
					// Dodajemo cruncherJob u Cruncher queue
					cruncher.addCruncherJob(cruncherJob);
				}
			}
			
		}
		
		//----------------------------------------------------------------------------------------
		// Other Cases - kada processedFiles nije prazan
		//----------------------------------------------------------------------------------------
		else {
			
			//----------------------------------------------------------------------------------------		
			// Ako postoji fajl koji se nalazi u listi readFiles, a koji se ne nalazi u listi processedFiles, onda to znaci da je dodat novi fajl.
			//----------------------------------------------------------------------------------------
			
			// Prolazimo kroz sve ucitane fajlove i proveravamo da li se nalaze u listi processedFiles
			for(int i = 0; i < readFiles.size(); i++) {
				
				boolean contains = false;
				for (Directory processedFile : processedFiles) {
					// Ako se fajl iz readFile liste nalazi u processedList, onda nista
					if(readFiles.get(i).getAbsolutePath().equals(processedFile.getFile().getAbsolutePath())) {
						contains = true;
						break;
					}
				}

				//-----------------------------------------------------
				// New File
				//-----------------------------------------------------
				// Ako se trenutni readFile nije u listi processedFilse, znaci da je dodat novi fajl.
				if(!contains) {
					
					// Akcija kada je fajl dodat, ucitavamo readFile odnosno njegove podatke
					Directory newDirectory = new Directory(readFiles.get(i));
					System.err.println("File Added: " + readFiles.get(i));
					processedFiles.add(newDirectory);								// Dodajemo fajl u processedFiles
					CruncherJob newJob = readFile(newDirectory);					// Pravimo novi CruncherJob
					cruncherJobs.add(newJob);										// Dodaje novi Job za dododati novi Fajl
					
					i = -1;
					//------------------------------------------------------
					
					// Dodajemo novi fajl u resultList u GUI
					for (Cruncher cruncher : linkedCrunchers) {
						
						Platform.runLater(new Runnable(){
							
							@Override
							public void run() {
								MainView.results.getItems().add("*" + newJob.getDirectory().getFile().getName() + "-arity" + cruncher.getArity());
								cruncher.addCruncherJob(newJob);
							}
							
						});
					}
				}
			}
			//----------------------------------------------------------------------------------------
			
			
			//-----------------------------------------------------
			// Deleted File
			//-----------------------------------------------------
			
			// Ako se obrisani fajl nalazi listi processedFiles, a ne nalazi se u listi readFiles znaci da je file obrisan.
			// Proveravamo za svaki fajl iz processedList da li se nalazi u readFiles
			for (int i = 0; i < processedFiles.size(); i++) {
				File processedFile = processedFiles.get(i).getFile();
				boolean contains = false;
				for (File readFile : readFiles) {
					if(processedFile.getAbsolutePath().equals(readFile.getAbsolutePath())) {
						contains = true;
						break;
					}
				}
				
				// Ako se ne nalazi znaci da je file obrisan
				if(!contains) {
					
					// Action kada je File uklonjen - ukloniti iz Output Rezultata rezultate za taj fajl
					//-----------------------------------------------------------
					System.err.println("File Removed: " + processedFiles.get(i).getFile().getAbsolutePath());
					File removedFile =  processedFiles.get(i).getFile();
					processedFiles.remove(processedFiles.get(i));
					
					// Prolazimo kroz sve Cruncher
					for (Cruncher cruncher : linkedCrunchers) {
						Platform.runLater(new Runnable(){
							@Override
							public void run() {
								
								try {
									
									// Uklanjamo file iz resultList iz GUI i iz FileOutput resultList
									
									boolean contains = false;
									for(int i = 0; i < MainView.fileOutputTask.getResults().size(); i++) {
										
										// Rezultat iz Modela
										Result currentResult = MainView.fileOutputTask.getResults().get(i);
										String modifiedResult = removedFile.getName() + "-arity" + cruncher.getArity();
										
										// Ako je trenutni rezultat iz fileOutputTask liste jednak rezultatu koji treba da se obrise.
										if(currentResult.getResultName().equals(modifiedResult)) {
											MainView.fileOutputTask.getResults().remove(currentResult);		// Brisemo
											
											// Ako se taj rezultat nalazi i u MainView.result list
											if(MainView.results.getItems().contains(modifiedResult)) {
												MainView.results.getItems().remove(modifiedResult);
											}
											
											// Ako se taj rezultat nalazi i u MainView.result list i rezultat nije zavrsen
											else if(MainView.results.getItems().contains("*" + modifiedResult)) {
												MainView.results.getItems().remove("*" + modifiedResult);
											}
											else {
												// Ako se rezutat ne nalazi u GUI
												throw new Exception("String: " + modifiedResult + " is not in Result List in GUI.");
											}
											
											// Reset
											contains = true;
											break;
										}
									}
									
									// Ako se rezultat ne nalazi u fileOutputTask resultList
									if(contains == false) {
										throw new Exception("Modified Result: " + removedFile.getName() + "-arity" + cruncher.getArity() + " is not in FilOutputTask Results.");	
									}
									
								} catch (Exception e) {
									e.printStackTrace();
								}
			
							}
						});
					
					}
					
					i = -1;
				}
			}	
			
			//-------------------------------------------------------------------------------------------------------------------------------------------------
			
			
			
			//-----------------------------------------------------
			// File Modified - check
			//-----------------------------------------------------
			
			// Kada proverimo da li je fajl obirsan ili dodat, onda proveravamo da li je modifikovan.

			// Prilazimo kroz processedFiles
			for (Directory directory : processedFiles) {
				
				// Proveramova da li su modifikovani od prosle iteracije
				if(directory.isModified()) {
					System.err.println("Modified File: " + directory.getFile().getAbsolutePath());
					
					// Modifikovani fajl uklanjamo iz processedFile liste, i onda se posle radi proces ucitavanja kao za novi fajl.
					processedFiles.remove(directory);
					directory.setModified();						// Reset lastModified time
					processedFiles.add(directory);
					CruncherJob job = readFile(directory);			// Ponovo ucitavamo modifikovani fajl.
					cruncherJobs.add(job);							// Dodajemo modifikovani fajl u crucher queue kako bi se opet obradio.
					
					// Uklanjamo rezultate za staru verziju modifikovanog fajla i dodajemo rezultate za novi modifikovani fajl.
					for (Cruncher cruncher : linkedCrunchers) {
						Platform.runLater(new Runnable(){
							@Override
							public void run() {
								
								try {
									
									boolean contains = false;
									for(int i = 0; i < MainView.fileOutputTask.getResults().size(); i++) {
										Result currentResult = MainView.fileOutputTask.getResults().get(i);
										String modifiedResult = job.getDirectory().getFile().getName() + "-arity" + cruncher.getArity();
										
										if(currentResult.getResultName().equals(modifiedResult)) {
											MainView.fileOutputTask.getResults().remove(currentResult);
											
											if(MainView.results.getItems().contains(modifiedResult)) {
												MainView.results.getItems().remove(modifiedResult);
											}
											else if(MainView.results.getItems().contains("*" + modifiedResult)) {
												MainView.results.getItems().remove("*" + modifiedResult);
											}
											else {
												throw new Exception("String: " + modifiedResult + " is not in Result List in GUI.");
											}
											
											MainView.results.getItems().add("*" + job.getDirectory().getFile().getName() + "-arity" + cruncher.getArity());
											
											contains = true;
											break;
										}
									}
									
									if(contains == false) {
										throw new Exception("Modified Result: " + job.getDirectory().getFile().getName() + "-arity" + cruncher.getArity() + " is not in FilOutputTask Results.");	
									}
									
								} catch (Exception e) {
									e.printStackTrace();
								}
								
								
							}
						});
						
						cruncher.addCruncherJob(job);
					}
					
				}
			}	
		}
		
		
		if(cruncherJobs.isEmpty()) {
			for (Directory directory : processedFiles) {
				
				CruncherJob job = readFile(directory);
				cruncherJobs.add(job);
				
			}
		}
		
		
	}
	
	
	// Ucitava podatke za fajl
	private CruncherJob readFile(Directory directory) {
		
		try {
			
			System.out.println("Processing File: " + directory.getFile().getName() + "(" + directory.getFile().length() / 1000000 + "mb)");
			
			Platform.runLater(new Runnable(){
				@Override
				public void run() {
					fileInputView.setStatus("Processing File: " + directory.getFile().getName());
				}
			});
						
			List<String> loadData = null;
			try {
				loadData = Files.readAllLines(Paths.get(directory.getFile().getPath()));
			} catch (OutOfMemoryError e) {
				System.err.println("Test123");
				e.printStackTrace();
			}
			
		    String data = loadData.stream()
		      .map(n -> String.valueOf(n))
		      .collect(Collectors.joining("\n"));
		    
		    Platform.runLater(new Runnable(){
				@Override
				public void run() {
					fileInputView.setStatus("Idle");
				}
			});
		    
		    
		    // Vraca cruncher job sa fajlom i podacima
			return new CruncherJob(directory, data);
			
		} catch (OutOfMemoryError e) {
			MainView.OutOfMemory(e);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		return null;

	}
	
	
	
	// Proverava da li se Fajl nalazi u Listi, na osnovu putanje fajla
	private boolean contains(List<File> list, File file) {
		
		if(list.isEmpty()) {
			return false;
		}
		
		for (File item : list) {
			if(item.getAbsolutePath().equals(file.getAbsolutePath())) {
				return true;
			}
		}
		
		return false;
	}
	
	private void logFileInput(boolean consoleLog, boolean fileLog, String log) {
		
		// Ne radi log za konzolu
//		if(consoleLog) {
//			System.out.println(log);
//		}
		
		if(fileLog) {
			
			try {
				
				File file = new File("./log/fileinput-" + fileInput.getName() + ".txt");
				
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
	
	// Pisemo bilo sta u logFile
	private void writeLog(String log) {
		
		try {
			
			File file = new File("./log/fileinput-" + fileInput.getName() + ".txt");
			
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
	
	private void logFileInputStart(boolean consoleLog, boolean fileLog) {
		
		String log = "----------------------------------------------------------\n";
		log += "|FileInput| - start                   " + App.getCurrentDate() + "\n";
		log += "----------------------------------------------------------\n";
		log += "FileInput: FileInput-" + fileInput.getName() + "\n";
		log += "Disk:      " + fileInput.getDisk() + "\n"; 
		log += "Linked Directories: ";
		for (Directory directory : directoriesToRead) {
			log += directory.getFile().getName() + ", ";
		}
		log += "\n";
		log += "Linked Crunchers: ";
		for (Cruncher cruncher : linkedCrunchers) {
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
	
	public void stop() {
		
		MainView.activeInputTasks.decrementAndGet();
		
		String log = "";
		log += "----------------------------------------------------------\n";
		log += "|Task| - " + "FileInput-" + fileInput + " - STOP           " + App.getCurrentDate() + "\n";
		log += "----------------------------------------------------------\n";
		System.out.println(log);
		writeLog(log);
		this.working = false;
	}

	@Override
	public String toString() {
		return fileInput.getName();
	}


}
