package boardinghouse.model;

public class Account {
    private final int accountId;
    private final String email;
    private final String phone;
    private final String passwordHash;
    private final Role role;

    // Overload 1: role as saved in the database
    public Account(int accountId, String email, String phone, String passwordHash, String role) {
        this(accountId, email, phone, passwordHash, Role.from(role));
    }

    // Overload 2: role as an enum
    public Account(int accountId, String email, String phone, String passwordHash, Role role) {
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

    // the role as text ("OWNER" or "TENANT"), used by the screens
    public String getRole() { return role.name(); }
    public Role getRoleType() { return role; }
    public boolean isOwner() { return role == Role.OWNER; }
}