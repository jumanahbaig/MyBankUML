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

/**
 * Utility class for accounting and security traceability. 
 * Keeps an append-only record of important actions such as logins, account creation, role updates, search/display events and so on for audition purposes. 
 * 
 * @author Sebastien, Jumanah
 */
public class Logs {
    private final String textFile;	// Path to log text file.
    
    /**
     * Creates a new log file with the given string path.
     * 
     * @param textFile Path to log file.
     */
    public Logs(String textFile) {
        this.textFile = textFile;
        createLogFile();        
    }
    
    /**
     * Creates the Log file if it does not exist.
     */
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

    /**
     * Appends a new Log entry to the end of the log file.
     * 
     * @param actor
     * @param action
     * @param target
     * @param details
     */
    public void append(String actor, String action, String target, String details) {
    	String logEntry = String.format("%s; Actor: %s; Action: %s; Target %s; Details: %s;", getTimeStamp(), actor, action, target, details);
    	
    	try (BufferedWriter writer = new BufferedWriter(new FileWriter(this.textFile, true))) {
    		writer.write(logEntry);
    		writer.newLine();
    	} 
    	catch (IOException e) {
    		System.out.println(String.format("An error occured while trying to append an entry to LOG text file with name %s.", this.textFile));
			e.printStackTrace();
		}
    }		
    
    /**
     * Gets a formatted time stamp for use in append.
     * @return yyy-MM-dd HH:mm:ss
     */
    private String getTimeStamp() {
    	LocalDateTime now = LocalDateTime.now();
    	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    	return now.format(formatter);
    }

    /**
     * Displays all log entries in the log file.
     */
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

    /**
     * Displays all log entries in the log file for a specific actor.
     * 
     * @param actor The person who performed a logged action.
     */
    public void listByUser(String actor) {
    	try (BufferedReader reader = new BufferedReader(new FileReader(this.textFile))) {
    		String line;
    		while((line = reader.readLine()) != null) {
    			String[] lineInfo = line.split(";");
    			String actorInfo = lineInfo[1];
    			if (actorInfo.contains(actor)) {
    				System.out.println(line);
    			}    			
    		}
    	} 
    	catch (FileNotFoundException e) {
    		System.out.println(String.format("Could not find LOG text file with name %s when attempting to list all entries with actor %s.", this.textFile, actor));
			e.printStackTrace();
		} 
    	catch (IOException e) {
    		System.out.println(String.format("An error occured while trying to list entries of LOG text file with name %s by actor %s.", this.textFile, actor));
			e.printStackTrace();
		}
    }
}
