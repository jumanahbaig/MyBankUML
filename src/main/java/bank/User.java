package bank;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.UUID;

public abstract class User {
    private String firstName;
    private String lastName;
    private String userName;
    private String password;
    private UserRepository userRepository;
    private Logs logs;

    protected User(String firstName, String lastName, String userName, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.userName = userName;
        this.password = password;
    }

    public void printUserInfo() {
        String roleLabel = "USER";
        System.out.println(
            "Username: " + getUserName()
                + " | Name: " + getFirstName() + " " + getLastName()
                + " | Role: " + roleLabel
        );
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void setLogs(Logs logs) {
        this.logs = logs;
    }

    public void changePassword(String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }

        String hashedPassword = hashPassword(newPassword);
        this.password = hashedPassword;

        if (userRepository != null) {
            userRepository.updatePassword(this);
        }

        if (logs != null) {
            logs.append(
                this.getUserName(),
                "PASSWORD_CHANGE",
                "USER",
                "Password changed successfully for user " + this.getUserName()
            );
        }
    }

    private String hashPassword(String newPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(newPassword.getBytes(StandardCharsets.UTF_8));
            try (Formatter formatter = new Formatter()) {
                for (byte b : hashedBytes) {
                    formatter.format("%02x", b);
                }
                return formatter.toString();
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to hash password", e);
        }
    }

    public void requestForgottenPassword() {
        String token = UUID.randomUUID().toString();

        if (logs != null) {
            logs.append(
                this.getUserName(),
                "PASSWORD_RESET_REQUEST",
                "ADMIN",
                "Password reset request received for user " + this.getUserName() + ". Token: " + token
            );
        }

        System.out.println("Admin notified: " + this.getUserName() + " requested a password reset. Token: " + token);

        this.password = token;  // Store temporary token as password for this demo.

        if (userRepository != null) {
            userRepository.updatePassword(this);
        }
    }
}
