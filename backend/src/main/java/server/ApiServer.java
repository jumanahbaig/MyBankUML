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

public class ApiServer {
    private static final Gson gson = new Gson();
    private static final DatabaseManager dbManager = new DatabaseManager();
    private static final UserRepository userRepository = new UserRepository(dbManager);
    private static final AccountRepository accountRepository = new AccountRepository(dbManager);
    private static final TransactionRepository transactionRepository = new TransactionRepository(dbManager);
    private static final CustomerService customerService = new CustomerService(userRepository, accountRepository);

    public static void main(String[] args) {
        System.out.println("Initializing database...");
        dbManager.initialize();

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
        app.post("/api/auth/password-reset/request", ApiServer::requestPasswordReset);

        // Customer endpoints
        app.post("/api/customers", ApiServer::createCustomer);
        app.get("/api/customers/{customerId}/accounts", ApiServer::getCustomerAccounts);

        // Account endpoints
        app.get("/api/accounts/{accountId}", ApiServer::getAccountById);
        app.get("/api/accounts/search", ApiServer::searchAccounts);
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

        System.out.println("API Server started on port 8080");
    }

    private static void login(Context ctx) {
        try {
            LoginRequest request = ctx.bodyAsClass(LoginRequest.class);
            System.out.println("Login attempt for username: " + request.getUsername());

            boolean loginSuccess = customerService.loginUser(request.getUsername(), request.getPassword(), null);
            System.out.println("Login success: " + loginSuccess);

            if (loginSuccess) {
                long userId = getUserId(request.getUsername());
                System.out.println("User ID: " + userId);

                String role = getRoleForUser(request.getUsername());
                System.out.println("User role: " + role);

                User user = getUserByUsername(request.getUsername());
                System.out.println("User object retrieved: " + user.getUserName());

                String token = JwtUtil.generateToken(user.getUserName(), role.toUpperCase(), userId);
                UserDTO userDTO = new UserDTO(userId, user.getUserName(), user.getFirstName(),
                        user.getLastName(), role.toLowerCase(),
                        getCurrentTimestamp(userId));
                ctx.json(new LoginResponse(token, userDTO));
            } else {
                ctx.status(HttpStatus.UNAUTHORIZED)
                        .json(new ErrorResponse("UNAUTHORIZED", "Invalid credentials"));
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

    private static void requestPasswordReset(Context ctx) {
        ctx.status(HttpStatus.OK).result("Password reset request submitted");
    }

    private static void createCustomer(Context ctx) {
        try {
            LoginRequest request = ctx.bodyAsClass(LoginRequest.class);
            Customer customer = customerService.createCustomer(
                    request.getUsername(),
                    request.getUsername(),
                    request.getUsername(),
                    request.getPassword()
            );
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
            int totalPages = (int) Math.ceil((double) total / limit);
            int start = (page - 1) * limit;
            int end = Math.min(start + limit, total);

            List<AccountDTO> paged = filtered.subList(start, end);

            SearchResponse response = new SearchResponse();
            response.accounts = paged;
            response.totalAccounts = total;
            response.currentPage = page;
            response.totalPages = totalPages;
            ctx.json(response);
        } catch (Exception e) {
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
                    request.getDescription()
            );

            transactionRepository.addTransaction(accountId, transaction);
            updateAccountBalance(accountId, request.getAmount(), transactionType);

            TransactionDTO dto = new TransactionDTO(
                    Long.parseLong(transaction.getId()),
                    transaction.getAmount(),
                    transaction.getType(),
                    transaction.getDescription(),
                    transaction.getTimestamp().toString()
            );

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
            List<User> results = userRepository.search(username, role);

            List<UserDTO> dtos = new ArrayList<>();
            for (User user : results) {
                long userId = getUserId(user.getUserName());
                dtos.add(new UserDTO(userId, user.getUserName(), user.getFirstName(),
                        user.getLastName(), user.getClass().getSimpleName().toLowerCase(),
                        getCurrentTimestamp(userId)));
            }
            ctx.json(dtos);
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(new ErrorResponse("ERROR", e.getMessage()));
        }
    }

    private static void updateUserRole(Context ctx) {
        try {
            ctx.status(HttpStatus.OK).result("Role updated");
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

    // Helper classes
    private static class SearchResponse {
        public List<AccountDTO> accounts;
        public int totalAccounts;
        public int currentPage;
        public int totalPages;
    }

    // Helper methods
    private static User getUserByUsername(String username) {
        List<User> users = userRepository.search(username);
        if (!users.isEmpty()) {
            return users.get(0);
        }
        throw new IllegalStateException("User not found");
    }

    private static String getRoleForUser(String username) {
        return userRepository.findRoleByUsername(username);
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
                            resultSet.getString("first_name") + " " + resultSet.getString("last_name")
                    ));
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
                            resultSet.getString("first_name") + " " + resultSet.getString("last_name")
                    );
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
                        resultSet.getString("first_name") + " " + resultSet.getString("last_name")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to get all accounts", e);
        }
        return accounts;
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
                            resultSet.getLong("id"),
                            resultSet.getDouble("amount"),
                            resultSet.getString("type"),
                            resultSet.getString("description"),
                            resultSet.getString("created_at")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to get transactions", e);
        }
        return transactions;
    }

    private static List<UserDTO> fetchAllUsers() {
        List<UserDTO> users = new ArrayList<>();
        String sql = "SELECT id, username, first_name, last_name, role, created_at FROM users";
        try (Connection connection = dbManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                users.add(new UserDTO(
                        resultSet.getLong("id"),
                        resultSet.getString("username"),
                        resultSet.getString("first_name"),
                        resultSet.getString("last_name"),
                        resultSet.getString("role").toLowerCase(),
                        resultSet.getString("created_at")
                ));
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
}
