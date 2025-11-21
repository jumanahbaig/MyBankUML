package bank;
import lombok.Getter;

@Getter
public class Transaction {

    //to identify transaction ex 100001, 100002, 200001 etc
    int id;
    //to track amount $ involved in transaction
    double amount;
    //to track what the transaction is about ex: "Deposit", "Withdrawal for rent" etc
    String receipt;
    //to track whether transaction is paid or not, if so add to account balance, if not do nothing
    boolean paid;
    //use to track whether transaction is deposit or withdrawal(aka increse or decrease in account balance)
    boolean deposit;

    Transaction(int id, double amount, boolean deposit, String receipt) {
        //id will be given when transaction is created by the account, 
        //so each account can have same id for their respectif transactions
        this.id = id;
        this.amount = amount;
        //true for deposit, false for withdrawal
        this.deposit = deposit;
        //generate receipt based on type of transaction
        if(deposit){
            this.paid = true; //deposits are always "paid", added to balance immediately
            this.receipt = "Deposit of amount: " + amount;
        } else {
            this.paid = false; //withdrawals need to be paid, i.e., processed
            this.receipt = "Withdrawal for: " + receipt + " of amount: " + amount;
        }
    }

    
    public void pay() {
        this.paid = true;
        System.out.println("Payment transaction is done.");
    }

    //this as been replace by info field getter
    // public void receipt() {
    //     System.out.println("Transaction receipt.");
    // }
}
