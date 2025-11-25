package bank;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public abstract class Account {
    //customer who owns the account, also use for retreval within bank system
    protected Customer customer;
    //list of transactions for the account
    protected List<Transaction> transactions;
    //cached balance
    protected double balance;
    //amount owed for pending withdrawals
    protected double owedAmount;
    

    public Account(Customer customer) {
        this.customer = customer;
        this.transactions = new ArrayList<>();
        this.balance = 0.0;
    }

    //method to add transaction to account via primitive params
    public void addTransaction(double amount, boolean deposit, String description) {
        String id = String.valueOf(transactions.size() + 1); //simple incremental id
        String type = deposit ? "credit" : "debit";
        addTransaction(new Transaction(id, amount, type, description));
    }

    //method to add transaction to account
    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
        if ("credit".equalsIgnoreCase(transaction.getType())) {
            balance += transaction.getAmount();
        } else if ("debit".equalsIgnoreCase(transaction.getType())) {
            balance -= transaction.getAmount();
        }
    }

    //method to pay for a transaction by index
    public void pay(int transactionIndex) {
        if (transactionIndex < 0 || transactionIndex >= transactions.size()) {
            throw new IndexOutOfBoundsException("Transaction index out of range.");
        }
        Transaction transaction = transactions.get(transactionIndex);
        if ("debit".equals(transaction.getType()) && getBalance() < transaction.getAmount()) {
            throw new IllegalStateException("Insufficient funds for this payment.");
        }
        // For credit transactions, no additional action needed; for debit we ensure funds are sufficient.
        balance = getBalance();
    }

    //method to get receipt for a transaction by index
    public void receipt(int transactionIndex) {
        if (transactionIndex < 0 || transactionIndex >= transactions.size()) {
            throw new IndexOutOfBoundsException("Transaction index out of range.");
        }
        Transaction transaction = transactions.get(transactionIndex);
        System.out.println(transaction.getTransactionDetails());
    }

    /**
     * Prints all transactions for this account.
     */
    public void printTransactionHistory() {
        if (transactions.isEmpty()) {
            System.out.println("No transactions found.");
        } else {
            for (Transaction transaction : transactions) {
                System.out.println(transaction.getTransactionDetails());
            }
        }
    }

    /**
     * Filters transactions by optional type and amount criteria.
     * If both criteria are null, returns all transactions.
     */
    public List<Transaction> searchTransactions(String transactionType, Double amount) {
        List<Transaction> result = new ArrayList<>();

        for (Transaction t : transactions) {
            boolean matches = true;

            if (transactionType != null && !transactionType.equalsIgnoreCase(t.getType())) {
                matches = false;
            }

            if (amount != null && Double.compare(amount, t.getAmount()) != 0) {
                matches = false;
            }

            if (matches) {
                result.add(t);
            }
        }

        return result;
    }
}
