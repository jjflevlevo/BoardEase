package boardinghouse.service;

import boardinghouse.dao.ActivityDAO;
import boardinghouse.dao.SettingsDAO;
import boardinghouse.util.DatabaseConnection;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

// saves and restores all data (database + payment screenshots) as one .zip file
public class BackupService {
    private static final String DB_ENTRY = "boardinghouse.db";
    private static final String UPLOADS_ENTRY = "uploads/";
    private static final Path DB_FILE = Path.of(DatabaseConnection.DB_FILE);
    private static final Path UPLOADS = Path.of("uploads");
    private static final Path SAFETY_FOLDER = Path.of("backups");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");

    private final SettingsDAO settings = new SettingsDAO();
    private final ActivityDAO activity = new ActivityDAO();

    public static String defaultFileName() {
        return "BoardEase_backup_" + LocalDateTime.now().format(STAMP) + ".zip";
    }

    // ---------- Backup ----------
    public void backup(Path target) throws IOException {
        Path snapshot = Files.createTempFile("boardease", ".db");
        Files.deleteIfExists(snapshot); // VACUUM INTO needs a file that does not exist yet
        try {
            // makes a clean copy of the database even while the app is using it
            try (Statement st = DatabaseConnection.getInstance().getConnection().createStatement()) {
                st.execute("VACUUM INTO '" + snapshot.toAbsolutePath().toString().replace("'", "''") + "'");
            } catch (SQLException e) {
                throw new IOException("Could not copy the database: " + e.getMessage(), e);
            }

            Path parent = target.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(target))) {
                addFile(zip, snapshot, DB_ENTRY);
                if (Files.isDirectory(UPLOADS)) {
                    try (DirectoryStream<Path> files = Files.newDirectoryStream(UPLOADS)) {
                        for (Path f : files) {
                            if (Files.isRegularFile(f)) addFile(zip, f, UPLOADS_ENTRY + f.getFileName());
                        }
                    }
                }
            }
        } finally {
            Files.deleteIfExists(snapshot);
        }
        settings.setLastBackup(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy  h:mm a")));
        activity.log("SYSTEM", "Backup created: " + target.getFileName());
    }

    private void addFile(ZipOutputStream zip, Path file, String name) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        Files.copy(file, zip);
        zip.closeEntry();
    }

    // ---------- Restore ----------
    // replaces the current data with a backup; a copy of the current data is saved first
    public String restore(Path source) throws IOException {
        Path extracted = Files.createTempFile("boardease_restore", ".db");
        boolean hasDatabase = false;
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(source))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().equals(DB_ENTRY)) {
                    Files.copy(zip, extracted, StandardCopyOption.REPLACE_EXISTING);
                    hasDatabase = true;
                }
            }
        }
        if (!hasDatabase || !looksValid(extracted)) {
            Files.deleteIfExists(extracted);
            throw new IOException("This file is not a BoardEase backup, or it is damaged.");
        }

        Path safety = SAFETY_FOLDER.resolve("before_restore_" + LocalDateTime.now().format(STAMP) + ".zip");
        backup(safety);

        DatabaseConnection.getInstance().close();
        Files.move(extracted, DB_FILE, StandardCopyOption.REPLACE_EXISTING);
        int images = restoreUploads(source);
        DatabaseConnection.getInstance().initialize(); // also upgrades an older backup

        activity.log("SYSTEM", "Data restored from " + source.getFileName());
        return "Restore complete (" + images + " payment screenshot(s) restored).\n\n"
                + "A copy of your previous data was saved to:\n" + safety.toAbsolutePath();
    }

    // a real BoardEase database has an accounts table with an owner
    private boolean looksValid(Path db) {
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + db.toAbsolutePath());
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM accounts WHERE role = 'OWNER'")) {
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    private int restoreUploads(Path source) throws IOException {
        int count = 0;
        Path base = UPLOADS.toAbsolutePath().normalize();
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(source))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (entry.isDirectory() || !name.startsWith(UPLOADS_ENTRY)) continue;
                String fileName = name.substring(UPLOADS_ENTRY.length());
                if (fileName.isBlank()) continue;

                Path dest = base.resolve(fileName).normalize();
                if (!dest.startsWith(base) || dest.equals(base)) continue; // ignore unsafe paths
                Files.createDirectories(base);
                Files.copy(zip, dest, StandardCopyOption.REPLACE_EXISTING);
                count++;
            }
        }
        return count;
    }
}