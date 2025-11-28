package bank;

public class Transaction {
    private final String id;
    private final double amount;
    private final String type; // e.g., "credit" or "debit"
    private final String description; // e.g., "Card payment"
    private final java.time.LocalDateTime timestamp;

    public Transaction(String id, double amount, String type, String description) {
        this(id, amount, type, description, java.time.LocalDateTime.now());
    }

    public Transaction(String id, double amount, String type, String description, java.time.LocalDateTime timestamp) {
        this.id = id;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.timestamp = timestamp;
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

    public java.time.LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getTransactionDetails() {
        return String.format(
                "ID: %s, Amount: %.2f, Type: %s, Description: %s, Date: %s",
                id, amount, type, description, timestamp);
    }

    public void pay() {
        System.out.println("Processing transaction: " + getTransactionDetails());
    }

    public void receipt() {
        System.out.println("Receipt for transaction: " + getTransactionDetails());
    }
}
