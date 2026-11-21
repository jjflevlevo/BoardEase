package boardinghouse.gui;

import boardinghouse.dao.MaintenanceDAO;
import boardinghouse.dao.TenantDAO;
import boardinghouse.model.MaintenanceRequest;
import boardinghouse.model.Tenant;
import boardinghouse.service.MaintenanceService;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

// tenant: send maintenance requests and follow their status
public class TenantRequestsPanel extends Section {
    private final int tenantId;
    private final MaintenanceDAO dao = new MaintenanceDAO();
    private final TenantDAO tenantDAO = new TenantDAO();
    private final MaintenanceService service = new MaintenanceService();
    private final JTable table = Theme.table("Sent", "Category", "Problem", "Status", "Owner's Note", "Updated", "Actions");
    private List<MaintenanceRequest> shown = new ArrayList<>();

    public TenantRequestsPanel(int tenantId) {
        this.tenantId = tenantId;
        setBorder(BorderFactory.createEmptyBorder());

        JButton add = Theme.primary("+  New Request");
        add.addActionListener(e -> create());

        JPanel toolbar = Ui.toolbar(table, add);
        Ui.statusPills(table, 3);
        RowActions.install(table, 6,
                RowActions.icon("eye", Theme.ACCENT, "View details", row -> view(shown.get(row))),
                RowActions.when(RowActions.text("Cancel", Theme.DANGER, "Cancel this request",
                                row -> cancel(shown.get(row))),
                        row -> row < shown.size() && shown.get(row).isOpen()));

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(toolbar, BorderLayout.NORTH);
        body.add(Theme.scroll(table), BorderLayout.CENTER);
        body.add(Ui.footer(table), BorderLayout.SOUTH);
        add(Ui.panelCard(Theme.title("Maintenance Requests"), body), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        shown = dao.findByTenant(tenantId);
        List<Object[]> rows = new ArrayList<>();
        for (MaintenanceRequest r : shown) {
            rows.add(new Object[]{Ui.localDate(r.getCreatedAt()), r.getCategory(), Ui.preview(r.getDescription(), 45),
                    r.getStatusLabel(), r.getOwnerNote() == null ? "-" : Ui.preview(r.getOwnerNote(), 40),
                    Ui.localDate(r.getUpdatedAt()), ""});
        }
        Theme.fill(table, rows);
    }

    private void create() {
        Tenant tenant = tenantDAO.findById(tenantId);
        if (tenant == null) { msg("Tenant not found."); return; }

        JComboBox<String> category = new JComboBox<>(MaintenanceService.CATEGORIES);
        JTextArea description = Ui.textArea("Describe the problem, e.g. The faucet in the bathroom is leaking.", 5);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.add(Theme.labeled("Category", category));
        form.add(Theme.gap(10));
        form.add(Ui.labeledBlock("What is the problem?", Ui.areaScroll(description), 130));
        if (tenant.getRoomId() == null) {
            JLabel hint = Theme.muted("You are not assigned to a room yet, so the request will not show a room.");
            hint.setAlignmentX(Component.LEFT_ALIGNMENT);
            form.add(Theme.gap(6));
            form.add(hint);
        }
        form.setPreferredSize(new Dimension(460, form.getPreferredSize().height));

        while (true) {
            int ok = JOptionPane.showConfirmDialog(this, form, "New Maintenance Request",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (ok != JOptionPane.OK_OPTION) return;

            String error = service.submit(tenant, (String) category.getSelectedItem(), description.getText().trim());
            if (error == null) {
                msg("Request sent. The owner will update its status here.");
                refresh();
                return;
            }
            msg(error);
        }
    }

    private void view(MaintenanceRequest r) {
        JOptionPane.showMessageDialog(this, MaintenancePanel.details(r), "Maintenance Request", JOptionPane.PLAIN_MESSAGE);
    }

    private void cancel(MaintenanceRequest r) {
        if (!Theme.confirm(this, "Cancel your " + r.getCategory().toLowerCase() + " request?", "Cancel Request")) return;
        String error = service.cancel(tenantId, r.getRequestId());
        if (error != null) msg(error);
        refresh();
    }
}