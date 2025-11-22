package bank;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Transaction {
    private final String id;        // Unique transaction ID
    private final double amount;    // Transaction amount (positive or negative)
    private final String type;      // "credit" or "debit"
    private final String description; // e.g., "deposit", "withdrawal"
    private final LocalDateTime timestamp; // Timestamp when transaction occurred

    public Transaction(String id, double amount, String type, String description) {
        this.id = id;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.timestamp = LocalDateTime.now(); // Capture the current time when transaction is created
    }

    public String getId() {
        return id;
    }

    public double getAmount() {
        return amount;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getTransactionDetails() {
        return String.format(
                "ID: %s, Amount: %.2f, Type: %s, Description: %s, Date: %s",
                id, amount, type, description, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }
}
