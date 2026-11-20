package boardinghouse.dao;

import boardinghouse.model.Account;
import boardinghouse.util.DatabaseConnection;
import java.sql.*;

public class AccountDAO {
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

    public boolean updatePassword(int accountId, String passwordHash) {
        try (PreparedStatement ps = conn().prepareStatement("UPDATE accounts SET password_hash = ? WHERE account_id = ?")) {
            ps.setString(1, passwordHash);
            ps.setInt(2, accountId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}