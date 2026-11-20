package boardinghouse.service;

import boardinghouse.dao.AccountDAO;
import boardinghouse.dao.ActivityDAO;
import boardinghouse.dao.TenantDAO;
import boardinghouse.model.Account;
import boardinghouse.model.Tenant;
import boardinghouse.util.PasswordUtil;

public class AuthService {
    private final AccountDAO accountDAO = new AccountDAO();
    private final TenantDAO tenantDAO = new TenantDAO();
    private final ActivityDAO activity = new ActivityDAO();

    public Account login(String email, String password) {
        Account acc = accountDAO.findByEmail(email);
        if (acc != null && PasswordUtil.verify(password, acc.getPasswordHash())) return acc;
        return null;
    }

    // Public sign up only creates TENANT accounts
    public boolean signUp(String email, String phone, String password, Tenant tenant) {
        if (accountDAO.findByEmail(email) != null) return false;
        if (!accountDAO.insert(email, phone, PasswordUtil.hash(password), "TENANT")) return false;
        tenant.setAccountId(accountDAO.findByEmail(email).getAccountId());
        boolean saved = tenantDAO.insert(tenant);
        if (saved) activity.log("TENANT", "New tenant registered: " + tenant.getName());
        return saved;
    }
}