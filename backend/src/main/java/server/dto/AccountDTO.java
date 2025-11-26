package server.dto;

public class AccountDTO {
    private long id;
    private long customerId;
    private String accountType;
    private String accountNumber;
    private double balance;
    private String createdAt;
    private String customerName;

    public AccountDTO(long id, long customerId, String accountType, String accountNumber,
                     double balance, String createdAt, String customerName) {
        this.id = id;
        this.customerId = customerId;
        this.accountType = accountType;
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.createdAt = createdAt;
        this.customerName = customerName;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(long customerId) {
        this.customerId = customerId;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
}
