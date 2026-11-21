package boardinghouse.dao;

import boardinghouse.model.Account;
import boardinghouse.util.DatabaseConnection;
import java.sql.*;

public class AccountDAO {
    // failed sign-in attempts and the time (in milliseconds) until which sign-in is blocked (0 = not locked)
    public record LockState(int failedAttempts, long lockedUntil) { }

    private Connection conn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public Account findByEmail(String email) {
        return find("SELECT * FROM accounts WHERE email = ?", email);
    }

    public Account findById(int accountId) {
        return find("SELECT * FROM accounts WHERE account_id = ?", accountId);
    }

    private Account find(String sql, Object param) {
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setObject(1, param);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Account(rs.getInt("account_id"), rs.getString("email"),
                        rs.getString("phone"), rs.getString("password_hash"), rs.getString("role"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public boolean insert(String email, String phone, String passwordHash, String role) {
        String sql = "INSERT INTO accounts (email, phone, password_hash, role) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, phone);
            ps.setString(3, passwordHash);
            ps.setString(4, role);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            return false; // for example, duplicate email
        }
    }

    // a new password also unlocks the account
    public boolean updatePassword(int accountId, String passwordHash) {
        String sql = "UPDATE accounts SET password_hash = ?, failed_attempts = 0, locked_until = NULL WHERE account_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setInt(2, accountId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // returns false when the email is already used by another account
    public boolean updateContact(int accountId, String email, String phone) {
        try (PreparedStatement ps = conn().prepareStatement("UPDATE accounts SET email = ?, phone = ? WHERE account_id = ?")) {
            ps.setString(1, email);
            ps.setString(2, phone);
            ps.setInt(3, accountId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            return false;
        }
    }

    // ---------- Sign-in lockout ----------
    public LockState lockState(int accountId) {
        String sql = "SELECT failed_attempts, locked_until FROM accounts WHERE account_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return new LockState(rs.getInt(1), rs.getLong(2));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return new LockState(0, 0);
    }

    public void saveLockState(int accountId, int failedAttempts, Long lockedUntil) {
        String sql = "UPDATE accounts SET failed_attempts = ?, locked_until = ? WHERE account_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, failedAttempts);
            if (lockedUntil == null) ps.setNull(2, Types.BIGINT); else ps.setLong(2, lockedUntil);
            ps.setInt(3, accountId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}