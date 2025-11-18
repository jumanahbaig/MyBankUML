package bank;

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
    	String logEntry = String.format("%s: actor %s, performed action %s on target %s. Details: %s.\n", getTimeStamp(), actor, action, target, details);
    	
    	try (FileWriter writer = new FileWriter(this.textFile, true)) {
    		writer.write(logEntry);
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
        // TODO: read the entire log file and print every entry to the console.
    }

    public void listByUser(String actor) {
        // TODO: filter log entries by actor and display only matching records.
    }
}
