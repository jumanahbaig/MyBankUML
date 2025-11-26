package server;

import bank.*;

public class DataSeeder {
    private final DatabaseManager dbManager;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CustomerService customerService;

    public DataSeeder() {
        this.dbManager = new DatabaseManager();
        this.dbManager.initialize();
        this.userRepository = new UserRepository(dbManager);
        this.accountRepository = new AccountRepository(dbManager);
        this.customerService = new CustomerService(userRepository, accountRepository);
    }

    public void seed() {
        System.out.println("Starting database seeding...");

        // Create admin user
        createAdmin("John", "Admin", "admin", "password123");

        // Create teller users
        createTeller("Sarah", "Johnson", "teller", "password123");
        createTeller("Mike", "Wilson", "teller2", "password123");

        // Create customer users with accounts
        createCustomerWithAccounts("Alice", "Smith", "customer", "password123");
        createCustomerWithAccounts("Bob", "Jones", "bob", "password123");
        createCustomerWithAccounts("Emma", "Davis", "emma", "password123");
        createCustomerWithAccounts("James", "Brown", "james", "password123");

        System.out.println("\nDatabase seeding completed!");
        System.out.println("\n=== Test Credentials ===");
        System.out.println("Admin:    username: admin    password: password123");
        System.out.println("Teller:   username: teller   password: password123");
        System.out.println("Customer: username: customer password: password123");
        System.out.println("Customer: username: bob      password: password123");
        System.out.println("Customer: username: emma     password: password123");
        System.out.println("Customer: username: james    password: password123");
    }

    private void createAdmin(String firstName, String lastName, String username, String password) {
        try {
            Admin admin = new Admin(firstName, lastName, username, password);
            userRepository.addUsers(admin);
            System.out.println("✓ Created admin: " + username);
        } catch (Exception e) {
            System.out.println("✗ Admin " + username + " already exists or error: " + e.getMessage());
        }
    }

    private void createTeller(String firstName, String lastName, String username, String password) {
        try {
            Teller teller = new Teller(firstName, lastName, username, password);
            userRepository.addUsers(teller);
            System.out.println("✓ Created teller: " + username);
        } catch (Exception e) {
            System.out.println("✗ Teller " + username + " already exists or error: " + e.getMessage());
        }
    }

    private void createCustomerWithAccounts(String firstName, String lastName, String username, String password) {
        try {
            // Create customer (automatically creates checking account)
            Customer customer = customerService.createCustomer(firstName, lastName, username, password);

            if (customer != null) {
                System.out.println("✓ Created customer: " + username + " with checking account");

                // Add a savings account for some customers
                if (username.equals("customer") || username.equals("bob")) {
                    Saving savingsAccount = new Saving(customer);
                    accountRepository.addAccount(savingsAccount);
                    System.out.println("  + Added savings account");
                }

                // Add a card account for some customers
                if (username.equals("customer") || username.equals("emma")) {
                    Card cardAccount = new Card(customer);
                    accountRepository.addAccount(cardAccount);
                    System.out.println("  + Added card account");
                }
            }
        } catch (Exception e) {
            System.out.println("✗ Customer " + username + " already exists or error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        DataSeeder seeder = new DataSeeder();
        seeder.seed();
    }
}
