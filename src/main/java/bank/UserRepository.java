package bank;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {
    // Handles opening connections and making sure the SQLite schema exists.
    private final DatabaseManager databaseManager;
    // Lightweight cache so callers can inspect the last loaded/added users without hitting the DB.
    private final List<User> userList;

    public UserRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.databaseManager.initialize();
        this.userList = new ArrayList<>();
    }

    public void addUsers(User user) {
        System.out.println("Adding user: " + user.getUserName());
        // UPSERT keeps the table in sync even if the same username is inserted twice (updates instead).
        String sql = "INSERT INTO users (first_name, last_name, username, password, role) "
                + "VALUES (?, ?, ?, ?, ?) "
                + "ON CONFLICT(username) DO UPDATE SET "
                + "first_name = excluded.first_name, "
                + "last_name = excluded.last_name, "
                + "password = excluded.password, "
                + "role = excluded.role;";
        // TODO: guard against concurrent modifications by synchronizing on the repository or using DB transactions.
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getFirstName());
            statement.setString(2, user.getLastName());
            statement.setString(3, user.getUserName());
            statement.setString(4, user.getPassword());
            statement.setString(5, user.getClass().getSimpleName().toUpperCase());
            statement.executeUpdate();
            userList.removeIf(existing -> existing.getUserName().equals(user.getUserName()));
            userList.add(user);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to add user " + user.getUserName(), e);
        }
    }

    public void deleteUsers(User user) {
        System.out.println("Deleting user: " + user.getUserName());
        if (!canDelete(user)) {
            System.out.println("Deletion aborted: cannot remove the last admin from the system.");
            return;
        }
        String sql = "DELETE FROM users WHERE username = ?";
        // TODO: ensure concurrent deletes don't race by using database-level locks or transactions.
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getUserName());
            statement.executeUpdate();
            userList.removeIf(existing -> existing.getUserName().equals(user.getUserName()));
        } catch (SQLException e) {
            throw new RuntimeException("Unable to delete user " + user.getUserName(), e);
        }
    }

    /**
     * Searches for users based on the given criteria.
     *
     * @param usernameFragment username to look for (exact match). If null/blank, no username filter is applied.
     * @param roleFilter role to restrict results to ("CUSTOMER", "TELLER", "ADMIN"), case-insensitive.
     *                   If null/blank, all roles are allowed.
     * @return list of matching users (possibly empty)
     */
    public List<User> search(String usernameFragment, String roleFilter) {
        System.out.println("Searching users. username=" + usernameFragment + ", role=" + roleFilter);

        StringBuilder sql = new StringBuilder(
                "SELECT first_name, last_name, username, password, role FROM users WHERE 1=1"
        );

        boolean hasUsername = usernameFragment != null && !usernameFragment.isBlank();
        boolean hasRole = roleFilter != null && !roleFilter.isBlank();

        if (hasUsername) {
            // For SRS use cases like login and create user, we want exact username matching.
            sql.append(" AND username = ?");
        }
        if (hasRole) {
            sql.append(" AND UPPER(role) = UPPER(?)");
        }

        List<User> results = new ArrayList<>();

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            if (hasUsername) {
                statement.setString(paramIndex++, usernameFragment);
            }
            if (hasRole) {
                statement.setString(paramIndex++, roleFilter);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(mapRowToUser(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to search for users", e);
        }

        return results;
    }

    /**
     * Convenience overload used in many SRS scenarios where only username matters,
     * e.g., login or checking if a user already exists before creation.
     */
    public List<User> search(String usernameFragment) {
        return search(usernameFragment, null);
    }

    /**
     * Returns all users with the given role (e.g. "CUSTOMER", "TELLER", "ADMIN").
     * If role is null/blank, returns all users.
     */
    public List<User> searchByRole(String role) {
        return search(null, role);
    }

    /**
     * SRS: used by tellers to search customers.
     * Teller can only search CUSTOMER users, optionally filtered by username.
     */
    public List<User> searchCustomersForTeller(String usernameFragment) {
        return search(usernameFragment, "CUSTOMER");
    }

    /**
     * SRS: used by admins to search any user type (CUSTOMER, TELLER, ADMIN)
     * based on username and optional role filter. If roleFilter is null, all roles are allowed.
     */
    public List<User> searchForAdmin(String usernameFragment, String roleFilter) {
        return search(usernameFragment, roleFilter);
    }

    // Currently unused; available if we ever cache user entities again.
    public void save() {
        System.out.println("Saving in-memory users to the database.");
        for (User user : new ArrayList<>(userList)) {
            addUsers(user);
        }
    }

    // Currently unused; reserved for future cache warmups.
    public void load() {
        System.out.println("Loading users from the database into memory.");
        userList.clear();
        String sql = "SELECT first_name, last_name, username, password, role FROM users";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                userList.add(mapRowToUser(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to load users from the database", e);
        }
    }

    public List<User> getUserList() {
        return userList;
    }

    public void listAll() {
        String sql = "SELECT first_name, last_name, username, password, role FROM users";
        boolean found = false;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                User user = mapRowToUser(resultSet);
                found = true;
                if (user != null) {
                    user.printUserInfo();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to list users", e);
        }

        if (!found) {
            System.out.println("No users found.");
        }
    }

    public void updatePassword(User user) {
        String sql = "UPDATE users SET password = ? WHERE username = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getPassword());
            statement.setString(2, user.getUserName());
            statement.executeUpdate();

            userList.removeIf(existing -> existing.getUserName().equals(user.getUserName()));
            userList.add(user);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to update password for " + user.getUserName(), e);
        }
    }

    public void updatePassword(String username, String newPassword) {
        String sql = "UPDATE users SET password = ? WHERE username = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, newPassword);
            statement.setString(2, username);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update password for user: " + username, e);
        }
    }

    private boolean canDelete(User user) {
        String role = user instanceof Admin ? "ADMIN" : null;
        if (role == null) {
            return true;
        }
        String sql = "SELECT COUNT(*) AS total FROM users WHERE role = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, role);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("total") > 1;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to verify delete constraints for " + user.getUserName(), e);
        }
        return false;
    }

    public String findRoleByUsername(String username) {
        String sql = "SELECT role FROM users WHERE username = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("role");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to retrieve role for " + username, e);
        }
        return null;
    }

    private User mapRowToUser(ResultSet resultSet) throws SQLException {
        String firstName = resultSet.getString("first_name");
        String lastName = resultSet.getString("last_name");
        String username = resultSet.getString("username");
        String password = resultSet.getString("password");
        String role = resultSet.getString("role");
        // Role column tells us which concrete subclass to materialize when loading from the DB.
        switch (role.toUpperCase()) {
            case "ADMIN":
                return new Admin(firstName, lastName, username, password, null, this);
            case "TELLER":
                return new Teller(firstName, lastName, username, password, null, this);
            default:
                return new Customer(firstName, lastName, username, password);
        }
    }
}
