package bank;

public class Transaction {
    public void pay() {
        // TODO: track amount, currency, and timestamps for every transaction.
        System.out.println("Payment transaction is done.");
    }

    public void receipt() {
        // TODO: wire this up to customer notifications (email/SMS) and audit logs.
        System.out.println("Transaction receipt.");
    }
}
