package boardinghouse.dao;

import boardinghouse.model.*;
import boardinghouse.util.DatabaseConnection;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class BillDAO {
    private static final String SELECT =
            "SELECT b.*, t.name AS tenant_name FROM bills b JOIN tenants t ON b.tenant_id = t.tenant_id ";

    private Connection conn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public boolean insert(Bill b) {
        String sql = "INSERT INTO bills (tenant_id, type, billing_month, amount, due_date) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, b.getTenantId());
            ps.setString(2, b.getType());
            ps.setString(3, b.getBillingMonth());
            ps.setDouble(4, b.getAmount());
            ps.setString(5, b.getDueDate().toString());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean exists(int tenantId, String type, String month) {
        String sql = "SELECT 1 FROM bills WHERE tenant_id = ? AND type = ? AND billing_month = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            ps.setString(2, type);
            ps.setString(3, month);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Bill findById(int billId) {
        List<Bill> list = query(SELECT + "WHERE b.bill_id = ?", billId);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Bill> findAll() {
        return query(SELECT + "ORDER BY b.billing_month DESC, t.name, b.type", null);
    }

    public List<Bill> findByTenant(int tenantId) {
        return query(SELECT + "WHERE b.tenant_id = ? ORDER BY b.billing_month DESC, b.type", tenantId);
    }

    public boolean updateStatus(int billId, String status) {
        try (PreparedStatement ps = conn().prepareStatement("UPDATE bills SET status = ? WHERE bill_id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, billId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Bill> query(String sql, Integer param) {
        List<Bill> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            if (param != null) ps.setInt(1, param);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    private Bill map(ResultSet rs) throws SQLException {
        int id = rs.getInt("bill_id");
        int tenantId = rs.getInt("tenant_id");
        String type = rs.getString("type");
        String month = rs.getString("billing_month");
        LocalDate due = LocalDate.parse(rs.getString("due_date"));
        String status = rs.getString("status");
        double amount = rs.getDouble("amount");

        Bill b;
        if ("RENT".equals(type)) b = new RentBill(id, tenantId, month, due, status, amount);
        else if ("WATER".equals(type)) b = new WaterBill(id, tenantId, month, due, status, amount);
        else b = new ElectricBill(id, tenantId, month, due, status, amount);
        b.setTenantName(rs.getString("tenant_name"));
        return b;
    }
}