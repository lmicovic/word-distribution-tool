# Word distribution tool

1. [Introduction](#introduction)
2. [System description](#system-description)
 - [Input pipe](#input-pipe)
 - [Cruncher pipe](#cruncher-pipe)
 - [Output pipe](#output-pipe)
3. [Quality of system](#quality-of-system)
 - [Memory stability](#memory-stability)
 - [Functional quality](#functional-quality)
4. [GUI system settings](#gui-system-settings)
5. [Configuration file](#configuration-file)
6. [Application usecase](#application-usecase)




## Introduction

Word distribution tool is system that counts the appearance number of specific words in provided text.

While system is running, we can add new text sources that are supposed to be processed in real time. System is capable to count the number of appearance of single word or number of appearance of bags of words. System can represents the processing results of single text source or to combine results using multiple sources. Sources are ASCII coded textual files.

- Word distribution tool is processing sources in concurrent way, using multiple threads.
- System is implemented in Java programming language.
- System is divided into multiple components. More about components could be found in System Component section of this document.
- System can handle errors and can inform user and can easily recover from those errors.
- User can interact with tool using GUI interface.
- System can be configured using configuration file.

## System description
Word distribution tool is functioning using “pipes”. System contains three types of pipes:
- [Input]($input-pipe) – pipes that are responsible to handle system inputs.
- [Cruncher](#cruncher-pipe) – pipes that are processing data.
- [Output]($output-pipe) – pipes that are responsible for storing and presenting system results.

Input pipe is always before Cruncher pipe and Cruncher pipe is always before Output pipe:

`Input -> Cruncher -> Output`

There are many types of Input, Cruncher and Input pipes. Type of pipes depends on type of input source of the system. This system currently is able to process ASCII encoded text files.

System can have multiple number of instances of every pipe, where every pipe instance is executing on its own thread.  Each pipe type has its own thread pool that is used by pipe instances to execute their job. Main purpose of each pipe is to provide data flow through the system.

Pipes communicates with each other using shared blocking queues. Cruncher pipe has its own shared blocking pipe, in which Input pipe will add jobs that Cruncher pipe should process. Output pipe has its own blocking pipe, in which Cruncher pipe adds processed data.

![Screenshot (133)](https://github.com/lmicovic/distributed-fractals-chaos-game/assets/172028832/9d61bfc2-d746-4bdf-9e12-67900e264cf9)

## Input pipe
Main purpose of Input pipe is to provide data to Cruncher pipe that it should process. Every Input pipe instance is able to connect to Cruncher’s blocking queue. Input pipe is creating jobs and putting them in Cruncher’s blocking queue, so Input pipe is produces for the Cruncher’s queue blocking queue, and Cruncher pipe is consumer of his blocking queue.

FileInput pipe is responsible for reading text from input file. FileInput pipe receives the disk on which are text files are present. Disks are represented as prefix name of some existing path in the file system, similar as it is defined in Linux operating system.

FileInput pipes then recursively travels through Disk’s directories looking for .txt file, then that file is added in a reading list. Reading file data is added as separate job inside of a thread pool that is intended for all FileInput pipes. Files from Disk are read sequentially, but reading file from different Disks are read concurrently. For example if we have 3 Disks as input we will have 3 threads in thread pool reading data from their files.

After reading files data is finished, the name of file as its data are forwarded in the Crunchers input – blocking queue of each Cruncher pipe that Input pipe is connected.

FileInput pipe is supervising its defined Disk path if there are any new files added or changed.

After FileInput pipe has done checking its Disk paths for any newly added or changed files, FileInput pipe blocks for fixed period of time, after that period of time pipe fill check again if there are any new changes in Disk files. Blocking period is set in project configuration file. Also FileInput pipe can be paused and then FileInput pipe is not scanning for any changes in directories. We can start FileInput again and it will start scanning directories for changes immediately, no matters what time passed after last defined blocking period time.

If some directory from InputFile pipe Disk is deleted all files from that directory will be deleted also in the system memory. If we add same directory again all files in that directory will be scanned again.

## Cruncher pipe
Cruncher pipe is responsible for processing data from input files. Every Cruncher pipe during its construction must to have arity attribute defined. Arity attributes represents Bag of Words size. Arity attributes is the main attribute that is different for every instance of Cruncher pipe. Every Cruncher pipe instance has following blocking queues:
- Own input blocking queue, Cruncher pipe is consumer of its input blocking queue, many Input pipe can be producers for one Cruncher input blocking queue. 
- One Cruncher pipe is also connected to several output blocking queues. Cruncher pipe is producer for output blocking queue, each of these output blocking queue has it’s own consumer which is Output pipe instance.

CounterCruncher pipe is used to calculate how many times one word or bag of words appears in specific text. Input of CounterCruncher is object that has path name of file and text that should be processed. The Job of processing one file’s text is done inside of Cruncher’s thread pool. Cruncher thread pool is used by all other instances of CounterCruncher pipe.

Each job inside Cruncher thread pool should be defined to process similar size of text data.

Arity attribute of Cruncher pipe dictates what Cruncher pipe is actually counting. Bag of word is actually set of words that are we counting. Words that are next each other in Bag of word must be next to each other in text also. For example, if arity of Cruncher pipe is 3, then Cruncher pipe will count the distribution of Bag of words size of 3 in given text. If arity is 1, then we will count every word separately.

When counting process for text starts, all Output pipe will be notified that processing of words have started.  Then specific Output pipe instance will show that counting process of specific file is in progress. When counting process finishes, Output pipe will have, as a result, the number of occurrence of each word in specific text file.

## Output pipe
Output pipe is used to store and present the results of word counting process.

Each Output pipe has one output blocking queue. Output pipe is consumer of its output blocking queue. Multiple Cruncher pipes can be provide for Output pipe output blocking queue.

CacheOutput pipe stores counting results in memory. Result is stored in the Map<String, Int> - (Key: resultname, Value: number of occurrence of each Bag of word for each arity in specified text).

This pipe provides aggregation of existing results using union and summation. Output pipe have its own job and that job is executed inside own thread pool. Aggregation is executed using union of existing results. If in different results appears the same word then results for that specific results are summarized.

For each result there are supported poll() and take() operations. Operation poll() is non-blocking operation that returns null if result of some job is not ready. Operation take() is blocking operation that is waiting until specific job result are ready.


## Quality of system
System meets the user requirements that are divided in to the two categories:
 - [Memory stability](#memory-stability)
 - [Functional quality](#functional-quality)

### Memory stability
Memory stability refers that system will work using reasonable amount of memory. System will work under 3GB of allocated memory.

Memory allocation for the Word distribution tool can be defined using following parameters inside Java Virtual Machine during the startup of the project.

In case if during the execution of the project system run out of allocated memory, error will be reported and execution of program will be suspended, without shutting down active jobs and threads. 

- Inside Eclipse IDE, we can specify memory restriction inside:

`Run Configuration/ Arguments/ VM Arguments`

![image001](https://github.com/lmicovic/word-distribution-tool/assets/172028832/f4362f27-5a9d-4ad4-91b6-2a8c1e3570ab)

- Inside IntelliJ IDE we can specify memory restrictions by selecting:

`Run/ Run … / Edit Configurations/ VM Options`

![image002](https://github.com/lmicovic/word-distribution-tool/assets/172028832/51d4c6c8-f1ea-419e-b0b2-4a01b12bca0c)



## Functional quality
Functional quality refers to resistance to specific errors.
- All GUI actions are enabled only if they are required.
- If results are requested but they are not ready, information will be shown.
- Exit of the application is clean, which includes prevention of starting new jobs. Before application shuts down it will wait until all active jobs finishes its executions, then application will be cleanly shutted down.

## GUI system settings
System supports following actions in GUI:
- Input
 - Creating new FileInput pipes connected to own Disk.
 - Connecting FileInput pipe with Cruncher pipe.
 - Disconnecting FileInput pipe with Cruncher pipe.
 - Adding directories in FileInput pipe.
 - Removing directories in FileInput pipe.
 - Starting/Pausing FileInput pipe.
 - Removing FileInput pipe.
 - Showing current FileInput pipe activity.
- Cruncher
 - Create new CounterCruncher pipes, every CounterCruncher pipe must have defined arity attribute.
 - Deleting CounterCruncher pipe.
 - Showing jobs that specific CounterCruncher pipe is executing.
- Output
 - There is only one Output pipe present in GUI.
 - Shows the list of results, if result is not ready error will be shown. Result will be sorted in descending order by word occurrence. Progress bar will be displayed on every k compartments inside of sorting process. Sorting process will have N*logN comparements since Collection.sort() is used. When loading result is done, result will be displayed as frequency graph for the first 100 words.
 - Starting aggregation jobs. Before starting job user will be prompted to insert the name of the summarization. If the name is not unique error will be shown. During process of summarization progress bar will be displayed.




## Configuration file
System can be configured using configuration file – application.properties that contains following parameters:

     #Blocking time for FileInput pipes, given in milliseconds
     file_input_sleep_time=5000
     
     #List of Disks for Input pipes
     disks=data/disk1;data/disk2

     #Limit for job division, given in number of characters
     counter_data_limit=10000000

     #Number of compartments after refreshing progress during sorting process
     sort_progress_limit=10000


All these parameters are loaded durning startup of program and could not be changed durning the program execution. These parameters can be changed only if program is shuted down and stared again.


# Application usecase
Usecase example using configuration file provided in [Configuration File Section].

**Scenario 1 – simple**
1. Create FileInput pipe for Disk data/disk1
2. Create CounterCruncher pipe with arity=1
3. Connect these pipe together
4. Add directory A as path inside FileInput pipe
5. Start FileInput pipe
6. Show separate results for each directory.
7. Show summarized result

**Scenario 2 – memory intensive**
1. Create FileInput pipe for Disk data/disk1
2. Create FileInput pipe for Disk data/disk2
3. Create CounterCruncher pipe with arity=1
4. Connect these FileInput pipes with CounterCruncher pipe
5. Add whole Disks as paths to FileInput pipe
6. Start FileInput pipe at the same time as possible.
7. All calculations should be done without error.

**Scenario 3 – summarizing during execution**
1. Create FileInput pipe for Disk data/disk1
2. Create CounterCruncher pipes with arity=1 and arity=2
3. Connect FileInput pipe with CounterCruncher pipes
4. Add Disk disk1 as path to FileInput
5. Start FileInput pipe
6. Try to get result that is not ready
7. When all 3 items are present in Output pipe, start summarization.
8. Summarization will be started only when all 4 jobs are finished.
9. Show result


**Scenario 4 – special cases**
1. Create FileInput pipe for Disk data/disk1
2. Create CounterCruncher pipe with arity=1
3. Connect these two pipes
4. Add directory A as path into FileInput pipe
5. Start FileInput pipe
6. Add new directory in directory A
7. Change values in directory A
8. Create CounterCruncher with arity=2
9. Connect FileInput pipe with CounterCruncher pipe
10. Disconnect FileInput pipe with from first CounterCruncher pipe that has arity=1
11. Delete directory A from FileInput pipe and add directory A to the samo FileInput again. 

