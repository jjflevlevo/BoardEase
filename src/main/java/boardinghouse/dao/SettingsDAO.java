package boardinghouse.dao;

import boardinghouse.util.DatabaseConnection;
import java.sql.*;

public class SettingsDAO {
    public record GcashInfo(String name, String phone) {}

    public static final int DEFAULT_AUTO_LOGOUT_MINUTES = 10;

    private static final String NAME_KEY = "gcash_name";
    private static final String PHONE_KEY = "gcash_phone";
    private static final String AUTO_LOGOUT_KEY = "auto_logout_minutes";
    private static final String LAST_BACKUP_KEY = "last_backup";

    private Connection conn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ---------- GCash ----------
    // returns null when the owner has not given any GCash information
    public GcashInfo getGcash() {
        String name = get(NAME_KEY);
        String phone = get(PHONE_KEY);
        if (name == null || name.isBlank() || phone == null || phone.isBlank()) return null;
        return new GcashInfo(name, phone);
    }

    public boolean hasGcash() { return getGcash() != null; }

    public void saveGcash(String name, String phone) {
        set(NAME_KEY, name);
        set(PHONE_KEY, phone);
    }

    public void clearGcash() {
        delete(NAME_KEY);
        delete(PHONE_KEY);
    }

    // ---------- Auto logout (0 means off) ----------
    public int getAutoLogoutMinutes() {
        String v = get(AUTO_LOGOUT_KEY);
        if (v == null) return DEFAULT_AUTO_LOGOUT_MINUTES;
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return DEFAULT_AUTO_LOGOUT_MINUTES;
        }
    }

    public void setAutoLogoutMinutes(int minutes) {
        set(AUTO_LOGOUT_KEY, String.valueOf(minutes));
    }

    // ---------- Backups ----------
    public String getLastBackup() { return get(LAST_BACKUP_KEY); }

    public void setLastBackup(String text) { set(LAST_BACKUP_KEY, text); }

    // ---------- Storage ----------
    private String get(String key) {
        try (PreparedStatement ps = conn().prepareStatement("SELECT setting_value FROM settings WHERE setting_key = ?")) {
            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString(1) : null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void set(String key, String value) {
        try (PreparedStatement ps = conn().prepareStatement(
                "INSERT OR REPLACE INTO settings (setting_key, setting_value) VALUES (?, ?)")) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void delete(String key) {
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM settings WHERE setting_key = ?")) {
            ps.setString(1, key);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}