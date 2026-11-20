package boardinghouse.dao;

import boardinghouse.model.Tenant;
import boardinghouse.util.DatabaseConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

public class TenantDAO {
    private Connection conn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public boolean insert(Tenant t) {
        String sql = "INSERT INTO tenants (account_id, name, address, age, occupation, emergency_no) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, t.getAccountId());
            ps.setString(2, t.getName());
            ps.setString(3, t.getAddress());
            ps.setInt(4, t.getAge());
            ps.setString(5, t.getOccupation());
            ps.setString(6, t.getEmergencyNo());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Tenant findByAccountId(int accountId) {
        String sql = "SELECT * FROM tenants WHERE account_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public List<Tenant> findAll() {
        List<Tenant> list = new ArrayList<>();
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM tenants ORDER BY name")) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public List<Tenant> findByRoom(int roomId) {
        List<Tenant> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement("SELECT * FROM tenants WHERE room_id = ? ORDER BY name")) {
            ps.setInt(1, roomId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public boolean assignRoom(int tenantId, Integer roomId) {
        String sql = "UPDATE tenants SET room_id = ? WHERE tenant_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            if (roomId == null) ps.setNull(1, Types.INTEGER); else ps.setInt(1, roomId);
            ps.setInt(2, tenantId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int countInRoom(int roomId) {
        String sql = "SELECT COUNT(*) FROM tenants WHERE room_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // Removes the tenant, their login account, bills, and payments (all or nothing)
    public boolean deleteCompletely(Tenant t) {
        List<String> proofs = new ArrayList<>();
        Connection c = null;
        try {
            c = conn();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT p.proof_path FROM payments p JOIN bills b ON p.bill_id = b.bill_id "
                    + "WHERE b.tenant_id = ? AND p.proof_path IS NOT NULL")) {
                ps.setInt(1, t.getTenantId());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) proofs.add(rs.getString(1));
            }

            c.setAutoCommit(false);
            try (PreparedStatement p1 = c.prepareStatement(
                         "DELETE FROM payments WHERE bill_id IN (SELECT bill_id FROM bills WHERE tenant_id = ?)");
                 PreparedStatement p2 = c.prepareStatement("DELETE FROM bills WHERE tenant_id = ?");
                 PreparedStatement p3 = c.prepareStatement("DELETE FROM tenants WHERE tenant_id = ?");
                 PreparedStatement p4 = c.prepareStatement("DELETE FROM accounts WHERE account_id = ?")) {
                p1.setInt(1, t.getTenantId());
                p1.executeUpdate();
                p2.setInt(1, t.getTenantId());
                p2.executeUpdate();
                p3.setInt(1, t.getTenantId());
                p3.executeUpdate();
                p4.setInt(1, t.getAccountId());
                p4.executeUpdate();
            }
            c.commit();
        } catch (SQLException e) {
            try { if (c != null) c.rollback(); } catch (SQLException ignored) { }
            return false;
        } finally {
            try { if (c != null) c.setAutoCommit(true); } catch (SQLException ignored) { }
        }

        for (String path : proofs) {
            try { Files.deleteIfExists(Path.of(path)); } catch (Exception ignored) { }
        }
        return true;
    }

    private Tenant map(ResultSet rs) throws SQLException {
        Tenant t = new Tenant(rs.getString("name"), rs.getString("address"), rs.getInt("age"),
                rs.getString("occupation"), rs.getString("emergency_no"));
        t.setTenantId(rs.getInt("tenant_id"));
        t.setAccountId(rs.getInt("account_id"));
        int room = rs.getInt("room_id");
        t.setRoomId(rs.wasNull() ? null : room);
        return t;
    }
}