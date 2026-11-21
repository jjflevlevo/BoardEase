package boardinghouse.gui;

import boardinghouse.dao.BillDAO;
import boardinghouse.dao.DepositDAO;
import boardinghouse.dao.PaymentDAO;
import boardinghouse.dao.RoomDAO;
import boardinghouse.dao.TenantDAO;
import boardinghouse.model.Bill;
import boardinghouse.model.Deposit;
import boardinghouse.model.Room;
import boardinghouse.model.Tenant;
import boardinghouse.service.PaymentService;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportsPanel extends Section {
    private final BillDAO billDAO = new BillDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final TenantDAO tenantDAO = new TenantDAO();
    private final RoomDAO roomDAO = new RoomDAO();
    private final DepositDAO depositDAO = new DepositDAO();
    private final PaymentService payments = new PaymentService();

    private final JTable overdue = Theme.table("Tenant", "Room", "Type", "Month", "Due Date", "Days Overdue",
            "Late Fee (\u20B1)", "Balance (\u20B1)", "Status");
    private final JTable income = Theme.table("Month", "Approved Income (\u20B1)");
    private final JTable balances = Theme.table("Tenant", "Room", "Status", "Remaining Debt (\u20B1)");
    private final JTable deposits = Theme.table("Tenant", "Room", "Amount (\u20B1)", "Received", "Status",
            "Used for Bills (\u20B1)", "Refunded (\u20B1)", "Settled");
    private final JLabel incomeTotal = totalLabel();
    private final JLabel unpaidTotal = totalLabel();
    private final JLabel heldTotal = totalLabel();

    public ReportsPanel() {
        JTabbedPane tabs = Ui.tabs();
        tabs.addTab("Overdue Bills", tab(overdue, null, "overdue_bills"));
        tabs.addTab("Monthly Income", tab(income, incomeTotal, "monthly_income"));
        tabs.addTab("Tenant Balances", tab(balances, unpaidTotal, "tenant_balances"));
        tabs.addTab("Deposits", tab(deposits, heldTotal, "deposits"));
        Ui.statusPills(overdue, 8);
        Ui.statusPills(balances, 2);
        Ui.statusPills(deposits, 4);
        add(tabs, BorderLayout.CENTER);
    }

    private static JLabel totalLabel() {
        JLabel l = new JLabel(" ");
        l.setFont(Theme.font(11, Font.BOLD));
        return l;
    }

    private JPanel tab(JTable t, JLabel total, String exportName) {
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);
        content.add(Ui.toolbar(t, Export.csvButton(t, exportName)), BorderLayout.NORTH);
        content.add(Theme.scroll(t), BorderLayout.CENTER);
        content.add(total == null ? Ui.footer(t) : Ui.footer(t, total), BorderLayout.SOUTH);

        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(16, 0, 0, 0));
        p.add(Ui.card(content), BorderLayout.CENTER);
        return p;
    }

    @Override
    public void refresh() {
        LocalDate today = LocalDate.now();

        // which room each tenant is in (current and former tenants)
        List<Tenant> everyone = tenantDAO.findEveryone();
        Map<Integer, String> roomOf = new HashMap<>();
        for (Tenant t : everyone) {
            Room r = t.getRoomId() == null ? null : roomDAO.findById(t.getRoomId());
            roomOf.put(t.getTenantId(), r == null ? "-" : r.getRoomNo());
        }

        // overdue bills
        List<Object[]> overdueRows = new ArrayList<>();
        for (Bill b : billDAO.findAll()) {
            if ("PAID".equals(b.getStatus()) || !today.isAfter(b.getDueDate())) continue;
            overdueRows.add(new Object[]{b.getTenantName(), roomOf.getOrDefault(b.getTenantId(), "-"), b.getType(),
                    b.getBillingMonth(), b.getDueDate().toString(), ChronoUnit.DAYS.between(b.getDueDate(), today),
                    Theme.money(b.getLateFee(today)), Theme.money(payments.balanceOf(b)), "OVERDUE"});
        }
        Theme.fill(overdue, overdueRows);

        // monthly income
        List<Object[]> incomeRows = new ArrayList<>();
        double totalIncome = 0;
        for (Map.Entry<String, Double> e : paymentDAO.incomeByMonth().entrySet()) {
            incomeRows.add(new Object[]{e.getKey(), Theme.money(e.getValue())});
            totalIncome += e.getValue();
        }
        Theme.fill(income, incomeRows);
        incomeTotal.setText("Total income: " + Ui.peso(totalIncome));

        // tenant balances (former tenants only appear while they still owe money)
        List<Object[]> balanceRows = new ArrayList<>();
        double grand = 0;
        for (Tenant t : everyone) {
            double debt = 0;
            for (Bill b : billDAO.findByTenant(t.getTenantId())) debt += payments.balanceOf(b);
            if (t.isMovedOut() && debt <= 0) continue;
            grand += debt;
            balanceRows.add(new Object[]{t.getName(), roomOf.getOrDefault(t.getTenantId(), "-"),
                    t.isMovedOut() ? "MOVED OUT" : "ACTIVE", Theme.money(debt)});
        }
        Theme.fill(balances, balanceRows);
        unpaidTotal.setText("Total unpaid: " + Ui.peso(grand));

        // deposits
        List<Object[]> depositRows = new ArrayList<>();
        for (Deposit d : depositDAO.findAll()) {
            boolean returned = d.isReturned();
            depositRows.add(new Object[]{d.getTenantName(), d.getRoomNo() == null ? "-" : d.getRoomNo(),
                    Theme.money(d.getAmount()), d.getReceivedOn(), d.getStatus(),
                    returned ? Theme.money(d.getDeducted()) : "-", returned ? Theme.money(d.getRefunded()) : "-",
                    d.getSettledOn() == null ? "-" : d.getSettledOn()});
        }
        Theme.fill(deposits, depositRows);
        heldTotal.setText("Total deposits held: " + Ui.peso(depositDAO.totalHeld()));
    }
}