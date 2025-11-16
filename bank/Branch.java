package bank;

import lombok.Getter;

@Getter
public class Branch {
    private final String address;
    private final Bank bank;

    public Branch(String address, Bank bank) {
        this.address = address;
        this.bank = bank;
        // to add to the bank
        bank.addBranch(this);
    }

    public void printBranchInfo() {
        // TODO: include branch-specific stats (active tellers/customers) and use centralized logging.
        System.out.println("Branch " + address + " From Bank " + bank.getName());
    }

    public void userLogin() {
        // TODO: manage authentication/session tracking for branch-specific logins.
    }

    public void userLogout() {
        // TODO: invalidate sessions and audit user activity upon logout.
    }
}
