package boardinghouse.gui;

import boardinghouse.dao.*;
import boardinghouse.model.Account;
import boardinghouse.model.Bill;
import boardinghouse.model.Room;
import boardinghouse.model.Tenant;
import boardinghouse.service.AuthService;
import boardinghouse.service.PaymentService;
import boardinghouse.util.PasswordUtil;
import boardinghouse.util.Validator;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TenantsPanel extends Section {
    private final TenantDAO tenantDAO = new TenantDAO();
    private final RoomDAO roomDAO = new RoomDAO();
    private final BillDAO billDAO = new BillDAO();
    private final AccountDAO accountDAO = new AccountDAO();
    private final AuthService auth = new AuthService();
    private final PaymentService payments = new PaymentService();
    private final Runnable onChange;
    private final JTable table = Theme.table("Name", "Room", "Contact", "Email", "Debt (\u20B1)", "Status", "Actions");
    private List<Tenant> shown = new ArrayList<>();

    public TenantsPanel(Runnable onChange) {
        this.onChange = onChange;

        JButton addBtn = Theme.primary("+  Add Tenant");
        addBtn.addActionListener(e -> addTenant());

        JPanel toolbar = Ui.toolbar(table, addBtn);
        Ui.statusPills(table, 5);
        RowActions.install(table, 6,
                RowActions.icon("eye", Theme.ACCENT, "View profile", row -> viewProfile(shown.get(row))),
                RowActions.icon("transfer", Theme.ACCENT, "Assign or transfer room",
                        row -> assignRoom(List.of(shown.get(row)))),
                RowActions.icon("trash", Theme.DANGER, "Delete tenant", row -> deleteTenants(List.of(shown.get(row)))));

        JButton assignBtn = Theme.secondary("Assign Room");
        assignBtn.addActionListener(e -> assignRoom(selectedTenants()));
        JButton removeBtn = Theme.secondary("Remove from Room");
        removeBtn.addActionListener(e -> removeFromRoom(selectedTenants()));
        JButton deleteBtn = Theme.secondary("Delete Selected");
        deleteBtn.addActionListener(e -> deleteTenants(selectedTenants()));

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(toolbar, BorderLayout.NORTH);
        body.add(Theme.scroll(table), BorderLayout.CENTER);
        body.add(Ui.footer(table, assignBtn, removeBtn, deleteBtn), BorderLayout.SOUTH);
        add(Ui.card(body), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        shown = tenantDAO.findAll();
        List<Object[]> rows = new ArrayList<>();
        for (Tenant t : shown) {
            Account a = accountDAO.findById(t.getAccountId());
            Room r = t.getRoomId() == null ? null : roomDAO.findById(t.getRoomId());
            rows.add(new Object[]{t.getName(), r == null ? "-" : r.getRoomNo(), a == null ? "-" : a.getPhone(),
                    a == null ? "-" : a.getEmail(), Theme.money(debtOf(t)), r == null ? "Unassigned" : "Active", ""});
        }
        Theme.fill(table, rows);
    }

    private double debtOf(Tenant t) {
        double total = 0;
        for (Bill b : billDAO.findByTenant(t.getTenantId())) total += payments.balanceOf(b);
        return total;
    }

    // all selected tenants (shows a message when nothing is selected)
    private List<Tenant> selectedTenants() {
        List<Tenant> list = new ArrayList<>();
        for (int i : Theme.selectedModelRows(table)) list.add(shown.get(i));
        if (list.isEmpty()) msg("Select at least one tenant first.");
        return list;
    }

    // ---------- Add tenant ----------
    private void addTenant() {
        JTextField name = Theme.field("Full name");
        JTextField email = Theme.field("name@example.com");
        JTextField phone = Theme.field("09XXXXXXXXX");
        JTextField password = Theme.field("6+ characters");
        JTextField address = Theme.field("Home address");
        JTextField occupation = Theme.field("Occupation");
        JTextField age = Theme.field("Age");
        JTextField emergency = Theme.field("09XXXXXXXXX");

        JPanel grid = new JPanel(new GridLayout(0, 2, 16, 0));
        grid.setOpaque(false);
        grid.add(Theme.labeled("Full name", name));
        grid.add(Theme.labeled("Email", email));
        grid.add(Theme.labeled("Phone no.", phone));
        grid.add(Theme.labeled("Temporary password (shown so you can pass it on)", password));
        grid.add(Theme.labeled("Address", address));
        grid.add(Theme.labeled("Occupation", occupation));
        grid.add(Theme.labeled("Age", age));
        grid.add(Theme.labeled("Emergency no.", emergency));
        grid.setPreferredSize(new Dimension(600, grid.getPreferredSize().height));

        while (true) {
            int ok = JOptionPane.showConfirmDialog(this, grid, "Add Tenant",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (ok != JOptionPane.OK_OPTION) return;

            String n = name.getText().trim();
            String mail = email.getText().trim();
            String ph = phone.getText().trim();
            String pw = password.getText().trim();
            String addr = address.getText().trim();
            String occ = occupation.getText().trim();
            String em = emergency.getText().trim();

            int ageValue = -1;
            try {
                ageValue = Integer.parseInt(age.getText().trim());
            } catch (NumberFormatException ignored) { }

            String problem = null;
            if (!Validator.isNotBlank(n)) problem = "Enter the tenant's full name.";
            else if (!Validator.isValidEmail(mail)) problem = "Enter a valid email address.";
            else if (accountDAO.findByEmail(mail) != null) problem = "That email is already registered.";
            else if (!Validator.isValidPhone(ph)) problem = "The phone no. must be 11 digits starting with 09.";
            else if (!Validator.isValidPassword(pw)) problem = "The temporary password must be at least 6 characters.";
            else if (!Validator.isNotBlank(addr)) problem = "Enter the address.";
            else if (!Validator.isNotBlank(occ)) problem = "Enter the occupation.";
            else if (!Validator.isValidAge(ageValue)) problem = "Age must be between 16 and 100.";
            else if (!Validator.isValidPhone(em)) problem = "The emergency no. must be 11 digits starting with 09.";

            if (problem != null) {
                msg(problem);
                continue;
            }

            boolean created = auth.signUp(mail, ph, pw, new Tenant(n, addr, ageValue, occ, em));
            msg(created ? "Tenant added. Give them their email and the temporary password so they can log in "
                    + "and change it in My Profile." : "Could not add the tenant.");
            refresh();
            return;
        }
    }

    // ---------- Profile ----------
    private void viewProfile(Tenant t) {
        Account a = accountDAO.findById(t.getAccountId());
        Room r = t.getRoomId() == null ? null : roomDAO.findById(t.getRoomId());

        JPanel grid = Theme.detailGrid(new String[][]{
                {"Name", t.getName()},
                {"Email", a == null ? "-" : a.getEmail()},
                {"Phone", a == null ? "-" : a.getPhone()},
                {"Password", "Hidden (stored as a salted hash)"},
                {"Address", t.getAddress()},
                {"Age", String.valueOf(t.getAge())},
                {"Occupation", t.getOccupation()},
                {"Emergency no.", t.getEmergencyNo()},
                {"Room", r == null ? "Unassigned" : r.getRoomNo()},
                {"Remaining debt", Ui.peso(debtOf(t))}});
        JLabel note = Theme.muted("<html><body style='width:380px'>Passwords are never stored in readable form, "
                + "so they cannot be shown. If a tenant forgets theirs, reset it and give them the temporary "
                + "password. They can change it in My Profile.</body></html>");

        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.add(grid, BorderLayout.CENTER);
        content.add(note, BorderLayout.SOUTH);

        Object[] options = {"Reset Password", "Close"};
        int choice = JOptionPane.showOptionDialog(this, content, "Tenant Profile: " + t.getName(),
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[1]);
        if (choice == 0) resetPassword(t, a);
    }

    private void resetPassword(Tenant t, Account a) {
        if (a == null) { msg("This tenant has no account."); return; }
        JTextField pw = Theme.field("Temporary password (6+ characters)");
        int ok = JOptionPane.showConfirmDialog(this,
                Theme.form(Theme.labeled("Temporary password for " + t.getName(), pw)),
                "Reset Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;

        String value = pw.getText().trim();
        if (!Validator.isValidPassword(value)) { msg("The password must be at least 6 characters."); return; }
        accountDAO.updatePassword(a.getAccountId(), PasswordUtil.hash(value));

        JTextField shownPassword = Theme.selectable(value);
        JLabel note = Theme.muted("Give this to " + t.getName() + ". They can change it in My Profile.");
        JOptionPane.showMessageDialog(this,
                Theme.form(new JLabel("Password reset. Temporary password:"), shownPassword, note),
                "Password Reset", JOptionPane.INFORMATION_MESSAGE);
    }

    // ---------- Rooms ----------
    private void assignRoom(List<Tenant> sel) {
        if (sel.isEmpty()) return;
        List<Room> rooms = roomDAO.findAll();
        if (rooms.isEmpty()) { msg("Add a room first."); return; }

        JComboBox<Theme.RoomItem> combo = new JComboBox<>(Theme.roomModel(rooms, false));
        String who = sel.size() == 1 ? sel.get(0).getName() : sel.size() + " tenants";
        int ok = JOptionPane.showConfirmDialog(this,
                Theme.form(Theme.labeled("Assign " + who + " to", combo)),
                "Assign or Transfer Room", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;

        Room room = ((Theme.RoomItem) combo.getSelectedItem()).room();
        List<Tenant> toMove = new ArrayList<>();
        for (Tenant t : sel) {
            if (t.getRoomId() == null || t.getRoomId() != room.getRoomId()) toMove.add(t);
        }
        if (toMove.isEmpty()) { msg("The selected tenant(s) are already in that room."); return; }

        int free = room.getCapacity() - tenantDAO.countInRoom(room.getRoomId());
        if (toMove.size() > free) {
            msg("Room " + room.getRoomNo() + " has only " + free + " free slot(s), but you selected "
                    + toMove.size() + " tenant(s).");
            return;
        }
        int done = 0;
        for (Tenant t : toMove) {
            if (tenantDAO.assignRoom(t.getTenantId(), room.getRoomId())) done++;
        }
        msg(done + " tenant(s) assigned to room " + room.getRoomNo() + ".");
        refresh();
    }

    private void removeFromRoom(List<Tenant> sel) {
        if (sel.isEmpty()) return;

        int done = 0, noRoom = 0;
        List<String> blocked = new ArrayList<>();
        for (Tenant t : sel) {
            if (t.getRoomId() == null) { noRoom++; continue; }
            double debt = debtOf(t);
            if (debt > 0) blocked.add(t.getName() + " (owes " + Ui.peso(debt) + ")");
            else if (tenantDAO.assignRoom(t.getTenantId(), null)) done++;
        }
        String result = done + " tenant(s) removed from their room.";
        if (noRoom > 0) result += "\n" + noRoom + " had no room.";
        if (!blocked.isEmpty()) result += "\nNot removed because of unpaid balance: " + String.join(", ", blocked);
        msg(result);
        refresh();
    }

    // ---------- Delete ----------
    private void deleteTenants(List<Tenant> sel) {
        if (sel.isEmpty()) return;

        double totalDebt = 0;
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < sel.size(); i++) {
            totalDebt += debtOf(sel.get(i));
            if (i < 5) names.append("  - ").append(sel.get(i).getName()).append("\n");
        }
        if (sel.size() > 5) names.append("  ... and ").append(sel.size() - 5).append(" more\n");

        String text = "Permanently delete " + (sel.size() == 1 ? "this tenant" : sel.size() + " tenants") + "?\n\n"
                + names + "\nThis removes their account, bills, and payment records.\n"
                + "Their approved payments will no longer count in the income report.";
        if (totalDebt > 0) text += "\n\nUnpaid balance that will be lost: " + Ui.peso(totalDebt);
        if (!Theme.confirmWarn(this, text, "Delete Tenant")) return;

        int deleted = 0;
        for (Tenant t : sel) {
            if (tenantDAO.deleteCompletely(t)) deleted++;
        }
        msg(deleted + " of " + sel.size() + " tenant(s) deleted.");
        onChange.run();
        refresh();
    }
}