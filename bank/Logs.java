package bank;

public class Logs {
    private final String textFile;

    public Logs(String textFile) {
        this.textFile = textFile;
        // Future: initialize file handlers, ensure file exists, etc.
    }

    public void append(String actor, String action, String target, String details) {
        // TODO: append a formatted line to the log file (actor/action/target/details/timestamp).
    }

    public void listAll() {
        // TODO: read the entire log file and print every entry to the console.
    }

    public void listByUser(String actor) {
        // TODO: filter log entries by actor and display only matching records.
    }
}
