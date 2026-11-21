package boardinghouse.gui;

import boardinghouse.dao.PaymentDAO;
import boardinghouse.dao.SettingsDAO;
import boardinghouse.dao.SettingsDAO.GcashInfo;
import boardinghouse.model.Payment;
import boardinghouse.service.PaymentService;
import boardinghouse.util.Validator;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PaymentsPanel extends Section {
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final PaymentService payments = new PaymentService();
    private final SettingsDAO settings = new SettingsDAO();
    private final Runnable onChange;
    private final JLabel gcashStatus = new JLabel();
    private final JTabbedPane tabs = Ui.tabs();
    private final Tab pendingTab;
    private final Tab approvedTab;
    private final Tab rejectedTab;

    public PaymentsPanel(Runnable onChange) {
        this.onChange = onChange;
        pendingTab = new Tab("PENDING");
        approvedTab = new Tab("APPROVED");
        rejectedTab = new Tab("REJECTED");

        JButton gcashBtn = Theme.secondary("GCash Settings");
        gcashBtn.addActionListener(e -> gcashSettings());
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(gcashStatus, BorderLayout.WEST);
        header.add(gcashBtn, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        tabs.addTab("Pending", pendingTab.panel);
        tabs.addTab("Approved", approvedTab.panel);
        tabs.addTab("Rejected", rejectedTab.panel);
        add(tabs, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        List<Payment> pending = paymentDAO.findPending();
        pendingTab.load(pending);
        approvedTab.load(paymentDAO.findByStatus("APPROVED"));
        rejectedTab.load(paymentDAO.findByStatus("REJECTED"));
        tabs.setTitleAt(0, "Pending (" + pending.size() + ")");
        updateGcashStatus();
    }

    // ---------- One tab (pending, approved, or rejected) ----------
    private class Tab {
        final JTable table = Theme.table("Tenant", "Bill", "Month", "Amount (\u20B1)", "Method", "Ref no.",
                "Date", "Status", "Actions");
        final JPanel panel = new JPanel(new BorderLayout());
        List<Payment> shown = new ArrayList<>();

        Tab(String status) {
            JPanel toolbar = Ui.toolbar(table, Export.csvButton(table, "payments_" + status.toLowerCase()));
            Ui.statusPills(table, 7);

            List<RowActions.RowAction> actions = new ArrayList<>();
            if (status.equals("PENDING")) {
                actions.add(RowActions.text("Approve", Theme.ACCENT, "Approve this payment",
                        row -> review(List.of(shown.get(row)), true)));
                actions.add(RowActions.text("Reject", Theme.DANGER, "Reject this payment",
                        row -> review(List.of(shown.get(row)), false)));
            }
            if (status.equals("APPROVED")) {
                actions.add(RowActions.icon("billing", Theme.SUCCESS, "View receipt",
                        row -> ReceiptDialog.show(PaymentsPanel.this, shown.get(row))));
            }
            actions.add(RowActions.when(
                    RowActions.icon("eye", Theme.ACCENT, "View proof of payment", row -> viewProof(shown.get(row))),
                    row -> row < shown.size() && shown.get(row).hasProof()));
            RowActions.install(table, 8, actions.toArray(new RowActions.RowAction[0]));

            JPanel footer;
            if (status.equals("PENDING")) {
                JButton approveSel = Theme.secondary("Approve Selected");
                approveSel.addActionListener(e -> review(selected(), true));
                JButton rejectSel = Theme.secondary("Reject Selected");
                rejectSel.addActionListener(e -> review(selected(), false));
                footer = Ui.footer(table, approveSel, rejectSel);
            } else {
                footer = Ui.footer(table);
            }

            JPanel content = new JPanel(new BorderLayout(0, 12));
            content.setOpaque(false);
            content.add(toolbar, BorderLayout.NORTH);
            content.add(Theme.scroll(table), BorderLayout.CENTER);
            content.add(footer, BorderLayout.SOUTH);

            panel.setOpaque(false);
            panel.setBorder(new EmptyBorder(16, 0, 0, 0));
            panel.add(Ui.card(content), BorderLayout.CENTER);
        }

        void load(List<Payment> list) {
            shown = list;
            List<Object[]> rows = new ArrayList<>();
            for (Payment p : list) {
                rows.add(new Object[]{p.getTenantName(), p.getBillType(), p.getBillingMonth(),
                        Theme.money(p.getAmount()), p.getMethod(), p.getRefNo() == null ? "-" : p.getRefNo(),
                        Ui.localDate(p.getPaidAt()), p.getStatus(), ""});
            }
            Theme.fill(table, rows);
        }

        List<Payment> selected() {
            List<Payment> list = new ArrayList<>();
            for (int i : Theme.selectedModelRows(table)) list.add(shown.get(i));
            return list;
        }
    }

    // ---------- Approve / reject (one or many) ----------
    private void review(List<Payment> sel, boolean approve) {
        if (sel.isEmpty()) { msg("Select at least one payment first."); return; }

        String verb = approve ? "approve" : "reject";
        if (sel.size() > 1 && !Theme.confirm(this, "Do you want to " + verb + " " + sel.size() + " payments?",
                approve ? "Approve Payments" : "Reject Payments")) return;

        int done = 0;
        List<String> failed = new ArrayList<>();
        String lastResult = "";
        for (Payment p : sel) {
            String result = approve ? payments.approve(p.getPaymentId()) : payments.reject(p.getPaymentId());
            lastResult = result;
            if (result.startsWith("Payment approved") || result.startsWith("Payment rejected")) done++;
            else failed.add("#" + p.getPaymentId() + ": " + result);
        }

        refresh();
        onChange.run();

        if (sel.size() == 1) {
            if (approve && lastResult.startsWith("Payment approved")) {
                int choice = JOptionPane.showConfirmDialog(this, lastResult + "\n\nOpen the receipt now?",
                        "Payment Approved", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                if (choice == JOptionPane.YES_OPTION) {
                    ReceiptDialog.show(this, paymentDAO.findById(sel.get(0).getPaymentId()));
                }
            } else {
                msg(lastResult);
            }
        } else {
            String summary = done + " payment(s) " + (approve ? "approved" : "rejected") + ".";
            if (!failed.isEmpty()) summary += "\nCould not process:\n" + String.join("\n", failed);
            msg(summary);
        }
    }

    // ---------- Proof image ----------
    private void viewProof(Payment p) {
        if (!p.hasProof()) { msg("No proof image was uploaded for this payment."); return; }
        try {
            BufferedImage img = ImageIO.read(new File(p.getProofPath()));
            if (img == null) { msg("The proof file is not a readable image."); return; }

            double scale = Math.min(1.0, Math.min(700.0 / img.getWidth(), 560.0 / img.getHeight()));
            Image scaled = img.getScaledInstance((int) (img.getWidth() * scale),
                    (int) (img.getHeight() * scale), Image.SCALE_SMOOTH);
            JOptionPane.showMessageDialog(this, new JLabel(new ImageIcon(scaled)),
                    "Proof of payment (Ref no. " + p.getRefNo() + ")", JOptionPane.PLAIN_MESSAGE);
        } catch (IOException e) {
            msg("Could not open the proof image. The file may have been moved or deleted.");
        }
    }

    // ---------- GCash settings ----------
    private void updateGcashStatus() {
        GcashInfo g = settings.getGcash();
        if (g == null) {
            gcashStatus.setText("GCash not set. Pay Online is off.");
            gcashStatus.setForeground(Theme.DANGER);
        } else {
            gcashStatus.setText("GCash: " + g.name() + "  |  " + g.phone());
            gcashStatus.setForeground(Theme.SUCCESS);
        }
    }

    private void gcashSettings() {
        GcashInfo current = settings.getGcash();
        JTextField user = Theme.field("GCash account name");
        JTextField phone = Theme.field("09XXXXXXXXX");
        if (current != null) {
            user.setText(current.name());
            phone.setText(current.phone());
        }
        JLabel note = Theme.muted("<html><body style='width:300px'>Tenants who choose Pay Online will see these details. "
                + "If you clear them, Pay Online is turned off for tenants.</body></html>");
        Object[] options = current == null
                ? new Object[]{"Save", "Cancel"}
                : new Object[]{"Save", "Clear GCash Info", "Cancel"};

        while (true) {
            int choice = JOptionPane.showOptionDialog(this,
                    Theme.form(note, Theme.labeled("GCash username", user), Theme.labeled("GCash phone no.", phone)),
                    "GCash Settings", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

            if (choice == 0) {
                String name = user.getText().trim();
                String number = phone.getText().trim();
                if (!Validator.isNotBlank(name)) { msg("Enter the GCash username."); continue; }
                if (!Validator.isValidPhone(number)) { msg("The GCash phone no. must be 11 digits starting with 09."); continue; }
                settings.saveGcash(name, number);
                msg("GCash information saved. Tenants can now pay online.");
            } else if (current != null && choice == 1) {
                if (Theme.confirmWarn(this, "Clear the GCash information?\nTenants will not be able to pay online "
                        + "until you add it again.", "Clear GCash Info")) {
                    settings.clearGcash();
                    msg("GCash information cleared. Pay Online is now off.");
                }
            }
            break;
        }
        updateGcashStatus();
    }
}