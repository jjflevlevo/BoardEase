package boardinghouse.dao;

import boardinghouse.model.Payment;
import boardinghouse.util.DatabaseConnection;
import java.sql.*;
import java.util.*;

public class PaymentDAO {
    private static final String SELECT =
            "SELECT p.*, b.type AS bill_type, b.billing_month, t.name AS tenant_name "
            + "FROM payments p JOIN bills b ON p.bill_id = b.bill_id "
            + "JOIN tenants t ON b.tenant_id = t.tenant_id ";

    private Connection conn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public boolean insert(Payment p) {
        String sql = "INSERT INTO payments (bill_id, amount, method, ref_no, proof_path) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, p.getBillId());
            ps.setDouble(2, p.getAmount());
            ps.setString(3, p.getMethod());
            ps.setString(4, p.getRefNo());
            ps.setString(5, p.getProofPath());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Payment findById(int paymentId) {
        List<Payment> list = query(SELECT + "WHERE p.payment_id = ?", paymentId);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Payment> findByTenant(int tenantId) {
        return query(SELECT + "WHERE b.tenant_id = ? ORDER BY p.payment_id DESC", tenantId);
    }

    public List<Payment> findPending() {
        return query(SELECT + "WHERE p.status = 'PENDING' ORDER BY p.payment_id", null);
    }

    // status is PENDING, APPROVED, or REJECTED (newest first)
    public List<Payment> findByStatus(String status) {
        return query(SELECT + "WHERE p.status = ? ORDER BY p.payment_id DESC", status);
    }

    public boolean updateStatus(int paymentId, String status) {
        try (PreparedStatement ps = conn().prepareStatement("UPDATE payments SET status = ? WHERE payment_id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, paymentId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public double sumApproved(int billId) { return sum(billId, "APPROVED"); }

    public double sumPending(int billId) { return sum(billId, "PENDING"); }

    private double sum(int billId, String status) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE bill_id = ? AND status = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, billId);
            ps.setString(2, status);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Double> incomeByMonth() {
        Map<String, Double> map = new LinkedHashMap<>();
        String sql = "SELECT substr(paid_at, 1, 7) AS m, SUM(amount) AS total FROM payments "
                + "WHERE status = 'APPROVED' GROUP BY m ORDER BY m DESC";
        try (Statement st = conn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) map.put(rs.getString("m"), rs.getDouble("total"));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return map;
    }

    private List<Payment> query(String sql, Object param) {
        List<Payment> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            if (param != null) ps.setObject(1, param);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Payment p = new Payment(rs.getInt("payment_id"), rs.getInt("bill_id"), rs.getDouble("amount"),
                        rs.getString("method"), rs.getString("ref_no"), rs.getString("status"), rs.getString("paid_at"));
                p.setProofPath(rs.getString("proof_path"));
                p.setDisplayInfo(rs.getString("tenant_name"), rs.getString("bill_type"), rs.getString("billing_month"));
                list.add(p);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }
}