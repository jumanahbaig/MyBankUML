package bank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Customer extends User {
    // Every customer must own exactly one checking account from day one.
    private final Account checkingAccount;
    // Tracks any additional accounts (savings, cards, etc.) requested later.
    private final List<Account> additionalAccounts;

    public Customer(String firstName, String lastName, String userName, String password) {
        super(firstName, lastName, userName, password);
        // Automatically provision the default checking account to honor the business rule.
        this.checkingAccount = new Check(this);
        this.additionalAccounts = new ArrayList<>();
    }

    public void requestNewAccount(Account account) {
        // Customers only initiate the request; actual approval happens elsewhere.
        // Use simple class names to make the logs easier to read (e.g., "Saving" instead of "bank.Saving").
        System.out.println("Requesting new account: " + account.getClass().getSimpleName());
        System.out.println("New " + account.getClass().getSimpleName() + " account request was successfully sent!");
    }

    /**
     * Called by bank staff once an account request is approved so we keep the portfolio in sync.
     */
    public boolean addAccount(Account account) {
        System.out.println("Attempting to add account for customer: " + account.getClass().getSimpleName());
        // Checking accounts remain unique per customer; other account types may be duplicated.
        if (account instanceof Check) {
            System.out.println("Cannot add another checking account for this customer.");
            return false;
        }
        // Reject accounts that were created for a different user to keep data consistent.
        if (account.getCustomer() != this) {
            System.out.println("Cannot add account that does not belong to this customer.");
            return false;
        }
        additionalAccounts.add(account);
        System.out.println("Account approved and added to customer profile.");
        return true;
    }

    public Account getCheckingAccount() {
        return checkingAccount;
    }

    public List<Account> getAdditionalAccounts() {
        return Collections.unmodifiableList(additionalAccounts);
    }

    public List<Account> getAllAccounts() {
        List<Account> combined = new ArrayList<>();
        combined.add(checkingAccount);
        combined.addAll(additionalAccounts);
        return Collections.unmodifiableList(combined);
    }

    public String getName() {
        return getFirstName() + " " + getLastName();
    }

    public void printCustomerInfo() {
        printUserInfo();
    }

    @Override
    public void printUserInfo() {
        super.printUserInfo();
        System.out.println("Checking Account: " + checkingAccount.getClass().getSimpleName());
        System.out.println("Additional Accounts: " + additionalAccounts.size());
        if (!additionalAccounts.isEmpty()) {
            additionalAccounts.forEach(account ->
                    System.out.println(" - " + account.getClass().getSimpleName()));
        }
    }

    public void requestAccountDeletion() {
        // TODO: notify bank staff to review and approve the customer's account deletion request.
    }

    @Override
    public void requestForgottenPassword() {
        // TODO: route forgotten password request to admin along with identity verification data.
    }
}
