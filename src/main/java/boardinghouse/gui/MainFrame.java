package boardinghouse.gui;

import boardinghouse.model.Account;
import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    public MainFrame() {
        super("BoardEase");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setIconImage(Logo.appIcon());
        setMinimumSize(new Dimension(1000, 680));
        setSize(1150, 760);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.BG);
        showLogin();
    }

    public void showLogin() { swap(new LoginPanel(this)); }

    public void showSignUp() { swap(new SignUpPanel(this)); }

    public void showTenant(Account account) { swap(new TenantPanel(this, account)); }

    public void showOwner(Account account) { swap(new OwnerPanel(this, account)); }

    private void swap(JPanel panel) {
        setContentPane(panel);
        revalidate();
        repaint();
    }
}