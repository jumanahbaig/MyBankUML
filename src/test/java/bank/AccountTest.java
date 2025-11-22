package bank;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccountTest {

    @Test
    void testTransactionHistory() {
        Customer customer = new Customer("Test", "User", "testuser", "password123");
        Account card = new Card(customer);
        card.addTransaction(new Transaction(UUID.randomUUID().toString(), 200.0, "debit", "Card payment"));
        card.addTransaction(new Transaction(UUID.randomUUID().toString(), 500.0, "credit", "Deposit"));

        card.printTransactionHistory();

        assertEquals(2, card.searchTransactions(null, null).size(), "Expected two transactions in history.");
    }

    @Test
    void testSearchTransactions() {
        Customer customer = new Customer("Test", "User", "testuser2", "password123");
        Account card = new Card(customer);
        card.addTransaction(new Transaction(UUID.randomUUID().toString(), 200.0, "debit", "Card payment"));
        card.addTransaction(new Transaction(UUID.randomUUID().toString(), 300.0, "credit", "Deposit"));
        card.addTransaction(new Transaction(UUID.randomUUID().toString(), 50.0, "debit", "Small payment"));

        List<Transaction> debits = card.searchTransactions("debit", null);
        assertEquals(2, debits.size(), "Expected two debit transactions.");

        List<Transaction> smallPayments = card.searchTransactions(null, 50.0);
        assertEquals(1, smallPayments.size(), "Expected one transaction with amount 50.0.");
    }

    @Test
    void testAddTransaction() {
        Customer customer = new Customer("Balance", "Tester", "balance_user", "password123");
        Account account = new Card(customer);

        Transaction creditTx = new Transaction(UUID.randomUUID().toString(), 200.0, "credit", "Deposit");
        account.addTransaction(creditTx);
        assertEquals(200.0, account.getBalance(), 0.0001, "Balance should reflect credit transaction");

        Transaction debitTx = new Transaction(UUID.randomUUID().toString(), 50.0, "debit", "Withdrawal");
        account.addTransaction(debitTx);
        assertEquals(150.0, account.getBalance(), 0.0001, "Balance should reflect debit transaction");

        assertEquals(2, account.getTransactions().size(), "There should be 2 transactions in the history.");
    }

    @Test
    void testSearchTransactionsByTypeAndAmount() {
        Customer customer = new Customer("Filter", "Tester", "filter_user", "password123");
        Account account = new Card(customer);

        account.addTransaction(new Transaction(UUID.randomUUID().toString(), 500.0, "credit", "Deposit"));
        account.addTransaction(new Transaction(UUID.randomUUID().toString(), 300.0, "debit", "Withdrawal"));
        account.addTransaction(new Transaction(UUID.randomUUID().toString(), 100.0, "debit", "Payment"));

        List<Transaction> credits = account.searchTransactions("credit", null);
        assertEquals(1, credits.size(), "Expected 1 credit transaction.");
        assertEquals(500.0, credits.get(0).getAmount(), 0.0001, "Expected credit amount to be 500.");

        List<Transaction> largeDebits = account.searchTransactions("debit", 300.0);
        assertEquals(1, largeDebits.size(), "Expected 1 debit transaction with amount 300.0.");
        assertEquals(300.0, largeDebits.get(0).getAmount(), 0.0001, "Expected debit amount to be 300.");

        List<Transaction> allTransactions = account.searchTransactions(null, null);
        assertEquals(3, allTransactions.size(), "Expected 3 transactions in total.");
    }
}
