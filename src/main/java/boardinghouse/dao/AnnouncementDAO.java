package boardinghouse.dao;

import boardinghouse.model.Announcement;
import boardinghouse.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AnnouncementDAO {
    private Connection conn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public boolean insert(String title, String message, boolean important) {
        String sql = "INSERT INTO announcements (title, message, priority) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, message);
            ps.setString(3, important ? "IMPORTANT" : "NORMAL");
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean update(int id, String title, String message, boolean important) {
        String sql = "UPDATE announcements SET title = ?, message = ?, priority = ? WHERE announcement_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, message);
            ps.setString(3, important ? "IMPORTANT" : "NORMAL");
            ps.setInt(4, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean delete(int id) {
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM announcements WHERE announcement_id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // newest first
    public List<Announcement> findAll() {
        return query("SELECT * FROM announcements ORDER BY announcement_id DESC");
    }

    public Announcement latest() {
        List<Announcement> list = query("SELECT * FROM announcements ORDER BY announcement_id DESC LIMIT 1");
        return list.isEmpty() ? null : list.get(0);
    }

    private List<Announcement> query(String sql) {
        List<Announcement> list = new ArrayList<>();
        try (Statement st = conn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Announcement(rs.getInt("announcement_id"), rs.getString("title"), rs.getString("message"),
                        "IMPORTANT".equals(rs.getString("priority")), rs.getString("created_at")));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }
}