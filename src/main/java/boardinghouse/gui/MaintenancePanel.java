package boardinghouse.gui;

import boardinghouse.dao.MaintenanceDAO;
import boardinghouse.model.MaintenanceRequest;
import boardinghouse.service.MaintenanceService;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

// owner: see tenants' maintenance requests and update their status
public class MaintenancePanel extends Section {
    private static final String[] FILTER_LABELS = {"All requests", "Open", "In Progress", "Done"};
    private static final String[] FILTER_STATUS = {null, "OPEN", "IN_PROGRESS", "DONE"};

    private final MaintenanceDAO dao = new MaintenanceDAO();
    private final MaintenanceService service = new MaintenanceService();
    private final Runnable onChange;
    private final JComboBox<String> filter = new JComboBox<>(FILTER_LABELS);
    private final JTable table = Theme.table("Date", "Tenant", "Room", "Category", "Problem", "Status", "Actions");
    private List<MaintenanceRequest> shown = new ArrayList<>();

    public MaintenancePanel(Runnable onChange) {
        this.onChange = onChange;
        filter.setPreferredSize(new Dimension(170, 40));
        filter.addActionListener(e -> refresh());

        JPanel toolbar = Ui.toolbar(table, Theme.buttonRow(filter, Export.csvButton(table, "maintenance_requests")));
        Ui.statusPills(table, 5);
        RowActions.install(table, 6,
                RowActions.icon("eye", Theme.ACCENT, "View details", row -> view(shown.get(row))),
                RowActions.when(RowActions.text("Start", Theme.ACCENT, "Mark as in progress",
                                row -> update(shown.get(row), "IN_PROGRESS")),
                        row -> row < shown.size() && shown.get(row).isOpen()),
                RowActions.when(RowActions.text("Done", Theme.SUCCESS, "Mark as done",
                                row -> update(shown.get(row), "DONE")),
                        row -> row < shown.size() && !shown.get(row).isDone()));

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(toolbar, BorderLayout.NORTH);
        body.add(Theme.scroll(table), BorderLayout.CENTER);
        body.add(Ui.footer(table), BorderLayout.SOUTH);
        add(Ui.card(body), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        String status = FILTER_STATUS[Math.max(0, filter.getSelectedIndex())];
        shown = new ArrayList<>();
        for (MaintenanceRequest r : dao.findAll()) {
            if (status == null || status.equals(r.getStatus())) shown.add(r);
        }
        List<Object[]> rows = new ArrayList<>();
        for (MaintenanceRequest r : shown) {
            rows.add(new Object[]{Ui.localDate(r.getCreatedAt()), r.getTenantName(),
                    r.getRoomNo() == null ? "-" : r.getRoomNo(), r.getCategory(), Ui.preview(r.getDescription(), 50),
                    r.getStatusLabel(), ""});
        }
        Theme.fill(table, rows);
    }

    // full details of a request (also used by the tenant's view)
    public static JPanel details(MaintenanceRequest r) {
        JPanel grid = Theme.detailGrid(new String[][]{
                {"Tenant", r.getTenantName()},
                {"Room", r.getRoomNo() == null ? "-" : r.getRoomNo()},
                {"Category", r.getCategory()},
                {"Status", r.getStatusLabel()},
                {"Sent", Ui.localDate(r.getCreatedAt())},
                {"Last update", Ui.localDate(r.getUpdatedAt())},
                {"Owner's note", r.getOwnerNote() == null ? "-" : r.getOwnerNote()}});

        JTextArea text = new JTextArea(r.getDescription(), 5, 40);
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setFont(Theme.font(11, Font.PLAIN));
        text.setBackground(Theme.SIDEBAR);
        text.setMargin(new Insets(8, 10, 8, 10));
        Theme.addContextMenu(text);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);
        content.add(grid, BorderLayout.NORTH);
        content.add(Ui.labeledBlock("Problem", Ui.areaScroll(text), 120), BorderLayout.CENTER);
        content.setPreferredSize(new Dimension(480, content.getPreferredSize().height));
        return content;
    }

    private void view(MaintenanceRequest r) {
        JOptionPane.showMessageDialog(this, details(r), "Maintenance Request", JOptionPane.PLAIN_MESSAGE);
    }

    private void update(MaintenanceRequest r, String status) {
        String label = "IN_PROGRESS".equals(status) ? "in progress" : "done";
        JTextArea note = Ui.textArea("Optional note for the tenant, e.g. The plumber will come on Saturday.", 3);
        if (r.getOwnerNote() != null) note.setText(r.getOwnerNote());

        JLabel question = new JLabel("Mark " + r.getTenantName() + "'s " + r.getCategory().toLowerCase()
                + " request as " + label + "?");
        question.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(question);
        content.add(Theme.gap(12));
        content.add(Ui.labeledBlock("Note to the tenant (optional)", Ui.areaScroll(note), 90));
        content.setPreferredSize(new Dimension(460, content.getPreferredSize().height));

        while (true) {
            int ok = JOptionPane.showConfirmDialog(this, content, "Update Request",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (ok != JOptionPane.OK_OPTION) return;
            String error = service.updateStatus(r.getRequestId(), status, note.getText().trim());
            if (error == null) break;
            msg(error);
        }
        refresh();
        onChange.run();
    }
}