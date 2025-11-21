package bank;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Bank {
    private final String name;
    private final List<Branch> branches;

    public Bank(String name) {
        this.name = name;
        this.branches = new ArrayList<>();
    }

    public void addBranch(Branch branch) {
        branches.add(branch);
    }

    public Branch createNewBranch(String address, String phoneNumber, Logs logs) {
        Branch branch = new Branch(address, phoneNumber, this, logs);
        // Branch constructor already calls bank.addBranch(this), so just return it.
        return branch;
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
