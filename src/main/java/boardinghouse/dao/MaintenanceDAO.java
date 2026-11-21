package boardinghouse.dao;

import boardinghouse.model.MaintenanceRequest;
import boardinghouse.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MaintenanceDAO {
    private static final String SELECT = "SELECT m.*, t.name AS tenant_name, r.room_no FROM maintenance_requests m "
            + "JOIN tenants t ON m.tenant_id = t.tenant_id LEFT JOIN rooms r ON m.room_id = r.room_id ";
    // open requests first, then in progress, then done; newest first inside each group
    private static final String ORDER = " ORDER BY CASE m.status WHEN 'OPEN' THEN 0 WHEN 'IN_PROGRESS' THEN 1 ELSE 2 END,"
            + " m.request_id DESC";

    private Connection conn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public boolean insert(int tenantId, Integer roomId, String category, String description) {
        String sql = "INSERT INTO maintenance_requests (tenant_id, room_id, category, description) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            if (roomId == null) ps.setNull(2, Types.INTEGER); else ps.setInt(2, roomId);
            ps.setString(3, category);
            ps.setString(4, description);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<MaintenanceRequest> findAll() {
        return query(SELECT + ORDER, null);
    }

    public List<MaintenanceRequest> findByTenant(int tenantId) {
        return query(SELECT + "WHERE m.tenant_id = ?" + ORDER, tenantId);
    }

    public MaintenanceRequest findById(int requestId) {
        List<MaintenanceRequest> list = query(SELECT + "WHERE m.request_id = ?", requestId);
        return list.isEmpty() ? null : list.get(0);
    }

    public boolean updateStatus(int requestId, String status, String ownerNote) {
        String sql = "UPDATE maintenance_requests SET status = ?, owner_note = ?, updated_at = CURRENT_TIMESTAMP "
                + "WHERE request_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, ownerNote);
            ps.setInt(3, requestId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // a tenant can only cancel their own request while it is still open
    public boolean deleteOpen(int requestId, int tenantId) {
        String sql = "DELETE FROM maintenance_requests WHERE request_id = ? AND tenant_id = ? AND status = 'OPEN'";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, requestId);
            ps.setInt(2, tenantId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int countByStatus(String status) {
        try (PreparedStatement ps = conn().prepareStatement("SELECT COUNT(*) FROM maintenance_requests WHERE status = ?")) {
            ps.setString(1, status);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<MaintenanceRequest> query(String sql, Integer param) {
        List<MaintenanceRequest> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            if (param != null) ps.setInt(1, param);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int room = rs.getInt("room_id");
                Integer roomId = rs.wasNull() ? null : room;
                MaintenanceRequest r = new MaintenanceRequest(rs.getInt("request_id"), rs.getInt("tenant_id"), roomId,
                        rs.getString("category"), rs.getString("description"), rs.getString("status"),
                        rs.getString("owner_note"), rs.getString("created_at"), rs.getString("updated_at"));
                r.setDisplayInfo(rs.getString("tenant_name"), rs.getString("room_no"));
                list.add(r);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }
}