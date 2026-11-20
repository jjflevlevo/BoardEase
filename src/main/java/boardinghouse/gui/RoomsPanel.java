package boardinghouse.gui;

import boardinghouse.dao.RoomDAO;
import boardinghouse.dao.TenantDAO;
import boardinghouse.model.Room;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class RoomsPanel extends Section {
    private final RoomDAO roomDAO = new RoomDAO();
    private final TenantDAO tenantDAO = new TenantDAO();
    private final JTable table = Theme.table("Room #", "Capacity", "Tenants", "Status", "Monthly Rate (\u20B1)", "Actions");
    private List<Room> shown = new ArrayList<>();

    public RoomsPanel() {
        JButton addBtn = Theme.primary("+  Add Room");
        addBtn.addActionListener(e -> addRoom());

        JPanel toolbar = Ui.toolbar(table, addBtn);
        Ui.statusPills(table, 3);
        RowActions.install(table, 5,
                RowActions.icon("edit", Theme.ACCENT, "Edit room", row -> edit(shown.get(row))),
                RowActions.icon("trash", Theme.DANGER, "Delete room", row -> delete(List.of(shown.get(row)))));

        JButton bulkDelete = Theme.secondary("Delete Selected");
        bulkDelete.addActionListener(e -> delete(selectedRooms()));

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(toolbar, BorderLayout.NORTH);
        body.add(Theme.scroll(table), BorderLayout.CENTER);
        body.add(Ui.footer(table, bulkDelete), BorderLayout.SOUTH);
        add(Ui.card(body), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        shown = roomDAO.findAll();
        List<Object[]> rows = new ArrayList<>();
        for (Room r : shown) {
            int tenants = tenantDAO.countInRoom(r.getRoomId());
            rows.add(new Object[]{r.getRoomNo(), r.getCapacity(), tenants + " / " + r.getCapacity(),
                    tenants > 0 ? "Occupied" : "Vacant", Theme.money(r.getMonthlyRent()), ""});
        }
        Theme.fill(table, rows);
    }

    private List<Room> selectedRooms() {
        List<Room> list = new ArrayList<>();
        for (int i : Theme.selectedModelRows(table)) list.add(shown.get(i));
        return list;
    }

    private void addRoom() {
        JTextField no = Theme.field("e.g. 101");
        JTextField cap = Theme.field("Max number of tenants");
        JTextField rent = Theme.field("Rent per tenant per month");
        int ok = JOptionPane.showConfirmDialog(this,
                Theme.form(Theme.labeled("Room no.", no), Theme.labeled("Capacity", cap),
                        Theme.labeled("Monthly rent per tenant (\u20B1)", rent)),
                "Add Room", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;

        try {
            String roomNo = no.getText().trim();
            int capacity = Integer.parseInt(cap.getText().trim());
            double monthlyRent = Double.parseDouble(rent.getText().trim());
            if (roomNo.isEmpty() || capacity <= 0 || monthlyRent <= 0) throw new NumberFormatException();
            msg(roomDAO.insert(roomNo, capacity, monthlyRent) ? "Room added." : "Room no. already exists.");
            refresh();
        } catch (NumberFormatException ex) {
            msg("Enter valid values.");
        }
    }

    private void edit(Room r) {
        JTextField no = Theme.field("Room no.");
        no.setText(r.getRoomNo());
        JTextField cap = Theme.field("Max number of tenants");
        cap.setText(String.valueOf(r.getCapacity()));
        JTextField rent = Theme.field("Rent per tenant per month");
        rent.setText(String.valueOf(r.getMonthlyRent()));
        JLabel note = Theme.muted("<html><body style='width:300px'>A new rent only applies to bills generated from now on. "
                + "Old bills keep their amounts.</body></html>");

        int ok = JOptionPane.showConfirmDialog(this,
                Theme.form(Theme.labeled("Room no.", no), Theme.labeled("Capacity", cap),
                        Theme.labeled("Monthly rent per tenant (\u20B1)", rent), note),
                "Edit Room", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;

        try {
            String roomNo = no.getText().trim();
            int capacity = Integer.parseInt(cap.getText().trim());
            double monthlyRent = Double.parseDouble(rent.getText().trim());
            if (roomNo.isEmpty() || capacity <= 0 || monthlyRent <= 0) throw new NumberFormatException();
            String error = roomDAO.update(r.getRoomId(), roomNo, capacity, monthlyRent);
            msg(error == null ? "Room updated." : error);
            refresh();
        } catch (NumberFormatException ex) {
            msg("Enter valid values.");
        }
    }

    private void delete(List<Room> sel) {
        if (sel.isEmpty()) { msg("Select at least one room first."); return; }

        String what = sel.size() == 1 ? "room " + sel.get(0).getRoomNo() : sel.size() + " rooms";
        if (!Theme.confirmWarn(this, "Delete " + what + "?\n\nRooms that still have tenants cannot be deleted.\n"
                + "The meter readings of a deleted room are removed too.", "Delete Rooms")) return;

        int deleted = 0;
        List<String> blocked = new ArrayList<>();
        for (Room r : sel) {
            if (roomDAO.delete(r.getRoomId())) deleted++;
            else blocked.add(r.getRoomNo());
        }
        String result = deleted + " room(s) deleted.";
        if (!blocked.isEmpty()) result += "\nNot deleted (still has tenants): " + String.join(", ", blocked);
        msg(result);
        refresh();
    }
}