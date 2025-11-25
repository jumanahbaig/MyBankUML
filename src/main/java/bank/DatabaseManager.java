package bank;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
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
            // Load the SQLite driver once so DriverManager can create jdbc:sqlite connections later.
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("SQLite JDBC driver not found on classpath", e);
        }
    }

    private static final String CREATE_USERS_TABLE =
            "CREATE TABLE IF NOT EXISTS users ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "first_name TEXT NOT NULL,"
                    + "last_name TEXT NOT NULL,"
                    + "username TEXT NOT NULL UNIQUE,"
                    + "password TEXT NOT NULL,"
                    + "role TEXT NOT NULL,"
                    + "created_at TEXT DEFAULT CURRENT_TIMESTAMP"
                    + ");";

    private static final String CREATE_ACCOUNTS_TABLE =
            "CREATE TABLE IF NOT EXISTS accounts ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "customer_id INTEGER NOT NULL,"
                    + "account_type TEXT NOT NULL,"
                    + "account_number TEXT NOT NULL UNIQUE,"
                    + "balance REAL NOT NULL DEFAULT 0,"
                    + "created_at TEXT DEFAULT CURRENT_TIMESTAMP,"
                    + "FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,"
                    + "UNIQUE(customer_id, account_type)"
                    + ");";

    private static final String CREATE_TRANSACTIONS_TABLE =
            "CREATE TABLE IF NOT EXISTS transactions ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "account_id INTEGER NOT NULL,"
                    + "amount REAL NOT NULL,"
                    + "type TEXT NOT NULL,"
                    + "description TEXT,"
                    + "created_at TEXT DEFAULT CURRENT_TIMESTAMP,"
                    + "FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE"
                    + ");";

    public void initialize() {
        // Create the physical db file if it does not already exist, then bootstrap tables + PRAGMAs.
        ensureDatabaseFileExists();
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("PRAGMA foreign_keys = ON");
            statement.executeUpdate(CREATE_USERS_TABLE);
            statement.executeUpdate(CREATE_ACCOUNTS_TABLE);
            statement.executeUpdate(CREATE_TRANSACTIONS_TABLE);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }

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
