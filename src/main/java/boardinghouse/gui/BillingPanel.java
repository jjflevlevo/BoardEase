package boardinghouse.gui;

import boardinghouse.dao.BillDAO;
import boardinghouse.dao.MeterReadingDAO;
import boardinghouse.dao.RoomDAO;
import boardinghouse.model.Bill;
import boardinghouse.model.Room;
import boardinghouse.service.BillingService;
import boardinghouse.service.PaymentService;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class BillingPanel extends Section {
    private static final String MONTH_PATTERN = "\\d{4}-(0[1-9]|1[0-2])";
    private static final DecimalFormat NUMBER = new DecimalFormat("#.##");

    private final RoomDAO roomDAO = new RoomDAO();
    private final MeterReadingDAO meterDAO = new MeterReadingDAO();
    private final BillDAO billDAO = new BillDAO();
    private final BillingService billing = new BillingService();
    private final PaymentService payments = new PaymentService();

    // meter reading form
    private final JComboBox<Theme.RoomItem> readingRoom = new JComboBox<>();
    private final JTextField readingMonth = Theme.field("yyyy-MM");
    private final JTextField electricField = Theme.field("Enter kWh reading");
    private final JTextField waterField = Theme.field("Enter cubic meter reading");
    private final JLabel electricPrev = Theme.muted(" ");
    private final JLabel waterPrev = Theme.muted(" ");

    // generate bills form
    private final JTextField genMonth = Theme.field("yyyy-MM");
    private final JTextField genDue = Theme.field("yyyy-MM-dd");
    private final JTextField waterRate = Theme.field("Price per cubic meter");
    private final JTextField electricRate = Theme.field("Price per kWh");
    private final JComboBox<Theme.RoomItem> genRoom = new JComboBox<>();

    private final JTable billsTable = Theme.table("Tenant", "Type", "Month", "Amount (\u20B1)", "Balance (\u20B1)",
            "Due Date", "Status");

    public BillingPanel() {
        JTabbedPane tabs = Ui.tabs();
        tabs.addTab("Meter Reading", wrap(readingCard()));
        tabs.addTab("Generate Bills", wrap(generateCard()));
        tabs.addTab("All Bills", billsView());
        add(tabs, BorderLayout.CENTER);

        String month = YearMonth.now().toString();
        readingMonth.setText(month);
        genMonth.setText(month);
        genDue.setText(LocalDate.now().plusDays(14).toString());

        readingRoom.addActionListener(e -> updatePrevious());
        watch(readingMonth, this::updatePrevious);
    }

    private static void watch(JTextComponent c, Runnable r) {
        c.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { r.run(); }
            @Override public void removeUpdate(DocumentEvent e) { r.run(); }
            @Override public void changedUpdate(DocumentEvent e) { r.run(); }
        });
    }

    // ---------- Layout ----------
    private JPanel wrap(JPanel card) {
        JPanel w = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        w.setOpaque(false);
        w.setBorder(new EmptyBorder(16, 0, 0, 0));
        w.add(card);
        return w;
    }

    private JPanel meterColumn(String label, JTextField field, JLabel previous) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(Theme.labeled(label, field));
        previous.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(previous);
        return p;
    }

    private JPanel readingCard() {
        JPanel card = Theme.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JPanel top = new JPanel(new GridLayout(1, 2, 16, 0));
        top.setOpaque(false);
        top.add(Theme.labeled("Room", readingRoom));
        top.add(Theme.labeled("Billing month", readingMonth));

        JPanel meters = new JPanel(new GridLayout(1, 2, 16, 0));
        meters.setOpaque(false);
        meters.add(meterColumn("Electric Meter (kWh)", electricField, electricPrev));
        meters.add(meterColumn("Water Meter (cubic meters)", waterField, waterPrev));

        JLabel hint = Theme.muted("Fill in one or both meters. A reading cannot be lower than the previous one.");
        JButton save = Theme.primary("Save Reading");
        save.addActionListener(e -> saveReadings());

        card.add(Theme.block(top, 66));
        card.add(Theme.gap(8));
        card.add(Theme.block(meters, 90));
        card.add(Theme.block(hint, 20));
        card.add(Theme.gap(16));
        card.add(Theme.block(save, 40));
        card.setPreferredSize(new Dimension(640, card.getPreferredSize().height));
        return card;
    }

    private JPanel generateCard() {
        JPanel card = Theme.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JPanel dates = new JPanel(new GridLayout(1, 2, 16, 0));
        dates.setOpaque(false);
        dates.add(Theme.labeled("Billing month", genMonth));
        dates.add(Theme.labeled("Due date", genDue));

        JPanel rates = new JPanel(new GridLayout(1, 2, 16, 0));
        rates.setOpaque(false);
        rates.add(Theme.labeled("Water rate per cubic meter (\u20B1)", waterRate));
        rates.add(Theme.labeled("Electric rate per kWh (\u20B1)", electricRate));

        JButton generate = Theme.primary("Generate Bills");
        generate.addActionListener(e -> generate());

        card.add(Theme.block(dates, 66));
        card.add(Theme.gap(8));
        card.add(Theme.block(rates, 66));
        card.add(Theme.gap(8));
        card.add(Theme.block(Theme.labeled("Room", genRoom), 66));
        card.add(Theme.gap(16));
        card.add(Theme.block(generate, 40));
        card.setPreferredSize(new Dimension(640, card.getPreferredSize().height));
        return card;
    }

    private JPanel billsView() {
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);
        content.add(Ui.toolbar(billsTable, null), BorderLayout.NORTH);
        content.add(Theme.scroll(billsTable), BorderLayout.CENTER);
        content.add(Ui.footer(billsTable), BorderLayout.SOUTH);
        Ui.statusPills(billsTable, 6);

        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(16, 0, 0, 0));
        p.add(Ui.card(content), BorderLayout.CENTER);
        return p;
    }

    // ---------- Data ----------
    @Override
    public void refresh() {
        List<Room> rooms = roomDAO.findAll();
        readingRoom.setModel(Theme.roomModel(rooms, false));
        genRoom.setModel(Theme.roomModel(rooms, true));
        updatePrevious();

        LocalDate today = LocalDate.now();
        List<Object[]> rows = new ArrayList<>();
        for (Bill b : billDAO.findAll()) {
            boolean overdue = !"PAID".equals(b.getStatus()) && today.isAfter(b.getDueDate());
            rows.add(new Object[]{b.getTenantName(), b.getType(), b.getBillingMonth(), Theme.money(b.getAmount()),
                    Theme.money(payments.balanceOf(b)), b.getDueDate().toString(),
                    overdue ? "OVERDUE" : b.getStatus()});
        }
        Theme.fill(billsTable, rows);
    }

    // shows the previous meter readings under the fields
    private void updatePrevious() {
        Theme.RoomItem item = (Theme.RoomItem) readingRoom.getSelectedItem();
        String month = readingMonth.getText().trim();
        if (item == null || item.room() == null || !month.matches(MONTH_PATTERN)) {
            electricPrev.setText(" ");
            waterPrev.setText(" ");
            return;
        }
        int roomId = item.room().getRoomId();
        Double e = meterDAO.findPrevious(roomId, "ELECTRIC", month);
        Double w = meterDAO.findPrevious(roomId, "WATER", month);
        electricPrev.setText(e == null ? "No earlier reading" : "Previous reading: " + NUMBER.format(e));
        waterPrev.setText(w == null ? "No earlier reading" : "Previous reading: " + NUMBER.format(w));
    }

    private void saveReadings() {
        Theme.RoomItem item = (Theme.RoomItem) readingRoom.getSelectedItem();
        if (item == null || item.room() == null) { msg("Add a room first."); return; }

        String month = readingMonth.getText().trim();
        if (!month.matches(MONTH_PATTERN)) { msg("Month must look like 2026-11."); return; }

        String electric = electricField.getText().trim();
        String water = waterField.getText().trim();
        if (electric.isEmpty() && water.isEmpty()) { msg("Enter at least one meter reading."); return; }

        int roomId = item.room().getRoomId();
        List<String> results = new ArrayList<>();
        if (!electric.isEmpty()) {
            String r = saveOne(roomId, "ELECTRIC", "Electric", electric, month);
            if (r.endsWith("saved.")) electricField.setText("");
            results.add(r);
        }
        if (!water.isEmpty()) {
            String r = saveOne(roomId, "WATER", "Water", water, month);
            if (r.endsWith("saved.")) waterField.setText("");
            results.add(r);
        }
        msg(String.join("\n", results));
        updatePrevious();
    }

    private String saveOne(int roomId, String type, String label, String text, String month) {
        double reading;
        try {
            reading = Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return label + ": enter a valid number.";
        }
        if (reading < 0) return label + ": the reading cannot be negative.";

        Double previous = meterDAO.findPrevious(roomId, type, month);
        if (previous != null && reading < previous) {
            return label + ": cannot be lower than the previous reading (" + NUMBER.format(previous) + ").";
        }
        return label + ": " + (meterDAO.insert(roomId, type, reading, month)
                ? "saved." : "a reading for that month already exists.");
    }

    private void generate() {
        String month = genMonth.getText().trim();
        if (!month.matches(MONTH_PATTERN)) { msg("Month must look like 2026-11."); return; }

        LocalDate due;
        try {
            due = LocalDate.parse(genDue.getText().trim());
        } catch (DateTimeParseException e) {
            msg("Due date must look like 2026-11-30.");
            return;
        }

        double wr, er;
        try {
            wr = Double.parseDouble(waterRate.getText().trim());
            er = Double.parseDouble(electricRate.getText().trim());
        } catch (NumberFormatException e) {
            msg("Enter valid water and electric rates.");
            return;
        }
        if (wr < 0 || er < 0) { msg("Rates cannot be negative."); return; }

        Theme.RoomItem item = (Theme.RoomItem) genRoom.getSelectedItem();
        if (item == null) { msg("Add a room first."); return; }

        String result = item.room() == null
                ? billing.generateBills(month, due, wr, er)
                : billing.generateBills(item.room(), month, due, wr, er);
        msg(result.trim());
        refresh();
    }
}