package bank;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Bank {
    private final String name;
    private final List<Branch> branches;
    private final Logs bankLogs;
    private final UserRepository userlist;
    private final AccountRepository cardAccount;
    private final AccountRepository checkAccount;
    private final AccountRepository savingAccount;

    public Bank(String name) {
        this.name = name;
        this.branches = new ArrayList<>();
        DatabaseManager databaseManager = new DatabaseManager();
        this.userlist = new UserRepository(databaseManager);
        this.cardAccount = new AccountRepository(databaseManager);
        this.checkAccount = new AccountRepository(databaseManager);
        this.savingAccount = new AccountRepository(databaseManager);
        this.bankLogs = new Logs("bank-log.txt");

    }

    public void addBranch(Branch branch) {
        branches.add(branch);
    }

    public Branch createNewBranch(String address, String phoneNumber, Logs logs) {
        return new Branch(address, phoneNumber, this, logs);
    }

    public void printBankInfo() {
        System.out.println("Bank: " + name);
        int totalActiveUsers = 0;
        for (Branch branch : branches) {
            System.out.println("  - " + branch.getBranchInfo());
            totalActiveUsers += branch.getActiveUserCount();
        }
        System.out.println(
            "Summary: " + branches.size() + " branch(es), " +
            totalActiveUsers + " active user(s) across all branches."
        );
    }
}
