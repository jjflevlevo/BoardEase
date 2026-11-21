package boardinghouse.gui;

import boardinghouse.dao.SettingsDAO;
import boardinghouse.model.Account;
import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private static final int CHECK_EVERY_MS = 15_000;

    private final SettingsDAO settings = new SettingsDAO();
    private long lastActivity = System.currentTimeMillis();
    private boolean loggedIn;

    public MainFrame() {
        super("BoardEase");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setIconImage(Logo.appIcon());
        setMinimumSize(new Dimension(1000, 680));
        setSize(1150, 760);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.BG);

        // any key press, click, scroll, or mouse movement counts as activity
        Toolkit.getDefaultToolkit().addAWTEventListener(e -> lastActivity = System.currentTimeMillis(),
                AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK
                        | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_WHEEL_EVENT_MASK);
        new Timer(CHECK_EVERY_MS, e -> checkIdle()).start();

        showLogin();
    }

    // Overload 1: plain login screen
    public void showLogin() { showLogin(null); }

    // Overload 2: login screen with a notice (for example, after an automatic logout)
    public void showLogin(String notice) {
        loggedIn = false;
        swap(new LoginPanel(this, notice));
    }

    public void showSignUp() {
        loggedIn = false;
        swap(new SignUpPanel(this));
    }

    public void showTenant(Account account) {
        startSession();
        swap(new TenantPanel(this, account));
    }

    public void showOwner(Account account) {
        startSession();
        swap(new OwnerPanel(this, account));
    }

    private void startSession() {
        loggedIn = true;
        lastActivity = System.currentTimeMillis();
    }

    private void swap(JPanel panel) {
        setContentPane(panel);
        revalidate();
        repaint();
    }

    // logs out after the number of idle minutes set by the owner (0 = off)
    private void checkIdle() {
        if (!loggedIn) return;
        int minutes = settings.getAutoLogoutMinutes();
        if (minutes <= 0) return;
        if (System.currentTimeMillis() - lastActivity < minutes * 60_000L) return;

        // close any open dialogs first
        for (Window w : Window.getWindows()) {
            if (w != this && w.isShowing()) w.dispose();
        }
        showLogin("You were logged out after " + minutes + " minute(s) of inactivity.");
    }
}