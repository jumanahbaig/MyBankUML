package bank;

import java.util.List;

/**
 * Coordinates customer creation so that both the user record and default checking account
 * are persisted together. This guarantees every customer has a real account number as soon
 * as they exist in the system.
 *
 * Acts as a simple transaction boundary: it creates a Customer object, writes the user row,
 * and immediately adds the auto-provisioned checking account through AccountRepository.
 * That decouples the entity (Customer) from persistence details and keeps the workflow reusable.
 */
public class CustomerService {
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final Logs logs;

    public CustomerService(UserRepository userRepository, AccountRepository accountRepository, Logs logs) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.logs = logs;
    }

    public CustomerService(UserRepository userRepository, AccountRepository accountRepository) {
        this(userRepository, accountRepository, null);
    }

    /**
     * End-to-end customer onboarding flow:
     * 1. Builds the Customer entity (which auto-creates a default checking account).
     * 2. Persists the user data via UserRepository so the customer has a database identity.
     * 3. Persists the default checking account via AccountRepository, generating an account number.
     */
    public Customer createCustomer(String firstName, String lastName, String userName, String password) {
        List<User> existing = userRepository.search(userName);
        if (!existing.isEmpty()) {
            System.out.println("User with username '" + userName + "' already exists. Creation aborted.");
            if (logs != null) {
                logs.append(
                    "SYSTEM",
                    "CREATE_CUSTOMER_FAILED",
                    userName,
                    "Attempted to create customer but username already exists."
                );
            }
            return null;
        }

        Customer customer = new Customer(firstName, lastName, userName, password);
        userRepository.addUsers(customer);
        accountRepository.addAccount(customer.getCheckingAccount());
        if (logs != null) {
            logs.append(
                customer.getUserName(),
                "CREATE_CUSTOMER",
                "CUSTOMER_AND_ACCOUNT",
                "Customer and default account created successfully."
            );
        }
        return customer;
    }

    /**
     * SRS: Teller search use case.
     * Returns customers whose username matches the given fragment (exact or partial, depending on repository implementation).
     * Tellers are only allowed to see CUSTOMERS.
     */
    public List<User> searchCustomersForTeller(String usernameFragment) {
        return userRepository.searchCustomersForTeller(usernameFragment);
    }

    /**
     * SRS: Admin search use case.
     * Admin can search any type of user. If roleFilter is null or blank, all roles are included.
     * Otherwise, roleFilter should be one of "CUSTOMER", "TELLER", or "ADMIN".
     */
    public List<User> searchUsersForAdmin(String usernameFragment, String roleFilter) {
        return userRepository.searchForAdmin(usernameFragment, roleFilter);
    }

    /**
     * Handles user login with simple username/password verification and optional logging.
     *
     * @return true if credentials are valid, false otherwise.
     */
    public boolean loginUser(String userName, String password, Branch branch) {
        List<User> matches = userRepository.search(userName);
        String branchTarget = (branch != null) ? "BRANCH_" + branch.getAddress() : "BRANCH_UNKNOWN";

        if (matches.isEmpty()) {
            if (logs != null) {
                logs.append(
                    userName,
                    "LOGIN_FAILED",
                    branchTarget,
                    "Login attempt failed at branch " + branchTarget + " (incorrect username or password)"
                );
            }
            return false;
        }

        User user = matches.get(0);
        if (user.getPassword().equals(password)) {
            if (branch != null) {
                branch.userLogin(user);
            }
            if (logs != null) {
                logs.append(
                    user.getUserName(),
                    "LOGIN_SUCCESS",
                    branchTarget,
                    "User logged in successfully at branch " + branchTarget
                );
            }
            return true;
        }

        if (logs != null) {
            logs.append(
                user.getUserName(),
                "LOGIN_FAILED",
                branchTarget,
                "Login attempt failed at branch " + branchTarget + " (incorrect username or password)"
            );
        }
        return false;
    }
}
