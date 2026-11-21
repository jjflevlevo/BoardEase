package boardinghouse.gui;

import boardinghouse.dao.*;
import boardinghouse.model.*;
import boardinghouse.service.PaymentService;
import boardinghouse.util.PasswordUtil;
import boardinghouse.util.Validator;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TenantPanel extends JPanel {
    private static final int NEWS_DAYS = 14;        // announcements newer than this show on the dashboard
    private static final int NEWS_MAX_CHARS = 220;  // longer messages are cut with "..." (View all shows everything)

    private final MainFrame frame;
    private final Account account;
    private final Tenant tenant;
    private final TenantDAO tenantDAO = new TenantDAO();
    private final AccountDAO accountDAO = new AccountDAO();
    private final RoomDAO roomDAO = new RoomDAO();
    private final BillDAO billDAO = new BillDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final MaintenanceDAO maintenanceDAO = new MaintenanceDAO();
    private final AnnouncementDAO announcementDAO = new AnnouncementDAO();
    private final PaymentService payments = new PaymentService();

    private final CardLayout views = new CardLayout();
    private final JPanel content = new JPanel(views);
    private final JLabel pageTitle = Theme.pageTitle("My Dashboard");
    private final Ui.Bell bell = new Ui.Bell(this::showNotifications);
    private final Sidebar sidebar;
    private final TenantRequestsPanel requestsPanel;
    private final AnnouncementFeed feed = new AnnouncementFeed();

    private final StatCard debtCard = new StatCard("Total Debt", "billing", Theme.ACCENT);
    private final StatCard paidCard = new StatCard("Paid This Month", "payments", Theme.SUCCESS);
    private final StatCard dueCard = new StatCard("Next Due", "history", Theme.DANGER);

    // latest announcement card
    private final RoundedPanel newsBanner = new RoundedPanel(new Color(0x16294A), Theme.ACCENT, 12, 16);
    private final NewsIcon newsIcon = new NewsIcon();
    private final JLabel newsTitle = new JLabel();
    private final JLabel newsDate = Theme.muted(" ");
    private final JTextArea newsMessage = new JTextArea();

    private final JTable billsTable = Theme.table("Type", "Month", "Amount (\u20B1)", "Late fee (\u20B1)",
            "Balance (\u20B1)", "Due Date", "Status");
    private final JTable historyTable = Theme.table("Bill", "Month", "Amount (\u20B1)", "Method", "Ref no.",
            "Proof", "Status", "Date", "Actions");
    private final JPanel profileGrid = new JPanel(new GridLayout(0, 2, 12, 16));

    private List<Bill> shownBills = new ArrayList<>();
    private List<Payment> shownPayments = new ArrayList<>();
    private int overdueCount;
    private int pendingCount;

    public TenantPanel(MainFrame frame, Account account) {
        this.frame = frame;
        this.account = account;
        this.tenant = tenantDAO.findByAccountId(account.getAccountId());
        this.requestsPanel = new TenantRequestsPanel(tenant.getTenantId());
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        Theme.singleSelect(billsTable); // one bill is paid at a time
        Ui.statusPills(billsTable, 6);
        Ui.statusPills(historyTable, 6);

        content.setOpaque(false);
        content.add(buildDashboard(), "dashboard");
        content.add(buildHistory(), "history");
        content.add(requestsPanel, "maintenance");
        content.add(feed, "announcements");
        content.add(buildProfile(), "profile");

        sidebar = new Sidebar(List.of(
                new Sidebar.Item("dashboard", "Dashboard", "dashboard"),
                new Sidebar.Item("history", "Payment History", "history"),
                new Sidebar.Item("maintenance", "Maintenance", "maintenance"),
                new Sidebar.Item("announcements", "Announcements", "announcements"),
                new Sidebar.Item("profile", "My Profile", "tenants")),
                tenant.getName(), "Tenant", this::open, this::confirmLogout);

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
        if (view.equals("maintenance")) requestsPanel.refresh();
        if (view.equals("announcements")) feed.refresh();
        views.show(content, view);
        sidebar.select(view);
        switch (view) {
            case "history" -> pageTitle.setText("Payment History");
            case "maintenance" -> pageTitle.setText("Maintenance");
            case "announcements" -> pageTitle.setText("Announcements");
            case "profile" -> pageTitle.setText("My Profile");
            default -> pageTitle.setText("My Dashboard");
        }
    }

    private void confirmLogout() {
        if (Theme.confirm(this, "Are you sure you want to log out?", "Logout")) frame.showLogin();
    }

    private void showNotifications() {
        int unfinished = 0;
        for (MaintenanceRequest r : maintenanceDAO.findByTenant(tenant.getTenantId())) {
            if (!r.isDone()) unfinished++;
        }
        Announcement latest = announcementDAO.latest();
        String text = overdueCount + " overdue bill(s)\n"
                + pendingCount + " payment(s) waiting for the owner's approval\n"
                + unfinished + " maintenance request(s) not finished yet";
        if (latest != null) text += "\n\nLatest announcement: " + latest.getTitle();
        JOptionPane.showMessageDialog(this, text, "Notifications", JOptionPane.INFORMATION_MESSAGE);
    }

    // ---------- Screens ----------
    private JPanel buildDashboard() {
        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setOpaque(false);

        buildNewsBanner();

        JPanel cards = new JPanel(new GridLayout(1, 3, 16, 0));
        cards.setOpaque(false);
        cards.add(debtCard);
        cards.add(paidCard);
        cards.add(dueCard);

        JPanel north = new JPanel(new BorderLayout(0, 16));
        north.setOpaque(false);
        north.add(newsBanner, BorderLayout.NORTH);
        north.add(cards, BorderLayout.CENTER);
        p.add(north, BorderLayout.NORTH);

        // the only filled button on this screen
        JButton pay = Theme.primary("Pay Now");
        pay.addActionListener(e -> payNow());

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(Ui.toolbar(billsTable, null), BorderLayout.NORTH);
        billsTable.getTableHeader().setToolTipText("Click a column header to sort.");
        body.add(Theme.scroll(billsTable), BorderLayout.CENTER);
        body.add(Theme.buttonRow(pay), BorderLayout.SOUTH);
        p.add(Ui.panelCard(Theme.title("My Bills"), body), BorderLayout.CENTER);
        return p;
    }

    // the latest announcement: icon, title, date, the full message, and View all
    private void buildNewsBanner() {
        newsBanner.setLayout(new BorderLayout(14, 0));

        newsTitle.setFont(Theme.font(12, Font.BOLD));

        newsMessage.setEditable(false);
        newsMessage.setFocusable(false);
        newsMessage.setLineWrap(true);
        newsMessage.setWrapStyleWord(true);
        newsMessage.setOpaque(false);
        newsMessage.setBorder(null);
        newsMessage.setFont(Theme.font(10.5f, Font.PLAIN));
        newsMessage.setForeground(Theme.TEXT);

        // the message wraps to the banner's width, so its height is recalculated when the width changes
        newsMessage.addComponentListener(new ComponentAdapter() {
            private int lastWidth = -1;

            @Override
            public void componentResized(ComponentEvent e) {
                int w = newsMessage.getWidth();
                if (w != lastWidth) {
                    lastWidth = w;
                    SwingUtilities.invokeLater(newsBanner::revalidate);
                }
            }
        });

        JButton viewAll = Theme.link("View all");
        viewAll.addActionListener(e -> open("announcements"));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        right.add(newsDate);
        right.add(viewAll);

        JPanel head = new JPanel(new BorderLayout(12, 0));
        head.setOpaque(false);
        head.add(newsTitle, BorderLayout.CENTER);
        head.add(right, BorderLayout.EAST);

        JPanel text = new JPanel(new BorderLayout(0, 6));
        text.setOpaque(false);
        text.add(head, BorderLayout.NORTH);
        text.add(newsMessage, BorderLayout.CENTER);

        JPanel iconWrap = new JPanel(new BorderLayout());
        iconWrap.setOpaque(false);
        iconWrap.add(newsIcon, BorderLayout.NORTH);

        newsBanner.add(iconWrap, BorderLayout.WEST);
        newsBanner.add(text, BorderLayout.CENTER);
    }

    private JPanel buildHistory() {
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(Ui.toolbar(historyTable, null), BorderLayout.NORTH);

        // receipt button on approved payments
        RowActions.install(historyTable, 8, RowActions.when(
                RowActions.icon("billing", Theme.SUCCESS, "View receipt",
                        row -> ReceiptDialog.show(this, shownPayments.get(row))),
                row -> row < shownPayments.size() && "APPROVED".equals(shownPayments.get(row).getStatus())));

        body.add(Theme.scroll(historyTable), BorderLayout.CENTER);
        body.add(Ui.footer(historyTable), BorderLayout.SOUTH);

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

        JButton edit = Theme.primary("Edit Profile");
        edit.addActionListener(e -> editProfile());
        JButton change = Theme.secondary("Change Password");
        change.addActionListener(e -> changePassword());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(edit);
        buttons.add(change);
        card.add(buttons, BorderLayout.SOUTH);

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
            boolean unpaid = !b.isPaid();
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
        shownPayments = paymentDAO.findByTenant(tenant.getTenantId());
        List<Object[]> history = new ArrayList<>();
        for (Payment p : shownPayments) {
            String date = Ui.localDate(p.getPaidAt());
            if (p.isApproved() && date.startsWith(month)) paidThisMonth += p.getAmount();
            if (p.isPending()) pendingCount++;
            history.add(new Object[]{p.getBillType(), p.getBillingMonth(), Theme.money(p.getAmount()), p.getMethod(),
                    p.getRefNo() == null ? "-" : p.getRefNo(), p.hasProof() ? "Yes" : "-", p.getStatus(), date, ""});
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

        updateNewsBanner();
        bell.setCount(overdueCount);
        bell.setToolTipText("Notifications");
        loadProfile();
    }

    private void updateNewsBanner() {
        Announcement latest = announcementDAO.latest();
        if (latest == null || !isRecent(latest.getCreatedAt())) {
            newsBanner.setVisible(false);
            return;
        }

        boolean important = latest.isImportant();
        Color tint = important ? Theme.DANGER : Theme.ACCENT;
        newsBanner.setFill(important ? new Color(0x2A1820) : new Color(0x16294A));
        newsBanner.setLine(tint);
        newsIcon.setTint(tint);

        newsTitle.setText((important ? "IMPORTANT: " : "") + latest.getTitle());
        newsTitle.setForeground(important ? Theme.DANGER : Theme.TEXT);
        newsDate.setText("Posted " + Ui.localDate(latest.getCreatedAt()));

        String message = latest.getMessage().trim();
        if (message.length() > NEWS_MAX_CHARS) message = message.substring(0, NEWS_MAX_CHARS - 3).trim() + "...";
        newsMessage.setText(message);

        newsBanner.setVisible(true);
        newsBanner.revalidate();
        newsBanner.repaint();
    }

    private static boolean isRecent(String utc) {
        try {
            LocalDateTime t = LocalDateTime.parse(utc.trim().replace(' ', 'T'));
            return t.isAfter(LocalDateTime.now(ZoneOffset.UTC).minusDays(NEWS_DAYS));
        } catch (Exception e) {
            return false;
        }
    }

    private void loadProfile() {
        Tenant t = tenantDAO.findByAccountId(account.getAccountId());
        Account a = accountDAO.findById(account.getAccountId());
        if (t == null) return;
        if (a == null) a = account;
        Room room = t.getRoomId() == null ? null : roomDAO.findById(t.getRoomId());
        profileGrid.removeAll();
        profileRow("Name", t.getName());
        profileRow("Email", a.getEmail());
        profileRow("Phone", a.getPhone());
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

    private void editProfile() {
        Tenant t = tenantDAO.findByAccountId(account.getAccountId());
        Account a = accountDAO.findById(account.getAccountId());
        if (t == null || a == null) return;
        if (ProfileDialog.edit(this, t, a, false)) loadProfile();
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
        if (bill.isPaid()) {
            JOptionPane.showMessageDialog(this, "That bill is already fully paid.");
            return;
        }
        PayDialog dialog = new PayDialog(SwingUtilities.getWindowAncestor(this), tenant, bill);
        dialog.setVisible(true);
        if (dialog.wasSubmitted()) refreshAll();
    }

    // ---------- Megaphone icon tile for the announcement card ----------
    private static class NewsIcon extends JComponent {
        private Color tint = Theme.ACCENT;

        NewsIcon() {
            setPreferredSize(new Dimension(40, 40));
        }

        void setTint(Color c) {
            tint = c;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), 45));
            g2.fillRoundRect(0, 0, 40, 40, 12, 12);
            Icons.draw(g2, "announcements", 10, 10, 20, tint);
            g2.dispose();
        }
    }
}