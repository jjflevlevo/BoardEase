package boardinghouse.dao;

import boardinghouse.util.DatabaseConnection;
import java.sql.*;

public class MeterReadingDAO {
    private Connection conn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public boolean insert(int roomId, String type, double reading, String month) {
        String sql = "INSERT INTO meter_readings (room_id, type, reading, billing_month) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setString(2, type);
            ps.setDouble(3, reading);
            ps.setString(4, month);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            return false; // a reading for that room, type, and month already exists
        }
    }

    public Double find(int roomId, String type, String month) {
        String sql = "SELECT reading FROM meter_readings WHERE room_id = ? AND type = ? AND billing_month = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setString(2, type);
            ps.setString(3, month);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble(1) : null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // latest reading from an earlier month
    public Double findPrevious(int roomId, String type, String month) {
        String sql = "SELECT reading FROM meter_readings WHERE room_id = ? AND type = ? AND billing_month < ? "
                + "ORDER BY billing_month DESC LIMIT 1";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setString(2, type);
            ps.setString(3, month);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble(1) : null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}