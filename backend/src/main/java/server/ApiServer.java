package server;

import bank.*;
import server.dto.*;
import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ApiServer {
    private static final Gson gson = new Gson();
    private static final DatabaseManager dbManager = new DatabaseManager();
    private static final UserRepository userRepository = new UserRepository(dbManager);
    private static final AccountRepository accountRepository = new AccountRepository(dbManager);
    private static final TransactionRepository transactionRepository = new TransactionRepository(dbManager);
    private static final CustomerService customerService = new CustomerService(userRepository, accountRepository);
    private static final SecurityService securityService = new SecurityService(dbManager);

    public static void main(String[] args) {
        System.out.println("Initializing database...");
        dbManager.initialize();
        securityService.initialize();

        System.out.println("Checking database content...");
        try {
            List<User> allUsers = userRepository.search(null, null);
            System.out.println("Total users in database: " + allUsers.size());
            if (allUsers.isEmpty()) {
                System.err.println("WARNING: Database is empty! Please run DataSeeder to populate the database.");
            } else {
                System.out.println("Users found:");
                for (User u : allUsers) {
                    System.out.println("  - " + u.getUserName() + " (" + u.getClass().getSimpleName() + ")");
                }
            }
        } catch (Exception e) {
            System.err.println("Error checking database: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("Starting Javalin server...");
        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.anyHost();
                });
            });
            config.jsonMapper(new io.javalin.json.JsonMapper() {
                @Override
                public String toJsonString(Object obj, java.lang.reflect.Type type) {
                    return gson.toJson(obj, type);
                }

                @Override
                public <T> T fromJsonString(String json, java.lang.reflect.Type targetType) {
                    return gson.fromJson(json, targetType);
                }
            });
        }).start(8080);

        // Auth endpoints
        app.post("/api/auth/login", ApiServer::login);
        app.post("/api/auth/logout", ApiServer::logout);
        app.post("/api/auth/change-password", ApiServer::changePassword);

        // Customer endpoints
        app.post("/api/customers", ApiServer::createCustomer);
        app.get("/api/customers/{customerId}/accounts", ApiServer::getCustomerAccounts);

        // Account request endpoints (must be before parameterized routes)
        app.post("/api/accounts/request", ApiServer::requestAccount);
        app.get("/api/accounts/requests", ApiServer::getAccountRequests);
        app.post("/api/accounts/requests/{requestId}/approve", ApiServer::approveAccountRequest);
        app.post("/api/accounts/requests/{requestId}/reject", ApiServer::rejectAccountRequest);

        // Account deletion request endpoints
        app.post("/api/accounts/{accountId}/delete-request", ApiServer::requestAccountDeletion);
        app.get("/api/admin/requests/account-deletion", ApiServer::getAccountDeletionRequests);
        app.post("/api/admin/requests/account-deletion/{requestId}/approve", ApiServer::approveAccountDeletion);
        app.post("/api/admin/requests/account-deletion/{requestId}/reject", ApiServer::rejectAccountDeletion);

        // Account endpoints
        app.get("/api/accounts/search", ApiServer::searchAccounts);
        app.get("/api/accounts/{accountId}", ApiServer::getAccountById);
        app.post("/api/accounts", ApiServer::createAccount);
        app.delete("/api/accounts/{accountNumber}", ApiServer::deleteAccount);

        // Transaction endpoints
        app.get("/api/accounts/{accountId}/transactions", ApiServer::getAccountTransactions);
        app.post("/api/accounts/{accountId}/transactions", ApiServer::createTransaction);

        // Admin endpoints
        app.get("/api/users", ApiServer::getAllUsers);
        app.get("/api/users/search", ApiServer::searchUsers);
        app.put("/api/users/{userId}/role", ApiServer::updateUserRole);
        app.put("/api/users/{userId}/status", ApiServer::toggleUserStatus);
        app.post("/api/users/{userId}/unlock", ApiServer::unlockUser);
        app.delete("/api/users/{userId}", ApiServer::deleteUser);
        app.post("/api/users", ApiServer::createUser);

        // Password reset endpoints
        app.post("/api/auth/password-reset/request", ApiServer::requestPasswordReset);
        app.get("/api/auth/password-reset/requests", ApiServer::getPasswordResetRequests);
        app.post("/api/auth/password-reset/approve/{requestId}", ApiServer::approvePasswordReset);
        app.post("/api/auth/password-reset/reject/{requestId}", ApiServer::rejectPasswordReset);

        System.out.println("API Server started on port 8080");
    }

    private static void login(Context ctx) {
        try {
            LoginRequest request = ctx.bodyAsClass(LoginRequest.class);
            System.out.println("Login attempt for username: " + request.getUsername());

            long userId;
            try {
                userId = getUserId(request.getUsername());
            } catch (Exception e) {
                // User not found
                ctx.status(HttpStatus.UNAUTHORIZED)
                        .json(new ErrorResponse("UNAUTHORIZED", "Invalid credentials"));
                return;
            }

            if (securityService.isLocked(userId)) {
                ctx.status(HttpStatus.UNAUTHORIZED)
                        .json(new ErrorResponse("LOCKED",
                                "Account is locked due to too many failed attempts. Please try again in 24 hours or contact admin."));
                return;
            }

            boolean loginSuccess = customerService.loginUser(request.getUsername(), request.getPassword(), null);
            System.out.println("Login success: " + loginSuccess);

            if (loginSuccess) {
                securityService.resetFailedAttempts(userId);

                String role = getRoleForUser(request.getUsername());
                System.out.println("User role: " + role);

                User user = getUserByUsername(request.getUsername());
                System.out.println("User object retrieved: " + user.getUserName());

                String token = JwtUtil.generateToken(user.getUserName(), role.toUpperCase(), userId);
                boolean forcePasswordChange = securityService.isPasswordChangeRequired(userId);

                UserDTO userDTO = new UserDTO(userId, user.getUserName(), user.getFirstName(),
                        user.getLastName(), role.toLowerCase(),
                        getCurrentTimestamp(userId), forcePasswordChange, false);
                ctx.json(new LoginResponse(token, userDTO));
            } else {
                securityService.recordFailedAttempt(userId);
                int attempts = securityService.getFailedAttempts(userId);

                if (attempts >= 5) {
                    ctx.status(HttpStatus.UNAUTHORIZED)
                            .json(new ErrorResponse("LOCKED", "Account is locked due to too many failed attempts."));
                } else if (attempts == 4) {
                    ctx.status(HttpStatus.UNAUTHORIZED)
                            .json(new ErrorResponse("WARNING",
                                    "Invalid credentials. Warning: 1 attempt remaining before account lock."));
                } else {
                    ctx.status(HttpStatus.UNAUTHORIZED)
                            .json(new ErrorResponse("UNAUTHORIZED", "Invalid credentials"));
                }
            }
        } catch (Exception e) {
            System.err.println("Login error: " + e.getMessage());
            e.printStackTrace();
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(new ErrorResponse("ERROR", e.getMessage()));
        }
    }

    private static void logout(Context ctx) {
        ctx.status(HttpStatus.OK).result("Logged out successfully");
    }

    private static void changePassword(Context ctx) {
        try {
            String authHeader = ctx.header("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                ctx.status(HttpStatus.UNAUTHORIZED).json(new ErrorResponse("UNAUTHORIZED", "Missing token"));
                return;
            }
            String token = authHeader.substring(7);
            String username = JwtUtil.getUsernameFromToken(token);
            long userId = getUserId(username);

            Map<String, String> body = ctx.bodyAsClass(Map.class);
            String newPassword = body.get("newPassword");

            if (newPassword == null || newPassword.length() < 6) {
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(new ErrorResponse("BAD_REQUEST", "Password must be at least 6 characters"));
                return;
            }

            userRepository.updatePassword(username, newPassword);
            securityService.setForcePasswordChange(userId, false);

            ctx.json(Map.of("message", "Password updated successfully"));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(new ErrorResponse("ERROR", e.getMessage()));
        }
    }

    private static void requestPasswordReset(Context ctx) {
        try {
            var requestBody = gson.fromJson(ctx.body(), Map.class);
            String username = (String) requestBody.get("username");

            if (username == null || username.isBlank()) {
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(new ErrorResponse("BAD_REQUEST", "Username is required"));
                return;
            }

            // Check if user exists
            long userId = getUserId(username);
            if (userId == -1) {
                ctx.status(HttpStatus.NOT_FOUND)
                        .json(new ErrorResponse("NOT_FOUND", "User not found"));
                return;
            }

            // Create password reset request
            String sql = "INSERT INTO password_reset_requests (user_id, username, status, requested_at) " +
                    "VALUES (?, ?, 'pending', datetime('now'))";

            try (Connection connection = dbManager.getConnection();
                    PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, userId);
                statement.setString(2, username);
                statement.executeUpdate();

                ctx.status(HttpStatus.CREATED)
                        .json(Map.of("message", "Password reset request created successfully"));
            }
        } catch (SQLException e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(new ErrorResponse("ERROR", "Failed to create password reset request"));
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                    .json(new ErrorResponse("ERROR", e.getMessage()));
        }
    }

    private static void getPasswordResetRequests(Context ctx) {
        try {
            // Only admins can view password reset requests
            String authHeader = ctx.header("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                ctx.status(HttpStatus.UNAUTHORIZED)
                        .json(new ErrorResponse("UNAUTHORIZED", "Missing token"));
                return;
            }

            String token = authHeader.substring(7);
            String role = JwtUtil.getRoleFromToken(token);

            if (!"admin".equalsIgnoreCase(role)) {
                ctx.status(HttpStatus.FORBIDDEN)
                        .json(new ErrorResponse("FORBIDDEN", "Only admins can view password reset requests"));
                return;
            }

            List<PasswordResetRequestDTO> requests = new ArrayList<>();
            String sql = "SELECT id, user_id, username, status, requested_at, resolved_at " +
                    "FROM password_reset_requests WHERE status = 'pending' ORDER BY requested_at DESC";

            try (Connection connection = dbManager.getConnection();
                    PreparedStatement statement = connection.prepareStatement(sql);
                    ResultSet rs = statement.executeQuery()) {

                while (rs.next()) {
                    requests.add(new PasswordResetRequestDTO(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("username") + "@bank.com", // Mock email
                            rs.getString("status"),
                            rs.getString("requested_at"),
                            rs.getString("resolved_at")));
                }
            }

            ctx.json(requests);
        } catch (SQLException e) {
            e.printStackTrace();
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(new ErrorResponse("ERROR", "Failed to fetch password reset requests"));
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                    .json(new ErrorResponse("ERROR", e.getMessage()));
        }
    }

    private static void approvePasswordReset(Context ctx) {
        try {
            // Only admins can approve password reset requests
            String authHeader = ctx.header("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                ctx.status(HttpStatus.UNAUTHORIZED)
                        .json(new ErrorResponse("UNAUTHORIZED", "Missing token"));
                return;
            }

            String token = authHeader.substring(7);
            String role = JwtUtil.getRoleFromToken(token);

            if (!"admin".equalsIgnoreCase(role)) {
                ctx.status(HttpStatus.FORBIDDEN)
                        .json(new ErrorResponse("FORBIDDEN", "Only admins can approve password reset requests"));
                return;
            }

            long requestId = Long.parseLong(ctx.pathParam("requestId"));

            // Get username from request
            String getRequestSql = "SELECT username FROM password_reset_requests WHERE id = ? AND status = 'pending'";
            String username;

            try (Connection connection = dbManager.getConnection();
                    PreparedStatement statement = connection.prepareStatement(getRequestSql)) {
                statement.setLong(1, requestId);
                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next()) {
                        ctx.status(HttpStatus.NOT_FOUND)
                                .json(new ErrorResponse("NOT_FOUND",
                                        "Password reset request not found or already processed"));
                        return;
                    }
                    username = rs.getString("username");
                }
            }

            // Generate temporary password
            String tempPassword = java.util.UUID.randomUUID().toString().substring(0, 8);

            // Update user password
            userRepository.updatePassword(username, tempPassword);

            // Update request status
            // Update request status
            String updateSql = "UPDATE password_reset_requests SET status = 'approved', resolved_at = datetime('now') WHERE id = ?";

            try (Connection connection = dbManager.getConnection();
                    PreparedStatement statement = connection.prepareStatement(updateSql)) {
                statement.setLong(1, requestId);
                statement.executeUpdate();
            }

            // Force password change on next login
            try {
                long userId = getUserId(username);
                securityService.setForcePasswordChange(userId, true);
            } catch (Exception e) {
                System.err.println("Error setting force password change: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Failed to set force password change: " + e.getMessage(), e);
            }

            ctx.json(Map.of(
                    "message", "Password reset approved",
                    "username", username,
                    "temporaryPassword", tempPassword));
        } catch (SQLException e) {
            e.printStackTrace();
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(new ErrorResponse("ERROR", "Failed to approve password reset"));
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                    .json(new ErrorResponse("ERROR", e.getMessage()));
        }
    }

    private static void rejectPasswordReset(Context ctx) {
        try {
            // Only admins can reject password reset requests
            String authHeader = ctx.header("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                ctx.status(HttpStatus.UNAUTHORIZED)
                        .json(new ErrorResponse("UNAUTHORIZED", "Missing token"));
                return;
            }

            String token = authHeader.substring(7);
            String role = JwtUtil.getRoleFromToken(token);

            if (!"admin".equalsIgnoreCase(role)) {
                ctx.status(HttpStatus.FORBIDDEN)
                        .json(new ErrorResponse("FORBIDDEN", "Only admins can reject password reset requests"));
                return;
            }

            long requestId = Long.parseLong(ctx.pathParam("requestId"));

            String updateSql = "UPDATE password_reset_requests SET status = 'rejected', " +
                    "resolved_at = datetime('now') WHERE id = ? AND status = 'pending'";

            try (Connection connection = dbManager.getConnection();
                    PreparedStatement statement = connection.prepareStatement(updateSql)) {
                statement.setLong(1, requestId);
                int updated = statement.executeUpdate();

                if (updated == 0) {
                    ctx.status(HttpStatus.NOT_FOUND)
                            .json(new ErrorResponse("NOT_FOUND",
                                    "Password reset request not found or already processed"));
                    return;
                }
            }

            ctx.json(Map.of("message", "Password reset request rejected"));
        } catch (SQLException e) {
            e.printStackTrace();
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(new ErrorResponse("ERROR", "Failed to reject password reset"));
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                    .json(new ErrorResponse("ERROR", e.getMessage()));
        }
    }

    private static void createCustomer(Context ctx) {
        try {
            LoginRequest request = ctx.bodyAsClass(LoginRequest.class);
            Customer customer = customerService.createCustomer(
                    request.getUsername(),
                    request.getUsername(),
                    request.getUsername(),
                    request.getPassword());
            ctx.status(HttpStatus.CREATED).json(customer);
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                    .json(new ErrorResponse("ERROR", e.getMessage()));
        }
    }

    private static void getCustomerAccounts(Context ctx) {
        try {
            long customerId = Long.parseLong(ctx.pathParam("customerId"));
            String username = getUsernameById(customerId);
            List<AccountDTO> accounts = getAccountsByUsername(username);
            ctx.json(accounts);
        } catch (IllegalStateException e) {
            ctx.status(HttpStatus.NOT_FOUND)
                    .json(new ErrorResponse("NOT_FOUND", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(new ErrorResponse("ERROR", e.getMessage()));
        }
    }

    private static void getAccountById(Context ctx) {
        try {
            long accountId = Long.parseLong(ctx.pathParam("accountId"));
            AccountDTO account = fetchAccountById(accountId);
            ctx.json(account);
        } catch (Exception e) {
            ctx.status(HttpStatus.NOT_FOUND)
                    .json(new ErrorResponse("NOT_FOUND", "Account not found"));
        }
    }

    private static void searchAccounts(Context ctx) {
        try {
            String query = ctx.queryParam("query");
            int page = Integer.parseInt(ctx.queryParamAsClass("page", String.class).getOrDefault("1"));
            int limit = Integer.parseInt(ctx.queryParamAsClass("limit", String.class).getOrDefault("10"));

            List<AccountDTO> allAccounts = getAllAccountsFromDB();
            List<AccountDTO> filtered = allAccounts;

            if (query != null && !query.isBlank()) {
                filtered = allAccounts.stream()
                        .filter(a -> a.getAccountNumber().contains(query) ||
                                a.getCustomerName().toLowerCase().contains(query.toLowerCase()))
                        .toList();
            }

            int total = filtered.size();
            int totalPages = total == 0 ? 1 : (int) Math.ceil((double) total / limit);
            int start = (page - 1) * limit;
            int end = Math.min(start + limit, total);

            // Prevent IndexOutOfBoundsException
            List<AccountDTO> paged;
            if (start >= total) {
                paged = new ArrayList<>();
            } else {
                paged = filtered.subList(start, end);
            }

            SearchResponse response = new SearchResponse();
            response.accounts = paged;
            response.totalAccounts = total;
            response.currentPage = page;
            response.totalPages = totalPages;
            ctx.json(response);
        } catch (Exception e) {
            e.printStackTrace(); // Log the error
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(new ErrorResponse("ERROR", e.getMessage()));
        }
    }

    private static void createAccount(Context ctx) {
        try {
            ctx.status(HttpStatus.CREATED).result("Account created");
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                    .json(new ErrorResponse("ERROR", e.getMessage()));
        }
    }

    private static void deleteAccount(Context ctx) {
        try {
            String authHeader = ctx.header("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                ctx.status(HttpStatus.UNAUTHORIZED)
                        .json(new ErrorResponse("UNAUTHORIZED", "Missing token"));
                return;
            }

            String token = authHeader.substring(7);
            String role = JwtUtil.getRoleFromToken(token);

            String accountNumber = ctx.pathParam("accountNumber");
            accountRepository.deleteAccount(accountNumber, role);
            ctx.status(HttpStatus.OK).result("Account deleted");
        } catch (IllegalStateException e) {
            ctx.status(HttpStatus.FORBIDDEN)
                    .json(new ErrorResponse("FORBIDDEN", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(new ErrorResponse("ERROR", e.getMessage()));
        }
    }

    private static void getAccountTransactions(Context ctx) {
        try {
            long accountId = Long.parseLong(ctx.pathParam("accountId"));
            List<TransactionDTO> transactions = getTransactionsForAccount(accountId);
            ctx.json(transactions);
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(new ErrorResponse("ERROR", e.getMessage()));
        }
    }

    private static void createTransaction(Context ctx) {
        try {
            long accountId = Long.parseLong(ctx.pathParam("accountId"));
            CreateTransactionRequest request = ctx.bodyAsClass(CreateTransactionRequest.class);

            String transactionType = mapFrontendTypeToBackend(request.getType());
            Transaction transaction = new Transaction(
                    generateTransactionId(),
                    request.getAmount(),
                    transactionType,
                    request.getDescription());

            transactionRepository.addTransaction(accountId, transaction);
            updateAccountBalance(accountId, request.getAmount(), transactionType);

            TransactionDTO dto = new TransactionDTO(
                    transaction.getId(),
                    transaction.getAmount(),
                    transaction.getType(),
                    transaction.getDescription(),
                    transaction.getTimestamp().toString());

            ctx.status(HttpStatus.CREATED).json(dto);
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                    .json(new ErrorResponse("ERROR", e.getMessage()));
        }
    }

    private static void getAllUsers(Context ctx) {
        try {
            List<UserDTO> users = fetchAllUsers();
            ctx.json(users);
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(new ErrorResponse("ERROR", e.getMessage()));
        }
    }

    private static void searchUsers(Context ctx) {
        try {
            String username = ctx.queryParam("username");
            String role = ctx.queryParam("role");

            List<UserDTO> allUsers = fetchAllUsers();
            List<UserDTO> filtered = new ArrayList<>(allUsers);

            if (username != null && !username.isBlank()) {
                filtered = filtered.stream()
                        .filter(u -> u.getUsername().toLowerCase().contains(username.toLowerCase()))
                        .toList();
            }

            if (role != null && !role.isBlank()) {
                filtered = filtered.stream()
                        .filter(u -> u.getRole().equalsIgnoreCase(role))
                        .toList();
            }

            ctx.json(filtered);
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(new ErrorResponse("ERROR", e.getMessage()));
        }
    }

    private static void updateUserRole(Context ctx) {
        try {
            long userId = Long.parseLong(ctx.pathParam("userId"));
            UpdateRoleRequest request = ctx.bodyAsClass(UpdateRoleRequest.class);
            String newRole = request.getRole();

            // Get current user info
            String username = getUsernameById(userId);
            String currentRole = getRoleForUser(username);

            // Restriction 1: Cannot change role OF a customer
            if ("customer".equalsIgnoreCase(currentRole)) {
                ctx.status(HttpStatus.FORBIDDEN)
                        .json(new ErrorResponse("FORBIDDEN", "Cannot change role of a customer account"));
                return;
            }

            // Restriction 2: Cannot change role TO a customer
            if ("customer".equalsIgnoreCase(newRole)) {
                ctx.status(HttpStatus.FORBIDDEN)
                        .json(new ErrorResponse("FORBIDDEN", "Cannot change an existing user to a customer role"));
                return;
            }

            userRepository.updateRole(userId, newRole);

            // Return the updated user
            User user = getUserByUsername(username);
            String timestamp = getCurrentTimestamp(userId);
            boolean force = securityService.isPasswordChangeRequired(userId);
            boolean locked = securityService.isLocked(userId);

            UserDTO userDTO = new UserDTO(
                    userId,
                    user.getUserName(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getClass().getSimpleName().toLowerCase(),
                    timestamp,
                    force,
                    locked);

            ctx.status(HttpStatus.OK).json(userDTO);
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                    .json(new ErrorResponse("ERROR", e.getMessage()));
        }
    }

    private static void unlockUser(Context ctx) {
        try {
            String authHeader = ctx.header("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                ctx.status(HttpStatus.UNAUTHORIZED).json(new ErrorResponse("UNAUTHORIZED", "Missing token"));
                return;
            }
            String token = authHeader.substring(7);
            String role = JwtUtil.getRoleFromToken(token);
            if (!"admin".equalsIgnoreCase(role)) {
                ctx.status(HttpStatus.FORBIDDEN).json(new ErrorResponse("FORBIDDEN", "Only admins can unlock users"));
                return;
            }

            long userId = Long.parseLong(ctx.pathParam("userId"));
            securityService.unlockUser(userId);
            ctx.status(HttpStatus.OK).json(Map.of("message", "User unlocked successfully"));
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                    .json(new ErrorResponse("ERROR", e.getMessage()));
        }
    }

    private static void deleteUser(Context ctx) {
        try {
            String authHeader = ctx.header("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                ctx.status(HttpStatus.UNAUTHORIZED).json(new ErrorResponse("UNAUTHORIZED", "Missing token"));
                return;
            }
            String token = authHeader.substring(7);
            String role = JwtUtil.getRoleFromToken(token);
            if (!"admin".equalsIgnoreCase(role)) {
                ctx.status(HttpStatus.FORBIDDEN).json(new ErrorResponse("FORBIDDEN", "Only admins can delete users"));
                return;
            }

            long userId = Long.parseLong(ctx.pathParam("userId"));

            // Delete user - CASCADE will automatically delete associated accounts
            String sql = "DELETE FROM users WHERE id = ?";
            try (Connection connection = dbManager.getConnection();
                    PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, userId);
                int rowsAffected = statement.executeUpdate();

                if (rowsAffected == 0) {
                    ctx.status(HttpStatus.NOT_FOUND)
                            .json(new ErrorResponse("NOT_FOUND", "User not found"));
                    return;
                }

                ctx.status(HttpStatus.OK).json(Map.of("message", "User and associated accounts deleted successfully"));
            }
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                    .json(new ErrorResponse("ERROR", e.getMessage()));
        }
    }

    private static void toggleUserStatus(Context ctx) {
        try {
            ctx.status(HttpStatus.OK).result("Status toggled");
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                    .json(new ErrorResponse("ERROR", e.getMessage()));
        }
    }

    private static List<TransactionDTO> getTransactionsForAccount(long accountId) {
        List<TransactionDTO> transactions = new ArrayList<>();
        String sql = "SELECT id, amount, type, description, created_at FROM transactions " +
                "WHERE account_id = ? ORDER BY created_at DESC";
        try (Connection connection = dbManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    transactions.add(new TransactionDTO(
                            String.valueOf(resultSet.getLong("id")),
                            resultSet.getDouble("amount"),
                            resultSet.getString("type"),
                            resultSet.getString("description"),
                            resultSet.getString("created_at")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to get transactions", e);
        }
        return transactions;
    }

    private static List<UserDTO> fetchAllUsers() {
        List<UserDTO> users = new ArrayList<>();
        String sql = "SELECT u.id, u.username, u.first_name, u.last_name, u.role, u.created_at FROM users u";
        try (Connection connection = dbManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                long userId = resultSet.getLong("id");
                boolean force = securityService.isPasswordChangeRequired(userId);
                boolean locked = securityService.isLocked(userId);

                users.add(new UserDTO(
                        userId,
                        resultSet.getString("username"),
                        resultSet.getString("first_name"),
                        resultSet.getString("last_name"),
                        resultSet.getString("role").toLowerCase(),
                        resultSet.getString("created_at"),
                        force,
                        locked));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to get users", e);
        }
        return users;
    }

    private static String mapFrontendTypeToBackend(String frontendType) {
        return switch (frontendType.toLowerCase()) {
            case "deposit" -> "credit";
            case "withdrawal", "payment" -> "debit";
            default -> frontendType;
        };
    }

    private static String generateTransactionId() {
        return "TXN-" + System.currentTimeMillis();
    }

    // Helper classes
    private static class CreateUserRequest {
        private String firstName;
        private String lastName;
        private String username;
        private String password;
        private String role;

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public String getRole() {
            return role;
        }
    }

    private static class PasswordResetRequestDTO {
        public long id;
        public String username;
        public String userEmail;
        public String status;
        public String requestedAt;
        public String resolvedAt;

        public PasswordResetRequestDTO(long id, String username, String userEmail, String status,
                String requestedAt, String resolvedAt) {
            this.id = id;
            this.username = username;
            this.userEmail = userEmail;
            this.status = status;
            this.requestedAt = requestedAt;
            this.resolvedAt = resolvedAt;
        }
    }

    private static void createUser(Context ctx) {
        try {
            String authHeader = ctx.header("Authorization");
            String creatorRole = null;

            // Check if request is authenticated
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                creatorRole = JwtUtil.getRoleFromToken(token);

                // If authenticated, must be admin or teller
                if (!"admin".equalsIgnoreCase(creatorRole) && !"teller".equalsIgnoreCase(creatorRole)) {
                    ctx.status(HttpStatus.FORBIDDEN)
                            .json(new ErrorResponse("FORBIDDEN", "Only admins and tellers can create users"));
                    return;
                }
            }

            CreateUserRequest request = ctx.bodyAsClass(CreateUserRequest.class);

            // Validate request
            if (request.getUsername() == null || request.getUsername().isBlank() ||
                    request.getPassword() == null || request.getPassword().isBlank() ||
                    request.getFirstName() == null || request.getFirstName().isBlank() ||
                    request.getLastName() == null || request.getLastName().isBlank() ||
                    request.getRole() == null || request.getRole().isBlank()) {
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(new ErrorResponse("BAD_REQUEST", "All fields are required"));
                return;
            }

            // Check if user already exists
            if (!userRepository.search(request.getUsername()).isEmpty()) {
                ctx.status(HttpStatus.CONFLICT)
                        .json(new ErrorResponse("CONFLICT", "Username already exists"));
                return;
            }

            // Role validation - only enforce if authenticated
            String newRole = request.getRole().toLowerCase();
            if (creatorRole != null) {
                // Tellers can only create customers
                if ("teller".equalsIgnoreCase(creatorRole) && !"customer".equals(newRole)) {
                    ctx.status(HttpStatus.FORBIDDEN)
                            .json(new ErrorResponse("FORBIDDEN", "Tellers can only create customers"));
                    return;
                }

                // Only admins can create admins or tellers
                if (("admin".equals(newRole) || "teller".equals(newRole)) &&
                        !"admin".equalsIgnoreCase(creatorRole)) {
                    ctx.status(HttpStatus.FORBIDDEN)
                            .json(new ErrorResponse("FORBIDDEN", "Only admins can create admins or tellers"));
                    return;
                }
            }

            User newUser;
            switch (newRole) {
                case "admin":
                    newUser = new Admin(request.getFirstName(), request.getLastName(), request.getUsername(),
                            request.getPassword());
                    break;
                case "teller":
                    newUser = new Teller(request.getFirstName(), request.getLastName(), request.getUsername(),
                            request.getPassword());
                    break;
                case "customer":
                    newUser = new Customer(request.getFirstName(), request.getLastName(), request.getUsername(),
                            request.getPassword());
                    break;
                default:
                    ctx.status(HttpStatus.BAD_REQUEST)
                            .json(new ErrorResponse("BAD_REQUEST", "Invalid role"));
                    return;
            }

            userRepository.addUsers(newUser);

            // If the new user is a customer, automatically create a checking account
            if ("customer".equals(newRole)) {
                long userId = getUserId(newUser.getUserName());
                String accountNumber = accountRepository.generateNewAccountNumber();
                accountRepository.createAccount(userId, "CHECK", accountNumber, 0.0);
            }

            // Return success response as JSON
            ctx.status(HttpStatus.CREATED).json(Map.of(
                    "message", "User created successfully",
                    "username", newUser.getUserName(),
                    "role", newRole));

        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                    .json(new ErrorResponse("ERROR", e.getMessage()));
        }
    }

    private static void updateAccountBalance(long accountId, double amount, String type) {
        String sql = "UPDATE accounts SET balance = balance + ? WHERE id = ?";
        try (Connection connection = dbManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            double delta = type.equals("credit") ? amount : -amount;
            statement.setDouble(1, delta);
            statement.setLong(2, accountId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to update balance", e);
        }
    }

    private static String getUsernameById(long userId) {
        String sql = "SELECT username FROM users WHERE id = ?";
        try (Connection connection = dbManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("username");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to get username", e);
        }
        throw new IllegalStateException("User not found");
    }

    private static String getCurrentTimestamp(long userId) {
        String sql = "SELECT created_at FROM users WHERE id = ?";
        try (Connection connection = dbManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("created_at");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to get timestamp", e);
        }
        return "";
    }

    private static List<AccountDTO> getAccountsByUsername(String username) {
        List<AccountDTO> accounts = new ArrayList<>();
        String sql = "SELECT a.id, a.customer_id, a.account_type, a.account_number, a.balance, a.created_at, " +
                "u.first_name, u.last_name FROM accounts a " +
                "JOIN users u ON u.id = a.customer_id " +
                "WHERE u.username = ?";
        try (Connection connection = dbManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    accounts.add(new AccountDTO(
                            resultSet.getLong("id"),
                            resultSet.getLong("customer_id"),
                            resultSet.getString("account_type").toLowerCase(),
                            resultSet.getString("account_number"),
                            resultSet.getDouble("balance"),
                            resultSet.getString("created_at"),
                            resultSet.getString("first_name") + " " + resultSet.getString("last_name")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to get accounts", e);
        }
        return accounts;
    }

    private static AccountDTO fetchAccountById(long accountId) {
        String sql = "SELECT a.id, a.customer_id, a.account_type, a.account_number, a.balance, a.created_at, " +
                "u.first_name, u.last_name FROM accounts a " +
                "JOIN users u ON u.id = a.customer_id " +
                "WHERE a.id = ?";
        try (Connection connection = dbManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new AccountDTO(
                            resultSet.getLong("id"),
                            resultSet.getLong("customer_id"),
                            resultSet.getString("account_type").toLowerCase(),
                            resultSet.getString("account_number"),
                            resultSet.getDouble("balance"),
                            resultSet.getString("created_at"),
                            resultSet.getString("first_name") + " " + resultSet.getString("last_name"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to get account", e);
        }
        throw new IllegalStateException("Account not found");
    }

    private static List<AccountDTO> getAllAccountsFromDB() {
        List<AccountDTO> accounts = new ArrayList<>();
        String sql = "SELECT a.id, a.customer_id, a.account_type, a.account_number, a.balance, a.created_at, " +
                "u.first_name, u.last_name FROM accounts a " +
                "JOIN users u ON u.id = a.customer_id";
        try (Connection connection = dbManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                accounts.add(new AccountDTO(
                        resultSet.getLong("id"),
                        resultSet.getLong("customer_id"),
                        resultSet.getString("account_type").toLowerCase(),
                        resultSet.getString("account_number"),
                        resultSet.getDouble("balance"),
                        resultSet.getString("created_at"),
                        resultSet.getString("first_name") + " " + resultSet.getString("last_name")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to get all accounts", e);
        }
        return accounts;
    }

    private static long getUserId(String username) {
        String sql = "SELECT id FROM users WHERE username = ?";
        try (Connection connection = dbManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("id");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to get user ID", e);
        }
        throw new IllegalStateException("User not found");
    }

    private static String getRoleForUser(String username) {
        String sql = "SELECT role FROM users WHERE username = ?";
        try (Connection connection = dbManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("role");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to get user role", e);
        }
        throw new IllegalStateException("User not found");
    }

    private static User getUserByUsername(String username) {
        try {
            List<User> results = userRepository.search(username);
            if (!results.isEmpty()) {
                return results.get(0);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to get user", e);
        }
        throw new IllegalStateException("User not found");
    }

    private static class SearchResponse {
        public List<AccountDTO> accounts;
        public int totalAccounts;
        public int currentPage;
        public int totalPages;
    }

    private static class AccountRequestDTO {
        public long id;
        public long userId;
        public String username;
        public String accountType;
        public String status;
        public String requestedAt;
        public String resolvedAt;

        public AccountRequestDTO(long id, long userId, String username, String accountType, String status,
                String requestedAt, String resolvedAt) {
            this.id = id;
            this.userId = userId;
            this.username = username;
            this.accountType = accountType;
            this.status = status;
            this.requestedAt = requestedAt;
            this.resolvedAt = resolvedAt;
        }
    }

    private static void requestAccount(Context ctx) {
        try {
            String authHeader = ctx.header("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                ctx.status(HttpStatus.UNAUTHORIZED).json(new ErrorResponse("UNAUTHORIZED", "Missing token"));
                return;
            }
            String token = authHeader.substring(7);
            String username = JwtUtil.getUsernameFromToken(token);
            long userId = getUserId(username);

            Map<String, String> body = ctx.bodyAsClass(Map.class);
            String accountType = body.get("accountType");

            if (accountType == null || accountType.isBlank()) {
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(new ErrorResponse("BAD_REQUEST", "Account type is required"));
                return;
            }

            String sql = "INSERT INTO account_requests (user_id, account_type, status, requested_at) " +
                    "VALUES (?, ?, 'pending', datetime('now'))";

            try (Connection connection = dbManager.getConnection();
                    PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, userId);
                statement.setString(2, accountType);
                statement.executeUpdate();

                ctx.status(HttpStatus.CREATED)
                        .json(Map.of("message", "Account request created successfully"));
            }
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(new ErrorResponse("ERROR", e.getMessage()));
        }
    }

    private static void getAccountRequests(Context ctx) {
        try {
            String authHeader = ctx.header("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                ctx.status(HttpStatus.UNAUTHORIZED).json(new ErrorResponse("UNAUTHORIZED", "Missing token"));
                return;
            }
            String token = authHeader.substring(7);
            String role = JwtUtil.getRoleFromToken(token);

            if (!"admin".equalsIgnoreCase(role) && !"teller".equalsIgnoreCase(role)) {
                ctx.status(HttpStatus.FORBIDDEN)
                        .json(new ErrorResponse("FORBIDDEN", "Only admins and tellers can view account requests"));
                return;
            }

            List<AccountRequestDTO> requests = new ArrayList<>();
            String sql = "SELECT ar.id, ar.user_id, u.username, ar.account_type, ar.status, ar.requested_at, ar.resolved_at "
                    +
                    "FROM account_requests ar " +
                    "JOIN users u ON u.id = ar.user_id " +
                    "WHERE ar.status = 'pending' " +
                    "ORDER BY ar.requested_at DESC";

            try (Connection connection = dbManager.getConnection();
                    PreparedStatement statement = connection.prepareStatement(sql);
                    ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    requests.add(new AccountRequestDTO(
                            resultSet.getLong("id"),
                            resultSet.getLong("user_id"),
                            resultSet.getString("username"),
                            resultSet.getString("account_type"),
                            resultSet.getString("status"),
                            resultSet.getString("requested_at"),
                            resultSet.getString("resolved_at")));
                }
            }
            ctx.json(requests);
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(new ErrorResponse("ERROR", e.getMessage()));
        }
    }

    private static void approveAccountRequest(Context ctx) {
        try {
            String authHeader = ctx.header("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                ctx.status(HttpStatus.UNAUTHORIZED).json(new ErrorResponse("UNAUTHORIZED", "Missing token"));
                return;
            }
            String token = authHeader.substring(7);
            String role = JwtUtil.getRoleFromToken(token);

            if (!"admin".equalsIgnoreCase(role) && !"teller".equalsIgnoreCase(role)) {
                ctx.status(HttpStatus.FORBIDDEN)
                        .json(new ErrorResponse("FORBIDDEN", "Only admins and tellers can approve account requests"));
                return;
            }

            long requestId = Long.parseLong(ctx.pathParam("requestId"));

            // Get request details
            String getSql = "SELECT user_id, account_type FROM account_requests WHERE id = ?";
            long userId = -1;
            String accountType = null;

            try (Connection connection = dbManager.getConnection();
                    PreparedStatement statement = connection.prepareStatement(getSql)) {
                statement.setLong(1, requestId);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        userId = rs.getLong("user_id");
                        accountType = rs.getString("account_type");
                    } else {
                        ctx.status(HttpStatus.NOT_FOUND).json(new ErrorResponse("NOT_FOUND", "Request not found"));
                        return;
                    }
                }
            }

            // Create account
            String accountNumber = accountRepository.generateNewAccountNumber();

            // Map frontend/request type to Bank package type
            String bankAccountType;
            switch (accountType.toLowerCase()) {
                case "checking":
                    bankAccountType = "CHECK";
                    break;
                case "savings":
                    bankAccountType = "SAVING";
                    break;
                case "credit":
                    bankAccountType = "CARD";
                    break;
                default:
                    bankAccountType = accountType; // Fallback
            }

            // Check if account already exists (Only for Checking accounts)
            if ("CHECK".equals(bankAccountType)) {
                String checkSql = "SELECT 1 FROM accounts WHERE customer_id = ? AND account_type = ?";
                try (Connection connection = dbManager.getConnection();
                        PreparedStatement statement = connection.prepareStatement(checkSql)) {
                    statement.setLong(1, userId);
                    statement.setString(2, bankAccountType);
                    try (ResultSet rs = statement.executeQuery()) {
                        if (rs.next()) {
                            ctx.status(HttpStatus.CONFLICT).json(new ErrorResponse("CONFLICT",
                                    "User already has an account of type " + bankAccountType));
                            return;
                        }
                    }
                }
            }

            accountRepository.createAccount(userId, bankAccountType, accountNumber, 0.0);

            // Update request status
            String updateSql = "UPDATE account_requests SET status = 'approved', resolved_at = datetime('now') WHERE id = ?";
            try (Connection connection = dbManager.getConnection();
                    PreparedStatement statement = connection.prepareStatement(updateSql)) {
                statement.setLong(1, requestId);
                statement.executeUpdate();
            }

            ctx.status(HttpStatus.OK).json(Map.of("message", "Account request approved and account created"));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(new ErrorResponse("ERROR", e.getMessage()));
        }
    }

    private static void rejectAccountRequest(Context ctx) {
        try {
            String authHeader = ctx.header("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                ctx.status(HttpStatus.UNAUTHORIZED).json(new ErrorResponse("UNAUTHORIZED", "Missing token"));
                return;
            }
            String token = authHeader.substring(7);
            String role = JwtUtil.getRoleFromToken(token);

            if (!"admin".equalsIgnoreCase(role) && !"teller".equalsIgnoreCase(role)) {
                ctx.status(HttpStatus.FORBIDDEN)
                        .json(new ErrorResponse("FORBIDDEN", "Only admins and tellers can reject account requests"));
                return;
            }

            long requestId = Long.parseLong(ctx.pathParam("requestId"));
            String sql = "UPDATE account_requests SET status = 'rejected', resolved_at = datetime('now') WHERE id = ?";

            try (Connection connection = dbManager.getConnection();
                    PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, requestId);
                int rows = statement.executeUpdate();
                if (rows > 0) {
                    ctx.status(HttpStatus.OK).json(Map.of("message", "Account request rejected"));
                } else {
                    ctx.status(HttpStatus.NOT_FOUND).json(new ErrorResponse("NOT_FOUND", "Request not found"));
                }
            }
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(new ErrorResponse("ERROR", e.getMessage()));
        }
    }

    private static class UpdateRoleRequest {
        private String role;

        public String getRole() {
            return role;
        }
    }

    private static class AccountDeletionRequestDTO {
        public long id;
        public long userId;
        public String username;
        public long accountId;
        public String accountNumber;
        public String accountType;
        public String status;
        public String reason;
        public String requestedAt;
        public String resolvedAt;

        public AccountDeletionRequestDTO(long id, long userId, String username, long accountId, String accountNumber,
                String accountType, String status, String reason, String requestedAt, String resolvedAt) {
            this.id = id;
            this.userId = userId;
            this.username = username;
            this.accountId = accountId;
            this.accountNumber = accountNumber;
            this.accountType = accountType;
            this.status = status;
            this.reason = reason;
            this.requestedAt = requestedAt;
            this.resolvedAt = resolvedAt;
        }
    }

    private static void requestAccountDeletion(Context ctx) {
        try {
            String authHeader = ctx.header("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                ctx.status(HttpStatus.UNAUTHORIZED).json(new ErrorResponse("UNAUTHORIZED", "Missing token"));
                return;
            }
            String token = authHeader.substring(7);
            String username = JwtUtil.getUsernameFromToken(token);
            long userId = getUserId(username);
            long accountId = Long.parseLong(ctx.pathParam("accountId"));

            // Verify account belongs to user
            AccountDTO account = fetchAccountById(accountId);
            if (account.getCustomerId() != userId) {
                ctx.status(HttpStatus.FORBIDDEN)
                        .json(new ErrorResponse("FORBIDDEN", "Account does not belong to user"));
                return;
            }

            // Prevent deletion of default checking account
            if ("CHECK".equalsIgnoreCase(account.getAccountType())
                    || "Checking".equalsIgnoreCase(account.getAccountType())) {
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(new ErrorResponse("BAD_REQUEST", "Cannot delete default checking account"));
                return;
            }

            Map<String, String> body = ctx.bodyAsClass(Map.class);
            String reason = body.get("reason");

            String sql = "INSERT INTO account_deletion_requests (user_id, account_id, status, reason, requested_at) " +
                    "VALUES (?, ?, 'pending', ?, datetime('now'))";

            try (Connection connection = dbManager.getConnection();
                    PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, userId);
                statement.setLong(2, accountId);
                statement.setString(3, reason);
                statement.executeUpdate();

                ctx.status(HttpStatus.CREATED)
                        .json(Map.of("message", "Account deletion request created successfully"));
            }
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(new ErrorResponse("ERROR", e.getMessage()));
        }
    }

    private static void getAccountDeletionRequests(Context ctx) {
        try {
            // Only admins can view deletion requests
            String authHeader = ctx.header("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                ctx.status(HttpStatus.UNAUTHORIZED).json(new ErrorResponse("UNAUTHORIZED", "Missing token"));
                return;
            }
            String token = authHeader.substring(7);
            String role = JwtUtil.getRoleFromToken(token);
            if (!"admin".equalsIgnoreCase(role)) {
                ctx.status(HttpStatus.FORBIDDEN).json(new ErrorResponse("FORBIDDEN", "Only admins can view requests"));
                return;
            }

            List<AccountDeletionRequestDTO> requests = new ArrayList<>();
            String sql = "SELECT adr.id, adr.user_id, u.username, adr.account_id, a.account_number, a.account_type, " +
                    "adr.status, adr.reason, adr.requested_at, adr.resolved_at " +
                    "FROM account_deletion_requests adr " +
                    "JOIN users u ON u.id = adr.user_id " +
                    "JOIN accounts a ON a.id = adr.account_id " +
                    "WHERE adr.status = 'pending' " +
                    "ORDER BY adr.requested_at DESC";

            try (Connection connection = dbManager.getConnection();
                    PreparedStatement statement = connection.prepareStatement(sql);
                    ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    requests.add(new AccountDeletionRequestDTO(
                            rs.getLong("id"),
                            rs.getLong("user_id"),
                            rs.getString("username"),
                            rs.getLong("account_id"),
                            rs.getString("account_number"),
                            rs.getString("account_type"),
                            rs.getString("status"),
                            rs.getString("reason"),
                            rs.getString("requested_at"),
                            rs.getString("resolved_at")));
                }
            }
            ctx.json(requests);
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(new ErrorResponse("ERROR", e.getMessage()));
        }
    }

    private static void approveAccountDeletion(Context ctx) {
        try {
            // Only admins can approve
            String authHeader = ctx.header("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                ctx.status(HttpStatus.UNAUTHORIZED).json(new ErrorResponse("UNAUTHORIZED", "Missing token"));
                return;
            }
            String token = authHeader.substring(7);
            String role = JwtUtil.getRoleFromToken(token);
            if (!"admin".equalsIgnoreCase(role)) {
                ctx.status(HttpStatus.FORBIDDEN)
                        .json(new ErrorResponse("FORBIDDEN", "Only admins can approve requests"));
                return;
            }

            long requestId = Long.parseLong(ctx.pathParam("requestId"));

            // Get request details
            String getSql = "SELECT account_id FROM account_deletion_requests WHERE id = ?";
            long accountId = -1;

            try (Connection connection = dbManager.getConnection();
                    PreparedStatement statement = connection.prepareStatement(getSql)) {
                statement.setLong(1, requestId);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        accountId = rs.getLong("account_id");
                    } else {
                        ctx.status(HttpStatus.NOT_FOUND).json(new ErrorResponse("NOT_FOUND", "Request not found"));
                        return;
                    }
                }
            }

            // Delete account
            // We need the account number to use accountRepository.deleteAccount(number,
            // role)
            AccountDTO account = fetchAccountById(accountId);
            accountRepository.deleteAccount(account.getAccountNumber(), "ADMIN");

            // Update request status
            String updateSql = "UPDATE account_deletion_requests SET status = 'approved', resolved_at = datetime('now') WHERE id = ?";
            try (Connection connection = dbManager.getConnection();
                    PreparedStatement statement = connection.prepareStatement(updateSql)) {
                statement.setLong(1, requestId);
                statement.executeUpdate();
            }

            ctx.status(HttpStatus.OK).json(Map.of("message", "Account deletion approved and account deleted"));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(new ErrorResponse("ERROR", e.getMessage()));
        }
    }

    private static void rejectAccountDeletion(Context ctx) {
        try {
            // Only admins can reject
            String authHeader = ctx.header("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                ctx.status(HttpStatus.UNAUTHORIZED).json(new ErrorResponse("UNAUTHORIZED", "Missing token"));
                return;
            }
            String token = authHeader.substring(7);
            String role = JwtUtil.getRoleFromToken(token);
            if (!"admin".equalsIgnoreCase(role)) {
                ctx.status(HttpStatus.FORBIDDEN)
                        .json(new ErrorResponse("FORBIDDEN", "Only admins can reject requests"));
                return;
            }

            long requestId = Long.parseLong(ctx.pathParam("requestId"));
            String sql = "UPDATE account_deletion_requests SET status = 'rejected', resolved_at = datetime('now') WHERE id = ?";

            try (Connection connection = dbManager.getConnection();
                    PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, requestId);
                int rows = statement.executeUpdate();
                if (rows > 0) {
                    ctx.status(HttpStatus.OK).json(Map.of("message", "Account deletion request rejected"));
                } else {
                    ctx.status(HttpStatus.NOT_FOUND).json(new ErrorResponse("NOT_FOUND", "Request not found"));
                }
            }
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(new ErrorResponse("ERROR", e.getMessage()));
        }
    }
}
