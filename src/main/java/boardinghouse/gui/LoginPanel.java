package boardinghouse.gui;

import boardinghouse.model.Account;
import boardinghouse.service.AuthService;
import boardinghouse.util.Validator;
import javax.swing.*;
import java.awt.*;
import java.util.prefs.Preferences;

public class LoginPanel extends JPanel {
    private static final String REMEMBER_KEY = "rememberedEmail";

    private final MainFrame frame;
    private final AuthService auth = new AuthService();
    private final Preferences prefs = Preferences.userNodeForPackage(LoginPanel.class);

    private final JTextField email = Theme.field("Enter your email");
    private final JPasswordField password = Theme.password("Enter your password");
    private final JLabel emailError = Theme.error();
    private final JLabel passwordError = Theme.error();
    private final JLabel loginError = Theme.error();
    private final JCheckBox remember = new JCheckBox("Remember Me");

    public LoginPanel(MainFrame frame) {
        this.frame = frame;
        setBackground(Theme.BG);
        setLayout(new GridBagLayout());

        // Remember Me keeps the email only, never the password
        String saved = prefs.get(REMEMBER_KEY, "");
        if (!saved.isEmpty()) {
            email.setText(saved);
            remember.setSelected(true);
        }

        Theme.clearErrorOnEdit(email, emailError);
        Theme.clearErrorOnEdit(password, passwordError);

        JPanel shell = new RoundedPanel(Theme.CARD, null, 12, 0);
        shell.setLayout(new BorderLayout());
        shell.setPreferredSize(new Dimension(1086, 620));
        shell.setMinimumSize(new Dimension(900, 560));
        shell.add(new BrandPanel(), BorderLayout.WEST);
        shell.add(buildForm(), BorderLayout.CENTER);
        add(shell);

        email.addActionListener(e -> password.requestFocusInWindow());
        SwingUtilities.invokeLater(() -> (saved.isEmpty() ? email : password).requestFocusInWindow());
    }

    private JPanel buildForm() {
        JPanel right = new JPanel(new GridBagLayout());
        right.setOpaque(false);

        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

        JLabel welcome = Theme.pageTitle("Welcome Back!");
        JLabel subtitle = new JLabel("Sign in to your account");
        subtitle.setForeground(Theme.MUTED);

        remember.setOpaque(false);
        remember.setForeground(Theme.MUTED);
        remember.setFocusPainted(false);
        JButton forgot = Theme.link("Forgot Password?");
        forgot.addActionListener(e -> showForgotPassword());
        JPanel options = new JPanel(new BorderLayout());
        options.setOpaque(false);
        options.add(remember, BorderLayout.WEST);
        options.add(forgot, BorderLayout.EAST);

        JButton signIn = Theme.primary("Sign In");
        signIn.addActionListener(e -> login());
        password.addActionListener(e -> login());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        footer.setOpaque(false);
        JLabel noAccount = new JLabel("Don't have an account?");
        noAccount.setForeground(Theme.MUTED);
        JButton signUp = Theme.link("Sign Up");
        signUp.addActionListener(e -> frame.showSignUp());
        footer.add(noAccount);
        footer.add(signUp);

        col.add(Theme.block(welcome, 36));
        col.add(Theme.block(subtitle, 24));
        col.add(Theme.gap(30));
        col.add(Theme.fieldRow("Email Address", email, emailError));
        col.add(Theme.fieldRow("Password", password, passwordError));
        col.add(Theme.block(options, 28));
        col.add(Theme.gap(8));
        col.add(Theme.block(loginError, 22));
        col.add(Theme.block(signIn, 44));
        col.add(Theme.gap(20));
        col.add(Theme.block(footer, 28));
        col.setPreferredSize(new Dimension(546, col.getPreferredSize().height));

        right.add(col);
        return right;
    }

    private void login() {
        String mail = email.getText().trim();
        String pw = new String(password.getPassword());
        loginError.setText(" ");

        boolean ok = true;
        ok &= Theme.check(email, emailError, Validator.isValidEmail(mail), "Enter a valid email address.");
        ok &= Theme.check(password, passwordError, !pw.isEmpty(), "Enter your password.");
        if (!ok) return;

        Account acc = auth.login(mail, pw);
        if (acc == null) {
            loginError.setText("Incorrect email or password. Please try again.");
            password.requestFocusInWindow();
            password.selectAll();
            return;
        }

        if (remember.isSelected()) prefs.put(REMEMBER_KEY, mail);
        else prefs.remove(REMEMBER_KEY);

        if ("OWNER".equals(acc.getRole())) frame.showOwner(acc);
        else frame.showTenant(acc);
    }

    // there is no email system, so password resets are done by the owner
    private void showForgotPassword() {
        JLabel text = new JLabel("<html><body style='width:340px'>Passwords are reset by the boarding house owner. "
                + "Contact the owner and ask them to reset your password.<br><br>"
                + "They will give you a temporary password. After you log in, change it under "
                + "My Profile, Change Password.</body></html>");
        JOptionPane.showMessageDialog(this, text, "Forgot Password", JOptionPane.INFORMATION_MESSAGE);
    }

    // left panel: the BoardEase logo and tagline on a dark blue gradient
    private static class BrandPanel extends JPanel {
        BrandPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(380, 0));
            setLayout(new GridBagLayout());

            Logo logo = new Logo(300);
            logo.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel description = new JLabel("<html><div style='text-align:center;width:300px'>"
                    + "Rent and Utilities Billing System</div></html>");
            description.setForeground(Theme.MUTED);
            description.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel tagline = new JLabel("<html><div style='text-align:center;width:300px'>"
                    + "Manage rooms, bills, and payments with ease.</div></html>");
            tagline.setForeground(Theme.PLACEHOLDER);
            tagline.setFont(Theme.font(9, Font.PLAIN));
            tagline.setAlignmentX(Component.CENTER_ALIGNMENT);

            JPanel content = new JPanel();
            content.setOpaque(false);
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.add(logo);
            content.add(Box.createVerticalStrut(24));
            content.add(description);
            content.add(Box.createVerticalStrut(36));
            content.add(tagline);
            add(content);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setPaint(new GradientPaint(0, 0, new Color(0x16294A), 0, h, new Color(0x0F1A2E)));
            g2.fillRoundRect(0, 0, w, h, 24, 24);
            g2.fillRect(w / 2, 0, w - w / 2, h); // square off the right side
            g2.dispose();
            super.paintComponent(g);
        }
    }
}