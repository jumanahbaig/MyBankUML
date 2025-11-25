package bank;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AccountRepository {
    private Account accountType;
    private List<Account> accountList;
    // Shared DatabaseManager ensures every repository instance talks to the same SQLite file.
    private final DatabaseManager databaseManager;
    private final Logs logs;

    public AccountRepository(DatabaseManager databaseManager) {
        this(databaseManager, null);
    }

    public AccountRepository(DatabaseManager databaseManager, Logs logs) {
        this.databaseManager = databaseManager;
        this.databaseManager.initialize();
        this.accountList = new ArrayList<>();
        this.logs = logs;
    }

    public void addAccount(Account account) {
        this.accountType = account;
        // Each insert writes the account owner + account type into the accounts table.
        System.out.println("Adding account: " + account.getClass().getSimpleName());
        // TODO: prevent race conditions when multiple accounts are created simultaneously (synchronize sequence generation).
        long customerId = findCustomerId(account.getCustomer());
        String sql = "INSERT INTO accounts (customer_id, account_type, account_number) "
                + "VALUES (?, ?, ?) "
                + "ON CONFLICT(customer_id, account_type) DO UPDATE SET "
                + "account_number = excluded.account_number;";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            statement.setString(2, account.getClass().getSimpleName().toUpperCase());
            statement.setString(3, generateAccountNumber(connection));
            statement.executeUpdate();
            accountList.add(account);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to add account for " + account.getCustomer().getUserName(), e);
        }
    }

    public void deleteAccount(Account account) {
        // Removes both the row in the DB and the cached instance for the given customer/type combo.
        System.out.println("Deleting account: " + account.getClass().getSimpleName());
        long customerId = findCustomerId(account.getCustomer());
        String sql = "DELETE FROM accounts "
                + "WHERE customer_id = ? AND account_type = ?;";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            statement.setString(2, account.getClass().getSimpleName().toUpperCase());
            statement.executeUpdate();
            accountList.removeIf(existing -> existing.getClass().equals(account.getClass())
                    && existing.getCustomer().getUserName().equals(account.getCustomer().getUserName()));
        } catch (SQLException e) {
            throw new RuntimeException("Unable to delete account for " + account.getCustomer().getUserName(), e);
        }
    }

    /**
     * Deletes an account by account number (if it exists) and logs the deletion when possible.
     * Only ADMIN role may delete accounts.
     */
    public void deleteAccount(String accountNumber, String userRole) {
        if (userRole == null || !"ADMIN".equalsIgnoreCase(userRole)) {
            throw new IllegalStateException("Only admins can delete accounts.");
        }

        String sql = "DELETE FROM accounts WHERE account_number = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountNumber);
            int rows = statement.executeUpdate();
            if (rows > 0) {
                if (logs != null) {
                    logs.append(
                        "SYSTEM",
                        "DELETE_ACCOUNT",
                        accountNumber,
                        "Account " + accountNumber + " deleted by admin."
                    );
                }
            } else {
                System.out.println("Account with number " + accountNumber + " does not exist.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting account", e);
        }
    }

    public void display(Customer customer) {
        System.out.println("Displaying accounts for customer: " + customer.getUserName());
        long customerId = findCustomerId(customer);
        String sql = "SELECT account_number, account_type, balance, created_at "
                + "FROM accounts "
                + "WHERE customer_id = ?;";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    System.out.printf("Account %s type=%s balance=%.2f created=%s%n",
                            resultSet.getString("account_number"),
                            resultSet.getString("account_type"),
                            resultSet.getDouble("balance"),
                            resultSet.getString("created_at"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to display accounts for " + customer.getUserName(), e);
        }
    }

    // Currently unused; kept in case we want to flush the cache explicitly.
    public void save() {
        System.out.println("Saving in-memory accounts to the database.");
        for (Account account : new ArrayList<>(accountList)) {
            addAccount(account);
        }
    }

    // Currently unused; intended for future cache warmups.
    public void load() {
        System.out.println("Loading accounts from the database.");
        // Refresh the in-memory list from scratch so it reflects the current database snapshot.
        accountList = new ArrayList<>();
        String sql = "SELECT a.account_type, u.first_name, u.last_name, u.username, u.password "
                + "FROM accounts a "
                + "JOIN users u ON u.id = a.customer_id;";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Customer owner = new Customer(
                        resultSet.getString("first_name"),
                        resultSet.getString("last_name"),
                        resultSet.getString("username"),
                        resultSet.getString("password"));
                accountList.add(createAccountInstance(resultSet.getString("account_type"), owner));
            }
            if (!accountList.isEmpty()) {
                accountType = accountList.get(accountList.size() - 1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to load accounts from the database", e);
        }
    }

    public Account getAccountType() {
        return accountType;
    }

    public void setAccountType(Account accountType) {
        this.accountType = accountType;
    }

    public List<Account> getAccountList() {
        return accountList;
    }

    public void setAccountList(List<Account> accountList) {
        this.accountList = accountList;
    }

    /**
     * Gets a single account by its database ID.
     */
    public Account getAccountById(long accountId) {
        String sql = "SELECT a.account_type, a.account_number, u.first_name, u.last_name, u.username, u.password " +
                "FROM accounts a " +
                "JOIN users u ON u.id = a.customer_id " +
                "WHERE a.id = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Customer owner = new Customer(
                            resultSet.getString("first_name"),
                            resultSet.getString("last_name"),
                            resultSet.getString("username"),
                            resultSet.getString("password")
                    );
                    return createAccountInstance(resultSet.getString("account_type"), owner);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to get account by ID", e);
        }
        throw new IllegalStateException("Account with ID " + accountId + " does not exist.");
    }

    /**
     * Gets a single account by its account number.
     */
    public Account getAccountByNumber(String accountNumber) {
        String sql = "SELECT a.account_type, a.account_number, u.first_name, u.last_name, u.username, u.password " +
                "FROM accounts a " +
                "JOIN users u ON u.id = a.customer_id " +
                "WHERE a.account_number = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Customer owner = new Customer(
                            resultSet.getString("first_name"),
                            resultSet.getString("last_name"),
                            resultSet.getString("username"),
                            resultSet.getString("password")
                    );
                    return createAccountInstance(resultSet.getString("account_type"), owner);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to get account by number", e);
        }
        throw new IllegalStateException("Account with number " + accountNumber + " does not exist.");
    }

    /**
     * Searches accounts by optional account number, account type, and customer username.
     * If all filters are null/blank, returns all accounts.
     */
    public List<Account> search(String accountNumber, String accountType, String username) {
        List<Account> results = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT a.account_type, a.account_number, u.first_name, u.last_name, u.username, u.password " +
                        "FROM accounts a " +
                        "JOIN users u ON u.id = a.customer_id " +
                        "WHERE 1=1"
        );

        boolean hasNumber = accountNumber != null && !accountNumber.isBlank();
        boolean hasType = accountType != null && !accountType.isBlank();
        boolean hasUser = username != null && !username.isBlank();

        if (hasNumber) {
            sql.append(" AND a.account_number = ?");
        }
        if (hasType) {
            sql.append(" AND a.account_type = ?");
        }
        if (hasUser) {
            sql.append(" AND u.username = ?");
        }

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            if (hasNumber) {
                statement.setString(paramIndex++, accountNumber);
            }
            if (hasType) {
                statement.setString(paramIndex++, accountType.toUpperCase());
            }
            if (hasUser) {
                statement.setString(paramIndex++, username);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Customer owner = new Customer(
                            resultSet.getString("first_name"),
                            resultSet.getString("last_name"),
                            resultSet.getString("username"),
                            resultSet.getString("password")
                    );
                    results.add(createAccountInstance(resultSet.getString("account_type"), owner));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to search accounts", e);
        }

        return results;
    }

    /**
     * Displays all accounts for the given username (SRS requirement).
     * Prints one line per account; prints a "no accounts" message if none are found.
     */
    public void displayAccountsForCustomer(String username) {
        String sql = "SELECT a.account_type, a.account_number, a.balance, u.first_name, u.last_name, u.username, u.password " +
                     "FROM accounts a " +
                     "JOIN users u ON u.id = a.customer_id " +
                     "WHERE u.username = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);

            boolean found = false;
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    found = true;
                    Account account = mapRowToAccount(resultSet);
                    String accountNumber = resultSet.getString("account_number");
                    double balance = resultSet.getDouble("balance");

                    System.out.println(
                        "Account " + accountNumber +
                        " | Type: " + account.getClass().getSimpleName() +
                        " | Balance: " + balance
                    );
                }
            }

            if (!found) {
                System.out.println("No accounts found for user: " + username);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Unable to display accounts for user: " + username, e);
        }
    }

    /**
     * Generates the next globally unique account number in the format ACCT-XXXXXXXXXX.
     * Relies on the database to track the highest value, avoiding duplicates even across restarts.
     */
    private String generateAccountNumber(Connection connection) throws SQLException {
        // Fetch the most recent numeric identifier and increment it for a global sequence.
        // TODO: handle concurrent increments safely (e.g., using database sequences or locks).
        String sql = "SELECT MAX(account_number) AS last_number FROM accounts";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            long nextValue = 1;
            if (resultSet.next()) {
                String lastValue = resultSet.getString("last_number");
                if (lastValue != null && lastValue.matches("ACCT-(\\d{10})")) {
                    nextValue = Long.parseLong(lastValue.substring(5)) + 1;
                }
            }
            return String.format("ACCT-%010d", nextValue);
        }
    }

    private long findCustomerId(Customer customer) {
        // Look up the numeric database identifier for this username so we can store it as the account owner.
        String sql = "SELECT id FROM users WHERE username = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customer.getUserName());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("id");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to locate customer " + customer.getUserName(), e);
        }
        throw new IllegalStateException("Customer " + customer.getUserName() + " does not exist in the database.");
    }

    private Account createAccountInstance(String accountType, Customer owner) {
        String normalized = accountType.toUpperCase();
        switch (normalized) {
            case "CARD":
                return new Card(owner);
            case "SAVING":
                return new Saving(owner);
            case "CHECK":
                return owner.getCheckingAccount();
            default:
                throw new IllegalArgumentException("Unknown account type: " + accountType);
        }
    }

    private Account mapRowToAccount(ResultSet resultSet) throws SQLException {
        Customer owner = new Customer(
                resultSet.getString("first_name"),
                resultSet.getString("last_name"),
                resultSet.getString("username"),
                resultSet.getString("password")
        );
        return createAccountInstance(resultSet.getString("account_type"), owner);
    }
}
