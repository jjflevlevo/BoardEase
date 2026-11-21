package boardinghouse.service;

import boardinghouse.dao.AccountDAO;
import boardinghouse.dao.ActivityDAO;
import boardinghouse.dao.TenantDAO;
import boardinghouse.model.Account;
import boardinghouse.model.Role;
import boardinghouse.model.Tenant;
import boardinghouse.util.PasswordUtil;

public class AuthService {
    public static final int MAX_ATTEMPTS = 5;
    public static final long LOCK_MILLIS = 60_000; // 1 minute

    // either the signed-in account, or the reason sign-in failed
    public record LoginResult(Account account, String error) {
        public boolean success() { return account != null; }
    }

    private final AccountDAO accountDAO = new AccountDAO();
    private final TenantDAO tenantDAO = new TenantDAO();
    private final ActivityDAO activity = new ActivityDAO();

    public LoginResult attemptLogin(String email, String password) {
        String wrong = "Incorrect email or password. Please try again.";
        Account acc = accountDAO.findByEmail(email);
        if (acc == null) return new LoginResult(null, wrong);

        long now = System.currentTimeMillis();
        AccountDAO.LockState lock = accountDAO.lockState(acc.getAccountId());
        if (lock.lockedUntil() > now) {
            long seconds = (lock.lockedUntil() - now + 999) / 1000;
            return new LoginResult(null, "Too many failed attempts. Try again in " + seconds + " second(s).");
        }

        if (PasswordUtil.verify(password, acc.getPasswordHash())) {
            if (lock.failedAttempts() > 0 || lock.lockedUntil() > 0) accountDAO.saveLockState(acc.getAccountId(), 0, null);

            // tenants who moved out can no longer sign in
            if (acc.getRoleType() == Role.TENANT) {
                Tenant t = tenantDAO.findByAccountId(acc.getAccountId());
                if (t != null && t.isMovedOut()) {
                    return new LoginResult(null, "This account was closed after moving out. Please contact the owner.");
                }
            }
            return new LoginResult(acc, null);
        }

        int attempts = lock.failedAttempts() + 1;
        if (attempts >= MAX_ATTEMPTS) {
            accountDAO.saveLockState(acc.getAccountId(), 0, now + LOCK_MILLIS);
            activity.log("REJECTED", "Sign-in locked for 1 minute after " + MAX_ATTEMPTS
                    + " wrong passwords: " + acc.getEmail());
            return new LoginResult(null, "Too many failed attempts. Sign-in is locked for 1 minute.");
        }
        accountDAO.saveLockState(acc.getAccountId(), attempts, null);
        return new LoginResult(null, wrong);
    }

    public Account login(String email, String password) {
        return attemptLogin(email, password).account();
    }

    // Public sign up only creates TENANT accounts
    public boolean signUp(String email, String phone, String password, Tenant tenant) {
        if (accountDAO.findByEmail(email) != null) return false;
        if (!accountDAO.insert(email, phone, PasswordUtil.hash(password), Role.TENANT.name())) return false;
        tenant.setAccountId(accountDAO.findByEmail(email).getAccountId());
        boolean saved = tenantDAO.insert(tenant);
        if (saved) activity.log("TENANT", "New tenant registered: " + tenant.getName());
        return saved;
    }
}