package boardinghouse.gui;

import boardinghouse.dao.*;
import boardinghouse.model.*;
import boardinghouse.service.PaymentService;
import boardinghouse.util.PasswordUtil;
import boardinghouse.util.Validator;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TenantPanel extends JPanel {
    private final MainFrame frame;
    private final Account account;
    private final Tenant tenant;
    private final TenantDAO tenantDAO = new TenantDAO();
    private final AccountDAO accountDAO = new AccountDAO();
    private final RoomDAO roomDAO = new RoomDAO();
    private final BillDAO billDAO = new BillDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final PaymentService payments = new PaymentService();

    private final CardLayout views = new CardLayout();
    private final JPanel content = new JPanel(views);
    private final JLabel pageTitle = Theme.pageTitle("My Dashboard");
    private final Ui.Bell bell = new Ui.Bell(this::showNotifications);
    private final Sidebar sidebar;

    private final StatCard debtCard = new StatCard("Total Debt", "billing", Theme.ACCENT);
    private final StatCard paidCard = new StatCard("Paid This Month", "payments", Theme.SUCCESS);
    private final StatCard dueCard = new StatCard("Next Due", "history", Theme.DANGER);

    private final JTable billsTable = Theme.table("Type", "Month", "Amount (\u20B1)", "Late fee (\u20B1)",
            "Balance (\u20B1)", "Due Date", "Status");
    private final JTable historyTable = Theme.table("Bill", "Month", "Amount (\u20B1)", "Method", "Ref no.",
            "Proof", "Status", "Date");
    private final JPanel profileGrid = new JPanel(new GridLayout(0, 2, 12, 16));

    private List<Bill> shownBills = new ArrayList<>();
    private int overdueCount;
    private int pendingCount;

    public TenantPanel(MainFrame frame, Account account) {
        this.frame = frame;
        this.account = account;
        this.tenant = tenantDAO.findByAccountId(account.getAccountId());
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        Theme.singleSelect(billsTable); // one bill is paid at a time
        Ui.statusPills(billsTable, 6);
        Ui.statusPills(historyTable, 6);

        content.setOpaque(false);
        content.add(buildDashboard(), "dashboard");
        content.add(buildHistory(), "history");
        content.add(buildProfile(), "profile");

        sidebar = new Sidebar(List.of(
                new Sidebar.Item("dashboard", "Dashboard", "dashboard"),
                new Sidebar.Item("history", "Payment History", "history"),
                new Sidebar.Item("profile", "My Profile", "tenants")),
                tenant == null ? account.getEmail() : tenant.getName(), "Tenant",
                this::open, this::confirmLogout);

        JPanel main = new JPanel(new BorderLayout(0, 16));
        main.setOpaque(false);
        main.setBorder(new EmptyBorder(24, 28, 24, 28));
        main.add(Ui.topBar(pageTitle, bell), BorderLayout.NORTH);
        main.add(content, BorderLayout.CENTER);

        add(sidebar, BorderLayout.WEST);
        add(main, BorderLayout.CENTER);
        open("dashboard");
    }

    // ---------- Navigation ----------
    private void open(String view) {
        refreshAll();
        views.show(content, view);
        sidebar.select(view);
        switch (view) {
            case "history" -> pageTitle.setText("Payment History");
            case "profile" -> pageTitle.setText("My Profile");
            default -> pageTitle.setText("My Dashboard");
        }
    }

    private void confirmLogout() {
        if (Theme.confirm(this, "Are you sure you want to log out?", "Logout")) frame.showLogin();
    }

    private void showNotifications() {
        JOptionPane.showMessageDialog(this,
                overdueCount + " overdue bill(s)\n" + pendingCount + " payment(s) waiting for the owner's approval",
                "Notifications", JOptionPane.INFORMATION_MESSAGE);
    }

    // ---------- Screens ----------
    private JPanel buildDashboard() {
        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setOpaque(false);

        JPanel cards = new JPanel(new GridLayout(1, 3, 16, 0));
        cards.setOpaque(false);
        cards.add(debtCard);
        cards.add(paidCard);
        cards.add(dueCard);
        p.add(cards, BorderLayout.NORTH);

        // the only filled button on this screen
        JButton pay = Theme.primary("Pay Now");
        pay.addActionListener(e -> payNow());

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(Theme.searchBar(billsTable), BorderLayout.NORTH);
        body.add(Theme.scroll(billsTable), BorderLayout.CENTER);
        body.add(Theme.buttonRow(pay), BorderLayout.SOUTH);
        p.add(Ui.panelCard(Theme.title("My Bills"), body), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildHistory() {
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(Theme.searchBar(historyTable), BorderLayout.NORTH);
        body.add(Theme.scroll(historyTable), BorderLayout.CENTER);

        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(Ui.panelCard(Theme.title("Payment History"), body), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildProfile() {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 20));
        profileGrid.setOpaque(false);
        card.add(profileGrid, BorderLayout.CENTER);

        JButton change = Theme.secondary("Change Password");
        change.addActionListener(e -> changePassword());
        JPanel btn = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btn.setOpaque(false);
        btn.add(change);
        card.add(btn, BorderLayout.SOUTH);

        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrap.setOpaque(false);
        wrap.add(card);

        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(wrap, BorderLayout.CENTER);
        return p;
    }

    // ---------- Data ----------
    private void refreshAll() {
        LocalDate today = LocalDate.now();

        shownBills = billDAO.findByTenant(tenant.getTenantId());
        double debt = 0;
        LocalDate nextDue = null;
        overdueCount = 0;
        List<Object[]> rows = new ArrayList<>();
        for (Bill b : shownBills) {
            double balance = payments.balanceOf(b);
            debt += balance;
            boolean unpaid = !"PAID".equals(b.getStatus());
            boolean overdue = unpaid && today.isAfter(b.getDueDate());
            if (overdue) overdueCount++;
            if (unpaid && balance > 0 && (nextDue == null || b.getDueDate().isBefore(nextDue))) {
                nextDue = b.getDueDate();
            }
            rows.add(new Object[]{b.getType(), b.getBillingMonth(), Theme.money(b.getAmount()),
                    Theme.money(b.getLateFee(today)), Theme.money(balance), b.getDueDate().toString(),
                    overdue ? "OVERDUE" : b.getStatus()});
        }
        Theme.fill(billsTable, rows);

        double paidThisMonth = 0;
        pendingCount = 0;
        String month = YearMonth.now().toString();
        List<Object[]> history = new ArrayList<>();
        for (Payment p : paymentDAO.findByTenant(tenant.getTenantId())) {
            if ("APPROVED".equals(p.getStatus()) && p.getPaidAt() != null && p.getPaidAt().startsWith(month)) {
                paidThisMonth += p.getAmount();
            }
            if ("PENDING".equals(p.getStatus())) pendingCount++;
            history.add(new Object[]{p.getBillType(), p.getBillingMonth(), Theme.money(p.getAmount()), p.getMethod(),
                    p.getRefNo() == null ? "-" : p.getRefNo(), p.hasProof() ? "Yes" : "-", p.getStatus(), p.getPaidAt()});
        }
        Theme.fill(historyTable, history);

        // summary cards
        debtCard.setValue(Ui.peso(debt));
        if (overdueCount > 0) debtCard.setNote(overdueCount + " overdue bill(s)", Theme.DANGER);
        else if (debt > 0) debtCard.setNote("Remaining balance", Theme.MUTED);
        else debtCard.setNote("All paid", Theme.SUCCESS);

        paidCard.setValue(Ui.peso(paidThisMonth));
        paidCard.setNote(pendingCount > 0 ? pendingCount + " waiting for approval" : "Approved payments", Theme.MUTED);

        if (nextDue == null) {
            dueCard.setValue("None");
            dueCard.setNote("No unpaid bills", Theme.SUCCESS);
        } else {
            dueCard.setValue(nextDue.format(DateTimeFormatter.ofPattern("MMM d, yyyy")));
            if (nextDue.isBefore(today)) dueCard.setNote("Overdue", Theme.DANGER);
            else dueCard.setNote("Upcoming", Theme.MUTED);
        }

        bell.setCount(overdueCount);
        bell.setToolTipText("Notifications");
        loadProfile();
    }

    private void loadProfile() {
        Tenant t = tenantDAO.findByAccountId(account.getAccountId());
        Room room = t.getRoomId() == null ? null : roomDAO.findById(t.getRoomId());
        profileGrid.removeAll();
        profileRow("Name", t.getName());
        profileRow("Email", account.getEmail());
        profileRow("Phone", account.getPhone());
        profileRow("Address", t.getAddress());
        profileRow("Age", String.valueOf(t.getAge()));
        profileRow("Occupation", t.getOccupation());
        profileRow("Emergency no.", t.getEmergencyNo());
        profileRow("Room", room == null ? "Not assigned yet" : room.getRoomNo());
        profileGrid.revalidate();
        profileGrid.repaint();
    }

    // values can be selected and copied
    private void profileRow(String label, String value) {
        profileGrid.add(Theme.muted(label));
        profileGrid.add(Theme.selectable(value));
    }

    private void changePassword() {
        JPasswordField current = Theme.password("Current password");
        JPasswordField next = Theme.password("New password (6+ characters)");
        JPasswordField confirm = Theme.password("Repeat the new password");
        int ok = JOptionPane.showConfirmDialog(this,
                Theme.form(Theme.labeled("Current password", current), Theme.labeled("New password", next),
                        Theme.labeled("Confirm new password", confirm)),
                "Change Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;

        Account fresh = accountDAO.findById(account.getAccountId());
        if (fresh == null || !PasswordUtil.verify(new String(current.getPassword()), fresh.getPasswordHash())) {
            JOptionPane.showMessageDialog(this, "Your current password is incorrect.");
            return;
        }
        String newPassword = new String(next.getPassword());
        if (!Validator.isValidPassword(newPassword)) {
            JOptionPane.showMessageDialog(this, "The new password must be at least 6 characters.");
            return;
        }
        if (!newPassword.equals(new String(confirm.getPassword()))) {
            JOptionPane.showMessageDialog(this, "The new passwords do not match.");
            return;
        }
        accountDAO.updatePassword(account.getAccountId(), PasswordUtil.hash(newPassword));
        JOptionPane.showMessageDialog(this, "Password changed.");
    }

    // ---------- Payment ----------
    private void payNow() {
        int[] rows = Theme.selectedModelRows(billsTable);
        if (rows.length != 1) {
            JOptionPane.showMessageDialog(this, "Select one bill first.");
            return;
        }
        Bill bill = shownBills.get(rows[0]);
        if ("PAID".equals(bill.getStatus())) {
            JOptionPane.showMessageDialog(this, "That bill is already fully paid.");
            return;
        }
        PayDialog dialog = new PayDialog(SwingUtilities.getWindowAncestor(this), tenant, bill);
        dialog.setVisible(true);
        if (dialog.wasSubmitted()) refreshAll();
    }
}