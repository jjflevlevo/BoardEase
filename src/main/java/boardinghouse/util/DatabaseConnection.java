package boardinghouse.util;

import java.nio.file.*;
import java.sql.*;

public class DatabaseConnection {
    public static final String DB_FILE = "boardinghouse.db";
    private static final String URL = "jdbc:sqlite:" + DB_FILE;
    private static DatabaseConnection instance;
    private Connection connection;

    private DatabaseConnection() {}

    public static DatabaseConnection getInstance() {
        if (instance == null) instance = new DatabaseConnection();
        return instance;
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(URL);
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }
        }
        return connection;
    }

    // used by restore, so the database file can be replaced
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException ignored) { }
        connection = null;
    }

    public void initialize() {
        try (Statement st = getConnection().createStatement()) {
            String sql = Files.readString(Path.of("sql/schema.sql"));
            for (String s : sql.split(";")) {
                if (!s.isBlank()) st.execute(s);
            }
            migrate(st);
        } catch (Exception e) {
            throw new RuntimeException("Database setup failed: " + e.getMessage(), e);
        }
    }

    // upgrades an older database without deleting any data
    private void migrate(Statement st) throws SQLException {
        addColumnIfMissing(st, "payments", "proof_path", "TEXT");
        addColumnIfMissing(st, "accounts", "failed_attempts", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing(st, "accounts", "locked_until", "INTEGER");
        addColumnIfMissing(st, "tenants", "status", "TEXT NOT NULL DEFAULT 'ACTIVE'");
        addColumnIfMissing(st, "tenants", "moved_out_on", "TEXT");
    }

    private void addColumnIfMissing(Statement st, String table, String column, String definition) throws SQLException {
        boolean found = false;
        try (ResultSet rs = st.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equals(rs.getString("name"))) found = true;
            }
        }
        if (!found) st.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
    }
}