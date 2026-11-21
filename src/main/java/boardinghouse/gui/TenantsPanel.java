package boardinghouse.gui;

import boardinghouse.dao.AccountDAO;
import boardinghouse.dao.RoomDAO;
import boardinghouse.dao.TenantDAO;
import boardinghouse.model.Account;
import boardinghouse.model.Deposit;
import boardinghouse.model.Room;
import boardinghouse.model.Tenant;
import boardinghouse.service.AuthService;
import boardinghouse.service.TenancyService;
import boardinghouse.util.PasswordUtil;
import boardinghouse.util.Validator;
import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class TenantsPanel extends Section {
    private static final String[] FILTERS = {"Current tenants", "Moved out", "All tenants"};

    private final TenantDAO tenantDAO = new TenantDAO();
    private final RoomDAO roomDAO = new RoomDAO();
    private final AccountDAO accountDAO = new AccountDAO();
    private final AuthService auth = new AuthService();
    private final TenancyService tenancy = new TenancyService();
    private final Runnable onChange;
    private final JComboBox<String> filter = new JComboBox<>(FILTERS);
    private final JTable table = Theme.table("Name", "Room", "Contact", "Email", "Debt (\u20B1)", "Deposit (\u20B1)",
            "Status", "Actions");
    private List<Tenant> shown = new ArrayList<>();

    public TenantsPanel(Runnable onChange) {
        this.onChange = onChange;

        JButton addBtn = Theme.primary("+  Add Tenant");
        addBtn.addActionListener(e -> addTenant());
        filter.setPreferredSize(new Dimension(170, 40));
        filter.addActionListener(e -> refresh());

        JPanel toolbar = Ui.toolbar(table, Theme.buttonRow(filter, Export.csvButton(table, "tenants"), addBtn));
        Ui.statusPills(table, 6);
        RowActions.install(table, 7,
                RowActions.icon("eye", Theme.ACCENT, "View profile", row -> viewProfile(shown.get(row))),
                RowActions.when(RowActions.icon("edit", Theme.ACCENT, "Edit tenant", row -> editTenant(shown.get(row))),
                        row -> isActive(row)),
                RowActions.when(RowActions.icon("transfer", Theme.ACCENT, "Assign or transfer room",
                        row -> assignRoom(List.of(shown.get(row)))), row -> isActive(row)),
                RowActions.when(RowActions.text("Restore", Theme.ACCENT, "Make this tenant active again",
                        row -> restore(shown.get(row))), row -> row < shown.size() && shown.get(row).isMovedOut()),
                RowActions.icon("trash", Theme.DANGER, "Delete permanently", row -> deleteTenants(List.of(shown.get(row)))));

        JButton assignBtn = Theme.secondary("Assign Room");
        assignBtn.addActionListener(e -> assignRoom(selectedTenants()));
        JButton removeBtn = Theme.secondary("Remove from Room");
        removeBtn.addActionListener(e -> removeFromRoom(selectedTenants()));
        JButton moveOutBtn = Theme.secondary("Move Out");
        moveOutBtn.addActionListener(e -> {
            List<Tenant> sel = selectedTenants();
            if (sel.size() > 1) msg("Select one tenant to move out.");
            else if (sel.size() == 1) moveOut(sel.get(0));
        });
        JButton deleteBtn = Theme.secondary("Delete Selected");
        deleteBtn.addActionListener(e -> deleteTenants(selectedTenants()));

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(toolbar, BorderLayout.NORTH);
        body.add(Theme.scroll(table), BorderLayout.CENTER);
        body.add(Ui.footer(table, assignBtn, removeBtn, moveOutBtn, deleteBtn), BorderLayout.SOUTH);
        add(Ui.card(body), BorderLayout.CENTER);
    }

    private boolean isActive(int row) {
        return row < shown.size() && !shown.get(row).isMovedOut();
    }

    @Override
    public void refresh() {
        int f = Math.max(0, filter.getSelectedIndex());
        shown = f == 0 ? tenantDAO.findAll() : f == 1 ? tenantDAO.findMovedOut() : tenantDAO.findEveryone();

        List<Object[]> rows = new ArrayList<>();
        for (Tenant t : shown) {
            Account a = accountDAO.findById(t.getAccountId());
            Room r = t.getRoomId() == null ? null : roomDAO.findById(t.getRoomId());
            Deposit d = tenancy.depositOf(t.getTenantId());
            String deposit = d == null ? "-" : d.isHeld() ? Theme.money(d.getAmount()) : "Returned";
            String status = t.isMovedOut() ? "MOVED OUT" : r == null ? "UNASSIGNED" : "ACTIVE";
            rows.add(new Object[]{t.getName(), r == null ? "-" : r.getRoomNo(), a == null ? "-" : a.getPhone(),
                    a == null ? "-" : a.getEmail(), Theme.money(tenancy.unpaidBalance(t.getTenantId())), deposit,
                    status, ""});
        }
        Theme.fill(table, rows);
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
        Deposit d = tenancy.depositOf(t.getTenantId());
        String deposit = d == null ? "None recorded"
                : d.isHeld() ? Ui.peso(d.getAmount()) + " held (received " + d.getReceivedOn() + ")"
                : Ui.peso(d.getAmount()) + " returned on " + d.getSettledOn() + " (refund " + Ui.peso(d.getRefunded()) + ")";

        JPanel grid = Theme.detailGrid(new String[][]{
                {"Name", t.getName()},
                {"Status", t.isMovedOut() ? "Moved out on " + t.getMovedOutOn() : "Active"},
                {"Email", a == null ? "-" : a.getEmail()},
                {"Phone", a == null ? "-" : a.getPhone()},
                {"Password", "Hidden (stored as a salted hash)"},
                {"Address", t.getAddress()},
                {"Age", String.valueOf(t.getAge())},
                {"Occupation", t.getOccupation()},
                {"Emergency no.", t.getEmergencyNo()},
                {"Room", r == null ? "Unassigned" : r.getRoomNo()},
                {"Security deposit", deposit},
                {"Remaining debt", Ui.peso(tenancy.unpaidBalance(t.getTenantId()))}});

        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setOpaque(false);
        content.add(grid, BorderLayout.CENTER);
        content.add(Theme.muted("Passwords cannot be shown. Use Reset Password to give the tenant a temporary one."),
                BorderLayout.SOUTH);

        String[] options = t.isMovedOut()
                ? new String[]{"Restore", "Reset Password", "Close"}
                : new String[]{"Edit", "Deposit", "Reset Password", "Move Out", "Close"};
        int choice = JOptionPane.showOptionDialog(this, content, "Tenant Profile: " + t.getName(),
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[options.length - 1]);
        if (choice < 0) return;
        switch (options[choice]) {
            case "Edit" -> editTenant(t);
            case "Deposit" -> recordDeposit(t);
            case "Reset Password" -> resetPassword(t, a);
            case "Move Out" -> moveOut(t);
            case "Restore" -> restore(t);
            default -> { }
        }
    }

    private void editTenant(Tenant t) {
        Tenant fresh = tenantDAO.findById(t.getTenantId());
        Account a = accountDAO.findById(t.getAccountId());
        if (fresh == null || a == null) {
            msg("Tenant not found.");
            return;
        }
        if (ProfileDialog.edit(this, fresh, a, true)) refresh();
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

    // ---------- Deposit ----------
    private void recordDeposit(Tenant t) {
        Tenant fresh = tenantDAO.findById(t.getTenantId());
        if (fresh == null || fresh.isMovedOut()) { msg("This tenant has moved out."); return; }

        Deposit current = tenancy.depositOf(fresh.getTenantId());
        Room room = fresh.getRoomId() == null ? null : roomDAO.findById(fresh.getRoomId());

        JTextField amount = Theme.field("Amount in pesos");
        JTextField date = Theme.field("yyyy-MM-dd");
        JTextField note = Theme.field("Optional, e.g. Paid in cash");
        if (current != null && current.isHeld()) {
            amount.setText(Theme.money(current.getAmount()));
            date.setText(current.getReceivedOn());
            if (current.getNote() != null) note.setText(current.getNote());
        } else {
            if (room != null) amount.setText(Theme.money(room.getMonthlyRent()));
            date.setText(LocalDate.now().toString());
        }

        JLabel info = Theme.muted(current != null && current.isReturned()
                ? "The previous deposit was already returned. Saving records a new one."
                : "Usually one month's rent. It pays unpaid bills when the tenant moves out.");
        JPanel form = Theme.form(info, Theme.labeled("Deposit amount (\u20B1)", amount),
                Theme.labeled("Date received", date), Theme.labeled("Note", note));

        while (true) {
            int ok = JOptionPane.showConfirmDialog(this, form, "Security Deposit: " + fresh.getName(),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (ok != JOptionPane.OK_OPTION) return;

            double value;
            LocalDate received;
            try {
                value = Double.parseDouble(amount.getText().trim().replace(",", ""));
            } catch (NumberFormatException e) {
                msg("Enter a valid amount.");
                continue;
            }
            try {
                received = LocalDate.parse(date.getText().trim());
            } catch (DateTimeParseException e) {
                msg("The date must look like 2026-11-21.");
                continue;
            }

            String error = tenancy.recordDeposit(fresh, value, received, note.getText().trim());
            if (error == null) {
                msg("Deposit saved.");
                refresh();
                return;
            }
            msg(error);
        }
    }

    // ---------- Move out and restore ----------
    private void moveOut(Tenant t) {
        Tenant fresh = tenantDAO.findById(t.getTenantId());
        if (fresh == null) { msg("Tenant not found."); return; }
        if (fresh.isMovedOut()) { msg("This tenant has already moved out."); return; }

        TenancyService.MoveOutPreview p = tenancy.preview(fresh);
        Room r = fresh.getRoomId() == null ? null : roomDAO.findById(fresh.getRoomId());
        JPanel grid = Theme.detailGrid(new String[][]{
                {"Tenant", fresh.getName()},
                {"Room", r == null ? "-" : r.getRoomNo()},
                {"Unpaid balance", Ui.peso(p.unpaid())},
                {"Deposit held", Ui.peso(p.deposit())},
                {"Deposit used for unpaid bills", Ui.peso(p.deduction())},
                {"Refund to give the tenant", Ui.peso(p.refund())},
                {"Still unpaid after the deposit", Ui.peso(p.remainingDebt())}});
        JLabel note = Theme.muted("<html><body style='width:380px'>The tenant leaves their room and can no longer "
                + "log in. Their bills and payment history are kept, and you can restore them later.</body></html>");

        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setOpaque(false);
        content.add(grid, BorderLayout.CENTER);
        content.add(note, BorderLayout.SOUTH);

        Object[] options = {"Move Out", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this, content, "Move Out: " + fresh.getName(),
                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
        if (choice != 0) return;

        TenancyService.Result result = tenancy.moveOut(fresh);
        msg(result.message());
        onChange.run();
        refresh();
    }

    private void restore(Tenant t) {
        if (!Theme.confirm(this, "Make " + t.getName() + " an active tenant again?\nThey will be able to log in.",
                "Restore Tenant")) return;
        TenancyService.Result result = tenancy.restore(tenantDAO.findById(t.getTenantId()));
        msg(result.message());
        refresh();
    }

    // ---------- Rooms ----------
    private void assignRoom(List<Tenant> sel) {
        if (sel.isEmpty()) return;
        for (Tenant t : sel) {
            if (t.isMovedOut()) {
                msg("Restore moved-out tenants before assigning them a room.");
                return;
            }
        }
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
            double debt = tenancy.unpaidBalance(t.getTenantId());
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
            totalDebt += tenancy.unpaidBalance(sel.get(i).getTenantId());
            if (i < 5) names.append("  - ").append(sel.get(i).getName()).append("\n");
        }
        if (sel.size() > 5) names.append("  ... and ").append(sel.size() - 5).append(" more\n");

        String text = "Permanently delete " + (sel.size() == 1 ? "this tenant" : sel.size() + " tenants") + "?\n\n"
                + names + "\nThis removes their account, deposit, requests, bills, and payment records.\n"
                + "Their approved payments will no longer count in the income report.\n"
                + "For a tenant who is leaving, use Move Out instead to keep their history.";
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