package server;

import bank.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SecurityService {
    private final DatabaseManager dbManager;

    public SecurityService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void initialize() {
        String sql = "CREATE TABLE IF NOT EXISTS user_security_settings (" +
                "user_id INTEGER PRIMARY KEY," +
                "force_password_change INTEGER DEFAULT 0," +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ");";
        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize security settings table", e);
        }
    }

    public void setForcePasswordChange(long userId, boolean force) {
        String sql = "INSERT OR REPLACE INTO user_security_settings (user_id, force_password_change) VALUES (?, ?)";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setInt(2, force ? 1 : 0);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error setting force password change: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to set force password change flag", e);
        }
    }

    public boolean isPasswordChangeRequired(long userId) {
        String sql = "SELECT force_password_change FROM user_security_settings WHERE user_id = ?";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("force_password_change") == 1;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false; // Default to false if no record found
    }
}
