package bank;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Coordinates the SQLite database lifecycle and makes sure the schema exists.
 */
public class DatabaseManager {
    private static final String DB_FILE = getDatabasePath();
    private static final String JDBC_URL = "jdbc:sqlite:" + DB_FILE;

    private static String getDatabasePath() {
        String currentDir = System.getProperty("user.dir");
        if (currentDir.endsWith("backend")) {
            return "../bank.db";
        }
        return "bank.db";
    }

    static {
        try {
            // Load the SQLite driver once so DriverManager can create jdbc:sqlite
            // connections later.
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("SQLite JDBC driver not found on classpath", e);
        }
    }

    private static final String CREATE_USERS_TABLE = "CREATE TABLE IF NOT EXISTS users ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "first_name TEXT NOT NULL,"
            + "last_name TEXT NOT NULL,"
            + "username TEXT NOT NULL UNIQUE,"
            + "password TEXT NOT NULL,"
            + "role TEXT NOT NULL,"
            + "created_at TEXT DEFAULT CURRENT_TIMESTAMP"
            + ");";

    private static final String CREATE_ACCOUNTS_TABLE = "CREATE TABLE IF NOT EXISTS accounts ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "customer_id INTEGER NOT NULL,"
            + "account_type TEXT NOT NULL,"
            + "account_number TEXT NOT NULL UNIQUE,"
            + "balance REAL NOT NULL DEFAULT 0,"
            + "created_at TEXT DEFAULT CURRENT_TIMESTAMP,"
            + "FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE"
            + ");";

    private static final String CREATE_UNIQUE_CHECKING_INDEX = "CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_checking ON accounts(customer_id, account_type) WHERE account_type = 'CHECK';";

    private static final String CREATE_TRANSACTIONS_TABLE = "CREATE TABLE IF NOT EXISTS transactions ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "account_id INTEGER NOT NULL,"
            + "amount REAL NOT NULL,"
            + "type TEXT NOT NULL,"
            + "description TEXT,"
            + "created_at TEXT DEFAULT CURRENT_TIMESTAMP,"
            + "FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE"
            + ");";

    public void initialize() {
        // Create the physical db file if it does not already exist, then bootstrap
        // tables + PRAGMAs.
        ensureDatabaseFileExists();
        try (Connection connection = getConnection();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("PRAGMA foreign_keys = ON");
            statement.executeUpdate(CREATE_USERS_TABLE);

            // Check if we need to migrate accounts table (if it has the old unique
            // constraint)
            migrateAccountsTable(connection);

            statement.executeUpdate(CREATE_ACCOUNTS_TABLE);
            statement.executeUpdate(CREATE_UNIQUE_CHECKING_INDEX);
            statement.executeUpdate(CREATE_TRANSACTIONS_TABLE);
            statement.executeUpdate(CREATE_ACCOUNT_REQUESTS_TABLE);
            statement.executeUpdate(CREATE_PASSWORD_RESET_REQUESTS_TABLE);
            statement.executeUpdate(CREATE_ACCOUNT_DELETION_REQUESTS_TABLE);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }

    private void migrateAccountsTable(Connection connection) throws SQLException {
        // Check if the old unique constraint exists by checking for the auto-index or
        // just try to migrate
        // Simplest way for this POC: Check if we can insert a duplicate non-checking
        // account.
        // Or better: Inspect table info.
        // Given constraints of the environment, we'll perform a safe migration:
        // 1. Rename old table
        // 2. Create new table without constraint
        // 3. Copy data
        // 4. Drop old table
        // We only do this if the table exists and we haven't already migrated (we can
        // check index existence)

        boolean indexExists = false;
        try (ResultSet rs = connection.getMetaData().getIndexInfo(null, null, "accounts", true, false)) {
            while (rs.next()) {
                if ("idx_unique_checking".equals(rs.getString("INDEX_NAME"))) {
                    indexExists = true;
                    break;
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        if (indexExists)
            return; // Already migrated

        // Check if table exists
        boolean tableExists = false;
        try (ResultSet rs = connection.getMetaData().getTables(null, null, "accounts", null)) {
            if (rs.next())
                tableExists = true;
        }

        if (!tableExists)
            return; // Nothing to migrate

        System.out.println("Migrating accounts table schema...");
        Statement stmt = connection.createStatement();
        connection.setAutoCommit(false);
        try {
            stmt.executeUpdate("ALTER TABLE accounts RENAME TO accounts_old");
            stmt.executeUpdate(CREATE_ACCOUNTS_TABLE);
            stmt.executeUpdate(
                    "INSERT INTO accounts (id, customer_id, account_type, account_number, balance, created_at) " +
                            "SELECT id, customer_id, account_type, account_number, balance, created_at FROM accounts_old");
            stmt.executeUpdate("DROP TABLE accounts_old");
            stmt.executeUpdate(CREATE_UNIQUE_CHECKING_INDEX);
            connection.commit();
            System.out.println("Migration successful.");
        } catch (SQLException e) {
            connection.rollback();
            // If migration fails (e.g. table doesn't exist yet), just ignore or log
            System.err.println("Migration failed (might be fresh DB): " + e.getMessage());
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private static final String CREATE_ACCOUNT_REQUESTS_TABLE = "CREATE TABLE IF NOT EXISTS account_requests ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "user_id INTEGER NOT NULL,"
            + "account_type TEXT NOT NULL,"
            + "status TEXT DEFAULT 'pending',"
            + "requested_at TEXT DEFAULT CURRENT_TIMESTAMP,"
            + "resolved_at TEXT,"
            + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE"
            + ");";

    private static final String CREATE_PASSWORD_RESET_REQUESTS_TABLE = "CREATE TABLE IF NOT EXISTS password_reset_requests ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "user_id INTEGER NOT NULL,"
            + "username TEXT NOT NULL,"
            + "user_email TEXT,"
            + "status TEXT DEFAULT 'pending',"
            + "requested_at TEXT DEFAULT CURRENT_TIMESTAMP,"
            + "resolved_at TEXT,"
            + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE"
            + ");";

    private static final String CREATE_ACCOUNT_DELETION_REQUESTS_TABLE = "CREATE TABLE IF NOT EXISTS account_deletion_requests ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "user_id INTEGER NOT NULL,"
            + "account_id INTEGER NOT NULL,"
            + "status TEXT DEFAULT 'pending',"
            + "reason TEXT,"
            + "requested_at TEXT DEFAULT CURRENT_TIMESTAMP,"
            + "resolved_at TEXT,"
            + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,"
            + "FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE"
            + ");";

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL);
    }

    private void ensureDatabaseFileExists() {
        Path dbPath = Path.of(DB_FILE);
        if (Files.exists(dbPath)) {
            return;
        }
        try {
            Files.createFile(dbPath);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create database file " + DB_FILE, e);
        }
    }
}
