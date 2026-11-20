package boardinghouse.dao;

import boardinghouse.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// the "Recent Activity" feed on the owner dashboard
public class ActivityDAO {
    public record Entry(String type, String message, String createdAt) { }

    private Connection conn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    // types: PAID, PENDING, REJECTED, BILLS, TENANT
    public void log(String type, String message) {
        try (PreparedStatement ps = conn().prepareStatement("INSERT INTO activity_log (type, message) VALUES (?, ?)")) {
            ps.setString(1, type);
            ps.setString(2, message);
            ps.executeUpdate();
        } catch (SQLException e) {
            // the activity feed must never break a payment or a billing action
        }
    }

    public List<Entry> recent(int limit) {
        List<Entry> list = new ArrayList<>();
        String sql = "SELECT type, message, created_at FROM activity_log ORDER BY activity_id DESC LIMIT ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(new Entry(rs.getString(1), rs.getString(2), rs.getString(3)));
        } catch (SQLException e) {
            return list;
        }
        return list;
    }
}