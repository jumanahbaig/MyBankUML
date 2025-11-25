package bank;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logs {
    private final String textFile;
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Logs(String textFile) {
        this.textFile = textFile;
    }

    /**
     * Appends a single log entry to the file, with timestamp and metadata.
     * Format:
     *   yyyy-MM-dd HH:mm:ss; Actor: <actor>; Action: <action>; Target <target>; Details: <details>;
     */
    public void append(String actor, String action, String target, String details) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String line = String.format(
            "%s; Actor: %s; Action: %s; Target: %s; Details: %s",
            timestamp, actor, action, target, details
        );

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(textFile, true))) {
            writer.write(line);
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to log file: " + textFile, e);
        }
    }

    /**
     * Reads and prints every log entry from the file.
     */
    public void listAll() {
        File file = new File(textFile);
        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read log file: " + textFile, e);
        }
    }

    /**
     * Reads the log file and prints only entries for the given actor.
     * A log line is considered to belong to an actor if it contains:
     *   "Actor: <actor>;"
     */
    public void listByUser(String actor) {
        File file = new File(textFile);
        if (!file.exists()) {
            return;
        }

        String marker = "Actor: " + actor + ";";

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(marker)) {
                    System.out.println(line);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read log file: " + textFile, e);
        }
    }
}
