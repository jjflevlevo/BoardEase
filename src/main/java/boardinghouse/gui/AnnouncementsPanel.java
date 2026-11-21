package boardinghouse.gui;

import boardinghouse.dao.AnnouncementDAO;
import boardinghouse.model.Announcement;
import boardinghouse.service.AnnouncementService;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

// owner: post, edit, and delete announcements that tenants see
public class AnnouncementsPanel extends Section {
    private final AnnouncementDAO dao = new AnnouncementDAO();
    private final AnnouncementService service = new AnnouncementService();
    private final JTable table = Theme.table("Posted", "Title", "Priority", "Message", "Actions");
    private List<Announcement> shown = new ArrayList<>();

    public AnnouncementsPanel() {
        JButton add = Theme.primary("+  New Announcement");
        add.addActionListener(e -> showForm(null));

        JPanel toolbar = Ui.toolbar(table, add);
        Ui.statusPills(table, 2);
        RowActions.install(table, 4,
                RowActions.icon("edit", Theme.ACCENT, "Edit announcement", row -> showForm(shown.get(row))),
                RowActions.icon("trash", Theme.DANGER, "Delete announcement", row -> delete(shown.get(row))));

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(toolbar, BorderLayout.NORTH);
        body.add(Theme.scroll(table), BorderLayout.CENTER);
        body.add(Ui.footer(table), BorderLayout.SOUTH);
        add(Ui.card(body), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        shown = dao.findAll();
        List<Object[]> rows = new ArrayList<>();
        for (Announcement a : shown) {
            rows.add(new Object[]{Ui.localDate(a.getCreatedAt()), a.getTitle(), a.isImportant() ? "IMPORTANT" : "NORMAL",
                    Ui.preview(a.getMessage(), 70), ""});
        }
        Theme.fill(table, rows);
    }

    // existing == null means a new announcement
    private void showForm(Announcement existing) {
        JTextField title = Theme.field("e.g. Water interruption on Saturday");
        JTextArea message = Ui.textArea("Write the details for your tenants...", 6);
        JCheckBox important = new JCheckBox("Mark as important (shown in red to tenants)");
        important.setOpaque(false);
        important.setFocusPainted(false);
        important.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (existing != null) {
            title.setText(existing.getTitle());
            message.setText(existing.getMessage());
            important.setSelected(existing.isImportant());
        }

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.add(Theme.labeled("Title", title));
        form.add(Theme.gap(10));
        form.add(Ui.labeledBlock("Message", Ui.areaScroll(message), 150));
        form.add(Theme.gap(10));
        form.add(important);
        form.setPreferredSize(new Dimension(480, form.getPreferredSize().height));

        String dialogTitle = existing == null ? "New Announcement" : "Edit Announcement";
        while (true) {
            int ok = JOptionPane.showConfirmDialog(this, form, dialogTitle,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (ok != JOptionPane.OK_OPTION) return;

            String t = title.getText().trim();
            String m = message.getText().trim();
            String error = existing == null
                    ? service.post(t, m, important.isSelected())
                    : service.edit(existing.getAnnouncementId(), t, m, important.isSelected());
            if (error == null) {
                msg(existing == null ? "Announcement posted. Tenants will see it on their dashboard."
                        : "Announcement updated.");
                refresh();
                return;
            }
            msg(error);
        }
    }

    private void delete(Announcement a) {
        if (!Theme.confirmWarn(this, "Delete the announcement \"" + a.getTitle() + "\"?", "Delete Announcement")) return;
        service.remove(a.getAnnouncementId());
        refresh();
    }
}