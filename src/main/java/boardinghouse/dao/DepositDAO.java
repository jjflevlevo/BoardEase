package boardinghouse.dao;

import boardinghouse.model.Deposit;
import boardinghouse.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DepositDAO {
    private static final String SELECT = "SELECT d.*, t.name AS tenant_name, r.room_no FROM deposits d "
            + "JOIN tenants t ON d.tenant_id = t.tenant_id LEFT JOIN rooms r ON t.room_id = r.room_id ";

    private Connection conn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public Deposit findByTenant(int tenantId) {
        List<Deposit> list = query(SELECT + "WHERE d.tenant_id = ?", tenantId);
        return list.isEmpty() ? null : list.get(0);
    }

    // deposits still held first
    public List<Deposit> findAll() {
        return query(SELECT + "ORDER BY d.status, t.name", null);
    }

    // records a deposit; a tenant has at most one, so saving again replaces it
    public void save(int tenantId, double amount, String receivedOn, String note) {
        String sql = "INSERT INTO deposits (tenant_id, amount, received_on, note) VALUES (?, ?, ?, ?) "
                + "ON CONFLICT(tenant_id) DO UPDATE SET amount = excluded.amount, received_on = excluded.received_on, "
                + "note = excluded.note, status = 'HELD', deducted = 0, refunded = 0, settled_on = NULL";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            ps.setDouble(2, amount);
            ps.setString(3, receivedOn);
            ps.setString(4, note);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // at move-out: part of the deposit pays unpaid bills, the rest goes back to the tenant
    public void settle(int tenantId, double deducted, double refunded, String settledOn) {
        String sql = "UPDATE deposits SET status = 'RETURNED', deducted = ?, refunded = ?, settled_on = ? WHERE tenant_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setDouble(1, deducted);
            ps.setDouble(2, refunded);
            ps.setString(3, settledOn);
            ps.setInt(4, tenantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public double totalHeld() {
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(SUM(amount), 0) FROM deposits WHERE status = 'HELD'")) {
            return rs.next() ? rs.getDouble(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Deposit> query(String sql, Integer param) {
        List<Deposit> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            if (param != null) ps.setInt(1, param);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Deposit d = new Deposit(rs.getInt("deposit_id"), rs.getInt("tenant_id"), rs.getDouble("amount"),
                        rs.getString("received_on"), rs.getString("status"), rs.getDouble("deducted"),
                        rs.getDouble("refunded"), rs.getString("settled_on"), rs.getString("note"));
                d.setDisplayInfo(rs.getString("tenant_name"), rs.getString("room_no"));
                list.add(d);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }
}