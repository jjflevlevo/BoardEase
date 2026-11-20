package boardinghouse.gui;

import boardinghouse.dao.AccountDAO;
import boardinghouse.model.Tenant;
import boardinghouse.service.AuthService;
import boardinghouse.util.Validator;
import javax.swing.*;
import java.awt.*;

public class SignUpPanel extends JPanel {
    private final MainFrame frame;
    private final AuthService auth = new AuthService();
    private final AccountDAO accountDAO = new AccountDAO();
    private int step = 1;

    // step 1: account
    private final JTextField email = Theme.field("name@example.com");
    private final JTextField phone = Theme.field("09XXXXXXXXX");
    private final JPasswordField password = Theme.password("At least 6 characters");
    private final JPasswordField confirm = Theme.password("Repeat your password");
    private final JLabel emailError = Theme.error();
    private final JLabel phoneError = Theme.error();
    private final JLabel passwordError = Theme.error();
    private final JLabel confirmError = Theme.error();

    // step 2: personal info
    private final JTextField name = Theme.field("Full name");
    private final JTextField address = Theme.field("Home address");
    private final JTextField age = Theme.field("Age");
    private final JTextField occupation = Theme.field("Occupation");
    private final JTextField emergency = Theme.field("09XXXXXXXXX");
    private final JLabel nameError = Theme.error();
    private final JLabel addressError = Theme.error();
    private final JLabel ageError = Theme.error();
    private final JLabel occupationError = Theme.error();
    private final JLabel emergencyError = Theme.error();

    private final CardLayout cards = new CardLayout();
    private final JPanel holder = new JPanel(cards);
    private final JPanel steps = new StepIndicator();
    private final JButton leftBtn = Theme.secondary("Cancel");
    private final JButton rightBtn = Theme.primary("Next \u2192");

    public SignUpPanel(MainFrame frame) {
        this.frame = frame;
        setBackground(Theme.BG);
        setLayout(new GridBagLayout());

        Theme.clearErrorOnEdit(email, emailError);
        Theme.clearErrorOnEdit(phone, phoneError);
        Theme.clearErrorOnEdit(password, passwordError);
        Theme.clearErrorOnEdit(confirm, confirmError);
        Theme.clearErrorOnEdit(name, nameError);
        Theme.clearErrorOnEdit(address, addressError);
        Theme.clearErrorOnEdit(age, ageError);
        Theme.clearErrorOnEdit(occupation, occupationError);
        Theme.clearErrorOnEdit(emergency, emergencyError);

        holder.setOpaque(false);
        holder.setAlignmentX(Component.LEFT_ALIGNMENT);
        holder.add(stepOne(), "1");
        holder.add(stepTwo(), "2");

        JLabel subtitle = new JLabel("Tell us a little about you");
        subtitle.setForeground(Theme.MUTED);

        JPanel buttons = new JPanel(new BorderLayout());
        buttons.setOpaque(false);
        buttons.add(leftBtn, BorderLayout.WEST);
        buttons.add(rightBtn, BorderLayout.EAST);

        RoundedPanel card = new RoundedPanel(Theme.CARD, Theme.BORDER, 12, 32);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(Theme.block(Theme.pageTitle("Create Your Account"), 36));
        card.add(Theme.block(subtitle, 24));
        card.add(Theme.gap(16));
        card.add(Theme.block(steps, 36));
        card.add(Theme.gap(20));
        card.add(holder);
        card.add(Theme.gap(12));
        card.add(Theme.block(buttons, 40));
        card.setPreferredSize(new Dimension(560, card.getPreferredSize().height));
        add(card);

        leftBtn.addActionListener(e -> back());
        rightBtn.addActionListener(e -> next());
        confirm.addActionListener(e -> next());
        emergency.addActionListener(e -> next());

        SwingUtilities.invokeLater(() -> email.requestFocusInWindow());
    }

    private JPanel column() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    private JPanel stepOne() {
        JPanel p = column();
        p.add(Theme.fieldRow("Email Address", email, emailError));
        p.add(Theme.fieldRow("Phone Number", phone, phoneError));
        p.add(Theme.fieldRow("Password", password, passwordError));
        p.add(Theme.fieldRow("Confirm Password", confirm, confirmError));
        return p;
    }

    private JPanel stepTwo() {
        JPanel p = column();
        JPanel row = new JPanel(new GridLayout(1, 2, 16, 0));
        row.setOpaque(false);
        row.add(Theme.fieldRow("Age", age, ageError));
        row.add(Theme.fieldRow("Occupation", occupation, occupationError));

        p.add(Theme.fieldRow("Full Name", name, nameError));
        p.add(Theme.fieldRow("Address", address, addressError));
        p.add(Theme.block(row, 84));
        p.add(Theme.fieldRow("Emergency Contact No.", emergency, emergencyError));
        return p;
    }

    // ---------- Navigation ----------
    private void next() {
        if (step == 1) {
            if (validateStepOne()) goTo(2);
        } else {
            submit();
        }
    }

    private void back() {
        if (step == 1) frame.showLogin();
        else goTo(1);
    }

    private void goTo(int s) {
        step = s;
        cards.show(holder, String.valueOf(s));
        Theme.setButtonText(leftBtn, s == 1 ? "Cancel" : "\u2190 Back");
        Theme.setButtonText(rightBtn, s == 1 ? "Next \u2192" : "Create Account");
        steps.repaint();
        (s == 1 ? email : name).requestFocusInWindow();
        revalidate();
        repaint();
    }

    // ---------- Validation ----------
    private boolean validateStepOne() {
        String mail = email.getText().trim();
        String ph = phone.getText().trim();
        String pw = new String(password.getPassword());
        String pw2 = new String(confirm.getPassword());

        boolean emailOk = Validator.isValidEmail(mail);
        String emailMessage = "Enter a valid email address.";
        if (emailOk && accountDAO.findByEmail(mail) != null) {
            emailOk = false;
            emailMessage = "That email is already registered.";
        }

        boolean ok = true;
        ok &= Theme.check(email, emailError, emailOk, emailMessage);
        ok &= Theme.check(phone, phoneError, Validator.isValidPhone(ph), "Use 11 digits starting with 09.");
        ok &= Theme.check(password, passwordError, Validator.isValidPassword(pw),
                "Password must be at least 6 characters.");
        ok &= Theme.check(confirm, confirmError, !pw2.isEmpty() && pw2.equals(pw), "Passwords do not match.");
        return ok;
    }

    private Integer parseAge() {
        try {
            return Integer.parseInt(age.getText().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void submit() {
        String n = name.getText().trim();
        String a = address.getText().trim();
        String o = occupation.getText().trim();
        String em = emergency.getText().trim();
        Integer ageValue = parseAge();

        boolean ok = true;
        ok &= Theme.check(name, nameError, Validator.isNotBlank(n), "Enter your full name.");
        ok &= Theme.check(address, addressError, Validator.isNotBlank(a), "Enter your address.");
        ok &= Theme.check(age, ageError, ageValue != null && Validator.isValidAge(ageValue), "Age must be 16 to 100.");
        ok &= Theme.check(occupation, occupationError, Validator.isNotBlank(o), "Enter your occupation.");
        ok &= Theme.check(emergency, emergencyError, Validator.isValidPhone(em), "Use 11 digits starting with 09.");
        if (!ok) return;

        Tenant tenant = new Tenant(n, a, ageValue, o, em);
        boolean created = auth.signUp(email.getText().trim(), phone.getText().trim(),
                new String(password.getPassword()), tenant);
        if (created) {
            JOptionPane.showMessageDialog(this, "Account created! You can now log in.");
            frame.showLogin();
        } else {
            goTo(1);
            Theme.check(email, emailError, false, "That email is already registered.");
        }
    }

    // numbered circles: Account ---- Personal Info
    private class StepIndicator extends JPanel {
        StepIndicator() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            String[] names = {"Account", "Personal Info"};
            int d = 28;
            int cy = getHeight() / 2;
            Font labelFont = Theme.font(10, Font.BOLD);
            FontMetrics lf = g2.getFontMetrics(labelFont);

            int firstWidth = d + 8 + lf.stringWidth(names[0]);
            int secondWidth = d + 8 + lf.stringWidth(names[1]);
            int[] x = {0, getWidth() - secondWidth};

            // line between the two steps
            g2.setColor(step > 1 ? Theme.ACCENT : Theme.BORDER);
            g2.fillRect(firstWidth + 12, cy - 1, Math.max(0, x[1] - firstWidth - 24), 2);

            for (int i = 0; i < 2; i++) {
                boolean active = step == i + 1;
                boolean done = step > i + 1;

                g2.setColor(active || done ? Theme.ACCENT : Theme.CARD_HOVER);
                g2.fillOval(x[i], cy - d / 2, d, d);
                if (!active && !done) {
                    g2.setColor(Theme.BORDER_STRONG);
                    g2.drawOval(x[i], cy - d / 2, d - 1, d - 1);
                }

                g2.setFont(Theme.font(10, Font.BOLD));
                FontMetrics nf = g2.getFontMetrics();
                String number = done ? "\u2713" : String.valueOf(i + 1);
                g2.setColor(active || done ? Color.WHITE : Theme.MUTED);
                g2.drawString(number, x[i] + (d - nf.stringWidth(number)) / 2,
                        cy + (nf.getAscent() - nf.getDescent()) / 2);

                g2.setFont(labelFont);
                g2.setColor(active || done ? Theme.TEXT : Theme.MUTED);
                g2.drawString(names[i], x[i] + d + 8, cy + (lf.getAscent() - lf.getDescent()) / 2);
            }
            g2.dispose();
        }
    }
}