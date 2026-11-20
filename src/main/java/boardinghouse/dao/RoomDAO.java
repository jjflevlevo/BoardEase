package boardinghouse.dao;

import boardinghouse.model.Room;
import boardinghouse.util.DatabaseConnection;
import java.sql.*;
import java.util.*;

public class RoomDAO {
    private Connection conn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public boolean insert(String roomNo, int capacity, double rent) {
        String sql = "INSERT INTO rooms (room_no, capacity, monthly_rent) VALUES (?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, roomNo);
            ps.setInt(2, capacity);
            ps.setDouble(3, rent);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            return false; // duplicate room number
        }
    }

    public List<Room> findAll() {
        List<Room> list = new ArrayList<>();
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM rooms ORDER BY room_no")) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public Room findById(int id) {
        try (PreparedStatement ps = conn().prepareStatement("SELECT * FROM rooms WHERE room_id = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    // returns null when the update worked, otherwise the reason it failed
    public String update(int roomId, String roomNo, int capacity, double rent) {
        try {
            int occupants = countTenants(roomId);
            if (capacity < occupants) {
                return "Capacity cannot be lower than the current number of tenants (" + occupants + ").";
            }
            String sql = "UPDATE rooms SET room_no = ?, capacity = ?, monthly_rent = ? WHERE room_id = ?";
            try (PreparedStatement ps = conn().prepareStatement(sql)) {
                ps.setString(1, roomNo);
                ps.setInt(2, capacity);
                ps.setDouble(3, rent);
                ps.setInt(4, roomId);
                return ps.executeUpdate() == 1 ? null : "Room not found.";
            }
        } catch (SQLException e) {
            return "Could not update the room. That room no. may already be used.";
        }
    }

    // a room with tenants cannot be deleted; its meter readings are deleted with it
    public boolean delete(int id) {
        try {
            if (countTenants(id) > 0) return false;
            try (PreparedStatement ps = conn().prepareStatement("DELETE FROM meter_readings WHERE room_id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn().prepareStatement("DELETE FROM rooms WHERE room_id = ?")) {
                ps.setInt(1, id);
                return ps.executeUpdate() == 1;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private int countTenants(int roomId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("SELECT COUNT(*) FROM tenants WHERE room_id = ?")) {
            ps.setInt(1, roomId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private Room map(ResultSet rs) throws SQLException {
        return new Room(rs.getInt("room_id"), rs.getString("room_no"),
                rs.getInt("capacity"), rs.getDouble("monthly_rent"));
    }
}