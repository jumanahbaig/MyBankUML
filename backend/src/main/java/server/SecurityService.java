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
                "failed_attempts INTEGER DEFAULT 0," +
                "locked_until TEXT," +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ");";
        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute(sql);

            // Try to add columns if they don't exist (migration)
            try {
                stmt.execute("ALTER TABLE user_security_settings ADD COLUMN failed_attempts INTEGER DEFAULT 0");
            } catch (SQLException e) {
                // Column likely already exists
            }
            try {
                stmt.execute("ALTER TABLE user_security_settings ADD COLUMN locked_until TEXT");
            } catch (SQLException e) {
                // Column likely already exists
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize security settings table", e);
        }
    }

    public void setForcePasswordChange(long userId, boolean force) {
        ensureRecordExists(userId);
        String sql = "UPDATE user_security_settings SET force_password_change = ? WHERE user_id = ?";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, force ? 1 : 0);
            stmt.setLong(2, userId);
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
        return false;
    }

    public void recordFailedAttempt(long userId) {
        ensureRecordExists(userId);
        // First get current attempts
        int attempts = getFailedAttempts(userId);
        attempts++;

        String sql = "UPDATE user_security_settings SET failed_attempts = ? WHERE user_id = ?";

        // If 5th attempt, lock for 24 hours
        if (attempts >= 5) {
            sql = "UPDATE user_security_settings SET failed_attempts = ?, locked_until = datetime('now', '+1 day') WHERE user_id = ?";
        }

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, attempts);
            stmt.setLong(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to record failed attempt", e);
        }
    }

    public void resetFailedAttempts(long userId) {
        ensureRecordExists(userId);
        String sql = "UPDATE user_security_settings SET failed_attempts = 0, locked_until = NULL WHERE user_id = ?";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to reset failed attempts", e);
        }
    }

    public boolean isLocked(long userId) {
        String sql = "SELECT locked_until FROM user_security_settings WHERE user_id = ?";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String lockedUntil = rs.getString("locked_until");
                    if (lockedUntil != null) {
                        // Check if lock is still valid
                        return checkLockDate(lockedUntil);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean checkLockDate(String lockedUntil) {
        String sql = "SELECT datetime('now') < ?";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, lockedUntil);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int getFailedAttempts(long userId) {
        String sql = "SELECT failed_attempts FROM user_security_settings WHERE user_id = ?";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("failed_attempts");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void unlockUser(long userId) {
        resetFailedAttempts(userId);
    }

    private void ensureRecordExists(long userId) {
        String sql = "INSERT OR IGNORE INTO user_security_settings (user_id, force_password_change, failed_attempts) VALUES (?, 0, 0)";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to ensure security record exists", e);
        }
    }
}
