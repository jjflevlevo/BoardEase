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

    public Tenant findById(int tenantId) {
        List<Tenant> list = query("SELECT * FROM tenants WHERE tenant_id = ?", tenantId);
        return list.isEmpty() ? null : list.get(0);
    }

    public Tenant findByAccountId(int accountId) {
        List<Tenant> list = query("SELECT * FROM tenants WHERE account_id = ?", accountId);
        return list.isEmpty() ? null : list.get(0);
    }

    // current tenants only (people who have moved out are not included)
    public List<Tenant> findAll() {
        return query("SELECT * FROM tenants WHERE status = 'ACTIVE' ORDER BY name", null);
    }

    public List<Tenant> findMovedOut() {
        return query("SELECT * FROM tenants WHERE status = 'MOVED_OUT' ORDER BY moved_out_on DESC, name", null);
    }

    // current and former tenants
    public List<Tenant> findEveryone() {
        return query("SELECT * FROM tenants ORDER BY status, name", null);
    }

    public List<Tenant> findByRoom(int roomId) {
        return query("SELECT * FROM tenants WHERE room_id = ? AND status = 'ACTIVE' ORDER BY name", roomId);
    }

    public boolean updateInfo(int tenantId, String name, String address, int age, String occupation, String emergencyNo) {
        String sql = "UPDATE tenants SET name = ?, address = ?, age = ?, occupation = ?, emergency_no = ? WHERE tenant_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, address);
            ps.setInt(3, age);
            ps.setString(4, occupation);
            ps.setString(5, emergencyNo);
            ps.setInt(6, tenantId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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
        String sql = "SELECT COUNT(*) FROM tenants WHERE room_id = ? AND status = 'ACTIVE'";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // ---------- Moving out ----------
    public boolean moveOut(int tenantId, String date) {
        String sql = "UPDATE tenants SET status = 'MOVED_OUT', moved_out_on = ?, room_id = NULL WHERE tenant_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, date);
            ps.setInt(2, tenantId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean restore(int tenantId) {
        String sql = "UPDATE tenants SET status = 'ACTIVE', moved_out_on = NULL WHERE tenant_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // Removes the tenant and everything connected to them (all or nothing)
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
            String[] steps = {
                    "DELETE FROM deposits WHERE tenant_id = ?",
                    "DELETE FROM maintenance_requests WHERE tenant_id = ?",
                    "DELETE FROM payments WHERE bill_id IN (SELECT bill_id FROM bills WHERE tenant_id = ?)",
                    "DELETE FROM bills WHERE tenant_id = ?",
                    "DELETE FROM tenants WHERE tenant_id = ?"};
            for (String sql : steps) {
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, t.getTenantId());
                    ps.executeUpdate();
                }
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM accounts WHERE account_id = ?")) {
                ps.setInt(1, t.getAccountId());
                ps.executeUpdate();
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

    private List<Tenant> query(String sql, Integer param) {
        List<Tenant> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            if (param != null) ps.setInt(1, param);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    private Tenant map(ResultSet rs) throws SQLException {
        Tenant t = new Tenant(rs.getString("name"), rs.getString("address"), rs.getInt("age"),
                rs.getString("occupation"), rs.getString("emergency_no"));
        t.setTenantId(rs.getInt("tenant_id"));
        t.setAccountId(rs.getInt("account_id"));
        int room = rs.getInt("room_id");
        t.setRoomId(rs.wasNull() ? null : room);
        t.setStatus(rs.getString("status"));
        t.setMovedOutOn(rs.getString("moved_out_on"));
        return t;
    }
}