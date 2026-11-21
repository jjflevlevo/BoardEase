package boardinghouse.gui;

import boardinghouse.dao.MaintenanceDAO;
import boardinghouse.dao.PaymentDAO;
import boardinghouse.model.Account;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OwnerPanel extends JPanel {
    private final MainFrame frame;
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final MaintenanceDAO maintenanceDAO = new MaintenanceDAO();
    private final CardLayout views = new CardLayout();
    private final JPanel content = new JPanel(views);
    private final Map<String, Section> sections = new LinkedHashMap<>();
    private final Map<String, String> titles = new LinkedHashMap<>();
    private final JLabel pageTitle = Theme.pageTitle("Dashboard");
    private final Ui.Bell bell = new Ui.Bell(this::showBellMenu);
    private final Sidebar sidebar;

    public OwnerPanel(MainFrame frame, Account account) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        sections.put("dashboard", new DashboardPanel());
        sections.put("rooms", new RoomsPanel());
        sections.put("tenants", new TenantsPanel(this::updateBadges));
        sections.put("billing", new BillingPanel());
        sections.put("payments", new PaymentsPanel(this::updateBadges));
        sections.put("maintenance", new MaintenancePanel(this::updateBadges));
        sections.put("announcements", new AnnouncementsPanel());
        sections.put("reports", new ReportsPanel());
        sections.put("settings", new SettingsPanel());

        titles.put("dashboard", "Dashboard");
        titles.put("rooms", "Rooms");
        titles.put("tenants", "Tenants");
        titles.put("billing", "Billing");
        titles.put("payments", "Payments");
        titles.put("maintenance", "Maintenance Requests");
        titles.put("announcements", "Announcements");
        titles.put("reports", "Reports");
        titles.put("settings", "Settings");

        content.setOpaque(false);
        for (Map.Entry<String, Section> e : sections.entrySet()) content.add(e.getValue(), e.getKey());

        sidebar = new Sidebar(List.of(
                new Sidebar.Item("dashboard", "Dashboard", "dashboard"),
                new Sidebar.Item("rooms", "Rooms", "rooms"),
                new Sidebar.Item("tenants", "Tenants", "tenants"),
                new Sidebar.Item("billing", "Billing", "billing"),
                new Sidebar.Item("payments", "Payments", "payments"),
                new Sidebar.Item("maintenance", "Maintenance", "maintenance"),
                new Sidebar.Item("announcements", "Announcements", "announcements"),
                new Sidebar.Item("reports", "Reports", "reports"),
                new Sidebar.Item("settings", "Settings", "settings")),
                "Owner", account.getEmail(), this::open, this::confirmLogout);

        JPanel top = Ui.topBar(pageTitle, bell);
        top.setBorder(new EmptyBorder(24, 24, 0, 24));

        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.add(top, BorderLayout.NORTH);
        main.add(content, BorderLayout.CENTER);

        add(sidebar, BorderLayout.WEST);
        add(main, BorderLayout.CENTER);
        open(paymentDAO.findPending().isEmpty() ? "dashboard" : "payments");
    }

    private void confirmLogout() {
        if (Theme.confirm(this, "Are you sure you want to log out?", "Logout")) frame.showLogin();
    }

    private void open(String key) {
        sections.get(key).refresh();
        pageTitle.setText(titles.get(key));
        sidebar.select(key);
        updateBadges();
        views.show(content, key);
    }

    // pending payments and open requests show on the sidebar and on the bell
    private void updateBadges() {
        int pending = paymentDAO.findPending().size();
        int open = maintenanceDAO.countByStatus("OPEN");
        sidebar.setBadge("payments", pending, Theme.WARN);
        sidebar.setBadge("maintenance", open, Theme.DANGER);
        bell.setCount(pending + open);
        bell.setToolTipText(pending + open == 0 ? "Nothing needs your attention" : "Click to see what needs your attention");
    }

    private void showBellMenu() {
        int pending = paymentDAO.findPending().size();
        int open = maintenanceDAO.countByStatus("OPEN");

        JPopupMenu menu = new JPopupMenu();
        JMenuItem payments = new JMenuItem(pending + " pending payment(s)");
        payments.addActionListener(e -> open("payments"));
        JMenuItem requests = new JMenuItem(open + " open maintenance request(s)");
        requests.addActionListener(e -> open("maintenance"));
        menu.add(payments);
        menu.add(requests);
        menu.show(bell, bell.getWidth() - menu.getPreferredSize().width, bell.getHeight());
    }
}