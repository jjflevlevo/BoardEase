package boardinghouse.model;

public class Account {
    private int accountId;
    private String email;
    private String phone;
    private String passwordHash;
    private String role;

    public Account(int accountId, String email, String phone, String passwordHash, String role) {
        this.accountId = accountId;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public int getAccountId() { return accountId; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
}