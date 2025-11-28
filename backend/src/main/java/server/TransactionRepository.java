package server;

import bank.DatabaseManager;
import bank.Transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

public class TransactionRepository {
    private final DatabaseManager databaseManager;

    public TransactionRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.databaseManager.initialize();
    }

    public void addTransaction(long accountId, Transaction transaction) {
        String sql = "INSERT INTO transactions (account_id, amount, type, description, created_at) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accountId);
            statement.setDouble(2, transaction.getAmount());
            statement.setString(3, transaction.getType());
            statement.setString(4, transaction.getDescription());
            statement.setString(5, transaction.getTimestamp().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to add transaction", e);
        }
    }

    public List<Transaction> getTransactionsByAccountId(long accountId) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT id, amount, type, description, created_at " +
                "FROM transactions " +
                "WHERE account_id = ? " +
                "ORDER BY created_at DESC";
        try (Connection connection = databaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Transaction transaction = new Transaction(
                            String.valueOf(resultSet.getLong("id")),
                            resultSet.getDouble("amount"),
                            resultSet.getString("type"),
                            resultSet.getString("description"));
                    transactions.add(transaction);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to get transactions for account", e);
        }
        return transactions;
    }

    public List<Transaction> searchTransactions(long accountId, String type, Double amount) {
        List<Transaction> results = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT id, amount, type, description, created_at " +
                        "FROM transactions " +
                        "WHERE account_id = ?");

        boolean hasType = type != null && !type.isBlank();
        boolean hasAmount = amount != null;

        if (hasType) {
            sql.append(" AND type = ?");
        }
        if (hasAmount) {
            sql.append(" AND amount = ?");
        }

        sql.append(" ORDER BY created_at DESC");

        try (Connection connection = databaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql.toString())) {

            statement.setLong(1, accountId);
            int paramIndex = 2;

            if (hasType) {
                statement.setString(paramIndex++, type);
            }
            if (hasAmount) {
                statement.setDouble(paramIndex++, amount);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Transaction transaction = new Transaction(
                            String.valueOf(resultSet.getLong("id")),
                            resultSet.getDouble("amount"),
                            resultSet.getString("type"),
                            resultSet.getString("description"));
                    results.add(transaction);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to search transactions", e);
        }

        return results;
    }

    public long findAccountId(String accountNumber) {
        String sql = "SELECT id FROM accounts WHERE account_number = ?";
        try (Connection connection = databaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("id");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to locate account " + accountNumber, e);
        }
        throw new IllegalStateException("Account " + accountNumber + " does not exist in the database.");
    }
}
