package boardinghouse;

import boardinghouse.dao.AccountDAO;
import boardinghouse.gui.MainFrame;
import boardinghouse.gui.Theme;
import boardinghouse.ui.LoginMenu;
import boardinghouse.util.DatabaseConnection;
import boardinghouse.util.PasswordUtil;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        DatabaseConnection.getInstance().initialize();

        AccountDAO dao = new AccountDAO();
        if (dao.findByEmail("owner@boardinghouse.com") == null) {
            dao.insert("owner@boardinghouse.com", "09123456789", PasswordUtil.hash("owner123"), "OWNER");
        }

        // run with the argument "console" to use the old console menus
        if (args.length > 0 && args[0].equals("console")) {
            new LoginMenu().show();
            return;
        }

        Theme.install();
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}