package boardinghouse;

import boardinghouse.dao.AccountDAO;
import boardinghouse.gui.MainFrame;
import boardinghouse.gui.Theme;
import boardinghouse.model.Role;
import boardinghouse.util.DatabaseConnection;
import boardinghouse.util.PasswordUtil;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class Main {
    private static final String OWNER_EMAIL = "owner@boardinghouse.com";

    public static void main(String[] args) {
        // any unexpected error shows a friendly message instead of silently stopping
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> showError(error));

        try {
            DatabaseConnection.getInstance().initialize();

            // the owner account is created on the first run
            AccountDAO dao = new AccountDAO();
            if (dao.findByEmail(OWNER_EMAIL) == null) {
                dao.insert(OWNER_EMAIL, "09123456789", PasswordUtil.hash("owner123"), Role.OWNER.name());
            }
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(null, "BoardEase could not open its database:\n" + e.getMessage(),
                    "BoardEase", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Theme.install();
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }

    private static void showError(Throwable error) {
        error.printStackTrace();
        String detail = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                "Something went wrong:\n" + detail + "\n\nYour data is safe. Please try again.",
                "BoardEase", JOptionPane.ERROR_MESSAGE));
    }
}