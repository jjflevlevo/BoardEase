package boardinghouse.gui;

import boardinghouse.dao.*;
import boardinghouse.model.*;
import boardinghouse.service.PaymentService;
import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

// owner home screen: summary cards, monthly income chart, and recent activity
public class DashboardPanel extends Section {
    private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    private final TenantDAO tenantDAO = new TenantDAO();
    private final RoomDAO roomDAO = new RoomDAO();
    private final BillDAO billDAO = new BillDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final ActivityDAO activityDAO = new ActivityDAO();
    private final PaymentService payments = new PaymentService();

    private final StatCard tenantsCard = new StatCard("Total Tenants", "tenants", Theme.ACCENT);
    private final StatCard roomsCard = new StatCard("Occupied Rooms", "rooms", Theme.SUCCESS);
    private final StatCard pendingCard = new StatCard("Pending Payments", "payments", Theme.WARN);
    private final StatCard unpaidCard = new StatCard("Total Unpaid", "billing", Theme.DANGER);

    private final BarChart chart = new BarChart();
    private final JComboBox<String> yearBox = new JComboBox<>();
    private final JPanel activityList = new JPanel();
    private Map<String, Double> income = new LinkedHashMap<>();
    private boolean loading;

    public DashboardPanel() {
        JPanel cards = new JPanel(new GridLayout(1, 4, 16, 0));
        cards.setOpaque(false);
        cards.add(tenantsCard);
        cards.add(roomsCard);
        cards.add(pendingCard);
        cards.add(unpaidCard);
        add(cards, BorderLayout.NORTH);

        // Monthly Income card
        yearBox.addActionListener(e -> {
            if (!loading) drawChart();
        });
        JPanel chartHeader = new JPanel(new BorderLayout());
        chartHeader.setOpaque(false);
        chartHeader.add(Theme.title("Monthly Income"), BorderLayout.WEST);
        chartHeader.add(yearBox, BorderLayout.EAST);
        JPanel chartCard = Ui.panelCard(chartHeader, chart);

        // Recent Activity card
        activityList.setOpaque(false);
        activityList.setLayout(new BoxLayout(activityList, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(activityList);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        JPanel activityCard = Ui.panelCard(Theme.title("Recent Activity"), scroll);

        JPanel lower = new JPanel(new GridBagLayout());
        lower.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.weightx = 0.62;
        c.insets = new Insets(0, 0, 0, 16);
        lower.add(chartCard, c);
        c.weightx = 0.38;
        c.insets = new Insets(0, 0, 0, 0);
        lower.add(activityCard, c);
        add(lower, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        List<Tenant> tenants = tenantDAO.findAll();
        List<Room> rooms = roomDAO.findAll();
        int occupied = 0;
        for (Room r : rooms) {
            if (tenantDAO.countInRoom(r.getRoomId()) > 0) occupied++;
        }

        List<Payment> pending = paymentDAO.findPending();
        double pendingAmount = 0;
        for (Payment p : pending) pendingAmount += p.getAmount();

        double unpaid = 0;
        for (Bill b : billDAO.findAll()) unpaid += payments.balanceOf(b);

        tenantsCard.setValue(String.valueOf(tenants.size()));
        tenantsCard.setNote("Registered tenants", Theme.MUTED);

        roomsCard.setValue(occupied + " / " + rooms.size());
        roomsCard.setNote(rooms.isEmpty() ? "No rooms yet" : (rooms.size() - occupied) + " vacant", Theme.MUTED);

        pendingCard.setValue(Ui.peso(pendingAmount));
        pendingCard.setNote(pending.size() + " waiting for approval", pending.isEmpty() ? Theme.MUTED : Theme.WARN);

        unpaidCard.setValue(Ui.peso(unpaid));
        unpaidCard.setNote("Across all tenants", Theme.MUTED);

        income = paymentDAO.incomeByMonth();
        loadYears();
        drawChart();
        loadActivity();
    }

    // ---------- Chart ----------
    private void loadYears() {
        TreeSet<String> years = new TreeSet<>(java.util.Comparator.reverseOrder());
        years.add(String.valueOf(Year.now().getValue()));
        for (String key : income.keySet()) {
            if (key != null && key.length() >= 4) years.add(key.substring(0, 4));
        }
        Object previous = yearBox.getSelectedItem();
        loading = true;
        yearBox.setModel(new DefaultComboBoxModel<>(years.toArray(new String[0])));
        if (previous != null && years.contains(previous.toString())) yearBox.setSelectedItem(previous);
        loading = false;
    }

    private void drawChart() {
        Object selected = yearBox.getSelectedItem();
        String year = selected == null ? String.valueOf(Year.now().getValue()) : selected.toString();
        double[] values = new double[12];
        for (int i = 0; i < 12; i++) {
            Double v = income.get(year + "-" + String.format("%02d", i + 1));
            values[i] = v == null ? 0 : v;
        }
        chart.setData(MONTHS, values);
    }

    // ---------- Recent activity ----------
    private void loadActivity() {
        activityList.removeAll();
        List<ActivityDAO.Entry> entries = activityDAO.recent(12);
        if (entries.isEmpty()) {
            JLabel empty = Theme.muted("No activity yet. Payments, new bills, and new tenants will show up here.");
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            activityList.add(empty);
        } else {
            for (ActivityDAO.Entry e : entries) activityList.add(activityRow(e));
        }
        activityList.revalidate();
        activityList.repaint();
    }

    private JPanel activityRow(ActivityDAO.Entry entry) {
        Color color = switch (entry.type()) {
            case "PAID" -> Theme.SUCCESS;
            case "PENDING" -> Theme.WARN;
            case "REJECTED" -> Theme.DANGER;
            default -> Theme.ACCENT;
        };

        JComponent dot = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillOval(0, 4, 10, 10);
                g2.dispose();
            }
        };
        dot.setPreferredSize(new Dimension(10, 18));

        JLabel message = new JLabel("<html><body style='width:230px'>" + Theme.esc(entry.message()) + "</body></html>");
        message.setFont(Theme.font(10, Font.PLAIN));
        JLabel time = Theme.muted(formatTime(entry.createdAt()));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(message);
        text.add(time);

        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(dot, BorderLayout.WEST);
        row.add(text, BorderLayout.CENTER);
        return row;
    }

    // the database saves times in UTC, so convert to this computer's time zone
    private static String formatTime(String utc) {
        try {
            LocalDateTime t = LocalDateTime.parse(utc.replace(' ', 'T'));
            ZonedDateTime local = t.atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.systemDefault());
            return local.format(DateTimeFormatter.ofPattern("MMM d, yyyy  h:mm a"));
        } catch (Exception e) {
            return utc;
        }
    }

    // keeps an unused-list import from being needed
    @SuppressWarnings("unused")
    private static List<String> none() { return new ArrayList<>(); }
}