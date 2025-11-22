package bank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountRepositoryTest {

    private DatabaseManager databaseManager;
    private UserRepository userRepository;
    private AccountRepository accountRepository;

    private Customer customerAlice;
    private Customer customerBob;

    @BeforeEach
    void setUp() {
        databaseManager = new DatabaseManager();
        userRepository = new UserRepository(databaseManager);
        accountRepository = new AccountRepository(databaseManager);

        // seed users
        customerAlice = new Customer("Alice", "Smith", "alice_user", "pass123");
        customerBob = new Customer("Bob", "Jones", "bob_user", "pass123");
        userRepository.addUsers(customerAlice);
        userRepository.addUsers(customerBob);

        // seed accounts
        Account aliceCheck = customerAlice.getCheckingAccount();
        Account bobCheck = customerBob.getCheckingAccount();
        Account aliceSaving = new Saving(customerAlice);

        accountRepository.addAccount(aliceCheck);
        accountRepository.addAccount(bobCheck);
        accountRepository.addAccount(aliceSaving);
    }

    @Test
    void searchByAccountNumber_returnsCorrectAccount() {
        String aliceAccountNumber = fetchAccountNumberForUser("alice_user");
        List<Account> result = accountRepository.search(aliceAccountNumber, null, null);
        assertFalse(result.isEmpty(), "Expected to find account by account number");
        for (Account acc : result) {
            assertEquals("alice_user", acc.getCustomer().getUserName(), "Account should belong to alice_user");
        }
    }

    @Test
    void searchByAccountType_returnsAllOfType() {
        List<Account> savings = accountRepository.search(null, "SAVING", null);
        assertFalse(savings.isEmpty(), "Expected to find saving accounts");
        for (Account acc : savings) {
            assertEquals(Saving.class, acc.getClass(), "Expected only Saving accounts");
        }
    }

    @Test
    void searchByUsername_returnsAccountsForCustomer() {
        List<Account> aliceAccounts = accountRepository.search(null, null, "alice_user");
        assertFalse(aliceAccounts.isEmpty(), "Expected accounts for alice_user");
        for (Account acc : aliceAccounts) {
            assertTrue(acc.getCustomer().getUserName().equals("alice_user"), "All accounts should belong to alice_user");
        }
    }

    @Test
    void searchWithNoFilters_returnsAllAccounts() {
        List<Account> accounts = accountRepository.search(null, null, null);
        // We seeded 3 accounts in setup
        assertTrue(accounts.size() >= 3, "Expected at least the seeded accounts when no filters provided");
    }

    @Test
    void testDeleteAccount() {
        // create a unique customer and account to delete
        Customer deleteUser = new Customer("Delete", "User", "delete_user", "pass123");
        userRepository.addUsers(deleteUser);
        Account deleteAccount = deleteUser.getCheckingAccount();
        accountRepository.addAccount(deleteAccount);

        String accountNumber = fetchAccountNumberForUser("delete_user");
        List<Account> before = accountRepository.search(accountNumber, null, null);
        assertFalse(before.isEmpty(), "Expected the account to exist before deletion.");

        accountRepository.deleteAccount(accountNumber, "ADMIN");

        List<Account> after = accountRepository.search(accountNumber, null, null);
        assertTrue(after.isEmpty(), "Expected the account to be deleted.");
    }

    private String fetchAccountNumberForUser(String username) {
        String sql = "SELECT account_number FROM accounts a JOIN users u ON u.id = a.customer_id WHERE u.username = ? LIMIT 1";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("account_number");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to fetch account number for " + username, e);
        }
        throw new IllegalStateException("No account found for user " + username);
    }
}
