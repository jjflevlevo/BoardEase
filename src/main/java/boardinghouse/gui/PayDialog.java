package boardinghouse.gui;

import boardinghouse.dao.SettingsDAO;
import boardinghouse.dao.SettingsDAO.GcashInfo;
import boardinghouse.model.Bill;
import boardinghouse.model.Tenant;
import boardinghouse.service.PaymentService;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;

public class PayDialog extends JDialog {
    private final PaymentService payments = new PaymentService();
    private final SettingsDAO settings = new SettingsDAO();
    private final Tenant tenant;
    private final Bill bill;
    private boolean submitted = false;
    private File proofFile;

    private final JRadioButton cash = radio("Cash");
    private final JRadioButton online = radio("Pay Online (GCash)");
    private final JTextField amount = Theme.field("Amount");
    private final JPanel onlinePanel = new JPanel();
    private final JPanel gcashBox = new JPanel(new BorderLayout());
    private final JTextField ref = Theme.field("Enter GCash reference number");
    private final JButton chooseFile = Theme.secondary("Choose File");
    private final JLabel fileName = Theme.muted("No file chosen");
    private final JLabel error = new JLabel(" ");
    private final JButton payBtn = Theme.primary("Pay Now");

    public PayDialog(Window owner, Tenant tenant, Bill bill) {
        super(owner, "Pay Bill", ModalityType.APPLICATION_MODAL);
        this.tenant = tenant;
        this.bill = bill;

        double balance = payments.balanceOf(bill);
        double lateFee = bill.getLateFee(LocalDate.now());
        double paid = Math.max(0, bill.getAmount() + lateFee - balance);
        amount.setText(String.format("%.2f", balance));
        error.setForeground(Theme.DANGER);

        ButtonGroup group = new ButtonGroup();
        group.add(cash);
        group.add(online);
        cash.setSelected(true);
        cash.addActionListener(e -> updateMethod());
        online.addActionListener(e -> updateMethod());
        chooseFile.addActionListener(e -> chooseImage());
        payBtn.addActionListener(e -> submit());
        JButton cancel = Theme.secondary("Cancel");
        cancel.addActionListener(e -> dispose());

        // left side: bill details
        JPanel left = column();
        left.add(Theme.block(Theme.title("Bill Details"), 28));
        left.add(Theme.gap(10));
        JPanel details = Theme.detailGrid(new String[][]{
                {"Type", bill.getType()},
                {"Month", bill.getBillingMonth()},
                {"Amount", Ui.peso(bill.getAmount())},
                {"Late fee", Ui.peso(lateFee)},
                {"Paid so far", Ui.peso(paid)}});
        details.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(details);
        left.add(Theme.gap(14));
        JSeparator line = new JSeparator();
        line.setForeground(Theme.BORDER);
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(line);
        left.add(Theme.gap(14));
        left.add(Theme.block(Theme.muted("Balance Due"), 18));
        JLabel total = new JLabel(Ui.peso(balance));
        total.setFont(Theme.font(20, Font.BOLD));
        total.setForeground(Theme.ACCENT);
        total.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(total);
        left.add(Box.createVerticalGlue());
        left.add(Box.createHorizontalStrut(220));
        left.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDER),
                BorderFactory.createEmptyBorder(0, 0, 0, 24)));

        // right side: payment method
        onlinePanel.setOpaque(false);
        onlinePanel.setLayout(new BoxLayout(onlinePanel, BoxLayout.Y_AXIS));
        onlinePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        gcashBox.setOpaque(false);
        gcashBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel uploadLabel = Theme.muted("Upload Receipt (Optional)");
        uploadLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel fileRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        fileRow.setOpaque(false);
        fileRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        fileRow.add(chooseFile);
        fileRow.add(fileName);
        onlinePanel.add(gcashBox);
        onlinePanel.add(Theme.gap(10));
        onlinePanel.add(Theme.labeled("Reference Number *", ref));
        onlinePanel.add(Theme.gap(6));
        onlinePanel.add(uploadLabel);
        onlinePanel.add(Theme.gap(4));
        onlinePanel.add(fileRow);

        JPanel radios = column();
        radios.add(cash);
        radios.add(Theme.gap(4));
        radios.add(online);

        JPanel right = column();
        right.add(Theme.block(Theme.title("Payment Method"), 28));
        right.add(Theme.gap(10));
        right.add(Theme.labeled("Amount to pay (\u20B1)", amount));
        right.add(Theme.gap(8));
        right.add(radios);
        right.add(Theme.gap(10));
        right.add(onlinePanel);
        right.add(Theme.gap(6));
        error.setAlignmentX(Component.LEFT_ALIGNMENT);
        right.add(error);
        right.add(Box.createHorizontalStrut(380));

        JPanel body = new JPanel(new BorderLayout(24, 0));
        body.setOpaque(false);
        body.add(left, BorderLayout.WEST);
        body.add(right, BorderLayout.CENTER);

        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(Theme.CARD);
        root.setBorder(new EmptyBorder(24, 28, 20, 28));
        root.add(body, BorderLayout.CENTER);
        root.add(Theme.buttonRow(cancel, payBtn), BorderLayout.SOUTH);

        setContentPane(root);
        updateMethod();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    public boolean wasSubmitted() { return submitted; }

    private static JRadioButton radio(String text) {
        JRadioButton r = new JRadioButton(text);
        r.setOpaque(false);
        r.setForeground(Theme.TEXT);
        r.setFocusPainted(false);
        r.setFont(Theme.font(11, Font.PLAIN));
        r.setAlignmentX(Component.LEFT_ALIGNMENT);
        return r;
    }

    private static JPanel column() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    // Pay Online is only valid when the owner has given GCash information
    private void updateMethod() {
        boolean on = online.isSelected();
        onlinePanel.setVisible(on);
        error.setText(" ");
        gcashBox.removeAll();

        if (!on) {
            payBtn.setEnabled(true);
        } else {
            GcashInfo g = settings.getGcash();
            if (g == null) {
                gcashBox.add(noticeBox(), BorderLayout.CENTER);
                ref.setEnabled(false);
                chooseFile.setEnabled(false);
                payBtn.setEnabled(false);
            } else {
                gcashBox.add(infoBox(g), BorderLayout.CENTER);
                ref.setEnabled(true);
                chooseFile.setEnabled(true);
                payBtn.setEnabled(true);
            }
        }
        gcashBox.revalidate();
        gcashBox.repaint();
        pack();
    }

    // the owner's GCash name and number (can be selected and copied)
    private JPanel infoBox(GcashInfo g) {
        RoundedPanel box = new RoundedPanel(new Color(0x1B2F52), Theme.ACCENT, 8, 12);
        box.setLayout(new BorderLayout());
        box.add(Theme.detailGrid(new String[][]{
                {"GCash Name", g.name()},
                {"GCash Number", g.phone()}}), BorderLayout.CENTER);
        return box;
    }

    private JPanel noticeBox() {
        RoundedPanel box = new RoundedPanel(new Color(0x3A1D24), Theme.DANGER, 8, 12);
        box.setLayout(new BorderLayout());
        JLabel l = new JLabel("<html><body style='width:320px'>GCash not available. The owner has not given any "
                + "GCash information yet. Please pay by cash.</body></html>");
        l.setForeground(Theme.DANGER);
        box.add(l, BorderLayout.CENTER);
        return box;
    }

    private void chooseImage() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Images (png, jpg, jpeg)", "png", "jpg", "jpeg"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            proofFile = fc.getSelectedFile();
            fileName.setText(proofFile.getName());
            pack();
        }
    }

    private void submit() {
        double amt;
        try {
            amt = Double.parseDouble(amount.getText().trim());
        } catch (NumberFormatException e) {
            showError("Enter a valid amount.");
            return;
        }

        String result = online.isSelected()
                ? payments.submitPayment(tenant.getTenantId(), bill.getBillId(), amt, "GCASH", ref.getText().trim(), proofFile)
                : payments.submitPayment(tenant.getTenantId(), bill.getBillId(), amt);

        if (result.startsWith("Payment submitted")) {
            submitted = true;
            JOptionPane.showMessageDialog(this, result);
            dispose();
        } else {
            showError(result);
        }
    }

    private void showError(String text) {
        error.setText("<html><body style='width:340px'>" + Theme.esc(text) + "</body></html>");
        pack();
    }
}