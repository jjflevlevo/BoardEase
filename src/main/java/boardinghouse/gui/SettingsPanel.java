package boardinghouse.gui;

import boardinghouse.dao.SettingsDAO;
import boardinghouse.service.AuthService;
import boardinghouse.service.BackupService;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;

// owner settings: automatic logout, and backup and restore
public class SettingsPanel extends Section {
    private static final String[] LOGOUT_LABELS = {"Off", "1 minute (for testing)", "5 minutes", "10 minutes",
            "15 minutes", "30 minutes"};
    private static final int[] LOGOUT_MINUTES = {0, 1, 5, 10, 15, 30};

    private final SettingsDAO settings = new SettingsDAO();
    private final BackupService backups = new BackupService();
    private final JComboBox<String> logoutBox = new JComboBox<>(LOGOUT_LABELS);
    private final JLabel lastBackup = Theme.muted(" ");

    public SettingsPanel() {
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.add(securityCard());
        column.add(Theme.gap(16));
        column.add(backupCard());

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(column, BorderLayout.NORTH);
        add(wrap, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        int minutes = settings.getAutoLogoutMinutes();
        int index = 3;
        for (int i = 0; i < LOGOUT_MINUTES.length; i++) {
            if (LOGOUT_MINUTES[i] == minutes) index = i;
        }
        logoutBox.setSelectedIndex(index);

        String last = settings.getLastBackup();
        lastBackup.setText(last == null ? "No backup made yet." : "Last backup: " + last);
    }

    // ---------- Cards ----------
    private JPanel stackCard(Component... items) {
        JPanel card = Theme.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        for (Component c : items) card.add(c);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
        return card;
    }

    private JPanel securityCard() {
        JButton save = Theme.secondary("Save");
        save.addActionListener(e -> saveLogout());
        logoutBox.setPreferredSize(new Dimension(210, 40));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        row.setOpaque(false);
        row.add(new JLabel("Log out automatically after no activity for:"));
        row.add(logoutBox);
        row.add(save);

        return stackCard(
                Theme.block(Theme.title("Security"), 28),
                Theme.gap(4),
                Theme.block(Theme.muted("Sign-in is locked for 1 minute after " + AuthService.MAX_ATTEMPTS
                        + " wrong passwords. Resetting a tenant's password also unlocks their account."), 20),
                Theme.gap(16),
                Theme.block(row, 40));
    }

    private JPanel backupCard() {
        JButton create = Theme.primary("Create Backup");
        create.addActionListener(e -> createBackup());
        JButton restore = Theme.danger("Restore from Backup");
        restore.addActionListener(e -> restoreBackup());

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        row.setOpaque(false);
        row.add(create);
        row.add(restore);

        return stackCard(
                Theme.block(Theme.title("Backup and Restore"), 28),
                Theme.gap(4),
                Theme.block(Theme.muted("A backup saves all rooms, tenants, bills, payments, and payment screenshots "
                        + "in one .zip file. Keep a copy on a flash drive or in Google Drive."), 20),
                Theme.gap(6),
                Theme.block(lastBackup, 20),
                Theme.gap(16),
                Theme.block(row, 40));
    }

    // ---------- Actions ----------
    private void saveLogout() {
        int minutes = LOGOUT_MINUTES[logoutBox.getSelectedIndex()];
        settings.setAutoLogoutMinutes(minutes);
        msg(minutes == 0 ? "Automatic logout is now off."
                : "Saved. Users are logged out after " + minutes + " minute(s) with no activity.");
    }

    private void createBackup() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("BoardEase backup (.zip)", "zip"));
        fc.setSelectedFile(new File(BackupService.defaultFileName()));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = fc.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".zip")) file = new File(file.getParentFile(), file.getName() + ".zip");
        if (file.exists() && !Theme.confirm(this, "Replace the existing file " + file.getName() + "?", "Create Backup")) return;

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            backups.backup(file.toPath());
            msg("Backup saved to:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            msg("Backup failed: " + e.getMessage());
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
        refresh();
    }

    private void restoreBackup() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("BoardEase backup (.zip)", "zip"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = fc.getSelectedFile();

        if (!Theme.confirmWarn(this, "Restore from " + file.getName() + "?\n\n"
                + "All current rooms, tenants, bills, and payments will be replaced by the backup.\n"
                + "A copy of the current data is saved first, just in case.\n"
                + "You will be logged out after the restore.", "Restore from Backup")) return;

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        String result;
        try {
            result = backups.restore(file.toPath());
        } catch (IOException e) {
            setCursor(Cursor.getDefaultCursor());
            msg("Restore failed: " + e.getMessage());
            return;
        }
        setCursor(Cursor.getDefaultCursor());
        msg(result);

        Window w = SwingUtilities.getWindowAncestor(this);
        if (w instanceof MainFrame frame) frame.showLogin("Data restored. Please log in again.");
    }
}