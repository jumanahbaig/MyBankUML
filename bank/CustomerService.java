package bank;

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

    public CustomerService(UserRepository userRepository, AccountRepository accountRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * End-to-end customer onboarding flow:
     * 1. Builds the Customer entity (which auto-creates a default checking account).
     * 2. Persists the user data via UserRepository so the customer has a database identity.
     * 3. Persists the default checking account via AccountRepository, generating an account number.
     */
    public Customer createCustomer(String firstName, String lastName, String userName, String password) {
        Customer customer = new Customer(firstName, lastName, userName, password);
        userRepository.addUsers(customer);
        accountRepository.addAccount(customer.getCheckingAccount());
        return customer;
    }
}
