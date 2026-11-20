package boardinghouse.ui;

import boardinghouse.dao.BillDAO;
import boardinghouse.dao.PaymentDAO;
import boardinghouse.dao.RoomDAO;
import boardinghouse.dao.TenantDAO;
import boardinghouse.model.*;
import boardinghouse.service.PaymentService;
import boardinghouse.util.InputHelper;
import java.time.LocalDate;
import java.util.List;

public class TenantMenu implements Menu {
    private final Account account;
    private final TenantDAO tenantDAO = new TenantDAO();
    private final RoomDAO roomDAO = new RoomDAO();
    private final BillDAO billDAO = new BillDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final PaymentService payments = new PaymentService();

    public TenantMenu(Account account) { this.account = account; }

    @Override
    public void show() {
        Tenant tenant = tenantDAO.findByAccountId(account.getAccountId());
        if (tenant == null) {
            System.out.println("No tenant profile found for this account.");
            return;
        }
        while (true) {
            System.out.println("\n=== Tenant Menu (" + tenant.getName() + ") ===");
            System.out.println("1. View my bills\n2. Make a payment\n3. Payment history\n4. My profile\n5. Logout");
            int c = InputHelper.readInt("Choose: ");
            if (c == 1) viewBills(tenant);
            else if (c == 2) makePayment(tenant);
            else if (c == 3) paymentHistory(tenant);
            else if (c == 4) profile(tenant);
            else if (c == 5) return;
            else System.out.println("Invalid choice.");
        }
    }

    private void profile(Tenant t) {
        Tenant fresh = tenantDAO.findByAccountId(account.getAccountId());
        Room room = fresh.getRoomId() == null ? null : roomDAO.findById(fresh.getRoomId());
        System.out.println("\n--- My profile ---");
        System.out.println("Name:         " + fresh.getName());
        System.out.println("Email:        " + account.getEmail());
        System.out.println("Phone:        " + account.getPhone());
        System.out.println("Address:      " + fresh.getAddress());
        System.out.println("Age:          " + fresh.getAge());
        System.out.println("Occupation:   " + fresh.getOccupation());
        System.out.println("Emergency no: " + fresh.getEmergencyNo());
        System.out.println("Room:         " + (room == null ? "Not assigned yet" : room.getRoomNo()));
    }

    private void viewBills(Tenant tenant) {
        List<Bill> bills = billDAO.findByTenant(tenant.getTenantId());
        if (bills.isEmpty()) { System.out.println("No bills yet."); return; }

        LocalDate today = LocalDate.now();
        double totalBalance = 0;
        System.out.printf("%-4s %-9s %-9s %-10s %-9s %-10s %-11s %s%n",
                "ID", "Type", "Month", "Amount", "Late fee", "Balance", "Due", "Status");
        for (Bill b : bills) {
            double balance = payments.balanceOf(b);
            totalBalance += balance;
            String status = b.getStatus();
            if (!"PAID".equals(status) && today.isAfter(b.getDueDate())) status += " (OVERDUE)";
            System.out.printf("%-4d %-9s %-9s %-10.2f %-9.2f %-10.2f %-11s %s%n", b.getBillId(), b.getType(),
                    b.getBillingMonth(), b.getAmount(), b.getLateFee(today), balance, b.getDueDate(), status);
        }
        System.out.printf("Total remaining debt: %.2f%n", totalBalance);
    }

    private void makePayment(Tenant tenant) {
        List<Bill> unpaid = billDAO.findByTenant(tenant.getTenantId()).stream()
                .filter(b -> !"PAID".equals(b.getStatus())).toList();
        if (unpaid.isEmpty()) { System.out.println("Nothing to pay. All bills are paid."); return; }

        LocalDate today = LocalDate.now();
        System.out.printf("%-4s %-9s %-9s %-10s %-9s %s%n", "ID", "Type", "Month", "Balance", "Late fee", "Due");
        for (Bill b : unpaid) {
            System.out.printf("%-4d %-9s %-9s %-10.2f %-9.2f %s%n", b.getBillId(), b.getType(), b.getBillingMonth(),
                    payments.balanceOf(b), b.getLateFee(today), b.getDueDate());
        }

        int billId = InputHelper.readInt("Bill ID to pay (0 to cancel): ");
        if (billId == 0) return;
        double amount = InputHelper.readDouble("Amount: ");

        System.out.println("1. Cash\n2. GCash");
        int m = InputHelper.readInt("Payment method: ");

        String result;
        if (m == 1) {
            result = payments.submitPayment(tenant.getTenantId(), billId, amount);
        } else if (m == 2) {
            String ref = InputHelper.readString("GCash reference no.: ");
            result = payments.submitPayment(tenant.getTenantId(), billId, amount, "GCASH", ref);
        } else {
            result = "Invalid payment method.";
        }
        System.out.println(result);
    }

    private void paymentHistory(Tenant tenant) {
        List<Payment> list = paymentDAO.findByTenant(tenant.getTenantId());
        if (list.isEmpty()) { System.out.println("No payments yet."); return; }
        System.out.printf("%-4s %-9s %-9s %-10s %-7s %-14s %-10s %s%n",
                "ID", "Bill", "Month", "Amount", "Method", "Ref no.", "Status", "Date");
        for (Payment p : list) {
            System.out.printf("%-4d %-9s %-9s %-10.2f %-7s %-14s %-10s %s%n", p.getPaymentId(), p.getBillType(),
                    p.getBillingMonth(), p.getAmount(), p.getMethod(),
                    p.getRefNo() == null ? "-" : p.getRefNo(), p.getStatus(), p.getPaidAt());
        }
    }
}