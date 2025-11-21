package bank;

import lombok.Getter;

@Getter
public class Branch {
    private final String address;
    private final String phoneNumber;
    private final Bank bank;
    // Currently active user at this branch (null if none)
    private User activeUser;

    // Optional logging utility
    private final Logs logs;

    public Branch(String address, Bank bank) {
        this(address, "N/A", bank, null);
    }

    public Branch(String address, Bank bank, Logs logs) {
        this(address, "N/A", bank, logs);
    }

    public Branch(String address, String phoneNumber, Bank bank) {
        this(address, phoneNumber, bank, null);
    }

    public Branch(String address, String phoneNumber, Bank bank, Logs logs) {
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.bank = bank;
        this.logs = logs;
        // to add to the bank
        bank.addBranch(this);
    }

    public void printBranchInfo() {
        System.out.println(getBranchInfo());
    }

    public String getBranchInfo() {
        String activeUserName = (activeUser != null) ? activeUser.getUserName() : "none";
        return "Branch " + address +
               " (phone: " + phoneNumber + ")" +
               " from Bank " + bank.getName() +
               " | active user: " + activeUserName;
    }

    public int getActiveUserCount() {
        return (activeUser != null) ? 1 : 0;
    }

    public void userLogin(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (activeUser != null && !activeUser.getUserName().equals(user.getUserName())) {
            throw new IllegalStateException(
                "Another user (" + activeUser.getUserName() + ") is already active at this branch."
            );
        }

        activeUser = user;

        if (logs != null) {
            logs.append(
                user.getUserName(),
                "LOGIN",
                address,
                "User logged in at branch " + address
            );
        }
    }

    public void userLogout(User user) {
        if (user == null) {
            return;
        }

        if (activeUser != null && activeUser.getUserName().equals(user.getUserName())) {

            if (logs != null) {
                logs.append(
                    user.getUserName(),
                    "LOGOUT",
                    address,
                    "User logged out from branch " + address
                );
            }

            activeUser = null;
        }
    }
}
