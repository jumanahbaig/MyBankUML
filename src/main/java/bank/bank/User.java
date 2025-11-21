package bank;

public abstract class User {
    private String firstName;
    private String lastName;
    private String userName;
    private String password;

    protected User(String firstName, String lastName, String userName, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.userName = userName;
        this.password = password;
    }

    public void printUserInfo() {
        System.out.println("==== " + getClass().getSimpleName() + " Profile ====");
        System.out.println("Name: " + firstName + " " + lastName);
        System.out.println("Username: " + userName);
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

    public void changePassword(String newPassword) {
        // TODO: enforce password policies, verify current credentials, and persist the change.
    }

    public void requestForgottenPassword() {
        // TODO: trigger forgotten password workflow (identity verification + temporary code).
    }
}
