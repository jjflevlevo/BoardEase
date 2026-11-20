package boardinghouse.util;

import java.nio.file.*;
import java.sql.*;

public class DatabaseConnection {
    private static final String URL = "jdbc:sqlite:boardinghouse.db";
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
        boolean hasProof = false;
        try (ResultSet rs = st.executeQuery("PRAGMA table_info(payments)")) {
            while (rs.next()) {
                if ("proof_path".equals(rs.getString("name"))) hasProof = true;
            }
        }
        if (!hasProof) st.execute("ALTER TABLE payments ADD COLUMN proof_path TEXT");
    }
}