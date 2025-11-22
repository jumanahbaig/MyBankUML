package bank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the SRS requirements around UserRepository.search().
 *
 * Covers:
 *  - search() finds an existing user by username
 *  - search() returns empty list for non-existing username
 *  - teller search: only CUSTOMERS are returned
 *  - admin search: can search any role, with optional role filter
 */
public class UserRepositorySearchTest {

    private DatabaseManager databaseManager;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // NOTE: Adjust this constructor call if your DatabaseManager needs parameters
        // (for example, a DB path like "jdbc:sqlite:bank.db").
        databaseManager = new DatabaseManager();
        userRepository = new UserRepository(databaseManager);

        // You can optionally clear or reset test data here if your DB supports it.
        // For now, we just ensure we're starting from a known state by adding test users.
        seedTestUsers();
    }

    private void seedTestUsers() {
        // Remove any existing users with these usernames if your repository supports delete.
        // This is optional but safer if tests run multiple times.
        // For now we assume usernames are unique and just add clearly test-only users.

        User customer = new Customer("Alice", "Customer", "alice_cust", "pass123");
        User teller = new Teller("Bob", "Teller", "bob_teller", "pass123");
        User admin = new Admin("Cara", "Admin", "cara_admin", "pass123");

        // Persist them through the repository
        userRepository.addUsers(customer);
        userRepository.addUsers(teller);
        userRepository.addUsers(admin);
    }

    /**
     * SRS unit test equivalent:
     * "Verify that calling search() correctly finds a user based on a specified username."
     */
    @Test
    void search_existingUsername_returnsUser() {
        List<User> results = userRepository.search("alice_cust");

        assertFalse(results.isEmpty(), "Expected at least one user for username alice_cust");
        assertEquals("alice_cust", results.get(0).getUserName());
    }

    /**
     * SRS unit test equivalent:
     * "Verify that search() does not return anything for a non-existing username."
     */
    @Test
    void search_nonExistingUsername_returnsEmptyList() {
        List<User> results = userRepository.search("does_not_exist_123");

        assertTrue(results.isEmpty(), "Expected no users for a non-existing username");
    }

    /**
     * SRS teller search use case:
     * Teller can only see CUSTOMERS when searching.
     */
    @Test
    void searchCustomersForTeller_returnsOnlyCustomers() {
        // This should internally call search(usernameFragment, "CUSTOMER")
        List<User> allCustomers = userRepository.searchCustomersForTeller(null);

        assertFalse(allCustomers.isEmpty(), "Expected at least one customer for teller search");

        for (User user : allCustomers) {
            assertTrue(user instanceof Customer,
                    "Teller search must only return Customer instances");
        }

        // Additionally, ensure that our known customer is visible:
        List<User> aliceOnly = userRepository.searchCustomersForTeller("alice_cust");
        assertFalse(aliceOnly.isEmpty(), "Expected alice_cust to be visible to the teller");
        assertEquals("alice_cust", aliceOnly.get(0).getUserName());
    }

    /**
     * SRS admin search use case:
     * Admin can search any user type, and may filter by role.
     */
    @Test
    void searchForAdmin_withRoleFilter_returnsOnlyThatRole() {
        // Without role filter: should see at least our three seeded users
        List<User> allUsers = userRepository.searchForAdmin(null, null);
        assertTrue(allUsers.size() >= 3, "Admin search without role filter should see all users");

        // With role filter "ADMIN" – should only get admins
        List<User> admins = userRepository.searchForAdmin(null, "ADMIN");
        assertFalse(admins.isEmpty(), "Expected at least one admin for role ADMIN");

        for (User user : admins) {
            assertTrue(user instanceof Admin,
                    "Admin search with role filter ADMIN must only return Admin instances");
        }

        // Make sure our known admin is present
        boolean foundCara = admins.stream()
                .anyMatch(u -> "cara_admin".equals(u.getUserName()));
        assertTrue(foundCara, "Expected cara_admin to be included in admin search for role ADMIN");
    }
}
