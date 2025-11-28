package server.dto;

public class UserDTO {
    private long id;
    private String username;
    private String firstName;
    private String lastName;
    private String role;
    private boolean isActive;
    private boolean forcePasswordChange;
    private String createdAt;

    public UserDTO(long id, String username, String firstName, String lastName, String role, String createdAt,
            boolean forcePasswordChange) {
        this.id = id;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.isActive = true;
        this.createdAt = createdAt;
        this.forcePasswordChange = forcePasswordChange;
    }

    public UserDTO(long id, String username, String firstName, String lastName, String role, String createdAt) {
        this(id, username, firstName, lastName, role, createdAt, false);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isForcePasswordChange() {
        return forcePasswordChange;
    }

    public void setForcePasswordChange(boolean forcePasswordChange) {
        this.forcePasswordChange = forcePasswordChange;
    }
}
