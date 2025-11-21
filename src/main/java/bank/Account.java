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
    //current balance of account
    protected int balance;
    //amount owed for pending withdrawals
    protected int owedAmount;
    

    public Account(Customer customer) {
        this.customer = customer;
        this.balance = 0;
        this.transactions = new ArrayList<>();
    }

    //method to add transaction to account
    //if deposit, increase balance immediately
    public void addTransaction(double amount, boolean is_deposit, String receipt) {
        int id = transactions.size() + 1; //simple incremental id
        Transaction transaction = new Transaction(id, amount, is_deposit, receipt);
        transactions.add(transaction);
        if(is_deposit){
            //increase balance immediately for deposits
            balance += amount;
        }else{
            //track owed amount for withdrawals
            owedAmount += amount;
        }
    }

    //method to pay for a transaction by id
    //only works for withdrawals
    //if withdrawal, decrease balance
    public void pay(int transactionId) {
        for (Transaction transaction : transactions) {
            if (transaction.getId() == transactionId) {
                if (!transaction.is_deposit()&& !transaction.isPaid()) {
                    balance -= transaction.getAmount();
                    owedAmount -= transaction.getAmount();
                    transaction.pay();
                    return;
                }
                
                System.out.println("Transaction with ID " + transactionId + " is either already paid or is a deposit.");
                return;
            }
        }
        System.out.println("Transaction with ID " + transactionId + " not found.");
    }

    //method to get receipt for a transaction by id
    public String receipt(int transactionId) {
        for (Transaction transaction : transactions) {
            if (transaction.getId() == transactionId) {
                System.out.println("Transaction receipt: " + transaction.getReceipt());
                return transaction.getReceipt();
            }
        }
        System.out.println("Transaction with ID " + transactionId + " not found.");
        return null;
    }
}
