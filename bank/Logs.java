package bank;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logs {
    private final String textFile;
    
    public Logs(String textFile) {
        this.textFile = textFile;
        createLogFile();        
    }
    
    private void createLogFile() {
    	try {
    		File logFile = new File(textFile);
    		
			if (logFile.createNewFile()) {
				System.out.println(String.format("Successfully created log file with name %s.", this.textFile));
			}
			else {
				System.out.println(String.format("Log file with name %s already exists.", this.textFile));
			}
		} 
    	catch (IOException e) {
			System.out.println(String.format("An error occured while trying to create LOG text file with name %s.", this.textFile));
			e.printStackTrace();
		}
    }

    public void append(String actor, String action, String target, String details) {
    	String logEntry = String.format("%s: actor %s performed action %s on target %s. Details: %s.", getTimeStamp(), actor, action, target, details);
    	
    	try (BufferedWriter writer = new BufferedWriter(new FileWriter(this.textFile, true))) {
    		writer.write(logEntry);
    		writer.newLine();
    	} 
    	catch (IOException e) {
    		System.out.println(String.format("An error occured while trying to append an entry to LOG text file with name %s.", this.textFile));
			e.printStackTrace();
		}
    }
    
    private String getTimeStamp() {
    	LocalDateTime now = LocalDateTime.now();
    	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    	return now.format(formatter);
    }

    public void listAll() {    	
    	try (BufferedReader reader = new BufferedReader(new FileReader(this.textFile))) {
    		String line;
    		while((line = reader.readLine()) != null) {
    			System.out.println(line);
    		}
    	} 
    	catch (FileNotFoundException e) {
    		System.out.println(String.format("Could not find LOG text file with name %s when attempting to list all of its entries.", this.textFile));
			e.printStackTrace();
		} 
    	catch (IOException e) {
    		System.out.println(String.format("An error occured while trying to list all entries of LOG text file with name %s.", this.textFile));
			e.printStackTrace();
		}
    }

    public void listByUser(String actor) {
        // TODO: filter log entries by actor and display only matching records.
    }
}
